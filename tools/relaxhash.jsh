import com.realtectonics.sim.*;
// Hash of relaxedColumn ground over a 2048x2048 area of seed 777 (out/p39_777.bin): must not change when only the
// rockfall tile size (Terrain.RELAX_TILE) or caching changes, since tiles are exact where they overlap. -Dfmt to time a format.
var d = PlanetData.load(java.nio.file.Path.of("out/p39_777.bin")); d.format = Integer.getInteger("fmt", d.format); var t = new Terrain(d, 777, 1, 1, 2);
long h = 0; long s = System.nanoTime();
var cs = t.relaxedColumns(-1024, -1024, 2048, 2048);
for (var c : cs) h = h * 31 + c.ground() * 7 + (c.water() & 0xffff) + c.cover().ordinal();
System.out.printf("hash %x, %.1fs%n", h, (System.nanoTime() - s) / 1e9);
/exit
