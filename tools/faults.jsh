import com.realtectonics.sim.*;
// Strike-slip faults (format 32): fault cells must lie in a valley (gouge erodes fast, Hydrology.FAULT_GOUGE: mean
// height below their 7x7 surroundings, negative) and their trace must carry a scarp (Terrain.faultScarp: max step > 0).
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); int W = d.w; double sum = 0; int n = 0;
for (int g = 0; g < d.metres.length; g++) { if (d.faultSlip[g] < 0.2f || d.metres[g] < 50 || Math.abs(d.faultSide[g]) > 0.7f) continue;
  int x = g % W, y = g / W; double s = 0; int k = 0; for (int dy = -3; dy <= 3; dy++) for (int dx = -3; dx <= 3; dx++) { int yy = y + dy; if (yy < 0 || yy >= d.h) continue; s += Math.max(0, d.metres[Math.floorMod(x + dx, W) + yy * W]); k++; }
  sum += d.metres[g] - s / k; n++; }
d.format = 31; var a = new Terrain(d, 777, 1, 1, 2); d.format = 32; var b = new Terrain(d, 777, 1, 1, 2); int step = 0;
int fast = 0; for (int g = 0; g < d.metres.length; g++) if (d.metres[g] > 50 && d.faultSlip[g] > d.faultSlip[fast]) fast = g; // the fastest fault on land
int[] at = b.blockOf(fast);
for (int z = at[1] - 128; z < at[1] + 128; z++) for (int x = at[0] - 128; x < at[0] + 128; x++) step = Math.max(step, Math.abs(b.column(x, z).ground() - a.column(x, z).ground()));
System.out.printf("fault cells %d, mean height vs surroundings %.0f m (want < 0), largest scarp offset %d blocks (want > 0)%n", n, sum / n, step);
/exit
