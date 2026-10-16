import com.realtectonics.sim.*;
// Largest river flow in each latitude row across the Nile (lon 28-36E) on out/earth.bin, and the rain there.
var d = PlanetData.load(java.nio.file.Path.of("out/earth.bin"));
for (double lat : new double[]{5, 10, 15, 20, 24, 28, 30}) {
    int y = (int) ((1 - Math.sin(Math.toRadians(lat))) * d.h / 2); float best = 0; int bx = 0;
    for (int x = (int) ((180 + 28) / 360.0 * d.w); x < (180 + 36) / 360.0 * d.w; x++) if (d.flow[x + y * d.w] > best) { best = d.flow[x + y * d.w]; bx = x; }
    System.out.printf("lat %4.0f flow %8.0f at lon %.1f rain %.0f%n", lat, best, bx * 360.0 / d.w - 180, d.precipitation[bx + y * d.w]);
}
float mx = 0; for (float f : d.flow) mx = Math.max(mx, f); System.out.println("max flow " + mx);
/exit
