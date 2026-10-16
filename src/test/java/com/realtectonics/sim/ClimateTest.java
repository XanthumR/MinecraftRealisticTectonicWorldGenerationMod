package com.realtectonics.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClimateTest {
    @Test
    void mountainsCastRainShadowsAndPolesAreCold() {
        int w = 256, h = 128;
        Planet p = new Planet(w, h, 8, 1);
        // Hand-built world: open ocean, one continent from x=100 to 200 with a ridge at x=120.
        for (int g = 0; g < w * h; g++) {
            int x = g % w;
            p.height[g] = x < 100 || x >= 200 ? Planet.OCEAN : x >= 118 && x <= 122 ? Planet.MAX_CRUST : Planet.CONTINENT;
        }
        Climate c = new Climate(p);

        int y = (int) Math.round((1 - Math.sin(Math.toRadians(45))) / 2 * h); // 45°N: westerlies, the west side is windward
        float windward = c.precipitation[115 + y * w], leeward = c.precipitation[130 + y * w];
        assertTrue(windward > 3 * leeward, "no rain shadow: windward " + windward + " leeward " + leeward);
        assertTrue(c.temperature[10 + (h / 2) * w] > c.temperature[10] + 30, "equator should be far warmer than the pole");

        // 55°N: the drift and the westerlies keep the west coast mild; deep inland the winters win (Norway vs Siberia).
        int north = (int) Math.round((1 - Math.sin(Math.toRadians(55))) / 2 * h);
        float coast = c.temperature[101 + north * w], inland = c.temperature[180 + north * w];
        assertTrue(coast > inland + 4, "no maritime climate: coast " + coast + " inland " + inland);
    }

    @Test
    void tradesBlockedByAWallStreamPolewardAlongIt() {
        int w = 1024, h = 512; // the rates are per km, set on this size; a coarser planet blurs a 1-cell range away
        float[] foot = new float[2];
        for (int wall = 0; wall < 2; wall++) { // a continent, then the same with a pass-less range across the trades ~28° inland (the Andes)
            Planet p = new Planet(w, h, 8, 1);
            for (int g = 0; g < w * h; g++) {
                int x = g % w;
                p.height[g] = x < 400 || x >= 800 ? Planet.OCEAN : wall == 1 && x >= 700 && x <= 719 ? Planet.MAX_CRUST : Planet.CONTINENT;
            }
            int y = (int) Math.round((1 - Math.sin(Math.toRadians(-28))) / 2 * h); // 28°S: the Chaco
            foot[wall] = new Climate(p).precipitation[732 + y * w];
        }
        assertTrue(foot[1] > 10 * foot[0], "no low-level jet along the range: " + foot[1] + " mm with it, " + foot[0] + " without");
    }
}
