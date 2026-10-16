import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
int cx = Integer.getInteger("cx"), cz = Integer.getInteger("cz");
var sb = new StringBuilder(); var cov = new StringBuilder();
for (int x = cx - 120; x <= cx + 120; x += 4) { var c = t.column(x, cz); sb.append(c.ground()).append(' '); cov.append(c.cover().name().charAt(0)); }
int g = 0; { int[] best = null; } 
System.out.println("ground: " + sb); System.out.println("cover:  " + cov);
float mn = 1e9f; int dry = 0; for (int i = 0; i < d.w*d.h; i++) if (d.metres[i] >= 0) { mn = Math.min(mn, d.precipitation[i]); if (d.precipitation[i] < 180) dry++; }
System.out.println("driest land " + mn + " mm, land cells under 180 mm: " + dry);
/exit
