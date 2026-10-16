"""Builds the Earth data the "Earth" planet starts from, into src/main/resources/realtectonics/earth/:
  elevation.i16.gz  2048 x 1024 metres (big-endian int16, row-major), in the planet's own projection: columns linear
                    in longitude from -180, rows equal-area (latitude = asin(1 - 2 (row + 0.5) / 1024)), as
                    Climate.latitude, so the planet's rows need no reprojection; widths 512-2048 divide it, 4096 doubles it.
  plates.u8.gz      the same grid: index of the PB2002 plate (Bird 2003) under each cell, 255 for none.
  plates.txt        plate codes and names, one per line, in index order.
  lakes.u8.gz       the same grid: 1 where Natural Earth (1:50m) has a lake, else 0.
  steps.f32.gz      PB2002 boundary steps: per step 6 big-endian floats: start lon, start lat, end lon, end lat,
                    class (index into CLASSES), divergence mm/yr (negative: converging), right-lateral slip mm/yr.
Sources: elevation from the public AWS terrarium tiles (Mapzen/Tilezen) at zoom 4; lakes from Natural Earth (public
domain, github.com/nvkelso/natural-earth-vector); plates and steps from the PB2002
GeoJSON of github.com/fraxen/tectonicplates (CC BY-SA, after Bird 2003, G-cubed 4(3) 1027).
Run from the project root: python tools/earthdata.py  (downloads to out/earth/, cached).
"""
import gzip, io, json, math, os, struct, urllib.request
import numpy as np
from PIL import Image, ImageDraw

W, H, ZOOM = 2048, 1024, 4
CACHE = 'out/earth'
OUT = 'src/main/resources/realtectonics/earth'
CLASSES = ['OSR', 'OTF', 'SUB', 'CRB', 'CTF', 'CCB', 'OCB']  # ridge, ocean transform, subduction, continental rift, continental transform, continental and oceanic convergence
GEO = 'https://raw.githubusercontent.com/fraxen/tectonicplates/master/GeoJSON/'


def fetch(url, path):
    if not os.path.exists(path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with urllib.request.urlopen(url, timeout=120) as r:
            open(path, 'wb').write(r.read())
    return path


def lat_of_row(r):
    return np.degrees(np.arcsin(np.clip(1 - 2 * (r + 0.5) / H, -1, 1)))


def row_of_lat(lat):
    return (1 - np.sin(np.radians(lat))) * H / 2 - 0.5


def elevation():
    n = 2 ** ZOOM
    mosaic = np.zeros((n * 256, n * 256), dtype=np.float32)
    for ty in range(n):
        for tx in range(n):
            p = fetch(f'https://s3.amazonaws.com/elevation-tiles-prod/terrarium/{ZOOM}/{tx}/{ty}.png', f'{CACHE}/tiles/{ZOOM}_{tx}_{ty}.png')
            a = np.asarray(Image.open(p).convert('RGB'), dtype=np.float32)
            mosaic[ty * 256:(ty + 1) * 256, tx * 256:(tx + 1) * 256] = a[..., 0] * 256 + a[..., 1] + a[..., 2] / 256 - 32768
    size = n * 256
    lons = -180 + (np.arange(W) + 0.5) * 360 / W
    lats = lat_of_row(np.arange(H))
    out = np.zeros((H, W), dtype=np.float32)
    # Each output cell averages a 3x3 spread of samples inside it, so ridges and fjords are not aliased away.
    for sy in (-1 / 3, 0, 1 / 3):
        for sx in (-1 / 3, 0, 1 / 3):
            la = np.clip(lat_of_row(np.arange(H) + sy), -85.0, 85.0)
            lo = lons + sx * 360 / W
            px = ((lo + 180) / 360 * size) % size
            py = (1 - np.log(np.tan(np.radians(la)) + 1 / np.cos(np.radians(la))) / math.pi) / 2 * size
            out += mosaic[np.clip(py.astype(int), 0, size - 1)[:, None], px.astype(int)[None, :] % size] / 9
    # Past 85 degrees the tiles stop: the Arctic is ocean, the Antarctic plateau goes on as its last row.
    north, south = lats > 85, lats < -85
    out[north] = -4000
    last = np.where(lats >= -85)[0].max()
    out[south] = out[last]
    return np.round(out).astype('>i2')


def plates():
    data = json.load(open(fetch(GEO + 'PB2002_plates.json', f'{CACHE}/PB2002_plates.json')))
    img = Image.new('L', (W, H), 255)
    draw = ImageDraw.Draw(img)
    names = []
    # Small plates last, so a plate inside a bigger one's ring is not painted over.
    feats = sorted(data['features'], key=lambda f: -sum(len(r[0]) if f['geometry']['type'] == 'MultiPolygon' else len(r) for r in f['geometry']['coordinates']))
    for f in feats:
        idx = len(names)
        names.append(f"{f['properties']['Code']} {f['properties']['PlateName']}")
        polys = f['geometry']['coordinates'] if f['geometry']['type'] == 'MultiPolygon' else [f['geometry']['coordinates']]
        for poly in polys:
            ring = poly[0]
            lons, lats = [ring[0][0]], [ring[0][1]]
            for lo, la in ring[1:]:  # unwrap: no jump of more than half the planet between neighbours
                while lo - lons[-1] > 180: lo -= 360
                while lo - lons[-1] < -180: lo += 360
                lons.append(lo); lats.append(la)
            if abs(lons[-1] - lons[0]) > 180:  # a ring round a pole: close it along the pole
                pole = 90 if np.mean(lats) > 0 else -90
                lons += [lons[-1], lons[0]]; lats += [pole, pole]
            xs = (np.array(lons) + 180) / 360 * W - 0.5
            ys = row_of_lat(np.array(lats))
            for shift in (-W, 0, W):
                draw.polygon(list(zip(xs + shift, ys)), fill=idx)
    return np.asarray(img, dtype=np.uint8), names


def lakes():
    data = json.load(open(fetch('https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_50m_lakes.geojson',
                                f'{CACHE}/ne_50m_lakes.geojson'), encoding='utf-8'))
    img = Image.new('L', (W, H), 0)
    draw = ImageDraw.Draw(img)
    for f in data['features']:
        ring = f['geometry']['coordinates'][0]
        xs = (np.array([c[0] for c in ring]) + 180) / 360 * W - 0.5
        ys = row_of_lat(np.array([c[1] for c in ring]))
        draw.polygon(list(zip(xs, ys)), fill=1)
    return np.asarray(img, dtype=np.uint8)


def steps():
    data = json.load(open(fetch(GEO + 'PB2002_steps.json', f'{CACHE}/PB2002_steps.json')))
    rows = []
    for f in data['features']:
        p = f['properties']
        if p['STEPCLASS'] not in CLASSES: continue
        rows.append([p['STARTLONG'], p['STARTLAT'], p['FINALLONG'], p['FINALLAT'], CLASSES.index(p['STEPCLASS']), p['VELOCITYDI'], p['VELOCITYRI']])
    return np.array(rows, dtype='>f4')


def main():
    os.makedirs(OUT, exist_ok=True)
    e = elevation()
    gzip.open(f'{OUT}/elevation.i16.gz', 'wb').write(e.tobytes())
    pl, names = plates()
    gzip.open(f'{OUT}/plates.u8.gz', 'wb').write(pl.tobytes())
    open(f'{OUT}/plates.txt', 'w', encoding='utf-8').write('\n'.join(names) + '\n')
    lk = lakes()
    gzip.open(f'{OUT}/lakes.u8.gz', 'wb').write(lk.tobytes())
    s = steps()
    gzip.open(f'{OUT}/steps.f32.gz', 'wb').write(s.tobytes())
    print(f'elevation {e.min()}..{e.max()} m, land {100 * (e > 0).mean():.1f}%; lakes {100 * lk.mean():.2f}%; plates {len(names)}, unpainted {100 * (pl == 255).mean():.2f}%; steps {len(s)}')
    # A look: land grey by height, sea blue, plate edges black.
    land = np.clip(e.astype(np.float32) / 5000, 0, 1)
    rgb = np.stack([60 + 150 * land, 90 + 120 * land, 160 - 60 * land], -1)
    rgb[e <= 0] = [40, 70, 150]
    edge = (pl != np.roll(pl, 1, 1)) | (pl != np.roll(pl, 1, 0))
    rgb[edge] = [0, 0, 0]
    Image.fromarray(rgb.astype(np.uint8)).resize((2048, 1024)).save(f'{CACHE}/earth.png')


if __name__ == '__main__':
    main()
