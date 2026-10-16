import com.realtectonics.sim.*;
var p = new Planet(1024, 512, 14, 0.5f, 42); for (int i = 0; i < 300; i++) p.step();
var c = new Climate(p); var hy = new Hydrology(p, c.precipitation, c.anomaly, 50, 777);
int full = 0, shrunk = 0, shrunkCells = 0, spill = 0; float drop = 0;
for (int g = 0; g < hy.w * hy.h; g++) if (hy.isLake(g)) { if (hy.lakeLevel(g) < hy.surface[g] - 1e-3f) { shrunkCells++; drop += Planet.metres(hy.surface[g]) - Planet.metres(hy.lakeLevel(g)); } else spill++; }
System.out.printf("lake cells: %d at their spill level, %d in shrunken (outletless) lakes, mean %.0f m below the rim%n", spill, shrunkCells, drop / Math.max(1, shrunkCells));
/exit
