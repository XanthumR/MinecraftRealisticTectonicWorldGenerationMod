exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""            boolean lake = water != Column.NO_WATER && lakeness > SHORE_FULL;""",
 """            // From format 31 the sheet thins toward a lake across its shore band, down to a calving front a few blocks
            // high, instead of keeping its full height to the water's edge as a cliff of ice dozens of blocks tall.
            if (rampShores && !Float.isNaN(ly) && top > ly + ICE_FRONT)
                top = ly + ICE_FRONT + (top - ly - ICE_FRONT) * smoothstep((0.5f - lakeness) / 0.48f);
            boolean lake = water != Column.NO_WATER && lakeness > SHORE_FULL;"""),
("""    static final float LAKE_CUT_MAX = 48;    // blocks above its surface beyond which a lake leaves the land standing (format 30)""",
 """    static final float LAKE_CUT_MAX = 48;    // blocks above its surface beyond which a lake leaves the land standing (format 30)
    static final float ICE_FRONT = 4;        // blocks: height of a glacier's calving front above a lake (format 31)"""),
])
print("ok")
