import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
var m = Terrain.class.getDeclaredMethod("ridgeline", double.class, double.class); m.setAccessible(true);
var rnd = new java.util.Random(1); double s = 0; for (int i = 0; i < 200000; i++) s += (float) m.invoke(t, rnd.nextDouble() * 30000, rnd.nextDouble() * 30000);
System.out.printf("mean ridgeline %.3f -> centre %.2f%n", s / 200000, 2 * s / 200000);
/exit
