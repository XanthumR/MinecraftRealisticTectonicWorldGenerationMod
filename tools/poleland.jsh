import com.realtectonics.sim.*;
// Land share of the box 25-55° poleward and ±30° of longitude, at a few Earth places: how much continent lies poleward.
var d = PlanetData.generate(1, PlanetSettings.DEFAULT.withWidth(1024).withEarth(true), x -> {});
double[][] spots = {{18, 62, 0}, {15, 88, 0}, {20, -88, 0}, {15, -70, 0}, {15, 10, 0}, {15, 40, 0}, {-15, 130, 0}, {-15, 40, 0}, {-15, -50, 0}, {18, 115, 0}, {15, -150, 0}, {10, 140, 0}};
for (double[] s : spots) {
    int land = 0, all = 0; double sign = Math.signum(s[0]);
    for (int y = 0; y < d.h; y++) { double lat = (Math.toDegrees(Math.asin(1 - 2 * (y + 0.5) / d.h))) * sign; if (lat < 25 || lat > 55) continue;
        for (double lon = s[1] - 30; lon <= s[1] + 30; lon += 360.0 / d.w) { int x = Math.floorMod((int) ((lon + 180) / 360 * d.w), d.w); all++; if (d.metres[x + y * d.w] >= 0) land++; } }
    System.out.printf("lat %4.0f lon %5.0f: land %.2f%n", s[0], s[1], land / (double) all);
}
/exit
