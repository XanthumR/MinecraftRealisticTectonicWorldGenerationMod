import com.realtectonics.sim.*;
// Calderas and cinder cones: list a few, and check each caldera lake and cinder for water spilling. From format 81 also
// counts supervolcanoes (seed 777: 3) and lava domes (46), and that no dome stands in a lake or lava (0; under the sea is fine).
// Every crater's lava or rainwater pool spilling onto lower dry ground (0); -Dfmt=N to see an older format.
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); d.format = Integer.getInteger("fmt", d.format); var t = new Terrain(d, 777, 1, 1, 2);
var f = Terrain.class.getDeclaredField("conesByCell"); f.setAccessible(true);
var lists = f.get(t).getClass().getDeclaredField("lists"); lists.setAccessible(true); // a CellMap since the speed work
var seen = new java.util.HashSet<Object>(); int cal = 0, cin = 0, spills = 0, domes = 0, wetDomes = 0; String calAt = null, cinAt = null; int craters = 0, poolSpills = 0; String poolAt = null;
for (var list : ((java.util.Map<Integer, java.util.List<?>>) lists.get(f.get(t))).values()) for (Object c : list) { if (!seen.add(c)) continue;
  var rc = c.getClass(); var acc = new java.util.HashMap<String, Object>();
  for (var comp : rc.getRecordComponents()) { var m = comp.getAccessor(); m.setAccessible(true); acc.put(comp.getName(), m.invoke(c)); }
  double cx = (double) acc.get("x"), cz = (double) acc.get("z"); float cr = (float) acc.get("craterRadius");
  int bx = (int) Math.round(cx), bz = (int) Math.round(cz);
  if ((boolean) acc.get("caldera")) { cal++; if (calAt == null) calAt = bx + " " + bz + " r" + cr;
    int R = (int) cr + 6; for (int z = bz - R; z <= bz + R; z++) for (int x = bx - R; x <= bx + R; x++) { var a = t.column(x, z); if (a.water() == Terrain.Column.NO_WATER || a.water() < a.ground()) continue;
      for (int[] o : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) { var b = t.column(x + o[0], z + o[1]); if (b.water() == Terrain.Column.NO_WATER && b.ground() < a.water()) { spills++; if (spills % 15 == 1) System.out.println("spill " + x + " " + z + " water " + a.water() + " ground " + a.ground() + " next dry " + b.ground() + " d " + Math.round(Math.hypot(x - cx, z - cz)) + " cr " + Math.round(cr) + " " + a.cover()); } } } }
  if (cr > 0 && !(boolean) acc.get("caldera") && !(boolean) acc.get("collapsed")) { craters++; int R = (int) cr + 4;
    for (int z = bz - R; z <= bz + R; z++) for (int x = bx - R; x <= bx + R; x++) { var a = t.column(x, z); int top = Math.max(a.water(), a.lava()); if (top < a.ground() || top < Terrain.SEA_Y) continue;
      for (int[] o : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) { var b = t.column(x + o[0], z + o[1]); if (b.water() == Terrain.Column.NO_WATER && b.lava() == Terrain.Column.NO_WATER && Math.max(b.ground(), b.ice()) < top) { poolSpills++; if (poolAt == null) poolAt = x + " " + z + " pool " + top + " next dry " + b.ground(); } } } }
  float dome = (float) acc.get("dome"); if (dome > 0) { domes++; var top = t.column((int) Math.round(cx), (int) Math.round(cz)); if (top.water() > top.ground() && top.water() >= Terrain.SEA_Y || top.lava() > top.ground()) wetDomes++; }
  float[][] k = (float[][]) acc.get("cinders"); cin += k.length; if (k.length > 0 && cinAt == null) cinAt = Math.round(k[0][0]) + " " + Math.round(k[0][1]);
}
System.out.println("volcanoes " + seen.size() + ", calderas " + cal + " (first " + calAt + "), cinder cones " + cin + " (first " + cinAt + "), caldera spill edges " + spills);
System.out.println("craters " + craters + ", pool spill edges " + poolSpills + (poolAt == null ? "" : " (first " + poolAt + ")"));
System.out.println("supervolcanoes " + d.volcanoes.stream().filter(Volcano::supervolcano).count() + ", lava domes " + domes + ", domes in a lake or lava (not the sea) " + wetDomes);
/exit
