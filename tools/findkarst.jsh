import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
int n = 0, at = -1; for (int g = 0; g < d.w*d.h; g++) if (d.metres[g] > 50 && Rock.of(d.rock[g]) == Rock.BASIN && d.precipitation[g] > 1300 && d.temperature[g] > 12 && d.ice[g] == 0 && Float.isNaN(d.lakeMetres[g]) && d.flow[g] < 50) { n++; if (at < 0) at = g; }
System.out.println("wet karst cells " + n + " at " + java.util.Arrays.toString(t.blockOf(at)));
/exit
