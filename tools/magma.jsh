import com.realtectonics.sim.*;
// Format 58: magma patches on active volcanoes. For each active volcano of seed 777, the share of its volcanic ground
// within 48 blocks of the summit that is magma, and the same 48-96 blocks out; dormant ones must stay at 0.
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {});
var t = new Terrain(d, 777, 1, 1, 2);
var ox = Terrain.class.getDeclaredField("originX"); ox.setAccessible(true); var oy = Terrain.class.getDeclaredField("originY"); oy.setAccessible(true);
int originX = ox.getInt(t), originY = oy.getInt(t), shown = 0;
for (var v : d.volcanoes) {
    if (v.gone() || shown >= 8) continue;
    int cx = (int) Math.round(((v.u() - originX + d.w / 2.0) % d.w - d.w / 2.0) * 32), cz = (int) Math.round((v.v() - originY) * 32);
    int[] vol = new int[2], hot = new int[2];
    for (int dz = -96; dz <= 96; dz += 2) for (int dx = -96; dx <= 96; dx += 2) {
        double r = Math.hypot(dx, dz); if (r > 96) continue; int ring = r < 48 ? 0 : 1;
        var c = t.column(cx + dx, cz + dz); if (!c.volcanic()) continue;
        vol[ring]++; if (t.magmaPatch(cx + dx, cz + dz)) hot[ring]++; }
    if (vol[0] == 0) continue;
    shown++;
    System.out.printf("%s at %d %d: magma %.1f%% of the ground within 48 blocks, %.1f%% at 48-96%n", v.active() ? "active " : "dormant", cx, cz,
            100.0 * hot[0] / vol[0], vol[1] == 0 ? 0 : 100.0 * hot[1] / vol[1]);
}
/exit
