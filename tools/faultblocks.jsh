import com.realtectonics.sim.*;
// Format 75 fault blocks: share of land back-arc extension stretched, and the most stretched spots in blocks (for
// uview3d.jsh / hillshade.jsh with -Dfmt=74 and 75). -Dseed (the world seed: it picks the spawn), -Dplanet=file (default out/p777_75.bin, made from seed 777 if missing).
var file = java.nio.file.Path.of(System.getProperty("planet", "out/p777_75.bin"));
PlanetData d;
if (java.nio.file.Files.exists(file)) d = PlanetData.load(file); else { d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); d.save(file); }
var t = new Terrain(d, Long.getLong("seed", 777), 1, 1, 2);
var ox = Terrain.class.getDeclaredField("originX"); ox.setAccessible(true); var oy = Terrain.class.getDeclaredField("originY"); oy.setAccessible(true);
int originX = (int) ox.get(t), originY = (int) oy.get(t);
int land = 0, some = 0, strong = 0;
var spots = new java.util.ArrayList<Integer>();
for (int g = 0; g < d.w * d.h; g++) {
    if (d.metres[g] < 0) continue;
    land++;
    if (d.backArc[g] > 0.05f) some++;
    if (d.backArc[g] > 0.5f) { strong++; spots.add(g); }
}
spots.sort((a, b) -> Float.compare(d.backArc[b], d.backArc[a]));
System.out.printf("land stretched %.1f%% (over half %.1f%%)%n", 100.0 * some / land, 100.0 * strong / land);
var shown = new java.util.ArrayList<Integer>();
for (int g : spots) {
    boolean near = false;
    for (int s : shown) near |= Math.abs(s % d.w - g % d.w) < 12 && Math.abs(s / d.w - g / d.w) < 12;
    if (near) continue;
    shown.add(g);
    int x = Math.floorMod(g % d.w - originX + d.w / 2, d.w) - d.w / 2;
    System.out.printf("  stretch %.2f at x %d z %d, %.0f m, %.0f mm, %s%n", d.backArc[g], x * 32, (g / d.w - originY) * 32, d.metres[g], d.precipitation[g], Rock.of(d.rock[g]));
    if (shown.size() == 8) break;
}
/exit
