import com.realtectonics.sim.*;
// Near the player in the user's newest world: water-to-water steps of 4+ (lake terraces) and dry walls of 15+ in one block.
var d = PlanetData.load(java.nio.file.Path.of("run/saves/New World (5)/realtectonics/planet.bin")); d.format = Integer.getInteger("fmt", d.format);
var t = new Terrain(d, 7616411802006901329L, 1, 1, 2);
int px = 1214, pz = 2919, R = 600, steps = 0, walls = 0; String s1 = null, w1 = null;
for (int z = pz - R; z <= pz + R; z += 2) for (int x = px - R; x <= px + R; x += 2) { var c = t.column(x, z); var n = t.column(x + 1, z);
  int ct = Math.max(Math.max(c.water(), c.ice()), c.ground()), nt = Math.max(Math.max(n.water(), n.ice()), n.ground());
  if (c.water() != Terrain.Column.NO_WATER && n.water() != Terrain.Column.NO_WATER && Math.abs(c.water() - n.water()) >= 4) { steps++; if (s1 == null) s1 = x + " " + z; }
  if (Math.abs(ct - nt) >= 15) { walls++; if (w1 == null) w1 = x + " " + z + " " + ct + "/" + nt; } }
System.out.println("fmt " + d.format + ": water steps " + steps + " (" + s1 + "), walls " + walls + " (" + w1 + ")");
/exit
