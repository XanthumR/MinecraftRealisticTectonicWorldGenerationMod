exec(open('tools/edit_glacier.py').read().split("C = ")[0])
edit('src/test/java/com/realtectonics/sim/HydrologyTest.java', [
("""        assertEquals(land, drained, 0.5f, "rain lost on the way to the sea");
    }""",
 """        assertEquals(land, drained, 0.5f, "rain lost on the way to the sea");

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
    }"""),
])
print("ok")
