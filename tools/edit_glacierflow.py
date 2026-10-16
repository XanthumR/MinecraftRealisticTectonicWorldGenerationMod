exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
# trough width grows with depth
("""            float width = 1 + 0.3f * noise(x, z, 256, 10), t = troughFraction(valley, width);""",
 """            float width = 1 + 0.3f * noise(x, z, 256, 10), t = troughFraction(valley, width);
            if (glacierFlow && valley != NO_RIVER) { // a deep trough is a wide one: walls steep, not sheer
                float wide = Math.min(TROUGH_MAX, Math.max(1, TROUGH_SLOPE * (ground - vy - 1) / (troughWidth(valley) * width)));
                t /= wide;
            }"""),
# simulated ice thickness
("""                float thick = Math.min(30, (GLACIER_MELT - temperature(u, v, vy + 1)) * 6);
                if (valley.ice != Hydrology.FJORD && thick > 1 && bilinear(data.precipitation, u, v) >= GLACIER_MIN_RAIN)""",
 """                float thick = glacierFlow ? glacier[valley.cell] + valley.t * (glacier[Math.max(0, data.receiver[valley.cell])] - glacier[valley.cell])
                        : Math.min(30, (GLACIER_MELT - temperature(u, v, vy + 1)) * 6);
                if (valley.ice != Hydrology.FJORD && thick > 1 && (glacierFlow || bilinear(data.precipitation, u, v) >= GLACIER_MIN_RAIN))"""),
("""    private static float troughFraction(River valley, float width) {
        return valley == NO_RIVER ? 1 : (valley.edge + valley.half) / (Math.min(64, 40 + 0.3f * (float) Math.sqrt(valley.flow)) * width);
    }""",
 """    private static float troughFraction(River valley, float width) {
        return valley == NO_RIVER ? 1 : (valley.edge + valley.half) / (troughWidth(valley) * width);
    }

    /** Blocks from a trough's axis to where its walls meet the land, before widening for depth. */
    private static float troughWidth(River valley) {
        return Math.min(64, 40 + 0.3f * (float) Math.sqrt(valley.flow));
    }

    /**
     * Today's glaciers, by mass balance. Above the snow line each cell gains ice from its snowfall; below it the ice
     * melts, faster the warmer it is. Ice flows downhill along the drainage, adding up from every cell above, so a
     * glacier's tongue reaches down its valley as far as its supply outlasts the melt. Thickness in blocks per cell.
     */
    private float[] glaciers() {
        float[] flux = new float[w * h], thick = new float[w * h];
        long[] order = new long[w * h];
        for (int g = 0; g < order.length; g++) { // highest first: sort by elevation, keeping the cell in the low bits
            int bits = Float.floatToIntBits(data.metres[g]);
            order[g] = (long) (bits ^ (bits >> 31) & 0x7fffffff) << 32 | g;
        }
        java.util.Arrays.sort(order);
        for (int i = order.length - 1; i >= 0; i--) {
            int g = (int) order[i];
            if (data.metres[g] < 0) continue;
            float t = data.temperature[g], snow = data.precipitation[g] / 1000 * Math.clamp((ICE_LINE - t) / 4, 0, 1);
            float balance = t < ICE_LINE ? snow : -ICE_MELT * (t - ICE_LINE); // metres of ice a year
            flux[g] = Math.max(0, flux[g] + balance);
            if (flux[g] <= 0) continue;
            thick[g] = Math.min(ICE_MAX, ICE_THICK * (float) Math.pow(flux[g], 0.4));
            int r = data.receiver[g];
            if (r >= 0 && data.metres[r] >= 0) flux[r] += flux[g];
        }
        return thick;
    }"""),
("""    private final boolean simCalderas;    // calderas simulated into the planet's cells, not stamped per block: format 22""",
 """    private final boolean simCalderas;    // calderas simulated into the planet's cells, not stamped per block: format 22
    private final boolean glacierFlow;    // glaciers by mass balance flowing down their valleys, troughs widening with depth: format 23
    private final float[] glacier;        // per cell: today's glacier ice, blocks thick (format 23)"""),
("""        simCalderas = data.format >= 22;""", """        simCalderas = data.format >= 22;
        glacierFlow = data.format >= 23;"""),
("""        if (landforms) glacialForms();""", """        if (landforms) glacialForms();
        glacier = glacierFlow ? glaciers() : null;"""),
("""    static final float MOUTH_SHORE = 0.2f;""", """    static final float ICE_LINE = -4;      // °C annual mean at a cell: colder, snow outlasts the summer and feeds glaciers
    static final float ICE_MELT = 0.7f;    // metres of ice melted a year per °C above that
    static final float ICE_THICK = 8, ICE_MAX = 40; // blocks of ice per (metres a year of ice flow)^0.4; thickest glacier
    static final float TROUGH_SLOPE = 1, TROUGH_MAX = 1.5f; // trough half-width per block of depth (rim walls ~2 up per 1 across); most widening, inside the 3-cell lookup
    static final float MOUTH_SHORE = 0.2f;"""),
])
edit('src/main/java/com/realtectonics/sim/PlanetData.java', [
("""    private static final int FORMAT = 22;""", """    private static final int FORMAT = 23;"""),
("""22 simulated calderas (no new data)""", """22 simulated calderas, 23 flowing glaciers (no new data)"""),
])
print("ok")
