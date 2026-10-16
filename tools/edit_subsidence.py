p = 'src/main/java/com/realtectonics/sim/Climate.java'
s = open(p, encoding='utf-8').read()
reps = [
("""            float band = (float) (0.6 + 0.4 * Math.cos(Math.toRadians(lat * 6)));""",
 """            float band = band(lat);"""),
("""            float westerly = smoothstep(25, 35, abs) * (1 - smoothstep(55, 65, abs)), band = (float) (0.6 + 0.4 * Math.cos(Math.toRadians(latitude(y, h) * 6)));""",
 """            float westerly = smoothstep(25, 35, abs) * (1 - smoothstep(55, 65, abs)), band = band(latitude(y, h));"""),
("""    /** Mean over the year of the warmth above 0 °C""",
 """    /**
     * How readily moist air rains at a latitude: air rises (wet) at the equator and near 60°, and sinks near 30° and at
     * the poles. Where the Hadley cell's air comes down, around 20-35°, it warms and dries as it sinks: the great desert
     * belt (Sahara, Arabia, Kalahari, Australia), deepened by SUBSIDENCE.
     */
    static float band(double lat) {
        double sink = (Math.abs(lat) - SUBSIDENCE_LAT) / SUBSIDENCE_WIDTH;
        return (float) ((0.6 + 0.4 * Math.cos(Math.toRadians(lat * 6))) * (1 - SUBSIDENCE * Math.exp(-sink * sink)));
    }

    /** Mean over the year of the warmth above 0 °C"""),
("""    static final float RECYCLING = 0.5f;""", """    static final double SUBSIDENCE = Double.parseDouble(System.getProperty("rt.sub", "0.6")); // how much drier the sinking air of the subtropics makes it, at the belt's centre
    static final double SUBSIDENCE_LAT = Double.parseDouble(System.getProperty("rt.sublat", "26")), SUBSIDENCE_WIDTH = 8; // degrees: centre and half-width of the desert belt
    static final float RECYCLING = 0.5f;"""),
]
for a, b in reps:
    assert s.count(a) == 1, a
    s = s.replace(a, b)
open(p, 'w', encoding='utf-8').write(s)
print("ok")
