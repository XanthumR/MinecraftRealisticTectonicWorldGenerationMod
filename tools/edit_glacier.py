def edit(p, pairs):
    s = open(p, encoding='utf-8').read()
    for a, b in pairs:
        assert s.count(a) == 1, (p, a)
        rest = s[s.index(a) + len(a):].split(chr(10), 1)[0]
        assert a.endswith(chr(10)) or '//' not in rest, ('anchor stops before a trailing comment; match the whole line', p, a)
        s = s.replace(a, b)
    open(p, 'w', encoding='utf-8', newline='').write(s)

C = 'src/main/java/com/realtectonics/sim/Climate.java'
edit(C, [
("""            float seaTemp = (float) (-25 + 52 * Math.cos(Math.toRadians(lat)));""",
 """            float seaTemp = seaLevelTemperature(lat);"""),
("""    private static float smoothstep(""",
 """    /** Annual mean °C at sea level: 27 at the equator, -25 at the poles. */
    static float seaLevelTemperature(double latitude) {
        return (float) (-25 + 52 * Math.cos(Math.toRadians(latitude)));
    }

    private static float smoothstep("""),
])

H = 'src/main/java/com/realtectonics/sim/Hydrology.java'
edit(H, [
("""    static final float COAST_RAMP = 6;          // cells over which land climbs from the shore to full height
""",
 """    static final float COAST_RAMP = 6;          // cells over which land climbs from the shore to full height
    // Ice Age glaciers: they left the U-shaped valleys, and where they cut down to the sea, the fjords.
    static final float ICE_AGE_COOLING = 8;     // °C colder at the last glacial maximum, at high latitudes
    static final float ICE_ACCUMULATION = -2;   // °C, ice-age annual mean: snow outlasts summer and builds glaciers
    static final float ICE_REACH = 3;           // °C: glacier tongues flow down into this much warmth before melting
    static final float ICE_MIN_RAIN = 250;      // mm: drier land stayed bare, as Siberia and interior Alaska did
    static final float GLACIAL_EROSION = 3;     // glaciers deepen valleys this many times faster than rivers
    static final float FJORD_METRES = 15;       // a glacial valley this close to sea level floods into a fjord
    public static final byte GLACIATED = 1, FJORD = 2;
"""),
("""    public final List<Volcano> volcanoes = new ArrayList<>(); // with their bases, as built into this landscape
""",
 """    public final List<Volcano> volcanoes = new ArrayList<>(); // with their bases, as built into this landscape
    public final byte[] ice;        // 0, GLACIATED or FJORD: what the Ice Age glaciers did here
"""),
("""        lake = new boolean[n];
""", """        lake = new boolean[n];
        ice = new byte[n];
"""),
("""        for (int i = 0; i < iterations; i++) {
            route();
            erode();
        }
        route();
        markLakes();""",
 """        for (int i = 0; i < iterations; i++) {
            route();
            markGlaciers();
            erode();
        }
        route();
        markGlaciers();
        markFjords();
        markLakes();"""),
("""    public boolean isLake(int g) {""",
 """    /** Snow builds glaciers where the ice age was cold and wet enough; the ice then flows down the valleys until it melts. */
    private void markGlaciers() {
        Arrays.fill(ice, (byte) 0);
        for (int i = count - 1; i >= 0; i--) { // donors before receivers
            int c = order[i], r = receiver[c];
            if (elevation[c] < 0) continue;
            if (iceAgeTemperature(c) < ICE_ACCUMULATION && rainMm[c] >= ICE_MIN_RAIN) ice[c] = GLACIATED;
            if (ice[c] != 0 && r >= 0 && elevation[r] >= 0 && iceAgeTemperature(r) < ICE_REACH) ice[r] = GLACIATED;
        }
    }

    private float iceAgeTemperature(int g) {
        return Climate.seaLevelTemperature(Climate.latitude(g / w, h)) - ICE_AGE_COOLING
                - Climate.LAPSE_RATE * Planet.metres(Math.max(elevation[g], 0));
    }

    /** Glacial valleys cut down to sea level, all the way from the sea: the sea flooded them when the ice melted. */
    private void markFjords() {
        for (int i = 0; i < count; i++) { // receivers before donors
            int c = order[i], r = receiver[c];
            if (ice[c] == GLACIATED && r >= 0 && Planet.metres(elevation[c]) < FJORD_METRES && (elevation[r] < 0 || ice[r] == FJORD)) ice[c] = FJORD;
        }
    }

    public boolean isLake(int g) {"""),
("""            float f = ERODIBILITY * (float) Math.pow(flow[c], AREA_EXPONENT) / dist;""",
 """            float f = ERODIBILITY * (ice[c] != 0 ? GLACIAL_EROSION : 1) * (float) Math.pow(flow[c], AREA_EXPONENT) / dist;"""),
])

D = 'src/main/java/com/realtectonics/sim/PlanetData.java'
edit(D, [
("""    private static final int FORMAT = 4; // 2 added volcanoes, 3 rock provinces, 4 volcano bases; older files load without them""",
 """    private static final int FORMAT = 5; // 2 added volcanoes, 3 rock provinces, 4 volcano bases, 5 glaciers; older files load without them"""),
("""    public final byte[] rock;           // Rock ordinal: the geological province
""", """    public final byte[] rock;           // Rock ordinal: the geological province
    public final byte[] ice;            // Hydrology.GLACIATED or FJORD, else 0
"""),
("""        rock = new byte[w * h]; // all MIXED until set
""", """        rock = new byte[w * h]; // all MIXED until set
        ice = new byte[w * h];
"""),
("""        System.arraycopy(hy.receiver, 0, d.receiver, 0, d.receiver.length);""",
 """        System.arraycopy(hy.receiver, 0, d.receiver, 0, d.receiver.length);
        System.arraycopy(hy.ice, 0, d.ice, 0, d.ice.length);"""),
("""            out.write(rock);
""", """            out.write(rock);
            out.write(ice);
"""),
("""            if (format >= 3) in.readFully(d.rock);""",
 """            if (format >= 3) in.readFully(d.rock);
            if (format >= 5) in.readFully(d.ice);"""),
])
print("ok")
