import com.realtectonics.sim.*;
// Exports a planet's simulated climate for tools/koppen.py: out/koppen/sim.f32, five little-endian float grids of
// w x h in turn (metres, mean temperature °C, half the summer-winter range °C, rain mm/yr, summer share), and
// out/koppen/sim.txt with "w h". -Dplanet (default out/earth.bin).
var d = PlanetData.load(java.nio.file.Path.of(System.getProperty("planet", "out/earth.bin")));
var buf = java.nio.ByteBuffer.allocate(5 * d.w * d.h * 4).order(java.nio.ByteOrder.LITTLE_ENDIAN);
for (float[] a : new float[][]{d.metres, d.temperature, d.amplitude, d.precipitation, d.summerRain}) for (float v : a) buf.putFloat(v);
java.nio.file.Files.write(java.nio.file.Path.of("out/koppen/sim.f32"), buf.array());
java.nio.file.Files.writeString(java.nio.file.Path.of("out/koppen/sim.txt"), d.w + " " + d.h);
/exit
