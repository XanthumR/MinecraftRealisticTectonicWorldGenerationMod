exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""    Column[] relaxTile(int x0, int z0) {
        int m = RELAX_STEPS + 1, n = RELAX_TILE + 2 * m;""",
 """    Column[] relaxTile(int x0, int z0) {
        int m = RELAX_STEPS + 1 + (simGullies ? GULLY_REACH + 3 : 0), n = RELAX_TILE + 2 * m; // gullies look GULLY_REACH upslope, plus routing and the cut"""),
("""        int[] off = {1, -1, n, -n, n + 1, n - 1, -n + 1, -n - 1};
        float[] limit = new float[8];""",
 """        int[] off = {1, -1, n, -n, n + 1, n - 1, -n + 1, -n - 1};
        if (simGullies) hgt = gullies(hgt, fixed, cs, n, off);
        float[] limit = new float[8];"""),
("""    /** The column at real block (x, z). */
    public Column column(int x, int z) {""",
 """    /**
     * From format 33: rain runs down the block terrain and cuts gullies where it gathers, instead of a pattern laid
     * over the slopes. Each block drains to its steepest lower neighbour; the water gathered from upslope and the
     * slope decide how deep it cuts (stream power), so gullies start faint below the crests, deepen downhill and join
     * like the branches of a tree. Rockfall afterwards widens each cut into a V. Only the blocks within GULLY_REACH
     * steps upslope count, so a block depends on nearby land alone and tiles still agree where they meet. Blocks
     * beside water, ice or lava, and covered ground (banks, salt, sinter, ponds), are not cut, so nothing can leak.
     */
    private float[] gullies(float[] hgt, boolean[] fixed, Column[] cs, int n, int[] off) {
        float[] smooth = hgt.clone(), dist = {1, 1, 1, 1, 1.4142f, 1.4142f, 1.4142f, 1.4142f};
        for (int i = n + 1; i < n * n - n - 1; i++) { // integer block steps give water no direction; a light blur does
            float sum = hgt[i] * 4;
            for (int k = 0; k < 8; k++) sum += hgt[i + off[k]] * (k < 4 ? 1 : 0.5f) / 1.5f;
            smooth[i] = sum / (4 + 4 / 1.5f + 2 / 1.5f);
        }
        int[] receiver = new int[n * n];
        float[] slope = new float[n * n];
        java.util.Arrays.fill(receiver, -1);
        for (int i = n + 1; i < n * n - n - 1; i++) {
            if (i % n == 0 || i % n == n - 1) continue;
            for (int k = 0; k < 8; k++) {
                float drop = (smooth[i] - smooth[i + off[k]]) / dist[k];
                if (drop > slope[i]) { slope[i] = drop; receiver[i] = i + off[k]; }
            }
        }
        float[] area = new float[n * n];
        java.util.Arrays.fill(area, 1);
        for (int round = 0; round < GULLY_REACH; round++) { // each round water from one block further upslope arrives
            float[] next = new float[n * n];
            java.util.Arrays.fill(next, 1);
            for (int i = 0; i < n * n; i++) if (receiver[i] >= 0) next[receiver[i]] += area[i];
            area = next;
        }
        float[] cut = new float[n * n], out = hgt.clone();
        for (int i = 0; i < n * n; i++)
            if (receiver[i] >= 0 && !fixed[i] && cs[i].cover() == Cover.NONE)
                cut[i] = Math.min(GULLY_DEPTH, GULLY_POWER * (float) Math.sqrt(area[i]) * slope[i]);
        for (int i = 0; i < n * n; i++) // never below the floor it drains to, so a gully runs out instead of ending in a pit
            if (cut[i] > 0) out[i] = Math.max(hgt[i] - cut[i], hgt[receiver[i]] - cut[receiver[i]]);
        return out;
    }

    /** The column at real block (x, z). */
    public Column column(int x, int z) {"""),
("""        if (gullies && natural > SEA_Y + 20) natural += gullies(x, z, u, v, natural - SEA_Y) * (1 - ridgeline * ridgeline * ridgeline); // none on the crests""",
 """        if (gullies && !simGullies && natural > SEA_Y + 20) natural += gullies(x, z, u, v, natural - SEA_Y) * (1 - ridgeline * ridgeline * ridgeline); // none on the crests; from 33 water cuts them (relaxTile)"""),
("""    private final boolean faults;         // a scarp along the trace of each strike-slip fault: format 32""",
 """    private final boolean faults;         // a scarp along the trace of each strike-slip fault: format 32
    private final boolean simGullies;     // gullies cut by water running down the block terrain, not a laid pattern: format 33"""),
("""        faults = data.format >= 32;""", """        faults = data.format >= 32;
        simGullies = data.format >= 33;"""),
("""    static final int GULLY_WIDE = 64, GULLY_NARROW = 16; // real blocks between gullies, two sizes; powers of two so they wrap""",
 """    static final int GULLY_WIDE = 64, GULLY_NARROW = 16; // real blocks between gullies, two sizes; powers of two so they wrap
    static final int GULLY_REACH = 24;                // format 33: blocks upslope whose water a gully gathers
    static final float GULLY_POWER = 0.3f;            // format 33: blocks cut per sqrt(blocks gathered) per block of slope"""),
])
p = 'src/main/java/com/realtectonics/sim/PlanetData.java'; s = open(p, encoding='utf-8').read()
assert s.count('FORMAT = 32;') == 1
s = s.replace('FORMAT = 32;', 'FORMAT = 33;').replace('32 strike-slip faults (faultSide, faultSlip); older', '32 strike-slip faults (faultSide, faultSlip), 33 gullies cut by water at block scale; older')
open(p, 'w', encoding='utf-8').write(s)
print("ok")
