import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
var rnd = new java.util.Random(5); int[][] hist = new int[6][6]; int high = 0;
for (int i = 0; i < 400000; i++) { int x = rnd.nextInt(t.circumference) - t.circumference / 2, z = rnd.nextInt(t.southPoleZ() - t.northPoleZ()) + t.northPoleZ();
    var c = t.column(x, z); if (c.ground() < Terrain.SEA_Y + 40 || c.water() != Terrain.Column.NO_WATER) continue; high++;
    int up = 0, down = 0;
    for (int[] o : new int[][]{{6,0},{-6,0},{0,6},{0,-6}}) { int dd = t.column(x + o[0], z + o[1]).ground() - c.ground(); up = Math.max(up, dd); down = Math.max(down, -dd); }
    hist[Math.min(5, up / 4)][Math.min(5, down / 4)]++; }
System.out.println("rows: up/4 (0..5+), cols: down/4; share of " + high);
for (var r : hist) { for (int v : r) System.out.printf("%6.2f", 100.0 * v / high); System.out.println(); }
/exit
