import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
int cx = Integer.getInteger("cx"), cz = Integer.getInteger("cz"), n = 1024, hits = 0, stacks = 0; int[] first = null, firstStack = null;
for (int z = cz - n / 2; z < cz + n / 2; z += 4) for (int x = cx - n / 2; x < cx + n / 2; x += 4) {
  var c = t.column(x, z); if (c.water() != Terrain.Column.NO_WATER || c.ground() < Terrain.SEA_Y || c.ground() > Terrain.SEA_Y + 2) continue;
  int wet = 0; for (int[] o : new int[][]{{12,0},{-12,0},{0,12},{0,-12}}) if (t.column(x + o[0], z + o[1]).water() != Terrain.Column.NO_WATER) wet++;
  if (wet >= 2) { hits++; if (first == null) first = new int[]{x, z}; } }
System.out.println("sandbar-like columns " + hits + " first " + java.util.Arrays.toString(first));
/exit
