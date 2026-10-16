exec(open('tools/edit_glacier.py').read().split("C = ")[0])
V = 'src/main/java/com/realtectonics/sim/Volcano.java'
edit(V, [
("""    public float surfaceMetres(double dx, double dz, boolean varied) {
        double d = Math.hypot(dx, dz);
        if (!varied) return surfaceMetres(d);""",
 """    public float surfaceMetres(double dx, double dz, int style) {
        double d = Math.hypot(dx, dz);
        if (style == ROUND) return surfaceMetres(d);"""),
("""            double off = Math.abs(Math.IEEEremainder(a - 2 * Math.PI * roll(8), 2 * Math.PI)) / SCAR_HALF;
            scar = SCAR_DEPTH * (1 - smooth(off)) * (1 - smooth(t / 0.7));""",
 """            double rel = a - 2 * Math.PI * roll(8);
            if (style == VARIED) scar = SCAR_DEPTH * (1 - smooth(Math.abs(Math.IEEEremainder(rel, 2 * Math.PI)) / SCAR_HALF)) * (1 - smooth(t / 0.7));
            else { // from format 42 the scar is a trough of even width in blocks: an angle squeezed its walls into a blade at the summit
                double side = Math.cos(rel) > 0 ? Math.abs(d * Math.sin(rel)) : d; // behind the summit only its head bites in
                scar = SCAR_DEPTH * (1 - smooth(side / (SCAR_WIDTH * radiusBlocks()))) * (1 - smooth(t / 0.7));
            }"""),
("""    static final double SCAR_HALF = 0.45;   // radians: half the scar's width""",
 """    static final double SCAR_HALF = 0.45;   // radians: half the scar's width (format 36)
    static final double SCAR_WIDTH = 0.25;  // of the footprint radius: half the scar's width (format 42)
    public static final int ROUND = 0, VARIED = 1, SMOOTH_SCARS = 2; // cone styles: before format 36, formats 36-41, from 42"""),
])
H = 'src/main/java/com/realtectonics/sim/Hydrology.java'
edit(H, [
("""                    float s = placed.surfaceMetres(dx * Terrain.BLOCKS_PER_CELL, dy * Terrain.BLOCKS_PER_CELL, true);""",
 """                    float s = placed.surfaceMetres(dx * Terrain.BLOCKS_PER_CELL, dy * Terrain.BLOCKS_PER_CELL, Volcano.SMOOTH_SCARS);"""),
])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""                rim = Math.min(rim, toY(placed.surfaceMetres(craterRadius * Math.cos(i * Math.PI / 8), craterRadius * Math.sin(i * Math.PI / 8), true)));""",
 """                rim = Math.min(rim, toY(placed.surfaceMetres(craterRadius * Math.cos(i * Math.PI / 8), craterRadius * Math.sin(i * Math.PI / 8), coneStyle)));"""),
("""        return softCeiling(toY(c.v.surfaceMetres(wrapBlocks(x - c.x), z - c.z, variedVolcanoes)));""",
 """        return softCeiling(toY(c.v.surfaceMetres(wrapBlocks(x - c.x), z - c.z, coneStyle)));"""),
("""        boolean low = solidShores && (cubic(lakes, u, v) > 0.02f || insideCaldera(x, z));
        for (Cone c : simCalderas && !low ? conesNear(x, z) : List.<Cone>of()) {
            double d = Math.hypot(wrapBlocks(x - c.x), z - c.z);
            if (c.collapsed || c.legacy || d >= c.reach()) continue;
            float s = coneY(c, x, z) + rough * (1 + 0.02f * c.height), fade = smoothstep((float) ((c.reach() - d) / (0.1 * c.radius)));""",
 """        // From format 42 the sharpening fades out across a lake's shore band instead of stopping at its outline, which
        // left the cone standing as a wall round the lake.
        float lakeness = cubic(lakes, u, v), overLake = smoothCones ? 1 - smoothstep((lakeness - 0.02f) / SHORE_FULL) : 1;
        boolean low = solidShores && (smoothCones ? insideCaldera(x, z) : lakeness > 0.02f || insideCaldera(x, z));
        for (Cone c : simCalderas && !low ? conesNear(x, z) : List.<Cone>of()) {
            double d = Math.hypot(wrapBlocks(x - c.x), z - c.z);
            if (c.collapsed || c.legacy || d >= c.reach()) continue;
            float s = coneY(c, x, z) + rough * (1 + 0.02f * c.height), fade = overLake * smoothstep((float) ((c.reach() - d) / (0.1 * c.radius)));"""),
("""                int pit = Math.round(rim - c.craterDepth * (1 - (float) (d * d / (c.craterRadius * c.craterRadius))) + wobble);""",
 """                // From format 42 the pit's walls rise to the rim on their own side, from its lowest point at the middle,
                // instead of the whole floor sitting below the rim's lowest notch under sheer walls on the high sides.
                float rimHere = smoothCones && d > 0 ? rim + (coneY(c, c.x + (x - c.x) * c.craterRadius / d, c.z + (z - c.z) * c.craterRadius / d) - rim) * (float) (d / c.craterRadius) : rim;
                int pit = Math.round(rimHere - c.craterDepth * (1 - (float) (d * d / (c.craterRadius * c.craterRadius))) + wobble);"""),
("""            if (inRiver && water >= SEA_Y) biome = temperature(u, v, water) < 0 ? Biome.FROZEN_RIVER : Biome.RIVER;""",
 """            if ((inRiver || smoothCones && lakeness > SHORE_FULL) && water >= SEA_Y) biome = temperature(u, v, water) < 0 ? Biome.FROZEN_RIVER : Biome.RIVER; // from 42 lakes too: one water colour"""),
("""    private final boolean spreadFlow;     // block-scale runoff divides between lower neighbours: no comb of grooves on even slopes: format 41""",
 """    private final boolean spreadFlow;     // block-scale runoff divides between lower neighbours: no comb of grooves on even slopes: format 41
    private final boolean smoothCones;    // collapse scars of even width, crater floors meet their own rim, peaks fade over lakes, lakes one biome: format 42
    private final int coneStyle;          // Volcano.ROUND, VARIED or SMOOTH_SCARS by format"""),
("""        spreadFlow = data.format >= 41;""", """        spreadFlow = data.format >= 41;
        smoothCones = data.format >= 42;
        coneStyle = smoothCones ? Volcano.SMOOTH_SCARS : variedVolcanoes ? Volcano.VARIED : Volcano.ROUND;"""),
])
p = 'src/main/java/com/realtectonics/sim/PlanetData.java'; s = open(p, encoding='utf-8').read()
assert s.count('FORMAT = 41;') == 1
s = s.replace('FORMAT = 41;', 'FORMAT = 42;').replace('41 runoff spreads over even slopes; older', '41 runoff spreads over even slopes, 42 smooth collapse scars and crater floors, lakes one biome; older')
open(p, 'w', encoding='utf-8').write(s)
print("ok")
