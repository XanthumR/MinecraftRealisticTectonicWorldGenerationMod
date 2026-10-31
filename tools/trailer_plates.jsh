import com.realtectonics.sim.*;
// Trailer footage: the plates of seed -Dseed (default 4) at PlanetSettings.DEFAULT drifting, one frame per tectonic step, to
// out/trailer/sim/plates_NNNN.png (planet size, elevation colours, hillshade, plate edges in white), the same run
// PlanetData.generate makes; then the finished planet's game map preview (rivers, climate, biomes) as final_map.png.
long seed = Long.getLong("seed", 4); var s = PlanetSettings.DEFAULT;
new java.io.File("out/trailer/sim").mkdirs();
var planet = new Planet(s.width(), s.height(), s.plates(), s.oceanFraction(), s.volcanoes(), seed);
int w = planet.w, h = planet.h; int[] frame = {0};
int colour(float m) { // ocean deep to shallow, land lowland green to brown highland to snow
    float[][] stops = m < 0 ? new float[][]{{-6000, 8, 24, 70}, {-3000, 20, 60, 130}, {-200, 60, 130, 200}, {0, 110, 180, 220}}
                            : new float[][]{{0, 70, 140, 60}, {600, 140, 170, 80}, {1800, 150, 120, 80}, {3500, 120, 100, 90}, {5000, 245, 245, 250}};
    if (m <= stops[0][0]) return (int) stops[0][1] << 16 | (int) stops[0][2] << 8 | (int) stops[0][3];
    for (int i = 1; i < stops.length; i++) if (m <= stops[i][0]) { float t = (m - stops[i - 1][0]) / (stops[i][0] - stops[i - 1][0]); int c = 0;
        for (int k = 1; k <= 3; k++) c = c << 8 | Math.round(stops[i - 1][k] + t * (stops[i][k] - stops[i - 1][k])); return c; }
    var l = stops[stops.length - 1]; return (int) l[1] << 16 | (int) l[2] << 8 | (int) l[3];
}
void write(Planet p, String name) throws Exception {
    var img = new java.awt.image.BufferedImage(w, h, 1);
    for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) { int g = x + y * w, e = (x + 1) % w + y * w, n = x + Math.min(h - 1, y + 1) * w;
        float m = Planet.metres(p.height[g] - Planet.SEA_LEVEL), me = Planet.metres(p.height[e] - Planet.SEA_LEVEL);
        float sh = m < 0 ? 1 : Math.max(0.6f, Math.min(1.3f, 1 + (m - me) / 1500));
        int c = colour(m), r = Math.min(255, (int) ((c >> 16 & 255) * sh)), gr = Math.min(255, (int) ((c >> 8 & 255) * sh)), b = Math.min(255, (int) ((c & 255) * sh));
        if (p.owner[g] != p.owner[e] || p.owner[g] != p.owner[n]) { r = (r + 255 * 2) / 3; gr = (gr + 255 * 2) / 3; b = (b + 255 * 2) / 3; } // plate edge
        img.setRGB(x, y, r << 16 | gr << 8 | b); }
    javax.imageio.ImageIO.write(img, "png", new java.io.File("out/trailer/sim/" + name + ".png"));
}
write(planet, "plates_0000");
PlanetData.tectonics(planet, s.steps(), done -> { try { write(planet, String.format("plates_%04d", ++frame[0])); } catch (Exception ex) { throw new RuntimeException(ex); } });
var d = PlanetData.generate(seed, s, x -> {});
var t = new Terrain(d, seed, 1, 1, 2); int[] rgb = t.mapColors(false);
var img = new java.awt.image.BufferedImage(d.w, d.h, 1); img.setRGB(0, 0, d.w, d.h, rgb, 0, d.w);
javax.imageio.ImageIO.write(img, "png", new java.io.File("out/trailer/sim/final_map.png"));
d.save(java.nio.file.Path.of("out/trailer/planet.bin")); // for scouting the shot list
System.out.println("frames " + frame[0]);
/exit
