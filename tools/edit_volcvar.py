exec(open('tools/edit_glacier.py').read().split("C = ")[0])
V = 'src/main/java/com/realtectonics/sim/Volcano.java'
edit(V, [
("""    /** Surface in metres at d blocks from the summit, ignoring the crater: strato cones concave like Fuji, shields domed like Hawaii. */""",
 """    static final float LOBE_MAX = 1.9f;     // varied cones: widest reach over the mean footprint (wide volcano, longest lobe)
    static final float SCAR_SHARE = 0.3f;   // varied cones: share of arc volcanoes whose flank slid away (Mount St Helens, Stromboli)
    static final float SCAR_DEPTH = 0.35f;  // of the height a collapse scar takes off the summit
    static final double SCAR_HALF = 0.45;   // radians: half the scar's width

    /** 0..1, fixed per volcano and per k: what makes this volcano unlike its neighbours. */
    private double roll(int k) {
        long hsh = Float.floatToIntBits(u) * 0x9E3779B97F4A7C15L ^ Float.floatToIntBits(v) * 0xC2B2AE3D27D4EB4FL ^ k * 0x94D049BB133111EBL;
        hsh = (hsh ^ (hsh >>> 31)) * 0xBF58476D1CE4E5B9L;
        return ((hsh ^ (hsh >>> 29)) & 0xFFFFFF) / (double) 0x1000000;
    }

    /**
     * From format 36 (varied = true): the surface dx, dz blocks from the summit, each volcano its own. Runny magma
     * builds wide, gentle cones and sticky magma narrow, steep ones; lava runs further down some sides than others,
     * so the footprint has lobes; and some arc volcanoes lost a flank in a landslide, leaving a horseshoe scar open
     * downhill. The profile runs in blocks, not metres: the sea is squeezed into fewer blocks than the land, so a
     * cone shaped in metres stands on its submarine flank as a narrow spire with a tiny island.
     */
    public float surfaceMetres(double dx, double dz, boolean varied) {
        double d = Math.hypot(dx, dz);
        if (!varied) return surfaceMetres(d);
        double a = Math.atan2(dz, dx), lobes = 1 + (0.25 + 0.2 * roll(1)) * (0.5 * Math.cos(a - 7 * roll(2)) + 0.3 * Math.cos(2 * a - 7 * roll(3)) + 0.2 * Math.cos(3 * a - 7 * roll(4)));
        double t = Math.min(1, d / (radiusBlocks() * (0.8 + 0.5 * roll(5)) * lobes));
        double drop = hotspot ? Math.pow(t, 1.6 + 0.8 * roll(6)) : Math.pow(t, 0.4 + 0.35 * roll(6)); // shields domed like Hawaii, strato cones concave like Fuji
        float b0 = Terrain.defaultBlocks(baseMetres), b1 = Terrain.defaultBlocks(baseMetres + riseMetres());
        double scar = 0;
        if (!hotspot && roll(7) < SCAR_SHARE) {
            double off = Math.abs(Math.IEEEremainder(a - 2 * Math.PI * roll(8), 2 * Math.PI)) / SCAR_HALF;
            scar = SCAR_DEPTH * (1 - smooth(off)) * (1 - smooth(t / 0.7));
        }
        return Terrain.defaultMetres(b0 + (b1 - b0) * (float) (1 - drop - scar));
    }

    private static double smooth(double x) {
        x = Math.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    /** Surface in metres at d blocks from the summit, ignoring the crater: strato cones concave like Fuji, shields domed like Hawaii. */"""),
])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""        return metres / (metres >= 0 ? LAND_METRES_PER_BLOCK : SEA_METRES_PER_BLOCK);
    }""", """        return metres / (metres >= 0 ? LAND_METRES_PER_BLOCK : SEA_METRES_PER_BLOCK);
    }

    /** The inverse of defaultBlocks. */
    static float defaultMetres(float blocks) {
        return blocks * (blocks >= 0 ? LAND_METRES_PER_BLOCK : SEA_METRES_PER_BLOCK);
    }"""),
("""    private float coneY(Cone c, double d) {
        return softCeiling(toY(c.v.surfaceMetres(d)));""",
 """    private float coneY(Cone c, double d) {
        return softCeiling(toY(c.v.surfaceMetres(d)));
    }

    /** The cone's surface at this column; from format 36 each volcano has its own shape, so the direction matters. */
    private float coneY(Cone c, double x, double z) {
        return softCeiling(toY(c.v.surfaceMetres(wrapBlocks(x - c.x), z - c.z, variedVolcanoes)));"""),
("""    private record Cone(Volcano v, double x, double z, float radius, float craterRadius, float craterDepth, float height, boolean legacy,
                        boolean caldera, float[][] cinders, boolean collapsed) {
        double reach() {
            return legacy ? radius : SUMMIT * radius;""",
 """    private record Cone(Volcano v, double x, double z, float radius, float craterRadius, float craterDepth, float height, boolean legacy,
                        boolean caldera, float[][] cinders, boolean collapsed, float rim, float lobes) {
        double reach() { // lobes: from format 36 a cone reaches further down some sides
            return legacy ? radius : SUMMIT * radius * lobes;"""),
("""    private final boolean deepTrenches;   // the abyss squeezed so ridges and trenches fit between sea level and bedrock: format 35""",
 """    private final boolean deepTrenches;   // the abyss squeezed so ridges and trenches fit between sea level and bedrock: format 35
    private final boolean variedVolcanoes; // each volcano its own width, steepness, lobes and collapse scar, profile in blocks: format 36"""),
("""        deepTrenches = data.format >= 35;""", """        deepTrenches = data.format >= 35;
        variedVolcanoes = data.format >= 36;"""),
("""            Cone cone = new Cone(placed, cx, cz, radius, craterRadius, craterDepth, height, legacy, caldera, cinders, collapsed);""",
 """            float rim = toY(placed.surfaceMetres(craterRadius)); // the lowest point of the rim holds the crater's pool
            for (int i = 0; variedVolcanoes && i < 16; i++)
                rim = Math.min(rim, toY(placed.surfaceMetres(craterRadius * Math.cos(i * Math.PI / 8), craterRadius * Math.sin(i * Math.PI / 8), true)));
            Cone cone = new Cone(placed, cx, cz, radius, craterRadius, craterDepth, height, legacy, caldera, cinders, collapsed, softCeiling(rim),
                    variedVolcanoes ? Volcano.LOBE_MAX : 1);"""),
("""            int reach = (int) Math.ceil((landforms ? cone.radius : cone.reach()) / BLOCKS_PER_CELL) + 1, cu = Math.round(v.u()), cv = Math.round(v.v());
            for (int dy = -reach; dy <= reach; dy++)
                for (int dx = -reach; dx <= reach; dx++)
                    conesByCell""",
 """            int reach = (int) Math.ceil((landforms ? cone.radius * cone.lobes : cone.reach()) / BLOCKS_PER_CELL) + 1, cu = Math.round(v.u()), cv = Math.round(v.v());
            for (int dy = -reach; dy <= reach; dy++)
                for (int dx = -reach; dx <= reach; dx++)
                    conesByCell"""),
("""            float s = coneY(c, d) + rough * (1 + 0.02f * c.height), fade = smoothstep((float) ((c.reach() - d) / (0.1 * c.radius)));
            if (s > natural) natural += fade * (s - natural);""",
 """            float s = coneY(c, x, z) + rough * (1 + 0.02f * c.height), fade = smoothstep((float) ((c.reach() - d) / (0.1 * c.radius)));
            if (s > natural) natural += fade * (s - natural);"""),
("""            float rim = coneY(c, c.craterRadius), pool = rim""", """            float rim = c.rim, pool = rim"""),
("""            } else if (d < c.craterRadius && !(ownCraters && ground > coneY(c, d) + CRATER_OWN + (rampShores ? c.craterDepth : 0)) && !drowned(c)) {""",
 """            } else if (d < c.craterRadius && !(ownCraters && ground > coneY(c, x, z) + CRATER_OWN + (rampShores ? c.craterDepth : 0)) && !drowned(c)) {"""),
("""                float kept = rampShores ? smoothstep((ground - coneY(c, d) - CRATER_OWN) / c.craterDepth) : 0;""",
 """                float kept = rampShores ? smoothstep((ground - coneY(c, x, z) - CRATER_OWN) / c.craterDepth) : 0;"""),
("""            if (d < c.reach() && !c.collapsed) y = Math.max(y, coneY(c, d));""",
 """            if (d < c.reach() && !c.collapsed) y = Math.max(y, coneY(c, x, z));"""),
])
H = 'src/main/java/com/realtectonics/sim/Hydrology.java'
edit(H, [
("""            float radius = placed.radiusBlocks() / Terrain.BLOCKS_PER_CELL;""",
 """            float radius = Volcano.LOBE_MAX * placed.radiusBlocks() / Terrain.BLOCKS_PER_CELL;"""),
("""                    float s = placed.surfaceMetres(d * Terrain.BLOCKS_PER_CELL);""",
 """                    float s = placed.surfaceMetres(dx * Terrain.BLOCKS_PER_CELL, dy * Terrain.BLOCKS_PER_CELL, true);
                    if (s <= placed.baseMetres()) continue; // past this side's footprint"""),
])
p = 'src/main/java/com/realtectonics/sim/PlanetData.java'; s = open(p, encoding='utf-8').read()
assert s.count('FORMAT = 35;') == 1
s = s.replace('FORMAT = 35;', 'FORMAT = 36;').replace('35 ridges and trenches (abyss squeezed); older', '35 ridges and trenches (abyss squeezed), 36 varied volcanoes; older')
open(p, 'w', encoding='utf-8').write(s)
print("ok")
