import com.realtectonics.sim.*;
var d = PlanetData.load(java.nio.file.Path.of("run/world/realtectonics/planet.bin"));
var t = new Terrain(d, 777);
for (var want : new Rock[]{Rock.CRATON, Rock.BASIN}) {
  outer: for (int r = 0; r < 3000; r += 16) for (int x = -r; x <= r; x += 16) { var c = t.column(x, r); if (c.rock() == want && !c.volcanic() && c.water() == Terrain.Column.NO_WATER && c.ground() > 80 && c.biome() != Biome.DESERT && c.biome() != Biome.SAVANNA) { System.out.println(want + " " + x + " " + r + " " + c.ground()); break outer; } }
}
/exit
