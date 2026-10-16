import com.realtectonics.sim.*;
var p = new Planet(1024, 512, 14, 777); for (int i = 0; i < 300; i++) p.step();
var c = new Climate(p); var hy = new Hydrology(p, c.precipitation, 50, 777);
var d = PlanetData.from(p, c, hy); var t = new Terrain(d, 777);
int[] map = t.mapColors(false);
var rnd = new java.util.Random(1);
int samples = 0, found = 0;
while (samples < 2000) {
  int g = rnd.nextInt(map.length);
  if (map[g] != Terrain.WATER_COLOR) continue;
  samples++;
  int[] b = t.blockOf(g); boolean any = false;
  for (int dz = -16; dz <= 16 && !any; dz++) for (int dx = -16; dx <= 16; dx++) if (t.column(b[0] + dx, b[1] + dz).water() != Terrain.Column.NO_WATER) { any = true; break; }
  if (any) found++;
}
int cells = 0, near = 0;
while (cells < 2000) {
  int g = rnd.nextInt(d.w * d.h);
  if (!(d.metres[g] >= 0 && d.flow[g] > Hydrology.RIVER_AREA) || !Float.isNaN(d.lakeMetres[g])) continue;
  cells++;
  int[] b = t.blockOf(g); boolean any = false;
  for (int dz = -60; dz <= 60 && !any; dz += 2) for (int dx = -60; dx <= 60; dx += 2) if (t.column(b[0] + dx, b[1] + dz).water() != Terrain.Column.NO_WATER) { any = true; break; }
  if (any) near++;
}
System.out.printf("new map: water pixels with water in-game within 16 blocks: %d of %d (%d%%)%n", found, samples, found * 100 / samples);
System.out.printf("simulation river cells with water within 60 blocks: %d%% (was 81%%)%n", near * 100 / cells);
/exit
