import com.realtectonics.sim.*;
for (float v : new float[]{0, 0.5f, 1, 2}) { var d = PlanetData.generate(777, new PlanetSettings(512, 10, 0.7f, 1, 1, v), x -> {}); long hot = d.volcanoes.stream().filter(Volcano::hotspot).filter(q -> !q.gone()).count(), arc = d.volcanoes.stream().filter(q -> !q.hotspot()).count(); System.out.printf("volcanoes %.1f: %d arc, %d hotspot%n", v, arc, hot); }
/exit
