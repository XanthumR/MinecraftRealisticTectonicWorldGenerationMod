exec(open('tools/edit_glacier.py').read().split("C = ")[0])
H = 'src/main/java/com/realtectonics/sim/Hydrology.java'
edit(H, [
("""import java.util.Random;
""", """import java.util.Random;
import java.util.function.IntPredicate;
"""),
("""    static final float COAST_RAMP = 6;          // cells over which land climbs from the shore to full height
""", """    static final float COAST_RAMP = 6;          // cells over which land climbs from the shore to full height
    // Continental shelves: the drowned rim of a continent, shallow out to the shelf break, then the slope to the deep floor.
    static final float SHELF_MIN = 1, SHELF_MAX = 6; // cells wide on quiet (passive) coasts, by broad noise
    static final float ACTIVE_SHELF = 0.5f;     // cells: next to a trench the floor drops away almost at once (Chile, Japan)
    static final int ACTIVE_REACH = 4;          // cells from an active trench that count as an active margin
    static final float SHELF_DEPTH = 60, SHELF_BREAK = 180; // metres at the shore and at the outer edge
    static final float SLOPE = 2000;            // metres per cell down the continental slope
"""),
("""        int[] shore = distanceToSea();
        for (int g = 0; g < n; g++)
            if (elevation[g] > 0) elevation[g] = Math.max(0.001f, elevation[g] * Math.min(1, shore[g] / COAST_RAMP));""",
 """        int[] shore = distanceFrom(g -> elevation[g] < 0);
        for (int g = 0; g < n; g++)
            if (elevation[g] > 0) elevation[g] = Math.max(0.001f, elevation[g] * Math.min(1, shore[g] / COAST_RAMP));
        int[] offshore = distanceFrom(g -> elevation[g] >= 0), trench = distanceFrom(g -> planet.subduction[g] > Planet.ARC_ACTIVITY);
        float[] breadth = Noise.fbm(w, h, w / 32, 4, new Random(seed * 13 + 5));
        for (int g = 0; g < n; g++) {
            if (elevation[g] >= 0) continue;
            float width = trench[g] <= ACTIVE_REACH ? ACTIVE_SHELF : SHELF_MIN + (SHELF_MAX - SHELF_MIN) * Math.clamp((breadth[g] - 0.5f) / 0.75f, 0, 1);
            float d = offshore[g] - 0.5f; // the shore lies half a cell out from the last land cell
            float m = d <= width ? -SHELF_DEPTH - (SHELF_BREAK - SHELF_DEPTH) * d / width : -SHELF_BREAK - SLOPE * (d - width);
            elevation[g] = Math.max(elevation[g], Planet.fromMetres(m)); // shelves only ever fill the deep floor in
        }"""),
("""    /** Cells (8-connected steps) to the nearest sea cell. */
    private int[] distanceToSea() {
        int[] dist = new int[w * h], queue = new int[w * h];
        Arrays.fill(dist, Integer.MAX_VALUE);
        int n = 0;
        for (int g = 0; g < w * h; g++) if (elevation[g] < 0) { dist[g] = 0; queue[n++] = g; }""",
 """    /** Cells (8-connected steps) to the nearest cell matching source. */
    private int[] distanceFrom(IntPredicate source) {
        int[] dist = new int[w * h], queue = new int[w * h];
        Arrays.fill(dist, Integer.MAX_VALUE);
        int n = 0;
        for (int g = 0; g < w * h; g++) if (source.test(g)) { dist[g] = 0; queue[n++] = g; }"""),
])
D = 'src/main/java/com/realtectonics/sim/PlanetData.java'
edit(D, [
("""    private static final int FORMAT = 5; // 2 added volcanoes, 3 rock provinces, 4 volcano bases, 5 glaciers; older files load without them""",
 """    private static final int FORMAT = 6; // 2 volcanoes, 3 rock provinces, 4 volcano bases, 5 glaciers, 6 shelves; older files load without them"""),
("""    public final byte[] ice;            // Hydrology.GLACIATED or FJORD, else 0
""", """    public final byte[] ice;            // Hydrology.GLACIATED or FJORD, else 0
    public int format = FORMAT;         // of the file this planet came from: older planets keep their old block heights
"""),
("""            PlanetData d = new PlanetData(in.readInt(), in.readInt());""",
 """            PlanetData d = new PlanetData(in.readInt(), in.readInt());
            d.format = format;"""),
])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""    static final float SEA_METRES_PER_BLOCK = 40;    // oceans squeezed: 4 km deep is 100 blocks""",
 """    static final float SEA_METRES_PER_BLOCK = 40;    // oceans squeezed: 4 km deep is 100 blocks
    static final float SHALLOW_METRES = 200, SHALLOW_METRES_PER_BLOCK = 20; // shelf seas get room: their 200 m take 10 blocks"""),
("""    private final float landMetresPerBlock;""",
 """    private final float landMetresPerBlock;
    private final float shallowBlocks;   // blocks the shallow sea takes; 0 on planets saved before shelves
    private final float offshoreSlope;   // how fast the sea floor drops off the smooth coastline"""),
("""        landMetresPerBlock = LAND_METRES_PER_BLOCK / mountainScale;""",
 """        landMetresPerBlock = LAND_METRES_PER_BLOCK / mountainScale;
        boolean shelves = data.format >= 6;
        shallowBlocks = shelves ? SHALLOW_METRES / SHALLOW_METRES_PER_BLOCK : 0;
        offshoreSlope = shelves ? 16 : 60;"""),
("""        return SEA_Y + metres / (metres >= 0 ? landMetresPerBlock : SEA_METRES_PER_BLOCK);
    }""",
 """        if (metres >= 0) return SEA_Y + metres / landMetresPerBlock;
        float shallow = shallowBlocks * SHALLOW_METRES_PER_BLOCK; // 0 on old planets: one scale all the way down
        return metres > -shallow ? SEA_Y + metres / SHALLOW_METRES_PER_BLOCK : SEA_Y - shallowBlocks + (metres + shallow) / SEA_METRES_PER_BLOCK;
    }"""),
("""        float b = y - SEA_Y;
        return b * (b >= 0 ? landMetresPerBlock : SEA_METRES_PER_BLOCK);""",
 """        float b = y - SEA_Y;
        if (b >= 0) return b * landMetresPerBlock;
        return b > -shallowBlocks ? b * SHALLOW_METRES_PER_BLOCK : -shallowBlocks * SHALLOW_METRES_PER_BLOCK + (b + shallowBlocks) * SEA_METRES_PER_BLOCK;"""),
("""        float shore = cubic(land, u, v) - 0.5f, beach = SEA_Y - 1.5f + shore * 60;""",
 """        float shore = cubic(land, u, v) - 0.5f, beach = SEA_Y - 1.5f + shore * (shore > 0 ? 60 : offshoreSlope);"""),
])
print("ok")
