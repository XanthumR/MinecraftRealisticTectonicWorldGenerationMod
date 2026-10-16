import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
var sb = new StringBuilder();
for (int x = 6420; x < 6500; x += 2) { var c = t.column(x, -608); sb.append(c.water() == Terrain.Column.NO_WATER ? "" + c.ground() : "~" + c.water()).append(' '); }
System.out.println(sb);
/exit
