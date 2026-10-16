"""Count blocks of given types in a 32x32 column area on the dev server (rcon): each type is swapped to a marker
block and back, and 'fill' reports how many it changed (replacing a block with itself counts nothing). Test worlds only. Usage: python tools/blockcount.py x z block [block ...]
Forceloads the area first; y from 40 to 360 in 32-block bands (fill's limit is 32768 blocks)."""
import re, subprocess, sys, time

def q(c):
    return subprocess.run(["python", "tools/rcon.py", c], capture_output=True, text=True).stdout

x, z = int(sys.argv[1]), int(sys.argv[2])
q(f"forceload add {x} {z} {x + 31} {z + 31}")
time.sleep(4)
for b in sys.argv[3:]:
    n = 0
    for y in range(40, 360, 32):
        m = re.search(r"filled (\d+)", q(f"fill {x} {y} {z} {x + 31} {y + 31} {z + 31} minecraft:structure_void replace {b}"))
        if m:
            n += int(m.group(1))
            q(f"fill {x} {y} {z} {x + 31} {y + 31} {z + 31} {b} replace minecraft:structure_void")
    print(f"{b}: {n}")
q(f"forceload remove {x} {z} {x + 31} {z + 31}")
