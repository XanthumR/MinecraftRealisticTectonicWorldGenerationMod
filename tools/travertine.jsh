import com.realtectonics.sim.*;
// Format 91 travertine terraces: finds every site on the planet, lists a few, and checks each for water spilling onto
// lower dry ground after rockfall (0; ice counts as a wall). -Dplanet=file (else seed 777 is generated), -Dfmt=N.
var pf = System.getProperty("planet");
var d = pf == null ? PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}) : PlanetData.load(java.nio.file.Path.of(pf));
d.format = Integer.getInteger("fmt", d.format); var t = new Terrain(d, 777, 1, 1, 2);
var site = Terrain.class.getDeclaredMethod("travertineSite", int.class, int.class, int.class); site.setAccessible(true);
var h = Terrain.class.getDeclaredMethod("hash01", int.class); h.setAccessible(true);
int grid = 256; int cells = d.w * Terrain.BLOCKS_PER_CELL / grid, rows = d.h * Terrain.BLOCKS_PER_CELL / grid;
var ox = Terrain.class.getDeclaredField("originX"); ox.setAccessible(true); var oy = Terrain.class.getDeclaredField("originY"); oy.setAccessible(true);
int x0 = (int) Math.round(-(int) ox.get(t) * Terrain.BLOCKS_PER_CELL), z0 = (int) Math.round(-(int) oy.get(t) * Terrain.BLOCKS_PER_CELL);
int sites = 0, spills = 0, pools = 0;
for (int gz = Math.floorDiv(z0, grid); gz < Math.floorDiv(z0, grid) + rows; gz++) for (int gx = 0; gx < cells; gx++) {
  int key = Math.floorMod(gx, cells) * 5953 + gz * 81799 + 31;
  if ((float) h.invoke(t, key) > 1f) continue;
  Object s = site.invoke(t, key, gx * grid, gz * grid);
  var pc = s.getClass().getRecordComponents(); pc[0].getAccessor().setAccessible(true); pc[1].getAccessor().setAccessible(true);
  int px = (int) pc[0].getAccessor().invoke(s), pz = (int) pc[1].getAccessor().invoke(s);
  var lv = s.getClass().getRecordComponents()[5].getAccessor(); lv.setAccessible(true);
  int[] levels = (int[]) lv.invoke(s); if (levels == null) continue;
  sites++;
  if (sites <= 10) System.out.println("terraces at " + px + " " + pz + " levels " + java.util.Arrays.toString(levels));
  for (int z = pz - 80; z <= pz + 80; z++) for (int x = px - 80; x <= px + 80; x++) {
    var a = t.relaxedColumn(x, z); if (a.cover() != Terrain.Cover.TRAVERTINE || a.water() == Terrain.Column.NO_WATER) continue;
    pools++;
    for (int[] o : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) { var b = t.relaxedColumn(x + o[0], z + o[1]);
      if (b.water() == Terrain.Column.NO_WATER && b.lava() == Terrain.Column.NO_WATER && Math.max(b.ground(), b.ice()) < a.water()) { if (spills++ < 5) System.out.println("  spill " + x + " " + z + " water " + a.water() + " next " + b.ground()); } }
  }
}
System.out.println("travertine sites " + sites + ", pool columns " + pools + ", spill edges " + spills);
/exit
