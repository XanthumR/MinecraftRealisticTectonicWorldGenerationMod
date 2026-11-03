import com.realtectonics.sim.*;
import java.awt.image.BufferedImage;
// Format 92 ore deposits: cells of each kind, the deposits map (out/deposits_map.png), and vertical slices through a
// porphyry stock, a kimberlite pipe and a banded iron field (out/ore_<kind>.png, 3 px a block), coloured by ore.
// Rock layers here are plain 6-block beds (the generator's own strata need Minecraft), so beds sit only roughly as in game.
// -Dplanet=file (else seed 777 is generated), -Dfmt=N.
var pf = System.getProperty("planet");
var d = pf == null ? PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}) : PlanetData.load(java.nio.file.Path.of(pf));
d.format = Integer.getInteger("fmt", d.format); var t = new Terrain(d, 777, 1, 1, 2);
byte[] kinds = t.depositCells();
int[] count = new int[Deposits.Kind.values().length];
for (byte k : kinds) if (k >= 0) count[k]++;
var ox = Terrain.class.getDeclaredField("originX"); ox.setAccessible(true); var oy = Terrain.class.getDeclaredField("originY"); oy.setAccessible(true);
int originX = (int) ox.get(t), originY = (int) oy.get(t);
int[] first = new int[count.length]; java.util.Arrays.fill(first, -1);
for (int g = 0; g < kinds.length; g++) if (kinds[g] >= 0 && first[kinds[g]] < 0) first[kinds[g]] = g;
for (var k : Deposits.Kind.values()) { int g = first[k.ordinal()];
  System.out.println(k + ": " + count[k.ordinal()] + " cells" + (g < 0 ? "" : ", first at x " + Math.floorMod(g % d.w - originX, d.w) * 32 + " z " + (g / d.w - originY) * 32)); }
var map = new BufferedImage(d.w, d.h, BufferedImage.TYPE_INT_RGB); map.setRGB(0, 0, d.w, d.h, t.depositColors(), 0, d.w);
javax.imageio.ImageIO.write(map, "png", new java.io.File("out/deposits_map.png"));
int[] ORE = {0, 0xd07a30, 0xf0d040, 0xb03020, 0x181818, 0x60e0e0, 0x3050c0, 0xe02020, 0x506878, 0xf4f4f4};
void slice(String name, int cx, int cz) throws Exception {
  int half = 48, lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE, ores = 0;
  var cs = new Terrain.Column[2 * half + 1]; var ls = new Deposits.Lode[cs.length];
  for (int i = 0; i < cs.length; i++) { cs[i] = t.relaxedColumn(cx - half + i, cz); ls[i] = t.lode(cx - half + i, cz, cs[i]); hi = Math.max(hi, Math.max(cs[i].ground(), cs[i].water())); lo = Math.min(lo, cs[i].ground()); }
  int top = hi + 4, bottom = lo - 210, s = 3;
  var img = new BufferedImage(cs.length * s, (top - bottom) * s, BufferedImage.TYPE_INT_RGB);
  for (int i = 0; i < cs.length; i++) for (int y = bottom; y < top; y++) { var c = cs[i]; int rgb;
    if (y > c.ground()) rgb = c.water() >= y ? 0x3c78dc : 0xc0d8f0;
    else { rgb = y < 0 ? 0x505058 : y > c.ground() - 4 ? 0x7a6040 : 0x8a8a8a;
      if (ls[i] != null) { int layer = Math.floorDiv(y + c.fold(), 6), lh = (layer * 0x9E3779B1) >>> 16;
        long hh = ((cx - half + i) * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (cz * 0x165667B19E3779F9L); hh = (hh ^ (hh >>> 31)) * 0xBF58476D1CE4E5B9L;
        var o = Deposits.ore(ls[i], y, c.ground(), lh, ((hh >>> 40) & 0xFFFF) / 65536f);
        if (o != Deposits.Ore.NONE) { rgb = ORE[o.ordinal()]; if (o.ordinal() < 8) ores++; }
        if (y == c.ground() && Deposits.sign(ls[i], c.ground(), lh) != Deposits.Sign.NONE) rgb = 0xe07020; } }
    for (int a = 0; a < s; a++) for (int b = 0; b < s; b++) img.setRGB(i * s + a, (top - 1 - y) * s + b, rgb); }
  javax.imageio.ImageIO.write(img, "png", new java.io.File("out/ore_" + name + ".png"));
  System.out.println(name + " slice at x " + cx + " z " + cz + ": " + ores + " ore blocks in the slice");
}
// A porphyry: the extinct arc volcano of the first porphyry cell, its stock under the cone's middle.
int pg = first[Deposits.Kind.PORPHYRY.ordinal()], bx = Math.floorMod(pg % d.w - originX, d.w) * 32, bz = (pg / d.w - originY) * 32, px = bx, pz = bz; float best = 9;
for (int z = bz - 48; z <= bz + 48; z += 2) for (int x = bx - 48; x <= bx + 48; x += 2) { var l = t.lode(x, z, t.relaxedColumn(x, z)); if (l != null && l.stock() < best) { best = l.stock(); px = x; pz = z; } }
slice("porphyry", px, pz);
// A kimberlite pipe: searched over the first pipe cell.
int kg = first[Deposits.Kind.KIMBERLITE.ordinal()]; int kx = 0, kz = 0; float kd = Float.MAX_VALUE;
if (kg >= 0) { int cx = Math.floorMod(kg % d.w - originX, d.w) * 32, cz = (kg / d.w - originY) * 32;
  for (int z = cz - 48; z <= cz + 48; z++) for (int x = cx - 48; x <= cx + 48; x++) { var l = t.lode(x, z, t.relaxedColumn(x, z)); if (l != null && l.pipe() < kd) { kd = l.pipe(); kx = x; kz = z; } }
  slice("kimberlite", kx, kz); }
int ig = first[Deposits.Kind.IRON.ordinal()];
if (ig >= 0) slice("iron", Math.floorMod(ig % d.w - originX, d.w) * 32, (ig / d.w - originY) * 32);
/exit
