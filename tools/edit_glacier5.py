exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""        River river = nearestRiver(u, v, false), valley = nearestRiver(u, v, true);
        int ry = Math.max((int) Math.floor(river.y), SEA_Y - 1), vy = Math.max((int) Math.floor(valley.y), SEA_Y - 1), ice = Column.NO_WATER;""",
 """        River river = nearestRiver(u, v, (byte) 0, Hydrology.RIVER_AREA), valley = nearestRiver(u, v, Hydrology.GLACIATED, Hydrology.GLACIER_AREA);
        int ry = Math.max((int) Math.floor(river.y), SEA_Y - 1), vy = Math.max((int) Math.floor(valley.y), SEA_Y - 1), ice = Column.NO_WATER;"""),
("""        boolean inFjord = false;
        if (valley != NO_RIVER) {
            float trough = Math.min(64, 40 + 0.3f * (float) Math.sqrt(valley.flow)) * (1 + 0.3f * noise(x, z, 256, 10));
            float t = (valley.edge + valley.half) / trough;
            inFjord = valley.ice == Hydrology.FJORD && t < 1;
            float floor = inFjord ? SEA_Y - 2 - Math.min(40, 8 + 0.1f * (float) Math.sqrt(valley.flow)) : vy + 1;
            if (t < 1) ground = Math.min(ground, Math.round(floor + (ground - floor) * t * t));
            float thick = Math.min(30, (GLACIER_MELT - temperature(u, v, vy + 1)) * 6);
            if (!inFjord && t < 1 && thick > 1 && bilinear(data.precipitation, u, v) >= GLACIER_MIN_RAIN)
                ice = Math.round(floor + thick * (1 - 0.5f * t * t)); // a little convex, like a real glacier's surface
        }""",
 """        // Fjords are looked up on their own, so a side valley's higher floor never fills a fjord in.
        boolean inFjord = false;
        if (valley != NO_RIVER) {
            float width = 1 + 0.3f * noise(x, z, 256, 10), t = troughFraction(valley, width);
            if (t < 1) {
                ground = Math.min(ground, trough(vy + 1, ground, t));
                float thick = Math.min(30, (GLACIER_MELT - temperature(u, v, vy + 1)) * 6);
                if (valley.ice != Hydrology.FJORD && thick > 1 && bilinear(data.precipitation, u, v) >= GLACIER_MIN_RAIN)
                    ice = Math.round(vy + 1 + thick * (1 - 0.5f * t * t)); // a little convex, like a real glacier's surface
            }
            River fjord = nearestRiver(u, v, Hydrology.FJORD, Hydrology.GLACIER_AREA);
            float tf = troughFraction(fjord, width);
            if (tf < 1) {
                inFjord = true;
                ground = Math.min(ground, trough(SEA_Y - 2 - Math.min(40, 8 + 0.1f * (float) Math.sqrt(fjord.flow)), ground, tf));
            }
        }"""),
("""    /** Segments run from each river cell to its receiver; the nearest edge wins. Glacial: Ice Age valleys instead, small ones too. */
    private River nearestRiver(double u, double v, boolean glacial) {""",
 """    /** Distance from a glacial valley's centreline as a fraction of its trough's half width; 1 and beyond is outside. */
    private static float troughFraction(River valley, float width) {
        return valley == NO_RIVER ? 1 : (valley.edge + valley.half) / (Math.min(64, 40 + 0.3f * (float) Math.sqrt(valley.flow)) * width);
    }

    /** U-shaped cross-section: flat floor, walls steepening up to the untouched ground at t = 1. */
    private static int trough(float floor, int ground, float t) {
        return Math.round(floor + (ground - floor) * t * t);
    }

    /** Segments run from each cell with at least this ice and flow to its receiver; the nearest edge wins. */
    private River nearestRiver(double u, double v, byte minIce, float minFlow) {"""),
("""                if (r < 0 || data.metres[c] < 0 || (glacial ? data.ice[c] == 0 || data.flow[c] <= Hydrology.GLACIER_AREA : data.flow[c] <= Hydrology.RIVER_AREA)) continue;""",
 """                if (r < 0 || data.metres[c] < 0 || data.ice[c] < minIce || data.flow[c] <= minFlow) continue;"""),
("""nearestRiver(warpU(x, z), warpV(x, z), false).edge""", """nearestRiver(warpU(x, z), warpV(x, z), (byte) 0, Hydrology.RIVER_AREA).edge"""),
])
print("ok")
