import com.realtectonics.sim.*;
var d = PlanetData.load(java.nio.file.Path.of("run/world/realtectonics/planet.bin"));
var t = new Terrain(d, 777);
var v = d.volcanoes.stream().filter(x -> !x.hotspot() && x.active()).findFirst().get();
int[] b = t.blockOf((int) v.u() + (int) v.v() * d.w);
for (int dz = -40; dz <= 40; dz++) for (int dx = -40; dx <= 40; dx++) { var c = t.column(b[0] + dx, b[1] + dz); if (c.lava() != Terrain.Column.NO_WATER) { System.out.println((b[0] + dx) + " " + c.lava() + " " + (b[1] + dz) + " " + c.ground() + " " + c.biome()); dz = 99; break; } }
/exit
