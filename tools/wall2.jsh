import com.realtectonics.sim.*;
Planet p = new Planet(256, 128, 8, 7); for (int i = 0; i < 50; i++) p.step();
Climate climate = new Climate(p); var data = PlanetData.from(p, climate, new Hydrology(p, climate.precipitation, 20, 7));
var t = new Terrain(data, 7, 1, 1, 2);
int x0 = Integer.getInteger("x0"), z0 = Integer.getInteger("z0");
for (int dz = -2; dz <= 2; dz++) { var sb = new StringBuilder(); for (int dx = -2; dx <= 2; dx++) { var c = t.column(x0+dx, z0+dz); sb.append(String.format("%4d/%-4s%s ", c.ground(), c.lava() == Terrain.Column.NO_WATER ? (c.water() == Terrain.Column.NO_WATER ? "-" : "w" + c.water()) : "L" + c.lava(), c.cover().name().substring(0, 2))); } System.out.println(sb); }
/exit
