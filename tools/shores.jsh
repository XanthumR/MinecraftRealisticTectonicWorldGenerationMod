import com.realtectonics.sim.*;
// Lake shore cliffs (dry ground 6+ above the water right beside a lake) and hot spring towers (sinter or pool
// standing 4+ above the ground 3 blocks out), for -Dfmt.
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); d.format = Integer.getInteger("fmt", d.format); var t = new Terrain(d, 777, 1, 1, 2);
var rnd = new java.util.Random(13); int lakeCols = 0, cliffs = 0, springs = 0, towers = 0; int[] cliffAt = null, towerAt = null;
for (int i = 0; i < 3000000; i++) { int x = rnd.nextInt(t.circumference) - t.circumference / 2, z = rnd.nextInt(t.southPoleZ() - t.northPoleZ()) + t.northPoleZ();
  var c = t.column(x, z);
  if (c.water() > Terrain.SEA_Y + 2 && c.water() > c.ground()) { lakeCols++;
    for (int[] o : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) { var n = t.column(x + o[0], z + o[1]); if (n.water() == Terrain.Column.NO_WATER && n.ice() == Terrain.Column.NO_WATER && n.ground() - c.water() >= 6) { cliffs++; if (cliffAt == null) cliffAt = new int[]{x, z}; break; } } }
  if (c.cover() == Terrain.Cover.SPRING || c.cover() == Terrain.Cover.SINTER) { springs++; int lo = c.ground();
    for (int[] o : new int[][]{{3,0},{-3,0},{0,3},{0,-3}}) lo = Math.min(lo, t.column(x + o[0], z + o[1]).ground());
    if (Math.max(c.ground(), c.water()) - lo >= 4) { towers++; if (towerAt == null) towerAt = new int[]{x, z}; } } }
System.out.printf("fmt %d: lake columns %d, at a cliff %d (%.1f%%) %s; spring columns %d, towering %d %s%n", d.format, lakeCols, cliffs, 100.0 * cliffs / Math.max(1, lakeCols), java.util.Arrays.toString(cliffAt), springs, towers, java.util.Arrays.toString(towerAt));
/exit
