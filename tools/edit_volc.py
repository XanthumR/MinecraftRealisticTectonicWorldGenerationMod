exec(open('tools/edit_glacier.py').read().split("C = ")[0])
V = 'src/main/java/com/realtectonics/sim/Volcano.java'
edit(V, [
(""" * @param baseMetres the ground it was built on; NaN for planets saved before volcanoes shaped the rivers
 */
public record Volcano(float u, float v, boolean hotspot, int age, float size, float baseMetres) {""",
 """ * @param baseMetres the ground it was built on; NaN for planets saved before volcanoes shaped the rivers
 * @param spread     footprint radius per block of height
 */
public record Volcano(float u, float v, boolean hotspot, int age, float size, float baseMetres, float spread) {
    static final float ARC_SPREAD = 1.6f, SHIELD_SPREAD = 2.2f;
    static final float OLD_SHIELD_SPREAD = 3.5f; // planets saved before tuning: young islands ~1,400 blocks across

    /** compact: planets from format 9 on; older ones keep their wide shields so their saved flanks still fit. */
    static float spread(boolean hotspot, boolean compact) {
        return hotspot ? (compact ? SHIELD_SPREAD : OLD_SHIELD_SPREAD) : ARC_SPREAD;
    }
"""),
("""        return new Volcano(u, v, hotspot, age, size, metres);""",
 """        return new Volcano(u, v, hotspot, age, size, metres, spread);"""),
("""        return height * (hotspot ? 3.5f : 1.6f);""",
 """        return height * spread;"""),
])
P = 'src/main/java/com/realtectonics/sim/Planet.java'
edit(P, [
("""    public Planet(int w, int h, int plateCount, float oceanFraction, long seed) {
        this.w = w;
        this.h = h;""",
 """    public Planet(int w, int h, int plateCount, float oceanFraction, long seed) {
        this(w, h, plateCount, oceanFraction, 1, seed);
    }

    /** volcanoes: 1 is Earth-like (for a game), 0 none, 2 twice as many arc volcanoes and hotspots. */
    public Planet(int w, int h, int plateCount, float oceanFraction, float volcanoes, long seed) {
        this.w = w;
        this.h = h;
        volcanism = volcanoes;"""),
("""    private final int[] hotspots;       // fixed in the mantle; plates drift over them""",
 """    private final int[] hotspots;       // fixed in the mantle; plates drift over them
    private final float volcanism;      // how many volcanoes, relative to normal"""),
("""        hotspots = new int[Math.max(2, Math.round(w * h / 131072f))]; // 4 on a medium planet""",
 """        hotspots = new int[Math.round(Math.max(2, w * h / 131072f) * volcanism)]; // 4 on a medium planet"""),
("""        for (int c : candidates) {
            boolean crowded = false;
            for (Volcano v : out)
                crowded |= Math.hypot(dx(v.u(), c % w), v.v() - c / w) < ARC_SPACING;
            if (!crowded) out.add(new Volcano(c % w, c / w, false, 0, volcanic.nextFloat(), Float.NaN));
        }
        for (int[] s : seamounts) out.add(new Volcano(s[0] % w, s[0] / w, true, stepCount - s[2], volcanic.nextFloat(), Float.NaN));""",
 """        double spacing = ARC_SPACING / Math.sqrt(volcanism); // twice the volcanoes along the same arcs
        for (int c : volcanism > 0 ? candidates : List.<Integer>of()) {
            boolean crowded = false;
            for (Volcano v : out)
                crowded |= Math.hypot(dx(v.u(), c % w), v.v() - c / w) < spacing;
            if (!crowded) out.add(new Volcano(c % w, c / w, false, 0, volcanic.nextFloat(), Float.NaN, Volcano.ARC_SPREAD));
        }
        for (int[] s : seamounts)
            out.add(new Volcano(s[0] % w, s[0] / w, true, stepCount - s[2], volcanic.nextFloat(), Float.NaN, Volcano.SHIELD_SPREAD));"""),
])
D = 'src/main/java/com/realtectonics/sim/PlanetData.java'
edit(D, [
("""    private static final int FORMAT = 8; // 2 volcanoes, 3 rock provinces, 4 volcano bases, 5 glaciers, 6 shelves, 7 deltas, 8 currents (no new data); older files load without them""",
 """    private static final int FORMAT = 9; // 2 volcanoes, 3 rock provinces, 4 volcano bases, 5 glaciers, 6 shelves, 7 deltas, 8 currents, 9 compact shields (no new data); older files load without them"""),
("""        Planet planet = new Planet(settings.width(), settings.height(), settings.plates(), settings.oceanFraction(), seed);""",
 """        Planet planet = new Planet(settings.width(), settings.height(), settings.plates(), settings.oceanFraction(), settings.volcanoes(), seed);"""),
("""                for (int n = in.readInt(); n > 0; n--)
                    d.volcanoes.add(new Volcano(in.readFloat(), in.readFloat(), in.readBoolean(), in.readInt(), in.readFloat(),
                            format >= 4 ? in.readFloat() : Float.NaN));""",
 """                for (int n = in.readInt(); n > 0; n--) {
                    float u = in.readFloat(), v = in.readFloat();
                    boolean hotspot = in.readBoolean();
                    d.volcanoes.add(new Volcano(u, v, hotspot, in.readInt(), in.readFloat(), format >= 4 ? in.readFloat() : Float.NaN,
                            Volcano.spread(hotspot, format >= 9)));
                }"""),
])
H = 'src/main/java/com/realtectonics/sim/Hydrology.java'
edit(H, [
("""        for (Volcano placed : volcanoes) {
            int cu = (int) placed.u(), cv = (int) placed.v();
            float radius = placed.radiusBlocks() / Terrain.BLOCKS_PER_CELL;
            for (int dy = -(int) radius; dy <= radius; dy++)
                for (int dx = -(int) radius; dx <= radius; dx++) {
                    double d = Math.hypot(dx, dy);
                    if (d >= radius || cv + dy < 0 || cv + dy >= h) continue;
                    int g = Math.floorMod(cu + dx, w) + (cv + dy) * w;
                    elevation[g] = Math.max(elevation[g], Planet.fromMetres(placed.surfaceMetres(d * Terrain.BLOCKS_PER_CELL)));
                }
        }""",
 """        // Overlapping cones blend with a smooth maximum, so neighbours in a chain merge without a straight crease.
        float[] cones = new float[n];
        Arrays.fill(cones, Float.NaN);
        for (Volcano placed : volcanoes) {
            int cu = (int) placed.u(), cv = (int) placed.v();
            float radius = placed.radiusBlocks() / Terrain.BLOCKS_PER_CELL;
            for (int dy = -(int) radius; dy <= radius; dy++)
                for (int dx = -(int) radius; dx <= radius; dx++) {
                    double d = Math.hypot(dx, dy);
                    if (d >= radius || cv + dy < 0 || cv + dy >= h) continue;
                    int g = Math.floorMod(cu + dx, w) + (cv + dy) * w;
                    float s = placed.surfaceMetres(d * Terrain.BLOCKS_PER_CELL);
                    cones[g] = Float.isNaN(cones[g]) ? s : smoothMax(cones[g], s, CONE_BLEND);
                }
        }
        for (int g = 0; g < n; g++)
            if (!Float.isNaN(cones[g])) elevation[g] = Math.max(elevation[g], Planet.fromMetres(cones[g]));"""),
("""    static final float COAST_RAMP = 6;          // cells over which land climbs from the shore to full height
""", """    static final float COAST_RAMP = 6;          // cells over which land climbs from the shore to full height
    static final float CONE_BLEND = 400;        // metres over which two overlapping volcanoes' slopes round into each other
"""),
("""    private void blur(float[] f) {""",
 """    /** Polynomial smooth maximum: equals max(a, b) once they differ by more than k, rounds the crease where they meet. */
    static float smoothMax(float a, float b, float k) {
        float t = Math.clamp(0.5f + 0.5f * (b - a) / k, 0, 1);
        return a + (b - a) * t + k * t * (1 - t);
    }

    private void blur(float[] f) {"""),
])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""            int reach = v.hotspot() ? 34 : 18; // cells: roughly the widest cone of each kind""",
 """            int reach = Float.isNaN(v.baseMetres()) ? (v.hotspot() ? 34 : 18) // legacy: roughly the widest cone of each kind
                    : (int) Math.ceil(v.radiusBlocks() / BLOCKS_PER_CELL);"""),
])
S = 'src/main/java/com/realtectonics/sim/PlanetSettings.java'
s = open(S, encoding='utf-8').read()
s = s.replace("/** zoom: world scale, how many times wider than BLOCKS_PER_CELL each cell is in blocks (1, 2 or 4). */\npublic record PlanetSettings(int width, int plates, float oceanFraction, float mountainScale, int zoom) {\n    public static final PlanetSettings DEFAULT = new PlanetSettings(1024, 14, 0.7f, 1, 1);",
 "/**\n * zoom: world scale, how many times wider than BLOCKS_PER_CELL each cell is in blocks (1, 2 or 4).\n * volcanoes: how many volcanoes, 1 normal, 0 none, 2 twice as many.\n */\npublic record PlanetSettings(int width, int plates, float oceanFraction, float mountainScale, int zoom, float volcanoes) {\n    public static final PlanetSettings DEFAULT = new PlanetSettings(1024, 14, 0.7f, 1, 1, 1);")
s = s.replace("return width == o.width && plates == o.plates && oceanFraction == o.oceanFraction;", "return width == o.width && plates == o.plates && oceanFraction == o.oceanFraction && volcanoes == o.volcanoes;")
import re
s = re.sub(r"new PlanetSettings\(([^)]*), (zoom|v)\);", lambda m: "new PlanetSettings(%s, %s, volcanoes);" % (m.group(1), m.group(2)), s)
s = s.rstrip()[:-1] + """
    public PlanetSettings withVolcanoes(float v) {
        return new PlanetSettings(width, plates, oceanFraction, mountainScale, zoom, v);
    }
}
"""
open(S, 'w', encoding='utf-8', newline='').write(s)
G = 'src/main/java/com/realtectonics/mod/TectonicChunkGenerator.java'
edit(G, [
("""                    .optionalFieldOf("zoom", 1).forGetter(PlanetSettings::zoom) // worlds from before world scale stay at 1
    ).apply(i, PlanetSettings::new));""",
 """                    .optionalFieldOf("zoom", 1).forGetter(PlanetSettings::zoom), // worlds from before world scale stay at 1
            Codec.floatRange(0, 2).optionalFieldOf("volcanoes", 1f).forGetter(PlanetSettings::volcanoes)
    ).apply(i, PlanetSettings::new));"""),
])
PS = 'src/main/java/com/realtectonics/mod/client/PlanetScreen.java'
edit(PS, [
("""                .create(cx - 155, y, 310, 20, Component.literal("World scale"), (button, z) -> settings = settings.withZoom(z)));""",
 """                .create(cx - 155, y, 150, 20, Component.literal("World scale"), (button, z) -> settings = settings.withZoom(z)));
        addRenderableWidget(new Slider(cx + 5, y, "Volcanoes", 0, 2, settings.volcanoes(), true, true, v -> settings = settings.withVolcanoes((float) v)));"""),
])
for f in ['src/test/java/com/realtectonics/sim/PlanetDataTest.java']:
    s = open(f, encoding='utf-8').read().replace(", 1, 1), p -> {})", ", 1, 1, 1), p -> {})")
    open(f, 'w', encoding='utf-8', newline='').write(s)
print("ok")
