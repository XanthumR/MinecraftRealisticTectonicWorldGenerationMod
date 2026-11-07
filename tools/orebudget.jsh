import com.realtectonics.sim.*;
import java.util.*;
import java.util.stream.*;
// Format 92 ore budget: the deposits' ore per chunk averaged over the whole planet's land, by ore, to set against
// vanilla's (thinned to ORE_KEEP outside deposits). One column per chunk (16-block grid) for beds, veins and placers;
// kimberlite pipes and porphyry stocks, too small for that grid, block by block (every 2 for stocks) round their cells.
// Strata as the generator's (format 34 beds, Random(34)), caves not cut. -Dplanet=file (else seed 777), -Dfmt=N.
var pf = System.getProperty("planet");
var d = pf == null ? PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}) : PlanetData.load(java.nio.file.Path.of(pf));
d.format = Integer.getInteger("fmt", d.format); var t = new Terrain(d, 777, 1, 1, 2);
int[] BED = new int[1024]; int beds = 0;
{ var r = new Random(34); for (int y = 0; y < 1024; beds++) for (int k = 3 + r.nextInt(8); k > 0 && y < 1024; k--) BED[y++] = beds; }
final int BEDS = beds;
int layerHash(Terrain.Column c, int y) { int s = y + c.fold(); return ((Math.floorDiv(s, 1024) * BEDS + BED[Math.floorMod(s, 1024)]) * 0x9E3779B1) >>> 16; }
float oreHash(int x, int y, int z) { long h = (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L); h = (h ^ (h >>> 31)) * 0xBF58476D1CE4E5B9L; return ((h >>> 40) & 0xFFFF) / 65536f; }
int N = Deposits.Ore.values().length;
// adds column (x, z)'s ore to n, as the generator places it (below ground - 4, or a river bed's top 2 blocks)
void count(long[] n, int x, int z, Terrain.Column c, Deposits.Lode l) {
  for (int y = Terrain.MIN_Y + 1; y <= c.ground(); y++) if (y < c.ground() - 4 || y >= c.ground() - 1) n[Deposits.ore(l, y, c.ground(), layerHash(c, y), oreHash(x, y, z)).ordinal()]++; }
var ox = Terrain.class.getDeclaredField("originX"); ox.setAccessible(true); var oy = Terrain.class.getDeclaredField("originY"); oy.setAccessible(true);
int originX = (int) ox.get(t), originY = (int) oy.get(t), W = d.w * 32;
long t0 = System.currentTimeMillis();
// the coarse pass: one column per chunk, stocks and pipes left out (the fine passes count them)
long[][] rows = IntStream.range(0, d.h * 2).parallel().mapToObj(row -> { long[] n = new long[N + 1]; int z = (row - 2 * originY) * 16 + 8;
  for (int x = 8; x < W; x += 16) { var c = t.column(x, z); if (c.ground() < Terrain.SEA_Y) continue; n[N]++; var l = t.lode(x, z, c);
    if (l != null) count(n, x, z, c, new Deposits.Lode(l.bed(), 1, 0, Float.MAX_VALUE, l.pipeRadius(), l.vein(), l.placer())); }
  return n; }).toArray(long[][]::new);
long[] coarse = new long[N + 1]; for (long[] r : rows) for (int i = 0; i <= N; i++) coarse[i] += r[i];
long landChunks = coarse[N];
// the fine passes round each porphyry and kimberlite cell; a set keeps neighbouring cells' windows from counting twice
byte[] kinds = t.depositCells(); long[] fine = new long[N]; var seen = new HashSet<Long>();
for (int g = 0; g < kinds.length; g++) {
  boolean pipe = kinds[g] == Deposits.Kind.KIMBERLITE.ordinal(); if (!pipe && kinds[g] != Deposits.Kind.PORPHYRY.ordinal()) continue;
  int cx = Math.floorMod(g % d.w - originX, d.w) * 32, cz = (g / d.w - originY) * 32, step = pipe ? 1 : 2, reach = pipe ? 40 : 64;
  for (int z = cz - reach; z <= cz + reach; z += step) for (int x = cx - reach; x <= cx + reach; x += step) {
    if (!seen.add((long) x << 32 ^ (z & 0xffffffffL) ^ (pipe ? 1L << 62 : 0))) continue;
    var c = t.column(x, z); var l = t.lode(x, z, c); if (l == null || (pipe ? l.pipe() >= l.pipeRadius() : l.stock() >= 1)) continue;
    long[] n = new long[N]; count(n, x, z, c, pipe ? new Deposits.Lode(Deposits.Ore.NONE, 1, 0, l.pipe(), l.pipeRadius(), false, 0) : new Deposits.Lode(Deposits.Ore.NONE, l.stock(), l.stockTop(), Float.MAX_VALUE, l.pipeRadius(), false, 0));
    for (int i = 0; i < N; i++) fine[i] += n[i] * step * step; } }
System.out.printf("%d land chunks, %.0f s%n", landChunks, (System.currentTimeMillis() - t0) / 1000.0);
// vanilla per chunk on plain land (format 91, x 300 z 300, 3x3 chunks: tools/orecount.py), and what thinning keeps of it
String[] ORES = {"coal", "iron", "copper", "gold", "redstone", "lapis", "diamond"};
Deposits.Ore[] ORE = {Deposits.Ore.COAL, Deposits.Ore.IRON, Deposits.Ore.COPPER, Deposits.Ore.GOLD, Deposits.Ore.REDSTONE, Deposits.Ore.LAPIS, Deposits.Ore.DIAMOND};
int[] VANILLA = {289, 105, 118, 30, 34, 25, 32}; int[] KEPT = {112, 44, 46, 11, 14, 10, 15};
System.out.println("ore       vanilla  kept  +deposits  = total  (total/vanilla)  deposit ore on the planet");
for (int i = 0; i < ORE.length; i++) { int o = ORE[i].ordinal(); long all = coarse[o] * 256 + fine[o]; double per = all / (double) landChunks; // a coarse column stands for its chunk's 256
  System.out.printf("%-9s %7d %5d %10.1f %8.1f %10.2f %16d%n", ORES[i], VANILLA[i], KEPT[i], per, KEPT[i] + per, (KEPT[i] + per) / VANILLA[i], all); }
System.exit(0);
