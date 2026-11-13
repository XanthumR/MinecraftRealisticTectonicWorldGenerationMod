package com.realtectonics.sim;

/**
 * Format 100: valleys. The planet erodes on 32-block cells, and between them the mountains were only the cells'
 * smooth surface plus ridged noise (the massifs), whose troughs are closed basins, not valleys: the game's ranges had
 * a tenth of the Alps' relief within 10 km and a far steeper spectrum (tools/realdem.py). Here the high land's relief
 * comes from water instead, on points 4 blocks apart (Terrain.VALLEY_STEP; Small and Medium planets only), as a range where uplift and river erosion balance
 * (Cordonnier et al. 2016): the land drains, through any hollow, to the lakes, sea and low ground round it (priority
 * flood, carving through sills; Rho8's random pick among the ways down), and each point stands above the point it drains to by a slope that shrinks with the
 * water gathered there (steady-state stream power, m/n = 1/2): steep where little gathers, the ridges, gentle down the
 * valleys, which branch like a tree. The drainage is worked out again on that land a few times. Only the relief within
 * each cell is kept (the cell's mean is the planet's own), as tall as the massifs were. Only tiles holding high land
 * are kept. Built once per world from the planet (Terrain.valleyLand samples it); read back bilinearly.
 */
final class Valleys {
    static final int TILE = 64;              // points per tile side (a power of two)
    static final int ROUNDS = 2;             // drainage worked out on the land the last one made (4 look the same; 1 leaves blobs)
    static final float FALL = 16;            // blocks a point stands over the one it drains to where only its own rain runs off (the ridges), at full relief; less with more water, as 1/sqrt(points gathered)
    static final float RELIEF = 120;         // blocks of relief that count as full (Terrain.MASSIF_RELIEF)
    static final int SMOOTH = 2;             // rounds of averaging with the 8 points round: soil creeps, and the grid's own steps go
    static final int MEAN_REACH = 2;         // cells either side whose mean height is the planet's (valleys up to that size are kept)

    /** What the land is at unzoomed (x, z): out[0] the planet's smooth surface, [1] blocks of relief it may have, [2] 0 free, 1 fixed (lake), 2 off the high land, [3] blocks of unevenness water finds its way by. */
    interface Land {
        void sample(double x, double z, float[] out);
    }

    private final int step, pw, ph, tw, th, xBase, zBase;
    private final int[] slotOf;              // tile -> slot, -1 where no high land
    private final float[] delta;             // per slot's points: blocks over the planet's smooth surface

    Valleys(Land land, int cellsW, int cellsH, int blocksPerCell, int originX, int originY, int step, int zoom, java.util.function.IntPredicate highCell) {
        this.step = step;
        pw = cellsW * blocksPerCell / step;
        ph = cellsH * blocksPerCell / step;
        tw = pw / TILE;
        th = (ph + TILE - 1) / TILE;
        xBase = -originX * blocksPerCell;
        zBase = -originY * blocksPerCell;
        slotOf = new int[tw * th];
        int slots = 0, cellsPerTile = TILE * step / blocksPerCell, per = blocksPerCell / step; // points per cell side
        for (int t = 0; t < tw * th; t++) { // a tile holds high land if a cell within 3 of it does (the coastlines' warp reaches 1.8)
            boolean high = false;
            for (int cy = (t / tw) * cellsPerTile - 3; !high && cy < (t / tw + 1) * cellsPerTile + 3; cy++)
                for (int cx = (t % tw) * cellsPerTile - 3; !high && cx < (t % tw + 1) * cellsPerTile + 3; cx++)
                    high = cy >= 0 && cy < cellsH && highCell.test(Math.floorMod(cx, cellsW) + cy * cellsW);
            slotOf[t] = high ? slots++ : -1;
        }
        int[] tileOfSlot = new int[slots];
        for (int t = 0; t < tw * th; t++) if (slotOf[t] >= 0) tileOfSlot[slotOf[t]] = t;
        int n = slots * TILE * TILE;
        // Memory: ~27 bytes a point (12 million on seed 777, Medium), so a 2 GB game still loads: half floats where a
        // sixteenth of a block is plenty, the flood's level array reused for the water gathered and the smoothing, and the
        // heights turned into the relief in place.
        float[] base = new float[n];
        short[] relief = new short[n], rough = new short[n];
        byte[] kind = new byte[n];
        java.util.stream.IntStream.range(0, slots).parallel().forEach(s -> {
            float[] out = new float[4];
            int t = tileOfSlot[s];
            for (int k = 0; k < TILE * TILE; k++) {
                int px = (t % tw) * TILE + k % TILE, pz = (t / tw) * TILE + k / TILE, i = s * TILE * TILE + k;
                if (pz >= ph) { kind[i] = 2; continue; }
                land.sample(xBase + px * (double) step, zBase + pz * (double) step, out);
                base[i] = out[0];
                relief[i] = Float.floatToFloat16(out[1]);
                kind[i] = (byte) out[2];
                rough[i] = Float.floatToFloat16(out[3]);
            }
        });
        for (int i = 0; i < n; i++) // a free point beside land outside is an outlet: water leaves the high land there
            for (int d = 0; kind[i] == 0 && d < 8; d++) {
                int j = neighbour(i, d, tileOfSlot);
                if (j < 0 || kind[j] == 2) kind[i] = 1;
            }
        int[] receiver = new int[n], order = new int[n];
        byte[] dir = new byte[n];
        float[] level = new float[n], area = level, hgt = base.clone(); // the flood's levels, then the water gathered
        byte[] state = new byte[n];              // 1 reached, 2 taken off the heap
        Heap heap = new Heap(level);
        float run = FALL * zoom; // per point, at any spacing: a slope over sqrt(the area in blocks) is the same per point however far apart they are
        for (int round = 0; round < ROUNDS; round++) {
            // Priority flood from the outlets: every point drains somewhere, and the order points are reached in lists
            // each before all it drains (downstream first). Carved, not filled: a filled hollow is a flat, and water
            // crosses a flat in dead straight lines; the unevenness lets it wander as it does on real ground.
            java.util.Arrays.fill(state, (byte) 0);
            for (int i = 0; i < n; i++)
                if (kind[i] == 1) { state[i] = 1; level[i] = hgt[i]; receiver[i] = -1; heap.push(i); }
            int count = 0;
            while (heap.size > 0) {
                int p = heap.pop();
                state[p] = 2;
                order[count++] = p;
                if (receiver[p] >= 0) drain(p, round, level, state, receiver, dir, tileOfSlot);
                for (int d = 0; d < 8; d++) {
                    int q = neighbour(p, d, tileOfSlot);
                    if (q < 0 || state[q] != 0 || kind[q] != 0) continue;
                    state[q] = 1;
                    receiver[q] = p;
                    dir[q] = (byte) d;
                    level[q] = hgt[q] + Float.float16ToFloat(rough[q]);
                    heap.push(q);
                }
            }
            java.util.Arrays.fill(area, 1);
            for (int k = count - 1; k >= 0; k--) {
                int p = order[k];
                if (receiver[p] >= 0) area[receiver[p]] += area[p];
            }
            for (int k = 0; k < count; k++) { // steady state: each point stands above its receiver by its slope
                int p = order[k], r = receiver[p];
                hgt[p] = r < 0 ? base[p] : hgt[r] + run * Float.float16ToFloat(relief[p]) / RELIEF * (dir[p] < 4 ? 1 : 1.4142f) / (float) Math.sqrt(area[p]);
            }
        }
        // Keep only the relief within each cell: the cell's mean height is the planet's.
        int cw = pw / per, ch = (ph + per - 1) / per;
        float[] sum = new float[cw * ch], cnt = new float[cw * ch];
        for (int i = 0; i < n; i++) {
            if (kind[i] == 2) continue;
            int c = cellOf(i, tileOfSlot, per, cw);
            sum[c] += hgt[i] - base[i];
            cnt[c]++;
        }
        float[] wide = new float[cw * ch];
        for (int c = 0; c < sum.length; c++) {
            float sw = 0, nw = 0;
            for (int dz = -MEAN_REACH; dz <= MEAN_REACH; dz++)
                for (int dx = -MEAN_REACH; dx <= MEAN_REACH; dx++) {
                    int e = Math.floorMod(c % cw + dx, cw) + Math.clamp(c / cw + dz, 0, ch - 1) * cw;
                    sw += sum[e];
                    nw += cnt[e];
                }
            wide[c] = nw > 0 ? sw / nw : 0;
        }
        java.util.stream.IntStream.range(0, n).parallel().forEach(i -> {
            if (kind[i] == 2) { hgt[i] = 0; return; }
            int t = tileOfSlot[i / (TILE * TILE)], k = i % (TILE * TILE);
            float fx = ((t % tw) * TILE + k % TILE + 0.5f) / per - 0.5f, fz = ((t / tw) * TILE + k / TILE + 0.5f) / per - 0.5f;
            int cx = (int) Math.floor(fx), cz = (int) Math.floor(fz);
            float ax = fx - cx, az = fz - cz, mean = 0; // cubic B-spline through the cells: no creases at their centres
            for (int b = -1; b <= 2; b++)
                for (int a = -1; a <= 2; a++) mean += spline(a - ax) * spline(b - az) * cellMean(wide, cx + a, cz + b, cw, ch);
            float r = Float.float16ToFloat(relief[i]);
            hgt[i] = Math.clamp(hgt[i] - base[i] - mean, -r, r);
        });
        delta = hgt;
        float[] old = level;
        for (int pass = 0; pass < SMOOTH; pass++) {
            System.arraycopy(delta, 0, old, 0, n);
            java.util.stream.IntStream.range(0, n).parallel().forEach(i -> {
                if (kind[i] == 2) return;
                float total = 2 * old[i], weight = 2;
                for (int d = 0; d < 8; d++) {
                    int j = neighbour(i, d, tileOfSlot);
                    if (j < 0) continue;
                    float w = d < 4 ? 1 : 0.5f;
                    total += w * old[j];
                    weight += w;
                }
                delta[i] = total / weight;
            });
        }
    }

    /** Cubic B-spline weight at distance t (|t| < 2): smooth to the second derivative, so the mean leaves no edges. */
    private static float spline(float t) {
        t = Math.abs(t);
        return t < 1 ? (4 - 6 * t * t + 3 * t * t * t) / 6 : t < 2 ? (2 - t) * (2 - t) * (2 - t) / 6 : 0;
    }

    /**
     * Point p drains to one of the points already taken off the heap beside it (each lies downstream of p), picked at
     * random in proportion to how steeply the land falls to it, as Hydrology's Rho8: water on an even slope wanders and
     * gathers into branching valleys instead of running down dead straight parallel grooves. Where the land falls to
     * none (a hollow being carved through) the point that reached it stays.
     */
    private void drain(int p, int round, float[] level, byte[] state, int[] receiver, byte[] dir, int[] tileOfSlot) {
        float total = 0;
        for (int d = 0; d < 8; d++) total += fall(p, d, level, state, tileOfSlot);
        if (total <= 0) return;
        int t = tileOfSlot[p / (TILE * TILE)], k = p % (TILE * TILE);
        int h = ((t % tw) * TILE + k % TILE) * 0x27d4eb2d ^ ((t / tw) * TILE + k / TILE) * 0x165667b1 ^ round * 0x9e3779b9;
        h ^= h >>> 15; h *= 0x2c1b3c6d; h ^= h >>> 12; h *= 0x297a2d39; h ^= h >>> 15;
        float pick = (h >>> 8) / 16777216f * total;
        for (int d = 0; d < 8; d++) {
            float f = fall(p, d, level, state, tileOfSlot);
            if (f <= 0) continue;
            receiver[p] = neighbour(p, d, tileOfSlot); // the last way down, should rounding run past it
            dir[p] = (byte) d;
            if ((pick -= f) < 0) return;
        }
    }

    /** How steeply the land falls from p to its neighbour in direction d, if that one is already off the heap; else 0. */
    private float fall(int p, int d, float[] level, byte[] state, int[] tileOfSlot) {
        int j = neighbour(p, d, tileOfSlot);
        return j >= 0 && state[j] == 2 && level[j] < level[p] ? (level[p] - level[j]) / (d < 4 ? 1 : 1.4142f) : 0;
    }

    private int cellOf(int i, int[] tileOfSlot, int per, int cw) {
        int t = tileOfSlot[i / (TILE * TILE)], k = i % (TILE * TILE);
        return ((t % tw) * TILE + k % TILE) / per + ((t / tw) * TILE + k / TILE) / per * cw;
    }

    private static float cellMean(float[] mean, int cx, int cz, int cw, int ch) {
        return mean[Math.floorMod(cx, cw) + Math.clamp(cz, 0, ch - 1) * cw];
    }

    /** Blocks over the planet's smooth surface at unzoomed (x, z): the relief water made; 0 off the high land. */
    float at(double x, double z) {
        double fx = (x - xBase) / step, fz = (z - zBase) / step;
        int px = (int) Math.floor(fx), pz = (int) Math.floor(fz);
        float ax = (float) (fx - px), az = (float) (fz - pz);
        return (value(px, pz) * (1 - ax) + value(px + 1, pz) * ax) * (1 - az) + (value(px, pz + 1) * (1 - ax) + value(px + 1, pz + 1) * ax) * az;
    }

    private float value(int px, int pz) {
        if (pz < 0 || pz >= ph) return 0;
        px = Math.floorMod(px, pw);
        int s = slotOf[px / TILE + pz / TILE * tw];
        return s < 0 ? 0 : delta[s * TILE * TILE + (pz % TILE) * TILE + px % TILE];
    }

    private static final int[] DX = {1, -1, 0, 0, 1, -1, 1, -1}, DZ = {0, 0, 1, -1, 1, 1, -1, -1};

    /** The point beside point i in direction d, or -1 past the poles or in a tile without high land. */
    private int neighbour(int i, int d, int[] tileOfSlot) {
        int k = i % (TILE * TILE), kx = k % TILE, kz = k / TILE;
        if (kx > 0 && kx < TILE - 1 && kz > 0 && kz < TILE - 1) return i + DX[d] + DZ[d] * TILE; // inside the tile
        int t = tileOfSlot[i / (TILE * TILE)];
        int px = Math.floorMod((t % tw) * TILE + k % TILE + DX[d], pw), pz = (t / tw) * TILE + k / TILE + DZ[d];
        if (pz < 0 || pz >= ph) return -1;
        int s = slotOf[px / TILE + pz / TILE * tw];
        return s < 0 ? -1 : s * TILE * TILE + (pz % TILE) * TILE + px % TILE;
    }

    /** Binary min-heap of points keyed by their flood level. */
    private static final class Heap {
        int[] a = new int[4096];                 // grows: only the flood's edge is ever on it
        final float[] key;
        int size;

        Heap(float[] key) {
            this.key = key;
        }

        void push(int p) {
            if (size == a.length) a = java.util.Arrays.copyOf(a, 2 * size);
            int i = size++;
            while (i > 0 && key[a[(i - 1) / 2]] > key[p]) { a[i] = a[(i - 1) / 2]; i = (i - 1) / 2; }
            a[i] = p;
        }

        int pop() {
            int top = a[0], last = a[--size], i = 0;
            while (2 * i + 1 < size) {
                int c = 2 * i + 1;
                if (c + 1 < size && key[a[c + 1]] < key[a[c]]) c++;
                if (key[a[c]] >= key[last]) break;
                a[i] = a[c];
                i = c;
            }
            a[i] = last;
            return top;
        }
    }
}
