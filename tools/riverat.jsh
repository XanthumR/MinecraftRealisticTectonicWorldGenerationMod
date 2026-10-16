import com.realtectonics.sim.*;
import java.lang.reflect.*;
// Which river each column belongs to, along a row: -Dcx -Dcz -Dr
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
Method wu = Terrain.class.getDeclaredMethod("warpU", double.class, double.class); wu.setAccessible(true);
Method wv = Terrain.class.getDeclaredMethod("warpV", double.class, double.class); wv.setAccessible(true);
Method ra = null; for (var m : Terrain.class.getDeclaredMethods()) if (m.getName().equals("riverAt")) ra = m; ra.setAccessible(true);
Field lk = Terrain.class.getDeclaredField("lakes"); lk.setAccessible(true);
Method cu = Terrain.class.getDeclaredMethod("cubic", float[].class, double.class, double.class); cu.setAccessible(true);
int cx = Integer.getInteger("cx"), cz = Integer.getInteger("cz"), R = Integer.getInteger("r", 4);
for (int x = cx - R; x <= cx + R; x++) { double u = (double) wu.invoke(t, (double) x, (double) cz), v = (double) wv.invoke(t, (double) x, (double) cz);
  var c = t.column(x, cz); Object r = ra.invoke(t, (double) x, (double) cz, u, v);
  System.out.printf("x %d ground %d water %d lakes %.2f %s%n", x, c.ground(), c.water(), (float) cu.invoke(t, (float[]) lk.get(t), u, v), r.toString()); }
Method cn = Terrain.class.getDeclaredMethod("conesNear", double.class, double.class); cn.setAccessible(true);
for (Object c : (java.util.List<?>) cn.invoke(t, (double) cx, (double) cz)) System.out.println("cone near: " + c.toString().substring(0, 160));
/exit
