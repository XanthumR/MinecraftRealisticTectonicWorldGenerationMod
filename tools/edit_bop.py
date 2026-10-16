exec(open('tools/edit_glacier.py').read().split("C = ")[0])
S = 'src/main/java/com/realtectonics/mod/TectonicBiomeSource.java'
edit(S, [
("""/** Biomes straight from the simulated climate, mapped onto vanilla biomes. */
public final class TectonicBiomeSource extends BiomeSource {
    public static final MapCodec<TectonicBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            RegistryOps.retrieveGetter(Registries.BIOME)
    ).apply(i, TectonicBiomeSource::new));""",
 """/**
 * Biomes straight from the simulated climate, mapped onto vanilla biomes, or with the "biomesoplenty" pack onto
 * Biomes O' Plenty's realistic ones where they match the climate better. Biome mods add their biomes to vanilla's
 * multi-noise source, which this one replaces, so they only appear through this table.
 * pack: "vanilla", "biomesoplenty", or "auto" (the preset's), which becomes "biomesoplenty" when that mod is loaded
 * as the world is created and is saved resolved, so a world keeps its biomes if the mod is added or removed later.
 * Worlds saved before packs existed have no field and stay vanilla.
 */
public final class TectonicBiomeSource extends BiomeSource {
    static final String VANILLA = "vanilla", BOP = "biomesoplenty", AUTO = "auto";
    public static final MapCodec<TectonicBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            RegistryOps.retrieveGetter(Registries.BIOME),
            Codec.STRING.optionalFieldOf("pack", VANILLA).forGetter(s -> s.pack)
    ).apply(i, TectonicBiomeSource::new));

    private final String pack;"""),
("""    public TectonicBiomeSource(HolderGetter<net.minecraft.world.level.biome.Biome> registry) {
        for (Biome b : Biome.values())
            biomes.put(b, registry.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace(vanilla(b)))));
    }""",
 """    public TectonicBiomeSource(HolderGetter<net.minecraft.world.level.biome.Biome> registry, String pack) {
        this.pack = AUTO.equals(pack) ? ModList.get().isLoaded(BOP) ? BOP : VANILLA : pack;
        for (Biome b : Biome.values()) {
            String modded = BOP.equals(this.pack) ? biomesOPlenty(b) : null; // missing mod: the vanilla biome stands in
            var holder = modded == null ? java.util.Optional.<Holder.Reference<net.minecraft.world.level.biome.Biome>>empty()
                    : registry.get(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(BOP, modded)));
            biomes.put(b, holder.isPresent() ? holder.get()
                    : registry.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace(vanilla(b)))));
        }
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
    }"""),
])
edit(S, [
("""import com.mojang.serialization.MapCodec;""", """import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;"""),
("""import net.minecraft.world.level.biome.Climate;""", """import net.minecraft.world.level.biome.Climate;
import net.neoforged.fml.ModList;"""),
])
P = 'src/main/resources/data/realtectonics/worldgen/world_preset/planet.json'
s = open(P, encoding='utf-8').read()
assert s.count('"biome_source": { "type": "realtectonics:planet" },') == 1
s = s.replace('"biome_source": { "type": "realtectonics:planet" },', '"biome_source": { "type": "realtectonics:planet", "pack": "auto" },')
open(P, 'w', encoding='utf-8').write(s)
G = 'build.gradle'
edit(G, [
("""    runtimeOnly 'maven.modrinth:distanthorizons:3.3.3-1.21.1'""",
 """    runtimeOnly 'maven.modrinth:distanthorizons:3.3.3-1.21.1'
    // Biomes O' Plenty for dev runs: new worlds pick its realistic biomes (TectonicBiomeSource "biomesoplenty" pack).
    runtimeOnly 'maven.modrinth:biomes-o-plenty:21.1.0.14'
    runtimeOnly 'maven.modrinth:terrablender:4.1.0.8'
    runtimeOnly 'maven.modrinth:glitchcore:2.1.0.2'"""),
])
print("ok")
