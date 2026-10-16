import com.realtectonics.sim.*;
import java.lang.reflect.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
Method wu = Terrain.class.getDeclaredMethod("warpU", double.class, double.class); wu.setAccessible(true);
Method wv = Terrain.class.getDeclaredMethod("warpV", double.class, double.class); wv.setAccessible(true);
Method tm = Terrain.class.getDeclaredMethod("temperature", double.class, double.class, float.class); tm.setAccessible(true);
Method cu = Terrain.class.getDeclaredMethod("cubic", float[].class, double.class, double.class); cu.setAccessible(true);
Field gy = Terrain.class.getDeclaredField("groundY"); gy.setAccessible(true); float[] G = (float[]) gy.get(t);
for (int[] p : new int[][]{{-2884, 4544}, {-2884 + 32, 4544}, {-2797, 3825}, {-2797, 3825 + 32}, {12895, 9433}, {12895 - 32, 9433}}) {
    double u = (double) wu.invoke(t, (double) p[0], (double) p[1]), v = (double) wv.invoke(t, (double) p[0], (double) p[1]);
    float sm = (float) cu.invoke(t, G, u, v); var c = t.column(p[0], p[1]);
    System.out.printf("%d,%d %s ground %d smooth %.0f t(ground) %.1f t(smooth) %.1f rain %.0f%n", p[0], p[1], c.biome(), c.ground(), sm,
        (float) tm.invoke(t, u, v, (float) c.ground()), (float) tm.invoke(t, u, v, sm), 0f);
}
/exit
