exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
# fields
("""    public final PlanetData data;
    public final int circumference; // blocks once around the planet""",
 """    public final PlanetData data;
    public final int circumference; // blocks once around the planet
    public final int zoom;          // world scale: each cell is BLOCKS_PER_CELL * zoom blocks wide
    public final int blocksPerCell;
    private final int span;         // circumference before zooming: the frame cells, warp and volcanoes live in"""),
# constructors
("""    public Terrain(PlanetData data, long seed) {
        this(data, seed, 1);
    }

    /** mountainScale 1 is the default; 2 makes mountains twice as tall in blocks (and twice as steep). */
    public Terrain(PlanetData data, long seed, float mountainScale) {
        this.data = data;
        this.seed = seed;""",
 """    public Terrain(PlanetData data, long seed) {
        this(data, seed, 1, 1);
    }

    /**
     * mountainScale 1 is the default; 2 makes mountains twice as tall in blocks (and twice as steep). zoom (a power of
     * two) stretches the planet sideways: continents, coasts and valleys get wider, while rivers, small bumps and rock
     * layers keep their size in blocks.
     */
    public Terrain(PlanetData data, long seed, float mountainScale, int zoom) {
        this.data = data;
        this.seed = seed;
        this.zoom = zoom;"""),
("""        circumference = w * BLOCKS_PER_CELL;""",
 """        span = w * BLOCKS_PER_CELL;
        blocksPerCell = BLOCKS_PER_CELL * zoom;
        circumference = span * zoom;"""),
# deltas: radius in real blocks
("""            float radius = Math.min(224, 64 + 2 * (float) Math.sqrt(data.flow[c]));""",
 """            float radius = Math.min(224, 64 + 2 * (float) Math.sqrt(data.flow[c])) * zoom; // reaches as many cells out to sea"""),
("""            int reach = (int) Math.ceil(radius / BLOCKS_PER_CELL) + 2;
            for (int dy = -reach; dy <= reach; dy++)
                for (int dx = -reach; dx <= reach; dx++)
                    deltasByCell""",
 """            int reach = (int) Math.ceil(radius / blocksPerCell) + 2;
            for (int dy = -reach; dy <= reach; dy++)
                for (int dx = -reach; dx <= reach; dx++)
                    deltasByCell"""),
("""    private DeltaSpot deltaAt(int x, int z, double u, double v) {""",
 """    private DeltaSpot deltaAt(double x, double z, double u, double v) {"""),
("""            double pu = wrapCells(u - d.u) * BLOCKS_PER_CELL, pv = (v - d.v) * BLOCKS_PER_CELL;""",
 """            double pu = wrapCells(u - d.u) * blocksPerCell, pv = (v - d.v) * blocksPerCell;"""),
("""    private List<Cone> conesNear(int x, int z) {""",
 """    private List<Cone> conesNear(double x, double z) {"""),
("""        double m = (dx % circumference + circumference + circumference / 2.0) % circumference;
        return m - circumference / 2.0;""",
 """        double m = (dx % span + span + span / 2.0) % span;
        return m - span / 2.0;"""),
# column entry
("""    public Column column(int x, int z) {
        double u = warpU(x, z), v = warpV(x, z);""",
 """    /** The column at real block (x, z). */
    public Column column(int x, int z) {
        return column(x / (double) zoom, z / (double) zoom);
    }

    /** x, z: unzoomed blocks. Large features are laid out in them; fine ones go through fine() and stay in real blocks. */
    private Column column(double x, double z) {
        double u = warpU(x, z), v = warpV(x, z);"""),
("""        int iu = (int) Math.round(u), iv = (int) Math.round(v);
        // Lowland rivers swing in meanders and spread wider; the query point wanders instead of the channel.
        float low = lowlandRivers ? Math.clamp(1 - (cubic(groundY, u, v) - SEA_Y) / LOWLAND_BLOCKS, 0, 1) : 0;
        double mu = low > 0 ? low * MEANDER * (noise(x, z, 128, 11) + 0.5 * noise(x, z, 64, 16)) : 0;
        double mv = low > 0 ? low * MEANDER * (noise(x, z, 128, 12) + 0.5 * noise(x, z, 64, 17)) : 0;
        River river = nearestRiver(u + mu, v + mv, (byte) 0, Hydrology.RIVER_AREA)
                .widened(1 + 0.5f * low);
        River valley""",
 """        int iu = (int) Math.round(u), iv = (int) Math.round(v);
        River river = riverAt(x, z, u, v);
        River valley"""),
("""                ground = Math.max(ground, noise(x, z, 32, 15) > 0.45f ? SEA_Y - 2 : SEA_Y); // pools among the reeds""",
 """                ground = Math.max(ground, fine(x, z, 32, 15) > 0.45f ? SEA_Y - 2 : SEA_Y); // pools among the reeds"""),
("""        int fold = Math.round(noise(x, z, 512, 5) * (12 + 0.1f * Math.max(0, ground - SEA_Y)) + noise(x, z, 64, 6) * 4);""",
 """        int fold = Math.round(fine(x, z, 512, 5) * (12 + 0.1f * Math.max(0, ground - SEA_Y)) + fine(x, z, 64, 6) * 4);"""),
# river helper
("""    /** The river channel nearest a warped position: blocks outside its edge""",
 """    /** 0 in the hills, rising to 1 on the coastal plain. */
    private float lowland(double u, double v) {
        return lowlandRivers ? Math.clamp(1 - (cubic(groundY, u, v) - SEA_Y) / LOWLAND_BLOCKS, 0, 1) : 0;
    }

    /** The river channel the game carves near (x, z): lowland rivers swing in meanders and spread wider. */
    private River riverAt(double x, double z, double u, double v) {
        float low = lowland(u, v); // the query point wanders instead of the channel; meanders are sized in real blocks
        double mu = low > 0 ? low * MEANDER / zoom * (fine(x, z, 128, 11) + 0.5 * fine(x, z, 64, 16)) : 0;
        double mv = low > 0 ? low * MEANDER / zoom * (fine(x, z, 128, 12) + 0.5 * fine(x, z, 64, 17)) : 0;
        return nearestRiver(u + mu, v + mv, (byte) 0, Hydrology.RIVER_AREA).widened(1 + 0.5f * low);
    }

    /** The river channel nearest a warped position: blocks outside its edge"""),
("""                float dist = (float) Math.hypot(u - au - t * su, v - cy - t * sv) * BLOCKS_PER_CELL;""",
 """                float dist = (float) Math.hypot(u - au - t * su, v - cy - t * sv) * blocksPerCell; // real blocks: rivers keep their width"""),
# map
("""                int g = cx + cy * w, x = wrap(cx - originX) * BLOCKS_PER_CELL, z = (cy - originY) * BLOCKS_PER_CELL;""",
 """                int g = cx + cy * w, x = wrap(cx - originX) * blocksPerCell, z = (cy - originY) * blocksPerCell;"""),
("""                if (!sea && !river && nearestRiver(warpU(x, z), warpV(x, z), (byte) 0, Hydrology.RIVER_AREA).edge < 1.5f * BLOCKS_PER_CELL)
                    search:
                    for (int oz = -14; oz <= 14; oz += 7)
                        for (int ox = -14; ox <= 14; ox += 7) {
                            Column s = column(x + ox, z + oz);
                            if (s.water() != Column.NO_WATER) { // a fjord counts too: it is narrower than a pixel
                                river = true;
                                break search;
                            }
                        }""",
 """                double sx = x / (double) zoom, sz = z / (double) zoom;
                int reach = blocksPerCell / 2 - 2, step = Math.max(7, 2 * reach / 8); // a 5x5 grid at zoom 1, 9x9 above
                if (!sea && !river && nearestRiver(warpU(sx, sz), warpV(sx, sz), (byte) 0, Hydrology.RIVER_AREA).edge < 1.5f * blocksPerCell)
                    search:
                    for (int oz = -reach; oz <= reach; oz += step)
                        for (int ox = -reach; ox <= reach; ox += step) {
                            double px = (x + ox) / (double) zoom, pz = (z + oz) / (double) zoom, pu = warpU(px, pz), pv = warpV(px, pz);
                            if (riverAt(px, pz, pu, pv).edge > 2 && cubic(lakes, pu, pv) < 0.4f) continue; // cheap: only a likely channel gets a full column
                            Column s = column(x + ox, z + oz);
                            if (s.water() != Column.NO_WATER) { // a fjord counts too: it is narrower than a pixel
                                river = true;
                                break search;
                            }
                        }"""),
# biomeAt
("""    public Biome biomeAt(int x, int z) {
        double u = warpU(x, z), v = warpV(x, z);""",
 """    public Biome biomeAt(int realX, int realZ) {
        double x = realX / (double) zoom, z = realZ / (double) zoom, u = warpU(x, z), v = warpV(x, z);"""),
# public geo api
("""        return Climate.latitudeAt(originY + 0.5 + z / BLOCKS_PER_CELL, h);""",
 """        return Climate.latitudeAt(originY + 0.5 + z / blocksPerCell, h);"""),
("""        double u = (originX + 0.5 + x / BLOCKS_PER_CELL) % w;
        return new double[]{u < 0 ? u + w : u, originY + 0.5 + z / BLOCKS_PER_CELL};""",
 """        double u = (originX + 0.5 + x / blocksPerCell) % w;
        return new double[]{u < 0 ? u + w : u, originY + 0.5 + z / blocksPerCell};"""),
("""        return (int) Math.round((-0.5 - originY) * BLOCKS_PER_CELL);""",
 """        return (int) Math.round((-0.5 - originY) * blocksPerCell);"""),
("""        return (int) Math.round((h - 0.5 - originY) * BLOCKS_PER_CELL);""",
 """        return (int) Math.round((h - 0.5 - originY) * blocksPerCell);"""),
("""        return new int[]{wrap(cell % w - originX) * BLOCKS_PER_CELL, (cell / w - originY) * BLOCKS_PER_CELL};""",
 """        return new int[]{wrap(cell % w - originX) * blocksPerCell, (cell / w - originY) * blocksPerCell};"""),
("""    private double warpU(int x, int z) {""", """    private double warpU(double x, double z) {"""),
("""    private double warpV(int x, int z) {""", """    private double warpV(double x, double z) {"""),
# detail + noise
("""    private float detail(int x, int z) {
        float sum = 0, amp = 1, total = 0;
        for (int spacing : DETAIL_SPACING) {
            sum += amp * noise(x, z, spacing, spacing);""",
 """    private float detail(double x, double z) {
        float sum = 0, amp = 1, total = 0;
        for (int spacing : DETAIL_SPACING) {
            sum += amp * fine(x, z, spacing, spacing);"""),
("""    /** 2D gradient noise in blocks; the lattice repeats every circumference blocks in x, so spacing must divide it (powers of two). */
    private float noise(double x, double z, int spacing, int salt) {
        double gx = x / spacing, gz = z / spacing;
        int period = circumference / spacing,""",
 """    /** Noise sized in real blocks, sampled at unzoomed (x, z): the same lattice at every zoom. */
    private float fine(double x, double z, int spacing, int salt) {
        return noise(x, z, spacing / zoom, salt);
    }

    /** 2D gradient noise in unzoomed blocks; the lattice repeats every span blocks in x, so spacing must divide it (powers of two). */
    private float noise(double x, double z, int spacing, int salt) {
        double gx = x / spacing, gz = z / spacing;
        int period = span / spacing,"""),
])
P = 'src/main/java/com/realtectonics/sim/PlanetSettings.java'
s = open(P, encoding='utf-8').read()
s = s.replace("public record PlanetSettings(int width, int plates, float oceanFraction, float mountainScale) {\n    public static final PlanetSettings DEFAULT = new PlanetSettings(1024, 14, 0.7f, 1);\n    public static final int[] WIDTHS = {512, 1024, 2048};",
"""/** zoom: world scale, how many times wider than BLOCKS_PER_CELL each cell is in blocks (1, 2 or 4). */
public record PlanetSettings(int width, int plates, float oceanFraction, float mountainScale, int zoom) {
    public static final PlanetSettings DEFAULT = new PlanetSettings(1024, 14, 0.7f, 1, 1);
    public static final int[] WIDTHS = {512, 1024, 2048}, ZOOMS = {1, 2, 4};""")
s = s.replace("/** Mountain scale only changes how metres become blocks, so it never needs a new simulation. */", "/** Mountain scale and zoom only change how the planet becomes blocks, so they never need a new simulation. */")
for a, b in [("(v, plates, oceanFraction, mountainScale)", "(v, plates, oceanFraction, mountainScale, zoom)"),
             ("(width, v, oceanFraction, mountainScale)", "(width, v, oceanFraction, mountainScale, zoom)"),
             ("(width, plates, v, mountainScale)", "(width, plates, v, mountainScale, zoom)"),
             ("(width, plates, oceanFraction, v)", "(width, plates, oceanFraction, v, zoom)")]:
    assert s.count(a) == 1, a
    s = s.replace(a, b)
s = s.rstrip()[:-1] + """
    public PlanetSettings withZoom(int v) {
        return new PlanetSettings(width, plates, oceanFraction, mountainScale, v);
    }
}
"""
open(P, 'w', encoding='utf-8', newline='').write(s)
G = 'src/main/java/com/realtectonics/mod/TectonicChunkGenerator.java'
edit(G, [
("""            Codec.floatRange(0.25f, 2f).fieldOf("mountain_scale").forGetter(PlanetSettings::mountainScale)
    ).apply(i, PlanetSettings::new));""",
 """            Codec.floatRange(0.25f, 2f).fieldOf("mountain_scale").forGetter(PlanetSettings::mountainScale),
            Codec.INT.validate(z -> z >= 1 && z <= 8 && Integer.bitCount(z) == 1 ? DataResult.success(z) : DataResult.error(() -> "World scale must be 1, 2, 4 or 8: " + z))
                    .optionalFieldOf("zoom", 1).forGetter(PlanetSettings::zoom) // worlds from before world scale stay at 1
    ).apply(i, PlanetSettings::new));"""),
])
edit('src/main/java/com/realtectonics/mod/RealTectonics.java', [
("""            terrain = new Terrain(data, seed, settings.mountainScale());""", """            terrain = new Terrain(data, seed, settings.mountainScale(), settings.zoom());"""),
])
edit('src/main/java/com/realtectonics/mod/client/PlanetScreen.java', [
("""            Terrain terrain = new Terrain(d, s, ps.mountainScale());""", """            Terrain terrain = new Terrain(d, s, ps.mountainScale(), ps.zoom());"""),
("""        addRenderableWidget(new Slider(cx + 5, y, "Mountains", 0.5, 2, settings.mountainScale(), true, false, v -> settings = settings.withMountainScale((float) v)));
        y += 24;""",
 """        addRenderableWidget(new Slider(cx + 5, y, "Mountains", 0.5, 2, settings.mountainScale(), true, false, v -> settings = settings.withMountainScale((float) v)));
        y += 24;
        addRenderableWidget(CycleButton.<Integer>builder(z -> Component.literal("x" + z + (z == 1 ? " (compact)" : "")))
                .withValues(Arrays.stream(PlanetSettings.ZOOMS).boxed().toList())
                .withInitialValue(settings.zoom())
                .create(cx - 155, y, 310, 20, Component.literal("World scale"), (button, z) -> settings = settings.withZoom(z)));
        y += 24;"""),
("""        return Math.max(100, Math.min(width - 40, (height - 24 - 8 - 3 * 24 - 8) * 2));""",
 """        return Math.max(100, Math.min(width - 40, (height - 24 - 8 - 4 * 24 - 8) * 2));"""),
])
edit('src/main/java/com/realtectonics/mod/client/MapScreen.java', [
("""    private record View(int x0, int y0, int mapW, int mapH, int left, int top, float scale, int w, double playerU, double playerV) {""",
 """    private record View(int x0, int y0, int mapW, int mapH, int left, int top, float scale, int w, double playerU, double playerV, int blocksPerCell) {"""),
("""            return new int[]{(int) Math.floor(player.getX() + du * Terrain.BLOCKS_PER_CELL),
                    (int) Math.floor(player.getZ() + (v - playerV) * Terrain.BLOCKS_PER_CELL)};""",
 """            return new int[]{(int) Math.floor(player.getX() + du * blocksPerCell),
                    (int) Math.floor(player.getZ() + (v - playerV) * blocksPerCell)};"""),
("""        view = new View(x0, y0, mapW, mapH, left, top, scale, w, p[0], p[1]);""",
 """        view = new View(x0, y0, mapW, mapH, left, top, scale, w, p[0], p[1], t.blocksPerCell);"""),
])
for f in ['src/test/java/com/realtectonics/sim/PlanetDataTest.java']:
    s = open(f, encoding='utf-8').read()
    s = s.replace("0.85f, 1)", "0.85f, 1, 1)").replace("0.5f, 1)", "0.5f, 1, 1)").replace("0.7f, 1)", "0.7f, 1, 1)")
    open(f, 'w', encoding='utf-8', newline='').write(s)
print("ok")
