exec(open('tools/edit_glacier.py').read().split("C = ")[0])
H = 'src/main/java/com/realtectonics/sim/Hydrology.java'
edit(H, [
("""    public Hydrology(Planet planet, float[] rainMm, float[] warmth, int iterations, long seed, DoubleConsumer progress) {
        this(planet.w, planet.h, rainMm, warmth);""",
 """    public Hydrology(Planet planet, float[] rainMm, float[] warmth, int iterations, long seed, DoubleConsumer progress) {
        this(planet, rainMm, warmth, null, iterations, seed, progress);
    }

    /**
     * swing: °C half the summer-winter range per cell, as Climate.amplitude; null for no seasons. Ice Age glaciers then
     * need cool summers, not just a cold year, and lakes evaporate by the warmth of their summers.
     */
    public Hydrology(Planet planet, float[] rainMm, float[] warmth, float[] swing, int iterations, long seed, DoubleConsumer progress) {
        this(planet.w, planet.h, rainMm, warmth);
        this.swing = swing;"""),
("""    private final float[] warmth;""", """    private final float[] warmth;
    private float[] swing;           // °C half the summer-winter range, or null: no seasons"""),
("""            if (iceAgeTemperature(c) < ICE_ACCUMULATION && rainMm[c] >= ICE_MIN_RAIN) ice[c] = GLACIATED;
            if (ice[c] != 0 && r >= 0 && elevation[r] >= 0 && iceAgeTemperature(r) < ICE_REACH) ice[r] = GLACIATED;""",
 """            // Ice builds where summers are too cool to melt the winter's snow: a cold year alone is not enough, as the
            // Ice Age left dry, hot-summered Siberia bare. Without seasons a typical swing stands in.
            if (iceAgeSummer(c) < ICE_ACCUMULATION + TYPICAL_SWING && rainMm[c] >= ICE_MIN_RAIN) ice[c] = GLACIATED;
            if (ice[c] != 0 && r >= 0 && elevation[r] >= 0 && iceAgeSummer(r) < ICE_REACH + TYPICAL_SWING) ice[r] = GLACIATED;"""),
("""    private float iceAgeTemperature(int g) {""",
 """    private float iceAgeSummer(int g) {
        return iceAgeTemperature(g) + (swing == null ? TYPICAL_SWING : swing[g]);
    }

    private float iceAgeTemperature(int g) {"""),
("""        return MIN_EVAPORATION + EVAPORATION_PER_DEGREE * Math.max(t, 0);""",
 """        return MIN_EVAPORATION + EVAPORATION_PER_DEGREE * Climate.warmMean(t, swing == null ? 0 : swing[g]); // summer heat dries lakes"""),
("""    static final float ICE_REACH = 3;""", """    static final float TYPICAL_SWING = 6;       // °C half-range the yearly-mean thresholds were tuned for: a maritime mid-latitude
    static final float ICE_REACH = 3;"""),
])
P = 'src/main/java/com/realtectonics/sim/PlanetData.java'
edit(P, [
("""    public final byte[] ice;            // Hydrology.GLACIATED or FJORD, else 0""",
 """    public final byte[] ice;            // Hydrology.GLACIATED or FJORD, else 0
    public final float[] amplitude;     // °C half the summer-winter range (format 24; 0 before)
    public final float[] summerRain;    // share of the rain falling in the warm half of the year (format 24; 0.5 before)"""),
("""        ice = new byte[w * h];
    }""", """        ice = new byte[w * h];
        amplitude = new float[w * h];
        summerRain = new float[w * h];
        java.util.Arrays.fill(summerRain, 0.5f);
    }"""),
("""        System.arraycopy(climate.precipitation, 0, d.precipitation, 0, d.precipitation.length);""",
 """        System.arraycopy(climate.precipitation, 0, d.precipitation, 0, d.precipitation.length);
        System.arraycopy(climate.amplitude, 0, d.amplitude, 0, d.amplitude.length);
        System.arraycopy(climate.summerRain, 0, d.summerRain, 0, d.summerRain.length);"""),
("""        Hydrology hy = new Hydrology(planet, climate.precipitation, climate.anomaly, EROSION, seed,""",
 """        Hydrology hy = new Hydrology(planet, climate.precipitation, climate.anomaly, climate.amplitude, EROSION, seed,"""),
("""            out.write(rock);
            out.write(ice);""", """            out.write(rock);
            out.write(ice);
            for (float v : amplitude) out.writeFloat(v);
            for (float v : summerRain) out.writeFloat(v);"""),
("""            if (format >= 5) in.readFully(d.ice);""", """            if (format >= 5) in.readFully(d.ice);
            if (format >= 24) {
                for (int i = 0; i < d.amplitude.length; i++) d.amplitude[i] = in.readFloat();
                for (int i = 0; i < d.summerRain.length; i++) d.summerRain[i] = in.readFloat();
            }"""),
("""    private static final int FORMAT = 23;""", """    private static final int FORMAT = 24;"""),
("""23 flowing glaciers (no new data)""", """23 flowing glaciers (no new data), 24 seasons (amplitude, summerRain)"""),
])
print("ok")
