import com.realtectonics.sim.*;
// Boreal check (format 45): over a lat/lon box of out/earth.bin (-Dlat0 -Dlat1 -Dlon0 -Dlon1, default Canada 50-62N
// 120-70W), share of land in snowy taiga vs taiga, ground with snow, and lake/river water that freezes; for formats 44 and 45.
var d = PlanetData.load(java.nio.file.Path.of("out/earth.bin"));
double la0 = Double.parseDouble(System.getProperty("lat0", "50")), la1 = Double.parseDouble(System.getProperty("lat1", "62")), lo0 = Double.parseDouble(System.getProperty("lon0", "-120")), lo1 = Double.parseDouble(System.getProperty("lon1", "-70"));
for (int fmt : new int[]{44, 45}) {
    d.format = fmt; var t = new Terrain(d, 1, 1, 1, 2); double[] o = t.mapPosition(0, 0);
    int land = 0, snowyTaiga = 0, taiga = 0, snow = 0, water = 0, ice = 0;
    for (int y = 0; y < d.h; y++) { double lat = Math.toDegrees(Math.asin(1 - 2 * (y + 0.5) / d.h)); if (lat < la0 || lat > la1) continue;
        for (int x = 0; x < d.w; x++) { double lon = (x + 0.5) * 360.0 / d.w - 180; if (lon < lo0 || lon > lo1) continue;
            int bx = (int) ((x - o[0]) * t.blocksPerCell), bz = (int) ((y - o[1]) * t.blocksPerCell); var c = t.column(bx, bz);
            if (c.water() != Terrain.Column.NO_WATER && c.water() >= c.ground() && c.water() >= 62) { water++; if (t.snowTemperature(bx, bz, c.water()) < 0) ice++; continue; }
            if (c.ground() < 63) continue; land++;
            if (c.biome() == Biome.SNOWY_TAIGA) snowyTaiga++; if (c.biome() == Biome.TAIGA || c.biome() == Biome.OLD_GROWTH_PINE_TAIGA) taiga++;
            if (t.snowTemperature(bx, bz, c.ground()) < 0) snow++; } }
    System.out.printf("format %d: snowy taiga %.0f%%, taiga %.0f%%, snow-covered %.0f%% of land; frozen %.0f%% of %d water columns%n", fmt, 100.0 * snowyTaiga / land, 100.0 * taiga / land, 100.0 * snow / land, 100.0 * ice / Math.max(water, 1), water);
}
/exit
