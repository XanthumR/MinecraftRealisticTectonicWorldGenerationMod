import com.realtectonics.sim.*;
// Lake shores: around salt flats and other lakes, water must never sit beside lower dry ground.
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
var rnd = new java.util.Random(11); int found = 0, spills = 0, tries = 0; int[] salt = null;
while (found < 25 && tries++ < 3000000) {
    int x = rnd.nextInt(t.circumference) - t.circumference / 2, z = rnd.nextInt(t.southPoleZ() - t.northPoleZ()) + t.northPoleZ();
    var c = t.column(x, z);
    boolean isSalt = c.cover() == Terrain.Cover.SALT;
    if (!isSalt && !(c.water() > Terrain.SEA_Y + 3 && c.water() - c.ground() > 3)) continue; // a salt flat or an upland lake
    found++; if (isSalt && salt == null) salt = new int[]{x, z};
    for (int dz = -48; dz <= 48; dz += 1) for (int dx = -48; dx <= 48; dx++) {
        var a = t.column(x + dx, z + dz); if (a.water() == Terrain.Column.NO_WATER || a.water() < a.ground() || a.water() <= Terrain.SEA_Y) continue;
        var b = t.column(x + dx + 1, z + dz); if (b.water() == Terrain.Column.NO_WATER && Math.max(b.ground(), b.ice()) < a.water() - 1) spills++;
    }
}
System.out.println("sites " + found + ", spills " + spills + ", a salt flat at " + java.util.Arrays.toString(salt));
/exit
