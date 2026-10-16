p = 'src/main/java/com/realtectonics/sim/Climate.java'
s = open(p, encoding='utf-8').read()
reps = [
("""                float moisture = 0, felt = 0, plateau = 0;
                // A high plateau poleward heats up in summer and draws the monsoon far inland (Tibet: India to 30°N;
                // the Colorado Plateau: Arizona): the reach grows with the highest ground at 25-40° in this column.
                for (int y = 0; y < h; y++) {
                    double lat = latitude(y, h) * -dir;
                    if (lat >= 25 && lat <= 40) plateau = Math.max(plateau, Planet.metres(planet.height[x + y * w] - Planet.SEA_LEVEL));
                }
                float reach = (float) MONSOON_REACH + PLATEAU_REACH * smoothstep(1000, 4000, plateau);""",
 """                float moisture = 0, felt = 0, reach = (float) MONSOON_REACH;"""),
("""    static final float PLATEAU_REACH = 15;       // degrees more behind a 4 km plateau (Tibet), less behind lower ones
""", ""),
("""                    float rain = Math.min(moisture, moisture * (RAINOUT + Math.max(0, rise) * OROGRAPHIC));
                    moisture -= rain;
                    // Strongest""",
 """                    float rain = Math.min(moisture, moisture * (RAINOUT + Math.max(0, rise) * OROGRAPHIC));
                    moisture -= rain;
                    if (m >= 0) moisture += rain * RECYCLING * MONSOON_RECYCLING; // summer land is warm: it gives rain back, as under the main winds
                    // Strongest"""),
("""    static final float RECYCLING = 0.5f;""", """    static final float MONSOON_RECYCLING = Float.parseFloat(System.getProperty("rt.mr", "1")); // of RECYCLING, inside the monsoon
    static final float RECYCLING = 0.5f;"""),
]
for a, b in reps:
    assert s.count(a) == 1, a
    s = s.replace(a, b)
open(p, 'w', encoding='utf-8').write(s)
print("ok")
