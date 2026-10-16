T = 'src/main/java/com/realtectonics/sim/Terrain.java'
s = open(T, encoding='utf-8').read()
head = """    private Column column(double x, double z) {
        double u = warpU(x, z), v = warpV(x, z);
"""
start = s.index(head) + len(head)
end_marker = """        natural = softCeiling(natural);
        int ground = Math.round(natural), water = Column.NO_WATER;
"""
end = s.index(end_marker, start)
pipe = s[start:end]
# old scree attempt inside the massif block goes away
old_scree = pipe[pipe.index("            // Scree: rock falling off a wall"):pipe.index("        if (gullies && natural > SEA_Y + 20)")]
pipe = pipe.replace(old_scree, "        }\n").replace("        boolean scree = false;\n", "")
pipe = pipe.replace("""        float rough = detail(x, z), smooth = natural; // biomes follow the land's shape, not every bump on it
""", """        float rough = detail(x, z), smooth = natural; // biomes follow the land's shape, not every bump on it
        keep[0] = smooth;
        keep[1] = shore;
""")
assert pipe.startswith("        float natural = cubic(groundY, u, v);")
newcol = """        float[] keep = new float[2];
        float natural = surface(x, z, u, v, keep), smooth = keep[0], shore = keep[1];
        // Scree: rock falling off a wall piles up at its foot. A few blocks away the ground stands far higher, while
        // on the other side it is gentle: a rubble apron, ragged at its edges.
        boolean scree = false;
        if (folds && natural > SEA_Y + SCREE_ABOVE && fine(x, z, 16, 72) > -0.3f) {
            double r = SCREE_REACH / (double) zoom;
            float up = 0, down = 0;
            for (int k = 0; k < 4; k++) {
                double sx = x + (k == 0 ? r : k == 1 ? -r : 0), sz = z + (k == 2 ? r : k == 3 ? -r : 0);
                float d = surface(sx, sz, warpU(sx, sz), warpV(sx, sz), keep) - natural;
                up = Math.max(up, d);
                down = Math.max(down, -d);
            }
            scree = up > SCREE_RISE && down < SCREE_RISE / 3;
        }
        int ground = Math.round(natural), water = Column.NO_WATER;
"""
method = """    /** The land's own surface at unzoomed (x, z), before rivers, glaciers and cones cut or build on it. keep: smooth height, shore. */
    private float surface(double x, double z, double u, double v, float[] keep) {
""" + pipe + """        return softCeiling(natural);
    }

"""
s = s[:start] + newcol + s[end + len(end_marker):]
doc = "    /** x, z: unzoomed blocks. Large features are laid out in them; fine ones go through fine() and stay in real blocks. */\n" + head
i = s.index(doc)
s = s[:i] + method + s[i:]
s = s.replace("    static final float SCREE_REACH = 6, SCREE_RISE = 10;", "    static final float SCREE_REACH = 6, SCREE_RISE = 10, SCREE_ABOVE = 40;")
open(T, 'w', encoding='utf-8').write(s)
print("ok")
