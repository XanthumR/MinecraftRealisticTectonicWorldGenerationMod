import com.realtectonics.sim.*;
// Top-down map of the game terrain (column(), no rockfall, fast): biome colours, hillshade, water blue, ice pale, and
// red where a neighbour stands more than 12 blocks per step higher (walls). -Dplanet -Dseed -Dms -Dzoom -Drw -Dfmt -Dcx -Dcz -Dn -Dst (stride) -Dname
var d = PlanetData.load(java.nio.file.Path.of(System.getProperty("planet")));
d.format = Integer.getInteger("fmt", d.format); var t = new Terrain(d, Long.getLong("seed", 777), Float.parseFloat(System.getProperty("ms", "1")), Integer.getInteger("zoom", 1), Float.parseFloat(System.getProperty("rw", "2")));
int cx = Integer.getInteger("cx"), cz = Integer.getInteger("cz"), n = Integer.getInteger("n", 1536), st = Integer.getInteger("st", 2);
int m = n / st; var cs = new Terrain.Column[m * m];
java.util.stream.IntStream.range(0, m).parallel().forEach(z -> { for (int x = 0; x < m; x++) cs[x + z * m] = t.column(cx - n / 2 + x * st, cz - n / 2 + z * st); });
var img = new java.awt.image.BufferedImage(m, m, 1); int walls = 0;
for (int z = 1; z < m - 1; z++) for (int x = 1; x < m - 1; x++) { var c = cs[x + z * m];
  int top = Math.max(Math.max(c.ground(), c.water()), c.ice());
  int e = Math.max(Math.max(cs[x + 1 + z * m].ground(), cs[x + 1 + z * m].ice()), cs[x + 1 + z * m].water());
  double sh = Math.max(0.3, Math.min(1.2, 0.8 + (top - e) * 0.05 / st));
  int rgb = c.water() != Terrain.Column.NO_WATER && c.water() >= c.ground() ? 0x3c78dc : c.ice() != Terrain.Column.NO_WATER ? 0xdcf0ff : c.biome().color;
  int dmax = 0; for (int k : new int[]{1, -1, m, -m}) { var o = cs[x + z * m + k]; dmax = Math.max(dmax, Math.max(Math.max(o.ground(), o.ice()), o.water()) - top); }
  if (dmax > 12 * st) { rgb = 0xff0000; walls++; }
  int r = (int) Math.min(255, (rgb >> 16 & 255) * sh), g = (int) Math.min(255, (rgb >> 8 & 255) * sh), b = (int) Math.min(255, (rgb & 255) * sh);
  img.setRGB(x, z, r << 16 | g << 8 | b); }
img.setRGB(m / 2, m / 2, 0xff00ff);
System.out.println("wall pixels " + walls);
javax.imageio.ImageIO.write(img, "png", new java.io.File("out/" + System.getProperty("name", "top") + ".png"));
/exit
