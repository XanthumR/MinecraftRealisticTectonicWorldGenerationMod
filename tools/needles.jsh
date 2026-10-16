import com.realtectonics.sim.*;
// Needles: high columns standing far above the ground 40 blocks around them. Format 18 against today's.
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {});
var now = new Terrain(d, 777, 1, 1, 2); d.format = 18; var old = new Terrain(d, 777, 1, 1, 2); d.format = 19;
for (var t : new Terrain[]{old, now}) { var rnd = new java.util.Random(8); int high = 0, needles = 0; double drop = 0; int[] at = null;
  for (int i = 0; i < 300000; i++) { int x = rnd.nextInt(t.circumference) - t.circumference / 2, z = rnd.nextInt(t.southPoleZ() - t.northPoleZ()) + t.northPoleZ();
    int y = t.column(x, z).ground(); if (y < Terrain.SEA_Y + 150) continue; high++; int lo = y;
    for (int k = 0; k < 8; k++) lo = Math.min(lo, t.column(x + (int) (40 * Math.cos(k * Math.PI / 4)), z + (int) (40 * Math.sin(k * Math.PI / 4))).ground());
    drop += y - lo; if (y - lo > 120) { needles++; if (at == null) at = new int[]{x, z, y, lo}; } }
  System.out.printf("high %d, mean drop in 40 blocks %.1f, drops over 120: %d %s%n", high, drop / high, needles, java.util.Arrays.toString(at)); }
/exit
