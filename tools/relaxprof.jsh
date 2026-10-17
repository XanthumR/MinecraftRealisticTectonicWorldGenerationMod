import com.realtectonics.sim.*;
// Where the time goes in relaxedColumns over a 2048x2048 area of seed 777 at -Dfmt (default 46): top methods by own
// samples and by samples inside them (JFR, 10 ms). Pair with relaxhash.jsh, whose hash must not change in speed work.
var d = PlanetData.load(java.nio.file.Path.of("out/p39_777.bin")); d.format = Integer.getInteger("fmt", 46); var t = new Terrain(d, 777, 1, 1, 2);
String tail(String s) { return s.substring(s.lastIndexOf(46) + 1); }
var r = new jdk.jfr.Recording(); r.enable("jdk.ExecutionSample").withPeriod(java.time.Duration.ofMillis(10)); r.start();
t.relaxedColumns(-1024, -1024, 2048, 2048);
r.stop(); var f = java.nio.file.Files.createTempFile("relax", ".jfr"); r.dump(f);
var self = new java.util.HashMap<String, Integer>(); var incl = new java.util.HashMap<String, Integer>(); int total = 0;
for (var e : jdk.jfr.consumer.RecordingFile.readAllEvents(f)) { var st = e.getStackTrace(); if (st == null) continue; total++;
  var fr = st.getFrames(); String top = tail(fr.get(0).getMethod().getType().getName()) + "." + fr.get(0).getMethod().getName() + ":" + fr.get(0).getLineNumber();
  self.merge(top, 1, Integer::sum); var seen = new java.util.HashSet<String>();
  for (var x : fr) { String n = tail(x.getMethod().getType().getName()) + "." + x.getMethod().getName(); if (x.getMethod().getType().getName().contains("realtectonics") && seen.add(n)) incl.merge(n, 1, Integer::sum); } }
final int T = total;
System.out.println("own time (line):"); self.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(Integer.getInteger("top", 16)).forEach(x -> System.out.printf("  %5.1f%%  %s%n", 100.0 * x.getValue() / T, x.getKey()));
System.out.println("inside (incl. callees):"); incl.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(Integer.getInteger("top", 16)).forEach(x -> System.out.printf("  %5.1f%%  %s%n", 100.0 * x.getValue() / T, x.getKey()));
/exit
