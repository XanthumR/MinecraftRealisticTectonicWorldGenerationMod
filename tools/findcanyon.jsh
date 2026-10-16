import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
int found = 0;
for (int c = 0; c < d.w*d.h && found < 3; c++) { int r = d.receiver[c]; if (r < 0 || d.flow[c] < 100 || d.precipitation[c] > 350 || d.metres[c] < 700 || d.metres[r] < 0) continue; float drop = (d.metres[c] - d.metres[r]) / 15f; if (drop > 10) continue; found++; System.out.println("canyon river at " + java.util.Arrays.toString(t.blockOf(c)) + " " + d.metres[c] + " m rain " + d.precipitation[c]); }
/exit
