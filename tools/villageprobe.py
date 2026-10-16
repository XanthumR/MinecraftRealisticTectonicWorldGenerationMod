import socket, struct, sys, re, json, time
s = socket.create_connection(("127.0.0.1", 25575), timeout=600)
def send(t, body):
    s.sendall(struct.pack("<iii", len(body.encode()) + 10, 1, t) + body.encode() + b"\0\0"); n = struct.unpack("<i", s.recv(4))[0]; data = b""
    while len(data) < n: data += s.recv(n - len(data))
    return data[8:-2].decode()
send(3, "dev")
cx, cz, r, step = int(sys.argv[1]), int(sys.argv[2]), 64, 4
print(send(2, f"forceload add {cx-r} {cz-r} {cx+r} {cz+r}"))
send(2, "kill @e[tag=probe]"); print(send(2, f"summon marker {cx} 300 {cz} {{Tags:[\"probe\"]}}"))
out = {}
for z in range(cz - r, cz + r + 1, step):
    for x in range(cx - r, cx + r + 1, step):
        for attempt in range(60):  # forceloaded chunks generate in the background
            y = re.search(r"to [-\d.]+, ([-\d.]+),", send(2, f"execute positioned {x} 0 {z} positioned over ocean_floor run tp @e[tag=probe,limit=1] ~ ~ ~"))
            if y: break
            time.sleep(1)
        if y: out[f"{x},{z}"] = int(float(y.group(1))) - 1
json.dump(out, open(f"build/vs_{cx}_{cz}.json", "w"))
print(len(out), "columns")
