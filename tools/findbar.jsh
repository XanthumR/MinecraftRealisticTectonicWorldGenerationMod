import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
var no = Terrain.class.getDeclaredMethod("noise", double.class, double.class, int.class, int.class); no.setAccessible(true);
var fb = Terrain.class.getDeclaredField("barriers"); fb.setAccessible(true); float[] ba = (float[]) fb.get(t);
for (int g = d.w * 120; g < d.w * 400; g++) { if (d.metres[g] < 0 || d.metres[g + 1] >= 0 || ba[g] < 0.8f) continue; int[] b = t.blockOf(g);
  if ((float) no.invoke(t, (double) b[0], (double) b[1], 512, 36) > 0.45f) { System.out.println("barrier coast at " + java.util.Arrays.toString(b)); break; } }
/exit
