exec(open('tools/edit_glacier.py').read().split("C = ")[0])
B = 'src/main/java/com/realtectonics/sim/Biome.java'
edit(B, [
("""    MEADOW(0x8cc85a), GROVE(0x5a8c78), SNOWY_SLOPES(0xdce6ec), JAGGED_PEAKS(0xf0f4f8), WINDSWEPT_HILLS(0x7d8c6e);""",
 """    MEADOW(0x8cc85a), GROVE(0x5a8c78), SNOWY_SLOPES(0xdce6ec), JAGGED_PEAKS(0xf0f4f8), WINDSWEPT_HILLS(0x7d8c6e),
    // From format 13: patches within the forests, and badlands on dry uplands.
    BIRCH_FOREST(0x6a9e50), OLD_GROWTH_BIRCH_FOREST(0x5e9448), DARK_FOREST(0x2c5a26), FLOWER_FOREST(0x7aae4a), CHERRY_GROVE(0xe8a8c8),
    SNOWY_TAIGA(0x50786c), OLD_GROWTH_PINE_TAIGA(0x3c5e3a), BADLANDS(0xc8643c), WOODED_BADLANDS(0xa86a44), ERODED_BADLANDS(0xd27848);"""),
])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""        if (metres < MOUNTAIN_METRES) return b;""",
 """        if (metres < MOUNTAIN_METRES) return forestVariety ? patches(b, metres, t, rain, x, z) : b;"""),
("""    /**
     * Seas by their surface temperature""",
 """    /**
     * Patches within the broad climate zones, the way real forests vary: dark forest where it is wettest, birch where
     * cooler, flowers where warmer, a rare cherry grove in warm wet hills; snowy and old-growth taiga; badlands on
     * dry uplands, wooded at their wetter edges.
     */
    private Biome patches(Biome b, float metres, float t, float rain, double x, double z) {
        float patch = noise(x, z, 512, 55) + 0.5f * noise(x, z, 256, 56), other = noise(x, z, 256, 57);
        if ((b == Biome.TEMPERATE_FOREST || b == Biome.GRASSLAND) && t > 12 && t < 18 && rain > 900 && metres > 400 && other > CHERRY_PATCH)
            return Biome.CHERRY_GROVE;
        if (b == Biome.TEMPERATE_FOREST) {
            if (rain > 1200 && patch > 0.35f) return Biome.DARK_FOREST;
            if (t < 11 && rain > 900 && patch < -0.65f) return Biome.OLD_GROWTH_BIRCH_FOREST;
            if (t < 13 && patch < -0.3f) return Biome.BIRCH_FOREST;
            if (t > 14 && other > 0.45f) return Biome.FLOWER_FOREST;
        }
        if (b == Biome.TAIGA) {
            if (t < 3) return Biome.SNOWY_TAIGA;
            if (rain > 900 && patch > 0.3f) return Biome.OLD_GROWTH_PINE_TAIGA;
        }
        if (b == Biome.DESERT && metres > BADLANDS_METRES) return rain > 300 ? Biome.WOODED_BADLANDS : Biome.BADLANDS;
        return b;
    }

    /**
     * Seas by their surface temperature"""),
("""        return new Column(ground, water, lava, ice, biome, fold, volcanic, rock, cover);""",
 """        if (forestVariety && cover == Cover.CLIFF && (biome == Biome.BADLANDS || biome == Biome.WOODED_BADLANDS || biome == Biome.DESERT))
            biome = Biome.ERODED_BADLANDS; // canyon walls
        return new Column(ground, water, lava, ice, biome, fold, volcanic, rock, cover);"""),
("""    private final boolean richBiomes;""", """    private final boolean richBiomes;
    private final boolean forestVariety;  // forest patches and badlands of format 13"""),
("""        richBiomes = data.format >= 12;""", """        richBiomes = data.format >= 12;
        forestVariety = data.format >= 13;"""),
("""    static final float MOUNTAIN_METRES = 1000, MEADOW_METRES = 1500, JAGGED_METRES = 3000;""",
 """    static final float MOUNTAIN_METRES = 1000, MEADOW_METRES = 1500, JAGGED_METRES = 3000;
    static final float CHERRY_PATCH = 0.6f, BADLANDS_METRES = 500; // cherry groves are rare; deserts this high are badlands"""),
])
edit('src/main/java/com/realtectonics/sim/PlanetData.java', [
("""    private static final int FORMAT = 12;""", """    private static final int FORMAT = 13;"""),
("""12 rich biomes (no new data)""", """12 rich biomes, 13 forest variety (no new data)"""),
])
S = 'src/main/java/com/realtectonics/mod/TectonicBiomeSource.java'
edit(S, [
("""            case WINDSWEPT_HILLS -> "windswept_hills";""", """            case WINDSWEPT_HILLS -> "windswept_hills";
            case BIRCH_FOREST -> "birch_forest";
            case OLD_GROWTH_BIRCH_FOREST -> "old_growth_birch_forest";
            case DARK_FOREST -> "dark_forest";
            case FLOWER_FOREST -> "flower_forest";
            case CHERRY_GROVE -> "cherry_grove";
            case SNOWY_TAIGA -> "snowy_taiga";
            case OLD_GROWTH_PINE_TAIGA -> "old_growth_pine_taiga";
            case BADLANDS -> "badlands";
            case WOODED_BADLANDS -> "wooded_badlands";
            case ERODED_BADLANDS -> "eroded_badlands";"""),
])
G = 'src/main/java/com/realtectonics/mod/TectonicChunkGenerator.java'
edit(G, [
("""        boolean arid = (c.biome() == Biome.DESERT || c.cover() == Terrain.Cover.CLIFF && c.biome() == Biome.SAVANNA) && y > c.ground() - ARID_DEPTH;""",
 """        boolean arid = (badlands(c.biome()) || c.biome() == Biome.DESERT || c.cover() == Terrain.Cover.CLIFF && c.biome() == Biome.SAVANNA) && y > c.ground() - ARID_DEPTH;"""),
("""            case ICE_CAP, SNOWY_PEAKS, TUNDRA, SNOWY_SLOPES, JAGGED_PEAKS, GROVE, SNOWY_BEACH -> true;""",
 """            case ICE_CAP, SNOWY_PEAKS, TUNDRA, SNOWY_SLOPES, JAGGED_PEAKS, GROVE, SNOWY_BEACH, SNOWY_TAIGA -> true;"""),
("""            case TUNDRA, GROVE -> SNOWY_GRASS;""", """            case TUNDRA, GROVE, SNOWY_TAIGA -> SNOWY_GRASS;
            case OLD_GROWTH_PINE_TAIGA -> PODZOL;
            case BADLANDS, ERODED_BADLANDS -> RED_SAND; // terracotta shows on every slope
            case WOODED_BADLANDS -> COARSE;"""),
("""            SAND_BANK = new Surface(""", """            RED_SAND = new Surface(Blocks.RED_SAND.defaultBlockState(), Blocks.RED_SANDSTONE.defaultBlockState(), false),
            COARSE = new Surface(Blocks.COARSE_DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), false),
            SAND_BANK = new Surface("""),
("""    /** Water that freezes over at the surface. */""",
 """    private static boolean badlands(Biome b) {
        return b == Biome.BADLANDS || b == Biome.WOODED_BADLANDS || b == Biome.ERODED_BADLANDS;
    }

    /** Water that freezes over at the surface. */"""),
])
print("ok")
