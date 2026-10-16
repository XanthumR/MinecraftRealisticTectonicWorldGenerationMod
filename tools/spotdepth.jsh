import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
int cx = Integer.getInteger("cx"), cz = Integer.getInteger("cz"), n = Integer.getInteger("n", 256);
var img = new java.awt.image.BufferedImage(n, n, 1); var cs = new Terrain.Column[n*n];
for (int z = 0; z < n; z++) for (int x = 0; x < n; x++) cs[x+z*n] = t.column(cx + x - n/2, cz + z - n/2);
for (int z = 0; z < n; z++) for (int x = 0; x < n; x++) { var c = cs[x+z*n]; int rgb; int top = Math.max(c.ground(), c.water());
  int xm = Math.max(x-1,0), zm = Math.max(z-1,0); var a = cs[xm+z*n]; var b = cs[x+zm*n];
  float f = Math.clamp(1 + 0.12f * ((top - Math.max(a.ground(), a.water())) + (top - Math.max(b.ground(), b.water()))), 0.3f, 1.7f);
  int dep = c.water() == Terrain.Column.NO_WATER ? -1 : Math.min(30, c.water() - c.ground()); rgb = dep < 0 ? c.biome().color : (int)(40 + 6 * (30 - dep)) << 16 | (int)(90 + 5 * (30 - dep)) << 8 | 220;
  int r=rgb>>16&255,g2=rgb>>8&255,bb=rgb&255; rgb = (int)Math.min(255,r*f)<<16|(int)Math.min(255,g2*f)<<8|(int)Math.min(255,bb*f);
  img.setRGB(x, z, rgb); }
javax.imageio.ImageIO.write(img, "png", new java.io.File("out/" + System.getProperty("name", "spot") + ".png"));
/exit
