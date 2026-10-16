exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
# troughs: deepest cut of all nearby
("""            if (t < 1) {
                ground = Math.min(ground, trough(vy + 1, ground, t));""",
 """            if (allTroughs) ground = Math.min(ground, troughCut(u, v, ground, width)); // every trough near, so overlaps blend
            if (t < 1) {
                if (!allTroughs) ground = Math.min(ground, trough(vy + 1, ground, t));"""),
("""    /** Blocks from a trough's axis to where its walls meet the land, before widening for depth. */""",
 """    /**
     * The ground after every glacial trough within reach has cut it: the deepest cut wins. Taking only the nearest
     * trough left a wall where two wide troughs overlap and the nearest switches from one to the other.
     */
    private int troughCut(double u, double v, int ground, float width) {
        int iu = (int) Math.round(u), iv = (int) Math.round(v), cut = ground;
        for (int dy = -3; dy <= 3; dy++)
            for (int dx = -3; dx <= 3; dx++) {
                int cy = iv + dy;
                if (cy < 0 || cy >= h) continue;
                int c = cell(iu + dx, cy), r = data.receiver[c];
                if (r < 0 || data.metres[c] < 0 || data.ice[c] < Hydrology.GLACIATED || data.flow[c] <= Hydrology.GLACIER_AREA) continue;
                double au = iu + dx, su = wrap(r % w - c % w), sv = r / w - cy;
                double t = Math.clamp(((u - au) * su + (v - cy) * sv) / (su * su + sv * sv), 0, 1);
                float dist = (float) Math.hypot(u - au - t * su, v - cy - t * sv) * blocksPerCell;
                float floor = Math.max(groundY[c] + (float) t * (groundY[r] - groundY[c]), SEA_Y - 1) + 1;
                float base = Math.min(64, 40 + 0.3f * (float) Math.sqrt(data.flow[c])) * width;
                float wide = Math.min(TROUGH_MAX, Math.max(1, TROUGH_SLOPE * (ground - floor) / base));
                float f = dist / (base * wide);
                if (f < 1) cut = Math.min(cut, trough(floor, ground, f));
            }
        return cut;
    }

    /** Blocks from a trough's axis to where its walls meet the land, before widening for depth. */"""),
# lakes: nearest lake's level, sills between lakes at different levels
("""        float ly = lakeness > 0.02f ? lakeLevel(iu, iv) : Float.NaN;
        if (!Float.isNaN(ly)) {""",
 """        float ly = lakeness > 0.02f ? lakeLevel(iu, iv) : Float.NaN, sill = Float.NaN;
        if (nearestLakes && !Float.isNaN(ly)) {
            float[] near = nearestLake(u, v);
            ly = near[0];
            sill = near[1];
        }
        if (!Float.isNaN(sill)) ground = Math.max(ground, Math.round(sill)); // the rock lip holding a higher lake above a lower one
        else if (!Float.isNaN(ly)) {"""),
("""    /** Highest lake surface within two cells: one level per lake, shared by its water and its shores. */""",
 """    /**
     * From format 28: the level of the nearest lake cell, so where two lakes at different levels lie close each keeps
     * its own; and the level of a higher lake when this spot is on the narrow sill between them (else NaN).
     */
    private float[] nearestLake(double u, double v) {
        int iu = (int) Math.round(u), iv = (int) Math.round(v);
        float level = Float.NaN, high = Float.NaN;
        double best = Double.MAX_VALUE, bestHigh = Double.MAX_VALUE;
        for (int dy = -2; dy <= 2; dy++)
            for (int dx = -2; dx <= 2; dx++) {
                float l = lakeY[cell(iu + dx, iv + dy)];
                if (Float.isNaN(l)) continue;
                double d = Math.hypot(iu + dx - u, iv + dy - v);
                if (d < best) { best = d; level = l; }
            }
        for (int dy = -2; dy <= 2; dy++)
            for (int dx = -2; dx <= 2; dx++) {
                float l = lakeY[cell(iu + dx, iv + dy)];
                double d = Math.hypot(iu + dx - u, iv + dy - v);
                if (!Float.isNaN(l) && l > level + 1 && d < bestHigh) { bestHigh = d; high = l; }
            }
        return new float[]{level, bestHigh - best < LAKE_SILL ? high : Float.NaN};
    }

    /** Highest lake surface within two cells: one level per lake, shared by its water and its shores. */"""),
("""    private final boolean iceSheets;      // glacier ice as one smooth surface over the land, merging and sloping to its edges: format 27""",
 """    private final boolean iceSheets;      // glacier ice as one smooth surface over the land, merging and sloping to its edges: format 27
    private final boolean allTroughs, nearestLakes; // every trough cuts, so overlaps blend; lakes keep their own level, sills between: format 28"""),
("""        iceSheets = data.format >= 27;""", """        iceSheets = data.format >= 27;
        allTroughs = nearestLakes = data.format >= 28;"""),
("""    static final float ICE_SUMMER_MAX = 15; // °C: summers this warm melt any glacier tongue that reaches them""",
 """    static final float ICE_SUMMER_MAX = 15; // °C: summers this warm melt any glacier tongue that reaches them
    static final float LAKE_SILL = 0.25f;   // cells: width of the rock sill between two lakes at different levels"""),
])
print("ok")
