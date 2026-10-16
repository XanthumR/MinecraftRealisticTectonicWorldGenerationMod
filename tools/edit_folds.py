exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""        if (landforms && Math.abs(shore) < 0.3f) natural = coast(x, z, u, v, shore, natural);
""",
 """        if (landforms && Math.abs(shore) < 0.3f) natural = coast(x, z, u, v, shore, natural);
        if (folds && natural > SEA_Y + 2) natural += foothills(x, z, u, v, natural);
"""),
("""    /** -1 in a gully's bottom to 1 on the rib between""",
 """    /**
     * Foothills: rolling hills on the low ground under a range, higher the more the range towers over it. Fold
     * mountains: on the flanks of collision belts the rock is crumpled into long parallel ridges (Zagros, Jura,
     * Appalachian Valley and Ridge). They follow the contours of the smoothed range, wobble, and plunge out along
     * their length. Both only raise the ground, so they never open a pit that would hold or spill water.
     */
    private float foothills(double x, double z, double u, double v, float natural) {
        float low = natural - SEA_Y, onLand = smoothstep((low - 2) / 10) * (1 - smoothstep(cubic(lakes, u, v) / 0.3f));
        if (onLand <= 0) return 0;
        float bumps = Math.clamp(0.5f + 0.5f * (0.65f * fine(x, z, 128, 68) + 0.35f * fine(x, z, 64, 69)), 0, 1);
        float hills = Math.min(FOOTHILL_MAX, FOOTHILL_SHARE * Math.max(0, cubic(foothill, u, v))) * bumps * (1 - smoothstep((low - 100) / 60));
        float belt = Math.min(1, 2 * cubic(orogen, u, v));
        if (belt <= 0.05f) return onLand * hills;
        double e = 0.5;
        double gu = (cubic(rangeY, u + e, v) - cubic(rangeY, u - e, v)) / (2 * e * blocksPerCell);
        double gv = (cubic(rangeY, u, v + e) - cubic(rangeY, u, v - e)) / (2 * e * blocksPerCell);
        float spacing = (float) (FOLD_RISE / Math.max(Math.hypot(gu, gv), 1e-4)); // real blocks between ridges
        float even = smoothstep((spacing - 48) / 48) * (1 - smoothstep((spacing - 384) / 256)); // none on flats or steep walls
        double phase = cubic(rangeY, u, v) / FOLD_RISE + 0.35 * fine(x, z, 256, 70);
        float crest = (float) (0.5 + 0.5 * Math.cos(2 * Math.PI * phase)), plunge = smoothstep((fine(x, z, 512, 71) + 0.5f) / 0.8f);
        float fold = FOLD_HEIGHT * belt * even * plunge * crest * crest * smoothstep((low - 4) / 12) * (1 - smoothstep((low - 80) / 80));
        return onLand * Math.max(hills, fold + 0.5f * hills);
    }

    /** -1 in a gully's bottom to 1 on the rib between"""),
("""    private final boolean wildRivers;""", """    private final boolean wildRivers;
    private final boolean folds;          // foothills and fold mountains: format 17
    private final float[] rangeY, foothill, orogen; // per cell, for folds: heavily smoothed height, how far a range nearby towers over it, collision belts blurred"""),
("""        wildRivers = data.format >= 16;""", """        wildRivers = data.format >= 16;
        folds = data.format >= 17;"""),
("""        for (int pass = 0; pass < 2; pass++) blur3x3(lakes);
""", """        for (int pass = 0; pass < 2; pass++) blur3x3(lakes);
        rangeY = folds ? new float[w * h] : null;
        foothill = folds ? new float[w * h] : null;
        orogen = folds ? new float[w * h] : null;
        for (int g = 0; folds && g < w * h; g++) {
            rangeY[g] = Math.max(groundY[g], SEA_Y);
            orogen[g] = data.rock[g] == Rock.OROGEN.ordinal() ? 1 : 0;
            float top = groundY[g];
            for (int dy = -FOOTHILL_REACH; dy <= FOOTHILL_REACH; dy++)
                for (int dx = -FOOTHILL_REACH; dx <= FOOTHILL_REACH; dx++) top = Math.max(top, groundY[cell(g % w + dx, g / w + dy)]);
            foothill[g] = top - groundY[g];
        }
        for (int pass = 0; folds && pass < 6; pass++) blur3x3(rangeY);
        for (int pass = 0; folds && pass < 2; pass++) blur3x3(foothill);
        for (int pass = 0; folds && pass < 4; pass++) blur3x3(orogen);
"""),
("""    static final float GULLY_DEPTH = 12;""", """    static final float GULLY_DEPTH = 12;
    static final int FOOTHILL_REACH = 3;          // cells: how far away a range still raises foothills
    static final float FOOTHILL_MAX = 24, FOOTHILL_SHARE = 0.2f; // blocks of foothill relief at most, and as a share of the range above
    static final float FOLD_HEIGHT = 30, FOLD_RISE = 48; // blocks: fold ridge height; rise of the smoothed range between two ridges"""),
])
edit('src/main/java/com/realtectonics/sim/PlanetData.java', [
("""    private static final int FORMAT = 16;""", """    private static final int FORMAT = 17;"""),
("""16 wild rivers (no new data)""", """16 wild rivers, 17 foothills and folds (no new data)"""),
])
print("ok")
