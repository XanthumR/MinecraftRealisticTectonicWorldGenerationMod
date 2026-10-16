exec(open('tools/edit_glacier.py').read().split("C = ")[0])
T = 'src/main/java/com/realtectonics/sim/Terrain.java'
edit(T, [
("""        if (gullies && natural > SEA_Y + 20) natural += gullies(x, z, u, v, natural - SEA_Y) * (1 - ridgeline * ridgeline * ridgeline); // none on the crests""",
 """        if (gullies && natural > SEA_Y + 20) natural += gullies(x, z, u, v, natural - SEA_Y) * (1 - ridgeline * ridgeline * ridgeline); // none on the crests
        if (faults) natural += faultScarp(x, z, u, v, natural);"""),
("""    /** x, z: unzoomed blocks. Large features are laid out in them; fine ones go through fine() and stay in real blocks. */""",
 """    /**
     * From format 32: the trace of a strike-slip fault, where the simulation's plates slide past each other. Each
     * earthquake shifts the land sideways and a little up or down, so the trace is a step running straight across hills
     * and valleys. Where the fault bends one way the two sides squeeze and one rises; the other way they pull and it
     * sags, so the step faces one side, then the other, along the fault. The valley along it is the simulation's:
     * the crushed rock erodes faster (Hydrology.FAULT_GOUGE).
     */
    private float faultScarp(double x, double z, double u, double v, float natural) {
        float slip = cubic(data.faultSlip, u, v);
        if (slip < 0.01f || natural < SEA_Y) return 0;
        float d = cubic(data.faultSide, u, v) * BLOCKS_PER_CELL / FAULT_SCARP_WIDTH; // widths from the trace
        if (Math.abs(d) > 2) return 0;
        float height = FAULT_SCARP * Math.min(1, slip / Hydrology.FAULT_FULL_SLIP) * Math.clamp(2 * noise(x, z, 512, 43), -1, 1);
        return height * (smoothstep(d + 0.5f) - 0.5f) * smoothstep((natural - SEA_Y) / 8); // none on the beach
    }

    /** x, z: unzoomed blocks. Large features are laid out in them; fine ones go through fine() and stay in real blocks. */"""),
("""    private final boolean rampShores;     // ...and cut less the higher it stands, so no wall where the limit is crossed: format 31""",
 """    private final boolean rampShores;     // ...and cut less the higher it stands, so no wall where the limit is crossed: format 31
    private final boolean faults;         // a scarp along the trace of each strike-slip fault: format 32"""),
("""        rampShores = data.format >= 31;""", """        rampShores = data.format >= 31;
        faults = data.format >= 32;"""),
("""    static final float ICE_FRONT = 4;        // blocks: height of a glacier's calving front above a lake (format 31)""",
 """    static final float ICE_FRONT = 4;        // blocks: height of a glacier's calving front above a lake (format 31)
    static final float FAULT_SCARP = 6, FAULT_SCARP_WIDTH = 3; // blocks a fast fault's scarp stands; unzoomed blocks its face spans"""),
])
print("ok")
