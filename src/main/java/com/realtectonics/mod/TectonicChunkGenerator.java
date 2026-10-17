package com.realtectonics.mod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.realtectonics.sim.Biome;
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
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Builds terrain columns straight from the simulated planet. Extends the noise generator only to keep
 * vanilla carvers (caves, ravines); its settings (data/realtectonics/worldgen/noise_settings/planet.json)
 * have sea_level -64 so those caves stay dry, and applyCarvers seals caves away from surface water.
 * The real sea level comes from getSeaLevel().
 */
public final class TectonicChunkGenerator extends NoiseBasedChunkGenerator {
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
            Codec.BOOL.optionalFieldOf("earth", false).forGetter(PlanetSettings::earth)
    ).apply(i, PlanetSettings::new));

    public static final MapCodec<TectonicChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(NoiseBasedChunkGenerator::generatorSettings),
            PLANET_CODEC.optionalFieldOf("planet", PlanetSettings.DEFAULT).forGetter(TectonicChunkGenerator::planet)
    ).apply(i, TectonicChunkGenerator::new));

    private static final BlockState AIR = Blocks.AIR.defaultBlockState(), STONE = Blocks.STONE.defaultBlockState(),
            DEEPSLATE = Blocks.DEEPSLATE.defaultBlockState(), BEDROCK = Blocks.BEDROCK.defaultBlockState(),
            WATER = Blocks.WATER.defaultBlockState(), ICE = Blocks.ICE.defaultBlockState(), SNOW = Blocks.SNOW.defaultBlockState(),
            LAVA = Blocks.LAVA.defaultBlockState(), MAGMA = Blocks.MAGMA_BLOCK.defaultBlockState(),
            PACKED_ICE = Blocks.PACKED_ICE.defaultBlockState(), BLUE_ICE = Blocks.BLUE_ICE.defaultBlockState(),
            MORAINE = Blocks.COBBLED_DEEPSLATE.defaultBlockState(); // rock debris riding on glacier ice (format 40)
    static final int BLUE_ICE_DEPTH = 6; // glacier ice this far down is squeezed clear of air bubbles

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
            Blocks.TERRACOTTA.defaultBlockState(), Blocks.ORANGE_TERRACOTTA.defaultBlockState(), Blocks.YELLOW_TERRACOTTA.defaultBlockState(),
            Blocks.TERRACOTTA.defaultBlockState(), Blocks.RED_TERRACOTTA.defaultBlockState(), Blocks.WHITE_TERRACOTTA.defaultBlockState(),
            Blocks.BROWN_TERRACOTTA.defaultBlockState(), Blocks.LIGHT_GRAY_TERRACOTTA.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState()};
    static final int ARID_DEPTH = 48;  // terracotta bands only near the surface, so ores still have stone deeper down
    static final int CLIFF_STEP = 4;   // blocks up in one step that make bare rock
    static final float SNOW_SLOPE = 0.8f, SNOW_SHADE = 1.2f, SNOW_PATCH = 1.0f, SNOW_SCOUR = 0.5f; // see snowStays
    private static final BlockState[] VOLCANIC_LAYERS = {
            Blocks.BASALT.defaultBlockState(), Blocks.TUFF.defaultBlockState(), Blocks.ANDESITE.defaultBlockState(), Blocks.BASALT.defaultBlockState(),
            Blocks.BLACKSTONE.defaultBlockState(), Blocks.TUFF.defaultBlockState(), Blocks.SMOOTH_BASALT.defaultBlockState()};
    static final int VOLCANIC_DEPTH = 40; // lava flows and ash layered over the bedrock below

    private static final Surface GRASS = new Surface(Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), false),
            SNOWY_GRASS = new Surface(Blocks.GRASS_BLOCK.defaultBlockState().setValue(SnowyDirtBlock.SNOWY, true), Blocks.DIRT.defaultBlockState(), true),
            PODZOL = new Surface(Blocks.PODZOL.defaultBlockState(), Blocks.DIRT.defaultBlockState(), false),
            SAND = new Surface(Blocks.SAND.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false),
            BEACH = new Surface(Blocks.SAND.defaultBlockState(), Blocks.SAND.defaultBlockState(), false),
            GRAVEL = new Surface(Blocks.GRAVEL.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), false),
            SPRING = new Surface(Blocks.SOUL_SAND.defaultBlockState(), Blocks.CALCITE.defaultBlockState(), false),
            SINTER = new Surface(Blocks.WHITE_TERRACOTTA.defaultBlockState(), Blocks.CALCITE.defaultBlockState(), false),
            SINTER_STAINED = new Surface(Blocks.ORANGE_TERRACOTTA.defaultBlockState(), Blocks.CALCITE.defaultBlockState(), false), // bacterial mats
            RED_SAND = new Surface(Blocks.RED_SAND.defaultBlockState(), Blocks.RED_SANDSTONE.defaultBlockState(), false),
            COARSE = new Surface(Blocks.COARSE_DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), false),
            SAND_BANK = new Surface(Blocks.SAND.defaultBlockState(), Blocks.SAND.defaultBlockState(), false),
            SALT = new Surface(Blocks.CALCITE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false), // a white salt crust
            MUD = new Surface(Blocks.MUD.defaultBlockState(), Blocks.MUD.defaultBlockState(), false),
            CLAY = new Surface(Blocks.CLAY.defaultBlockState(), Blocks.DIRT.defaultBlockState(), false),
            SNOW_PACK = new Surface(Blocks.SNOW_BLOCK.defaultBlockState(), Blocks.PACKED_ICE.defaultBlockState(), false),
            ROCK = new Surface(null, null, false),
            SCREE_STONE = new Surface(Blocks.COBBLESTONE.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), false);

    private final PlanetSettings planet;

    public TectonicChunkGenerator(BiomeSource biomes, Holder<NoiseGeneratorSettings> settings, PlanetSettings planet) {
        super(biomes, settings);
        this.planet = planet;
    }

    public PlanetSettings planet() {
        return planet;
    }

    public TectonicChunkGenerator withPlanet(PlanetSettings planet) {
        return new TectonicChunkGenerator(getBiomeSource(), generatorSettings(), planet);
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

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random, StructureManager structures, ChunkAccess chunk) {
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ(), minY = chunk.getMinBuildHeight();
        Terrain.Column[] cols = columns(chunk, structures);

        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
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
                if (!glacier && !wet && RealTectonics.terrain().gentleShores) s = dryEdge(s, c, x0 + lx, z0 + lz);
                if (!glacier && !wet) s = moddedGround(chunk, s, c, x0 + lx, z0 + lz);
                if (!glacier && !wet && !scree(c, slope) && alpine(c.biome()) && !snowStays(slope, shaded, RealTectonics.terrain().patch(x0 + lx, z0 + lz)))
                    s = ROCK; // steep faces, ridges and wind-scoured patches stand bare above the snow
                Terrain terrain = RealTectonics.terrain();
                boolean frozen = frozen(terrain, c, x0 + lx, z0 + lz);
                // Snow lies wherever the planet's climate is below freezing, except on bare rock and warm ground.
                boolean snowLayer = terrain.snowLine
                        ? !wet && !hot && !glacier && s.top != null && !warmGround(s) && terrain.snowTemperature(x0 + lx, z0 + lz, c.ground()) < 0
                        : s.snowLayer;
                if (terrain.snowLine && s.top != null && s.top.hasProperty(SnowyDirtBlock.SNOWY))
                    s = new Surface(s.top.setValue(SnowyDirtBlock.SNOWY, snowLayer), s.filler, snowLayer);
                // Snow, ice or a structure's foundation can lift the top past the build limit: never write above it.
                int top = Math.min(wet ? c.water() : hot ? c.lava() : glacier ? c.ice() : c.ground() + (snowLayer ? 1 : 0), chunk.getMaxBuildHeight() - 1);

                LevelChunkSection section = null;
                int sectionIndex = -1;
                for (int y = minY; y <= top; y++) {
                    BlockState state = y == minY ? BEDROCK
                            : y < c.ground() - 4 ? rock(c, y)
                            : y < c.ground() ? (s.filler == null ? rock(c, y) : s.filler)
                            : y == c.ground() ? (hot ? MAGMA : s.top == null ? rock(c, y) : s.top)
                            : wet ? (frozen && y == c.water() ? ICE : WATER)
                            : hot ? LAVA
                            : glacier ? (y == c.ice() && c.cover() == Terrain.Cover.MORAINE ? MORAINE
                                    : c.ice() - y >= BLUE_ICE_DEPTH || y == c.ice() && c.cover() == Terrain.Cover.BARE_ICE ? BLUE_ICE : PACKED_ICE)
                            : SNOW;
                    int index = chunk.getSectionIndex(y);
                    if (index != sectionIndex) {
                        section = chunk.getSection(index);
                        sectionIndex = index;
                    }
                    section.setBlockState(lx, y & 15, lz, state, false);
                }
                // Where a river steps down a block, let the edge water flow when the chunk loads: rapids, not a frozen wall.
                if (wet && lowestNeighbourTop(cols, i) < c.water()) chunk.markPosForPostprocessing(new BlockPos(x0 + lx, c.water(), z0 + lz));
                BlockState surfaceBlock = s.top == null ? rock(c, c.ground()) : s.top;
                oceanFloor.update(lx, glacier ? c.ice() : c.ground(), lz, glacier ? PACKED_ICE : surfaceBlock);
                worldSurface.update(lx, top, lz, top == c.ground() ? surfaceBlock : wet ? WATER : hot ? LAVA : glacier ? PACKED_ICE : SNOW);
            }
        return CompletableFuture.completedFuture(chunk);
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
    public void applyCarvers(WorldGenRegion region, long seed, RandomState random, BiomeManager biomes, StructureManager structures,
                             ChunkAccess chunk, GenerationStep.Carving step) {
        super.applyCarvers(region, seed, random, biomes, structures, chunk, step);
        sealCavesFromWater(chunk, structures);
    }

    /**
     * Carvers run with no sea at all, and vanilla lets them carve water (overworld_carver_replaceables), so they
     * leave air tunnels through seas, lakes and rivers. Undo that: water goes back above the bed, rock below it
     * and beside it. Crater lava lakes get the same treatment.
     */
    private static void sealCavesFromWater(ChunkAccess chunk, StructureManager structures) {
        Terrain.Column[] cols = columns(chunk, structures);
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lz = 0; lz < 16; lz++)
            for (int lx = 0; lx < 16; lx++) {
                int i = lx + 1 + (lz + 1) * 18;
                Terrain.Column c = cols[i];
                int nearFluid = Math.max(Math.max(fluid(c), Math.max(fluid(cols[i - 1]), fluid(cols[i + 1]))),
                        Math.max(fluid(cols[i - 18]), fluid(cols[i + 18])));
                int top = Math.min(Math.max(fluid(c), Math.min(nearFluid, c.ground())), chunk.getMaxBuildHeight() - 1);
                // From format 43 only where air can touch the fluid: water and lava lie above the ground, so from the
                // lowest ground here or beside up. Caves deeper down stay, closed off and dry, instead of being filled.
                int bottom = RealTectonics.terrain().fastMath ? Math.max(chunk.getMinBuildHeight() + 1, Math.min(Math.min(c.ground(),
                        Math.min(cols[i - 1].ground(), cols[i + 1].ground())), Math.min(cols[i - 18].ground(), cols[i + 18].ground()))) : chunk.getMinBuildHeight() + 1;
                for (int y = bottom; y <= top; y++)
                    if (chunk.getBlockState(pos.set(x0 + lx, y, z0 + lz)).isAir())
                        chunk.setBlockState(pos, y <= c.ground() ? rock(c, y)
                                : c.lava() != Terrain.Column.NO_WATER ? LAVA
                                : frozen(RealTectonics.terrain(), c, x0 + lx, z0 + lz) && y == c.water() ? ICE : WATER, false);
            }
    }

    /** Layered bedrock geology: each layer is one rock type everywhere, shifted up and down by the column's fold. */
    static BlockState rock(Terrain.Column c, int y) {
        if (y < 0) return DEEPSLATE;
        int t = y + c.fold(), layer = RealTectonics.terrain().foldedStrata // from format 34 beds vary in thickness, as real ones do
                ? Math.floorDiv(t, STRATA_PERIOD) * STRATA_BEDS + STRATA_BED[Math.floorMod(t, STRATA_PERIOD)] : Math.floorDiv(t, LAYER_THICKNESS);
        int hash = (layer * 0x9E3779B1) >>> 16;
        if (c.volcanic() && y > c.ground() - VOLCANIC_DEPTH) return VOLCANIC_LAYERS[hash % VOLCANIC_LAYERS.length];
        // Terracotta bands belong to deserts and canyon walls (the badlands look); savanna keeps its own rock.
        boolean arid = (badlands(c.biome()) || c.biome() == Biome.DESERT || c.cover() == Terrain.Cover.CLIFF && c.biome() == Biome.SAVANNA) && y > c.ground() - ARID_DEPTH;
        BlockState[] layers = arid ? ARID_LAYERS : PROVINCE_LAYERS[c.rock().ordinal()];
        return layers[hash % layers.length];
    }

    private static Surface surface(Terrain.Column c, int slope, boolean wet) {
        if (c.cover() == Terrain.Cover.SPRING) return SPRING; // soul sand sends up streams of bubbles, like steam
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
            new Surface(Blocks.LIGHT_GRAY_TERRACOTTA.defaultBlockState(), Blocks.TERRACOTTA.defaultBlockState(), false),
            new Surface(Blocks.CYAN_TERRACOTTA.defaultBlockState(), Blocks.TERRACOTTA.defaultBlockState(), false),
            new Surface(Blocks.LIGHT_BLUE_TERRACOTTA.defaultBlockState(), Blocks.TERRACOTTA.defaultBlockState(), false)};

    /**
     * Biomes O' Plenty's own ground in its biomes, after its surface rules (BOPSurfaceRuleData, 1.21.1): this generator
     * builds every surface itself, so a BOP biome would otherwise grow its plants on our plain grass. Only plain grass and
     * beach sand change; cliffs, scree, banks, salt and snow keep what the terrain says. Patches follow Terrain.patch, a
     * few dozen blocks across, where BOP uses its surface noise; thresholds give BOP's shares (patch: q65 0.09, q80 0.2, q90 0.3).
     */
    private static Surface moddedGround(ChunkAccess chunk, Surface s, Terrain.Column c, int x, int z) {
        if (s != GRASS && s != BEACH) return s;
        var key = chunk.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(c.ground()), QuartPos.fromBlock(z)).unwrapKey();
        if (key.isEmpty() || !key.get().location().getNamespace().equals("biomesoplenty")) return s;
        float patch = RealTectonics.terrain().patch(x, z);
        return switch (key.get().location().getPath()) {
            case "redwood_forest" -> s == GRASS ? PODZOL : s;                                  // a carpet of needles everywhere
            case "old_growth_woodland" -> s == GRASS && patch > 0.3f ? COARSE : s;             // bare patches under the old trees (~10%)
            case "lush_savanna" -> { // red-orange soil showing through; looked up here, as BOP registers its blocks after this class loads
                BlockState orange = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("biomesoplenty", "orange_sand")).defaultBlockState();
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
        return s == SPRING || s == SINTER || s == SINTER_STAINED;
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
        if (!terrain.snowLine) return;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(), below = new BlockPos.MutableBlockPos();
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
        for (int lz = 0; lz < 16; lz++)
            for (int lx = 0; lx < 16; lx++) {
                int x = x0 + lx, z = z0 + lz, y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                BlockState b = level.getBlockState(pos.set(x, y, z));
                boolean cold = terrain.snowTemperature(x, z, y) < 0;
                if (b.is(Blocks.SNOW)) {
                    BlockState under = level.getBlockState(below.set(x, y - 1, z));
                    if (!cold || bareRock(under)) {
                        level.setBlock(pos, AIR, 2);
                        if (under.hasProperty(SnowyDirtBlock.SNOWY)) level.setBlock(below, under.setValue(SnowyDirtBlock.SNOWY, false), 2);
                    }
                } else if (b.is(Blocks.ICE)) {
                    if (!cold) level.setBlock(pos, WATER, 2);
                } else if (cold && b.getFluidState().isEmpty() && !bareRock(b) && level.getBlockState(below.set(x, y + 1, z)).isAir()
                        && SNOW.canSurvive(level, below)) {
                    level.setBlock(below, SNOW, 2);
                    if (b.hasProperty(SnowyDirtBlock.SNOWY)) level.setBlock(pos, b.setValue(SnowyDirtBlock.SNOWY, true), 2);
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
            int y = level.getMinBuildHeight() + i;
            states[i] = y <= c.ground() ? STONE : y <= c.water() ? WATER : y <= c.ice() ? PACKED_ICE : AIR;
        }
        return new NoiseColumn(level.getMinBuildHeight(), states);
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
