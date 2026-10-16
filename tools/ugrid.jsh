import com.realtectonics.sim.*;
// Ground and water level (wNN) around -Dcx -Dcz on the seed-777 planet, -Dr half-size.
var d = PlanetData.load(java.nio.file.Path.of("run/saves/New World (5)/realtectonics/planet.bin")); d.format = Integer.getInteger("fmt", d.format); var t = new Terrain(d, 7616411802006901329L, 1, 1, 2);
int cx = Integer.getInteger("cx"), cz = Integer.getInteger("cz"), R = Integer.getInteger("r", 6);
for (int z = cz - R; z <= cz + R; z++) { var sb = new StringBuilder(); for (int x = cx - R; x <= cx + R; x++) { var c = t.column(x, z); sb.append(String.format("%4d%-5s", c.ground(), c.water() != Terrain.Column.NO_WATER ? "w" + c.water() : c.ice() != Terrain.Column.NO_WATER ? "i" + c.ice() : "")); } System.out.println(sb); }
/exit
