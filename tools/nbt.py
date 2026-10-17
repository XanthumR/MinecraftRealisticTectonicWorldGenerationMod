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
import glob, os
def load(path):
    f = gzip.open(path); t = rd(f, 1); rd(f, 8); return rd(f, t)
root, here = load(sys.argv[1]), os.path.dirname(sys.argv[1])
wg = root['Data'].get('WorldGenSettings')
if wg is None: # Minecraft 26.x keeps it in its own file
    wg = load(os.path.join(here, 'data', 'minecraft', 'world_gen_settings.dat'))
    wg = wg.get('data', wg)
print('seed', wg.get('seed'), '| keys', sorted(wg)[:8])
dims = wg.get('dimensions', {})
print('planet', dims.get('minecraft:overworld', {}).get('generator', {}).get('planet'))
for p in glob.glob(os.path.join(here, 'playerdata', '*.dat')) + glob.glob(os.path.join(here, 'players', 'data', '*.dat')): # the player's last saved spot
    pl = load(p)
    print('player', pl.get('Pos'), 'yaw/pitch', pl.get('Rotation'))
