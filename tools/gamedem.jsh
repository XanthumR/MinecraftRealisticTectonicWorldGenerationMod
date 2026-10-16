import com.realtectonics.sim.*;
// Exports the game's most mountainous boxes as elevation grids in metres (relaxedColumn, what the game builds), for
// tools/realdem.py to compare with real mountain ranges. Boxes are -Dbw x -Dbh cells (10 x 6: ~390 x 230 km at
// Medium), the -Dk highest by mean land elevation, not overlapping. Writes out/dem/game_<i>.f32 (row-major, little
// endian) and out/dem/game.txt (one line per box: name w h km-per-block). -Dtarget=2500 picks boxes of that mean land
// height instead of the highest. -Dplanet (default: generate -Dseed 777).
var path = System.getProperty("planet"); long seed = Long.getLong("seed", 777);
var d = path == null ? PlanetData.generate(seed, PlanetSettings.DEFAULT, x -> {}) : PlanetData.load(java.nio.file.Path.of(path));
var t = new Terrain(d, seed, Float.parseFloat(System.getProperty("ms", "1")), Integer.getInteger("zoom", 1), Float.parseFloat(System.getProperty("rw", "2")));
double target = Double.parseDouble(System.getProperty("target", "0")); int bw = Integer.getInteger("bw", 10), bh = Integer.getInteger("bh", 6), k = Integer.getInteger("k", 4), W = d.w, H = d.h;
double kmPerBlock = 40030.0 / (W * Terrain.BLOCKS_PER_CELL * Integer.getInteger("zoom", 1));
boolean[] used = new boolean[W * H]; var meta = new StringBuilder();
for (int i = 0; i < k; i++) {
  double best = Double.NEGATIVE_INFINITY; int bx = 0, by = 0;
  for (int y = H / 8; y + bh < H * 7 / 8; y++) for (int x = 0; x < W; x++) { double s = 0; boolean clash = false;
    for (int dy = 0; dy < bh && !clash; dy++) for (int dx = 0; dx < bw; dx++) { int g = (x + dx) % W + (y + dy) * W; if (used[g]) { clash = true; break; } s += Math.max(0, d.metres[g]); }
    double mean = s / (bw * bh), score = target > 0 ? -Math.abs(mean - target) : s; // -Dtarget: boxes whose mean land height is nearest it
    if (!clash && score > best) { best = score; bx = x; by = y; } }
  for (int dy = -2; dy < bh + 2; dy++) for (int dx = -2; dx < bw + 2; dx++) { int y = by + dy; if (y >= 0 && y < H) used[Math.floorMod(bx + dx, W) + y * W] = true; }
  int[] o = t.blockOf(bx + by * W); int n = bw * Terrain.BLOCKS_PER_CELL, m = bh * Terrain.BLOCKS_PER_CELL;
  var buf = java.nio.ByteBuffer.allocate(n * m * 4).order(java.nio.ByteOrder.LITTLE_ENDIAN);
  float[] row = new float[n * m];
  java.util.stream.IntStream.range(0, m).parallel().forEach(z -> { for (int x = 0; x < n; x++) { var c = t.relaxedColumn(o[0] + x, o[1] + z); row[x + z * n] = t.toMetres(c.ground()); } });
  for (float f : row) buf.putFloat(f);
  java.nio.file.Files.write(java.nio.file.Path.of("out/dem/game_" + i + ".f32"), buf.array());
  meta.append("game_" + i + " " + n + " " + m + " " + kmPerBlock + " at " + o[0] + "," + o[1] + "\n");
  System.out.println("box " + i + " at block " + o[0] + "," + o[1] + (target > 0 ? " mean off target by " + (int) -best + " m" : " mean cell metres " + (int) (best / (bw * bh))));
}
java.nio.file.Files.writeString(java.nio.file.Path.of("out/dem/game.txt"), meta);
/exit
