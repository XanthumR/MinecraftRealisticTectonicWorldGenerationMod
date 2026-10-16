import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
var counts = new java.util.EnumMap<Biome, Integer>(Biome.class); var rnd = new java.util.Random(2); int land = 0;
long s = System.nanoTime();
for (int i = 0; i < 400000; i++) { int x = rnd.nextInt(t.circumference) - t.circumference / 2, z = rnd.nextInt(t.southPoleZ() - t.northPoleZ()) + t.northPoleZ(); var b = t.biomeAt(x, z); counts.merge(b, 1, Integer::sum); }
System.out.printf("biomeAt %.1f us each%n", (System.nanoTime() - s) / 1e3 / 400000);
counts.forEach((b, n) -> System.out.printf("%-22s %5.2f%%%n", b, 100.0 * n / 400000));
/exit
