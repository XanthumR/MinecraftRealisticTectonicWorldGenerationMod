package com.realtectonics.sim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.stream.IntStream;

/**
 * Plate tectonics on a cylinder: X wraps (east-west), Y is bounded (poles).
 * Each cell holds crust thickness; thickness above SEA_LEVEL is land.
 * Pure Java, no Minecraft classes, so the mod can reuse it unchanged.
 */
public final class Planet {
    public static final float OCEAN = 1.0f;       // old, cold oceanic crust
    public static final float RIDGE = 1.8f;       // fresh crust at spreading ridges
    public static final float CONTINENT = 4.0f;
    public static final float SEA_LEVEL = 2.5f;
    public static final float MAX_CRUST = 12f;     // thicker crust collapses under its own weight: plateaus, not spikes
    static final float OCEAN_AGING = 0.004f;      // ridges sink as they move away
    static final float SUBDUCTION_UPLIFT = 0.15f; // volcanic arcs (Andes, Japan)
    static final float COLLISION_UPLIFT = 1.0f;   // continents never sink: all mass folds up (Himalaya)
    static final float DIFFUSION = 0.04f;         // hillslope creep; rivers do the rest, interleaved with the plates
    static final float COLLISION_DRAG = 0.002f;   // per overlapping continent cell: a 50-cell front stops a plate in ~30 steps
    static final float OLD_OCEAN = OCEAN + 0.2f;  // ocean floor this cold and dense sinks at trenches
    static final float SLAB_PULL = 0.00025f;      // per trench cell; terminal speed = pull * trench length / drag
    static final float MANTLE_DRAG = 0.05f;       // without trenches pulling, a plate stops in ~60 steps
    static final float MAX_SPEED = 0.9f;          // step() assumes plates shift at most one cell per step
    static final float MIN_PLATE_FRACTION = 1 / 400f; // smaller plates weld onto a neighbour
    static final float RIFT_SPEED = 0.3f;         // how hard the two halves of a rifted plate push apart
    static final float SUBDUCTION_MEMORY = 0.97f; // trench activity fades over ~30 steps: only live arcs erupt
    static final float ARC_ACTIVITY = 5;          // recent subduction needed for an arc behind it
    public static final float RIDGE_ACTIVITY = 2; // recent spreading that makes a cell a ridge axis (a gap opens every 1/speed steps)
    static final int ARC_MIN = 3, ARC_MAX = 6;    // cells from trench to volcanic arc (~150-250 km, as on Earth)
    static final int ARC_SPACING = 10;            // cells between arc volcanoes; Earth's are closer, but this is a game
    static final int HOTSPOT_PERIOD = 20;         // steps between eruptions of a hotspot
    static final float RIFT_SPREAD = 0.1f;        // cells per step two plates must separate by to tear a continent
    static final int RIFT_VOLCANO_SPACING = 40;   // cells between rift volcanoes: one or two per rift, or their cones fill it
    static final float COLLISION_MEMORY = 0.998f; // folding is all but permanent: old belts like the Appalachians stay deformed
    static final int FAULT_WINDOW = 3;            // cells around a boundary cell that give the boundary its direction
    static final int FAULT_REACH = 4;             // cells either side of a strike-slip boundary its distance field reaches; past it, the field stops signing sides
    static final float FAULT_MIN_SLIP = 0.05f;    // cells per step of sliding that make a boundary a fault worth showing
    // The supercontinent cycle (Wilson 1966; Nance et al. 1988): one continent insulates the mantle beneath it, the
    // heat lifts and tears it apart, the pieces drift off, and in time the oceans between them close again.
    static final float MANTLE_FLOW = 0.06f;       // per step, on a plate that is all continent: terminal ~1.2 cells/step against MANTLE_DRAG, capped at MAX_SPEED
    static final float FLOW_NORTH_SOUTH = 0.5f;   // the flow's north-south share: crust carried across a pole is lost
    static final int DISPERSE_STEPS = 100;        // steps the pieces of a supercontinent drift apart before the oceans start closing
    static final int FINAL_DRIFT = 150;           // the last breakup this many steps before the end, so the planet is left with drifting continents
    static final float GATHERED = 1.2f;           // continents this close together (times the first supercontinent's spread) are one again
    static final float COAST_ROUGHNESS = 0.68f;   // noise persistence of the supercontinent: ragged coasts, peninsulas, islands
    static final float COAST_WARP = 120;          // cells the supercontinent's outline is pushed around: arms and bays, not a disc
    static final float SUPERCONTINENT_PLATES = 0.4f; // share of the plates born under the supercontinent: the pieces it breaks into
    static final float RIFT_ROUGHNESS = 0.6f;     // noise persistence of the ground plates grow through: how ragged their edges are
    /**
     * From format 62: Earth's plates by share of its surface, largest first (PB2002, Bird 2003, rounded): 7 major plates
     * (~80%), 9 minor ones (~15%), then microplates. A planet of N plates aims for the N largest, rescaled to the whole.
     */
    static final double[] EARTH_PLATES = {0.205, 0.115, 0.114, 0.109, 0.095, 0.090, 0.082,
            0.037, 0.031, 0.024, 0.017, 0.011, 0.010, 0.0095, 0.0064, 0.0059};
    static final double MICRO_FIRST = 0.0045, MICRO_RATIO = 0.86; // the microplates after them: shares falling geometrically
    static final float SHAPE_PULL = 0.7f;         // how strongly the supercontinent's crust gathers round its centre, against the noise

    public final int w, h;
    public final Plate[] plates;
    private double[] shares;     // from format 62: the share of the surface each plate slot aims for (plateShares); null on Earth
    public final float[] height; // composited crust thickness per global cell
    public final int[] owner;    // plate index per global cell
    public final float[] subduction; // recent subduction per cell: arc volcanoes rise behind active trenches
    public final float[] collision;  // recent continent-continent collision per cell: mountain belts, folded rock
    public final float[] spreading;  // recent new crust per cell: where plates pull apart, the axis of a mid-ocean ridge
    public final boolean earth;      // today's Earth (EarthData), not simulated: it never steps, and Hydrology takes its relief as it is
    private final float[] earthSlip; // Earth's fault slip from PB2002's transform faults, in place of strikeSlip's; null otherwise
    public final boolean[] earthLakes; // Earth's real lakes (Natural Earth); null otherwise
    private final boolean[] gap;     // cells this step's new crust fills
    private final Random rnd, volcanic; // separate streams, so volcanoes don't reshuffle the continents
    final long seed;
    private final int[] hotspots;       // fixed in the mantle; plates drift over them
    private final float volcanism;      // how many volcanoes, relative to normal
    private final double[] colCos, colSin; // per map column: its angle around the planet, for averaging plate centroids
    private final List<int[]> seamounts = new ArrayList<>(); // {cell, plate, birth step}: hotspot volcanoes riding their plates
    private int stepCount;
    private double centreX, centreY;      // the supercontinent that last broke up, or the meeting point while they gather
    private boolean gathering;
    private int phaseStart, lastBreakup = Integer.MAX_VALUE; // step the current phase began; step of the planned final breakup
    private final double firstSpread;     // mean distance of continental cells from their centre in the first supercontinent

    public static final class Plate {
        final float[] crust; // plate-local frame, 0 = no crust here
        final boolean[] rowHasCrust; // per local row: false means that whole row is empty, so compositing skips it
        float ox, oy;        // offset of local frame in global cells
        int oxi, oyi;
        float vx, vy;        // cells per step, |v| < 1

        Plate(int width, int height, float vx, float vy) {
            crust = new float[width * height];
            rowHasCrust = new boolean[height];
            this.vx = vx;
            this.vy = vy;
        }
    }

    public Planet(int w, int h, int plateCount, long seed) {
        this(w, h, plateCount, PlanetSettings.DEFAULT.oceanFraction(), seed);
    }

    public Planet(int w, int h, int plateCount, float oceanFraction, long seed) {
        this(w, h, plateCount, oceanFraction, 1, seed);
    }

    /** volcanoes: 1 is Earth-like (for a game), 0 none, 2 twice as many arc volcanoes and hotspots. */
    public Planet(int w, int h, int plateCount, float oceanFraction, float volcanoes, long seed) {
        this.w = w;
        this.h = h;
        volcanism = volcanoes;
        colCos = new double[w];
        colSin = new double[w];
        for (int x = 0; x < w; x++) {
            colCos[x] = Math.cos(2 * Math.PI * x / w);
            colSin[x] = Math.sin(2 * Math.PI * x / w);
        }
        this.seed = seed;
        earth = false;
        earthSlip = null;
        earthLakes = null;
        rnd = new Random(seed);
        height = continents(oceanFraction, rnd);
        owner = growPlates(plateCount, rnd);
        plates = new Plate[plateCount];
        for (int p = 0; p < plateCount; p++) {
            double angle = rnd.nextDouble() * Math.PI * 2, speed = 0.2 + rnd.nextDouble() * 0.6;
            plates[p] = new Plate(w, h, (float) (Math.cos(angle) * speed), (float) (Math.sin(angle) * speed));
        }
        for (int g = 0; g < w * h; g++) plates[owner[g]].crust[g] = height[g];
        double[] spread = continentSpread();
        firstSpread = spread[2];
        mantleFlow(1 / MANTLE_DRAG); // the breakup has begun: the pieces already move apart at their full pace
        markRows();
        subduction = new float[w * h];
        collision = new float[w * h];
        spreading = new float[w * h];
        gap = new boolean[w * h];
        volcanic = new Random(seed * 31 + 7);
        hotspots = new int[Math.round(Math.max(2, w * h / 131072f) * volcanism)]; // 4 on a medium planet
        for (int i = 0; i < hotspots.length; i++) // not under the ice caps
            hotspots[i] = volcanic.nextInt(w) + (int) (h * (0.15 + 0.7 * volcanic.nextDouble())) * w;
    }

    /**
     * Irregular plates of varied size: randomized flood fill from random seeds. From format 39 the fill crosses ground
     * of varying toughness, a fractal noise field, so plate edges (the rifts that later tear the continents) wander at
     * every scale like real rifts along old weak zones, instead of running as straight lines between seeds.
     */
    private int[] growPlates(int count, Random rnd) {
        float[] tough = Noise.fbm(w, h, w / 16, 2, rnd, RIFT_ROUGHNESS);
        int[] own = new int[w * h];
        Arrays.fill(own, -1);
        double[] growth = new double[count], target = plateShares(count);
        shares = target;
        int[] starts = new int[count];
        for (int p = 0; p < count; p++) {
            growth[p] = 1 / Math.sqrt(target[p]); // a plate's area goes about as 1 / growth²: low = big plate
            int start = rnd.nextInt(w * h);
            // From format 39 a share of the plates start inside the supercontinent, so it is always torn into pieces.
            // The largest come first, as Earth's continents ride its biggest plates.
            for (int tries = 0; p < SUPERCONTINENT_PLATES * count && height[start] < SEA_LEVEL && tries < 1000; tries++) start = rnd.nextInt(w * h);
            starts[p] = start;
        }
        // Each plate grows until it holds its share; the fastest would otherwise run over the slow ones. Ground walled in
        // by full plates then goes to whichever reaches it first, in a second round without limits.
        int[] area = new int[count];
        PriorityQueue<double[]> queue = new PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));
        for (int p = 0; p < count; p++) queue.add(new double[]{0, starts[p], p});
        for (int round = 0; round < 2; round++) {
            while (!queue.isEmpty()) {
                double[] e = queue.poll();
                int g = (int) e[1], p = (int) e[2];
                if (own[g] != -1 || round == 0 && area[p] >= target[p] * w * h) continue;
                own[g] = p;
                area[p]++;
                int x = g % w, y = g / w;
                for (int n : new int[]{Math.floorMod(x - 1, w) + y * w, (x + 1) % w + y * w, y > 0 ? g - w : -1, y < h - 1 ? g + w : -1})
                    if (n >= 0 && own[n] == -1) queue.add(new double[]{e[0] + growth[p] * (0.2 + tough[n] * tough[n]) * (1 + rnd.nextDouble()), n, p});
            }
            for (int g = 0; g < w * h; g++) {
                if (own[g] != -1) continue;
                int x = g % w, y = g / w;
                for (int n : new int[]{Math.floorMod(x - 1, w) + y * w, (x + 1) % w + y * w, y > 0 ? g - w : -1, y < h - 1 ? g + w : -1})
                    if (n >= 0 && own[n] != -1) queue.add(new double[]{0, g, own[n]});
            }
        }
        return own;
    }

    /** Share of the surface each of count plates aims for, largest first: Earth's largest count plates, rescaled. */
    public static double[] plateShares(int count) {
        double[] s = new double[count];
        double sum = 0, micro = MICRO_FIRST;
        for (int p = 0; p < count; p++) {
            s[p] = p < EARTH_PLATES.length ? EARTH_PLATES[p] : micro;
            if (p >= EARTH_PLATES.length) micro *= MICRO_RATIO;
            sum += s[p];
        }
        for (int p = 0; p < count; p++) s[p] /= sum;
        return s;
    }

    /**
     * The first supercontinent (a Pangaea): all the continental crust gathered in one landmass around a random centre,
     * its outline from rough fractal noise pushed around by two more noise fields, so it has arms, bays, peninsulas
     * and offshore islands instead of a smooth blob. oceanFraction of the planet starts as ocean.
     */
    private float[] continents(float oceanFraction, Random rnd) {
        float[] n = Noise.fbm(w, h, w / 8, 2, rnd, COAST_ROUGHNESS), wx = Noise.fbm(w, h, w / 8, 8, rnd), wy = Noise.fbm(w, h, w / 8, 8, rnd);
        float norm = 0, wmean = 0;
        for (int cell = w / 8; cell >= 2; cell /= 2) norm += (float) Math.pow(COAST_ROUGHNESS, Math.log(w / 8.0 / cell) / Math.log(2));
        for (int cell = w / 8; cell >= 8; cell /= 2) wmean += 0.5f * (float) Math.pow(0.5, Math.log(w / 8.0 / cell) / Math.log(2));
        centreX = rnd.nextInt(w);
        centreY = h * (0.4 + 0.2 * rnd.nextDouble());
        double land = (1 - oceanFraction) * w * h, a = Math.sqrt(land / (Math.PI * 0.75)), b = Math.min(0.75 * a, 0.4 * h); // a little wider than tall
        float[] score = new float[w * h];
        for (int g = 0; g < w * h; g++) {
            int x = g % w, y = g / w;
            int sx = Math.floorMod(Math.round(x + COAST_WARP * (wx[g] - wmean)), w), sy = Math.clamp(Math.round(y + COAST_WARP * (wy[g] - wmean)), 0, h - 1);
            double d = Math.hypot(dx(centreX, x) / a, (y - centreY) / b);
            score[g] = n[sx + sy * w] / norm + SHAPE_PULL * (float) (1 - d);
        }
        float[] sorted = score.clone();
        Arrays.sort(sorted);
        float threshold = sorted[(int) (sorted.length * oceanFraction)];
        float[] crust = new float[w * h];
        for (int g = 0; g < crust.length; g++)
            crust[g] = score[g] > threshold ? CONTINENT + Math.min(1, (score[g] - threshold) * 3) : OCEAN + rnd.nextFloat() * 0.3f; // old shields stand low: mountains come from collisions
        return crust;
    }

    /**
     * Today's Earth on a w x h planet: real elevation and PB2002's plates, with the histories the rest of the
     * simulation reads (trenches and arcs, collision belts, ridges, fault slip) drawn from PB2002's boundary steps.
     * The plates do not move; climate, rivers, lakes and glaciers run on it as on any planet.
     */
    public static Planet earth(int w, int h, long seed) {
        return new Planet(w, h, seed, new EarthData(w, h));
    }

    private Planet(int w, int h, long seed, EarthData e) {
        this.w = w;
        this.h = h;
        this.seed = seed;
        earth = true;
        volcanism = 1;
        colCos = new double[w];
        colSin = new double[w];
        for (int x = 0; x < w; x++) {
            colCos[x] = Math.cos(2 * Math.PI * x / w);
            colSin[x] = Math.sin(2 * Math.PI * x / w);
        }
        rnd = new Random(seed);
        volcanic = new Random(seed * 31 + 7);
        height = new float[w * h];
        for (int g = 0; g < w * h; g++) height[g] = SEA_LEVEL + fromMetres(e.elevation[g]);
        owner = e.plate.clone();
        plates = new Plate[e.plateCount];
        for (int p = 0; p < plates.length; p++) plates[p] = new Plate(w, h, 0, 0);
        for (int g = 0; g < w * h; g++) plates[owner[g]].crust[g] = height[g];
        markRows();
        subduction = e.subduction;
        collision = e.collision;
        spreading = e.spreading;
        earthSlip = e.slip;
        earthLakes = e.lake;
        gap = new boolean[w * h];
        hotspots = new int[0]; // ponytail: Earth's hotspots (Hawaii, Iceland...) and volcanoes are not placed yet
        firstSpread = 0;
    }

    /** Where the continents are: {centre x, centre y, mean distance of continental cells from that centre}. */
    private double[] continentSpread() {
        double cs = 0, sn = 0, cy = 0, spread = 0;
        int n = 0;
        for (int g = 0; g < w * h; g++)
            if (height[g] >= SEA_LEVEL) { cs += colCos[g % w]; sn += colSin[g % w]; cy += g / w; n++; }
        if (n == 0) return new double[]{0, h / 2.0, 0};
        double cx = Math.floorMod((long) (Math.atan2(sn, cs) * w / (2 * Math.PI)), w);
        cy /= n;
        for (int g = 0; g < w * h; g++) if (height[g] >= SEA_LEVEL) spread += Math.hypot(dx(cx, g % w), g / w - cy);
        return new double[]{cx, cy, spread / n};
    }

    /** The planned length of the run: the last supercontinent breaks up FINAL_DRIFT steps before its end. */
    public void plan(int steps) {
        lastBreakup = Math.max(0, steps - FINAL_DRIFT);
    }

    /**
     * The mantle's slow push on the continents. While a supercontinent breaks up, each plate carrying continent is
     * driven away from its centre; after DISPERSE_STEPS the flow turns and draws them back toward their common centre,
     * closing the young oceans between them (as the Iapetus closed), until they are one again and the next breakup
     * begins. The push scales with the plate's share of continent: ocean plates move by slab pull alone.
     */
    private void mantleFlow(double scale) {
        double[] where = continentSpread();
        boolean breakup = gathering && (where[2] < GATHERED * firstSpread || stepCount >= lastBreakup);
        if (breakup || !gathering && stepCount - phaseStart >= DISPERSE_STEPS && lastBreakup - stepCount > DISPERSE_STEPS) { // no time to gather: keep drifting
            gathering = !breakup;
            phaseStart = stepCount;
            if (breakup) lastBreakup = stepCount >= lastBreakup ? Integer.MAX_VALUE : lastBreakup;
            centreX = where[0];
            centreY = where[1];
        } else if (gathering) {
            centreX = where[0];
            centreY = h / 2.0; // continents meet at low latitudes, away from the poles
        }
        int n = plates.length;
        double[] cos = new double[n], sin = new double[n], cy = new double[n];
        int[] cont = new int[n], area = new int[n];
        for (int g = 0; g < w * h; g++) {
            int p = owner[g];
            area[p]++;
            if (height[g] < SEA_LEVEL) continue;
            cos[p] += colCos[g % w];
            sin[p] += colSin[g % w];
            cy[p] += g / w;
            cont[p]++;
        }
        for (int p = 0; p < n; p++) {
            if (cont[p] == 0) continue;
            double px = Math.atan2(sin[p], cos[p]) * w / (2 * Math.PI), ddx = dx(centreX, px), ddy = cy[p] / cont[p] - centreY, len = Math.hypot(ddx, ddy);
            if (len < 1) continue;
            double push = (gathering ? -1 : 1) * MANTLE_FLOW * scale * cont[p] / (double) area[p] / len;
            setVelocity(plates[p], plates[p].vx + ddx * push, plates[p].vy + ddy * push * FLOW_NORTH_SOUTH);
        }
    }

    /** Elevation above sea level, in crust units, to metres. Non-linear so plains stay low and MAX_CRUST lands near Everest. */
    public static float metres(float e) {
        return e >= 0 ? 250 * (float) Math.pow(e, 1.6) : 2700 * e;
    }

    /** The inverse of metres(). */
    public static float fromMetres(float m) {
        return m >= 0 ? (float) Math.pow(m / 250, 1 / 1.6) : m / 2700;
    }

    private int local(Plate p, int gx, int gy) {
        return Math.floorMod(gx - p.oxi, w) + Math.floorMod(gy - p.oyi, h) * w;
    }

    public void step() {
        int[] shiftX = new int[plates.length], shiftY = new int[plates.length];
        for (int i = 0; i < plates.length; i++) {
            Plate p = plates[i];
            int ox0 = p.oxi, oy0 = p.oyi;
            p.ox += p.vx;
            p.oy += p.vy;
            p.oxi = Math.round(p.ox);
            int noy = Math.round(p.oy);
            if (noy != p.oyi) {
                // The local frame is a torus but the poles are not: drop the row that wrapped across them.
                // ponytail: crust is destroyed/created at the poles; fine while poles are ice caps
                int gy = noy < p.oyi ? h - 1 : 0;
                p.oyi = noy;
                int ly = Math.floorMod(gy - noy, h);
                Arrays.fill(p.crust, ly * w, ly * w + w, 0);
            }
            shiftX[i] = p.oxi - ox0;
            shiftY[i] = p.oyi - oy0;
        }
        // Hotspot volcanoes ride their plate; off the poles they are gone.
        seamounts.removeIf(s -> {
            int y = s[0] / w + shiftY[s[1]];
            s[0] = Math.floorMod(s[0] % w + shiftX[s[1]], w) + y * w;
            return y < 0 || y >= h;
        });
        // Rows are independent: each global cell maps to a distinct local cell of every plate.
        AtomicIntegerArray collisions = new AtomicIntegerArray(plates.length);
        IntStream.range(0, h).parallel().forEach(y -> compositeRow(y, collisions));
        spreadEvenly();
        // Continents are too buoyant to subduct, so colliding plates grind to a halt instead of eating each other.
        for (int p = 0; p < plates.length; p++) {
            float drag = 1 / (1 + COLLISION_DRAG * collisions.get(p));
            plates[p].vx *= drag;
            plates[p].vy *= drag;
        }
        float[] next = height.clone();
        IntStream.range(0, h).parallel().forEach(y -> diffuseRow(y, next));
        System.arraycopy(next, 0, height, 0, height.length);
        seamounts.removeIf(s -> owner[s[0]] != s[1]); // subducted or overridden with their crust
        Centroids c = centroids();
        slabPull(c);
        mantleFlow(1);
        recyclePlate(c);
        for (int[] s : seamounts) s[1] = owner[s[0]]; // a rift or merger hands them to the plate now under them
        if (++stepCount % HOTSPOT_PERIOD == 0)
            for (int g : hotspots) seamounts.add(new int[]{g, owner[g], stepCount});
        markRows();
    }

    /** A plate has crust exactly where it owns the cell (compositing zeroes every loser), so ownership says which rows hold any. */
    private void markRows() {
        for (Plate p : plates) Arrays.fill(p.rowHasCrust, false);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                Plate p = plates[owner[x + y * w]];
                p.rowHasCrust[Math.floorMod(y - p.oyi, h)] = true;
            }
    }

    /** 1 on cells of old ocean floor diving under a plate that closes on it now: where a trench runs (Peru-Chile, Japan). */
    public float[] trenches() {
        float[] trench = new float[w * h];
        for (int y = 0; y < h - 1; y++)
            for (int x = 0; x < w; x++) {
                int g = x + y * w;
                for (int k = 0; k < 2; k++) { // east and south neighbours: each boundary once
                    int n = k == 0 ? (x + 1) % w + y * w : g + w, s = sinker(g, n);
                    if (s < 0) continue;
                    float closing = k == 0 ? plates[owner[g]].vx - plates[owner[n]].vx : plates[owner[g]].vy - plates[owner[n]].vy;
                    if (closing > RIFT_SPREAD) trench[s] = 1;
                }
            }
        return trench;
    }

    /** 1 on land cells where two plates pull apart now: the floor of a young continental rift (East Africa). */
    public float[] rifts() {
        float[] rift = new float[w * h];
        for (int y = 0; y < h - 1; y++)
            for (int x = 0; x < w; x++) {
                int g = x + y * w;
                for (int k = 0; k < 2; k++) { // east and south neighbours: each boundary once
                    int n = k == 0 ? (x + 1) % w + y * w : g + w, a = owner[g], b = owner[n];
                    if (a == b || height[g] < SEA_LEVEL || height[n] < SEA_LEVEL) continue;
                    float apart = k == 0 ? plates[b].vx - plates[a].vx : plates[b].vy - plates[a].vy;
                    if (apart > RIFT_SPREAD) rift[g] = rift[n] = 1;
                }
            }
        return rift;
    }

    /**
     * Strike-slip on cells at a plate boundary now: how fast the plate across the boundary slides past along it,
     * in cells per step (San Andreas, North Anatolian, Alpine Fault). The boundary's direction comes from the plate
     * cells around within FAULT_WINDOW, so a staircase of cells still reads as one straight fault.
     */
    public float[] strikeSlip() {
        if (earthSlip != null) return earthSlip.clone();
        float[] slip = new float[w * h];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int g = x + y * w, a = owner[g];
                boolean edge = false;
                for (int nb : new int[]{Math.floorMod(x - 1, w) + y * w, (x + 1) % w + y * w, y > 0 ? g - w : g, y < h - 1 ? g + w : g})
                    edge |= owner[nb] != a;
                if (!edge) continue;
                float nx = 0, ny = 0, vx = 0, vy = 0; // toward the other plate; its velocity, weighted the same
                for (int dy = -FAULT_WINDOW; dy <= FAULT_WINDOW; dy++)
                    for (int dx = -FAULT_WINDOW; dx <= FAULT_WINDOW; dx++) {
                        int yy = y + dy;
                        if (yy < 0 || yy >= h || dx == 0 && dy == 0) continue;
                        int b = owner[Math.floorMod(x + dx, w) + yy * w];
                        if (b == a) continue;
                        float k = 1f / (dx * dx + dy * dy);
                        nx += dx * k;
                        ny += dy * k;
                        vx += plates[b].vx * k;
                        vy += plates[b].vy * k;
                    }
                float len = (float) Math.hypot(nx, ny), wsum = 0;
                if (len == 0) continue;
                for (int dy = -FAULT_WINDOW; dy <= FAULT_WINDOW; dy++) // the same weights again, to average the velocity
                    for (int dx = -FAULT_WINDOW; dx <= FAULT_WINDOW; dx++) {
                        int yy = y + dy;
                        if (yy >= 0 && yy < h && !(dx == 0 && dy == 0) && owner[Math.floorMod(x + dx, w) + yy * w] != a) wsum += 1f / (dx * dx + dy * dy);
                    }
                float rx = vx / wsum - plates[a].vx, ry = vy / wsum - plates[a].vy; // the other side's motion, seen from this plate
                slip[g] = Math.abs(rx * ny - ry * nx) / len; // along the boundary; the rest opens a rift or builds a range
            }
        return slip;
    }

    /**
     * The zones around boundaries where plates slide past each other. [0]: signed distance in cells to the boundary,
     * up to FAULT_REACH, negative on the lower-numbered plate: its zero line is the fault's trace. [1]: the slip rate,
     * spread over the zone. Both blurred, so the trace runs as a smooth line instead of a staircase of cells.
     */
    public float[][] faultZones() {
        float[] slip = strikeSlip(), side = new float[w * h], rate = new float[w * h];
        int[] dist = new int[w * h], across = new int[w * h], queue = new int[w * h];
        Arrays.fill(dist, Integer.MAX_VALUE);
        Arrays.fill(side, FAULT_REACH);
        int n = 0;
        for (int g = 0; g < w * h; g++) {
            if (slip[g] < FAULT_MIN_SLIP) continue;
            int x = g % w, y = g / w;
            for (int nb : new int[]{Math.floorMod(x - 1, w) + y * w, (x + 1) % w + y * w, y > 0 ? g - w : g, y < h - 1 ? g + w : g})
                if (owner[nb] != owner[g]) across[g] = owner[nb];
            dist[g] = 0;
            rate[g] = slip[g];
            queue[n++] = g;
        }
        for (int i = 0; i < n; i++) { // outward through each plate, carrying the plate across and the slip
            int c = queue[i], x = c % w, y = c / w;
            side[c] = (owner[c] > across[c] ? 1 : -1) * (dist[c] + 0.5f); // the boundary lies half a cell out
            if (dist[c] == FAULT_REACH - 1) continue;
            for (int nb : new int[]{Math.floorMod(x - 1, w) + y * w, (x + 1) % w + y * w, y > 0 ? c - w : -1, y < h - 1 ? c + w : -1})
                if (nb >= 0 && dist[nb] == Integer.MAX_VALUE && owner[nb] == owner[c]) {
                    dist[nb] = dist[c] + 1;
                    across[nb] = across[c];
                    rate[nb] = 0; // only the boundary cells slip; the blur spreads it 2 cells, well inside the reach, so the field's far edge is no trace
                    queue[n++] = nb;
                }
        }
        Geology.blur(side, w, h);
        for (int pass = 0; pass < 2; pass++) Geology.blur(rate, w, h);
        return new float[][]{side, rate};
    }

    /** Erosion lowered a land cell: the crust under it thins too. */
    void setLandHeight(int g, float crust) {
        height[g] = crust;
        Plate p = plates[owner[g]];
        p.crust[local(p, g % w, g / w)] = crust;
    }

    /** Cells inland from an active trench, through the overriding plate, up to maxDist; MAX_VALUE beyond. */
    public int[] trenchDistance(int maxDist) {
        int[] dist = new int[w * h], queue = new int[w * h];
        Arrays.fill(dist, Integer.MAX_VALUE);
        int n = 0;
        for (int g = 0; g < w * h; g++) if (subduction[g] > ARC_ACTIVITY) { dist[g] = 0; queue[n++] = g; }
        for (int i = 0; i < n; i++) {
            int c = queue[i], x = c % w, y = c / w;
            if (dist[c] == maxDist) continue;
            for (int nb : new int[]{Math.floorMod(x - 1, w) + y * w, (x + 1) % w + y * w, y > 0 ? c - w : -1, y < h - 1 ? c + w : -1})
                if (nb >= 0 && dist[nb] == Integer.MAX_VALUE && owner[nb] == owner[c]) { dist[nb] = dist[c] + 1; queue[n++] = nb; }
        }
        return dist;
    }

    /** Arc volcanoes behind the trenches active now, plus every hotspot volcano still riding a plate. */
    public List<Volcano> volcanoes() {
        if (earth) return List.of(); // the real ones stand in the elevation already
        int[] dist = trenchDistance(ARC_MAX);
        List<Integer> candidates = new ArrayList<>();
        for (int g = 0; g < w * h; g++) if (dist[g] >= ARC_MIN && dist[g] <= ARC_MAX) candidates.add(g);
        Collections.shuffle(candidates, volcanic);
        List<Volcano> out = new ArrayList<>();
        double spacing = ARC_SPACING / Math.sqrt(volcanism); // twice the volcanoes along the same arcs
        for (int c : volcanism > 0 ? candidates : List.<Integer>of()) {
            boolean crowded = false;
            for (Volcano v : out)
                crowded |= Math.hypot(dx(v.u(), c % w), v.v() - c / w) < spacing;
            if (!crowded) out.add(new Volcano(c % w, c / w, false, 0, volcanic.nextFloat(), Float.NaN, Volcano.ARC_SPREAD));
        }
        for (int[] s : seamounts)
            out.add(new Volcano(s[0] % w, s[0] / w, true, stepCount - s[2], volcanic.nextFloat(), Float.NaN, Volcano.SHIELD_SPREAD));
        float[] rift = rifts(); // continental rifts have their volcanoes too (Kilimanjaro), more widely spaced
        for (int g = 0; g < rift.length && volcanism > 0; g++) {
            if (rift[g] == 0) continue;
            boolean crowded = false;
            for (Volcano v : out) crowded |= Math.hypot(dx(v.u(), g % w), v.v() - g / w) < RIFT_VOLCANO_SPACING / Math.sqrt(volcanism);
            if (!crowded) out.add(new Volcano(g % w, g / w, false, 0, volcanic.nextFloat(), Float.NaN, Volcano.ARC_SPREAD));
        }
        return out;
    }

    private record Centroids(double[] x, double[] y, int[] area) {}

    private Centroids centroids() {
        int n = plates.length;
        double[] cos = new double[n], sin = new double[n], cy = new double[n];
        int[] area = new int[n];
        for (int g = 0; g < w * h; g++) { // centroids; X is a circle, so average it as an angle
            int p = owner[g];
            cos[p] += colCos[g % w];
            sin[p] += colSin[g % w];
            cy[p] += g / w;
            area[p]++;
        }
        double[] cx = new double[n];
        for (int p = 0; p < n; p++) {
            cx[p] = Math.floorMod((long) (Math.atan2(sin[p], cos[p]) * w / (2 * Math.PI)), w);
            if (area[p] > 0) cy[p] /= area[p];
        }
        return new Centroids(cx, cy, area);
    }

    /** Wrapped east-west distance from a to b, in -w/2..w/2. */
    private double dx(double a, double b) {
        return Math.floorMod((long) (b - a + w / 2), w) - w / 2;
    }

    /** Old ocean floor sinking at a trench drags its plate toward that trench (Forsyth & Uyeda 1975). */
    private void slabPull(Centroids c) {
        double[] fx = new double[plates.length], fy = new double[plates.length];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int g = x + y * w;
                for (int k = 0; k < 2; k++) { // east and south neighbours: every boundary once
                    int nb = k == 0 ? (x + 1) % w + y * w : y < h - 1 ? g + w : -1;
                    if (nb < 0 || owner[nb] == owner[g]) continue;
                    int sink = sinker(g, nb);
                    if (sink < 0) continue;
                    int p = owner[sink];
                    double dx = dx(c.x[p], sink % w), dy = sink / w - c.y[p];
                    double len = Math.hypot(dx, dy);
                    if (len > 0) { fx[p] += dx / len; fy[p] += dy / len; }
                }
            }
        for (int p = 0; p < plates.length; p++)
            setVelocity(plates[p], plates[p].vx * (1 - MANTLE_DRAG) + fx[p] * SLAB_PULL, plates[p].vy * (1 - MANTLE_DRAG) + fy[p] * SLAB_PULL);
    }

    private static void setVelocity(Plate p, double vx, double vy) {
        double speed = Math.hypot(vx, vy), k = speed > MAX_SPEED ? MAX_SPEED / speed : 1;
        p.vx = (float) (vx * k);
        p.vy = (float) (vy * k);
    }

    /**
     * Keeps the plate count steady (the Wilson cycle): a plate subducted down to a sliver welds onto
     * the neighbour it touches most, and its slot is reused to rift the largest plate in two.
     */
    private void recyclePlate(Centroids c) {
        int dead = -1, largest = 0;
        for (int p = 0; p < plates.length; p++) {
            // From format 62 the slot rifts the plate furthest past its share, not simply the largest: halving the
            // largest every time a microplate died ground the big plates down until all were alike.
            if (shares == null ? c.area[p] > c.area[largest] : c.area[p] / shares[p] > c.area[largest] / shares[largest]) largest = p;
            // From format 62 a microplate is welded on only once it has shrunk to a quarter of its share: at a fixed
            // 1/400 Earth's microplates died at once, their slots split the largest plates, and all ended alike.
            double min = shares == null ? MIN_PLATE_FRACTION : Math.min(MIN_PLATE_FRACTION, shares[p] / 4);
            if (c.area[p] < w * h * min && (dead < 0 || c.area[p] < c.area[dead])) dead = p;
        }
        if (dead < 0) return;
        Plate d = plates[dead], big = plates[largest];

        int[] contact = new int[plates.length];
        for (int g = 0; g < w * h; g++) {
            if (owner[g] != dead) continue;
            int x = g % w, y = g / w;
            for (int nb : new int[]{Math.floorMod(x - 1, w) + y * w, (x + 1) % w + y * w, y > 0 ? g - w : -1, y < h - 1 ? g + w : -1})
                if (nb >= 0 && owner[nb] != dead) contact[owner[nb]]++;
        }
        int host = 0;
        for (int p = 1; p < plates.length; p++) if (contact[p] > contact[host]) host = p;
        for (int g = 0; g < w * h; g++) {
            if (owner[g] != dead) continue;
            plates[host].crust[local(plates[host], g % w, g / w)] = height[g];
            owner[g] = host;
        }
        Arrays.fill(d.crust, 0);

        // Rift: everything on one side of a wiggly line through the centroid moves to the reused slot.
        double angle = rnd.nextDouble() * Math.PI * 2, nx = Math.cos(angle), ny = Math.sin(angle);
        double phase = rnd.nextDouble() * Math.PI * 2, scale = w / 1024.0;
        d.ox = big.ox;
        d.oy = big.oy;
        d.oxi = big.oxi;
        d.oyi = big.oyi;
        for (int g = 0; g < w * h; g++) {
            if (owner[g] != largest) continue;
            double dx = dx(c.x[largest], g % w), dy = g / w - c.y[largest], along = -dx * ny + dy * nx;
            double wiggle = 16 * scale * (Math.sin(along / (23 * scale) + phase) + 0.5 * Math.sin(along / (9 * scale) + 2 * phase));
            if (dx * nx + dy * ny + wiggle <= 0) continue;
            int l = local(big, g % w, g / w); // same frame offset, so same local index in d
            d.crust[l] = big.crust[l];
            big.crust[l] = 0;
            owner[g] = dead;
        }
        setVelocity(d, big.vx + nx * RIFT_SPEED, big.vy + ny * RIFT_SPEED);
        setVelocity(big, big.vx - nx * RIFT_SPEED, big.vy - ny * RIFT_SPEED);
    }

    /** The cell that subducts at the boundary between neighbours a and b, or -1. Same rule as compositeRow. */
    private int sinker(int a, int b) {
        int pa = owner[a], pb = owner[b];
        if (pa == pb) return -1;
        if (height[a] < OLD_OCEAN && (height[b] >= SEA_LEVEL || pb > pa)) return a;
        if (height[b] < OLD_OCEAN && (height[a] >= SEA_LEVEL || pa > pb)) return b;
        return -1;
    }

    /** Decide who owns each cell and resolve plate boundaries. */
    private void compositeRow(int y, AtomicIntegerArray collisions) {
        int[] row = new int[plates.length], shift = new int[plates.length]; // local index of x = 0 is row + shift
        int[] active = new int[plates.length]; // plates with any crust in this row, in index order
        int actives = 0;
        for (int p = 0; p < plates.length; p++) {
            int ly = Math.floorMod(y - plates[p].oyi, h);
            row[p] = ly * w;
            shift[p] = Math.floorMod(-plates[p].oxi, w);
            if (plates[p].rowHasCrust[ly]) active[actives++] = p;
        }
        int[] present = new int[plates.length]; // plates with crust over this cell, in index order
        for (int x = 0; x < w; x++) {
            int g = x + y * w, best = -1, count = 0;
            float bestC = 0;
            boolean subducting = false, colliding = false;
            // Continents float on top. Between two oceanic plates the higher index wins, so a
            // whole plate subducts along the boundary instead of the plates shredding cell by cell.
            for (int a = 0; a < actives; a++) {
                int p = active[a];
                float c = plates[p].crust[row[p] + wrapX(x + shift[p])];
                if (c > 0) present[count++] = p;
                if (c > 0 && (best < 0 || c >= SEA_LEVEL || bestC < SEA_LEVEL)) { bestC = c; best = p; }
            }
            boolean spreads = best < 0;
            if (spreads) { // divergent: plates pulled apart, new crust wells up
                best = owner[g];
                bestC = RIDGE;
            } else {
                for (int i = 0; i < count; i++) { // convergent: the loser goes under
                    int p = present[i];
                    if (p == best) continue;
                    int l = row[p] + wrapX(x + shift[p]);
                    float c = plates[p].crust[l];
                    if (c == 0) continue;
                    plates[p].crust[l] = 0;
                    if (c >= SEA_LEVEL) { // only a continent can win against a continent
                        bestC += c * COLLISION_UPLIFT;
                        colliding = true;
                        collisions.incrementAndGet(p);
                        collisions.incrementAndGet(best);
                    }
                    else {
                        subducting = true; // ocean crust diving under: feeds an arc of volcanoes inland
                        // ponytail: ocean-ocean subduction adds no uplift here (it streaked the sea floor); the arc volcanoes make those islands
                        if (bestC >= SEA_LEVEL) bestC += c * SUBDUCTION_UPLIFT;
                    }
                }
            }
            subduction[g] = subduction[g] * SUBDUCTION_MEMORY + (subducting ? 1 : 0);
            collision[g] = collision[g] * COLLISION_MEMORY + (colliding ? 1 : 0);
            spreading[g] = spreading[g] * SUBDUCTION_MEMORY + (spreads ? 1 : 0);
            gap[g] = spreads;
            if (bestC < SEA_LEVEL) bestC = Math.max(OCEAN, bestC - OCEAN_AGING);
            bestC = Math.min(bestC, MAX_CRUST); // ponytail: excess mass just vanishes; spread it sideways if plateaus look too flat
            plates[best].crust[row[best] + wrapX(x + shift[best])] = bestC;
            owner[g] = best;
            height[g] = bestC;
        }
    }

    /**
     * New crust welling up between two plates freezes onto both, not just the one that moved off: each gap cell joins
     * the neighbouring plate whose edge there is older, so the flanks take turns and the ridge stays in the middle of
     * its ocean (the Mid-Atlantic Ridge), not stuck to the coast of the plate that stood still. Sequential, so the
     * result is the same on every run.
     */
    private void spreadEvenly() {
        for (int g = 0; g < w * h; g++) {
            if (!gap[g]) continue;
            int x = g % w, y = g / w, from = owner[g], to = from;
            float oldest = -1;
            for (int k = 0; k < 4; k++) {
                int ny = y + (k == 2 ? -1 : k == 3 ? 1 : 0);
                if (ny < 0 || ny >= h) continue;
                int n = Math.floorMod(x + (k == 0 ? -1 : k == 1 ? 1 : 0), w) + ny * w;
                if (gap[n]) continue;
                float age = height[n] >= SEA_LEVEL ? Float.MAX_VALUE : RIDGE - height[n]; // a continent's edge is the oldest of all
                if (age > oldest || age == oldest && owner[n] < to) { oldest = age; to = owner[n]; }
            }
            if (to == from) continue;
            plates[from].crust[local(plates[from], x, y)] = 0;
            plates[to].crust[local(plates[to], x, y)] = height[g];
            owner[g] = to;
        }
    }

    /** x in 0..2w-1 back into 0..w-1. */
    private int wrapX(int x) {
        return x >= w ? x - w : x;
    }

    /** Smooth within a plate so collisions make ranges, not one-cell spikes. Never across the coast, so continents keep their shape. */
    private void diffuseRow(int y, float[] next) {
        for (int x = 0; x < w; x++) {
            int g = x + y * w, p = owner[g];
            float sum = 0;
            boolean land = height[g] > SEA_LEVEL;
            for (int k = 0; k < 4; k++) {
                int n = k == 0 ? (x == 0 ? w - 1 : x - 1) + y * w : k == 1 ? (x == w - 1 ? 0 : x + 1) + y * w : k == 2 ? (y > 0 ? g - w : -1) : y < h - 1 ? g + w : -1;
                if (n >= 0 && owner[n] == p && (height[n] > SEA_LEVEL) == land) sum += height[n] - height[g];
            }
            next[g] = height[g] + DIFFUSION * sum;
            plates[p].crust[local(plates[p], x, y)] = next[g];
        }
    }
}
