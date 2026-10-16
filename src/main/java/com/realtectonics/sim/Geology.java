package com.realtectonics.sim;

import java.util.Random;

/** Reads the plate history into a geological province per cell. */
final class Geology {
    static final float OROGEN_ACTIVITY = 0.6f; // blurred collision history that marks a mountain belt
    static final int ARC_REACH = Planet.ARC_MAX + 2; // arc rock spreads a little past the volcano line
    static final float BASIN_METRES = 600;     // sediment collects in lowland river valleys, deltas and lake basins
    static final float BASIN_WETNESS = 0.15f;  // blurred river-and-lake share that makes a lowland a basin
    static final float SHIELD_SHARE = 0.45f;   // of stable continent, roughly how much is exposed granite shield

    private Geology() {}

    static byte[] provinces(Planet planet, float[] metres, float[] flow, float[] lakeMetres) {
        int w = planet.w, h = planet.h;
        float[] belts = planet.collision.clone(); // collisions happen on a line; the deformed belt is wider
        for (int pass = 0; pass < 3; pass++) blur(belts, w, h);
        float[] wet = new float[w * h]; // rivers and lakes, spread over their floodplains
        for (int g = 0; g < wet.length; g++) wet[g] = flow[g] > Hydrology.RIVER_AREA || !Float.isNaN(lakeMetres[g]) ? 1 : 0;
        for (int pass = 0; pass < 3; pass++) blur(wet, w, h);
        int[] trench = planet.trenchDistance(ARC_REACH);
        // Stable continent is granite basement, exposed as shield in eroded uplands, elsewhere buried under
        // flat sedimentary platform. Broad noise picks which, tilted toward shields on higher ground.
        float[] cover = Noise.fbm(w, h, w / 16, 4, new Random(planet.seed * 17 + 3));
        float[] sorted = cover.clone();
        java.util.Arrays.sort(sorted);
        float shieldAbove = sorted[(int) (sorted.length * (1 - SHIELD_SHARE))];
        byte[] rock = new byte[w * h];
        for (int g = 0; g < rock.length; g++) {
            Rock r;
            if (planet.height[g] < Planet.SEA_LEVEL) r = Rock.OCEANIC; // crust thickness, not elevation: islands on ocean crust stay oceanic
            else if (belts[g] > OROGEN_ACTIVITY) r = Rock.OROGEN;
            else if (trench[g] <= ARC_REACH) r = Rock.ARC;
            else if (metres[g] < BASIN_METRES && wet[g] > BASIN_WETNESS) r = Rock.BASIN;
            else r = cover[g] + (metres[g] - 500) / 3000 > shieldAbove ? Rock.CRATON : Rock.BASIN;
            rock[g] = (byte) r.ordinal();
        }
        return rock;
    }

    static void blur(float[] f, int w, int h) {
        float[] src = f.clone();
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                float sum = 0;
                for (int dy = -1; dy <= 1; dy++)
                    for (int dx = -1; dx <= 1; dx++) sum += src[Math.floorMod(x + dx, w) + Math.clamp(y + dy, 0, h - 1) * w];
                f[x + y * w] = sum / 9;
            }
    }
}
