import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777);
System.out.println("corner biomeAt " + t.biomeAt(-12016, 5988));
int n = 0;
for (int z = 5900; z < 6200 && n < 1; z += 4) for (int x = -12100; x < -11900; x += 4) { boolean all = true; for (int k = -8; k <= 8; k += 4) for (int j = -8; j <= 8; j += 4) all &= t.biomeAt(x + k, z + j) == Biome.MANGROVE; if (all) { System.out.println("INSIDE " + x + " " + z + " " + t.column(x, z)); n++; break; } }
/exit
