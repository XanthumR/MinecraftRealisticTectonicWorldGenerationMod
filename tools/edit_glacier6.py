exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""                double t = Math.clamp(((u - au) * su + (v - cy) * sv) / (su * su + sv * sv), 0, 1);""",
 """                // A fjord's mouth runs on one more cell out to sea: its first sea cell can be a pocket the smooth coastline closes.
                double reach = minIce == Hydrology.FJORD && data.metres[r] < 0 ? 2 : 1;
                double t = Math.clamp(((u - au) * su + (v - cy) * sv) / (su * su + sv * sv), 0, reach);"""),
("""                float half = Math.min(40, 2 + 0.12f * (float) Math.sqrt(data.flow[c]));
                if (dist - half < best.edge)
                    best = new River(dist - half, half, (float) (groundY[c] + t * (Math.max(groundY[r], SEA_Y - 1) - groundY[c])), data.ice[c], data.flow[c]);""",
 """                float half = Math.min(40, 2 + 0.12f * (float) Math.sqrt(data.flow[c]));
                if (dist - half < best.edge)
                    best = new River(dist - half, half, (float) (groundY[c] + Math.min(t, 1) * (Math.max(groundY[r], SEA_Y - 1) - groundY[c])), data.ice[c], data.flow[c]);"""),
("""trough(SEA_Y - 2 - Math.min(40, 8 + 0.1f * (float) Math.sqrt(fjord.flow)), ground, tf)""",
 """trough(SEA_Y - 2 - Math.min(40, 16 + 0.1f * (float) Math.sqrt(fjord.flow)), ground, tf)"""),
])
print("ok")
