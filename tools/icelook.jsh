import com.realtectonics.sim.*;
// Format 40 glaciers over a 1000-block square of any planet: ice cover and thickness at formats 39 and 40 (want less
// and thinner at 40), and at 40 the bare ice below the snow line and its moraines (want medial > 0 where glaciers join).
// -Dplanet -Dseed -Dms -Dzoom -Drw -Dcx -Dcz. Reference: the user's "New World" (seed 417682158270455126, ms 0.6,
// zoom 2, rw 2.5) at -27100 -2100: 39% ice 19 blocks thick -> 37% 14 thick, medial 763.
var d = PlanetData.load(java.nio.file.Path.of(System.getProperty("planet")));
long seed = Long.getLong("seed", 777); float ms = Float.parseFloat(System.getProperty("ms", "1")), rw = Float.parseFloat(System.getProperty("rw", "2"));
int zoom = Integer.getInteger("zoom", 1), cx = Integer.getInteger("cx"), cz = Integer.getInteger("cz");
for (int f : new int[]{39, 40}) { d.format = f; var t = new Terrain(d, seed, ms, zoom, rw);
  int n = 0, ice = 0, thick = 0, bare = 0, edge = 0, medial = 0;
  for (int z = cz - 500; z < cz + 500; z += 2) for (int x = cx - 500; x < cx + 500; x += 2) { var c = t.column(x, z); n++;
    if (c.ice() == Terrain.Column.NO_WATER) continue; ice++; thick += c.ice() - c.ground();
    if (c.cover() == Terrain.Cover.BARE_ICE) bare++; else if (c.cover() == Terrain.Cover.MORAINE) { if (c.ice() - c.ground() <= 2) edge++; else medial++; } }
  System.out.printf("fmt %d: ice %.0f%% (%d blocks thick), bare %d, edge moraine %d, medial moraine %d%n", f, 100.0 * ice / n, ice > 0 ? thick / ice : 0, bare, edge, medial); }
/exit
