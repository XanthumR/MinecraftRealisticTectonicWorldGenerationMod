import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777);
var txt = java.nio.file.Files.readString(java.nio.file.Path.of("build/village_surface.json"));
var parts = txt.replaceAll("[{}\" ]", "").split(",");
int n = 0, same = 0, up = 0, down = 0, maxUp = 0, maxDown = 0;
var grid = new StringBuilder();
var map = new java.util.TreeMap<String, Integer>();
for (int i = 0; i + 1 < parts.length; i += 2) { int x = Integer.parseInt(parts[i]); String[] zy = parts[i + 1].split(":"); int z = Integer.parseInt(zy[0]), y = Integer.parseInt(zy[1]); var c = t.column(x, z); int diff = y - c.ground(); n++; if (diff == 0) same++; else if (diff > 0) { up++; maxUp = Math.max(maxUp, diff); } else { down++; maxDown = Math.min(maxDown, diff); } map.put(z + "," + x, diff); }
System.out.printf("columns %d: unchanged %d, higher %d (max +%d), lower %d (max %d)%n", n, same, up, maxUp, down, maxDown);
for (int z = 192; z <= 320; z += 8) { var sb = new StringBuilder(); for (int x = 144; x <= 272; x += 8) { int v = map.getOrDefault(z + "," + x, 999); sb.append(String.format("%4d", v)); } System.out.println(sb); }
/exit
