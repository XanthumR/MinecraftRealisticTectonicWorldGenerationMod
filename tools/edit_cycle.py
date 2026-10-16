exec(open('tools/edit_glacier.py').read().split("C = ")[0])
N = 'src/main/java/com/realtectonics/sim/Noise.java'
edit(N, [
("""    static float[] fbm(int w, int h, int baseCell, int minCell, Random rnd) {
        float[] out = new float[w * h];
        float amp = 1;
        for (int cell = baseCell; cell >= minCell; cell /= 2, amp *= 0.5f) {""",
 """    static float[] fbm(int w, int h, int baseCell, int minCell, Random rnd) {
        return fbm(w, h, baseCell, minCell, rnd, 0.5f);
    }

    /** persistence: amplitude kept per octave; above 0.5 the finer scales stay strong, as on ragged real coastlines. */
    static float[] fbm(int w, int h, int baseCell, int minCell, Random rnd, float persistence) {
        float[] out = new float[w * h];
        float amp = 1;
        for (int cell = baseCell; cell >= minCell; cell /= 2, amp *= persistence) {"""),
])
P = 'src/main/java/com/realtectonics/sim/Planet.java'
edit(P, [
("""    static final float FAULT_MIN_SLIP = 0.05f;    // cells per step of sliding that make a boundary a fault worth showing""",
 """    static final float FAULT_MIN_SLIP = 0.05f;    // cells per step of sliding that make a boundary a fault worth showing
    // The supercontinent cycle (Wilson 1966; Nance et al. 1988): one continent insulates the mantle beneath it, the
    // heat lifts and tears it apart, the pieces drift off, and in time the oceans between them close again.
    static final float MANTLE_FLOW = 0.02f;       // per step, on a plate that is all continent: terminal ~0.4 cells/step against MANTLE_DRAG
    static final float FLOW_NORTH_SOUTH = 0.5f;   // the flow's north-south share: crust carried across a pole is lost
    static final int DISPERSE_STEPS = 100;        // steps the pieces of a supercontinent drift apart before the oceans start closing
    static final int FINAL_DRIFT = 150;           // the last breakup this many steps before the end, so the planet is left with drifting continents
    static final float GATHERED = 1.2f;           // continents this close together (times the first supercontinent's spread) are one again
    static final float COAST_ROUGHNESS = 0.68f;   // noise persistence of the supercontinent: ragged coasts, peninsulas, islands
    static final float COAST_WARP = 40;           // cells the supercontinent's outline is pushed around: arms and bays, not a disc"""),
("""    private int stepCount;""",
 """    private int stepCount;
    private double centreX, centreY;      // the supercontinent that last broke up, or the meeting point while they gather
    private boolean gathering;
    private int phaseStart, lastBreakup = Integer.MAX_VALUE; // step the current phase began; step of the planned final breakup
    private final double firstSpread;     // mean distance of continental cells from their centre in the first supercontinent"""),
("""        for (int g = 0; g < w * h; g++) plates[owner[g]].crust[g] = height[g];
        markRows();""",
 """        for (int g = 0; g < w * h; g++) plates[owner[g]].crust[g] = height[g];
        double[] spread = continentSpread();
        firstSpread = spread[2];
        mantleFlow(1 / MANTLE_DRAG); // the breakup has begun: the pieces already move apart at their full pace
        markRows();"""),
("""    /** Continental crust as blobs from X-periodic fractal noise; oceanFraction of the planet starts as ocean. */
    private float[] continents(float oceanFraction, Random rnd) {
        float[] n = Noise.fbm(w, h, w / 8, 2, rnd);
        float[] sorted = n.clone();
        Arrays.sort(sorted);
        float threshold = sorted[(int) (sorted.length * oceanFraction)];
        float[] crust = new float[w * h];
        for (int g = 0; g < crust.length; g++)
            crust[g] = n[g] > threshold ? CONTINENT + (n[g] - threshold) * 4 : OCEAN + rnd.nextFloat() * 0.3f;
        return crust;
    }""",
 """    /**
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
            score[g] = n[sx + sy * w] / norm + 1.3f * (float) (1 - d);
        }
        float[] sorted = score.clone();
        Arrays.sort(sorted);
        float threshold = sorted[(int) (sorted.length * oceanFraction)];
        float[] crust = new float[w * h];
        for (int g = 0; g < crust.length; g++)
            crust[g] = score[g] > threshold ? CONTINENT + (score[g] - threshold) * 4 : OCEAN + rnd.nextFloat() * 0.3f;
        return crust;
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
        if (breakup || !gathering && stepCount - phaseStart >= DISPERSE_STEPS && stepCount < lastBreakup) {
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
    }"""),
("""        slabPull(c);""", """        slabPull(c);
        mantleFlow(1);"""),
])
D = 'src/main/java/com/realtectonics/sim/PlanetData.java'
edit(D, [
("""    public static void tectonics(Planet planet, int steps, DoubleConsumer progress) {""",
 """    public static void tectonics(Planet planet, int steps, DoubleConsumer progress) {
        planet.plan(steps);"""),
])
p = 'src/main/java/com/realtectonics/sim/PlanetData.java'; s = open(p, encoding='utf-8').read()
assert s.count('FORMAT = 38;') == 1
s = s.replace('FORMAT = 38;', 'FORMAT = 39;').replace('38 lake-effect rain, green riverbanks, ice-free active craters; older', '38 lake-effect rain, green riverbanks, ice-free active craters, 39 supercontinent cycle (sim only); older')
open(p, 'w', encoding='utf-8').write(s)
print("ok")
