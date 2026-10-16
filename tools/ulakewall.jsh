import com.realtectonics.sim.*;
import java.lang.reflect.*;
// Lake water (lake mask > 0.3) standing 5+ above a neighbour's top, near the player in the user's newest world.
var d = PlanetData.load(java.nio.file.Path.of("run/saves/New World (5)/realtectonics/planet.bin")); d.format = Integer.getInteger("fmt", d.format);
var t = new Terrain(d, 7616411802006901329L, 1, 1, 2);
Method wu = Terrain.class.getDeclaredMethod("warpU", double.class, double.class); wu.setAccessible(true);
Method wv = Terrain.class.getDeclaredMethod("warpV", double.class, double.class); wv.setAccessible(true);
Method cu = Terrain.class.getDeclaredMethod("cubic", float[].class, double.class, double.class); cu.setAccessible(true);
Field lk = Terrain.class.getDeclaredField("lakes"); lk.setAccessible(true); float[] L = (float[]) lk.get(t);
int px = 1214, pz = 2919, R = 2500, n = 0; java.util.List<String> at = new java.util.ArrayList<>();
for (int z = pz - R; z <= pz + R; z += 3) for (int x = px - R; x <= px + R; x += 3) { var c = t.column(x, z); if (c.water() == Terrain.Column.NO_WATER) continue;
  double u = (double) wu.invoke(t, (double) x, (double) z), v = (double) wv.invoke(t, (double) x, (double) z); if ((float) cu.invoke(t, L, u, v) < 0.3f) continue;
  for (int[] o : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) { var b = t.column(x + o[0], z + o[1]); int top = Math.max(Math.max(b.water(), b.ice()), b.ground());
    if (c.water() - top >= 5) { n++; if (at.size() < 5) at.add(x + " " + z + " " + c.water() + ">" + top + " ice " + c.ice() + "/" + b.ice()); break; } } }
System.out.println("fmt " + d.format + " lake walls " + n + " " + at);
/exit
