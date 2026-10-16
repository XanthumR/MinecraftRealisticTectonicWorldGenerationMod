import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {});
int land = 0; float peak = 0; var hs = new java.util.ArrayList<Float>(); double rough = 0; int rn = 0;
for (int g = 0; g < d.w*d.h; g++) { if (d.metres[g] < 0) continue; land++; peak = Math.max(peak, d.metres[g]); hs.add(d.metres[g]);
  if (d.metres[g] > 1500) { int x = g % d.w, y = g / d.w; if (y > 0 && y < d.h-1) { float m = 0; for (int k : new int[]{-1, 1, -d.w, d.w}) { int n2 = (k == -1 || k == 1) ? Math.floorMod(x + k, d.w) + y * d.w : g + k; m += Math.abs(d.metres[n2] - d.metres[g]); } rough += m / 4; rn++; } } }
java.util.Collections.sort(hs);
System.out.printf("land %.1f%%, median %.0f m, p90 %.0f m, p99 %.0f m, peak %.0f m, mountain cells >1500 m %d, mean neighbour relief there %.0f m%n", 100.0*land/(d.w*d.h), hs.get(hs.size()/2), hs.get(hs.size()*9/10), hs.get(hs.size()*99/100), peak, rn, rough/rn);
/exit
