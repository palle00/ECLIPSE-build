"""Ore floor overlays (3 variants each + the plain icon) in the vanilla style: 2-4 chunky pebbles per tile, each lit on
its upper left with a dark lower rim; every ore has its own pebble form. Usage: python ores.py OUT_DIR"""
import sys, os, math, random
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from itemlib import *

def pebble(ic, r, cx, cy, size, sides, rnd, sharp=False):
    pts = []
    for i in range(sides):
        a = i * 2 * math.pi / sides + rnd.uniform(-.25, .25)
        d = size * (rnd.uniform(.75, 1.05) if not sharp or i % 2 else rnd.uniform(1.2, 1.5))
        pts.append((cx + d * math.cos(a), cy + d * .85 * math.sin(a)))
    ic.put(poly(pts), r, lambda px, py: 1 if (px - cx) + (py - cy) < -size * .3 else 2)

def crystal(ic, r, cx, cy, h, w):
    ic.put(poly([(cx - w, cy + h * .4), (cx - w * .6, cy - h * .5), (cx, cy - h), (cx + w * .6, cy - h * .5), (cx + w, cy + h * .4), (cx, cy + h * .6)]), r,
           lambda px, py: 1 if px < cx else 2)

ORES = {  # name: (colour, form)
    "ore-nickel": ("b0a470", "chunk"), "ore-chromium": ("6e6a78", "octa"), "ore-cobalt": ("4a68b8", "cube"), "ore-tungsten": ("5a5650", "chunk"),
    "ore-molybdenum": ("7a8496", "flake"), "ore-platinum": ("dfe2e8", "nugget"), "ore-aetherite": ("6fd0f0", "crystal"), "ore-bauxite": ("b87850", "round"),
}
LAYOUTS = [[(9, 9, 8), (23, 8, 6), (18, 22, 9)], [(10, 20, 9), (24, 22, 7), (15, 7, 6)], [(8, 8, 6), (21, 13, 9.5), (9, 23, 7), (25, 26, 4)]]

if len(sys.argv) < 3:
  for name, (col, form) in ORES.items():
      for v, layout in enumerate(LAYOUTS):
          rnd = random.Random(sum(map(ord, name)) * 7 + v)                          # stable across runs
          ic = Icon(); r = ic.ramp(col)
          for (cx, cy, s) in layout:
              if form == "chunk": pebble(ic, r, cx, cy, s, 6, rnd)
              elif form == "octa": ic.put(poly([(cx, cy - s), (cx + s, cy), (cx, cy + s), (cx - s, cy)]), r, lambda px, py, cx=cx, cy=cy: 1 if px < cx and py < cy else 3 if px > cx and py > cy else 2)
              elif form == "cube": ic.put(rect(cx - s * .75, cy - s * .75, cx + s * .75, cy + s * .75), r, lambda px, py, cy=cy, s=s: 1 if py < cy - s * .3 else 2)
              elif form == "flake": ic.put(poly([(p[0], cy + (p[1] - cy) * .55) for p in ngon(cx, cy, s, 6, rnd.uniform(0, 30))]), r, lambda px, py, cy=cy: 1 if py < cy else 2)
              elif form == "nugget": pebble(ic, r, cx, cy, s * .8, 8, rnd)
              elif form == "crystal": crystal(ic, r, cx, cy, s * 1.2, s * .55)
              else: ic.put(circle(cx, cy, s * .8), r, lambda px, py, cx=cx, cy=cy, s=s: 1 if math.hypot(px - cx + s * .3, py - cy + s * .3) < s * .4 else 2)
          ic.write(f"{name}{v + 1}")
          if v == 0: ic.write(name)
      print("drafted", name)

def shadow(png):
    """Vanilla ore pebbles cast a soft shadow (black, alpha 76) down and right of each pebble."""
    from PIL import Image
    im = Image.open(png).convert("RGBA"); px = im.load(); w, h = im.size
    solid = {(x, y) for y in range(h) for x in range(w) if px[x, y][3] == 255}
    for (x, y) in solid:
        for (dx, dy) in ((1, 1), (1, 2), (0, 2)):
            q = (x + dx, y + dy)
            if q not in solid and q[0] < w and q[1] < h: px[q] = (0, 0, 0, 76)
    im.save(png)

if len(sys.argv) > 2 and sys.argv[2] == "--shadow":
    import glob
    for f in glob.glob(os.path.join(sys.argv[1], "ore-*.png")): shadow(f)
