import com.realtectonics.sim.*;
// Lakes one biome (format 42): share of lake-centre columns whose biome is RIVER or FROZEN_RIVER, so the whole lake has
// one water colour (want ~100% at 42). -Dplanet -Dseed (default: generate seed 777).
var path = System.getProperty("planet"); var d = path == null ? PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}) : PlanetData.load(java.nio.file.Path.of(path));
long seed = Long.getLong("seed", 777);
for (int f : new int[]{41, 42}) { d.format = f; var t = new Terrain(d, seed, 1, 1, 2); int n = 0, river = 0;
  for (int g = 0; g < d.metres.length; g += 3) { if (Float.isNaN(d.lakeMetres[g]) || d.metres[g] < 0) continue; int[] b = t.blockOf(g); var c = t.column(b[0], b[1]);
    if (c.water() == Terrain.Column.NO_WATER || c.water() < c.ground()) continue; n++; if (c.biome() == Biome.RIVER || c.biome() == Biome.FROZEN_RIVER) river++; }
  System.out.printf("fmt %d: lake columns with the river biome %d of %d%n", f, river, n); }
/exit
