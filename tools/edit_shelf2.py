exec(open('tools/edit_glacier.py').read().split("C = ")[0])
H = 'src/main/java/com/realtectonics/sim/Hydrology.java'
edit(H, [
("""        float[] breadth = Noise.fbm(w, h, w / 32, 4, new Random(seed * 13 + 5));
        for (int g = 0; g < n; g++) {
            if (elevation[g] >= 0) continue;
            float width = trench[g] <= ACTIVE_REACH ? ACTIVE_SHELF : SHELF_MIN + (SHELF_MAX - SHELF_MIN) * Math.clamp((breadth[g] - 0.5f) / 0.75f, 0, 1);
            float d = offshore[g] - 0.5f; // the shore lies half a cell out from the last land cell
            float m = d <= width ? -SHELF_DEPTH - (SHELF_BREAK - SHELF_DEPTH) * d / width : -SHELF_BREAK - SLOPE * (d - width);
            elevation[g] = Math.max(elevation[g], Planet.fromMetres(m)); // shelves only ever fill the deep floor in
        }""",
 """        float[] breadth = Noise.fbm(w, h, w / 32, 4, new Random(seed * 13 + 5)), shelf = new float[n];
        for (int g = 0; g < n; g++) {
            float width = trench[g] <= ACTIVE_REACH ? ACTIVE_SHELF : SHELF_MIN + (SHELF_MAX - SHELF_MIN) * Math.clamp((breadth[g] - 0.5f) / 0.75f, 0, 1);
            float d = Math.max(offshore[g] - 0.5f, 0); // the shore lies half a cell out from the last land cell
            shelf[g] = d <= width ? -SHELF_DEPTH - (SHELF_BREAK - SHELF_DEPTH) * d / width : -SHELF_BREAK - SLOPE * (d - width);
        }
        for (int pass = 0; pass < 2; pass++) blur(shelf); // whole-cell distances would leave the slope in steps
        for (int g = 0; g < n; g++) // shelves only ever fill the deep floor in
            if (elevation[g] < 0) elevation[g] = Math.max(elevation[g], Planet.fromMetres(Math.min(shelf[g], -SHELF_DEPTH)));"""),
("""    private boolean touchesLand(int g) {""",
 """    private void blur(float[] f) {
        float[] src = f.clone();
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                float sum = 0;
                for (int dy = -1; dy <= 1; dy++)
                    for (int dx = -1; dx <= 1; dx++) sum += src[Math.floorMod(x + dx, w) + Math.clamp(y + dy, 0, h - 1) * w];
                f[x + y * w] = sum / 9;
            }
    }

    private boolean touchesLand(int g) {"""),
])
print("ok")
