import com.realtectonics.sim.*;
// Varied volcanoes (format 36): each cone's reach along 16 directions (blocks to where it meets its base) must differ
// between directions (lobes, want > 0.1) and between volcanoes (want > 0.1), and a cone rising from the sea must leave
// a bigger island than the format-35 metre profile did (island radius ratio, want > 1.5).
var v = new Volcano(10, 10, false, 0, 0.5f, -3300, 1.6f); // an arc volcano from 3.3 km deep, like the user's
double old = 0, now = 0; for (int i = 0; i < 4000 && v.surfaceMetres(i) > 0; i++) old = i;
for (int i = 0; i < 4000 && v.surfaceMetres(i, 0, Volcano.SMOOTH_SCARS) > 0; i++) now = i;
var stats = new java.util.ArrayList<double[]>(); var rnd = new java.util.Random(1);
for (int k = 0; k < 50; k++) { var c = new Volcano(rnd.nextInt(1024), rnd.nextInt(512), k % 3 == 0, 0, rnd.nextFloat(), 0, k % 3 == 0 ? 2.2f : 1.6f);
  double[] r = new double[16]; for (int a = 0; a < 16; a++) { double ca = Math.cos(a * Math.PI / 8), sa = Math.sin(a * Math.PI / 8); int d = 0; while (d < 4000 && c.surfaceMetres(d * ca, d * sa, Volcano.SMOOTH_SCARS) > 1) d++; r[a] = d; }
  stats.add(r); }
double lobes = 0, means[] = new double[stats.size()];
for (int k = 0; k < stats.size(); k++) { var s = java.util.Arrays.stream(stats.get(k)).summaryStatistics(); means[k] = s.getAverage(); lobes += (s.getMax() - s.getMin()) / s.getAverage() / stats.size(); }
var m = java.util.Arrays.stream(means).summaryStatistics(); double sd = Math.sqrt(java.util.Arrays.stream(means).map(x -> (x - m.getAverage()) * (x - m.getAverage())).sum() / means.length);
System.out.printf("island radius %.0f -> %.0f blocks (ratio %.1f, want > 1.5); lobes %.2f (want > 0.1); spread between volcanoes %.2f (want > 0.1)%n", old, now, now / old, lobes, sd / m.getAverage());
/exit
