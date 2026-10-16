"""Per-region dry-climate shares, simulated vs real (run after koppen.py's inputs exist): BW desert, BS steppe."""
import numpy as np, sys
sys.argv = sys.argv[:1]
exec(open('tools/koppen.py', encoding='utf-8').read().split("def main")[0])
w, h = map(int, open(f'{DIR}/sim.txt').read().split())
metres, mat, amp, rain, share = np.fromfile(f'{DIR}/sim.f32', '<f4').reshape(5, h, w)
lat = np.degrees(np.arcsin(np.clip(1 - 2 * (np.arange(h) + 0.5) / h, -1, 1))); lon = -180 + (np.arange(w) + 0.5) * 360 / w
month = np.arange(12); peak = np.where(lat >= 0, 6.5, 0.5)[:, None, None]
T = mat[..., None] + amp[..., None] * np.cos(2 * np.pi * (month - peak) / 12)
summer = np.zeros(T.shape, bool); north = (lat >= 0)[:, None]
summer[..., 3:9] = north[..., None]; summer[..., [9, 10, 11, 0, 1, 2]] = ~north[..., None]
P = np.where(summer, (rain * share)[..., None] / 6, (rain * (1 - share))[..., None] / 6)
sim = classify(T, P); src = real_map()
real = src[np.clip(((90 - lat) / 0.5).astype(int), 0, 359)[:, None], np.clip(((lon + 180) / 0.5).astype(int), 0, 719)[None, :]]
land = (metres >= 0) & (real > 0); LAT, LON = np.meshgrid(lat, lon, indexing='ij')
regions = {'India': (8, 32, 68, 90), 'N India plain': (22, 30, 75, 88), 'Deccan': (12, 22, 73, 82), 'S America': (-56, 12, -82, -34),
           'Brazil east': (-25, -3, -50, -35), 'Argentina': (-40, -22, -68, -57), 'Nile Egypt': (22, 31, 30, 33), 'Arabia': (12, 32, 35, 60), 'Chaco': (-32, -18, -66, -56), 'Sahel': (10, 18, -15, 35),
           'Australia': (-38, -12, 113, 154), 'SW US+Mexico': (20, 37, -118, -95), 'Puna/Atacama': (-28, -18, -71, -65)}
for n, (a, b, c, d) in regions.items():
    m = land & (LAT >= a) & (LAT <= b) & (LON >= c) & (LON <= d)
    f = lambda cls, ks: 100 * np.isin(cls, ks)[m].mean()
    print(f"{n:14s} BW sim {f(sim,[4,5]):3.0f}% real {f(real,[4,5]):3.0f}% | BS sim {f(sim,[6,7]):3.0f}% real {f(real,[6,7]):3.0f}% | rain sim {np.median(rain[m]):5.0f}mm")
