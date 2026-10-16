import socket, struct, sys
def pkt(i, t, body): b = body.encode() + b"\0\0"; return struct.pack("<iii", len(b) + 8, i, t) + b
s = socket.create_connection(("127.0.0.1", 25575), timeout=600)
def send(t, body):
    s.sendall(pkt(1, t, body)); n = struct.unpack("<i", s.recv(4))[0]; data = b""
    while len(data) < n: data += s.recv(n - len(data))
    return data[8:-2].decode()
send(3, "dev")
for cmd in sys.argv[1:]: print(">", cmd, "\n", send(2, cmd))
