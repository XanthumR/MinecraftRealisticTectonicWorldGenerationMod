import com.realtectonics.sim.*;
// Simulated calderas: how many formed, how many hold a lake in the simulation, and where one is in blocks.
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
int n = 0, lakes = 0, w = d.w; java.util.List<String> at = new java.util.ArrayList<>();
for (Volcano v : d.volcanoes) { if (v.gone() || !v.caldera()) continue; n++;
  boolean lake = false; for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) { int g = Math.floorMod((int) v.u() + dx, w) + Math.clamp((int) v.v() + dy, 0, d.h - 1) * w; lake |= !Float.isNaN(d.lakeMetres[g]); }
  if (lake) lakes++;
  int bx = Math.floorMod((int) v.u() - t.spawnCell % w + w / 2, w) * 32 - w * 16, bz = ((int) v.v() - t.spawnCell / w) * 32;
  if (at.size() < 6) at.add(bx + " " + bz + (lake ? " lake" : " dry") + (v.hotspot() ? " shield" : " arc") + " r" + Math.round(v.radiusBlocks() * 0.3));
}
System.out.println("calderas " + n + ", with a lake " + lakes + " " + at);
/exit
