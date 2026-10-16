import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
int cx = Integer.getInteger("cx"), cz = Integer.getInteger("cz"), n = Integer.getInteger("n");
var img = new java.awt.image.BufferedImage(n, n, 1); var cs = new Terrain.Column[n*n];
java.util.stream.IntStream.range(0, n).parallel().forEach(z -> { for (int x = 0; x < n; x++) cs[x+z*n] = t.column(cx + x - n/2, cz + z - n/2); });
int springs = 0;
for (int z = 0; z < n; z++) for (int x = 0; x < n; x++) { var c = cs[x+z*n]; int rgb;
  if (c.cover() == Terrain.Cover.SPRING) { rgb = 0x40e0ff; springs++; } else if (c.cover() == Terrain.Cover.SINTER) rgb = 0xf0a040;
  else if (c.lava() != Terrain.Column.NO_WATER) rgb = 0xff5000; else if (c.water() != Terrain.Column.NO_WATER) rgb = 0x3c78dc; else if (c.volcanic()) rgb = 0x302828; else rgb = c.biome().color;
  var a = cs[Math.max(x-1,0)+z*n]; float f = Math.clamp(1 + 0.06f * (c.ground() - a.ground()), 0.4f, 1.6f);
  int r=rgb>>16&255,g2=rgb>>8&255,b=rgb&255; img.setRGB(x, z, (int)Math.min(255,r*f)<<16|(int)Math.min(255,g2*f)<<8|(int)Math.min(255,b*f)); }
javax.imageio.ImageIO.write(img, "png", new java.io.File("out/volcano-flows.png")); System.out.println("spring columns " + springs);
/exit
