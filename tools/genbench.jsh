import com.realtectonics.sim.*;
import java.util.concurrent.*;
// Chunk generation load like Distant Horizons: 8 threads, a 48x48-chunk area in random order. Each chunk asks for its
// 18x18 columns twice (fill and carvers), as the generator does. Old path: column(); new: relaxedColumn() (format 29).
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {});
int cx0 = Integer.getInteger("cx", 2944) >> 4, cz0 = Integer.getInteger("cz", 3200) >> 4, N = 48;
var order = new java.util.ArrayList<int[]>(); for (int z = 0; z < N; z++) for (int x = 0; x < N; x++) order.add(new int[]{cx0 + x, cz0 + z});
java.util.Collections.shuffle(order, new java.util.Random(1));
for (int fmt : new int[]{Integer.getInteger("f1", 28), Integer.getInteger("f2", 29)}) {
  d.format = fmt; var t = new Terrain(d, 777, 1, 1, 2);
  var pool = Executors.newFixedThreadPool(8); long s = System.nanoTime(); var jobs = new java.util.ArrayList<Future<?>>();
  for (int[] c : order) jobs.add(pool.submit(() -> { for (int pass = 0; pass < 2; pass++) t.relaxedColumns(c[0] * 16 - 1, c[1] * 16 - 1, 18, 18); }));
  for (var j : jobs) j.get(); pool.shutdown();
  System.out.printf("format %d: %.0f chunks/s%n", fmt, N * N / ((System.nanoTime() - s) / 1e9));
}
/exit
