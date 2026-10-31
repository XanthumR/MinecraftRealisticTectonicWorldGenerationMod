"""Trailer thumbnail: out/thumb/thumbnail.png, 1280x720 (YouTube). A frame of a recorded clip (out/thumb/cand1.png: shot 1,
the sunset volcano, 4 s in) under the title over its sky, with the globe logo (tools/logo.py).
Run: python tools/thumbnail.py [background.png] [WxH out.png]   (Instagram post: 1080x1350 out/thumb/instagram.png;
portrait sizes put the title on two lines)
"""
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

W, H = map(int, sys.argv[2].split('x')) if len(sys.argv) > 2 else (1280, 720)
out = sys.argv[3] if len(sys.argv) > 3 else 'out/thumb/thumbnail.png'
tall = H > W
bg = Image.open(sys.argv[1] if len(sys.argv) > 1 else 'out/thumb/cand1.png').convert('RGB')
s = max(W / bg.width, H / bg.height)
bg = bg.resize((round(bg.width * s), round(bg.height * s)), Image.LANCZOS)
bg = bg.crop(((bg.width - W) // 2, (bg.height - H) // 2, (bg.width - W) // 2 + W, (bg.height - H) // 2 + H))
# a touch more contrast and warmth, darker at the top where the title sits
a = np.asarray(bg, np.float32) / 255
a = np.clip((a - 0.5) * 1.15 + 0.5, 0, 1) * np.array([1.04, 1.0, 0.94])
top = np.clip(1 - np.arange(H) / ((0.4 if tall else 0.45) * H), 0, 1)[:, None, None]
a = a * (1 - 0.55 * top)
img = Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8)).convert('RGBA')


def text(img, xy, s, size, fill, font='C:/Windows/Fonts/arialbd.ttf', glow=(255, 110, 20), stroke=6, anchor='mm'):
    f = ImageFont.truetype(font, size)
    halo = Image.new('RGBA', img.size)
    ImageDraw.Draw(halo).text(xy, s, font=f, fill=glow + (200,), anchor=anchor, stroke_width=stroke, stroke_fill=glow + (200,))
    img = Image.alpha_composite(img, halo.filter(ImageFilter.GaussianBlur(14)))  # lava glow behind the letters
    layer = Image.new('RGBA', img.size)
    ImageDraw.Draw(layer).text(xy, s, font=f, fill=fill, anchor=anchor, stroke_width=4, stroke_fill=(25, 12, 6, 255))
    return Image.alpha_composite(img, layer)


if tall:
    img = text(img, (W // 2, 120), 'REALISTIC', 150, (255, 244, 225, 255))
    img = text(img, (W // 2, 265), 'TECTONICS', 150, (255, 244, 225, 255))
    img = text(img, (W // 2, 375), 'A PLANET, SIMULATED', 58, (255, 196, 120, 255), glow=(0, 0, 0), stroke=4)
else:
    img = text(img, (W // 2, 100), 'REALISTIC TECTONICS', 104, (255, 244, 225, 255))
    img = text(img, (W // 2, 188), 'A PLANET, SIMULATED', 46, (255, 196, 120, 255), glow=(0, 0, 0), stroke=4)
g = 230 if tall else 190
globe = Image.open('out/logo/logo_512.png').convert('RGBA').resize((g, g), Image.LANCZOS)
mask = Image.new('L', globe.size, 0); ImageDraw.Draw(mask).ellipse((g // 19, g // 19, g - g // 19, g - g // 19), fill=255)
img.paste(globe, (W - g - 25, H - g - 25), mask.filter(ImageFilter.GaussianBlur(3)))
img.convert('RGB').save(out)
print(out)
