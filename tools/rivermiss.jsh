import com.realtectonics.sim.*;
var p = new Planet(1024, 512, 14, 777); for (int i = 0; i < 300; i++) p.step();
var c = new Climate(p); var hy = new Hydrology(p, c.precipitation, 50, 777);
var d = PlanetData.from(p, c, hy); var t = new Terrain(d, 777);
var rnd = new java.util.Random(1);
int samples = 0, missing = 0, seaLevel = 0, nearLake = 0, coastal = 0;
var examples = new StringBuilder();
while (samples < 2000) {
  int g = rnd.nextInt(d.w * d.h);
  if (!(d.metres[g] >= 0 && d.flow[g] > Hydrology.RIVER_AREA) || !Float.isNaN(d.lakeMetres[g])) continue;
  samples++;
  int[] b = t.blockOf(g);
  boolean any = false; int lowest = Integer.MAX_VALUE;
  for (int dz = -60; dz <= 60 && !any; dz += 2) for (int dx = -60; dx <= 60; dx += 2) { var col = t.column(b[0] + dx, b[1] + dz); if (col.water() != Terrain.Column.NO_WATER) { any = true; break; } }
  if (any) continue;
  missing++;
  boolean lake = false, sea = false;
  for (int k = -3; k <= 3; k++) for (int j = -3; j <= 3; j++) { int n = Math.floorMod(g % d.w + j, d.w) + Math.clamp(g / d.w + k, 0, d.h - 1) * d.w; lake |= !Float.isNaN(d.lakeMetres[n]); sea |= d.metres[n] < 0; }
  if (lake) nearLake++; if (sea) coastal++;
  if (examples.length() < 600) examples.append(String.format("  cell %d: %.0f m, flow %.0f, near lake %b, near sea %b, column ground %d%n", g, d.metres[g], d.flow[g], lake, sea, t.column(b[0], b[1]).ground()));
}
System.out.printf("missing %d of %d (no water of any kind within 60 blocks); near a lake %d, near the sea %d%n", missing, samples, nearLake, coastal);
System.out.print(examples);
/exit
