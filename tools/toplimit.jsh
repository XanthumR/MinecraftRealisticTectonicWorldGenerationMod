import com.realtectonics.sim.*;
// No column may reach past the build limit: sample the user's planet near its highest cells.
var d = PlanetData.load(java.nio.file.Path.of("run/saves/New World/realtectonics/planet.bin"));
var t = new Terrain(d, -6439041022017931045L, 2f, 1, 2.5f);
var rnd = new java.util.Random(1);
int over = 0;
int high = 0;
int top = 0;
for (int i = 0; i < 3000000; i++) { int x = rnd.nextInt(t.circumference) - t.circumference / 2, z = rnd.nextInt(t.southPoleZ() - t.northPoleZ()) + t.northPoleZ();
  var c = t.column(x, z); int m = Math.max(Math.max(c.ground(), c.water()), Math.max(c.lava(), c.ice())); top = Math.max(top, m); if (m > 700) high++; if (m > Terrain.MAX_Y - 1) over++; }
System.out.println("highest top " + top + ", columns above 700: " + high + ", past the limit: " + over);
/exit
