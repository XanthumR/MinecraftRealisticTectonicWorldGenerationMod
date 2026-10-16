exec(open('tools/edit_glacier.py').read().split("C = ")[0])
P = 'src/main/java/com/realtectonics/sim/Planet.java'
edit(P, [
("""     * Strike-slip on land cells at a plate boundary now: how fast the plate across the boundary slides past along it,""",
 """     * Strike-slip on cells at a plate boundary now: how fast the plate across the boundary slides past along it,"""),
("""                if (height[g] < SEA_LEVEL) continue;
                boolean edge = false;""",
 """                boolean edge = false;"""),
("""    /** Erosion lowered a land cell: the crust under it thins too. */""",
 """    /**
     * The zones around boundaries where plates slide past each other. [0]: signed distance in cells to the boundary,
     * up to FAULT_REACH, negative on the lower-numbered plate: its zero line is the fault's trace. [1]: the slip rate,
     * spread over the zone. Both blurred, so the trace runs as a smooth line instead of a staircase of cells.
     */
    public float[][] faultZones() {
        float[] slip = strikeSlip(), side = new float[w * h], rate = new float[w * h];
        int[] dist = new int[w * h], across = new int[w * h], queue = new int[w * h];
        Arrays.fill(dist, Integer.MAX_VALUE);
        Arrays.fill(side, FAULT_REACH);
        int n = 0;
        for (int g = 0; g < w * h; g++) {
            if (slip[g] < FAULT_MIN_SLIP) continue;
            int x = g % w, y = g / w;
            for (int nb : new int[]{Math.floorMod(x - 1, w) + y * w, (x + 1) % w + y * w, y > 0 ? g - w : g, y < h - 1 ? g + w : g})
                if (owner[nb] != owner[g]) across[g] = owner[nb];
            dist[g] = 0;
            rate[g] = slip[g];
            queue[n++] = g;
        }
        for (int i = 0; i < n; i++) { // outward through each plate, carrying the plate across and the slip
            int c = queue[i], x = c % w, y = c / w;
            side[c] = (owner[c] > across[c] ? 1 : -1) * (dist[c] + 0.5f); // the boundary lies half a cell out
            if (dist[c] == FAULT_REACH - 1) continue;
            for (int nb : new int[]{Math.floorMod(x - 1, w) + y * w, (x + 1) % w + y * w, y > 0 ? c - w : -1, y < h - 1 ? c + w : -1})
                if (nb >= 0 && dist[nb] == Integer.MAX_VALUE && owner[nb] == owner[c]) {
                    dist[nb] = dist[c] + 1;
                    across[nb] = across[c];
                    rate[nb] = rate[c];
                    queue[n++] = nb;
                }
        }
        Geology.blur(side, w, h);
        for (int pass = 0; pass < 2; pass++) Geology.blur(rate, w, h);
        return new float[][]{side, rate};
    }

    /** Erosion lowered a land cell: the crust under it thins too. */"""),
("""    static final int FAULT_WINDOW = 3;            // cells around a boundary cell that give the boundary its direction""",
 """    static final int FAULT_WINDOW = 3;            // cells around a boundary cell that give the boundary its direction
    static final int FAULT_REACH = 3;             // cells either side of a strike-slip boundary that belong to its fault zone
    static final float FAULT_MIN_SLIP = 0.05f;    // cells per step of sliding that make a boundary a fault worth showing"""),
])

D = 'src/main/java/com/realtectonics/sim/PlanetData.java'
edit(D, [
("""    private static final int FORMAT = 31;""", """    private static final int FORMAT = 32;"""),
("""31 lake shores ramp up to high land; older""", """31 lake shores ramp up to high land, 32 strike-slip faults (faultSide, faultSlip); older"""),
("""    public final float[] summerRain;    // share of the rain falling in the warm half of the year (format 24; 0.5 before)""",
 """    public final float[] summerRain;    // share of the rain falling in the warm half of the year (format 24; 0.5 before)
    public final float[] faultSide;     // cells to the trace of a strike-slip fault, signed by side, up to Planet.FAULT_REACH (format 32)
    public final float[] faultSlip;     // that fault's slip rate in cells per step, spread over its zone; 0 away from faults (format 32)"""),
("""        java.util.Arrays.fill(summerRain, 0.5f);""", """        java.util.Arrays.fill(summerRain, 0.5f);
        faultSide = new float[w * h];
        faultSlip = new float[w * h];"""),
("""        System.arraycopy(Geology.provinces(planet, d.metres, d.flow, d.lakeMetres), 0, d.rock, 0, d.rock.length);""",
 """        System.arraycopy(Geology.provinces(planet, d.metres, d.flow, d.lakeMetres), 0, d.rock, 0, d.rock.length);
        float[][] faults = planet.faultZones();
        System.arraycopy(faults[0], 0, d.faultSide, 0, d.faultSide.length);
        System.arraycopy(faults[1], 0, d.faultSlip, 0, d.faultSlip.length);"""),
("""            for (float v : summerRain) out.writeFloat(v);
        }""", """            for (float v : summerRain) out.writeFloat(v);
            for (float v : faultSide) out.writeFloat(v);
            for (float v : faultSlip) out.writeFloat(v);
        }"""),
("""                for (int i = 0; i < d.summerRain.length; i++) d.summerRain[i] = in.readFloat();
            }""", """                for (int i = 0; i < d.summerRain.length; i++) d.summerRain[i] = in.readFloat();
            }
            if (format >= 32) {
                for (int i = 0; i < d.faultSide.length; i++) d.faultSide[i] = in.readFloat();
                for (int i = 0; i < d.faultSlip.length; i++) d.faultSlip[i] = in.readFloat();
            }"""),
])

H = 'src/main/java/com/realtectonics/sim/Hydrology.java'
edit(H, [
("""    static final float RIFT_SHOULDER = 300, RIFT_KEEL = 5; // metres the shoulders rise; the floor stays at least this far above the sea""",
 """    static final float RIFT_SHOULDER = 300, RIFT_KEEL = 5; // metres the shoulders rise; the floor stays at least this far above the sea
    static final float FAULT_GOUGE = 3, FAULT_FULL_SLIP = 0.4f; // times faster crushed rock along a strike-slip fault erodes; slip (cells/step) for all of it"""),
("""    private float erodibility = ERODIBILITY;""", """    private float erodibility = ERODIBILITY;
    private float[] weakness;        // per cell, times faster the rock erodes: crushed along faults; null for all alike"""),
("""        int[] offshore = distanceFrom(g -> elevation[g] >= 0), trench""",
 """        // Strike-slip faults grind their rock to gouge, which wears away fast: rivers find the fault and follow it, so
        // it shows as a long straight valley (the North Anatolian Fault, the San Andreas, the Great Glen).
        float[] slip = planet.strikeSlip();
        weakness = new float[n];
        for (int g = 0; g < n; g++) weakness[g] = Math.min(1, slip[g] / FAULT_FULL_SLIP);
        Geology.blur(weakness, w, h);
        for (int g = 0; g < n; g++) weakness[g] = 1 + FAULT_GOUGE * Math.min(1, 3 * weakness[g]); // the blur spreads a line over 3 cells
        int[] offshore = distanceFrom(g -> elevation[g] >= 0), trench"""),
("""            float f = erodibility * (ice[c] != 0 ? GLACIAL_EROSION : 1) * (float) Math.pow(flow[c], AREA_EXPONENT) / dist;""",
 """            float f = erodibility * (ice[c] != 0 ? GLACIAL_EROSION : 1) * (weakness == null ? 1 : weakness[c]) * (float) Math.pow(flow[c], AREA_EXPONENT) / dist;"""),
])
print("ok")
