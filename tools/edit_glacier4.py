exec(open('tools/edit_glacier.py').read().split("C = ")[0])
H = 'src/main/java/com/realtectonics/sim/Hydrology.java'
edit(H, [
("""    static final float FJORD_METRES = 15;       // a glacial valley this close to sea level floods into a fjord
""", """    static final float FJORD_METRES = 15;       // a glacial valley this close to sea level floods into a fjord
    static final float FJORD_RELIEF = 500;      // metres: only valleys among mountains are fjords; lowland ones stay rivers
    public static final float GLACIER_AREA = 20; // flow that gathers enough ice to carve its own trough
"""),
("""            if (ice[c] == GLACIATED && r >= 0 && Planet.metres(elevation[c]) < FJORD_METRES && (elevation[r] < 0 || ice[r] == FJORD)) ice[c] = FJORD;""",
 """            if (ice[c] == GLACIATED && r >= 0 && Planet.metres(elevation[c]) < FJORD_METRES && (elevation[r] < 0 || ice[r] == FJORD)
                    && Planet.metres(highestNear(c, 2)) >= FJORD_RELIEF) ice[c] = FJORD;"""),
("""    public boolean isLake(int g) {""",
 """    private float highestNear(int g, int radius) {
        float top = elevation[g];
        for (int dy = -radius; dy <= radius; dy++)
            for (int dx = -radius; dx <= radius; dx++) {
                int y = g / w + dy;
                if (y >= 0 && y < h) top = Math.max(top, elevation[Math.floorMod(g % w + dx, w) + y * w]);
            }
        return top;
    }

    public boolean isLake(int g) {"""),
])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""        River river = nearestRiver(u, v);
        int ry = Math.max((int) Math.floor(river.y), SEA_Y - 1), ice = Column.NO_WATER;""",
 """        River river = nearestRiver(u, v, false), valley = nearestRiver(u, v, true);
        int ry = Math.max((int) Math.floor(river.y), SEA_Y - 1), vy = Math.max((int) Math.floor(valley.y), SEA_Y - 1), ice = Column.NO_WATER;"""),
("""        if (river.ice != 0) {
            float trough = Math.min(64, 40 + 0.3f * (float) Math.sqrt(river.flow)), t = (river.edge + river.half) / trough;
            boolean fjord = river.ice == Hydrology.FJORD;
            float floor = fjord ? SEA_Y - 2 - Math.min(40, 8 + 0.1f * (float) Math.sqrt(river.flow)) : ry + 1;
            if (t < 1) ground = Math.min(ground, Math.round(floor + (ground - floor) * t * t));
            float thick = Math.min(30, (GLACIER_MELT - temperature(u, v, ry + 1)) * 6);
            if (!fjord && t < 1 && thick > 1 && bilinear(data.precipitation, u, v) >= GLACIER_MIN_RAIN)
                ice = Math.round(floor + thick * (1 - 0.5f * t * t)); // a little convex, like a real glacier's surface
        }
        float bank = 8 + 2 * river.half;
        if (river.ice != Hydrology.FJORD && river.edge < bank && natural >= SEA_Y - 1) { // at the mouth the sea takes over""",
 """        boolean inFjord = false;
        if (valley != NO_RIVER) {
            float trough = Math.min(64, 40 + 0.3f * (float) Math.sqrt(valley.flow)) * (1 + 0.3f * noise(x, z, 256, 10));
            float t = (valley.edge + valley.half) / trough;
            inFjord = valley.ice == Hydrology.FJORD && t < 1;
            float floor = inFjord ? SEA_Y - 2 - Math.min(40, 8 + 0.1f * (float) Math.sqrt(valley.flow)) : vy + 1;
            if (t < 1) ground = Math.min(ground, Math.round(floor + (ground - floor) * t * t));
            float thick = Math.min(30, (GLACIER_MELT - temperature(u, v, vy + 1)) * 6);
            if (!inFjord && t < 1 && thick > 1 && bilinear(data.precipitation, u, v) >= GLACIER_MIN_RAIN)
                ice = Math.round(floor + thick * (1 - 0.5f * t * t)); // a little convex, like a real glacier's surface
        }
        float bank = 8 + 2 * river.half;
        if (!inFjord && river.edge < bank && natural >= SEA_Y - 1) { // at the mouth the sea takes over"""),
("""    /** Segments run from each river cell to its receiver; the nearest edge wins. */
    private River nearestRiver(double u, double v) {""",
 """    /** Segments run from each river cell to its receiver; the nearest edge wins. Glacial: Ice Age valleys instead, small ones too. */
    private River nearestRiver(double u, double v, boolean glacial) {"""),
("""                if (r < 0 || data.flow[c] <= Hydrology.RIVER_AREA || data.metres[c] < 0) continue;""",
 """                if (r < 0 || data.metres[c] < 0 || (glacial ? data.ice[c] == 0 || data.flow[c] <= Hydrology.GLACIER_AREA : data.flow[c] <= Hydrology.RIVER_AREA)) continue;"""),
("""nearestRiver(warpU(x, z), warpV(x, z)).edge""", """nearestRiver(warpU(x, z), warpV(x, z), false).edge"""),
])
print("ok")
