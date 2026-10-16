import com.realtectonics.sim.*;
var p = new Planet(1024, 512, 14, 0.7f, 1, 777); PlanetData.tectonics(p, 300, x -> {});
var c = new Climate(p);
// alternation: share of land cells that are a strict local extreme along x or along y (bumpiness at the 1-2 cell scale)
java.util.function.BiFunction<float[], Integer, Double> alt = (e, w) -> { int n = 0, a = 0; for (int g = w; g < e.length - w; g++) { int x = g % w; if (x == 0 || x == w-1 || e[g] <= 0) continue; n++; boolean ex = (e[g] - e[g-1]) * (e[g] - e[g+1]) > 0, ey = (e[g] - e[g-w]) * (e[g] - e[g+w]) > 0; if (ex || ey) a++; } return 100.0 * a / n; };
float[] crust = new float[p.height.length]; for (int g = 0; g < crust.length; g++) crust[g] = p.height[g] - Planet.SEA_LEVEL;
System.out.printf("after tectonics: %.1f%% of land cells are 1-cell bumps or dips%n", alt.apply(crust, p.w));
for (int iters : new int[]{0, 50}) { var hy = new Hydrology(p, c.precipitation, c.anomaly, iters, 777, x -> {}); System.out.printf("hydrology with %d erosion passes: %.1f%%%n", iters, alt.apply(hy.elevation, p.w)); }
/exit
