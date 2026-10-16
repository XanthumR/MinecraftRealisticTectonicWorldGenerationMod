import com.realtectonics.sim.*;
// Cross-section through the highest fold-belt mountain (Terrain.orogen, volcanoes excluded) of seed 777: rock beds as the generator lays them
// (TectonicChunkGenerator.rock), format -Dfmt. Bands alternate dark/light by bed; sky blue above the ground.
var d = PlanetData.load(java.nio.file.Path.of(System.getProperty("planet", "out/p777.bin"))); d.format = Integer.getInteger("fmt", d.format);
var t = new Terrain(d, 777, 1, 1, 2);
var ox = Terrain.class.getDeclaredField("originX"); ox.setAccessible(true); var oy = Terrain.class.getDeclaredField("originY"); oy.setAccessible(true);
var of = Terrain.class.getDeclaredField("orogen"); of.setAccessible(true); float[] O = (float[]) of.get(t);
int best = -1; for (int g = 0; g < d.metres.length; g++) if (O[g] > 0.6f && (best < 0 || d.metres[g] > d.metres[best])) best = g; // a fold belt, not a volcano
int cx = Integer.getInteger("cx", Math.floorMod((best % d.w - (int) ox.get(t)) * 32, d.w * 32)), cz = Integer.getInteger("cz", (best / d.w - (int) oy.get(t)) * 32);
System.out.println("section at z " + cz + ", x around " + cx + ", " + d.metres[best] + " m");
int[] bed = new int[1024]; int beds; { var r = new java.util.Random(34); int b = 0; for (int y = 0; y < 1024; b++) for (int k = 3 + r.nextInt(8); k > 0 && y < 1024; k--) bed[y++] = b; beds = b; }
int W = 768, lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE; var cs = new Terrain.Column[W];
for (int x = 0; x < W; x++) { cs[x] = t.relaxedColumn(cx - W / 2 + x, cz); lo = Math.min(lo, cs[x].ground()); hi = Math.max(hi, cs[x].ground()); }
lo -= 60; int H = hi - lo + 10; var img = new java.awt.image.BufferedImage(W, H * 2, 1);
for (int x = 0; x < W; x++) for (int y = lo; y < lo + H; y++) { var c = cs[x]; int rgb;
  if (y > c.ground()) rgb = 0xbcd4e6; else { int tt = y + c.fold(); int layer = d.format >= 34 ? Math.floorDiv(tt, 1024) * beds + bed[Math.floorMod(tt, 1024)] : Math.floorDiv(tt, 6);
    int hsh = (layer * 0x9E3779B1) >>> 16; int v = 70 + (hsh % 7) * 25; rgb = v << 16 | v << 8 | v; }
  int py = (lo + H - 1 - y) * 2; img.setRGB(x, py, rgb); img.setRGB(x, py + 1, rgb); }
javax.imageio.ImageIO.write(img, "png", new java.io.File("out/" + System.getProperty("name", "strata") + ".png"));
/exit
