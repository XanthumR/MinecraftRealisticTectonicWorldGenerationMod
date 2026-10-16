exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""        // Ponds: small hollows in wet, level ground hold rainwater. The level sits a block under the lowest point of the
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
                    float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;""",
 """        // Ponds: small hollows in wet, level ground hold rainwater. The level sits a block under the lowest point of the
        // rim, only level rims get one, and a low berm closes the rim, so a pond never spills. Everything that decides
        // whether a pond is there is read at its middle, so each pond is whole or absent.
        if (folds && water == Column.NO_WATER && lava == Column.NO_WATER && ice == Column.NO_WATER && rain > POND_RAIN) {
            int gx = (int) Math.floor(x / POND_GRID), gz = (int) Math.floor(z / POND_GRID), cells = span / POND_GRID;
            for (int dz = -1; dz <= 1; dz++)
                for (int dx = -1; dx <= 1; dx++) {
                    int key = Math.floorMod(gx + dx, cells) * 6007 + (gz + dz) * 92639 + 17; // repeats around the planet
                    if (hash01(key) > POND_CHANCE) continue;
                    double px = (gx + dx + 0.2 + 0.6 * hash01(key + 1)) * POND_GRID, pz = (gz + dz + 0.2 + 0.6 * hash01(key + 2)) * POND_GRID;
                    double d = Math.hypot(wrapBlocks(x - px), z - pz), radius = (4 + 6 * hash01(key + 3)) / zoom;
                    if (d >= radius + POND_BERM) continue;
                    double cu = warpU(px, pz), cv = warpV(px, pz);
                    float wet = bilinear(data.precipitation, cu, cv);
                    if (hash01(key) > POND_CHANCE * smoothstep((wet - POND_RAIN) / 800) || cubic(lakes, cu, cv) > 0.01f
                            || !conesNear(px, pz).isEmpty() || riverAt(px, pz, cu, cv).edge < radius + POND_BERM + 4) continue;
                    float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;"""),
("""                    if (hi - lo > POND_LEVEL) continue;
                    int level = Math.round(lo) - 1, depth = 2 + Math.round(2 * hash01(key + 4));
                    ground = Math.min(ground, level - Math.max(1, Math.round(depth * (float) Math.sqrt(1 - d * d / (radius * radius)))));
                    water = level;
                    cover = Cover.POND;""",
 """                    if (hi - lo > POND_LEVEL || lo < SEA_Y + 4) continue;
                    int level = Math.round(lo) - 1, depth = 2 + Math.round(2 * hash01(key + 4));
                    if (d >= radius) { // the berm
                        ground = Math.max(ground, level);
                        continue;
                    }
                    ground = Math.min(ground, level - Math.max(1, Math.round(depth * (float) Math.sqrt(1 - d * d / (radius * radius)))));
                    water = level;
                    cover = Cover.POND;"""),
("""    static final float POND_RAIN = 450, POND_CHANCE = 0.3f, POND_LEVEL = 2;""",
 """    static final float POND_RAIN = 450, POND_CHANCE = 0.3f, POND_LEVEL = 2, POND_BERM = 2;"""),
])
print("ok")
