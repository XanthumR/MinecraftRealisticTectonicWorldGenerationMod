import com.realtectonics.sim.*;
// The TerrainTest planet; grid of ground (w = water level shown) around -Dcx -Dcz.
Planet p = new Planet(256, 128, 8, 7);
for (int i = 0; i < 50; i++) p.step();
Climate climate = new Climate(p);
PlanetData data = PlanetData.from(p, climate, new Hydrology(p, climate.precipitation, 20, 7));
Terrain t = new Terrain(data, 7, 1, 1, PlanetSettings.DEFAULT.riverWidth());
int cx = Integer.getInteger("cx"), cz = Integer.getInteger("cz");
for (int z = cz - 5; z <= cz + 5; z++) { var sb = new StringBuilder(); for (int x = cx - 5; x <= cx + 5; x++) { var c = t.column(x, z); sb.append(String.format("%4d%-5s", c.ground(), c.water() != Terrain.Column.NO_WATER ? "w" + c.water() : "")); } System.out.println(sb); }
System.out.println(t.column(cx, cz)); System.out.println(t.column(cx - 1, cz));
/exit
