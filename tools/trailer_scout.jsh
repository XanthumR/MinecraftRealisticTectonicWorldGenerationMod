import com.realtectonics.sim.*;
// Trailer shot scouting on out/trailer/planet.bin (seed -Dseed, default 4, PlanetSettings.DEFAULT): one candidate spot per
// feature, as block coordinates with the ground height there, for /tp in a world made with that seed and default settings.
long seed = Long.getLong("seed", 4);
var d = PlanetData.load(java.nio.file.Path.of("out/trailer/planet.bin"));
var t = new Terrain(d, seed, 1, 1, 2);
var ox = Terrain.class.getDeclaredField("originX"); ox.setAccessible(true); var oy = Terrain.class.getDeclaredField("originY"); oy.setAccessible(true);
int originX = ox.getInt(t), originY = oy.getInt(t), w = d.w, h = d.h;
int bx(double u) { return (int) Math.round(((u - originX + w * 1.5) % w - w / 2.0) * 32); }
int bz(double v) { return (int) Math.round((v - originY) * 32); }
void show(String what, int x, int z) { var c = t.column(x, z);
    System.out.printf("%-28s x %6d z %6d  ground %4d water %s ice %s  %s%n", what, x, z, c.ground(), c.water() == Terrain.Column.NO_WATER ? "-" : "" + c.water(), c.ice() == Terrain.Column.NO_WATER ? "-" : "" + c.ice(), c.biome()); }
void cell(String what, int g) { show(what, bx(g % w + 0.5), bz(g / w + 0.5)); }
// volcanoes: the biggest active one on land, and a collapsed caldera with its lake
Volcano big = null, cal = null;
for (var v : d.volcanoes) { if (v.gone()) continue; int g = (int) v.u() + (int) v.v() * w; if (d.metres[g] < 0) continue;
    if (v.active() && (big == null || v.size() > big.size())) big = v;
    if (v.caldera() && !Float.isNaN(d.lakeMetres[g]) && (cal == null || v.size() > cal.size())) cal = v; }
if (big != null) show("active volcano", bx(big.u()), bz(big.v()));
if (cal != null) show("caldera lake", bx(cal.u()), bz(cal.v()));
int mouth = -1, river = -1, glacier = -1, fjord = -1, fold = -1, craton = -1; float fjordRelief = 0, foldRelief = 0;
for (int g = w; g < w * (h - 1); g++) {
    if (d.metres[g] < 0) continue; int r = d.receiver[g];
    if (r >= 0 && d.metres[r] < 0 && (mouth < 0 || d.flow[g] > d.flow[mouth])) mouth = g;
    if (d.metres[g] > 300 && (river < 0 || d.flow[g] > d.flow[river])) river = g;
    if (d.ice[g] == Hydrology.GLACIATED && r >= 0 && d.ice[r] == 0 && d.metres[r] >= 0 && (glacier < 0 || d.flow[g] > d.flow[glacier])) glacier = g; // a snout: ice flowing onto bare land
    float relief = 0; for (int k : new int[]{1, -1, w, -w}) relief = Math.max(relief, Math.abs(d.metres[g + k] - d.metres[g]));
    if (d.ice[g] == Hydrology.FJORD && (fjord < 0 || relief > fjordRelief)) { fjord = g; fjordRelief = relief; } // the steepest walls
    if (d.rock[g] == Rock.OROGEN.ordinal() && d.ice[g] == 0 && d.metres[g] > 1500 && d.metres[g] < 3500 && (fold < 0 || relief > foldRelief)) { fold = g; foldRelief = relief; } // rugged, below the ice
    if (d.rock[g] == Rock.CRATON.ordinal() && d.ice[g] == 0 && d.metres[g] > 900 && d.metres[g] < 1500 && (craton < 0 || d.flow[g] < d.flow[craton])) craton = g; }
cell("river mouth (largest)", mouth); cell("big river inland", river); cell("glacier snout", glacier); cell("fjord", fjord);
cell("rugged fold mountains", fold); cell("high craton (deep dark)", craton);
// biome meetings, per cell centre: a desert beside a rainforest, and badlands
var bio = new Biome[w * h];
java.util.stream.IntStream.range(1, h - 1).parallel().forEach(y -> { for (int x = 0; x < w; x++) bio[x + y * w] = t.column(bx(x + 0.5), bz(y + 0.5)).biome(); });
int edge = -1, bad = -1, cherry = -1;
for (int g = 2 * w; g < w * (h - 2); g++) {
    if (bio[g] == Biome.DESERT) for (int k : new int[]{2, -2, 2 * w, -2 * w}) if (bio[g + k] == Biome.RAINFOREST || bio[g + k] == Biome.TROPICAL_SEASONAL_FOREST) edge = g;
    if (bio[g] == Biome.BADLANDS || bio[g] == Biome.ERODED_BADLANDS) bad = g;
    if (bio[g] == Biome.CHERRY_GROVE) cherry = g; }
if (edge >= 0) cell("desert beside rainforest", edge); if (bad >= 0) cell("badlands", bad); if (cherry >= 0) cell("cherry grove", cherry);
/exit
