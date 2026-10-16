exec(open('tools/edit_glacier.py').read().split("C = ")[0])
B = 'src/main/java/com/realtectonics/sim/Biome.java'
edit(B, [
("""    SNOWY_PEAKS(0xe6eef4), ROCKY_PEAKS(0x8c8c8c), SWAMP(0x4c6e3c), MANGROVE(0x3c5a32); // the last two only on deltas""",
 """    SNOWY_PEAKS(0xe6eef4), ROCKY_PEAKS(0x8c8c8c), SWAMP(0x4c6e3c), MANGROVE(0x3c5a32), // the last two only on deltas
    // Planets from format 12 on: rivers, shores, ocean temperatures and mountain zones, picked per block in Terrain.
    RIVER(0x3c64d2), FROZEN_RIVER(0xa0b4dc), BEACH(0xf0e2a8), SNOWY_BEACH(0xf2f2ea), STONY_SHORE(0x8a8a8a),
    COLD_OCEAN(0x2a4a9a), LUKEWARM_OCEAN(0x2060b0), DEEP_COLD_OCEAN(0x14306e), DEEP_LUKEWARM_OCEAN(0x123c7a), DEEP_FROZEN_OCEAN(0x8ca0c8),
    MEADOW(0x8cc85a), GROVE(0x5a8c78), SNOWY_SLOPES(0xdce6ec), JAGGED_PEAKS(0xf0f4f8), WINDSWEPT_HILLS(0x7d8c6e);"""),
])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
# river flag
("""            if (river.edge <= 0 && braided) { // shallow channels threading between gravel bars
                boolean bar = fine(x, z, 16, 43) + 0.5f * fine(x, z, 64, 44) > BRAID_BARS;
                ground = Math.min(ground, bar ? ry : ry - 1);
                water = ry;
                cover = Cover.GRAVEL;
            } else if (river.edge <= 0) {
                ground = Math.min(ground, ry - 1 - (organic ? bedDepth(x, z, river.edge + river.half, river.half) : (int) Math.min(5, river.half / 4)));
                water = ry;""",
 """            if (river.edge <= 0 && braided) { // shallow channels threading between gravel bars
                boolean bar = fine(x, z, 16, 43) + 0.5f * fine(x, z, 64, 44) > BRAID_BARS;
                ground = Math.min(ground, bar ? ry : ry - 1);
                water = ry;
                cover = Cover.GRAVEL;
                inRiver = true;
            } else if (river.edge <= 0) {
                ground = Math.min(ground, ry - 1 - (organic ? bedDepth(x, z, river.edge + river.half, river.half) : (int) Math.min(5, river.half / 4)));
                water = ry;
                inRiver = true;"""),
("""        // Braided rivers: below the old glaciers""", """        boolean inRiver = false;
        // Braided rivers: below the old glaciers"""),
# final biome
("""        return new Column(ground, water, lava, ice, marsh && ground >= SEA_Y - 2 ? marsh(x, z, u, v) : biome(x, z, u, v, Math.max(organic && !(landforms && smooth < SEA_Y && ground >= SEA_Y) ? Math.min(ground, smooth) : ground, ice)), fold, volcanic, rock, cover);""",
 """        Biome biome = marsh && ground >= SEA_Y - 2 ? marsh(x, z, u, v) : biome(x, z, u, v, Math.max(organic && !(landforms && smooth < SEA_Y && ground >= SEA_Y) ? Math.min(ground, smooth) : ground, ice));
        if (richBiomes && water != Column.NO_WATER && water >= ground) {
            if (inRiver && water >= SEA_Y) biome = temperature(u, v, water) < 0 ? Biome.FROZEN_RIVER : Biome.RIVER;
        } else if (richBiomes && !marsh && ice == Column.NO_WATER && ground <= SEA_Y + 2 && shore > -0.05f && shore < SHORE_BAND) {
            biome = landforms && cubic(cliffy, u, v) > 0.4f ? Biome.STONY_SHORE : temperature(u, v, ground) < 0 ? Biome.SNOWY_BEACH : Biome.BEACH;
        }
        return new Column(ground, water, lava, ice, biome, fold, volcanic, rock, cover);"""),
# biome(): ocean temperatures and mountain zones
("""        return Biome.classify(toMetres(y), t, rain, iceCapBelow);
    }""",
 """        Biome b = Biome.classify(toMetres(y), t, rain, iceCapBelow);
        return richBiomes ? zone(b, toMetres(y), t, rain, x, z) : b;
    }

    /**
     * Seas by their surface temperature (currents included) and depth; mountainsides in zones by height and
     * climate: snowy slopes under the peaks, spruce groves in the cold wet, alpine meadows, windswept dry uplands.
     */
    private Biome zone(Biome b, float metres, float t, float rain, double x, double z) {
        if (metres < 0) {
            boolean deep = metres < DEEP_SEA;
            return t < -2 ? deep ? Biome.DEEP_FROZEN_OCEAN : Biome.FROZEN_OCEAN : t < 10 ? deep ? Biome.DEEP_COLD_OCEAN : Biome.COLD_OCEAN
                    : t < 20 ? deep ? Biome.DEEP_OCEAN : Biome.OCEAN : t < 25 ? deep ? Biome.DEEP_LUKEWARM_OCEAN : Biome.LUKEWARM_OCEAN : Biome.WARM_OCEAN;
        }
        if (metres < MOUNTAIN_METRES) return b;
        if (b == Biome.SNOWY_PEAKS) return metres > JAGGED_METRES && t < -6 ? Biome.JAGGED_PEAKS : b;
        if (b == Biome.ROCKY_PEAKS) return b;
        if (t < -2) return Biome.SNOWY_SLOPES;
        if (t < 4) return rain >= 400 ? Biome.GROVE : Biome.WINDSWEPT_HILLS;
        if (t < 12 && metres > MEADOW_METRES && rain >= 350 && noise(x, z, 256, 54) > -0.3f) return Biome.MEADOW;
        if (rain < 600) return Biome.WINDSWEPT_HILLS;
        return b;
    }"""),
# biomeAt: the column itself on rich planets
("""    public Biome biomeAt(int realX, int realZ) {
        double x = realX / (double) zoom, z = realZ / (double) zoom, u = warpU(x, z), v = warpV(x, z);""",
 """    public Biome biomeAt(int realX, int realZ) {
        if (richBiomes) return column(realX, realZ).biome(); // rivers and shores need the whole column
        double x = realX / (double) zoom, z = realZ / (double) zoom, u = warpU(x, z), v = warpV(x, z);"""),
("""    private final boolean landforms;""", """    private final boolean landforms;
    private final boolean richBiomes;     // rivers, shores, ocean temperatures and mountain zones of format 12"""),
("""        landforms = data.format >= 11;""", """        landforms = data.format >= 11;
        richBiomes = data.format >= 12;"""),
("""    static final int CANYON_BENCHES = 4;""", """    static final int CANYON_BENCHES = 4;
    static final float SHORE_BAND = 0.12f;          // land-mask units inland that count as the shore
    static final float DEEP_SEA = -1000;            // metres: deeper seas get the deep ocean biomes
    static final float MOUNTAIN_METRES = 1000, MEADOW_METRES = 1500, JAGGED_METRES = 3000;"""),
])
edit('src/main/java/com/realtectonics/sim/PlanetData.java', [
("""    private static final int FORMAT = 11;""", """    private static final int FORMAT = 12;"""),
("""11 landforms (no new data)""", """11 landforms, 12 rich biomes (no new data)"""),
])
S = 'src/main/java/com/realtectonics/mod/TectonicBiomeSource.java'
edit(S, [
("""            case MANGROVE -> "mangrove_swamp";""", """            case MANGROVE -> "mangrove_swamp";
            case RIVER -> "river";
            case FROZEN_RIVER -> "frozen_river";
            case BEACH -> "beach";
            case SNOWY_BEACH -> "snowy_beach";
            case STONY_SHORE -> "stony_shore";
            case COLD_OCEAN -> "cold_ocean";
            case LUKEWARM_OCEAN -> "lukewarm_ocean";
            case DEEP_COLD_OCEAN -> "deep_cold_ocean";
            case DEEP_LUKEWARM_OCEAN -> "deep_lukewarm_ocean";
            case DEEP_FROZEN_OCEAN -> "deep_frozen_ocean";
            case MEADOW -> "meadow";
            case GROVE -> "grove";
            case SNOWY_SLOPES -> "snowy_slopes";
            case JAGGED_PEAKS -> "jagged_peaks";
            case WINDSWEPT_HILLS -> "windswept_hills";"""),
])
G = 'src/main/java/com/realtectonics/mod/TectonicChunkGenerator.java'
edit(G, [
("""                boolean frozen = c.biome() == Biome.FROZEN_OCEAN;""", """                boolean frozen = frozen(c.biome());"""),
("""                                : c.biome() == Biome.FROZEN_OCEAN && y == c.water() ? ICE : WATER, false);""",
 """                                : frozen(c.biome()) && y == c.water() ? ICE : WATER, false);"""),
("""        boolean snowy = c.biome() == Biome.ICE_CAP || c.biome() == Biome.SNOWY_PEAKS || c.biome() == Biome.TUNDRA;""",
 """        boolean snowy = switch (c.biome()) {
            case ICE_CAP, SNOWY_PEAKS, TUNDRA, SNOWY_SLOPES, JAGGED_PEAKS, GROVE, SNOWY_BEACH -> true;
            default -> false;
        };"""),
("""            case ICE_CAP, SNOWY_PEAKS -> SNOW_PACK;
            case TUNDRA -> SNOWY_GRASS;
            case ROCKY_PEAKS -> ROCK;""",
 """            case ICE_CAP, SNOWY_PEAKS, SNOWY_SLOPES, JAGGED_PEAKS -> SNOW_PACK;
            case TUNDRA, GROVE -> SNOWY_GRASS;
            case ROCKY_PEAKS, STONY_SHORE -> ROCK;
            case BEACH, SNOWY_BEACH -> BEACH;"""),
("""    /** The surface was built along with the terrain in fillFromNoise. */""",
 """    /** Water that freezes over at the surface. */
    private static boolean frozen(Biome b) {
        return b == Biome.FROZEN_OCEAN || b == Biome.DEEP_FROZEN_OCEAN || b == Biome.FROZEN_RIVER;
    }

    /** The surface was built along with the terrain in fillFromNoise. */"""),
])
print("ok")
