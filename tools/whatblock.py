import socket, struct, sys
s = socket.create_connection(("127.0.0.1", 25575), timeout=600)
def send(t, body):
    s.sendall(struct.pack("<iii", len(body.encode()) + 10, 1, t) + body.encode() + b"\0\0"); n = struct.unpack("<i", s.recv(4))[0]; data = b""
    while len(data) < n: data += s.recv(n - len(data))
    return data[8:-2].decode()
send(3, "dev")
tags = ["minecraft:air", "#minecraft:leaves", "#minecraft:logs", "#minecraft:planks", "#minecraft:dirt", "minecraft:dirt_path", "minecraft:cobblestone", "#minecraft:wool", "#minecraft:beds", "#minecraft:fences", "#minecraft:stairs", "#minecraft:slabs", "minecraft:glass_pane", "#minecraft:doors", "minecraft:hay_block", "minecraft:water", "minecraft:farmland", "minecraft:short_grass", "#minecraft:flowers", "minecraft:torch", "minecraft:wall_torch", "minecraft:bell", "minecraft:lantern", "minecraft:composter", "#minecraft:base_stone_overworld", "minecraft:smooth_stone", "minecraft:stripped_oak_log", "#minecraft:crops", "minecraft:white_terracotta"]
x, z, y0, y1 = map(int, sys.argv[1:5])
for y in range(y0, y1 + 1):
    hit = next((t for t in tags if "passed" in send(2, f"execute if block {x} {y} {z} {t}")), "?")
    print(y, hit)
