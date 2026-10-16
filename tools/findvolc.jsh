import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
for (var v : d.volcanoes) if (v.active() && !v.hotspot() && v.baseMetres() > 200) { System.out.println("active arc volcano at " + java.util.Arrays.toString(t.blockOf((int) v.u() + (int) v.v() * d.w)) + " radius " + v.radiusBlocks()); break; }
/exit
