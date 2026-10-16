exec(open('tools/edit_glacier.py').read().split("C = ")[0])
C = 'src/main/java/com/realtectonics/sim/Climate.java'
edit(C, [
("""    static final double MONSOON_REACH = 35;      // degrees: how far poleward the monsoon pushes
""", """    static final double MONSOON_REACH = 35;      // degrees: how far poleward the monsoon pushes
    static final float SEA_SWING = 4, LAND_SWING = 27; // °C, half the summer-winter range at high latitude over sea and deep inland (Yakutsk ~30)
    static final float MEDITERRANEAN = 0.35f;    // share of the westerlies' rain the summer high pushes into winter at 30-45° (dry summers)
    static final float TROPICAL_SUMMER = 0.25f;  // share of tropical rain the sun-following rain belt moves into summer
    static final float MONSOON_SUMMER = 0.9f;    // share of monsoon rain that falls in summer
"""),
("""    public final float[] anomaly;       // °C the oceans and continentality add to a cell's latitude-and-altitude temperature
""", """    public final float[] anomaly;       // °C the oceans and continentality add to a cell's latitude-and-altitude temperature
    public final float[] amplitude;     // °C, half the range between summer and winter: the sea keeps it small, continents large
    public final float[] summerRain;    // share of the year's rain falling in the warm half: monsoons high, Mediterranean coasts low
"""),
("""        anomaly = new float[w * h];
        seaAnomaly = currents(planet);""", """        anomaly = new float[w * h];
        amplitude = new float[w * h];
        summerRain = new float[w * h];
        seaAnomaly = currents(planet);"""),
("""            float band = (float) (0.6 + 0.4 * Math.cos(Math.toRadians(lat * 6)));""",
 """            float band = (float) (0.6 + 0.4 * Math.cos(Math.toRadians(lat * 6)));
            // Seasons: the swing grows toward the poles and far inland, where no sea stores the summer's heat.
            float seaSwing = 1 + (SEA_SWING - 1) * smoothstep(10, 45, abs), landSwing = 2 + (LAND_SWING - 2) * smoothstep(0, 65, abs);
            float summerShare = 0.5f + TROPICAL_SUMMER * smoothstep(5, 10, abs) * (1 - smoothstep(20, 28, abs)); // rain belt follows the sun
            float winterWesterlies = 0.5f - MEDITERRANEAN * smoothstep(28, 34, abs) * (1 - smoothstep(42, 48, abs));"""),
("""                float moisture = 0, felt = 0, marine = 0, continental = 0;""",
 """                float moisture = 0, felt = 0, marine = 0, continental = 0, land = 0;
                float season = dir > 0 ? winterWesterlies : summerShare; // summer share of the rain these winds bring"""),
("""                    continental += ((m < 0 ? 0 : inland) - continental) * k;""",
 """                    continental += ((m < 0 ? 0 : inland) - continental) * k;
                    land += ((m < 0 ? 0 : 1) - land) * k; // how far this air has come over land"""),
("""                        anomaly[g] += share * warmth;
                        precipitation[g] += share * rain * MM_PER_UNIT;""",
 """                        anomaly[g] += share * warmth;
                        precipitation[g] += share * rain * MM_PER_UNIT;
                        amplitude[g] += share * (seaSwing + (landSwing - seaSwing) * land);
                        summerRain[g] += share * rain * MM_PER_UNIT * season;"""),
("""        for (int pass = 0; pass < 4; pass++) blurNorthSouth(anomaly);""",
 """        for (int pass = 0; pass < 4; pass++) {
            blurNorthSouth(anomaly);
            blurNorthSouth(amplitude);
        }"""),
("""        for (int pass = 0; pass < 4; pass++) blurNorthSouth(precipitation);
    }""", """        for (int pass = 0; pass < 4; pass++) {
            blurNorthSouth(precipitation);
            blurNorthSouth(summerRain);
        }
        for (int g = 0; g < w * h; g++) summerRain[g] = precipitation[g] > 0 ? Math.clamp(summerRain[g] / precipitation[g], 0, 1) : 0.5f;
    }

    /** Mean over the year of the warmth above 0 °C, for annual mean t and half-range a: degree-years of melt and evaporation. */
    public static float warmMean(float t, float a) {
        if (a <= 0 || t >= a) return Math.max(t, 0);
        if (t <= -a) return 0;
        double edge = Math.acos(-t / a); // the warm part of the year runs from -edge to +edge around midsummer
        return (float) ((t * edge + a * Math.sin(edge)) / Math.PI);
    }

    /** Share of the year below freezing, for annual mean t and half-range a. */
    public static float frozenShare(float t, float a) {
        if (a <= 0) return t < 0 ? 1 : 0;
        return (float) (1 - Math.acos(Math.clamp(-t / a, -1, 1)) / Math.PI);
    }"""),
("""                    if (m >= 0) wet[g] = season * rain * MM_PER_UNIT;""", """                    if (m >= 0) wet[g] = season * rain * MM_PER_UNIT;"""),
("""        for (int g = 0; g < w * h; g++) if (planet.height[g] >= Planet.SEA_LEVEL) precipitation[g] += wet[g];""",
 """        for (int g = 0; g < w * h; g++)
            if (planet.height[g] >= Planet.SEA_LEVEL) {
                precipitation[g] += wet[g];
                summerRain[g] += wet[g] * MONSOON_SUMMER;
            }"""),
])
print("ok")
