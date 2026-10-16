import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
var f1 = Terrain.class.getDeclaredField("riverLevel"); f1.setAccessible(true); var f2 = Terrain.class.getDeclaredField("groundY"); f2.setAccessible(true);
float[] lv = (float[]) f1.get(t), gy = (float[]) f2.get(t);
int deep = 0; float maxCut = 0; int at = -1;
for (int g = 0; g < lv.length; g++) { float cut = gy[g] - lv[g]; if (cut > 3) deep++; if (cut > maxCut) { maxCut = cut; at = g; } }
System.out.printf("cells cut > 3 blocks: %d, deepest %.1f at %s rain %.0f m %.0f%n", deep, maxCut, java.util.Arrays.toString(t.blockOf(at)), d.precipitation[at], d.metres[at]);
int c = 0; for (int g = 0; g < lv.length; g++) { int[] b = t.blockOf(g); if (Math.abs(b[0] - 3488) < 2 && Math.abs(b[1] + 960) < 2) c = g; }
for (int k = 0, g = c; k < 12 && g >= 0; k++, g = d.receiver[g]) System.out.printf("cell %d ground %.1f level %.1f rain %.0f flow %.0f%n", g, gy[g], lv[g], d.precipitation[g], d.flow[g]);
int withWater = 0, checked = 0; int[] dry = null;
for (int g = 0; g < lv.length; g++) { if (gy[g] - lv[g] < 10) continue; checked++; int[] b = t.blockOf(g); boolean wet = false;
  for (int dz = -40; dz <= 40 && !wet; dz += 2) for (int dx = -40; dx <= 40; dx += 2) { var col = t.column(b[0]+dx, b[1]+dz); if (col.water() != Terrain.Column.NO_WATER && col.water() < gy[g] - 5) { wet = true; break; } }
  if (wet) withWater++; else if (dry == null) dry = b; }
System.out.println("canyon cells cut >10: " + checked + ", with river water well below the plateau within 40 blocks: " + withWater + "; a dry one at " + java.util.Arrays.toString(dry));
/exit
