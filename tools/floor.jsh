import com.realtectonics.sim.*;
var t = new Terrain(PlanetData.load(java.nio.file.Path.of("run/world/realtectonics/planet.bin")), 777);
var out = new StringBuilder();
for (int x = 928; x < 992; x++) for (int z = 928; z < 992; z++) { var c = t.column(x, z); if (c.water() == Terrain.Column.NO_WATER) continue; for (int y = c.ground() - 3; y <= c.water(); y++) out.append(x).append(" ").append(y).append(' ').append(z).append('\n'); }
java.nio.file.Files.writeString(java.nio.file.Path.of("build/floor.txt"), out);
/exit
