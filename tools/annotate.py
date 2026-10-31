"""Labels with arrows on a render: annotate.py in.png out.png "title" spots.txt, spots.txt lines "label x y" (from horstspots.jsh)."""
import math, sys
from PIL import Image, ImageDraw, ImageFont

src, out, title, spots = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]
im = Image.open(src).convert("RGB")
pad = 90
sheet = Image.new("RGB", (im.width, im.height + pad), (24, 26, 30))
sheet.paste(im, (0, pad))
d = ImageDraw.Draw(sheet)
try:
    font, big = ImageFont.truetype("arialbd.ttf", 44), ImageFont.truetype("arial.ttf", 40)
except OSError:
    font = big = ImageFont.load_default()
d.text((20, 22), title, fill=(240, 240, 240), font=big)
colours = {"horst": (220, 40, 40), "graben": (30, 90, 220), "scarp": (250, 170, 0)}
names = {"horst": "HORST", "graben": "GRABEN", "scarp": "FAULT SCARP"}
points = sorted((l.split() for l in open(spots).read().split("\n") if l.strip()), key=lambda p: int(p[1]))
for i, (kind, x, y) in enumerate(points):
    x, y = int(x), int(y) + pad
    # Labels in a row in the sky over the spots, in their left-to-right order, so the arrows never cross
    mid = sum(int(p[1]) for p in points) / len(points)
    lx = min(max(mid + (i - (len(points) - 1) / 2) * 470, 260), im.width - 260)
    ly = pad + 70
    text = names[kind]
    w = d.textlength(text, font=font)
    box = (lx - w / 2 - 14, ly - 34, lx + w / 2 + 14, ly + 30)
    d.rounded_rectangle(box, 10, fill=(255, 255, 255), outline=colours[kind], width=5)
    d.text((lx - w / 2, ly - 26), text, fill=colours[kind], font=font)
    # Arrow from the label's edge to the spot, head at the spot
    sx, sy = lx, ly + 30
    d.line((sx, sy, x, y), fill=colours[kind], width=8)
    a = math.atan2(y - sy, x - sx)
    head = [(x, y), (x - 40 * math.cos(a - 0.4), y - 40 * math.sin(a - 0.4)), (x - 40 * math.cos(a + 0.4), y - 40 * math.sin(a + 0.4))]
    d.polygon(head, fill=colours[kind])
sheet.save(out)
print(out, sheet.size)
