import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {});
int w = d.w, shown = 0;
for (Volcano v : d.volcanoes) { if (v.gone() || !v.caldera() || shown++ >= 3) continue;
  int r = (int) Math.ceil(v.radiusBlocks() * 0.3 / 32) + 2; System.out.println("caldera r cells " + v.radiusBlocks() * 0.3 / 32 + " base " + v.baseMetres());
  for (int dx = -r; dx <= r; dx++) { int g = Math.floorMod((int) v.u() + dx, w) + (int) v.v() * w;
    System.out.printf("  dx %+d metres %.0f ideal %.0f lake %s flow %.0f%n", dx, d.metres[g], v.surfaceMetres(Math.abs(dx) * 32, true), d.lakeMetres[g], d.flow[g]); } }
/exit
