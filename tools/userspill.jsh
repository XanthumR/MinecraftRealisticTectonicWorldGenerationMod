import com.realtectonics.sim.*;
var d = PlanetData.load(java.nio.file.Path.of("run/saves/New World (2)/realtectonics/planet.bin")); d.format = 20;
var t = new Terrain(d, -5073067326693234624L, 1.1f, 4, 2.51f);
int px = -1239, pz = 136904, R = 400, spills = 0, wet = 0;
for (int z = pz - R; z <= pz + R; z++) for (int x = px - R; x <= px + R; x++) { var c = t.column(x, z); if (c.water() == Terrain.Column.NO_WATER || c.water() < c.ground()) continue; wet++;
  for (int[] o : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) { var n = t.column(x + o[0], z + o[1]); if (n.water() == Terrain.Column.NO_WATER && n.ground() < c.water()) spills++; } }
System.out.println("water columns " + wet + ", spill edges " + spills);
/exit
