import com.realtectonics.sim.*;
// Flood-season rivers: big gentle reaches fed by snowmelt or a monsoon. Counts and one spot.
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
int big = 0, monsoon = 0, melt = 0; int[] at = null;
for (int g = 0; g < d.flow.length; g++) { int r = d.receiver[g]; if (d.flow[g] < 400 || r < 0 || d.metres[g] < 0 || d.metres[r] < 0) continue;
  if ((d.metres[g] - d.metres[r]) / 15f > 3) continue; big++;
  float fs = Climate.frozenShare(d.temperature[g], d.amplitude[g]);
  boolean m = d.summerRain[g] > 0.75f, s = fs > 0.3f && fs < 0.7f; if (m) monsoon++; if (s) melt++;
  if ((m || s) && d.ice[g] == 0 && at == null && d.flow[g] > 1500) at = t.blockOf(g); }
System.out.println("big gentle river cells " + big + ", monsoon-fed " + monsoon + ", snowmelt-fed " + melt + ", one at " + java.util.Arrays.toString(at));
/exit
