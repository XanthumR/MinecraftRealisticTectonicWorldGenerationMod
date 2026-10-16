package com.realtectonics.sim;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CancellationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanetDataTest {
    @Test
    void oceanSettingControlsLandAndCancellingStops() {
        float wet = land(PlanetData.generate(1, new PlanetSettings(256, 6, 0.85f, 1, 1, 1, 300, 1), p -> {}));
        float dry = land(PlanetData.generate(1, new PlanetSettings(256, 6, 0.5f, 1, 1, 1, 300, 1), p -> {}));
        // Volcanic islands add land whatever the setting, and on this tiny planet they are big, so the gap is modest.
        assertTrue(dry > wet + 0.1, "50% ocean should leave clearly more land than 85%: " + dry + " vs " + wet);

        Thread.currentThread().interrupt(); // what the preview screen does when settings change mid-run
        try {
            assertThrows(CancellationException.class, () -> PlanetData.generate(1, new PlanetSettings(256, 6, 0.7f, 1, 1, 1, 300, 1), p -> {}));
        } finally {
            Thread.interrupted(); // clear the flag for other tests
        }
    }

    @Test
    void provincesFollowTheCrust() {
        Planet p = new Planet(256, 128, 8, 3);
        for (int i = 0; i < 100; i++) p.step();
        Climate climate = new Climate(p);
        PlanetData d = PlanetData.from(p, climate, new Hydrology(p, climate.precipitation, 20, 3));
        int[] count = new int[Rock.values().length];
        for (int g = 0; g < d.rock.length; g++) {
            Rock r = Rock.of(d.rock[g]);
            count[r.ordinal()]++;
            if (p.height[g] < Planet.SEA_LEVEL) assertEquals(Rock.OCEANIC, r, "ocean crust at " + g);
            else assertNotEquals(Rock.OCEANIC, r, "continental crust at " + g);
        }
        assertEquals(0, count[Rock.MIXED.ordinal()], "the old mix is only for planets saved before provinces");
        assertTrue(count[Rock.CRATON.ordinal()] > 0 && count[Rock.BASIN.ordinal()] > 0, "continents need both shield and sedimentary cover");
    }

    private static float land(PlanetData d) {
        int n = 0;
        for (float m : d.metres) if (m >= 0) n++;
        return n / (float) d.metres.length;
    }
}
