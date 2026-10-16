import com.realtectonics.sim.*;
java.lang.reflect.Method m = Hydrology.class.getDeclaredMethod("erodeCrust", Planet.class, int.class, float.class); m.setAccessible(true);
for (float[] cfg : new float[][]{{10, 1, 1}, {5, 1, 1.5f}, {5, 2, 3}}) {
  var p = new Planet(1024, 512, 14, 0.7f, 1, 777);
  for (int i = 0; i < 300; i++) { p.step(); if (cfg[0] > 0 && (i + 1) % (int) cfg[0] == 0) m.invoke(null, p, (int) cfg[1], 0.01f * cfg[2]); }
  var c = new Climate(p); var d = PlanetData.from(p, c, new Hydrology(p, c.precipitation, c.anomaly, 50, 777, x -> {}));
  int land = 0, lakes = 0, rivers = 0; var hs = new java.util.ArrayList<Float>();
  for (int g = 0; g < d.w*d.h; g++) { if (d.metres[g] < 0) continue; land++; hs.add(d.metres[g]); if (!Float.isNaN(d.lakeMetres[g])) lakes++; if (d.flow[g] > 200) rivers++; }
  java.util.Collections.sort(hs);
  System.out.printf("every %.0f x%.0f k%.1f: land %d, median %.0f m, p99 %.0f m, lakes %d, river cells %d%n", cfg[0], cfg[1], cfg[2], land, hs.get(hs.size()/2), hs.get(hs.size()*99/100), lakes, rivers);
}
/exit
