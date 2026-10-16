import com.realtectonics.sim.*;
// Ponds: count them on a land sample and check every pond's edge for water spilling onto lower dry ground.
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
var rnd = new java.util.Random(9); int found = 0, spills = 0, cols = 0; int[] at = null;
d.format = 16; var old = new Terrain(d, 777, 1, 1, 2); d.format = 17;
while (found < 60) {
    int x = rnd.nextInt(t.circumference) - t.circumference / 2, z = rnd.nextInt(t.southPoleZ() - t.northPoleZ()) + t.northPoleZ();
    var c = t.column(x, z);
    if (c.cover() != Terrain.Cover.POND || c.water() == Terrain.Column.NO_WATER) continue;
    found++; if (at == null) at = new int[]{x, z};
    for (int dz = -14; dz <= 14; dz++) for (int dx = -14; dx <= 14; dx++) {
        var a = t.column(x + dx, z + dz); if (a.cover() != Terrain.Cover.POND || a.water() == Terrain.Column.NO_WATER) continue; cols++;
        for (int[] o : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) { var b = t.column(x + dx + o[0], z + dz + o[1]);
            if (b.water() == Terrain.Column.NO_WATER && b.ground() < a.water()) { spills++; if (spills < 12) System.out.println("spill water " + a.water() + " ground " + a.ground() + " -> dry " + b.ground() + " at " + (x + dx) + "," + (z + dz) + " biome " + a.biome() + " cover " + b.cover()); } }
    }
}
System.out.println("ponds hit " + found + ", water columns near them " + cols + ", spill edges " + spills + ", first " + java.util.Arrays.toString(at));
/exit
