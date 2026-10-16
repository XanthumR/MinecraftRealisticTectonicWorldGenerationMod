import com.realtectonics.sim.*;
var p = new Planet(1024, 512, 14, 777); for (int i = 0; i < 300; i++) p.step();
var c = new Climate(p); var hy = new Hydrology(p, c.precipitation, 50, 777);
var d = PlanetData.from(p, c, hy); var t = new Terrain(d, 777);
int land = 0, glac = 0, fj = 0, mouths = 0;
int[] len = new int[d.w * d.h];
var mouthList = new java.util.ArrayList<Integer>();
for (int g = 0; g < d.w * d.h; g++) { if (d.metres[g] >= 0) land++; if (d.ice[g] == 1) glac++; if (d.ice[g] == 2) { fj++; int r = d.receiver[g]; if (d.metres[r] < 0) { mouths++; mouthList.add(g);} } }
// fjord length: longest chain of fjord cells upstream of each mouth
int best = 0, bestMouth = -1; int[] depth = new int[d.w*d.h];
for (int g = 0; g < d.w*d.h; g++) if (d.ice[g]==2) { int n=0, x=g; while (d.ice[x]==2) { n++; x=d.receiver[x]; } if (n>best) {best=n; bestMouth=x;} depth[g]=n; }
var hist = new int[12]; for (int m : mouthList) { int L=0; for (int g=0; g<d.w*d.h; g++) if (d.ice[g]==2) { int x=g,n=0; while(d.ice[x]==2){n++; if (x==m) break; x=d.receiver[x];} if (x==m) L=Math.max(L,n);} hist[Math.min(L,11)]++; }
System.out.printf("land %d, glaciated %d (%.1f%% of land), fjord cells %d, fjord mouths %d, longest fjord %d cells%n", land, glac, 100.0*glac/land, fj, mouths, best);
System.out.println("fjord length histogram (cells): " + java.util.Arrays.toString(hist));
var lats = new int[10]; for (int m : mouthList) lats[(int)Math.min(9, Math.abs(Climate.latitude(m / d.w, d.h)) / 10)]++;
System.out.println("fjord mouths by |latitude| decade: " + java.util.Arrays.toString(lats));
// glacier columns sampled around glaciated cells
var rnd = new java.util.Random(3); int s=0, iceCols=0, fjWater=0, fjSamples=0;
while (s < 3000) { int g = rnd.nextInt(d.w*d.h); if (d.ice[g]!=1 || d.flow[g] <= Hydrology.RIVER_AREA) continue; s++; int[] b=t.blockOf(g); if (t.column(b[0],b[1]).ice()!=Terrain.Column.NO_WATER) iceCols++; }
s = 0; while (s < 1000 && fj > 0) { int g = rnd.nextInt(d.w*d.h); if (d.ice[g]!=2) continue; s++; int[] b=t.blockOf(g); boolean any=false; for (int dz=-24; dz<=24 && !any; dz+=3) for (int dx=-24; dx<=24; dx+=3) { var col=t.column(b[0]+dx,b[1]+dz); if (col.water()==Terrain.SEA_Y-1 && col.ground() < Terrain.SEA_Y-5) {any=true;break;} } if (any) fjWater++; }
System.out.printf("glaciated river cells with ice at the centre: %d of 3000; fjord cells with deep sea water within 24 blocks: %d of %d%n", iceCols, fjWater, s);
System.out.println("longest fjord mouth cell " + bestMouth + " at block " + java.util.Arrays.toString(t.blockOf(bestMouth)) + " lat " + Climate.latitude(bestMouth / d.w, d.h));
/exit
