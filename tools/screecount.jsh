import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
var rnd = new java.util.Random(5); int high = 0, scree = 0; int[] at = null;
for (int i = 0; i < 400000; i++) { int x = rnd.nextInt(t.circumference) - t.circumference / 2, z = rnd.nextInt(t.southPoleZ() - t.northPoleZ()) + t.northPoleZ();
    var c = t.column(x, z); if (c.ground() < Terrain.SEA_Y + 60) continue; high++;
    if (c.cover() == Terrain.Cover.SCREE) { scree++; if (at == null || rnd.nextInt(50) == 0) at = new int[]{x, z}; } }
System.out.printf("high ground %d, scree %.1f%%, one at %s%n", high, 100.0 * scree / high, java.util.Arrays.toString(at));
/exit
