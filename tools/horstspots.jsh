import com.realtectonics.sim.*;
// Where the horsts, grabens and fault scarps are in a uview3d.jsh render (format 76+), as image pixels, for labelling it:
// same -Dplanet -Dseed -Dcx -Dcz -Dn as the render; prints "kind x y" lines for tools/annotate.py: the largest horst
// (ground raised 8+ blocks over format 74, on a block's top; the one standing highest), the scarp and the graben (stretched land left low) nearest it.
var d = PlanetData.load(java.nio.file.Path.of(System.getProperty("planet"))); d.format = Integer.getInteger("fmt", d.format);
var t = new Terrain(d, Long.getLong("seed", 777), 1, 1, 2);
int fmt = d.format; d.format = 74; var flat = new Terrain(d, Long.getLong("seed", 777), 1, 1, 2); d.format = fmt; // the same land without fault blocks
int cx = Integer.getInteger("cx"), cz = Integer.getInteger("cz"), n = Integer.getInteger("n", 192), s = 3;
var wu = Terrain.class.getDeclaredMethod("warpU", double.class, double.class); wu.setAccessible(true);
var wv = Terrain.class.getDeclaredMethod("warpV", double.class, double.class); wv.setAccessible(true);
var cu = Terrain.class.getDeclaredMethod("cubic", float[].class, double.class, double.class); cu.setAccessible(true);
var fb = Terrain.class.getDeclaredMethod("faultBlock", double.class, double.class, double.class, double.class, float.class); fb.setAccessible(true);
var cs = new Terrain.Column[n * n]; int[] kind = new int[n * n], raised = new int[n * n]; // kind: 0 none, 1 horst, 2 graben, 3 scarp
for (int z = cz - n / 2; z < cz + n / 2 + 256; z += 256) for (int x = cx - n / 2; x < cx + n / 2 + 256; x += 256) t.relaxedColumn(Math.min(x, cx + n / 2 - 1), Math.min(z, cz + n / 2 - 1));
int hi = Integer.MIN_VALUE;
for (int i = 0; i < n * n; i++) {
    int x = cx + i % n - n / 2, z = cz + i / n - n / 2;
    var c = cs[i] = t.relaxedColumn(x, z);
    hi = Math.max(hi, Math.max(Math.max(c.ground(), c.water()), c.ice()));
    if (c.water() != Terrain.Column.NO_WATER && c.water() >= c.ground()) continue;
    double u = (double) wu.invoke(t, (double) x, (double) z), v = (double) wv.invoke(t, (double) x, (double) z);
    float st = (float) cu.invoke(t, d.backArc, u, v);
    if (st < 0.3f) continue;
    float[] b = (float[]) fb.invoke(t, (double) x, (double) z, u, v, st);
    if (b[0] < 8) continue;
    int raise = raised[i] = c.ground() - flat.relaxedColumn(x, z).ground();
    kind[i] = b[1] > 0.95f && raise >= 8 ? 1 : b[1] < 0.02f && raise < 3 ? 2 : b[1] > 0.3f && b[1] < 0.7f && raise >= 4 ? 3 : 0;
}
int[] comp = new int[n * n]; java.util.Arrays.fill(comp, -1);
int[] horst = null;
for (int k = 1; k <= 3; k++) {
    var parts = new java.util.ArrayList<int[]>(); // {size, representative}
    for (int i = 0; i < n * n; i++) {
        if (kind[i] != k || comp[i] >= 0) continue;
        var q = new java.util.ArrayDeque<Integer>(); var members = new java.util.ArrayList<Integer>(); q.add(i); comp[i] = i;
        double sx = 0, sz = 0, lift = 0;
        while (!q.isEmpty()) { int c = q.poll(); members.add(c); sx += c % n; sz += c / n; lift += raised[c];
            for (int nb : new int[]{c - 1, c + 1, c - n, c + n}) if (nb >= 0 && nb < n * n && Math.abs(nb % n - c % n) <= 1 && kind[nb] == k && comp[nb] < 0) { comp[nb] = i; q.add(nb); } }
        double mx = sx / members.size(), mz = sz / members.size(); int best = members.get(0);
        for (int m : members) // horsts and scarps: where the block stands highest, the part you see; grabens: the patch's middle
            if (k == 2 ? Math.hypot(m % n - mx, m / n - mz) < Math.hypot(best % n - mx, best / n - mz) : raised[m] > raised[best]) best = m;
        parts.add(new int[]{members.size(), best, (int) (lift / members.size())});
    }
    parts.removeIf(a -> a[0] < 60);
    if (parts.isEmpty()) continue;
    final int[] h = horst;
    parts.sort(k == 1 || h == null ? (a, b) -> b[2] - a[2] : (a, b) -> Double.compare(Math.hypot(a[1] % n - h[1] % n, a[1] / n - h[1] / n), Math.hypot(b[1] % n - h[1] % n, b[1] / n - h[1] / n)));
    if (k == 1) horst = parts.get(0);
    {
        int i = parts.get(0)[1], x = i % n, z = i / n; var c = cs[i];
        int top = Math.max(Math.max(c.ground(), c.water()), c.ice());
        System.out.println(new String[]{"", "horst", "graben", "scarp"}[k] + " " + ((x - z + n) * s + s) + " " + ((x + z) * s / 2 + (hi - top) * s + 10));
    }
}
/exit
