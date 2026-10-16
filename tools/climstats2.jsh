import com.realtectonics.sim.*;
var p = new Planet(1024, 512, 14, 777); for (int i = 0; i < 300; i++) p.step();
var c = new Climate(p); int w = p.w, h = p.h;
boolean[] land = new boolean[w*h]; for (int g = 0; g < w*h; g++) land[g] = p.height[g] >= Planet.SEA_LEVEL;
java.util.function.BiFunction<Integer,Integer,Integer> seaDist = (g, dir) -> { int x = g % w, y = g / w; for (int k = 1; k < 40; k++) if (!land[Math.floorMod(x + dir*k, w) + y*w]) return k; return 99; };
for (double[] band : new double[][]{{20, 28}, {28, 35}}) {
  double sum = 0, rain = 0, high = 0; int n = 0; var rs = new java.util.ArrayList<Float>();
  for (int g = 0; g < w*h; g++) { if (!land[g]) continue; double lat = Math.abs(Climate.latitude(g / w, h)); if (lat < band[0] || lat > band[1]) continue;
    if (seaDist.apply(g, -1) <= 3 && seaDist.apply(g, 1) > 15) { sum += c.anomaly[g]; rain += c.precipitation[g]; n++; rs.add(c.precipitation[g]); high += Planet.metres(p.height[g] - Planet.SEA_LEVEL); } }
  java.util.Collections.sort(rs);
  System.out.printf("wide-continent west coast %.0f-%.0f: n=%d warmth %+.1f, rain mean %.0f median %.0f, mean height %.0f m%n", band[0], band[1], n, sum/n, rain/n, rs.get(n/2), high/n);
}
/exit
