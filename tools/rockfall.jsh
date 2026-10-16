import com.realtectonics.sim.*;
import java.lang.reflect.*;
// Rockfall (format 29): tiles agree where they overlap (seams), time per tile, leaks, and how much ground moved.
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
Method rt = Terrain.class.getDeclaredMethod("relaxTile", int.class, int.class); rt.setAccessible(true);
var tf = Terrain.class.getDeclaredField("RELAX_TILE"); tf.setAccessible(true); int T = (int) tf.get(null), mism = 0;
// 1. seams: a tile shifted by half its width must give the same blocks where the two overlap
int bx = Integer.getInteger("cx", -7600), bz = Integer.getInteger("cz", 240);
var a = (Terrain.Column[]) rt.invoke(t, bx, bz); var b = (Terrain.Column[]) rt.invoke(t, bx + T / 2, bz + T / 2);
for (int z = T / 2; z < T; z++) for (int x = T / 2; x < T; x++) if (!a[x + z * T].equals(b[(x - T / 2) + (z - T / 2) * T])) mism++;
System.out.println("seam mismatches " + mism + " of " + (T / 2) * (T / 2));
// 2. time per tile, 3. leaks and change over a few tiles around the spot
long s = System.nanoTime(); int tiles = 0, changed = 0, scree = 0, leaks = 0, maxDrop = 0, cols = 0;
for (int tz = -1; tz <= 1; tz++) for (int tx = -1; tx <= 1; tx++) { int x0 = bx + tx * T, z0 = bz + tz * T; var tile = (Terrain.Column[]) rt.invoke(t, x0, z0); tiles++;
  for (int z = 1; z < T - 1; z++) for (int x = 1; x < T - 1; x++) { var c = tile[x + z * T]; var o = t.column(x0 + x, z0 + z); cols++;
    if (c.ground() != o.ground()) { changed++; maxDrop = Math.max(maxDrop, o.ground() - c.ground()); }
    if (c.cover() == Terrain.Cover.SCREE && o.cover() != Terrain.Cover.SCREE) scree++;
    if (c.water() == Terrain.Column.NO_WATER || c.water() < c.ground()) continue;
    for (int k : new int[]{-1, 1, -T, T}) { var n = tile[x + z * T + k]; if (n.water() == Terrain.Column.NO_WATER && c.water() - Math.max(n.ground(), n.ice()) > 1) leaks++; } } }
System.out.printf("%.0f ms per tile; columns changed %.1f%%, deepest cut %d, new scree %.1f%%, leak edges %d%n", (System.nanoTime() - s) / 1e6 / tiles, 100.0 * changed / cols, maxDrop, 100.0 * scree / cols, leaks);
/exit
