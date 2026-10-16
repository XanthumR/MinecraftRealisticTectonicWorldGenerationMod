import com.realtectonics.sim.*;
for (int w : new int[]{1024, 2048}) { long s = System.nanoTime(); PlanetData.generate(777, new PlanetSettings(w, 14, 0.7f, 1, 1, 1), x -> {}); System.out.printf("width %d: %.1fs%n", w, (System.nanoTime() - s) / 1e9); }
/exit
