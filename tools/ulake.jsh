import com.realtectonics.sim.*;
import java.lang.reflect.*;
var d = PlanetData.load(java.nio.file.Path.of("run/saves/New World (5)/realtectonics/planet.bin")); var t = new Terrain(d, 7616411802006901329L, 1, 1, 2);
Method wu = Terrain.class.getDeclaredMethod("warpU", double.class, double.class); wu.setAccessible(true);
Method wv = Terrain.class.getDeclaredMethod("warpV", double.class, double.class); wv.setAccessible(true);
Method nl = Terrain.class.getDeclaredMethod("nearestLake", double.class, double.class); nl.setAccessible(true);
Method cu = Terrain.class.getDeclaredMethod("cubic", float[].class, double.class, double.class); cu.setAccessible(true);
Field lk = Terrain.class.getDeclaredField("lakes"); lk.setAccessible(true); Field ly = Terrain.class.getDeclaredField("lakeY"); ly.setAccessible(true);
Method ra = null; for (var m : Terrain.class.getDeclaredMethods()) if (m.getName().equals("riverAt")) ra = m; ra.setAccessible(true);
for (int x : new int[]{1229, 1230, 1231, 1232, 1233}) { int z = 2126; double u = (double) wu.invoke(t, (double) x, (double) z), v = (double) wv.invoke(t, (double) x, (double) z);
  float[] n = (float[]) nl.invoke(t, u, v); var c = t.column(x, z); var r = ra.invoke(t, (double) x, (double) z, u, v).toString();
  System.out.printf("x %d water %d lakes %.2f nearest %.0f sill %.0f cell %d,%d lakeY %.0f river %s%n", x, c.water(), (float) cu.invoke(t, (float[]) lk.get(t), u, v), n[0], n[1], Math.round(u), Math.round(v), ((float[]) ly.get(t))[(int) Math.round(u) + (int) Math.round(v) * d.w], r.substring(0, 60)); }
/exit
