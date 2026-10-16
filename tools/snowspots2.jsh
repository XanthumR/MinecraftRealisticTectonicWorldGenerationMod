import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
var rnd = new java.util.Random(4); int n = 0;
for (int i = 0; i < 3000000 && n < 3; i++) { int x = rnd.nextInt(8000) - 4000, z = rnd.nextInt(8000) - 4000; var c = t.column(x, z);
  if (c.water() != Terrain.Column.NO_WATER || c.ice() != Terrain.Column.NO_WATER || c.ground() > 112) continue; float tp = t.snowTemperature(x, z, c.ground());
  if (tp < -1 && (c.biome() == Biome.WINDSWEPT_HILLS || c.biome() == Biome.TAIGA || c.biome() == Biome.STEPPE || c.biome() == Biome.MEADOW)) { n++; System.out.println("COLDWARMBIOME " + x + " " + c.ground() + " " + z + " " + tp + " " + c.biome()); } }
/exit
