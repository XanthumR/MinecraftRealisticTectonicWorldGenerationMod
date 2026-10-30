import com.realtectonics.sim.*;
// The game's map preview of a random planet (-Dseed, default 777) at PlanetSettings.DEFAULT: out/rand_map.png, plus its ice share.
// -Dwidth=N for another size, -Dmedium=true for Medium-sized continents: out/rand_map_<width>[_medium].png.
long seed = Long.getLong("seed", 777); int width = Integer.getInteger("width", 1024); boolean medium = Boolean.getBoolean("medium");
PlanetSettings ps = PlanetSettings.DEFAULT.withWidth(width).withMediumContinents(medium);
var d = PlanetData.generate(seed, ps, x -> {});
var t = new Terrain(d, seed, 1, 1, 2); int[] rgb = t.mapColors(false);
var img = new java.awt.image.BufferedImage(d.w, d.h, 1); img.setRGB(0, 0, d.w, d.h, rgb, 0, d.w);
javax.imageio.ImageIO.write(img, "png", new java.io.File(width == 1024 && !medium ? "out/rand_map.png" : "out/rand_map_" + width + (medium ? "_medium" : "") + ".png"));
int land = 0, ice = 0; for (int g = 0; g < d.w * d.h; g++) if (d.metres[g] >= 0) { land++; if (d.ice[g] != 0) ice++; }
System.out.printf("plates %d, land %.1f%%, land with glacier ice %.1f%%%n", ps.plateCount(), 100.0 * land / (d.w * d.h), 100.0 * ice / land);
/exit
