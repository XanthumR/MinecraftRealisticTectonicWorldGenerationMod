import com.realtectonics.sim.*;
// Format 74 wet gorges: river cells whose level a wet upland gorge lowered, how deep, and the deepest spots in blocks
// (for uview3d.jsh). -Dseed (the world seed: it picks the spawn, so the coordinates), -Dplanet=file (default out/p777_cur.bin, made from seed 777 if missing).
var file = java.nio.file.Path.of(System.getProperty("planet", "out/p777_cur.bin"));
PlanetData d;
if (java.nio.file.Files.exists(file)) d = PlanetData.load(file); else { d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); d.save(file); }
float[] level(PlanetData p, int fmt) throws Exception {
    p.format = fmt;
    var t = new Terrain(p, Long.getLong("seed", 777), 1, 1, 2);
    var f = Terrain.class.getDeclaredField("riverLevel"); f.setAccessible(true);
    return (float[]) f.get(t);
}
float[] before = level(d, 73), after = level(d, 74);
var t = new Terrain(d, Long.getLong("seed", 777), 1, 1, 2);
var fg = Terrain.class.getDeclaredField("groundY"); fg.setAccessible(true); float[] gy = (float[]) fg.get(t);
var ox = Terrain.class.getDeclaredField("originX"); ox.setAccessible(true); var oy = Terrain.class.getDeclaredField("originY"); oy.setAccessible(true);
int originX = (int) ox.get(t), originY = (int) oy.get(t);
var cut = new java.util.ArrayList<int[]>();
int dryCut = 0;
for (int g = 0; g < d.w * d.h; g++) {
    if (gy[g] - before[g] > 3) dryCut++;
    float depth = before[g] - after[g];
    if (depth > 3) cut.add(new int[]{g, Math.round(gy[g] - after[g])});
}
cut.sort((a, b) -> b[1] - a[1]);
System.out.printf("dry canyon cells (format 73) %d, cells a wet gorge cut over 3 blocks %d%n", dryCut, cut.size());
for (int i = 0; i < Math.min(12, cut.size()); i++) {
    int g = cut.get(i)[0], cx = g % d.w, cy = g / d.w, x = Math.floorMod(cx - originX + d.w / 2, d.w) - d.w / 2;
    System.out.printf("  depth %d blocks at x %d z %d (rock %s, rain %.0f mm)%n", cut.get(i)[1], x * 32, (cy - originY) * 32, Rock.of(d.rock[g]), d.precipitation[g]);
}
/exit
