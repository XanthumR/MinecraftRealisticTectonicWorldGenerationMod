import com.realtectonics.sim.*;
import java.lang.reflect.*;
var d = PlanetData.load(java.nio.file.Path.of("run/saves/New World (2)/realtectonics/planet.bin"));
var t = new Terrain(d, -5073067326693234624L, 1.1f, 4, 2.51f);
Method wu = Terrain.class.getDeclaredMethod("warpU", double.class, double.class); wu.setAccessible(true);
Method wv = Terrain.class.getDeclaredMethod("warpV", double.class, double.class); wv.setAccessible(true);
Method cu = Terrain.class.getDeclaredMethod("cubic", float[].class, double.class, double.class); cu.setAccessible(true);
Method su = Terrain.class.getDeclaredMethod("surface", double.class, double.class, double.class, double.class, float[].class); su.setAccessible(true);
Method ra = null; for (var m : Terrain.class.getDeclaredMethods()) if (m.getName().equals("riverAt")) ra = m; ra.setAccessible(true);
Field gy = Terrain.class.getDeclaredField("groundY"); gy.setAccessible(true); float[] G = (float[]) gy.get(t);
Field lk = Terrain.class.getDeclaredField("lakes"); lk.setAccessible(true); float[] L = (float[]) lk.get(t);
int X = Integer.getInteger("x"), Z0 = Integer.getInteger("z0"), Z1 = Integer.getInteger("z1");
for (int z = Z0; z <= Z1; z += 2) { double x = X / 4.0, zz = z / 4.0, u = (double) wu.invoke(t, x, zz), v = (double) wv.invoke(t, x, zz);
  Object r = ra.invoke(t, x, zz, u, v); var c = t.column(X, z);
  System.out.printf("z %d cell %.0f surface %.0f final %d water %d lakes %.2f river %s%n", z, (float) cu.invoke(t, G, u, v), (float) su.invoke(t, x, zz, u, v, new float[3]), c.ground(), c.water(), (float) cu.invoke(t, L, u, v), r.toString().split(", top")[0]); }
/exit
