import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777);
long s = System.nanoTime(); t.mapColors(false); System.out.printf("medium map drawn in %.2fs%n", (System.nanoTime() - s) / 1e9);
/exit
