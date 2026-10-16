import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777);
int foundF = 0, foundG = 0;
for (int g = 0; g < d.w*d.h && (foundF < 1 || foundG < 1); g++) {
  int[] b = t.blockOf(g);
  if (foundF < 1 && d.ice[g] == 2) for (int dz = -32; dz <= 32 && foundF < 1; dz += 4) for (int dx = -32; dx <= 32; dx += 4) { var c = t.column(b[0]+dx, b[1]+dz); if (c.water()==62 && c.ground() < 40) { System.out.println("FJORD " + (b[0]+dx) + " " + (b[1]+dz) + " " + c); foundF++; break; } }
  if (foundG < 1 && d.ice[g] == 1 && Math.abs(b[0]) < 12000) for (int dz = -32; dz <= 32 && foundG < 1; dz += 4) for (int dx = -32; dx <= 32; dx += 4) { var c = t.column(b[0]+dx, b[1]+dz); if (c.ice() != Terrain.Column.NO_WATER && c.ice() - c.ground() > 8) { System.out.println("GLACIER " + (b[0]+dx) + " " + (b[1]+dz) + " " + c); foundG++; break; } }
}
/exit
