import com.realtectonics.sim.*;
import java.lang.reflect.*;
// -Dplanet to check a saved planet (seed 777 otherwise). Lake water (lake mask > 0.3) standing 2+ above any neighbour's top (ground, water or ice): a leak. Should be 0.
var d = System.getProperty("planet") != null ? PlanetData.load(java.nio.file.Path.of(System.getProperty("planet"))) : PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); d.format = Integer.getInteger("fmt", d.format); var t = new Terrain(d, 777, 1, 1, 2);
Method wu = Terrain.class.getDeclaredMethod("warpU", double.class, double.class); wu.setAccessible(true);
Method wv = Terrain.class.getDeclaredMethod("warpV", double.class, double.class); wv.setAccessible(true);
Method cu = Terrain.class.getDeclaredMethod("cubic", float[].class, double.class, double.class); cu.setAccessible(true);
Field lk = Terrain.class.getDeclaredField("lakes"); lk.setAccessible(true); float[] L = (float[]) lk.get(t);
var rnd = new java.util.Random(5); int lakeCols = 0, leaks = 0; int[] at = null;
for (int i = 0; i < 4000000; i++) { int x = rnd.nextInt(t.circumference) - t.circumference / 2, z = rnd.nextInt(t.southPoleZ() - t.northPoleZ()) + t.northPoleZ();
  var c = t.column(x, z); if (c.water() == Terrain.Column.NO_WATER || c.water() <= Terrain.SEA_Y) continue;
  double u = (double) wu.invoke(t, (double) x, (double) z), v = (double) wv.invoke(t, (double) x, (double) z); if ((float) cu.invoke(t, L, u, v) < 0.3f) continue; lakeCols++;
  for (int[] o : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) { var b = t.column(x + o[0], z + o[1]); if (c.water() - Math.max(Math.max(b.water(), b.ice()), b.ground()) >= 2) { leaks++; if (at == null) at = new int[]{x, z}; break; } } }
System.out.println("fmt " + d.format + ": lake columns " + lakeCols + ", leaking " + leaks + " " + java.util.Arrays.toString(at));
/exit
