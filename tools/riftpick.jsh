import com.realtectonics.sim.*;
var p = new Planet(1024, 512, 14, 0.7f, 1, 777); PlanetData.tectonics(p, 300, x -> {});
float[] r = p.rifts(); var c = new Climate(p); var d = PlanetData.from(p, c, new Hydrology(p, c.precipitation, c.anomaly, 50, 777, x -> {})); var t = new Terrain(d, 777, 1, 1, 2);
int best = -1; int most = 0;
for (int g = 0; g < r.length; g++) { if (r[g] == 0) continue; int k = 0; for (int dy = -3; dy <= 3; dy++) for (int dx = -3; dx <= 3; dx++) if (!Float.isNaN(d.lakeMetres[Math.floorMod(g % p.w + dx, p.w) + Math.clamp(g / p.w + dy, 0, p.h - 1) * p.w])) k++; if (k > most) { most = k; best = g; } }
System.out.println("rift with lakes at " + java.util.Arrays.toString(t.blockOf(best)) + " floor " + d.metres[best] + " m, lake cells around " + most);
/exit
