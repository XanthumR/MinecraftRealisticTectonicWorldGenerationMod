import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {});
int mtn = -1; for (int g = d.w * 180; g < d.w * 330 && mtn < 0; g++) if (d.metres[g] > 2500 && d.metres[g] < 4000 && d.ice[g] == 0) mtn = g;
for (int zoom : new int[]{1, 2}) {
  var t = new Terrain(d, 777, 1, zoom);
  int[][] spots = {{0, 0}, t.blockOf(mtn)};
  String[] names = {"spawn", "mountain"};
  for (int s = 0; s < 2; s++) {
    int n = 768; var img = new java.awt.image.BufferedImage(n, n, 1); int[] at = spots[s];
    Terrain.Column[] cs = new Terrain.Column[n*n];
    java.util.stream.IntStream.range(0, n).parallel().forEach(z -> { for (int x = 0; x < n; x++) cs[x+z*n] = t.column(at[0] + x - n/2, at[1] + z - n/2); });
    for (int z = 0; z < n; z++) for (int x = 0; x < n; x++) { var c = cs[x+z*n]; int rgb;
      int top = Math.max(c.ground(), c.water());
      if (c.water() != Terrain.Column.NO_WATER) rgb = 0x3060c0;
      else { rgb = c.biome().color; }
      // hillshade from the north-west, like a real relief map
      int xm = Math.max(x-1,0), zm = Math.max(z-1,0);
      float dzdx = c.ground() - cs[xm + z*n].ground(), dzdz = c.ground() - cs[x + zm*n].ground();
      float f = Math.clamp(1 + 0.25f * (dzdx + dzdz), 0.35f, 1.65f);
      int r=rgb>>16&255,g2=rgb>>8&255,b=rgb&255; rgb = (int)Math.min(255,r*f)<<16|(int)Math.min(255,g2*f)<<8|(int)Math.min(255,b*f);
      img.setRGB(x, z, rgb); }
    javax.imageio.ImageIO.write(img, "png", new java.io.File("out/look-" + names[s] + zoom + ".png"));
  }
}
/exit
