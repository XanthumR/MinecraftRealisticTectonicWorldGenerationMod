import com.realtectonics.sim.*;
// Land relief of a planet: share of land above 500/1000/2000/3000 m, the highest point, rain and plates; out/continents_plates_<width>[_medium].png.
// -Dwidth=N (default 1024), -Dmedium=true for Medium-sized continents, -Dseed=N (default 777). Medium-sized continents should match Medium.
long seed = Long.getLong("seed", 777); int width = Integer.getInteger("width", 1024); boolean medium = Boolean.getBoolean("medium");
var d = PlanetData.generate(seed, PlanetSettings.DEFAULT.withWidth(width).withMediumContinents(medium), x -> {});
int land = 0; int[] above = new int[4]; float[] at = {500, 1000, 2000, 3000}; float top = 0; double rain = 0;
for (int g = 0; g < d.w * d.h; g++) {
    if (d.metres[g] < 0) continue;
    land++; top = Math.max(top, d.metres[g]); rain += d.precipitation[g];
    for (int i = 0; i < 4; i++) if (d.metres[g] > at[i]) above[i]++;
}
System.out.printf("width %d%s: plates %d, land %.1f%%, above 500/1000/2000/3000 m: %.1f / %.1f / %.1f / %.1f %% of land, top %.0f m, mean rain %.0f mm, volcanoes %d%n",
        width, medium ? " medium" : "", d.plateVx.length, 100.0 * land / (d.w * d.h), 100.0 * above[0] / land, 100.0 * above[1] / land, 100.0 * above[2] / land, 100.0 * above[3] / land, top, rain / land, d.volcanoes.size());
var img = new java.awt.image.BufferedImage(d.w, d.h, 1); img.setRGB(0, 0, d.w, d.h, MapLayers.plates(d), 0, d.w);
javax.imageio.ImageIO.write(img, "png", new java.io.File("out/continents_plates_" + width + (medium ? "_medium" : "") + ".png"));
/exit
