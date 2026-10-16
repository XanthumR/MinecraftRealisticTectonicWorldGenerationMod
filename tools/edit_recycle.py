exec(open('tools/edit_glacier.py').read().split("C = ")[0])
import re
C='src/main/java/com/realtectonics/sim/Climate.java'
line = [l for l in open(C, encoding='utf-8').read().split('\n') if 'static final float LAKE_RAIN_MAX = 1000;' in l][0]
edit(C, [
("""                    float rain = Math.min(moisture, moisture * stable * (band * RAINOUT + Math.max(0, rise) * OROGRAPHIC));
                    moisture -= rain;
                    if (i >= w) {
                        anomaly[g] += share * warmth;""",
 """                    float rain = Math.min(moisture, moisture * stable * (band * RAINOUT + Math.max(0, rise) * OROGRAPHIC));
                    moisture -= rain;
                    // Moisture recycling: land gives part of its rain back to the air, through plants and wet soil, so
                    // rain reaches deep into continents (a third to half of the Amazon's is recycled). Frozen ground
                    // gives little: it needs a growing season.
                    float swingHere = seaSwing + (landSwing - seaSwing) * land;
                    if (m >= 0) moisture += rain * RECYCLING * smoothstep(0, 15, t + swingHere);
                    if (i >= w) {
                        anomaly[g] += share * warmth;"""),
("""                        amplitude[g] += share * (seaSwing + (landSwing - seaSwing) * land);""",
 """                        amplitude[g] += share * swingHere;"""),
(line, """    static final float RECYCLING = Float.parseFloat(System.getProperty("rt.recycling", "0.5")); // share of land rain that evaporates back into the air in a warm season
""" + line),
])
print("ok")
