import com.realtectonics.sim.*;
// The planet screen's 3D view (Relief) as PNGs: -Dplanet=out/x.bin [-Dseed=777] -> out/relief_whole.png, out/relief_close.png
var d = PlanetData.load(java.nio.file.Path.of(System.getProperty("planet")));
var t = new Terrain(d, Long.getLong("seed", 777), 1, 1, 2);
int[] rgb = t.mapColors(false);
float[] hs = Relief.heights(d, 1, t.blocksPerCell);
double cu = t.spawnCell % d.w, cv = t.spawnCell / d.w;
for (var view : new Object[][]{{"whole", d.w / 2.0, 0.0}, {"close", 48.0, 0.6}}) {
  int ow = 1200, oh = 600;
  int[] px = Relief.render(rgb, hs, d.w, d.h, (double) view[1] == d.w / 2.0 ? d.w / 2.0 : cu, (double) view[1] == d.w / 2.0 ? d.h / 2.0 : cv, (double) view[2], Math.toRadians(40), (double) view[1], ow, oh);
  var img = new java.awt.image.BufferedImage(ow, oh, 1); img.setRGB(0, 0, ow, oh, px, 0, ow);
  javax.imageio.ImageIO.write(img, "png", new java.io.File("out/relief_" + view[0] + ".png"));
  long sky = java.util.Arrays.stream(px).filter(c -> c == Relief.SKY).count();
  System.out.println(view[0] + ": sky " + sky * 100 / px.length + "%");
}
/exit
