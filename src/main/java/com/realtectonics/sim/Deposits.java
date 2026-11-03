package com.realtectonics.sim;

/**
 * From format 92: ore deposits where the planet's geology concentrated them, instead of vanilla's even spread (which the
 * generator thins to a third). Terrain.lode says which deposit a column lies in; ore() picks the block at a height. Each
 * deposit comes from what the simulation already made: extinct arc volcanoes have a porphyry copper stock under them,
 * erupting ones gold veins, old cratons banded iron beds and kimberlite pipes, basins coal seams, collision belts lapis
 * beds, and rivers wash gold downstream from the deposits they cut. Beds follow the rock layers, so they fold with them.
 */
public final class Deposits {
    private Deposits() {}

    /** A block a deposit puts in place of its host rock. KIMBERLITE and QUARTZ are the barren rock of a pipe and a vein. */
    public enum Ore { NONE, COPPER, GOLD, IRON, COAL, DIAMOND, LAPIS, REDSTONE, KIMBERLITE, QUARTZ }

    /** A kind of deposit, for the map and /tectonic: its colour and what made it. */
    public enum Kind {
        PORPHYRY(0xd07a30, "porphyry copper and gold, under an extinct volcano"),
        VEINS(0xf0d040, "gold and cinnabar (redstone) veins, from an erupting volcano's hot water"),
        IRON(0xb03020, "banded iron beds, laid in an ancient ocean on old crust"),
        COAL(0x303030, "coal seams, old swamps buried in a sedimentary basin"),
        KIMBERLITE(0x60c0e0, "a kimberlite pipe with diamonds, from deep under an old craton"),
        LAPIS(0x3050c0, "lapis beds in marble baked where continents collided"),
        PLACER(0xe0c070, "gold washed into the river gravel from a deposit upstream");

        public final int color;
        public final String what;

        Kind(int color, String what) {
            this.color = color;
            this.what = what;
        }
    }

    /** What the ground shows over a deposit, so it can be prospected. */
    public enum Sign { NONE, GOSSAN, RED_SOIL, BLUE_GROUND }

    /**
     * A column's deposits. bed: the ore of its strata field (IRON, COAL, LAPIS) or NONE. stock: distance to a porphyry
     * stock's axis over its radius (1+ off it), stockTop its top y. pipe: blocks to a kimberlite pipe's axis (MAX_VALUE
     * off it), pipeRadius its radius at the surface. vein: in a vein sheet. placer: share of river bed blocks that are gold.
     */
    public record Lode(Ore bed, float stock, int stockTop, float pipe, float pipeRadius, boolean vein, float placer) {}

    static final int STOCK_DEPTH = 160;      // blocks a porphyry stock reaches down from its top (2.4 km)
    static final float STOCK_DOME = 24;      // blocks over which its top rounds off into a dome
    static final float STOCK_CORE = 0.35f;   // of the radius: the gold-rich core
    static final float CORE_GOLD = 0.03f, CORE_COPPER = 0.08f, SHELL_COPPER = 0.05f; // shares of the stock's blocks
    static final int PIPE_DEPTH = 200;       // blocks a kimberlite pipe reaches down (3 km); it narrows to a third there
    static final float PIPE_DIAMOND = 0.008f; // share of its blue ground that is diamond
    static final int VEIN_DEPTH = 48;        // blocks under the ground veins reach: epithermal, they formed near the surface
    static final float VEIN_GOLD = 0.12f, VEIN_REDSTONE = 0.12f; // shares of a vein's blocks; the rest is quartz (calcite)
    static final float BED_IRON = 0.35f, BED_COAL = 0.5f, BED_LAPIS = 0.06f; // shares of an ore bed's blocks
    static final int GOSSAN_DEPTH = 10;      // blocks: a stock whose top lies this near the ground rusts the ground over it

    /** Whether the rock layer with this hash is one of the field's ore beds: about one bed in 17, 13 or 19. */
    static boolean bed(Ore bed, int layerHash) {
        return switch (bed) {
            case IRON -> layerHash % 17 == 0;
            case COAL -> layerHash % 13 == 1;
            case LAPIS -> layerHash % 19 == 2;
            default -> false;
        };
    }

    /**
     * The ore at height y of a column in lode l (ground: its top block; layerHash: its rock layer's hash, as the
     * generator's rock() has it; r: 0..1, a hash of the block). NONE keeps the host rock. The surface stays as it is,
     * except gold in a river's bed.
     */
    public static Ore ore(Lode l, int y, int ground, int layerHash, float r) {
        if (l.placer() > 0 && y >= ground - 1) return r < l.placer() ? Ore.GOLD : Ore.NONE;
        if (y >= ground - 1) return Ore.NONE;
        if (l.stock() < 1 && y <= l.stockTop() && y > l.stockTop() - STOCK_DEPTH) {
            float e = l.stock() / (float) Math.min(1, Math.sqrt((l.stockTop() - y + 1) / STOCK_DOME)); // a domed top
            if (e < STOCK_CORE) return r < CORE_GOLD ? Ore.GOLD : r < CORE_GOLD + CORE_COPPER ? Ore.COPPER : Ore.NONE;
            if (e < 1) return r < SHELL_COPPER ? Ore.COPPER : Ore.NONE;
        }
        if (l.pipe() < l.pipeRadius() && y > ground - PIPE_DEPTH) { // a carrot: widest at the top
            float width = l.pipeRadius() * (1 - 0.65f * (ground - y) / (float) PIPE_DEPTH);
            if (l.pipe() < width) return r < PIPE_DIAMOND ? Ore.DIAMOND : Ore.KIMBERLITE;
        }
        if (l.vein() && y > ground - VEIN_DEPTH) return r < VEIN_GOLD ? Ore.GOLD : r < VEIN_GOLD + VEIN_REDSTONE ? Ore.REDSTONE : Ore.QUARTZ;
        if (l.bed() != Ore.NONE && bed(l.bed(), layerHash))
            return r < switch (l.bed()) { case IRON -> BED_IRON; case COAL -> BED_COAL; default -> BED_LAPIS; } ? l.bed() : Ore.NONE;
        return Ore.NONE;
    }

    /** What the top block at the column's ground shows of its deposit. */
    public static Sign sign(Lode l, int ground, int layerHash) {
        if (l.stock() < 1 && l.stockTop() >= ground - GOSSAN_DEPTH) return Sign.GOSSAN; // the sulphides rusted at the surface
        if (l.pipe() < l.pipeRadius()) return Sign.BLUE_GROUND;
        if (l.bed() == Ore.IRON && bed(Ore.IRON, layerHash)) return Sign.RED_SOIL; // an iron bed crops out
        return Sign.NONE;
    }
}
