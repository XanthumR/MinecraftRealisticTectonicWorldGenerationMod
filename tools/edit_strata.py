exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""        int fold = Math.round(fine(x, z, 512, 5) * (12 + 0.1f * Math.max(0, ground - SEA_Y)) + fine(x, z, 64, 6) * 4);""",
 """        int fold = Math.round(fine(x, z, 512, 5) * (12 + 0.1f * Math.max(0, ground - SEA_Y)) + fine(x, z, 64, 6) * 4);
        if (foldedStrata && folds) fold += strataFold(x, z, u, v);"""),
("""    /** -1 in a gully's bottom to 1 on the rib between: waves across the slope from a jittered grid, blended. */""",
 """    /**
     * From format 34: how far the rock layers are pushed up or down here, in blocks, where plates collided. Squeezed
     * between two plates the layers buckle into folds whose axes run along the range (the same contours the fold
     * ridges follow), tight and steep where the collision was strongest, open at the belt's edge, and crumpled
     * smaller on top, so a range shows bands dipping and arching across its faces instead of lying level.
     */
    private int strataFold(double x, double z, double u, double v) {
        float belt = Math.min(1, 3 * cubic(orogen, u, v));
        if (belt <= 0.05f) return 0;
        double phase = cubic(rangeY, u, v) / STRATA_RISE + 0.35 * fine(x, z, 256, 75);
        float folds = STRATA_FOLD * belt * (0.6f + 0.4f * fine(x, z, 512, 76)); // fold amplitude swells and dies along each axis
        return Math.round(folds * (float) Math.sin(2 * Math.PI * phase) + STRATA_CRUMPLE * belt * fine(x, z, 128, 77));
    }

    /** -1 in a gully's bottom to 1 on the rib between: waves across the slope from a jittered grid, blended. */"""),
("""    private final boolean simGullies;     // gullies cut by water running down the block terrain, not a laid pattern: format 33""",
 """    private final boolean simGullies;     // gullies cut by water running down the block terrain, not a laid pattern: format 33
    public final boolean foldedStrata;    // rock layers folded in collision belts, of varied thickness: format 34"""),
("""        simGullies = data.format >= 33;""", """        simGullies = data.format >= 33;
        foldedStrata = data.format >= 34;"""),
("""    static final float FOLD_HEIGHT = 30, FOLD_RISE = 24, FOLD_BELT_METRES = 1500; // blocks: fold ridge height, rise of the smoothed range between two ridges; metres: ranges this high get fold belts""",
 """    static final float FOLD_HEIGHT = 30, FOLD_RISE = 24, FOLD_BELT_METRES = 1500; // blocks: fold ridge height, rise of the smoothed range between two ridges; metres: ranges this high get fold belts
    static final float STRATA_FOLD = 40, STRATA_RISE = 24, STRATA_CRUMPLE = 20; // format 34, blocks: rock layers' fold amplitude, smoothed-range rise per fold, smaller crumpling"""),
])
G = 'src/main/java/com/realtectonics/mod/TectonicChunkGenerator.java'
edit(G, [
("""        int layer = Math.floorDiv(y + c.fold(), LAYER_THICKNESS), hash = (layer * 0x9E3779B1) >>> 16;""",
 """        int t = y + c.fold(), layer = RealTectonics.terrain().foldedStrata // from format 34 beds vary in thickness, as real ones do
                ? Math.floorDiv(t, STRATA_PERIOD) * STRATA_BEDS + STRATA_BED[Math.floorMod(t, STRATA_PERIOD)] : Math.floorDiv(t, LAYER_THICKNESS);
        int hash = (layer * 0x9E3779B1) >>> 16;"""),
("""    static final int LAYER_THICKNESS = 6;""",
 """    static final int LAYER_THICKNESS = 6;
    // Format 34: beds 3 to 10 blocks thick, drawn once; STRATA_BED gives the bed at each height within the repeating stack.
    static final int STRATA_PERIOD = 1024;
    static final int[] STRATA_BED = new int[STRATA_PERIOD];
    static final int STRATA_BEDS;
    static {
        java.util.Random r = new java.util.Random(34);
        int bed = 0;
        for (int y = 0; y < STRATA_PERIOD; bed++)
            for (int k = 3 + r.nextInt(8); k > 0 && y < STRATA_PERIOD; k--) STRATA_BED[y++] = bed;
        STRATA_BEDS = bed;
    }"""),
])
p = 'src/main/java/com/realtectonics/sim/PlanetData.java'; s = open(p, encoding='utf-8').read()
assert s.count('FORMAT = 33;') == 1
s = s.replace('FORMAT = 33;', 'FORMAT = 34;').replace('33 gullies cut by water at block scale; older', '33 gullies cut by water at block scale, 34 folded strata; older')
open(p, 'w', encoding='utf-8').write(s)
print("ok")
