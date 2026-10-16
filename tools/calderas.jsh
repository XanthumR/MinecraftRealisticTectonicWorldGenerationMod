import com.realtectonics.sim.*;
// Calderas and cinder cones: list a few, and check each caldera lake and cinder for water spilling.
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
var f = Terrain.class.getDeclaredField("conesByCell"); f.setAccessible(true);
var seen = new java.util.HashSet<Object>(); int cal = 0, cin = 0, spills = 0; String calAt = null, cinAt = null;
for (var list : ((java.util.Map<Integer, java.util.List<?>>) f.get(t)).values()) for (Object c : list) { if (!seen.add(c)) continue;
  var rc = c.getClass(); var acc = new java.util.HashMap<String, Object>();
  for (var comp : rc.getRecordComponents()) { var m = comp.getAccessor(); m.setAccessible(true); acc.put(comp.getName(), m.invoke(c)); }
  double cx = (double) acc.get("x"), cz = (double) acc.get("z"); float cr = (float) acc.get("craterRadius");
  int bx = (int) Math.round(cx), bz = (int) Math.round(cz);
  if ((boolean) acc.get("caldera")) { cal++; if (calAt == null) calAt = bx + " " + bz + " r" + cr;
    int R = (int) cr + 6; for (int z = bz - R; z <= bz + R; z++) for (int x = bx - R; x <= bx + R; x++) { var a = t.column(x, z); if (a.water() == Terrain.Column.NO_WATER || a.water() < a.ground()) continue;
      for (int[] o : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) { var b = t.column(x + o[0], z + o[1]); if (b.water() == Terrain.Column.NO_WATER && b.ground() < a.water()) { spills++; if (spills % 15 == 1) System.out.println("spill " + x + " " + z + " water " + a.water() + " ground " + a.ground() + " next dry " + b.ground() + " d " + Math.round(Math.hypot(x - cx, z - cz)) + " cr " + Math.round(cr) + " " + a.cover()); } } } }
  float[][] k = (float[][]) acc.get("cinders"); cin += k.length; if (k.length > 0 && cinAt == null) cinAt = Math.round(k[0][0]) + " " + Math.round(k[0][1]);
}
System.out.println("volcanoes " + seen.size() + ", calderas " + cal + " (first " + calAt + "), cinder cones " + cin + " (first " + cinAt + "), caldera spill edges " + spills);
/exit
