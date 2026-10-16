package com.realtectonics.sim;

/** Whittaker biomes from annual temperature and rainfall. Mapping to Minecraft biomes comes in the mod layer. */
public enum Biome {
    FROZEN_OCEAN(0xb4c8e6), DEEP_OCEAN(0x0a1e5a), OCEAN(0x1e46a0), WARM_OCEAN(0x1e78b4),
    ICE_CAP(0xf4f8fc), TUNDRA(0xa0aa96), TAIGA(0x2d5a46), STEPPE(0xb4b478),
    DESERT(0xe6d296), GRASSLAND(0x9cbe5a), TEMPERATE_FOREST(0x46823c), TEMPERATE_RAINFOREST(0x286e46),
    SAVANNA(0xc8b45a), TROPICAL_SEASONAL_FOREST(0x6e9632), RAINFOREST(0x14641e),
    SNOWY_PEAKS(0xe6eef4), ROCKY_PEAKS(0x8c8c8c), SWAMP(0x4c6e3c), MANGROVE(0x3c5a32), // the last two only on deltas
    // Planets from format 12 on: rivers, shores, ocean temperatures and mountain zones, picked per block in Terrain.
    RIVER(0x3c64d2), FROZEN_RIVER(0xa0b4dc), BEACH(0xf0e2a8), SNOWY_BEACH(0xf2f2ea), STONY_SHORE(0x8a8a8a),
    COLD_OCEAN(0x2a4a9a), LUKEWARM_OCEAN(0x2060b0), DEEP_COLD_OCEAN(0x14306e), DEEP_LUKEWARM_OCEAN(0x123c7a), DEEP_FROZEN_OCEAN(0x8ca0c8),
    MEADOW(0x8cc85a), GROVE(0x5a8c78), SNOWY_SLOPES(0xdce6ec), JAGGED_PEAKS(0xf0f4f8), WINDSWEPT_HILLS(0x7d8c6e),
    // From format 13: patches within the forests, and badlands on dry uplands.
    BIRCH_FOREST(0x6a9e50), OLD_GROWTH_BIRCH_FOREST(0x5e9448), DARK_FOREST(0x2c5a26), FLOWER_FOREST(0x7aae4a), CHERRY_GROVE(0xe8a8c8),
    SNOWY_TAIGA(0x50786c), OLD_GROWTH_PINE_TAIGA(0x3c5e3a), BADLANDS(0xc8643c), WOODED_BADLANDS(0xa86a44), ERODED_BADLANDS(0xd27848),
    // From format 15 the ice cap is snowy plains, with ice spikes in rare patches.
    SNOWY_PLAINS(0xf0f4f8), ICE_SPIKES(0xc8e0f8);

    public final int color;

    Biome(int color) {
        this.color = color;
    }

    public static final float ICE_CAP_BELOW = -14, OLD_ICE_CAP_BELOW = -8; // °C annual mean; Yakutsk at -9 is taiga

    public static Biome classify(float metres, float temperature, float rainMm) {
        return classify(metres, temperature, rainMm, ICE_CAP_BELOW);
    }

    /**
     * With seasons (Köppen's climate types): swing is half the summer-winter range, summerShare the share of the rain
     * falling in the warm half. Trees need a summer above 10 °C; how much rain makes land dry rises with heat, and with
     * how much of it falls in summer, when it evaporates; rainforest needs rain all year round.
     */
    public static Biome classify(float metres, float temperature, float rainMm, float swing, float summerShare) {
        if (metres < 0) return classify(metres, temperature, rainMm);
        if (metres > 2000 && temperature < 0) return SNOWY_PEAKS;
        if (metres > 3000) return ROCKY_PEAKS;
        float warm = temperature + swing, cold = temperature - swing;
        if (warm < 0) return ICE_CAP;  // no month above freezing
        if (warm < 10) return TUNDRA;  // summers too short and cool for trees
        float dryBelow = 20 * temperature + (summerShare >= 0.7f ? 280 : summerShare >= 0.3f ? 140 : 0);
        if (rainMm < dryBelow / 2) return temperature >= 10 ? DESERT : STEPPE; // cold deserts are bare steppe
        if (rainMm < dryBelow) return temperature >= 18 ? SAVANNA : STEPPE;
        if (cold >= 18) { // tropical: no winter, only wet and dry seasons
            boolean drySeason = Math.abs(summerShare - 0.5f) > 0.25f;
            return drySeason ? (rainMm < 1500 ? SAVANNA : TROPICAL_SEASONAL_FOREST) : rainMm < 1800 ? TROPICAL_SEASONAL_FOREST : RAINFOREST;
        }
        if (cold < -3) return warm < 22 ? TAIGA : rainMm < 600 ? GRASSLAND : TEMPERATE_FOREST; // snowy winters; hot summers grow broadleaf forest
        return rainMm < 600 ? GRASSLAND : rainMm < 1800 ? TEMPERATE_FOREST : TEMPERATE_RAINFOREST;
    }

    /** iceCapBelow: planets from before continental climates used OLD_ICE_CAP_BELOW, and keep it so their biomes stay put. */
    public static Biome classify(float metres, float temperature, float rainMm, float iceCapBelow) {
        if (metres < 0) return temperature < -2 ? FROZEN_OCEAN : metres < -3000 ? DEEP_OCEAN : temperature > 24 ? WARM_OCEAN : OCEAN;
        if (metres > 2000 && temperature < 0) return SNOWY_PEAKS;
        if (metres > 3000) return ROCKY_PEAKS; // above the tree line
        if (temperature < iceCapBelow) return ICE_CAP;
        if (temperature < 0) return TUNDRA;
        if (temperature < 8) return rainMm < 300 ? STEPPE : TAIGA;
        if (temperature < 20) return rainMm < 250 ? DESERT : rainMm < 600 ? GRASSLAND : rainMm < 1800 ? TEMPERATE_FOREST : TEMPERATE_RAINFOREST;
        return rainMm < 300 ? DESERT : rainMm < 1000 ? SAVANNA : rainMm < 2000 ? TROPICAL_SEASONAL_FOREST : RAINFOREST;
    }
}
