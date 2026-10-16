import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
var f = Terrain.class.getDeclaredField("glacier"); f.setAccessible(true); float[] ice = (float[]) f.get(t);
int[] bins = new int[6]; int shown = 0;
for (int g = 0; g < ice.length; g++) { if (ice[g] <= 1) continue; int r = d.receiver[g]; if (r < 0 || ice[r] > 1 || d.metres[r] < 0 || d.ice[g] != 1 || d.flow[g] <= 20) continue;
  float T = d.temperature[g]; bins[Math.clamp((int) Math.floor((T + 5) / 5), 0, 5)]++;
  if (T > 10 && shown++ < 4) System.out.printf("warm snout T %.1f swing %.1f rain %.0f flow %.0f at %s%n", T, d.amplitude[g], d.precipitation[g], d.flow[g], java.util.Arrays.toString(t.blockOf(g))); }
System.out.println("snouts by annual mean: <0 " + bins[0] + ", 0-5 " + bins[1] + ", 5-10 " + bins[2] + ", 10-15 " + bins[3] + ", 15+ " + (bins[4] + bins[5]));
/exit
