import com.realtectonics.sim.*;
import java.lang.reflect.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
Method wu = Terrain.class.getDeclaredMethod("warpU", double.class, double.class); wu.setAccessible(true);
Method wv = Terrain.class.getDeclaredMethod("warpV", double.class, double.class); wv.setAccessible(true);
Method cu = Terrain.class.getDeclaredMethod("cubic", float[].class, double.class, double.class); cu.setAccessible(true);
Method su = Terrain.class.getDeclaredMethod("surface", double.class, double.class, double.class, double.class, float[].class); su.setAccessible(true);
Field gy = Terrain.class.getDeclaredField("groundY"); gy.setAccessible(true); float[] G = (float[]) gy.get(t);
int cx = Integer.getInteger("cx"), cz = Integer.getInteger("cz");
for (int dz = -48; dz <= 48; dz += 8) for (int dx = -48; dx <= 48; dx += 48) { double x = cx + dx, z = cz + dz, u = (double) wu.invoke(t, x, z), v = (double) wv.invoke(t, x, z);
  System.out.printf("%+3d %+3d cell %.0f surface %.0f final %d %s%n", dx, dz, (float) cu.invoke(t, G, u, v), (float) su.invoke(t, x, z, u, v, new float[3]), t.column((int) x, (int) z).ground(), t.column((int) x, (int) z).biome()); }
/exit
