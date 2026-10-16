package com.realtectonics.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HydrologyTest {
    @Test
    void allRainReachesTheSea() {
        Planet p = new Planet(256, 128, 8, 42);
        for (int i = 0; i < 50; i++) p.step();
        float[] rain = new float[p.height.length];
        java.util.Arrays.fill(rain, 1000); // 1 m/yr: each land cell adds 1 to the flow
        Hydrology hy = new Hydrology(p, rain, 20, 42);

        int land = 0;
        float drained = 0;
        for (int g = 0; g < hy.elevation.length; g++) {
            assertTrue(Float.isFinite(hy.elevation[g]));
            if (hy.elevation[g] >= 0) land++;
            if (hy.receiver[g] < 0) drained += hy.flow[g];
            // every downstream path ends at an outlet without looping
            int c = g, hops = 0;
            while (hy.receiver[c] >= 0) {
                assertTrue(hy.surface[hy.receiver[c]] < hy.surface[c], "water flows uphill at " + c);
                c = hy.receiver[c];
                assertTrue(++hops <= hy.elevation.length, "cycle");
            }
        }
        assertTrue(drained <= land + 0.5f, "rain made up on the way to the sea"); // lakes may evaporate some of it

        // A lake stands on its floor and never above its basin's rim.
        for (int g = 0; g < hy.elevation.length; g++)
            if (hy.isLake(g)) assertTrue(hy.lakeLevel(g) >= hy.elevation[g] && hy.lakeLevel(g) <= hy.surface[g], "lake level out of its basin at " + g);

        // Fjords: Ice Age valleys open to the sea all the way down, and never in the warm low latitudes.
        int fjords = 0;
        for (int g = 0; g < hy.ice.length; g++) {
            if (hy.ice[g] != Hydrology.FJORD) continue;
            fjords++;
            assertTrue(Math.abs(Climate.latitude(g / hy.w, hy.h)) > 35, "fjord in the warm latitudes at " + g);
            int c = g;
            while (hy.elevation[c] >= 0) {
                assertTrue(hy.ice[c] == Hydrology.FJORD, "fjord cut off from the sea at " + g);
                c = hy.receiver[c];
            }
        }
        assertTrue(fjords > 0, "no fjords, so the check proved nothing");

        // Continental shelves: the sea beside land is mostly shallow, yet the deep ocean floor survives.
        java.util.List<Float> coastal = new java.util.ArrayList<>();
        float deepest = 0;
        for (int g = 0; g < hy.elevation.length; g++) {
            float m = Planet.metres(hy.elevation[g]);
            deepest = Math.min(deepest, m);
            if (m < 0 && hy.elevation[(g + 1) % hy.elevation.length] >= 0) coastal.add(m);
        }
        java.util.Collections.sort(coastal);
        assertTrue(coastal.get(coastal.size() / 2) > -Hydrology.SHELF_BREAK, "no shelf: median coastal depth " + coastal.get(coastal.size() / 2));
        assertTrue(deepest < -3000, "shelves filled in the deep ocean");
    }
}
