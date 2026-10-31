package com.realtectonics.sim;

/**
 * What the player picks when creating the world.
 * zoom: world scale, how many times wider than BLOCKS_PER_CELL each cell is in blocks (1, 2 or 4).
 * volcanoes: how many volcanoes, 1 normal, 0 none, 2 twice as many.
 * steps: how long the plates move: a young planet has many small continents, an old one fewer, bigger, more worn ones.
 * riverWidth: rivers and creeks this many times wider than the simulation's own widths.
 * earth: today's Earth (real continents and plates, Planet.earth) instead of a simulated planet; plates, ocean,
 * volcanoes and steps then do nothing.
 * mediumContinents: on a planet bigger than Medium, continents, plates and climate stay the size they are on Medium and
 * there are more of them (plates counts them per Medium planet's worth of surface), instead of the same few grown big.
 * cliffHeight: blocks from one tableland bench to the next, so the height of each of its cliffs (format 77 worlds).
 * start: on Earth, the region spawn (block 0, 0) lies in, an index into START_NAMES; 0 picks anywhere by seed, as worlds
 * from before the setting did.
 */
public record PlanetSettings(int width, int plates, float oceanFraction, float mountainScale, int zoom, float volcanoes,
                             int steps, float riverWidth, boolean earth, boolean mediumContinents, float cliffHeight, int start) {
    public static final float CLIFF_HEIGHT = 24; // Terrain.TABLE_STEP
    public static final String[] START_NAMES = {"Anywhere", "Europe", "North America", "South America", "Africa", "Asia", "Australia"};
    static final double[][] START_PLACES = {null, {51.5, 0}, {39, -98}, {-15, -55}, {5, 20}, {35, 105}, {-30, 145}}; // latitude, longitude: Greenwich, so X runs with real longitude; Kansas; Mato Grosso; the Congo; China; New South Wales
    static final int EUROPE = 1; // new worlds start here: the old default dropped players on any continent
    public static final PlanetSettings DEFAULT = new PlanetSettings(1024, 14, 0.7f, 1, 1, 1, 300, 2, false);
    static final int MEDIUM = 1024; // cells round a Medium planet
    public static final int MAX_PLATES = 256; // plates are saved as one byte per cell

    public PlanetSettings(int width, int plates, float oceanFraction, float mountainScale, int zoom, float volcanoes, int steps, float riverWidth, boolean earth) {
        this(width, plates, oceanFraction, mountainScale, zoom, volcanoes, steps, riverWidth, earth, false, CLIFF_HEIGHT, EUROPE);
    }

    public PlanetSettings(int width, int plates, float oceanFraction, float mountainScale, int zoom, float volcanoes, int steps, float riverWidth) {
        this(width, plates, oceanFraction, mountainScale, zoom, volcanoes, steps, riverWidth, false);
    }
    public static final int[] WIDTHS = {512, 1024, 2048, 4096}, ZOOMS = {1, 2, 4}; // Huge: ~125 s to simulate, ~170 s with Medium-sized continents (224 plates)

    public int height() {
        return width / 2;
    }

    /** Cells round the planet its continents, plates and climate are sized for (Planet.scaleWidth). */
    public int scaleWidth() {
        return mediumContinents && !earth ? Math.min(width, MEDIUM) : width;
    }

    /** Plates on the whole planet: the slider's count for each Medium planet's worth of surface. */
    public int plateCount() {
        int k = width / scaleWidth();
        return Math.min(MAX_PLATES, plates * k * k);
    }

    /** Mountain scale, zoom, river width and cliff height only change how the planet becomes blocks, so they never need a new simulation. */
    public boolean sameSimulation(PlanetSettings o) {
        return width == o.width && earth == o.earth && (earth || plates == o.plates && oceanFraction == o.oceanFraction && volcanoes == o.volcanoes && steps == o.steps
                && scaleWidth() == o.scaleWidth());
    }

    public PlanetSettings withWidth(int v) {
        return new PlanetSettings(v, plates, oceanFraction, mountainScale, zoom, volcanoes, steps, riverWidth, earth, mediumContinents, cliffHeight, start);
    }

    public PlanetSettings withPlates(int v) {
        return new PlanetSettings(width, v, oceanFraction, mountainScale, zoom, volcanoes, steps, riverWidth, earth, mediumContinents, cliffHeight, start);
    }

    public PlanetSettings withOceanFraction(float v) {
        return new PlanetSettings(width, plates, v, mountainScale, zoom, volcanoes, steps, riverWidth, earth, mediumContinents, cliffHeight, start);
    }

    public PlanetSettings withMountainScale(float v) {
        return new PlanetSettings(width, plates, oceanFraction, v, zoom, volcanoes, steps, riverWidth, earth, mediumContinents, cliffHeight, start);
    }

    public PlanetSettings withZoom(int v) {
        return new PlanetSettings(width, plates, oceanFraction, mountainScale, v, volcanoes, steps, riverWidth, earth, mediumContinents, cliffHeight, start);
    }

    public PlanetSettings withVolcanoes(float v) {
        return new PlanetSettings(width, plates, oceanFraction, mountainScale, zoom, v, steps, riverWidth, earth, mediumContinents, cliffHeight, start);
    }

    public PlanetSettings withSteps(int v) {
        return new PlanetSettings(width, plates, oceanFraction, mountainScale, zoom, volcanoes, v, riverWidth, earth, mediumContinents, cliffHeight, start);
    }

    public PlanetSettings withEarth(boolean v) {
        return new PlanetSettings(width, plates, oceanFraction, mountainScale, zoom, volcanoes, steps, riverWidth, v, mediumContinents, cliffHeight, start);
    }

    public PlanetSettings withMediumContinents(boolean v) {
        return new PlanetSettings(width, plates, oceanFraction, mountainScale, zoom, volcanoes, steps, riverWidth, earth, v, cliffHeight, start);
    }

    public PlanetSettings withCliffHeight(float v) {
        return new PlanetSettings(width, plates, oceanFraction, mountainScale, zoom, volcanoes, steps, riverWidth, earth, mediumContinents, v, start);
    }

    /** Where on the planet spawn goes, as latitude and longitude; null: anywhere (and always on a simulated planet). */
    public double[] startPlace() {
        return earth ? START_PLACES[start] : null;
    }

    public PlanetSettings withStart(int v) {
        return new PlanetSettings(width, plates, oceanFraction, mountainScale, zoom, volcanoes, steps, riverWidth, earth, mediumContinents, cliffHeight, v);
    }

    public PlanetSettings withRiverWidth(float v) {
        return new PlanetSettings(width, plates, oceanFraction, mountainScale, zoom, volcanoes, steps, v, earth, mediumContinents, cliffHeight, start);
    }
}
