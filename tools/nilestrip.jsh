import com.realtectonics.sim.*;
// Biomes across the Nile at 25N on out/earth.bin, zoom 1: one letter per 4 blocks (D desert, S savanna, G grass, R river, F forest, other ?).
var d = PlanetData.load(java.nio.file.Path.of("out/earth.bin")); var t = new Terrain(d, 1, 1, 1, 2);
double[] o = t.mapPosition(0, 0);
int z = (int) Math.round(((1 - Math.sin(Math.toRadians(25))) * d.h / 2 - o[1]) * t.blocksPerCell);
int cx = (int) Math.round(((180 + 32.7) / 360 * d.w - o[0]) * t.blocksPerCell);
for (int dz : new int[]{-64, 0, 64}) {
    var sb = new StringBuilder();
    for (int x = cx - 600; x < cx + 600; x += 4) { var b = t.column(x, z + dz).biome(); sb.append(switch (b) { case DESERT -> 'D'; case SAVANNA -> 'S'; case GRASSLAND -> 'G'; case RIVER -> 'R'; case TEMPERATE_FOREST -> 'F'; case BADLANDS -> 'B'; default -> '?'; }); }
    System.out.println(sb);
}
/exit
