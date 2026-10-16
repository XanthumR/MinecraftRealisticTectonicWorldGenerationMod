import com.realtectonics.sim.*;
import java.lang.reflect.*;
var d = PlanetData.load(java.nio.file.Path.of("run/saves/New World (1)/realtectonics/planet.bin"));
d.format = 29; var a = new Terrain(d, -1060870475776369119L, 1, 1, 2); d.format = 30; var b = new Terrain(d, -1060870475776369119L, 1, 1, 2);
Method wu = Terrain.class.getDeclaredMethod("warpU", double.class, double.class); wu.setAccessible(true);
Method wv = Terrain.class.getDeclaredMethod("warpV", double.class, double.class); wv.setAccessible(true);
Method cu = Terrain.class.getDeclaredMethod("cubic", float[].class, double.class, double.class); cu.setAccessible(true);
Method su = Terrain.class.getDeclaredMethod("surface", double.class, double.class, double.class, double.class, float[].class); su.setAccessible(true);
Field lk = Terrain.class.getDeclaredField("lakes"); lk.setAccessible(true); float[] L = (float[]) lk.get(b);
Method nl = Terrain.class.getDeclaredMethod("nearestLake", double.class, double.class); nl.setAccessible(true);
int z = Integer.getInteger("z", 1650);
for (int x = Integer.getInteger("x0", -3100); x <= Integer.getInteger("x1", -2760); x += Integer.getInteger("dx", 12)) {
  double u = (double) wu.invoke(b, (double) x, (double) z), v = (double) wv.invoke(b, (double) x, (double) z);
  var ca = a.column(x, z); var cb = b.column(x, z); float[] nlv = (float[]) nl.invoke(b, u, v);
  System.out.printf("x %d surface %.0f lakes %.2f ly %.0f | f29 g %d w %d | f30 g %d w %d%n", x, (float) su.invoke(b, (double) x, (double) z, u, v, new float[3]), (float) cu.invoke(b, L, u, v), nlv[0], ca.ground(), ca.water(), cb.ground(), cb.water()); }
/exit
