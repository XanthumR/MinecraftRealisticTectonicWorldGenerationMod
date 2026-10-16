import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777);
boolean m = false, ch = false;
for (int z = 6112 - 300; z < 6112 + 300 && !(m && ch); z += 3) for (int x = -12064 - 300; x < -12064 + 300; x += 3) { var c = t.column(x, z);
  if (!m && c.biome() == Biome.MANGROVE && c.water() == Terrain.Column.NO_WATER && c.ground() == 63) { System.out.println("MARSH " + x + " " + z + " " + c + " biomeAt " + t.biomeAt(x, z)); m = true; }
  if (!ch && m && c.water() == 62 && c.ground() < 60 && t.column(x + 9, z).biome() == Biome.MANGROVE) { System.out.println("CHANNEL " + x + " " + z + " " + c); ch = true; } }
/exit
