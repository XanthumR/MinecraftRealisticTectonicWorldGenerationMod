import com.realtectonics.sim.*;
var p = new Planet(1024, 512, 14, 0.5f, 42); for (int i = 0; i < 300; i++) p.step();
var c = new Climate(p); var hy = new Hydrology(p, c.precipitation, c.anomaly, 50, 42);
var d = PlanetData.from(p, c, hy); var t = new Terrain(d, 42);
int checked = 0, bad = 0;
for (int g = 0; g < hy.w * hy.h && checked < 5; g += 97) {
  if (!hy.isLake(g) || hy.lakeLevel(g) >= hy.surface[g] - 1e-3f) continue;
  checked++; int[] centre = t.blockOf(g); int n = 200, size = 2*n+1; var cols = new Terrain.Column[size*size];
  for (int z = 0; z < size; z++) for (int x = 0; x < size; x++) cols[x+z*size] = t.column(centre[0]+x-n, centre[1]+z-n);
  int water = 0;
  for (int z = 1; z < size-1; z++) for (int x = 1; x < size-1; x++) { var col = cols[x+z*size]; if (col.water() == Terrain.Column.NO_WATER) continue; if (col.water() >= Terrain.SEA_Y) water++;
    for (int k : new int[]{-1, 1, -size, size}) { var o = cols[x+z*size+k]; if (col.water() - Math.max(o.ground(), o.water()) > 1) bad++; } }
  System.out.println("shrunken lake near " + java.util.Arrays.toString(centre) + ": inland water columns " + water);
}
System.out.println("checked " + checked + " shrunken lakes, open water walls: " + bad);
/exit
