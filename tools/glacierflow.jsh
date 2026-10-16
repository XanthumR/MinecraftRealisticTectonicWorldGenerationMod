import com.realtectonics.sim.*;
// Simulated glaciers: cells with ice, termini, how warm they reach, and the longest valley glacier's snout in blocks.
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
var f = Terrain.class.getDeclaredField("glacier"); f.setAccessible(true); float[] ice = (float[]) f.get(t);
int cells = 0, valley = 0, ends = 0; float warm = -99, best = 0; int bestCell = -1;
for (int g = 0; g < ice.length; g++) { if (ice[g] <= 1) continue; cells++; if (d.ice[g] == 1 && d.flow[g] > 20) valley++;
  int r = d.receiver[g]; if (r >= 0 && ice[r] <= 1 && d.metres[r] >= 0 && d.ice[g] == 1 && d.flow[g] > 20) { ends++; warm = Math.max(warm, d.temperature[g]); if (d.flow[g] > best) { best = d.flow[g]; bestCell = g; } } }
int[] b = bestCell < 0 ? null : t.blockOf(bestCell);
System.out.printf("ice cells %d, in glacial valleys %d, valley snouts %d, warmest snout %.1f C, biggest snout at %s ice %.0f blocks%n", cells, valley, ends, warm, java.util.Arrays.toString(b), bestCell < 0 ? 0 : ice[bestCell]);
/exit
