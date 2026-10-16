import com.realtectonics.sim.*;
// Earth planet's climate only, straight to out/koppen/sim.f32 (see climexport.jsh), for tools/tune.sh.
long seed = Long.getLong("seed", 0); var d = seed == 0 ? PlanetData.generate(1, PlanetSettings.DEFAULT.withWidth(Integer.getInteger("width", 1024)).withEarth(true), x -> {}) : PlanetData.generate(seed, PlanetSettings.DEFAULT, x -> {}); // -Dseed: a random planet instead
var buf = java.nio.ByteBuffer.allocate(5 * d.w * d.h * 4).order(java.nio.ByteOrder.LITTLE_ENDIAN);
for (float[] a : new float[][]{d.metres, d.temperature, d.amplitude, d.precipitation, d.summerRain}) for (float v : a) buf.putFloat(v);
java.nio.file.Files.write(java.nio.file.Path.of("out/koppen/sim.f32"), buf.array());
java.nio.file.Files.writeString(java.nio.file.Path.of("out/koppen/sim.txt"), d.w + " " + d.h);
/exit
