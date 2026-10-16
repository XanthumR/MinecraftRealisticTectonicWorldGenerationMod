exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""    static final float GLACIER_MIN_RAIN = 300; // mm: glaciers need snow to feed them""",
 """    static final float GLACIER_MIN_RAIN = 300; // mm: glaciers need snow to feed them
    static final float LOWLAND_BLOCKS = 20;    // below this height above the sea, rivers slow down, widen and meander
    static final float MEANDER = 0.8f;         // cells a lowland river swings to either side
    static final float DELTA_FLOW = 2000;      // a river this big builds a delta where it meets a shallow sea
    static final float DELTA_SHELF = -150;     // metres: in deeper water the sediment slides away instead
    static final double DELTA_SPREAD = Math.toRadians(55); // half-angle of the fan
    static final int DISTRIBUTARIES = 4;
    static final float DELTA_APRON = 4;        // blocks of run per block of drop down the delta's underwater front"""),
("""    private final Map<Integer, List<Cone>> conesByCell = new HashMap<>(); // every cell a cone reaches
""", """    private final Map<Integer, List<Cone>> conesByCell = new HashMap<>(); // every cell a cone reaches
    private final boolean lowlandRivers; // meanders and deltas; off on planets saved before them, so their rivers stay put
    private final Map<Integer, List<Delta>> deltasByCell = new HashMap<>(); // by warped cell

    /** A river mouth's fan of sediment, in warped cell coordinates: apex on the last land cell, pointing out to sea. */
    private record Delta(double u, double v, double dirU, double dirV, float radius, float half) {}

    /** Where a column sits on a delta: on its plain (marsh or a channel), or on the underwater front beyond it. */
    private record DeltaSpot(boolean plain, boolean channel, float channelHalf, float beyondFront) {}
"""),
("""        offshoreSlope = shelves ? 16 : 60;""",
 """        offshoreSlope = shelves ? 16 : 60;
        lowlandRivers = data.format >= 7;"""),
("""                    conesByCell.computeIfAbsent(cell(cu + dx, cv + dy), k -> new ArrayList<>()).add(cone);
        }
    }""",
 """                    conesByCell.computeIfAbsent(cell(cu + dx, cv + dy), k -> new ArrayList<>()).add(cone);
        }
        for (int c = 0; lowlandRivers && c < w * h; c++) {
            int r = data.receiver[c];
            if (r < 0 || data.metres[c] < 0 || data.metres[r] >= 0 || data.metres[r] < DELTA_SHELF || data.flow[c] <= DELTA_FLOW
                    || data.ice[c] == Hydrology.FJORD || !Float.isNaN(data.lakeMetres[c])) continue;
            double du = wrap(r % w - c % w), dv = r / w - c / w, len = Math.hypot(du, dv);
            float radius = Math.min(224, 48 + 0.6f * (float) Math.sqrt(data.flow[c]));
            Delta delta = new Delta(c % w, c / w, du / len, dv / len, radius, Math.min(40, 2 + 0.12f * (float) Math.sqrt(data.flow[c])));
            int reach = (int) Math.ceil(radius / BLOCKS_PER_CELL) + 2;
            for (int dy = -reach; dy <= reach; dy++)
                for (int dx = -reach; dx <= reach; dx++)
                    deltasByCell.computeIfAbsent(cell(c % w + dx, c / w + dy), k -> new ArrayList<>()).add(delta);
        }
    }

    /** The delta this warped position lies on, if any; the one it is deepest inside wins. */
    private DeltaSpot deltaAt(int x, int z, double u, double v) {
        DeltaSpot best = null;
        float bestBeyond = Float.MAX_VALUE;
        for (Delta d : deltasByCell.getOrDefault(cell((int) Math.round(u), (int) Math.round(v)), List.of())) {
            double pu = wrapCells(u - d.u) * BLOCKS_PER_CELL, pv = (v - d.v) * BLOCKS_PER_CELL;
            double along = pu * d.dirU + pv * d.dirV, across = pu * d.dirV - pv * d.dirU, r = Math.hypot(pu, pv);
            double angle = Math.atan2(across, along) + 0.12 * noise(x, z, 48, 14); // distributaries wander
            if (Math.abs(angle) > DELTA_SPREAD + 0.3) continue;
            float spread = (float) (Math.abs(angle) / DELTA_SPREAD), front = d.radius * (1 + 0.2f * noise(x, z, 64, 13)) * (1 - 0.3f * spread * spread);
            float beyond = (float) r - front + (spread > 1 ? (spread - 1) * d.radius : 0);
            if (beyond >= bestBeyond) continue;
            // Lateral distance to the nearest distributary: they fan out evenly from the apex.
            double gap = 2 * DELTA_SPREAD * 0.8 / DISTRIBUTARIES, offset = angle + DELTA_SPREAD * 0.8;
            double nearest = Math.abs(offset - gap * (Math.floor(offset / gap) + 0.5));
            float half = Math.max(2, d.half * 0.6f);
            bestBeyond = beyond;
            best = new DeltaSpot(beyond < 0 && spread <= 1, nearest * r < half, half, beyond);
        }
        return best;
    }

    /** Shortest east-west offset in cells, across the seam if that is shorter. */
    private double wrapCells(double du) {
        return du - w * Math.floor(du / w + 0.5);
    }"""),
("""        River river = nearestRiver(u, v, (byte) 0, Hydrology.RIVER_AREA), valley = nearestRiver(u, v, Hydrology.GLACIATED, Hydrology.GLACIER_AREA);""",
 """        // Lowland rivers swing in meanders and spread wider; the query point wanders instead of the channel.
        float low = lowlandRivers ? Math.clamp(1 - (cubic(groundY, u, v) - SEA_Y) / LOWLAND_BLOCKS, 0, 1) : 0;
        River river = nearestRiver(u + low * MEANDER * noise(x, z, 128, 11), v + low * MEANDER * noise(x, z, 128, 12), (byte) 0, Hydrology.RIVER_AREA)
                .widened(1 + 0.5f * low);
        River valley = nearestRiver(u, v, Hydrology.GLACIATED, Hydrology.GLACIER_AREA);"""),
("""        // Lakes: shores follow the blurred mask, and everything near a lake stands at least as high as its""",
 """        // Deltas: the river drops its sediment as a low fan of marsh pushed out into the sea, crossed by channels.
        boolean marsh = false;
        DeltaSpot delta = lowlandRivers && !inFjord ? deltaAt(x, z, u, v) : null;
        if (delta != null && ground <= SEA_Y + 2) {
            if (delta.plain && delta.channel) {
                ground = Math.min(ground, SEA_Y - 2 - (int) (delta.channelHalf / 4));
                water = SEA_Y - 1;
            } else if (delta.plain) {
                ground = Math.max(ground, noise(x, z, 24, 15) > 0.45f ? SEA_Y - 2 : SEA_Y); // pools among the reeds
                marsh = true;
            } else {
                ground = Math.max(ground, SEA_Y - 2 - Math.round(delta.beyondFront / DELTA_APRON));
            }
        }

        // Lakes: shores follow the blurred mask, and everything near a lake stands at least as high as its"""),
("""        return new Column(ground, water, lava, ice, biome(u, v, Math.max(ground, ice)), fold, volcanic, rock);""",
 """        return new Column(ground, water, lava, ice, marsh && ground >= SEA_Y - 2 ? marsh(u, v) : biome(u, v, Math.max(ground, ice)), fold, volcanic, rock);"""),
("""    private record River(float edge, float half, float y, byte ice, float flow) {}""",
 """    private record River(float edge, float half, float y, byte ice, float flow) {
        River widened(float k) {
            return this == NO_RIVER ? this : new River(edge - half * (k - 1), half * k, y, ice, flow);
        }
    }"""),
("""            if (d < c.reach()) y = Math.max(y, coneY(c, d));
        }
        return biome(u, v, y);
    }""",
 """            if (d < c.reach()) y = Math.max(y, coneY(c, d));
        }
        DeltaSpot delta = lowlandRivers ? deltaAt(x, z, u, v) : null;
        if (delta != null && delta.plain && y <= SEA_Y + 2) return marsh(u, v);
        return biome(u, v, y);
    }

    /** Delta marsh: mangroves in the tropics, swamp in between, plain tundra or grass where it is cold. */
    private Biome marsh(double u, double v) {
        float t = temperature(u, v, SEA_Y);
        return t > 20 ? Biome.MANGROVE : t > 5 ? Biome.SWAMP : biome(u, v, SEA_Y);
    }"""),
])
B = 'src/main/java/com/realtectonics/sim/Biome.java'
edit(B, [
("""    SNOWY_PEAKS(0xe6eef4), ROCKY_PEAKS(0x8c8c8c);""",
 """    SNOWY_PEAKS(0xe6eef4), ROCKY_PEAKS(0x8c8c8c), SWAMP(0x4c6e3c), MANGROVE(0x3c5a32); // the last two only on deltas"""),
])
S = 'src/main/java/com/realtectonics/mod/TectonicBiomeSource.java'
edit(S, [
("""            case ROCKY_PEAKS -> "stony_peaks";""", """            case ROCKY_PEAKS -> "stony_peaks";
            case SWAMP -> "swamp";
            case MANGROVE -> "mangrove_swamp";"""),
])
G = 'src/main/java/com/realtectonics/mod/TectonicChunkGenerator.java'
edit(G, [
("""            SNOW_PACK = new Surface(""", """            MUD = new Surface(Blocks.MUD.defaultBlockState(), Blocks.MUD.defaultBlockState(), false),
            SNOW_PACK = new Surface("""),
("""        if (c.ground() <= Terrain.SEA_Y + 1) return BEACH;""",
 """        boolean marsh = c.biome() == Biome.SWAMP || c.biome() == Biome.MANGROVE;
        if (c.ground() <= Terrain.SEA_Y + 1 && !marsh) return BEACH;"""),
("""            case TEMPERATE_RAINFOREST -> PODZOL;""", """            case TEMPERATE_RAINFOREST -> PODZOL;
            case MANGROVE -> MUD;"""),
])
D = 'src/main/java/com/realtectonics/sim/PlanetData.java'
edit(D, [
("""    private static final int FORMAT = 6; // 2 volcanoes, 3 rock provinces, 4 volcano bases, 5 glaciers, 6 shelves; older files load without them""",
 """    private static final int FORMAT = 7; // 2 volcanoes, 3 rock provinces, 4 volcano bases, 5 glaciers, 6 shelves, 7 deltas (no new data); older files load without them"""),
])
print("ok")
