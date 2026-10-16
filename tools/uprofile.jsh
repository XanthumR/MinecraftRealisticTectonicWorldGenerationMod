import com.realtectonics.sim.*;
// Profile along a line through any planet, two formats side by side (relaxedColumn: what the game builds): ground, water, ice, cover.
// -Dplanet -Dseed -Dms -Dzoom -Drw -Dfa -Dfb (formats) -Dx0 -Dz0 -Dx1 -Dz1 -Dstep. Find which format introduced a wall or a leak.
var d = PlanetData.load(java.nio.file.Path.of(System.getProperty("planet"))); long seed = Long.getLong("seed", 777);
float ms = Float.parseFloat(System.getProperty("ms", "1")), rw = Float.parseFloat(System.getProperty("rw", "2")); int zoom = Integer.getInteger("zoom", 1);
d.format = Integer.getInteger("fa", d.format); var a = new Terrain(d, seed, ms, zoom, rw); d.format = Integer.getInteger("fb", d.format); var b = new Terrain(d, seed, ms, zoom, rw);
int x0 = Integer.getInteger("x0"), z0 = Integer.getInteger("z0"), x1 = Integer.getInteger("x1", x0), z1 = Integer.getInteger("z1", z0), step = Integer.getInteger("step", 1);
int len = Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0));
java.util.function.IntFunction<String> f = v -> v == Terrain.Column.NO_WATER ? "-" : String.valueOf(v);
for (int i = 0; i <= len; i += step) { int x = x0 + (len == 0 ? 0 : (x1 - x0) * i / len), z = z0 + (len == 0 ? 0 : (z1 - z0) * i / len);
  var ca = a.relaxedColumn(x, z); var cb = b.relaxedColumn(x, z);
  System.out.printf("%d %d | A g %d w %s ice %s %s | B g %d w %s ice %s %s%n", x, z, ca.ground(), f.apply(ca.water()), f.apply(ca.ice()), ca.cover(), cb.ground(), f.apply(cb.water()), f.apply(cb.ice()), cb.cover()); }
/exit
