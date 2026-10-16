import com.realtectonics.sim.*;
// A glacial valley cell lying deepest below the land two cells either side: where trough walls are tallest.
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
int w = d.w, best = -1; float most = 0;
for (int g = 2 * w; g < w * (d.h - 2); g++) { if (d.ice[g] != 1 || d.flow[g] < 20 || d.metres[g] < 0) continue; int x = g % w;
  float side = Math.min(Math.max(d.metres[(x + 2) % w + g / w * w], d.metres[Math.floorMod(x - 2, w) + g / w * w]), Math.max(d.metres[g + 2 * w], d.metres[g - 2 * w]));
  if (side - d.metres[g] > most) { most = side - d.metres[g]; best = g; } }
System.out.println("deepest trough " + most + " m at " + java.util.Arrays.toString(t.blockOf(best)));
/exit
