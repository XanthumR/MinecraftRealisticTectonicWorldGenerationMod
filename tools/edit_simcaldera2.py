exec(open('tools/edit_glacier.py').read().split("C = ")[0])
H = 'src/main/java/com/realtectonics/sim/Hydrology.java'
edit(H, [
("""        // Volcanoes go in before the rivers, so water runs off their flanks instead of under them.
        // Every base is measured before any cone goes up: neighbours in a chain must not stack on each other.
        for (Volcano v : planet.volcanoes())
            if (!v.gone()) volcanoes.add(v.withBase(Planet.metres(elevation[(int) v.u() + (int) v.v() * w])));
        // Overlapping cones blend with a smooth maximum, so neighbours in a chain merge without a straight crease.
        float[] cones = new float[n];
        Arrays.fill(cones, Float.NaN);
        for (Volcano placed : volcanoes) {
            int cu = (int) placed.u(), cv = (int) placed.v();
            float radius = placed.radiusBlocks() / Terrain.BLOCKS_PER_CELL;
            for (int dy = -(int) radius; dy <= radius; dy++)
                for (int dx = -(int) radius; dx <= radius; dx++) {
                    double d = Math.hypot(dx, dy);
                    if (d >= radius || cv + dy < 0 || cv + dy >= h) continue;
                    int g = Math.floorMod(cu + dx, w) + (cv + dy) * w;
                    float s = placed.surfaceMetres(d * Terrain.BLOCKS_PER_CELL);
                    cones[g] = Float.isNaN(cones[g]) ? s : smoothMax(cones[g], s, CONE_BLEND);
                }
        }
        float[] beforeCones = elevation.clone();
        for (int g = 0; g < n; g++)
            if (!Float.isNaN(cones[g])) elevation[g] = Math.max(elevation[g], Planet.fromMetres(cones[g]));
        for (int i = 0; i < iterations; i++) {
            if (Thread.currentThread().isInterrupted()) throw new CancellationException();
            route();""",
 """        // Volcanoes are young beside the land they stand on: they go up for the last part of the erosion, so they come
        // out sharp, and still before the last rivers, so water runs off their flanks instead of under them.
        float[] beforeCones = null;
        int conesAt = (int) (iterations * (1 - VOLCANO_YOUTH));
        for (int i = 0; i < iterations; i++) {
            if (Thread.currentThread().isInterrupted()) throw new CancellationException();
            if (i == conesAt) beforeCones = buildCones(planet);
            route();"""),
("""        collapseCalderas(beforeCones);
        route();""",
 """        if (beforeCones == null) beforeCones = buildCones(planet); // no erosion at all
        collapseCalderas(beforeCones);
        route();"""),
("""    /**
     * Calderas are young beside the volcanoes they cut: the whole cone went up and has been wearing down with the
     * land around it; now the summit of each volcano that drained its magma chamber falls in. Only the volcano's own
     * rock goes: never below the land it was built on, so a range beside it stands untouched. The lakes that fill
     * the basins up to the lowest notch in their rims come from markLakes, like any other.
     */
    private void collapseCalderas(float[] beforeCones) {
        for (Volcano v : volcanoes) {
            if (!v.caldera()) continue;
            int cu = (int) v.u(), cv = (int) v.v(), r = (int) Math.ceil(Volcano.CALDERA_SHARE * v.radiusBlocks() / Terrain.BLOCKS_PER_CELL);
            for (int dy = -r; dy <= r; dy++)
                for (int dx = -r; dx <= r; dx++) {
                    if (cv + dy < 0 || cv + dy >= h) continue;
                    int g = Math.floorMod(cu + dx, w) + (cv + dy) * w;
                    float floor = Planet.fromMetres(v.surfaceMetres(Math.hypot(dx, dy) * Terrain.BLOCKS_PER_CELL, true));
                    elevation[g] = Math.min(elevation[g], Math.max(beforeCones[g], floor));
                }
        }
    }""",
 """    /** Puts every volcano's cone into the land; returns the land as it was before them. */
    private float[] buildCones(Planet planet) {
        int n = w * h;
        // Every base is measured before any cone goes up: neighbours in a chain must not stack on each other.
        for (Volcano v : planet.volcanoes())
            if (!v.gone()) volcanoes.add(v.withBase(Planet.metres(elevation[(int) v.u() + (int) v.v() * w])));
        // Overlapping cones blend with a smooth maximum, so neighbours in a chain merge without a straight crease.
        float[] cones = new float[n];
        Arrays.fill(cones, Float.NaN);
        for (Volcano placed : volcanoes) {
            int cu = (int) placed.u(), cv = (int) placed.v();
            float radius = placed.radiusBlocks() / Terrain.BLOCKS_PER_CELL;
            for (int dy = -(int) radius; dy <= radius; dy++)
                for (int dx = -(int) radius; dx <= radius; dx++) {
                    double d = Math.hypot(dx, dy);
                    if (d >= radius || cv + dy < 0 || cv + dy >= h) continue;
                    int g = Math.floorMod(cu + dx, w) + (cv + dy) * w;
                    float s = placed.surfaceMetres(d * Terrain.BLOCKS_PER_CELL);
                    cones[g] = Float.isNaN(cones[g]) ? s : smoothMax(cones[g], s, CONE_BLEND);
                }
        }
        float[] before = elevation.clone();
        for (int g = 0; g < n; g++)
            if (!Float.isNaN(cones[g])) elevation[g] = Math.max(elevation[g], Planet.fromMetres(cones[g]));
        return before;
    }

    /**
     * Calderas are young beside the volcanoes they cut: the cone went up and wore down with the land around it; now
     * the summit of each volcano that drained its magma chamber falls in. The summit as erosion left it sinks by up to
     * CALDERA_SINK of the volcano's height, a flat floor under walls steepening to the rim. Only the volcano's own
     * rock goes, never below the land it was built on, so a range beside it stands untouched. Lakes fill the basins
     * up to the lowest notch in their rims in markLakes, like any other lake.
     */
    private void collapseCalderas(float[] beforeCones) {
        for (Volcano v : volcanoes) {
            if (!v.caldera()) continue;
            int cu = (int) v.u(), cv = (int) v.v();
            float r = Volcano.CALDERA_SHARE * v.radiusBlocks() / Terrain.BLOCKS_PER_CELL;
            float summit = Planet.metres(elevation[cu + cv * w]);
            float sink = Math.min(Volcano.CALDERA_DEPTH, CALDERA_SINK * (summit - v.baseMetres()));
            if (sink <= 0) continue;
            for (int dy = -(int) r; dy <= r; dy++)
                for (int dx = -(int) r; dx <= r; dx++) {
                    double d = Math.hypot(dx, dy) / r;
                    if (d >= 1 || cv + dy < 0 || cv + dy >= h) continue;
                    int g = Math.floorMod(cu + dx, w) + (cv + dy) * w;
                    float t = (float) Math.clamp((d - 0.6) / 0.4, 0, 1), wall = t * t * (3 - 2 * t);
                    float sunk = Planet.fromMetres(Planet.metres(elevation[g]) - sink * (1 - wall * wall));
                    elevation[g] = Math.max(beforeCones[g], Math.min(elevation[g], sunk));
                }
        }
    }"""),
])
V = 'src/main/java/com/realtectonics/sim/Volcano.java'
edit(V, [
("""    /** As surfaceMetres, but a collapsed summit is a caldera: a flat floor under walls that steepen up to its rim. */
    public float surfaceMetres(double d, boolean collapsed) {
        double r = CALDERA_SHARE * radiusBlocks();
        if (!collapsed || d >= r) return surfaceMetres(d);
        float rim = surfaceMetres(r), depth = Math.min(CALDERA_DEPTH, 0.4f * (rim - baseMetres));
        float t = (float) Math.clamp((d / r - 0.6) / 0.4, 0, 1), wall = t * t * (3 - 2 * t);
        return rim - depth + depth * wall * wall;
    }

""", ""),
])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""        return softCeiling(toY(c.v.surfaceMetres(d, c.collapsed)));""", """        return softCeiling(toY(c.v.surfaceMetres(d)));"""),
])
print("ok")
