exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""        // Karst: rain dissolves limestone from below; the roof falls in as sinkholes, or as water-filled cenotes.""",
 """        // Ponds: small hollows in wet, level ground hold rainwater. The level sits a block under the lowest point of the
        // rim, and only level rims get one, so a pond never spills down a slope.
        if (folds && water == Column.NO_WATER && lava == Column.NO_WATER && ice == Column.NO_WATER && ground > SEA_Y + 2
                && rain > POND_RAIN && river.edge > 12 && lakeness < 0.01f && conesNear(x, z).isEmpty()) {
            int gx = (int) Math.floor(x / POND_GRID), gz = (int) Math.floor(z / POND_GRID), cells = span / POND_GRID;
            for (int dz = -1; dz <= 1; dz++)
                for (int dx = -1; dx <= 1; dx++) {
                    int key = Math.floorMod(gx + dx, cells) * 6007 + (gz + dz) * 92639 + 17; // repeats around the planet
                    if (hash01(key) > POND_CHANCE * smoothstep((rain - POND_RAIN) / 800)) continue;
                    double px = (gx + dx + 0.2 + 0.6 * hash01(key + 1)) * POND_GRID, pz = (gz + dz + 0.2 + 0.6 * hash01(key + 2)) * POND_GRID;
                    double d = Math.hypot(wrapBlocks(x - px), z - pz), radius = (4 + 6 * hash01(key + 3)) / zoom;
                    if (d >= radius) continue;
                    float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
                    for (int k = 0; k < 4; k++) {
                        double rx = px + (radius + 2) * Math.cos(k * Math.PI / 2), rz = pz + (radius + 2) * Math.sin(k * Math.PI / 2);
                        float rim = surface(rx, rz, warpU(rx, rz), warpV(rx, rz), new float[3]);
                        lo = Math.min(lo, rim);
                        hi = Math.max(hi, rim);
                    }
                    if (hi - lo > POND_LEVEL) continue;
                    int level = Math.round(lo) - 1, depth = 2 + Math.round(2 * hash01(key + 4));
                    ground = Math.min(ground, level - Math.max(1, Math.round(depth * (float) Math.sqrt(1 - d * d / (radius * radius)))));
                    water = level;
                }
        }
        // Karst: rain dissolves limestone from below; the roof falls in as sinkholes, or as water-filled cenotes."""),
("""    static final int KARST_GRID = 64;""", """    static final int POND_GRID = 128;                        // blocks: at most one pond per square this size
    static final float POND_RAIN = 450, POND_CHANCE = 0.3f, POND_LEVEL = 2; // mm a year; share of squares with a pond at most; blocks a rim may tilt
    static final int KARST_GRID = 64;"""),
])
print("ok")
