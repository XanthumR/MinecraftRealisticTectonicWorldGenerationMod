import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
int best = -1, most = 0;
for (int g = d.w * 150; g < d.w * 360; g += 7) { if (d.metres[g] < 100 || d.metres[g] > 600 || d.ice[g] != 0) continue; int k = 0;
  for (int dy = -4; dy <= 4; dy++) for (int dx = -4; dx <= 4; dx++) { int q = Math.floorMod(g % d.w + dx, d.w) + (g / d.w + dy) * d.w; if (d.flow[q] > 20 && d.metres[q] >= 0) k++; }
  if (k > most) { most = k; best = g; } }
System.out.println("creek country at " + java.util.Arrays.toString(t.blockOf(best)) + " stream cells " + most);
/exit
