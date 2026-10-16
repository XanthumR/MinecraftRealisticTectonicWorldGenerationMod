import com.realtectonics.sim.*;
for (int w : new int[]{1024, 2048}) {
long t = System.nanoTime();
var p = new Planet(w, w / 2, 14, 0.7f, 1, 777); for (int i = 0; i < 300; i++) p.step();
long t1 = System.nanoTime(); var c = new Climate(p);
long t2 = System.nanoTime(); var hy = new Hydrology(p, c.precipitation, c.anomaly, 50, 777);
long t3 = System.nanoTime(); var d = PlanetData.from(p, c, hy);
long t4 = System.nanoTime(); var tr = new Terrain(d, 777);
long t5 = System.nanoTime();
System.out.printf("width %d: plates %.1fs, climate %.1fs, hydrology %.1fs, data %.1fs, terrain %.1fs%n", w, (t1-t)/1e9, (t2-t1)/1e9, (t3-t2)/1e9, (t4-t3)/1e9, (t5-t4)/1e9);
}
/exit
