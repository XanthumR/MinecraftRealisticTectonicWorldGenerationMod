exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""        float ridgeline = 0;
        if (massifs && natural > SEA_Y + 20) {
            float r1 = 1 - Math.abs(fine(x, z, 512, 64)), r2 = 1 - Math.abs(fine(x, z, 256, 65)), r3 = 1 - Math.abs(fine(x, z, 128, 66));
            ridgeline = (r1 * r1 + 0.5f * r2 * r2 * r1 + 0.25f * r3 * r3 * r2) / 1.75f; // finer ridges ride on the bigger ones
            float high = natural - SEA_Y, relief = Math.min(MASSIF_RELIEF, MASSIF_SHARE * high) * smoothstep((high - 20) / 60);
            natural += relief * (2 * ridgeline - 0.8f);
        }""",
 """        float ridgeline = 0;
        boolean scree = false;
        if (massifs && natural > SEA_Y + 20) {
            ridgeline = ridgeline(x, z);
            float high = natural - SEA_Y, relief = Math.min(MASSIF_RELIEF, MASSIF_SHARE * high) * smoothstep((high - 20) / 60);
            natural += relief * (2 * ridgeline - 0.8f);
            // Scree: rock falling off a wall piles up at its foot. Here a few blocks away the ridges stand far higher,
            // while the ground below is gentle: a rubble apron, ragged at its edges.
            if (folds && relief > 20) {
                double r = SCREE_REACH / (double) zoom;
                float up = 0, down = 0;
                for (int k = 0; k < 4; k++) {
                    float d = 2 * relief * (ridgeline(x + (k == 0 ? r : k == 1 ? -r : 0), z + (k == 2 ? r : k == 3 ? -r : 0)) - ridgeline);
                    up = Math.max(up, d);
                    down = Math.max(down, -d);
                }
                scree = up > SCREE_RISE && down < SCREE_RISE / 3 && fine(x, z, 16, 72) > -0.3f;
            }
        }"""),
("""    /**
     * Foothills: rolling hills""",
 """    /** Ridged massif relief, 0 in the valleys to 1 on the crests: finer ridges ride on the bigger ones. */
    private float ridgeline(double x, double z) {
        float r1 = 1 - Math.abs(fine(x, z, 512, 64)), r2 = 1 - Math.abs(fine(x, z, 256, 65)), r3 = 1 - Math.abs(fine(x, z, 128, 66));
        return (r1 * r1 + 0.5f * r2 * r2 * r1 + 0.25f * r3 * r3 * r2) / 1.75f;
    }

    /**
     * Foothills: rolling hills"""),
("""        return new Column(ground, water, lava, ice, biome, fold, volcanic, rock, cover);""",
 """        if (scree && cover == Cover.NONE && water == Column.NO_WATER && lava == Column.NO_WATER && ice == Column.NO_WATER) cover = Cover.SCREE;
        return new Column(ground, water, lava, ice, biome, fold, volcanic, rock, cover);"""),
("""    public enum Cover { NONE, SALT, GRAVEL, SINTER, SPRING, CLIFF, BANK }""",
 """    public enum Cover { NONE, SALT, GRAVEL, SINTER, SPRING, CLIFF, BANK, SCREE }"""),
("""    static final int FOOTHILL_REACH = 3;""", """    static final float SCREE_REACH = 6, SCREE_RISE = 10; // real blocks to look for a wall above; blocks it must rise in that reach
    static final int FOOTHILL_REACH = 3;"""),
])
G = 'src/main/java/com/realtectonics/mod/TectonicChunkGenerator.java'
edit(G, [
("""                if (!glacier && !wet && alpine(c.biome()) && !snowStays(slope, shaded, RealTectonics.terrain().patch(x0 + lx, z0 + lz)))""",
 """                if (!glacier && !wet && !scree(c, slope) && alpine(c.biome()) && !snowStays(slope, shaded, RealTectonics.terrain().patch(x0 + lx, z0 + lz)))"""),
("""        if ((slope >= CLIFF_STEP || c.cover() == Terrain.Cover.CLIFF && slope >= 2) && c.biome() != Biome.DESERT) return ROCK;""",
 """        if (scree(c, slope)) return ((c.fold() + c.ground()) & 3) == 0 ? SCREE_STONE : GRAVEL; // rubble under a wall, some of it boulders
        if ((slope >= CLIFF_STEP || c.cover() == Terrain.Cover.CLIFF && slope >= 2) && c.biome() != Biome.DESERT) return ROCK;"""),
("""    /** Above the tree line, where snow lies only where it can. */""",
 """    private static boolean scree(Terrain.Column c, int slope) {
        return c.cover() == Terrain.Cover.SCREE && slope < CLIFF_STEP;
    }

    /** Above the tree line, where snow lies only where it can. */"""),
("""            ROCK = new Surface(null, null, false);""",
 """            ROCK = new Surface(null, null, false),
            SCREE_STONE = new Surface(Blocks.COBBLESTONE.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), false);"""),
])
print("ok")
