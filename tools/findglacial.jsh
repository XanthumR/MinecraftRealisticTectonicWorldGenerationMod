import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
var fh = Terrain.class.getDeclaredField("hornsByCell"); fh.setAccessible(true); var fe = Terrain.class.getDeclaredField("glacialEnd"); fe.setAccessible(true);
var horns = (java.util.Map<?, ?>) fh.get(t); boolean[] ends = (boolean[]) fe.get(t);
int nEnd = 0, endAt = -1; for (int g = 0; g < ends.length; g++) if (ends[g]) { nEnd++; if (endAt < 0 && d.flow[g] > 200) endAt = g; }
int hornAt = -1, nHorn = 0;
for (int g = 0; g < d.w*d.h; g++) { if (d.ice[g] == 0 || d.metres[g] < 2500) continue; boolean top = true; for (int dy = -2; dy <= 2 && top; dy++) for (int dx = -2; dx <= 2; dx++) if ((dx != 0 || dy != 0) && d.metres[Math.floorMod(g % d.w + dx, d.w) + Math.clamp(g / d.w + dy, 0, d.h - 1) * d.w] >= d.metres[g]) top = false; if (top) { nHorn++; if (hornAt < 0) hornAt = g; } }
System.out.println("horns " + nHorn + " first " + java.util.Arrays.toString(t.blockOf(hornAt)) + "; glacier ends " + nEnd + " a big one at " + java.util.Arrays.toString(t.blockOf(endAt)));
/exit
