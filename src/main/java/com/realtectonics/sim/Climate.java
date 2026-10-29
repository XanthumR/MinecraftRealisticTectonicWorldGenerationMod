package com.realtectonics.sim;

/**
 * Annual-mean climate from latitude and terrain. Temperature falls with latitude and altitude.
 * Rain comes from moisture the prevailing winds carry off the oceans; it thins out inland and
 * is wrung out by mountains, leaving rain shadows behind them. Ocean gyres warm or chill the sea along
 * each coast, the winds carry that inland, and summer monsoons blow wet sea air poleward onto tropical land.
 */
public final class Climate {
    static final float LAPSE_RATE = 6.5f / 1000; // °C per metre
    static final float EVAPORATION = 0.1f;       // per cell: how fast air over the sea saturates
    static final float RAINOUT = 0.015f;         // per cell: background rain from moist air, reaches ~1000 km inland
    static final float OROGRAPHIC = 0.00012f;    // per metre climbed: rain forced out of rising air
    static final float TERRAIN_SMOOTHING = 0.3f; // air rises over a slope spread across ~3 cells, not at a coastal cliff
    static final float REFERENCE_WIDTH = 1024;   // cells round the planet the per-cell rates here were set for (Medium)
    static final float MM_PER_UNIT = 130000;     // calibration: wet equatorial coasts get ~2000 mm
    static final float BOUNDARY_CURRENT = 4;     // cells: warm western boundary currents hug their coast (Gulf Stream)
    static final float EASTERN_CURRENT = 8;      // cells: eastern ones spread wider (California, Humboldt, North Atlantic Drift)
    static final float CURRENT_ANOMALY = 5;      // °C most currents warm or chill the sea
    static final float DRIFT_ANOMALY = 8;        // °C the subpolar drift warms high-latitude west coasts: Bergen is ~8° above its latitude
    static final float STABILITY = 0.1f;         // per °C chilled: air over a cold current is stable and rains less (California, Chile)
    static final float CONTINENTAL = 8;          // °C colder deep inside high-latitude continents (Siberia, Canada)
    static final float SEA_COUPLING = 0.3f;      // per cell: air over the sea takes on the sea's warmth
    static final float LAND_COUPLING = 0.05f;    // per cell: inland, air forgets the ocean over ~20 cells
    static final float MONSOON = 0.6f;           // share of the year the summer monsoon blows, at its peak latitudes
    static final double MONSOON_REACH = 20;      // degrees: how far poleward the monsoon pushes (the West African one stops near 18°N; at 35 it greened the Sahara: tools/koppen.py)
    static final float SEA_SWING = 4, LAND_SWING = 27; // °C, half the summer-winter range at high latitude over sea and deep inland (Yakutsk ~30)
    static final float MEDITERRANEAN = 0.35f;    // share of the westerlies' rain the summer high pushes into winter at 30-45° (dry summers)
    static final float TROPICAL_SUMMER = 0.25f;  // share of tropical rain the sun-following rain belt moves into summer
    static final float MONSOON_SUMMER = 0.9f;  // share of monsoon rain that falls in summer
    static final float CONTINENTAL_SUMMER = 0.15f; // extra summer share of rain deep inland, where summer storms bring most of it
    static final float LEE_DRYING = 10;          // how hard sinking air holds its rain back, relative to rising air wringing it out (keeps rain shadows over 3x with recycling)
    static final float RECYCLING_MAX_MM = 800;   // mm a year land can evaporate back at most: heavy rain on a windward slope mostly runs off
    static final float SUMMER_FLOW = 1.6f;       // strength of the warm sea air flowing poleward onto east coasts in summer (best fit to Köppen with FLOW_MIXING, tools/koppen.py)
    static final double FLOW_FROM = 15, FLOW_TO = 55; // degrees it is swept between
    static final float FLOW_MIXING = 0.03f;      // share of the summer stream's water lost per land cell: it waters coasts, not whole continents
    static final double SUBSIDENCE = 0.9;        // how much drier the sinking air of the subtropics makes it, at the belt's centre (best fit to Köppen, tools/koppen.py)
    static final double SUBSIDENCE_LAT = 26, SUBSIDENCE_WIDTH = 12; // degrees: centre and half-width of the desert belt (best fit to Köppen, tools/tune.sh)
    static final float RECYCLING = 0.5f;         // share of land rain that evaporates back into the air in a warm season (Amazon: a third to a half; best fit to Köppen, tools/koppen.py)
    static final float LAKE_RAIN_MAX = 1000;     // mm a year lakes can add: the snow belts east of the Great Lakes get ~5 m of snow, ~0.5 m of water
    static final float LAKE_EVAPORATION = 0.5f;  // of the sea's: a lake is shallow, and frozen part of the year where it is cold

    static final float PLATEAU_REACH = 20;       // degrees more the monsoon reaches behind a high subtropical plateau (Tibet: India to 30-35°N)
    static final float PLATEAU_LOW = 1500, PLATEAU_HIGH = 3500; // m: mean height at 28-38° (over PLATEAU_SPREAD) where that starts and is full
    static final double PLATEAU_FROM = 28, PLATEAU_TO = 38; // degrees: the latitudes whose height draws the monsoon
    static final float PLATEAU_SPREAD = 12;      // degrees of longitude either side a plateau's heat low pulls the monsoon (all of India, not only under Tibet)
    static final float EAST_RELIEF = 0.8f;       // share of the subtropics' dryness lifted on coasts with an ocean to the east (Uruguay, Natal, NSW, the Carolinas)
    static final float EAST_FROM = 24;           // degrees: east-coast relief fades in over 8 degrees from here; nearer the equator east coasts can be dry (Somalia)
    static final float EAST_REACH = 10, WIDE_OCEAN = 6; // degrees: how far inland it reaches; how wide open sea must be to count (not the Red Sea)
    static final float DIVIDING_HEIGHT = 700;    // m, U/N: how far stable air can climb on its own speed (7 m/s trades, buoyancy frequency 0.01/s)
    static final float VAPOUR_HEIGHT = 2000;     // m, scale height of water vapour: half of it lies below ~1.4 km
    static final float BARRIER_AHEAD = 5;        // degrees: how far ahead air feels a range (the Andes are ~4 wide)
    static final float GAP_REACH = 3;            // degrees north and south air looks for a way round or a pass
    static final float POTENTIAL_ET = 1500;      // mm a year wet tropical land can evaporate (the Amazon gives off ~1200-1500)
    static final float FLANK_REACH = 6;          // degrees out to sea and inland the equatorward coast wind reaches (the California Current is ~1000 km wide)
    static final float FLANK_OCEAN = 30;         // degrees of open ocean a high needs to sit over (the Pacific, the Atlantic; not the Gulf of Mexico)
    static final float MARINE_LAYER = 1000;      // m: depth of the cool coastal air under the high's inversion; higher ground stops it
    static final float MASS_ELEVATION = 0.3f;    // share of the lapse cooling a broad plateau's heated ground wins back (Tibet ~8 °C of ~29)
    static final float MASS_REACH = 3;           // degrees: how broad high ground must be to heat the air over it
    static final float REGIME_WIDTH = 30;        // degrees of longitude either side whose poleward land counts toward a monsoon regime
    static final float REGIME_LAND = 0.85f;      // land share poleward at which a continent's own seasons outweigh the ocean's high (Asia ~0.97; North America ~0.7)

    public final int w, h;

    public final float[] temperature;   // °C
    public final float[] precipitation; // mm per year
    public final float[] anomaly;       // °C the oceans and continentality add to a cell's latitude-and-altitude temperature
    public final float[] amplitude;     // °C, half the range between summer and winter: the sea keeps it small, continents large
    public final float[] summerRain;    // share of the year's rain falling in the warm half: monsoons high, Mediterranean coasts low
    final float[] seaAnomaly;           // °C ocean currents add to the sea surface
    private final float evaporation, rainout, seaCoupling, landCoupling, smoothing, flowMixing, mmPerUnit, boundaryCurrent, easternCurrent; // the rates for this planet's cell size
    private final int passes, reach;    // box blur passes and radius (cells) that spread as far as 4 passes of radius 1 do on the reference planet
    final float[][] regime;             // per column, north then south: how fully a monsoon rules its tropics (monsoonRegime)
    final float[] flank;                // 0-1, the eastern flank of a subtropical high: winds toward the equator off a west coast (eastFlank)

    public Climate(Planet planet) {
        w = planet.w;
        h = planet.h;
        // The rates above are per cell of a 1024-wide planet; cells of other planets are other sizes, so each is turned
        // into the same rate per kilometre, and a cell's rain is spread over its own width. Otherwise a Large planet's
        // air loses its water twice as fast per km and its continents dry out from the coast (Siberia 69% -> 26%).
        float s = REFERENCE_WIDTH / w;
        evaporation = per(EVAPORATION, s);
        rainout = per(RAINOUT, s);
        seaCoupling = per(SEA_COUPLING, s);
        landCoupling = per(LAND_COUPLING, s);
        smoothing = per(TERRAIN_SMOOTHING, s);
        flowMixing = per(FLOW_MIXING, s);
        mmPerUnit = MM_PER_UNIT / s;
        boundaryCurrent = BOUNDARY_CURRENT / s;
        easternCurrent = EASTERN_CURRENT / s;
        reach = Math.max(1, Math.round(1 / s)); // a pass of radius r spreads with variance r(r + 1)/3: match 4 of radius 1 at s = 1
        passes = Math.max(1, Math.round(8 / (s * s * reach * (reach + 1))));
        temperature = new float[w * h];
        precipitation = new float[w * h];
        anomaly = new float[w * h];
        amplitude = new float[w * h];
        summerRain = new float[w * h];
        seaAnomaly = currents(planet);
        float[] blocked = new float[w * h], barrier = barrierAhead(planet);
        regime = new float[][]{monsoonRegime(planet, 1), monsoonRegime(planet, -1)};
        flank = eastFlank(planet);
        java.util.stream.IntStream.range(0, h).parallel().forEach(y -> { // rows are independent
            double lat = latitude(y, h), abs = Math.abs(lat);
            float seaTemp = seaLevelTemperature(lat);
            // Three-cell circulation: trade winds and polar easterlies blow west, mid-latitude westerlies blow
            // east. Between them are calm, shifting zones, so blend the two over ~10° instead of flipping.
            float westerly = smoothstep(25, 35, abs) * (1 - smoothstep(55, 65, abs));
            float[] eastOcean = eastOcean(planet, y), reversed = regime[lat >= 0 ? 0 : 1];
            float dryBand = band(lat, 0), wetBand = band(lat, 1);
            float tropics = smoothstep(2, 10, abs) * (1 - smoothstep(30, 40, abs)); // the trades' belt; the reversal needs the Coriolis turn, so not on the equator itself
            // Seasons: the swing grows toward the poles and far inland, where no sea stores the summer's heat.
            float seaSwing = 1 + (SEA_SWING - 1) * smoothstep(10, 45, abs), landSwing = 2 + (LAND_SWING - 2) * smoothstep(0, 65, abs);
            float summerShare = 0.5f + TROPICAL_SUMMER * smoothstep(5, 10, abs) * (1 - smoothstep(20, 28, abs)); // rain belt follows the sun
            float winterWesterlies = 0.5f - MEDITERRANEAN * smoothstep(28, 34, abs) * (1 - smoothstep(42, 48, abs));
            for (int dir : new int[]{1, -1}) {
                float share = dir > 0 ? westerly : 1 - westerly;
                if (share == 0) continue;
                float moisture = 0, felt = 0, marine = 0, continental = 0, land = 0, open = 1;
                float season = dir > 0 ? winterWesterlies : summerShare; // summer share of the rain these winds bring, at the coast
                float inland = -CONTINENTAL * smoothstep(30, 60, abs); // far from any ocean, high-latitude winters win
                for (int i = 0; i < 2 * w; i++) { // the first lap only spins up moisture, so the wrap seam is steady
                    int g = Math.floorMod(dir * i, w) + y * w;
                    float m = Planet.metres(planet.height[g] - Planet.SEA_LEVEL), ground = Math.max(m, 0);
                    // Air carries the sea's warmth or chill inland, and gains the continent's own as it goes.
                    float k = m < 0 ? seaCoupling : landCoupling;
                    marine += ((m < 0 ? seaAnomaly[g] : 0) - marine) * k;
                    continental += ((m < 0 ? 0 : inland) - continental) * k;
                    land += ((m < 0 ? 0 : 1) - land) * k; // how far this air has come over land
                    float warmth = marine + continental, t = seaTemp + warmth - LAPSE_RATE * ground;
                    // Where a monsoon reverses the winds, the trades never blow: winter brings dry air off the continent
                    // (the north-east monsoon over Arabia and India) and summer's rain is the monsoon's own.
                    float off = dir < 0 ? reversed[g % w] * tropics : 0;
                    if (m < 0) moisture += (capacity(t) - moisture) * evaporation * (1 - off); // warm currents feed more rain
                    moisture -= moisture * evaporation * off;
                    // Blocking (Sheppard's dividing streamline): stable air lower than the barrier ahead less U/N cannot
                    // climb it and turns aside. Water vapour crowds into the lowest few km, so a high range turns most of
                    // the moisture away (the Andes, Tibet) and a low one hardly any. The trades' share streams poleward
                    // along the barrier: the low-level jets east of the Andes and the Rockies.
                    if (dir < 0) {
                        float through = (float) Math.exp(-Math.max(0, barrier[g] - ground - DIVIDING_HEIGHT) / VAPOUR_HEIGHT);
                        if (through >= 1) open = 1; // past the crest: the next barrier blocks afresh
                        else if (through < open) {
                            float turned = moisture * (1 - through / open);
                            moisture -= turned;
                            open = through;
                            if (i >= w) blocked[g] += share * turned;
                        }
                    }
                    float rise = (ground - felt) * smoothing;
                    felt += rise;
                    float stable = Math.clamp(1 + STABILITY * Math.min(marine, 0), 0.3f, 1), band = dryBand + (wetBand - dryBand) * eastOcean[g % w]; // band() is linear in eastOcean
                    // Air sinking down a lee slope warms and stops raining (the föhn): rain shadows stay dry downwind.
                    float rain = Math.min(moisture, moisture * stable * (band * rainout + Math.max(0, rise) * OROGRAPHIC) / (1 + Math.max(0, -rise) * OROGRAPHIC * LEE_DRYING));
                    moisture -= rain;
                    // Moisture recycling: land gives part of its rain back to the air, through plants and wet soil, so
                    // rain reaches deep into continents (a third to half of the Amazon's is recycled). Frozen ground
                    // gives little: it needs a growing season.
                    float swingHere = seaSwing + (landSwing - seaSwing) * land;
                    // Only the plain rain counts: what mountains wring out runs off their slopes, which is what keeps their
                    // lee dry. Plants and soil give back so much at most.
                    if (m >= 0) moisture += Math.min(Math.min(rain, (moisture + rain) * stable * band * rainout) * RECYCLING, RECYCLING_MAX_MM / mmPerUnit) * smoothstep(0, 15, t + swingHere);
                    if (i >= w) {
                        anomaly[g] += share * warmth;
                        precipitation[g] += share * rain * aloft(band * rainout, rise, ground) * mmPerUnit;
                        amplitude[g] += share * swingHere;
                        // Dry summers are a west-coast climate: inland the summer heat brings thunderstorms instead.
                        float here = season + (0.5f + CONTINENTAL_SUMMER - season) * land;
                        summerRain[g] += share * rain * aloft(band * rainout, rise, ground) * mmPerUnit * here;
                    }
                }
            }
        });
        blurNorthSouth(anomaly);
        blurNorthSouth(amplitude);
        massElevation(planet);
        for (int g = 0; g < w * h; g++)
            temperature[g] = seaLevelTemperature(latitude(g / w, h)) + anomaly[g] - LAPSE_RATE * Math.max(Planet.metres(planet.height[g] - Planet.SEA_LEVEL), 0);
        monsoon(planet);
        summerFlow(planet, null, SUMMER_FLOW);
        summerFlow(planet, blocked, 1);
        // ponytail: winds are zonal plus the monsoon; a north-south blur stands in for the rest of meridional mixing
        // ponytail: cold eastern currents only chill coasts the westerlies reach; trade winds blow offshore there, so the
        // Atacama and Namib coasts are dry (from the continent behind them) but not cool
        blurNorthSouth(precipitation);
        blurNorthSouth(summerRain);
        for (int g = 0; g < w * h; g++) summerRain[g] = precipitation[g] > 0 ? Math.clamp(summerRain[g] / precipitation[g], 0, 1) : 0.5f;
    }

    /**
     * Lake-effect rain, from format 38: big lakes give off water too, and the same winds carry it to their far shores
     * and beyond (the snow belts east of the Great Lakes, the wet shores of Lake Victoria). Lakes are only known once
     * the rivers have run, so this is a second pass that only adds rain; it feeds the biomes, not the rivers.
     */
    public void lakeEffect(Planet planet, java.util.function.IntPredicate lake) {
        float[] extra = new float[w * h];
        for (int y = 0; y < h; y++) {
            double abs = Math.abs(latitude(y, h));
            float westerly = smoothstep(25, 35, abs) * (1 - smoothstep(55, 65, abs)), band = band(latitude(y, h));
            for (int dir : new int[]{1, -1}) {
                float share = dir > 0 ? westerly : 1 - westerly, moisture = 0, felt = 0;
                if (share == 0) continue;
                for (int i = 0; i < 2 * w; i++) {
                    int g = Math.floorMod(dir * i, w) + y * w;
                    float ground = Math.max(Planet.metres(planet.height[g] - Planet.SEA_LEVEL), 0);
                    if (lake.test(g)) moisture += (capacity(temperature[g]) - moisture) * evaporation * LAKE_EVAPORATION * warmShare(temperature[g], amplitude[g]);
                    if (planet.height[g] < Planet.SEA_LEVEL) moisture = 0; // over the sea the sea's own moisture already counts
                    float rise = (ground - felt) * smoothing;
                    felt += rise;
                    float rain = Math.min(moisture, moisture * (band * rainout + Math.max(0, rise) * OROGRAPHIC));
                    moisture -= rain;
                    if (i >= w) extra[g] += share * rain * aloft(band * rainout, rise, ground) * mmPerUnit;
                }
            }
        }
        blurNorthSouth(extra);
        for (int g = 0; g < w * h; g++) precipitation[g] += Math.min(extra[g], LAKE_RAIN_MAX); // ponytail: a cap where a big lake's air climbs a range at once
    }

    /** A rate per reference cell as the same rate over s reference cells. */
    private static float per(float rate, float s) {
        return 1 - (float) Math.pow(1 - rate, s);
    }

    /** Share of the year above freezing, when open water can evaporate. */
    private static float warmShare(float t, float a) {
        return 1 - frozenShare(t, a);
    }

    /**
     * How readily moist air rains at a latitude: air rises (wet) at the equator and near 60°, and sinks near 30° and at
     * the poles. Where the Hadley cell's air comes down, around 20-35°, it warms and dries as it sinks: the great desert
     * belt (Sahara, Arabia, Kalahari, Australia), deepened by SUBSIDENCE.
     */
    static float band(double lat) {
        return band(lat, 0);
    }

    /**
     * eastOcean, 0-1: how near a wide ocean lies to the east. The subtropical highs sit over the oceans and their air
     * sinks hardest on their eastern side, over the west coasts (Atacama, Namib, Sahara); round their western side warm
     * sea air streams onto the east coasts instead (southern Brazil, the Bay of Bengal into India, Natal, eastern
     * Australia), so the desert belt fades there.
     */
    static float band(double lat, float eastOcean) {
        double sink = (Math.abs(lat) - SUBSIDENCE_LAT) / SUBSIDENCE_WIDTH;
        float dry = (float) ((0.6 + 0.4 * Math.cos(Math.toRadians(lat * 6))) * (1 - SUBSIDENCE * Math.exp(-sink * sink)));
        return dry + (1 - dry) * EAST_RELIEF * eastOcean * smoothstep(EAST_FROM, EAST_FROM + 8, Math.abs(lat)) * (1 - smoothstep(40, 50, Math.abs(lat)));
    }

    /**
     * The height the trades must climb to get past the ground within BARRIER_AHEAD degrees to the west. Air goes round
     * a lone massif and through gaps, so this is the lowest of the ridge heights within GAP_REACH degrees north and
     * south: only a long wall with no pass (the Andes, the Himalaya) blocks it.
     */
    private float[] barrierAhead(Planet planet) {
        float[] top = new float[w * h], pass = new float[w * h];
        int ahead = Math.max(1, Math.round(BARRIER_AHEAD / 360 * w)), side = Math.max(1, Math.round(GAP_REACH / 180 * h)), rows = 2 * side + 1;
        // Window extremes by doubling: a[i] holds the extreme of the len entries from i, len doubling up to the window,
        // then two overlapping spans cover it. log2(window) passes instead of window compares per cell.
        java.util.stream.IntStream.range(0, h).parallel().forEach(y -> { // highest of the ahead cells to the west, wrapping
            float[] a = new float[w + ahead];
            for (int i = 0; i < w + ahead; i++) a[i] = Math.max(0, Planet.metres(planet.height[Math.floorMod(i - ahead, w) + y * w] - Planet.SEA_LEVEL));
            int len = 1;
            for (; 2 * len <= ahead; len *= 2)
                for (int i = 0; i + len < w + ahead; i++) a[i] = Math.max(a[i], a[i + len]);
            for (int x = 0; x < w; x++) top[x + y * w] = Math.max(a[x], a[x + ahead - len]); // cells x - ahead .. x - 1
        });
        float[] a = new float[(h + 2 * side) * w], b = new float[a.length]; // lowest over rows y - side .. y + side, clamped at the poles
        for (int r = 0; r < h + 2 * side; r++) System.arraycopy(top, Math.clamp(r - side, 0, h - 1) * w, a, r * w, w);
        int len = 1;
        for (; 2 * len <= rows; len *= 2) {
            float[] from = a, to = b;
            int span = len;
            java.util.stream.IntStream.range(0, h + 2 * side - span).parallel().forEach(r -> {
                for (int x = 0; x < w; x++) to[r * w + x] = Math.min(from[r * w + x], from[(r + span) * w + x]);
            });
            a = to;
            b = from;
        }
        float[] low = a;
        int span = len;
        java.util.stream.IntStream.range(0, h).parallel().forEach(y -> {
            for (int x = 0; x < w; x++) pass[x + y * w] = Math.min(low[y * w + x], low[(y + rows - span) * w + x]);
        });
        return pass;
    }

    /**
     * How fully a monsoon rules the tropics of each column, 0-1, dir 1 north and -1 south. A continent that covers most of
     * the latitudes poleward (25-55°, REGIME_WIDTH degrees either side) builds a winter high and a summer low of its own
     * that outweigh the ocean's subtropical high: the winds reverse with the seasons instead of the trades blowing all
     * year, and no warm current runs up the sea's western shore (the summer monsoon wells cold water up off Somalia and
     * Oman). On Earth only Asia is that big; the Americas, Africa and Australia keep their trades.
     */
    private float[] monsoonRegime(Planet planet, int dir) {
        float[] share = new float[w];
        int[] column = new int[w];
        int side = Math.round(REGIME_WIDTH / 360 * w), rows = 0;
        for (int y = 0; y < h; y++) {
            double lat = latitude(y, h) * dir;
            if (lat < 25 || lat > 55) continue;
            rows++;
            for (int x = 0; x < w; x++) if (planet.height[x + y * w] >= Planet.SEA_LEVEL) column[x]++;
        }
        int land = 0;
        for (int k = -side; k <= side; k++) land += column[Math.floorMod(k, w)];
        for (int x = 0; x < w; x++) { // a window sliding east
            share[x] = smoothstep(REGIME_LAND - 0.1f, REGIME_LAND + 0.1f, land / (double) Math.max(rows * (2 * side + 1), 1));
            land += column[Math.floorMod(x + side + 1, w)] - column[Math.floorMod(x - side, w)];
        }
        return share;
    }

    /**
     * The eastern flank of the subtropical highs, 0-1: the sea off a west coast and the coast behind it, at 15-40°. Round
     * the east side of a high over a wide ocean the surface wind blows toward the equator along the coast (the California,
     * Humboldt, Canary and Benguela winds), bringing air from cooler latitudes over upwelled water, so no summer stream
     * or monsoon carries sea moisture poleward there: Baja and Sonora, the Atacama, the Namib, Western Australia.
     */
    private float[] eastFlank(Planet planet) {
        float[] near = new float[w * h];
        float reach = FLANK_REACH / 360 * w, wide = FLANK_OCEAN / 360 * w;
        for (int y = 0; y < h; y++) {
            double abs = Math.abs(latitude(y, h));
            float belt = smoothstep(12, 18, abs) * (1 - smoothstep(36, 42, abs));
            if (belt == 0) continue;
            boolean[] coast = new boolean[w]; // land with a wide ocean straight to its west
            float run = 0;
            for (int i = 0; i < 2 * w; i++) { // walking east, two laps so the seam sees the ocean before it
                int x = i % w;
                boolean sea = planet.height[x + y * w] < Planet.SEA_LEVEL;
                if (!sea && i >= w) coast[x] = run >= wide;
                run = sea ? run + 1 : 0;
            }
            float land = w, sea = w;
            for (int i = 0; i < 2 * w; i++) { // inland from the coast, walking east
                int x = i % w;
                float metres = Planet.metres(planet.height[x + y * w] - Planet.SEA_LEVEL);
                land = coast[x] ? 0 : metres >= 0 && metres < MARINE_LAYER ? land + 1 : w; // the coast wind is shallow: ranges above it fence it in
                if (i >= w && planet.height[x + y * w] >= Planet.SEA_LEVEL) near[x + y * w] = belt * (float) Math.exp(-land / reach);
            }
            for (int i = 0; i < 2 * w; i++) { // out to sea from the coast, walking west
                int x = Math.floorMod(-i, w);
                sea = coast[x] ? 0 : planet.height[x + y * w] < Planet.SEA_LEVEL ? sea + 1 : w;
                if (i >= w && planet.height[x + y * w] < Planet.SEA_LEVEL) near[x + y * w] = belt * (float) Math.exp(-sea / reach);
            }
        }
        return near;
    }

    /**
     * The mass elevation effect: the sunlit ground of a broad high plateau heats the air over it, so it is far warmer
     * than free air at its height (Lhasa, 3650 m, averages 8 °C; Tibet runs 4-11 °C warm). A lone peak has no such
     * ground round it and stays cold. Adds MASS_ELEVATION of the lapse cooling back, by the ground's height averaged
     * over MASS_REACH degrees, and never more than the cell's own height.
     */
    private void massElevation(Planet planet) {
        float[] ground = new float[w * h], across = new float[w * h];
        for (int g = 0; g < w * h; g++) ground[g] = Math.max(Planet.metres(planet.height[g] - Planet.SEA_LEVEL), 0);
        int rx = Math.max(1, Math.round(MASS_REACH / 360 * w)), ry = Math.max(1, Math.round(MASS_REACH / 180 * h));
        for (int y = 0; y < h; y++) { // running sums along each row, wrapping
            float sum = 0;
            for (int k = -rx; k <= rx; k++) sum += ground[Math.floorMod(k, w) + y * w];
            for (int x = 0; x < w; x++) {
                across[x + y * w] = sum / (2 * rx + 1);
                sum += ground[Math.floorMod(x + rx + 1, w) + y * w] - ground[Math.floorMod(x - rx, w) + y * w];
            }
        }
        for (int x = 0; x < w; x++) { // then down each column, clamped at the poles
            float sum = 0;
            int n = 0;
            for (int y = 0; y <= Math.min(ry, h - 1); y++, n++) sum += across[x + y * w];
            for (int y = 0; y < h; y++) {
                int g = x + y * w;
                anomaly[g] += MASS_ELEVATION * LAPSE_RATE * Math.min(sum / n, ground[g]);
                if (y + ry + 1 < h) { sum += across[x + (y + ry + 1) * w]; n++; }
                if (y - ry >= 0) { sum -= across[x + (y - ry) * w]; n--; }
            }
        }
    }

    /** Along row y, 1 on a wide ocean (WIDE_OCEAN degrees of open sea), falling off over EAST_REACH degrees westward from it. */
    private float[] eastOcean(Planet planet, int y) {
        float[] near = new float[w];
        float wide = WIDE_OCEAN / 360 * w, reach = EAST_REACH / 360 * w, run = 0, d = w;
        for (int i = 0; i < 2 * w; i++) { // walking west; two laps, so the wrap seam sees the ocean beyond it
            int x = Math.floorMod(-i, w);
            run = planet.height[x + y * w] < Planet.SEA_LEVEL ? run + 1 : 0;
            d = run >= wide ? 0 : Math.min(d + 1, w); // a narrow sea (the Red Sea, the Gulf of California) is no ocean
            if (i >= w) near[x] = (float) Math.exp(-d / reach);
        }
        return near;
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
    }

    /** Box blurs along each column (rows clamped at the poles), running sums, so the cost doesn't grow with reach. */
    private void blurNorthSouth(float[] f) {
        float[] src = new float[w * h], sum = new float[w];
        for (int pass = 0; pass < passes; pass++) { // a running sum per column, moved down a row at a time: memory in order
            System.arraycopy(f, 0, src, 0, w * h);
            java.util.Arrays.fill(sum, 0);
            for (int k = -reach; k <= reach; k++) addRow(sum, src, Math.clamp(k, 0, h - 1), 1);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) f[x + y * w] = sum[x] / (2 * reach + 1);
                addRow(sum, src, Math.min(y + reach + 1, h - 1), 1);
                addRow(sum, src, Math.max(y - reach, 0), -1);
            }
        }
    }

    private void addRow(float[] sum, float[] f, int y, int sign) {
        for (int x = 0; x < w; x++) sum[x] += sign * f[x + y * w];
    }

    /** Box blurs along each row, wrapping, running sums. */
    private void blurEastWest(float[] f) {
        float[] row = new float[w];
        for (int pass = 0; pass < passes; pass++)
            for (int y = 0; y < h; y++) {
                System.arraycopy(f, y * w, row, 0, w);
                float sum = 0;
                for (int k = -reach; k <= reach; k++) sum += row[Math.floorMod(k, w)];
                for (int x = 0; x < w; x++) {
                    f[x + y * w] = sum / (2 * reach + 1);
                    sum += row[Math.floorMod(x + reach + 1, w)] - row[Math.floorMod(x - reach, w)];
                }
            }
    }

    /**
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
            float eastern = DRIFT_ANOMALY * smoothstep(42, 50, abs) * (1 - smoothstep(70, 80, abs)) - CURRENT_ANOMALY * (1 - smoothstep(30, 40, abs));
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
                sst[x + y * w] = western * (float) Math.exp(-fromWest[x] / boundaryCurrent) + eastern * (float) Math.exp(-fromEast[x] / easternCurrent);
        }
        return sst;
    }

    /**
     * Summer monsoon: land heats faster than sea, so in summer moist tropical sea air is drawn poleward onto the land.
     * Swept along each column from the equator to MONSOON_REACH degrees, raining as it goes, hardest against mountains.
     */
    /**
     * Summer air pushed poleward round the western side of the subtropical highs: warm, wet sea air carried onto the
     * east coasts of continents at 25-45 degrees (the Gulf of Mexico into the eastern US, the South China Sea into China,
     * the Coral Sea onto eastern Australia). Each column is swept poleward from 15 degrees; the air takes up water only
     * over warm sea (a warm current), so coasts off cold currents (the Sahara's, California's) stay dry. Most of it falls
     * in summer.
     */
    private void summerFlow(Planet planet, float[] turned, float strength) {
        float[] wet = new float[w * h];
        for (int x = 0; x < w; x++)
            for (int dir : new int[]{-1, 1}) { // north, and south
                float moisture = 0, felt = 0;
                for (int y = h / 2 + (dir < 0 ? -1 : 0); y >= 0 && y < h; y += dir) {
                    double abs = Math.abs(latitude(y, h));
                    if (turned == null && abs < FLOW_FROM) continue;
                    if (abs > FLOW_TO) break;
                    int g = x + y * w;
                    float m = Planet.metres(planet.height[g] - Planet.SEA_LEVEL), ground = Math.max(m, 0);
                    if (turned != null) moisture += turned[g]; // trade air a barrier turned aside joins the stream where it turned
                    else if (m < 0) moisture += (capacity(temperature[g]) - moisture) * evaporation * smoothstep(-1, 2, seaAnomaly[g]) * (1 - regime[dir < 0 ? 0 : 1][x]) * (1 - flank[g]); // no high to flow round where the monsoon rules
                    moisture -= moisture * evaporation * flank[g]; // off a west coast the wind blows toward the equator: no stream poleward
                    float rise = (ground - felt) * smoothing;
                    felt += rise;
                    float rain = Math.min(moisture, moisture * (rainout + Math.max(0, rise) * OROGRAPHIC) / (1 + Math.max(0, -rise) * OROGRAPHIC * LEE_DRYING));
                    moisture -= rain;
                    if (m >= 0) moisture += Math.min(rain * RECYCLING, RECYCLING_MAX_MM / mmPerUnit);
                    if (m >= 0) moisture *= 1 - flowMixing; // over land the stream spreads and mixes away: it waters coasts, not whole continents
                    float season = strength * (1 - smoothstep((float) FLOW_TO - 8, (float) FLOW_TO, abs));
                    if (m >= 0) wet[g] = season * rain * aloft(rainout, rise, ground) * mmPerUnit;
                }
            }
        blurEastWest(wet); // each column was swept alone; the flow is broad
        for (int g = 0; g < w * h; g++)
            if (planet.height[g] >= Planet.SEA_LEVEL) {
                precipitation[g] += wet[g];
                summerRain[g] += wet[g] * MONSOON_SUMMER;
            }
    }

    /**
     * Degrees poleward the summer monsoon reaches in each column, dir -1 north, 1 south. A high plateau in the
     * subtropics heats in summer and draws the monsoon far inland (Tibet: India to 30°N), and its heat low pulls over
     * ~2000 km either side, so the plateau's height is averaged over PLATEAU_SPREAD degrees of longitude.
     */
    private float[] plateauReach(Planet planet, int dir) {
        float[] high = new float[w], reach = new float[w];
        for (int x = 0; x < w; x++) {
            int rows = 0;
            for (int y = 0; y < h; y++) {
                double lat = latitude(y, h) * -dir;
                if (lat >= PLATEAU_FROM && lat <= PLATEAU_TO) { high[x] += Math.max(0, Planet.metres(planet.height[x + y * w] - Planet.SEA_LEVEL)); rows++; }
            }
            high[x] /= Math.max(rows, 1);
        }
        int spread = Math.round(PLATEAU_SPREAD / 360 * w);
        for (int x = 0; x < w; x++) {
            float sum = 0;
            for (int k = -spread; k <= spread; k++) sum += high[Math.floorMod(x + k, w)];
            reach[x] = (float) MONSOON_REACH + PLATEAU_REACH * smoothstep(PLATEAU_LOW, PLATEAU_HIGH, sum / (2 * spread + 1));
        }
        return reach;
    }

    private void monsoon(Planet planet) {
        float[] wet = new float[w * h];
        float[][] reaches = {plateauReach(planet, -1), plateauReach(planet, 1)};
        for (int x = 0; x < w; x++)
            for (int dir : new int[]{-1, 1}) { // north from the equator, and south
                float moisture = 0, felt = 0, reach = reaches[dir < 0 ? 0 : 1][x];
                for (int y = h / 2 + (dir < 0 ? -1 : 0); y >= 0 && y < h; y += dir) {
                    double abs = Math.abs(latitude(y, h));
                    if (abs > reach) break;
                    int g = x + y * w;
                    float m = Planet.metres(planet.height[g] - Planet.SEA_LEVEL), ground = Math.max(m, 0);
                    // The sea evaporates freely; land as far as its water allows: a rainforest gives off as much as a
                    // tropical sea, so the monsoon also draws on the Congo and the Amazon, not only on the coast.
                    float supply = m < 0 ? 1 : Math.min(1, precipitation[g] / POTENTIAL_ET);
                    moisture += (capacity(temperature[g]) - moisture) * evaporation * supply * (1 - flank[g]);
                    moisture -= moisture * evaporation * flank[g]; // off a west coast the wind blows toward the equator: no monsoon up it
                    float rise = (ground - felt) * smoothing;
                    felt += rise;
                    float rain = Math.min(moisture, moisture * (rainout + Math.max(0, rise) * OROGRAPHIC));
                    moisture -= rain;
                    if (m >= 0) moisture += Math.min(Math.min(rain, (moisture + rain) * rainout) * RECYCLING, RECYCLING_MAX_MM / mmPerUnit); // and its own summer rain, as under the other winds
                    // Strongest at 10° to MONSOON_REACH less 10: nearer the equator it's rainy all year anyway, farther out the heat low fades.
                    float season = MONSOON * smoothstep(3, 12, abs) * (1 - smoothstep(reach - 10, reach, abs));
                    if (m >= 0) wet[g] = season * rain * aloft(rainout, rise, ground) * mmPerUnit;
                }
            }
        blurEastWest(wet); // each column was swept alone; the monsoon front is broad
        for (int g = 0; g < w * h; g++)
            if (planet.height[g] >= Planet.SEA_LEVEL) {
                precipitation[g] += wet[g];
                summerRain[g] += wet[g] * MONSOON_SUMMER;
            }
    }

    /**
     * Degrees, +90 at the top row (north pole), -90 at the bottom. Rows are equal-area (Lambert):
     * every row is the same width, so polar rows cover few degrees and the ice caps keep Earth's share of land.
     */
    public static double latitude(int y, int h) {
        return latitudeAt(y + 0.5, h);
    }

    /** Latitude at a fractional row position: 0 is the north edge of the map, h the south edge. */
    public static double latitudeAt(double row, int h) {
        return Math.toDegrees(Math.asin(Math.clamp(1 - 2 * row / h, -1, 1)));
    }

    /** Annual mean °C at sea level: 27 at the equator, -25 at the poles. */
    static float seaLevelTemperature(double latitude) {
        return (float) (-25 + 52 * Math.cos(Math.toRadians(latitude)));
    }

    private static float smoothstep(float a, float b, double x) {
        float t = (float) Math.clamp((x - a) / (b - a), 0, 1);
        return t * t * (3 - 2 * t);
    }

    /**
     * Of the rain air wrings out over ground this high, the share that falls here. A climb dries the air as before (so rain
     * shadows stay), but the vapour lies low, thinning with height, so most of what a high climb condenses came from low in
     * the column and fell on the slopes below: rain peaks on the lower slopes and the highest summits stay dry (Smith and
     * Barstad). plain is the rain rate without the climb.
     * ponytail: what falls upslope is dropped, not added to the cells below; their own climb already rains on them
     */
    static float aloft(float plain, float rise, float ground) {
        float lifted = Math.max(0, rise) * OROGRAPHIC, share = lifted / Math.max(1e-12f, plain + lifted);
        return 1 - share * (1 - (float) Math.exp(-ground / VAPOUR_HEIGHT));
    }

    /** Warm air holds more water, 1 at 25 °C. Gentler than Clausius-Clapeyron's 7%/°C, as real rainfall is. */
    static float capacity(float t) {
        return (float) Math.exp(0.045 * (t - 25));
    }
}
