import socket, struct, sys, json
s = socket.create_connection(("127.0.0.1", 25575), timeout=600)
def send(t, body):
    s.sendall(struct.pack("<iii", len(body.encode()) + 10, 1, t) + body.encode() + b"\0\0"); n = struct.unpack("<i", s.recv(4))[0]; data = b""
    while len(data) < n: data += s.recv(n - len(data))
    return data[8:-2].decode()
def isblock(x, y, z, b): return "passed" in send(2, f"execute if block {x} {y} {z} {b}")
send(3, "dev")
cx, cz = sys.argv[1], sys.argv[2]; surf = json.load(open(f"build/vs_{cx}_{cz}.json"))
plan = json.load(open(f"build/vp_{cx}_{cz}.json"))
floating = checked = 0; where = []
for k, top in surf.items():
    x, z = map(int, k.split(",")); g = plan[k]
    if top - g < 3: continue  # something stands here
    checked += 1
    y0 = next((y for y in range(g - 24, top + 1) if isblock(x, y, z, "minecraft:air")), None)
    if y0 is None or y0 >= top: continue
    natural = any(isblock(x, y0 - 1, z, t) for t in ["#minecraft:dirt", "#minecraft:base_stone_overworld", "#minecraft:sand", "minecraft:gravel", "minecraft:sandstone"])
    if not natural: continue
    y1 = next((y for y in range(y0, top + 1) if not isblock(x, y, z, "minecraft:air")), None)
    if y1 is not None and any(isblock(x, y1, z, t) for t in ["#minecraft:planks", "minecraft:cobblestone", "#minecraft:logs", "minecraft:smooth_stone", "#minecraft:terracotta", "minecraft:mossy_cobblestone"]):
        floating += 1; where.append((x, y0, y1, z))
print(f"structure columns {checked}, floating over natural ground: {floating} {where[:6]}")
