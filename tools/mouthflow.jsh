import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {});
var l = new java.util.ArrayList<Float>(); int shelf = 0;
for (int c = 0; c < d.w * d.h; c++) { int r = d.receiver[c]; if (r >= 0 && d.metres[c] >= 0 && d.metres[r] < 0 && d.flow[c] > 200) { l.add(d.flow[c]); if (d.metres[r] >= -150) shelf++; } }
java.util.Collections.sort(l); int n = l.size();
System.out.printf("river mouths %d (on shelf %d): median %.0f p90 %.0f p99 %.0f max %.0f%n", n, shelf, l.get(n/2), l.get(n*9/10), l.get(n*99/100), l.get(n-1));
/exit
