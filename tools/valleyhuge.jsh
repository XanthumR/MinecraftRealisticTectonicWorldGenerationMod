import com.realtectonics.sim.*;
// Format 100 valleys on big planets: terrain build time at format 99 and 100, valley points, and heap after the build.
// -Dwidth (default 4096: Huge), -Dmedium=true for "Continents: Medium-sized". Saves the planet as out/huge777_<width>.bin
// and reuses it.
int width = Integer.getInteger("width", 4096); boolean medium = Boolean.getBoolean("medium");
var path = java.nio.file.Path.of("out/huge777_" + width + (medium ? "m" : "") + ".bin");
var s = PlanetSettings.DEFAULT;
long t0 = System.nanoTime();
PlanetData d = java.nio.file.Files.exists(path) ? PlanetData.load(path) : null;
if (d == null) { d = PlanetData.generate(777, new PlanetSettings(width, s.plates(), s.oceanFraction(), 1, 1, 1, s.steps(), s.riverWidth(), false, medium, s.cliffHeight(), s.start()), x -> {}); d.save(path); }
System.out.println("planet " + d.w + "x" + d.h + " in " + (System.nanoTime() - t0) / 1000000 + " ms");
var rt = Runtime.getRuntime();
for (int f : new int[]{99, 100}) {
  d.format = f; System.gc(); long before = rt.totalMemory() - rt.freeMemory(); t0 = System.nanoTime();
  var t = new Terrain(d, 777, 1, 1, 2);
  long ms = (System.nanoTime() - t0) / 1000000; System.gc();
  var g = Terrain.class.getDeclaredField("valleyGrid"); g.setAccessible(true); var v = g.get(t); int points = 0;
  if (v != null) { var dl = v.getClass().getDeclaredField("delta"); dl.setAccessible(true); points = ((float[]) dl.get(v)).length; }
  System.out.println("format " + f + ": " + ms + " ms, valley points " + points + ", terrain keeps " + (rt.totalMemory() - rt.freeMemory() - before) / 1048576 + " MB");
  t = null;
}
/exit
