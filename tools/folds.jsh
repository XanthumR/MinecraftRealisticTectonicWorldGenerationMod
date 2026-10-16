import com.realtectonics.sim.*;
// Hillshade of a collision belt's flank, format 16 (left) against today's (right): foothills and fold ridges.
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {});
var now = new Terrain(d, 777, 1, 1, 2);
int w = d.w, pick = -1; var rnd = new java.util.Random(Long.getLong("pick", 1));
for (int tries = 0; tries < 1000000 && pick < 0; tries++) {
    int g = rnd.nextInt(w * d.h);
    if (d.rock[g] != Rock.OROGEN.ordinal() || d.metres[g] < 200 || d.metres[g] > 900) continue;
    if (d.metres[(g + 4) % (w * d.h)] < 300 || d.metres[Math.floorMod(g - 4, w * d.h)] < 300) pick = g;
}
int cx = Integer.getInteger("cx", Math.floorMod(pick % w - now.spawnCell % w + w / 2, w) * 32 - w * 16), cz = Integer.getInteger("cz", (pick / w - now.spawnCell / w) * 32);
System.out.println("spot " + cx + " " + cz);
d.format = 16; var old = new Terrain(d, 777, 1, 1, 2); d.format = 17;
int n = 2048, s = 4, W = n / s;
var img = new java.awt.image.BufferedImage(2 * W + 8, W, 1);
for (int k = 0; k < 2; k++) {
    var t = k == 0 ? old : now; int[] hgt = new int[W * W]; var cs = new Terrain.Column[W * W];
    java.util.stream.IntStream.range(0, W).parallel().forEach(j -> { for (int i = 0; i < W; i++) cs[i + j * W] = t.column(cx - n / 2 + i * s, cz - n / 2 + j * s); });
    for (int j = 1; j < W; j++) for (int i = 1; i < W; i++) {
        var c = cs[i + j * W]; int y = c.ground();
        double sh = Math.clamp(0.6 + 0.25 * ((y - cs[i - 1 + j * W].ground()) - (y - cs[i + (j - 1) * W].ground())) * -1 + 0.002 * (y - 64), 0.15, 1.2);
        int rgb = c.water() != Terrain.Column.NO_WATER && c.water() >= y ? 0x3c78dc : c.biome().color;
        int r = (int) Math.min(255, (rgb >> 16 & 255) * sh), gg = (int) Math.min(255, (rgb >> 8 & 255) * sh), b = (int) Math.min(255, (rgb & 255) * sh);
        img.setRGB(i + k * (W + 8), j, r << 16 | gg << 8 | b);
    }
}
javax.imageio.ImageIO.write(img, "png", new java.io.File("out/folds" + Long.getLong("pick", 1) + ".png"));
/exit
