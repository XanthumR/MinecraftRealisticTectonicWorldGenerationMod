import socket, struct, sys
s = socket.create_connection(("127.0.0.1", 25575), timeout=600)
def send(t, body):
    b = body.encode() + b"\0\0"; s.sendall(struct.pack("<iii", len(b) + 8, 1, t) + b)
    n = struct.unpack("<i", s.recv(4))[0]; data = b""
    while len(data) < n: data += s.recv(n - len(data))
    return data[8:-2].decode()
send(3, "dev")
hits = [p for p in open(sys.argv[1]).read().split("\n") if p and any("passed" in send(2, f"execute if block {p} minecraft:{b}") for b in ("air", "cave_air"))]
print(f"air at or just under the sea floor: {len(hits)}", hits[:5])
