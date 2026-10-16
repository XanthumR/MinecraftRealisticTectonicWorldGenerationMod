exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""    static final float GULLY_POWER = 0.8f;            // format 33: blocks cut per sqrt(blocks gathered) per block of slope""",
 """    static final float GULLY_POWER = 0.8f;            // format 33: blocks cut per sqrt(blocks gathered) per block of slope
    static final float GULLY_THRESHOLD = 1.5f;        // format 37: blocks of that stream power spent before water cuts at all (the soil holds)
    static final float BEACH_SLOPE = 60;              // blocks the beach rises per unit of land mask; from format 37 divided by the beach's width factor"""),
("""    private final boolean variedVolcanoes; // each volcano its own width, steepness, lobes and collapse scar, profile in blocks: format 36""",
 """    private final boolean variedVolcanoes; // each volcano its own width, steepness, lobes and collapse scar, profile in blocks: format 36
    private final boolean variedShores;   // beaches wide in bays and at river mouths, narrow and shingly on headlands; no gully pits: format 37
    private final float[] bay, silt;      // format 37: land mask blurred wide (above 0.5 in a bay); river sand reaching the coast"""),
("""        variedVolcanoes = data.format >= 36;""", """        variedVolcanoes = data.format >= 36;
        variedShores = data.format >= 37;"""),
("""        for (int pass = 0; pass < 3; pass++) blur3x3(land);""",
 """        for (int pass = 0; pass < 3; pass++) blur3x3(land);
        bay = variedShores ? land.clone() : null;
        silt = variedShores ? new float[w * h] : null;
        for (int g = 0; variedShores && g < w * h; g++) // rivers reaching the sea bring the sand that beaches are made of
            silt[g] = data.metres[g] >= 0 && data.receiver[g] >= 0 && data.metres[data.receiver[g]] < 0 ? Math.min(1, data.flow[g] / (8 * Hydrology.RIVER_AREA)) : 0;
        for (int pass = 0; variedShores && pass < 6; pass++) { blur3x3(bay); blur3x3(silt); }"""),
("""        float shore = cubic(land, u, v) - 0.5f, beach = SEA_Y - 1.5f + shore * (shore > 0 ? 60 : offshoreSlope);""",
 """        float shore = cubic(land, u, v) - 0.5f, beach = SEA_Y - 1.5f + shore * (shore > 0 ? BEACH_SLOPE / beachWidth(x, z, u, v) : offshoreSlope);"""),
("""    /** The column at real block (x, z). */
    public Column column(int x, int z) {""",
 """    /**
     * From format 37: how wide the beach is, as a factor on the plain one. Waves sweep sand along the coast into bays,
     * where it piles into broad beaches, and off the headlands, which keep only a narrow strip of shingle; river
     * mouths add the sand their rivers carry down. 1 on planets before.
     */
    private float beachWidth(double x, double z, double u, double v) {
        if (!variedShores) return 1;
        float sheltered = smoothstep((cubic(bay, u, v) - 0.3f) / 0.4f), sand = Math.min(1, 4 * cubic(silt, u, v));
        return (0.4f + 2.6f * sheltered + 2 * sand) * (0.6f + 0.4f * (1 + fine(x, z, 256, 81)));
    }

    /** The column at real block (x, z). */
    public Column column(int x, int z) {"""),
("""                cut[i] = Math.min(GULLY_DEPTH, GULLY_POWER * (float) Math.sqrt(area[i]) * slope[i]);""",
 """                cut[i] = Math.min(GULLY_DEPTH, variedShores // from 37 weak runoff cuts nothing (no pits a block deep), and waves keep beaches smooth
                        ? Math.max(0, GULLY_POWER * (float) Math.sqrt(area[i]) * slope[i] - GULLY_THRESHOLD) * smoothstep((hgt[i] - SEA_Y - 2) / 4)
                        : GULLY_POWER * (float) Math.sqrt(area[i]) * slope[i]);"""),
("""            biome = landforms && cubic(cliffy, u, v) > 0.4f ? Biome.STONY_SHORE : temperature(u, v, ground) < 0 ? Biome.SNOWY_BEACH : Biome.BEACH;""",
 """            biome = landforms && cubic(cliffy, u, v) > 0.4f ? Biome.STONY_SHORE : temperature(u, v, ground) < 0 ? Biome.SNOWY_BEACH : Biome.BEACH;
            if (variedShores && biome == Biome.BEACH && cover == Cover.NONE && beachWidth(x, z, u, v) < 0.7f) cover = Cover.GRAVEL; // shingle on exposed headlands"""),
])
p = 'src/main/java/com/realtectonics/sim/PlanetData.java'; s = open(p, encoding='utf-8').read()
assert s.count('FORMAT = 36;') == 1
s = s.replace('FORMAT = 36;', 'FORMAT = 37;').replace('36 varied volcanoes; older', '36 varied volcanoes, 37 varied beaches and no gully pits; older')
open(p, 'w', encoding='utf-8').write(s)
print("ok")
