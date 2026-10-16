import com.realtectonics.sim.*;
// The game's map preview of a random planet (-Dseed, default 777) at PlanetSettings.DEFAULT: out/rand_map.png, plus its ice share.
long seed = Long.getLong("seed", 777); var d = PlanetData.generate(seed, PlanetSettings.DEFAULT, x -> {});
var t = new Terrain(d, seed, 1, 1, 2); int[] rgb = t.mapColors(false);
var img = new java.awt.image.BufferedImage(d.w, d.h, 1); img.setRGB(0, 0, d.w, d.h, rgb, 0, d.w);
javax.imageio.ImageIO.write(img, "png", new java.io.File("out/rand_map.png"));
int land = 0, ice = 0; for (int g = 0; g < d.w * d.h; g++) if (d.metres[g] >= 0) { land++; if (d.ice[g] != 0) ice++; }
System.out.printf("land with glacier ice %.1f%%%n", 100.0 * ice / land);
/exit
