import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
int picked = 0, falls = 0, spills = 0, firstFall = -1; int[] fallAt = null;
var rnd = new java.util.Random(4);
while (picked < 40) { int c = rnd.nextInt(d.w * d.h); int r = d.receiver[c]; if (r < 0 || d.metres[c] < 0 || d.flow[c] < 50 || d.ice[c] != 0 || d.metres[r] < 0) continue;
  float dropBlocks = (d.metres[c] - d.metres[r]) / 15f; if (dropBlocks < 8) continue; picked++;
  int[] b = t.blockOf(c); int n = 80, size = 2*n+1; var cs = new Terrain.Column[size*size];
  for (int z = 0; z < size; z++) for (int x = 0; x < size; x++) cs[x+z*size] = t.column(b[0]+x-n, b[1]+z-n);
  boolean fell = false;
  for (int z = 1; z < size-1; z++) for (int x = 1; x < size-1; x++) { var col = cs[x+z*size]; if (col.water() == Terrain.Column.NO_WATER || col.water() < Terrain.SEA_Y) continue;
    for (int k : new int[]{-1, 1, -size, size}) { var o = cs[x+z*size+k]; int otop = Math.max(Math.max(o.ground(), o.water()), o.ice()); // glacier ice holds water like rock
      if (col.water() - otop > 1) { if (o.water() != Terrain.Column.NO_WATER) { if (col.water() - o.water() >= 4) { fell = true; if (fallAt == null) fallAt = new int[]{b[0]+x-n, b[1]+z-n}; } } else { spills++; System.out.println("spill " + (b[0]+x-n) + " " + (b[1]+z-n) + " water " + col.water() + " ground " + col.ground() + " dry " + o.ground() + " " + col.biome() + "/" + o.biome() + " cover " + o.cover() + " volcanic " + o.volcanic()); } } } }
  if (fell) falls++; }
System.out.printf("steep reaches %d, with a waterfall of 4+ blocks %d, water spilling onto dry land: %d column edges; a fall at %s%n", picked, falls, spills, java.util.Arrays.toString(fallAt));
/exit
