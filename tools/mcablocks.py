import io, os, struct, sys, zlib
# Top non-air blocks straight from a saved world's region files (no server needed):
# python tools/mcablocks.py "<world dir>" x0 z0 x1 z1 [y]   -> one letter per column, legend after; with y, blocks at that height.
exec(open(os.path.join(os.path.dirname(__file__), 'nbt.py')).read().split('import glob')[0])

world, x0, z0, x1, z1 = sys.argv[1], *map(int, sys.argv[2:6])
fixed_y = int(sys.argv[6]) if len(sys.argv) > 6 else None
chunks = {}

def chunk(cx, cz):
    if (cx, cz) not in chunks:
        path = os.path.join(world, 'dimensions/minecraft/overworld/region', f'r.{cx >> 5}.{cz >> 5}.mca')
        with open(path, 'rb') as f: data = f.read()
        off = struct.unpack('>I', data[4 * ((cx & 31) + (cz & 31) * 32):][:4])[0]
        if off >> 8 == 0: chunks[(cx, cz)] = None; return None
        start = (off >> 8) * 4096; n, kind = struct.unpack('>IB', data[start:start + 5])
        raw = zlib.decompress(data[start + 5:start + 4 + n])
        f = io.BytesIO(raw); t = rd(f, 1); rd(f, 8); root = rd(f, t)
        chunks[(cx, cz)] = {s['Y']: s['block_states'] for s in root['sections'] if 'block_states' in s}
    return chunks[(cx, cz)]

def block(x, y, z):
    secs = chunk(x >> 4, z >> 4)
    bs = secs and secs.get(y >> 4)
    if not bs: return 'minecraft:air'
    pal = bs['palette']
    if len(pal) == 1: return pal[0]['Name']
    bits = max(4, (len(pal) - 1).bit_length()); per = 64 // bits
    i = (x & 15) + (z & 15) * 16 + (y & 15) * 256
    word = bs['data'][i // per] & 0xFFFFFFFFFFFFFFFF
    return pal[(word >> (i % per * bits)) & ((1 << bits) - 1)]['Name']

legend = {}
for z in range(z0, z1 + 1):
    row = ''
    for x in range(x0, x1 + 1):
        if fixed_y is not None: name = block(x, fixed_y, z)
        else:
            y = 160
            while y > -64 and block(x, y, z) in ('minecraft:air', 'minecraft:cave_air'): y -= 1
            name = block(x, y, z)
        legend.setdefault(name, chr(ord('A') + len(legend)) if name != 'minecraft:water' else '~')
        row += legend[name]
    print(z, row)
print({v: k for k, v in legend.items()})
