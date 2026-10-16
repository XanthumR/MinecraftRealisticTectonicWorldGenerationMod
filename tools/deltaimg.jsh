import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
var mouths = new java.util.ArrayList<Integer>();
for (int c = 0; c < d.w * d.h; c++) { int r = d.receiver[c]; if (r >= 0 && d.metres[c] >= 0 && d.metres[r] < 0 && d.metres[r] >= -150 && d.flow[c] > 500 && d.ice[c] != 2 && Float.isNaN(d.lakeMetres[c])) mouths.add(c); }
int allMouths = 0; for (int c = 0; c < d.w * d.h; c++) { int r = d.receiver[c]; if (r >= 0 && d.metres[c] >= 0 && d.metres[r] < 0 && d.flow[c] > 500) allMouths++; }
mouths.sort((a, b) -> Float.compare(d.flow[b], d.flow[a]));
System.out.println("deltas " + mouths.size() + " of " + allMouths + " big river mouths");
java.util.function.BiConsumer<Integer, String> render = (cellIdx, name) -> {
  int[] at = t.blockOf(cellIdx); int n = 768; var img = new java.awt.image.BufferedImage(n, n, 1);
  Terrain.Column[] cs = new Terrain.Column[n*n];
  java.util.stream.IntStream.range(0, n).parallel().forEach(z -> { for (int x = 0; x < n; x++) cs[x+z*n] = t.column(at[0] + x - n/2, at[1] + z - n/2); });
  for (int z = 0; z < n; z++) for (int x = 0; x < n; x++) { var c = cs[x+z*n]; int rgb;
    if (c.water() != Terrain.Column.NO_WATER) { int dep = Math.min(60, c.water() - c.ground()); float f = dep / 60f; rgb = (int)(60*(1-f)) << 16 | (int)(190 - 160*f) << 8 | (int)(225 - 120*f); }
    else { rgb = c.biome().color; int l = cs[Math.max(x-1,0)+z*n].ground() - cs[Math.min(x+1,n-1)+z*n].ground(); float f = Math.clamp(1 + l*0.08f, 0.5f, 1.5f); int r=rgb>>16&255,g2=rgb>>8&255,b=rgb&255; rgb = (int)Math.min(255,r*f)<<16|(int)Math.min(255,g2*f)<<8|(int)Math.min(255,b*f); }
    img.setRGB(x, z, rgb); }
  try { javax.imageio.ImageIO.write(img, "png", new java.io.File("out/" + name + ".png")); } catch (Exception e) { throw new RuntimeException(e); }
  System.out.println(name + " at " + java.util.Arrays.toString(at) + " flow " + d.flow[cellIdx] + " temp " + d.temperature[cellIdx]);
};
render.accept(mouths.get(0), "delta1");
render.accept(mouths.get(mouths.size() / 2), "delta2");
// lowland meandering river: a big river cell low and inland
int low = -1; for (int c = 0; c < d.w*d.h && low < 0; c++) if (d.flow[c] > 400 && d.metres[c] > 0 && d.metres[c] < 200 && d.ice[c] == 0) { boolean inland = true; for (int k = -6; k <= 6; k++) inland &= d.metres[Math.floorMod(c % d.w + k, d.w) + c / d.w * d.w] >= 0; if (inland) low = c; }
render.accept(low, "meander");
/exit
