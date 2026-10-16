exec(open('tools/edit_glacier.py').read().split("C = ")[0])
D = 'src/main/java/com/realtectonics/sim/PlanetData.java'
t = open(D, encoding='utf-8').read(); assert t.count('FORMAT = 31;') == 1; open(D, 'w', encoding='utf-8').write(t.replace('FORMAT = 31;', 'FORMAT = 32;'))
edit(D, [
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
