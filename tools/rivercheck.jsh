import com.realtectonics.sim.*;
var p = new Planet(1024, 512, 14, 777); for (int i = 0; i < 300; i++) p.step();
var c = new Climate(p); var hy = new Hydrology(p, c.precipitation, 50, 777);
var d = PlanetData.from(p, c, hy); var t = new Terrain(d, 777);
int samples = 0, atPixel = 0, within60 = 0;
var rnd = new java.util.Random(1);
while (samples < 2000) {
  int g = rnd.nextInt(d.w * d.h);
  if (!(d.metres[g] >= 0 && d.flow[g] > Hydrology.RIVER_AREA) || !Float.isNaN(d.lakeMetres[g])) continue;
  samples++;
  int[] b = t.blockOf(g); // where the map draws this river pixel
  boolean here = false, near = false;
  for (int dz = -60; dz <= 60 && !near; dz += 2) for (int dx = -60; dx <= 60; dx += 2) {
    var col = t.column(b[0] + dx, b[1] + dz);
    if (col.water() != Terrain.Column.NO_WATER && col.water() > Terrain.SEA_Y - 1) { near = true; if (Math.abs(dx) <= 16 && Math.abs(dz) <= 16) here = true; break; }
  }
  for (int dz = -16; dz <= 16 && !here; dz += 2) for (int dx = -16; dx <= 16; dx += 2) { var col = t.column(b[0] + dx, b[1] + dz); if (col.water() != Terrain.Column.NO_WATER && col.water() > Terrain.SEA_Y - 1) { here = true; break; } }
  if (here) atPixel++; if (near || here) within60++;
}
System.out.printf("river pixels: %d  |  water inside the pixel's own 32x32 blocks: %d%%  |  water within 60 blocks: %d%%%n", samples, atPixel * 100 / samples, within60 * 100 / samples);
/exit
