import com.realtectonics.sim.*;
import java.lang.reflect.*;
// Where the finished ground lies far below the land's own surface near the player: what cuts it there?
var d = PlanetData.load(java.nio.file.Path.of("run/saves/New World (3)/realtectonics/planet.bin"));
d.format = Integer.getInteger("fmt", d.format); var t = new Terrain(d, -651888280600469852L, 1.1f, 2, 2.51f);
Method su = Terrain.class.getDeclaredMethod("surface", double.class, double.class, double.class, double.class, float[].class); su.setAccessible(true);
Method wu = Terrain.class.getDeclaredMethod("warpU", double.class, double.class); wu.setAccessible(true);
Method wv = Terrain.class.getDeclaredMethod("warpV", double.class, double.class); wv.setAccessible(true);
int px = -23106, pz = 1485, R = 800; java.util.List<int[]> cuts = new java.util.ArrayList<>();
for (int z = pz - R; z <= pz + R; z += 8) for (int x = px - R; x <= px + R; x += 8) { double ux = x / 2.0, uz = z / 2.0;
  float s = (float) su.invoke(t, ux, uz, (double) wu.invoke(t, ux, uz), (double) wv.invoke(t, ux, uz), new float[3]); var c = t.column(x, z);
  int cut = Math.round(s) - c.ground(); if (cut > 60) cuts.add(new int[]{cut, x, z, c.ground(), c.water(), c.ice()}); }
cuts.sort((a, b) -> b[0] - a[0]);
for (int i = 0; i < Math.min(8, cuts.size()); i++) System.out.println("cut " + cuts.get(i)[0] + " at " + cuts.get(i)[1] + " " + cuts.get(i)[2] + " ground " + cuts.get(i)[3] + " water " + cuts.get(i)[4] + " ice " + cuts.get(i)[5]);
System.out.println("deep cuts " + cuts.size());
for (Volcano v : d.volcanoes) { int[] b = t.blockOf((int) v.u() + (int) v.v() * d.w); if (Math.abs(b[0] - px) < 3000 && Math.abs(b[1] - pz) < 3000) System.out.println("volcano " + java.util.Arrays.toString(b) + " caldera " + v.caldera() + " r " + v.radiusBlocks() * 2); }
/exit
