exec(open('tools/edit_glacier.py').read().split("C = ")[0])
H = 'src/main/java/com/realtectonics/sim/Hydrology.java'
edit(H, [
("""    static final float LAKE_EVAPORATION = 1;    // metres per year off a lake surface
""",
 """    static final float MIN_EVAPORATION = 0.1f;  // metres per year off a lake surface near freezing (Siberia, Canada)
    static final float EVAPORATION_PER_DEGREE = 0.07f; // more per °C above that: ~2 m/yr on a hot desert lake
"""),
("""    private final boolean[] lake;""", """    private final boolean[] lake;
    private final float[] lakeLevel; // lake surface where lake is true: the spill level, or lower in a shrunken lake"""),
("""        lake = new boolean[n];
""", """        lake = new boolean[n];
        lakeLevel = new float[n];
"""),
("""    public boolean isLake(int g) {
        return lake[g];
    }""",
 """    public boolean isLake(int g) {
        return lake[g];
    }

    /** Lake surface, relative to sea level; only meaningful where isLake. */
    public float lakeLevel(int g) {
        return lakeLevel[g];
    }

    /** Metres per year a lake loses at this cell, by today's annual mean temperature. */
    private float evaporation(int g) {
        float t = Climate.seaLevelTemperature(Climate.latitude(g / w, h)) + warmth[g] - Climate.LAPSE_RATE * Planet.metres(Math.max(elevation[g], 0));
        return MIN_EVAPORATION + EVAPORATION_PER_DEGREE * Math.max(t, 0);
    }"""),
("""    /** A filled basin holds a lake only if inflow keeps up with evaporation; otherwise it's a dry salt flat. */
    // ponytail: constant evaporation; scale it with temperature if cold lakes look too rare
    private void markLakes() {""",
 """    /**
     * A basin's lake grows until evaporation off its surface uses up the water flowing in. If inflow keeps up with
     * the whole basin, it fills to the brim and spills; otherwise it shrinks into the deepest part and has no outlet
     * (Great Salt Lake, the Caspian). Too little water at all and the basin is a dry salt flat.
     */
    // ponytail: evaporation by temperature alone; dry air evaporates more, add aridity if desert lakes look too big
    private void markLakes() {"""),
("""            if (inflow >= LAKE_EVAPORATION * size)
                for (int i = 0; i < size; i++) lake[basin[i]] = true;""",
 """            // Fill from the deepest cell up while the inflow still covers the evaporation.
            Integer[] byDepth = new Integer[size];
            for (int i = 0; i < size; i++) byDepth[i] = basin[i];
            java.util.Arrays.sort(byDepth, (a, b) -> Float.compare(elevation[a], elevation[b]));
            float lost = 0;
            int filled = 0;
            while (filled < size && lost + evaporation(byDepth[filled]) <= inflow) lost += evaporation(byDepth[filled++]);
            // A full basin sits at its spill level; a shrunken lake at the rim of the next cell up, which encloses it.
            float level = filled == size ? surface[byDepth[0]] : elevation[byDepth[filled]];
            for (int i = 0; i < filled; i++) {
                lake[byDepth[i]] = true;
                lakeLevel[byDepth[i]] = level;
            }
            sink[outlet] = lost;"""),
("""            float inflow = 0; // the basin's lowest-draining cell gathers everything that enters it""",
 """            float inflow = 0; // the basin's lowest-draining cell gathers everything that enters it
            int outlet = start;"""),
("""                inflow = Math.max(inflow, flow[c]);""",
 """                if (flow[c] > inflow) { inflow = flow[c]; outlet = c; }"""),
("""        markLakes();
    }""",
 """        markLakes();
        accumulate(); // lakes keep what they evaporate: below a lake with no outlet, the river is gone
    }"""),
("""        Arrays.fill(flow, 0);
        for (int i = count - 1; i >= 0; i--) {
            int c = order[i];
            if (elevation[c] >= 0) flow[c] += rainMm[c] / 1000;
            if (receiver[c] >= 0) flow[receiver[c]] += flow[c];
        }""",
 """        accumulate();
    }

    /** Rain gathered downstream, minus what each lake evaporates where its basin drains. */
    private void accumulate() {
        Arrays.fill(flow, 0);
        for (int i = count - 1; i >= 0; i--) {
            int c = order[i];
            if (elevation[c] >= 0) flow[c] += rainMm[c] / 1000;
            flow[c] = Math.max(0, flow[c] - sink[c]);
            if (receiver[c] >= 0) flow[receiver[c]] += flow[c];
        }"""),
("""        lakeLevel = new float[n];
""", """        lakeLevel = new float[n];
        sink = new float[n];
"""),
("""    private final float[] lakeLevel; // lake surface where lake is true: the spill level, or lower in a shrunken lake""",
 """    private final float[] lakeLevel; // lake surface where lake is true: the spill level, or lower in a shrunken lake
    private final float[] sink;      // metres per year a lake evaporates, taken from the flow at its basin's outlet"""),
])
edit('src/main/java/com/realtectonics/sim/PlanetData.java', [
("""            d.lakeMetres[g] = hy.isLake(g) ? Planet.metres(hy.surface[g]) : Float.NaN;""",
 """            d.lakeMetres[g] = hy.isLake(g) ? Planet.metres(hy.lakeLevel(g)) : Float.NaN;"""),
])
print("ok")
