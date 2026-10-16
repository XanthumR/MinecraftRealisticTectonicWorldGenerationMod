exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""    public Column relaxedColumn(int x, int z) {
        if (!rockfall) return column(x, z);
        int tx = Math.floorDiv(x, RELAX_TILE), tz = Math.floorDiv(z, RELAX_TILE);
        long key = (long) tx << 32 | (tz & 0xffffffffL);
        Column[] tile;
        synchronized (relaxedTiles) {
            tile = relaxedTiles.get(key);
        }
        if (tile == null) {
            tile = relaxTile(tx * RELAX_TILE, tz * RELAX_TILE);
            synchronized (relaxedTiles) {
                relaxedTiles.put(key, tile);
            }
        }
        return tile[(x - tx * RELAX_TILE) + (z - tz * RELAX_TILE) * RELAX_TILE];
    }""",
 """    public Column relaxedColumn(int x, int z) {
        if (!rockfall) return column(x, z);
        int tx = Math.floorDiv(x, RELAX_TILE), tz = Math.floorDiv(z, RELAX_TILE);
        return tile(tx, tz)[(x - tx * RELAX_TILE) + (z - tz * RELAX_TILE) * RELAX_TILE];
    }

    /** The w x h block of relaxed columns from real block (x0, z0), row by row: one tile lookup per tile, not per column. */
    public Column[] relaxedColumns(int x0, int z0, int w, int h) {
        Column[] out = new Column[w * h];
        for (int z = 0; z < h; z++)
            for (int x = 0; x < w; ) {
                int bx = x0 + x, bz = z0 + z, tx = Math.floorDiv(bx, RELAX_TILE), tz = Math.floorDiv(bz, RELAX_TILE);
                if (!rockfall) { out[x + z * w] = column(bx, bz); x++; continue; }
                Column[] tile = tile(tx, tz);
                int lx = bx - tx * RELAX_TILE, lz = bz - tz * RELAX_TILE, run = Math.min(w - x, RELAX_TILE - lx);
                System.arraycopy(tile, lx + lz * RELAX_TILE, out, x + z * w, run);
                x += run;
            }
        return out;
    }

    /** A relaxed tile, computed once: threads asking for one being computed wait for it instead of repeating it. */
    private Column[] tile(int tx, int tz) {
        long key = (long) tx << 32 | (tz & 0xffffffffL);
        java.util.concurrent.CompletableFuture<Column[]> f = relaxedTiles.get(key);
        if (f == null) {
            java.util.concurrent.CompletableFuture<Column[]> mine = new java.util.concurrent.CompletableFuture<>();
            f = relaxedTiles.putIfAbsent(key, mine);
            if (f == null) {
                f = mine;
                tileOrder.add(key);
                while (tileOrder.size() > RELAX_CACHE) { // ponytail: oldest-first eviction, not least-recently-used; fine while chunks arrive in rough order
                    Long old = tileOrder.poll();
                    if (old != null) relaxedTiles.remove(old);
                }
                try {
                    mine.complete(relaxTile(tx * RELAX_TILE, tz * RELAX_TILE));
                } catch (RuntimeException e) {
                    relaxedTiles.remove(key);
                    mine.completeExceptionally(e);
                }
            }
        }
        return f.join();
    }"""),
("""    private final Map<Long, Column[]> relaxedTiles = new java.util.LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Column[]> eldest) {
            return size() > RELAX_CACHE;
        }
    };""",
 """    private final java.util.concurrent.ConcurrentHashMap<Long, java.util.concurrent.CompletableFuture<Column[]>> relaxedTiles = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentLinkedQueue<Long> tileOrder = new java.util.concurrent.ConcurrentLinkedQueue<>();"""),
# stop once nothing moves: later rounds would change nothing, so results stay exact
("""        for (int step = 0; step < RELAX_STEPS; step++) {
            float[] next = hgt.clone();""",
 """        for (int step = 0; step < RELAX_STEPS; step++) {
            float[] next = hgt.clone();
            boolean moved = false;"""),
("""                    float move = excess * RELAX_RATE;
                    next[i] -= move;""",
 """                    float move = excess * RELAX_RATE;
                    moved = true;
                    next[i] -= move;"""),
("""            hgt = next;
        }
        Column[] tile = new Column[RELAX_TILE * RELAX_TILE];""",
 """            hgt = next;
            if (!moved) break; // settled: further rounds would change nothing, so tiles still agree exactly
        }
        Column[] tile = new Column[RELAX_TILE * RELAX_TILE];"""),
])
G = 'src/main/java/com/realtectonics/mod/TectonicChunkGenerator.java'
edit(G, [
("""            for (int dx = -1; dx <= 16; dx++) cols[dx + 1 + (dz + 1) * 18] = beardify(terrain.relaxedColumn(x0 + dx, z0 + dz), beard, x0 + dx, z0 + dz);""",
 """            for (int dx = -1; dx <= 16; dx++) cols[dx + 1 + (dz + 1) * 18] = beardify(relaxed[dx + 1 + (dz + 1) * 18], beard, x0 + dx, z0 + dz);"""),
])
print("ok")
