import com.realtectonics.sim.*;
var p = new Planet(1024, 512, 14, 0.7f, 1, 777); PlanetData.tectonics(p, 300, x -> {});
var c = new Climate(p); var hy = new Hydrology(p, c.precipitation, c.anomaly, 50, 777, x -> {});
int w = hy.w, h = hy.h, glacialMouth = 0, low = 0, relief = 0, both = 0;
for (int g = 0; g < w*h; g++) { int r = hy.receiver[g]; if (hy.ice[g] == 0 || r < 0 || hy.elevation[g] < 0 || hy.elevation[r] >= 0) continue; glacialMouth++;
  boolean isLow = Planet.metres(hy.elevation[g]) < 15; float top = hy.elevation[g]; for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) { int y = g / w + dy; if (y >= 0 && y < h) top = Math.max(top, hy.elevation[Math.floorMod(g % w + dx, w) + y * w]); }
  boolean rel = Planet.metres(top) >= 500; if (isLow) low++; if (rel) relief++; if (isLow && rel) both++; }
System.out.printf("glacial cells draining straight to sea %d: below 15 m %d, among 500 m relief %d, both %d%n", glacialMouth, low, relief, both);
/exit
