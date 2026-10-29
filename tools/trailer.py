"""Cuts the mod trailer: out/trailer/trailer.mp4, 1920x1080 at 60 fps, ~60 s.

Sim part from tools/trailer_plates.jsh (out/trailer/sim), in-game part from the user's OBS clips out/trailer/clips/shotNN.mp4
(SHOTS below, the shot list in out/trailer/SHOTS.md); a missing clip falls back to its scout render (trailer_scout.jsh +
uview3d.jsh) panned slowly, so the cut can be timed before anything is recorded. Music: out/trailer/music.* if present, faded
out at the end; with --bpm N every shot lasts SHOT_BEATS beats, and with --beat T (seconds of any beat in the track) the
music is trimmed so a beat falls on the first shot: cuts land on the beat. This track: --bpm 146 --beat 0.022.
Every frame is drawn here with PIL and piped to the ffmpeg that imageio-ffmpeg ships (pip install imageio-ffmpeg).
Run: python tools/trailer.py [--bpm 146 --beat 0.022] [--preview] [--reel]   (--preview: 960x540 at 30 fps, for checking
timing fast; --reel: Instagram Reel, 1080x1920 at 30 fps to trailer_reel.mp4: clips cropped to their middle, the planet
panned, captions above the app's buttons)
"""
import argparse, glob, os, subprocess
import imageio_ffmpeg
from PIL import Image, ImageDraw, ImageFont, ImageEnhance, ImageFilter

OUT = 'out/trailer'
SHOT_SECONDS, SHOT_BEATS = 4.5, 12
SHOTS = [  # (clip number, caption, scout render used until the clip exists)
    (1, 'Volcanoes, born from the plates', 'volcano'),
    (2, 'Calderas that collapsed and filled with lakes', 'caldera'),
    (3, 'Glaciers that flow down their valleys', 'glacier'),
    (4, 'Mountains raised where plates collide', 'fold'),
    (5, 'Rivers that carve their way to the sea', 'mouth'),
    (6, 'Climate decides every biome', 'desert'),
    (7, 'Caves and cave biomes below', None),
    (8, 'One whole planet, to the horizon', None),
]

ap = argparse.ArgumentParser()
ap.add_argument('--bpm', type=float)
ap.add_argument('--beat', type=float, default=0.0)
ap.add_argument('--preview', action='store_true')
ap.add_argument('--reel', action='store_true')
a = ap.parse_args()
W, H, FPS = (540, 960, 30) if a.preview and a.reel else (1080, 1920, 30) if a.reel else (960, 540, 30) if a.preview else (1920, 1080, 60)
UNIT = min(W, H) / 1080  # font pixels per design pixel
CAP_Y = 0.70 if a.reel else 0.86  # a Reel's bottom fifth is under the app's caption and buttons
shot_s = SHOT_BEATS * 60 / a.bpm if a.bpm else SHOT_SECONDS


def font(px, bold=False):
    return ImageFont.truetype('C:/Windows/Fonts/' + ('segoeuib.ttf' if bold else 'segoeui.ttf'), int(px * UNIT))


def fit(im, zoom=1.0, cx=0.5, cy=0.5):
    """im scaled to cover the frame (times zoom), centred on (cx, cy) of the image."""
    s = max(W / im.width, H / im.height) * zoom
    w, h = int(im.width * s), int(im.height * s)
    big = im.resize((w, h), Image.BILINEAR)
    x = min(max(int(cx * w - W / 2), 0), w - W); y = min(max(int(cy * h - H / 2), 0), h - H)
    return big.crop((x, y, x + W, y + H))


def caption(im, text, alpha, size=54, y=None, bold=False):
    if alpha <= 0: return im
    layer = Image.new('RGBA', im.size)
    if a.reel: size *= 1.4  # read on a phone held upright
    d = ImageDraw.Draw(layer); f = font(size, bold)
    lines = ['']
    for word in text.split():  # wrapped to 90% of the frame's width
        lines[-1:] = [f'{lines[-1]} {word}'.strip()] if d.textlength(f'{lines[-1]} {word}', font=f) <= 0.9 * W or not lines[-1] else [lines[-1], word]
    step = 1.25 * size * UNIT
    yy = H * (CAP_Y if y is None else y) - step * len(lines) / 2
    if a.reel:  # a soft dark band behind, as bright snow or sand under white text is unreadable
        band = Image.new('RGBA', im.size); ImageDraw.Draw(band).rectangle((0, yy - 0.3 * step, W, yy + step * (len(lines) + 0.1)), fill=(0, 0, 0, int(110 * alpha)))
        im = Image.alpha_composite(im.convert('RGBA'), band.filter(ImageFilter.GaussianBlur(step / 3)))
    for line in lines:
        x = (W - d.textlength(line, font=f)) / 2
        d.text((x + 3, yy + 3), line, font=f, fill=(0, 0, 0, int(160 * alpha)))
        d.text((x, yy), line, font=f, fill=(255, 255, 255, int(255 * alpha)))
        yy += step
    return Image.alpha_composite(im.convert('RGBA'), layer).convert('RGB')


def fade(t, start, end, edge=0.5):
    """0 outside [start, end], easing to 1 over edge seconds inside it."""
    if t < start or t > end: return 0.0
    return min(1.0, (t - start) / edge, (end - t) / edge)


def blend(x, y, k):
    return Image.blend(x, y, max(0.0, min(1.0, k)))


class Clip:
    """A recorded clip, decoded at the trailer's size and rate from its middle (the ends hold the start and stop of the
    recording); it holds its last frame if it runs short."""
    def __init__(self, path, seconds):
        # length from the header (no decoding); stdin closed, or ffmpeg on Windows can sit waiting on the inherited one
        info = subprocess.run([imageio_ffmpeg.get_ffmpeg_exe(), '-hide_banner', '-i', path], stdin=subprocess.DEVNULL, capture_output=True, text=True).stderr
        h, m, sec = info.split('Duration: ')[1].split(',')[0].split(':'); dur = int(h) * 3600 + int(m) * 60 + float(sec)
        self.gen = imageio_ffmpeg.read_frames(path, input_params=['-ss', f'{max(0.0, (dur - seconds) / 2):.2f}'], output_params=['-vf', f'fps={FPS},scale={W}:{H}:force_original_aspect_ratio=increase,crop={W}:{H}'])
        meta = next(self.gen); self.size = meta['size']; self.last = None
    def frame(self):
        try: self.last = Image.frombytes('RGB', self.size, next(self.gen))
        except StopIteration: pass
        return self.last


sim = sorted(glob.glob(f'{OUT}/sim/plates_*.png'))
final = Image.open(f'{OUT}/sim/final_map.png').convert('RGB')
music = (glob.glob(f'{OUT}/music.*') or [None])[0]

# The timeline, in seconds.
T_INTRO, T_DRIFT, T_MAP = 2.5, 8.0, 4.5
t_shots = T_INTRO + T_DRIFT + T_MAP
t_end = t_shots + shot_s * len(SHOTS)
total = t_end + 6.0

cmd = [imageio_ffmpeg.get_ffmpeg_exe(), '-y', '-loglevel', 'error', '-f', 'rawvideo', '-pix_fmt', 'rgb24', '-s', f'{W}x{H}', '-r', str(FPS), '-i', '-']
if music: cmd += ['-ss', f'{(a.beat - t_shots) % (60 / a.bpm) if a.bpm else 0:.3f}', '-i', music, '-af', f'afade=t=out:st={total - 2.5:.2f}:d=2.5', '-shortest', '-c:a', 'aac', '-b:a', '192k']
cmd += ['-c:v', 'libx264', '-pix_fmt', 'yuv420p', '-crf', '18', '-preset', 'medium', f'{OUT}/trailer{"_reel" if a.reel else ""}{"_preview" if a.preview else ""}.mp4']
enc = subprocess.Popen(cmd, stdin=subprocess.PIPE)

clips, scouts, black = {}, {}, Image.new('RGB', (W, H))
for i in range(int(total * FPS)):
    t = i / FPS
    if t < T_INTRO + T_DRIFT:  # the plates drift: sim frames blended between, a slow push in
        k = max(0.0, (t - T_INTRO) / T_DRIFT) * (len(sim) - 1); j = min(int(k), len(sim) - 2)
        im = blend(Image.open(sim[j]).convert('RGB'), Image.open(sim[j + 1]).convert('RGB'), k - j)
        im = fit(im, 1.0 + 0.15 * t / (T_INTRO + T_DRIFT), 0.35 + 0.3 * t / (T_INTRO + T_DRIFT) if a.reel else 0.5)
        if t < T_INTRO: im = blend(black, ImageEnhance.Brightness(im).enhance(0.5), t / T_INTRO)
        im = caption(im, 'A planet, simulated', fade(t, 0.3, T_INTRO + 1.5), 84, 0.5, True)
        im = caption(im, 'Plates drift, collide and rift apart', fade(t, T_INTRO + 1.5, T_INTRO + 5))
        im = caption(im, 'Mountains rise where they meet', fade(t, T_INTRO + 5, T_INTRO + T_DRIFT))
    elif t < t_shots:  # the finished planet: rivers, climate and biomes fade in over the bare plates
        u = (t - T_INTRO - T_DRIFT) / T_MAP
        cx = 0.65 if a.reel else 0.5
        im = blend(fit(Image.open(sim[-1]).convert('RGB'), 1.15 + 0.1 * u, cx), fit(final, 1.15 + 0.1 * u, cx), u * 2)
        im = caption(im, 'Rivers carve the land. Climate decides the biomes.', fade(t, T_INTRO + T_DRIFT + 0.3, t_shots))
    elif t < t_end:  # the shots, each from its clip or, until recorded, its scout render
        n = int((t - t_shots) / shot_s); u = (t - t_shots - n * shot_s) / shot_s
        num, text, scout = SHOTS[n]
        path = f'{OUT}/clips/shot{num:02d}.mp4'
        if os.path.exists(path):
            if n not in clips:
                for k in list(clips): clips.pop(k).gen.close()  # stop the last shot's decoder
                clips[n] = Clip(path, shot_s)
            im = clips[n].frame()
        elif scout and os.path.exists(f'{OUT}/scout_{scout}.png'):
            src = scouts.setdefault(scout, Image.open(f'{OUT}/scout_{scout}.png').convert('RGB'))
            im = fit(src, 1.6 + 0.25 * u, 0.4 + 0.2 * u, 0.5)
        else:
            im = caption(Image.new('RGB', (W, H), (40, 40, 44)), f'shot {num:02d} goes here', 1, 40, 0.4)
        if u < 0.08 and n > 0: im = blend(black, im, u / 0.08)  # a quick dip between shots
        im = caption(im, text, fade(t - t_shots - n * shot_s, 0.25, shot_s - 0.1, 0.35))
    else:  # end card
        u = t - t_end
        im = blend(black, ImageEnhance.Brightness(fit(final, 1.0, 0.62 if a.reel else 0.5)).enhance(0.35), min(1, u / 1.0))
        im = caption(im, 'Realistic Tectonics', fade(u, 0.5, 99), 120, 0.42, True)
        im = caption(im, 'NeoForge 1.21.1  ·  Fabric 26.2', fade(u, 1.2, 99), 48, 0.58)
        im = caption(im, 'Plates · erosion · rivers · glaciers · volcanoes · climate · caves', fade(u, 1.8, 99), 36, 0.66)
        im = blend(im, black, (u - 5.0) / 1.0)
    enc.stdin.write(im.tobytes())
enc.stdin.close(); enc.wait()
print(f'{total:.1f} s, shots {shot_s:.2f} s each, music {music}, exit {enc.returncode}')
