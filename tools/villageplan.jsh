import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777);
for (int[] c : new int[][]{{208, 256}, {3280, -192}, {-3152, 1120}, {6288, 2336}}) {
  var sb = new StringBuilder("{"); boolean first = true;
  for (int z = c[1] - 64; z <= c[1] + 64; z += 4) for (int x = c[0] - 64; x <= c[0] + 64; x += 4) { sb.append(first ? "" : ", ").append("\"").append(x).append(",").append(z).append("\": ").append(t.column(x, z).ground()); first = false; }
  java.nio.file.Files.writeString(java.nio.file.Path.of("build/vp_" + c[0] + "_" + c[1] + ".json"), sb.append("}").toString());
}
/exit
