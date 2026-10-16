exec(open('tools/edit_glacier.py').read().split("C = ")[0])
E = 'src/main/java/com/realtectonics/sim/EarthData.java'
edit(E, [
("""        plateCount = count;""", """        plateCount = count;
        // Below sea level is sea only where the ocean reaches: the Caspian depression, the Dead Sea and the Qattara are
        // dry land (the Caspian then a lake, from its lake cells), not drowned at sea level.
        boolean[] ocean = new boolean[w * h];
        int[] queue = new int[w * h];
        int n = 0, deepest = 0;
        for (int g = 0; g < w * h; g++) if (elevation[g] < elevation[deepest]) deepest = g;
        ocean[deepest] = true;
        queue[n++] = deepest;
        for (int i = 0; i < n; i++) {
            int c = queue[i], x = c % w, y = c / w;
            for (int nb : new int[]{Math.floorMod(x - 1, w) + y * w, (x + 1) % w + y * w, y > 0 ? c - w : -1, y < h - 1 ? c + w : -1})
                if (nb >= 0 && !ocean[nb] && elevation[nb] < 0) { ocean[nb] = true; queue[n++] = nb; }
        }
        for (int g = 0; g < w * h; g++) if (elevation[g] < 0 && !ocean[g]) elevation[g] = INLAND_LOW;"""),
("""    static final float OVERRIDING_METRES = -3000; // beside a trench, cells above this are the overriding plate (arc or continent), the rest the sea floor diving""",
 """    static final float INLAND_LOW = 1;      // metres given to ground below sea level that the ocean does not reach
    static final float OVERRIDING_METRES = -3000; // beside a trench, cells above this are the overriding plate (arc or continent), the rest the sea floor diving"""),
])
print("ok")
