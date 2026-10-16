exec(open('tools/edit_glacier.py').read().split("C = ")[0])
H = 'src/main/java/com/realtectonics/sim/Hydrology.java'
edit(H, [
("""    public Hydrology(Planet planet, float[] rainMm, float[] warmth, int iterations, long seed, DoubleConsumer progress) {
        this.rainMm = rainMm;
        this.warmth = warmth;
        w = planet.w;
        h = planet.h;
        int n = w * h;
        elevation = new float[n];
        surface = new float[n];
        receiver = new int[n];
        flow = new float[n];
        order = new int[n];
        lake = new boolean[n];
        lakeLevel = new float[n];
        sink = new float[n];
        ice = new byte[n];
        // Roughen the smooth tectonic domes""",
 """    public Hydrology(Planet planet, float[] rainMm, float[] warmth, int iterations, long seed, DoubleConsumer progress) {
        this(planet.w, planet.h, rainMm, warmth);
        int n = w * h;
        // Roughen the smooth tectonic domes"""),
("""        // ponytail: erosion runs after tectonics, not interleaved with uplift (Cordonnier 2016); interleave if ranges look too smooth
""", ""),
("""    /** Snow builds glaciers where""",
 """    private Hydrology(int w, int h, float[] rainMm, float[] warmth) {
        this.w = w;
        this.h = h;
        this.rainMm = rainMm;
        this.warmth = warmth;
        int n = w * h;
        elevation = new float[n];
        surface = new float[n];
        receiver = new int[n];
        flow = new float[n];
        order = new int[n];
        lake = new boolean[n];
        lakeLevel = new float[n];
        sink = new float[n];
        ice = new byte[n];
    }

    /**
     * Rivers cutting the land while the plates still move (Cordonnier 2016): erosion passes on the bare crust, written
     * back into the plates, so valleys deepen as ranges rise and ride along with them. Uniform rain: the climate
     * only exists once the continents have settled.
     */
    static void erodeCrust(Planet planet, int passes) {
        float[] rain = new float[planet.w * planet.h];
        Arrays.fill(rain, 1000);
        Hydrology hy = new Hydrology(planet.w, planet.h, rain, new float[rain.length]);
        for (int g = 0; g < rain.length; g++) hy.elevation[g] = planet.height[g] - Planet.SEA_LEVEL;
        for (int i = 0; i < passes; i++) {
            hy.route();
            hy.erode();
        }
        for (int g = 0; g < rain.length; g++)
            if (hy.elevation[g] >= 0) planet.setLandHeight(g, hy.elevation[g] + Planet.SEA_LEVEL);
    }

    /** Snow builds glaciers where"""),
])
P = 'src/main/java/com/realtectonics/sim/Planet.java'
edit(P, [
("""    /** Cells inland from an active trench""",
 """    /** Erosion lowered a land cell: the crust under it thins too. */
    void setLandHeight(int g, float crust) {
        height[g] = crust;
        Plate p = plates[owner[g]];
        p.crust[local(p, g % w, g / w)] = crust;
    }

    /** Cells inland from an active trench"""),
])
D = 'src/main/java/com/realtectonics/sim/PlanetData.java'
edit(D, [
("""    static final double PLATES_SHARE = 0.6; // of the progress bar: plates take ~60% of the time, rivers the rest""",
 """    static final double PLATES_SHARE = 0.6; // of the progress bar: plates take ~60% of the time, rivers the rest
    static final int ERODE_EVERY = 10;      // tectonic steps between erosion passes while the plates move"""),
("""        for (int i = 0; i < STEPS; i++) {
            if (Thread.currentThread().isInterrupted()) throw new CancellationException();
            planet.step();
            progress.accept(PLATES_SHARE * (i + 1) / STEPS);
        }""",
 """        tectonics(planet, STEPS, done -> progress.accept(PLATES_SHARE * done));"""),
("""    private float[][] floats() {""",
 """    /** Plates moving, with rivers wearing the land down as it rises. Reports progress 0..1; interrupting cancels. */
    public static void tectonics(Planet planet, int steps, DoubleConsumer progress) {
        for (int i = 0; i < steps; i++) {
            if (Thread.currentThread().isInterrupted()) throw new CancellationException();
            planet.step();
            if ((i + 1) % ERODE_EVERY == 0) Hydrology.erodeCrust(planet, 1);
            progress.accept((i + 1) / (double) steps);
        }
    }

    private float[][] floats() {"""),
])
edit('src/main/java/com/realtectonics/sim/Prototype.java', [
("""        for (int i = 0; i < steps; i++) planet.step();""", """        PlanetData.tectonics(planet, steps, done -> {});"""),
])
print("ok")
