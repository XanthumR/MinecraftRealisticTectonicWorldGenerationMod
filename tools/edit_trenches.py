exec(open('tools/edit_glacier.py').read().split("C = ")[0])
P = 'src/main/java/com/realtectonics/sim/Planet.java'
edit(P, [
("""    static final float ARC_ACTIVITY = 5;          // recent subduction needed for an arc behind it""",
 """    static final float ARC_ACTIVITY = 5;          // recent subduction needed for an arc behind it
    public static final float RIDGE_ACTIVITY = 2; // recent spreading that makes a cell a ridge axis (a gap opens every 1/speed steps)"""),
("""    public final float[] collision;  // recent continent-continent collision per cell: mountain belts, folded rock""",
 """    public final float[] collision;  // recent continent-continent collision per cell: mountain belts, folded rock
    public final float[] spreading;  // recent new crust per cell: where plates pull apart, the axis of a mid-ocean ridge"""),
("""        collision = new float[w * h];""",
 """        collision = new float[w * h];
        spreading = new float[w * h];"""),
("""            if (best < 0) { // divergent: plates pulled apart, new crust wells up""",
 """            boolean spreads = best < 0;
            if (spreads) { // divergent: plates pulled apart, new crust wells up"""),
("""            collision[g] = collision[g] * COLLISION_MEMORY + (colliding ? 1 : 0);""",
 """            collision[g] = collision[g] * COLLISION_MEMORY + (colliding ? 1 : 0);
            spreading[g] = spreading[g] * SUBDUCTION_MEMORY + (spreads ? 1 : 0);"""),
])
H = 'src/main/java/com/realtectonics/sim/Hydrology.java'
edit(H, [
("""    static final float SLOPE = 2000;            // metres per cell down the continental slope""",
 """    static final float SLOPE = 2000;            // metres per cell down the continental slope
    static final float RIDGE_CREST = -2300, RIDGE_FLANK = 16; // metres at a ridge's axis; cells its flanks take to sink to the old floor
    static final float TRENCH_FLOOR = -7000, TRENCH_AXIS = 1.5f; // metres at a trench's bottom; cells it lies out from the subduction front"""),
("""            if (elevation[g] < 0) elevation[g] = Math.max(elevation[g], Planet.fromMetres(Math.min(shelf[g], -SHELF_DEPTH)));""",
 """            if (elevation[g] < 0) elevation[g] = Math.max(elevation[g], Planet.fromMetres(Math.min(shelf[g], -SHELF_DEPTH)));
        // Mid-ocean ridges and trenches. New crust joins only the plate that moved off, so its age alone makes half a
        // ridge: a ramp ending in a 2 km cliff. A real ridge stands on both flanks, deepening with the square root of
        // its distance from the axis as the crust cools (Parsons & Sclater 1977). Where old floor dives under a plate a
        // narrow trench runs just off the subduction front, 3 km below the abyss (Peru-Chile, Mariana).
        int[] axis = distanceFrom(g -> planet.spreading[g] > Planet.RIDGE_ACTIVITY);
        float[] ridge = new float[n], deep = new float[n];
        for (int g = 0; g < n; g++) {
            ridge[g] = RIDGE_CREST + (Planet.metres(Planet.OCEAN - Planet.SEA_LEVEL) - RIDGE_CREST) * (float) Math.sqrt(Math.min(1, axis[g] / RIDGE_FLANK));
            deep[g] = Math.max(0, 1 - Math.abs(trench[g] - TRENCH_AXIS) / TRENCH_AXIS);
        }
        for (int pass = 0; pass < 2; pass++) { blur(ridge); blur(deep); }
        for (int g = 0; g < n; g++)
            if (elevation[g] < 0) // ponytail: trenches cut the overriding side too, and no outer rise; plates don't say which side dives
                elevation[g] = Math.min(Math.max(elevation[g], Planet.fromMetres(ridge[g])), Planet.fromMetres(TRENCH_FLOOR * deep[g]));"""),
])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""    static final float SHALLOW_METRES = 200, SHALLOW_METRES_PER_BLOCK = 20; // shelf seas get room: their 200 m take 10 blocks""",
 """    static final float SHALLOW_METRES = 200, SHALLOW_METRES_PER_BLOCK = 20; // shelf seas get room: their 200 m take 10 blocks
    static final float ABYSS_METRES = 3000, ABYSS_METRES_PER_BLOCK = 100; // format 35: below 3 km squeezed harder, so 7 km trenches fit above bedrock"""),
("""    public final boolean foldedStrata;    // rock layers folded in collision belts, of varied thickness: format 34""",
 """    public final boolean foldedStrata;    // rock layers folded in collision belts, of varied thickness: format 34
    private final boolean deepTrenches;   // the abyss squeezed so ridges and trenches fit between sea level and bedrock: format 35"""),
("""        foldedStrata = data.format >= 34;""", """        foldedStrata = data.format >= 34;
        deepTrenches = data.format >= 35;"""),
("""        return metres > -shallow ? SEA_Y + metres / SHALLOW_METRES_PER_BLOCK : SEA_Y - shallowBlocks + (metres + shallow) / SEA_METRES_PER_BLOCK;""",
 """        if (deepTrenches && metres < -ABYSS_METRES) return SEA_Y - shallowBlocks - (ABYSS_METRES - shallow) / SEA_METRES_PER_BLOCK + (metres + ABYSS_METRES) / ABYSS_METRES_PER_BLOCK;
        return metres > -shallow ? SEA_Y + metres / SHALLOW_METRES_PER_BLOCK : SEA_Y - shallowBlocks + (metres + shallow) / SEA_METRES_PER_BLOCK;"""),
("""        return b > -shallowBlocks ? b * SHALLOW_METRES_PER_BLOCK : -shallowBlocks * SHALLOW_METRES_PER_BLOCK + (b + shallowBlocks) * SEA_METRES_PER_BLOCK;""",
 """        float abyss = shallowBlocks + (ABYSS_METRES - shallowBlocks * SHALLOW_METRES_PER_BLOCK) / SEA_METRES_PER_BLOCK; // blocks down to 3 km
        if (deepTrenches && b < -abyss) return -ABYSS_METRES + (b + abyss) * ABYSS_METRES_PER_BLOCK;
        return b > -shallowBlocks ? b * SHALLOW_METRES_PER_BLOCK : -shallowBlocks * SHALLOW_METRES_PER_BLOCK + (b + shallowBlocks) * SEA_METRES_PER_BLOCK;"""),
])
p = 'src/main/java/com/realtectonics/sim/PlanetData.java'; s = open(p, encoding='utf-8').read()
assert s.count('FORMAT = 34;') == 1
s = s.replace('FORMAT = 34;', 'FORMAT = 35;').replace('34 folded strata; older', '34 folded strata, 35 ridges and trenches (abyss squeezed); older')
open(p, 'w', encoding='utf-8').write(s)
print("ok")
