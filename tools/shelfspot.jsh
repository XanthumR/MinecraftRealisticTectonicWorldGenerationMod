import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777);
for (int x = 14624; x < 14624 + 600; x += 8) { var c = t.column(x, 1952 - 300); if (c.water() == 62 && c.ground() < 58 && c.ground() > 45) { System.out.println("SHELF " + x + " " + (1952 - 300) + " " + c); break; } }
/exit
