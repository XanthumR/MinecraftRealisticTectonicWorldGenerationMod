import com.realtectonics.sim.*;
// Format 85: blown volcanoes (horseshoe crater, debris avalanche, hummocks). Saves seed 777 to out/p85_777.bin for
// uview3d.jsh and lists the blown ones in block coordinates, with the debris lobe's middle and the hummocks' height there.
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); d.save(java.nio.file.Path.of("out/p85_777.bin"));
var t = new Terrain(d, 777, 1, 1, 2);
var of = Terrain.class.getDeclaredField("originX"); of.setAccessible(true); int ox = (int) of.get(t);
var oy = Terrain.class.getDeclaredField("originY"); oy.setAccessible(true); int oz = (int) oy.get(t);
int blown = 0;
for (Volcano v : d.volcanoes) {
  if (v.gone() || v.hotspot()) continue;
  if (!v.blown()) continue;
  blown++;
  int w = d.w; double bx = Math.IEEEremainder((v.u() - ox) * Terrain.BLOCKS_PER_CELL, w * Terrain.BLOCKS_PER_CELL), bz = (v.v() - oz) * Terrain.BLOCKS_PER_CELL;
  // the lobe's middle: the direction of most debris at one radius out
  double best = 0, ba = 0, r = v.radiusBlocks();
  for (int i = 0; i < 64; i++) { double a = i * Math.PI / 32, f = v.debris(r * Math.cos(a), r * Math.sin(a)); if (f > best) { best = f; ba = a; } }
  if (blown <= 12) System.out.printf("blown %s active %s r %.0f summit x %.0f z %.0f lobe x %.0f z %.0f%n", blown, v.active(), r, bx, bz, bx + 1.1 * r * Math.cos(ba), bz + 1.1 * r * Math.sin(ba));
}
System.out.println("blown volcanoes " + blown);
/exit
