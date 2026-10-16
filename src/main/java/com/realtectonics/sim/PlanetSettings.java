package com.realtectonics.sim;

/**
 * What the player picks when creating the world.
 * zoom: world scale, how many times wider than BLOCKS_PER_CELL each cell is in blocks (1, 2 or 4).
 * volcanoes: how many volcanoes, 1 normal, 0 none, 2 twice as many.
 * steps: how long the plates move: a young planet has many small continents, an old one fewer, bigger, more worn ones.
 * riverWidth: rivers and creeks this many times wider than the simulation's own widths.
 * earth: today's Earth (real continents and plates, Planet.earth) instead of a simulated planet; plates, ocean,
 * volcanoes and steps then do nothing.
 */
public record PlanetSettings(int width, int plates, float oceanFraction, float mountainScale, int zoom, float volcanoes,
                             int steps, float riverWidth, boolean earth) {
    public static final PlanetSettings DEFAULT = new PlanetSettings(1024, 14, 0.7f, 1, 1, 1, 300, 2, false);

    public PlanetSettings(int width, int plates, float oceanFraction, float mountainScale, int zoom, float volcanoes, int steps, float riverWidth) {
        this(width, plates, oceanFraction, mountainScale, zoom, volcanoes, steps, riverWidth, false);
    }
    public static final int[] WIDTHS = {512, 1024, 2048, 4096}, ZOOMS = {1, 2, 4}; // Huge: ~100 s and ~3 GB to simulate

    public int height() {
        return width / 2;
    }

    /** Mountain scale, zoom and river width only change how the planet becomes blocks, so they never need a new simulation. */
    public boolean sameSimulation(PlanetSettings o) {
        return width == o.width && earth == o.earth && (earth || plates == o.plates && oceanFraction == o.oceanFraction && volcanoes == o.volcanoes && steps == o.steps);
    }

    public PlanetSettings withWidth(int v) {
        return new PlanetSettings(v, plates, oceanFraction, mountainScale, zoom, volcanoes, steps, riverWidth, earth);
    }

    public PlanetSettings withPlates(int v) {
        return new PlanetSettings(width, v, oceanFraction, mountainScale, zoom, volcanoes, steps, riverWidth, earth);
    }

    public PlanetSettings withOceanFraction(float v) {
        return new PlanetSettings(width, plates, v, mountainScale, zoom, volcanoes, steps, riverWidth, earth);
    }

    public PlanetSettings withMountainScale(float v) {
        return new PlanetSettings(width, plates, oceanFraction, v, zoom, volcanoes, steps, riverWidth, earth);
    }

    public PlanetSettings withZoom(int v) {
        return new PlanetSettings(width, plates, oceanFraction, mountainScale, v, volcanoes, steps, riverWidth, earth);
    }

    public PlanetSettings withVolcanoes(float v) {
        return new PlanetSettings(width, plates, oceanFraction, mountainScale, zoom, v, steps, riverWidth, earth);
    }

    public PlanetSettings withSteps(int v) {
        return new PlanetSettings(width, plates, oceanFraction, mountainScale, zoom, volcanoes, v, riverWidth, earth);
    }

    public PlanetSettings withEarth(boolean v) {
        return new PlanetSettings(width, plates, oceanFraction, mountainScale, zoom, volcanoes, steps, riverWidth, v);
    }

    public PlanetSettings withRiverWidth(float v) {
        return new PlanetSettings(width, plates, oceanFraction, mountainScale, zoom, volcanoes, steps, v, earth);
    }
}
