exec(open('tools/edit_glacier.py').read().split("C = ")[0])
V = 'src/main/java/com/realtectonics/sim/Volcano.java'
edit(V, [
("""    /** Surface in metres at d blocks from the summit, ignoring the crater: strato cones concave like Fuji, shields domed like Hawaii. */""",
 """    static final float CALDERA_SHARE = 0.3f;        // of the footprint radius: Crater Lake is ~9 km across on a ~30 km volcano
    static final float CALDERA_DEPTH = 1200;        // metres a caldera floor sinks below its rim at most
    static final float CALDERA_SHIELDS = 0.35f;     // share of shield volcanoes past their active years that collapsed
    static final int CALDERA_AGE = 120;             // steps after which a shield's caldera has worn away

    /**
     * Whether the volcano's summit fell in. A volcano that drains a large magma chamber in one great eruption loses
     * the roof over it. Bigger arc volcanoes had bigger chambers, so they collapse more often; shields collapse in
     * their late active years and the caldera wears away as the island ages. Never while still erupting.
     */
    public boolean caldera() {
        if (active()) return false;
        long hsh = Float.floatToIntBits(u) * 0x9E3779B97F4A7C15L ^ Float.floatToIntBits(v) * 0xC2B2AE3D27D4EB4FL;
        hsh = (hsh ^ (hsh >>> 31)) * 0xBF58476D1CE4E5B9L;
        double roll = ((hsh ^ (hsh >>> 29)) & 0xFFFFFF) / (double) 0x1000000;
        return hotspot ? age < CALDERA_AGE && roll < CALDERA_SHIELDS : roll < size;
    }

    /** As surfaceMetres, but a collapsed summit is a caldera: a flat floor under walls that steepen up to its rim. */
    public float surfaceMetres(double d, boolean collapsed) {
        double r = CALDERA_SHARE * radiusBlocks();
        if (!collapsed || d >= r) return surfaceMetres(d);
        float rim = surfaceMetres(r), depth = Math.min(CALDERA_DEPTH, 0.4f * (rim - baseMetres));
        float t = (float) Math.clamp((d / r - 0.6) / 0.4, 0, 1), wall = t * t * (3 - 2 * t);
        return rim - depth + depth * wall * wall;
    }

    /** Surface in metres at d blocks from the summit, ignoring the crater: strato cones concave like Fuji, shields domed like Hawaii. */"""),
])
H = 'src/main/java/com/realtectonics/sim/Hydrology.java'
edit(H, [
("""        for (Volcano placed : volcanoes) {
            int cu = (int) placed.u(), cv = (int) placed.v();""",
 """        for (Volcano placed : volcanoes) {
            int cu = (int) placed.u(), cv = (int) placed.v();
            boolean collapsed = placed.caldera(); // the cone goes up as max(land, cone): a caldera removes only the volcano's own rock"""),
("""                    float s = placed.surfaceMetres(d * Terrain.BLOCKS_PER_CELL);""",
 """                    float s = placed.surfaceMetres(d * Terrain.BLOCKS_PER_CELL, collapsed);"""),
])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""            boolean caldera = calderas && height >= CALDERA_MIN && !v.active()""",
 """            boolean collapsed = simCalderas && v.caldera(); // from format 22 the simulation built it, lake and all
            boolean caldera = calderas && !simCalderas && height >= CALDERA_MIN && !v.active()"""),
("""            float craterRadius = caldera ? CALDERA_RADIUS * radius : radius * (v.hotspot() ? 0.08f : 0.06f) + 5;""",
 """            float craterRadius = collapsed ? 0 : caldera ? CALDERA_RADIUS * radius : radius * (v.hotspot() ? 0.08f : 0.06f) + 5;"""),
("""            Cone cone = new Cone(placed, cx, cz, radius, craterRadius, craterDepth, height, legacy, caldera, cinders);""",
 """            Cone cone = new Cone(placed, cx, cz, radius, craterRadius, craterDepth, height, legacy, caldera, cinders, collapsed);"""),
("""                        boolean caldera, float[][] cinders) {""",
 """                        boolean caldera, float[][] cinders, boolean collapsed) {"""),
("""    private final boolean calderas;       // calderas and cinder cones: format 21""",
 """    private final boolean calderas;       // calderas and cinder cones: format 21
    private final boolean simCalderas;    // calderas simulated into the planet's cells, not stamped per block: format 22"""),
("""        calderas = data.format >= 21;""", """        calderas = data.format >= 21;
        simCalderas = data.format >= 22;"""),
])
edit('src/main/java/com/realtectonics/sim/PlanetData.java', [
("""    private static final int FORMAT = 21;""", """    private static final int FORMAT = 22;"""),
("""21 calderas and cinder cones (no new data)""", """21 calderas and cinder cones, 22 simulated calderas (no new data)"""),
])
print("ok")
