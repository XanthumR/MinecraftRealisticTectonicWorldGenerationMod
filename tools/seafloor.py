"""Shaded relief of a planet's cells with the sea drained, seen from above like a bathymetry map, for landforms too big
for uview3d.jsh (trenches, ridges, shelves, island arcs): seafloor.py planet.bin out.png cellX cellY [width=192]
[height=width*2/3]. cellX "trench" picks the deepest sea floor away from the poles. Each cell is 4 pixels."""
import gzip, struct, sys
import numpy as np
from PIL import Image

path, out = sys.argv[1], sys.argv[2]
with gzip.open(path) as f:
    fmt, w, h = struct.unpack(">iii", f.read(12))
    metres = np.frombuffer(f.read(4 * w * h), dtype=">f4").reshape(h, w).astype(float)
if sys.argv[3] == "trench":
    band = metres[h // 6: 5 * h // 6]
    y, x = np.unravel_index(np.argmin(band), band.shape)
    cx, cy = int(x), int(y) + h // 6
else:
    cx, cy = int(sys.argv[3]), int(sys.argv[4])
nw = int(sys.argv[5]) if len(sys.argv) > 5 else 192
nh = int(sys.argv[6]) if len(sys.argv) > 6 else nw * 2 // 3
ys = np.clip(np.arange(cy - nh // 2, cy + nh // 2), 0, h - 1)
xs = np.arange(cx - nw // 2, cx + nw // 2) % w
m = metres[np.ix_(ys, xs)]

# Bathymetric colours: pale shelf, mid-blue slopes and ridges, navy abyss, near-black trench; land green to brown to white
stops = [(-8000, (8, 10, 45)), (-6000, (20, 35, 100)), (-4500, (35, 75, 160)), (-3000, (70, 130, 205)), (-1000, (120, 180, 230)),
         (-200, (150, 205, 240)), (0, (175, 225, 245)), (1, (70, 140, 60)), (1000, (150, 160, 80)), (2500, (150, 110, 70)), (4500, (240, 240, 245))]
xs_, cols = [s[0] for s in stops], np.array([s[1] for s in stops], float)
rgb = np.stack([np.interp(m, xs_, cols[:, k]) for k in range(3)], -1)
# Light from the north-west over the relief, sea floor exaggerated so its ridges and trenches stand out
gy, gx = np.gradient(m / 30.0)
shade = np.clip(0.75 + 0.35 * (-gx - gy) / np.sqrt(1 + gx * gx + gy * gy), 0.35, 1.25)
img = Image.fromarray(np.clip(rgb * shade[..., None], 0, 255).astype(np.uint8)).resize((nw * 4, nh * 4), Image.NEAREST)
img.save(out)
print(out, img.size, "cells", cx, cy, "depth range", int(m.min()), int(m.max()))
