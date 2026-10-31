import com.realtectonics.sim.*;
// One example of each tectonic landform on a planet, as "name blockX blockZ cellX cellY" lines (for uview3d.jsh at the
// blocks, tools/seafloor.py at the cells). -Dplanet (default out/p777_75.bin), -Dseed (default 777: picks the spawn).
var d = PlanetData.load(java.nio.file.Path.of(System.getProperty("planet", "out/p777_75.bin")));
long seed = Long.getLong("seed", 777);
var t = new Terrain(d, seed, 1, 1, 2);
var ox = Terrain.class.getDeclaredField("originX"); ox.setAccessible(true); var oy = Terrain.class.getDeclaredField("originY"); oy.setAccessible(true);
int originX = (int) ox.get(t), originY = (int) oy.get(t), w = d.w, h = d.h;
void show(String name, int g) { System.out.println(name + " " + (Math.floorMod(g % w - originX + w / 2, w) - w / 2) * 32 + " " + (g / w - originY) * 32 + " " + g % w + " " + g / w); }
int at(int x, int y) { return Math.floorMod(x, w) + Math.clamp(y, 0, h - 1) * w; }
// Share of cells within r that pass a test, and their mean of a field
double share(int g, int r, java.util.function.IntPredicate p) { int n = 0, k = 0; for (int dy = -r; dy <= r; dy++) for (int dx = -r; dx <= r; dx++) { n++; if (p.test(at(g % w + dx, g / w + dy))) k++; } return k / (double) n; }
int best = -1; double score;
// Fold mountains: the low flank of a wide collision belt under a high range, where Terrain.foothills lays its fold ridges
score = -1; for (int g = 0; g < w * h; g++) if (d.rock[g] == Rock.OROGEN.ordinal() && d.metres[g] > 300 && d.metres[g] < 1200 && d.ice[g] == 0) {
    float top = 0; for (int dy = -6; dy <= 6; dy++) for (int dx = -6; dx <= 6; dx++) top = Math.max(top, d.metres[at(g % w + dx, g / w + dy)]);
    double s = share(g, 4, c -> d.rock[c] == Rock.OROGEN.ordinal()) * Math.min(top - d.metres[g], 3000); if (s > score) { score = s; best = g; } }
show("fold", best);
// Volcanic mountain: the cone standing highest over the land it was built on, not collapsed
Volcano big = null; for (Volcano v : d.volcanoes) if (!v.gone() && !v.hotspot() && !Float.isNaN(v.baseMetres()) && d.metres[at((int) v.u(), (int) v.v())] > 0
    && (big == null || d.metres[at((int) v.u(), (int) v.v())] - v.baseMetres() > d.metres[at((int) big.u(), (int) big.v())] - big.baseMetres())) big = v;
show("volcano", at((int) big.u(), (int) big.v()));
// Plateau: high land, as even as possible all round
score = Double.MAX_VALUE; for (int g = 0; g < w * h; g++) { if (d.metres[g] < 2500 || d.ice[g] != 0) continue; double mean = 0, var = 0; int n = 0;
    for (int dy = -4; dy <= 4; dy++) for (int dx = -4; dx <= 4; dx++) { mean += d.metres[at(g % w + dx, g / w + dy)]; n++; } mean /= n;
    for (int dy = -4; dy <= 4; dy++) for (int dx = -4; dx <= 4; dx++) { double e = d.metres[at(g % w + dx, g / w + dy)] - mean; var += e * e; }
    double s = Math.sqrt(var / n) / mean; if (mean > 2500 && s < score) { score = s; best = g; } }
show("plateau", best);
// Ocean trench: the deepest sea floor
best = 0; for (int g = 0; g < w * h; g++) if (d.metres[g] < d.metres[best]) best = g;
show("trench", best);
// Island arc: an arc volcano (not a hotspot) standing in open sea
score = -1; for (Volcano v : d.volcanoes) { if (v.hotspot() || v.gone()) continue; int g = at((int) v.u(), (int) v.v()); double s = share(g, 6, c -> d.metres[c] < 0); if (d.metres[g] > 0 && s > score) { score = s; best = g; } }
show("arc", best);
// Mid-ocean ridge: the shallowest sea floor far from any land
int[] far = new int[w * h]; java.util.Arrays.fill(far, Integer.MAX_VALUE); var q = new java.util.ArrayDeque<Integer>();
for (int g = 0; g < w * h; g++) if (d.metres[g] >= 0) { far[g] = 0; q.add(g); }
while (!q.isEmpty()) { int c = q.poll(); for (int nb : new int[]{at(c % w - 1, c / w), at(c % w + 1, c / w), c / w > 0 ? c - w : c, c / w < h - 1 ? c + w : c}) if (far[nb] == Integer.MAX_VALUE) { far[nb] = far[c] + 1; q.add(nb); } }
best = -1; for (int g = 0; g < w * h; g++) if (far[g] > 40 && g / w > h / 6 && g / w < 5 * h / 6 && (best < 0 || d.metres[g] > d.metres[best])) best = g;
show("ridge", best);
// Continental shelf: the coast with the most shallow sea (0 to -200 m) off it
score = -1; for (int g = 0; g < w * h; g++) { if (far[g] != 1 || g / w < h / 5 || g / w > 4 * h / 5) continue; double s = share(g, 8, c -> d.metres[c] < 0 && d.metres[c] > -200); if (s > score) { score = s; best = g; } }
show("shelf", best);
// Transform fault: the fastest strike-slip fault on land
best = -1; for (int g = 0; g < w * h; g++) if (d.metres[g] > 50 && (best < 0 || d.faultSlip[g] > d.faultSlip[best])) best = g;
show("transform", best);
/exit
