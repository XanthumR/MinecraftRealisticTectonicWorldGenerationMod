import com.realtectonics.sim.*;
// Biome clashes: how often a dry hot biome sits within 32 blocks of a snowy one. Should be ~0 with transitions.
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
var hot = java.util.EnumSet.of(Biome.DESERT, Biome.BADLANDS, Biome.WOODED_BADLANDS, Biome.ERODED_BADLANDS, Biome.SAVANNA);
var snow = java.util.EnumSet.of(Biome.TUNDRA, Biome.ICE_CAP, Biome.SNOWY_PLAINS, Biome.ICE_SPIKES, Biome.SNOWY_TAIGA, Biome.SNOWY_SLOPES,
        Biome.SNOWY_PEAKS, Biome.JAGGED_PEAKS, Biome.GROVE, Biome.SNOWY_BEACH, Biome.FROZEN_RIVER);
var rnd = new java.util.Random(3); int hots = 0, clashes = 0;
for (int i = 0; i < 300000; i++) {
    int x = rnd.nextInt(t.circumference) - t.circumference / 2, z = rnd.nextInt(t.southPoleZ() - t.northPoleZ()) + t.northPoleZ();
    if (!hot.contains(t.biomeAt(x, z))) continue;
    hots++;
    boolean clash = false;
    for (int k = 0; k < 8 && !clash; k++) clash = snow.contains(t.biomeAt(x + (int) (32 * Math.cos(k * Math.PI / 4)), z + (int) (32 * Math.sin(k * Math.PI / 4))));
    if (clash) { clashes++; if (clashes <= 5) System.out.println("clash at " + x + " " + z + " " + t.biomeAt(x, z)); }
}
System.out.printf("hot samples %d, next to snow %d (%.2f%%)%n", hots, clashes, 100.0 * clashes / hots);
/exit
