"""Share of land in Köppen BW and BS on the planet exported to out/koppen/sim.f32 (any planet, no real map needed)."""
import numpy as np
exec(open('tools/koppen.py', encoding='utf-8').read().split("def main")[0])
w, h = map(int, open(f'{DIR}/sim.txt').read().split())
metres, mat, amp, rain, share = np.fromfile(f'{DIR}/sim.f32', '<f4').reshape(5, h, w)
lat = np.degrees(np.arcsin(np.clip(1 - 2 * (np.arange(h) + 0.5) / h, -1, 1))); month = np.arange(12)
T = mat[..., None] + amp[..., None] * np.cos(2 * np.pi * (month - np.where(lat >= 0, 6.5, 0.5)[:, None, None]) / 12)
summer = np.zeros(T.shape, bool); north = (lat >= 0)[:, None]
summer[..., 3:9] = north[..., None]; summer[..., [9, 10, 11, 0, 1, 2]] = ~north[..., None]
sim = classify(T, np.where(summer, (rain * share)[..., None] / 6, (rain * (1 - share))[..., None] / 6)); land = metres >= 0
print(f"BW {100 * np.isin(sim, [4, 5])[land].mean():.1f}%  BS {100 * np.isin(sim, [6, 7])[land].mean():.1f}%  hot desert BWh {100 * (sim == 4)[land].mean():.1f}%")
