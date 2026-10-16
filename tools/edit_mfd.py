exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""        float[] area = new float[n * n];
        java.util.Arrays.fill(area, 1);
        for (int round = 0; round < GULLY_REACH; round++) { // each round water from one block further upslope arrives
            float[] next = new float[n * n];
            java.util.Arrays.fill(next, 1);
            for (int i = 0; i < n * n; i++) if (receiver[i] >= 0) next[receiver[i]] += area[i];
            area = next;
        }""",
 """        // From format 41 water spreads to every lower neighbour by how steeply each lies below (Quinn et al. 1991):
        // sent all one way, it splits an even slope into parallel lines a few blocks apart that each cut a groove, a
        // comb of steps across the hillside. Spread, it stays a sheet on even ground and gathers only in hollows.
        float[] share = spreadFlow ? new float[n * n * 8] : null;
        for (int i = n + 1; spreadFlow && i < n * n - n - 1; i++) {
            float sum = 0;
            for (int k = 0; k < 8; k++) {
                float drop = (smooth[i] - smooth[i + off[k]]) / dist[k];
                if (drop > 0) sum += share[i * 8 + k] = (float) Math.pow(drop, GULLY_SPREAD);
            }
            for (int k = 0; k < 8 && sum > 0; k++) share[i * 8 + k] /= sum;
        }
        float[] area = new float[n * n];
        java.util.Arrays.fill(area, 1);
        for (int round = 0; round < GULLY_REACH; round++) { // each round water from one block further upslope arrives
            float[] next = new float[n * n];
            java.util.Arrays.fill(next, 1);
            for (int i = 0; i < n * n; i++)
                if (spreadFlow) { for (int k = 0; k < 8; k++) if (share[i * 8 + k] > 0) next[i + off[k]] += area[i] * share[i * 8 + k]; }
                else if (receiver[i] >= 0) next[receiver[i]] += area[i];
            area = next;
        }"""),
("""    static final float GULLY_THRESHOLD = 1.5f;        // format 37: blocks of that stream power spent before water cuts at all (the soil holds)""",
 """    static final float GULLY_THRESHOLD = 1.5f;        // format 37: blocks of that stream power spent before water cuts at all (the soil holds)
    static final float GULLY_SPREAD = 1.1f;           // format 41: exponent on each lower neighbour's drop when water divides between them"""),
("""    private final boolean flowingIce;     // ice gathers in valleys fed from above, steep ground sheds it, the surface slopes smoothly: format 40""",
 """    private final boolean flowingIce;     // ice gathers in valleys fed from above, steep ground sheds it, the surface slopes smoothly: format 40
    private final boolean spreadFlow;     // block-scale runoff divides between lower neighbours: no comb of grooves on even slopes: format 41"""),
("""        flowingIce = data.format >= 40;""", """        flowingIce = data.format >= 40;
        spreadFlow = data.format >= 41;"""),
])
p = 'src/main/java/com/realtectonics/sim/PlanetData.java'; s = open(p, encoding='utf-8').read()
assert s.count('FORMAT = 40;') == 1
s = s.replace('FORMAT = 40;', 'FORMAT = 41;').replace('40 flowing ice; older', '40 flowing ice, 41 runoff spreads over even slopes; older')
open(p, 'w', encoding='utf-8').write(s)
print("ok")
