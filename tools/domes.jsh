import com.realtectonics.sim.*;
// Format 107 dome mountains of a fresh planet (-Dseed, default 777; -Do=file to save it): where each stands, its size, how far it was bowed up and how far its core still stands over the plains round it (seed 777: 4 domes, cores 1.1-2.3 km over their plains).
var d = PlanetData.generate(Long.getLong("seed", 777), PlanetSettings.DEFAULT, p -> {});
if (System.getProperty("o") != null) d.save(java.nio.file.Path.of(System.getProperty("o")));
var t = new Terrain(d, Long.getLong("seed", 777), 1, 1, 2);
System.out.println("domes " + d.domes.size());
for (Dome m : d.domes) { int g = (int) m.u() + (int) m.v() * d.w; float edge = 9e9f, top = -9e9f;
  for (int dy = -8; dy <= 8; dy++) for (int dx = -8; dx <= 8; dx++) { int y = (int) m.v() + dy; if (y < 0 || y >= d.h) continue; int k = Math.floorMod((int) m.u() + dx, d.w) + y * d.w; float l = m.lift(k % d.w + 0.5, y + 0.5, d.w);
    if (l > 0.5f) top = Math.max(top, d.metres[k]); else if (l == 0 && Math.hypot(dx, dy) < m.a() + 3) edge = Math.min(edge, d.metres[k]); }
  int[] b = t.blockOf(g);
  System.out.printf("dome at block %d %d: %.1f x %.1f cells, bowed %.0f m; core now %.0f m, plains round %.0f m, rock %s%n", b[0], b[1], 2 * m.a(), 2 * m.b(), m.metres(), top, edge, Rock.values()[d.rock[g]]); }
/exit
