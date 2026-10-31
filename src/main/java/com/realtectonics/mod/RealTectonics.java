package com.realtectonics.mod;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.realtectonics.sim.PlanetData;
import com.realtectonics.sim.PlanetSettings;
import com.realtectonics.sim.Terrain;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod(RealTectonics.MODID)
public final class RealTectonics {
    public static final String MODID = "realtectonics";
    static final int POLE_MARGIN = 16; // blocks short of the map edge where the ice sheet stops you
    private static final Logger LOG = LogUtils.getLogger();

    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> GENERATORS = DeferredRegister.create(Registries.CHUNK_GENERATOR, MODID);
    private static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES = DeferredRegister.create(Registries.BIOME_SOURCE, MODID);

    static {
        GENERATORS.register("planet", () -> TectonicChunkGenerator.CODEC);
        BIOME_SOURCES.register("planet", () -> TectonicBiomeSource.CODEC);
    }

    // ponytail: one planet per running server; fine for single-player and dedicated servers
    private static volatile Terrain terrain;
    private static volatile MinecraftServer server; // for the registries the cave field is built from

    /** A planet the world-creation preview already simulated; the new world takes it instead of simulating again. */
    private record Preview(long seed, PlanetSettings settings, PlanetData data) {}

    private static volatile Preview preview;

    public static void offerPreview(long seed, PlanetSettings settings, PlanetData data) {
        preview = new Preview(seed, settings, data);
    }

    public RealTectonics(IEventBus modBus) {
        GENERATORS.register(modBus);
        BIOME_SOURCES.register(modBus);
        modBus.addListener(MapTeleport::register);
        NeoForge.EVENT_BUS.addListener(RealTectonics::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener(RealTectonics::onServerStarted);
        NeoForge.EVENT_BUS.addListener(RealTectonics::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(TectonicCommand::register);
    }

    /** The running planet, or null when no Tectonic Planet world is loaded in this game. */
    static net.minecraft.core.RegistryAccess registries() {
        return server.registryAccess();
    }

    public static Terrain terrainOrNull() {
        return terrain;
    }

    public static Terrain terrain() {
        Terrain t = terrain;
        if (t == null) throw new IllegalStateException("Tectonic planet requested before the server loaded it");
        return t;
    }

    /** Runs before the levels load: simulate the planet once per world, then reuse the saved result. */
    private static void onServerAboutToStart(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        RealTectonics.server = server;
        LevelStem overworld = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM).get(LevelStem.OVERWORLD);
        if (overworld == null || !(overworld.generator() instanceof TectonicChunkGenerator generator)) {
            terrain = null;
            return;
        }
        PlanetSettings settings = generator.planet();
        long seed = server.getWorldData().worldGenOptions().seed();
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
            terrain = new Terrain(data, seed, settings.mountainScale(), settings.zoom(), settings.riverWidth(), settings.cliffHeight(), settings.startPlace());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load or save " + file, e);
        }
    }

    /**
     * With Immersive Portals installed, one wrap zone exactly a planet wide: walk off the east edge, come in
     * from the west, and see across the seam. Its north and south portals sit far past the poles, out of reach.
     * Set up once per world; without Immersive Portals the terrain still repeats every circumference.
     */
    private static void onServerStarted(ServerStartedEvent event) {
        Terrain t = terrain;
        if (t == null || !ModList.get().isLoaded("immersive_portals_core")) return;
        MinecraftServer server = event.getServer();
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
    private static void onPlayerTick(PlayerTickEvent.Post event) {
        Terrain t = terrain;
        if (t == null || !(event.getEntity() instanceof ServerPlayer player) || player.level().dimension() != Level.OVERWORLD) return;
        double z = player.getZ(), north = t.northPoleZ() + POLE_MARGIN, south = t.southPoleZ() - POLE_MARGIN;
        if (z >= north && z <= south) return;
        Entity mover = player.getRootVehicle(); // boats and horses stop too
        mover.teleportTo(mover.getX(), mover.getY(), Math.clamp(z, north, south));
        player.displayClientMessage(Component.literal("The polar ice sheet is impassable."), true);
    }
}
