import com.realtectonics.sim.*;
long t = System.nanoTime();
var p = new Planet(1024, 512, 14, 0.7f, 1, 777); for (int i = 0; i < 300; i++) p.step();
long t1 = System.nanoTime();
long hash = 0; for (int g = 0; g < p.height.length; g++) hash = hash * 31 + Float.floatToIntBits(p.height[g]) + 7L * p.owner[g] + Float.floatToIntBits(p.subduction[g]) * 3L;
System.out.printf("plates %.2fs hash %x volcanoes %d%n", (t1 - t) / 1e9, hash, p.volcanoes().size());
/exit
