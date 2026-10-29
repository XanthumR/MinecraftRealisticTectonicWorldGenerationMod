package com.realtectonics.sim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Block-level terrain from the per-cell planet: smooth heights, detail noise, river channels, lakes, sea.
 * A pure function of (x, z), so it is thread-safe. X wraps around the planet; Z runs pole to pole.
 */
public final class Terrain {
    public static final int SEA_Y = 63, MIN_Y = -64, MAX_Y = 767;
    public static final int BLOCKS_PER_CELL = 32;
    static final float LAND_METRES_PER_BLOCK = 15;   // at mountain scale 1: a 9 km peak reaches y~680
    static final float SEA_METRES_PER_BLOCK = 40;    // oceans squeezed: 4 km deep is 100 blocks
    static final float SHALLOW_METRES = 200, SHALLOW_METRES_PER_BLOCK = 20; // shelf seas get room: their 200 m take 10 blocks
    static final float ABYSS_METRES = 3000, ABYSS_METRES_PER_BLOCK = 100; // format 35: below 3 km squeezed harder, so 7 km trenches fit above bedrock
    static final int[] DETAIL_SPACING = {256, 128, 64, 32, 16};
    static final double WARP = 1.2; // cells: bends coastlines, rivers and biome borders off the cell grid
    static final double SUMMIT = 0.4; // of a cone's radius: the part too fine for the cell grid, shaped per block
    static final float HILLS = 4;     // blocks of extra hill relief per zoom step beyond 1
    static final float WIGGLE = 12, PINCH = 0.25f, RAGGED = 1.2f; // wild rivers: blocks of twist, share of width change, blocks of ragged edge
    static final float WILD_CREEK_AREA = 60;  // smaller and every cell of a plain slope grows its own creek: a comb, not a network
    static final float CREEK_AREA = 50;  // flow where a creek begins: rivers grow out of them instead of appearing full width
    static final int SPLINE_STEPS = 8;
    static final float FALL_MIN = 8;     // blocks a reach drops before it falls at one ledge
    static final float FALL_EASE = 0.15f; // share of the drop left as gentle slope above and below the falls
    static final float FALL_RIM = 0.15f;  // of a reach: how far past the lip the banks stay at the upper level
    static final float FALL_CHANCE = 0.4f, FALL_ALWAYS = 20; // share of steep reaches with falls; every reach dropping this far
    static final float GORGE_MIN = 12;   // blocks of drop per reach that make a narrow gorge
    static final float CANYON_HEIGHT = 15, CANYON_RAIN = 400; // dry uplands (blocks above sea, mm) where rivers cut canyons
    static final int CANYON_BENCHES = 4;
    static final float SHORE_BAND = 0.12f;          // land-mask units inland that count as the shore
    static final float DEEP_SEA = -1000;            // metres: deeper seas get the deep ocean biomes
    static final float MOUNTAIN_METRES = 1000, MEADOW_METRES = 1500, JAGGED_METRES = 3000;
    static final float MASSIF_RELIEF = 120, MASSIF_SHARE = 0.45f; // blocks of peak-and-valley relief at most, and as a share of height
    static final float ICE_SPIKE_PATCH = 0.55f;       // ice spikes only where this noise peaks: rare patches on the ice
    static final float GULLY_DEPTH = 12;             // blocks: deepest gully cut, on the steepest high slopes
    static final float SCREE_RIVER = 16; // blocks from a river edge: its bars and floodplain are the river's, not a wall's rubble
    static final float SCREE_REACH = 12, SCREE_RISE = 12, SCREE_ABOVE = 40; // real blocks to look for a wall above; blocks it must rise in that reach; scree only this high above the sea
    static final float TALUS_SLOPE = 1.5f, TALUS_ABOVE = 20; // blocks up per block across a cell: steeper sheds (56°); land this far above the sea only
    static final int TALUS_PASSES = 60;
    static final float TALUS_MASSIF_RELIEF = 80, MASSIF_MEAN = 1.1f, RIDGE_CAP = 30; // format 19: smaller massif relief, centred; ridged detail capped in blocks
    static final int FOOTHILL_REACH = 3;          // cells: how far away a range still raises foothills
    static final float FOOTHILL_MAX = 24, FOOTHILL_SHARE = 0.2f; // blocks of foothill relief at most, and as a share of the range above
    static final float FOLD_HEIGHT = 30, FOLD_RISE = 24, FOLD_BELT_METRES = 1500; // blocks: fold ridge height, rise of the smoothed range between two ridges; metres: ranges this high get fold belts
    static final float STRATA_FOLD = 40, STRATA_RISE = 24, STRATA_CRUMPLE = 20; // format 34, blocks: rock layers' fold amplitude, smoothed-range rise per fold, smaller crumpling
    static final int GULLY_WIDE = 64, GULLY_NARROW = 16; // real blocks between gullies, two sizes; powers of two so they wrap
    static final int GULLY_REACH = 24;                // format 33: blocks upslope whose water a gully gathers
    static final float GULLY_POWER = 0.8f;            // format 33: blocks cut per sqrt(blocks gathered) per block of slope
    static final float GULLY_THRESHOLD = 1.5f;        // format 37: blocks of that stream power spent before water cuts at all (the soil holds)
    static final float GULLY_SPREAD = 1.1f;           // format 41: exponent on each lower neighbour's drop when water divides between them
    static final int GULLY_SMOOTH = 2;                // format 41: blocks either side the heights are averaged over (twice) before water is routed
    static final int GULLY_LOCAL = 4;                 // format 41: blocks around over which the surroundings' cut is averaged
    static final float GULLY_RELIEF = 4;              // format 41: blocks cut per block of stream power above the surroundings'
    static final float BEACH_SLOPE = 60;              // blocks the beach rises per unit of land mask; from format 37 divided by the beach's width factor
    static final float RIPARIAN_MM = 700, RIPARIAN_FLOW = 2000, RIPARIAN_REACH = 48; // format 38: rain-equivalent a big river's groundwater gives its banks; flow that counts as big; blocks its green strip reaches
    static final float FROZEN_MOST = 0.5f; // format 45: cos(60°), the shift that makes a 0 °C test mean frozen two-thirds of the year
    static final float BASE_GRADE = 0.1f;  // format 46: blocks the lowest allowed ground rises per block away from a river's bank
    static final float BASE_FULL = 16, BASE_FADE = 32; // format 61: blocks from the bank the base level holds fully, then fades out over
    static final float CRATER_HEAT = 2.5f;            // format 38: craters' radii from an active vent within which its heat keeps ice off
    static final float CHERRY_PATCH = 0.45f, BADLANDS_METRES = 500; // cherry groves are rare; deserts this high are badlands
    static final float SOFT_BADLANDS_METRES = 800; // format 44: sedimentary deserts this high are badlands
    static final float CALDERA_SHARE = 0.45f, CALDERA_SHIELDS = 0.35f, CALDERA_ISLANDS = 0.5f; // extinct arc volcanoes, shields, calderas with an island
    static final float CALDERA_RADIUS = 0.16f, CALDERA_DEPTH = 70, CALDERA_MIN = 40, CALDERA_LAKE = 0.35f; // of the footprint; blocks at most; smallest volcano; lake this far below the rim, of the depth
    static final int CALDERA_AGE = 120, CINDER_AGE = 60;  // shields older than this have worn their caldera away; younger than this still sprout cinder cones
    static final float CINDER_RADIUS = 22, CINDER_STEEP = 0.5f; // blocks across a cinder cone's base, half; height per block of radius
    static final float LAKE_SHORE_RISE = 24; // blocks the ground may rise across a lake's shore band, from the water up
    static final float LAKE_CUT_MAX = 48;    // blocks above its surface beyond which a lake leaves the land standing (format 30)
    static final float ICE_FRONT = 4;        // blocks: height of a glacier's calving front above a lake (format 31)
    static final float FAULT_SCARP = 10, FAULT_SCARP_WIDTH = 2; // blocks a fast fault's scarp stands (150 m, a pressure ridge); unzoomed blocks its face spans
    static final int SPRING_TILT = 3;     // blocks the ground round a hot spring may rise above its pool
    static final float CRATER_OWN = 4;    // blocks above its volcano's own cone the ground may stand and still be cut by its crater
    static final float ICE_EDGE = 12;     // blocks below the land the ice surface sinks at cells without ice, so glaciers thin to their edges
    static final float ICE_DRAPE = 12;    // blocks of ice at most over the rock, ~180 m at Mountains 1.0 (format 55; from 56 over the rock blurred ICE_BLUR wide)
    static final double MAGMA_REACH = 0.6; // share of an active cone's reach out to which magma breaks through its ground in patches (format 58)
    static final int ICE_BLUR = 8;        // blocks: half-width of each of the two box means the rock is blurred by under glacier ice (format 56)
    static final float ICE_SUMMER_MAX = 15; // °C: summers this warm melt any glacier tongue that reaches them
    static final float LAKE_SILL = 0.25f;   // cells: width of the rock sill between two lakes at different levels
    /**
     * Where a tile's columns are worked out in parallel: every core, at the lowest priority. On Java's shared pool, at
     * normal priority, each tile took every core for half a second, and the game's render and server threads, also at
     * normal priority, waited their turn: frames of 30-60 ms (tools/stutter.jsh: 56-60 frames over 10 ms a run; here
     * 1-2, worst ~11 ms). Tiles now yield to the game. The terrain doesn't depend on the threads.
     */
    static final java.util.concurrent.ForkJoinPool TILE_POOL = new java.util.concurrent.ForkJoinPool(
            Runtime.getRuntime().availableProcessors(), pool -> {
                java.util.concurrent.ForkJoinWorkerThread t = java.util.concurrent.ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                t.setName("Tectonic terrain " + t.getPoolIndex());
                t.setPriority(Thread.MIN_PRIORITY);
                t.setDaemon(true);
                return t;
            }, null, false);
    static final int BASE_BLOCK = 64, BASE_CACHE = 192; // real blocks per side of a shared square of plain columns (a power of two); squares kept (at most ~38 MB; 512 was no faster)
    static final int RELAX_TILE = 256, RELAX_STEPS = 23, RELAX_CACHE = 24; // blocks per rockfall tile (512 halves the margin overhead but 6 cached tiles thrash under parallel wide generation, and 16 cost ~250 MB; no whole-pipeline gain); rounds of rockfall (the tile margin is one more); tiles kept
    static final float RELAX_TALUS = 2, RELAX_RATE = 0.2f, SCREE_FALLEN = 4; // blocks up per block across that rock stands; share of the excess falling per round; blocks of rubble that make scree
    static final float RELAX_HARDNESS = 0.45f; // how much rock strength varies: the steepest stand about twice the weakest
    static final float RELAX_RATE_STABLE = 0.05f; // format 30: share of the excess per neighbour per round, stable with 8 neighbours (<= 1/16)
    static final float ICE_DDF = 2.2f;     // metres of ice melted per degree-year above freezing (6 mm a day per °C)
    static final float SCREE_PATCH = 0.1f, SCREE_FROST = 0.6f; // raggedness noise above which scree lies; how far frost lowers that bar
    static final float BRAID_FLOW = 400, BRAID_DROP = 3, MONSOON_BRAID = 0.75f; // flood-season rivers this big and gentle braid; summer rain share of a monsoon
    static final float ICE_LINE = -4;      // °C annual mean at a cell: colder, snow outlasts the summer and feeds glaciers
    static final float ICE_MELT = 0.7f;    // metres of ice melted a year per °C above that
    static final float ICE_THICK = 8, ICE_MAX = 40; // blocks of ice per (metres a year of ice flow)^0.4; thickest glacier
    static final float ICE_SHED = 5;      // format 40: blocks of that thickness ice needs before it stays: thin snowfields on open slopes melt or blow away
    static final float ICE_STEEP = 0.6f;  // format 40: blocks per block (~31 degrees) above which a cell's snow avalanches down instead of building ice
    static final float ICE_BARE = 1;      // format 40: °C summer mean at the ice surface above which the winter's snow melts off it: the ablation zone
    static final float MORAINE_HALF = 2, MORAINE_WANDER = 4; // format 40: real blocks: half-width of a medial moraine; how far it wanders off the centreline
    static final float TROUGH_SLOPE = 1, TROUGH_MAX = 1.5f; // trough half-width per block of depth (rim walls ~2 up per 1 across); most widening, inside the 3-cell lookup
    static final float MOUTH_SHORE = 0.2f; // land mask above the coastline (0.5 = deep inland) where rivers have come down to the sea
    static final float PERCHED_HOLD = 4; // blocks of level bank beside a river that runs above the land around it
    static final int BANK_RISE = 3, BANK_WIDTH = 4; // blocks above the water and out from it that a river keeps bare of soil
    static final double FLOW_RING = 40;                   // blocks: radius of the circle the lava-flow noise is read around
    static final int SPRING_AGE = 80, SPRING_GRID = 32;   // hotspot volcanoes younger than this still have hot springs; one per square at most
    static final float SPRING_CHANCE = 0.3f, GEYSER_SHARE = 0.2f, SPRING_FIELD = 0.35f;
    static final double SPRING_MARGIN = 8; // blocks: a spring and its rim stay this far inside its volcano's footprint
    static final double CALDERA_WALL = 128; // blocks inside a collapsed caldera's edge over which neighbouring cones' shaping fades out: format 63
    static final int RIFT_STEPS = 3;          // fault blocks a rift wall breaks into (format 66)
    static final float RIFT_RISER = 0.25f;    // share of each block's width its scarp spans: steep faces between broad benches
    static final float RIFT_JITTER = 0.06f;   // of the floor's drop: the faults wander instead of following its contours exactly
    static final float RIFT_STEP = 12;        // blocks each fault block drops (180 m), from format 69
    static final float VALLEY_MIN_FLOW = 0.5f, VALLEY_FULL = 20; // flow (Hydrology.flow) where a side valley starts, and where it is cut in full (format 67)
    static final float VALLEY_DEPTH = 24, VALLEY_SHARE = 0.3f; // blocks a side valley cuts at most, and as a share of the land's height above the sea
    static final float VALLEY_SLOPE = 0.6f;   // blocks its walls climb per block out from the line: ~30°, as V-shaped stream valleys stand
    static final float FAN_RAIN = 700, FAN_STEEP = 6, FAN_GENTLE = 4;   // dry mountain fronts: steep reach above, gentle below
    static final float FAN_RADIUS = 90, FAN_SLOPE = 0.12f;           // blocks out from the apex, and fall per block
    static final double FAN_SPREAD = Math.toRadians(75);             // half-angle of the cone
    static final float SHORE_FULL = 0.15f, LAKE_TAPER = 40; // lake mask above which the shore stands at the surface; blocks it falls by the outer edge
    static final int POND_GRID = 128;                        // blocks: at most one pond per square this size
    static final float POND_RAIN = 450, POND_CHANCE = 0.3f, POND_LEVEL = 2, POND_BERM = 2; // mm a year; share of squares with a pond at most; blocks a rim may tilt
    static final int KARST_GRID = 64;                       // blocks: at most one sinkhole per square this size
    static final float KARST_RAIN = 500, KARST_CHANCE = 0.35f; // mm: limestone dissolves above this; share of squares with a sinkhole at most
    static final float CENOTE_RAIN = 1200, CENOTE_SHARE = 0.4f; // wet karst: share of sinkholes that are water-filled cenotes
    static final float BRAID_WIDTH = 2, BRAID_BARS = 0.25f;   // braided meltwater rivers: how much wider, and how much of them is gravel bar
    static final float OXBOW_FLOW = 300;                      // rivers this big leave oxbow lakes on their floodplains
    static final int OXBOW_RISE = 2;                          // blocks a bank may stand above the river and still be floodplain an oxbow lies on (format 51)
    static final float CIRQUE_WIDTH = 1.4f;              // cirque bowl radius, in trough half widths
    static final float MORAINE_HEIGHT = 10;              // blocks of rubble across a valley where its glacier ended
    static final float HORN_METRES = 2500, HORN_HEIGHT = 60, HORN_RADIUS = 72; // glaciated summits above this get pyramid peaks
    static final float SALT_RAIN = 350, SALT_RISE = 3;      // mm a year below which lake shores crust with salt; blocks above the lake
    static final float DUNE_RAIN = 250, DUNE_HEIGHT = 9;    // mm a year below which sand piles into dunes this many blocks high
    static final float ATOLL_SUMMIT = 100, ATOLL_DEEPEST = -2500, ATOLL_LATITUDE = 35; // sunken hotspot volcanoes in warm seas
    static final float ATOLL_SHARE = 0.4f;             // reef ring radius, as a share of the old volcano's footprint
    static final float REEF_HALF = 5, REEF_FRONT = 12; // blocks: half width of the reef crest, and of its steep outer face
    static final float CLIFF_METRES = 250, CLIFF_HEIGHT = 30;   // hard coasts above this get cliffs up to this many blocks
    static final float STACK_REACH = 0.12f, STACK_NOISE = 0.72f; // how far out sea stacks stand, and how rare they are
    static final float BARRIER_METRES = 120;                  // soft coasts lower than this get barrier islands
    static final float BARRIER_AT = 0.22f, BARRIER_HALF = 0.05f;  // offshore, in land-mask units: the island's middle and half width
    static final float CANYON_CUT = 0.8f, CANYON_MAX = 50; // canyon depth: share of the height above CANYON_HEIGHT; deepest cut // straight pieces per river reach when measuring distance to its curve
    static final double BULGE = 0.5;   // cells a reach's curve can stray from the straight line between its cells
    /** What lies on top of the ground where it is not soil, sand or bare rock. */
    public enum Cover { NONE, SALT, GRAVEL, SINTER, SPRING, CLIFF, BANK, SCREE, POND, BARE_ICE, MORAINE } // the last two on glacier ice, from format 40

    public static final int WATER_COLOR = 0x1e46b4, LAVA_COLOR = 0xff5a14, GLACIER_COLOR = 0xc8e6f5;
    static final float GLACIER_MELT = -1;      // °C annual mean at a valley floor: colder, and today's glaciers still fill it
    static final float GLACIER_MIN_RAIN = 300; // mm: glaciers need snow to feed them
    static final float LOWLAND_BLOCKS = 20;    // below this height above the sea, rivers slow down, widen and meander
    static final float MEANDER = 2;            // cells of query wander: lowland rivers swing ~20 blocks to either side
    static final float DELTA_FLOW = 500;       // a river this big (the top ~15% of mouths) builds a delta where it meets a shallow sea
    static final float DELTA_SHELF = -150;     // metres: in deeper water the sediment slides away instead
    static final double DELTA_SPREAD = Math.toRadians(55); // half-angle of the fan
    static final int DISTRIBUTARIES = 4;
    static final float DELTA_APRON = 4;        // blocks of run per block of drop down the delta's underwater front

    /**
     * ground: top solid block. water, lava: top fluid block, or NO_WATER. ice: top of glacier ice standing on the
     * ground, or NO_WATER. fold: vertical shift of the rock layers, in blocks. volcanic: on a volcano's flanks, where
     * its own rock shows. rock: the bedrock's province.
     */
    public record Column(int ground, int water, int lava, int ice, Biome biome, int fold, boolean volcanic, Rock rock, Cover cover) {
        public static final int NO_WATER = Integer.MIN_VALUE;
    }

    public final PlanetData data;
    public final int circumference; // blocks once around the planet
    public final int zoom;          // world scale: each cell is BLOCKS_PER_CELL * zoom blocks wide
    public final int blocksPerCell;
    private final int span;         // circumference before zooming: the frame cells, warp and volcanoes live in
    private final int w, h;
    private final float[] groundY;  // per cell, in blocks
    private final float[] land;     // land/sea mask blurred over ~1.5 cells: its 0.5 contour is a smooth coastline
    private final float[] lakes;    // lake mask blurred the same way: smooth lake shores
    private final float[] lakeY;    // lake surface per cell, in blocks; NaN where there is no lake
    private final int originX, originY; // cell under block (0, 0): a spot worth spawning on
    private final long seed;
    private final float landMetresPerBlock;
    private final float shallowBlocks;   // blocks the shallow sea takes; 0 on planets saved before shelves
    private final float offshoreSlope;   // how fast the sea floor drops off the smooth coastline
    private final CellMap<Cone> conesByCell = new CellMap<>(); // every cell a cone reaches
    private final CellMap<Atoll> atollsByCell = new CellMap<>();
    private final float iceCapBelow;   // °C: older planets keep their old ice line
    private final boolean lowlandRivers;  // meanders and deltas: format 7; off on older planets, so their rivers stay put
    private final float riverWidth;
    private final boolean organic;        // curved rivers, creeks, ragged deltas, patchy biomes: format 10
    private final boolean landforms;      // waterfalls, gorges, canyons and the other landforms: format 11
    private final boolean richBiomes;     // rivers, shores, ocean temperatures and mountain zones: format 12
    private final boolean forestVariety;  // forest patches and badlands: format 13
    private final boolean gullies;        // downhill gullies on mountainsides: format 14
    private final boolean massifs;        // mountain relief, varied gullies, rare ice spikes: format 15
    private final boolean wildRivers;     // twisting, pinching, ragged rivers with more creeks: format 16
    private final boolean folds;          // foothills, fold mountains, scree and ponds: format 17
    public final boolean snowLine;        // snow and ice follow the planet's climate, not each biome's fixed snow line: format 18
    private final boolean talus;          // talus on the cells, tamer relief on high ground: format 19
    private final boolean perchedBanks;   // banks above lower land ease down past a levee instead of ending in a wall: format 20
    private final boolean calderas;       // calderas and cinder cones: format 21
    private final boolean simCalderas;    // calderas simulated into the planet's cells, not stamped per block: format 22
    private final boolean glacierFlow;    // glaciers by mass balance flowing down their valleys, troughs widening with depth: format 23
    private final boolean seasons;        // summers and winters: biomes, glacier balance, frost scree, flood-season rivers: format 24
    public final boolean gentleShores;    // lakes flood hollows instead of cutting cliffs, springs on level ground, desert edges mixed: format 25
    private final boolean ownCraters;     // craters only where their volcano is the top of the land: format 26
    private final boolean iceSheets;      // glacier ice as one smooth surface over the land, merging and sloping to its edges: format 27
    private final boolean allTroughs, nearestLakes; // every trough cuts, so overlaps blend; lakes keep their own level, sills between: format 28
    public final boolean rockfall;        // block-scale rockfall over each tile of the world (relaxedColumn): format 29
    private final boolean solidShores;    // lakes never cut land standing far above them: format 30
    private final boolean rampShores;     // ...and cut less the higher it stands, so no wall where the limit is crossed: format 31
    private final boolean faults;         // a scarp along the trace of each strike-slip fault: format 32
    private final boolean simGullies;     // gullies cut by water running down the block terrain, not a laid pattern: format 33
    public final boolean foldedStrata;    // rock layers folded in collision belts, of varied thickness: format 34
    private final boolean deepTrenches;   // the abyss squeezed so ridges and trenches fit between sea level and bedrock: format 35
    private final boolean variedVolcanoes; // each volcano its own width, steepness, lobes and collapse scar, profile in blocks: format 36
    private final boolean variedShores;   // beaches wide in bays and at river mouths, narrow and shingly on headlands; no gully pits: format 37
    private final boolean wetBanks;       // rivers green their banks; active craters melt the ice around them: format 38
    private final boolean flowingIce;     // ice gathers in valleys fed from above, steep ground sheds it, the surface slopes smoothly: format 40
    private final boolean spreadFlow;     // block-scale runoff divides between lower neighbours: no comb of grooves on even slopes: format 41
    private final byte[] iceNear;         // per cell: the most ice (Hydrology.GLACIATED, FJORD) of any glacial trough segment within 3 cells
    private final int widthMask;          // w - 1 when w is a power of two (every planet the settings make), else -1
    private final boolean softBadlands;   // badlands only on high sedimentary rock, other high deserts stay desert: format 44
    private final boolean longWinters;    // snow, lake ice and snowy taiga only where frozen two-thirds of the year, not half: format 45
    private final boolean activeSummits;  // an erupting volcano's summit buries the massif's ridges round its crater: format 45
    private final boolean baseLevel;      // block-scale relief never digs below the nearest river's surface: format 46
    public final boolean noGravel;        // no gravel on top (the user: realistic, but it looks bad in play): sand under water, coarse dirt or stone on land: format 47
    private final boolean openMouths;     // a river's channel reaches land that rounds to sea level, so no dry sand line parts it from the sea: format 48
    private final boolean noDeltas;       // no deltas (the user: they look weird, thin marsh fingers between wide channels): format 49
    private final boolean openChannels;   // a river's channel runs on past the coastline at its depth, not into the beach's ramp: format 50
    private final boolean flatOxbows;     // oxbow lakes only on low floodplain, not dug as walled pits into high banks: format 51
    private final boolean deepBraids;     // a braided river keeps the plain river's channel down its middle, on land and past the coast: format 52
    public final boolean plainGround;     // where gravel was on land, the biome's own ground, not coarse dirt (the user: remove the patches): format 53
    public final boolean stillSteps;      // a river's one-block steps are not set flowing at load, only falls of two or more: format 54
    private final boolean drapedIce;      // glacier ice at most ICE_DRAPE thick, so it lies on the mountain instead of doming over it: format 55
    private final boolean blurredIce;     // that limit over the blurred rock (relaxTile), so the ice smooths over rock steps instead of copying them as walls: format 56
    public final boolean firn;            // above the snow line glacier ice lies under snow and firn (TectonicChunkGenerator.FIRN_DEPTH): format 57
    private final boolean magmaVents;     // patches of magma on active volcanoes, thickest round the crater (magmaPatch): format 58
    public final boolean noiseCaves;      // vanilla's noise caves under the terrain (mod.Caves): format 59
    public final boolean undergroundBiomes; // dripstone under limestone, lush caves under wet climates, the deep dark under old high crust (TectonicBiomeSource): format 60
    private final boolean baseFade;       // the base level fades out away from the river instead of ending where the river search does: format 61
    private final boolean wrappedRims;    // a crater floor finds its rim across the planet's seam too (format 42 used the unwrapped offset): format 62
    private final boolean calderaWalls;   // neighbouring cones' shaping fades across a caldera's edge instead of stopping at it: format 63
    private final boolean riftScarps;     // a rift's walls step down in fault blocks instead of sloping smoothly: formats 66-69 only
    private final boolean sideValleys;    // every drainage link too small for a river cuts a V-shaped valley: format 67
    public final boolean geologyOres;     // ores where the rock's history put them (Deposits), over vanilla's: format 68
    private final boolean riftBenches;    // rift walls terraced by height, so steps stay benches where erosion softened the wall: format 69
    public final boolean fastMath;        // distances by a plain square root instead of Math.hypot (same to the last bit or two); caves sealed from water only near it: format 43
    private final boolean smoothCones;    // collapse scars of even width, crater floors meet their own rim, peaks fade over lakes, lakes one biome: format 42
    private final int coneStyle;          // Volcano.ROUND, VARIED or SMOOTH_SCARS by format
    private final float[] bay, silt;      // format 37: land mask blurred wide (above 0.5 in a bay); river sand reaching the coast
    private final java.util.concurrent.ConcurrentHashMap<Long, java.util.concurrent.CompletableFuture<Column[]>> relaxedTiles = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentLinkedQueue<Long> tileOrder = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private final java.util.concurrent.ConcurrentHashMap<Long, java.util.concurrent.CompletableFuture<Column[]>> baseBlocks = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentLinkedQueue<Long> baseOrder = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private final float[] iceTop;         // per cell: the glacier ice surface in blocks, below the ground where there is none (format 27)
    private final float[] glacier;        // per cell: today's glacier ice, blocks thick (format 23)
    private final boolean[] merged;       // per cell: ice here comes from two glaciers that joined upstream, so a medial moraine runs on it (format 40)
    private final float[] rangeY, foothill, orogen; // per cell, for folds: heavily smoothed height, how far a range nearby towers over it, collision belts blurred
    private final boolean[] glacialHead, glacialEnd; // per cell: where an Ice Age glacier began, and where it ended
    private final CellMap<Horn> hornsByCell = new CellMap<>();
    private final float[] cliffy, barriers; // per cell, blurred: hard high coasts that end in cliffs, soft low ones with barrier islands
    private final float[] riverLevel;    // per cell: river water level in blocks; below the ground where a dry upland's river cut a canyon
    private final int[] mainDonor;       // per cell: the upstream neighbour bringing the most water, or -1
    private final CellMap<Delta> deltasByCell = new CellMap<>(); // by warped cell
    private final CellMap<Fan> deltasByCellFans = new CellMap<>(); // alluvial fans, by warped cell

    /** An alluvial fan at a mountain front: apex where the valley opens, spreading toward the stream's next cell. */
    private record Fan(double u, double v, double dirU, double dirV, float radius, float apex) {}

    /** A river mouth's fan of sediment, in warped cell coordinates: apex on the last land cell, pointing out to sea. */
    private record Delta(double u, double v, double dirU, double dirV, float radius, float half) {}

    /** Where a column sits on a delta: on its plain (marsh or a channel), or on the underwater front beyond it. */
    private record DeltaSpot(boolean plain, boolean channel, float channelHalf, float beyondFront, float fromAxis) {}

    /**
     * A volcano in blocks. Its flanks are already in the planet's cells (rivers run off them); per block we only
     * sharpen the summit and cut the crater. Planets saved before that (legacy) get the whole cone per block.
     */
    private record Cone(Volcano v, double x, double z, float radius, float craterRadius, float craterDepth, float height, boolean legacy,
                        boolean caldera, float[][] cinders, boolean collapsed, float rim, float lobes, float baseY, float kept) {
        double reach() { // lobes: from format 36 a cone reaches further down some sides
            return legacy ? radius : SUMMIT * radius * lobes;
        }
    }
    public final int spawnCell;

    public Terrain(PlanetData data, long seed) {
        this(data, seed, 1, 1);
    }

    /**
     * mountainScale 1 is the default; 2 makes mountains twice as tall in blocks (and twice as steep). zoom (a power of
     * two) stretches the planet sideways: continents, coasts and valleys get wider, while rivers, small bumps and rock
     * layers keep their size in blocks.
     */
    public Terrain(PlanetData data, long seed, float mountainScale, int zoom) {
        this(data, seed, mountainScale, zoom, 1);
    }

    /** riverWidth: rivers, creeks and delta channels this many times wider in blocks. */
    public Terrain(PlanetData data, long seed, float mountainScale, int zoom, float riverWidth) {
        this.riverWidth = riverWidth;
        this.data = data;
        this.seed = seed;
        this.zoom = zoom;
        landMetresPerBlock = LAND_METRES_PER_BLOCK / mountainScale;
        boolean shelves = data.format >= 6;
        shallowBlocks = shelves ? SHALLOW_METRES / SHALLOW_METRES_PER_BLOCK : 0;
        offshoreSlope = shelves ? 16 : 60;
        lowlandRivers = data.format >= 7;
        organic = data.format >= 10;
        landforms = data.format >= 11;
        richBiomes = data.format >= 12;
        forestVariety = data.format >= 13;
        gullies = data.format >= 14;
        massifs = data.format >= 15;
        wildRivers = data.format >= 16;
        folds = data.format >= 17;
        talus = data.format >= 19;
        perchedBanks = data.format >= 20;
        calderas = data.format >= 21;
        simCalderas = data.format >= 22;
        glacierFlow = data.format >= 23;
        seasons = data.format >= 24;
        gentleShores = data.format >= 25;
        ownCraters = data.format >= 26;
        iceSheets = data.format >= 27;
        allTroughs = nearestLakes = data.format >= 28;
        rockfall = data.format >= 29;
        solidShores = data.format >= 30;
        rampShores = data.format >= 31;
        faults = data.format >= 32;
        simGullies = data.format >= 33;
        foldedStrata = data.format >= 34;
        deepTrenches = data.format >= 35;
        variedVolcanoes = data.format >= 36;
        variedShores = data.format >= 37;
        wetBanks = data.format >= 38;
        flowingIce = data.format >= 40;
        spreadFlow = data.format >= 41;
        smoothCones = data.format >= 42;
        fastMath = data.format >= 43;
        softBadlands = data.format >= 44;
        longWinters = data.format >= 45;
        activeSummits = data.format >= 45;
        baseLevel = data.format >= 46;
        noGravel = data.format >= 47;
        openMouths = data.format >= 48;
        noDeltas = data.format >= 49;
        openChannels = data.format >= 50;
        flatOxbows = data.format >= 51;
        deepBraids = data.format >= 52;
        plainGround = data.format >= 53;
        stillSteps = data.format >= 54;
        drapedIce = data.format >= 55;
        blurredIce = data.format >= 56;
        firn = data.format >= 57;
        magmaVents = data.format >= 58;
        noiseCaves = data.format >= 59;
        undergroundBiomes = data.format >= 60;
        baseFade = data.format >= 61;
        wrappedRims = data.format >= 62;
        calderaWalls = data.format >= 63;
        riftScarps = data.format >= 66 && data.format < 70; // the user took young rifts out again at 70
        sideValleys = data.format >= 67;
        geologyOres = data.format >= 68;
        riftBenches = data.format >= 69;
        widthMask = (data.w & (data.w - 1)) == 0 ? data.w - 1 : -1;
        coneStyle = smoothCones ? Volcano.SMOOTH_SCARS : variedVolcanoes ? Volcano.VARIED : Volcano.ROUND;
        snowLine = data.format >= 18;
        iceCapBelow = data.format >= 8 ? Biome.ICE_CAP_BELOW : Biome.OLD_ICE_CAP_BELOW;
        w = data.w;
        h = data.h;
        span = w * BLOCKS_PER_CELL;
        blocksPerCell = BLOCKS_PER_CELL * zoom;
        circumference = span * zoom;
        groundY = new float[w * h];
        land = new float[w * h];
        lakes = new float[w * h];
        lakeY = new float[w * h];
        for (int g = 0; g < groundY.length; g++) {
            groundY[g] = toY(data.metres[g]);
            land[g] = data.metres[g] >= 0 ? 1 : 0;
            lakes[g] = Float.isNaN(data.lakeMetres[g]) ? 0 : 1;
            lakeY[g] = (float) Math.floor(toY(data.lakeMetres[g])); // NaN stays NaN
        }
        if (talus) talus();
        for (int pass = 0; pass < 3; pass++) blur3x3(land);
        bay = variedShores ? land.clone() : null;
        silt = variedShores ? new float[w * h] : null;
        for (int g = 0; variedShores && g < w * h; g++) // rivers reaching the sea bring the sand that beaches are made of
            silt[g] = data.metres[g] >= 0 && data.receiver[g] >= 0 && data.metres[data.receiver[g]] < 0 ? Math.min(1, data.flow[g] / (8 * Hydrology.RIVER_AREA)) : 0;
        for (int pass = 0; variedShores && pass < 6; pass++) { blur3x3(bay); blur3x3(silt); }
        for (int pass = 0; pass < 2; pass++) blur3x3(lakes);
        rangeY = folds ? new float[w * h] : null;
        foothill = folds ? new float[w * h] : null;
        orogen = folds ? new float[w * h] : null;
        for (int g = 0; folds && g < w * h; g++) {
            rangeY[g] = Math.max(groundY[g], SEA_Y);
            orogen[g] = data.rock[g] == Rock.OROGEN.ordinal() || data.metres[g] > FOLD_BELT_METRES ? 1 : 0; // collision belts and every high range
            float top = groundY[g];
            for (int dy = -FOOTHILL_REACH; dy <= FOOTHILL_REACH; dy++)
                for (int dx = -FOOTHILL_REACH; dx <= FOOTHILL_REACH; dx++) top = Math.max(top, groundY[cell(g % w + dx, g / w + dy)]);
            foothill[g] = top - groundY[g];
        }
        for (int pass = 0; folds && pass < 6; pass++) blur3x3(rangeY);
        for (int pass = 0; folds && pass < 2; pass++) blur3x3(foothill);
        for (Volcano v : folds ? data.volcanoes : List.<Volcano>of()) { // a cone is high but not a fold belt: no rings around it
            int r = (int) Math.ceil(v.radiusBlocks() / BLOCKS_PER_CELL) + 1;
            for (int dy = -r; dy <= r; dy++)
                for (int dx = -r; dx <= r; dx++) orogen[cell(Math.round(v.u()) + dx, Math.round(v.v()) + dy)] = 0;
        }
        for (int pass = 0; folds && pass < 8; pass++) blur3x3(orogen); // out over the flanks
        mainDonor = new int[w * h];
        java.util.Arrays.fill(mainDonor, -1);
        for (int c = 0; c < w * h; c++) {
            int r = data.receiver[c];
            if (r >= 0 && (mainDonor[r] < 0 || data.flow[c] > data.flow[mainDonor[r]])) mainDonor[r] = c;
        }
        spawnCell = spawnCell();
        originX = spawnCell % w;
        originY = spawnCell / w;
        riverLevel = groundY.clone();
        if (landforms) canyons();
        cliffy = new float[w * h];
        barriers = new float[w * h];
        if (landforms) coastTypes();
        glacialHead = new boolean[w * h];
        glacialEnd = new boolean[w * h];
        if (landforms) glacialForms();
        iceNear = new byte[w * h]; // nearestRiver's trough search turns up nothing on most land: skip it there
        for (int c = 0; c < w * h; c++) {
            if (data.receiver[c] < 0 || data.metres[c] < 0 || data.ice[c] == 0 || data.flow[c] <= Hydrology.GLACIER_AREA) continue;
            for (int dy = -3; dy <= 3; dy++)
                for (int dx = -3; dx <= 3; dx++) {
                    int y = c / w + dy;
                    if (y >= 0 && y < h) { int k = cell(c % w + dx, y); iceNear[k] = (byte) Math.max(iceNear[k], data.ice[c]); }
                }
        }
        glacier = glacierFlow ? glaciers() : null;
        merged = flowingIce ? new boolean[w * h] : null;
        if (flowingIce) { // the rock between two joining glaciers rides on down the middle of the one they make
            int[] donors = new int[w * h];
            Integer[] high = new Integer[w * h];
            for (int g = 0; g < w * h; g++) {
                high[g] = g;
                int r = data.receiver[g];
                if (glacier[g] > 1 && r >= 0 && glacier[r] > 1) donors[r]++;
            }
            java.util.Arrays.sort(high, (a, b) -> Float.compare(data.metres[b], data.metres[a]));
            for (int g : high) {
                merged[g] |= donors[g] >= 2;
                int r = data.receiver[g];
                if (merged[g] && r >= 0 && glacier[r] > 1) merged[r] = true;
            }
        }
        iceTop = iceSheets ? new float[w * h] : null;
        for (int g = 0; iceSheets && g < w * h; g++) // the ice surface per cell: land plus ice, sinking away from the ice, down to a lake's water
            iceTop[g] = glacier[g] > 1 ? Math.min(groundY[g] + glacier[g], Float.isNaN(lakeY[g]) ? Float.MAX_VALUE : lakeY[g]) : groundY[g] - ICE_EDGE;
        // From format 40 the ice surface is smoothed over the ice itself: ice flows, so its surface slopes evenly down
        // the valley instead of doming over each cell's centre.
        for (int pass = 0; flowingIce && iceSheets && pass < 3; pass++) {
            float[] next = iceTop.clone();
            for (int g = 0; g < w * h; g++) {
                if (glacier[g] <= 1) continue;
                float sum = 0, n = 0;
                for (int dy = -1; dy <= 1; dy++)
                    for (int dx = -1; dx <= 1; dx++) {
                        int c = cell(g % w + dx, g / w + dy);
                        if (glacier[c] > 1) { sum += iceTop[c]; n++; }
                    }
                next[g] = Math.min(sum / n, Float.isNaN(lakeY[g]) ? Float.MAX_VALUE : lakeY[g]);
            }
            System.arraycopy(next, 0, iceTop, 0, next.length);
        }
        for (Volcano v : data.volcanoes) {
            if (v.gone()) continue;
            boolean legacy = Float.isNaN(v.baseMetres());
            Volcano placed = legacy ? v.withBase(toMetres(cubic(groundY, v.u(), v.v()))) : v;
            float radius = placed.radiusBlocks(), height = toY(placed.surfaceMetres(0)) - toY(placed.baseMetres());
            if (height < 8) continue;
            double cx = wrapBlocks((v.u() - originX) * BLOCKS_PER_CELL), cz = (v.v() - originY) * BLOCKS_PER_CELL;
            int key = Math.round(v.u() * 7919) ^ Math.round(v.v() * 104729);
            // Calderas: an old volcano's summit, emptied by its last great eruption, falls in: a wide flat-floored basin
            // under steep walls, filled by a lake (Crater Lake). Extinct arc volcanoes and middle-aged shields.
            boolean collapsed = simCalderas && v.caldera(); // from format 22 the simulation built it, lake and all
            boolean caldera = calderas && !simCalderas && height >= CALDERA_MIN && !v.active()
                    && hash01(key) < (v.hotspot() ? (v.age() < CALDERA_AGE ? CALDERA_SHIELDS : 0) : CALDERA_SHARE);
            float craterRadius = collapsed ? 0 : caldera ? CALDERA_RADIUS * radius : radius * (v.hotspot() ? 0.08f : 0.06f) + 5;
            float craterDepth = caldera ? Math.min(CALDERA_DEPTH, 0.3f * height) : Math.min(25, 0.1f * height);
            // Cinder cones: young volcanoes scatter small steep cones of their own over their flanks.
            boolean young = v.hotspot() ? v.age() < CINDER_AGE : v.active();
            float[][] cinders = new float[calderas && young ? 2 + (int) (4 * hash01(key + 1)) : 0][];
            for (int i = 0; i < cinders.length; i++) {
                double a = 2 * Math.PI * hash01(key + 10 + i), d = radius * (0.3 + 0.5 * hash01(key + 20 + i));
                float r = CINDER_RADIUS * (0.6f + 0.8f * hash01(key + 30 + i));
                cinders[i] = new float[]{(float) (cx + d * Math.cos(a)), (float) (cz + d * Math.sin(a)), r, CINDER_STEEP * r};
            }
            float rim = toY(placed.surfaceMetres(craterRadius)); // the lowest point of the rim holds the crater's pool
            for (int i = 0; variedVolcanoes && i < 16; i++)
                rim = Math.min(rim, toY(placed.surfaceMetres(craterRadius * Math.cos(i * Math.PI / 8), craterRadius * Math.sin(i * Math.PI / 8), coneStyle)));
            // From format 42 the cone at block scale stands only as high as erosion left it in the simulation: the ideal
            // peak laid over a summit the rivers wore down stood as a tower, its crater on top, beside the valleys.
            float baseY = toY(placed.baseMetres()), topY = toY(placed.surfaceMetres(0));
            float kept = smoothCones && !legacy ? Math.clamp((groundY[cell(Math.round(v.u()), Math.round(v.v()))] - baseY) / Math.max(1, topY - baseY), 0, 1) : 1;
            rim = baseY + kept * (rim - baseY);
            Cone cone = new Cone(placed, cx, cz, radius, craterRadius, craterDepth, height, legacy, caldera, cinders, collapsed, softCeiling(rim),
                    variedVolcanoes ? Volcano.LOBE_MAX : 1, baseY, kept);
            // Listed over the whole footprint once lava flows and hot springs reach past the summit.
            int reach = (int) Math.ceil((landforms ? cone.radius * cone.lobes : cone.reach()) / BLOCKS_PER_CELL) + 1, cu = Math.round(v.u()), cv = Math.round(v.v());
            for (int dy = -reach; dy <= reach; dy++)
                for (int dx = -reach; dx <= reach; dx++)
                    conesByCell.add(cell(cu + dx, cv + dy), cone);
        }
        for (Volcano v : landforms ? data.volcanoes : List.<Volcano>of()) {
            float summit = v.surfaceMetres(0);
            if (!v.hotspot() || v.gone() || summit > ATOLL_SUMMIT || summit < ATOLL_DEEPEST || Math.abs(Climate.latitude((int) v.v(), h)) > ATOLL_LATITUDE) continue;
            Atoll a = new Atoll(wrapBlocks((v.u() - originX) * BLOCKS_PER_CELL), (v.v() - originY) * BLOCKS_PER_CELL, ATOLL_SHARE * v.radiusBlocks());
            int reach = (int) Math.ceil((a.radius * 1.3 + REEF_HALF + REEF_FRONT) / BLOCKS_PER_CELL) + 1, cu = Math.round(v.u()), cv = Math.round(v.v());
            for (int dy = -reach; dy <= reach; dy++)
                for (int dx = -reach; dx <= reach; dx++) atollsByCell.add(cell(cu + dx, cv + dy), a);
        }
        for (int c = 0; landforms && c < w * h; c++) {
            int r = data.receiver[c], up = mainDonor[c];
            if (r < 0 || up < 0 || data.flow[c] < CREEK_AREA || data.ice[c] != 0 || data.precipitation[c] > FAN_RAIN || data.metres[r] < 0) continue;
            if (groundY[up] - groundY[c] < FAN_STEEP || groundY[c] - groundY[r] > FAN_GENTLE) continue;
            double du = wrap(r % w - c % w), dv = r / w - c / w, len = hyp(du, dv);
            Fan fan = new Fan(c % w, c / w, du / len, dv / len, FAN_RADIUS * zoom, groundY[c] + 2);
            for (int dy = -3; dy <= 3; dy++)
                for (int dx = -3; dx <= 3; dx++) deltasByCellFans.add(cell(c % w + dx, c / w + dy), fan);
        }
        for (int c = 0; lowlandRivers && !noDeltas && c < w * h; c++) { // from format 49 none: rivers meet the sea at a plain mouth
            int r = data.receiver[c];
            if (r < 0 || data.metres[c] < 0 || data.metres[r] >= 0 || data.metres[r] < DELTA_SHELF || data.flow[c] <= DELTA_FLOW
                    || data.ice[c] == Hydrology.FJORD || !Float.isNaN(data.lakeMetres[c])) continue;
            double du = wrap(r % w - c % w), dv = r / w - c / w, len = hyp(du, dv);
            float radius = Math.min(224, 64 + 2 * (float) Math.sqrt(data.flow[c])) * zoom; // reaches as many cells out to sea
            Delta delta = new Delta(c % w, c / w, du / len, dv / len, radius, riverWidth * Math.min(40, 2 + 0.12f * (float) Math.sqrt(data.flow[c])));
            int reach = (int) Math.ceil(radius / blocksPerCell) + 2;
            for (int dy = -reach; dy <= reach; dy++)
                for (int dx = -reach; dx <= reach; dx++)
                    deltasByCell.add(cell(c % w + dx, c / w + dy), delta);
        }
    }

    /** The delta this warped position lies on, if any; the one it is deepest inside wins. */
    private DeltaSpot deltaAt(double x, double z, double u, double v) {
        DeltaSpot best = null;
        float bestBeyond = Float.MAX_VALUE;
        for (Delta d : deltasByCell.get(cell((int) Math.round(u), (int) Math.round(v)))) {
            double pu = wrapCells(u - d.u) * blocksPerCell, pv = (v - d.v) * blocksPerCell;
            double along = pu * d.dirU + pv * d.dirV, across = pu * d.dirV - pv * d.dirU, r = hyp(pu, pv);
            double wander = organic ? 0.35 * (noise(x, z, 64, 14) + 0.5 * noise(x, z, 32, 20)) : 0.12 * noise(x, z, 64, 14);
            double angle = Math.atan2(across, along) + wander; // distributaries wander
            if (Math.abs(angle) > DELTA_SPREAD + 0.3) continue;
            float spread = (float) (Math.abs(angle) / DELTA_SPREAD); // a rounded lobe: the front closes in at the sides
            float ragged = organic ? 0.3f * (noise(x, z, 64, 13) + 0.5f * noise(x, z, 32, 21)) : 0.2f * noise(x, z, 64, 13);
            float front = d.radius * (1 + ragged) * (float) Math.sqrt(Math.max(0, 1 - spread * spread));
            float beyond = (float) r - front;
            if (beyond >= bestBeyond) continue;
            // Lateral distance to the nearest distributary: they fan out evenly from the apex.
            double gap = 2 * DELTA_SPREAD * 0.8 / DISTRIBUTARIES, offset = angle + DELTA_SPREAD * 0.8;
            double nearest = Math.abs(offset - gap * (Math.floor(offset / gap) + 0.5));
            float half = Math.max(2, d.half * 0.6f);
            bestBeyond = beyond;
            best = new DeltaSpot(beyond < 0 && spread <= 1, nearest * r < half, half, beyond, (float) (nearest * r));
        }
        return best;
    }

    /** Shortest east-west offset in cells, across the seam if that is shorter. */
    private double wrapCells(double du) {
        return du - w * Math.floor(du / w + 0.5);
    }

    /** A sunken volcano's reef ring, in unzoomed blocks like the cones. */
    private record Atoll(double x, double z, float radius) {}

    private List<Atoll> atollsNear(double x, double z) {
        return atollsByCell.get(cell((int) Math.floor(originX + 0.5 + x / (double) BLOCKS_PER_CELL),
                (int) Math.floor(originY + 0.5 + z / (double) BLOCKS_PER_CELL)));
    }

    private List<Cone> conesNear(double x, double z) {
        return conesByCell.get(cell((int) Math.floor(originX + 0.5 + x / (double) BLOCKS_PER_CELL),
                (int) Math.floor(originY + 0.5 + z / (double) BLOCKS_PER_CELL)));
    }

    /** Height in blocks above sea level at the default mountain scale; fixes the size of things in the planet's cells. */
    static float defaultBlocks(float metres) {
        return metres / (metres >= 0 ? LAND_METRES_PER_BLOCK : SEA_METRES_PER_BLOCK);
    }

    /** The inverse of defaultBlocks. */
    static float defaultMetres(float blocks) {
        return blocks * (blocks >= 0 ? LAND_METRES_PER_BLOCK : SEA_METRES_PER_BLOCK);
    }

    private float coneY(Cone c, double d) {
        return softCeiling(toY(c.v.surfaceMetres(d)));
    }

    /** The cone's surface at this column; from format 36 each volcano has its own shape, so the direction matters. */
    private float coneY(Cone c, double x, double z) {
        return softCeiling(c.baseY + c.kept * (toY(c.v.surfaceMetres(wrapBlocks(x - c.x), z - c.z, coneStyle)) - c.baseY));
    }

    /** Shortest east-west offset in blocks, across the seam if that is shorter. */
    private double wrapBlocks(double dx) {
        double m = (dx % span + span + span / 2.0) % span;
        return m - span / 2.0;
    }

    public float toY(float metres) {
        if (metres >= 0) return SEA_Y + metres / landMetresPerBlock;
        float shallow = shallowBlocks * SHALLOW_METRES_PER_BLOCK; // 0 on old planets: one scale all the way down
        if (deepTrenches && metres < -ABYSS_METRES) return SEA_Y - shallowBlocks - (ABYSS_METRES - shallow) / SEA_METRES_PER_BLOCK + (metres + ABYSS_METRES) / ABYSS_METRES_PER_BLOCK;
        return metres > -shallow ? SEA_Y + metres / SHALLOW_METRES_PER_BLOCK : SEA_Y - shallowBlocks + (metres + shallow) / SEA_METRES_PER_BLOCK;
    }

    public float toMetres(float y) {
        float b = y - SEA_Y;
        if (b >= 0) return b * landMetresPerBlock;
        float abyss = shallowBlocks + (ABYSS_METRES - shallowBlocks * SHALLOW_METRES_PER_BLOCK) / SEA_METRES_PER_BLOCK; // blocks down to 3 km
        if (deepTrenches && b < -abyss) return -ABYSS_METRES + (b + abyss) * ABYSS_METRES_PER_BLOCK;
        return b > -shallowBlocks ? b * SHALLOW_METRES_PER_BLOCK : -shallowBlocks * SHALLOW_METRES_PER_BLOCK + (b + shallowBlocks) * SEA_METRES_PER_BLOCK;
    }

    /**
     * The column at real block (x, z) after rockfall (format 29): the terrain's shapes are idealised curves and walls,
     * so every tile of the world is also simulated at block scale. Rock steeper than it can stand falls to the
     * ground below, round after round, until cliffs crumble into steep faces with scree aprons at their feet. Water,
     * lava, ice and the ground next to them stay put, so no lake or river leaks; rock falling into them is carried
     * away. Each round updates every block at once, so a block depends only on the land within RELAX_STEPS of it:
     * tiles computed with a margin that wide agree exactly where they meet. Older planets get column() unchanged.
     */
    public Column relaxedColumn(int x, int z) {
        if (!rockfall) return column(x, z);
        int tx = Math.floorDiv(x, RELAX_TILE), tz = Math.floorDiv(z, RELAX_TILE);
        return tile(tx, tz)[(x - tx * RELAX_TILE) + (z - tz * RELAX_TILE) * RELAX_TILE];
    }

    /** The w x h block of relaxed columns from real block (x0, z0), row by row: one tile lookup per tile, not per column. */
    public Column[] relaxedColumns(int x0, int z0, int w, int h) {
        Column[] out = new Column[w * h];
        for (int z = 0; z < h; z++)
            for (int x = 0; x < w; ) {
                int bx = x0 + x, bz = z0 + z, tx = Math.floorDiv(bx, RELAX_TILE), tz = Math.floorDiv(bz, RELAX_TILE);
                if (!rockfall) { out[x + z * w] = column(bx, bz); x++; continue; }
                Column[] tile = tile(tx, tz);
                int lx = bx - tx * RELAX_TILE, lz = bz - tz * RELAX_TILE, run = Math.min(w - x, RELAX_TILE - lx);
                System.arraycopy(tile, lx + lz * RELAX_TILE, out, x + z * w, run);
                x += run;
            }
        return out;
    }

    /**
     * Lists of things by cell (cones, atolls, horns, deltas, fans), with a bit per cell saying whether there are any:
     * most cells have none, and every column asked, boxing its cell number into a hash map lookup each time.
     */
    private static final class CellMap<T> {
        private final Map<Integer, List<T>> lists = new HashMap<>();
        private final java.util.BitSet any = new java.util.BitSet();

        void add(int cell, T thing) {
            lists.computeIfAbsent(cell, k -> new ArrayList<>()).add(thing);
            any.set(cell);
        }

        List<T> get(int cell) {
            return any.get(cell) ? lists.get(cell) : List.of();
        }
    }

    /** A relaxed tile, computed once: threads asking for one being computed wait for it instead of repeating it. */
    private Column[] tile(int tx, int tz) {
        return cached(relaxedTiles, tileOrder, RELAX_CACHE, tx, tz, () -> relaxTile(tx * RELAX_TILE, tz * RELAX_TILE));
    }

    /**
     * The plain columns (before rockfall) of a BASE_BLOCK square, computed once. A rockfall tile works on itself and a
     * wide margin, which the tiles beside it also cover: before, each computed those columns again, 2.2 times the
     * tile's own. Columns depend only on their position, so sharing them changes nothing.
     */
    private Column[] baseBlock(int bx, int bz) {
        return cached(baseBlocks, baseOrder, BASE_CACHE, bx, bz, () -> {
            Column[] b = new Column[BASE_BLOCK * BASE_BLOCK];
            int r = (int) SCREE_REACH, size = BASE_BLOCK + 2 * r;
            SurfaceGrid previous = SURFACES.get();
            SURFACES.set(new SurfaceGrid(this, bx * BASE_BLOCK - r, bz * BASE_BLOCK - r, size, new float[4 * size * size], new boolean[size * size]));
            try {
                for (int z = 0; z < BASE_BLOCK; z++)
                    for (int x = 0; x < BASE_BLOCK; x++) b[x + z * BASE_BLOCK] = column(bx * BASE_BLOCK + x, bz * BASE_BLOCK + z);
            } finally {
                SURFACES.set(previous);
            }
            return b;
        });
    }

    /** Computed once per key: threads asking for one being computed wait for it instead of repeating it. */
    private static Column[] cached(java.util.concurrent.ConcurrentHashMap<Long, java.util.concurrent.CompletableFuture<Column[]>> map,
                                   java.util.concurrent.ConcurrentLinkedQueue<Long> order, int limit, int kx, int kz,
                                   java.util.function.Supplier<Column[]> compute) {
        long key = (long) kx << 32 | (kz & 0xffffffffL);
        java.util.concurrent.CompletableFuture<Column[]> f = map.get(key);
        if (f == null) {
            java.util.concurrent.CompletableFuture<Column[]> mine = new java.util.concurrent.CompletableFuture<>();
            f = map.putIfAbsent(key, mine);
            if (f == null) {
                f = mine;
                order.add(key);
                while (order.size() > limit) { // ponytail: oldest-first eviction, not least-recently-used; fine while chunks arrive in rough order
                    Long old = order.poll();
                    if (old != null) map.remove(old);
                }
                try {
                    mine.complete(compute.get());
                } catch (RuntimeException e) {
                    map.remove(key);
                    mine.completeExceptionally(e);
                }
            }
        }
        return f.join();
    }

    /** Rockfall over the tile of real blocks starting at (x0, z0); returns its columns, row by row. */
    Column[] relaxTile(int x0, int z0) {
        int m = RELAX_STEPS + 1 + (simGullies ? GULLY_REACH + 4 : 0) + (spreadFlow ? 2 * GULLY_SMOOTH + GULLY_LOCAL : 0), n = RELAX_TILE + 2 * m; // gullies look GULLY_REACH upslope, plus smoothing, routing, widening and the cut
        Column[] cs = new Column[n * n];
        float[] hgt = new float[n * n], fallen = new float[n * n], strength = new float[n * n];
        // Blocks in parallel: the threads that need this tile are waiting on it, so it should finish fast.
        int bx0 = Math.floorDiv(x0 - m, BASE_BLOCK), bz0 = Math.floorDiv(z0 - m, BASE_BLOCK), nb = Math.floorDiv(x0 - m + n - 1, BASE_BLOCK) - bx0 + 1;
        Runnable blocks = () -> java.util.stream.IntStream.range(0, nb * nb).parallel().forEach(k -> {
            int bx = bx0 + k % nb, bz = bz0 + k / nb;
            Column[] b = baseBlock(bx, bz);
            int ox = bx * BASE_BLOCK - (x0 - m), oz = bz * BASE_BLOCK - (z0 - m); // block corner in tile coordinates
            int from = Math.max(0, -ox), to = Math.min(BASE_BLOCK, n - ox);
            for (int z = Math.max(0, -oz); z < Math.min(BASE_BLOCK, n - oz); z++)
                System.arraycopy(b, from + z * BASE_BLOCK, cs, ox + from + (oz + z) * n, to - from);
        });
        TILE_POOL.submit(blocks).join();
        for (int i = 0; i < n * n; i++) {
            hgt[i] = cs[i].ground();
            // Some rock stands steeper than other: hard beds and jointed rock vary over a few dozen blocks.
            double ux = (x0 - m + i % n) / (double) zoom, uz = (z0 - m + i / n) / (double) zoom;
            strength[i] = 1 + RELAX_HARDNESS * (0.7f * fine(ux, uz, 32, 73) + 0.3f * fine(ux, uz, 8, 74));
        }
        boolean[] fixed = new boolean[n * n]; // wet, icy or beside water: holds lakes and rivers in
        for (int i = 0; i < n * n; i++) {
            int x = i % n, z = i / n;
            if (x == 0 || z == 0 || x == n - 1 || z == n - 1) { fixed[i] = true; continue; }
            for (int dz = -1; dz <= 1 && !fixed[i]; dz++)
                for (int dx = -1; dx <= 1 && !fixed[i]; dx++) {
                    Column c = cs[i + dx + dz * n];
                    fixed[i] = c.water() != Column.NO_WATER || c.lava() != Column.NO_WATER || dx == 0 && dz == 0 && c.ice() != Column.NO_WATER;
                }
        }
        int[] off = {1, -1, n, -n, n + 1, n - 1, -n + 1, -n - 1};
        if (simGullies) hgt = gullies(hgt, fixed, cs, n, off);
        float[] limit = new float[8];
        for (int k = 0; k < 8; k++) limit[k] = RELAX_TALUS * (k < 4 ? 1 : 1.4142f);
        float[] spare = new float[n * n]; // two height buffers in turn, not a new one each round (~13 MB of garbage a tile)
        for (int step = 0; step < RELAX_STEPS; step++) {
            float[] next = spare;
            System.arraycopy(hgt, 0, next, 0, n * n);
            boolean moved = false;
            for (int i = n + 1; i < n * n - n - 1; i++) {
                if (fixed[i]) continue;
                for (int k = 0; k < 8; k++) {
                    int j = i + off[k];
                    float excess = hgt[i] - hgt[j] - limit[k] * strength[i];
                    if (excess <= 0) continue;
                    // Format 29 moved a fifth per neighbour: a pit took 160% of what it needed from 8 sides at once,
                    // overshot, and the swing grew into spires. From 30 eight sides together cannot fill past level.
                    float move = excess * (solidShores ? RELAX_RATE_STABLE : RELAX_RATE);
                    moved = true;
                    next[i] -= move;
                    if (!fixed[j]) { // into water the rubble is carried away
                        next[j] += move;
                        fallen[j] += move;
                    }
                }
            }
            spare = hgt;
            hgt = next;
            if (!moved) break; // settled: further rounds would change nothing, so tiles still agree exactly
        }
        // From format 56 glacier ice stands at most ICE_DRAPE over the rock blurred ~2 ICE_BLUR wide: over the rock itself
        // (format 55) a step in the rock under the ice, which rockfall never wears down there, came through as a wall of ice.
        float[] bed = null;
        for (int i = 0; blurredIce && bed == null && i < n * n; i++)
            if (cs[i].ice() != Column.NO_WATER) {
                bed = new float[n * n];
                for (int k = 0; k < n * n; k++) bed[k] = cs[k].ground();
                bed = boxMean(boxMean(bed, n, ICE_BLUR), n, ICE_BLUR);
            }
        Column[] tile = new Column[RELAX_TILE * RELAX_TILE];
        for (int z = 0; z < RELAX_TILE; z++)
            for (int x = 0; x < RELAX_TILE; x++) {
                int i = x + m + (z + m) * n;
                Column c = cs[i];
                int ground = Math.round(hgt[i]), ice = bed == null || c.ice() == Column.NO_WATER ? c.ice() : Math.min(c.ice(), Math.round(bed[i] + ICE_DRAPE));
                Cover cover = c.cover() == Cover.NONE && fallen[i] > SCREE_FALLEN ? Cover.SCREE : c.cover();
                tile[x + z * RELAX_TILE] = ground == c.ground() && cover == c.cover() && ice == c.ice() ? c
                        : new Column(ground, c.water(), c.lava(), ice > ground ? ice : Column.NO_WATER, c.biome(), c.fold(), c.volcanic(), c.rock(), cover);
            }
        return tile;
    }

    /**
     * From format 33: rain runs down the block terrain and cuts gullies where it gathers, instead of a pattern laid
     * over the slopes. Each block drains to its steepest lower neighbour; the water gathered from upslope and the
     * slope decide how deep it cuts (stream power), so gullies start faint below the crests, deepen downhill and join
     * like the branches of a tree. Rockfall afterwards widens each cut into a V. Only the blocks within GULLY_REACH
     * steps upslope count, so a block depends on nearby land alone and tiles still agree where they meet. Blocks
     * beside water, ice or lava, and covered ground (banks, salt, sinter, ponds), are not cut, so nothing can leak.
     */
    private float[] gullies(float[] hgt, boolean[] fixed, Column[] cs, int n, int[] off) {
        float[] smooth = hgt.clone(), dist = {1, 1, 1, 1, 1.4142f, 1.4142f, 1.4142f, 1.4142f};
        // Integer block steps give water no direction; a light blur does. From format 41 two box means 5 blocks wide:
        // the light blur left a gentle slope's staircase (a step every few blocks) in the surface, and the water
        // gathered in time with the steps.
        if (spreadFlow) smooth = boxMean(boxMean(hgt, n, GULLY_SMOOTH), n, GULLY_SMOOTH);
        for (int pass = 0; !spreadFlow && pass < 1; pass++) {
            float[] src = smooth.clone();
            for (int i = n + 1; i < n * n - n - 1; i++) {
                float sum = src[i] * 4;
                for (int k = 0; k < 8; k++) sum += src[i + off[k]] * (k < 4 ? 1 : 0.5f) / 1.5f;
                smooth[i] = sum / (4 + 4 / 1.5f + 2 / 1.5f);
            }
        }
        int[] receiver = new int[n * n];
        float[] slope = new float[n * n];
        java.util.Arrays.fill(receiver, -1);
        for (int i = n + 1; i < n * n - n - 1; i++) {
            if (i % n == 0 || i % n == n - 1) continue;
            for (int k = 0; k < 8; k++) {
                float drop = (smooth[i] - smooth[i + off[k]]) / dist[k];
                if (drop > slope[i]) { slope[i] = drop; receiver[i] = i + off[k]; }
            }
        }
        // From format 41 water spreads to every lower neighbour by how steeply each lies below (Quinn et al. 1991):
        // sent all one way, it splits an even slope into parallel lines a few blocks apart that each cut a groove, a
        // comb of steps across the hillside. Spread, it stays a sheet on even ground and gathers only in hollows.
        float[] share = spreadFlow ? new float[n * n * 8] : null;
        for (int i = n + 1; spreadFlow && i < n * n - n - 1; i++) {
            float sum = 0;
            for (int k = 0; k < 8; k++) {
                float drop = (smooth[i] - smooth[i + off[k]]) / dist[k];
                if (drop > 0) sum += share[i * 8 + k] = (float) Math.pow(drop, GULLY_SPREAD);
            }
            for (int k = 0; k < 8 && sum > 0; k++) share[i * 8 + k] /= sum;
        }
        float[] area = new float[n * n], next = new float[n * n];
        java.util.Arrays.fill(area, 1);
        // Where each block's water goes, listed once (block by block, neighbour by neighbour, the order the sums need):
        // the rounds below then skip the dry directions instead of testing all eight every round.
        int[] first = new int[n * n + 1], to = null;
        float[] part = null;
        if (spreadFlow) {
            int edges = 0;
            for (int i = 0; i < n * n * 8; i++) if (share[i] > 0) edges++;
            to = new int[edges];
            part = new float[edges];
            for (int i = 0, e = 0; i < n * n; i++) {
                first[i] = e;
                for (int k = 0; k < 8; k++) if (share[i * 8 + k] > 0) { to[e] = i + off[k]; part[e++] = share[i * 8 + k]; }
                first[i + 1] = e;
            }
        }
        for (int round = 0; round < GULLY_REACH; round++) { // each round water from one block further upslope arrives
            java.util.Arrays.fill(next, 1);
            for (int i = 0; i < n * n; i++)
                if (spreadFlow) { for (int e = first[i]; e < first[i + 1]; e++) next[to[e]] += area[i] * part[e]; }
                else if (receiver[i] >= 0) next[receiver[i]] += area[i];
            float[] done = area; // two buffers in turn, not a new one each round
            area = next;
            next = done;
        }
        float[] cut = new float[n * n], out = hgt.clone();
        for (int i = 0; i < n * n; i++)
            if (receiver[i] >= 0 && !fixed[i] && cs[i].cover() == Cover.NONE)
                cut[i] = Math.min(GULLY_DEPTH, variedShores // from 37 weak runoff cuts nothing (no pits a block deep), and waves keep beaches smooth
                        ? Math.max(0, GULLY_POWER * (float) Math.sqrt(area[i]) * slope[i] - GULLY_THRESHOLD) * smoothstep((hgt[i] - SEA_Y - 2) / 4)
                        : GULLY_POWER * (float) Math.sqrt(area[i]) * slope[i]);
        if (spreadFlow) { // a gully is lower than the ground beside it: only water gathering more than its surroundings cuts one
            float[] mean = boxMean(cut, n, GULLY_LOCAL);
            for (int i = 0; i < n * n; i++) cut[i] = Math.min(GULLY_DEPTH, GULLY_RELIEF * Math.max(0, cut[i] - mean[i]));
        }
        float[] wide = cut.clone(); // water cuts a line; its banks slump in, so the groove is a few blocks wide
        for (int i = n + 1; i < n * n - n - 1; i++) {
            if (fixed[i] || cs[i].cover() != Cover.NONE) continue;
            float sum = cut[i] * 2;
            for (int k = 0; k < 8; k++) sum += cut[i + off[k]] * (k < 4 ? 0.5f : 0.25f);
            wide[i] = Math.max(sum / 5, 0.6f * cut[i]);
        }
        cut = wide;
        for (int i = 0; i < n * n; i++) // never below the floor it drains to, so a gully runs out instead of ending in a pit
            if (cut[i] > 0) out[i] = Math.max(hgt[i] - cut[i], receiver[i] < 0 ? hgt[i] : hgt[receiver[i]] - cut[receiver[i]]);
        return out;
    }

    /**
     * From format 37: how wide the beach is, as a factor on the plain one. Waves sweep sand along the coast into bays,
     * where it piles into broad beaches, and off the headlands, which keep only a narrow strip of shingle; river
     * mouths add the sand their rivers carry down. 1 on planets before.
     */
    private float beachWidth(double x, double z, double u, double v) {
        if (!variedShores) return 1;
        float sheltered = smoothstep((cubic(bay, u, v) - 0.3f) / 0.4f), sand = Math.min(1, 4 * cubic(silt, u, v));
        return (0.4f + 2.6f * sheltered + 2 * sand) * (0.6f + 0.4f * (1 + fine(x, z, 256, 81)));
    }

    /** From format 38: 1 inside an active volcano's crater, fading to 0 at CRATER_HEAT crater radii, where ice may lie again. */
    private float craterHeat(double x, double z) {
        float heat = 0;
        for (Cone c : conesNear(x, z)) {
            if (!c.v().active() || c.collapsed() || c.craterRadius() <= 0) continue;
            double d = hyp(wrapBlocks(x - c.x()), z - c.z());
            heat = Math.max(heat, 1 - smoothstep((float) ((d - c.craterRadius()) / ((CRATER_HEAT - 1) * c.craterRadius()))));
        }
        return heat;
    }

    /**
     * From format 58: whether real block (x, z) on an active volcano is a patch of magma, where hot rock and gas break
     * through cracks and vents. Patches a few blocks across, most round the crater, fewer down to MAGMA_REACH of the cone.
     */
    public boolean magmaPatch(int realX, int realZ) {
        if (!magmaVents) return false;
        double x = realX / (double) zoom, z = realZ / (double) zoom;
        float heat = 0;
        for (Cone c : conesNear(x, z)) {
            if (!c.v().active() || c.collapsed()) continue;
            double d = hyp(wrapBlocks(x - c.x()), z - c.z());
            heat = Math.max(heat, 1 - smoothstep((float) (d / (MAGMA_REACH * c.reach()))));
        }
        return heat > 0 && fine(x, z, 8, 91) + 0.5f * fine(x, z, 4, 92) > 1.1f - 0.6f * heat;
    }

    /** Mean of f over the (2r+1) square around each point of an n x n grid; edges average what they have. */
    private static float[] boxMean(float[] f, int n, int r) {
        float[] rows = new float[n * n], out = new float[n * n];
        for (int z = 0; z < n; z++)
            for (int x = 0; x < n; x++) {
                float s = 0;
                int c = 0;
                for (int k = Math.max(0, x - r); k <= Math.min(n - 1, x + r); k++, c++) s += f[k + z * n];
                rows[x + z * n] = s / c;
            }
        for (int z = 0; z < n; z++)
            for (int x = 0; x < n; x++) {
                float s = 0;
                int c = 0;
                for (int k = Math.max(0, z - r); k <= Math.min(n - 1, z + r); k++, c++) s += rows[x + k * n];
                out[x + z * n] = s / c;
            }
        return out;
    }

    /** The column at real block (x, z). */
    public Column column(int x, int z) {
        return column(x / (double) zoom, z / (double) zoom);
    }

    /**
     * Surfaces already worked out in the square of columns this thread is building, its margin included: the scree check
     * reads the surface SCREE_REACH blocks either side, which is another column's own, so each was worked out up to five
     * times. Kept per square and thread; a surface depends only on its position, so the answer is the same.
     */
    private record SurfaceGrid(Terrain owner, int x0, int z0, int size, float[] values, boolean[] known) {}

    private static final ThreadLocal<SurfaceGrid> SURFACES = new ThreadLocal<>();

    /** surface(), from this thread's square when it holds that real block; u and v NaN: work out the warp if needed. */
    private float surfaceAt(double x, double z, double u, double v, float[] keep) {
        SurfaceGrid g = SURFACES.get();
        double rx = x * zoom, rz = z * zoom;
        int gx = (int) Math.floor(rx) - (g == null ? 0 : g.x0), gz = (int) Math.floor(rz) - (g == null ? 0 : g.z0);
        boolean inGrid = g != null && g.owner == this && rx == Math.floor(rx) && rz == Math.floor(rz) && gx >= 0 && gz >= 0 && gx < g.size && gz < g.size;
        if (inGrid && g.known[gx + gz * g.size]) {
            int i = 4 * (gx + gz * g.size);
            keep[0] = g.values[i + 1];
            keep[1] = g.values[i + 2];
            keep[2] = g.values[i + 3];
            return g.values[i];
        }
        float natural = surface(x, z, Double.isNaN(u) ? warpU(x, z) : u, Double.isNaN(v) ? warpV(x, z) : v, keep);
        if (inGrid) {
            int i = 4 * (gx + gz * g.size);
            g.values[i] = natural;
            g.values[i + 1] = keep[0];
            g.values[i + 2] = keep[1];
            g.values[i + 3] = keep[2];
            g.known[gx + gz * g.size] = true;
        }
        return natural;
    }

    /** The land's own surface at unzoomed (x, z), before rivers, glaciers and cones cut or build on it. keep: smooth height, shore, roughness. */
    private float surface(double x, double z, double u, double v, float[] keep) {
        float natural = cubic(groundY, u, v);
        // Cell coastlines are staircases. Near the coast, follow a gentle beach profile off the blurred mask instead.
        float shore = cubic(land, u, v) - 0.5f, beach = SEA_Y - 1.5f + shore * (shore > 0 ? BEACH_SLOPE / beachWidth(x, z, u, v) : offshoreSlope);
        natural = shore > 0 ? Math.max(natural, beach) : Math.min(natural, beach); // no sea pockets inland, no islets offshore
        natural = beach + smoothstep(Math.abs(shore) / 0.25f) * (natural - beach);
        float rough = detail(x, z), smooth = natural; // biomes follow the land's shape, not every bump on it
        keep[0] = smooth;
        keep[1] = shore;
        keep[2] = rough;
        float above = Math.max(0, natural - SEA_Y);
        natural += rough * (organic ? 5 + 0.06f * above : 3 + 0.05f * above); // mountains are rougher than plains
        if (organic) { // ridged noise: sharp crests and spurs on high ground instead of rounded humps
            float ridge = 1 - Math.abs(fine(x, z, 128, 26)) - 0.5f * Math.abs(fine(x, z, 64, 27));
            natural += ridge * (talus ? Math.min(0.12f * above, RIDGE_CAP) : 0.12f * above) * smoothstep(above / 60);
        }
        if (zoom > 1) { // a zoomed cell spans hills the planet's cells can't hold; none at the shore, so no islets
            above = Math.max(0, natural - SEA_Y);
            natural += (zoom - 1) * HILLS * (noise(x, z, 128, 18) + 0.5f * noise(x, z, 64, 19)) * (1 + 0.02f * above) * smoothstep(above / 12);
        }
        if (landforms && Math.abs(shore) < 0.3f) natural = coast(x, z, u, v, shore, natural);
        if (folds && natural > SEA_Y + 2) natural += foothills(x, z, u, v, natural);
        // Massifs: a range is many peaks, spurs and deep valleys, not one dome. Ridged relief at the 128-512 block
        // scale, growing with height, so plains stay calm and the highest ranges are cut the deepest.
        float ridgeline = 0;
        if (massifs && natural > SEA_Y + 20) {
            ridgeline = ridgeline(x, z);
            float high = natural - SEA_Y, relief = Math.min(talus ? TALUS_MASSIF_RELIEF : MASSIF_RELIEF, MASSIF_SHARE * high) * smoothstep((high - 20) / 60);
            natural += relief * (2 * ridgeline - (talus ? MASSIF_MEAN : 0.8f)); // from format 19 peaks and valleys around the planet's height, not above it
        }
        if (gullies && !simGullies && natural > SEA_Y + 20) natural += gullies(x, z, u, v, natural - SEA_Y) * (1 - ridgeline * ridgeline * ridgeline); // none on the crests; from 33 water cuts them (relaxTile)
        if (sideValleys && natural > SEA_Y + 4) natural = sideValleys(u, v, natural);
        if (faults) natural += faultScarp(x, z, u, v, natural);
        if (riftScarps) natural += riftScarp(x, z, u, v, natural);
        natural = softCeiling(natural);
        // From format 22 volcano summits are sharpened here, as part of the land, so rivers cut through them later
        // instead of being buried by them.
        // From format 30 not over a lake nor inside a collapsed caldera: there the simulation says the land lies low,
        // and a neighbour's ideal peak laid over it rebuilt what the collapse took away.
        // From format 42 the sharpening fades out across a lake's shore band instead of stopping at its outline, which
        // left the cone standing as a wall round the lake.
        float lakeness = cubic(lakes, u, v), overLake = smoothCones ? 1 - smoothstep((lakeness - 0.02f) / SHORE_FULL) : 1;
        // From format 63 that fades in across the caldera's wall: an active neighbour burying the massif up to the
        // caldera's edge but not inside left the old ridges standing as a wall along a circle (x 4710 z 3133).
        float inside = calderaWalls ? insideCaldera(x, z, CALDERA_WALL) : insideCaldera(x, z, 0) > 0 ? 1 : 0;
        boolean low = solidShores && (smoothCones ? inside >= 1 : lakeness > 0.02f || inside >= 1);
        float open = 1 - (calderaWalls ? inside : 0);
        for (Cone c : simCalderas && !low ? conesNear(x, z) : List.<Cone>of()) {
            double d = hyp(wrapBlocks(x - c.x), z - c.z);
            if (c.collapsed || c.legacy || d >= c.reach()) continue;
            float s = coneY(c, x, z) + rough * (1 + 0.02f * c.height), fade = open * overLake * smoothstep((float) ((c.reach() - d) / (0.1 * c.radius)));
            if (s > natural) natural += fade * (s - natural);
            // From format 45 an erupting volcano's summit buries the range it grew on: its lava covers any ridge of
            // the massif round the crater, which stood over the rim and left the crater without a lava lake.
            else if (activeSummits && c.v.active()) natural -= open * (natural - s) * (1 - smoothstep((float) ((d - 2 * c.craterRadius) / (0.3 * c.radius))));
        }
        return natural;
    }

    /**
     * From format 32: the trace of a strike-slip fault, where the simulation's plates slide past each other. Each
     * earthquake shifts the land sideways and a little up or down, so the trace is a step running straight across hills
     * and valleys. Where the fault bends one way the two sides squeeze and one rises; the other way they pull and it
     * sags, so the step faces one side, then the other, along the fault. The valley along it is the simulation's:
     * the crushed rock erodes faster (Hydrology.FAULT_GOUGE).
     */
    private float faultScarp(double x, double z, double u, double v, float natural) {
        float slip = cubic(data.faultSlip, u, v);
        if (slip < 0.01f || natural < SEA_Y) return 0;
        float d = cubic(data.faultSide, u, v) * BLOCKS_PER_CELL / FAULT_SCARP_WIDTH; // widths from the trace
        if (Math.abs(d) > 2) return 0;
        float height = FAULT_SCARP * Math.min(1, slip / Hydrology.FAULT_FULL_SLIP) * Math.clamp(2 * noise(x, z, 512, 43), -1, 1);
        return height * (smoothstep(d + 0.5f) - 0.5f) * smoothstep((natural - SEA_Y) / 8); // none on the beach
    }

    /**
     * From format 66: a rift's walls as faults. The simulation drops a rift's floor smoothly (Hydrology.shape), but the
     * crust breaks along a few normal faults running with the rift, each block dropped further than the one outside it,
     * so the wall climbs in steps: steep scarps between benches (the Gregory Rift, the Ethiopian escarpment). The drop is
     * only moved about, never deepened: the floor and the shoulders keep the simulation's heights.
     */
    private float riftScarp(double x, double z, double u, double v, float natural) {
        float down = cubic(data.riftFloor, u, v);
        if (down <= 0.01f || down >= 0.99f || natural <= SEA_Y) return 0;
        // From format 69 by height: the wall's smooth height (the planet's, without the hills and gullies on it, so the
        // faults run along the rift rather than up every gully) is terraced in RIFT_STEP blocks, strongest mid-wall.
        // Higher ground always stays higher, so it can't raise a crest over a slot. Before, the steps were laid on the floor's
        // drop as the simulation made it; where erosion had softened the wall since, each bench tilted back into the
        // wall, leaving knife-edged ribs between slots (the user: "what are these shapes").
        if (riftBenches) {
            if (cubic(lakes, u, v) > 0.01f) return 0;
            float wall = cubic(groundY, u, v) + 3 * noise(x, z, 128, 58), f = wall / RIFT_STEP, k = (float) Math.floor(f);
            float terraced = RIFT_STEP * (k + smoothstep((f - k - 0.5f) / RIFT_RISER + 0.5f));
            return 4 * down * (1 - down) * (terraced - wall) * smoothstep((natural - SEA_Y) / 8); // none on the beach
        }
        float f = Math.clamp(down + RIFT_JITTER * noise(x, z, 256, 58), 0, 1) * RIFT_STEPS, k = Math.min((float) Math.floor(f), RIFT_STEPS - 1);
        float stair = (k + smoothstep((f - k - 0.5f) / RIFT_RISER + 0.5f)) / RIFT_STEPS;
        float before = (natural - SEA_Y) / (1 - Hydrology.RIFT_SINK * down); // roughly the ground before the floor dropped
        return -Hydrology.RIFT_SINK * before * (stair - down) * smoothstep((natural - SEA_Y) / 8); // none on the beach
    }

    /**
     * From format 67: side valleys. Every drainage link of the planet (a cell's flow to its receiver) has a valley along
     * it, not only those big enough to carry a river: the small streams cut V-shaped valleys into the slopes, deeper the
     * more ground drains through them, so a range is furrowed by branching valleys instead of rising in smooth sheets.
     * Each cuts into the land as it stands, by its depth on the line and tapering to nothing depth / VALLEY_SLOPE out,
     * where valleys meet the deeper one wins; near rivers base level (format 46) keeps them above their water, and none
     * is cut over a lake.
     */
    private float sideValleys(double u, double v, float natural) {
        if (cubic(lakes, u, v) > 0.01f) return natural;
        int iu = (int) Math.round(u), iv = (int) Math.round(v);
        float cut = 0;
        for (int dy = -2; dy <= 2; dy++)
            for (int dx = -2; dx <= 2; dx++) {
                int cy = iv + dy;
                if (cy < 0 || cy >= h) continue;
                int c = cell(iu + dx, cy), r = data.receiver[c];
                float flow = data.flow[c], above = groundY[c] - SEA_Y;
                if (r < 0 || data.metres[r] < 0 || flow < VALLEY_MIN_FLOW || flow > Hydrology.RIVER_AREA || above < 8) continue; // rivers have valleys of their own
                float depth = Math.min(VALLEY_DEPTH, VALLEY_SHARE * above) * smoothstep((float) (Math.log(flow / VALLEY_MIN_FLOW) / Math.log(VALLEY_FULL / VALLEY_MIN_FLOW)));
                float reach = depth / VALLEY_SLOPE;
                double au = iu + dx, bu = au + wrap(r % w - c % w), bv = r / w, su = bu - au, sv = bv - cy;
                double t = Math.clamp(((u - au) * su + (v - cy) * sv) / (su * su + sv * sv), 0, 1);
                if (depth <= cut || hyp(u - au - t * su, v - cy - t * sv) * blocksPerCell > reach + BULGE * blocksPerCell) continue; // can't win
                // Along the curve the river would take through these cells (as in nearestRiver), so valleys bend, not kink.
                int up = mainDonor[c], down = data.receiver[r];
                double pu0 = up < 0 ? 2 * au - bu : au + wrap(up % w - c % w), pv0 = up < 0 ? 2 * cy - bv : up / w;
                double pu3 = down < 0 ? 2 * bu - au : bu + wrap(down % w - r % w), pv3 = down < 0 ? 2 * bv - cy : down / w;
                double best = Double.MAX_VALUE, prevU = au, prevV = cy;
                for (int k = 1; k <= SPLINE_STEPS; k++) {
                    double s = k / (double) SPLINE_STEPS, qu = catmullD(pu0, au, bu, pu3, s), qv = catmullD(pv0, cy, bv, pv3, s);
                    double eu = qu - prevU, ev = qv - prevV, len2 = eu * eu + ev * ev;
                    double f = len2 == 0 ? 0 : Math.clamp(((u - prevU) * eu + (v - prevV) * ev) / len2, 0, 1);
                    best = Math.min(best, hyp(u - prevU - f * eu, v - prevV - f * ev));
                    prevU = qu;
                    prevV = qv;
                }
                float off = (float) best * blocksPerCell;
                if (off < reach) cut = Math.max(cut, (depth - VALLEY_SLOPE * off) * smoothstep(1 - off / reach)); // V, rounded at the rim
            }
        return natural - cut;
    }

    /** x, z: unzoomed blocks. Large features are laid out in them; fine ones go through fine() and stay in real blocks. */
    private Column column(double x, double z) {
        return column(x, z, true);
    }

    /** ponds false: the ground as it would be without ponds, which is what a pond's rim is judged by. */
    private Column column(double x, double z, boolean ponds) {
        double u = warpU(x, z), v = warpV(x, z);
        float[] keep = new float[3];
        float natural = surfaceAt(x, z, u, v, keep), smooth = keep[0], shore = keep[1], rough = keep[2];
        // Scree: rock falling off a wall piles up at its foot. A few blocks away the ground stands far higher, while
        // on the other side it is gentle: a rubble apron, ragged at its edges.
        boolean scree = false;
        // From format 24 rock shatters most where the ground freezes and thaws through the year: more scree there.
        float screePatch = seasons ? SCREE_PATCH - SCREE_FROST * (frost(u, v, natural) - 0.5f) : SCREE_PATCH;
        if (folds && natural > SEA_Y + SCREE_ABOVE && fine(x, z, 16, 72) > screePatch) {
            double r = SCREE_REACH / (double) zoom;
            float up = 0, down = 0;
            for (int k = 0; k < 4; k++) { // four surfaces, mostly read from this square's (surfaceAt)
                double sx = x + (k == 0 ? r : k == 1 ? -r : 0), sz = z + (k == 2 ? r : k == 3 ? -r : 0);
                float d = surfaceAt(sx, sz, Double.NaN, Double.NaN, keep) - natural;
                up = Math.max(up, d);
                down = Math.max(down, -d);
            }
            scree = up > SCREE_RISE && down < up / 2;
        }
        int ground = Math.round(natural), water = Column.NO_WATER;

        int iu = (int) Math.round(u), iv = (int) Math.round(v);
        River river = riverAt(x, z, u, v);
        // From format 46, base level: no valley is cut below the water it drains to. The block-scale relief (ridges,
        // massif valleys, hills) knows nothing of rivers, and dug hollows beside them deeper than their water, leaving
        // rivers perched on thin levees above their own valley. Relief may still lower the land, but not below the
        // nearest river's surface plus a gentle grade away from it, nor below the planet's own surface.
        // From format 61 the raise fades out from BASE_FULL to BASE_FULL + BASE_FADE blocks off the bank: it ran out of
        // the river's reach in full, so a creek above a valley stood the land up to a flat table ending in a cliff.
        if (baseLevel && river != NO_RIVER && river.y >= SEA_Y) {
            float floor = Math.min(smooth, river.y + 1 + BASE_GRADE * Math.max(0, river.edge));
            if (baseFade && floor > ground) floor = ground + (floor - ground) * (1 - smoothstep((river.edge - BASE_FULL) / BASE_FADE));
            ground = Math.max(ground, Math.round(floor));
        }
        River valley = nearestRiver(u, v, Hydrology.GLACIATED, Hydrology.GLACIER_AREA);
        int ry = Math.max((int) Math.floor(river.y), SEA_Y - 1), vy = Math.max((int) Math.floor(valley.y), SEA_Y - 1), ice = Column.NO_WATER;
        // Ice Age glaciers left U-shaped troughs: flat floors, walls steepening upwards. Where they cut below today's
        // sea, it flooded them into fjords. In the coldest valleys glaciers still fill the trough.
        // Fjords are looked up on their own, so a side valley's higher floor never fills a fjord in.
        boolean inFjord = false;
        Cover cover = Cover.NONE;
        if (valley != NO_RIVER) {
            float width = 1 + 0.3f * noise(x, z, 256, 10), t = troughFraction(valley, width);
            if (glacierFlow && valley != NO_RIVER) { // a deep trough is a wide one: walls steep, not sheer
                float wide = Math.min(TROUGH_MAX, Math.max(1, TROUGH_SLOPE * (ground - vy - 1) / (troughWidth(valley) * width)));
                t /= wide;
            }
            // Cirque: where a glacier began, a wider bowl under a steep headwall instead of the trough's rounded end.
            if (landforms && valley.t == 0 && glacialHead[valley.cell]) {
                float bowl = t / CIRQUE_WIDTH;
                if (bowl < 1) ground = Math.min(ground, Math.round(vy + 1 + (ground - vy - 1) * bowl * bowl * bowl));
            }
            // Terminal moraine: a ridge of rubble across the valley where the Ice Age glacier ended; the river cuts it.
            if (landforms && t < 1 && valley.t > 0.75f && glacialEnd[valley.cell]) {
                float ridge = MORAINE_HEIGHT * smoothstep(1 - Math.abs(valley.t - 0.88f) / 0.1f) * (1 - t * t);
                if (ridge >= 1) {
                    ground += Math.round(ridge);
                    cover = Cover.GRAVEL;
                }
            }
            if (allTroughs) ground = Math.min(ground, troughCut(u, v, ground, width)); // every trough near, so overlaps blend
            if (t < 1) {
                if (!allTroughs) ground = Math.min(ground, trough(vy + 1, ground, t));
                float thick = glacierFlow ? glacier[valley.cell] + valley.t * (glacier[Math.max(0, data.receiver[valley.cell])] - glacier[valley.cell])
                        : Math.min(30, (GLACIER_MELT - temperature(u, v, vy + 1)) * 6);
                if (!iceSheets && valley.ice != Hydrology.FJORD && thick > 1 && (glacierFlow || bilinear(data.precipitation, u, v) >= GLACIER_MIN_RAIN))
                    ice = Math.round(vy + 1 + thick * (1 - 0.5f * t * t)); // a little convex, like a real glacier's surface
            }
            River fjord = nearestRiver(u, v, Hydrology.FJORD, Hydrology.GLACIER_AREA);
            float tf = troughFraction(fjord, width);
            if (tf < 1) {
                inFjord = true;
                ground = Math.min(ground, trough(SEA_Y - 2 - Math.min(40, 16 + 0.1f * (float) Math.sqrt(fjord.flow)), ground, tf));
            }
        }
        float bank = 8 + 2 * river.half, drop = river.top - river.bottom;
        boolean gorge = false, canyon = false;
        if (landforms && !inFjord && river != NO_RIVER) {
            // Waterfalls: a steep reach drops at one ledge instead of a block at a time. Its valley floor is lifted above
            // the ledge and cut down below it, fading out across three valley widths, so the pool above stays enclosed.
            boolean falls = drop >= FALL_MIN && (hash01(river.cell) < FALL_CHANCE || drop >= FALL_ALWAYS);
            if (falls && river.edge < 2 * bank) {
                // The banks keep the upper level a little past the lip: a rock rim beside the falls, so the pool above
                // never spills sideways where the lip line crosses the block grid at a slant. The lip itself curves
                // upstream in the middle, where the water wears it back fastest: a horseshoe, not a weir.
                float side = Math.clamp((river.edge + river.half) / (2 * bank), 0, 1);
                float ledge = 0.35f + 0.3f * hash01(river.cell + 1) - 0.12f * (1 - side * side) + 0.05f * noise(x, z, 32, 30)
                        + (river.edge > 0 ? FALL_RIM : 0), ease = FALL_EASE * drop;
                ledge = Math.clamp(ledge, 0.15f, 0.9f);
                float level = river.t < ledge ? river.top - ease * river.t / ledge : river.bottom + ease * (1 - river.t) / (1 - ledge);
                float shift = (level - river.y) * (1 - smoothstep((river.edge - bank) / bank));
                natural += shift;
                ground += Math.round(shift);
                ry = Math.max((int) Math.floor(level), SEA_Y - 1);
            }
            gorge = drop >= GORGE_MIN;
            canyon = !gorge && river.incision > 3;
            if (gorge) bank = 4 + 1.2f * river.half;   // narrow, walls straight up from the water
            if (canyon) bank = 12 + 2 * river.half + 1.5f * river.incision; // wide, stepping down in benches
        }
        // Alluvial fans: where a stream leaves its mountain valley for the dry plain it drops its gravel in a cone.
        if (landforms) for (Fan fan : deltasByCellFans.get(cell(iu, iv))) {
            double pu = wrapCells(u - fan.u) * blocksPerCell, pv = (v - fan.v) * blocksPerCell, d = hyp(pu, pv);
            double angle = Math.abs(Math.atan2(pu * fan.dirV - pv * fan.dirU, pu * fan.dirU + pv * fan.dirV));
            if (d >= fan.radius || angle > FAN_SPREAD) continue;
            float top = fan.apex - FAN_SLOPE * (float) d - 6 * smoothstep((float) (angle / FAN_SPREAD)) * (float) (d / fan.radius);
            if (top > ground) {
                ground = Math.round(top);
                natural = Math.max(natural, top);
                cover = Cover.GRAVEL;
            }
        }
        boolean inRiver = false;
        // Braided rivers: below the old glaciers, meltwater rivers spread wide and shallow over their gravel.
        boolean braided = landforms && river != NO_RIVER && (data.ice[river.cell] != 0 || seasons && seasonalFloods(river))
                && ice == Column.NO_WATER && !(iceSheets && glacier[river.cell] > 1) && !inFjord && !gorge; // braids start below the ice
        if (braided) {
            river = river.widened(BRAID_WIDTH);
            bank = 8 + 2 * river.half;
        }
        // From format 22 a river is at sea level across the coastal strip: a steep coastal river drops to the sea in a
        // fall where it reaches the strip, instead of running perched beside a dry beach.
        int bankRy = ry; // the banks keep the river's own height, so the water above the fall stays enclosed
        if (simCalderas && shore < MOUTH_SHORE) ry = SEA_Y - 1;
        if (!inFjord && river.edge < bank && natural >= SEA_Y - 1) { // at the mouth the sea takes over
            float low = landforms ? lowland(u, v) : 0;
            if (river.edge <= 0 && braided) { // shallow channels threading between gravel bars
                boolean bar = fine(x, z, 16, 43) + 0.5f * fine(x, z, 64, 44) > BRAID_BARS;
                // From format 52 the plain river's channel runs on down the middle: braiding starts at a cell's edge,
                // and a bed a block deep there met the channel above in a wall. Bars stand only on the shallows beside it.
                int core = deepBraids ? channelDepth(x, z, river.edge + river.half, river.half / BRAID_WIDTH) : 0;
                ground = Math.min(ground, core > 0 ? ry - 1 - core : bar ? ry : ry - 1);
                water = ry;
                cover = Cover.GRAVEL;
                inRiver = true;
            } else if (river.edge <= 0) {
                ground = Math.min(ground, ry - 1 - (organic ? bedDepth(x, z, river.edge + river.half, river.half) : (int) Math.min(5, river.half / 4)));
                water = ry;
                inRiver = true;
            } else { // valley sides ease from just above the water back to the natural slope
                // The bank starts flush with the water (one block up on planets from before), then eases to the land.
                // A steep river climbs block by block, so its bank keeps a lip that high, or the water above spills over.
                float e = river.edge / bank, s = gorge ? (float) Math.sqrt(e) : canyon ? benches(e) : smoothstep(e);
                int lip = landforms ? bankRy + Math.round(1.5f * drop / blocksPerCell * zoom) : ry + 1;
                // Never down from the water: where the land around lies lower, the bank is a flat floodplain at the water.
                // From format 20 that floodplain is only a levee: past PERCHED_HOLD blocks it eases down to the land.
                float down = perchedBanks ? smoothstep((river.edge - PERCHED_HOLD) / Math.max(1, bank - PERCHED_HOLD)) : 0;
                ground = Math.round(lip + (ground >= lip || !massifs ? s : down) * (ground - lip));
                if (landforms && (gorge || canyon)) cover = Cover.CLIFF;                       // walls that stand as bare rock
                else if (landforms && ground <= ry + BANK_RISE && river.edge < BANK_WIDTH) cover = Cover.BANK; // sand, gravel or mud at the water's edge
                // Oxbow lakes: loops the meandering river cut off, left as crescents of still water on its floodplain.
                if (low > 0.5f && river.flow > OXBOW_FLOW && river.edge > 3 && river.edge < 0.8f * bank && fine(x, z, 64, 48) > 0.35f
                        && (!flatOxbows || ground <= ry + OXBOW_RISE) // from format 51 only on the floodplain, not cut into a cliff
                        && oxbowAt(x, z, u, v).edge < 0) {
                    ground = ry - 2;
                    water = ry;
                }
            }
        }
        // From format 48 land the river left to the sea (just under its surface) but that rounds up to it is sea floor:
        // the sea floods only below its surface, so that half block stood as a dry line of sand between river and sea.
        // From format 50 the channel runs on past the coastline as deep as it was: the beach's ramp rises to the
        // surface at the coast, so a deep river ended against an underwater wall across its mouth.
        // A braided river's is the plain river's channel down its middle (from format 52; before, as deep as its widened shape).
        else if (openChannels && !inFjord && river.edge <= 0)
            ground = Math.min(ground, SEA_Y - 2 - (braided && deepBraids ? channelDepth(x, z, river.edge + river.half, river.half / BRAID_WIDTH)
                    : organic ? bedDepth(x, z, river.edge + river.half, river.half) : (int) Math.min(5, river.half / 4)));
        else if (openMouths && !inFjord && river.edge < BANK_WIDTH && ground == SEA_Y - 1) ground = SEA_Y - 2;

        // Deltas: the river drops its sediment as a low fan of marsh pushed out into the sea, crossed by channels.
        boolean marsh = false;
        DeltaSpot delta = lowlandRivers && !inFjord ? deltaAt(x, z, u, v) : null;
        if (delta != null && ground <= SEA_Y + 2) {
            if (delta.plain && delta.channel) {
                ground = Math.min(ground, SEA_Y - 2 - (organic ? bedDepth(x, z, delta.fromAxis, delta.channelHalf) : (int) (delta.channelHalf / 4)));
                water = SEA_Y - 1;
            } else if (delta.plain) {
                ground = Math.max(ground, fine(x, z, 32, 15) > 0.45f ? SEA_Y - 2 : SEA_Y); // pools among the reeds
                marsh = true;
            } else {
                ground = Math.max(ground, SEA_Y - 2 - Math.round(delta.beyondFront / DELTA_APRON));
            }
        }

        // Lakes: shores follow the blurred mask, and everything near a lake stands at least as high as its
        // surface, so lake water is always enclosed.
        float lakeness = cubic(lakes, u, v), rain = bilinear(data.precipitation, u, v);
        float ly = lakeness > 0.02f ? lakeLevel(iu, iv) : Float.NaN, sill = Float.NaN;
        if (nearestLakes && !Float.isNaN(ly)) {
            float[] near = nearestLake(u, v);
            ly = near[0];
            sill = near[1];
        }
        if (!Float.isNaN(sill)) ground = Math.max(ground, Math.round(sill)); // the rock lip holding a higher lake above a lower one
        else if (!Float.isNaN(ly)) {
            // From format 22 water also fills any hollow below the surface inside the shore ring, so a lake one or
            // two cells across (a caldera's) is water, not a flat floor raised to its level.
            // From format 25 the shore band slopes: the ground there may rise from the water's edge up to
            // LAKE_SHORE_RISE blocks across the band, so a hill reaching the lake meets it as a slope, not a cut cliff.
            // From format 30 the lake's blurred outline, drawn on 32-block cells, may only cut land that barely rises
            // above it; a volcano or a range standing inside the outline is the lake's shore, not cut off along it.
            // From format 31 that limit is a ramp, not a line: the higher the land stands above the lake the less of it
            // the outline cuts, so a shore climbing past the limit rises as a slope, not a wall 40 blocks high.
            float kept = rampShores ? smoothstep((ground - ly) / LAKE_CUT_MAX) : solidShores && ground > ly + LAKE_CUT_MAX ? 1 : 0;
            boolean standing = kept >= 1;
            int uncut = ground;
            if (gentleShores && !standing && lakeness > SHORE_FULL && lakeness <= 0.5f) {
                ground = Math.min(ground, Math.round(ly - 1 + LAKE_SHORE_RISE * smoothstep((0.5f - lakeness) / (0.5f - SHORE_FULL))));
                ground = Math.round(ground + kept * (uncut - ground));
            }
            boolean flooded = !standing && (lakeness > 0.5f || simCalderas && lakeness > SHORE_FULL && ground < (int) ly);
            if (flooded) {
                ground = Math.min(ground, (int) ly - 1 - Math.max(0, Math.round((lakeness - 0.5f) * 8)));
                ground = Math.round(ground + kept * (uncut - ground)); // partly kept: dry land above the water, holding it in
                if (ground < (int) ly) water = Math.max(water, (int) ly);
            } else if (!gentleShores || lakeness <= SHORE_FULL || standing) {
                // The shore ring stands at the surface; past it the ground slopes down instead of ending in a wall
                // where an outlet canyon has cut the land below the lake.
                float taper = folds ? LAKE_TAPER * smoothstep((SHORE_FULL - lakeness) / (SHORE_FULL - 0.02f)) : 0;
                ground = Math.max(ground, Math.round(ly - taper));
                // Salt flats: a desert lake shrinks and leaves its old bed as a flat white crust around it.
                if (landforms && rain < SALT_RAIN && lakeness > 0.08f && ground <= ly + SALT_RISE && (!folds || ground >= ly - SALT_RISE)) {
                    ground = (int) ly;
                    cover = Cover.SALT;
                }
            }
        }
        // Dune seas in the driest deserts: long ridges across the wind (east-west here, so crests run north-south).
        if (landforms && water == Column.NO_WATER && rain < DUNE_RAIN && ground > SEA_Y + 1 && cover == Cover.NONE) {
            float erg = smoothstep((DUNE_RAIN - rain) / 100) * smoothstep(noise(x, z, 512, 40) * 2 + 0.3f);
            if (erg > 0) {
                // x runs 4 times faster through a 4 times wider lattice: 32-block spacing east-west, 128 north-south,
                // and the lattice still repeats exactly around the planet. Cubed, so crests are sharp and troughs broad.
                float crest = 1 - Math.abs(noise(4 * x, z, 128, 41)), small = 1 - Math.abs(fine(x, z, 16, 42));
                ground += Math.round(erg * (DUNE_HEIGHT * crest * crest * crest + 1.5f * small));
            }
        }
        // Volcano summits, sharper than the cell grid can hold, and their craters. Craters hold lava while
        // active, rainwater once extinct; the pool sits well below the rim, so the crater always encloses it.
        int lava = Column.NO_WATER;
        boolean volcanic = false, crater = false; // crater: a crater or caldera holds this column's lake or lava
        for (Cone c : conesNear(x, z)) {
            double d = hyp(wrapBlocks(x - c.x), z - c.z);
            if (d >= (landforms ? c.radius + SPRING_MARGIN : c.reach())) continue;
            float rim = c.rim, pool = rim - c.craterDepth * (c.caldera ? CALDERA_LAKE : 0.6f), wobble = rough * (1 + 0.02f * c.height);
            if (d < c.craterRadius && c.caldera) {
                // A flat floor under walls that steepen up to the rim; maybe a young cone rising from the lake as an island.
                float wall = smoothstep((float) (d / c.craterRadius - 0.7f) / 0.3f), floor = rim - c.craterDepth;
                ground = Math.round(floor + c.craterDepth * wall * wall + wobble * (0.3f + 0.7f * wall));
                if (hash01(Math.round(c.v.u() * 7919) ^ Math.round(c.v.v() * 104729) + 5) < CALDERA_ISLANDS) {
                    double ia = 2 * Math.PI * hash01(Math.round(c.v.u() * 31) + 7), ix = c.x + 0.4 * c.craterRadius * Math.cos(ia), iz = c.z + 0.4 * c.craterRadius * Math.sin(ia);
                    double e = hyp(wrapBlocks(x - ix), z - iz), ir = 0.3 * c.craterRadius;
                    if (e < ir) ground = Math.max(ground, Math.round(floor + c.craterDepth * 0.8f * (float) (1 - e / ir)));
                }
            } else if (d < c.craterRadius && !(ownCraters && ground > coneY(c, x, z) + CRATER_OWN + (rampShores ? c.craterDepth : 0)) && !drowned(c)) {
                // From format 26 a crater is only cut where this volcano is the land: where a neighbour in the chain or a
                // mountain stands higher, the crater ends against it instead of boring a round pit into it.
                // From format 31 it fades out over a crater's depth of rise, so it meets the neighbour as a slope, not a wall.
                float kept = rampShores ? smoothstep((ground - coneY(c, x, z) - CRATER_OWN) / c.craterDepth) : 0;
                // From format 42 the pit's walls rise to the rim on their own side, from its lowest point at the middle,
                // instead of the whole floor sitting below the rim's lowest notch under sheer walls on the high sides.
                double off = wrappedRims ? wrapBlocks(x - c.x) : x - c.x;
                float rimHere = smoothCones && d > 0 ? rim + (coneY(c, c.x + off * c.craterRadius / d, c.z + (z - c.z) * c.craterRadius / d) - rim) * (float) (d / c.craterRadius) : rim;
                int pit = Math.round(rimHere - c.craterDepth * (1 - (float) (d * d / (c.craterRadius * c.craterRadius))) + wobble);
                ground = Math.round(pit + kept * Math.max(0, ground - pit));
            } else if (d < c.reach() && !simCalderas) { // from format 22 the summit is sharpened in surface(), before the rivers
                float s = coneY(c, d) + wobble, fade = c.legacy ? 1 : smoothstep((float) ((c.reach() - d) / (0.1 * c.radius)));
                if (s > ground) ground = Math.round(ground + fade * (s - ground));
            }
            for (float[] k : c.cinders) { // each a steep cone of ash with a crater, standing on the flank; rivers keep their course
                double e = hyp(wrapBlocks(x - k[0]), z - k[1]), cr = 0.3 * k[2];
                if (e >= k[2] || river.edge < 4 || water != Column.NO_WATER) continue;
                float rise = e < cr ? k[3] - 0.4f * k[3] * (1 - (float) (e * e / (cr * cr))) : k[3] * (float) ((k[2] - e) / (k[2] - cr));
                ground += Math.round(rise); // on the flank as it is: the cone leans with the slope it stands on
                volcanic = true;
            }
            // Fresh lava and ash near the top; plants have long since covered the lower flanks and extinct cones.
            volcanic |= d < c.radius * ((c.v.active() ? 0.35 : 0.12) + 0.08 * noise(x, z, 64, 7));
            if (landforms && c.v.active() && d > c.craterRadius) {
                // Lava flows: tongues of fresh basalt running down the flanks, each with a low levee of its own.
                double a = Math.atan2(z - c.z, wrapBlocks(x - c.x)) + 0.35 * noise(x, z, 64, 51) + 0.15 * noise(x, z, 32, 52); // flows wander downhill
                float tongue = noise(FLOW_RING * Math.cos(a), FLOW_RING * Math.sin(a), 16, 50); // the same noise all the way down a ray
                float reach = (float) (c.radius * (0.35 + 0.45 * Math.pow(Math.max(0, tongue), 0.7)));
                if (tongue > 0.15f && d < reach) {
                    volcanic = true;
                    ground += Math.round(2 * smoothstep((tongue - 0.15f) / 0.2f));
                }
            }
            // Hot springs on the lower flanks of young volcanoes: steaming pools ringed by terraces of sinter.
            if (ponds && landforms && (c.v.active() || c.v.hotspot() && c.v.age() < SPRING_AGE) && ground > SEA_Y + 2) {
                int gx = (int) Math.floor(x / SPRING_GRID), gz = (int) Math.floor(z / SPRING_GRID), cells = span / SPRING_GRID;
                for (int dz = -1; dz <= 1; dz++)
                    for (int dx = -1; dx <= 1; dx++) {
                        int key = Math.floorMod(gx + dx, cells) * 7919 + (gz + dz) * 104729 + 17;
                        if (hash01(key) > SPRING_CHANCE) continue;
                        double px = (gx + dx + 0.5) * SPRING_GRID, pz = (gz + dz + 0.5) * SPRING_GRID, out = hyp(wrapBlocks(px - c.x), pz - c.z);
                        if (noise(px, pz, 256, 53) < SPRING_FIELD) continue; // springs gather in geothermal basins
                        if (out < 0.3 * c.radius || out > c.radius - SPRING_MARGIN) continue; // on the lower flanks, rim and all
                        double r = hyp(wrapBlocks(x - px), z - pz), size = 2.5 + 2.5 * hash01(key + 1);
                        if (r >= size + 2) continue;
                        int level = gentleShores ? springLevel(key, px, pz, size) // the finished ground, level all round
                                : Math.round(landAt(px, pz)); // the pool's surface: the real ground at its middle, not the ideal cone
                        if (level == Integer.MIN_VALUE || level <= SEA_Y + 2 || nearCrater(px, pz)) continue; // not on the sea floor, nor in a crater
                        if (r < size) {
                            boolean geyser = hash01(key + 2) < GEYSER_SHARE && r < 1.2;
                            ground = level - (geyser ? 6 : 2);
                            water = level;
                            cover = Cover.SPRING;
                        } else { // the rim: sinter built up just above the water, so the pool never spills
                            ground = Math.max(ground, level + 1);
                            cover = Cover.SINTER;
                        }
                    }
            }
            if (d < c.craterRadius && ground < pool && !drowned(c)) {
                crater = true;
                if (c.v.active() && pool > SEA_Y) lava = (int) pool;
                else water = Math.max(water, (int) pool);
            }
        }
        // Karst: rain dissolves limestone from below; the roof falls in as sinkholes, or as water-filled cenotes.
        if (landforms && water == Column.NO_WATER && ground > SEA_Y + 3 && rain > KARST_RAIN && river.edge > 10 && lakeness < 0.01f
                && Rock.of(data.rock[cell((int) Math.round(u), (int) Math.round(v))]) == Rock.BASIN) {
            int gx = (int) Math.floor(x / KARST_GRID), gz = (int) Math.floor(z / KARST_GRID), cells = span / KARST_GRID;
            for (int dz = -1; dz <= 1; dz++)
                for (int dx = -1; dx <= 1; dx++) {
                    int key = Math.floorMod(gx + dx, cells) * 7919 + (gz + dz) * 104729; // repeats around the planet
                    if (hash01(key) > KARST_CHANCE * smoothstep((rain - KARST_RAIN) / 600)) continue;
                    double px = (gx + dx + 0.2 + 0.6 * hash01(key + 1)) * KARST_GRID, pz = (gz + dz + 0.2 + 0.6 * hash01(key + 2)) * KARST_GRID;
                    double d = hyp(wrapBlocks(x - px), z - pz), radius = 3 + 4 * hash01(key + 3);
                    if (d >= radius) continue;
                    int depth = 4 + Math.round(8 * hash01(key + 4));
                    if (hash01(key + 5) < CENOTE_SHARE && rain > CENOTE_RAIN) { // a sheer shaft down to the water table
                        water = ground - 4;
                        ground -= depth + 4;
                    } else ground -= Math.round(depth * (float) Math.pow(1 - d * d / (radius * radius), 0.7)); // a bowl
                }
        }
        // Ponds: small hollows in wet, level ground hold rainwater. The level sits a block under the lowest point of the
        // rim, only level rims get one, and a low berm closes the rim, so a pond never spills. Everything that decides
        // whether a pond is there is read at its middle, so each pond is whole or absent.
        if (ponds && folds && water == Column.NO_WATER && lava == Column.NO_WATER && ice == Column.NO_WATER && rain > 0.8f * POND_RAIN) { // the middle decides; this only skips the dry lands
            int gx = (int) Math.floor(x / POND_GRID), gz = (int) Math.floor(z / POND_GRID), cells = span / POND_GRID;
            for (int dz = -1; dz <= 1; dz++)
                for (int dx = -1; dx <= 1; dx++) {
                    int key = Math.floorMod(gx + dx, cells) * 6007 + (gz + dz) * 92639 + 17; // repeats around the planet
                    if (hash01(key) > POND_CHANCE) continue;
                    double px = (gx + dx + 0.2 + 0.6 * hash01(key + 1)) * POND_GRID, pz = (gz + dz + 0.2 + 0.6 * hash01(key + 2)) * POND_GRID;
                    double d = hyp(wrapBlocks(x - px), z - pz), radius = (4 + 6 * hash01(key + 3)) / zoom;
                    if (d >= radius + POND_BERM) continue;
                    double cu = warpU(px, pz), cv = warpV(px, pz);
                    float wet = bilinear(data.precipitation, cu, cv);
                    if (hash01(key) > POND_CHANCE * smoothstep((wet - POND_RAIN) / 800) || cubic(lakes, cu, cv) > 0.01f
                            || !conesNear(px, pz).isEmpty() || riverAt(px, pz, cu, cv).edge < radius + POND_BERM + 4) continue;
                    int level = pondLevel(key, px, pz, radius);
                    if (level == Integer.MIN_VALUE) continue;
                    int depth = 2 + Math.round(2 * hash01(key + 4));
                    if (d >= radius) { // the berm
                        ground = Math.max(ground, level);
                        continue;
                    }
                    ground = Math.min(ground, level - Math.max(1, Math.round(depth * (float) Math.sqrt(1 - d * d / (radius * radius)))));
                    water = level;
                    cover = Cover.POND;
                }
        }
        // Horns: four flat faces meeting in a point, cut back by the cirques around the summit.
        if (landforms && !(calderas && crater)) for (Horn horn : hornsByCell.get(cell((int) Math.floor(originX + 0.5 + x / (double) BLOCKS_PER_CELL),
                (int) Math.floor(originY + 0.5 + z / (double) BLOCKS_PER_CELL)))) {
            double dx = wrapBlocks(x - horn.x), dz = z - horn.z, ca = Math.cos(horn.angle), sa = Math.sin(horn.angle);
            double face = Math.max(Math.abs(dx * ca + dz * sa), Math.abs(dz * ca - dx * sa)) / HORN_RADIUS;
            if (face < 1) {
                ground += Math.round(horn.height * (float) Math.pow(1 - face, 1.6));
                // From format 28 a horn only raises the ground; a lake keeps its water up to the horn's foot, instead
                // of losing it inside the horn's square footprint.
                if (!nearestLakes) water = Column.NO_WATER;
            }
        }
        // Atolls: where an old volcano has sunk in warm sea, the reef that fringed it kept growing up to the surface.
        for (Atoll a : atollsNear(x, z)) {
            double d = hyp(wrapBlocks(x - a.x), z - a.z), ring = a.radius * (1 + 0.15 * noise(x, z, 64, 37));
            float half = REEF_HALF * (1 + 0.4f * noise(x, z, 32, 38)), off = (float) Math.abs(d - ring);
            if (off < half) ground = fine(x, z, 32, 39) > -0.25f ? SEA_Y + 1 : SEA_Y - 2;         // reef crest, sandy islets on it
            else if (d < ring) ground = SEA_Y - 4 - Math.round(3 * smoothstep((off - half) / 20));   // the lagoon, floored with coral sand
            else if (off < half + REEF_FRONT) ground = Math.max(ground, SEA_Y - 2 - Math.round(2 * (off - half))); // the steep reef front
        }
        if (lava != Column.NO_WATER && lava <= ground) lava = Column.NO_WATER; // a neighbouring cone buried this crater
        if (lava != Column.NO_WATER) water = Column.NO_WATER;
        if (water != Column.NO_WATER && water <= ground) water = Column.NO_WATER; // a lake shore or a cone dammed a channel
        // From format 27 glacier ice is one smooth surface over the land: it fills valleys and bowls, merges where
        // glaciers meet and slopes down to its edges, leaving ridges bare; at a lake or the sea it ends in a low cliff.
        if (iceSheets) {
            float top = cubic(iceTop, u, v);
            // From format 55 at most ICE_DRAPE thick: a cell's ice was one level sheet at the planet's height, and the block
            // relief cuts the rock ~30 blocks below that, so the ice stood up over it as a dome. Now it lies on the mountain.
            if (drapedIce && !blurredIce) top = Math.min(top, ground + ICE_DRAPE); // from 56 in relaxTile, over the blurred rock
            // A cell is coarse: its mean may be cold while its valley floors lie far lower and warmer. Ice thins out and
            // ends where the summer down here is too warm for it, as real snouts end at 11-15 °C summers.
            if (top > ground + 1) {
                float summer = temperature(u, v, ground) + bilinear(data.amplitude, u, v);
                top = ground + (top - ground) * smoothstep((ICE_SUMMER_MAX - summer) / 4);
            }
            // From format 31 the sheet thins toward a lake across its shore band, down to a calving front a few blocks
            // high, instead of keeping its full height to the water's edge as a cliff of ice dozens of blocks tall.
            if (rampShores && !Float.isNaN(ly) && top > ly + ICE_FRONT)
                top = ly + ICE_FRONT + (top - ly - ICE_FRONT) * smoothstep((SHORE_FULL - lakeness) / (SHORE_FULL - 0.02f));
            boolean lake = water != Column.NO_WATER && lakeness > SHORE_FULL;
            if (wetBanks) top = ground + (top - ground) * (1 - craterHeat(x, z)); // from 38 an erupting crater melts its ice away
            // Over a river the ice must stand above the water or the river stays open: a thin snout never lies below it.
            boolean over = top > ground + 1 && (water == Column.NO_WATER || top >= water + 2);
            ice = over && !lake && ground >= SEA_Y - 1 ? Math.round(top) : Column.NO_WATER;
        }
        if (ice <= ground || lava != Column.NO_WATER) ice = Column.NO_WATER;
        // From format 40, below the snow line on the ice the summer melts the snow off and the glacier shows bare blue
        // ice, streaked with the rock it carries: dark stripes running down the flow where the debris of each valley wall
        // joined the ice (medial moraines), and a rubble band along its thin edges (lateral moraines).
        if (flowingIce && iceSheets && ice != Column.NO_WATER && temperature(u, v, ice) + bilinear(data.amplitude, u, v) > ICE_BARE) {
            float axis = Math.abs(valley.edge + valley.half + MORAINE_WANDER * fine(x, z, 64, 90)); // real blocks from the trough's centreline
            boolean medial = valley != NO_RIVER && merged[valley.cell] && axis < MORAINE_HALF;
            cover = medial || ice - ground <= 2 ? Cover.MORAINE : Cover.BARE_ICE;
        }
        if (ice != Column.NO_WATER) water = Column.NO_WATER; // the river runs out from under the snout
        if (water == Column.NO_WATER && lava == Column.NO_WATER && ice == Column.NO_WATER && ground < SEA_Y - 1) water = SEA_Y - 1;
        ground = Math.clamp(ground, MIN_Y + 1, MAX_Y - 2);
        water = Math.min(water, MAX_Y - 1); // NO_WATER stays NO_WATER; a glacier on the highest peaks stops at the build limit
        lava = Math.min(lava, MAX_Y - 1);
        ice = Math.min(ice, MAX_Y - 1);
        // Rock layers bend gently under plains and fold hard inside mountain ranges, where plates collided.
        int fold = Math.round(fine(x, z, 512, 5) * (12 + 0.1f * Math.max(0, ground - SEA_Y)) + fine(x, z, 64, 6) * 4);
        if (foldedStrata && folds) fold += strataFold(x, z, u, v);
        // Province edges interfinger over a few dozen blocks instead of following the warped cell outline.
        Rock rock = Rock.of(data.rock[cell((int) Math.round(u + 0.4 * noise(x, z, 32, 8)), (int) Math.round(v + 0.4 * noise(x, z, 32, 9)))]);
        // From format 38 a river waters its banks from below: through a desert it runs in a green strip (the Nile), wider
        // and greener the bigger the river.
        float big = wetBanks && river != NO_RIVER ? (float) Math.sqrt(Math.min(1, river.flow() / RIPARIAN_FLOW)) : 0;
        float banks = big > 0 ? RIPARIAN_MM * big * (1 - smoothstep(Math.max(0, river.edge()) / (RIPARIAN_REACH * big))) : 0;
        Biome biome = marsh && ground >= SEA_Y - 2 ? marsh(x, z, u, v) : biome(x, z, u, v, Math.max(organic && !(landforms && smooth < SEA_Y && ground >= SEA_Y) ? Math.min(ground, smooth) : ground, ice), banks);
        if (richBiomes && water != Column.NO_WATER && water >= ground) {
            if ((inRiver || smoothCones && lakeness > SHORE_FULL) && water >= SEA_Y) biome = winterLook(u, v, temperature(u, v, water)) < 0 ? Biome.FROZEN_RIVER : Biome.RIVER; // from 42 lakes too: one water colour
        } else if (richBiomes && !marsh && ice == Column.NO_WATER && ground <= SEA_Y + 2 && shore > -0.05f && shore < SHORE_BAND) {
            biome = landforms && cubic(cliffy, u, v) > 0.4f ? Biome.STONY_SHORE : temperature(u, v, ground) < 0 ? Biome.SNOWY_BEACH : Biome.BEACH;
            if (variedShores && biome == Biome.BEACH && cover == Cover.NONE && beachWidth(x, z, u, v) < 0.7f) cover = Cover.GRAVEL; // shingle on exposed headlands
        }
        if (forestVariety && cover == Cover.CLIFF && (biome == Biome.BADLANDS || biome == Biome.WOODED_BADLANDS || biome == Biome.DESERT))
            biome = Biome.ERODED_BADLANDS; // canyon walls
        if (scree && river.edge > SCREE_RIVER && cover == Cover.NONE && water == Column.NO_WATER && lava == Column.NO_WATER && ice == Column.NO_WATER) cover = Cover.SCREE;
        return new Column(ground, water, lava, ice, biome, fold, volcanic, rock, cover);
    }

    /**
     * Blocks below the shallowest bed, at this distance from a channel's axis: deepest along the middle and
     * shelving up to the banks, with pools and shallow bars along its length. Wide rivers run deeper.
     */
    /** Depth below the water of a channel half blocks wide, 0 beyond it: bedDepth, or the flat bed of planets before organic rivers. */
    private int channelDepth(double x, double z, float fromAxis, float half) {
        return fromAxis >= half ? 0 : organic ? bedDepth(x, z, fromAxis, half) : (int) Math.min(5, half / 4);
    }

    private int bedDepth(double x, double z, float fromAxis, float half) {
        float across = 1 - (fromAxis / half) * (fromAxis / half); // 1 on the axis, 0 at the bank
        float deepest = Math.min(12, 1.5f + half / 3);
        float pools = 1 + 0.45f * fine(x, z, 64, 28) + 0.2f * fine(x, z, 16, 29);
        return Math.max(0, Math.round(across * deepest * pools));
    }

    /** A horn: a pyramid peak where cirques ate into a summit from three or four sides. Unzoomed blocks. */
    private record Horn(double x, double z, double angle, float height) {}

    /** Glacier heads and ends along the Ice Age valleys, and horns on the highest glaciated summits. */
    private void glacialForms() {
        for (int c = 0; c < w * h; c++) {
            if (data.ice[c] == 0 || data.flow[c] <= Hydrology.GLACIER_AREA) continue;
            int up = mainDonor[c], r = data.receiver[c];
            glacialHead[c] = up < 0 || data.ice[up] == 0 || data.flow[up] <= Hydrology.GLACIER_AREA;
            glacialEnd[c] = r >= 0 && data.metres[r] >= 0 && data.ice[r] == 0;
        }
        for (int c = 0; c < w * h; c++) {
            if (data.ice[c] == 0 || data.metres[c] < HORN_METRES) continue;
            boolean top = true;
            for (int dy = -2; dy <= 2 && top; dy++)
                for (int dx = -2; dx <= 2; dx++) if ((dx != 0 || dy != 0) && data.metres[cell(c % w + dx, c / w + dy)] >= data.metres[c]) top = false;
            if (!top) continue;
            Horn horn = new Horn(wrapBlocks((c % w - originX) * BLOCKS_PER_CELL), (c / w - originY) * BLOCKS_PER_CELL, hash01(c) * Math.PI / 2, HORN_HEIGHT);
            for (int dy = -2; dy <= 2; dy++)
                for (int dx = -2; dx <= 2; dx++) hornsByCell.add(cell(c % w + dx, c / w + dy), horn);
        }
    }

    /** Which coasts get cliffs and which barrier islands; spread a cell out to sea so both sides of a shore agree. */
    private void coastTypes() {
        for (int g = 0; g < w * h; g++) {
            if (data.metres[g] < 0) continue;
            Rock r = Rock.of(data.rock[g]);
            boolean hard = r == Rock.CRATON || r == Rock.OROGEN || r == Rock.ARC || r == Rock.OCEANIC;
            float hills = 0; // the shore ramp flattens the coast itself: judge it by the ground just behind
            for (int dy = -2; dy <= 2; dy++)
                for (int dx = -2; dx <= 2; dx++) hills = Math.max(hills, data.metres[cell(g % w + dx, g / w + dy)]);
            if (hard && hills > CLIFF_METRES) cliffy[g] = 1;
            if (!hard && hills < BARRIER_METRES && data.temperature[g] > 0) barriers[g] = 1; // sea ice grinds them away
        }
        for (float[] f : new float[][]{cliffy, barriers}) {
            float[] src = f.clone();
            for (int g = 0; g < w * h; g++) {
                if (data.metres[g] >= 0) continue;
                int x = g % w, y = g / w;
                for (int dy = -1; dy <= 1; dy++)
                    for (int dx = -1; dx <= 1; dx++) f[g] = Math.max(f[g], src[cell(x + dx, y + dy)]);
            }
            blur3x3(f);
        }
    }

    /**
     * Rivers on dry uplands cut canyons: the drier and higher the plateau, the deeper. Levels are then made to never
     * rise downstream, so canyon water always runs downhill and meets the open river at its mouth without a step up.
     */
    private void canyons() {
        Integer[] byHeight = new Integer[w * h];
        for (int g = 0; g < byHeight.length; g++) byHeight[g] = g;
        Arrays.sort(byHeight, (a, b) -> Float.compare(groundY[a], groundY[b])); // receivers mostly come first
        for (int g : byHeight) {
            int r = data.receiver[g];
            if (!Float.isNaN(lakeY[g])) { // a river entering a lake ends at its surface, not on its floor
                riverLevel[g] = Math.max(groundY[g], lakeY[g]);
                continue;
            }
            float below = r >= 0 ? Math.max(riverLevel[r], SEA_Y - 1) : groundY[g], level = Math.max(groundY[g], below);
            float rain = data.precipitation[g];
            if (r >= 0 && data.flow[g] >= CREEK_AREA && groundY[g] > SEA_Y + CANYON_HEIGHT && rain < CANYON_RAIN && data.ice[g] == 0) {
                // Cut in from where the river leaves the plateau: never below the reach downstream, so the canyon
                // deepens by the river's own fall with every cell upstream, as real canyons grow headward.
                float want = Math.min(CANYON_MAX, CANYON_CUT * (groundY[g] - SEA_Y - CANYON_HEIGHT));
                float dry = smoothstep((CANYON_RAIN - rain) / 150), big = smoothstep(data.flow[g] / (4 * CREEK_AREA));
                level = Math.max(below, groundY[g] - want * dry * big);
            }
            riverLevel[g] = level;
        }
    }

    /**
     * Coasts by rock and relief. Hard rock on high ground meets the sea in cliffs, with deep water at their foot and
     * sea stacks standing off them. Soft, low coasts get a sandy barrier island offshore, broken by inlets, with a
     * shallow lagoon behind it. Elsewhere the plain beach stays.
     */
    private float coast(double x, double z, double u, double v, float shore, float natural) {
        float cliff = cubic(cliffy, u, v) * smoothstep((noise(x, z, 256, 31) + 0.35f) / 0.3f); // cliffs come and go along a coast
        float inland = cubic(groundY, u, v) - SEA_Y, barrier = cubic(barriers, u, v) * (1 - cliff) * smoothstep(noise(x, z, 512, 36) * 2);
        inland = Math.max(inland, CLIFF_HEIGHT * 0.5f); // cliffs stand on the hills behind the flattened shore
        if (cliff > 0.05f) {
            float height = Math.min(CLIFF_HEIGHT * cliff * (0.7f + 0.3f * noise(x, z, 64, 35)), inland);
            if (shore > 0) return Math.max(natural, SEA_Y - 1 + height * smoothstep(shore / 0.03f));
            if (-shore < STACK_REACH && fine(x, z, 16, 32) > STACK_NOISE) return SEA_Y - 1 + 0.8f * height; // a sea stack
            return Math.min(natural, SEA_Y - 2 - 8 * cliff * smoothstep(-shore / 0.05f));      // deep water at the foot
        }
        if (barrier > 0.3f && shore < 0) {
            float out = -shore;
            if (Math.abs(out - BARRIER_AT) < BARRIER_HALF && noise(x, z, 128, 33) > -0.55f) // the island, cut by inlets
                return SEA_Y + 0.5f + 1.5f * Math.max(0, fine(x, z, 16, 34));               // with low dunes
            if (out < BARRIER_AT) return Math.min(natural, SEA_Y - 2 - 1.5f * smoothstep(out / BARRIER_AT)); // the lagoon
        }
        return natural;
    }

    /**
     * Gullies: every mountainside is veined with small valleys running straight downhill, the way runoff cuts them.
     * Stripes across the local slope, each grid point's own wave turned to the slope it sits on and blended with its
     * neighbours', so the grooves follow the mountain's shape and branch where slopes meet. Two sizes, in real blocks.
     */
    private float gullies(double x, double z, double u, double v, float above) {
        double e = 0.05; // cells: the slope of the planet's own surface, in blocks per real block
        double gu = (cubic(groundY, u + e, v) - cubic(groundY, u - e, v)) / (2 * e * blocksPerCell);
        double gv = (cubic(groundY, u, v + e) - cubic(groundY, u, v - e)) / (2 * e * blocksPerCell);
        double steep = hyp(gu, gv);
        if (steep < 0.05) return 0;
        double px = -gv / steep, pz = gu / steep; // across the slope: the gullies run perpendicular to this
        float depth = (float) Math.min(GULLY_DEPTH, GULLY_DEPTH * steep * 4) * smoothstep((above - 20) / 40);
        return depth * (gullyWaves(x, z, px, pz, GULLY_WIDE, 60) + 0.35f * gullyWaves(x, z, px, pz, GULLY_NARROW, 61));
    }

    private final ThreadLocal<long[]> lastSpring = ThreadLocal.withInitial(() -> new long[]{Long.MIN_VALUE, 0}); // spring key, its level

    /**
     * A hot spring's pool level: the finished ground at its middle, or MIN_VALUE where the ground around it is not
     * level enough to hold a pool behind a low sinter rim. Like pondLevel, the last answer is kept per thread.
     */
    private int springLevel(int key, double px, double pz, double size) {
        long[] last = lastSpring.get();
        if (last[0] == key) return (int) last[1];
        Column mid = column(px, pz, false);
        int level = mid.ground();
        if (mid.water() != Column.NO_WATER || mid.lava() != Column.NO_WATER || mid.ice() != Column.NO_WATER) level = Integer.MIN_VALUE;
        for (int k = 0; k < 8 && level != Integer.MIN_VALUE; k++) {
            double a = k * Math.PI / 4;
            int rim = column(px + (size + 1) * Math.cos(a), pz + (size + 1) * Math.sin(a), false).ground();
            if (rim < level - 1 || rim > level + SPRING_TILT) level = Integer.MIN_VALUE; // the sinter rim would stand as a tower
        }
        last[0] = key;
        last[1] = level;
        return level;
    }

    /** Rain over what the land needs to stay out of dryness (Köppen), at real block (x, y, z): below 0.5 desert, below 1 steppe. */
    public float aridity(int realX, int realZ, float y) {
        double x = realX / (double) zoom, z = realZ / (double) zoom, u = warpU(x, z), v = warpV(x, z);
        float t = localTemperature(x, z, u, v, y), rain = bilinear(data.precipitation, u, v) * (1 + 0.12f * noise(x, z, 256, 24) + 0.06f * noise(x, z, 64, 25));
        float s = bilinear(data.summerRain, u, v), need = 20 * t + (s >= 0.7f ? 280 : s >= 0.3f ? 140 : 0);
        return need <= 0 ? 10 : rain / need;
    }

    private final ThreadLocal<long[]> lastPond = ThreadLocal.withInitial(() -> new long[]{Long.MIN_VALUE, 0}); // pond key, its level

    /**
     * A pond's water level, or MIN_VALUE where it can't hold one: a block under the lowest of eight points on its rim,
     * judged on the finished ground (rivers and valleys cut), dry and level all round, with no gully under the middle.
     * The neighbouring columns of a pond ask in turn, so the last answer is kept per thread.
     */
    private int pondLevel(int key, double px, double pz, double radius) {
        long[] last = lastPond.get();
        if (last[0] == key) return (int) last[1];
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int k = 0; k <= 8; k++) {
            double a = k * Math.PI / 4, r = k == 8 ? 0 : radius + 1;
            Column c = column(px + r * Math.cos(a), pz + r * Math.sin(a), false);
            if (c.water() != Column.NO_WATER || c.lava() != Column.NO_WATER || c.ice() != Column.NO_WATER) { lo = Integer.MIN_VALUE; break; }
            if (k == 8) { if (c.ground() < lo - POND_LEVEL) lo = Integer.MIN_VALUE; break; } // the middle: no hidden gully
            lo = Math.min(lo, c.ground());
            hi = Math.max(hi, c.ground());
        }
        int level = lo == Integer.MIN_VALUE || hi - lo > POND_LEVEL || lo < SEA_Y + 4 ? Integer.MIN_VALUE : lo - 1;
        last[0] = key;
        last[1] = level;
        return level;
    }

    /**
     * Talus: rock on a slope steeper than it can stand slides down until it can. The planet's cells are far wider than
     * they are tall in blocks, so a range one or two cells across turns into a needle; here the steepest cells shed
     * onto their lower neighbours, mass kept, so the range widens at its foot and its peaks settle. The limit is a real
     * slope in blocks, so wider world scales are barely touched. Rivers, lakes and the sea are left as they are, so
     * no river is dammed and no coast moves.
     */
    private void talus() {
        float limit = TALUS_SLOPE * blocksPerCell;
        boolean[] fixed = new boolean[w * h];
        for (int g = 0; g < w * h; g++)
            fixed[g] = data.metres[g] < 0 || data.flow[g] >= CREEK_AREA || !Float.isNaN(data.lakeMetres[g]);
        int[] dx = {1, -1, 0, 0, 1, 1, -1, -1}, dy = {0, 0, 1, -1, 1, -1, 1, -1};
        for (int pass = 0; pass < TALUS_PASSES; pass++) {
            boolean moved = false;
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    int g = x + y * w;
                    if (fixed[g] || groundY[g] < SEA_Y + TALUS_ABOVE) continue;
                    for (int k = 0; k < 8; k++) { // ponytail: Gauss-Seidel sweep, order-dependent but converges; fine for a one-off pass
                        int ny = y + dy[k];
                        if (ny < 0 || ny >= h) continue;
                        int n = cell(x + dx[k], ny);
                        float excess = groundY[g] - groundY[n] - limit * (k < 4 ? 1 : 1.4142f);
                        if (excess <= 0) continue;
                        groundY[g] -= excess * 0.25f;
                        if (!fixed[n]) groundY[n] += excess * 0.25f; // into a river valley the rubble is carried away
                        moved = true;
                    }
                }
            if (!moved) break;
        }
    }

    /** 0..1: how much of the year the ground at height y crosses freezing; most where the mean is near 0 and the swing large. */
    private float frost(double u, double v, float y) {
        float a = bilinear(data.amplitude, u, v);
        return a <= 0 ? 0 : Math.clamp(1 - Math.abs(temperature(u, v, y)) / a, 0, 1);
    }

    /**
     * Rivers whose water comes in one flood season (snowmelt in spring, the monsoon in summer) carry more gravel than
     * they can keep in one channel: on gentle reaches they braid across it.
     */
    private boolean seasonalFloods(River river) {
        if (river.flow < BRAID_FLOW || river.top - river.bottom > BRAID_DROP) return false;
        int c = river.cell;
        float melt = Climate.frozenShare(data.temperature[c], data.amplitude[c]);
        return data.summerRain[c] > MONSOON_BRAID || melt > 0.3f && melt < 0.7f;
    }

    /** From format 30: a summit standing in a lake keeps no crater; a sealed bowl there would ring the lake with a wall. */
    private boolean drowned(Cone c) {
        return solidShores && lakes[cell(Math.round(c.v.u()), Math.round(c.v.v()))] > 0.02f;
    }

    /** Within a collapsed volcano's caldera (and a little beyond its rim), in unzoomed blocks. */
    /** 1 deeper than wall blocks inside a collapsed caldera's edge, easing to 0 at it; with wall 0, above 0 anywhere inside. */
    private float insideCaldera(double x, double z, double wall) {
        float in = 0;
        for (Cone c : conesNear(x, z)) {
            double past = c.collapsed ? 1.2 * Volcano.CALDERA_SHARE * c.radius - hyp(wrapBlocks(x - c.x), z - c.z) : 0;
            if (past > 0) in = Math.max(in, wall > 0 ? smoothstep((float) (past / wall)) : 1);
        }
        return in;
    }

    /** Ridged massif relief, 0 in the valleys to 1 on the crests: finer ridges ride on the bigger ones. */
    private float ridgeline(double x, double z) {
        float r1 = 1 - Math.abs(fine(x, z, 512, 64)), r2 = 1 - Math.abs(fine(x, z, 256, 65)), r3 = 1 - Math.abs(fine(x, z, 128, 66));
        return (r1 * r1 + 0.5f * r2 * r2 * r1 + 0.25f * r3 * r3 * r2) / 1.75f;
    }

    /**
     * Foothills: rolling hills on the low ground under a range, higher the more the range towers over it. Fold
     * mountains: on the flanks of collision belts the rock is crumpled into long parallel ridges (Zagros, Jura,
     * Appalachian Valley and Ridge). They follow the contours of the smoothed range, wobble, and plunge out along
     * their length. Both only raise the ground, so they never open a pit that would hold or spill water.
     */
    private float foothills(double x, double z, double u, double v, float natural) {
        float low = natural - SEA_Y, onLand = smoothstep((low - 2) / 10) * (1 - smoothstep(cubic(lakes, u, v) / 0.3f));
        if (onLand <= 0) return 0;
        float bumps = Math.clamp(0.5f + 0.5f * (0.65f * fine(x, z, 128, 68) + 0.35f * fine(x, z, 64, 69)), 0, 1);
        float hills = Math.min(FOOTHILL_MAX, FOOTHILL_SHARE * Math.max(0, cubic(foothill, u, v))) * bumps * (1 - smoothstep((low - 100) / 60));
        float belt = Math.min(1, 3 * cubic(orogen, u, v));
        if (belt <= 0.05f) return onLand * hills;
        double e = 0.5;
        double gu = (cubic(rangeY, u + e, v) - cubic(rangeY, u - e, v)) / (2 * e * blocksPerCell);
        double gv = (cubic(rangeY, u, v + e) - cubic(rangeY, u, v - e)) / (2 * e * blocksPerCell);
        float spacing = (float) (FOLD_RISE / Math.max(hyp(gu, gv), 1e-4)); // real blocks between ridges
        float even = smoothstep((spacing - 48) / 48) * (1 - smoothstep((spacing - 384) / 256)); // none on flats or steep walls
        double phase = cubic(rangeY, u, v) / FOLD_RISE + 0.35 * fine(x, z, 256, 70);
        float crest = (float) (0.5 + 0.5 * Math.cos(2 * Math.PI * phase)), plunge = smoothstep((fine(x, z, 512, 71) + 0.5f) / 0.8f);
        float fold = FOLD_HEIGHT * belt * even * plunge * crest * crest * smoothstep((low - 4) / 12) * (1 - smoothstep((low - 80) / 80));
        return onLand * Math.max(hills, fold + 0.5f * hills);
    }

    /**
     * From format 34: how far the rock layers are pushed up or down here, in blocks, where plates collided. Squeezed
     * between two plates the layers buckle into folds whose axes run along the range (the same contours the fold
     * ridges follow), tight and steep where the collision was strongest, open at the belt's edge, and crumpled
     * smaller on top, so a range shows bands dipping and arching across its faces instead of lying level.
     */
    private int strataFold(double x, double z, double u, double v) {
        float belt = Math.min(1, 3 * cubic(orogen, u, v));
        if (belt <= 0.05f) return 0;
        double phase = cubic(rangeY, u, v) / STRATA_RISE + 0.35 * fine(x, z, 256, 75);
        float folds = STRATA_FOLD * belt * (0.6f + 0.4f * fine(x, z, 512, 76)); // fold amplitude swells and dies along each axis
        return Math.round(folds * (float) Math.sin(2 * Math.PI * phase) + STRATA_CRUMPLE * belt * fine(x, z, 128, 77));
    }

    /** -1 in a gully's bottom to 1 on the rib between: waves across the slope from a jittered grid, blended. */
    private float gullyWaves(double x, double z, double px, double pz, int spacing, int salt) {
        double grid = spacing / (double) zoom; // unzoomed blocks per grid step, so gullies keep their real-block size
        int gx = (int) Math.floor(x / grid), gz = (int) Math.floor(z / grid), cells = (int) Math.round(span / grid);
        double sum = 0, weights = 0, sigma = 0.7 * grid, k = 2 * Math.PI / grid;
        for (int dz = -1; dz <= 2; dz++)
            for (int dx = -1; dx <= 2; dx++) {
                int key = Math.floorMod(gx + dx, cells) * 92821 + (gz + dz) * 68917 + salt * 31;
                double cx = (gx + dx + hash01(key)) * grid, cz = (gz + dz + hash01(key + 1)) * grid;
                double ox = wrapBlocks(x - cx), oz = z - cz, wgt = Math.exp(-(ox * ox + oz * oz) / (2 * sigma * sigma));
                float strength = massifs ? 0.3f + 0.7f * hash01(key + 3) : 1; // some gullies deep, some faint, some gone
                sum += wgt * strength * Math.cos((ox * px + oz * pz) * k + 2 * Math.PI * hash01(key + 2));
                weights += wgt;
            }
        float wave = (float) (sum / weights);
        return wave > 0 ? (float) Math.sqrt(wave) : -(float) Math.pow(-wave, 1.5); // sharp ribs, rounded gully floors
    }

    /**
     * 0..1 at real block (x, z), coherent over several hundred blocks with ragged edges: which of a climate's biomes
     * grows here, so one climate zone holds stands of different woods and grasslands, as real ones do.
     */
    public float variety(int realX, int realZ) {
        double x = realX / (double) zoom, z = realZ / (double) zoom;
        float n = fine(x, z, 1024, 64) + 0.35f * fine(x, z, 256, 65); // bell-shaped: tanh spreads it so each option gets an even share
        return Math.min(0.999f, 0.5f + 0.5f * (float) Math.tanh(2.5 * n));
    }

    /** Patchiness for snow and rock, -1..1, at real block (x, z): coherent over a few dozen blocks. */
    public float patch(int realX, int realZ) {
        double x = realX / (double) zoom, z = realZ / (double) zoom;
        return 0.7f * fine(x, z, 32, 62) + 0.3f * fine(x, z, 8, 63);
    }

    /** Canyon wall profile, 0 at the river to 1 at the rim: flat benches joined by steep cliffs, as layered rock erodes. */
    private static float benches(float e) {
        float k = Math.clamp(e, 0, 1) * CANYON_BENCHES, step = (float) Math.floor(k);
        return Math.min(1, (step + smoothstep((k - step - 0.6f) / 0.4f)) / CANYON_BENCHES);
    }

    /** 0..1, fixed per cell and seed. */
    private float hash01(int cell) {
        long hsh = (cell * 0x9E3779B97F4A7C15L) ^ seed;
        hsh = (hsh ^ (hsh >>> 31)) * 0xBF58476D1CE4E5B9L;
        return ((hsh >>> 40) & 0xFFFF) / 65536f;
    }

    private boolean nearCrater(double x, double z) {
        for (Cone c : conesNear(x, z)) if (hyp(wrapBlocks(x - c.x), z - c.z) < 2 * c.craterRadius + SPRING_MARGIN) return true;
        return false;
    }

    /** The ground at unzoomed (x, z) before rivers, lakes and landforms: the planet's surface plus its small hills. */
    private float landAt(double x, double z) {
        double u = warpU(x, z), v = warpV(x, z);
        float natural = cubic(groundY, u, v), above = Math.max(0, natural - SEA_Y);
        return natural + detail(x, z) * (5 + 0.06f * above);
    }

    /** 0 in the hills, rising to 1 on the coastal plain. */
    private float lowland(double u, double v) {
        return lowlandRivers ? Math.clamp(1 - (cubic(groundY, u, v) - SEA_Y) / LOWLAND_BLOCKS, 0, 1) : 0;
    }

    /** The river channel the game carves near (x, z): lowland rivers swing in meanders and spread wider. */
    private River riverAt(double x, double z, double u, double v) {
        float low = lowland(u, v); // the query point wanders instead of the channel; meanders are sized in real blocks
        double mu = low > 0 ? low * MEANDER / zoom * (fine(x, z, 128, 11) + 0.5 * fine(x, z, 64, 16)) : 0;
        double mv = low > 0 ? low * MEANDER / zoom * (fine(x, z, 128, 12) + 0.5 * fine(x, z, 64, 17)) : 0;
        if (!wildRivers) return nearestRiver(u + mu, v + mv, (byte) 0, organic ? CREEK_AREA : Hydrology.RIVER_AREA).widened(1 + 0.5f * low);
        // Wild rivers twist every few dozen blocks as well, pinch and swell along their length, have ragged banks,
        // and gather far more small creeks.
        double twist = WIGGLE * (0.5 + 0.5 * low) / blocksPerCell;
        mu += twist * (fine(x, z, 64, 70) + 0.5 * fine(x, z, 32, 71) + 0.25 * fine(x, z, 16, 76));
        mv += twist * (fine(x, z, 64, 72) + 0.5 * fine(x, z, 32, 73) + 0.25 * fine(x, z, 16, 77));
        River r = nearestRiver(u + mu, v + mv, (byte) 0, WILD_CREEK_AREA);
        if (r == NO_RIVER) return r;
        r = r.widened((1 + 0.5f * low) * (1 + PINCH * fine(x, z, 64, 74)));
        return new River(r.edge() + RAGGED * fine(x, z, 8, 75), r.half(), r.y(), r.ice(), r.flow(), r.top(), r.bottom(), r.t(), r.cell(), r.incision());
    }


    /** A loop the river once took: the same channel along another meander path, for oxbow lakes. */
    private River oxbowAt(double x, double z, double u, double v) {
        double mu = 1.6 * MEANDER / zoom * (fine(x, z, 128, 45) + 0.5 * fine(x, z, 64, 46));
        double mv = 1.6 * MEANDER / zoom * (fine(x, z, 128, 47) + 0.5 * fine(x, z, 64, 49));
        return nearestRiver(u + mu, v + mv, (byte) 0, OXBOW_FLOW);
    }

    /** The river channel nearest a warped position: blocks outside its edge (negative inside), half width, water level, its cell's ice. */
    /** ... top, bottom: water level at the reach's two ends; t: how far along it; cell: where the reach starts. */
    private record River(float edge, float half, float y, byte ice, float flow, float top, float bottom, float t, int cell, float incision) {
        River widened(float k) {
            return this == NO_RIVER ? this : new River(edge - half * (k - 1), half * k, y, ice, flow, top, bottom, t, cell, incision);
        }
    }

    private static final River NO_RIVER = new River(Float.MAX_VALUE, 0, 0, (byte) 0, 0, 0, 0, 0, -1, 0);

    /** Distance from a glacial valley's centreline as a fraction of its trough's half width; 1 and beyond is outside. */
    private static float troughFraction(River valley, float width) {
        return valley == NO_RIVER ? 1 : (valley.edge + valley.half) / (troughWidth(valley) * width);
    }

    /**
     * The ground after every glacial trough within reach has cut it: the deepest cut wins. Taking only the nearest
     * trough left a wall where two wide troughs overlap and the nearest switches from one to the other.
     */
    private int troughCut(double u, double v, int ground, float width) {
        int iu = (int) Math.round(u), iv = (int) Math.round(v), cut = ground;
        for (int dy = -3; dy <= 3; dy++)
            for (int dx = -3; dx <= 3; dx++) {
                int cy = iv + dy;
                if (cy < 0 || cy >= h) continue;
                int c = cell(iu + dx, cy), r = data.receiver[c];
                if (r < 0 || data.metres[c] < 0 || data.ice[c] < Hydrology.GLACIATED || data.flow[c] <= Hydrology.GLACIER_AREA) continue;
                double au = iu + dx, su = wrap(r % w - c % w), sv = r / w - cy;
                double t = Math.clamp(((u - au) * su + (v - cy) * sv) / (su * su + sv * sv), 0, 1);
                float dist = (float) hyp(u - au - t * su, v - cy - t * sv) * blocksPerCell;
                float floor = Math.max(groundY[c] + (float) t * (groundY[r] - groundY[c]), SEA_Y - 1) + 1;
                float base = Math.min(64, 40 + 0.3f * (float) Math.sqrt(data.flow[c])) * width;
                float wide = Math.min(TROUGH_MAX, Math.max(1, TROUGH_SLOPE * (ground - floor) / base));
                float f = dist / (base * wide);
                if (f < 1) cut = Math.min(cut, trough(floor, ground, f));
            }
        return cut;
    }

    /** Blocks from a trough's axis to where its walls meet the land, before widening for depth. */
    private static float troughWidth(River valley) {
        return Math.min(64, 40 + 0.3f * (float) Math.sqrt(valley.flow));
    }

    /**
     * Today's glaciers, by mass balance. Above the snow line each cell gains ice from its snowfall; below it the ice
     * melts, faster the warmer it is. Ice flows downhill along the drainage, adding up from every cell above, so a
     * glacier's tongue reaches down its valley as far as its supply outlasts the melt. Thickness in blocks per cell.
     */
    private float[] glaciers() {
        float[] flux = new float[w * h], thick = new float[w * h];
        long[] order = new long[w * h];
        for (int g = 0; g < order.length; g++) { // highest first: sort by elevation, keeping the cell in the low bits
            int bits = Float.floatToIntBits(data.metres[g]);
            order[g] = (long) (bits ^ (bits >> 31) & 0x7fffffff) << 32 | g;
        }
        java.util.Arrays.sort(order);
        for (int i = order.length - 1; i >= 0; i--) {
            int g = (int) order[i];
            if (data.metres[g] < 0) continue;
            float t = data.temperature[g], balance; // metres of ice a year
            if (seasons) { // the winter's snow against the summer's melt
                float a = data.amplitude[g], s = data.summerRain[g], frozen = Climate.frozenShare(t, a);
                float snowShare = frozen <= 0.5f ? (1 - s) * 2 * frozen : (1 - s) + s * 2 * (frozen - 0.5f); // the frozen months are winter first
                balance = data.precipitation[g] / 1000 * snowShare - ICE_DDF * Climate.warmMean(t, a);
            } else {
                float snow = data.precipitation[g] / 1000 * Math.clamp((ICE_LINE - t) / 4, 0, 1);
                balance = t < ICE_LINE ? snow : -ICE_MELT * (t - ICE_LINE);
            }
            flux[g] = Math.max(0, flux[g] + balance);
            if (flux[g] <= 0) continue;
            int r = data.receiver[g];
            // From format 40 ice lies where it is fed: snow on steep ground slides down to the cell below as avalanches,
            // and a thin sheet on an open slope does not last, so ice gathers in the valleys and hollows and the ridges
            // and walls between them stand bare.
            boolean steep = flowingIce && r >= 0 && (groundY[g] - groundY[r]) / blocksPerCell > ICE_STEEP;
            thick[g] = steep ? 0 : Math.min(ICE_MAX, Math.max(0, ICE_THICK * (float) Math.pow(flux[g], 0.4) - (flowingIce ? ICE_SHED : 0)));
            if (r >= 0 && data.metres[r] >= 0) flux[r] += flux[g];
        }
        return thick;
    }

    /** U-shaped cross-section: flat floor, walls steepening up to the untouched ground at t = 1. */
    private static int trough(float floor, int ground, float t) {
        return Math.round(floor + (ground - floor) * t * t);
    }

    /** Segments run from each cell with at least this ice and flow to its receiver; the nearest edge wins. */
    private River nearestRiver(double u, double v, byte minIce, float minFlow) {
        int iu = (int) Math.round(u), iv = (int) Math.round(v), n = minIce == Hydrology.FJORD || glacierFlow && minIce == Hydrology.GLACIATED ? 3 : 2; // fjord mouths and wide troughs reach further
        if (minIce > 0 && minFlow == Hydrology.GLACIER_AREA && iceNear[cell(iu, iv)] < minIce) return NO_RIVER; // no trough within reach: the search finds none
        River best = NO_RIVER;
        for (int dy = -n; dy <= n; dy++)
            for (int dx = -n; dx <= n; dx++) {
                int cy = iv + dy;
                if (cy < 0 || cy >= h) continue;
                int c = cell(iu + dx, cy), r = data.receiver[c];
                if (r < 0 || data.metres[c] < 0 || data.ice[c] < minIce || data.flow[c] <= minFlow) continue;
                double au = iu + dx, bu = au + wrap(r % w - c % w), bv = r / w;
                double su = bu - au, sv = bv - cy;
                // A fjord's mouth runs on one more cell out to sea: its first sea cell can be a pocket the smooth coastline closes.
                double reach = minIce == Hydrology.FJORD && data.metres[r] < 0 ? 2 : 1;
                double t = Math.clamp(((u - au) * su + (v - cy) * sv) / (su * su + sv * sv), 0, reach);
                float dist = (float) hyp(u - au - t * su, v - cy - t * sv) * blocksPerCell; // real blocks: rivers keep their width
                float half = riverWidth * Math.min(40, organic ? 0.5f + 0.13f * (float) Math.sqrt(data.flow[c]) : 2 + 0.12f * (float) Math.sqrt(data.flow[c]));
                if (organic && t <= 1 && dist - BULGE * blocksPerCell - half < best.edge) {
                    // The channel curves through the cell centres instead of kinking at each: a Catmull-Rom spline
                    // from the main branch upstream, through this cell and its receiver, on to the next one down.
                    int up = mainDonor[c], down = data.receiver[r];
                    double pu0 = up < 0 ? 2 * au - bu : au + wrap(up % w - c % w), pv0 = up < 0 ? 2 * cy - bv : up / w;
                    double pu3 = down < 0 ? 2 * bu - au : bu + wrap(down % w - r % w), pv3 = down < 0 ? 2 * bv - cy : down / w;
                    double bestD = Double.MAX_VALUE, prevU = au, prevV = cy;
                    for (int k = 1; k <= SPLINE_STEPS; k++) {
                        double s = k / (double) SPLINE_STEPS;
                        double qu = catmullD(pu0, au, bu, pu3, s), qv = catmullD(pv0, cy, bv, pv3, s);
                        double eu = qu - prevU, ev = qv - prevV, len2 = eu * eu + ev * ev;
                        double f = len2 == 0 ? 0 : Math.clamp(((u - prevU) * eu + (v - prevV) * ev) / len2, 0, 1);
                        double dd = hyp(u - prevU - f * eu, v - prevV - f * ev);
                        if (dd < bestD) { bestD = dd; t = (k - 1 + f) / SPLINE_STEPS; }
                        prevU = qu;
                        prevV = qv;
                    }
                    dist = (float) bestD * blocksPerCell;
                }
                if (dist - half < best.edge) {
                    float[] lv = minIce == 0 ? riverLevel : groundY; // glacial troughs follow the ground, rivers their own level
                    float top = lv[c], bottom = Math.max(lv[r], SEA_Y - 1), along = (float) Math.min(t, 1);
                    float cut = (groundY[c] - lv[c]) + along * ((groundY[r] - lv[r]) - (groundY[c] - lv[c]));
                    best = new River(dist - half, half, top + along * (bottom - top), data.ice[c], data.flow[c], top, bottom, along, c, cut);
                }
            }
        return best;
    }

    /**
     * The planet map as the game builds it, one pixel per cell: what stands at each cell's centre, with the warp,
     * rivers, lakes and volcanoes the player will actually find there. Geology shows rock provinces instead of biomes.
     */
    public int[] mapColors(boolean geology) {
        int[] rgb = new int[w * h], top = new int[w * h];
        IntStream.range(0, h).parallel().forEach(cy -> {
            for (int cx = 0; cx < w; cx++) {
                int g = cx + cy * w, x = wrap(cx - originX) * blocksPerCell, z = (cy - originY) * blocksPerCell;
                Column c = column(x, z);
                top[g] = c.ground();
                boolean sea = c.water() == SEA_Y - 1 && c.ground() < SEA_Y - 1;
                // Rivers are narrower than a pixel and the warp bends them, so near a channel look at the pixel's own
                // blocks: it is water on the map only if the game really puts water somewhere in it.
                boolean river = !sea && c.water() != Column.NO_WATER;
                double sx = x / (double) zoom, sz = z / (double) zoom;
                int reach = blocksPerCell / 2 - 2, step = Math.max(7, 2 * reach / 8); // a 5x5 grid at zoom 1, 9x9 above
                boolean near = !sea && !river && nearestRiver(warpU(sx, sz), warpV(sx, sz), (byte) 0, Hydrology.RIVER_AREA).edge < 1.5f * blocksPerCell;
                Biome biome = c.biome();
                if (near)
                    search:
                    for (int oz = -reach; oz <= reach; oz += step)
                        for (int ox = -reach; ox <= reach; ox += step) {
                            double px = (x + ox) / (double) zoom, pz = (z + oz) / (double) zoom, pu = warpU(px, pz), pv = warpV(px, pz);
                            if (riverAt(px, pz, pu, pv).edge > 2 && cubic(lakes, pu, pv) < 0.4f) continue; // cheap: only a likely channel gets a full column
                            Column s = column(x + ox, z + oz);
                            if (s.water() != Column.NO_WATER) { // a fjord counts too: it is narrower than a pixel
                                river = true;
                                break search;
                            }
                        }
                // A river through dry land waters a green strip narrower than a pixel (format 38): show it, as the Nile
                // shows from space, by the greenest of the pixel's own blocks.
                if (near && !river && wetBanks && !geology && (biome == Biome.DESERT || biome == Biome.STEPPE || biome == Biome.BADLANDS))
                    green:
                    for (int oz = -reach; oz <= reach; oz += step)
                        for (int ox = -reach; ox <= reach; ox += step) {
                            Biome b = column(x + ox, z + oz).biome();
                            if (b == Biome.SAVANNA || b == Biome.GRASSLAND || b == Biome.TEMPERATE_FOREST || b == Biome.TROPICAL_SEASONAL_FOREST) {
                                biome = b;
                                break green;
                            }
                        }
                rgb[g] = c.lava() != Column.NO_WATER ? LAVA_COLOR : river ? WATER_COLOR : c.ice() != Column.NO_WATER && !geology ? GLACIER_COLOR
                        : geology ? c.rock().color : biome.color;
            }
        });
        for (int g = 0; g < rgb.length; g++) { // hillshade, light from the west
            int x = g % w, row = g - x;
            if (rgb[g] == WATER_COLOR || top[g] < SEA_Y - 1) continue;
            float f = Math.clamp(1 + (top[row + Math.floorMod(x - 1, w)] - top[row + (x + 1) % w]) * 0.015f, 0.4f, 1.6f);
            int c = rgb[g], r = c >> 16 & 255, gr = c >> 8 & 255, b = c & 255;
            rgb[g] = f <= 1 ? (int) (r * f) << 16 | (int) (gr * f) << 8 | (int) (b * f)
                    : (int) (r + (255 - r) * (f - 1)) << 16 | (int) (gr + (255 - gr) * (f - 1)) << 8 | (int) (b + (255 - b) * (f - 1));
        }
        for (Volcano v : data.volcanoes) { // a small plus: red if erupting, dark brown if not
            if (v.gone()) continue;
            int x = (int) v.u(), y = (int) v.v(), c = v.active() ? 0xE03C1E : 0x50403A;
            for (int k = -1; k <= 1; k++) {
                rgb[Math.floorMod(x + k, w) + y * w] = c;
                rgb[x + Math.clamp(y + k, 0, h - 1) * w] = c;
            }
        }
        return rgb;
    }

    /**
     * From format 28: the level of the nearest lake cell, so where two lakes at different levels lie close each keeps
     * its own; and the level of a higher lake when this spot is on the narrow sill between them (else NaN).
     */
    private float[] nearestLake(double u, double v) {
        int iu = (int) Math.round(u), iv = (int) Math.round(v);
        float level = Float.NaN, high = Float.NaN;
        double best = Double.MAX_VALUE, bestHigh = Double.MAX_VALUE;
        for (int dy = -2; dy <= 2; dy++)
            for (int dx = -2; dx <= 2; dx++) {
                float l = lakeY[cell(iu + dx, iv + dy)];
                if (Float.isNaN(l)) continue;
                double d = hyp(iu + dx - u, iv + dy - v);
                if (d < best) { best = d; level = l; }
            }
        for (int dy = -2; dy <= 2; dy++)
            for (int dx = -2; dx <= 2; dx++) {
                float l = lakeY[cell(iu + dx, iv + dy)];
                double d = hyp(iu + dx - u, iv + dy - v);
                if (!Float.isNaN(l) && l > level + 1 && d < bestHigh) { bestHigh = d; high = l; }
            }
        return new float[]{level, bestHigh - best < LAKE_SILL ? high : Float.NaN};
    }

    /** Highest lake surface within two cells: one level per lake, shared by its water and its shores. */
    private float lakeLevel(int iu, int iv) {
        float level = Float.NaN;
        for (int dy = -2; dy <= 2; dy++)
            for (int dx = -2; dx <= 2; dx++) {
                float l = lakeY[cell(iu + dx, iv + dy)];
                if (!Float.isNaN(l) && (Float.isNaN(level) || l > level)) level = l;
            }
        return level;
    }

    /** Cheap version for biome lookups: no rivers, no detail noise. */
    /**
     * biomeAt, read from a rockfall tile or a square of plain columns already worked out where there is one: the game
     * asks for biomes just before it builds a chunk, and building the whole column again for each was ~8% of the
     * generator's time. Rockfall keeps biomes, so the answer is the same. Never works out a tile just for this.
     */
    public Biome biomeAtCached(int realX, int realZ) {
        if (!richBiomes) return biomeAt(realX, realZ);
        Column c = ready(relaxedTiles, realX, realZ, RELAX_TILE);
        if (c == null) c = ready(baseBlocks, realX, realZ, BASE_BLOCK);
        return c != null ? c.biome() : biomeAt(realX, realZ);
    }

    /** The column at (x, z) from a square of side size in the cache, or null if that square isn't finished. */
    private static Column ready(java.util.concurrent.ConcurrentHashMap<Long, java.util.concurrent.CompletableFuture<Column[]>> map, int x, int z, int size) {
        int kx = Math.floorDiv(x, size), kz = Math.floorDiv(z, size);
        var f = map.get((long) kx << 32 | (kz & 0xffffffffL));
        return f != null && f.isDone() && !f.isCompletedExceptionally() ? f.join()[(x - kx * size) + (z - kz * size) * size] : null;
    }

    public Biome biomeAt(int realX, int realZ) {
        if (richBiomes) return column(realX, realZ).biome(); // rivers and shores need the whole column
        double x = realX / (double) zoom, z = realZ / (double) zoom, u = warpU(x, z), v = warpV(x, z);
        float y = cubic(groundY, u, v);
        for (Cone c : conesNear(x, z)) { // so a volcano's summit gets snow and peak biomes, not the plain below
            double d = hyp(wrapBlocks(x - c.x), z - c.z);
            if (d < c.reach() && !c.collapsed) y = Math.max(y, coneY(c, x, z));
        }
        DeltaSpot delta = lowlandRivers ? deltaAt(x, z, u, v) : null;
        if (delta != null && delta.plain && y <= SEA_Y + 2) return marsh(x, z, u, v);
        return biome(x, z, u, v, y);
    }

    /** Delta marsh: mangroves in the tropics, swamp in between, plain tundra or grass where it is cold. */
    private Biome marsh(double x, double z, double u, double v) {
        float t = temperature(u, v, SEA_Y);
        return t > 20 ? Biome.MANGROVE : t > 5 ? Biome.SWAMP : biome(x, z, u, v, SEA_Y);
    }

    /**
     * Climate blended between cells, then corrected to this block's own altitude (snow lines follow terrain).
     * Local weather wobbles around it at several scales, so biomes meet in ragged borders and patches of one
     * reach into the other (groves on the prairie, meadows in the forest) instead of along a smooth line.
     */
    private Biome biome(double x, double z, double u, double v, float y) {
        return biome(x, z, u, v, y, 0);
    }

    /** banks: mm of rain a river's groundwater stands in for. */
    private Biome biome(double x, double z, double u, double v, float y, float banks) {
        float t = localTemperature(x, z, u, v, y), rain = bilinear(data.precipitation, u, v) + banks;
        if (organic) rain *= 1 + 0.12f * noise(x, z, 256, 24) + 0.06f * noise(x, z, 64, 25);
        Biome b = seasons ? Biome.classify(toMetres(y), t, rain, bilinear(data.amplitude, u, v), bilinear(data.summerRain, u, v))
                : Biome.classify(toMetres(y), t, rain, iceCapBelow);
        return richBiomes ? zone(b, toMetres(y), t, rain, x, z) : b;
    }

    /**
     * Patches within the broad climate zones, the way real forests vary: dark forest where it is wettest, birch where
     * cooler, flowers where warmer, a rare cherry grove in warm wet hills; snowy and old-growth taiga; badlands on
     * dry uplands, wooded at their wetter edges.
     */
    private Biome patches(Biome b, float metres, float t, float rain, double x, double z) {
        float patch = noise(x, z, 512, 55) + 0.5f * noise(x, z, 256, 56), other = noise(x, z, 256, 57);
        if ((b == Biome.TEMPERATE_FOREST || b == Biome.GRASSLAND) && t > 12 && t < 18 && rain > 900 && metres > 400 && other > CHERRY_PATCH)
            return Biome.CHERRY_GROVE;
        if (b == Biome.TEMPERATE_FOREST) {
            if (rain > 1000 && patch > 0.15f) return Biome.DARK_FOREST;
            if (t < 12 && rain > 800 && patch < -0.45f) return Biome.OLD_GROWTH_BIRCH_FOREST;
            if (t < 14 && patch < -0.15f) return Biome.BIRCH_FOREST;
            if (t > 13 && other > 0.25f) return Biome.FLOWER_FOREST;
        }
        if (b == Biome.TAIGA) {
            if (longWinters ? winterLook(warpU(x, z), warpV(x, z), t) < 0 : t < 3) return Biome.SNOWY_TAIGA;
            if (rain > 700 && patch > 0.1f) return Biome.OLD_GROWTH_PINE_TAIGA;
        }
        // From format 44 badlands only where soft layered rock (a sedimentary basin) stands high and is cut up (the
        // Colorado Plateau, the Dakota badlands); a high desert on old shield rock is sand and stone (the Sahara, Arabia).
        if (b == Biome.DESERT && metres > (softBadlands ? SOFT_BADLANDS_METRES : BADLANDS_METRES)
                && (!softBadlands || Rock.of(data.rock[cell((int) Math.round(warpU(x, z)), (int) Math.round(warpV(x, z)))]) == Rock.BASIN))
            return rain > 200 ? Biome.WOODED_BADLANDS : Biome.BADLANDS;
        if (massifs && b == Biome.ICE_CAP) return noise(x, z, 256, 67) > ICE_SPIKE_PATCH ? Biome.ICE_SPIKES : Biome.SNOWY_PLAINS;
        return b;
    }

    /**
     * Seas by their surface temperature (currents included) and depth; mountainsides in zones by height and
     * climate: snowy slopes under the peaks, spruce groves in the cold wet, alpine meadows, windswept dry uplands.
     */
    private Biome zone(Biome b, float metres, float t, float rain, double x, double z) {
        if (metres < 0) {
            boolean deep = metres < DEEP_SEA;
            return t < -2 ? deep ? Biome.DEEP_FROZEN_OCEAN : Biome.FROZEN_OCEAN : t < 10 ? deep ? Biome.DEEP_COLD_OCEAN : Biome.COLD_OCEAN
                    : t < 20 ? deep ? Biome.DEEP_OCEAN : Biome.OCEAN : t < 25 ? deep ? Biome.DEEP_LUKEWARM_OCEAN : Biome.LUKEWARM_OCEAN : Biome.WARM_OCEAN;
        }
        if (metres < MOUNTAIN_METRES) return forestVariety ? patches(b, metres, t, rain, x, z) : b;
        if (b == Biome.SNOWY_PEAKS) return metres > JAGGED_METRES && t < -6 ? Biome.JAGGED_PEAKS : b;
        if (b == Biome.ROCKY_PEAKS) return b;
        if (t < -2) return Biome.SNOWY_SLOPES;
        if (t < 4) return rain >= 400 ? Biome.GROVE : Biome.WINDSWEPT_HILLS;
        if (t < 12 && metres > MEADOW_METRES && rain >= 350 && noise(x, z, 256, 54) > -0.3f) return Biome.MEADOW;
        if (rain < 600) return Biome.WINDSWEPT_HILLS;
        return b;
    }

    /** Annual mean °C at block height y, with the local wobble biomes see. */
    private float localTemperature(double x, double z, double u, double v, float y) {
        return temperature(u, v, y) + (organic ? 1.5f * noise(x, z, 256, 22) + 0.75f * noise(x, z, 64, 23) : 0);
    }

    /** Annual mean °C at real block (x, y, z), as the biomes see it: below 0 snow lies and water freezes. */
    public float snowTemperature(int realX, int realZ, float y) {
        double x = realX / (double) zoom, z = realZ / (double) zoom;
        double u = warpU(x, z), v = warpV(x, z);
        return winterLook(u, v, localTemperature(x, z, u, v, y));
    }

    /**
     * Minecraft has no seasons, so a block shows the state it is in most of the year. Below 0 here it is frozen for
     * more than two-thirds of it (from format 45; before, half: the annual mean): the tundra and the tree line stay
     * white, while boreal Canada and Siberia, frozen ~55% of the year with 16-18 °C summers, keep green taiga and open lakes.
     */
    private float winterLook(double u, double v, float t) {
        return longWinters ? t + FROZEN_MOST * bilinear(data.amplitude, u, v) : t;
    }

    /** Annual mean °C at block height y. */
    private float temperature(double u, double v, float y) {
        float cellTemp = bilinear(data.temperature, u, v), cellMetres = Math.max(bilinear(data.metres, u, v), 0);
        return cellTemp - Climate.LAPSE_RATE * (Math.max(toMetres(y), 0) - cellMetres);
    }

    /** A temperate, dry-footed land cell well away from the coast and off any volcano, picked by seed. */
    private int spawnCell() {
        boolean[] volcanic = new boolean[w * h];
        for (Volcano v : data.volcanoes) {
            if (v.gone()) continue;
            int reach = Float.isNaN(v.baseMetres()) ? (v.hotspot() ? 34 : 18) // legacy: roughly the widest cone of each kind
                    : (int) Math.ceil(v.radiusBlocks() / BLOCKS_PER_CELL);
            for (int dy = -reach; dy <= reach; dy++)
                for (int dx = -reach; dx <= reach; dx++)
                    if (dx * dx + dy * dy <= reach * reach) volcanic[cell((int) v.u() + dx, (int) v.v() + dy)] = true;
        }
        List<Integer> good = new ArrayList<>();
        int anyLand = 0;
        for (int g = 0; g < w * h; g++) {
            if (data.metres[g] < 0) continue;
            anyLand = g;
            Biome b = Biome.classify(data.metres[g], data.temperature[g], data.precipitation[g], iceCapBelow);
            boolean pleasant = b == Biome.GRASSLAND || b == Biome.TEMPERATE_FOREST || b == Biome.SAVANNA || b == Biome.TROPICAL_SEASONAL_FOREST;
            if (pleasant && !volcanic[g] && Float.isNaN(data.lakeMetres[g]) && data.flow[g] < Hydrology.RIVER_AREA && inland(g, 3)) good.add(g);
        }
        return good.isEmpty() ? anyLand : good.get(Math.floorMod(Long.hashCode(seed), good.size()));
    }

    private boolean inland(int g, int radius) {
        for (int dy = -radius; dy <= radius; dy++)
            for (int dx = -radius; dx <= radius; dx++) {
                int y = g / w + dy;
                if (y < 0 || y >= h || data.metres[cell(g % w + dx, y)] < 0) return false;
            }
        return true;
    }

    /** Degrees north (+) or south (-). */
    public double latitude(double z) {
        return Climate.latitudeAt(originY + 0.5 + z / blocksPerCell, h);
    }

    /** Degrees east (+) or west (-). Spawn is on the 0° meridian, so the wrap seam is the 180° line. */
    public double longitude(double x) {
        double deg = x / circumference * 360 % 360;
        return deg >= 180 ? deg - 360 : deg < -180 ? deg + 360 : deg;
    }

    /** Where block (x, z) lies on the planet map, in cells from its top-left corner; x wraps. */
    public double[] mapPosition(double x, double z) {
        double u = (originX + 0.5 + x / blocksPerCell) % w;
        return new double[]{u < 0 ? u + w : u, originY + 0.5 + z / blocksPerCell};
    }

    /** Block z of the north and south map edges: the poles. */
    public int northPoleZ() {
        return (int) Math.round((-0.5 - originY) * blocksPerCell);
    }

    public int southPoleZ() {
        return (int) Math.round((h - 0.5 - originY) * blocksPerCell);
    }

    /** Block x and z near the centre of a cell (before warping), on the copy of the planet nearest spawn. */
    public int[] blockOf(int cell) {
        return new int[]{wrap(cell % w - originX) * blocksPerCell, (cell / w - originY) * blocksPerCell};
    }

    private double warpU(double x, double z) {
        return originX + x / (double) BLOCKS_PER_CELL + WARP * (noise(x, z, 256, 1) + 0.5 * noise(x, z, 128, 3));
    }

    private double warpV(double x, double z) {
        return originY + z / (double) BLOCKS_PER_CELL + WARP * (noise(x, z, 256, 2) + 0.5 * noise(x, z, 128, 4));
    }

    // ponytail: beyond the poles the last row repeats forever; a world border or the wrap portals (step 6) hide it
    private int cell(int cx, int cy) {
        return (widthMask >= 0 ? cx & widthMask : Math.floorMod(cx, w)) + Math.clamp(cy, 0, h - 1) * w; // the mask: same answer, faster
    }

    /**
     * Distance. From format 43 a plain square root: Math.hypot guards against overflow and underflow at sizes this
     * planet never reaches, and costs about a fifth of a column's time. The two can differ in the last bit, which can
     * flip a rare block, so older planets keep Math.hypot.
     */
    private double hyp(double a, double b) {
        return fastMath ? Math.sqrt(a * a + b * b) : Math.hypot(a, b);
    }

    /** Shortest east-west step between columns, across the seam if that is shorter. */
    private int wrap(int dx) {
        return Math.floorMod(dx + w / 2, w) - w / 2;
    }

    /** Catmull-Rom bicubic over cell centres, so slopes have no creases at cell edges. */
    private float cubic(float[] f, double u, double v) {
        int iu = (int) Math.floor(u), iv = (int) Math.floor(v);
        float fu = (float) (u - iu), fv = (float) (v - iv);
        // The four columns and rows once, not per sample: the same arithmetic in the same order, so the same answer.
        int x0 = cell(iu - 1, 0), x1 = cell(iu, 0), x2 = cell(iu + 1, 0), x3 = cell(iu + 2, 0);
        int r0 = Math.clamp(iv - 1, 0, h - 1) * w, r1 = Math.clamp(iv, 0, h - 1) * w, r2 = Math.clamp(iv + 1, 0, h - 1) * w, r3 = Math.clamp(iv + 2, 0, h - 1) * w;
        return catmull(catmull(f[r0 + x0], f[r0 + x1], f[r0 + x2], f[r0 + x3], fu), catmull(f[r1 + x0], f[r1 + x1], f[r1 + x2], f[r1 + x3], fu),
                catmull(f[r2 + x0], f[r2 + x1], f[r2 + x2], f[r2 + x3], fu), catmull(f[r3 + x0], f[r3 + x1], f[r3 + x2], f[r3 + x3], fu), fv);
    }

    /** Block height of a summit this many metres high at a mountain scale, before detail noise. */
    public static int peakY(float metres, float mountainScale) {
        return Math.round(softCeiling(SEA_Y + metres * mountainScale / LAND_METRES_PER_BLOCK));
    }

    /** Summits ease into the build limit instead of being sliced flat by it. */
    static float softCeiling(float y) {
        float knee = MAX_Y - 150, room = 140;
        return y < knee ? y : knee + room * (1 - (float) Math.exp(-(y - knee) / room));
    }

    private void blur3x3(float[] f) {
        float[] src = f.clone();
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                float sum = 0;
                for (int dy = -1; dy <= 1; dy++)
                    for (int dx = -1; dx <= 1; dx++) sum += src[cell(x + dx, y + dy)];
                f[x + y * w] = sum / 9;
            }
    }

    private float bilinear(float[] f, double u, double v) {
        int iu = (int) Math.floor(u), iv = (int) Math.floor(v);
        float fu = (float) (u - iu), fv = (float) (v - iv);
        float top = f[cell(iu, iv)] + fu * (f[cell(iu + 1, iv)] - f[cell(iu, iv)]);
        float bottom = f[cell(iu, iv + 1)] + fu * (f[cell(iu + 1, iv + 1)] - f[cell(iu, iv + 1)]);
        return top + fv * (bottom - top);
    }

    private static double catmullD(double a, double b, double c, double d, double t) {
        return b + 0.5 * t * (c - a + t * (2 * a - 5 * b + 4 * c - d + t * (3 * (b - c) + d - a)));
    }

    private static float catmull(float a, float b, float c, float d, float t) {
        return b + 0.5f * t * (c - a + t * (2 * a - 5 * b + 4 * c - d + t * (3 * (b - c) + d - a)));
    }

    private static float smoothstep(float t) {
        t = Math.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }

    /** Fractal detail, roughly -1..1, periodic around the planet like everything else. */
    private float detail(double x, double z) {
        float sum = 0, amp = 1, total = 0;
        for (int spacing : DETAIL_SPACING) {
            sum += amp * fine(x, z, spacing, spacing);
            total += amp;
            amp *= 0.5f;
        }
        return sum / total;
    }

    /** Noise sized in real blocks, sampled at unzoomed (x, z): the same lattice at every zoom. */
    private float fine(double x, double z, int spacing, int salt) {
        return noise(x, z, spacing / zoom, salt);
    }

    /** 2D gradient noise in unzoomed blocks; the lattice repeats every span blocks in x, so spacing must divide it (powers of two). */
    private float noise(double x, double z, int spacing, int salt) {
        double gx = x / spacing, gz = z / spacing;
        int period = span / spacing, x0 = (int) Math.floor(gx), z0 = (int) Math.floor(gz);
        double fx = gx - x0, fz = gz - z0, sx = fx * fx * (3 - 2 * fx), sz = fz * fz * (3 - 2 * fz);
        double n00 = grad(x0, z0, fx, fz, period, salt), n10 = grad(x0 + 1, z0, fx - 1, fz, period, salt);
        double n01 = grad(x0, z0 + 1, fx, fz - 1, period, salt), n11 = grad(x0 + 1, z0 + 1, fx - 1, fz - 1, period, salt);
        double top = n00 + sx * (n10 - n00), bottom = n01 + sx * (n11 - n01);
        return (float) ((top + sz * (bottom - top)) * 1.4);
    }

    private double grad(int ix, int iz, double dx, double dz, int period, int salt) {
        long hsh = seed ^ (((period & (period - 1)) == 0 ? ix & (period - 1) : Math.floorMod(ix, period)) * 0x9E3779B97F4A7C15L) ^ (iz * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        hsh = (hsh ^ (hsh >>> 30)) * 0xBF58476D1CE4E5B9L;
        hsh = (hsh ^ (hsh >>> 27)) * 0x94D049BB133111EBL;
        double a = ((hsh ^ (hsh >>> 31)) & 1023) * (Math.PI * 2 / 1024);
        return Math.cos(a) * dx + Math.sin(a) * dz;
    }
}
