exec(open('tools/edit_glacier.py').read().split("C = ")[0])
G = 'src/main/java/com/realtectonics/mod/TectonicChunkGenerator.java'
edit(G, [
("""            LAVA = Blocks.LAVA.defaultBlockState(), MAGMA = Blocks.MAGMA_BLOCK.defaultBlockState();""",
 """            LAVA = Blocks.LAVA.defaultBlockState(), MAGMA = Blocks.MAGMA_BLOCK.defaultBlockState(),
            PACKED_ICE = Blocks.PACKED_ICE.defaultBlockState(), BLUE_ICE = Blocks.BLUE_ICE.defaultBlockState();
    static final int BLUE_ICE_DEPTH = 6; // glacier ice this far down is squeezed clear of air bubbles"""),
("""                boolean wet = c.water() >= c.ground(), hot = c.lava() != Terrain.Column.NO_WATER;
                Surface s = surface(c, slope, wet);
                boolean frozen = c.biome() == Biome.FROZEN_OCEAN;
                int top = wet ? c.water() : hot ? c.lava() : c.ground() + (s.snowLayer ? 1 : 0);""",
 """                boolean wet = c.water() >= c.ground(), hot = c.lava() != Terrain.Column.NO_WATER, glacier = c.ice() != Terrain.Column.NO_WATER;
                Surface s = glacier ? GRAVEL : surface(c, slope, wet); // ground moraine under the ice
                boolean frozen = c.biome() == Biome.FROZEN_OCEAN;
                int top = wet ? c.water() : hot ? c.lava() : glacier ? c.ice() : c.ground() + (s.snowLayer ? 1 : 0);"""),
("""                            : hot ? LAVA
                            : SNOW;""",
 """                            : hot ? LAVA
                            : glacier ? (c.ice() - y >= BLUE_ICE_DEPTH ? BLUE_ICE : PACKED_ICE)
                            : SNOW;"""),
("""                worldSurface.update(lx, top, lz, top == c.ground() ? surfaceBlock : wet ? WATER : hot ? LAVA : SNOW);""",
 """                worldSurface.update(lx, top, lz, top == c.ground() ? surfaceBlock : wet ? WATER : hot ? LAVA : glacier ? PACKED_ICE : SNOW);"""),
("""        return (floor ? c.ground() : Math.max(c.ground(), c.water())) + 1;""",
 """        return (floor ? c.ground() : Math.max(Math.max(c.ground(), c.water()), c.ice())) + 1;"""),
("""            states[i] = y <= c.ground() ? STONE : y <= c.water() ? WATER : AIR;""",
 """            states[i] = y <= c.ground() ? STONE : y <= c.water() ? WATER : y <= c.ice() ? PACKED_ICE : AIR;"""),
])
print("ok")
