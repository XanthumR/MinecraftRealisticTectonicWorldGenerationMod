package com.realtectonics.sim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.function.DoubleConsumer;
import java.util.function.IntPredicate;

/**
 * Rivers, lakes and fluvial erosion on the finished tectonic height map.
 * Stream power law solved implicitly (Braun & Willett 2013) on a depression-filled
 * surface (Priority-Flood, Barnes et al. 2014), so every drop of rain reaches the sea.
 */
public final class Hydrology {
    static final int[] DX = {-1, 0, 1, -1, 1, -1, 0, 1}, DY = {-1, -1, -1, 0, 0, 1, 1, 1};
    static final float ERODIBILITY = 0.01f;  // K in the stream power law
    static final float AREA_EXPONENT = 0.5f; // m; slope exponent n = 1 keeps the implicit solve exact
    static final float CREEP = 0.03f;         // hillslope diffusion per pass: soil creeping downhill keeps valleys apart
    static final float CHANNEL_AREA = 50;     // flow above which water, not creep, shapes the ground: valley floors stay cut
    public static final float RIVER_AREA = 200; // flow before a stream counts as a river
    static final float MIN_EVAPORATION = 0.1f;  // metres per year off a lake surface near freezing (Siberia, Canada)
    static final float EVAPORATION_PER_DEGREE = 0.07f; // more per °C above that: ~2 m/yr on a hot desert lake
    static final float ROUTE_WANDER = 0.5f;     // how much chance bends water off the steepest way down: streams merge
    static final float COAST_RAMP = 6;          // cells over which land climbs from the shore to full height
    static final float RIFT_SINK = 0.7f;        // share of its height a rift floor drops between the faults (East Africa: 1500 m to ~500)
    static final float RIFT_SHOULDER = 300, RIFT_KEEL = 5; // metres the shoulders rise; the floor stays at least this far above the sea
    static final float FAULT_GOUGE = 8, FAULT_FULL_SLIP = 0.4f; // times faster crushed rock along a strike-slip fault erodes; slip (cells/step) for all of it
    static final float VOLCANO_YOUTH = 0.33f;   // share of the erosion the volcanoes stand through: they are young beside the land
    static final float CALDERA_SINK = 0.4f;     // of a collapsing volcano's height its summit sinks by, at most Volcano.CALDERA_DEPTH
    static final float CONE_BLEND = 400;        // metres over which two overlapping volcanoes' slopes round into each other
    // Continental shelves: the drowned rim of a continent, shallow out to the shelf break, then the slope to the deep floor.
    static final float SHELF_MIN = 1, SHELF_MAX = 6; // cells wide on quiet (passive) coasts, by broad noise
    static final float ACTIVE_SHELF = 0.5f;     // cells: next to a trench the floor drops away almost at once (Chile, Japan)
    static final int ACTIVE_REACH = 4;          // cells from an active trench that count as an active margin
    static final float SHELF_DEPTH = 60, SHELF_BREAK = 180; // metres at the shore and at the outer edge
    static final float SLOPE = 2000;            // metres per cell down the continental slope
    static final float EARTH_LAKE_CUT = 30;     // metres a real lake (Earth planet) is cut below the flat water the elevation shows
    static final float RIDGE_CREST = -2300, RIDGE_FLANK = 16; // metres at a ridge's axis; cells its flanks take to sink to the old floor
    static final float TRENCH_FLOOR = -7500, TRENCH_HALF = 2; // metres at a trench's axis; cells from the axis to its rims
    // Ice Age glaciers: they left the U-shaped valleys, and where they cut down to the sea, the fjords.
    static final float ICE_AGE_COOLING = 8;     // °C colder at the last glacial maximum, at high latitudes
    static final float ICE_ACCUMULATION = -2;   // °C, ice-age annual mean: snow outlasts summer and builds glaciers
    static final float TYPICAL_SWING = 6;       // °C half-range the yearly-mean thresholds were tuned for: a maritime mid-latitude
    static final float ICE_REACH = 3;           // °C: glacier tongues flow down into this much warmth before melting
    static final float ICE_MIN_RAIN = 250;      // mm: drier land stayed bare, as Siberia and interior Alaska did
    static final float GLACIAL_EROSION = 3;     // glaciers deepen valleys this many times faster than rivers
    static final float FJORD_METRES = 15;       // a glacial valley this close to sea level floods into a fjord
    static final float FJORD_RELIEF = 350;      // metres within 3 cells: only valleys among mountains are fjords; lowland ones stay rivers
    public static final float GLACIER_AREA = 20; // flow that gathers enough ice to carve its own trough
    public static final byte GLACIATED = 1, FJORD = 2;

    public final int w, h;
    public final float[] elevation; // relative to sea level
    public final float[] surface;   // water surface; above elevation where a lake sits
    public final int[] receiver;    // next cell downstream, -1 for sea and outlets
    public final float[] flow;      // rain from upstream, in cells x metres per year
    public final List<Volcano> volcanoes = new ArrayList<>(); // with their bases, as built into this landscape
    public final byte[] ice;        // 0, GLACIATED or FJORD: what the Ice Age glaciers did here
    private final boolean[] lake;
    private final float[] lakeLevel; // lake surface where lake is true: the spill level, or lower in a shrunken lake
    private final float[] sink;      // metres per year a lake evaporates, taken from the flow at its basin's outlet
    private final int[] order;      // outlets first, every cell after its receiver
    private final float[] rainMm;
    private final float[] warmth;
    private float[] swing;           // °C half the summer-winter range, or null: no seasons
    private long routeSeed;         // for the random weights in wander()
    private int count;
    private float erodibility = ERODIBILITY;
    private float[] weakness;        // per cell, times faster the rock erodes: crushed along faults; null for all alike

    public Hydrology(Planet planet, float[] rainMm, int iterations, long seed) {
        this(planet, rainMm, new float[planet.w * planet.h], iterations, seed, done -> {});
    }

    /**
     * warmth: °C the oceans and continentality add to each cell, as Climate.anomaly; it moves the Ice Age ice too.
     * progress: 0..1 through the erosion passes; interrupting the thread cancels between passes.
     */
    public Hydrology(Planet planet, float[] rainMm, float[] warmth, int iterations, long seed, DoubleConsumer progress) {
        this(planet, rainMm, warmth, null, iterations, seed, progress);
    }

    /**
     * swing: °C half the summer-winter range per cell, as Climate.amplitude; null for no seasons. Ice Age glaciers then
     * need cool summers, not just a cold year, and lakes evaporate by the warmth of their summers.
     */
    public Hydrology(Planet planet, float[] rainMm, float[] warmth, float[] swing, int iterations, long seed, DoubleConsumer progress) {
        this(planet.w, planet.h, rainMm, warmth);
        this.swing = swing;
        routeSeed = seed;
        int n = w * h;
        // Roughen the smooth tectonic domes so channels branch instead of running in parallel.
        float[] noise = Noise.fbm(w, h, planet.scaleWidth / 64, 4, new Random(seed)); // no 2-cell octave: it shows as an egg-crate grid in game
        for (int g = 0; g < n; g++) {
            float e = planet.height[g] - Planet.SEA_LEVEL;
            elevation[g] = e < 0 ? e : Math.max(0.001f, planet.earth ? e : e + (noise[g] - 0.875f) * (0.3f + 0.15f * e)); // fbm mean is ~0.875; mountains get rougher
        }
        if (!planet.earth) shape(planet, seed); // Earth's relief is real: its coasts, rifts, shelves, ridges and trenches are in it
        else drainEarth(planet.earthLakes);
        // Strike-slip faults grind their rock to gouge, which wears away fast: rivers find the fault and follow it, so
        // it shows as a long straight valley (the North Anatolian Fault, the San Andreas, the Great Glen).
        float[] slip = planet.strikeSlip();
        weakness = new float[n];
        for (int g = 0; g < n; g++) weakness[g] = Math.min(1, slip[g] / FAULT_FULL_SLIP);
        Geology.blur(weakness, w, h);
        for (int g = 0; g < n; g++) weakness[g] = 1 + FAULT_GOUGE * Math.min(1, 3 * weakness[g]); // the blur spreads a line over 3 cells
        erodeAndBuild(planet, iterations, progress);
    }

    /**
     * Earth's relief averaged into cells closes the gorges real rivers leave basins by (the Congo's, the Niger's), so
     * nearly every basin would hold a lake. Real ones drain: fill every hollow up to where it spills, so the water runs
     * on as it does. Real lakes are flat water in the elevation, not hollows: cut them EARTH_LAKE_CUT metres in, and
     * markLakes fills them back to where they spill, their own level.
     */
    private void drainEarth(boolean[] lakes) {
        for (int g = 0; g < w * h; g++)
            if (lakes[g] && elevation[g] > 0) elevation[g] = Planet.fromMetres(Math.max(1, Planet.metres(elevation[g]) - EARTH_LAKE_CUT));
        route();
        for (int g = 0; g < w * h; g++)
            if (!lakes[g] && elevation[g] >= 0) elevation[g] = Math.max(elevation[g], surface[g]);
    }

    /** The made-up planet's relief, shaped where plate crust alone would be wrong: coasts, rifts, shelves, ridges, trenches. */
    private void shape(Planet planet, long seed) {
        int n = w * h;
        // Continents rise gently from the shore; tectonic crust alone makes every coast a cliff.
        int[] shore = distanceFrom(g -> elevation[g] < 0);
        for (int g = 0; g < n; g++)
            if (elevation[g] > 0) elevation[g] = Math.max(0.001f, elevation[g] * Math.min(1, shore[g] / COAST_RAMP));
        // Rift valleys: where a continent is pulled apart its floor drops between faults and the shoulders rise.
        float[] floor = planet.rifts(), shoulders = floor.clone();
        blur(floor);
        for (int pass = 0; pass < 4; pass++) blur(shoulders);
        for (int g = 0; g < n; g++) {
            if (elevation[g] <= 0 || shoulders[g] == 0) continue;
            float down = Math.min(1, 3 * floor[g]), up = Math.max(0, Math.min(1, 4 * shoulders[g]) - down);
            float m = Planet.metres(elevation[g]);
            elevation[g] = Planet.fromMetres(Math.max(RIFT_KEEL, m * (1 - RIFT_SINK * down) + RIFT_SHOULDER * up));
        }
        int[] offshore = distanceFrom(g -> elevation[g] >= 0), trench = distanceFrom(g -> planet.subduction[g] > Planet.ARC_ACTIVITY);
        float[] breadth = Noise.fbm(w, h, planet.scaleWidth / 32, 4, new Random(seed * 13 + 5)), shelf = new float[n];
        for (int g = 0; g < n; g++) {
            float width = trench[g] <= ACTIVE_REACH ? ACTIVE_SHELF : SHELF_MIN + (SHELF_MAX - SHELF_MIN) * Math.clamp((breadth[g] - 0.5f) / 0.75f, 0, 1);
            float d = Math.max(offshore[g] - 0.5f, 0); // the shore lies half a cell out from the last land cell
            shelf[g] = d <= width ? -SHELF_DEPTH - (SHELF_BREAK - SHELF_DEPTH) * d / width : -SHELF_BREAK - SLOPE * (d - width);
        }
        for (int pass = 0; pass < 2; pass++) blur(shelf); // whole-cell distances would leave the slope in steps
        for (int g = 0; g < n; g++) // shelves only ever fill the deep floor in
            if (elevation[g] < 0) elevation[g] = Math.max(elevation[g], Planet.fromMetres(Math.min(shelf[g], -SHELF_DEPTH)));
        // Mid-ocean ridges and trenches. The crust's age sinks the floor evenly; a real ridge's flanks deepen with the
        // square root of their distance from the axis as the crust cools (Parsons & Sclater 1977), so it stands as a
        // rounded crest, and where a plate's age broke off (a ridge that jumped) it still slopes both ways. Where old
        // floor dives under a plate a narrow trench runs along the hinge, 3 km below the abyss (Peru-Chile, Mariana).
        float[] diving = planet.trenches();
        int[] axis = distanceFrom(g -> planet.spreading[g] > Planet.RIDGE_ACTIVITY), hinge = distanceFrom(g -> diving[g] > 0);
        float[] ridge = new float[n], deep = new float[n];
        for (int g = 0; g < n; g++) {
            ridge[g] = RIDGE_CREST + (Planet.metres(Planet.OCEAN - Planet.SEA_LEVEL) - RIDGE_CREST) * (float) Math.sqrt(Math.min(1, axis[g] / RIDGE_FLANK));
            deep[g] = Math.max(0, 1 - hinge[g] / TRENCH_HALF);
        }
        for (int pass = 0; pass < 2; pass++) { blur(ridge); blur(deep); }
        for (int g = 0; g < n; g++) {
            if (elevation[g] >= 0) continue; // ponytail: trenches cut the overriding side too, and no outer rise
            float m = Math.max(Planet.metres(elevation[g]), ridge[g]), k = Math.min(1, 1.5f * deep[g]); // the blur takes a third off the axis
            elevation[g] = Planet.fromMetres(Math.min(m, m + (TRENCH_FLOOR - m) * k));
        }
    }

    private void erodeAndBuild(Planet planet, int iterations, DoubleConsumer progress) {
        // Volcanoes are young beside the land they stand on: they go up for the last part of the erosion, so they come
        // out sharp, and still before the last rivers, so water runs off their flanks instead of under them.
        float[] beforeCones = null;
        int conesAt = (int) (iterations * (1 - VOLCANO_YOUTH));
        for (int i = 0; i < iterations; i++) {
            if (Thread.currentThread().isInterrupted()) throw new CancellationException();
            if (i == conesAt) beforeCones = buildCones(planet);
            route();
            markGlaciers();
            erode();
            creep();
            progress.accept((i + 1) / (double) (iterations + 1));
        }
        if (beforeCones == null) beforeCones = buildCones(planet); // no erosion at all
        collapseCalderas(beforeCones);
        route();
        markGlaciers();
        markFjords();
        markLakes();
        accumulate(); // lakes keep what they evaporate: below a lake with no outlet, the river is gone
    }

    /** Puts every volcano's cone into the land; returns the land as it was before them. */
    private float[] buildCones(Planet planet) {
        int n = w * h;
        // Every base is measured before any cone goes up: neighbours in a chain must not stack on each other.
        for (Volcano v : planet.volcanoes())
            if (!v.gone()) volcanoes.add(v.withBase(Planet.metres(elevation[(int) v.u() + (int) v.v() * w])));
        // Overlapping cones blend with a smooth maximum, so neighbours in a chain merge without a straight crease.
        float[] cones = new float[n];
        Arrays.fill(cones, Float.NaN);
        for (Volcano placed : volcanoes) {
            int cu = (int) placed.u(), cv = (int) placed.v();
            float radius = Volcano.LOBE_MAX * placed.radiusBlocks() / Terrain.BLOCKS_PER_CELL;
            for (int dy = -(int) radius; dy <= radius; dy++)
                for (int dx = -(int) radius; dx <= radius; dx++) {
                    double d = Math.hypot(dx, dy);
                    if (d >= radius || cv + dy < 0 || cv + dy >= h) continue;
                    int g = Math.floorMod(cu + dx, w) + (cv + dy) * w;
                    float s = placed.surfaceMetres(dx * Terrain.BLOCKS_PER_CELL, dy * Terrain.BLOCKS_PER_CELL, Volcano.SMOOTH_SCARS);
                    if (s <= placed.baseMetres()) continue; // past this side's footprint
                    cones[g] = Float.isNaN(cones[g]) ? s : smoothMax(cones[g], s, CONE_BLEND);
                }
        }
        float[] before = elevation.clone();
        for (int g = 0; g < n; g++)
            if (!Float.isNaN(cones[g])) elevation[g] = Math.max(elevation[g], Planet.fromMetres(cones[g]));
        return before;
    }

    /**
     * Calderas are young beside the volcanoes they cut: the cone went up and wore down with the land around it; now
     * the summit of each volcano that drained its magma chamber falls in. The summit as erosion left it sinks by up to
     * CALDERA_SINK of the volcano's height, a flat floor under walls steepening to the rim. Only the volcano's own
     * rock goes, never below the land it was built on, so a range beside it stands untouched. Lakes fill the basins
     * up to the lowest notch in their rims in markLakes, like any other lake.
     */
    private void collapseCalderas(float[] beforeCones) {
        for (Volcano v : volcanoes) {
            if (!v.caldera()) continue;
            int cu = (int) v.u(), cv = (int) v.v();
            float r = Volcano.CALDERA_SHARE * v.radiusBlocks() / Terrain.BLOCKS_PER_CELL;
            float summit = Planet.metres(elevation[cu + cv * w]);
            float sink = Math.min(Volcano.CALDERA_DEPTH, CALDERA_SINK * (summit - v.baseMetres()));
            if (sink <= 0) continue;
            for (int dy = -(int) r; dy <= r; dy++)
                for (int dx = -(int) r; dx <= r; dx++) {
                    double d = Math.hypot(dx, dy) / r;
                    if (d >= 1 || cv + dy < 0 || cv + dy >= h) continue;
                    int g = Math.floorMod(cu + dx, w) + (cv + dy) * w;
                    float t = (float) Math.clamp((d - 0.6) / 0.4, 0, 1), wall = t * t * (3 - 2 * t);
                    float sunk = Planet.fromMetres(Planet.metres(elevation[g]) - sink * (1 - wall * wall));
                    elevation[g] = Math.max(beforeCones[g], Math.min(elevation[g], sunk));
                }
        }
    }

    private Hydrology(int w, int h, float[] rainMm, float[] warmth) {
        this.w = w;
        this.h = h;
        this.rainMm = rainMm;
        this.warmth = warmth;
        int n = w * h;
        elevation = new float[n];
        surface = new float[n];
        receiver = new int[n];
        flow = new float[n];
        order = new int[n];
        lake = new boolean[n];
        lakeLevel = new float[n];
        sink = new float[n];
        ice = new byte[n];
    }

    /**
     * Rivers cutting the land while the plates still move (Cordonnier 2016): erosion passes on the bare crust, written
     * back into the plates, so valleys deepen as ranges rise and ride along with them. Uniform rain: the climate
     * only exists once the continents have settled. Basins are closed while they form: their water evaporates
     * instead of spilling, so no river saws through the rim and drains the lake before the world even exists.
     */
    static void erodeCrust(Planet planet, int passes, float erodibility) {
        float[] rain = new float[planet.w * planet.h];
        Arrays.fill(rain, 1000);
        Hydrology hy = new Hydrology(planet.w, planet.h, rain, new float[rain.length]);
        hy.erodibility = erodibility;
        for (int g = 0; g < rain.length; g++) hy.elevation[g] = planet.height[g] - Planet.SEA_LEVEL;
        for (int i = 0; i < passes; i++) {
            hy.route();
            for (int g = 0; g < rain.length; g++) hy.sink[g] = hy.inBasin(g) ? Float.MAX_VALUE : 0;
            hy.accumulate();
            hy.erode();
            hy.creep();
        }
        for (int g = 0; g < rain.length; g++)
            if (hy.elevation[g] >= 0) planet.setLandHeight(g, hy.elevation[g] + Planet.SEA_LEVEL);
    }

    /** Snow builds glaciers where the ice age was cold and wet enough; the ice then flows down the valleys until it melts. */
    private void markGlaciers() {
        Arrays.fill(ice, (byte) 0);
        for (int i = count - 1; i >= 0; i--) { // donors before receivers
            int c = order[i], r = receiver[c];
            if (elevation[c] < 0) continue;
            // Ice builds where summers are too cool to melt the winter's snow: a cold year alone is not enough, as the
            // Ice Age left dry, hot-summered Siberia bare. Without seasons a typical swing stands in.
            if (iceAgeSummer(c) < ICE_ACCUMULATION + TYPICAL_SWING && rainMm[c] >= ICE_MIN_RAIN) ice[c] = GLACIATED;
            if (ice[c] != 0 && r >= 0 && elevation[r] >= 0 && iceAgeSummer(r) < ICE_REACH + TYPICAL_SWING) ice[r] = GLACIATED;
        }
    }

    private float iceAgeSummer(int g) {
        return iceAgeTemperature(g) + (swing == null ? TYPICAL_SWING : swing[g]);
    }

    private float iceAgeTemperature(int g) {
        return Climate.seaLevelTemperature(Climate.latitude(g / w, h)) + warmth[g] - ICE_AGE_COOLING
                - Climate.LAPSE_RATE * Planet.metres(Math.max(elevation[g], 0));
    }

    /** Glacial valleys cut down to sea level, all the way from the sea: the sea flooded them when the ice melted. */
    private void markFjords() {
        for (int i = 0; i < count; i++) { // receivers before donors
            int c = order[i], r = receiver[c];
            if (ice[c] == GLACIATED && r >= 0 && Planet.metres(elevation[c]) < FJORD_METRES && (elevation[r] < 0 || ice[r] == FJORD)
                    && Planet.metres(highestNear(c, 3)) >= FJORD_RELIEF) ice[c] = FJORD;
        }
    }

    private float highestNear(int g, int radius) {
        float top = elevation[g];
        for (int dy = -radius; dy <= radius; dy++)
            for (int dx = -radius; dx <= radius; dx++) {
                int y = g / w + dy;
                if (y >= 0 && y < h) top = Math.max(top, elevation[Math.floorMod(g % w + dx, w) + y * w]);
            }
        return top;
    }

    public boolean isLake(int g) {
        return lake[g];
    }

    /** Lake surface, relative to sea level; only meaningful where isLake. */
    public float lakeLevel(int g) {
        return lakeLevel[g];
    }

    /** Metres per year a lake loses at this cell, by today's annual mean temperature. */
    private float evaporation(int g) {
        float t = Climate.seaLevelTemperature(Climate.latitude(g / w, h)) + warmth[g] - Climate.LAPSE_RATE * Planet.metres(Math.max(elevation[g], 0));
        return MIN_EVAPORATION + EVAPORATION_PER_DEGREE * Climate.warmMean(t, swing == null ? 0 : swing[g]); // summer heat dries lakes
    }

    private boolean inBasin(int g) {
        return surface[g] > elevation[g] + 1e-3f;
    }

    /**
     * A basin's lake grows until evaporation off its surface uses up the water flowing in. If inflow keeps up with
     * the whole basin, it fills to the brim and spills; otherwise it shrinks into the deepest part and has no outlet
     * (Great Salt Lake, the Caspian). Too little water at all and the basin is a dry salt flat.
     */
    // ponytail: evaporation by temperature alone; dry air evaporates more, add aridity if desert lakes look too big
    private void markLakes() {
        boolean[] seen = new boolean[w * h];
        int[] basin = new int[w * h];
        for (int start = 0; start < w * h; start++) {
            if (seen[start] || !inBasin(start)) continue;
            int size = 0;
            float inflow = 0; // the basin's lowest-draining cell gathers everything that enters it
            int outlet = start;
            basin[size++] = start;
            seen[start] = true;
            for (int i = 0; i < size; i++) {
                int c = basin[i], cx = c % w, cy = c / w;
                if (flow[c] > inflow) { inflow = flow[c]; outlet = c; }
                for (int k = 0; k < 8; k++) {
                    int ny = cy + DY[k];
                    if (ny < 0 || ny >= h) continue;
                    int nb = Math.floorMod(cx + DX[k], w) + ny * w;
                    if (!seen[nb] && inBasin(nb)) { seen[nb] = true; basin[size++] = nb; }
                }
            }
            // Fill from the deepest cell up while the inflow still covers the evaporation.
            Integer[] byDepth = new Integer[size];
            for (int i = 0; i < size; i++) byDepth[i] = basin[i];
            java.util.Arrays.sort(byDepth, (a, b) -> Float.compare(elevation[a], elevation[b]));
            float lost = 0;
            int filled = 0;
            while (filled < size && lost + evaporation(byDepth[filled]) <= inflow) lost += evaporation(byDepth[filled++]);
            // A full basin sits at its spill level; a shrunken lake at the rim of the next cell up, which encloses it.
            for (int i = 0; i < filled; i++) {
                lake[byDepth[i]] = true;
                lakeLevel[byDepth[i]] = filled == size ? surface[byDepth[i]] : elevation[byDepth[filled]];
            }
            sink[outlet] = lost;
        }
    }

    public boolean isRiver(int g) {
        return elevation[g] >= 0 && flow[g] > RIVER_AREA;
    }

    // ponytail: land on the map's top and bottom rows drains off the edge; fine while poles are ice caps
    private boolean outlet(int g) {
        return elevation[g] < 0 || g < w || g >= w * (h - 1);
    }

    /** Priority-Flood: fill depressions into lakes, pick each cell's steepest receiver, accumulate flow. */
    private void route() {
        boolean[] seen = new boolean[w * h];
        LongHeap queue = new LongHeap(w * h);
        for (int g = 0; g < w * h; g++) {
            if (!outlet(g)) continue;
            seen[g] = true;
            surface[g] = elevation[g];
            receiver[g] = -1;
            if (elevation[g] >= 0 || touchesLand(g)) queue.add(key(g)); // open sea never needs visiting
        }
        count = 0;
        while (!queue.isEmpty()) {
            int c = (int) queue.poll(), cx = c % w, cy = c / w;
            order[count++] = c;
            boolean pickReceiver = !outlet(c);
            if (pickReceiver) receiver[c] = -1;
            float steepest = 0;
            for (int k = 0; k < 8; k++) {
                int ny = cy + DY[k];
                if (ny < 0 || ny >= h) continue;
                int nb = Math.floorMod(cx + DX[k], w) + ny * w;
                if (!seen[nb]) { // unvisited neighbours are uphill or in a hollow behind c: flood them
                    seen[nb] = true;
                    surface[nb] = Math.max(elevation[nb], Math.nextUp(surface[c]));
                    queue.add(key(nb));
                } else if (pickReceiver) { // visited neighbours are final; strictly lower ones can take c's water
                    // Rho8: steepness weighted by a fixed random factor per cell and direction. Pure steepest descent
                    // sends every cell of a plain slope the same way, in parallel streams that never meet.
                    float slope = (surface[c] - surface[nb]) / (DX[k] != 0 && DY[k] != 0 ? 1.4142135f : 1) * wander(c, k);
                    if (slope > steepest) { steepest = slope; receiver[c] = nb; }
                }
            }
        }
        accumulate();
    }

    /** Rain gathered downstream, minus what each lake evaporates where its basin drains. */
    private void accumulate() {
        Arrays.fill(flow, 0);
        for (int i = count - 1; i >= 0; i--) {
            int c = order[i];
            if (elevation[c] >= 0) flow[c] += rainMm[c] / 1000;
            flow[c] = Math.max(0, flow[c] - sink[c]);
            if (receiver[c] >= 0) flow[receiver[c]] += flow[c];
        }
    }

    /**
     * Soil creep: land relaxes toward its neighbours. Without it every cell grows its own channel and the land
     * corrugates at the grid's own spacing; with it, small ridges round off and only real valleys survive.
     */
    private void creep() {
        float[] before = elevation.clone();
        for (int g = 0; g < before.length; g++) {
            if (before[g] <= 0 || flow[g] > CHANNEL_AREA) continue; // river beds keep their cut
            int x = g % w, y = g / w;
            float sum = 0;
            for (int k = 0; k < 4; k++) {
                int n = k == 0 ? Math.floorMod(x - 1, w) + y * w : k == 1 ? (x + 1) % w + y * w : k == 2 ? (y > 0 ? g - w : g) : y < h - 1 ? g + w : g;
                sum += Math.max(before[n], 0) - before[g]; // the sea pulls a coast down only to sea level
            }
            elevation[g] = Math.max(0.001f, before[g] + CREEP * sum);
        }
    }

    /** Implicit stream power erosion, receivers before donors. Inside lakes it averages up: sediment infill. */
    private void erode() {
        for (int i = 0; i < count; i++) {
            int c = order[i], r = receiver[c];
            if (r < 0) continue;
            float dist = c % w == r % w || c / w == r / w ? 1 : 1.4142135f;
            float f = erodibility * (ice[c] != 0 ? GLACIAL_EROSION : 1) * (weakness == null ? 1 : weakness[c]) * (float) Math.pow(flow[c], AREA_EXPONENT) / dist;
            float base = Math.max(elevation[r], 0); // rivers cut down to sea level, not below
            elevation[c] = (elevation[c] + f * base) / (1 + f);
        }
    }

    /** Cells (8-connected steps) to the nearest cell matching source. */
    private int[] distanceFrom(IntPredicate source) {
        int[] dist = new int[w * h], queue = new int[w * h];
        Arrays.fill(dist, Integer.MAX_VALUE);
        int n = 0;
        for (int g = 0; g < w * h; g++) if (source.test(g)) { dist[g] = 0; queue[n++] = g; }
        for (int i = 0; i < n; i++) {
            int c = queue[i], cx = c % w, cy = c / w;
            for (int k = 0; k < 8; k++) {
                int ny = cy + DY[k];
                if (ny < 0 || ny >= h) continue;
                int nb = Math.floorMod(cx + DX[k], w) + ny * w;
                if (dist[nb] == Integer.MAX_VALUE) { dist[nb] = dist[c] + 1; queue[n++] = nb; }
            }
        }
        return dist;
    }

    /** Polynomial smooth maximum: equals max(a, b) once they differ by more than k, rounds the crease where they meet. */
    static float smoothMax(float a, float b, float k) {
        float t = Math.clamp(0.5f + 0.5f * (b - a) / k, 0, 1);
        return a + (b - a) * t + k * t * (1 - t);
    }

    private void blur(float[] f) {
        float[] src = f.clone();
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                float sum = 0;
                for (int dy = -1; dy <= 1; dy++)
                    for (int dx = -1; dx <= 1; dx++) sum += src[Math.floorMod(x + dx, w) + Math.clamp(y + dy, 0, h - 1) * w];
                f[x + y * w] = sum / 9;
            }
    }

    private boolean touchesLand(int g) {
        int x = g % w, y = g / w;
        for (int k = 0; k < 8; k++) {
            int ny = y + DY[k];
            if (ny >= 0 && ny < h && elevation[Math.floorMod(x + DX[k], w) + ny * w] >= 0) return true;
        }
        return false;
    }

    /** Min-heap of primitive longs: PriorityQueue<Long> boxed every cell of every routing pass. */
    private static final class LongHeap {
        private long[] a;
        private int size;

        LongHeap(int capacity) {
            a = new long[capacity];
        }

        boolean isEmpty() {
            return size == 0;
        }

        void add(long v) {
            if (size == a.length) a = Arrays.copyOf(a, size * 2);
            int i = size++;
            while (i > 0 && a[(i - 1) / 2] > v) {
                a[i] = a[(i - 1) / 2];
                i = (i - 1) / 2;
            }
            a[i] = v;
        }

        long poll() {
            long top = a[0], last = a[--size];
            int i = 0;
            while (2 * i + 1 < size) {
                int c = 2 * i + 1;
                if (c + 1 < size && a[c + 1] < a[c]) c++;
                if (a[c] >= last) break;
                a[i] = a[c];
                i = c;
            }
            a[i] = last;
            return top;
        }
    }

    /** A fixed factor in [1 - ROUTE_WANDER, 1 + ROUTE_WANDER] per cell and direction. */
    private float wander(int cell, int direction) {
        long hsh = (cell * 8L + direction) * 0x9E3779B97F4A7C15L ^ routeSeed;
        hsh = (hsh ^ (hsh >>> 31)) * 0xBF58476D1CE4E5B9L;
        return 1 - ROUTE_WANDER + 2 * ROUTE_WANDER * ((hsh >>> 40) & 0xFFFF) / 65536f;
    }

    /** Orders by surface height (float bits made sortable for negatives), cell index in the low bits. */
    private long key(int g) {
        int b = Float.floatToIntBits(surface[g]);
        return (long) (b ^ ((b >> 31) & 0x7fffffff)) << 32 | g;
    }
}
