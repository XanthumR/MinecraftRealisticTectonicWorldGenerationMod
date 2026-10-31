"""Before/after renders side by side with labels, for showing the user a change: compare.py out.png "title" a.png "label a" b.png "label b" [...]."""
import sys
from PIL import Image, ImageDraw, ImageFont

out, title, pairs = sys.argv[1], sys.argv[2], sys.argv[3:]
images = [(Image.open(pairs[i]).convert("RGB"), pairs[i + 1]) for i in range(0, len(pairs), 2)]
h = max(im.height for im, _ in images)
images = [(im.resize((im.width * h // im.height, h)), label) for im, label in images]  # same height
try:
    font, small = ImageFont.truetype("arial.ttf", 40), ImageFont.truetype("arial.ttf", 30)
except OSError:
    font = small = ImageFont.load_default()
pad, top = 20, 110
sheet = Image.new("RGB", (sum(im.width for im, _ in images) + pad * (len(images) + 1), h + top + pad), (24, 26, 30))
d = ImageDraw.Draw(sheet)
d.text((pad, 15), title, fill=(240, 240, 240), font=font)
x = pad
for im, label in images:
    d.text((x, 65), label, fill=(200, 210, 220), font=small)
    sheet.paste(im, (x, top))
    x += im.width + pad
sheet.save(out)
print(out, sheet.size)
