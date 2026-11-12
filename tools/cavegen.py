"""Generates tools/cavecheck.py's three seed 777 squares on the dev server (rcon on), waits until they are loaded, then
stops the server so the chunks are saved. Usage: python tools/cavegen.py"""
import socket, struct, time

s = socket.create_connection(("127.0.0.1", 25575), timeout=1200)


def q(body, t=2):
    b = body.encode() + b"\0\0"
    s.sendall(struct.pack("<iii", len(b) + 8, 1, t) + b)
    n = struct.unpack("<i", s.recv(4))[0]
    data = b""
    while len(data) < n:
        data += s.recv(n - len(data))
    return data[8:-2].decode()


q("dev", 3)
SQUARES = [(256, 768), (0, 256), (512, 768)]  # rivers on flat land, mountains, a dry plain (tools/cavearea.jsh)
for x, z in SQUARES:
    q(f"forceload add {x} {z} {x + 255} {z + 255}")
pending = [(x // 16 + i, z // 16 + j) for x, z in SQUARES for i in range(16) for j in range(16)]
t0 = time.time()
while pending:
    pending = [(a, b) for (a, b) in pending if "passed" not in q(f"execute if loaded {a * 16 + 8} 64 {b * 16 + 8}")]
    if pending: time.sleep(2)
print(f"loaded in {time.time() - t0:.0f} s")
q("stop")
