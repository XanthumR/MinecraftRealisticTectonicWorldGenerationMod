import com.realtectonics.sim.*;
var d = PlanetData.load(java.nio.file.Path.of("run/saves/New World/realtectonics/planet.bin")); var t = new Terrain(d, -3747261372555919606L, 1, 1, 2);
var counts = new java.util.TreeMap<String, Integer>(); int[] spikeAt = null;
for (int z = 425 - 600; z <= 425 + 600; z += 8) for (int x = 2530 - 600; x <= 2530 + 600; x += 8) { var b = t.biomeAt(x, z); counts.merge(b.name(), 1, Integer::sum); if (b == Biome.ICE_SPIKES && spikeAt == null) spikeAt = new int[]{x, z}; }
System.out.println(counts + " first ice spikes " + java.util.Arrays.toString(spikeAt));
/exit
