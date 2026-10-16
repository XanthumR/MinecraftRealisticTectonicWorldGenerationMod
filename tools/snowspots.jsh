import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
var rnd = new java.util.Random(4); int warm = 0, cold = 0;
while (warm < 2 || cold < 2) { int x = rnd.nextInt(8000) - 4000, z = rnd.nextInt(8000) - 4000; var c = t.column(x, z);
  if (c.water() != Terrain.Column.NO_WATER || c.ice() != Terrain.Column.NO_WATER) continue; float tp = t.snowTemperature(x, z, c.ground());
  if (warm < 2 && c.biome() == Biome.WINDSWEPT_HILLS && c.ground() > 140 && tp > 2) { warm++; System.out.println("WARM " + x + " " + c.ground() + " " + z + " " + tp + " " + c.biome()); }
  if (cold < 2 && tp < -3 && c.ground() < 110 && c.biome() != Biome.ICE_CAP && c.biome() != Biome.SNOWY_PLAINS) { cold++; System.out.println("COLD " + x + " " + c.ground() + " " + z + " " + tp + " " + c.biome()); } }
/exit
