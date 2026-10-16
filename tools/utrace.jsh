import com.realtectonics.sim.*;
var d = PlanetData.load(java.nio.file.Path.of("run/saves/New World (5)/realtectonics/planet.bin")); d.format = Integer.getInteger("fmt", d.format); var t = new Terrain(d, 7616411802006901329L, 1, 1, 2);
System.out.println(t.column(Integer.getInteger("tx"), Integer.getInteger("tz")));
/exit
