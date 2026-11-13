package com.realtectonics.mod;

import net.minecraft.resources.Identifier;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.realtectonics.sim.Biome;
import com.realtectonics.sim.Deposits;
import com.realtectonics.sim.PlanetSettings;
import com.realtectonics.sim.Rock;
import com.realtectonics.sim.Terrain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds terrain columns straight from the simulated planet. A vanilla noise generator inside it only carves
 * caves and ravines and spawns the first animals; its settings (data/realtectonics/worldgen/noise_settings/planet.json)
 * have sea_level -64 so those caves stay dry, and applyCarvers seals caves away from surface water.
 * The real sea level comes from getSeaLevel().
 */
public final class TectonicChunkGenerator extends ChunkGenerator {
    /** Saved in the world's generator data, like a superflat world's layers. */
    static final Codec<PlanetSettings> PLANET_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.validate(w -> w >= 256 && w <= 4096 && w % 256 == 0 ? DataResult.success(w) : DataResult.error(() -> "Planet width must be a multiple of 256 between 256 and 4096: " + w))
                    .fieldOf("width").forGetter(PlanetSettings::width),
            Codec.intRange(2, 60).fieldOf("plates").forGetter(PlanetSettings::plates),
            Codec.floatRange(0.3f, 0.95f).fieldOf("ocean_fraction").forGetter(PlanetSettings::oceanFraction),
            Codec.floatRange(0.25f, 2f).fieldOf("mountain_scale").forGetter(PlanetSettings::mountainScale),
            Codec.INT.validate(z -> z >= 1 && z <= 8 && Integer.bitCount(z) == 1 ? DataResult.success(z) : DataResult.error(() -> "World scale must be 1, 2, 4 or 8: " + z))
                    .optionalFieldOf("zoom", 1).forGetter(PlanetSettings::zoom), // worlds from before world scale stay at 1
            Codec.floatRange(0, 2).optionalFieldOf("volcanoes", 1f).forGetter(PlanetSettings::volcanoes),
            Codec.intRange(50, 1000).optionalFieldOf("steps", 300).forGetter(PlanetSettings::steps),
            Codec.floatRange(0.25f, 8).optionalFieldOf("river_width", 1f).forGetter(PlanetSettings::riverWidth), // older worlds keep their rivers
            Codec.BOOL.optionalFieldOf("earth", false).forGetter(PlanetSettings::earth),
            Codec.BOOL.optionalFieldOf("medium_continents", false).forGetter(PlanetSettings::mediumContinents),
            Codec.floatRange(4, 64).optionalFieldOf("cliff_height", PlanetSettings.CLIFF_HEIGHT).forGetter(PlanetSettings::cliffHeight),
            Codec.intRange(0, PlanetSettings.START_NAMES.length - 1).optionalFieldOf("start", 0).forGetter(PlanetSettings::start) // older Earth worlds keep spawn where the seed put it
    ).apply(i, PlanetSettings::new));

    public static final MapCodec<TectonicChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(g -> g.settings),
            PLANET_CODEC.optionalFieldOf("planet", PlanetSettings.DEFAULT).forGetter(TectonicChunkGenerator::planet)
    ).apply(i, TectonicChunkGenerator::new));

    private static final BlockState AIR = Blocks.AIR.defaultBlockState(), STONE = Blocks.STONE.defaultBlockState(),
            DEEPSLATE = Blocks.DEEPSLATE.defaultBlockState(), BEDROCK = Blocks.BEDROCK.defaultBlockState(),
            WATER = Blocks.WATER.defaultBlockState(), ICE = Blocks.ICE.defaultBlockState(), SNOW = Blocks.SNOW.defaultBlockState(), SNOW_BLOCK = Blocks.SNOW_BLOCK.defaultBlockState(),
            LAVA = Blocks.LAVA.defaultBlockState(), MAGMA = Blocks.MAGMA_BLOCK.defaultBlockState(),
            PACKED_ICE = Blocks.PACKED_ICE.defaultBlockState(), BLUE_ICE = Blocks.BLUE_ICE.defaultBlockState(),
            MORAINE = Blocks.COBBLED_DEEPSLATE.defaultBlockState(); // rock debris riding on glacier ice (format 40)
    /** Format 93: moraine is whatever the glacier scraped off, mixed, not one dark block (all bare rock, so no snow pass on it). */
    private static final BlockState[] MORAINES = {MORAINE, MORAINE, Blocks.STONE.defaultBlockState(), Blocks.ANDESITE.defaultBlockState(), Blocks.TUFF.defaultBlockState()};
    static final int BLUE_ICE_DEPTH = 6; // glacier ice this far down is squeezed clear of air bubbles
    static final int FIRN_DEPTH = 2;     // blocks of snow and firn over glacier ice above the snow line, where it gains each winter's snow (format 57)

    /** A null top or filler means bare rock: the column's rock layers show through. */
    private record Surface(BlockState top, BlockState filler, boolean snowLayer) {}

    static final int LAYER_THICKNESS = 6;
    // Format 34: beds 3 to 10 blocks thick, drawn once; STRATA_BED gives the bed at each height within the repeating stack.
    static final int STRATA_PERIOD = 1024;
    static final int[] STRATA_BED = new int[STRATA_PERIOD];
    static final int STRATA_BEDS;
    static {
        java.util.Random r = new java.util.Random(34);
        int bed = 0;
        for (int y = 0; y < STRATA_PERIOD; bed++)
            for (int k = 3 + r.nextInt(8); k > 0 && y < STRATA_PERIOD; k--) STRATA_BED[y++] = bed;
        STRATA_BEDS = bed;
    }
    static final float DESERT_EDGE = 0.3f, STEPPE_EDGE = 0.8f; // aridity over which desert opens into scrub, and up to which steppe has sandy patches
    private static final BlockState GRANITE = Blocks.GRANITE.defaultBlockState(), DIORITE = Blocks.DIORITE.defaultBlockState(),
            ANDESITE = Blocks.ANDESITE.defaultBlockState(), TUFF = Blocks.TUFF.defaultBlockState(), CALCITE = Blocks.CALCITE.defaultBlockState(),
            BASALT = Blocks.BASALT.defaultBlockState(), SANDSTONE = Blocks.SANDSTONE.defaultBlockState();
    /** Rock layers per geological province (Rock ordinal order). Stone stays common so vanilla ores keep their hosts. */
    private static final BlockState[][] PROVINCE_LAYERS = {
            {STONE, STONE, ANDESITE, STONE, DIORITE, TUFF, STONE, GRANITE, CALCITE, ANDESITE},  // MIXED: worlds from before provinces
            {DEEPSLATE, DEEPSLATE, BASALT, TUFF, DEEPSLATE, Blocks.SMOOTH_BASALT.defaultBlockState()}, // OCEANIC
            {GRANITE, GRANITE, STONE, DIORITE, GRANITE, STONE, STONE},                              // CRATON
            {DEEPSLATE, STONE, CALCITE, TUFF, DEEPSLATE, STONE, ANDESITE},                          // OROGEN
            {ANDESITE, ANDESITE, TUFF, STONE, BASALT, DIORITE, STONE},                              // ARC
            {SANDSTONE, STONE, CALCITE, SANDSTONE, STONE, Blocks.SMOOTH_SANDSTONE.defaultBlockState(), STONE}}; // BASIN
    private static final BlockState[] ARID_LAYERS = {
            Blocks.TERRACOTTA.defaultBlockState(), Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), Blocks.DYED_TERRACOTTA.yellow().defaultBlockState(),
            Blocks.TERRACOTTA.defaultBlockState(), Blocks.DYED_TERRACOTTA.red().defaultBlockState(), Blocks.DYED_TERRACOTTA.white().defaultBlockState(),
            Blocks.DYED_TERRACOTTA.brown().defaultBlockState(), Blocks.DYED_TERRACOTTA.lightGray().defaultBlockState(), Blocks.SANDSTONE.defaultBlockState()};
    static final int ARID_DEPTH = 48;  // terracotta bands only near the surface, so ores still have stone deeper down
    static final int CLIFF_STEP = 4;   // blocks up in one step that make bare rock
    static final float SNOW_SLOPE = 0.8f, SNOW_SHADE = 1.2f, SNOW_PATCH = 1.0f, SNOW_SCOUR = 0.5f; // see snowStays
    private static final BlockState[] VOLCANIC_LAYERS = {
            Blocks.BASALT.defaultBlockState(), Blocks.TUFF.defaultBlockState(), Blocks.ANDESITE.defaultBlockState(), Blocks.BASALT.defaultBlockState(),
            Blocks.BLACKSTONE.defaultBlockState(), Blocks.TUFF.defaultBlockState(), Blocks.SMOOTH_BASALT.defaultBlockState()};
    static final int VOLCANIC_DEPTH = 40; // lava flows and ash layered over the bedrock below

    private static final Surface GRASS = new Surface(Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), false),
            SNOWY_GRASS = new Surface(Blocks.GRASS_BLOCK.defaultBlockState().setValue(SnowyBlock.SNOWY, true), Blocks.DIRT.defaultBlockState(), true),
            PODZOL = new Surface(Blocks.PODZOL.defaultBlockState(), Blocks.DIRT.defaultBlockState(), false),
            SAND = new Surface(Blocks.SAND.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false),
            BEACH = new Surface(Blocks.SAND.defaultBlockState(), Blocks.SAND.defaultBlockState(), false),
            GRAVEL = new Surface(Blocks.GRAVEL.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), false),
            SPRING = new Surface(Blocks.SOUL_SAND.defaultBlockState(), Blocks.CALCITE.defaultBlockState(), false),
            SINTER = new Surface(Blocks.DYED_TERRACOTTA.white().defaultBlockState(), Blocks.CALCITE.defaultBlockState(), false),
            SINTER_STAINED = new Surface(Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), Blocks.CALCITE.defaultBlockState(), false), // bacterial mats
            RED_SAND = new Surface(Blocks.RED_SAND.defaultBlockState(), Blocks.RED_SANDSTONE.defaultBlockState(), false),
            COARSE = new Surface(Blocks.COARSE_DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), false),
            SAND_BANK = new Surface(Blocks.SAND.defaultBlockState(), Blocks.SAND.defaultBlockState(), false),
            SALT = new Surface(Blocks.CALCITE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false), // a white salt crust
            MUD = new Surface(Blocks.MUD.defaultBlockState(), Blocks.MUD.defaultBlockState(), false),
            CLAY = new Surface(Blocks.CLAY.defaultBlockState(), Blocks.DIRT.defaultBlockState(), false),
            SNOW_PACK = new Surface(Blocks.SNOW_BLOCK.defaultBlockState(), Blocks.PACKED_ICE.defaultBlockState(), false),
            ROCK = new Surface(null, null, false),
            ASH = new Surface(Blocks.TUFF.defaultBlockState(), Blocks.TUFF.defaultBlockState(), false), // fresh ash fall (format 86): no soil yet
            CHIMNEY_CAP = new Surface(Blocks.ANDESITE.defaultBlockState(), Blocks.TUFF.defaultBlockState(), false), // a fairy chimney's hard cap (format 90)
            TRAVERTINE = new Surface(Blocks.CALCITE.defaultBlockState(), Blocks.CALCITE.defaultBlockState(), false), // a hot spring's terraces (format 91)
            SCREE_STONE = new Surface(Blocks.COBBLESTONE.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), false),
            MAGMA_PATCH = new Surface(Blocks.MAGMA_BLOCK.defaultBlockState(), null, false), // hot rock breaking through an active volcano's ground (format 58)
            GOSSAN = new Surface(Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), Blocks.DYED_TERRACOTTA.brown().defaultBlockState(), false), // rusted sulphides over a porphyry (format 92)
            RED_SOIL = new Surface(Blocks.DYED_TERRACOTTA.red().defaultBlockState(), Blocks.DYED_TERRACOTTA.red().defaultBlockState(), false), // weathered iron bed (format 92)
            BLUE_GROUND = new Surface(Blocks.CLAY.defaultBlockState(), Blocks.TUFF.defaultBlockState(), false); // kimberlite weathered to clay (format 92)
    /** Format 92: each Deposits.Ore's block in stone and in deepslate; NONE is never placed. */
    private static final BlockState[][] ORE_BLOCKS = {
            {STONE, STONE},
            {Blocks.COPPER_ORE.defaultBlockState(), Blocks.DEEPSLATE_COPPER_ORE.defaultBlockState()},
            {Blocks.GOLD_ORE.defaultBlockState(), Blocks.DEEPSLATE_GOLD_ORE.defaultBlockState()},
            {Blocks.IRON_ORE.defaultBlockState(), Blocks.DEEPSLATE_IRON_ORE.defaultBlockState()},
            {Blocks.COAL_ORE.defaultBlockState(), Blocks.DEEPSLATE_COAL_ORE.defaultBlockState()},
            {Blocks.DIAMOND_ORE.defaultBlockState(), Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState()},
            {Blocks.LAPIS_ORE.defaultBlockState(), Blocks.DEEPSLATE_LAPIS_ORE.defaultBlockState()},
            {Blocks.REDSTONE_ORE.defaultBlockState(), Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState()},
            {TUFF, TUFF},           // kimberlite's blue ground
            {CALCITE, CALCITE}};    // a vein's quartz
    /** Format 92: vanilla's ores, of which only ORE_KEEP stay outside the deposits; emerald is mountain-only already. */
    private static final java.util.Set<net.minecraft.world.level.block.Block> THINNED = new java.util.HashSet<>();
    static {
        for (BlockState[] o : ORE_BLOCKS) if (o[0] != STONE && o[0] != TUFF && o[0] != CALCITE) { THINNED.add(o[0].getBlock()); THINNED.add(o[1].getBlock()); }
    }
    static final float ORE_KEEP = 0.35f;

    private final PlanetSettings planet;
    private final Holder<NoiseGeneratorSettings> settings;
    private final NoiseBasedChunkGenerator vanilla; // caves, ravines and first animals; it is final, so held rather than extended
    private volatile Caves caves;           // noise caves for the world's RandomState below (format 59)
    private volatile RandomState cavesFor;

    public TectonicChunkGenerator(BiomeSource biomes, Holder<NoiseGeneratorSettings> settings, PlanetSettings planet) {
        super(biomes, TectonicChunkGenerator::decoration);
        this.planet = planet;
        this.settings = settings;
        this.vanilla = new NoiseBasedChunkGenerator(biomes, settings);
    }

    /**
     * Each biome's features without William Wythers' Overhauled Overworld's ground disks and extra ores: 30-256 disks a
     * chunk, each testing ~2,000 blocks and their neighbours to find slopes to paint stone, scree or snow on (half the
     * generation time on our land), and 50-100 ore veins a chunk in windswept and volcanic country. Our columns already
     * lay rock on steep slopes, scree and snow from the planet, and our deposits put the ores where the geology did;
     * WWOO's trees, plants and rocks stay. Only the per-chunk lookup changes: the step indices, so other features' seeds, stay.
     */
    private static BiomeGenerationSettings decoration(Holder<net.minecraft.world.level.biome.Biome> biome) {
        return DECORATION.computeIfAbsent(biome, b -> {
            BiomeGenerationSettings s = b.value().getGenerationSettings();
            BiomeGenerationSettings.PlainBuilder out = new BiomeGenerationSettings.PlainBuilder();
            s.getCarvers().forEach(out::addCarver);
            List<net.minecraft.core.HolderSet<PlacedFeature>> steps = s.features();
            for (int step = 0; step < steps.size(); step++)
                for (Holder<PlacedFeature> f : steps.get(step)) if (!groundDisk(f)) out.addFeature(step, f);
            return out.build();
        });
    }

    private static final Map<Holder<net.minecraft.world.level.biome.Biome>, BiomeGenerationSettings> DECORATION = new ConcurrentHashMap<>();

    private static boolean groundDisk(Holder<PlacedFeature> f) {
        Feature<?> type = f.value().feature().value().feature();
        return (type == Feature.DISK || type == Feature.ORE || type == Feature.SCATTERED_ORE)
                && f.unwrapKey().map(k -> k.identifier().getNamespace().equals("wythers")).orElse(false);
    }

    public PlanetSettings planet() {
        return planet;
    }

    public TectonicChunkGenerator withPlanet(PlanetSettings planet) {
        return new TectonicChunkGenerator(getBiomeSource(), settings, planet);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public int getMinY() {
        return Terrain.MIN_Y;
    }

    @Override
    public int getGenDepth() {
        return Terrain.MAX_Y - Terrain.MIN_Y + 1;
    }

    @Override
    public int getSeaLevel() {
        return Terrain.SEA_Y;
    }

    /**
     * Where the noise step runs. Minecraft calls it from its single worldgen mailbox, one chunk at a time, and vanilla's
     * own noise hands its work to a pool; done in place, ours held every chunk's steps behind it, and the workers sat
     * idle three quarters of the time (JFR: ~47% CPU on 12 cores). Plain threads: the terrain tiles wait on futures, which
     * deadlocked on a ForkJoin pool (a jshell timing on the common pool hung). The blocks don't depend on the thread.
     */
    private static final java.util.concurrent.ExecutorService FILL = java.util.concurrent.Executors.newFixedThreadPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() - 1), r -> {
                Thread t = new Thread(r, "Tectonic chunk fill");
                t.setPriority(Thread.NORM_PRIORITY - 1); // below the render and server threads, as the tile pool is (see Terrain.TILE_POOL)
                t.setDaemon(true);
                return t;
            });

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random, StructureManager structures, ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(() -> fill(random, structures, chunk), FILL);
    }

    private ChunkAccess fill(RandomState random, StructureManager structures, ChunkAccess chunk) {
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ(), minY = chunk.getMinY();
        Terrain.Column[] cols = columns(chunk, structures);
        int minSection = chunk.getMinSectionY();

        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        Caves.Field caveField = null;
        if (RealTectonics.terrain().noiseCaves) {
            int highest = minY;
            for (Terrain.Column c : cols) highest = Math.max(highest, c.ground());
            caveField = caves(random).new Field(x0, z0, minY, highest);
        }
        int[] ceilings = RealTectonics.terrain().deepCaves ? caveCeilings(x0, z0, cols) : null;
        for (int lz = 0; lz < 16; lz++)
            for (int lx = 0; lx < 16; lx++) {
                int i = lx + 1 + (lz + 1) * 18;
                Terrain.Column c = cols[i];
                // Steepness of the land surface: a neighbour under water counts at its water level, so a bank beside a
                // deep riverbed is not mistaken for a cliff.
                int slope = Math.max(Math.max(Math.abs(c.ground() - landTop(cols[i - 1])), Math.abs(c.ground() - landTop(cols[i + 1]))),
                        Math.max(Math.abs(c.ground() - landTop(cols[i - 18])), Math.abs(c.ground() - landTop(cols[i + 18]))));
                boolean wet = c.water() >= c.ground(), hot = c.lava() != Terrain.Column.NO_WATER, glacier = c.ice() != Terrain.Column.NO_WATER;
                // Which way the ground faces: rising toward the south means a north face, shaded, where snow lasts.
                boolean shaded = landTop(cols[i + 18]) > landTop(cols[i - 18]);
                Surface s = glacier ? GRAVEL : surface(c, slope, wet);
                // Gameplay over realism (format 47): beds, bars, fans, moraines and scree lose their gray gravel patches.
                // From format 53 the land ones get the biome's own ground, as if no gravel lay there, not coarse dirt.
                if (!glacier && s == GRAVEL && RealTectonics.terrain().noGravel) s = wet ? BEACH : scree(c, slope) ? ROCK
                        : RealTectonics.terrain().plainGround ? surface(new Terrain.Column(c.ground(), c.water(), c.lava(), c.ice(), c.biome(), c.fold(),
                                c.volcanic(), c.rock(), Terrain.Cover.NONE), slope, wet) : COARSE;
                if (!glacier && !wet && RealTectonics.terrain().gentleShores) s = dryEdge(s, c, x0 + lx, z0 + lz);
                if (!glacier && !wet && !hot && c.volcanic() && RealTectonics.terrain().magmaPatch(x0 + lx, z0 + lz)) s = MAGMA_PATCH;
                if (!glacier && !wet) s = moddedGround(chunk, s, c, x0 + lx, z0 + lz);
                Deposits.Lode lode = RealTectonics.terrain().lode(x0 + lx, z0 + lz, c);
                if (lode != null && !glacier && !wet) s = switch (Deposits.sign(lode, c.ground(), layerHash(c, c.ground()))) { // what a prospector looks for
                    case GOSSAN -> GOSSAN;
                    case RED_SOIL -> RED_SOIL;
                    case BLUE_GROUND -> BLUE_GROUND;
                    case NONE -> s;
                };
                if (!glacier && !wet && !scree(c, slope) && alpine(c.biome()) && !snowStays(slope, shaded, RealTectonics.terrain().patch(x0 + lx, z0 + lz)))
                    s = ROCK; // steep faces, ridges and wind-scoured patches stand bare above the snow
                Terrain terrain = RealTectonics.terrain();
                boolean frozen = frozen(terrain, c, x0 + lx, z0 + lz);
                boolean firn = glacier && terrain.firn && c.cover() != Terrain.Cover.BARE_ICE && c.cover() != Terrain.Cover.MORAINE;
                // Snow lies wherever the planet's climate is below freezing, except on bare rock and warm ground.
                boolean snowLayer = terrain.snowLine
                        ? !wet && !hot && !glacier && s.top != null && !warmGround(s) && terrain.snowTemperature(x0 + lx, z0 + lz, c.ground()) < 0
                        : s.snowLayer;
                if (terrain.snowLine && s.top != null && s.top.hasProperty(SnowyBlock.SNOWY))
                    s = new Surface(s.top.setValue(SnowyBlock.SNOWY, snowLayer), s.filler, snowLayer);
                // Snow, ice or a structure's foundation can lift the top past the build limit: never write above it.
                int top = Math.min(wet ? c.water() : hot ? c.lava() : glacier ? c.ice() : c.ground() + (snowLayer ? 1 : 0), chunk.getMaxY());

                // Caves keep a roof under water, lava and ice, here or beside; elsewhere entrances may open at the surface.
                boolean held = wet || hot || glacier;
                for (int n : new int[]{i - 1, i + 1, i - 18, i + 18})
                    held |= cols[n].water() >= cols[n].ground() || cols[n].lava() != Terrain.Column.NO_WATER || cols[n].ice() != Terrain.Column.NO_WATER;
                int caveTop = caveField == null ? Integer.MIN_VALUE : c.ground() - (held ? Caves.ROOF : 0), solidTop = minY;
                if (ceilings != null) caveTop = Math.min(caveTop, ceilings[lx + lz * 16]);
                // From format 72 no pillars where the cave is open straight up to the sky: vanilla's stand in closed caverns,
                // but where an entrance opened a cavern from above they rose from the pit, cut off flat where the entrance
                // layer began (the user: "there is this kind of pillars in caves"). Pillars under the pit's rim stay.
                int open = Integer.MAX_VALUE; // lowest y of the cave open straight up from here
                if (terrain.openCaves && caveTop == c.ground())
                    for (int y = caveTop; y > minY + Caves.FLOOR && caveField.air(lx, y, lz, c.ground() - y, false); y--) open = y;
                BlockState solidState = BEDROCK;
                LevelChunkSection section = null;
                int sectionIndex = -1;
                for (int y = minY; y <= top; y++) {
                    BlockState state = y == minY ? BEDROCK
                            : y < c.ground() - 4 ? rock(c, y)
                            : y < c.ground() ? (s.filler == null ? rock(c, y) : s.filler)
                            : y == c.ground() ? (hot ? MAGMA : s.top == null ? rock(c, y) : s.top)
                            : wet ? (frozen && y == c.water() ? ICE : WATER)
                            : hot ? LAVA
                            : glacier ? (y == c.ice() && c.cover() == Terrain.Cover.MORAINE
                                    ? terrain.raggedMoraine ? MORAINES[(int) (oreHash(x0 + lx, y, z0 + lz) * MORAINES.length)] : MORAINE
                                    : firn && c.ice() - y < FIRN_DEPTH ? SNOW_BLOCK // above the snow line (no bare ice, no rubble): last winters' snow
                                    : c.ice() - y >= BLUE_ICE_DEPTH || y == c.ice() && c.cover() == Terrain.Cover.BARE_ICE ? BLUE_ICE : PACKED_ICE)
                            : SNOW;
                    if (lode != null && y > minY && (y < c.ground() - 4 || y >= c.ground() - 1 && y <= c.ground())) { // ore in the rock, gold in a river's bed
                        Deposits.Ore o = Deposits.ore(lode, y, c.ground(), layerHash(c, y), oreHash(x0 + lx, y, z0 + lz));
                        if (o != Deposits.Ore.NONE) state = ORE_BLOCKS[o.ordinal()][y < 0 || state == DEEPSLATE ? 1 : 0];
                    }
                    if (y <= caveTop && y > minY + Caves.FLOOR && caveField.air(lx, y, lz, c.ground() - y, y < open)) state = y < Caves.LAVA_BELOW ? LAVA : AIR;
                    else if (y <= c.ground()) {
                        solidTop = y;
                        solidState = state;
                    } else if (state == SNOW && solidTop < c.ground()) state = AIR; // no snow over a cave's open mouth
                    int index = (y >> 4) - minSection; // chunk.getSectionIndex, without asking the chunk for its floor every block
                    if (index != sectionIndex) {
                        section = chunk.getSection(index);
                        sectionIndex = index;
                    }
                    section.setBlockState(lx, y & 15, lz, state, false);
                }
                if (wet && c.cover() == Terrain.Cover.CENOTE) hangVines(chunk, cols, i, c, x0 + lx, z0 + lz, lx, lz);
                // Where a river steps down a block, let the edge water flow when the chunk loads: rapids, not a frozen wall.
                // From format 54 only falls of two or more: over a one-block step the water spreads as a sheet on the water below,
                // and at each inner corner of a diagonal step, between two sources, Minecraft makes it a new source that spreads on.
                if (wet && lowestNeighbourTop(cols, i) < c.water() - (RealTectonics.terrain().stillSteps ? 1 : 0))
                    chunk.markPosForPostProcessing(new BlockPos(x0 + lx, c.water(), z0 + lz));
                BlockState surfaceBlock = s.top == null ? rock(c, c.ground()) : s.top;
                if (solidTop < c.ground() && !held) { // a cave opens at the surface here: the highest solid block is lower down
                    oceanFloor.update(lx, solidTop, lz, solidState);
                    worldSurface.update(lx, solidTop, lz, solidState);
                    continue;
                }
                oceanFloor.update(lx, glacier ? c.ice() : c.ground(), lz, glacier ? PACKED_ICE : surfaceBlock);
                worldSurface.update(lx, top, lz, top == c.ground() ? surfaceBlock : wet ? WATER : hot ? LAVA : glacier ? PACKED_ICE : SNOW);
            }
        return chunk;
    }

    /**
     * The chunk's columns plus a one-block border, so edge columns see their neighbours. Index lx + 1 + (lz + 1) * 18.
     * Ground near village houses and other jigsaw pieces is eased onto them, as vanilla's noise does.
     */
    private static Terrain.Column[] columns(ChunkAccess chunk, StructureManager structures) {
        Terrain terrain = RealTectonics.terrain();
        Beardifier beard = Beardifier.forStructuresInChunk(structures, chunk.getPos());
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
        Terrain.Column[] cols = new Terrain.Column[18 * 18], relaxed = terrain.relaxedColumns(x0 - 1, z0 - 1, 18, 18);
        if (beard == Beardifier.EMPTY) return relaxed; // no structure here, as in most chunks: the ground stands as it is
        Probe probe = new Probe();
        for (int dz = -1; dz <= 16; dz++)
            for (int dx = -1; dx <= 16; dx++) cols[dx + 1 + (dz + 1) * 18] = beardify(relaxed[dx + 1 + (dz + 1) * 18], beard, x0 + dx, z0 + dz, probe);
        return cols;
    }

    static final int BEARD_REACH = 24;     // blocks the ground may rise or sink under a structure
    static final double BEARD_SLOPE = 24;  // blocks per unit of density: vanilla's surface responds about this softly

    /**
     * Foundations under structures and clearance above them. The column's ground becomes a density, solid below
     * and air above, and the beardifier adds to it near each piece; the new ground is the top solid block.
     */
    private static Terrain.Column beardify(Terrain.Column c, Beardifier beard, int x, int z, Probe probe) {
        int top = c.ground() - BEARD_REACH;
        probe.x = x;
        probe.z = z;
        for (int y = c.ground() + BEARD_REACH; y > top; y--)
            if ((c.ground() + 0.5 - y) / BEARD_SLOPE + beard.compute(probe.at(y)) > 0) {
                top = y;
                break;
            }
        if (top == c.ground()) return c;
        int none = Terrain.Column.NO_WATER;
        return new Terrain.Column(top, c.water() > top ? c.water() : none, c.lava() > top ? c.lava() : none, c.ice() > top ? c.ice() : none,
                c.biome(), c.fold(), c.volcanic(), c.rock(), c.cover());
    }

    /**
     * One reusable point for the beardifier: a new one per block tested was a few percent of all the garbage while
     * chunks generate nonstop, and garbage is what stalls the game (ZGC allocation stalls).
     */
    private static final class Probe implements DensityFunction.FunctionContext {
        int x, y, z;

        Probe at(int y) {
            this.y = y;
            return this;
        }

        @Override public int blockX() { return x; }
        @Override public int blockY() { return y; }
        @Override public int blockZ() { return z; }
    }

    private static int landTop(Terrain.Column c) {
        return Math.max(c.ground(), c.water());
    }

    /** The noise caves of this world's RandomState, built on first use. */
    private Caves caves(RandomState random) {
        Caves c = caves;
        if (cavesFor != random) {
            c = new Caves(RealTectonics.registries(), random);
            caves = c;
            cavesFor = random;
        }
        return c;
    }

    private static int lowestNeighbourTop(Terrain.Column[] cols, int i) {
        int lowest = Integer.MAX_VALUE;
        for (int n : new int[]{i - 1, i + 1, i - 18, i + 18}) lowest = Math.min(lowest, Math.max(cols[n].ground(), fluid(cols[n])));
        return lowest;
    }

    /** Top water or lava block, or NO_WATER. */
    private static int fluid(Terrain.Column c) {
        return Math.max(c.water(), c.lava());
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState random, BiomeManager biomes, StructureManager structures, ChunkAccess chunk) {
        Terrain.Column[] cols = columns(chunk, structures);
        int[] ceilings = RealTectonics.terrain().deepCaves ? caveCeilings(chunk.getPos().getMinBlockX(), chunk.getPos().getMinBlockZ(), cols) : null;
        BlockState[][] roofs = ceilings == null ? null : roofs(chunk, cols, ceilings);
        vanilla.applyCarvers(region, seed, random, biomes, structures, chunk);
        sealCavesFromWater(chunk, cols, ceilings, roofs);
        if (RealTectonics.terrain().newBiomes && sulfurRock().length > 0) lineSulfurCaves(chunk, cols, sulfurRock());
    }

    /**
     * Format 98: each column's blocks from just over its cave ceiling to one over its ground (a snow layer), as filled
     * before the carvers cut their tunnels: put back where a tunnel cut them, a roofed cave keeps the grass, soil, sand
     * and snow of the land round it, not a patch of bare stone.
     */
    private static BlockState[][] roofs(ChunkAccess chunk, Terrain.Column[] cols, int[] ceilings) {
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState[][] out = new BlockState[256][];
        for (int lz = 0; lz < 16; lz++)
            for (int lx = 0; lx < 16; lx++) {
                int i = lx + lz * 16, lo = roofBottom(chunk, ceilings[i]), hi = Math.min(cols[lx + 1 + (lz + 1) * 18].ground() + 1, chunk.getMaxY());
                BlockState[] band = new BlockState[Math.max(0, hi - lo + 1)];
                for (int y = lo; y <= hi; y++) band[y - lo] = chunk.getBlockState(pos.set(x0 + lx, y, z0 + lz));
                out[i] = band;
            }
        return out;
    }

    private static int roofBottom(ChunkAccess chunk, int ceiling) {
        return Math.max(ceiling + 1, chunk.getMinY() + 1);
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        vanilla.spawnOriginalMobs(region);
    }

    /**
     * Carvers run with no sea at all, and vanilla lets them carve water (overworld_carver_replaceables), so they
     * leave air tunnels through seas, lakes and rivers. Undo that: water goes back above the bed, rock below it
     * and beside it. Crater lava lakes get the same treatment.
     */
    private static void sealCavesFromWater(ChunkAccess chunk, Terrain.Column[] cols, int[] ceilings, BlockState[][] roofs) {
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lz = 0; lz < 16; lz++)
            for (int lx = 0; lx < 16; lx++) {
                int i = lx + 1 + (lz + 1) * 18;
                Terrain.Column c = cols[i];
                int nearFluid = Math.max(Math.max(fluid(c), Math.max(fluid(cols[i - 1]), fluid(cols[i + 1]))),
                        Math.max(fluid(cols[i - 18]), fluid(cols[i + 18])));
                int top = Math.min(Math.max(fluid(c), Math.min(nearFluid, c.ground())), chunk.getMaxY());
                // From format 43 only where air can touch the fluid: water and lava lie above the ground, so from the
                // lowest ground here or beside up. Caves deeper down stay, closed off and dry, instead of being filled.
                int bottom = RealTectonics.terrain().fastMath ? Math.max(chunk.getMinY() + 1, Math.min(Math.min(c.ground(),
                        Math.min(cols[i - 1].ground(), cols[i + 1].ground())), Math.min(cols[i - 18].ground(), cols[i + 18].ground()))) : chunk.getMinY() + 1;
                if (ceilings != null) { // format 98: the carvers' tunnels kept as deep as the noise caves, filled back as they were
                    BlockState[] band = roofs[lx + lz * 16];
                    for (int k = 0, lo = roofBottom(chunk, ceilings[lx + lz * 16]); k < band.length; k++)
                        if (!band[k].isAir() && chunk.getBlockState(pos.set(x0 + lx, lo + k, z0 + lz)).isAir()) chunk.setBlockState(pos, band[k]);
                }
                for (int y = bottom; y <= top; y++)
                    if (chunk.getBlockState(pos.set(x0 + lx, y, z0 + lz)).isAir())
                        chunk.setBlockState(pos, y <= c.ground() ? rock(c, y)
                                : c.lava() != Terrain.Column.NO_WATER ? LAVA
                                : frozen(RealTectonics.terrain(), c, x0 + lx, z0 + lz) && y == c.water() ? ICE : WATER);
            }
    }

    static final int WATER_ROOF = 16;   // format 98: blocks of rock kept between a cave and the bed of water, lava or ice over it
    static final int WATER_REACH = 12;  // blocks sideways from that bed over which the roof thins a block per block to Caves.ROOF, so no cave runs under a river's bank
    static final int MOUTH_REACH = 8;   // blocks round a column within which its relief is measured
    static final int MOUTH_RELIEF = 8;  // blocks of relief within MOUTH_REACH below which a cave stays roofed: mouths only in hillsides, cliffs and mountains
    /**
     * Format 98: per column of the chunk (lx + lz * 16), the highest y a cave may reach. Under water, lava and ice and
     * up to WATER_REACH blocks beside it the rock over a cave thickens to WATER_ROOF, where a cavern 5 blocks under a
     * riverbed left the river floating over it (a player's report). On gentle land caves keep a roof: vanilla's
     * entrances opened all over our wide flat lowlands, where real caves open in valley walls, cliffs and mountains.
     */
    private static int[] caveCeilings(int x0, int z0, Terrain.Column[] cols) {
        int r = WATER_REACH, w = 16 + 2 * r, m = MOUTH_REACH, rows = 16 + 2 * m;
        Terrain.Column[] near = RealTectonics.terrain().relaxedColumns(x0 - r, z0 - r, w, w);
        // Relief: the highest and lowest dry ground in the square of MOUTH_REACH round each column, rows first, then columns.
        int[] rowLo = new int[rows * 16], rowHi = new int[rows * 16];
        for (int z = 0; z < rows; z++)
            for (int lx = 0; lx < 16; lx++) {
                int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
                for (int dx = -m; dx <= m; dx++) {
                    Terrain.Column n = near[lx + r + dx + (z + r - m) * w];
                    if (n.water() >= n.ground()) continue; // banks of a river or lake are not cliffs
                    lo = Math.min(lo, n.ground());
                    hi = Math.max(hi, n.ground());
                }
                rowLo[lx + z * 16] = lo;
                rowHi[lx + z * 16] = hi;
            }
        // Under water, lava and ice: its bed less WATER_ROOF, rising a block per block away from it (rows, then columns:
        // a diamond, not a circle; ponytail: exact enough for a roof, and as cheap by the sea as inland).
        int[] rowRoof = new int[w * 16];
        for (int z = 0; z < w; z++)
            for (int lx = 0; lx < 16; lx++) {
                int roof = Integer.MAX_VALUE;
                for (int dx = -r; dx <= r; dx++) {
                    Terrain.Column n = near[lx + r + dx + z * w];
                    if (n.water() >= n.ground() || n.lava() != Terrain.Column.NO_WATER || n.ice() != Terrain.Column.NO_WATER)
                        roof = Math.min(roof, n.ground() - WATER_ROOF + Math.abs(dx) * (WATER_ROOF - Caves.ROOF) / r);
                }
                rowRoof[lx + z * 16] = roof;
            }
        int[] out = new int[256];
        for (int lz = 0; lz < 16; lz++)
            for (int lx = 0; lx < 16; lx++) {
                int ground = cols[lx + 1 + (lz + 1) * 18].ground(), ceiling = ground, lo = ground, hi = ground;
                for (int dz = 0; dz <= 2 * m; dz++) {
                    lo = Math.min(lo, rowLo[lx + (lz + dz) * 16]);
                    hi = Math.max(hi, rowHi[lx + (lz + dz) * 16]);
                }
                for (int dz = -r; dz <= r; dz++) {
                    int roof = rowRoof[lx + (lz + r + dz) * 16];
                    if (roof != Integer.MAX_VALUE) ceiling = Math.min(ceiling, roof + Math.abs(dz) * (WATER_ROOF - Caves.ROOF) / r);
                }
                out[lx + lz * 16] = hi - lo < MOUTH_RELIEF ? Math.min(ceiling, ground - Caves.ROOF) : ceiling;
            }
        return out;
    }

    private static final BlockState[] NO_SULFUR = {};
    private static volatile BlockState[] sulfurRock; // looked up once the blocks are registered

    /** Format 99: sulfur and cinnabar, Vanilla Backport's on 1.21.1 or the game's own on 26.x; empty without them. */
    private static BlockState[] sulfurRock() {
        if (sulfurRock == null) {
            var s = BuiltInRegistries.BLOCK.getOptional(Identifier.withDefaultNamespace("sulfur"));
            var c = BuiltInRegistries.BLOCK.getOptional(Identifier.withDefaultNamespace("cinnabar"));
            sulfurRock = s.isPresent() && c.isPresent() ? new BlockState[]{s.get().defaultBlockState(), c.get().defaultBlockState()} : NO_SULFUR;
        }
        return sulfurRock;
    }

    /**
     * Format 99: sulfur caves (TectonicBiomeSource: volcanic arcs and volcanoes, deep enough) crusted where hot gas rose
     * through them: sulfur on the floor 1-3 blocks deep and on the ceiling, cinnabar in patches. The game's own lays the
     * whole rock in beds of both; here they read as stamped stripes cut off at the province's edge, so the rock stays
     * the province's. Its sulfur pools and spikes grow only from sulfur. Runs after the carvers, so their tunnels get it too.
     */
    private static void lineSulfurCaves(ChunkAccess chunk, Terrain.Column[] cols, BlockState[] sulfur) {
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lz = 0; lz < 16; lz++)
            for (int lx = 0; lx < 16; lx++) {
                Terrain.Column c = cols[lx + 1 + (lz + 1) * 18];
                if (c.rock() != Rock.ARC && !c.volcanic()) continue;
                int x = x0 + lx, z = z0 + lz, depth = 1 + (int) (oreHash(x, 0, z) * 3), sinceAir = Integer.MAX_VALUE;
                BlockState above = null;
                for (int y = Math.min(c.ground() - TectonicBiomeSource.UNDER_DEPTH - 1, TectonicBiomeSource.UNDER_TOP); y > chunk.getMinY() + 1; y--) {
                    BlockState b = chunk.getBlockState(pos.set(x, y, z));
                    boolean air = b.isAir();
                    if (air && above != null && !above.isAir() && above.getFluidState().isEmpty() && above != BEDROCK) { // the block above is a cave's ceiling
                        BlockState crust = crust(sulfur, x, y + 1, z);
                        chunk.setBlockState(pos.set(x, y + 1, z), crust);
                        pos.set(x, y, z);
                    }
                    sinceAir = air ? 0 : sinceAir == Integer.MAX_VALUE ? sinceAir : sinceAir + 1;
                    if (!air && sinceAir <= depth && b.getFluidState().isEmpty() && b != BEDROCK) {
                        BlockState crust = crust(sulfur, x, y, z);
                        chunk.setBlockState(pos, crust);
                    }
                    above = b;
                }
            }
    }

    /** Sulfur, cinnabar in patches a few blocks across. ponytail: two sines, not the game's 3D gradient noise. */
    private static BlockState crust(BlockState[] sulfur, int x, int y, int z) {
        return Math.sin(y * 0.3 + 2.5 * Math.sin(x * 0.11) * Math.sin(z * 0.13)) < -0.75 ? sulfur[1] : sulfur[0];
    }

    /** Layered bedrock geology: each layer is one rock type everywhere, shifted up and down by the column's fold. */
    static BlockState rock(Terrain.Column c, int y) {
        if (y < 0) return DEEPSLATE;
        int hash = layerHash(c, y);
        if (c.volcanic() && y > c.ground() - VOLCANIC_DEPTH) return VOLCANIC_LAYERS[hash % VOLCANIC_LAYERS.length];
        if ((c.cover() == Terrain.Cover.IGNIMBRITE || c.cover() == Terrain.Cover.CHIMNEY || c.cover() == Terrain.Cover.CHIMNEY_CAP) && y > c.ground() - VOLCANIC_DEPTH)
            return (hash & 3) == 0 ? CALCITE : TUFF; // pale tuff, whiter beds (format 89; fairy chimneys from 90)
        // Terracotta bands belong to deserts and canyon walls (the badlands look); savanna keeps its own rock.
        boolean arid = (badlands(c.biome()) || c.biome() == Biome.DESERT || c.cover() == Terrain.Cover.CLIFF && c.biome() == Biome.SAVANNA) && y > c.ground() - ARID_DEPTH;
        BlockState[] layers = arid ? ARID_LAYERS : PROVINCE_LAYERS[c.rock().ordinal()];
        return layers[hash % layers.length];
    }

    /** The hash of the rock layer at height y of the column: which rock it is, and from format 92 whether it is an ore bed. */
    static int layerHash(Terrain.Column c, int y) {
        int t = y + c.fold(), layer = RealTectonics.terrain().foldedStrata // from format 34 beds vary in thickness, as real ones do
                ? Math.floorDiv(t, STRATA_PERIOD) * STRATA_BEDS + STRATA_BED[Math.floorMod(t, STRATA_PERIOD)] : Math.floorDiv(t, LAYER_THICKNESS);
        return (layer * 0x9E3779B1) >>> 16;
    }

    /** 0..1 for a block: which of a deposit's blocks are ore, and which vanilla ores stay (format 92). */
    static final int VINE_WALL = 3;          // blocks a cenote's wall must stand over the water to grow vines (format 96)
    static final float VINE_SHARE = 0.7f;    // share of wall columns with a strand
    private static final int[] SIDE = {-1, 1, -18, 18};
    private static final BooleanProperty[] FACE = {VineBlock.WEST, VineBlock.EAST, VineBlock.NORTH, VineBlock.SOUTH};

    /**
     * Format 96: vines hang from a cenote's rim down its walls toward the water, as in the Yucatan's open cenotes (Ik Kil):
     * each strand from the top of its wall, its own length, some walls bare. Placed in the open shaft above the water,
     * clinging to whichever neighbours rise as walls; cols are the chunk's columns with a one-column margin.
     */
    private static void hangVines(ChunkAccess chunk, Terrain.Column[] cols, int i, Terrain.Column c, int x, int z, int lx, int lz) {
        int[] from = new int[4];
        int highest = Integer.MIN_VALUE;
        for (int k = 0; k < 4; k++) {
            Terrain.Column wall = cols[i + SIDE[k]];
            int span = wall.ground() - c.water();
            from[k] = Integer.MAX_VALUE;
            if (wall.water() >= wall.ground() || span < VINE_WALL || oreHash(x, 1000 + k, z) > VINE_SHARE) continue;
            from[k] = wall.ground() - (int) (oreHash(x, 2000 + k, z) * span); // the strand's lowest block
            highest = Math.max(highest, wall.ground());
        }
        for (int y = c.water() + 1; y <= Math.min(highest, chunk.getMaxY()); y++) {
            BlockState vine = null;
            for (int k = 0; k < 4; k++)
                if (y >= from[k] && y <= cols[i + SIDE[k]].ground()) vine = (vine == null ? Blocks.VINE.defaultBlockState() : vine).setValue(FACE[k], true);
            if (vine != null) chunk.setBlockState(new BlockPos(x, y, z), vine);
        }
    }

    static float oreHash(int x, int y, int z) {
        long h = (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L);
        h = (h ^ (h >>> 31)) * 0xBF58476D1CE4E5B9L;
        return ((h >>> 40) & 0xFFFF) / 65536f;
    }

    /**
     * From format 92 vanilla's ores, spread evenly through all rock, are thinned to ORE_KEEP outside the deposits, so
     * the rich ore lies where the geology put it. Only sections whose palette may hold one are looked at. Ores another
     * chunk's decoration later spills into this one stay: a few at chunk edges.
     */
    private static void thinOres(ChunkAccess chunk, Terrain terrain) {
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
        Terrain.Column[] cols = null;
        Deposits.Lode[] lodes = null;
        for (int si = 0; si < chunk.getSectionsCount(); si++) {
            LevelChunkSection section = chunk.getSection(si);
            if (section.hasOnlyAir() || !section.maybeHas(b -> THINNED.contains(b.getBlock()))) continue;
            if (cols == null) {
                cols = terrain.relaxedColumns(x0, z0, 16, 16);
                lodes = new Deposits.Lode[256];
                for (int i = 0; i < 256; i++) lodes[i] = terrain.lode(x0 + (i & 15), z0 + (i >> 4), cols[i]);
            }
            int y0 = chunk.getSectionYFromSectionIndex(si) << 4;
            for (int y = 0; y < 16; y++)
                for (int z = 0; z < 16; z++)
                    for (int x = 0; x < 16; x++) {
                        if (!THINNED.contains(section.getBlockState(x, y, z).getBlock()) || oreHash(x0 + x, y0 + y, z0 + z) < ORE_KEEP) continue;
                        Terrain.Column c = cols[x + z * 16];
                        Deposits.Lode l = lodes[x + z * 16];
                        if (l != null && Deposits.ore(l, y0 + y, c.ground(), layerHash(c, y0 + y), oreHash(x0 + x, y0 + y, z0 + z)) != Deposits.Ore.NONE) continue; // a deposit's own
                        section.setBlockState(x, y, z, rock(c, y0 + y), false);
                    }
        }
    }

    private static Surface surface(Terrain.Column c, int slope, boolean wet) {
        if (c.cover() == Terrain.Cover.SPRING) return SPRING; // soul sand sends up streams of bubbles, like steam
        if (c.cover() == Terrain.Cover.TRAVERTINE) return TRAVERTINE; // white calcite rims and pool floors (format 91)
        if (c.cover() == Terrain.Cover.CHIMNEY) return ROCK; // a fairy chimney's bare tuff (format 90)
        if (c.cover() == Terrain.Cover.CHIMNEY_CAP) return CHIMNEY_CAP; // the harder rock that capped it
        if (c.cover() == Terrain.Cover.SINTER) return (c.ground() & 1) == 0 ? SINTER : SINTER_STAINED;
        if (wet && c.cover() == Terrain.Cover.POND) return (c.fold() & 2) == 0 ? MUD : CLAY; // still water settles fine silt
        if (wet) return c.water() - c.ground() > 8 ? GRAVEL : BEACH;
        // Real cliffs are bare: four blocks up in one step, or the walls of a gorge or canyon. Ordinary steep banks keep their soil.
        if (scree(c, slope)) return ((c.fold() + c.ground()) & 3) == 0 ? SCREE_STONE : GRAVEL; // rubble under a wall, some of it boulders
        if ((slope >= CLIFF_STEP || c.cover() == Terrain.Cover.CLIFF && slope >= 2) && c.biome() != Biome.DESERT) return ROCK;
        if (c.cover() == Terrain.Cover.BANK) return switch (c.biome()) { // riverbanks: mud in the wet tropics, sand where dry, gravel in the cold
            case RAINFOREST, TROPICAL_SEASONAL_FOREST, MANGROVE, SWAMP -> MUD;
            case DESERT, SAVANNA, STEPPE, GRASSLAND -> SAND_BANK;
            case TUNDRA, TAIGA, ICE_CAP, SNOWY_PEAKS, ROCKY_PEAKS -> GRAVEL;
            default -> (c.fold() & 2) == 0 ? SAND_BANK : GRAVEL; // a mix along temperate rivers
        };
        if (c.cover() == Terrain.Cover.SALT) return SALT;
        if (c.cover() == Terrain.Cover.GRAVEL) return GRAVEL;
        boolean marsh = c.biome() == Biome.SWAMP || c.biome() == Biome.MANGROVE;
        if (c.ground() <= Terrain.SEA_Y + 1 && !marsh) return BEACH;
        boolean snowy = switch (c.biome()) {
            case ICE_CAP, SNOWY_PEAKS, TUNDRA, SNOWY_SLOPES, JAGGED_PEAKS, GROVE, SNOWY_BEACH, SNOWY_TAIGA, SNOWY_PLAINS, ICE_SPIKES -> true;
            default -> false;
        };
        if (c.volcanic() && !snowy) return ROCK; // bare lava and ash; snow-capped summits keep their snow
        if (c.cover() == Terrain.Cover.ASH && !snowy) return ASH;
        return switch (c.biome()) {
            case DESERT -> SAND;
            case ICE_CAP, SNOWY_PEAKS, SNOWY_SLOPES, JAGGED_PEAKS, SNOWY_PLAINS, ICE_SPIKES -> SNOW_PACK;
            case TUNDRA, GROVE, SNOWY_TAIGA -> SNOWY_GRASS;
            case OLD_GROWTH_PINE_TAIGA -> PODZOL;
            case BADLANDS, ERODED_BADLANDS -> RED_SAND; // terracotta shows on every slope
            case WOODED_BADLANDS -> COARSE;
            case ROCKY_PEAKS, STONY_SHORE -> ROCK;
            case BEACH, SNOWY_BEACH -> BEACH;
            case TEMPERATE_RAINFOREST -> PODZOL;
            case MANGROVE -> MUD;
            default -> GRASS;
        };
    }

    private static final Surface[] RAINFOREST_BANDS = { // BOP's rocky rainforest: banded clays showing through
            new Surface(Blocks.TERRACOTTA.defaultBlockState(), Blocks.TERRACOTTA.defaultBlockState(), false),
            new Surface(Blocks.DYED_TERRACOTTA.lightGray().defaultBlockState(), Blocks.TERRACOTTA.defaultBlockState(), false),
            new Surface(Blocks.DYED_TERRACOTTA.cyan().defaultBlockState(), Blocks.TERRACOTTA.defaultBlockState(), false),
            new Surface(Blocks.DYED_TERRACOTTA.lightBlue().defaultBlockState(), Blocks.TERRACOTTA.defaultBlockState(), false)};

    /**
     * Biomes O' Plenty's own ground in its biomes, after its surface rules (BOPSurfaceRuleData, 1.21.1): this generator
     * builds every surface itself, so a BOP biome would otherwise grow its plants on our plain grass. Only plain grass and
     * beach sand change; cliffs, scree, banks, salt and snow keep what the terrain says. Patches follow Terrain.patch, a
     * few dozen blocks across, where BOP uses its surface noise; thresholds give BOP's shares (patch: q65 0.09, q80 0.2, q90 0.3).
     */
    private static Surface moddedGround(ChunkAccess chunk, Surface s, Terrain.Column c, int x, int z) {
        if (s != GRASS && s != BEACH) return s;
        var key = chunk.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(c.ground()), QuartPos.fromBlock(z)).unwrapKey();
        if (key.isEmpty() || !key.get().identifier().getNamespace().equals("biomesoplenty")) return s;
        float patch = RealTectonics.terrain().patch(x, z);
        return switch (key.get().identifier().getPath()) {
            case "redwood_forest" -> s == GRASS ? PODZOL : s;                                  // a carpet of needles everywhere
            case "old_growth_woodland" -> s == GRASS && patch > 0.3f ? COARSE : s;             // bare patches under the old trees (~10%)
            case "lush_savanna" -> { // red-orange soil showing through; looked up here, as BOP registers its blocks after this class loads
                BlockState orange = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("biomesoplenty", "orange_sand")).defaultBlockState();
                yield s == GRASS && patch > 0.24f && !orange.isAir() ? new Surface(orange, Blocks.DIRT.defaultBlockState(), false) : s; // ~15%
            }
            case "rocky_rainforest" -> s == GRASS && patch > 0.09f // ~35%, in bands as BOP has them
                    ? RAINFOREST_BANDS[patch > 0.38f ? 3 : patch > 0.3f ? 2 : patch > 0.2f ? 1 : 0] : s;
            case "gravel_beach" -> GRAVEL;
            default -> s;
        };
    }

    private static boolean scree(Terrain.Column c, int slope) {
        return c.cover() == Terrain.Cover.SCREE && slope < CLIFF_STEP;
    }

    /** Above the tree line, where snow lies only where it can. */
    private static boolean alpine(Biome b) {
        return b == Biome.SNOWY_PEAKS || b == Biome.JAGGED_PEAKS || b == Biome.SNOWY_SLOPES || b == Biome.ICE_CAP || b == Biome.SNOWY_PLAINS || b == Biome.ICE_SPIKES;
    }

    /** Snow settles on gentle ground and lasts on shaded north faces; patch (-1..1) makes the edge ragged. */
    static boolean snowStays(int slope, boolean shaded, float patch) {
        return patch > -SNOW_SCOUR && slope <= SNOW_SLOPE + (shaded ? SNOW_SHADE : 0) + SNOW_PATCH * patch;
    }

    private static boolean badlands(Biome b) {
        return b == Biome.BADLANDS || b == Biome.WOODED_BADLANDS || b == Biome.ERODED_BADLANDS;
    }

    /** Water that freezes over at the surface. */
    /**
     * Desert gives way to steppe through patchy ground, not at a line: near the edge of dryness the sand opens up
     * into coarse dirt and tufts of grass, and the steppe beyond it grows sandy bare patches.
     */
    private static Surface dryEdge(Surface s, Terrain.Column c, int x, int z) {
        boolean desert = c.biome() == Biome.DESERT, steppe = c.biome() == Biome.STEPPE || c.biome() == Biome.GRASSLAND;
        if (!(desert && s == SAND || steppe && s == GRASS)) return s;
        Terrain terrain = RealTectonics.terrain();
        float dry = terrain.aridity(x, z, c.ground()), patch = 0.5f + 0.5f * terrain.patch(x, z);
        if (desert) { // 0.5 is the desert's edge: from 0.3 up, more and more of it is scrubby ground
            float open = Math.clamp((dry - DESERT_EDGE) / (0.5f - DESERT_EDGE), 0, 1);
            return patch < open * 0.5f ? GRASS : patch < open ? COARSE : s;
        }
        float bare = Math.clamp((STEPPE_EDGE - dry) / (STEPPE_EDGE - 0.5f), 0, 1); // 0.5 up to STEPPE_EDGE: sandy patches thin out
        return patch < bare * 0.4f ? SAND : patch < bare * 0.7f ? COARSE : s;
    }

    /** Whether this column's water freezes over: by the planet's climate at the surface, or on older worlds by biome. */
    private static boolean frozen(Terrain terrain, Terrain.Column c, int x, int z) {
        return terrain.snowLine ? terrain.snowTemperature(x, z, c.water()) < 0 : frozen(c.biome());
    }

    /** Hot springs and their sinter stay clear of snow. */
    private static boolean warmGround(Surface s) {
        return s == SPRING || s == SINTER || s == SINTER_STAINED || s == MAGMA_PATCH || s == TRAVERTINE;
    }

    /**
     * Vanilla's freeze step snows and ices by each biome's fixed snow line, and features (trees) come after the
     * terrain's own snow. Redo the top of every column by the planet's climate: clear snow where it is above freezing
     * or on bare rock, thaw ice, and snow over tree tops where it is below.
     */
    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structures) {
        super.applyBiomeDecoration(level, chunk, structures);
        Terrain terrain = RealTectonics.terrain();
        if (terrain.deposits) thinOres(chunk, terrain);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(), below = new BlockPos.MutableBlockPos();
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
        // Vanilla's river and ocean biomes lay round gravel disks on the bed; in shallow braided rivers they read as gray
        // blotches at the surface, so the bed's top block goes back to sand (format 47).
        if (terrain.noGravel)
            for (int lz = 0; lz < 16; lz++)
                for (int lx = 0; lx < 16; lx++) {
                    int x = x0 + lx, z = z0 + lz, y = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
                    if (level.getBlockState(pos.set(x, y, z)).is(Blocks.GRAVEL) && !level.getFluidState(below.set(x, y + 1, z)).isEmpty())
                        level.setBlock(pos, Blocks.SAND.defaultBlockState(), 2);
                }
        if (!terrain.snowLine) return;
        for (int lz = 0; lz < 16; lz++)
            for (int lx = 0; lx < 16; lx++) {
                int x = x0 + lx, z = z0 + lz, y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                BlockState b = level.getBlockState(pos.set(x, y, z));
                boolean cold = terrain.snowTemperature(x, z, y) < 0;
                if (b.is(Blocks.SNOW)) {
                    BlockState under = level.getBlockState(below.set(x, y - 1, z));
                    if (!cold || bareRock(under)) {
                        level.setBlock(pos, AIR, 2);
                        if (under.hasProperty(SnowyBlock.SNOWY)) level.setBlock(below, under.setValue(SnowyBlock.SNOWY, false), 2);
                    }
                } else if (b.is(Blocks.ICE)) {
                    if (!cold) level.setBlock(pos, WATER, 2);
                } else if (cold && b.getFluidState().isEmpty() && !bareRock(b) && level.getBlockState(below.set(x, y + 1, z)).isAir()
                        && SNOW.canSurvive(level, below)) {
                    level.setBlock(below, SNOW, 2);
                    if (b.hasProperty(SnowyBlock.SNOWY)) level.setBlock(pos, b.setValue(SnowyBlock.SNOWY, true), 2);
                }
            }
    }

    private static final java.util.Set<net.minecraft.world.level.block.Block> BARE = new java.util.HashSet<>();
    static {
        for (BlockState[] layers : PROVINCE_LAYERS) for (BlockState s : layers) BARE.add(s.getBlock());
        for (BlockState s : ARID_LAYERS) BARE.add(s.getBlock());
        for (BlockState s : VOLCANIC_LAYERS) BARE.add(s.getBlock());
        BARE.add(Blocks.MAGMA_BLOCK);
        BARE.add(Blocks.STONE);
        BARE.add(Blocks.BLUE_ICE); // bare glacier ice below the snow line: only there is blue ice the top block (format 40)
        BARE.add(Blocks.COBBLED_DEEPSLATE); // moraine on that ice
    }

    /** The terrain's own bare rock: cliffs, scoured ridges, salt crusts and lava flows that it left free of snow. */
    private static boolean bareRock(BlockState b) {
        return BARE.contains(b.getBlock());
    }

    private static boolean frozen(Biome b) {
        return b == Biome.FROZEN_OCEAN || b == Biome.DEEP_FROZEN_OCEAN || b == Biome.FROZEN_RIVER;
    }

    /** The surface was built along with the terrain in fillFromNoise. */
    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState random, ChunkAccess chunk) {
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        Terrain.Column c = RealTectonics.terrain().column(x, z);
        boolean floor = type == Heightmap.Types.OCEAN_FLOOR || type == Heightmap.Types.OCEAN_FLOOR_WG;
        return (floor ? c.ground() : Math.max(Math.max(c.ground(), c.water()), c.ice())) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        Terrain.Column c = RealTectonics.terrain().column(x, z);
        BlockState[] states = new BlockState[level.getHeight()];
        for (int i = 0; i < states.length; i++) {
            int y = level.getMinY() + i;
            states[i] = y <= c.ground() ? STONE : y <= c.water() ? WATER : y <= c.ice() ? PACKED_ICE : AIR;
        }
        return new NoiseColumn(level.getMinY(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        Terrain terrain = RealTectonics.terrain();
        Terrain.Column c = terrain.column(pos.getX(), pos.getZ());
        double lat = terrain.latitude(pos.getZ()), lon = terrain.longitude(pos.getX());
        info.add(String.format("Tectonic: %.2f°%s %.2f°%s, %s, ground %d (%.0f m)", Math.abs(lat), lat >= 0 ? "N" : "S",
                Math.abs(lon), lon >= 0 ? "E" : "W", c.biome(), c.ground(), terrain.toMetres(c.ground())));
    }
}
