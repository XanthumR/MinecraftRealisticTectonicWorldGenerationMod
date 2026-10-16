import com.realtectonics.sim.*;
var p = new Planet(1024, 512, 14, 0.7f, 1, 777); PlanetData.tectonics(p, 300, x -> {});
var fvx = Planet.Plate.class.getDeclaredField("vx"); fvx.setAccessible(true); var fvy = Planet.Plate.class.getDeclaredField("vy"); fvy.setAccessible(true);
int w = p.w, h = p.h, rift = 0, boundaryLand = 0;
for (int y = 0; y < h - 1; y++) for (int x = 0; x < w; x++) { int g = x + y * w;
  for (int k = 0; k < 2; k++) { int n = k == 0 ? (x + 1) % w + y * w : g + w; int a = p.owner[g], b = p.owner[n]; if (a == b) continue;
    if (p.height[g] < Planet.SEA_LEVEL || p.height[n] < Planet.SEA_LEVEL) continue; boundaryLand++;
    float dvx = (float) fvx.get(p.plates[b]) - (float) fvx.get(p.plates[a]), dvy = (float) fvy.get(p.plates[b]) - (float) fvy.get(p.plates[a]);
    float sep = k == 0 ? dvx : dvy; if (sep > 0.1f) rift++; } }
System.out.println("land plate boundary edges " + boundaryLand + ", diverging (rifts) " + rift);
/exit
