import com.realtectonics.sim.*;
import java.lang.reflect.*;
// Rockfall must never pile rubble above the rock it fell from: count tile columns higher than every original column within 3.
var d = System.getProperty("planet") == null ? PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}) : PlanetData.load(java.nio.file.Path.of(System.getProperty("planet")));
d.format = Integer.getInteger("fmt", d.format); var t = new Terrain(d, Long.getLong("seed", 777), 1, 1, 2);
Method rt = Terrain.class.getDeclaredMethod("relaxTile", int.class, int.class); rt.setAccessible(true);
var tf = Terrain.class.getDeclaredField("RELAX_TILE"); tf.setAccessible(true); int T = (int) tf.get(null), bx = Integer.getInteger("cx", -7600), bz = Integer.getInteger("cz", 240), over = 0, worst = 0, R = Integer.getInteger("tiles", 3); int[] at = null;
for (int tz = -R; tz <= R; tz++) for (int tx = -R; tx <= R; tx++) { int x0 = bx + tx * T, z0 = bz + tz * T; var tile = (Terrain.Column[]) rt.invoke(t, x0, z0);
  int[] raw = new int[(T + 6) * (T + 6)]; for (int i = 0; i < raw.length; i++) raw[i] = t.column(x0 - 3 + i % (T + 6), z0 - 3 + i / (T + 6)).ground();
  for (int z = 0; z < T; z++) for (int x = 0; x < T; x++) { int hi = Integer.MIN_VALUE; for (int dz = 0; dz <= 6; dz++) for (int dx = 0; dx <= 6; dx++) hi = Math.max(hi, raw[x + dx + (z + dz) * (T + 6)]);
    int g = tile[x + z * T].ground(); if (g > hi) { over++; if (g - hi > worst) { worst = g - hi; at = new int[]{x0 + x, z0 + z}; } } } }
System.out.println("columns above all nearby rock: " + over + ", worst by " + worst + " at " + java.util.Arrays.toString(at));
/exit
