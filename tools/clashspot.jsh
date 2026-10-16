import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
for (int[] p : new int[][]{{-2884, 4544}, {12895, 9433}, {-2797, 3825}})
    for (int k = 0; k < 8; k++) { int x = p[0] + (int) (32 * Math.cos(k * Math.PI / 4)), z = p[1] + (int) (32 * Math.sin(k * Math.PI / 4));
        var c = t.column(x, z); System.out.println(p[0] + "," + p[1] + " k" + k + " " + c.biome() + " y" + c.ground() + " here y" + t.column(p[0], p[1]).ground()); }
/exit
