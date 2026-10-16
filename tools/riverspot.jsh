import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
var rnd = new java.util.Random(5); boolean r = false, m = false;
while (!(r && m)) { int x = (rnd.nextInt(t.circumference) - t.circumference / 2) & ~3, z = (rnd.nextInt(8000) - 4000) & ~3; var c = t.column(x, z);
  if (!r && c.biome() == Biome.DARK_FOREST) { System.out.println("DARK " + x + " " + z + " water " + c.water()); r = true; }
  if (!m && c.biome() == Biome.BADLANDS) { System.out.println("BADLANDS " + x + " " + z + " ground " + c.ground()); m = true; } }
/exit
