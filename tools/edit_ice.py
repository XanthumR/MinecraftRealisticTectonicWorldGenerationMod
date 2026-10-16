exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""    static final float ICE_THICK = 8, ICE_MAX = 40; // blocks of ice per (metres a year of ice flow)^0.4; thickest glacier""",
 """    static final float ICE_THICK = 8, ICE_MAX = 40; // blocks of ice per (metres a year of ice flow)^0.4; thickest glacier
    static final float ICE_SHED = 5;      // format 40: blocks of that thickness ice needs before it stays: thin snowfields on open slopes melt or blow away
    static final float ICE_STEEP = 0.6f;  // format 40: blocks per block (~31 degrees) above which a cell's snow avalanches down instead of building ice"""),
("""    private final boolean wetBanks;       // rivers green their banks; active craters melt the ice around them: format 38""",
 """    private final boolean wetBanks;       // rivers green their banks; active craters melt the ice around them: format 38
    private final boolean flowingIce;     // ice gathers in valleys fed from above, steep ground sheds it, the surface slopes smoothly: format 40"""),
("""        wetBanks = data.format >= 38;""", """        wetBanks = data.format >= 38;
        flowingIce = data.format >= 40;"""),
("""            flux[g] = Math.max(0, flux[g] + balance);
            if (flux[g] <= 0) continue;
            thick[g] = Math.min(ICE_MAX, ICE_THICK * (float) Math.pow(flux[g], 0.4));
            int r = data.receiver[g];""",
 """            flux[g] = Math.max(0, flux[g] + balance);
            if (flux[g] <= 0) continue;
            int r = data.receiver[g];
            // From format 40 ice lies where it is fed: snow on steep ground slides down to the cell below as avalanches,
            // and a thin sheet on an open slope does not last, so ice gathers in the valleys and hollows and the ridges
            // and walls between them stand bare.
            boolean steep = flowingIce && r >= 0 && (groundY[g] - groundY[r]) / blocksPerCell > ICE_STEEP;
            thick[g] = steep ? 0 : Math.min(ICE_MAX, Math.max(0, ICE_THICK * (float) Math.pow(flux[g], 0.4) - (flowingIce ? ICE_SHED : 0)));"""),
("""            iceTop[g] = glacier[g] > 1 ? Math.min(groundY[g] + glacier[g], Float.isNaN(lakeY[g]) ? Float.MAX_VALUE : lakeY[g]) : groundY[g] - ICE_EDGE;""",
 """            iceTop[g] = glacier[g] > 1 ? Math.min(groundY[g] + glacier[g], Float.isNaN(lakeY[g]) ? Float.MAX_VALUE : lakeY[g]) : groundY[g] - ICE_EDGE;
        // From format 40 the ice surface is smoothed over the ice itself: ice flows, so its surface slopes evenly down
        // the valley instead of doming over each cell's centre.
        for (int pass = 0; flowingIce && iceSheets && pass < 3; pass++) {
            float[] next = iceTop.clone();
            for (int g = 0; g < w * h; g++) {
                if (glacier[g] <= 1) continue;
                float sum = 0, n = 0;
                for (int dy = -1; dy <= 1; dy++)
                    for (int dx = -1; dx <= 1; dx++) {
                        int c = cell(g % w + dx, g / w + dy);
                        if (glacier[c] > 1) { sum += iceTop[c]; n++; }
                    }
                next[g] = Math.min(sum / n, Float.isNaN(lakeY[g]) ? Float.MAX_VALUE : lakeY[g]);
            }
            System.arraycopy(next, 0, iceTop, 0, next.length);
        }"""),
])
p = 'src/main/java/com/realtectonics/sim/PlanetData.java'; s = open(p, encoding='utf-8').read()
assert s.count('FORMAT = 39;') == 1
s = s.replace('FORMAT = 39;', 'FORMAT = 40;').replace('39 supercontinent cycle (sim only); older', '39 supercontinent cycle (sim only), 40 flowing ice; older')
open(p, 'w', encoding='utf-8').write(s)
print("ok")
