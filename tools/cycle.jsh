import com.realtectonics.sim.*;
// Supercontinent cycle: the plates of seed -Dseed (777) at PlanetSettings.DEFAULT, land white, sea shaded by crust,
// snapshots every -Devery steps (60) stacked top to bottom in out/cycle.png, quarter size. Prints, per snapshot, how
// gathered the continents are: mean distance of continental cells from their centre, in cells (low = a supercontinent).
long seed = Long.getLong("seed", 777); int every = Integer.getInteger("every", 60), steps = Integer.getInteger("steps", PlanetSettings.DEFAULT.steps());
var s = PlanetSettings.DEFAULT; var p = new Planet(s.width(), s.height(), s.plates(), s.oceanFraction(), s.volcanoes(), seed); p.plan(steps);
int w = p.w, h = p.h, q = 4, shots = steps / every + 1; var img = new java.awt.image.BufferedImage(w / q, h / q * shots, 1);
for (int i = 0, shot = 0; i <= steps; i++) {
  if (i % every == 0) {
    double cs = 0, sn = 0, cy = 0; int n = 0;
    for (int g = 0; g < w * h; g++) if (p.height[g] >= Planet.SEA_LEVEL) { double a = 2 * Math.PI * (g % w) / w; cs += Math.cos(a); sn += Math.sin(a); cy += g / w; n++; }
    double cx = Math.floorMod((long) (Math.atan2(sn, cs) * w / (2 * Math.PI)), w); cy /= n; double spread = 0;
    for (int g = 0; g < w * h; g++) if (p.height[g] >= Planet.SEA_LEVEL) spread += Math.hypot(Math.floorMod((long) (g % w - cx + w / 2), w) - w / 2, g / w - cy) / n;
    System.out.printf("step %d: land %.1f%%, spread %.0f cells%n", i, 100.0 * n / (w * h), spread);
    for (int y = 0; y < h / q; y++) for (int x = 0; x < w / q; x++) { float c = p.height[x * q + y * q * w];
      int rgb = c >= Planet.SEA_LEVEL ? (c > 6 ? 0x8a6a4a : 0xe8e8d8) : (int) (60 + 80 * Math.min(1, (c - 1) / 0.8f)) | 0x102040;
      img.setRGB(x, y + shot * (h / q), rgb); }
    shot++;
  }
  if (i < steps) p.step();
}
javax.imageio.ImageIO.write(img, "png", new java.io.File("out/" + System.getProperty("name", "cycle") + ".png"));
/exit
