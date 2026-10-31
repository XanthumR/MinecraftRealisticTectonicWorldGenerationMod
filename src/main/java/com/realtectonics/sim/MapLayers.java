package com.realtectonics.sim;

/** The planet map's extra views (M, then G), RGB per cell: plates, rain and temperature. */
public final class MapLayers {
    private MapLayers() {}

    /** Each plate its own colour, lighter on land, boundaries white. Empty before format 64 (no plates saved). */
    public static int[] plates(PlanetData d) {
        int w = d.w;
        int[] rgb = new int[w * d.h];
        for (int g = 0; g < rgb.length; g++) {
            int a = d.plate[g], x = g % w, e = (x + 1) % w + g - x, n = Math.min(g + w, rgb.length - 1);
            if (a != d.plate[e] || a != d.plate[n]) { rgb[g] = 0xFFFFFF; continue; }
            int h = (a & 255) * 0x9E3779B1; // golden-ratio hash: neighbouring ids get far-apart colours
            float lit = d.metres[g] >= 0 ? 1 : 0.55f;
            rgb[g] = Math.round((90 + (h >>> 24) % 140) * lit) << 16 | Math.round((90 + (h >>> 16 & 255) % 140) * lit) << 8 | Math.round((90 + (h >>> 8 & 255) % 140) * lit);
        }
        return rgb;
    }

    /** Rain a year on a log ramp: brown desert under 100 mm, yellow, green, blue over 4000 mm; sea dark. */
    public static int[] rain(PlanetData d) {
        float[][] ramp = {{0, 150, 100, 60}, {0.5f, 230, 200, 90}, {0.75f, 60, 160, 60}, {1, 30, 70, 200}};
        int[] rgb = new int[d.w * d.h];
        for (int g = 0; g < rgb.length; g++)
            rgb[g] = d.metres[g] < 0 ? 0x1A2A40 : ramp(ramp, (float) (Math.log10(Math.max(100, d.precipitation[g])) - 2) / 1.6f);
        return rgb;
    }

    /** Yearly mean temperature: blue at -30 °C, white at 0, red at 30. */
    public static int[] temperature(PlanetData d) {
        float[][] ramp = {{0, 40, 60, 200}, {0.5f, 240, 240, 240}, {1, 200, 40, 30}};
        int[] rgb = new int[d.w * d.h];
        for (int g = 0; g < rgb.length; g++) {
            int c = ramp(ramp, (d.temperature[g] + 30) / 60);
            rgb[g] = d.metres[g] < 0 ? (c >> 1) & 0x7F7F7F : c; // the sea darker
        }
        return rgb;
    }

    /** Each plate's middle in cells {u, v} (u averaged round the planet), NaN for plates with no cells left. */
    public static float[][] plateCentres(PlanetData d) {
        int n = d.plateVx.length;
        double[] cs = new double[n], sn = new double[n], vy = new double[n];
        int[] count = new int[n];
        for (int g = 0; g < d.plate.length; g++) {
            int p = d.plate[g] & 255;
            double a = 2 * Math.PI * (g % d.w) / d.w;
            cs[p] += Math.cos(a);
            sn[p] += Math.sin(a);
            vy[p] += g / d.w;
            count[p]++;
        }
        float[][] out = new float[n][];
        for (int p = 0; p < n; p++)
            out[p] = count[p] == 0 ? new float[]{Float.NaN, Float.NaN}
                    : new float[]{(float) Math.floorMod(Math.round(Math.atan2(sn[p], cs[p]) / (2 * Math.PI) * d.w), d.w), (float) (vy[p] / count[p])};
        return out;
    }

    private static int ramp(float[][] stops, float t) {
        t = Math.clamp(t, 0, 1);
        int i = 1;
        while (i < stops.length - 1 && t > stops[i][0]) i++;
        float k = Math.clamp((t - stops[i - 1][0]) / (stops[i][0] - stops[i - 1][0]), 0, 1);
        int c = 0;
        for (int j = 1; j <= 3; j++) c = c << 8 | Math.round(stops[i - 1][j] + k * (stops[i][j] - stops[i - 1][j]));
        return c;
    }
}
