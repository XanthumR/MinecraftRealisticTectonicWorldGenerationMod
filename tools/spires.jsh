import com.realtectonics.sim.*;
// Spires near the player: a column standing 40+ above every column 4 blocks around it (game columns, after rockfall).
var d = PlanetData.load(java.nio.file.Path.of(System.getProperty("planet"))); d.format = Integer.getInteger("fmt", d.format);
var t = new Terrain(d, Long.getLong("seed"), 1, 1, 2);
int px = Integer.getInteger("px"), pz = Integer.getInteger("pz"), R = Integer.getInteger("r", 400), found = 0, need = Integer.getInteger("need", 40), step = Integer.getInteger("step", 2); boolean relax = Boolean.parseBoolean(System.getProperty("relax", "true"));
for (int z = pz - R; z <= pz + R && found < 6; z += step) for (int x = px - R; x <= px + R && found < 6; x += step) {
  var c = relax ? t.relaxedColumn(x, z) : t.column(x, z); int top = Math.max(c.ground(), c.ice()), lo = Integer.MAX_VALUE;
  for (int[] o : new int[][]{{4,0},{-4,0},{0,4},{0,-4}}) { var n = relax ? t.relaxedColumn(x + o[0], z + o[1]) : t.column(x + o[0], z + o[1]); lo = Math.min(lo, Math.max(n.ground(), n.ice())); }
  if (top - lo >= need) { found++; var raw = t.column(x, z); System.out.println("spire " + x + " " + z + " top " + top + " around " + lo + " | raw ground " + raw.ground() + " ice " + raw.ice() + " " + c); } }
System.out.println("found " + found);
/exit
