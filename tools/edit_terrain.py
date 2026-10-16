p = 'src/main/java/com/realtectonics/sim/Terrain.java'
s = open(p, encoding='utf-8').read()


def r(a, b):
    global s
    assert s.count(a) == 1, a
    s = s.replace(a, b)


r("""import java.util.Map;
""", """import java.util.Map;
import java.util.stream.IntStream;
""")
r("""    static final double WARP = 1.2; // cells: bends coastlines, rivers and biome borders off the cell grid
""", """    static final double WARP = 1.2; // cells: bends coastlines, rivers and biome borders off the cell grid
    static final double SUMMIT = 0.4; // of a cone's radius: the part too fine for the cell grid, shaped per block
    public static final int WATER_COLOR = 0x1e46b4, LAVA_COLOR = 0xff5a14;
""")
r("""    /** A volcano in blocks. Strato cones are concave like Fuji; shields are broad domes like Hawaii. */
    private record Cone(double x, double z, float peak, float height, float radius, float craterRadius, float craterDepth,
                        boolean shield, boolean active) {
        /** Surface at distance d from the summit, ignoring the crater. */
        float surface(double d) {
            double t = d / radius;
            return (float) (peak - height * (shield ? t * t : Math.sqrt(t)));
        }
    }""", """    /**
     * A volcano in blocks. Its flanks are already in the planet's cells (rivers run off them); per block we only
     * sharpen the summit and cut the crater. Planets saved before that (legacy) get the whole cone per block.
     */
    private record Cone(Volcano v, double x, double z, float radius, float craterRadius, float craterDepth, float height, boolean legacy) {
        double reach() {
            return legacy ? radius : SUMMIT * radius;
        }
    }""")
r("""        for (Volcano v : data.volcanoes) {
            if (v.gone()) continue;
            float base = cubic(groundY, v.u(), v.v()), baseMetres = toMetres(base);
            // Metres above the ground it stands on. Island arcs rise from deep water, so they get the depth back.
            float rise = v.hotspot() ? 7000 * (1 - v.age() / (float) Volcano.HOTSPOT_LIFETIME)
                    : 2500 + 1500 * v.size() + Math.max(0, -baseMetres * 0.8f);
            float peak = softCeiling(toY(baseMetres + rise)), height = peak - base;
            if (height < 8) continue;
            float radius = height * (v.hotspot() ? 3.5f : 1.6f);
            Cone cone = new Cone(wrapBlocks((v.u() - originX) * BLOCKS_PER_CELL), (v.v() - originY) * BLOCKS_PER_CELL, peak, height, radius,
                    radius * (v.hotspot() ? 0.08f : 0.06f) + 5, Math.min(25, 0.1f * height), v.hotspot(), v.active());
            int reach = (int) Math.ceil(radius / BLOCKS_PER_CELL) + 1, cu = Math.round(v.u()), cv = Math.round(v.v());""",
  """        for (Volcano v : data.volcanoes) {
            if (v.gone()) continue;
            boolean legacy = Float.isNaN(v.baseMetres());
            Volcano placed = legacy ? v.withBase(toMetres(cubic(groundY, v.u(), v.v()))) : v;
            float radius = placed.radiusBlocks(), height = toY(placed.surfaceMetres(0)) - toY(placed.baseMetres());
            if (height < 8) continue;
            Cone cone = new Cone(placed, wrapBlocks((v.u() - originX) * BLOCKS_PER_CELL), (v.v() - originY) * BLOCKS_PER_CELL, radius,
                    radius * (v.hotspot() ? 0.08f : 0.06f) + 5, Math.min(25, 0.1f * height), height, legacy);
            int reach = (int) Math.ceil(cone.reach() / BLOCKS_PER_CELL) + 1, cu = Math.round(v.u()), cv = Math.round(v.v());""")
r("""    /** Shortest east-west offset in blocks, across the seam if that is shorter. */
    private double wrapBlocks(double dx) {""", """    /** Height in blocks above sea level at the default mountain scale; fixes the size of things in the planet's cells. */
    static float defaultBlocks(float metres) {
        return metres / (metres >= 0 ? LAND_METRES_PER_BLOCK : SEA_METRES_PER_BLOCK);
    }

    private float coneY(Cone c, double d) {
        return softCeiling(toY(c.v.surfaceMetres(d)));
    }

    /** Shortest east-west offset in blocks, across the seam if that is shorter. */
    private double wrapBlocks(double dx) {""")
r("""        // Rivers: the nearest channel among segments from each river cell to its receiver.
        int iu = (int) Math.round(u), iv = (int) Math.round(v);
        float best = Float.MAX_VALUE, bestHalf = 0, riverY = 0;
        for (int dy = -2; dy <= 2; dy++)
            for (int dx = -2; dx <= 2; dx++) {
                int cy = iv + dy;
                if (cy < 0 || cy >= h) continue;
                int c = cell(iu + dx, cy), r = data.receiver[c];
                if (r < 0 || data.flow[c] <= Hydrology.RIVER_AREA || data.metres[c] < 0) continue;
                double au = iu + dx, bu = au + wrap(r % w - c % w), bv = r / w;
                double su = bu - au, sv = bv - cy;
                double t = Math.clamp(((u - au) * su + (v - cy) * sv) / (su * su + sv * sv), 0, 1);
                float dist = (float) Math.hypot(u - au - t * su, v - cy - t * sv) * BLOCKS_PER_CELL;
                float half = Math.min(40, 2 + 0.12f * (float) Math.sqrt(data.flow[c]));
                if (dist - half < best) {
                    best = dist - half;
                    bestHalf = half;
                    riverY = (float) (groundY[c] + t * (Math.max(groundY[r], SEA_Y - 1) - groundY[c]));
                }
            }
        float bank = 8 + 2 * bestHalf;
        if (best < bank && natural >= SEA_Y - 1) { // at the mouth the sea takes over
            int ry = Math.max((int) Math.floor(riverY), SEA_Y - 1);
            if (best <= 0) {
                ground = Math.min(ground, ry - 1 - (int) Math.min(5, bestHalf / 4));
                water = ry;
            } else { // valley sides ease from just above the water back to the natural slope
                float s = smoothstep(best / bank);
                ground = Math.round(ry + 1 + s * (ground - ry - 1));
            }
        }
""", """        int iu = (int) Math.round(u), iv = (int) Math.round(v);
        River river = nearestRiver(u, v);
        float bank = 8 + 2 * river.half;
        if (river.edge < bank && natural >= SEA_Y - 1) { // at the mouth the sea takes over
            int ry = Math.max((int) Math.floor(river.y), SEA_Y - 1);
            if (river.edge <= 0) {
                ground = Math.min(ground, ry - 1 - (int) Math.min(5, river.half / 4));
                water = ry;
            } else { // valley sides ease from just above the water back to the natural slope
                float s = smoothstep(river.edge / bank);
                ground = Math.round(ry + 1 + s * (ground - ry - 1));
            }
        }
""")
r("""        // Volcanoes stand on top of everything else. Craters hold lava while active, rainwater once extinct;
        // the pool sits well below the rim, so the crater always encloses it.
        int lava = Column.NO_WATER;
        boolean volcanic = false;
        for (Cone c : conesNear(x, z)) {
            double d = Math.hypot(wrapBlocks(x - c.x), z - c.z);
            if (d >= c.radius) continue;
            float rim = c.surface(c.craterRadius), pool = rim - c.craterDepth * 0.6f;
            float s = d < c.craterRadius ? rim - c.craterDepth * (1 - (float) (d * d / (c.craterRadius * c.craterRadius))) : c.surface(d);
            s += rough * (1 + 0.02f * c.height);
            if (s > ground) {
                ground = Math.round(s);
                // Fresh lava and ash near the top; plants have long since covered the lower flanks and extinct cones.
                volcanic = d < c.radius * ((c.active ? 0.35 : 0.12) + 0.08 * noise(x, z, 64, 7));
            }
            if (d < c.craterRadius && ground < pool) {
                if (c.active && pool > SEA_Y) lava = (int) pool;
                else water = Math.max(water, (int) pool);
            }
        }""", """        // Volcano summits, sharper than the cell grid can hold, and their craters. Craters hold lava while
        // active, rainwater once extinct; the pool sits well below the rim, so the crater always encloses it.
        int lava = Column.NO_WATER;
        boolean volcanic = false;
        for (Cone c : conesNear(x, z)) {
            double d = Math.hypot(wrapBlocks(x - c.x), z - c.z);
            if (d >= c.reach()) continue;
            float rim = coneY(c, c.craterRadius), pool = rim - c.craterDepth * 0.6f, wobble = rough * (1 + 0.02f * c.height);
            if (d < c.craterRadius) {
                ground = Math.round(rim - c.craterDepth * (1 - (float) (d * d / (c.craterRadius * c.craterRadius))) + wobble);
            } else {
                float s = coneY(c, d) + wobble, fade = c.legacy ? 1 : smoothstep((float) ((c.reach() - d) / (0.1 * c.radius)));
                if (s > ground) ground = Math.round(ground + fade * (s - ground));
            }
            // Fresh lava and ash near the top; plants have long since covered the lower flanks and extinct cones.
            volcanic |= d < c.radius * ((c.v.active() ? 0.35 : 0.12) + 0.08 * noise(x, z, 64, 7));
            if (d < c.craterRadius && ground < pool) {
                if (c.v.active() && pool > SEA_Y) lava = (int) pool;
                else water = Math.max(water, (int) pool);
            }
        }""")
r("""    /** Highest lake surface within two cells: one level per lake, shared by its water and its shores. */""",
  """    /** The river channel nearest a warped position: blocks outside its edge (negative inside), half width, water level. */
    private record River(float edge, float half, float y) {}

    private static final River NO_RIVER = new River(Float.MAX_VALUE, 0, 0);

    /** Segments run from each river cell to its receiver; the nearest edge wins. */
    private River nearestRiver(double u, double v) {
        int iu = (int) Math.round(u), iv = (int) Math.round(v);
        River best = NO_RIVER;
        for (int dy = -2; dy <= 2; dy++)
            for (int dx = -2; dx <= 2; dx++) {
                int cy = iv + dy;
                if (cy < 0 || cy >= h) continue;
                int c = cell(iu + dx, cy), r = data.receiver[c];
                if (r < 0 || data.flow[c] <= Hydrology.RIVER_AREA || data.metres[c] < 0) continue;
                double au = iu + dx, bu = au + wrap(r % w - c % w), bv = r / w;
                double su = bu - au, sv = bv - cy;
                double t = Math.clamp(((u - au) * su + (v - cy) * sv) / (su * su + sv * sv), 0, 1);
                float dist = (float) Math.hypot(u - au - t * su, v - cy - t * sv) * BLOCKS_PER_CELL;
                float half = Math.min(40, 2 + 0.12f * (float) Math.sqrt(data.flow[c]));
                if (dist - half < best.edge)
                    best = new River(dist - half, half, (float) (groundY[c] + t * (Math.max(groundY[r], SEA_Y - 1) - groundY[c])));
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
                int g = cx + cy * w, x = wrap(cx - originX) * BLOCKS_PER_CELL, z = (cy - originY) * BLOCKS_PER_CELL;
                Column c = column(x, z);
                top[g] = c.ground();
                boolean sea = c.water() == SEA_Y - 1 && c.ground() < SEA_Y - 1;
                // Rivers are narrower than a pixel, so draw them wherever a channel passes within half a pixel.
                boolean river = !sea && (c.water() != Column.NO_WATER || nearestRiver(warpU(x, z), warpV(x, z)).edge < BLOCKS_PER_CELL / 2f);
                rgb[g] = c.lava() != Column.NO_WATER ? LAVA_COLOR : river ? WATER_COLOR : geology ? c.rock().color : c.biome().color;
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

    /** Highest lake surface within two cells: one level per lake, shared by its water and its shores. */""")
r("""        for (Cone c : conesNear(x, z)) { // so a volcano's summit gets snow and peak biomes, not the plain below
            double d = Math.hypot(wrapBlocks(x - c.x), z - c.z);
            if (d < c.radius) y = Math.max(y, c.surface(d));
        }""", """        for (Cone c : conesNear(x, z)) { // so a volcano's summit gets snow and peak biomes, not the plain below
            double d = Math.hypot(wrapBlocks(x - c.x), z - c.z);
            if (d < c.reach()) y = Math.max(y, coneY(c, d));
        }""")
open(p, 'w', encoding='utf-8', newline='').write(s)

# The per-cell map renderers in PlanetData give way to Terrain.mapColors, which draws what the game builds.
p = 'src/main/java/com/realtectonics/sim/PlanetData.java'
s = open(p, encoding='utf-8').read()
start = s.index("    /** RGB per cell: geological provinces")
end = s.index("    private float[][] floats()")
s = s[:start] + s[end:]
open(p, 'w', encoding='utf-8', newline='').write(s)
print("edited")
