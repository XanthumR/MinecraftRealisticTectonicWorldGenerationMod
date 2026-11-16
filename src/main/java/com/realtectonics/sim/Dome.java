package com.realtectonics.sim;

/**
 * Format 107: a dome mountain (the Black Hills, the Adirondacks, the Ozarks). Deep in a calm continent the crust bowed
 * up into an oval dome; erosion stripped the sedimentary beds off its top down to the granite core, and the beds left
 * dipping off its flanks wear into rings of ridges (hogbacks) with valleys between.
 *
 * @param u      centre, in cells
 * @param v      centre, in cells
 * @param a      semi-axis along the long side, in cells
 * @param b      semi-axis across, in cells
 * @param angle  of the long side, in radians
 * @param metres how far the beds were bowed up at the middle
 */
public record Dome(float u, float v, float a, float b, float angle, float metres) {
    /** 1 at the middle, falling smoothly to 0 at the edge of the oval; w wraps the planet east-west. */
    public float lift(double cu, double cv, int w) {
        double dx = cu - u, dy = cv - v;
        dx -= w * Math.rint(dx / w);
        double c = Math.cos(angle), s = Math.sin(angle), along = (dx * c + dy * s) / a, across = (dy * c - dx * s) / b;
        double r2 = along * along + across * across;
        return r2 >= 1 ? 0 : (float) ((1 - r2) * (1 - r2));
    }
}
