import com.realtectonics.sim.*;
var p = new Planet(256, 128, 8, 7); for (int i = 0; i < 50; i++) p.step();
var c = new Climate(p); var d = PlanetData.from(p, c, new Hydrology(p, c.precipitation, 20, 7)); var t = new Terrain(d, 7);
int[] map = t.mapColors(false); int pix = 0; int[] hist = new int[6]; // nearest water: <=16, <=24, <=32, <=48, <=64, none
for (int g = 0; g < map.length && pix < 200; g++) {
  if (map[g] != Terrain.WATER_COLOR) continue; pix++;
  int[] b = t.blockOf(g); int best = 999;
  for (int dz = -64; dz <= 64; dz++) for (int dx = -64; dx <= 64; dx++) if (Math.max(Math.abs(dx), Math.abs(dz)) < best && t.column(b[0] + dx, b[1] + dz).water() != Terrain.Column.NO_WATER) best = Math.max(Math.abs(dx), Math.abs(dz));
  hist[best <= 16 ? 0 : best <= 24 ? 1 : best <= 32 ? 2 : best <= 48 ? 3 : best <= 64 ? 4 : 5]++;
}
System.out.printf("water pixels %d; nearest water <=16: %d, <=24: %d, <=32: %d, <=48: %d, <=64: %d, none within 64: %d%n", pix, hist[0], hist[1], hist[2], hist[3], hist[4], hist[5]);
/exit
