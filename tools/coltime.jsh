import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777);
long sum = 0; for (int r = 0; r < 3; r++) { long s = System.nanoTime(); for (int z = 0; z < 1000; z++) for (int x = 0; x < 1000; x++) sum += t.column(x * 7, z * 7 - 3000).ground(); System.out.printf("1M columns: %.2fs%n", (System.nanoTime() - s) / 1e9); }
/exit
