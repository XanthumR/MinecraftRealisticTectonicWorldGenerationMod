exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
# 1. massifs: ridged relief at the 128-512 block scale on high ground
("""        if (gullies && natural > SEA_Y + 20) natural += gullies(x, z, u, v, natural - SEA_Y);""",
 """        // Massifs: a range is many peaks, spurs and deep valleys, not one dome. Ridged relief at the 128-512 block
        // scale, growing with height, so plains stay calm and the highest ranges are cut the deepest.
        float crest = 0;
        if (massifs && natural > SEA_Y + 20) {
            float r1 = 1 - Math.abs(fine(x, z, 512, 64)), r2 = 1 - Math.abs(fine(x, z, 256, 65)), r3 = 1 - Math.abs(fine(x, z, 128, 66));
            crest = (r1 * r1 + 0.5f * r2 * r2 * r1 + 0.25f * r3 * r3 * r2) / 1.75f; // finer ridges ride on the bigger ones
            float high = natural - SEA_Y, relief = Math.min(MASSIF_RELIEF, MASSIF_SHARE * high) * smoothstep((high - 20) / 60);
            natural += relief * (2 * crest - 0.8f);
        }
        if (gullies && natural > SEA_Y + 20) natural += gullies(x, z, u, v, natural - SEA_Y) * (1 - crest * crest * crest); // none on the crests"""),
# 2. gullies: each wave its own strength, so spacing and depth vary
("""                sum += wgt * Math.cos((ox * px + oz * pz) * k + 2 * Math.PI * hash01(key + 2));
                weights += wgt;""",
 """                float strength = massifs ? 0.3f + 0.7f * hash01(key + 3) : 1; // some gullies deep, some faint, some gone
                sum += wgt * strength * Math.cos((ox * px + oz * pz) * k + 2 * Math.PI * hash01(key + 2));
                weights += wgt;"""),
# 4. ice spikes only in patches
("""        if (b == Biome.DESERT && metres > BADLANDS_METRES) return rain > 200 ? Biome.WOODED_BADLANDS : Biome.BADLANDS;""",
 """        if (b == Biome.DESERT && metres > BADLANDS_METRES) return rain > 200 ? Biome.WOODED_BADLANDS : Biome.BADLANDS;
        if (massifs && b == Biome.ICE_CAP) return noise(x, z, 256, 67) > ICE_SPIKE_PATCH ? Biome.ICE_SPIKES : Biome.SNOWY_PLAINS;"""),
("""    private final boolean gullies;""", """    private final boolean gullies;
    private final boolean massifs;        // mountain relief, varied gullies, rare ice spikes: format 15"""),
("""        gullies = data.format >= 14;""", """        gullies = data.format >= 14;
        massifs = data.format >= 15;"""),
("""    static final float GULLY_DEPTH = 12;""", """    static final float MASSIF_RELIEF = 120, MASSIF_SHARE = 0.45f; // blocks of peak-and-valley relief at most, and as a share of height
    static final float ICE_SPIKE_PATCH = 0.55f;       // ice spikes only where this noise peaks: rare patches on the ice
    static final float GULLY_DEPTH = 12;"""),
])
B = 'src/main/java/com/realtectonics/sim/Biome.java'
edit(B, [
("""    SNOWY_TAIGA(0x50786c), OLD_GROWTH_PINE_TAIGA(0x3c5e3a), BADLANDS(0xc8643c), WOODED_BADLANDS(0xa86a44), ERODED_BADLANDS(0xd27848);""",
 """    SNOWY_TAIGA(0x50786c), OLD_GROWTH_PINE_TAIGA(0x3c5e3a), BADLANDS(0xc8643c), WOODED_BADLANDS(0xa86a44), ERODED_BADLANDS(0xd27848),
    // From format 15 the ice cap is snowy plains, with ice spikes in rare patches.
    SNOWY_PLAINS(0xf0f4f8), ICE_SPIKES(0xc8e0f8);"""),
])
edit('src/main/java/com/realtectonics/sim/PlanetData.java', [
("""    private static final int FORMAT = 14;""", """    private static final int FORMAT = 15;"""),
("""14 gullies (no new data)""", """14 gullies, 15 massifs (no new data)"""),
])
edit('src/main/java/com/realtectonics/mod/TectonicBiomeSource.java', [
("""            case ERODED_BADLANDS -> "eroded_badlands";""", """            case ERODED_BADLANDS -> "eroded_badlands";
            case SNOWY_PLAINS -> "snowy_plains";
            case ICE_SPIKES -> "ice_spikes";"""),
])
G = 'src/main/java/com/realtectonics/mod/TectonicChunkGenerator.java'
edit(G, [
("""        return b == Biome.SNOWY_PEAKS || b == Biome.JAGGED_PEAKS || b == Biome.SNOWY_SLOPES || b == Biome.ICE_CAP;""",
 """        return b == Biome.SNOWY_PEAKS || b == Biome.JAGGED_PEAKS || b == Biome.SNOWY_SLOPES || b == Biome.ICE_CAP || b == Biome.SNOWY_PLAINS || b == Biome.ICE_SPIKES;"""),
("""            case ICE_CAP, SNOWY_PEAKS, SNOWY_SLOPES, JAGGED_PEAKS -> SNOW_PACK;""", """            case ICE_CAP, SNOWY_PEAKS, SNOWY_SLOPES, JAGGED_PEAKS, SNOWY_PLAINS, ICE_SPIKES -> SNOW_PACK;"""),
("""            case ICE_CAP, SNOWY_PEAKS, TUNDRA, SNOWY_SLOPES, JAGGED_PEAKS, GROVE, SNOWY_BEACH, SNOWY_TAIGA -> true;""",
 """            case ICE_CAP, SNOWY_PEAKS, TUNDRA, SNOWY_SLOPES, JAGGED_PEAKS, GROVE, SNOWY_BEACH, SNOWY_TAIGA, SNOWY_PLAINS, ICE_SPIKES -> true;"""),
# 3. more rock above the snow line
("""    static final float SNOW_SLOPE = 1.5f, SNOW_SHADE = 1.5f, SNOW_PATCH = 1.2f, SNOW_SCOUR = 0.75f; // see snowStays""",
 """    static final float SNOW_SLOPE = 0.8f, SNOW_SHADE = 1.2f, SNOW_PATCH = 1.0f, SNOW_SCOUR = 0.5f; // see snowStays"""),
])
print("ok")
