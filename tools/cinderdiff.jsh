import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var now = new Terrain(d, 777, 1, 1, 2); d.format = 20; var old = new Terrain(d, 777, 1, 1, 2);
int cx = Integer.getInteger("cx"), cz = Integer.getInteger("cz");
for (int dz = -24; dz <= 24; dz += 4) { var sb = new StringBuilder(); for (int dx = -24; dx <= 24; dx += 4) sb.append(String.format("%4d", now.column(cx + dx, cz + dz).ground() - old.column(cx + dx, cz + dz).ground())); System.out.println(sb); }
/exit
