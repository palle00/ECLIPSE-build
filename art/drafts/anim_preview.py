"""Preview blocks with their moving layers at three moments (1 world unit = 4 px, y up), plus a 1x row at game scale.

Usage: python anim_preview.py SPRITES STAGE OUT.png SPEC.py
SPEC.py defines BLOCKS = [(name, liquid_rgb_or_None, [layer, ...]), ...] where a layer is one of
  ("rot", suffix, deg)              full-size layer turning about the block centre
  ("spin", suffix, x, y, deg)       small part turning at x, y world units from the centre
  ("swing", suffix, px, py, deg)    full-size layer swinging +-deg about pivot px, py
  ("slide", suffix, dx, dy)         full-size layer sliding +-dx, dy
  ("press", suffix)                 full-size layer, shown 7% smaller at the press moment
  ("static", suffix)                full-size layer drawn as is
"""
import sys, os, runpy
from PIL import Image

spr, stage, out, specfile = sys.argv[1:5]
BLOCKS = runpy.run_path(specfile)["BLOCKS"]

def load(n):
    for d in (spr, stage):
        p = os.path.join(d, n + ".png")
        if os.path.exists(p): return Image.open(p).convert("RGBA")
    return None

def frame(name, liquid, layers, s):
    main = load(name); w, h = main.size
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    bottom = load(name + "-bottom")
    if bottom: im.alpha_composite(bottom)
    if liquid: pad = 16 if w >= 96 else 12; im.alpha_composite(Image.new("RGBA", (w - 2 * pad, h - 2 * pad), tuple(liquid) + (235,)), (pad, pad))
    im.alpha_composite(main)
    for L in layers:
        kind, suf = L[0], L[1]
        part = load(name + suf)
        if part is None: print("missing", name + suf); continue
        if kind == "rot": im.alpha_composite(part.rotate(L[2] * (s + 1), resample=Image.NEAREST))
        elif kind == "spin":
            r = part.rotate(L[4] * (s + 1), resample=Image.NEAREST)
            im.alpha_composite(r, (int(round(w / 2 + L[2] * 4 - r.width / 2)), int(round(h / 2 - L[3] * 4 - r.height / 2))))
        elif kind == "swing": im.alpha_composite(part.rotate(L[4] * s, resample=Image.NEAREST, center=(w / 2 + L[2] * 4, h / 2 - L[3] * 4)))
        elif kind == "slide":
            t = Image.new("RGBA", (w, h)); t.alpha_composite(part, (int(L[2] * 4 * s), int(-L[3] * 4 * s))); im.alpha_composite(t)
        elif kind == "press":
            if s == 1:
                k = int(w * .93); p2 = part.resize((k, k), Image.NEAREST); im.alpha_composite(p2, ((w - k) // 2, (h - k) // 2))
            else: im.alpha_composite(part)
        else: im.alpha_composite(part)
    return im

cols = len(BLOCKS); cell = 200
sheet = Image.new("RGBA", (cols * (cell + 8), 3 * (cell + 8) + 110), (52, 52, 62, 255))
for i, (name, liquid, layers) in enumerate(BLOCKS):
    for j, s in enumerate((-1, 0, 1)):
        f = frame(name, liquid, layers, s); sc = cell // f.width
        sheet.alpha_composite(f.resize((f.width * sc, f.height * sc), Image.NEAREST), (i * (cell + 8), j * (cell + 8)))
    sheet.alpha_composite(frame(name, liquid, layers, 0), (i * (cell + 8), 3 * (cell + 8) + 6))
sheet.save(out)
print("preview", out)
