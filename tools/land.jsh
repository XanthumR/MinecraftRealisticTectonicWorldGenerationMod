import com.realtectonics.sim.*;
var p = new Planet(1024, 512, 14, 777); for (int i = 0; i < 300; i++) p.step();
int crust = 0; for (float c : p.height) if (c >= Planet.SEA_LEVEL) crust++;
var c = new Climate(p); var hy = new Hydrology(p, c.precipitation, 50, 777);
int land = 0; for (float e : hy.elevation) if (e >= 0) land++;
System.out.printf("medium planet: continental crust %.1f%%, land after volcanoes %.1f%%%n", 100.0 * crust / p.height.length, 100.0 * land / p.height.length);
/exit
