import com.realtectonics.sim.*;
// Format 100 valleys: how deep they cut. Builds the same area at -Dfa (99) and -Dfb (100) and prints the histogram of
// ground lowered (out/valleys_cut.png), the share of land cut 4+ blocks, and the deepest cut. -Dplanet -Dcx -Dcz -Dn (blocks, default 512).
var path = java.nio.file.Path.of(System.getProperty("planet", "out/p777_97.bin"));
int cx = Integer.getInteger("cx", -9376), cz = Integer.getInteger("cz", 512), n = Integer.getInteger("n", 512);
Terrain[] ts = new Terrain[2]; int[] fs = {Integer.getInteger("fa", 99), Integer.getInteger("fb", 100)};
for (int k = 0; k < 2; k++) { var d = PlanetData.load(path); d.format = fs[k]; long t0 = System.nanoTime(); ts[k] = new Terrain(d, 777, 1, 1, 2); System.out.println("format " + fs[k] + " built in " + (System.nanoTime() - t0) / 1000000 + " ms"); }
int[] hist = new int[8]; int land = 0, deep = 0, max = 0; var img = new java.awt.image.BufferedImage(n / 2, n / 2, 1); // out/valleys_cut.png: lowered red, raised blue
for (int z = 0; z < n; z += 2) for (int x = 0; x < n; x += 2) {
  var a = ts[0].column(cx - n / 2 + x, cz - n / 2 + z); var b = ts[1].column(cx - n / 2 + x, cz - n / 2 + z);
  if (a.water() != Terrain.Column.NO_WATER || a.ground() < 64) continue;
  land++; int cut = a.ground() - b.ground(); int sh = Math.min(255, Math.abs(cut) * 12); img.setRGB(x / 2, z / 2, cut > 0 ? sh << 16 : sh); max = Math.max(max, cut); if (cut >= 4) deep++;
  hist[Math.min(7, Math.max(0, cut < 1 ? 0 : 32 - Integer.numberOfLeadingZeros(cut)))]++; }
System.out.println("land " + land + ", cut 4+ blocks " + (100 * deep / Math.max(1, land)) + "%, deepest " + max + "; by blocks <1,1,2-3,4-7,8-15,16-31,32-63,64+: " + java.util.Arrays.toString(hist));
javax.imageio.ImageIO.write(img, "png", new java.io.File("out/valleys_cut.png"));
/exit
