exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""    /** The column at real block (x, z). */""",
 """    /**
     * The column at real block (x, z) after rockfall (format 29): the terrain's shapes are idealised curves and walls,
     * so every tile of the world is also simulated at block scale. Rock steeper than it can stand falls to the
     * ground below, round after round, until cliffs crumble into steep faces with scree aprons at their feet. Water,
     * lava, ice and the ground next to them stay put, so no lake or river leaks; rock falling into them is carried
     * away. Each round updates every block at once, so a block depends only on the land within RELAX_STEPS of it:
     * tiles computed with a margin that wide agree exactly where they meet. Older planets get column() unchanged.
     */
    public Column relaxedColumn(int x, int z) {
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
    }

    /** Rockfall over the tile of real blocks starting at (x0, z0); returns its columns, row by row. */
    Column[] relaxTile(int x0, int z0) {
        int m = RELAX_STEPS + 1, n = RELAX_TILE + 2 * m;
        Column[] cs = new Column[n * n];
        float[] hgt = new float[n * n], fallen = new float[n * n];
        for (int i = 0; i < n * n; i++) {
            cs[i] = column(x0 - m + i % n, z0 - m + i / n);
            hgt[i] = cs[i].ground();
        }
        boolean[] fixed = new boolean[n * n]; // wet, icy or beside water: holds lakes and rivers in
        for (int i = 0; i < n * n; i++) {
            int x = i % n, z = i / n;
            if (x == 0 || z == 0 || x == n - 1 || z == n - 1) { fixed[i] = true; continue; }
            for (int dz = -1; dz <= 1 && !fixed[i]; dz++)
                for (int dx = -1; dx <= 1 && !fixed[i]; dx++) {
                    Column c = cs[i + dx + dz * n];
                    fixed[i] = c.water() != Column.NO_WATER || c.lava() != Column.NO_WATER || dx == 0 && dz == 0 && c.ice() != Column.NO_WATER;
                }
        }
        int[] off = {1, -1, n, -n, n + 1, n - 1, -n + 1, -n - 1};
        float[] limit = new float[8];
        for (int k = 0; k < 8; k++) limit[k] = RELAX_TALUS * (k < 4 ? 1 : 1.4142f);
        for (int step = 0; step < RELAX_STEPS; step++) {
            float[] next = hgt.clone();
            for (int i = n + 1; i < n * n - n - 1; i++) {
                if (fixed[i]) continue;
                for (int k = 0; k < 8; k++) {
                    int j = i + off[k];
                    float excess = hgt[i] - hgt[j] - limit[k];
                    if (excess <= 0) continue;
                    float move = excess * RELAX_RATE;
                    next[i] -= move;
                    if (!fixed[j]) { // into water the rubble is carried away
                        next[j] += move;
                        fallen[j] += move;
                    }
                }
            }
            hgt = next;
        }
        Column[] tile = new Column[RELAX_TILE * RELAX_TILE];
        for (int z = 0; z < RELAX_TILE; z++)
            for (int x = 0; x < RELAX_TILE; x++) {
                int i = x + m + (z + m) * n;
                Column c = cs[i];
                int ground = Math.round(hgt[i]);
                Cover cover = c.cover() == Cover.NONE && fallen[i] > SCREE_FALLEN ? Cover.SCREE : c.cover();
                tile[x + z * RELAX_TILE] = ground == c.ground() && cover == c.cover() ? c
                        : new Column(ground, c.water(), c.lava(), c.ice() > ground ? c.ice() : Column.NO_WATER, c.biome(), c.fold(), c.volcanic(), c.rock(), cover);
            }
        return tile;
    }

    /** The column at real block (x, z). */"""),
("""    private final boolean allTroughs, nearestLakes; // every trough cuts, so overlaps blend; lakes keep their own level, sills between: format 28""",
 """    private final boolean allTroughs, nearestLakes; // every trough cuts, so overlaps blend; lakes keep their own level, sills between: format 28
    public final boolean rockfall;        // block-scale rockfall over each tile of the world (relaxedColumn): format 29
    private final Map<Long, Column[]> relaxedTiles = new java.util.LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Column[]> eldest) {
            return size() > RELAX_CACHE;
        }
    };"""),
("""        allTroughs = nearestLakes = data.format >= 28;""", """        allTroughs = nearestLakes = data.format >= 28;
        rockfall = data.format >= 29;"""),
("""    static final float LAKE_SILL = 0.25f;   // cells: width of the rock sill between two lakes at different levels""",
 """    static final float LAKE_SILL = 0.25f;   // cells: width of the rock sill between two lakes at different levels
    static final int RELAX_TILE = 128, RELAX_STEPS = 23, RELAX_CACHE = 96; // blocks per rockfall tile; rounds of rockfall (the tile margin is one more); tiles kept
    static final float RELAX_TALUS = 2, RELAX_RATE = 0.2f, SCREE_FALLEN = 2; // blocks up per block across that rock stands; share of the excess falling per round; blocks of rubble that make scree"""),
])
p='src/main/java/com/realtectonics/sim/PlanetData.java'; s=open(p,encoding='utf-8').read()
assert s.count('private static final int FORMAT = 28;')==1
s=s.replace('private static final int FORMAT = 28;','private static final int FORMAT = 29;').replace('28 blended troughs and lake sills; older','28 blended troughs and lake sills, 29 block-scale rockfall; older')
open(p,'w',encoding='utf-8').write(s)
G = 'src/main/java/com/realtectonics/mod/TectonicChunkGenerator.java'
edit(G, [
("""            for (int dx = -1; dx <= 16; dx++) cols[dx + 1 + (dz + 1) * 18] = beardify(terrain.column(x0 + dx, z0 + dz), beard, x0 + dx, z0 + dz);""",
 """            for (int dx = -1; dx <= 16; dx++) cols[dx + 1 + (dz + 1) * 18] = beardify(terrain.relaxedColumn(x0 + dx, z0 + dz), beard, x0 + dx, z0 + dz);"""),
])
print("ok")
