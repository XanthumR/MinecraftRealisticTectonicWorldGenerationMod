import com.realtectonics.sim.*;
// Water next to lower dry ground (glacier ice counts as a wall) in a box of the game's terrain (relaxedColumn):
// -Dplanet -Dfmt -Dseed -Dcx -Dcz -Dn [-Dcliff]. For checking a new feature's own area; prints the count and the first few spots.
var d = PlanetData.load(java.nio.file.Path.of(System.getProperty("planet"))); d.format = Integer.getInteger("fmt", d.format);
var t = new Terrain(d, Long.getLong("seed", 777), 1, 1, 2, Float.parseFloat(System.getProperty("cliff", "24")));
int cx = Integer.getInteger("cx"), cz = Integer.getInteger("cz"), n = Integer.getInteger("n", 384), x0 = cx - n / 2, z0 = cz - n / 2;
var cs = new Terrain.Column[(n + 2) * (n + 2)];
for (int i = 0; i < cs.length; i++) cs[i] = t.relaxedColumn(x0 - 1 + i % (n + 2), z0 - 1 + i / (n + 2));
int spills = 0, water = 0; var at = new java.util.ArrayList<String>();
for (int z = 1; z <= n; z++) for (int x = 1; x <= n; x++) {
    var c = cs[x + z * (n + 2)];
    if (c.water() == Terrain.Column.NO_WATER || c.water() < c.ground()) continue;
    water++;
    for (int k : new int[]{-1, 1, -(n + 2), n + 2}) {
        var o = cs[x + z * (n + 2) + k];
        int wall = Math.max(o.ground(), o.ice() == Terrain.Column.NO_WATER ? Integer.MIN_VALUE : o.ice());
        if (o.water() == Terrain.Column.NO_WATER && wall < c.water()) { spills++; if (at.size() < 5) at.add((x0 - 1 + x) + " " + (z0 - 1 + z)); }
    }
}
System.out.println("water columns " + water + ", spill edges " + spills + " " + at);
/exit
