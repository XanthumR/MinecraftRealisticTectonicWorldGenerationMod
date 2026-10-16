import com.realtectonics.sim.*;
// Elevation and rain along one latitude (-Dlat, -Dlon0, -Dlon1) of the Earth planet, every 1 degree.
var d = PlanetData.generate(1, PlanetSettings.DEFAULT.withWidth(1024).withEarth(true), x -> {});
double lat = Double.parseDouble(System.getProperty("lat", "-28"));
int y = (int) ((1 - Math.sin(Math.toRadians(lat))) * d.h / 2);
for (double lon = Double.parseDouble(System.getProperty("lon0", "-70")); lon <= Double.parseDouble(System.getProperty("lon1", "-40")); lon += 1) {
    int x = (int) ((lon + 180) / 360 * d.w), g = x + y * d.w;
    System.out.printf("lon %5.0f  m %5.0f  T %4.1f  rain %5.0f  summer %.2f%n", lon, d.metres[g], d.temperature[g], d.precipitation[g], d.summerRain[g]);
}
/exit
