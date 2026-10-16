import com.realtectonics.sim.*;
var t = new Terrain(PlanetData.load(java.nio.file.Path.of("run/world/realtectonics/planet.bin")), 777);
int bx = 0, bz = 0, best = Integer.MIN_VALUE;
for (int z = -9000; z <= 9000; z += 32) for (int x = -16384; x < 16384; x += 32) { var c = t.column(x, z); if (c.ground() > best) { best = c.ground(); bx = x; bz = z; } }
System.out.println("peak " + bx + " " + bz + " y=" + best + " " + t.column(bx, bz).biome());
var s = t.column(0, 0); System.out.println("spawn y=" + s.ground() + " " + s.biome());
/exit
