import com.realtectonics.sim.*;
var p = new Planet(1024, 512, 14, 777); for (int i = 0; i < 300; i++) p.step();
var c = new Climate(p); var hy = new Hydrology(p, c.precipitation, 50, 777);
int w = p.w, h = p.h; int[] dist = new int[w*h], q = new int[w*h]; java.util.Arrays.fill(dist, 99); int n = 0;
for (int g = 0; g < w*h; g++) if (hy.elevation[g] >= 0) { dist[g] = 0; q[n++] = g; }
for (int i = 0; i < n; i++) { int g = q[i], x = g % w, y = g / w; for (int dy=-1; dy<=1; dy++) for (int dx=-1; dx<=1; dx++) { int ny=y+dy; if (ny<0||ny>=h) continue; int nb = Math.floorMod(x+dx,w)+ny*w; if (dist[nb]==99) { dist[nb]=dist[g]+1; q[n++]=nb; } } }
for (int d = 1; d <= 10; d++) { var l = new java.util.ArrayList<Float>(); for (int g = 0; g < w*h; g++) if (dist[g]==d) l.add(Planet.metres(hy.elevation[g])); java.util.Collections.sort(l); System.out.printf("dist %d: n=%d p10 %.0f median %.0f p90 %.0f%n", d, l.size(), l.get(l.size()/10), l.get(l.size()/2), l.get(l.size()*9/10)); }
/exit
