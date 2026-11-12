import os, sys
from PIL import Image
# Pictures for tools/cavecheck.py's squares: python tools/caverender.py "<world dir>" x0 z0 size out_prefix [z [y0 y1]]
#   <prefix>_map.png: top-down, the surface shaded by height, water blue, cave mouths (as cavecheck counts them) red
#   <prefix>_cut.png: a west-east cut along row z (default: the row with the most water), cave air black, water blue
exec(open(os.path.join(os.path.dirname(__file__), 'cavecheck.py')).read().split('for a in range(2')[0])
x0, z0, n, prefix = int(sys.argv[2]), int(sys.argv[3]), int(sys.argv[4]), sys.argv[5]
T = {(x, z): top(x, z) for x in range(x0, x0 + n) for z in range(z0, z0 + n)}
lo, hi = min(y for y, _ in T.values()), max(y for y, _ in T.values())
img = Image.new('RGB', (n, n))
for (x, z), (y, wet) in T.items():
    ring = sorted(T[(x + i, z + j)][0] for i in range(-4, 5) for j in range(-4, 5) if (x + i, z + j) in T)
    g = int(60 + 170 * (y - lo) / max(1, hi - lo))
    shade = 25 * (T.get((x - 1, z - 1), (y, 0))[0] - y)  # lit from the north-west
    g = max(0, min(255, g + shade))
    img.putpixel((x - x0, z - z0), (40, 90, 200) if wet else (230, 30, 30) if y <= ring[len(ring) // 2] - 4 else (g, g, g))
img.resize((n * 3, n * 3), Image.NEAREST).save(prefix + '_map.png')
row = int(sys.argv[6]) if len(sys.argv) > 6 else max(range(z0, z0 + n), key=lambda z: sum(T[(x, z)][1] for x in range(x0, x0 + n)))
ys = range(int(sys.argv[7]), int(sys.argv[8])) if len(sys.argv) > 8 else range(min(lo, 60) - 60, hi + 10)
cut = Image.new('RGB', (n, len(ys)))
for x in range(x0, x0 + n):
    for k, y in enumerate(ys):
        b = block(x, y, row)
        sky = y > T[(x, row)][0]
        c = (40, 90, 200) if b == 'minecraft:water' else (200, 225, 240) if b in AIR and sky else (10, 10, 10) if b in AIR \
            else (200, 80, 20) if b == 'minecraft:lava' else (120, 110, 100)
        cut.putpixel((x - x0, len(ys) - 1 - k), c)
cut.resize((n * 3, len(ys) * 3), Image.NEAREST).save(prefix + '_cut.png')
print(prefix, 'cut along z', row)
