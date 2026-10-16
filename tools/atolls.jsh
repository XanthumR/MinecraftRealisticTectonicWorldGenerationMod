import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
int n = 0;
for (var v : d.volcanoes) { if (!v.hotspot() || v.gone()) continue; float top = v.surfaceMetres(0); int g = (int) v.u() + (int) v.v() * d.w; float temp = d.temperature[g];
  if (top < 100 && top > -2500 && Math.abs(Climate.latitude((int) v.v(), d.h)) < 35) { n++; if (n <= 5) System.out.printf("age %d summit %.0f m base %.0f radius %.0f blocks at %s%n", v.age(), top, v.baseMetres(), v.radiusBlocks(), java.util.Arrays.toString(t.blockOf(g))); } }
System.out.println("atoll candidates " + n);
/exit
