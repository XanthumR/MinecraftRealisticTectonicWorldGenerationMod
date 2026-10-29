"""Mod logo: the trailer planet (seed 4, out/trailer/sim from trailer_plates.jsh + its plate edges) as a globe whose plate
boundaries glow like fresh lava, on dark space. Writes out/logo/logo_512.png and logo_400.png (CurseForge wants >= 400 square).
Run: python tools/logo.py [lon0 lat0]   (the globe's centre, as fractions of the map width and degrees)
"""
import os, sys
import numpy as np
from PIL import Image, ImageFilter

N = 1024  # drawn at 2x, scaled down for smooth edges
lon0 = float(sys.argv[1]) if len(sys.argv) > 1 else 0.62
lat0 = np.radians(float(sys.argv[2]) if len(sys.argv) > 2 else 22)
tex = np.asarray(Image.open('out/trailer/sim/final_map.png').convert('RGB'), np.float32) / 255
edge = Image.open('out/trailer/sim/plate_edges.png').convert('L')
edge = np.asarray(edge.filter(ImageFilter.MaxFilter(3)), np.float32) / 255
th, tw = tex.shape[:2]

r = 0.40 * N
y, x = np.mgrid[0:N, 0:N].astype(np.float32)
px, py = (x - N / 2) / r, (N / 2 - y) / r
d2 = px * px + py * py
on = d2 < 1
pz = np.sqrt(np.clip(1 - d2, 0, 1))
# orthographic: rotate the view point (lon0, lat0) to the centre
lat = np.arcsin(np.clip(py * np.cos(lat0) + pz * np.sin(lat0), -1, 1))
lon = np.arctan2(px, pz * np.cos(lat0) - py * np.sin(lat0))
u = ((lon / (2 * np.pi) + lon0) % 1) * (tw - 1)
v = (1 - (np.sin(lat) + 1) / 2) * (th - 1)  # the planet's map is equal-area: rows even in sin(latitude)
ui, vi = u.astype(int), v.astype(int)
col = tex[vi, ui]
glow = edge[vi, ui]

light = np.array([-0.55, 0.6, 0.58]); light /= np.linalg.norm(light)
shade = np.clip(px * light[0] + py * light[1] + pz * light[2], 0, 1)
col = col * (0.18 + 0.95 * shade[..., None]) * (0.75 + 0.25 * pz[..., None])  # sun from the upper left, darker limb
lava = np.array([1.0, 0.45, 0.08])
col = col * (1 - 0.85 * glow[..., None]) + lava * glow[..., None] * (0.55 + 0.6 * shade[..., None])

# background: deep space with a faint blue atmosphere halo round the globe
rr = np.sqrt(d2)
bg = np.stack([0.02 + 0.03 * (1 - y / N), 0.03 + 0.04 * (1 - y / N), 0.07 + 0.06 * (1 - y / N)], -1)
halo = np.exp(-np.clip(rr - 1, 0, None) / 0.06) * (~on)
bg = bg + halo[..., None] * np.array([0.25, 0.5, 0.95]) * 0.8
rim = np.exp(-(1 - np.clip(rr, 0, 1)) / 0.03) * on  # thin bright atmosphere at the edge of the disc
img = np.where(on[..., None], col + rim[..., None] * np.array([0.2, 0.45, 0.9]) * 0.6, bg)

# lava glow bleeds a little beyond the lines
gl = Image.fromarray((np.clip(glow * on * (0.4 + shade), 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(6))
img = img + (np.asarray(gl, np.float32)[..., None] / 255) * lava * 0.6

out = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
os.makedirs('out/logo', exist_ok=True)
for s in (512, 400):
    out.resize((s, s), Image.LANCZOS).save(f'out/logo/logo_{s}.png')
print('out/logo/logo_512.png, logo_400.png')
