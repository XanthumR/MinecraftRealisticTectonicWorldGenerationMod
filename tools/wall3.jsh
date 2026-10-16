import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
int x0 = Integer.getInteger("x0"), z0 = Integer.getInteger("z0");
for (int dz = -2; dz <= 2; dz++) { var sb = new StringBuilder(); for (int dx = -2; dx <= 2; dx++) { var c = t.column(x0+dx, z0+dz); sb.append(String.format("%4d/%-5s%s ", c.ground(), c.lava() == Terrain.Column.NO_WATER ? (c.water() == Terrain.Column.NO_WATER ? "-" : "w" + c.water()) : "L" + c.lava(), c.cover().name().substring(0, 2))); } System.out.println(sb); }
/exit
