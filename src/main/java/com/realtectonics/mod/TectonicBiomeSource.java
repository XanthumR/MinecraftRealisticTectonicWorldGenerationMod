package com.realtectonics.mod;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.realtectonics.sim.Biome;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Biomes straight from the simulated climate, mapped onto vanilla biomes, or with the "biomesoplenty" pack onto
 * Biomes O' Plenty's realistic ones where they match the climate better. Biome mods add their biomes to vanilla's
 * multi-noise source, which this one replaces, so they only appear through this table.
 * pack: "vanilla", "biomesoplenty" (one biome per climate), "biomesoplenty_varied" (several per climate, in patches
 * a few hundred blocks across), or "auto" (the preset's), which becomes "biomesoplenty_varied" when that mod is loaded
 * as the world is created and is saved resolved, so a world keeps its biomes if the mod is added or removed later.
 * Worlds saved before packs existed have no field and stay vanilla.
 */
public final class TectonicBiomeSource extends BiomeSource {
    static final String VANILLA = "vanilla", BOP = "biomesoplenty", BOP_VARIED = "biomesoplenty_varied", AUTO = "auto";
    public static final MapCodec<TectonicBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            RegistryOps.retrieveGetter(Registries.BIOME),
            Codec.STRING.optionalFieldOf("pack", VANILLA).forGetter(s -> s.pack)
    ).apply(i, TectonicBiomeSource::new));

    private final String pack;

    private final Map<Biome, List<Holder<net.minecraft.world.level.biome.Biome>>> biomes = new EnumMap<>(Biome.class);

    /** Last biome per column slot. Chunks ask once per 4x4x4 cell, but the answer only depends on the column. */
    private record ColumnCache(long[] keys, Holder<?>[] values) {}

    private final ThreadLocal<ColumnCache> cache = ThreadLocal.withInitial(() -> {
        long[] keys = new long[16];
        Arrays.fill(keys, Long.MIN_VALUE);
        return new ColumnCache(keys, new Holder<?>[16]);
    });

    public TectonicBiomeSource(HolderGetter<net.minecraft.world.level.biome.Biome> registry, String pack) {
        this.pack = AUTO.equals(pack) ? ModList.get().isLoaded(BOP) ? BOP_VARIED : VANILLA : pack;
        for (Biome b : Biome.values()) {
            List<String> ids = BOP_VARIED.equals(this.pack) ? varied(b)
                    : BOP.equals(this.pack) && biomesOPlenty(b) != null ? List.of(BOP + ":" + biomesOPlenty(b)) : List.of();
            List<Holder<net.minecraft.world.level.biome.Biome>> found = new ArrayList<>();
            for (String id : ids) registry.get(ResourceKey.create(Registries.BIOME, ResourceLocation.parse(id))).ifPresent(found::add);
            if (found.isEmpty()) // missing mod: the vanilla biome stands in
                found.add(registry.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace(vanilla(b)))));
            biomes.put(b, found);
        }
    }

    /**
     * Several biomes per simulated climate for the "biomesoplenty_varied" pack, all real vegetation: a zone grows
     * stands of each (Terrain.variety picks which). Vanilla ids mixed in keep vanilla's woods among them. Empty keeps
     * the single vanilla biome. Ids that are not registered (mod missing) drop out.
     */
    static List<String> varied(Biome b) {
        String v = "minecraft:" + vanilla(b), p = BOP + ":";
        return switch (b) {
            case TUNDRA -> List.of(p + "tundra", p + "muskeg", p + "tundra");                             // shrub tundra, boggy tundra
            case TAIGA -> List.of(v, p + "maple_woods", p + "fir_clearing", v);                          // boreal spruce, maple, fir
            case SNOWY_TAIGA -> List.of(p + "snowy_coniferous_forest", p + "snowy_fir_clearing", v, p + "snowy_maple_woods");
            case TEMPERATE_RAINFOREST -> List.of(p + "coniferous_forest", p + "redwood_forest", v);       // Pacific Northwest
            case TEMPERATE_FOREST -> List.of(p + "woodland", p + "seasonal_forest", v, p + "forested_field");
            case GRASSLAND -> List.of(p + "grassland", p + "field", v, p + "pasture");
            case STEPPE -> List.of(p + "prairie", p + "shrubland", p + "scrubland");                      // tall grass to dry scrub
            case SAVANNA -> List.of(v, p + "lush_savanna", v, p + "dryland");
            case RAINFOREST -> List.of(p + "rainforest", v, p + "rocky_rainforest");
            case SWAMP -> List.of(p + "bayou", p + "marsh", p + "wetland");
            case WINDSWEPT_HILLS -> List.of(p + "highland", p + "moor", v);
            case DARK_FOREST -> List.of(p + "old_growth_woodland", v);
            case BIRCH_FOREST, OLD_GROWTH_BIRCH_FOREST -> List.of(v, p + "aspen_glade");
            case FLOWER_FOREST -> List.of(v, p + "lavender_field");
            case BEACH -> List.of(v, p + "dune_beach", v);
            case STONY_SHORE -> List.of(p + "gravel_beach", v);
            default -> List.of();
        };
    }

    /**
     * Biomes O' Plenty biomes that fit a simulated climate better than vanilla's: real vegetation, not fantasy ones.
     * null keeps vanilla's. ponytail: one biome per climate; variety inside a zone still comes from Terrain.patches.
     */
    static String biomesOPlenty(Biome b) {
        return switch (b) {
            case TUNDRA -> "tundra";                                  // low shrubs and lichen, not bare snowy plains
            case RAINFOREST -> "rainforest";                          // tall broadleaf canopy, not cocoa-hung jungle
            case SWAMP -> "bayou";                                    // cypress swamp
            case TEMPERATE_RAINFOREST -> "coniferous_forest";         // Pacific Northwest conifers
            case SNOWY_TAIGA -> "snowy_coniferous_forest";
            case STEPPE -> "prairie";                                 // tall grass, few trees
            case GRASSLAND -> "grassland";
            case TEMPERATE_FOREST -> "woodland";                      // mixed broadleaf woods
            case DARK_FOREST -> "old_growth_woodland";
            case WINDSWEPT_HILLS -> "highland";                       // moorland hills
            case STONY_SHORE -> "gravel_beach";
            default -> null;
        };
    }

    static String vanilla(Biome b) {
        return switch (b) {
            case FROZEN_OCEAN -> "frozen_ocean";
            case DEEP_OCEAN -> "deep_ocean";
            case OCEAN -> "ocean";
            case WARM_OCEAN -> "warm_ocean";
            case ICE_CAP -> "ice_spikes";
            case TUNDRA -> "snowy_plains";
            case TAIGA -> "taiga";
            case STEPPE, GRASSLAND -> "plains";
            case DESERT -> "desert";
            case TEMPERATE_FOREST -> "forest";
            case TEMPERATE_RAINFOREST -> "old_growth_spruce_taiga";
            case SAVANNA -> "savanna";
            case TROPICAL_SEASONAL_FOREST -> "sparse_jungle";
            case RAINFOREST -> "jungle";
            case SNOWY_PEAKS -> "frozen_peaks";
            case ROCKY_PEAKS -> "stony_peaks";
            case SWAMP -> "swamp";
            case MANGROVE -> "mangrove_swamp";
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
            case WINDSWEPT_HILLS -> "windswept_hills";
            case BIRCH_FOREST -> "birch_forest";
            case OLD_GROWTH_BIRCH_FOREST -> "old_growth_birch_forest";
            case DARK_FOREST -> "dark_forest";
            case FLOWER_FOREST -> "flower_forest";
            case CHERRY_GROVE -> "cherry_grove";
            case SNOWY_TAIGA -> "snowy_taiga";
            case OLD_GROWTH_PINE_TAIGA -> "old_growth_pine_taiga";
            case BADLANDS -> "badlands";
            case WOODED_BADLANDS -> "wooded_badlands";
            case ERODED_BADLANDS -> "eroded_badlands";
            case SNOWY_PLAINS -> "snowy_plains";
            case ICE_SPIKES -> "ice_spikes";
        };
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<net.minecraft.world.level.biome.Biome>> collectPossibleBiomes() {
        return biomes.values().stream().flatMap(List::stream).distinct();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Holder<net.minecraft.world.level.biome.Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        ColumnCache c = cache.get();
        long key = (long) x << 32 | (z & 0xffffffffL);
        int slot = (x & 3) | (z & 3) << 2;
        if (c.keys[slot] != key) {
            c.keys[slot] = key;
            int bx = QuartPos.toBlock(x), bz = QuartPos.toBlock(z);
            var options = biomes.get(RealTectonics.terrain().biomeAtCached(bx, bz));
            c.values[slot] = options.size() == 1 ? options.get(0) : options.get((int) (RealTectonics.terrain().variety(bx, bz) * options.size()));
        }
        return (Holder<net.minecraft.world.level.biome.Biome>) c.values[slot];
    }
}
