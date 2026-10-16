exec(open('tools/edit_glacier.py').read().split("C = ")[0])
edit('src/test/java/com/realtectonics/sim/HydrologyTest.java', [
("""        assertTrue(fjords > 0, "no fjords, so the check proved nothing");
    }""",
 """        assertTrue(fjords > 0, "no fjords, so the check proved nothing");

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
    }"""),
])
print("ok")
