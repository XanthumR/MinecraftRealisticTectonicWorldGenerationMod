package com.realtectonics.sim;

/**
 * The plates at one moment of the run, at half the planet's resolution: ground in METRES_STEP steps and the plate under
 * each cell. A run keeps FRAMES of them (PlanetData.drift, format 64) so the map can replay the plates drifting.
 */
public record Drift(int w, int h, byte[] ground, byte[] plate) {
    public static final int FRAMES = 30;          // kept per run, evenly over its steps; the map blends between them
    static final float METRES_STEP = 60;          // m per unit of ground: -7.7 to +7.6 km
    private static final float[][] COLORS = {     // m, r, g, b: deep sea to shallows, then lowland green to highland brown to snow
            {-6000, 8, 24, 70}, {-3000, 20, 60, 130}, {-200, 60, 130, 200}, {0, 110, 180, 220},
            {0, 70, 140, 60}, {600, 140, 170, 80}, {1800, 150, 120, 80}, {3500, 120, 100, 90}, {5000, 245, 245, 250}};

    /** The planet as it stands, every other cell. */
    public static Drift of(Planet p) {
        int w = p.w / 2, h = p.h / 2;
        byte[] ground = new byte[w * h], plate = new byte[w * h];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int g = 2 * x + 2 * y * p.w;
                ground[x + y * w] = (byte) Math.clamp(Math.round(Planet.metres(p.height[g] - Planet.SEA_LEVEL) / METRES_STEP), -128, 127);
                plate[x + y * w] = (byte) p.owner[g];
            }
        return new Drift(w, h, ground, plate);
    }

    /** RGB per cell: elevation colours lit from the west, plate boundaries pale. */
    public int[] colors() {
        int[] rgb = new int[w * h];
        for (int g = 0; g < rgb.length; g++) {
            int x = g % w, e = (x + 1) % w + g - x, n = Math.min(g + w, rgb.length - 1);
            float m = ground[g] * METRES_STEP, east = ground[e] * METRES_STEP;
            int i = m < 0 ? 1 : 5; // sea and land ramps meet at 0 m without blending
            while (i < (m < 0 ? 3 : COLORS.length - 1) && m > COLORS[i][0]) i++;
            float t = Math.clamp((m - COLORS[i - 1][0]) / (COLORS[i][0] - COLORS[i - 1][0]), 0, 1);
            float shade = m < 0 ? 1 : Math.clamp(1 + (m - east) / 3000, 0.6f, 1.3f); // the east cell two cells away: twice the drop of the full map's 1500
            boolean edge = plate[g] != plate[e] || plate[g] != plate[n];
            int c = 0;
            for (int k = 1; k <= 3; k++) {
                float v = Math.min(255, (COLORS[i - 1][k] + t * (COLORS[i][k] - COLORS[i - 1][k])) * shade);
                c = c << 8 | Math.round(edge ? (v + 2 * 255) / 3 : v);
            }
            rgb[g] = c;
        }
        return rgb;
    }
}
