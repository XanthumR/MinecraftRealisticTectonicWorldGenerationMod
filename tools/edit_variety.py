exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""    /** Patchiness for snow and rock, -1..1, at real block (x, z): coherent over a few dozen blocks. */""",
 """    /**
     * 0..1 at real block (x, z), coherent over several hundred blocks with ragged edges: which of a climate's biomes
     * grows here, so one climate zone holds stands of different woods and grasslands, as real ones do.
     */
    public float variety(int realX, int realZ) {
        double x = realX / (double) zoom, z = realZ / (double) zoom;
        return Math.clamp(0.5f + 0.8f * (fine(x, z, 1024, 64) + 0.35f * fine(x, z, 256, 65)), 0, 0.999f);
    }

    /** Patchiness for snow and rock, -1..1, at real block (x, z): coherent over a few dozen blocks. */"""),
])
S = 'src/main/java/com/realtectonics/mod/TectonicBiomeSource.java'
edit(S, [
("""    static final String VANILLA = "vanilla", BOP = "biomesoplenty", AUTO = "auto";""",
 """    static final String VANILLA = "vanilla", BOP = "biomesoplenty", BOP_VARIED = "biomesoplenty_varied", AUTO = "auto";"""),
(""" * pack: "vanilla", "biomesoplenty", or "auto" (the preset's), which becomes "biomesoplenty" when that mod is loaded""",
 """ * pack: "vanilla", "biomesoplenty" (one biome per climate), "biomesoplenty_varied" (several per climate, in patches
 * a few hundred blocks across), or "auto" (the preset's), which becomes "biomesoplenty_varied" when that mod is loaded"""),
("""    private final Map<Biome, Holder<net.minecraft.world.level.biome.Biome>> biomes = new EnumMap<>(Biome.class);""",
 """    private final Map<Biome, List<Holder<net.minecraft.world.level.biome.Biome>>> biomes = new EnumMap<>(Biome.class);"""),
("""        this.pack = AUTO.equals(pack) ? ModList.get().isLoaded(BOP) ? BOP : VANILLA : pack;
        for (Biome b : Biome.values()) {
            String modded = BOP.equals(this.pack) ? biomesOPlenty(b) : null; // missing mod: the vanilla biome stands in
            var holder = modded == null ? java.util.Optional.<Holder.Reference<net.minecraft.world.level.biome.Biome>>empty()
                    : registry.get(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(BOP, modded)));
            biomes.put(b, holder.isPresent() ? holder.get()
                    : registry.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace(vanilla(b)))));
        }
    }""",
 """        this.pack = AUTO.equals(pack) ? ModList.get().isLoaded(BOP) ? BOP_VARIED : VANILLA : pack;
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
    }"""),
("""        return biomes.values().stream().distinct();""", """        return biomes.values().stream().flatMap(List::stream).distinct();"""),
("""            c.values[slot] = biomes.get(RealTectonics.terrain().biomeAt(QuartPos.toBlock(x), QuartPos.toBlock(z)));""",
 """            int bx = QuartPos.toBlock(x), bz = QuartPos.toBlock(z);
            var options = biomes.get(RealTectonics.terrain().biomeAt(bx, bz));
            c.values[slot] = options.size() == 1 ? options.get(0) : options.get((int) (RealTectonics.terrain().variety(bx, bz) * options.size()));"""),
("""import java.util.Arrays;""", """import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;"""),
])
print("ok")
