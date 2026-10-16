package com.realtectonics.sim;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

/** Usage: gradlew run --args="[width] [height] [steps] [plates] [seed] [erosion]" -> out/*.png */
public final class Prototype {
    public static void main(String[] args) throws IOException {
        int w = arg(args, 0, 1024), h = arg(args, 1, 512), steps = arg(args, 2, 300), plateCount = arg(args, 3, 14);
        long seed = args.length > 4 ? Long.parseLong(args[4]) : System.currentTimeMillis();
        int erosion = arg(args, 5, 50);
        if (w % 256 != 0) throw new IllegalArgumentException("width must be a multiple of 256");

        long t = System.nanoTime();
        Planet planet = new Planet(w, h, plateCount, seed);
        PlanetData.tectonics(planet, steps, done -> {});
        System.out.printf("seed %d: %d steps in %.1fs%n", seed, steps, (System.nanoTime() - t) / 1e9);
        t = System.nanoTime();
        Climate climate = new Climate(planet);
        Hydrology hydro = new Hydrology(planet, climate.precipitation, climate.anomaly, erosion, seed, done -> {});
        float peak = 0;
        for (float e : hydro.elevation) peak = Math.max(peak, e);
        System.out.printf("%d erosion passes in %.1fs, highest peak %.1f%n", erosion, (System.nanoTime() - t) / 1e9, peak);

        File out = new File("out");
        out.mkdirs();
        ImageIO.write(reliefImage(w, h, planet.height, Planet.SEA_LEVEL), "png", new File(out, "height.png"));
        ImageIO.write(terrainImage(hydro), "png", new File(out, "terrain.png"));
        ImageIO.write(rampImage(w, h, climate.temperature, -30, 0, 30, 0x2850c8, 0xf0f0f0, 0xc82814), "png", new File(out, "temperature.png"));
        ImageIO.write(rampImage(w, h, climate.precipitation, 0, 1000, 3000, 0xc8aa78, 0x50a03c, 0x14287a), "png", new File(out, "rainfall.png"));
        PlanetData data = PlanetData.from(planet, climate, hydro);
        System.out.printf("%d volcanoes (%d erupting)%n", data.volcanoes.stream().filter(v -> !v.gone()).count(), data.volcanoes.stream().filter(Volcano::active).count());
        Terrain terrain = new Terrain(data, seed);
        for (boolean geology : new boolean[]{false, true}) {
            BufferedImage map = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            map.setRGB(0, 0, w, h, terrain.mapColors(geology), 0, w);
            ImageIO.write(map, "png", new File(out, geology ? "geology.png" : "biomes.png"));
        }
        t = System.nanoTime();
        ImageIO.write(blockImage(terrain, 0, 0, 1024), "png", new File(out, "spawn.png"));
        for (boolean hotspot : new boolean[]{true, false}) // one erupting volcano of each kind, up close
            data.volcanoes.stream().filter(v -> v.hotspot() == hotspot && v.active()).findFirst().ifPresent(v -> {
                int[] at = terrain.blockOf((int) v.u() + (int) v.v() * w);
                try {
                    ImageIO.write(blockImage(terrain, at[0], at[1], hotspot ? 2048 : 1024), "png", new File(out, hotspot ? "volcano-hotspot.png" : "volcano-arc.png"));
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            });
        System.out.printf("block-level spawn preview in %.1fs%n", (System.nanoTime() - t) / 1e9);
        ImageIO.write(plateImage(planet), "png", new File(out, "plates.png"));
        System.out.println("wrote " + out.getAbsolutePath());
    }

    private static int arg(String[] args, int i, int fallback) {
        return args.length > i ? Integer.parseInt(args[i]) : fallback;
    }

    static BufferedImage reliefImage(int w, int h, float[] values, float seaLevel) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                float e = values[x + y * w] - seaLevel;
                int rgb = e < 0
                        ? mix(0x001450, 0x3c78c8, 1 + e / (Planet.SEA_LEVEL - Planet.OCEAN))
                        : e < 4 ? mix(0x3c823c, 0x8c6e46, e / 4) : mix(0x8c6e46, 0xffffff, (e - 4) / 4);
                // hillshade: light from the west
                float slope = values[Math.floorMod(x - 1, w) + y * w] - values[(x + 1) % w + y * w];
                img.setRGB(x, y, shade(rgb, e < 0 ? 1 : 1 + slope * 0.3f));
            }
        return img;
    }

    static BufferedImage terrainImage(Hydrology hy) {
        BufferedImage img = reliefImage(hy.w, hy.h, hy.elevation, 0);
        for (int g = 0; g < hy.elevation.length; g++) {
            if (hy.isLake(g)) img.setRGB(g % hy.w, g / hy.w, 0x3c78c8);
            else if (hy.isRiver(g)) img.setRGB(g % hy.w, g / hy.w, 0x1e46b4);
        }
        return img;
    }

    /** What the game will generate around block (cx, cz), one pixel per block, viewed from above. */
    static BufferedImage blockImage(Terrain terrain, int cx, int cz, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        int[] west = new int[size];
        for (int x = 0; x < size; x++)
            for (int z = 0; z < size; z++) {
                Terrain.Column c = terrain.column(cx + x - size / 2, cz + z - size / 2);
                int rgb = c.lava() != Terrain.Column.NO_WATER ? 0xff6a00
                        : c.water() != Terrain.Column.NO_WATER ? mix(0x1e46b4, 0x3c78c8, 1 - (c.water() - c.ground()) / 20f)
                        : c.volcanic() ? 0x3a3532 : c.biome().color;
                float shade = x == 0 ? 1 : 1 + (c.ground() - west[z]) * 0.08f;
                west[z] = c.ground();
                img.setRGB(x, z, shade(rgb, c.water() != Terrain.Column.NO_WATER ? 1 : Math.clamp(shade, 0.4f, 1.6f)));
            }
        return img;
    }

    /** Two-segment colour ramp: lo..mid blends c0 to c1, mid..hi blends c1 to c2. */
    static BufferedImage rampImage(int w, int h, float[] v, float lo, float mid, float hi, int c0, int c1, int c2) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int g = 0; g < v.length; g++)
            img.setRGB(g % w, g / w, v[g] < mid ? mix(c0, c1, (v[g] - lo) / (mid - lo)) : mix(c1, c2, (v[g] - mid) / (hi - mid)));
        return img;
    }

    static BufferedImage plateImage(Planet p) {
        int[] colors = new Random(0).ints(p.plates.length, 0x404040, 0xffffff).toArray();
        BufferedImage img = new BufferedImage(p.w, p.h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < p.h; y++)
            for (int x = 0; x < p.w; x++) {
                int o = p.owner[x + y * p.w];
                boolean edge = o != p.owner[(x + 1) % p.w + y * p.w] || (y + 1 < p.h && o != p.owner[x + (y + 1) * p.w]);
                img.setRGB(x, y, edge ? 0 : shade(colors[o], p.height[x + y * p.w] > Planet.SEA_LEVEL ? 1 : 0.6f));
            }
        return img;
    }

    private static int mix(int a, int b, float t) {
        t = Math.clamp(t, 0, 1);
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | g << 8 | bl;
    }

    private static int shade(int rgb, float f) {
        return f <= 1 ? mix(0, rgb, f) : mix(rgb, 0xffffff, f - 1);
    }
}
