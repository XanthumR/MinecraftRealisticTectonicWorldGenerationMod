// Hash of a whole simulated planet (every saved array and volcano) plus the time it took: simulation refactors must keep it.
// -Dwidth=N (default 1024), -Dmedium=true for Medium-sized continents, -Dseed=N (default 777).
import com.realtectonics.sim.*;
int width = Integer.getInteger("width", 1024);
long seed = Long.getLong("seed", 777);
PlanetSettings ps = PlanetSettings.DEFAULT.withWidth(width).withMediumContinents(Boolean.getBoolean("medium"));
long t0 = System.nanoTime();
PlanetData d = PlanetData.generate(seed, ps, p -> {});
double secs = (System.nanoTime() - t0) / 1e9;
long hsh = 1;
for (float[] a : new float[][]{d.metres, d.lakeMetres, d.flow, d.temperature, d.precipitation, d.amplitude, d.summerRain, d.faultSide, d.faultSlip, d.plateVx, d.plateVy})
    for (float v : a) hsh = hsh * 31 + Float.floatToIntBits(v);
for (int v : d.receiver) hsh = hsh * 31 + v;
for (byte[] a : new byte[][]{d.rock, d.ice, d.plate}) for (byte v : a) hsh = hsh * 31 + v;
for (Volcano v : d.volcanoes) hsh = hsh * 31 + Float.floatToIntBits(v.u()) * 7 + Float.floatToIntBits(v.v()) + v.age();
int land = 0; for (float m : d.metres) if (m >= 0) land++;
System.out.printf("width %d, plates %d, land %.1f%%, %.1f s, hash %x%n", width, d.plateVx.length, 100.0 * land / d.metres.length, secs, hsh);
System.exit(0);
