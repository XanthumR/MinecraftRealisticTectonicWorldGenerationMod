import com.realtectonics.sim.*;
// Talus: how steep the planet's cells are before (format 18) and after, and its tallest peaks.
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {});
long s = System.nanoTime(); var now = new Terrain(d, 777, 1, 1, 2); System.out.printf("terrain build %.2fs%n", (System.nanoTime() - s) / 1e9);
d.format = 18; var old = new Terrain(d, 777, 1, 1, 2); d.format = 19;
var f = Terrain.class.getDeclaredField("groundY"); f.setAccessible(true);
for (var t : new Terrain[]{old, now}) { float[] g = (float[]) f.get(t); int w = d.w, steep = 0; float top = 0; int at = 0;
  for (int i = 0; i < g.length; i++) { if (g[i] > top) { top = g[i]; at = i; } int x = i % w; if (x + 1 < w && Math.abs(g[i] - g[i + 1]) > 48) steep++; }
  int bx = Math.floorMod(at % w - t.spawnCell % w + w / 2, w) * 32 - w * 16, bz = (at / w - t.spawnCell / w) * 32;
  System.out.printf("top cell y %.0f at %d %d, cell steps over 48 blocks: %d%n", top, bx, bz, steep); }
/exit
