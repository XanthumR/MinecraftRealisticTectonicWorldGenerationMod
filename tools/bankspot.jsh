import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
for (int x = 6400; x < 7000; x++) { var c = t.column(x, -608); if (c.cover() == Terrain.Cover.BANK) { System.out.println("BANK " + x + " -608 " + c); break; } }
/exit
