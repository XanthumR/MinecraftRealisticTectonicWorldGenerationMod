import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
int cx = Integer.getInteger("cx"), cz = Integer.getInteger("cz"), n = Integer.getInteger("n"), size = 2 * n + 1, spills = 0, springs = 0; int[] at = null;
var cs = new Terrain.Column[size * size];
java.util.stream.IntStream.range(0, size).parallel().forEach(z -> { for (int x = 0; x < size; x++) cs[x + z * size] = t.column(cx + x - n, cz + z - n); });
for (int z = 1; z < size - 1; z++) for (int x = 1; x < size - 1; x++) { var c = cs[x + z * size]; if (c.cover() == Terrain.Cover.SPRING) springs++;
  int top = Math.max(c.water(), c.lava()); if (top == Terrain.Column.NO_WATER || top < Terrain.SEA_Y) continue;
  for (int k : new int[]{-1, 1, -size, size}) { var o = cs[x + z * size + k]; boolean pool = Math.max(o.water(), o.lava()) >= o.ground();
    if (!pool && top - o.ground() > 1) { spills++; if (at == null) at = new int[]{cx + x - n, cz + z - n}; } } }
System.out.println("spring columns " + springs + ", spills " + spills + " first " + java.util.Arrays.toString(at));
/exit
