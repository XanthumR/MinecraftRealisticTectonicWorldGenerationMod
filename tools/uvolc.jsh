import com.realtectonics.sim.*;
// Volcanoes near the player: where they are in blocks, size, active, caldera, and the crater's relaxed shape.
var d = PlanetData.load(java.nio.file.Path.of(System.getProperty("planet"))); var t = new Terrain(d, Long.getLong("seed"), 1, 1, 2);
int px = Integer.getInteger("px"), pz = Integer.getInteger("pz");
for (Volcano v : d.volcanoes) { if (v.gone()) continue; int[] b = t.blockOf((int) v.u() + (int) v.v() * d.w);
  if (Math.abs(b[0] - px) > 700 || Math.abs(b[1] - pz) > 700) continue;
  System.out.printf("volcano at %d %d r %.0f active %s caldera %s hotspot %s age %d%n", b[0], b[1], v.radiusBlocks(), v.active(), v.caldera(), v.hotspot(), v.age()); }
/exit
