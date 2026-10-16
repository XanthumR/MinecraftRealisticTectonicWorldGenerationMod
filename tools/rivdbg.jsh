import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
var wu = Terrain.class.getDeclaredMethod("warpU", double.class, double.class); wu.setAccessible(true);
var wv = Terrain.class.getDeclaredMethod("warpV", double.class, double.class); wv.setAccessible(true);
var ra = Terrain.class.getDeclaredMethod("riverAt", double.class, double.class, double.class, double.class); ra.setAccessible(true);
var nr = Terrain.class.getDeclaredMethod("nearestRiver", double.class, double.class, byte.class, float.class); nr.setAccessible(true);
int x0 = Integer.getInteger("x0"), z0 = Integer.getInteger("z0"), x1 = Integer.getInteger("x1"), z1 = Integer.getInteger("z1");
for (int i = 0; i <= 16; i++) { int x = x0 + (x1 - x0) * i / 16, z = z0 + (z1 - z0) * i / 16; double u = (double) wu.invoke(t, (double) x, (double) z), v = (double) wv.invoke(t, (double) x, (double) z);
  Object r = ra.invoke(t, (double) x, (double) z, u, v); var c = t.column(x, z);
  System.out.println(x + "," + z + " " + c.ground() + "/" + c.water() + " " + r); }
/exit
