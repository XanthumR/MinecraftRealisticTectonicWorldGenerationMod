package com.realtectonics.mod;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * From format 59: vanilla's noise caves under the planet's own terrain: cheese caverns, spaghetti and noodle tunnels,
 * stone pillars and the entrances that open them to the surface. The pieces are vanilla's own registered density
 * functions, put together as its overworld does (NoiseRouterData.underground); vanilla's terrain density, which keeps
 * all but the entrances away from the surface, is stood in for by the depth below our ground. Worked out on vanilla's
 * lattice, every 4 blocks across and 8 up, and interpolated between, as its NoiseChunk does.
 */
final class Caves {
    static final double DENSITY_PER_BLOCK = 0.05; // vanilla's terrain density grows about this much per block down from the surface: entrances only in the top ~31
    static final double SURFACE_BAND = 1.5625;    // vanilla: where the terrain density is below this, only entrances open
    static final int LAVA_BELOW = -54;            // vanilla: caves lower than this fill with lava
    static final int FLOOR = 4;                   // blocks above the world's floor that stay solid over the bedrock
    static final int ROOF = 4;                    // blocks of rock kept over a cave under water, lava or ice
    private static final int CELL_XZ = 4, CELL_Y = 8, CORNERS = 16 / CELL_XZ + 1;

    private final DensityFunction spaghetti2d, roughness, entrances, noodle, pillars;
    private final NormalNoise cheese, layer;

    Caves(RegistryAccess access, RandomState random) {
        Registry<DensityFunction> functions = access.lookupOrThrow(Registries.DENSITY_FUNCTION);
        DensityFunction.Visitor wire = new DensityFunction.Visitor() { // give each noise this world's seeded sampler, as RandomState does
            @Override
            public DensityFunction apply(DensityFunction f) {
                return f;
            }

            @Override
            public DensityFunction.NoiseHolder visitNoise(DensityFunction.NoiseHolder h) {
                return new DensityFunction.NoiseHolder(h.noiseData(), random.getOrCreateNoise(h.noiseData().unwrapKey().orElseThrow()));
            }
        };
        spaghetti2d = cave(functions, "spaghetti_2d", wire);
        roughness = cave(functions, "spaghetti_roughness_function", wire);
        entrances = cave(functions, "entrances", wire);
        noodle = cave(functions, "noodle", wire);
        pillars = cave(functions, "pillars", wire);
        cheese = random.getOrCreateNoise(Noises.CAVE_CHEESE);
        layer = random.getOrCreateNoise(Noises.CAVE_LAYER);
    }

    private static DensityFunction cave(Registry<DensityFunction> functions, String name, DensityFunction.Visitor wire) {
        return functions.getValueOrThrow(ResourceKey.create(Registries.DENSITY_FUNCTION, Identifier.withDefaultNamespace("overworld/caves/" + name))).mapAll(wire);
    }

    /**
     * One chunk's cave field from y0 up: per lattice corner the parts that do not hang on the depth (cheese plus layers,
     * entrances, tunnels, pillars, noodles), so each block only adds its own depth term.
     */
    final class Field {
        final int y0, ny;
        final float[] cheesy, entrance, tunnels, pillar, noodles;

        Field(int x0, int z0, int y0, int y1) {
            this.y0 = y0;
            ny = Math.floorDiv(y1 - y0, CELL_Y) + 2;
            int n = CORNERS * CORNERS * ny;
            cheesy = new float[n];
            entrance = new float[n];
            tunnels = new float[n];
            pillar = new float[n];
            noodles = new float[n];
            Probe p = new Probe();
            for (int cy = 0; cy < ny; cy++)
                for (int cz = 0; cz < CORNERS; cz++)
                    for (int cx = 0; cx < CORNERS; cx++) {
                        int i = cx + CORNERS * (cz + CORNERS * cy);
                        p.x = x0 + cx * CELL_XZ;
                        p.y = y0 + cy * CELL_Y;
                        p.z = z0 + cz * CELL_XZ;
                        double l = layer.getValue(p.x, p.y * 8.0, p.z);
                        cheesy[i] = (float) (4 * l * l + Mth.clamp(0.27 + cheese.getValue(p.x, p.y * 0.6666666666666666, p.z), -1, 1));
                        entrance[i] = (float) entrances.compute(p);
                        tunnels[i] = (float) (spaghetti2d.compute(p) + roughness.compute(p));
                        double pl = pillars.compute(p);
                        pillar[i] = (float) (pl >= 0.03 ? pl : -1e6);
                        noodles[i] = (float) noodle.compute(p);
                    }
        }

        /** Whether block (lx, y, lz) of the chunk, depth blocks below the ground, is cave; pillars: leave vanilla's stone pillars standing. */
        boolean air(int lx, int y, int lz, int depth, boolean pillars) {
            int cx = lx / CELL_XZ, cz = lz / CELL_XZ, cy = Math.floorDiv(y - y0, CELL_Y);
            double fx = (lx % CELL_XZ) / (double) CELL_XZ, fz = (lz % CELL_XZ) / (double) CELL_XZ, fy = (y - y0 - cy * CELL_Y) / (double) CELL_Y;
            int i = cx + CORNERS * (cz + CORNERS * cy);
            double sloped = depth * DENSITY_PER_BLOCK, d;
            if (sloped < SURFACE_BAND) d = Math.min(sloped, 5 * lerp(entrance, i, fx, fy, fz)); // near the surface only the entrances
            else {
                double caves = Math.min(lerp(cheesy, i, fx, fy, fz) + Mth.clamp(1.5 - 0.64 * sloped, 0, 0.5), lerp(entrance, i, fx, fy, fz));
                d = Math.min(caves, lerp(tunnels, i, fx, fy, fz));
                if (pillars) d = Math.max(d, lerp(pillar, i, fx, fy, fz));
            }
            return Math.min(d, lerp(noodles, i, fx, fy, fz)) < 0;
        }
    }

    /** Trilinear between the 8 corners round index i: f steps +1 in x, +CORNERS in z, +CORNERS² in y. */
    private static double lerp(float[] f, int i, double fx, double fy, double fz) {
        int up = CORNERS * CORNERS;
        return Mth.lerp3(fx, fz, fy, f[i], f[i + 1], f[i + CORNERS], f[i + CORNERS + 1],
                f[i + up], f[i + up + 1], f[i + up + CORNERS], f[i + up + CORNERS + 1]);
    }

    private static final class Probe implements DensityFunction.FunctionContext {
        int x, y, z;

        @Override public int blockX() { return x; }
        @Override public int blockY() { return y; }
        @Override public int blockZ() { return z; }
    }
}
