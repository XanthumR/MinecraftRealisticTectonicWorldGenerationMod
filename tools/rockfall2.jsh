import com.realtectonics.sim.*;
import java.lang.reflect.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
Method rt = Terrain.class.getDeclaredMethod("relaxTile", int.class, int.class); rt.setAccessible(true);
int T = 128, bx = 2944, bz = 3200, rawLeaks = 0, tiles = 0; long s = System.nanoTime();
for (int tz = -2; tz <= 2; tz++) for (int tx = -2; tx <= 2; tx++) { rt.invoke(t, bx + tx * T, bz + tz * T); tiles++; }
System.out.printf("tile alone %.0f ms%n", (System.nanoTime() - s) / 1e6 / tiles);
s = System.nanoTime(); for (int i = 0; i < T * T * 4; i++) t.column(bx + i % 256, bz + i / 256); System.out.printf("plain columns for the same area as one tile: %.0f ms%n", (System.nanoTime() - s) / 1e6 / 4);
for (int tz = -2; tz <= 2; tz++) for (int tx = -2; tx <= 2; tx++) { int x0 = bx + tx * T, z0 = bz + tz * T;
  for (int z = 1; z < T - 1; z++) for (int x = 1; x < T - 1; x++) { var c = t.column(x0 + x, z0 + z); if (c.water() == Terrain.Column.NO_WATER || c.water() < c.ground()) continue;
    for (int[] o : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) { var n = t.column(x0 + x + o[0], z0 + z + o[1]); if (n.water() == Terrain.Column.NO_WATER && c.water() - Math.max(n.ground(), n.ice()) > 1) { rawLeaks++; System.out.println("raw leak " + (x0 + x) + " " + (z0 + z)); } } } }
System.out.println("raw leak edges " + rawLeaks);
/exit
