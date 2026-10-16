p = 'src/main/java/com/realtectonics/sim/Climate.java'
s = open(p, encoding='utf-8').read()
reps = [
("""        monsoon(planet);""", """        monsoon(planet);
        summerFlow(planet);"""),
("""    private void monsoon(Planet planet) {""",
 """    /**
     * Summer air pushed poleward round the western side of the subtropical highs: warm, wet sea air carried onto the
     * east coasts of continents at 25-45 degrees (the Gulf of Mexico into the eastern US, the South China Sea into China,
     * the Coral Sea onto eastern Australia). Each column is swept poleward from 15 degrees; the air takes up water only
     * over warm sea (a warm current), so coasts off cold currents (the Sahara's, California's) stay dry. Most of it falls
     * in summer.
     */
    private void summerFlow(Planet planet) {
        float[] wet = new float[w * h];
        for (int x = 0; x < w; x++)
            for (int dir : new int[]{-1, 1}) { // north, and south
                float moisture = 0, felt = 0;
                for (int y = h / 2 + (dir < 0 ? -1 : 0); y >= 0 && y < h; y += dir) {
                    double abs = Math.abs(latitude(y, h));
                    if (abs < FLOW_FROM) continue;
                    if (abs > FLOW_TO) break;
                    int g = x + y * w;
                    float m = Planet.metres(planet.height[g] - Planet.SEA_LEVEL), ground = Math.max(m, 0);
                    if (m < 0) moisture += (capacity(temperature[g]) - moisture) * EVAPORATION * smoothstep(-1, 2, seaAnomaly[g]);
                    float rise = (ground - felt) * TERRAIN_SMOOTHING;
                    felt += rise;
                    float rain = Math.min(moisture, moisture * (RAINOUT + Math.max(0, rise) * OROGRAPHIC) / (1 + Math.max(0, -rise) * OROGRAPHIC * LEE_DRYING));
                    moisture -= rain;
                    if (m >= 0) moisture += Math.min(rain * RECYCLING, RECYCLING_MAX_MM / MM_PER_UNIT);
                    float season = SUMMER_FLOW * (1 - smoothstep((float) FLOW_TO - 8, (float) FLOW_TO, abs));
                    if (m >= 0) wet[g] = season * rain * MM_PER_UNIT;
                }
            }
        for (int pass = 0; pass < 4; pass++) { // each column was swept alone; the flow is broad
            float[] src = wet.clone();
            for (int g = 0; g < w * h; g++) {
                int x = g % w, row = g - x;
                wet[g] = (src[row + Math.floorMod(x - 1, w)] + src[g] + src[row + (x + 1) % w]) / 3;
            }
        }
        for (int g = 0; g < w * h; g++)
            if (planet.height[g] >= Planet.SEA_LEVEL) {
                precipitation[g] += wet[g];
                summerRain[g] += wet[g] * MONSOON_SUMMER;
            }
    }

    private void monsoon(Planet planet) {"""),
("""    static final float RECYCLING = 0.5f;""", """    static final float SUMMER_FLOW = Float.parseFloat(System.getProperty("rt.sf", "0.6")); // share of the year warm sea air flows poleward onto east coasts
    static final double FLOW_FROM = 15, FLOW_TO = Double.parseDouble(System.getProperty("rt.sfto", "48")); // degrees it is swept between
    static final float RECYCLING = 0.5f;"""),
]
for a, b in reps:
    assert s.count(a) == 1, a
    s = s.replace(a, b)
open(p, 'w', encoding='utf-8').write(s)
print("ok")
