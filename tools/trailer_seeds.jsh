import com.realtectonics.sim.*;
// Trailer planet scouting: game map preview of seeds -Dseeds=1,2,3 at PlanetSettings.DEFAULT to out/trailer/seed_N.png, with
// counts of what a trailer wants on one planet: active volcanoes, glacier ice share, the biggest river, biome mix.
for (String s : System.getProperty("seeds", "1,2,3").split(",")) {
    long seed = Long.parseLong(s); var d = PlanetData.generate(seed, PlanetSettings.DEFAULT, x -> {});
    var t = new Terrain(d, seed, 1, 1, 2); int[] rgb = t.mapColors(false);
    var img = new java.awt.image.BufferedImage(d.w, d.h, 1); img.setRGB(0, 0, d.w, d.h, rgb, 0, d.w);
    javax.imageio.ImageIO.write(img, "png", new java.io.File("out/trailer/seed_" + seed + ".png"));
    int land = 0, ice = 0; float flow = 0; for (int g = 0; g < d.w * d.h; g++) if (d.metres[g] >= 0) { land++; if (d.ice[g] != 0) ice++; flow = Math.max(flow, d.flow[g]); }
    long active = d.volcanoes.stream().filter(v -> !v.gone() && v.active()).count();
    System.out.printf("seed %d: land %.0f%%, ice %.1f%% of land, active volcanoes %d, top flow %.0f%n", seed, 100.0 * land / (d.w * d.h), 100.0 * ice / land, active, flow);
}
/exit
