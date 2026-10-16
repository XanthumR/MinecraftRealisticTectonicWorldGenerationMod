exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""            boolean standing = solidShores && ground > ly + LAKE_CUT_MAX;
            if (gentleShores && !standing && lakeness > SHORE_FULL && lakeness <= 0.5f)
                ground = Math.min(ground, Math.round(ly - 1 + LAKE_SHORE_RISE * smoothstep((0.5f - lakeness) / (0.5f - SHORE_FULL))));
            boolean flooded = !standing && (lakeness > 0.5f || simCalderas && lakeness > SHORE_FULL && ground < (int) ly);
            if (flooded) {
                ground = Math.min(ground, (int) ly - 1 - Math.max(0, Math.round((lakeness - 0.5f) * 8)));
                water = Math.max(water, (int) ly);
            } else if (!gentleShores || lakeness <= SHORE_FULL || standing) {""",
 """            // From format 31 that limit is a ramp, not a line: the higher the land stands above the lake the less of it
            // the outline cuts, so a shore climbing past the limit rises as a slope, not a wall 40 blocks high.
            float keep = rampShores ? smoothstep((ground - ly) / LAKE_CUT_MAX) : solidShores && ground > ly + LAKE_CUT_MAX ? 1 : 0;
            boolean standing = keep >= 1;
            int uncut = ground;
            if (gentleShores && !standing && lakeness > SHORE_FULL && lakeness <= 0.5f) {
                ground = Math.min(ground, Math.round(ly - 1 + LAKE_SHORE_RISE * smoothstep((0.5f - lakeness) / (0.5f - SHORE_FULL))));
                ground = Math.round(ground + keep * (uncut - ground));
            }
            boolean flooded = !standing && (lakeness > 0.5f || simCalderas && lakeness > SHORE_FULL && ground < (int) ly);
            if (flooded) {
                ground = Math.min(ground, (int) ly - 1 - Math.max(0, Math.round((lakeness - 0.5f) * 8)));
                ground = Math.round(ground + keep * (uncut - ground)); // partly kept: dry land above the water, holding it in
                if (ground < (int) ly) water = Math.max(water, (int) ly);
            } else if (!gentleShores || lakeness <= SHORE_FULL || standing) {"""),
("""    private final boolean solidShores;    // lakes never cut land standing far above them: format 30""",
 """    private final boolean solidShores;    // lakes never cut land standing far above them: format 30
    private final boolean rampShores;     // ...and cut less the higher it stands, so no wall where the limit is crossed: format 31"""),
("""        solidShores = data.format >= 30;""", """        solidShores = data.format >= 30;
        rampShores = data.format >= 31;"""),
])
p = 'src/main/java/com/realtectonics/sim/PlanetData.java'; s = open(p, encoding='utf-8').read()
assert s.count('private static final int FORMAT = 30;') == 1
s = s.replace('private static final int FORMAT = 30;', 'private static final int FORMAT = 31;').replace('no peaks over calderas or lakes; older', 'no peaks over calderas or lakes, 31 lake shores ramp up to high land; older')
open(p, 'w', encoding='utf-8').write(s)
print("ok")
