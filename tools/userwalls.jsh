import com.realtectonics.sim.*;
var d = PlanetData.load(java.nio.file.Path.of("run/saves/New World (2)/realtectonics/planet.bin"));
d.format = Integer.getInteger("fmt", d.format); System.out.println("format " + d.format);
var t = new Terrain(d, -5073067326693234624L, 1.1f, 4, 2.51f);
int px = -1239, pz = 136904, R = 400; java.util.List<String> at = new java.util.ArrayList<>();
for (int z = pz - R; z <= pz + R; z += 2) for (int x = px - R; x <= px + R; x += 2) { var c = t.column(x, z); if (c.water() != Terrain.Column.NO_WATER) continue;
  var n = t.column(x + 1, z); var m = t.column(x, z + 1);
  for (var o : new Terrain.Column[]{n, m}) if (o.water() == Terrain.Column.NO_WATER && Math.abs(o.ground() - c.ground()) >= 12) { if (at.size() < 4000) at.add(x + " " + z + " " + c.ground() + "/" + o.ground() + " " + c.biome() + " " + c.cover() + "/" + o.cover()); } }
System.out.println("wall edges " + at.size()); at.stream().limit(4).forEach(System.out::println);
/exit
