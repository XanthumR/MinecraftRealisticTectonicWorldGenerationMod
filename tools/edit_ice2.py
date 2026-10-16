exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""    public enum Cover { NONE, SALT, GRAVEL, SINTER, SPRING, CLIFF, BANK, SCREE, POND }""",
 """    public enum Cover { NONE, SALT, GRAVEL, SINTER, SPRING, CLIFF, BANK, SCREE, POND, BARE_ICE, MORAINE } // the last two on glacier ice, from format 40"""),
("""    static final float ICE_STEEP = 0.6f;  // format 40: blocks per block (~31 degrees) above which a cell's snow avalanches down instead of building ice""",
 """    static final float ICE_STEEP = 0.6f;  // format 40: blocks per block (~31 degrees) above which a cell's snow avalanches down instead of building ice
    static final float ICE_BARE = 1;      // format 40: °C summer mean at the ice surface above which the winter's snow melts off it: the ablation zone
    static final int MORAINE_SPACING = 64; // format 40: real blocks between the debris stripes down a glacier's lower reaches
    static final float MORAINE_RIB = 0.9f; // format 40: how narrow those stripes are (wave crest above this)"""),
("""        if (ice <= ground || lava != Column.NO_WATER) ice = Column.NO_WATER;""",
 """        if (ice <= ground || lava != Column.NO_WATER) ice = Column.NO_WATER;
        // From format 40, below the snow line on the ice the summer melts the snow off and the glacier shows bare blue
        // ice, streaked with the rock it carries: dark stripes running down the flow where the debris of each valley wall
        // joined the ice (medial moraines), and a rubble band along its thin edges (lateral moraines).
        if (flowingIce && iceSheets && ice != Column.NO_WATER && temperature(u, v, ice) + bilinear(data.amplitude, u, v) > ICE_BARE) {
            double e = 0.05;
            double gu = cubic(iceTop, u + e, v) - cubic(iceTop, u - e, v), gv = cubic(iceTop, u, v + e) - cubic(iceTop, u, v - e), len = Math.hypot(gu, gv);
            boolean medial = len > 1e-6 && gullyWaves(x, z, -gv / len, gu / len, MORAINE_SPACING, 90) > MORAINE_RIB;
            cover = medial || ice - ground <= 2 ? Cover.MORAINE : Cover.BARE_ICE;
        }"""),
])
G = 'src/main/java/com/realtectonics/mod/TectonicChunkGenerator.java'
edit(G, [
("""            PACKED_ICE = Blocks.PACKED_ICE.defaultBlockState(), BLUE_ICE = Blocks.BLUE_ICE.defaultBlockState();""",
 """            PACKED_ICE = Blocks.PACKED_ICE.defaultBlockState(), BLUE_ICE = Blocks.BLUE_ICE.defaultBlockState(),
            MORAINE = Blocks.COBBLED_DEEPSLATE.defaultBlockState(); // rock debris riding on glacier ice (format 40)"""),
("""                            : glacier ? (c.ice() - y >= BLUE_ICE_DEPTH ? BLUE_ICE : PACKED_ICE)""",
 """                            : glacier ? (y == c.ice() && c.cover() == Terrain.Cover.MORAINE ? MORAINE
                                    : c.ice() - y >= BLUE_ICE_DEPTH || y == c.ice() && c.cover() == Terrain.Cover.BARE_ICE ? BLUE_ICE : PACKED_ICE)"""),
("""        BARE.add(Blocks.STONE);""", """        BARE.add(Blocks.STONE);
        BARE.add(Blocks.BLUE_ICE); // bare glacier ice below the snow line: only there is blue ice the top block (format 40)
        BARE.add(Blocks.COBBLED_DEEPSLATE); // moraine on that ice"""),
])
print("ok")
