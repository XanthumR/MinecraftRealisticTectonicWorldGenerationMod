import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
var rnd = new java.util.Random(17);
for (int i = 0; i < 5000000; i++) { int x = rnd.nextInt(t.circumference) - t.circumference / 2, z = rnd.nextInt(t.southPoleZ() - t.northPoleZ()) + t.northPoleZ();
  if (t.biomeAt(x, z) == Biome.DESERT && (t.biomeAt(x + 40, z) == Biome.STEPPE || t.biomeAt(x + 40, z) == Biome.GRASSLAND)) { System.out.println(x + " " + z); break; } }
/exit
