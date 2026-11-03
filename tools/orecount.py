"""Ore blocks in an n x n chunk square around block (x, z) on the dev server (rcon on): forceloads it, waits until every
chunk is loaded, then counts each ore with 'fill ... stone replace <ore>' (it destroys them: a test world only).
Usage: python tools/orecount.py x z n. Format 92 deposits and vanilla's thinned ores, against format 91."""
import socket, struct, sys, time, re

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
x, z, n = int(sys.argv[1]), int(sys.argv[2]), int(sys.argv[3])
cx0, cz0 = x // 16 - n // 2, z // 16 - n // 2
q(f"forceload add {cx0 * 16} {cz0 * 16} {(cx0 + n) * 16 - 1} {(cz0 + n) * 16 - 1}")
pending = [(a, b) for a in range(cx0, cx0 + n) for b in range(cz0, cz0 + n)]
while pending:
    pending = [(a, b) for (a, b) in pending if "passed" not in q(f"execute if loaded {a * 16 + 8} 64 {b * 16 + 8}")]
    if pending:
        time.sleep(0.5)
total = {}
for ore in ["coal", "iron", "copper", "gold", "redstone", "lapis", "diamond", "emerald"]:
    count = 0
    for name in [f"{ore}_ore", f"deepslate_{ore}_ore"]:
        for a in range(cx0, cx0 + n):
            for b in range(cz0, cz0 + n):
                for y in range(-64, 768, 128):  # 16 x 16 x 128 = the most one fill may touch
                    m = re.search(r"filled (\d+)", q(f"fill {a * 16} {y} {b * 16} {a * 16 + 15} {y + 127} {b * 16 + 15} minecraft:stone replace minecraft:{name}"))
                    count += int(m.group(1)) if m else 0
    total[ore] = count
q(f"forceload remove {cx0 * 16} {cz0 * 16} {(cx0 + n) * 16 - 1} {(cz0 + n) * 16 - 1}")
print(f"x {x} z {z}, {n}x{n} chunks, per chunk: " + ", ".join(f"{k} {v / (n * n):.0f}" for k, v in total.items()))
