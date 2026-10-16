import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
int cx = Integer.getInteger("cx"), cz = Integer.getInteger("cz"); d.format = Integer.getInteger("fmt", d.format); t = new Terrain(d, 777, 1, 1, 2);
for (int z = cz - 8; z <= cz + 8; z++) { var sb = new StringBuilder();
  for (int x = cx - 8; x <= cx + 8; x++) { var c = t.column(x, z); sb.append(String.format("%4d%-3s", c.ground(), c.water() != Terrain.Column.NO_WATER ? "w" : c.ice() != Terrain.Column.NO_WATER ? "i" + (c.ice() - c.ground()) : " ")); }
  System.out.println(sb); }
/exit
