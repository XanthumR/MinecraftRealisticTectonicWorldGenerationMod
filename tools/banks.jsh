import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
var rnd = new java.util.Random(9); int cols = 0, oldBare = 0, newBare = 0, bank = 0, picked = 0;
while (picked < 60) { int g = rnd.nextInt(d.w * d.h); if (d.metres[g] < 100 || d.flow[g] < 200 || d.ice[g] != 0) continue; picked++; int[] b = t.blockOf(g);
  for (int dz = -40; dz <= 40; dz += 2) for (int dx = -40; dx <= 40; dx += 2) { var c = t.column(b[0]+dx, b[1]+dz); if (c.water() != Terrain.Column.NO_WATER) continue;
    boolean nearWater = false; int slope = 0;
    for (int[] o : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) { var n = t.column(b[0]+dx+o[0], b[1]+dz+o[1]); slope = Math.max(slope, Math.abs(n.ground() - c.ground())); }
    for (int k = -6; k <= 6 && !nearWater; k += 3) for (int j = -6; j <= 6; j += 3) if (t.column(b[0]+dx+k, b[1]+dz+j).water() != Terrain.Column.NO_WATER) { nearWater = true; break; }
    if (!nearWater) continue; cols++;
    if (slope >= 3 && c.biome() != Biome.DESERT) oldBare++;
    if ((slope >= 4 || c.cover() == Terrain.Cover.CLIFF && slope >= 2) && c.biome() != Biome.DESERT) newBare++;
    if (c.cover() == Terrain.Cover.BANK) bank++; } }
System.out.printf("dry columns within 6 blocks of river water: %d; bare rock before %d (%.0f%%), now %d (%.0f%%); sand/gravel/mud banks %d (%.0f%%)%n", cols, oldBare, 100.0*oldBare/cols, newBare, 100.0*newBare/cols, bank, 100.0*bank/cols);
/exit
