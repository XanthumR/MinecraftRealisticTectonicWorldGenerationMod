import com.realtectonics.sim.*;
// The Earth planet (PlanetSettings.earth): generates it at -Dwidth (1024), saves out/earth.bin, and writes the game's
// own map preview (biomes, rivers, ice) to out/earth_map.png. Run with build/resources/main on the class path.
int width = Integer.getInteger("width", 1024);
var s = PlanetSettings.DEFAULT.withWidth(width).withEarth(true); long t0 = System.nanoTime();
var d = PlanetData.generate(1, s, x -> {}); System.out.printf("generated in %.1fs%n", (System.nanoTime() - t0) / 1e9);
d.save(java.nio.file.Path.of("out/earth.bin"));
var t = new Terrain(d, 1, 1, 1, 2); int[] rgb = t.mapColors(false);
var img = new java.awt.image.BufferedImage(d.w, d.h, 1); img.setRGB(0, 0, d.w, d.h, rgb, 0, d.w);
javax.imageio.ImageIO.write(img, "png", new java.io.File("out/earth_map.png"));
int land = 0; for (float m : d.metres) if (m >= 0) land++; System.out.printf("land %.1f%%, lakes %d cells, volcanoes %d%n", 100.0 * land / d.metres.length, java.util.stream.IntStream.range(0, d.w * d.h).filter(g -> !Float.isNaN(d.lakeMetres[g])).count(), d.volcanoes.size());
/exit
