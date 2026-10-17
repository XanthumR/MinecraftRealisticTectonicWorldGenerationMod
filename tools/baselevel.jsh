import com.realtectonics.sim.*;
// Format 46 (base level): how much ground it raises over an area, by distance from the nearest river's bank.
// -Dplanet -Dseed -Dms -Dzoom -Drw -Dcx -Dcz -Dn; seed 777 defaults without -Dplanet.
var path = System.getProperty("planet"); long seed = Long.getLong("seed", 777);
var d = path == null ? PlanetData.generate(seed, PlanetSettings.DEFAULT, x -> {}) : PlanetData.load(java.nio.file.Path.of(path));
float ms = Float.parseFloat(System.getProperty("ms", "1")), rw = Float.parseFloat(System.getProperty("rw", "2")); int zoom = Integer.getInteger("zoom", 1);
d.format = 45; var a = new Terrain(d, seed, ms, zoom, rw); d.format = 46; var b = new Terrain(d, seed, ms, zoom, rw);
int cx = Integer.getInteger("cx", 0), cz = Integer.getInteger("cz", 0), n = Integer.getInteger("n", 512);
int changed = 0, total = 0, maxUp = 0; long sumUp = 0;
for (int z = 0; z < n; z += 2) for (int x = 0; x < n; x += 2) {
    var p = a.column(cx - n / 2 + x, cz - n / 2 + z); var q = b.column(cx - n / 2 + x, cz - n / 2 + z); total++;
    int up = q.ground() - p.ground(); if (up != 0) { changed++; sumUp += up; maxUp = Math.max(maxUp, up); } }
System.out.printf("columns raised %.1f%%, mean raise %.1f blocks where raised, max %d%n", 100.0 * changed / total, changed > 0 ? sumUp / (double) changed : 0, maxUp);
/exit
