import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
var rnd = new java.util.Random(21); int found = 0, towers = 0, tries = 0;
while (found < 150 && tries++ < 20000000) {
    int x = rnd.nextInt(t.circumference) - t.circumference / 2, z = rnd.nextInt(t.southPoleZ() - t.northPoleZ()) + t.northPoleZ();
    var c = t.column(x, z); if (c.cover() != Terrain.Cover.POND || c.water() == Terrain.Column.NO_WATER) continue;
    found++; int low = Integer.MAX_VALUE;
    for (int dz = -16; dz <= 16; dz++) for (int dx = -16; dx <= 16; dx++) { var a = t.column(x + dx, z + dz);
        if (a.cover() != Terrain.Cover.POND) continue;
        for (int ez = -4; ez <= 4; ez++) for (int ex = -4; ex <= 4; ex++) { var b = t.column(x + dx + ex, z + dz + ez); if (b.water() == Terrain.Column.NO_WATER) low = Math.min(low, b.ground() - a.water()); } }
    if (low < -4) { towers++; if (towers < 5) System.out.println("tower " + x + " " + z + " drop " + low); }
}
System.out.println("ponds " + found + ", towers " + towers);
/exit
