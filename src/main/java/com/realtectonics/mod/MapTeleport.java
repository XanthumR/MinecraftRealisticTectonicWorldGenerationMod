package com.realtectonics.mod;

import com.realtectonics.sim.Terrain;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Right-click on the planet map: the client asks, the server checks permission and lands you on the real surface. */
public record MapTeleport(int x, int z) implements CustomPacketPayload {
    public static final Type<MapTeleport> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RealTectonics.MODID, "map_teleport"));
    static final StreamCodec<RegistryFriendlyByteBuf, MapTeleport> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, MapTeleport::x, ByteBufCodecs.VAR_INT, MapTeleport::z, MapTeleport::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, CODEC, MapTeleport::handle); // handled on the server thread
    }

    private static void handle(MapTeleport request, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        Terrain t = RealTectonics.terrainOrNull();
        if (t == null || player.level().dimension() != Level.OVERWORLD) return;
        if (!player.hasPermissions(2)) { // same rule as /tp: cheats or operator
            player.sendSystemMessage(Component.literal("Teleporting from the map needs cheats enabled (or operator)."));
            return;
        }
        // Stay inside the wrap zone, where your builds are, and off the polar ice edge.
        int half = t.circumference / 2;
        int x = Math.floorMod(request.x + half, t.circumference) - half;
        int z = Math.clamp(request.z, t.northPoleZ() + RealTectonics.POLE_MARGIN + 1, t.southPoleZ() - RealTectonics.POLE_MARGIN - 1);
        ServerLevel level = player.serverLevel();
        level.getChunk(x >> 4, z >> 4); // generate it now, so the height below is the real surface, trees and water included
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        if (level.getFluidState(new BlockPos(x, y - 1, z)).is(FluidTags.LAVA)) {
            player.sendSystemMessage(Component.literal("That's a lava lake. Pick a spot beside it."));
            return;
        }
        player.teleportTo(level, x + 0.5, y, z + 0.5, player.getYRot(), player.getXRot());
    }
}
