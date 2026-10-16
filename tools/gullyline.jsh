import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
var sb = new StringBuilder(); for (int x = -11500; x < -11300; x += 4) sb.append(t.column(x, -2400).ground()).append(' '); System.out.println("now:  " + sb);
d.format = 13; var t0 = new Terrain(d, 777, 1, 1, 2); sb = new StringBuilder(); for (int x = -11500; x < -11300; x += 4) sb.append(t0.column(x, -2400).ground()).append(' '); System.out.println("was:  " + sb);
/exit
