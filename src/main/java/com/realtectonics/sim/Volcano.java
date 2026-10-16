package com.realtectonics.sim;

/**
 * A volcano at cell (u, v). Arc volcanoes stand behind subduction zones; hotspot volcanoes were erupted by a
 * plume fixed in the mantle and carried off by their plate, so older ones lie further down the chain.
 *
 * @param age        steps since a hotspot volcano erupted (0 for arc volcanoes, which are all active arcs)
 * @param size       0..1, varies height and which arc volcanoes are still erupting
 * @param baseMetres the ground it was built on; NaN for planets saved before volcanoes shaped the rivers
 * @param spread     footprint radius per block of height
 */
public record Volcano(float u, float v, boolean hotspot, int age, float size, float baseMetres, float spread) {
    static final float ARC_SPREAD = 1.6f, SHIELD_SPREAD = 2.2f;
    static final float OLD_SHIELD_SPREAD = 3.5f; // planets saved before tuning: young islands ~1,400 blocks across

    /** compact: planets from format 9 on; older ones keep their wide shields so their saved flanks still fit. */
    static float spread(boolean hotspot, boolean compact) {
        return hotspot ? (compact ? SHIELD_SPREAD : OLD_SHIELD_SPREAD) : ARC_SPREAD;
    }

    public static final int HOTSPOT_LIFETIME = 200; // steps until a hotspot volcano has sunk and eroded away
    static final int HOTSPOT_ACTIVE = 30;           // steps a fresh hotspot volcano keeps a lava lake

    public boolean active() {
        return hotspot ? age < HOTSPOT_ACTIVE : size > 0.6f;
    }

    public boolean gone() {
        return hotspot && age >= HOTSPOT_LIFETIME;
    }

    Volcano withBase(float metres) {
        return new Volcano(u, v, hotspot, age, size, metres, spread);
    }

    /** Metres it rises above its base. Island arcs rise from deep water, so they get the depth back. */
    public float riseMetres() {
        return hotspot ? 7000 * (1 - age / (float) HOTSPOT_LIFETIME) : 2500 + 1500 * size + Math.max(0, -baseMetres * 0.8f);
    }

    /** Footprint radius in blocks. Fixed by the planet; the mountain-height setting only makes cones steeper. */
    public float radiusBlocks() {
        float height = Terrain.defaultBlocks(baseMetres + riseMetres()) - Terrain.defaultBlocks(baseMetres);
        return height * spread;
    }

    static final float CALDERA_SHARE = 0.3f;        // of the footprint radius: Crater Lake is ~9 km across on a ~30 km volcano
    static final float CALDERA_DEPTH = 1200;        // metres a caldera floor sinks below its rim at most
    static final float CALDERA_SHIELDS = 0.35f;     // share of shield volcanoes past their active years that collapsed
    static final int CALDERA_AGE = 120;             // steps after which a shield's caldera has worn away

    /**
     * Whether the volcano's summit fell in. A volcano that drains a large magma chamber in one great eruption loses
     * the roof over it. Bigger arc volcanoes had bigger chambers, so they collapse more often; shields collapse in
     * their late active years and the caldera wears away as the island ages. Never while still erupting.
     */
    public boolean caldera() {
        if (active()) return false;
        long hsh = Float.floatToIntBits(u) * 0x9E3779B97F4A7C15L ^ Float.floatToIntBits(v) * 0xC2B2AE3D27D4EB4FL;
        hsh = (hsh ^ (hsh >>> 31)) * 0xBF58476D1CE4E5B9L;
        double roll = ((hsh ^ (hsh >>> 29)) & 0xFFFFFF) / (double) 0x1000000;
        return hotspot ? age < CALDERA_AGE && roll < CALDERA_SHIELDS : roll < size;
    }

    static final float LOBE_MAX = 1.9f;     // varied cones: widest reach over the mean footprint (wide volcano, longest lobe)
    static final float SCAR_SHARE = 0.3f;   // varied cones: share of arc volcanoes whose flank slid away (Mount St Helens, Stromboli)
    static final float SCAR_DEPTH = 0.35f;  // of the height a collapse scar takes off the summit
    static final double SCAR_HALF = 0.45;   // radians: half the scar's width (format 36)
    static final double SCAR_WIDTH = 0.25;  // of the footprint radius: half the scar's width (format 42)
    public static final int ROUND = 0, VARIED = 1, SMOOTH_SCARS = 2; // cone styles: before format 36, formats 36-41, from 42

    /** 0..1, fixed per volcano and per k: what makes this volcano unlike its neighbours. */
    private double roll(int k) {
        long hsh = Float.floatToIntBits(u) * 0x9E3779B97F4A7C15L ^ Float.floatToIntBits(v) * 0xC2B2AE3D27D4EB4FL ^ k * 0x94D049BB133111EBL;
        hsh = (hsh ^ (hsh >>> 31)) * 0xBF58476D1CE4E5B9L;
        return ((hsh ^ (hsh >>> 29)) & 0xFFFFFF) / (double) 0x1000000;
    }

    /**
     * From format 36 (varied = true): the surface dx, dz blocks from the summit, each volcano its own. Runny magma
     * builds wide, gentle cones and sticky magma narrow, steep ones; lava runs further down some sides than others,
     * so the footprint has lobes; and some arc volcanoes lost a flank in a landslide, leaving a horseshoe scar open
     * downhill. The profile runs in blocks, not metres: the sea is squeezed into fewer blocks than the land, so a
     * cone shaped in metres stands on its submarine flank as a narrow spire with a tiny island.
     */
    public float surfaceMetres(double dx, double dz, int style) {
        double d = Math.hypot(dx, dz);
        if (style == ROUND) return surfaceMetres(d);
        double a = Math.atan2(dz, dx), lobes = 1 + (0.25 + 0.2 * roll(1)) * (0.5 * Math.cos(a - 7 * roll(2)) + 0.3 * Math.cos(2 * a - 7 * roll(3)) + 0.2 * Math.cos(3 * a - 7 * roll(4)));
        double t = Math.min(1, d / (radiusBlocks() * (0.8 + 0.5 * roll(5)) * lobes));
        double drop = hotspot ? Math.pow(t, 1.6 + 0.8 * roll(6)) : Math.pow(t, 0.4 + 0.35 * roll(6)); // shields domed like Hawaii, strato cones concave like Fuji
        float b0 = Terrain.defaultBlocks(baseMetres), b1 = Terrain.defaultBlocks(baseMetres + riseMetres());
        double scar = 0;
        if (!hotspot && roll(7) < SCAR_SHARE) {
            double rel = a - 2 * Math.PI * roll(8);
            if (style == VARIED) scar = SCAR_DEPTH * (1 - smooth(Math.abs(Math.IEEEremainder(rel, 2 * Math.PI)) / SCAR_HALF)) * (1 - smooth(t / 0.7));
            else { // from format 42 the scar is a trough of even width in blocks: an angle squeezed its walls into a blade at the summit
                double side = Math.cos(rel) > 0 ? Math.abs(d * Math.sin(rel)) : d; // behind the summit only its head bites in
                scar = SCAR_DEPTH * (1 - smooth(side / (SCAR_WIDTH * radiusBlocks()))) * (1 - smooth(t / 0.7));
            }
        }
        return Terrain.defaultMetres(b0 + (b1 - b0) * (float) (1 - drop - scar));
    }

    private static double smooth(double x) {
        x = Math.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    /** Surface in metres at d blocks from the summit, ignoring the crater: strato cones concave like Fuji, shields domed like Hawaii. */
    public float surfaceMetres(double d) {
        double t = Math.min(1, d / radiusBlocks());
        return (float) (baseMetres + riseMetres() * (1 - (hotspot ? t * t : Math.sqrt(t))));
    }
}
