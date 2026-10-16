exec(open('tools/edit_glacier.py').read().split("C = ")[0])
edit('src/main/java/com/realtectonics/sim/Terrain.java', [
("""            double r = SCREE_REACH / (double) zoom;
            float up = 0, down = 0;
            for (int k = 0; k < 4; k++) {
                double sx = x + (k == 0 ? r : k == 1 ? -r : 0), sz = z + (k == 2 ? r : k == 3 ? -r : 0);
                float d = surface(sx, sz, warpU(sx, sz), warpV(sx, sz), keep) - natural;
                up = Math.max(up, d);
                down = Math.max(down, -d);
            }
            scree = up > SCREE_RISE && down < up / 2;""",
 """            double gu = cubic(groundY, u + 0.5, v) - cubic(groundY, u - 0.5, v), gv = cubic(groundY, u, v + 0.5) - cubic(groundY, u, v - 0.5);
            double len = Math.max(Math.hypot(gu, gv), 1e-6), r = SCREE_REACH / (double) zoom, px = r * gu / len, pz = r * gv / len;
            float up = surface(x + px, z + pz, warpU(x + px, z + pz), warpV(x + px, z + pz), keep) - natural;   // the mountainside above
            float down = natural - surface(x - px, z - pz, warpU(x - px, z - pz), warpV(x - px, z - pz), keep); // and below
            scree = up > SCREE_RISE && down < up / 2;"""),
])
print("ok")
