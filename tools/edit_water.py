exec(open('tools/edit_glacier.py').read().split("C = ")[0])
C = 'src/main/java/com/realtectonics/sim/Climate.java'
edit(C, [
("""    static final float CONTINENTAL_SUMMER = 0.15f; // extra summer share of rain deep inland, where summer storms bring most of it""",
 """    static final float CONTINENTAL_SUMMER = 0.15f; // extra summer share of rain deep inland, where summer storms bring most of it
    static final float LAKE_EVAPORATION = 0.5f;  // of the sea's: a lake is shallow, and frozen part of the year where it is cold"""),
("""    /** Mean over the year of the warmth above 0 °C, for annual mean t and half-range a: degree-years of melt and evaporation. */""",
 """    /**
     * Lake-effect rain, from format 38: big lakes give off water too, and the same winds carry it to their far shores
     * and beyond (the snow belts east of the Great Lakes, the wet shores of Lake Victoria). Lakes are only known once
     * the rivers have run, so this is a second pass that only adds rain; it feeds the biomes, not the rivers.
     */
    public void lakeEffect(Planet planet, java.util.function.IntPredicate lake) {
        float[] extra = new float[w * h];
        for (int y = 0; y < h; y++) {
            double abs = Math.abs(latitude(y, h));
            float westerly = smoothstep(25, 35, abs) * (1 - smoothstep(55, 65, abs)), band = (float) (0.6 + 0.4 * Math.cos(Math.toRadians(latitude(y, h) * 6)));
            for (int dir : new int[]{1, -1}) {
                float share = dir > 0 ? westerly : 1 - westerly, moisture = 0, felt = 0;
                if (share == 0) continue;
                for (int i = 0; i < 2 * w; i++) {
                    int g = Math.floorMod(dir * i, w) + y * w;
                    float ground = Math.max(Planet.metres(planet.height[g] - Planet.SEA_LEVEL), 0);
                    if (lake.test(g)) moisture += (capacity(temperature[g]) - moisture) * EVAPORATION * LAKE_EVAPORATION * warmShare(temperature[g], amplitude[g]);
                    if (planet.height[g] < Planet.SEA_LEVEL) moisture = 0; // over the sea the sea's own moisture already counts
                    float rise = (ground - felt) * TERRAIN_SMOOTHING;
                    felt += rise;
                    float rain = Math.min(moisture, moisture * (band * RAINOUT + Math.max(0, rise) * OROGRAPHIC));
                    moisture -= rain;
                    if (i >= w) extra[g] += share * rain * MM_PER_UNIT;
                }
            }
        }
        for (int pass = 0; pass < 4; pass++) blurNorthSouth(extra);
        for (int g = 0; g < w * h; g++) precipitation[g] += extra[g];
    }

    /** Share of the year above freezing, when open water can evaporate. */
    private static float warmShare(float t, float a) {
        return 1 - frozenShare(t, a);
    }

    /** Mean over the year of the warmth above 0 °C, for annual mean t and half-range a: degree-years of melt and evaporation. */"""),
])
P = 'src/main/java/com/realtectonics/sim/PlanetData.java'
edit(P, [
("""        System.arraycopy(climate.precipitation, 0, d.precipitation, 0, d.precipitation.length);""",
 """        climate.lakeEffect(planet, hy::isLake);
        System.arraycopy(climate.precipitation, 0, d.precipitation, 0, d.precipitation.length);"""),
])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""    static final float BEACH_SLOPE = 60;              // blocks the beach rises per unit of land mask; from format 37 divided by the beach's width factor""",
 """    static final float BEACH_SLOPE = 60;              // blocks the beach rises per unit of land mask; from format 37 divided by the beach's width factor
    static final float RIPARIAN_MM = 700, RIPARIAN_FLOW = 2000, RIPARIAN_REACH = 48; // format 38: rain-equivalent a big river's groundwater gives its banks; flow that counts as big; blocks its green strip reaches
    static final float CRATER_HEAT = 2.5f;            // format 38: craters' radii from an active vent within which its heat keeps ice off"""),
("""    private final boolean variedShores;   // beaches wide in bays and at river mouths, narrow and shingly on headlands; no gully pits: format 37""",
 """    private final boolean variedShores;   // beaches wide in bays and at river mouths, narrow and shingly on headlands; no gully pits: format 37
    private final boolean wetBanks;       // rivers green their banks; active craters melt the ice around them: format 38"""),
("""        variedShores = data.format >= 37;""", """        variedShores = data.format >= 37;
        wetBanks = data.format >= 38;"""),
("""        Biome biome = marsh && ground >= SEA_Y - 2 ? marsh(x, z, u, v) : biome(x, z, u, v, Math.max(organic && !(landforms && smooth < SEA_Y && ground >= SEA_Y) ? Math.min(ground, smooth) : ground, ice));""",
 """        // From format 38 a river waters its banks from below: through a desert it runs in a green strip (the Nile), wider
        // and greener the bigger the river.
        float big = wetBanks && river != NO_RIVER ? (float) Math.sqrt(Math.min(1, river.flow() / RIPARIAN_FLOW)) : 0;
        float banks = big > 0 ? RIPARIAN_MM * big * (1 - smoothstep(Math.max(0, river.edge()) / (RIPARIAN_REACH * big))) : 0;
        Biome biome = marsh && ground >= SEA_Y - 2 ? marsh(x, z, u, v) : biome(x, z, u, v, Math.max(organic && !(landforms && smooth < SEA_Y && ground >= SEA_Y) ? Math.min(ground, smooth) : ground, ice), banks);"""),
("""    private Biome biome(double x, double z, double u, double v, float y) {
        float t = localTemperature(x, z, u, v, y), rain = bilinear(data.precipitation, u, v);""",
 """    private Biome biome(double x, double z, double u, double v, float y) {
        return biome(x, z, u, v, y, 0);
    }

    /** banks: mm of rain a river's groundwater stands in for. */
    private Biome biome(double x, double z, double u, double v, float y, float banks) {
        float t = localTemperature(x, z, u, v, y), rain = bilinear(data.precipitation, u, v) + banks;"""),
("""            boolean lake = water != Column.NO_WATER && lakeness > SHORE_FULL;
            // Over a river""",
 """            boolean lake = water != Column.NO_WATER && lakeness > SHORE_FULL;
            if (wetBanks) top = ground + (top - ground) * (1 - craterHeat(x, z)); // from 38 an erupting crater melts its ice away
            // Over a river"""),
("""    /** The column at real block (x, z). */
    public Column column(int x, int z) {""",
 """    /** From format 38: 1 inside an active volcano's crater, fading to 0 at CRATER_HEAT crater radii, where ice may lie again. */
    private float craterHeat(double x, double z) {
        float heat = 0;
        for (Cone c : conesNear(x, z)) {
            if (!c.v().active() || c.collapsed() || c.craterRadius() <= 0) continue;
            double d = Math.hypot(wrapBlocks(x - c.x()), z - c.z());
            heat = Math.max(heat, 1 - smoothstep((float) ((d - c.craterRadius()) / ((CRATER_HEAT - 1) * c.craterRadius()))));
        }
        return heat;
    }

    /** The column at real block (x, z). */
    public Column column(int x, int z) {"""),
])
p = 'src/main/java/com/realtectonics/sim/PlanetData.java'; s = open(p, encoding='utf-8').read()
assert s.count('FORMAT = 37;') == 1
s = s.replace('FORMAT = 37;', 'FORMAT = 38;').replace('37 varied beaches and no gully pits; older', '37 varied beaches and no gully pits, 38 lake-effect rain, green riverbanks, ice-free active craters; older')
open(p, 'w', encoding='utf-8').write(s)
print("ok")
