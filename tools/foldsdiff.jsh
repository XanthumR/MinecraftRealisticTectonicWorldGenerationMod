import com.realtectonics.sim.*;
// What format 17 adds: new ground minus format-16 ground, 0 black to 40+ white, over 8192 blocks; orogen tinted red.
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {});
var now = new Terrain(d, 777, 1, 1, 2);
d.format = 16; var old = new Terrain(d, 777, 1, 1, 2); d.format = 17;
int cx = Integer.getInteger("cx", 320), cz = Integer.getInteger("cz", 992), n = 8192, s = 8, W = n / s;
var img = new java.awt.image.BufferedImage(W, W, 1); int[] max = {0};
java.util.stream.IntStream.range(0, W).parallel().forEach(j -> { for (int i = 0; i < W; i++) {
    int x = cx - n / 2 + i * s, z = cz - n / 2 + j * s;
    var a = now.column(x, z); var b = old.column(x, z); int dd = a.ground() - b.ground();
    int v = Math.clamp(dd * 6, 0, 255), sea = a.ground() < 63 ? 60 : 0;
    img.setRGB(i, j, (a.rock() == Rock.OROGEN ? Math.min(255, v + 70) : v) << 16 | v << 8 | Math.min(255, v + sea));
}});
javax.imageio.ImageIO.write(img, "png", new java.io.File("out/foldsdiff.png"));
/exit
