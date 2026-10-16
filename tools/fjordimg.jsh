import com.realtectonics.sim.*;
var p = new Planet(1024, 512, 14, 777); for (int i = 0; i < 300; i++) p.step();
var c = new Climate(p); var hy = new Hydrology(p, c.precipitation, 50, 777);
var d = PlanetData.from(p, c, hy); var t = new Terrain(d, 777);
var rnd = new java.util.Random(3); int s=0, fjWater=0;
while (s < 1000) { int g = rnd.nextInt(d.w*d.h); if (d.ice[g]!=2) continue; s++; int[] b=t.blockOf(g); boolean any=false; for (int dz=-64; dz<=64 && !any; dz+=4) for (int dx=-64; dx<=64; dx+=4) { var col=t.column(b[0]+dx,b[1]+dz); if (col.water()==Terrain.SEA_Y-1 && col.ground() < Terrain.SEA_Y-5) {any=true;break;} } if (any) fjWater++; }
System.out.println("fjord cells with deep water within 64 blocks: " + fjWater + " of 1000");
int iceTotal = 0, cold = 0;
for (int g = 0; g < d.w*d.h; g++) if (d.ice[g]==1 && d.flow[g] > Hydrology.RIVER_AREA && d.temperature[g] < -1) { cold++; int[] b=t.blockOf(g); boolean any=false; for (int dz=-48; dz<=48 && !any; dz+=8) for (int dx=-48; dx<=48; dx+=8) if (t.column(b[0]+dx,b[1]+dz).ice()!=Terrain.Column.NO_WATER) {any=true;break;} if (any) iceTotal++; }
System.out.println("cold (<-1C) glacial river cells with glacier ice within 48 blocks: " + iceTotal + " of " + cold);
for (int[] at : new int[][]{{-352, 12000}, {Integer.parseInt(System.getProperty("gx","0")), 0}}) {}
// render 1536x1536 blocks around the long fjord, and around a glacier
java.util.function.BiConsumer<int[], String> render = (centre, name) -> {
  int n = 1536; var img = new java.awt.image.BufferedImage(n, n, 1);
  int[] top = new int[n*n]; Terrain.Column[] cs = new Terrain.Column[n*n];
  java.util.stream.IntStream.range(0, n).parallel().forEach(z -> { for (int x = 0; x < n; x++) { var col = t.column(centre[0]-n/2+x, centre[1]-n/2+z); cs[x+z*n]=col; top[x+z*n]=Math.max(col.ground(), col.ice()); } });
  for (int z = 0; z < n; z++) for (int x = 0; x < n; x++) { var col = cs[x+z*n]; int rgb;
    if (col.ice()!=Terrain.Column.NO_WATER) rgb = 0xc8e6f5; else if (col.water()!=Terrain.Column.NO_WATER) { int dep = Math.min(40, col.water()-col.ground()); rgb = (20) << 16 | (70 - dep) << 8 | (200 - dep*3); }
    else { int h = col.ground()-63; rgb = col.biome().color; }
    if (col.water()==Terrain.Column.NO_WATER) { int l = top[Math.max(x-1,0)+z*n] - top[Math.min(x+1,n-1)+z*n]; float f = Math.clamp(1 + l*0.08f, 0.4f, 1.6f); int r=rgb>>16&255,g2=rgb>>8&255,b=rgb&255; rgb = f<=1 ? (int)(r*f)<<16|(int)(g2*f)<<8|(int)(b*f) : (int)(r+(255-r)*(f-1))<<16|(int)(g2+(255-g2)*(f-1))<<8|(int)(b+(255-b)*(f-1)); }
    img.setRGB(x, z, rgb); }
  try { javax.imageio.ImageIO.write(img, "png", new java.io.File("out/" + name + ".png")); } catch (Exception e) { throw new RuntimeException(e); }
};
render.accept(new int[]{7200 - 400, -1312 - 300}, "fjord");
int gl = -1; double bestT = 5;
for (int g = 0; g < d.w*d.h; g++) if (d.ice[g]==1 && d.flow[g] > 20 && d.temperature[g] < bestT && d.precipitation[g] > 600 && Math.abs(Climate.latitude(g/d.w,d.h)) < 60) { bestT = d.temperature[g]; gl = g; }
System.out.println("glacier cell temp " + bestT + " lat " + Climate.latitude(gl/d.w,d.h) + " at " + java.util.Arrays.toString(t.blockOf(gl)));
render.accept(t.blockOf(gl), "glacier");
/exit
