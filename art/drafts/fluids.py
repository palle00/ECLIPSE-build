"""Liquid and gas icons in the vanilla convention: liquids are drops/blobs/splashes, gases the vanilla swirl.
Usage: python fluids.py OUT_DIR"""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from itemlib import *

def drop(ic, r, cx=16, top=3, rad=11, cy=19):
    """A teardrop: a round body with a point at the top."""
    ic.put(union(circle(cx, cy, rad), poly([(cx - rad * .72, cy - rad * .7), (cx, top), (cx + rad * .72, cy - rad * .7)])), r,
           lambda px, py: 1 if (px - cx) + (py - cy) < -rad * .55 else 2)
    for (x, y) in ((cx - rad * .45, cy - rad * .25), (cx - rad * .45, cy - rad * .1), (cx - rad * .3, cy - rad * .45)): ic.px(x, y, r[0])

def blob(ic, r):
    """An oil-like blob with a drip."""
    ic.put(union(circle(14, 12, 9.5), circle(20, 18, 9), circle(11, 20, 7)), r, lambda px, py: 1 if px + py < 20 else 2)   # lumpy oil-like blob
    ic.put(circle(25, 28.5, 2.6), r, lambda px, py: 2)                               # a drip falling off it
    for (x, y) in ((9, 8), (10, 8), (8, 9)): ic.px(x, y, r[0])

def acid(ic, r):
    """A drop and a smaller drop (vanilla slag style), with a bubble: corrosive liquids."""
    drop(ic, r, 13, 3, 10, 18)
    ic.put(circle(25, 26, 4.2), r, lambda px, py: 1 if px + py < 50 else 2)
    ic.put(ring(19, 11, 1.2, 2.2), r, lambda px, py: 0, rim=False)

def splash(ic, r):
    """A cryo-style splash: a sideways blob with a flung droplet."""
    ic.put(union(circle(18, 19, 10), poly([(10.5, 12), (3, 3), (18.5, 9)])), r, lambda px, py: 1 if (px - 18) + (py - 19) < -5 else 2)   # a drop flung to the side
    ic.put(circle(27, 6, 3.4), r, lambda px, py: 1)
    for (x, y) in ((13, 15), (14, 14), (12, 16)): ic.px(x, y, r[0])

def swirl(ic, r):
    """The vanilla gas swirl: a thick spiral curling in to the centre."""
    def t(px, py):
        x, y = px - 16, py - 16
        rr, th = math.hypot(x, y), (math.atan2(y, x) + math.pi * 1.5) % (2 * math.pi)
        s = 4.2 + th / (2 * math.pi) * 8.2                                     # spiral radius grows round one turn
        return abs(rr - s) <= 2.9 or rr <= 3.3
    ic.put(t, r, lambda px, py: 1 if (px - 16) + (py - 16) < -6 else 2)

def specks(ic, r, spots, k=4):
    for (x, y) in spots: ic.px(x, y, r[k])

LIQUIDS = {  # name: (colour, drawer, extra)
    "purified-water": ("8fd3ff", drop, None), "heavy-water": ("6fa8e8", drop, None), "chilled-water": ("7fd8ff", drop, "ice"),
    "brine": ("a7c9b4", drop, "salt"), "methanol": ("d6e4f0", drop, None),
    "naphtha": ("d9a45a", blob, None), "diesel": ("d8b25e", blob, None), "biofuel": ("f0e6a0", blob, None), "acrylonitrile": ("c9b8e8", blob, None),
    "acidic-waste": ("8f8a3a", blob, "murk"), "wastewater": ("6f7f5a", blob, "murk"),
    "sulfuric-acid": ("e8b84a", acid, None), "hydrochloric-acid": ("dfe882", acid, None), "nitric-acid": ("f0d98c", acid, None),
    "aqua-regia": ("e8873a", acid, None), "sodium-hydroxide": ("d8dff0", acid, None),
    "liquid-nitrogen": ("bfe3ff", splash, None), "liquid-sodium": ("e0e2e8", splash, "metal"),
}
GASES = {"steam": "e8edf0", "oxygen": "d8efff", "chlorine": "b6dc4c", "methane": "c4b3e6", "ethylene": "aee3cf", "ammonia": "c9e6f2",
         "flue-gas": "8a8580", "carbon-dioxide": "c8ccd0", "propylene": "e6d3a8", "argon": "c9d6ff", "deuterium": "ffb3c8"}

for name, (col, draw, extra) in LIQUIDS.items():
    ic = Icon(); r = ic.ramp(col); draw(ic, r)
    if extra == "ice":
        w = ic.letters(["f4fbff"])[0]
        for (x, y) in ((19, 17), (20, 16), (21, 17), (20, 18), (20, 15), (20, 19), (18, 17), (22, 17)): ic.px(x, y, w)
    elif extra == "salt":
        w = ic.letters(["f4f6f0"])[0]
        for (x, y) in ((19, 20), (14, 24), (21, 25), (17, 15)): ic.px(x, y, w)
    elif extra == "murk": specks(ic, r, [(14, 14), (20, 12), (18, 18), (11, 17), (23, 16)])
    elif extra == "metal":
        for (x, y) in ((12, 15), (13, 15), (14, 15), (15, 16), (20, 21)): ic.px(x, y, r[0])
    ic.write(name); print("drafted", name)
for name, col in GASES.items():
    ic = Icon(); swirl(ic, ic.ramp(col)); ic.write(name); print("drafted", name)
