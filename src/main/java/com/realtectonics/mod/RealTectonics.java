package com.realtectonics.mod;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.realtectonics.sim.PlanetData;
import com.realtectonics.sim.PlanetSettings;
import com.realtectonics.sim.Terrain;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RealTectonics implements ModInitializer {
    public static final String MODID = "realtectonics";
    static final int POLE_MARGIN = 16; // blocks short of the map edge where the ice sheet stops you
    private static final Logger LOG = LogUtils.getLogger();

    // ponytail: one planet per running server; fine for single-player and dedicated servers
    private static volatile Terrain terrain;

    /** A planet the world-creation preview already simulated; the new world takes it instead of simulating again. */
    private record Preview(long seed, PlanetSettings settings, PlanetData data) {}

    private static volatile Preview preview;

    public static void offerPreview(long seed, PlanetSettings settings, PlanetData data) {
        preview = new Preview(seed, settings, data);
    }

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, Identifier.fromNamespaceAndPath(MODID, "planet"), TectonicChunkGenerator.CODEC);
        Registry.register(BuiltInRegistries.BIOME_SOURCE, Identifier.fromNamespaceAndPath(MODID, "planet"), TectonicBiomeSource.CODEC);
        MapTeleport.register();
        ServerLifecycleEvents.SERVER_STARTING.register(RealTectonics::onServerAboutToStart); // before the levels load
        ServerLifecycleEvents.SERVER_STARTED.register(RealTectonics::onServerStarted);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) onPlayerTick(player);
        });
    }

    /** The running planet, or null when no Tectonic Planet world is loaded in this game. */
    public static Terrain terrainOrNull() {
        return terrain;
    }

    public static Terrain terrain() {
        Terrain t = terrain;
        if (t == null) throw new IllegalStateException("Tectonic planet requested before the server loaded it");
        return t;
    }

    /** Runs before the levels load: simulate the planet once per world, then reuse the saved result. */
    private static void onServerAboutToStart(MinecraftServer server) {
        LevelStem overworld = server.registryAccess().lookupOrThrow(Registries.LEVEL_STEM).getValue(LevelStem.OVERWORLD);
        if (overworld == null || !(overworld.generator() instanceof TectonicChunkGenerator generator)) {
            terrain = null;
            return;
        }
        PlanetSettings settings = generator.planet();
        long seed = server.getWorldGenSettings().options().seed();
        Path file = server.getWorldPath(LevelResource.ROOT).resolve(MODID).resolve("planet.bin");
        try {
            PlanetData data;
            Preview p = preview;
            preview = null;
            if (Files.exists(file)) {
                data = PlanetData.load(file);
            } else {
                if (p != null && p.seed == seed && p.settings.sameSimulation(settings)) {
                    data = p.data;
                } else {
                    LOG.info("Simulating a new planet for seed {} with {}...", seed, settings);
                    long t = System.nanoTime();
                    data = PlanetData.generate(seed, settings, done -> {});
                    LOG.info("Planet simulated in {}s", String.format("%.1f", (System.nanoTime() - t) / 1e9));
                }
                data.save(file);
            }
            terrain = new Terrain(data, seed, settings.mountainScale(), settings.zoom(), settings.riverWidth());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load or save " + file, e);
        }
    }

    /**
     * With Immersive Portals installed, one wrap zone exactly a planet wide: walk off the east edge, come in
     * from the west, and see across the seam. Its north and south portals sit far past the poles, out of reach.
     * Set up once per world; without Immersive Portals the terrain still repeats every circumference.
     */
    private static void onServerStarted(MinecraftServer server) {
        Terrain t = terrain;
        if (t == null || !FabricLoader.getInstance().isModLoaded("imm_ptl_core")) return;
        Path marker = server.getWorldPath(LevelResource.ROOT).resolve(MODID).resolve("wrap.txt");
        if (Files.exists(marker)) return;
        int half = t.circumference / 2, far = 4 * t.circumference;
        // The second corner is an inclusive block, so -half .. half - 1 is exactly one circumference wide.
        String command = String.format("portal global create_inward_wrapping %d %d %d %d", -half, -far, half - 1, far - 1);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
        try {
            Files.writeString(marker, command);
        } catch (IOException e) {
            LOG.warn("Could not record the world wrap in {}; it may be set up again next start", marker, e);
        }
    }

    /**
     * The poles are an impassable ice sheet. A real pole crossing would mirror the player (you come out at the
     * opposite longitude heading south), which Minecraft cannot do, and wrapping to the other pole would be worse.
     */
    private static void onPlayerTick(ServerPlayer player) {
        Terrain t = terrain;
        if (t == null || player.level().dimension() != Level.OVERWORLD) return;
        double z = player.getZ(), north = t.northPoleZ() + POLE_MARGIN, south = t.southPoleZ() - POLE_MARGIN;
        if (z >= north && z <= south) return;
        Entity mover = player.getRootVehicle(); // boats and horses stop too
        mover.teleportTo(mover.getX(), mover.getY(), Math.clamp(z, north, south));
        player.sendOverlayMessage(Component.literal("The polar ice sheet is impassable."));
    }
}
