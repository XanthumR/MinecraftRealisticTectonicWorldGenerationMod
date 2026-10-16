"""Compare the game's mountains with real ones.

Real elevations come from the public AWS terrarium tiles (Mapzen/Tilezen, no key). Each range is a box the size of the
game's boxes (tools/gamedem.jsh writes out/dem/game_*.f32 and game.txt), resampled to the game's block width, so both
are measured on the same grid. Prints a table and writes out/dem/compare.png (hillshades side by side).
Run from the project root: python tools/realdem.py
"""
import io, math, os, urllib.request
import numpy as np
from PIL import Image

OUT = 'out/dem'
RANGES = {  # name: (lat centre, lon centre); boxes are cut to the game box size around these
    'Alps': (46.4, 9.0),
    'Himalaya (Nepal)': (28.2, 85.5),
    'Andes (Bolivia)': (-17.5, -68.0),
    'Rockies (Colorado)': (39.2, -106.3),
    'Appalachians (old)': (35.9, -82.8),
}
ZOOM = 9  # terrarium zoom: ~300 m pixels at mid latitudes, finer than a game block


def tile(z, x, y):
    path = f'{OUT}/tiles/{z}_{x}_{y}.png'
    if not os.path.exists(path):
        os.makedirs(f'{OUT}/tiles', exist_ok=True)
        with urllib.request.urlopen(f'https://s3.amazonaws.com/elevation-tiles-prod/terrarium/{z}/{x}/{y}.png', timeout=60) as r:
            open(path, 'wb').write(r.read())
    a = np.asarray(Image.open(path).convert('RGB'), dtype=np.float64)
    return a[..., 0] * 256 + a[..., 1] + a[..., 2] / 256 - 32768


def real_box(lat, lon, w_km, h_km, km):
    """Elevation in metres on a w_km x h_km box around (lat, lon), sampled every km kilometres."""
    n, m = int(round(w_km / km)), int(round(h_km / km))
    xs = (np.arange(n) - n / 2 + 0.5) * km
    ys = (np.arange(m) - m / 2 + 0.5) * km
    lats = lat - ys / 111.32
    lons = lon + xs[None, :] / (111.32 * np.cos(np.radians(lats[:, None])))
    scale = 2 ** ZOOM
    px = (lons + 180) / 360 * scale * 256
    py = (1 - np.log(np.tan(np.radians(lats[:, None])) + 1 / np.cos(np.radians(lats[:, None]))) / math.pi) / 2 * scale * 256
    py = np.broadcast_to(py, px.shape)
    tx, ty = (px // 256).astype(int), (py // 256).astype(int)
    out = np.zeros(px.shape)
    cache = {}
    for (a, b) in set(zip(tx.ravel(), ty.ravel())):
        cache[(a, b)] = tile(ZOOM, a, b)
    # average the ~300 m pixels inside each sample instead of point-sampling, as the game's cells are smooth
    for j in range(m):
        for i in range(n):
            t = cache[(tx[j, i], ty[j, i])]
            out[j, i] = t[int(py[j, i]) % 256, int(px[j, i]) % 256]
    return out


def metrics(z, km):
    land = z > 0
    gy, gx = np.gradient(z, km * 1000)
    slope = np.degrees(np.arctan(np.hypot(gx, gy)))
    def relief(win_km):
        r = max(1, int(round(win_km / km / 2)))
        from numpy.lib.stride_tricks import sliding_window_view as sw
        p = np.pad(z, r, mode='edge')
        v = sw(p, (2 * r + 1, 2 * r + 1))
        return (v.max(axis=(2, 3)) - v.min(axis=(2, 3)))[land].mean()
    # radial power spectrum slope over wavelengths 5-100 km: how roughness is shared between big and small features
    f = np.abs(np.fft.fftshift(np.fft.fft2(z - z.mean()))) ** 2
    ky = np.fft.fftshift(np.fft.fftfreq(z.shape[0], km)); kx = np.fft.fftshift(np.fft.fftfreq(z.shape[1], km))
    kk = np.hypot(*np.meshgrid(kx, ky))
    sel = (kk > 1 / 100) & (kk < 1 / 5)
    beta = -np.polyfit(np.log(kk[sel]), np.log(f[sel]), 1)[0]
    return {
        'median m': np.median(z[land]), 'p95 m': np.percentile(z[land], 95), 'max m': z.max(),
        'mean slope': slope[land].mean(), 'p90 slope': np.percentile(slope[land], 90),
        'relief 10km': relief(10), 'relief 50km': relief(50), 'spectrum': beta,
    }


def hillshade(z, km):
    gy, gx = np.gradient(z, km * 1000)
    sh = np.clip(0.6 + (gx - gy) * 3, 0, 1)
    col = np.clip((z - 0) / 6000, 0, 1)
    rgb = np.stack([0.35 + 0.6 * col, 0.55 + 0.35 * col, 0.3 + 0.6 * col], -1) * sh[..., None]
    rgb[z <= 0] = [0.2, 0.35, 0.6]
    return Image.fromarray((np.clip(rgb, 0, 1) * 255).astype(np.uint8))


def main():
    games = []
    for line in open(f'{OUT}/game.txt'):
        name, n, m, km = line.split()[:4]
        n, m, km = int(n), int(m), float(km)
        games.append((name, np.fromfile(f'{OUT}/{name}.f32', dtype='<f4').reshape(m, n).astype(np.float64), km))
    km, (m, n) = games[0][2], games[0][1].shape
    rows, images = [], []
    for name, z, _ in games:
        rows.append((name, metrics(z, km))); images.append((name, z))
    for name, (lat, lon) in RANGES.items():
        z = real_box(lat, lon, n * km, m * km, km)
        rows.append((name, metrics(z, km))); images.append((name, z))
    keys = list(rows[0][1].keys())
    print(f'{"":22}' + ''.join(f'{k:>12}' for k in keys))
    for name, r in rows:
        print(f'{name:22}' + ''.join(f'{r[k]:12.1f}' for k in keys))
    w, h = images[0][1].shape[1], images[0][1].shape[0]
    sheet = Image.new('RGB', (w * 3, h * ((len(images) + 2) // 3)), 'white')
    for i, (name, z) in enumerate(images):
        sheet.paste(hillshade(z, km), ((i % 3) * w, (i // 3) * h))
    sheet.save(f'{OUT}/compare.png')
    print(f'{km:.2f} km per sample, boxes {n * km:.0f} x {m * km:.0f} km; images in order: ' + ', '.join(n for n, _ in images))


if __name__ == '__main__':
    main()
