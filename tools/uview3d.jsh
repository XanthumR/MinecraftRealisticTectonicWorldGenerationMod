import com.realtectonics.sim.*;
// Isometric block view of the game's terrain: -Dcx -Dcz centre, -Dn size in blocks, -Dname output, -Dv vertical exaggeration, -Dcliff tableland cliff height (format 77)
var d = PlanetData.load(java.nio.file.Path.of(System.getProperty("planet")));
d.format = Integer.getInteger("fmt", d.format); var t = new Terrain(d, Long.getLong("seed", 777), Float.parseFloat(System.getProperty("ms", "1")), Integer.getInteger("zoom", 1), Float.parseFloat(System.getProperty("rw", "2")), Float.parseFloat(System.getProperty("cliff", "24")));
int cx = Integer.getInteger("cx"), cz = Integer.getInteger("cz"), n = Integer.getInteger("n", 192);
double vx = Double.parseDouble(System.getProperty("v", "1"));
var cs = new Terrain.Column[n * n];
// Rockfall tiles one by one first (each builds its rows in parallel): many at once from the parallel loop below starve the
// common pool and hang when the view spans several tiles.
for (int z = cz - n / 2; z < cz + n / 2 + 256; z += 256) for (int x = cx - n / 2; x < cx + n / 2 + 256; x += 256) t.relaxedColumn(Math.min(x, cx + n / 2 - 1), Math.min(z, cz + n / 2 - 1));
java.util.stream.IntStream.range(0, n).parallel().forEach(z -> { for (int x = 0; x < n; x++) cs[x + z * n] = t.relaxedColumn(cx + x - n / 2, cz + z - n / 2); });
int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
for (var c : cs) { lo = Math.min(lo, c.ground()); hi = Math.max(hi, Math.max(Math.max(c.ground(), c.water()), c.ice())); }
int s = 3, W = 2 * n * s, H = n * s + (int) ((hi - lo) * s * vx) + 20;
var img = new java.awt.image.BufferedImage(W, H, 1);
var g = img.createGraphics(); g.setColor(new java.awt.Color(0xbcd4e6)); g.fillRect(0, 0, W, H);
for (int sum = 0; sum < 2 * n - 1; sum++)
  for (int x = Math.max(0, sum - n + 1); x <= Math.min(n - 1, sum); x++) {
    int z = sum - x; var c = cs[x + z * n];
    int top = Math.max(Math.max(c.ground(), c.water()), c.ice());
    int base = lo - 2;
    int sx = (x - z + n) * s, sy = (x + z) * s / 2 + (int) ((hi - top) * s * vx) + 10, hgt = (int) ((top - base) * s * vx);
    int rgb = c.water() >= c.ground() && c.water() != Terrain.Column.NO_WATER ? 0x3c78dc : c.ice() != Terrain.Column.NO_WATER ? (c.cover() == Terrain.Cover.MORAINE ? 0x4a4a50 : c.cover() == Terrain.Cover.BARE_ICE ? 0x8cc4e8 : 0xdcf0ff)
        : c.biome().color;
    if (c.water() == Terrain.Column.NO_WATER && c.ice() == Terrain.Column.NO_WATER) { // steep ground shows rock
      int sl = 0; for (int k : new int[]{-1, 1, -n, n}) { int i = x + z * n + k; if (i >= 0 && i < cs.length) sl = Math.max(sl, Math.abs(cs[i].ground() - c.ground())); }
      if (sl >= 3) rgb = 0x8a8580;
    }
    if (c.cover() == Terrain.Cover.SCREE) rgb = 0xd8b070;
    if (c.cover() == Terrain.Cover.SALT) rgb = 0xf4f4f0;
    g.setColor(new java.awt.Color(rgb)); g.fillRect(sx, sy, 2 * s, s);
    int r = rgb >> 16 & 255, gg = rgb >> 8 & 255, b = rgb & 255;
    g.setColor(new java.awt.Color((int) (r * 0.65), (int) (gg * 0.65), (int) (b * 0.65))); g.fillRect(sx, sy + s, s, Math.max(0, hgt));
    g.setColor(new java.awt.Color((int) (r * 0.8), (int) (gg * 0.8), (int) (b * 0.8))); g.fillRect(sx + s, sy + s, s, Math.max(0, hgt));
  }
javax.imageio.ImageIO.write(img, "png", new java.io.File("out/" + System.getProperty("name", "view") + ".png"));
/exit
