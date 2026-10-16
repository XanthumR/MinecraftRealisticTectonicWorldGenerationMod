"""Chunk generation speed on the dev server, the whole pipeline (terrain, biomes, features, light): forceloads an n x n
chunk square at chunk (cx, cz) and times until every chunk is loaded (rcon 'execute if loaded' on each, one
connection). Usage: python tools/genspeed.py cx cz n. Use a fresh area each run: loaded chunks come back from disk."""
import socket, struct, sys, time

s = socket.create_connection(("127.0.0.1", 25575), timeout=600)


def q(body, t=2):
    b = body.encode() + b"\0\0"
    s.sendall(struct.pack("<iii", len(b) + 8, 1, t) + b)
    n = struct.unpack("<i", s.recv(4))[0]
    data = b""
    while len(data) < n:
        data += s.recv(n - len(data))
    return data[8:-2].decode()


q("dev", 3)
cx, cz, n = int(sys.argv[1]), int(sys.argv[2]), int(sys.argv[3])
start = time.time()
for bx in range(cx, cx + n, 16):  # forceload takes at most 256 chunks per command
    for bz in range(cz, cz + n, 16):
        q(f"forceload add {bx * 16} {bz * 16} {min(bx + 16, cx + n) * 16 - 1} {min(bz + 16, cz + n) * 16 - 1}")
pending = [(x, z) for x in range(cx, cx + n) for z in range(cz, cz + n)]
while pending:
    pending = [(x, z) for (x, z) in pending if "passed" not in q(f"execute if loaded {x * 16 + 8} 64 {z * 16 + 8}")]
    if pending:
        time.sleep(0.5)
t = time.time() - start
print(f"{n * n} chunks in {t:.1f}s: {n * n / t:.1f} chunks/s")
q("forceload remove all")
