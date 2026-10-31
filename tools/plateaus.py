"""Plateaus on a planet: high land, flat over ~9 cells, with a steep edge within ~10 cells, away from volcanoes.
plateaus.py planet.bin [count=5]: prints "cellX cellY metres relief edge-drop" for the best spots, best first."""
import gzip, struct, sys
import numpy as np
from scipy.ndimage import uniform_filter, maximum_filter, minimum_filter

with gzip.open(sys.argv[1]) as f:
    fmt, w, h = struct.unpack(">iii", f.read(12))
    metres = np.frombuffer(f.read(4 * w * h), dtype=">f4").reshape(h, w).astype(float)
    f.read(4 * w * h * 4)  # lakes, flow, temperature, rain
    f.read(4 * w * h)  # receivers
    volcanoes = []
    for _ in range(struct.unpack(">i", f.read(4))[0]):
        u, v = struct.unpack(">ff", f.read(8)); f.read(1); f.read(4); f.read(4)
        if fmt >= 4: f.read(4)
        volcanoes.append((u, v))
count = int(sys.argv[2]) if len(sys.argv) > 2 else 5
mean = uniform_filter(metres, 9, mode=("nearest", "wrap"))
relief = np.sqrt(np.maximum(uniform_filter(metres ** 2, 9, mode=("nearest", "wrap")) - mean ** 2, 0))  # spread over 9x9 cells
edge = mean - minimum_filter(metres, 21, mode=("nearest", "wrap"))  # how far the land drops away within 10 cells
near = np.zeros((h, w), bool)
yy, xx = np.mgrid[0:h, 0:w]
for u, v in volcanoes:  # cones and their shields are not plateaus
    near |= (np.minimum(abs(xx - u), w - abs(xx - u)) ** 2 + (yy - v) ** 2) < 30 ** 2
score = np.where((mean > 1200) & ~near & (edge > 800), mean / (relief + 100) * np.minimum(edge, 2500), 0)
taken = np.zeros((h, w), bool)
for _ in range(count):
    s = np.where(taken, 0, score)
    y, x = np.unravel_index(np.argmax(s), s.shape)
    if s[y, x] <= 0: break
    print(x, y, int(mean[y, x]), int(relief[y, x]), int(edge[y, x]))
    taken |= (np.minimum(abs(xx - x), w - abs(xx - x)) ** 2 + (yy - y) ** 2) < 60 ** 2
