package com.realtectonics.mod;

import com.realtectonics.sim.Deposits;
import com.realtectonics.sim.Terrain;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.Map;
import java.util.Set;

/**
 * From format 68: ores where the geology put them (Deposits), added after vanilla's own. Each chunk reads the rock
 * province at its middle; veins stay inside the chunk, so the result does not depend on the order chunks decorate in.
 */
final class GeologyOres {
    private static final Map<String, Block[]> ORES = Map.of( // stone ore, deepslate ore
            "copper", new Block[]{Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE},
            "gold", new Block[]{Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE},
            "iron", new Block[]{Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE},
            "coal", new Block[]{Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE},
            "diamond", new Block[]{Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE},
            "emerald", new Block[]{Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE},
            "lapis", new Block[]{Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE},
            "redstone", new Block[]{Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE});
    // The province rocks that aren't in vanilla's stone tags but hold ore here.
    private static final Set<Block> HOSTS = Set.of(Blocks.SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.CALCITE, Blocks.BASALT, Blocks.SMOOTH_BASALT);
    static final float PIPE_ORE = 0.12f; // share of a kimberlite pipe's blocks that are diamond ore
    static final int PIPE_RADIUS = 2;

    private GeologyOres() {}

    static void place(WorldGenLevel level, ChunkAccess chunk, Terrain terrain) {
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
        Terrain.Column c = terrain.column(x0 + 8, z0 + 8);
        RandomSource r = RandomSource.create(level.getSeed() ^ chunk.getPos().toLong() * 0x9E3779B97F4A7C15L);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int floor = level.getMinBuildHeight() + 5;
        for (Deposits.Deposit d : Deposits.of(c.rock())) {
            Block[] ore = ORES.get(d.ore());
            int top = Math.min(d.maxY(), c.ground() - 4); // under the soil, not breaking out at the surface
            if (top <= d.minY()) continue;
            int count = (int) d.perChunk() + (r.nextFloat() < d.perChunk() % 1 ? 1 : 0);
            for (int i = 0; i < count; i++) {
                int y = Math.max(floor, d.minY() + r.nextInt(top - d.minY()));
                switch (d.form()) {
                    case VEIN -> { // a random walk of size blocks, kept 2 blocks inside the chunk
                        int x = x0 + 2 + r.nextInt(12), z = z0 + 2 + r.nextInt(12);
                        for (int k = 0; k < d.size(); k++) {
                            set(level, pos.set(x, y, z), ore);
                            x = Math.clamp(x + r.nextInt(3) - 1, x0, x0 + 15);
                            y = Math.max(floor, y + r.nextInt(3) - 1);
                            z = Math.clamp(z + r.nextInt(3) - 1, z0, z0 + 15);
                        }
                    }
                    case SEAM -> { // a flat bed under most of the chunk, its edge ragged
                        for (int z = z0; z < z0 + 16; z++)
                            for (int x = x0; x < x0 + 16; x++)
                                if (r.nextFloat() < 0.8f)
                                    for (int k = 0; k < d.size(); k++) set(level, pos.set(x, y + k, z), ore);
                    }
                    case PIPE -> { // from the deep up to maxY, a little wider toward the top
                        int px = x0 + 4 + r.nextInt(8), pz = z0 + 4 + r.nextInt(8);
                        for (int yy = floor; yy <= top; yy++)
                            for (int dz = -PIPE_RADIUS; dz <= PIPE_RADIUS; dz++)
                                for (int dx = -PIPE_RADIUS; dx <= PIPE_RADIUS; dx++)
                                    if (dx * dx + dz * dz <= PIPE_RADIUS * PIPE_RADIUS + 1 && r.nextFloat() < PIPE_ORE) set(level, pos.set(px + dx, yy, pz + dz), ore);
                    }
                }
            }
        }
    }

    /** Ore in place of host rock only: never in air, water, caves' fluids, bedrock or another ore. */
    private static void set(WorldGenLevel level, BlockPos pos, Block[] ore) {
        BlockState b = level.getBlockState(pos);
        boolean deep = b.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
        if (deep || b.is(BlockTags.STONE_ORE_REPLACEABLES) || HOSTS.contains(b.getBlock()))
            level.setBlock(pos, (deep ? ore[1] : ore[0]).defaultBlockState(), 2);
    }
}
