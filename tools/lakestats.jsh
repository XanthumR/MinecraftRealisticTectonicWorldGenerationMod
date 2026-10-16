import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {});
int[] lakes = new int[9], land = new int[9];
for (int g = 0; g < d.w*d.h; g++) { if (d.metres[g] < 0) continue; int t = (int) Math.clamp((d.temperature[g] + 20) / 6, 0, 8); land[t]++; if (!Float.isNaN(d.lakeMetres[g])) lakes[t]++; }
int all = 0; for (int i = 0; i < 9; i++) { all += lakes[i]; System.out.printf("%+3d..%+3d C: lake cells %5d of %6d land (%.2f%%)%n", -20 + 6*i, -14 + 6*i, lakes[i], land[i], 100.0 * lakes[i] / Math.max(1, land[i])); }
System.out.println("lake cells " + all);
/exit
