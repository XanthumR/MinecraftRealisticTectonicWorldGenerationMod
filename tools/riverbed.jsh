import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
int[] at = {-12512 + 120, 5824 + 120}; int n = 512; var img = new java.awt.image.BufferedImage(n, n, 1);
for (int z = 0; z < n; z++) for (int x = 0; x < n; x++) { var c = t.column(at[0] + x - n, at[1] + z - n / 2); int rgb;
  if (c.water() != Terrain.Column.NO_WATER && c.water() >= Terrain.SEA_Y - 1) { int dep = Math.min(12, c.water() - c.ground()); float f = dep / 12f; rgb = (int)(150*(1-f)) << 16 | (int)(220 - 150*f) << 8 | (int)(255 - 90*f); }
  else rgb = c.biome().color;
  img.setRGB(x, z, rgb); }
javax.imageio.ImageIO.write(img, "png", new java.io.File("out/riverbed.png"));
// one cross-section: walk east along a row through the river, print water depth
var sb = new StringBuilder();
for (int x = at[0] - n; x < at[0]; x++) { var c = t.column(x, at[1]); if (c.water() != Terrain.Column.NO_WATER && c.water() >= Terrain.SEA_Y) sb.append(c.water() - c.ground()).append(' '); else if (sb.length() > 0 && !sb.toString().endsWith("| ")) sb.append("| "); }
System.out.println("depths along one row: " + sb);
/exit
