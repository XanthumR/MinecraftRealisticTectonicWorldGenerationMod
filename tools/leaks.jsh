import com.realtectonics.sim.*;
var t = new Terrain(PlanetData.load(java.nio.file.Path.of("run/world/realtectonics/planet.bin")), 777);
int n = 1500, size = 2 * n + 1; var cols = new Terrain.Column[size * size];
for (int z = 0; z < size; z++) for (int x = 0; x < size; x++) cols[x + z * size] = t.column(x - n, z - n);
int wet = 0, step = 0, wall = 0;
for (int z = 1; z < size - 1; z++) for (int x = 1; x < size - 1; x++) {
  var c = cols[x + z * size]; if (c.water() == Terrain.Column.NO_WATER) continue; wet++;
  int worst = 0;
  for (int k : new int[]{-1, 1, -size, size}) { var o = cols[x + z * size + k]; int top = Math.max(o.ground(), o.water()); worst = Math.max(worst, c.water() - top); }
  if (worst == 1) step++; else if (worst > 1) wall++;
}
System.out.printf("water columns %d, 1-block steps %d, open walls >1 block %d%n", wet, step, wall);
/exit
