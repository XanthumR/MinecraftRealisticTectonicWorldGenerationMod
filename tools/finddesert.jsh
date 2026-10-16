import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
var no = Terrain.class.getDeclaredMethod("noise", double.class, double.class, int.class, int.class); no.setAccessible(true);
int[] dune = null, salt = null; int dunes = 0, salts = 0;
for (int g = d.w * 100; g < d.w * 412; g += 3) { if (d.metres[g] < 50) continue; int[] b = t.blockOf(g);
  if (dune == null && d.precipitation[g] < 160 && (float) no.invoke(t, (double) b[0], (double) b[1], 512, 40) > 0.3f) dune = b;
  if (d.precipitation[g] < 300 && !Float.isNaN(d.lakeMetres[g])) { salts++; if (salt == null) salt = b; } }
System.out.println("erg at " + java.util.Arrays.toString(dune) + ", desert lake cells " + salts + " first " + java.util.Arrays.toString(salt));
/exit
