"""Item icons, family 1: metals, alloys, glass and silicon, each with its hot form (same silhouette, glowing ramp)."""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from itemlib import *

def ingot(ic, col, hot=False, variant=0):
    """A bevelled ingot lying at 45 degrees with a highlight stripe; variants change the profile."""
    r = ic.ramp(col, hot)
    L, Wd, c = [(33, 17, 4), (31, 20, 6), (34, 14, 3)][variant]                       # standard, heavy block, slim bar
    ic.put(poly(bar(16, 16, L, Wd, chamfer=c)), r, lambda px, py: 3)                 # the slab
    ic.put(poly(bar(15, 15, L - 9, Wd - 8, chamfer=max(1, c - 2))), r,               # its raised top, lit from the upper left
           lambda px, py: 1 if (px - 15) + (py - 15) < 0 else 2)
    return r

def nugget(ic, col, hot=False):
    r = ic.ramp(col, hot)
    pts = [(4, 14), (10, 5), (20, 3), (28, 9), (29, 19), (23, 27), (12, 29), (4, 23)]
    def f(px, py):                                                                  # three soft facets
        if px + py < 24: return 1
        if px - py > 2: return 2
        return 3
    ic.put(poly(pts), r, f)
    for (x, y) in ((10, 9), (11, 9), (10, 10), (14, 7)): ic.px(x, y, r[0])
    return r

def faceted(ic, col, hot=False, sides=6):
    """A faceted crystal chunk (chromium, vanadium)."""
    r = ic.ramp(col, hot)
    pts = ngon(16, 16, 14, sides, 90 / sides if sides % 2 == 0 else -90)
    def f(px, py):
        a = math.degrees(math.atan2(py - 16, px - 16)) % 360
        return [1, 0, 1, 2, 3, 2][int(a // (360 / 6)) % 6] if math.hypot(px - 16, py - 16) > 6 else 2
    ic.put(poly(pts), r, f)
    return r

def billets(ic, col, hot=False):
    """Three round billets seen end-on (magnesium)."""
    r = ic.ramp(col, hot)
    for (cx, cy) in ((16, 9), (9, 21), (23, 21)):
        ic.put(circle(cx, cy, 8.2), r, lambda px, py, cx=cx, cy=cy: 1 if math.hypot(px - cx + 2.5, py - cy + 2.5) < 3.5 else 2)
    return r

def insert(ic, col, hot=False):
    """A tungsten-carbide cutting insert: rounded triangle with a clamping hole."""
    r = ic.ramp(col, hot)
    rh = [(16, 2), (29, 11), (16, 30), (3, 21)]                                      # rhombic cutting insert with its clamping hole
    ic.put(minus(poly(rh), circle(16, 16, 3.6)), r, lambda px, py: 1 if px + py < 30 else 2)
    for (x, y) in ((15, 3), (16, 3), (17, 4), (28, 11)): ic.px(x, y, r[0])          # glints on the cutting edges
    return r

def polysilicon(ic, col, hot=False):
    """A chunk of polysilicon: angular, with bright crystal facets."""
    r = ic.ramp(col, hot)
    pts = [(5, 12), (12, 4), (22, 5), (28, 13), (26, 24), (17, 28), (7, 24)]
    def f(px, py):
        if px + py < 22: return 1
        if px - py > 6: return 3
        return 2
    ic.put(poly(pts), r, f)
    for (x, y) in ((11, 9), (12, 10), (19, 15), (20, 14), (9, 18)): ic.px(x, y, r[0])
    return r

def pane(ic, col, hot=False, mesh=True):
    """A glass pane tilted to the viewer, with a wire mesh (reinforced glass)."""
    r = ic.ramp(col, hot)
    pts = [(4, 10), (20, 3), (28, 22), (12, 29)]
    ic.put(poly(pts), r, lambda px, py: 0 if abs((px - 10) - (py - 9) * 0.4) < 1.2 or abs((px - 15) - (py - 7) * 0.4) < .8 else 2)
    if mesh:                                                                        # embedded wire: one strand each way
        k = r[3]
        ic.line(4 + 16 * .5, 10 - 7 * .5, 12 + 16 * .5, 29 - 7 * .5, k)
        ic.line(4 + 8 * .5, 10 + 19 * .5, 20 + 8 * .5, 3 + 19 * .5, k)
    return r

def diamond_gem(ic, col, hot=False):
    """Metaglass-like cut diamond."""
    r = ic.ramp(col, hot)
    ic.put(poly([(16, 2), (30, 16), (16, 30), (2, 16)]), r, lambda px, py: 0 if (px - 16) + (py - 16) < -9 else 1 if px < 16 and py < 16 else 2 if px > 16 and py > 16 else 1)
    return r

def surge_ingot(ic, col, hot=False):
    r = ingot(ic, col, hot, 1)
    k = ic.letters(["fff4c0"])[0]
    for (x, y) in ((11, 19), (12, 18), (13, 18), (14, 17), (15, 17), (16, 16), (17, 15), (18, 15), (19, 14)): ic.px(x, y, k)   # charge vein
    return r

def stamped_ingot(ic, col, hot=False):
    """Superalloy: a heavy ingot with a stamped mark."""
    r = ingot(ic, col, hot, 1)
    for (x, y) in ((17, 15), (18, 16), (19, 15), (17, 17), (19, 17)): ic.px(x, y, r[3])
    return r

def chrome(ic, col, hot=False):
    """Chromium: a mirror-polished ingot - high-contrast reflection bands across the raised top."""
    r = ingot(ic, col, hot, 0)
    for (o, k) in ((-3, 4), (-2, 4), (2, 0), (3, 0)):
        for t in range(-8, 9):
            x, y = 15 + t * .707 + o * .707, 15 - t * .707 + o * .707
            if ic.g[int(y)][int(x)] in (r[1], r[2]): ic.px(x, y, r[k])
    return r

DESIGNS = {
    # name: (drawer, colour, hot form, hot colour)
    "nickel": (lambda ic, c, h=False: ingot(ic, c, h, 0), "b4c2a6", None, None),
    "chromium": (chrome, "c3cedb", None, None),
    "cobalt": (lambda ic, c, h=False: ingot(ic, c, h, 2), "4f6fd1", None, None),
    "tungsten": (lambda ic, c, h=False: ingot(ic, c, h, 1), "8a8c96", None, None),
    "lithium": (lambda ic, c, h=False: nugget(ic, c, h), "d9cfe9", None, None),
    "aluminium": (lambda ic, c, h=False: ingot(ic, c, h, 0), "b9c4cf", "hot-aluminium", "ffb27a"),
    "magnesium": (billets, "d2d7dc", None, None),
    "molybdenum": (lambda ic, c, h=False: ingot(ic, c, h, 2), "8c9bb0", None, None),
    "vanadium": (lambda ic, c, h=False: faceted(ic, c, h, 5), "7a5fa8", None, None),
    "platinum": (nugget, "e4e6ea", None, None),
    "superalloy": (stamped_ingot, "cdb98a", "hot-superalloy", "ff9a4a"),
    "surge-superalloy": (surge_ingot, "f2c45a", "hot-surge-superalloy", "ffc04a"),
    "tungsten-carbide": (insert, "6a6e80", "hot-tungsten-carbide", "e0623a"),
    "high-purity-silicon": (polysilicon, "6d7a99", "hot-silicon", "c8704a"),
    "reinforced-glass": (pane, "9fc7d6", "hot-reinforced-glass", "f0aa7a"),
}
for name, (draw, col, hot, hotcol) in DESIGNS.items():
    ic = Icon(); draw(ic, col); ic.write(name); print("drafted", name)
    if hot:
        ic = Icon(); draw(ic, hotcol, True); ic.write(hot); print("drafted", hot)
# hot forms of vanilla materials keep the vanilla silhouettes: titanium (slab), metaglass (diamond)
ic = Icon(); ingot(ic, "e8906a", True, 1); ic.write("hot-titanium"); print("drafted hot-titanium")
ic = Icon(); diamond_gem(ic, "ffc89a", True); ic.write("hot-metaglass"); print("drafted hot-metaglass")
