import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
int cx = -1792, cz = 1952, n = 450, size = 2*n+1, rims = 0, tall = 0, maxRise = 0;
var cs = new Terrain.Column[size*size];
java.util.stream.IntStream.range(0, size).parallel().forEach(z -> { for (int x = 0; x < size; x++) cs[x+z*size] = t.column(cx+x-n, cz+z-n); });
for (int z = 1; z < size-1; z++) for (int x = 1; x < size-1; x++) { var c = cs[x+z*size]; if (c.cover() != Terrain.Cover.SINTER) continue; rims++;
  int lowest = Integer.MAX_VALUE; for (int k : new int[]{-1,1,-size,size}) { var o = cs[x+z*size+k]; if (o.cover() != Terrain.Cover.SINTER && o.cover() != Terrain.Cover.SPRING) lowest = Math.min(lowest, o.ground()); }
  if (lowest != Integer.MAX_VALUE) { int rise = c.ground() - lowest; maxRise = Math.max(maxRise, rise); if (rise > 3) tall++; } }
System.out.println("rim columns " + rims + ", rims standing more than 3 blocks above the land beside them: " + tall + ", highest " + maxRise);
/exit
