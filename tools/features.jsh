import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {});
int fj = 0, glac = 0, lakes = 0, mouths = 0, deltas = 0, rivers = 0;
for (int c = 0; c < d.w*d.h; c++) { if (d.ice[c] == 2) fj++; if (d.ice[c] == 1) glac++; if (!Float.isNaN(d.lakeMetres[c])) lakes++; if (d.metres[c] >= 0 && d.flow[c] > Hydrology.RIVER_AREA) rivers++;
  int r = d.receiver[c]; if (r >= 0 && d.metres[c] >= 0 && d.metres[r] < 0 && d.flow[c] > 200) { mouths++; if (d.flow[c] > 500 && d.metres[r] >= -150 && d.ice[c] != 2) deltas++; } }
System.out.printf("fjord cells %d, glaciated %d, lake cells %d, river cells %d, river mouths %d, delta mouths %d, volcanoes %d%n", fj, glac, lakes, rivers, mouths, deltas, d.volcanoes.size());
/exit
