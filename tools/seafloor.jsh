import com.realtectonics.sim.*;
// Whole-planet sea floor from a planet file's cells: depth shaded dark (deep) to pale (shallow), hillshaded, land grey.
// Prints a histogram of ocean depths. -Dplanet (default out/p777.bin) -Dname
var d = PlanetData.load(java.nio.file.Path.of(System.getProperty("planet", "out/p777.bin")));
int w = d.w, h = d.h; var img = new java.awt.image.BufferedImage(w, h, 1); int[] hist = new int[12];
for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) { float m = d.metres[x + y * w];
  float e = d.metres[(x + 1) % w + y * w], sh = (float) Math.max(0.5, Math.min(1.4, 1 + (m - e) / 400));
  int rgb;
  if (m >= 0) rgb = 0x909090;
  else { hist[(int) Math.min(11, -m / 1000)]++; float t = Math.min(1, -m / 7500); // pale cyan shelf, blue ridges, navy abyss, purple trenches
    int[] k = {0xb0f0ff, 0x60b0f0, 0x2860c0, 0x10287a, 0x5a1078}; float f = t * 4; int i = Math.min(3, (int) f); f -= i;
    int a = k[i], b = k[i + 1]; rgb = 0; for (int sft = 0; sft <= 16; sft += 8) rgb |= (int) ((a >> sft & 255) * (1 - f) + (b >> sft & 255) * f) << sft; }
  int r = (int) Math.min(255, (rgb >> 16 & 255) * sh), g = (int) Math.min(255, (rgb >> 8 & 255) * sh), b = (int) Math.min(255, (rgb & 255) * sh);
  img.setRGB(x, y, r << 16 | g << 8 | b); }
for (int i = 0; i < 12; i++) System.out.println(i + "-" + (i + 1) + " km: " + hist[i]);
javax.imageio.ImageIO.write(img, "png", new java.io.File("out/" + System.getProperty("name", "seafloor") + ".png"));
/exit
