import com.realtectonics.sim.*;
// Oases (format 95): finds the spring-fed pools in desert over the whole planet, checks none spills and each is brim-full (its lowest bank level with the water).
// -Dplanet (default out/p777.bin) -Dseed (777) -Dfmt (the file's own). Looks round every cell of Terrain.oasisField.
var d = PlanetData.load(java.nio.file.Path.of(System.getProperty("planet", "out/p777.bin"))); d.format = Integer.getInteger("fmt", d.format);
var t = new Terrain(d, Long.getLong("seed", 777), 1, 1, 2);
java.util.function.Function<String, Object> get = n -> { try { var f = Terrain.class.getDeclaredField(n); f.setAccessible(true); return f.get(t); } catch (Exception e) { throw new RuntimeException(e); } };
float[] field = (float[]) get.apply("oasisField"); int ox = (int) get.apply("originX"), oy = (int) get.apply("originY");
var cells = java.util.stream.IntStream.range(0, d.w * d.h).filter(c -> field[c] > 0).toArray();
var found = new java.util.concurrent.ConcurrentHashMap<Long, int[]>(); // one pool per 128-block square
java.util.Arrays.stream(cells).parallel().forEach(c -> {
  int bx = (c % d.w - ox) * 32, bz = (c / d.w - oy) * 32; // the cell's middle, near enough: the warp moves it a few dozen blocks
  for (int z = bz - 160; z < bz + 160; z += 4) for (int x = bx - 160; x < bx + 160; x += 4) {
    var col = t.column(x, z);
    if (col.cover() != Terrain.Cover.POND) continue;
    found.putIfAbsent(Math.floorDiv(x, 128) * 1000000L + Math.floorDiv(z, 128), new int[]{x, z, col.water()});
  }
});
int spills = 0, short_ = 0; // spilling edges; oases whose lowest bank stands above their water (not brim-full)
for (int[] o : found.values()) { // water never stands above dry ground beside it, and the lowest bank is level with it
  int lowest = Integer.MAX_VALUE;
  for (int dz = -50; dz <= 50; dz++) for (int dx = -50; dx <= 50; dx++) {
    var c = t.column(o[0] + dx, o[1] + dz); if (c.water() == Terrain.Column.NO_WATER) continue;
    for (int[] k : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) { var e = t.column(o[0] + dx + k[0], o[1] + dz + k[1]);
      if (e.water() != Terrain.Column.NO_WATER) continue;
      if (e.ground() < c.water()) spills++;
      lowest = Math.min(lowest, e.ground() - c.water()); }
  }
  if (lowest > 0) { short_++; System.out.println("  not brim-full by " + lowest + ": x " + o[0] + " z " + o[1]); }
}
System.out.println("spring cells " + cells.length + ", oases " + found.size() + ", spilling edges " + spills + ", not brim-full " + short_);
found.values().stream().sorted((a, b) -> a[0] - b[0]).limit(15).forEach(o -> System.out.println("  x " + o[0] + " z " + o[1] + " water " + o[2] + " ring " + t.column(o[0] + 12, o[1]).biome() + " beyond " + t.column(o[0] + 60, o[1]).biome()));
/exit
