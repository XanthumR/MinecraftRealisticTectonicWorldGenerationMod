exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
# lakes: no cliffs cut into hills
("""            if (lakeness > 0.5f || simCalderas && lakeness > SHORE_FULL && ground < (int) ly) {
                ground = Math.min(ground, (int) ly - 1 - Math.max(0, Math.round((lakeness - 0.5f) * 8)));
                water = Math.max(water, (int) ly);
            } else {""",
 """            // From format 25 a lake floods only the ground below its surface (and shaves a block or two off what
            // barely rises above it); hills reaching into it stay as islands and headlands, not cut into cliffs.
            boolean flooded = gentleShores ? lakeness > SHORE_FULL && ground < ly + LAKE_SHAVE
                    : lakeness > 0.5f || simCalderas && lakeness > SHORE_FULL && ground < (int) ly;
            if (flooded) {
                ground = Math.min(ground, (int) ly - 1 - Math.max(0, Math.round((lakeness - 0.5f) * 8)));
                water = Math.max(water, (int) ly);
            } else if (!gentleShores || lakeness <= SHORE_FULL) {"""),
# springs from the finished ground
("""                        int level = Math.round(landAt(px, pz)); // the pool's surface: the real ground at its middle, not the ideal cone
                        if (level <= SEA_Y + 2 || nearCrater(px, pz)) continue; // not on the sea floor, nor in a crater""",
 """                        int level = gentleShores ? springLevel(key, px, pz, size) // the finished ground, level all round
                                : Math.round(landAt(px, pz)); // the pool's surface: the real ground at its middle, not the ideal cone
                        if (level == Integer.MIN_VALUE || level <= SEA_Y + 2 || nearCrater(px, pz)) continue; // not on the sea floor, nor in a crater"""),
("""            if (landforms && (c.v.active() || c.v.hotspot() && c.v.age() < SPRING_AGE) && ground > SEA_Y + 2) {""",
 """            if (ponds && landforms && (c.v.active() || c.v.hotspot() && c.v.age() < SPRING_AGE) && ground > SEA_Y + 2) {"""),
("""    private final ThreadLocal<long[]> lastPond""",
 """    private final ThreadLocal<long[]> lastSpring = ThreadLocal.withInitial(() -> new long[]{Long.MIN_VALUE, 0}); // spring key, its level

    /**
     * A hot spring's pool level: the finished ground at its middle, or MIN_VALUE where the ground around it is not
     * level enough to hold a pool behind a low sinter rim. Like pondLevel, the last answer is kept per thread.
     */
    private int springLevel(int key, double px, double pz, double size) {
        long[] last = lastSpring.get();
        if (last[0] == key) return (int) last[1];
        Column mid = column(px, pz, false);
        int level = mid.ground();
        if (mid.water() != Column.NO_WATER || mid.lava() != Column.NO_WATER || mid.ice() != Column.NO_WATER) level = Integer.MIN_VALUE;
        for (int k = 0; k < 8 && level != Integer.MIN_VALUE; k++) {
            double a = k * Math.PI / 4;
            int rim = column(px + (size + 1) * Math.cos(a), pz + (size + 1) * Math.sin(a), false).ground();
            if (rim < level - 1 || rim > level + SPRING_TILT) level = Integer.MIN_VALUE; // the sinter rim would stand as a tower
        }
        last[0] = key;
        last[1] = level;
        return level;
    }

    /** Rain over what the land needs to stay out of dryness (Köppen), at real block (x, y, z): below 0.5 desert, below 1 steppe. */
    public float aridity(int realX, int realZ, float y) {
        double x = realX / (double) zoom, z = realZ / (double) zoom, u = warpU(x, z), v = warpV(x, z);
        float t = localTemperature(x, z, u, v, y), rain = bilinear(data.precipitation, u, v) * (1 + 0.12f * noise(x, z, 256, 24) + 0.06f * noise(x, z, 64, 25));
        float s = bilinear(data.summerRain, u, v), need = 20 * t + (s >= 0.7f ? 280 : s >= 0.3f ? 140 : 0);
        return need <= 0 ? 10 : rain / need;
    }

    private final ThreadLocal<long[]> lastPond"""),
("""    private final boolean seasons;""", """    private final boolean seasons;
    public final boolean gentleShores;    // lakes flood hollows instead of cutting cliffs, springs on level ground, desert edges mixed: format 25"""),
("""        seasons = data.format >= 24;""", """        seasons = data.format >= 24;
        gentleShores = data.format >= 25;"""),
("""    static final float ICE_DDF = 2.2f;""", """    static final float LAKE_SHAVE = 2;    // blocks above a lake's surface it still floods, shaving the ground down
    static final int SPRING_TILT = 3;     // blocks the ground round a hot spring may rise above its pool
    static final float ICE_DDF = 2.2f;"""),
])
edit('src/main/java/com/realtectonics/sim/PlanetData.java', [
("""    private static final int FORMAT = 24;""", """    private static final int FORMAT = 25;"""),
("""24 seasons (amplitude, summerRain); older""", """24 seasons (amplitude, summerRain), 25 gentle lake shores; older"""),
])
G = 'src/main/java/com/realtectonics/mod/TectonicChunkGenerator.java'
edit(G, [
("""                Surface s = glacier ? GRAVEL : surface(c, slope, wet);""",
 """                Surface s = glacier ? GRAVEL : surface(c, slope, wet);
                if (!glacier && !wet && RealTectonics.terrain().gentleShores) s = dryEdge(s, c, x0 + lx, z0 + lz);"""),
("""    /** Whether this column's water freezes over""",
 """    /**
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

    /** Whether this column's water freezes over"""),
("""    static final int LAYER_THICKNESS = 6;
""", """    static final int LAYER_THICKNESS = 6;
    static final float DESERT_EDGE = 0.3f, STEPPE_EDGE = 0.8f; // aridity over which desert opens into scrub, and up to which steppe has sandy patches
"""),
])
print("ok")
