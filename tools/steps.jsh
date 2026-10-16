import com.realtectonics.sim.*;
for (int s : new int[]{100, 300, 600}) { long t = System.nanoTime(); var d = PlanetData.generate(777, new PlanetSettings(512, 10, 0.7f, 1, 1, 1, s, 2), x -> {});
  int land = 0; float peak = 0; for (int g = 0; g < d.w*d.h; g++) if (d.metres[g] >= 0) { land++; peak = Math.max(peak, d.metres[g]); }
  System.out.printf("steps %d: %.1fs, land %.1f%%, highest %.0f m%n", s, (System.nanoTime() - t) / 1e9, 100.0 * land / (d.w*d.h), peak); }
/exit
