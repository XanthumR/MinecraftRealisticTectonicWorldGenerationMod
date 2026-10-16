import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777);
// a passive coast cell: land with sea 3 cells east, shelf-shallow
int pick = -1;
for (int g = d.w * 150; g < d.w * 360 && pick < 0; g++) { if (d.metres[g] < 0) continue; int e = g + 1; if (d.metres[e] < 0 && d.metres[g+3] > -200 && d.metres[g+3] < 0 && d.metres[g+8] < -2000) pick = g; }
int[] at = t.blockOf(pick); System.out.println("coast at " + java.util.Arrays.toString(at));
int n = 1024, s = 2; var img = new java.awt.image.BufferedImage(n, n, 1);
Terrain.Column[] cs = new Terrain.Column[n*n];
java.util.stream.IntStream.range(0, n).parallel().forEach(z -> { for (int x = 0; x < n; x++) cs[x+z*n] = t.column(at[0] + (x - n/2) * s, at[1] + (z - n/2) * s); });
int[] hist = new int[130];
for (int z = 0; z < n; z++) for (int x = 0; x < n; x++) { var c = cs[x+z*n]; int rgb;
  if (c.water() != Terrain.Column.NO_WATER) { int dep = Math.min(120, c.water() - c.ground()); hist[dep]++; float f = dep / 120f; rgb = (int)(60*(1-f)) << 16 | (int)(200 - 170*f) << 8 | (int)(220 - 120*f); }
  else { rgb = c.biome().color; int l = cs[Math.max(x-1,0)+z*n].ground() - cs[Math.min(x+1,n-1)+z*n].ground(); float f = Math.clamp(1 + l*0.05f, 0.5f, 1.5f); int r=rgb>>16&255,g2=rgb>>8&255,b=rgb&255; rgb = (int)Math.min(255,r*f)<<16|(int)Math.min(255,g2*f)<<8|(int)Math.min(255,b*f); }
  img.setRGB(x, z, rgb); }
javax.imageio.ImageIO.write(img, "png", new java.io.File("out/shelf.png"));
var sb = new StringBuilder("water depth histogram (blocks, count): "); for (int i = 0; i < 130; i += 5) { int sum = 0; for (int j = i; j < Math.min(i+5,130); j++) sum += hist[j]; sb.append(i).append(":").append(sum).append(" "); } System.out.println(sb);
// islets check: land columns with sea all around within 16 blocks that weren't land cells nearby
/exit
