exec(open('tools/edit_glacier.py').read().split("C = ")[0])
G = 'src/main/java/com/realtectonics/mod/TectonicChunkGenerator.java'
edit(G, [
("""                if (!glacier && !wet && RealTectonics.terrain().gentleShores) s = dryEdge(s, c, x0 + lx, z0 + lz);""",
 """                if (!glacier && !wet && RealTectonics.terrain().gentleShores) s = dryEdge(s, c, x0 + lx, z0 + lz);
                if (!glacier && !wet) s = moddedGround(chunk, s, c, x0 + lx, z0 + lz);"""),
("""    private static boolean scree(Terrain.Column c, int slope) {""",
 """    private static final BlockState ORANGE_SAND = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("biomesoplenty", "orange_sand")).defaultBlockState();
    private static final Surface[] RAINFOREST_BANDS = { // BOP's rocky rainforest: banded clays showing through
            new Surface(Blocks.TERRACOTTA.defaultBlockState(), Blocks.TERRACOTTA.defaultBlockState(), false),
            new Surface(Blocks.LIGHT_GRAY_TERRACOTTA.defaultBlockState(), Blocks.TERRACOTTA.defaultBlockState(), false),
            new Surface(Blocks.CYAN_TERRACOTTA.defaultBlockState(), Blocks.TERRACOTTA.defaultBlockState(), false),
            new Surface(Blocks.LIGHT_BLUE_TERRACOTTA.defaultBlockState(), Blocks.TERRACOTTA.defaultBlockState(), false)};

    /**
     * Biomes O' Plenty's own ground in its biomes, after its surface rules (BOPSurfaceRuleData, 1.21.1): this generator
     * builds every surface itself, so a BOP biome would otherwise grow its plants on our plain grass. Only plain grass and
     * beach sand change; cliffs, scree, banks, salt and snow keep what the terrain says. Patches follow Terrain.patch, a
     * few dozen blocks across, where BOP uses its surface noise.
     */
    private static Surface moddedGround(ChunkAccess chunk, Surface s, Terrain.Column c, int x, int z) {
        if (s != GRASS && s != BEACH) return s;
        var key = chunk.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(c.ground()), QuartPos.fromBlock(z)).unwrapKey();
        if (key.isEmpty() || !key.get().location().getNamespace().equals("biomesoplenty")) return s;
        float patch = RealTectonics.terrain().patch(x, z);
        return switch (key.get().location().getPath()) {
            case "redwood_forest" -> s == GRASS ? PODZOL : s;                                  // a carpet of needles everywhere
            case "old_growth_woodland" -> s == GRASS && patch > 0.45f ? COARSE : s;            // bare patches under the old trees
            case "lush_savanna" -> s == GRASS && patch > 0.4f && !ORANGE_SAND.isAir()
                    ? new Surface(ORANGE_SAND, Blocks.DIRT.defaultBlockState(), false) : s;      // red-orange soil showing through
            case "rocky_rainforest" -> s == GRASS && patch > 0.2f ? RAINFOREST_BANDS[Math.min(3, (int) ((patch - 0.2f) / 0.15f))] : s;
            case "gravel_beach" -> GRAVEL;
            default -> s;
        };
    }

    private static boolean scree(Terrain.Column c, int slope) {"""),
])
print("ok")
