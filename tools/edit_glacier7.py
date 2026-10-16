exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""        int iu = (int) Math.round(u), iv = (int) Math.round(v);
        River best = NO_RIVER;
        for (int dy = -2; dy <= 2; dy++)
            for (int dx = -2; dx <= 2; dx++) {
                int cy = iv + dy;
                if (cy < 0 || cy >= h) continue;
                int c = cell(iu + dx, cy), r = data.receiver[c];""",
 """        int iu = (int) Math.round(u), iv = (int) Math.round(v), n = minIce == Hydrology.FJORD ? 3 : 2; // fjord mouths reach further
        River best = NO_RIVER;
        for (int dy = -n; dy <= n; dy++)
            for (int dx = -n; dx <= n; dx++) {
                int cy = iv + dy;
                if (cy < 0 || cy >= h) continue;
                int c = cell(iu + dx, cy), r = data.receiver[c];"""),
])
print("ok")
