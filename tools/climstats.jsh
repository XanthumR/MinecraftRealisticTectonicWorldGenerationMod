import com.realtectonics.sim.*;
var p = new Planet(1024, 512, 14, 777); for (int i = 0; i < 300; i++) p.step();
var c = new Climate(p); int w = p.w, h = p.h;
boolean[] land = new boolean[w*h]; for (int g = 0; g < w*h; g++) land[g] = p.height[g] >= Planet.SEA_LEVEL;
java.util.function.BiFunction<Integer,Integer,Integer> seaDist = (g, dir) -> { int x = g % w, y = g / w; for (int k = 1; k < 40; k++) if (!land[Math.floorMod(x + dir*k, w) + y*w]) return k; return 99; };
String[] names = {"west coast", "east coast", "interior"};
for (double[] band : new double[][]{{50, 62}, {20, 35}}) {
  double[] sum = new double[3], rain = new double[3]; int[] n = new int[3];
  for (int g = 0; g < w*h; g++) { if (!land[g]) continue; double lat = Math.abs(Climate.latitude(g / w, h)); if (lat < band[0] || lat > band[1]) continue;
    int dw = seaDist.apply(g, -1), de = seaDist.apply(g, 1); int k = dw <= 3 ? 0 : de <= 3 ? 1 : (dw > 15 && de > 15 ? 2 : -1); if (k < 0) continue;
    sum[k] += c.anomaly[g]; rain[k] += c.precipitation[g]; n[k]++; }
  for (int k = 0; k < 3; k++) System.out.printf("lat %.0f-%.0f %s: n=%d warmth %+.1f C, rain %.0f mm%n", band[0], band[1], names[k], n[k], sum[k]/n[k], rain[k]/n[k]);
}
// monsoon: land 10-25 deg with sea within 6 cells toward the equator vs same latitudes without
double[] r = new double[2]; int[] n = new int[2];
for (int g = 0; g < w*h; g++) { if (!land[g]) continue; int y = g / w; double lat = Climate.latitude(y, h); if (Math.abs(lat) < 10 || Math.abs(lat) > 25) continue; int dir = lat > 0 ? 1 : -1; boolean sea = false; for (int k = 1; k <= 6; k++) sea |= !land[g % w + Math.clamp(y + dir*k, 0, h-1)*w]; r[sea?0:1] += c.precipitation[g]; n[sea?0:1]++; }
System.out.printf("10-25 deg land, sea on the equator side: %.0f mm (n=%d); other: %.0f mm (n=%d)%n", r[0]/n[0], n[0], r[1]/n[1], n[1]);
/exit
