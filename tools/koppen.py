"""Compare the Earth planet's simulated climate with the real Köppen-Geiger map (Beck et al. 2018, 0.5 deg, 1980-2016,
doi:10.1038/sdata.2018.214, figshare 6396959).

The simulation keeps a yearly mean temperature, half the summer-winter range, yearly rain and the share of it falling
in the warm half of the year. Twelve months are rebuilt from those (temperature a sine peaking in July in the north and
January in the south; rain spread evenly over each half-year) and classified with Beck's own rules (KoppenGeiger.m).
The real map is sampled at each planet cell's latitude and longitude. Prints agreement on land for the 5 main groups,
for 13 second-level classes and for all 30, the confusion of main groups, and writes out/koppen/compare.png (real
above, simulated below, Beck's colours) and out/koppen/groups.png (main groups, agree grey).
Run: jshell tools/climexport.jsh (class path "build/classes/java/main;build/resources/main"), then python tools/koppen.py
"""
import os, urllib.request, zipfile
import numpy as np
from PIL import Image

DIR = 'out/koppen'
NAMES = ['', 'Af', 'Am', 'Aw', 'BWh', 'BWk', 'BSh', 'BSk', 'Csa', 'Csb', 'Csc', 'Cwa', 'Cwb', 'Cwc', 'Cfa', 'Cfb', 'Cfc',
         'Dsa', 'Dsb', 'Dsc', 'Dsd', 'Dwa', 'Dwb', 'Dwc', 'Dwd', 'Dfa', 'Dfb', 'Dfc', 'Dfd', 'ET', 'EF']
COLORS = [(255, 255, 255), (0, 0, 255), (0, 120, 255), (70, 170, 250), (255, 0, 0), (255, 150, 150), (245, 165, 0), (255, 220, 100),
          (255, 255, 0), (200, 200, 0), (150, 150, 0), (150, 255, 150), (100, 200, 100), (50, 150, 50), (200, 255, 80), (100, 255, 80),
          (50, 200, 0), (255, 0, 255), (200, 0, 200), (150, 50, 150), (150, 100, 150), (170, 175, 255), (90, 120, 220), (75, 80, 180),
          (50, 0, 135), (0, 255, 255), (55, 200, 255), (0, 125, 125), (0, 70, 95), (178, 178, 178), (102, 102, 102)]


def real_map():
    tif = f'{DIR}/Beck_KG_V1_present_0p5.tif'
    if not os.path.exists(tif):
        zp = f'{DIR}/Beck_KG_V1.zip'
        if not os.path.exists(zp):
            urllib.request.urlretrieve('https://ndownloader.figshare.com/files/12407516', zp)
        zipfile.ZipFile(zp).extract('Beck_KG_V1_present_0p5.tif', DIR)
    return np.asarray(Image.open(tif))  # 360 x 720, from 90N and 180W


def classify(T, P):
    """Beck et al. 2018 (KoppenGeiger.m) on monthly T (..., 12) and P (..., 12); months 0 = January."""
    summer = np.zeros(T.shape, bool)
    north = T[..., 3:9].mean(-1) > T[..., [9, 10, 11, 0, 1, 2]].mean(-1)
    summer[..., 3:9] = north[..., None]
    summer[..., [9, 10, 11, 0, 1, 2]] = ~north[..., None]
    Pw = np.where(~summer, P, 0).sum(-1); Ps = np.where(summer, P, 0).sum(-1)
    Pdry = P.min(-1)
    Psdry = np.where(summer, P, np.inf).min(-1); Pswet = np.where(summer, P, -np.inf).max(-1)
    Pwdry = np.where(~summer, P, np.inf).min(-1); Pwwet = np.where(~summer, P, -np.inf).max(-1)
    MAT, MAP = T.mean(-1), P.sum(-1)
    Tmon10, Thot, Tcold = (T > 10).sum(-1), T.max(-1), T.min(-1)
    Pth = 2 * MAT + 14
    Pth = np.where(Pw * 2.333 > Ps, 2 * MAT, Pth)
    Pth = np.where(Ps * 2.333 > Pw, 2 * MAT + 28, Pth)
    out = np.zeros(T.shape[:-1], np.uint8)
    B = MAP < 10 * Pth
    BW = B & (MAP < 5 * Pth)
    out[BW & (MAT >= 18)] = 4; out[BW & (MAT < 18)] = 5
    out[B & ~BW & (MAT >= 18)] = 6; out[B & ~BW & (MAT < 18)] = 7
    A = (Tcold >= 18) & ~B
    Af = A & (Pdry >= 60); Am = A & ~Af & (Pdry >= 100 - MAP / 25)
    out[Af] = 1; out[Am] = 2; out[A & ~Af & ~Am] = 3
    for letter, base, cold in (('C', 8, (Tcold > 0) & (Tcold < 18)), ('D', 17, Tcold <= 0)):
        G = (Thot > 10) & cold & ~B
        s = G & (Psdry < 40) & (Psdry < Pwwet / 3)
        w = G & (Pwdry < Pswet / 10)
        both = s & w
        s = s & ~(both & (Ps <= Pw)); w = w & ~(both & (Ps > Pw))
        f = G & ~s & ~w
        for k, sub in enumerate((s, w, f)):
            b = base + (4 if letter == 'D' else 3) * k if letter == 'D' else base + 3 * k
            a = sub & (Thot >= 22)
            bb = sub & ~a & (Tmon10 >= 4)
            if letter == 'C':
                c = sub & ~a & ~bb & (Tmon10 >= 1)
                out[a], out[bb], out[c] = b, b + 1, b + 2
            else:
                d = sub & ~a & ~bb & (Tcold < -38)
                out[a], out[bb], out[sub & ~a & ~bb & ~d], out[d] = b, b + 1, b + 2, b + 3
    E = (Thot <= 10) & ~B
    out[E & (Thot > 0)] = 29; out[E & (Thot <= 0)] = 30
    return out


def main():
    w, h = map(int, open(f'{DIR}/sim.txt').read().split())
    g = np.fromfile(f'{DIR}/sim.f32', '<f4').reshape(5, h, w)
    metres, mat, amp, rain, share = g
    lat = np.degrees(np.arcsin(np.clip(1 - 2 * (np.arange(h) + 0.5) / h, -1, 1)))
    lon = -180 + (np.arange(w) + 0.5) * 360 / w
    month = np.arange(12)
    peak = np.where(lat >= 0, 6.5, 0.5)[:, None, None]  # mid-July or mid-January
    T = mat[..., None] + amp[..., None] * np.cos(2 * np.pi * (month - peak) / 12)
    summer = np.zeros(T.shape, bool)
    north = (lat >= 0)[:, None]
    summer[..., 3:9] = north[..., None]
    summer[..., [9, 10, 11, 0, 1, 2]] = ~north[..., None]
    P = np.where(summer, (rain * share)[..., None] / 6, (rain * (1 - share))[..., None] / 6)
    sim = classify(T, P)
    real_src = real_map()
    ri = np.clip(((90 - lat) / 0.5).astype(int), 0, 359)
    ci = np.clip(((lon + 180) / 0.5).astype(int), 0, 719)
    real = real_src[ri[:, None], ci[None, :]]
    land = (metres >= 0) & (real > 0)
    group = np.array([0, 1, 1, 1, 2, 2, 2, 2] + [3] * 9 + [4] * 12 + [5, 5])
    second = np.array([0, 1, 2, 3, 4, 4, 5, 5, 6, 6, 6, 7, 7, 7, 8, 8, 8, 9, 9, 9, 9, 10, 10, 10, 10, 11, 11, 11, 11, 12, 13])
    print(f'land cells compared: {land.sum()} ({w}x{h})')
    for name, m in (('main group (A B C D E)', group), ('second level (Af Am Aw BW BS Cs Cw Cf Ds Dw Df ET EF)', second), ('all 30 classes', np.arange(31))):
        print(f'{name}: {100 * (m[sim] == m[real])[land].mean():.1f}% agree')
    regions = {'India': (8, 32, 68, 90), 'Sahara': (16, 32, -15, 35), 'Amazon': (-15, 5, -75, -45), 'Eastern US': (28, 45, -95, -70),
               'Siberia': (50, 70, 60, 140), 'Australia': (-38, -12, 113, 154), 'Europe': (36, 60, -10, 30), 'East China': (22, 40, 105, 122)}
    LAT, LON = np.meshgrid(lat, lon, indexing='ij')
    print('main-group agreement by region: ' + ', '.join(
        f"{n} {100 * (group[sim] == group[real])[land & (LAT >= b[0]) & (LAT <= b[1]) & (LON >= b[2]) & (LON <= b[3])].mean():.0f}%" for n, b in regions.items()))
    print('\nmain groups, rows real, columns simulated (share of real land cells):')
    gs = 'ABCDE'
    print('      ' + ''.join(f'{c:>7}' for c in gs) + '   real share  sim share')
    for i in range(1, 6):
        row = [((group[real] == i) & (group[sim] == j) & land).sum() for j in range(1, 6)]
        tot = sum(row)
        print(f'  {gs[i - 1]}   ' + ''.join(f'{100 * r / max(tot, 1):6.0f}%' for r in row) + f'   {100 * tot / land.sum():8.1f}%  {100 * ((group[sim] == i) & land).sum() / land.sum():8.1f}%')
    pal = np.array(COLORS, np.uint8)
    img = np.full((h * 2 + 8, w, 3), 255, np.uint8)
    for k, cls in enumerate((real, sim)):
        part = pal[np.where(land, cls, 0)]
        part[metres < 0] = (40, 60, 100)
        img[k * (h + 8):k * (h + 8) + h] = part
    Image.fromarray(img).save(f'{DIR}/compare.png')
    gp = np.array([(255, 255, 255), (0, 90, 255), (240, 60, 40), (90, 200, 60), (40, 200, 220), (150, 150, 150)], np.uint8)
    diff = np.where(land & (group[sim] != group[real]), group[sim], 0)
    gimg = gp[diff]; gimg[land & (group[sim] == group[real])] = (215, 215, 215); gimg[metres < 0] = (40, 60, 100)
    Image.fromarray(gimg).save(f'{DIR}/groups.png')


if __name__ == '__main__':
    main()
