import com.realtectonics.sim.*;
var t = new Terrain(PlanetData.load(java.nio.file.Path.of("run/world/realtectonics/planet.bin")), 777);
String sea = null, land = null;
for (int r = 0; r < 4000 && (sea == null || land == null); r += 16) for (int x = -r; x <= r && (sea == null || land == null); x += 16) {
  int z = r; var c = t.column(x, z);
  boolean deep = true, high = true;
  for (int dx = 0; dx < 48; dx += 8) for (int dz = 0; dz < 48; dz += 8) { var o = t.column(x + dx, z + dz); deep &= o.ground() < 20; high &= o.water() == Terrain.Column.NO_WATER && o.ground() > 100; }
  if (deep && sea == null) sea = (x & ~15) + " " + (z & ~15);
  if (high && land == null) land = (x & ~15) + " " + (z & ~15);
}
System.out.println("sea " + sea); System.out.println("land " + land);
/exit
