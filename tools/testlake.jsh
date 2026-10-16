import com.realtectonics.sim.*;
Planet p = new Planet(256, 128, 8, 7); for (int i = 0; i < 50; i++) p.step();
Climate climate = new Climate(p); var hy = new Hydrology(p, climate.precipitation, 20, 7); var data = PlanetData.from(p, climate, hy);
int lakes = 0, first = -1; for (int g = 0; g < data.w*data.h; g++) if (!Float.isNaN(data.lakeMetres[g])) { lakes++; if (first < 0) first = g; }
System.out.println("lake cells " + lakes + " first " + first + " level " + data.lakeMetres[first] + " ground " + data.metres[first] + " lat " + Climate.latitude(first / data.w, data.h));
/exit
