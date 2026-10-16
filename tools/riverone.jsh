import com.realtectonics.sim.*;
var p = new Planet(1024, 512, 14, 777); for (int i = 0; i < 300; i++) p.step();
var c = new Climate(p); var hy = new Hydrology(p, c.precipitation, 50, 777);
var d = PlanetData.from(p, c, hy); var t = new Terrain(d, 777);
int g = 161304, w = d.w;
System.out.printf("cell (%d,%d) metres %.0f flow %.0f receiver (%d,%d) metres %.0f%n", g % w, g / w, d.metres[g], d.flow[g], d.receiver[g] % w, d.receiver[g] / w, d.metres[d.receiver[g]]);
System.out.println("neighbour cell metres:");
for (int dy = -2; dy <= 2; dy++) { var sb = new StringBuilder(); for (int dx = -2; dx <= 2; dx++) { int n = (g % w + dx) + (g / w + dy) * w; sb.append(String.format("%7.0f%s", d.metres[n], d.flow[n] > Hydrology.RIVER_AREA ? "r" : " ")); } System.out.println(sb); }
int[] b = t.blockOf(g); int lo = 9999, hi = -9999;
for (int dz = -64; dz <= 64; dz += 4) for (int dx = -64; dx <= 64; dx += 4) { int gr = t.column(b[0] + dx, b[1] + dz).ground(); lo = Math.min(lo, gr); hi = Math.max(hi, gr); }
System.out.println("block ground around it: " + lo + " .. " + hi + "   (river cell would be y " + Math.round(t.toY(d.metres[g])) + ")");
/exit
