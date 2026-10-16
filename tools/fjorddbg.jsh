import com.realtectonics.sim.*;
var p = new Planet(1024, 512, 14, 777); for (int i = 0; i < 300; i++) p.step();
var c = new Climate(p); var hy = new Hydrology(p, c.precipitation, 50, 777);
var d = PlanetData.from(p, c, hy); var t = new Terrain(d, 777);
int[] b = t.blockOf(49623); System.out.println("mouth cell centre " + java.util.Arrays.toString(b));
// print cell grid around mouth: ice/metres/lake/flow
int mx = 49623 % d.w, my = 49623 / d.w;
for (int y = my - 6; y <= my + 6; y++) { var sb = new StringBuilder(); for (int x = mx - 6; x <= mx + 6; x++) { int g = x + y*d.w; sb.append(String.format("%s%5.0f%s ", d.ice[g]==2?"F":d.ice[g]==1?"g":" ", d.metres[g], Float.isNaN(d.lakeMetres[g])?" ":"L")); } System.out.println(sb); }
for (int y = my - 6; y <= my + 6; y++) { var sb = new StringBuilder(); for (int x = mx - 6; x <= mx + 6; x++) { int g = x + y*d.w; int r = d.receiver[g]; String a = r<0?"*": r==g-1?"<":r==g+1?">":r==g-d.w?"^":r==g+d.w?"v":r==g-d.w-1?"\\":r==g-d.w+1?"/":r==g+d.w-1?"/":"\\"; sb.append(a); } System.out.println(sb); }
for (int z = b[1] - 160; z <= b[1] + 160; z += 16) { var sb = new StringBuilder(); for (int x = b[0] - 160; x <= b[0] + 160; x += 16) { var col = t.column(x, z); sb.append(String.format("%4d%s", col.ground(), col.water()==Terrain.Column.NO_WATER?" ":col.water()==62?"~":"w")); } System.out.println(sb); }
/exit
