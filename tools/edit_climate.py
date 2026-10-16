exec(open('tools/edit_glacier.py').read().split("C = ")[0])
C = 'src/main/java/com/realtectonics/sim/Climate.java'
edit(C, [
("""/**
 * Annual-mean climate from latitude and terrain. Temperature falls with latitude and altitude.
 * Rain comes from moisture the prevailing winds carry off the oceans; it thins out inland and
 * is wrung out by mountains, leaving rain shadows behind them.
 */""",
 """/**
 * Annual-mean climate from latitude and terrain. Temperature falls with latitude and altitude.
 * Rain comes from moisture the prevailing winds carry off the oceans; it thins out inland and
 * is wrung out by mountains, leaving rain shadows behind them. Ocean gyres warm or chill the sea along
 * each coast, the winds carry that inland, and summer monsoons blow wet sea air poleward onto tropical land.
 */"""),
("""    static final float MM_PER_UNIT = 130000;     // calibration: wet equatorial coasts get ~2000 mm
""", """    static final float MM_PER_UNIT = 130000;     // calibration: wet equatorial coasts get ~2000 mm
    static final float BOUNDARY_CURRENT = 4;     // cells: warm western boundary currents hug their coast (Gulf Stream)
    static final float EASTERN_CURRENT = 8;      // cells: eastern ones spread wider (California, Humboldt, North Atlantic Drift)
    static final float CURRENT_ANOMALY = 5;      // °C the strongest currents warm or chill the sea
    static final float CONTINENTAL = 8;          // °C colder deep inside high-latitude continents (Siberia, Canada)
    static final float SEA_COUPLING = 0.3f;      // per cell: air over the sea takes on the sea's warmth
    static final float LAND_COUPLING = 0.05f;    // per cell: inland, air forgets the ocean over ~20 cells
    static final float MONSOON = 0.6f;           // share of the year the summer monsoon blows, at its peak latitudes
    static final double MONSOON_REACH = 35;      // degrees: how far poleward the monsoon pushes
"""),
("""    public final float[] precipitation; // mm per year
""", """    public final float[] precipitation; // mm per year
    public final float[] anomaly;       // °C the oceans and continentality add to a cell's latitude-and-altitude temperature
    final float[] seaAnomaly;           // °C ocean currents add to the sea surface
"""),
("""        temperature = new float[w * h];
        precipitation = new float[w * h];
        for (int y = 0; y < h; y++) {""",
 """        temperature = new float[w * h];
        precipitation = new float[w * h];
        anomaly = new float[w * h];
        seaAnomaly = currents(planet);
        for (int y = 0; y < h; y++) {"""),
("""                float moisture = 0, felt = 0;
                for (int i = 0; i < 2 * w; i++) { // the first lap only spins up moisture, so the wrap seam is steady
                    int g = Math.floorMod(dir * i, w) + y * w;
                    float m = Planet.metres(planet.height[g] - Planet.SEA_LEVEL), ground = Math.max(m, 0);
                    float t = seaTemp - LAPSE_RATE * ground;
                    if (m < 0) moisture += (capacity(t) - moisture) * EVAPORATION;""",
 """                float moisture = 0, felt = 0, warmth = 0;
                float inland = -CONTINENTAL * smoothstep(30, 60, abs); // far from any ocean, high-latitude winters win
                for (int i = 0; i < 2 * w; i++) { // the first lap only spins up moisture, so the wrap seam is steady
                    int g = Math.floorMod(dir * i, w) + y * w;
                    float m = Planet.metres(planet.height[g] - Planet.SEA_LEVEL), ground = Math.max(m, 0);
                    warmth += ((m < 0 ? seaAnomaly[g] : inland) - warmth) * (m < 0 ? SEA_COUPLING : LAND_COUPLING);
                    float t = seaTemp + warmth - LAPSE_RATE * ground;
                    if (m < 0) moisture += (capacity(t) - moisture) * EVAPORATION; // warm currents feed more rain"""),
("""                    if (i >= w) {
                        temperature[g] = t;
                        precipitation[g] += share * rain * MM_PER_UNIT;
                    }""",
 """                    if (i >= w) {
                        anomaly[g] += share * warmth;
                        precipitation[g] += share * rain * MM_PER_UNIT;
                    }"""),
("""        // ponytail: winds are purely east-west; a north-south blur stands in for meridional mixing""",
 """        for (int g = 0; g < w * h; g++)
            temperature[g] = seaLevelTemperature(latitude(g / w, h)) + anomaly[g] - LAPSE_RATE * Math.max(Planet.metres(planet.height[g] - Planet.SEA_LEVEL), 0);
        monsoon(planet);
        // ponytail: winds are zonal plus the monsoon; a north-south blur stands in for the rest of meridional mixing
        // ponytail: cold eastern currents only chill coasts the westerlies reach; trade winds blow offshore there, so the
        // Atacama and Namib coasts are dry (from the continent behind them) but not cool"""),
("""    /**
     * Degrees, +90 at the top row""",
 """    /**
     * Sea-surface warmth from the wind-driven gyres. A western boundary current runs poleward warm in the subtropics
     * (Gulf Stream, Kuroshio) and equatorward cold in subpolar seas (Labrador, Oyashio). On the eastern side of an
     * ocean, cold water wells up in the tropics and subtropics (Humboldt, Benguela), while the drift of the subpolar
     * gyre brings warm water to high-latitude west coasts (Norway, British Columbia).
     */
    private float[] currents(Planet planet) {
        float[] sst = new float[w * h];
        int[] fromWest = new int[w], fromEast = new int[w]; // cells to land on that side, along the row
        for (int y = 0; y < h; y++) {
            double abs = Math.abs(latitude(y, h));
            float western = CURRENT_ANOMALY * (smoothstep(10, 20, abs) * (1 - smoothstep(40, 50, abs)) - smoothstep(45, 55, abs) * (1 - smoothstep(70, 80, abs)));
            float eastern = CURRENT_ANOMALY * (smoothstep(42, 50, abs) * (1 - smoothstep(70, 80, abs)) - (1 - smoothstep(30, 40, abs)));
            for (int dir : new int[]{1, -1}) {
                int[] dist = dir > 0 ? fromWest : fromEast;
                int d = w;
                for (int i = 0; i < 2 * w; i++) { // two laps, so the wrap seam sees the land beyond it
                    int x = Math.floorMod(dir * i, w);
                    d = planet.height[x + y * w] >= Planet.SEA_LEVEL ? 0 : Math.min(d + 1, w);
                    if (i >= w) dist[x] = d;
                }
            }
            for (int x = 0; x < w; x++)
                sst[x + y * w] = western * (float) Math.exp(-fromWest[x] / BOUNDARY_CURRENT) + eastern * (float) Math.exp(-fromEast[x] / EASTERN_CURRENT);
        }
        return sst;
    }

    /**
     * Summer monsoon: land heats faster than sea, so in summer moist tropical sea air is drawn poleward onto the land.
     * Swept along each column from the equator to MONSOON_REACH degrees, raining as it goes, hardest against mountains.
     */
    private void monsoon(Planet planet) {
        for (int x = 0; x < w; x++)
            for (int dir : new int[]{-1, 1}) { // north from the equator, and south
                float moisture = 0, felt = 0;
                for (int y = h / 2 + (dir < 0 ? -1 : 0); y >= 0 && y < h; y += dir) {
                    double abs = Math.abs(latitude(y, h));
                    if (abs > MONSOON_REACH) break;
                    int g = x + y * w;
                    float m = Planet.metres(planet.height[g] - Planet.SEA_LEVEL), ground = Math.max(m, 0);
                    if (m < 0) moisture += (capacity(temperature[g]) - moisture) * EVAPORATION;
                    float rise = (ground - felt) * TERRAIN_SMOOTHING;
                    felt += rise;
                    float rain = Math.min(moisture, moisture * (RAINOUT + Math.max(0, rise) * OROGRAPHIC));
                    moisture -= rain;
                    // Strongest at 10-25°: nearer the equator it's rainy all year anyway, farther out the heat low fades.
                    float season = MONSOON * smoothstep(3, 12, abs) * (1 - smoothstep(25, (float) MONSOON_REACH, abs));
                    if (m >= 0) precipitation[g] += season * rain * MM_PER_UNIT;
                }
            }
    }

    /**
     * Degrees, +90 at the top row"""),
])
H = 'src/main/java/com/realtectonics/sim/Hydrology.java'
edit(H, [
("""    public Hydrology(Planet planet, float[] rainMm, int iterations, long seed) {
        this.rainMm = rainMm;""",
 """    public Hydrology(Planet planet, float[] rainMm, int iterations, long seed) {
        this(planet, rainMm, new float[planet.w * planet.h], iterations, seed);
    }

    /** warmth: °C the oceans and continentality add to each cell, as Climate.anomaly; it moves the Ice Age ice too. */
    public Hydrology(Planet planet, float[] rainMm, float[] warmth, int iterations, long seed) {
        this.rainMm = rainMm;
        this.warmth = warmth;"""),
("""    private final float[] rainMm;
""", """    private final float[] rainMm;
    private final float[] warmth;
"""),
("""        return Climate.seaLevelTemperature(Climate.latitude(g / w, h)) - ICE_AGE_COOLING""",
 """        return Climate.seaLevelTemperature(Climate.latitude(g / w, h)) + warmth[g] - ICE_AGE_COOLING"""),
])
edit('src/main/java/com/realtectonics/sim/PlanetData.java', [
("""        Hydrology hy = new Hydrology(planet, climate.precipitation, EROSION, seed);""",
 """        Hydrology hy = new Hydrology(planet, climate.precipitation, climate.anomaly, EROSION, seed);"""),
])
edit('src/main/java/com/realtectonics/sim/Prototype.java', [
("""        Hydrology hydro = new Hydrology(planet, climate.precipitation, erosion, seed);""",
 """        Hydrology hydro = new Hydrology(planet, climate.precipitation, climate.anomaly, erosion, seed);"""),
])
print("ok")
