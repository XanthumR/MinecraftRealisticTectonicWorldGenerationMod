import com.realtectonics.sim.*;
// A bare-rock wall (15+ in one block) that format 27 has and 28 does not, near the player.
var d = PlanetData.load(java.nio.file.Path.of("run/saves/New World (5)/realtectonics/planet.bin"));
d.format = 27; var a = new Terrain(d, 7616411802006901329L, 1, 1, 2); d.format = 28; var b = new Terrain(d, 7616411802006901329L, 1, 1, 2);
int px = 1214, pz = 2919, R = 1500, found = 0;
for (int z = pz - R; z <= pz + R && found < 3; z += 2) for (int x = px - R; x <= px + R && found < 3; x += 2) {
  var c = a.column(x, z); var n = a.column(x + 1, z);
  if (c.water() != Terrain.Column.NO_WATER || n.water() != Terrain.Column.NO_WATER || c.ice() != Terrain.Column.NO_WATER || n.ice() != Terrain.Column.NO_WATER || Math.abs(c.ground() - n.ground()) < 15) continue;
  if (Math.abs(b.column(x, z).ground() - b.column(x + 1, z).ground()) < 6) { found++; System.out.println("gone " + x + " " + z + " was " + c.ground() + "/" + n.ground() + " now " + b.column(x, z).ground() + "/" + b.column(x + 1, z).ground()); } }
/exit
