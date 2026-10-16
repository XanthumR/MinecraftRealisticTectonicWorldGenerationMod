exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""            lakeY[g] = (float) Math.floor(toY(data.lakeMetres[g])); // NaN stays NaN
        }
""",
 """            lakeY[g] = (float) Math.floor(toY(data.lakeMetres[g])); // NaN stays NaN
        }
        if (data.format >= 19) talus();
"""),
("""    /** Ridged massif relief""",
 """    /**
     * Talus: rock on a slope steeper than it can stand slides down until it can. The planet's cells are far wider than
     * they are tall in blocks, so a range one or two cells across turns into a needle; here the steepest cells shed
     * onto their lower neighbours, mass kept, so the range widens at its foot and its peaks settle. The limit is a real
     * slope in blocks, so wider world scales are barely touched. Rivers, lakes and the sea are left as they are, so
     * no river is dammed and no coast moves.
     */
    private void talus() {
        float limit = TALUS_SLOPE * blocksPerCell;
        boolean[] fixed = new boolean[w * h];
        for (int g = 0; g < w * h; g++)
            fixed[g] = data.metres[g] < 0 || data.flow[g] >= CREEK_AREA || !Float.isNaN(data.lakeMetres[g]);
        int[] dx = {1, -1, 0, 0, 1, 1, -1, -1}, dy = {0, 0, 1, -1, 1, -1, 1, -1};
        for (int pass = 0; pass < TALUS_PASSES; pass++) {
            boolean moved = false;
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    int g = x + y * w;
                    if (fixed[g] || groundY[g] < SEA_Y + TALUS_ABOVE) continue;
                    for (int k = 0; k < 8; k++) { // ponytail: Gauss-Seidel sweep, order-dependent but converges; fine for a one-off pass
                        int ny = y + dy[k];
                        if (ny < 0 || ny >= h) continue;
                        int n = cell(x + dx[k], ny);
                        float excess = groundY[g] - groundY[n] - limit * (k < 4 ? 1 : 1.4142f);
                        if (fixed[n] || excess <= 0) continue;
                        groundY[g] -= excess * 0.25f;
                        groundY[n] += excess * 0.25f;
                        moved = true;
                    }
                }
            if (!moved) break;
        }
    }

    /** Ridged massif relief"""),
("""    static final int FOOTHILL_REACH = 3;""", """    static final float TALUS_SLOPE = 1, TALUS_ABOVE = 20; // blocks up per block across a cell: steeper sheds (45°); land this far above the sea only
    static final int TALUS_PASSES = 60;
    static final int FOOTHILL_REACH = 3;"""),
])
edit('src/main/java/com/realtectonics/sim/PlanetData.java', [
("""    private static final int FORMAT = 18;""", """    private static final int FORMAT = 19;"""),
("""18 climate snow line (no new data)""", """18 climate snow line, 19 talus (no new data)"""),
])
print("ok")
