import com.realtectonics.sim.*;
var d = PlanetData.generate(777, PlanetSettings.DEFAULT, x -> {}); var t = new Terrain(d, 777, 1, 1, 2);
var f = Terrain.class.getDeclaredField("deltasByCellFans"); f.setAccessible(true); var m = (java.util.Map<Integer, java.util.List<?>>) f.get(t);
var fans = new java.util.HashSet<Object>(); m.values().forEach(fans::addAll);
Object first = fans.iterator().next(); System.out.println("fans " + fans.size() + " e.g. " + first + " at " + java.util.Arrays.toString(t.blockOf(317 + 123 * d.w)));
/exit
