package com.realtectonics.sim;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerrainTest {
    @Test
    void walkingAroundThePlanetComesBackToTheSameTerrain(@TempDir Path dir) throws IOException {
        Planet p = new Planet(256, 128, 8, 7);
        for (int i = 0; i < 50; i++) p.step();
        Climate climate = new Climate(p);
        PlanetData data = PlanetData.from(p, climate, new Hydrology(p, climate.precipitation, 20, 7));

        Path file = dir.resolve("planet.bin");
        data.save(file);
        PlanetData loaded = PlanetData.load(file);
        assertArrayEquals(data.metres, loaded.metres);
        assertArrayEquals(data.receiver, loaded.receiver);
        assertArrayEquals(data.rock, loaded.rock);
        assertArrayEquals(data.plate, loaded.plate);
        assertArrayEquals(data.plateVx, loaded.plateVx);
        assertArrayEquals(data.riftFloor, loaded.riftFloor);
        assertEquals(data.drift.size(), loaded.drift.size());

        Terrain t = new Terrain(loaded, 7, 1, 1, PlanetSettings.DEFAULT.riverWidth());
        assertTrue(t.column(0, 0).water() == Terrain.Column.NO_WATER, "spawn should be dry land");
        assertEquals(0, t.longitude(0), 1e-9, "spawn is on the prime meridian");
        assertEquals(-180, t.longitude(-t.circumference / 2), 1e-9, "the wrap seam is the 180° line");
        assertEquals(t.longitude(1234), t.longitude(1234 + 3 * t.circumference), 1e-9);
        assertEquals(90, t.latitude(t.northPoleZ()), 1e-6);
        assertEquals(-90, t.latitude(t.southPoleZ()), 1e-6);
        double[] home = t.mapPosition(0, 0), around = t.mapPosition(-5 * t.circumference, 0);
        assertEquals(t.spawnCell, (int) home[0] + (int) home[1] * loaded.w, "the map marker at spawn sits on the spawn cell");
        assertEquals(home[0], around[0], 1e-9, "map position wraps with the planet");
        for (int z = -1500; z <= 1500; z += 37)
            for (int x = -3000; x <= 3000; x += 41) {
                Terrain.Column c = t.column(x, z);
                assertEquals(c, t.column(x + t.circumference, z), "seam at " + x + "," + z);
                assertTrue(c.ground() > Terrain.MIN_Y && c.ground() < Terrain.MAX_Y);
                assertTrue(c.water() == Terrain.Column.NO_WATER || c.water() > c.ground(), "water below ground at " + x + "," + z);
            }

        // World scale: a zoomed planet still wraps exactly, and its map still only shows water the game has.
        for (int zoom : new int[]{2, 4}) {
            Terrain z = new Terrain(loaded, 7, 1, zoom);
            assertEquals(t.circumference * zoom, z.circumference);
            for (int x = -9000; x <= 9000; x += 997) assertEquals(z.column(x, 300), z.column(x + z.circumference, 300), "zoom " + zoom + " seam at " + x);
            int[] map = z.mapColors(false);
            int g = 0;
            while (map[g] != Terrain.WATER_COLOR) g++;
            int[] at = z.blockOf(g);
            boolean wet = false;
            for (int dz = -z.blocksPerCell / 2; dz <= z.blocksPerCell / 2 && !wet; dz++)
                for (int dx = -z.blocksPerCell / 2; dx <= z.blocksPerCell / 2 && !wet; dx++) wet = z.column(at[0] + dx, at[1] + dz).water() != Terrain.Column.NO_WATER;
            assertTrue(wet, "zoom " + zoom + ": the map shows water the game lacks at " + at[0] + "," + at[1]);
        }

        // Lakes and the sea must be enclosed. River steps of one block are allowed (the game lets those flow), and so
        // is water falling into a pool below: a waterfall.
        int lake = 0, mostAround = 0; // the middle of the biggest lake: a one-cell pond is too small to show in blocks
        for (int g = loaded.w * 2; g < loaded.w * (loaded.h - 2); g++) {
            int nearby = 0;
            for (int dy = -2; dy <= 2; dy++)
                for (int dx = -2; dx <= 2; dx++)
                    if (!Float.isNaN(loaded.lakeMetres[Math.floorMod(g % loaded.w + dx, loaded.w) + (g / loaded.w + dy) * loaded.w])) nearby++;
            if (nearby > mostAround) { mostAround = nearby; lake = g; }
        }
        int[] centre = t.blockOf(lake);
        int n = 300, size = 2 * n + 1, inland = 0;
        Terrain.Column[] cols = new Terrain.Column[size * size];
        for (int z = 0; z < size; z++)
            for (int x = 0; x < size; x++) cols[x + z * size] = t.column(centre[0] + x - n, centre[1] + z - n);
        for (int z = 1; z < size - 1; z++)
            for (int x = 1; x < size - 1; x++) {
                Terrain.Column c = cols[x + z * size];
                if (c.water() == Terrain.Column.NO_WATER) continue;
                if (c.water() >= Terrain.SEA_Y) inland++;
                for (int k : new int[]{-1, 1, -size, size}) {
                    Terrain.Column o = cols[x + z * size + k];
                    boolean fallsIntoPool = o.water() != Terrain.Column.NO_WATER && o.water() >= o.ground(); // a waterfall
                    assertTrue(fallsIntoPool || c.water() - Math.max(Math.max(o.ground(), o.water()), o.ice()) <= 1, /* glacier ice holds water in too */ "open water wall at " + (centre[0] + x - n) + "," + (centre[1] + z - n));
                }
            }
        assertTrue(inland > 0, "no lakes or rivers in the scanned area, so the check proved nothing");

        // The map shows what the game builds: where it draws water, the player finds water.
        int[] map = t.mapColors(false);
        int waterPixels = 0, confirmed = 0;
        for (int g = 0; g < map.length && waterPixels < 200; g++) {
            if (map[g] != Terrain.WATER_COLOR) continue;
            waterPixels++;
            int[] at = t.blockOf(g);
            search:
            for (int dz = -16; dz <= 16; dz += 2)
                for (int dx = -16; dx <= 16; dx += 2)
                    if (t.column(at[0] + dx, at[1] + dz).water() != Terrain.Column.NO_WATER) {
                        confirmed++;
                        break search;
                    }
        }
        assertTrue(waterPixels > 20, "the map should show some rivers and lakes");
        assertTrue(confirmed >= waterPixels * 0.9, "map water missing in the game: " + confirmed + " of " + waterPixels);

        // Volcanoes survive saving, and an erupting one holds its lava lake inside the crater.
        assertEquals(data.volcanoes, loaded.volcanoes);
        Volcano erupting = loaded.volcanoes.stream().filter(Volcano::active).findFirst().orElseThrow();
        int[] summit = t.blockOf((int) erupting.u() + (int) erupting.v() * loaded.w);
        int r = 120, side = 2 * r + 1, lava = 0;
        Terrain.Column[] crater = new Terrain.Column[side * side];
        for (int z = 0; z < side; z++)
            for (int x = 0; x < side; x++) crater[x + z * side] = t.column(summit[0] + x - r, summit[1] + z - r);
        for (int z = 1; z < side - 1; z++)
            for (int x = 1; x < side - 1; x++) {
                Terrain.Column c = crater[x + z * side];
                if (c.lava() == Terrain.Column.NO_WATER) continue;
                lava++;
                assertTrue(c.lava() > c.ground());
                for (int k : new int[]{-1, 1, -side, side}) {
                    Terrain.Column o = crater[x + z * side + k];
                    assertTrue(Math.max(o.ground(), Math.max(o.water(), o.lava())) >= c.lava(), "lava would spill at " + (summit[0] + x - r) + "," + (summit[1] + z - r));
                }
            }
        assertTrue(lava > 0, "an erupting volcano should have a lava lake");
    }
}
