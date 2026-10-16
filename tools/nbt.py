import gzip, struct, sys
# Minimal NBT reader: prints the world seed, our generator's planet settings and the player's position from level.dat.
def rd(f, t):
    if t == 1: return struct.unpack('>b', f.read(1))[0]
    if t == 2: return struct.unpack('>h', f.read(2))[0]
    if t == 3: return struct.unpack('>i', f.read(4))[0]
    if t == 4: return struct.unpack('>q', f.read(8))[0]
    if t == 5: return struct.unpack('>f', f.read(4))[0]
    if t == 6: return struct.unpack('>d', f.read(8))[0]
    if t == 7: n = rd(f, 3); return f.read(n)
    if t == 8: n = struct.unpack('>H', f.read(2))[0]; return f.read(n).decode('utf-8', 'replace')
    if t == 9: et = rd(f, 1); n = rd(f, 3); return [rd(f, et) for _ in range(n)]
    if t == 10:
        d = {}
        while True:
            tt = rd(f, 1)
            if tt == 0: return d
            name = rd(f, 8); d[name] = rd(f, tt)
    if t == 11: n = rd(f, 3); return [rd(f, 3) for _ in range(n)]
    if t == 12: n = rd(f, 3); return [rd(f, 4) for _ in range(n)]
f = gzip.open(sys.argv[1]); t = rd(f, 1); rd(f, 8); root = rd(f, 10)
wg = root['Data']['WorldGenSettings']
print('seed', wg['seed']); print('planet', wg['dimensions']['minecraft:overworld']['generator'].get('planet'))
import glob, os
for p in glob.glob(os.path.join(os.path.dirname(sys.argv[1]), 'playerdata', '*.dat')): # the player's last saved spot
    pf = gzip.open(p); rd(pf, 1); rd(pf, 8); pl = rd(pf, 10)
    print('player', pl.get('Pos'), 'yaw/pitch', pl.get('Rotation'))
