package com.realtectonics.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanetTest {
    @Test
    void platesCollideIntoMountains() {
        Planet p = new Planet(256, 128, 8, 42);
        float startMax = max(p.height);
        for (int i = 0; i < 100; i++) p.step();

        int land = 0;
        int[] area = new int[p.plates.length];
        for (int g = 0; g < p.height.length; g++) {
            assertTrue(p.owner[g] >= 0 && p.owner[g] < p.plates.length);
            area[p.owner[g]]++;
            assertTrue(Float.isFinite(p.height[g]) && p.height[g] >= Planet.OCEAN * 0.5f && p.height[g] <= Planet.MAX_CRUST, "height " + p.height[g]);
            if (p.height[g] > Planet.SEA_LEVEL) land++;
        }
        for (int a : area) assertTrue(a > 0, "a plate was swallowed and never reborn");
        float landFraction = land / (float) p.height.length;
        assertTrue(landFraction > 0.1 && landFraction < 0.6, "land " + landFraction);
        assertTrue(max(p.height) > startMax + 1, "no mountains formed");
        float fastest = 0;
        for (Planet.Plate pl : p.plates) fastest = Math.max(fastest, (float) Math.hypot(pl.vx, pl.vy));
        assertTrue(fastest > 0.05, "slab pull should keep plates moving, fastest " + fastest);
    }

    private static float max(float[] a) {
        float m = 0;
        for (float v : a) m = Math.max(m, v);
        return m;
    }
}
