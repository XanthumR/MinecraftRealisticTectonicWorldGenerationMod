exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
# biomes by season
("""        Biome b = Biome.classify(toMetres(y), t, rain, iceCapBelow);""",
 """        Biome b = seasons ? Biome.classify(toMetres(y), t, rain, bilinear(data.amplitude, u, v), bilinear(data.summerRain, u, v))
                : Biome.classify(toMetres(y), t, rain, iceCapBelow);"""),
# glaciers by winter snow and summer melt
("""            float t = data.temperature[g], snow = data.precipitation[g] / 1000 * Math.clamp((ICE_LINE - t) / 4, 0, 1);
            float balance = t < ICE_LINE ? snow : -ICE_MELT * (t - ICE_LINE); // metres of ice a year""",
 """            float t = data.temperature[g], balance; // metres of ice a year
            if (seasons) { // the winter's snow against the summer's melt
                float a = data.amplitude[g], s = data.summerRain[g], frozen = Climate.frozenShare(t, a);
                float snowShare = frozen <= 0.5f ? (1 - s) * 2 * frozen : (1 - s) + s * 2 * (frozen - 0.5f); // the frozen months are winter first
                balance = data.precipitation[g] / 1000 * snowShare - ICE_DDF * Climate.warmMean(t, a);
            } else {
                float snow = data.precipitation[g] / 1000 * Math.clamp((ICE_LINE - t) / 4, 0, 1);
                balance = t < ICE_LINE ? snow : -ICE_MELT * (t - ICE_LINE);
            }"""),
# frost shattering makes scree
("""        if (folds && natural > SEA_Y + SCREE_ABOVE && fine(x, z, 16, 72) > 0.1f) {""",
 """        // From format 24 rock shatters most where the ground freezes and thaws through the year: more scree there.
        float screePatch = seasons ? SCREE_PATCH - SCREE_FROST * (frost(u, v, natural) - 0.5f) : SCREE_PATCH;
        if (folds && natural > SEA_Y + SCREE_ABOVE && fine(x, z, 16, 72) > screePatch) {"""),
# seasonal floods braid rivers
("""        boolean braided = landforms && river != NO_RIVER && data.ice[river.cell] != 0 && ice == Column.NO_WATER && !inFjord && !gorge;""",
 """        boolean braided = landforms && river != NO_RIVER && (data.ice[river.cell] != 0 || seasons && seasonalFloods(river))
                && ice == Column.NO_WATER && !inFjord && !gorge;"""),
("""    /** Ridged massif relief""",
 """    /** 0..1: how much of the year the ground at height y crosses freezing; most where the mean is near 0 and the swing large. */
    private float frost(double u, double v, float y) {
        float a = bilinear(data.amplitude, u, v);
        return a <= 0 ? 0 : Math.clamp(1 - Math.abs(temperature(u, v, y)) / a, 0, 1);
    }

    /**
     * Rivers whose water comes in one flood season (snowmelt in spring, the monsoon in summer) carry more gravel than
     * they can keep in one channel: on gentle reaches they braid across it.
     */
    private boolean seasonalFloods(River river) {
        if (river.flow < BRAID_FLOW || river.top - river.bottom > BRAID_DROP) return false;
        int c = river.cell;
        float melt = Climate.frozenShare(data.temperature[c], data.amplitude[c]);
        return data.summerRain[c] > MONSOON_BRAID || melt > 0.3f && melt < 0.7f;
    }

    /** Ridged massif relief"""),
("""    private final boolean glacierFlow;""", """    private final boolean glacierFlow;
    private final boolean seasons;        // summers and winters: biomes, glacier balance, frost scree, flood-season rivers: format 24"""),
("""        glacierFlow = data.format >= 23;""", """        glacierFlow = data.format >= 23;
        seasons = data.format >= 24;"""),
("""    static final float ICE_LINE = -4;""", """    static final float ICE_DDF = 2.2f;     // metres of ice melted per degree-year above freezing (6 mm a day per °C)
    static final float SCREE_PATCH = 0.1f, SCREE_FROST = 0.6f; // raggedness noise above which scree lies; how far frost lowers that bar
    static final float BRAID_FLOW = 400, BRAID_DROP = 3, MONSOON_BRAID = 0.75f; // flood-season rivers this big and gentle braid; summer rain share of a monsoon
    static final float ICE_LINE = -4;"""),
])
print("ok")
