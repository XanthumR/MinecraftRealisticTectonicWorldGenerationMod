"""Stutter report for a JFR recording of the game (jcmd <pid> JFR.start settings=profile duration=60s filename=x.jfr):
FPS per second (Minecraft's own minecraft.ClientFps), ZGC allocation stalls, the longest safepoints, CPU samples per
thread, the render thread's busiest code, and the share of allocation and worldgen CPU from our mod (realtectonics).
Run: python tools/jfrstutter.py x.jfr   (needs the JDK's jfr tool, JFR below)"""
import collections, json, re, subprocess, sys

JFR = r"C:\Program Files\Java\jdk-25.0.4\bin\jfr.exe"
path = sys.argv[1]
run = subprocess.run([JFR, "-J-Xmx4g", "print", "--json", "--stack-depth", "15", "--events",
                      "minecraft.ClientFps,jdk.ZAllocationStall,jdk.SafepointBegin,jdk.ExecutionSample,jdk.ObjectAllocationSample,jdk.GCPhasePause",
                      path], capture_output=True, text=True, encoding="utf-8")
if not run.stdout.strip():
    sys.exit("jfr print failed: " + run.stderr[-500:])
events = json.loads(run.stdout)["recording"]["events"]


def ms(d):
    m = re.match(r"PT([0-9.]+)S", d or "")
    return float(m.group(1)) * 1000 if m else 0.0


def thread(v, key="eventThread"):
    t = (v.get(key) or v.get("sampledThread") or {}).get("javaName") or "?"
    return re.sub(r"[ #-]?\d+$", "", t)[:34]


def ours(frames):
    return any("realtectonics" in f["method"]["type"]["name"] for f in frames)


def where(frames):
    for f in frames:
        n = f["method"]["type"]["name"].replace("/", ".")
        if "realtectonics" in n:
            return "ours: " + n.split(".")[-1] + "." + f["method"]["name"]
    for f in frames:
        n = f["method"]["type"]["name"].replace("/", ".")
        for mod in ("distanthorizons", "seibel", "sodium", "voxy", "iris"):
            if mod in n:
                return mod + ": " + n.split(".")[-1] + "." + f["method"]["name"]
    n = frames[0]["method"]["type"]["name"].replace("/", ".") if frames else "?"
    return "mc/java: " + n.split(".")[-1] + "." + (frames[0]["method"]["name"] if frames else "")


fps = sorted(e["values"]["fps"] for e in events if e["type"] == "minecraft.ClientFps")
if fps:
    print(f"FPS per second: mean {sum(fps) / len(fps):.0f}, min {fps[0]}, 10th percentile {fps[len(fps) // 10]}, max {fps[-1]} ({len(fps)} s)")
stalls = [(thread(e["values"]), ms(e["values"]["duration"])) for e in events if e["type"] == "jdk.ZAllocationStall"]
render = [d for t, d in stalls if t == "Render thread"]
print(f"allocation stalls: {len(stalls)}, render thread {len(render)} (worst {max(render, default=0):.0f} ms)")
sp = sorted(ms(e["values"]["duration"]) for e in events if e["type"] == "jdk.SafepointBegin")
gc = sorted(ms(e["values"]["duration"]) for e in events if e["type"] == "jdk.GCPhasePause")
print(f"safepoints: {len(sp)}, longest {sp[-1] if sp else 0:.1f} ms, over 5 ms: {sum(d > 5 for d in sp)}; GC pauses longest {gc[-1] if gc else 0:.2f} ms")

cpu = collections.Counter()
cpu_ours = collections.Counter()
render_code = collections.Counter()
for e in events:
    if e["type"] != "jdk.ExecutionSample":
        continue
    v = e["values"]
    t = thread(v, "sampledThread")
    fr = (v.get("stackTrace") or {}).get("frames") or []
    cpu[t] += 1
    if ours(fr):
        cpu_ours[t] += 1
    if t == "Render thread":
        render_code[where(fr)] += 1
total = sum(cpu.values()) or 1
print("\nCPU samples by thread (share of all; part in our code):")
for t, c in cpu.most_common(10):
    print(f"  {t:34s} {100 * c / total:5.1f}%   ours {100 * cpu_ours[t] / max(c, 1):5.1f}%")
rt = sum(render_code.values()) or 1
print("\nRender thread, busiest code:")
for k, c in render_code.most_common(8):
    print(f"  {100 * c / rt:5.1f}%  {k}")

alloc = collections.Counter()
alloc_where = collections.Counter()
atotal = 0
for e in events:
    if e["type"] != "jdk.ObjectAllocationSample":
        continue
    v = e["values"]
    w = v["weight"]
    atotal += w
    fr = (v.get("stackTrace") or {}).get("frames") or []
    alloc["ours" if ours(fr) else "other"] += w
    if ours(fr):
        alloc_where[where(fr) + "  <" + v["objectClass"]["name"].split("/")[-1] + ">"] += w
print(f"\nallocation: ours {100 * alloc['ours'] / max(atotal, 1):.1f}% of all sampled")
for k, w in alloc_where.most_common(8):
    print(f"  {100 * w / max(atotal, 1):5.1f}%  {k}")
