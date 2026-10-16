import com.realtectonics.sim.*;
var p = new Planet(1024, 512, 14, 777); for (int i = 0; i < 300; i++) p.step();
var c = new Climate(p); var hy = new Hydrology(p, c.precipitation, c.anomaly, 50, 777);
int dry = 0, lake = 0; double dryT = 0, dryFlow = 0;
for (int g = 0; g < hy.w * hy.h; g++) { boolean basin = hy.surface[g] > hy.elevation[g] + 1e-3f; if (!basin) continue; if (hy.isLake(g)) lake++; else { dry++; dryT += c.temperature[g]; dryFlow += hy.flow[g]; } }
System.out.printf("basin cells: %d lake, %d dry (mean %.1f C, mean flow %.2f)%n", lake, dry, dryT / Math.max(1, dry), dryFlow / Math.max(1, dry));
/exit
