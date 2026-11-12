import os, sys
# Cave mouths and thin rock under water in a saved world's region files (format 98's cave roofs), per square:
# python tools/cavecheck.py "<world dir>" x0 z0 size [size2 ...]  (squares of size blocks from x0 z0, e.g. made by forceload)
#   mouths: patches (4+ columns) where the top of the terrain lies 4+ below the median of the 9x9 round it, not water
#           (cave mouths, plus a few cliff bases, the same with and without caves)
#   thin:   water columns with cave air within ROOF blocks under their bed; near: air that close under the water's
#           level within REACH blocks of the water, sideways
#   caves:  cave air blocks per column below the surface (every 4th column), so caves are kept, only roofed
ROOF, REACH = 12, 8
REGION = 'region' if os.path.isdir(os.path.join(sys.argv[1], 'region')) else 'dimensions/minecraft/overworld/region'  # 1.21.1, 26.x
exec(open(os.path.join(os.path.dirname(__file__), 'mcablocks.py')).read().split('legend = {}')[0].replace(
    "world, x0, z0, x1, z1 = sys.argv[1], *map(int, sys.argv[2:6])", "world = sys.argv[1]").replace(
    "fixed_y = int(sys.argv[6]) if len(sys.argv) > 6 else None", "").replace(
    "'dimensions/minecraft/overworld/region'", "REGION"))

AIR = ('minecraft:air', 'minecraft:cave_air')
SOFT = ('leaves', 'log', 'grass', 'fern', 'flower', 'bush', 'vine', 'snow', 'poppy', 'dandelion', 'tulip', 'orchid', 'allium',
        'bluet', 'daisy', 'cornflower', 'lily', 'rose', 'lilac', 'peony', 'sapling', 'mushroom', 'cactus', 'cane', 'kelp',
        'seagrass', 'moss_carpet', 'pumpkin', 'melon', 'berry', 'dripleaf', 'azalea', 'roots', 'wood', 'petals', 'mangrove', 'litter')


def soft(name):
    return name in AIR or not name.endswith('_block') and any(s in name for s in SOFT)


def top(x, z):
    """Highest terrain block (plants, trees and snow layers skipped) and whether it is water."""
    y = 319
    while y > -60:
        b = block(x, y, z)
        if not soft(b): return y, b == 'minecraft:water'
        y -= 1
    return y, False


for a in range(2, len(sys.argv) - 2, 3):
    x0, z0, n = map(int, sys.argv[a:a + 3])
    T = {(x, z): top(x, z) for x in range(x0, x0 + n) for z in range(z0, z0 + n)}
    low = set()
    for (x, z), (y, wet) in T.items():
        if wet: continue
        ring = sorted(T[(x + i, z + j)][0] for i in range(-4, 5) for j in range(-4, 5) if (x + i, z + j) in T)
        if y <= ring[len(ring) // 2] - 4: low.add((x, z))
    seen, mouths = set(), 0
    for p in low:  # patches of 4+ columns
        if p in seen: continue
        stack, size = [p], 0
        seen.add(p)
        while stack:
            x, z = stack.pop(); size += 1
            for q in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)):
                if q in low and q not in seen: seen.add(q); stack.append(q)
        mouths += size >= 4
    wet = [(x, z) for (x, z), (y, w) in T.items() if w]
    thin = 0
    for x, z in wet:
        y = T[(x, z)][0]
        while block(x, y, z) == 'minecraft:water': y -= 1
        thin += any(block(x, y - k, z) in AIR for k in range(ROOF))
    near = set()
    for x, z in wet[::7]:
        level = T[(x, z)][0]
        for i in range(-REACH, REACH + 1, 2):
            for j in range(-REACH, REACH + 1, 2):
                q = (x + i, z + j)
                if q in T and not T[q][1] and q not in near and any(block(q[0], level - k, q[1]) in AIR for k in range(1, ROOF)) \
                        and T[q][0] >= level: near.add(q)
    air = cols = 0
    for (x, z), (y, w) in list(T.items())[::16]:
        cols += 1
        air += sum(block(x, k, z) in AIR for k in range(-58, y - 4))
    print(f"x {x0} z {z0} {n}: mouths {mouths} ({len(low)} columns), water columns {len(wet)}, thin roof {thin}, "
          f"air beside water {len(near)}, cave air {air / cols:.1f} per column")
