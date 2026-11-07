import com.realtectonics.sim.*;
// Cenotes (format 96): over an area, the water columns a format adds over format 95 (its cenotes), grouped into pools,
// and every water column's spills onto lower dry ground beside it, for both. -Dplanet -Dseed -Dcx -Dcz -Dn (blocks, 1024).
var path = java.nio.file.Path.of(System.getProperty("planet", "out/p777.bin")); long seed = Long.getLong("seed", 777);
var dOld = PlanetData.load(path); dOld.format = 95; var old = new Terrain(dOld, seed, 1, 1, 2);
var dNew = PlanetData.load(path); dNew.format = Integer.getInteger("fmt", 96); var t = new Terrain(dNew, seed, 1, 1, 2);
int cx = Integer.getInteger("cx", 0), cz = Integer.getInteger("cz", 0), n = Integer.getInteger("n", 1024), x0 = cx - n / 2, z0 = cz - n / 2;
var a = new Terrain.Column[n * n]; var b = new Terrain.Column[n * n];
java.util.stream.IntStream.range(0, n).parallel().forEach(j -> { for (int i = 0; i < n; i++) { a[i + j * n] = old.column(x0 + i, z0 + j); b[i + j * n] = t.column(x0 + i, z0 + j); } });
java.util.function.Function<Terrain.Column[], Integer> spills = cs -> { int s = 0;
  for (int j = 1; j < n - 1; j++) for (int i = 1; i < n - 1; i++) { var c = cs[i + j * n]; if (c.water() == Terrain.Column.NO_WATER) continue;
    for (int k : new int[]{-1, 1, -n, n}) { var e = cs[i + j * n + k]; if (e.water() == Terrain.Column.NO_WATER && e.ground() < c.water()) s++; } }
  return s; };
// the new cenotes: water where format 95 had none (or other water), flood-filled into pools
boolean[] seen = new boolean[n * n]; var pools = new java.util.ArrayList<int[]>();
for (int p = 0; p < n * n; p++) {
  if (seen[p] || b[p].water() == Terrain.Column.NO_WATER || b[p].water() == a[p].water()) continue;
  var stack = new java.util.ArrayDeque<Integer>(); stack.push(p); seen[p] = true; int size = 0, lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE, rim = Integer.MAX_VALUE, rubble = 0;
  while (!stack.isEmpty()) { int q = stack.pop(); size++; lo = Math.min(lo, b[q].water()); hi = Math.max(hi, b[q].water());
    for (int k : new int[]{-1, 1, -n, n}) { int r = q + k; if (r < 0 || r >= n * n) continue;
      if (b[r].water() == Terrain.Column.NO_WATER) { rim = Math.min(rim, b[r].ground()); if (b[r].cover() == Terrain.Cover.SCREE) rubble++; continue; }
      if (!seen[r] && b[r].water() != a[r].water()) { seen[r] = true; stack.push(r); } } }
  pools.add(new int[]{x0 + p % n, z0 + p / n, size, lo, hi, rim - lo, rubble});
}
System.out.println("format 95 spilling edges " + spills.apply(a) + ", format " + dNew.format + " " + spills.apply(b) + "; new cenote pools " + pools.size()
    + ", with two water levels " + pools.stream().filter(q -> q[3] != q[4]).count());
for (int j = 1; j < n - 1; j++) for (int i = 1; i < n - 1; i++) { var c = b[i + j * n]; var c0 = a[i + j * n]; if (c.water() == Terrain.Column.NO_WATER) continue;
  for (int k : new int[]{-1, 1, -n, n}) { var e = b[i + j * n + k]; var e0 = a[i + j * n + k];
    if (e.water() == Terrain.Column.NO_WATER && e.ground() < c.water() && !(c0.water() != Terrain.Column.NO_WATER && e0.water() == Terrain.Column.NO_WATER && e0.ground() < c0.water()))
      System.out.println("  new spill at " + (x0 + i) + " " + (z0 + j) + ": water " + c.water() + " (was " + c0.water() + ") beside dry " + e.ground() + " (was " + e0.ground() + ", water " + e0.water() + ") " + e.cover()); } }
System.out.println("     x      z  columns  water  rim above water  rubble beside");
pools.stream().sorted((p, q) -> q[2] - p[2]).limit(20).forEach(q -> System.out.println(String.format("%6d %6d %8d %6d %16d %8d", q[0], q[1], q[2], q[3], q[5], q[6])));
/exit
