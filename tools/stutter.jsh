import com.realtectonics.sim.*;
// Microstutter check: a "render thread" (max priority, like Minecraft's) spends 5 ms of CPU per frame while 4 worldgen
// threads pull fresh rockfall tiles nonstop. Wall time per frame above 5 ms is time the render thread lost its core.
// Run with -R-XX:+UseZGC. The stand-in runs at normal priority, as Minecraft's does (-Dprio to change): with tiles on the
// shared pool at normal priority 56-60 frames went over 10 ms (worst 30-59 ms); on Terrain.TILE_POOL (lowest priority) 1-2 (~11 ms).
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
t.relaxedColumns(0, 0, 16, 16); // warm up
var done = new java.util.concurrent.atomic.AtomicBoolean(); var next = new java.util.concurrent.atomic.AtomicInteger();
var gens = new java.util.ArrayList<Thread>();
for (int k = 0; k < 4; k++) { var th = new Thread(() -> { while (!done.get()) { int i = next.getAndIncrement(); if (i >= 40) break; t.relaxedColumns(1024 + i * 256, 4096, 16, 16); } }); th.start(); gens.add(th); }
var frames = new java.util.ArrayList<Double>(); Thread.currentThread().setPriority(Integer.getInteger("prio", Thread.NORM_PRIORITY)); // Minecraft's render thread runs at normal priority
long end = System.nanoTime() + 60_000_000_000L; double sink = 0;
while (gens.stream().anyMatch(Thread::isAlive) && System.nanoTime() < end) {
    long s = System.nanoTime(), work = 0;
    while (work < 5_000_000) { long a = System.nanoTime(); for (int i = 0; i < 2000; i++) sink += Math.sqrt(i + sink % 7); work += Math.min(System.nanoTime() - a, 200_000); } // ponytail: CPU-time proxy, gaps over 0.2 ms count as lost
    frames.add((System.nanoTime() - s) / 1e6);
}
done.set(true); java.util.Collections.sort(frames); int n = frames.size();
System.out.printf("frames %d  median %.1f ms  p99 %.1f ms  max %.1f ms  frames over 10 ms: %d%n", n, frames.get(n / 2), frames.get((int) (n * 0.99)), frames.get(n - 1), frames.stream().filter(f -> f > 10).count());
/exit
