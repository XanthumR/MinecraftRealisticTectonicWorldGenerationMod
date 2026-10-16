import com.realtectonics.sim.*;
// Gully comb (format 41): on an even slope, the share of columns the gully pass cuts or raises by a block or more
// against the plain column (want near 0 at 41). -Dplanet -Dseed -Dms -Dzoom -Drw -Dcx -Dcz. Reference: the user's
// "New World (1)" (seed -5592192546219388092) at 3330 2130: 40 had a 4-block comb, 41 none.
var d = PlanetData.load(java.nio.file.Path.of(System.getProperty("planet")));
long seed = Long.getLong("seed", 777); float ms = Float.parseFloat(System.getProperty("ms", "1")), rw = Float.parseFloat(System.getProperty("rw", "2"));
int zoom = Integer.getInteger("zoom", 1), cx = Integer.getInteger("cx"), cz = Integer.getInteger("cz");
for (int f : new int[]{40, 41}) { d.format = f; var t = new Terrain(d, seed, ms, zoom, rw); int n = 0, moved = 0;
  for (int z = cz - 32; z < cz + 32; z++) for (int x = cx - 32; x < cx + 32; x++) { n++; if (Math.abs(t.relaxedColumn(x, z).ground() - t.column(x, z).ground()) >= 1) moved++; }
  System.out.printf("fmt %d: %.0f%% of columns cut or raised by the gully and rockfall pass%n", f, 100.0 * moved / n); }
/exit
