import com.realtectonics.sim.*;
// Top-down hillshade of the game's terrain (relaxedColumn): -Dplanet -Dseed -Dms -Drw -Dfmt -Dcx -Dcz -Dn -Dname. Water blue.
var d = PlanetData.load(java.nio.file.Path.of(System.getProperty("planet"))); d.format = Integer.getInteger("fmt", d.format);
var t = new Terrain(d, Long.getLong("seed", 777), Float.parseFloat(System.getProperty("ms", "1")), 1, Float.parseFloat(System.getProperty("rw", "2")));
int cx = Integer.getInteger("cx"), cz = Integer.getInteger("cz"), n = Integer.getInteger("n", 384);
var cs = new Terrain.Column[n * n];
// Rockfall tiles one by one first (each builds its rows in parallel): many at once from the parallel loop below starve the
// common pool and hang when the view spans several tiles (as in uview3d.jsh).
for (int z = cz - n / 2; z < cz + n / 2 + 256; z += 256) for (int x = cx - n / 2; x < cx + n / 2 + 256; x += 256) t.relaxedColumn(Math.min(x, cx + n / 2 - 1), Math.min(z, cz + n / 2 - 1));
java.util.stream.IntStream.range(0, n).parallel().forEach(z -> { for (int x = 0; x < n; x++) cs[x + z * n] = t.relaxedColumn(cx - n / 2 + x, cz - n / 2 + z); });
var img = new java.awt.image.BufferedImage(n, n, 1);
for (int z = 1; z < n - 1; z++) for (int x = 1; x < n - 1; x++) { var c = cs[x + z * n];
  if (c.water() != Terrain.Column.NO_WATER) { img.setRGB(x, z, 0x3c78dc); continue; }
  float gx = cs[x + 1 + z * n].ground() - cs[x - 1 + z * n].ground(), gz = cs[x + (z + 1) * n].ground() - cs[x + (z - 1) * n].ground();
  float l = (float) Math.clamp(0.6 + (-gx - gz) * 0.08, 0.05, 1); int v = (int) (l * 230); img.setRGB(x, z, v << 16 | v << 8 | v); }
javax.imageio.ImageIO.write(img, "png", new java.io.File("out/" + System.getProperty("name", "shade") + ".png"));
/exit
