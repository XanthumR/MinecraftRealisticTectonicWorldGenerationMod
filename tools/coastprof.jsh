import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
var wu = Terrain.class.getDeclaredMethod("warpU", double.class, double.class); wu.setAccessible(true);
var wv = Terrain.class.getDeclaredMethod("warpV", double.class, double.class); wv.setAccessible(true);
var cu = Terrain.class.getDeclaredMethod("cubic", float[].class, double.class, double.class); cu.setAccessible(true);
var no = Terrain.class.getDeclaredMethod("noise", double.class, double.class, int.class, int.class); no.setAccessible(true);
var fl = Terrain.class.getDeclaredField("land"); fl.setAccessible(true); var fb = Terrain.class.getDeclaredField("barriers"); fb.setAccessible(true);
float[] land = (float[]) fl.get(t), bar = (float[]) fb.get(t);
int z = Integer.getInteger("z"), x0 = Integer.getInteger("x0");
for (int x = x0; x < x0 + 240; x += 8) { double u = (double) wu.invoke(t, (double) x, (double) z), v = (double) wv.invoke(t, (double) x, (double) z);
  float shore = (float) cu.invoke(t, land, u, v) - 0.5f, b = (float) cu.invoke(t, bar, u, v); float nz = (float) no.invoke(t, (double) x, (double) z, 512, 36);
  var c = t.column(x, z); System.out.printf("x %d shore %.3f barrier %.2f noise512 %.2f ground %d water %s%n", x, shore, b, nz, c.ground(), c.water() == Terrain.Column.NO_WATER ? "-" : "" + c.water()); }
/exit
