exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""        if (landforms && Math.abs(shore) < 0.3f) natural = coast(x, z, u, v, shore, natural);""",
 """        if (landforms && Math.abs(shore) < 0.3f) natural = coast(x, z, u, v, shore, natural);
        if (gullies && natural > SEA_Y + 20) natural += gullies(x, z, u, v, natural - SEA_Y);"""),
("""    /** Canyon wall profile, 0 at the river to 1 at the rim""",
 """    /**
     * Gullies: every mountainside is veined with small valleys running straight downhill, the way runoff cuts them.
     * Stripes across the local slope, each grid point's own wave turned to the slope it sits on and blended with its
     * neighbours', so the grooves follow the mountain's shape and branch where slopes meet. Two sizes, in real blocks.
     */
    private float gullies(double x, double z, double u, double v, float above) {
        double e = 0.05; // cells: the slope of the planet's own surface, in blocks per real block
        double gu = (cubic(groundY, u + e, v) - cubic(groundY, u - e, v)) / (2 * e * blocksPerCell);
        double gv = (cubic(groundY, u, v + e) - cubic(groundY, u, v - e)) / (2 * e * blocksPerCell);
        double steep = Math.hypot(gu, gv);
        if (steep < 0.05) return 0;
        double px = -gv / steep, pz = gu / steep; // across the slope: the gullies run perpendicular to this
        float depth = (float) Math.min(GULLY_DEPTH, GULLY_DEPTH * steep * 1.5) * smoothstep((above - 20) / 40);
        return depth * (gullyWaves(x, z, px, pz, GULLY_WIDE, 60) + 0.35f * gullyWaves(x, z, px, pz, GULLY_NARROW, 61));
    }

    /** -1 in a gully's bottom to 1 on the rib between: waves across the slope from a jittered grid, blended. */
    private float gullyWaves(double x, double z, double px, double pz, int spacing, int salt) {
        double grid = spacing / (double) zoom; // unzoomed blocks per grid step, so gullies keep their real-block size
        int gx = (int) Math.floor(x / grid), gz = (int) Math.floor(z / grid), cells = (int) Math.round(span / grid);
        double sum = 0, weights = 0, sigma = 0.7 * grid, k = 2 * Math.PI / grid;
        for (int dz = -1; dz <= 2; dz++)
            for (int dx = -1; dx <= 2; dx++) {
                int key = Math.floorMod(gx + dx, cells) * 92821 + (gz + dz) * 68917 + salt * 31;
                double cx = (gx + dx + hash01(key)) * grid, cz = (gz + dz + hash01(key + 1)) * grid;
                double ox = wrapBlocks(x - cx), oz = z - cz, wgt = Math.exp(-(ox * ox + oz * oz) / (2 * sigma * sigma));
                sum += wgt * Math.cos((ox * px + oz * pz) * k + 2 * Math.PI * hash01(key + 2));
                weights += wgt;
            }
        float wave = (float) (sum / weights);
        return wave > 0 ? (float) Math.sqrt(wave) : -wave * wave * 0 - (float) Math.pow(-wave, 1.5); // sharp ribs, rounded gully floors
    }

    /** Patchiness for snow and rock, -1..1, at real block (x, z): coherent over a few dozen blocks. */
    public float patch(int realX, int realZ) {
        double x = realX / (double) zoom, z = realZ / (double) zoom;
        return 0.7f * fine(x, z, 32, 62) + 0.3f * fine(x, z, 8, 63);
    }

    /** Canyon wall profile, 0 at the river to 1 at the rim"""),
("""    private final boolean forestVariety;""", """    private final boolean forestVariety;
    private final boolean gullies;        // downhill gullies on mountainsides, format 14"""),
("""        forestVariety = data.format >= 13;""", """        forestVariety = data.format >= 13;
        gullies = data.format >= 14;"""),
("""    static final float CHERRY_PATCH = 0.45f,""", """    static final float GULLY_DEPTH = 10;             // blocks: deepest gully cut, on the steepest high slopes
    static final int GULLY_WIDE = 48, GULLY_NARROW = 16; // real blocks between gullies, two sizes
    static final float CHERRY_PATCH = 0.45f,"""),
])
edit('src/main/java/com/realtectonics/sim/PlanetData.java', [
("""    private static final int FORMAT = 13;""", """    private static final int FORMAT = 14;"""),
("""13 forest variety (no new data)""", """13 forest variety, 14 gullies (no new data)"""),
])
G = 'src/main/java/com/realtectonics/mod/TectonicChunkGenerator.java'
edit(G, [
("""                Surface s = glacier ? GRAVEL : surface(c, slope, wet); // ground moraine under the ice""",
 """                // Which way the ground faces: rising toward the south means a north face, shaded, where snow lasts.
                boolean shaded = landTop(cols[i + 18]) > landTop(cols[i - 18]);
                Surface s = glacier ? GRAVEL : surface(c, slope, wet);
                if (!glacier && !wet && alpine(c.biome()) && !snowStays(slope, shaded, RealTectonics.terrain().patch(x0 + lx, z0 + lz)))
                    s = ROCK; // steep faces, ridges and wind-scoured patches stand bare above the snow"""),
("""    private static boolean badlands(Biome b) {""",
 """    /** Above the tree line, where snow lies only where it can. */
    private static boolean alpine(Biome b) {
        return b == Biome.SNOWY_PEAKS || b == Biome.JAGGED_PEAKS || b == Biome.SNOWY_SLOPES || b == Biome.ICE_CAP;
    }

    /** Snow settles on gentle ground and lasts on shaded north faces; patch (-1..1) makes the edge ragged. */
    static boolean snowStays(int slope, boolean shaded, float patch) {
        return patch > -SNOW_SCOUR && slope <= SNOW_SLOPE + (shaded ? SNOW_SHADE : 0) + SNOW_PATCH * patch;
    }

    private static boolean badlands(Biome b) {"""),
("""    static final int CLIFF_STEP = 4;""", """    static final int CLIFF_STEP = 4;
    static final float SNOW_SLOPE = 1.5f, SNOW_SHADE = 1.5f, SNOW_PATCH = 1.2f, SNOW_SCOUR = 0.75f; // see snowStays"""),
])
print("ok")
