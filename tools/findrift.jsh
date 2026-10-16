import com.realtectonics.sim.*;
var p = new Planet(1024, 512, 14, 0.7f, 1, 777); PlanetData.tectonics(p, 300, x -> {});
float[] r = p.rifts(); int n = 0, at = -1; for (int g = 0; g < r.length; g++) if (r[g] > 0) { n++; if (at < 0 || (g % 7 == 0)) at = g; }
var c = new Climate(p); var d = PlanetData.from(p, c, new Hydrology(p, c.precipitation, c.anomaly, 50, 777, x -> {})); var t = new Terrain(d, 777, 1, 1, 2);
int lakes = 0; for (int g = 0; g < r.length; g++) if (r[g] > 0) for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) { int q = Math.floorMod(g % p.w + dx, p.w) + Math.clamp(g / p.w + dy, 0, p.h - 1) * p.w; if (!Float.isNaN(d.lakeMetres[q])) lakes++; }
System.out.println("rift cells " + n + ", lake cells near rifts " + lakes + ", a rift at " + java.util.Arrays.toString(t.blockOf(at)) + " " + d.metres[at] + " m");
/exit
