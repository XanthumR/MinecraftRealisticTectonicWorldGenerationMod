import com.realtectonics.sim.*;
// Time of the Earth planet's whole simulation at -Dwidth (4096 = Huge), to watch the climate's cost.
var s = PlanetSettings.DEFAULT.withWidth(Integer.getInteger("width", 4096)).withEarth(true);
PlanetData.generate(1, s.withWidth(512), x -> {}); long t0 = System.nanoTime();
PlanetData.generate(1, s, x -> {}); System.out.printf("generated in %.1fs%n", (System.nanoTime() - t0) / 1e9);
/exit
