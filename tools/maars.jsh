import com.realtectonics.sim.*;
// Format 88 maars: counts them (lake, dry, breached), lists a few, and checks every maar lake for water spilling onto
// lower dry ground (0; glacier ice counts as a wall), and posts round them after rockfall: a column 2+ blocks over all four
// neighbours (0; a ring rising as a cliff at the water left them). -Dplanet=file (else seed 777 is generated), -Dfmt=N.
var pf = System.getProperty("planet");
var d = pf == null ? PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}) : PlanetData.load(java.nio.file.Path.of(pf));
d.format = Integer.getInteger("fmt", d.format); var t = new Terrain(d, 777, 1, 1, 2);
var f = Terrain.class.getDeclaredField("conesByCell"); f.setAccessible(true);
var lists = f.get(t).getClass().getDeclaredField("lists"); lists.setAccessible(true);
var rimM = Terrain.class.getDeclaredMethod("maarRim", Class.forName("com.realtectonics.sim.Terrain$Maar")); rimM.setAccessible(true);
var seen = new java.util.HashSet<Object>(); int lakes = 0, dry = 0, breached = 0, spills = 0, listed = 0, posts = 0; String spillAt = null;
for (var list : ((java.util.Map<Integer, java.util.List<?>>) lists.get(f.get(t))).values()) for (Object c : list) { if (!seen.add(c)) continue;
  var mf = c.getClass().getDeclaredMethod("maars"); mf.setAccessible(true);
  for (Object m : (Object[]) mf.invoke(c)) {
    var acc = new java.util.HashMap<String, Object>();
    for (var comp : m.getClass().getRecordComponents()) { var a = comp.getAccessor(); a.setAccessible(true); acc.put(comp.getName(), a.invoke(m)); }
    double mx = (double) acc.get("x"), mz = (double) acc.get("z"); float r = (float) acc.get("r"); boolean lake = (boolean) acc.get("lake");
    float rim = (float) rimM.invoke(t, m);
    if (rim == Float.NEGATIVE_INFINITY) { breached++; continue; }
    if (lake) lakes++; else dry++;
    int bx = (int) Math.round(mx), bz = (int) Math.round(mz);
    var mid = t.column(bx, bz);
    if (listed++ < 8) System.out.printf("maar x %d z %d r %.0f rim %.0f floor %d water %s%n", bx, bz, r, rim, mid.ground(), mid.water() == Terrain.Column.NO_WATER ? "-" : "" + mid.water());
    int R = (int) (2 * r) + 2;
    for (int z = bz - R; z <= bz + R; z++) for (int x = bx - R; x <= bx + R; x++) { if (Math.hypot(x - mx, z - mz) >= 1.5 * r) continue;
      int g = t.relaxedColumn(x, z).ground();
      int hi = Integer.MIN_VALUE; for (int[] o : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) hi = Math.max(hi, t.relaxedColumn(x + o[0], z + o[1]).ground());
      if (g >= hi + 2) { posts++; if (posts <= 3) System.out.println("  post " + x + " " + z + " ground " + g + " neighbours up to " + hi); } }
    for (int z = bz - R; z <= bz + R; z++) for (int x = bx - R; x <= bx + R; x++) { if (Math.hypot(x - mx, z - mz) >= r) continue; // only the maar's own water: rivers round it keep their old one-block steps
      var a = t.column(x, z); if (a.water() == Terrain.Column.NO_WATER || a.water() < a.ground()) continue;
      for (int[] o : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) { var b = t.column(x + o[0], z + o[1]);
        if (b.water() == Terrain.Column.NO_WATER && b.lava() == Terrain.Column.NO_WATER && Math.max(b.ground(), b.ice()) < a.water()) { spills++; if (spills <= 12) System.out.printf("  spill %d %d water %d ground %d next dry %d (%d %d) e %.2f next e %.2f rim %.0f lake %s%n", x, z, a.water(), a.ground(), b.ground(), x + o[0], z + o[1], Math.hypot(x - mx, z - mz) / r, Math.hypot(x + o[0] - mx, z + o[1] - mz) / r, rim, lake); if (spillAt == null) spillAt = x + " " + z + " water " + a.water() + " next dry " + b.ground(); } } }
  }
}
System.out.println("maars: lakes " + lakes + ", dry " + dry + ", breached " + breached + "; spill edges " + spills + ", posts " + posts + (spillAt == null ? "" : " (first " + spillAt + ")"));
/exit
