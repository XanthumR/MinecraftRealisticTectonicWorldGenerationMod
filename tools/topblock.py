import subprocess, sys
# Find the top non-air block at x z by binary-style scan with rcon "execute if block".
x, z = sys.argv[1], sys.argv[2]
def q(c): return subprocess.run(["python", "tools/rcon.py", c], capture_output=True, text=True).stdout.strip()
for y in range(260, 40, -1):
    if "passed" in q(f"execute unless block {x} {y} {z} minecraft:air"):
        for b in ["snow", "grass_block[snowy=true]", "grass_block[snowy=false]", "ice", "water", "stone", "andesite", "snow_block", "spruce_leaves", "oak_leaves", "short_grass"]:
            if "passed" in q(f"execute if block {x} {y} {z} minecraft:{b}"): print(x, z, "top y", y, b); break
        else: print(x, z, "top y", y, "other")
        break
