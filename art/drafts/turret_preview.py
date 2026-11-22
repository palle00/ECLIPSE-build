"""Preview a turret as the game draws it: vanilla base, outlined turret, outlined parts (mirrored ones twice), at
part progress 0 and 1. Usage: python turret_preview.py SPRITES BASES OUT.png SPEC.py
SPEC.py: TURRETS = [(name, base_size, [(suffix, mirror, moveX, moveY, moveRot), ...]), ...]  (move in world units)"""
import sys, os, runpy
from PIL import Image, ImageDraw

spr, bases, out, spec = sys.argv[1:5]
TURRETS = runpy.run_path(spec)["TURRETS"]
OUTLINE = (0x40, 0x40, 0x49, 255)

def outlined(im, r=4):
    """The game's turret outline: every transparent pixel within r of an opaque one becomes #404049."""
    w, h = im.size; src = im.load()
    res = Image.new("RGBA", (w + 2 * r, h + 2 * r)); dst = res.load()
    solid = [(x, y) for y in range(h) for x in range(w) if src[x, y][3] > 0]
    offs = [(dx, dy) for dy in range(-r, r + 1) for dx in range(-r, r + 1) if dx * dx + dy * dy <= r * r]
    for (x, y) in solid:
        for (dx, dy) in offs: dst[x + r + dx, y + r + dy] = OUTLINE
    res.alpha_composite(im, (r, r))
    return res

def load(n):
    p = os.path.join(spr, n + ".png")
    return Image.open(p).convert("RGBA") if os.path.exists(p) else None

def frame(name, size, parts, prog):
    px = size * 32
    canvas = Image.new("RGBA", (px + 48, px + 48), (60, 58, 52, 255))
    base = Image.open(os.path.join(bases, f"block-{size}.png")).convert("RGBA")
    canvas.alpha_composite(base, (24 + (px - base.width) // 2, 24 + (px - base.height) // 2))
    layers = [(load(name), 0, 0)]                                                   # (image, dx, dy)
    for (suf, mirror, mx, my, mr) in parts:
        pr, pl = (load(name + suf + "-r"), load(name + suf + "-l")) if mirror == "rl" else (load(name + suf), None)
        if pr is None: continue
        for sign in ((1, -1) if mirror else (1,)):
            src_im = pr if sign == 1 or pl is None else pl
            im = src_im if sign == 1 else src_im.transpose(Image.FLIP_LEFT_RIGHT)
            if mr: im = im.rotate(-mr * prog * sign, resample=Image.NEAREST)
            layers.append((im, int(round(mx * prog * 4 * sign)), int(round(-my * prog * 4))))
    for (im, dx, dy) in layers:                                                     # outlines below everything, as the game does
        o = outlined(im)
        canvas.alpha_composite(o, (24 + (px - o.width) // 2 + dx, 24 + (px - o.height) // 2 + dy))
    for (im, dx, dy) in layers:
        canvas.alpha_composite(im, (24 + (px - im.width) // 2 + dx, 24 + (px - im.height) // 2 + dy))
    return canvas

rows = []
for (name, size, parts) in TURRETS:
    rows.append([frame(name, size, parts, 0), frame(name, size, parts, 1)])
W = max(sum(f.width * 2 + 16 for f in r) for r in rows); H = sum(r[0].height * 2 + 16 for r in rows)
sheet = Image.new("RGBA", (W, H), (40, 40, 48, 255)); y = 0
for r in rows:
    x = 0
    for f in r:
        sheet.alpha_composite(f.resize((f.width * 2, f.height * 2), Image.NEAREST), (x, y)); x += f.width * 2 + 16
    sheet.alpha_composite(r[0], (x, y)); y += r[0].height * 2 + 16
sheet.save(out); print("preview", out)
