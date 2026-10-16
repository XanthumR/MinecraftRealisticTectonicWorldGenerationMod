import com.realtectonics.sim.*;
// Four panels per planet cell: seasonal swing (black 0 to white 30 °C), summer share of rain (blue wet winters, red wet
// summers), biomes from yearly means, biomes with seasons.
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {});
int w = d.w, h = d.h; var img = new java.awt.image.BufferedImage(w * 2, h * 2, 1);
var counts = new java.util.TreeMap<String, int[]>();
for (int g = 0; g < w * h; g++) { int x = g % w, y = g / w; float m = d.metres[g], t = d.temperature[g], r = d.precipitation[g], a = d.amplitude[g], s = d.summerRain[g];
  int sw = Math.clamp((int) (a / 30 * 255), 0, 255); img.setRGB(x, y, sw * 0x010101);
  int sr = Math.clamp((int) ((s - 0.5f) * 2 * 255), -255, 255); img.setRGB(x + w, y, m < 0 ? 0x202020 : sr > 0 ? sr << 16 | 0x30 : 0x30 << 8 | -sr);
  Biome b0 = Biome.classify(m, t, r, Biome.ICE_CAP_BELOW), b1 = Biome.classify(m, t, r, a, s);
  img.setRGB(x, y + h, b0.color); img.setRGB(x + w, y + h, b1.color);
  if (m >= 0) { counts.computeIfAbsent(b0.name(), k -> new int[2])[0]++; counts.computeIfAbsent(b1.name(), k -> new int[2])[1]++; } }
javax.imageio.ImageIO.write(img, "png", new java.io.File("out/seasons.png"));
counts.forEach((k, v) -> System.out.printf("%-26s yearly %6d  seasons %6d%n", k, v[0], v[1]));
float amin = 99, amax = 0; for (int g = 0; g < w * h; g++) if (d.metres[g] >= 0) { amin = Math.min(amin, d.amplitude[g]); amax = Math.max(amax, d.amplitude[g]); }
System.out.println("land swing " + amin + " .. " + amax);
/exit
