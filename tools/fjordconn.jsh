import com.realtectonics.sim.*;
var p = new Planet(1024, 512, 14, 777); for (int i = 0; i < 300; i++) p.step();
var c = new Climate(p); var hy = new Hydrology(p, c.precipitation, 50, 777);
var d = PlanetData.from(p, c, hy); var t = new Terrain(d, 777);
// heads: fjord cells no fjord cell drains into
boolean[] fed = new boolean[d.w*d.h]; for (int g = 0; g < fed.length; g++) if (d.ice[g]==2 && d.receiver[g]>=0) fed[d.receiver[g]] = true;
var heads = new java.util.ArrayList<Integer>(); for (int g = 0; g < fed.length; g++) if (d.ice[g]==2 && !fed[g]) heads.add(g);
java.util.Collections.shuffle(heads, new java.util.Random(5));
int tried = 0, open = 0, noWater = 0, R = 450, N = 2*R+1;
for (int head : heads) { if (tried == 30) break; tried++;
  int[] b = t.blockOf(head); int sx = Integer.MIN_VALUE, sz = 0;
  for (int dz = -64; dz <= 64 && sx == Integer.MIN_VALUE; dz += 2) for (int dx = -64; dx <= 64; dx += 2) { var col = t.column(b[0]+dx, b[1]+dz); if (col.water()==Terrain.SEA_Y-1 && col.ground() < Terrain.SEA_Y-3) { sx = b[0]+dx; sz = b[1]+dz; break; } }
  if (sx == Integer.MIN_VALUE) { noWater++; continue; }
  byte[] seen = new byte[N*N]; var q = new java.util.ArrayDeque<int[]>(); q.add(new int[]{sx, sz}); seen[R + R*N] = 1; boolean reached = false;
  while (!q.isEmpty() && !reached) { int[] a = q.poll();
    for (int[] o : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) { int x = a[0]+o[0], z = a[1]+o[1], ix = x - sx + R, iz = z - sz + R; if (ix<0||iz<0||ix>=N||iz>=N||seen[ix+iz*N]!=0) continue; seen[ix+iz*N]=1;
      var col = t.column(x, z); if (col.water() != Terrain.SEA_Y-1) continue; if (col.ground() < Terrain.SEA_Y - 60) { reached = true; break; } q.add(new int[]{x, z}); } }
  if (reached) open++; else System.out.println("closed fjord near " + java.util.Arrays.toString(b));
}
System.out.printf("fjord heads %d; sampled %d: reach the open sea %d, no fjord water near head %d%n", heads.size(), tried, open, noWater);
/exit
