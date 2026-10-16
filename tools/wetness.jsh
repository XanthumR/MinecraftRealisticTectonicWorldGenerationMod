import com.realtectonics.sim.*;
// Format 38 water and biomes. Lake effect: rain added within 3 cells of a lake (want > 0) and its largest amount.
// Green banks: share of desert columns beside big rivers (within 24 blocks of the channel, flow > 500, rain < 500 mm) at format 37
// and 38 (want lower at 38). Seed 777 at PlanetSettings.DEFAULT.
var s = PlanetSettings.DEFAULT; var p = new Planet(s.width(), s.height(), s.plates(), s.oceanFraction(), s.volcanoes(), 777);
PlanetData.tectonics(p, s.steps(), x -> {});
var cl = new Climate(p); var hy = new Hydrology(p, cl.precipitation, cl.anomaly, cl.amplitude, 40, 777, x -> {});
float[] before = cl.precipitation.clone(); cl.lakeEffect(p, hy::isLake);
int w = p.w; double near = 0; int n = 0; float most = 0;
for (int g = 0; g < before.length; g++) { most = Math.max(most, cl.precipitation[g] - before[g]); boolean byLake = false;
  for (int dy = -3; dy <= 3 && !byLake; dy++) for (int dx = -3; dx <= 3; dx++) { int y = g / w + dy; if (y >= 0 && y < p.h && hy.isLake(Math.floorMod(g % w + dx, w) + y * w)) { byLake = true; break; } }
  if (byLake && !hy.isLake(g) && p.height[g] >= Planet.SEA_LEVEL) { near += cl.precipitation[g] - before[g]; n++; } }
System.out.printf("lake effect: %.0f mm more on average within 3 cells of a lake (want > 0), at most %.0f mm%n", near / n, most);
var d = PlanetData.generate(777, s, x -> {}); int[] desert = new int[2], banks = new int[2];
for (int f = 0; f < 2; f++) { d.format = 37 + f; var t = new Terrain(d, 777, 1, 1, 2);
  for (int g = 0; g < d.metres.length; g++) { if (d.flow[g] < 500 || d.metres[g] < 0 || d.precipitation[g] > 500) continue; int[] b = t.blockOf(g);
    for (int k = -24; k <= 24; k += 4) { var c = t.column(b[0] + k, b[1]); if (c.water() != Terrain.Column.NO_WATER) continue; banks[f]++; if (c.biome() == Biome.DESERT || c.biome() == Biome.STEPPE || c.biome() == Biome.BADLANDS) desert[f]++; } } }
System.out.printf("dry columns beside big rivers that are desert, steppe or badlands: fmt 37 %d of %d, fmt 38 %d of %d (want fewer)%n", desert[0], banks[0], desert[1], banks[1]);
/exit
