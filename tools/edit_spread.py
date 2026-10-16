exec(open('tools/edit_glacier.py').read().split("C = ")[0])
edit('src/main/java/com/realtectonics/sim/Planet.java', [
("""    public final float[] spreading;  // recent new crust per cell: where plates pull apart, the axis of a mid-ocean ridge""",
 """    public final float[] spreading;  // recent new crust per cell: where plates pull apart, the axis of a mid-ocean ridge
    private final boolean[] gap;     // cells this step's new crust fills"""),
("""        spreading = new float[w * h];
        volcanic""", """        spreading = new float[w * h];
        gap = new boolean[w * h];
        volcanic"""),
("""        IntStream.range(0, h).parallel().forEach(y -> compositeRow(y, collisions));""",
 """        IntStream.range(0, h).parallel().forEach(y -> compositeRow(y, collisions));
        spreadEvenly();"""),
("""            spreading[g] = spreading[g] * SUBDUCTION_MEMORY + (spreads ? 1 : 0);""",
 """            spreading[g] = spreading[g] * SUBDUCTION_MEMORY + (spreads ? 1 : 0);
            gap[g] = spreads;"""),
("""    /** x in 0..2w-1 back into 0..w-1. */""",
 """    /**
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

    /** x in 0..2w-1 back into 0..w-1. */"""),
])
print("ok")
