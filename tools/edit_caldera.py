exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""    private record Cone(Volcano v, double x, double z, float radius, float craterRadius, float craterDepth, float height, boolean legacy) {""",
 """    private record Cone(Volcano v, double x, double z, float radius, float craterRadius, float craterDepth, float height, boolean legacy,
                        boolean caldera, float[][] cinders) {"""),
("""            Cone cone = new Cone(placed, wrapBlocks((v.u() - originX) * BLOCKS_PER_CELL), (v.v() - originY) * BLOCKS_PER_CELL, radius,
                    radius * (v.hotspot() ? 0.08f : 0.06f) + 5, Math.min(25, 0.1f * height), height, legacy);""",
 """            double cx = wrapBlocks((v.u() - originX) * BLOCKS_PER_CELL), cz = (v.v() - originY) * BLOCKS_PER_CELL;
            int key = Math.round(v.u() * 7919) ^ Math.round(v.v() * 104729);
            // Calderas: an old volcano's summit, emptied by its last great eruption, falls in: a wide flat-floored basin
            // under steep walls, filled by a lake (Crater Lake). Extinct arc volcanoes and middle-aged shields.
            boolean caldera = calderas && height >= CALDERA_MIN && !v.active()
                    && hash01(key) < (v.hotspot() ? (v.age() < CALDERA_AGE ? CALDERA_SHIELDS : 0) : CALDERA_SHARE);
            float craterRadius = caldera ? CALDERA_RADIUS * radius : radius * (v.hotspot() ? 0.08f : 0.06f) + 5;
            float craterDepth = caldera ? Math.min(CALDERA_DEPTH, 0.3f * height) : Math.min(25, 0.1f * height);
            // Cinder cones: young volcanoes scatter small steep cones of their own over their flanks.
            boolean young = v.hotspot() ? v.age() < CINDER_AGE : v.active();
            float[][] cinders = new float[calderas && young ? 2 + (int) (4 * hash01(key + 1)) : 0][];
            for (int i = 0; i < cinders.length; i++) {
                double a = 2 * Math.PI * hash01(key + 10 + i), d = radius * (0.3 + 0.5 * hash01(key + 20 + i));
                float r = CINDER_RADIUS * (0.6f + 0.8f * hash01(key + 30 + i));
                cinders[i] = new float[]{(float) (cx + d * Math.cos(a)), (float) (cz + d * Math.sin(a)), r, CINDER_STEEP * r};
            }
            Cone cone = new Cone(placed, cx, cz, radius, craterRadius, craterDepth, height, legacy, caldera, cinders);"""),
("""            float rim = coneY(c, c.craterRadius), pool = rim - c.craterDepth * 0.6f, wobble = rough * (1 + 0.02f * c.height);
            if (d < c.craterRadius) {
                ground = Math.round(rim - c.craterDepth * (1 - (float) (d * d / (c.craterRadius * c.craterRadius))) + wobble);
            } else if (d < c.reach()) {""",
 """            float rim = coneY(c, c.craterRadius), pool = rim - c.craterDepth * (c.caldera ? CALDERA_LAKE : 0.6f), wobble = rough * (1 + 0.02f * c.height);
            if (d < c.craterRadius && c.caldera) {
                // A flat floor under walls that steepen up to the rim; maybe a young cone rising from the lake as an island.
                float wall = smoothstep((float) (d / c.craterRadius - 0.7f) / 0.3f), floor = rim - c.craterDepth;
                ground = Math.round(floor + c.craterDepth * wall * wall + wobble * (0.3f + 0.7f * wall));
                if (hash01(Math.round(c.v.u() * 7919) ^ Math.round(c.v.v() * 104729) + 5) < CALDERA_ISLANDS) {
                    double ia = 2 * Math.PI * hash01(Math.round(c.v.u() * 31) + 7), ix = c.x + 0.4 * c.craterRadius * Math.cos(ia), iz = c.z + 0.4 * c.craterRadius * Math.sin(ia);
                    double e = Math.hypot(wrapBlocks(x - ix), z - iz), ir = 0.3 * c.craterRadius;
                    if (e < ir) ground = Math.max(ground, Math.round(floor + c.craterDepth * 0.8f * (float) (1 - e / ir)));
                }
            } else if (d < c.craterRadius) {
                ground = Math.round(rim - c.craterDepth * (1 - (float) (d * d / (c.craterRadius * c.craterRadius))) + wobble);
            } else if (d < c.reach()) {"""),
("""            // Fresh lava and ash near the top; plants have long since covered the lower flanks and extinct cones.""",
 """            for (float[] k : c.cinders) { // each a steep cone of ash with a crater, standing on the flank; rivers keep their course
                double e = Math.hypot(wrapBlocks(x - k[0]), z - k[1]), cr = 0.3 * k[2];
                if (e >= k[2] || river.edge < 4 || water != Column.NO_WATER) continue;
                float base = landAt(k[0], k[1]), top = e < cr ? base + k[3] - 0.4f * k[3] * (1 - (float) (e * e / (cr * cr)))
                        : base + k[3] * (float) ((k[2] - e) / (k[2] - cr));
                ground = Math.max(ground, Math.round(top));
                volcanic = true;
            }
            // Fresh lava and ash near the top; plants have long since covered the lower flanks and extinct cones."""),
("""    private final boolean perchedBanks;""", """    private final boolean perchedBanks;
    private final boolean calderas;       // calderas and cinder cones: format 21"""),
("""        perchedBanks = data.format >= 20;""", """        perchedBanks = data.format >= 20;
        calderas = data.format >= 21;"""),
("""    static final float PERCHED_HOLD = 4;""", """    static final float CALDERA_SHARE = 0.45f, CALDERA_SHIELDS = 0.35f, CALDERA_ISLANDS = 0.5f; // extinct arc volcanoes, shields, calderas with an island
    static final float CALDERA_RADIUS = 0.16f, CALDERA_DEPTH = 70, CALDERA_MIN = 40, CALDERA_LAKE = 0.35f; // of the footprint; blocks at most; smallest volcano; lake this far below the rim, of the depth
    static final int CALDERA_AGE = 120, CINDER_AGE = 60;  // shields older than this have worn their caldera away; younger than this still sprout cinder cones
    static final float CINDER_RADIUS = 16, CINDER_STEEP = 0.45f; // blocks across a cinder cone's base, half; height per block of radius
    static final float PERCHED_HOLD = 4;"""),
])
edit('src/main/java/com/realtectonics/sim/PlanetData.java', [
("""    private static final int FORMAT = 20;""", """    private static final int FORMAT = 21;"""),
("""20 perched banks (no new data)""", """20 perched banks, 21 calderas and cinder cones (no new data)"""),
])
print("ok")
