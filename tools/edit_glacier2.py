exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""    public static final int WATER_COLOR = 0x1e46b4, LAVA_COLOR = 0xff5a14;""",
 """    public static final int WATER_COLOR = 0x1e46b4, LAVA_COLOR = 0xff5a14, GLACIER_COLOR = 0xc8e6f5;
    static final float GLACIER_MELT = -1;      // °C annual mean at a valley floor: colder, and today's glaciers still fill it
    static final float GLACIER_MIN_RAIN = 300; // mm: glaciers need snow to feed them"""),
("""     * ground: top solid block. water, lava: top fluid block, or NO_WATER. fold: vertical shift of the rock layers,
     * in blocks. volcanic: on a volcano's flanks, where its own rock shows. rock: the bedrock's province.
     */
    public record Column(int ground, int water, int lava, Biome biome, int fold, boolean volcanic, Rock rock) {""",
 """     * ground: top solid block. water, lava: top fluid block, or NO_WATER. ice: top of glacier ice standing on the
     * ground, or NO_WATER. fold: vertical shift of the rock layers, in blocks. volcanic: on a volcano's flanks, where
     * its own rock shows. rock: the bedrock's province.
     */
    public record Column(int ground, int water, int lava, int ice, Biome biome, int fold, boolean volcanic, Rock rock) {"""),
("""        int iu = (int) Math.round(u), iv = (int) Math.round(v);
        River river = nearestRiver(u, v);
        float bank = 8 + 2 * river.half;
        if (river.edge < bank && natural >= SEA_Y - 1) { // at the mouth the sea takes over
            int ry = Math.max((int) Math.floor(river.y), SEA_Y - 1);""",
 """        int iu = (int) Math.round(u), iv = (int) Math.round(v);
        River river = nearestRiver(u, v);
        int ry = Math.max((int) Math.floor(river.y), SEA_Y - 1), ice = Column.NO_WATER;
        // Ice Age glaciers left U-shaped troughs: flat floors, walls steepening upwards. Where they cut below today's
        // sea, it flooded them into fjords. In the coldest valleys glaciers still fill the trough.
        if (river.ice != 0) {
            float trough = Math.min(64, 40 + 0.3f * (float) Math.sqrt(river.flow)), t = (river.edge + river.half) / trough;
            boolean fjord = river.ice == Hydrology.FJORD;
            float floor = fjord ? SEA_Y - 2 - Math.min(40, 8 + 0.1f * (float) Math.sqrt(river.flow)) : ry + 1;
            if (t < 1) ground = Math.min(ground, Math.round(floor + (ground - floor) * t * t));
            float thick = Math.min(30, (GLACIER_MELT - temperature(u, v, ry + 1)) * 6);
            if (!fjord && t < 1 && thick > 1 && bilinear(data.precipitation, u, v) >= GLACIER_MIN_RAIN)
                ice = Math.round(floor + thick * (1 - 0.5f * t * t)); // a little convex, like a real glacier's surface
        }
        float bank = 8 + 2 * river.half;
        if (river.ice != Hydrology.FJORD && river.edge < bank && natural >= SEA_Y - 1) { // at the mouth the sea takes over"""),
("""        if (water == Column.NO_WATER && lava == Column.NO_WATER && ground < SEA_Y - 1) water = SEA_Y - 1;
        ground = Math.clamp(ground, MIN_Y + 1, MAX_Y - 2);""",
 """        if (ice <= ground || lava != Column.NO_WATER) ice = Column.NO_WATER;
        if (ice != Column.NO_WATER) water = Column.NO_WATER; // the river runs out from under the snout
        if (water == Column.NO_WATER && lava == Column.NO_WATER && ice == Column.NO_WATER && ground < SEA_Y - 1) water = SEA_Y - 1;
        ground = Math.clamp(ground, MIN_Y + 1, MAX_Y - 2);"""),
("""        return new Column(ground, water, lava, biome(u, v, ground), fold, volcanic, rock);""",
 """        return new Column(ground, water, lava, ice, biome(u, v, Math.max(ground, ice)), fold, volcanic, rock);"""),
("""    /** The river channel nearest a warped position: blocks outside its edge (negative inside), half width, water level. */
    private record River(float edge, float half, float y) {}

    private static final River NO_RIVER = new River(Float.MAX_VALUE, 0, 0);""",
 """    /** The river channel nearest a warped position: blocks outside its edge (negative inside), half width, water level, its cell's ice. */
    private record River(float edge, float half, float y, byte ice, float flow) {}

    private static final River NO_RIVER = new River(Float.MAX_VALUE, 0, 0, (byte) 0, 0);"""),
("""                    best = new River(dist - half, half, (float) (groundY[c] + t * (Math.max(groundY[r], SEA_Y - 1) - groundY[c])));""",
 """                    best = new River(dist - half, half, (float) (groundY[c] + t * (Math.max(groundY[r], SEA_Y - 1) - groundY[c])), data.ice[c], data.flow[c]);"""),
("""                            if (s.water() != Column.NO_WATER && !(s.water() == SEA_Y - 1 && s.ground() < SEA_Y - 1)) {""",
 """                            if (s.water() != Column.NO_WATER) { // a fjord counts too: it is narrower than a pixel"""),
("""                rgb[g] = c.lava() != Column.NO_WATER ? LAVA_COLOR : river ? WATER_COLOR : geology ? c.rock().color : c.biome().color;""",
 """                rgb[g] = c.lava() != Column.NO_WATER ? LAVA_COLOR : river ? WATER_COLOR : c.ice() != Column.NO_WATER && !geology ? GLACIER_COLOR
                        : geology ? c.rock().color : c.biome().color;"""),
("""    private Biome biome(double u, double v, float y) {
        float m = toMetres(y);
        float cellTemp = bilinear(data.temperature, u, v), cellMetres = Math.max(bilinear(data.metres, u, v), 0);
        float t = cellTemp - Climate.LAPSE_RATE * (Math.max(m, 0) - cellMetres);
        return Biome.classify(m, t, bilinear(data.precipitation, u, v));
    }""",
 """    private Biome biome(double u, double v, float y) {
        return Biome.classify(toMetres(y), temperature(u, v, y), bilinear(data.precipitation, u, v));
    }

    /** Annual mean °C at block height y. */
    private float temperature(double u, double v, float y) {
        float cellTemp = bilinear(data.temperature, u, v), cellMetres = Math.max(bilinear(data.metres, u, v), 0);
        return cellTemp - Climate.LAPSE_RATE * (Math.max(toMetres(y), 0) - cellMetres);
    }"""),
])
print("ok")
