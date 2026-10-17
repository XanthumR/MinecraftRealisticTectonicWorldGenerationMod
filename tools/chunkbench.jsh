import com.realtectonics.sim.*;
// Chunk-generation stand-in (seed 777, out/p39_777.bin at -Dfmt, default 46): for each chunk of an n x n chunk area,
// the 16 biome lookups the game makes before building it (biomeAt or, with -Dcached=true, biomeAtCached), then the
// 18 x 18 columns fillFromNoise reads. Prints the time and checks the cached biomes equal biomeAt's.
var d = PlanetData.load(java.nio.file.Path.of("out/p39_777.bin")); d.format = Integer.getInteger("fmt", 46);
var t = new Terrain(d, 777, 1, 1, 2); var ref = new Terrain(d, 777, 1, 1, 2);
boolean cached = Boolean.getBoolean("cached"); int n = Integer.getInteger("n", 32), x0 = Integer.getInteger("cx", 0), z0 = Integer.getInteger("cz", 0);
long s = System.nanoTime(), biomeNanos = 0; int mismatch = 0;
for (int cz = 0; cz < n; cz++) for (int cx = 0; cx < n; cx++) {
    int bx = x0 + cx * 16, bz = z0 + cz * 16; long b = System.nanoTime();
    for (int q = 0; q < 16; q++) { int x = bx + (q & 3) * 4, z = bz + (q >> 2) * 4; var got = cached ? t.biomeAtCached(x, z) : t.biomeAt(x, z); if (cached && got != ref.biomeAt(x, z)) mismatch++; }
    biomeNanos += System.nanoTime() - b;
    t.relaxedColumns(bx - 1, bz - 1, 18, 18);
}
System.out.printf("cached=%b: %d chunks in %.1fs, biome lookups %.2fs, mismatches %d%n", cached, n * n, (System.nanoTime() - s) / 1e9, biomeNanos / 1e9, mismatch);
/exit
