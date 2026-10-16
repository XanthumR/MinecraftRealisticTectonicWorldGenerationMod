package com.realtectonics.sim;

import java.util.Random;

final class Noise {
    private Noise() {}

    /** Fractal value noise from baseCell down to minCell, periodic in X so the east-west seam is invisible. w must be divisible by baseCell. */
    static float[] fbm(int w, int h, int baseCell, int minCell, Random rnd) {
        return fbm(w, h, baseCell, minCell, rnd, 0.5f);
    }

    /** persistence: amplitude kept per octave; above 0.5 the finer scales stay strong, as on ragged real coastlines. */
    static float[] fbm(int w, int h, int baseCell, int minCell, Random rnd, float persistence) {
        float[] out = new float[w * h];
        float amp = 1;
        for (int cell = baseCell; cell >= minCell; cell /= 2, amp *= persistence) {
            int gw = w / cell, gh = h / cell + 2;
            float[] lattice = new float[gw * gh];
            for (int i = 0; i < lattice.length; i++) lattice[i] = rnd.nextFloat();
            for (int y = 0; y < h; y++) {
                int iy = y / cell;
                float ty = smooth((y % cell) / (float) cell);
                for (int x = 0; x < w; x++) {
                    int ix = x / cell, ix1 = (ix + 1) % gw;
                    float tx = smooth((x % cell) / (float) cell);
                    float top = lerp(lattice[ix + iy * gw], lattice[ix1 + iy * gw], tx);
                    float bottom = lerp(lattice[ix + (iy + 1) * gw], lattice[ix1 + (iy + 1) * gw], tx);
                    out[x + y * w] += amp * lerp(top, bottom, ty);
                }
            }
        }
        return out;
    }

    private static float smooth(float t) { return t * t * (3 - 2 * t); }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
}
