exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""    private Biome biome(double x, double z, double u, double v, float y) {
        float t = temperature(u, v, y), rain = bilinear(data.precipitation, u, v);
        if (organic) {
            t += 1.5f * noise(x, z, 256, 22) + 0.75f * noise(x, z, 64, 23);
            rain *= 1 + 0.12f * noise(x, z, 256, 24) + 0.06f * noise(x, z, 64, 25);
        }""",
 """    private Biome biome(double x, double z, double u, double v, float y) {
        float t = localTemperature(x, z, u, v, y), rain = bilinear(data.precipitation, u, v);
        if (organic) rain *= 1 + 0.12f * noise(x, z, 256, 24) + 0.06f * noise(x, z, 64, 25);"""),
("""    /** Annual mean °C at block height y. */""",
 """    /** Annual mean °C at block height y, with the local wobble biomes see. */
    private float localTemperature(double x, double z, double u, double v, float y) {
        return temperature(u, v, y) + (organic ? 1.5f * noise(x, z, 256, 22) + 0.75f * noise(x, z, 64, 23) : 0);
    }

    /** Annual mean °C at real block (x, y, z), as the biomes see it: below 0 snow lies and water freezes. */
    public float snowTemperature(int realX, int realZ, float y) {
        double x = realX / (double) zoom, z = realZ / (double) zoom;
        return localTemperature(x, z, warpU(x, z), warpV(x, z), y);
    }

    /** Annual mean °C at block height y. */"""),
("""    private final boolean folds;""", """    private final boolean folds;
    public final boolean snowLine;        // snow and ice follow the planet's climate, not each biome's fixed snow line: format 18"""),
("""        folds = data.format >= 17;""", """        folds = data.format >= 17;
        snowLine = data.format >= 18;"""),
])
edit('src/main/java/com/realtectonics/sim/PlanetData.java', [
("""    private static final int FORMAT = 17;""", """    private static final int FORMAT = 18;"""),
("""17 foothills and folds (no new data)""", """17 foothills and folds, 18 climate snow line (no new data)"""),
])
G = 'src/main/java/com/realtectonics/mod/TectonicChunkGenerator.java'
edit(G, [
("""                boolean frozen = frozen(c.biome());
                int top = wet ? c.water() : hot ? c.lava() : glacier ? c.ice() : c.ground() + (s.snowLayer ? 1 : 0);""",
 """                Terrain terrain = RealTectonics.terrain();
                boolean frozen = frozen(terrain, c, x0 + lx, z0 + lz);
                // Snow lies wherever the planet's climate is below freezing, except on bare rock and warm ground.
                boolean snowLayer = terrain.snowLine
                        ? !wet && !hot && !glacier && s.top != null && !warmGround(s) && terrain.snowTemperature(x0 + lx, z0 + lz, c.ground()) < 0
                        : s.snowLayer;
                if (terrain.snowLine && s.top != null && s.top.hasProperty(SnowyDirtBlock.SNOWY))
                    s = new Surface(s.top.setValue(SnowyDirtBlock.SNOWY, snowLayer), s.filler, snowLayer);
                int top = wet ? c.water() : hot ? c.lava() : glacier ? c.ice() : c.ground() + (snowLayer ? 1 : 0);"""),
("""                                : frozen(c.biome()) && y == c.water() ? ICE : WATER, false);""",
 """                                : frozen(RealTectonics.terrain(), c, x0 + lx, z0 + lz) && y == c.water() ? ICE : WATER, false);"""),
("""    private static boolean frozen(Biome b) {""",
 """    /** Whether this column's water freezes over: by the planet's climate at the surface, or on older worlds by biome. */
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
    }

    /** The terrain's own bare rock: cliffs, scoured ridges, salt crusts and lava flows that it left free of snow. */
    private static boolean bareRock(BlockState b) {
        return BARE.contains(b.getBlock());
    }

    private static boolean frozen(Biome b) {"""),
("""import net.minecraft.world.level.StructureManager;""", """import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;"""),
])
print("ok")
