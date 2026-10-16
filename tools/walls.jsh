import com.realtectonics.sim.*;
// Lowland walls: a 12+ block step in one block on low land near water. Format by -Dfmt.
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); d.format = Integer.getInteger("fmt", 20); var t = new Terrain(d, 777, 1, 1, 2);
var rnd = new java.util.Random(3); int land = 0, walls = 0; java.util.List<String> at = new java.util.ArrayList<>();
for (int i = 0; i < 2000000; i++) { int x = rnd.nextInt(t.circumference) - t.circumference / 2, z = rnd.nextInt(t.southPoleZ() - t.northPoleZ()) + t.northPoleZ();
  var c = t.column(x, z); if (c.ground() < Terrain.SEA_Y + 2 || c.ground() > Terrain.SEA_Y + 50 || c.water() != Terrain.Column.NO_WATER) continue; land++;
  for (int[] o : new int[][]{{1,0},{0,1}}) { var n = t.column(x + o[0], z + o[1]); if (n.water() == Terrain.Column.NO_WATER && Math.abs(n.ground() - c.ground()) >= 12 && c.cover() != Terrain.Cover.CLIFF && n.cover() != Terrain.Cover.CLIFF) { walls++; if (at.size() < 6) at.add(x + " " + z + " " + c.ground() + "/" + n.ground() + " " + c.biome()); } } }
System.out.println("fmt " + d.format + " lowland " + land + " walls " + walls + " " + at);
/exit
