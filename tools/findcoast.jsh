import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
var fc = Terrain.class.getDeclaredField("cliffy"); fc.setAccessible(true); var fb = Terrain.class.getDeclaredField("barriers"); fb.setAccessible(true);
float[] cl = (float[]) fc.get(t), ba = (float[]) fb.get(t);
int nc = 0, nb = 0, coast = 0; int[] c1 = null, b1 = null;
for (int g = d.w * 100; g < d.w * 400; g++) { if (d.metres[g] < 0 || d.metres[g + 1] >= 0) continue; coast++;
  if (cl[g] > 0.7f) { nc++; if (c1 == null) c1 = t.blockOf(g); }
  if (ba[g] > 0.7f) { nb++; if (b1 == null) b1 = t.blockOf(g); } }
System.out.println("coast cells " + coast + ": cliffy " + nc + ", barrier " + nb + "; cliff at " + java.util.Arrays.toString(c1) + " barrier at " + java.util.Arrays.toString(b1));
/exit
