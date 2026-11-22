"""Item icons, family 2: ore minerals, raw materials, powders, chemicals, organics and fuel."""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from itemlib import *

def cube(ic, r, cx, cy, s):
    """An isometric cube: lit top, mid left face, dark right face."""
    top = [(cx, cy - s), (cx + s, cy - s / 2), (cx, cy), (cx - s, cy - s / 2)]
    left = [(cx - s, cy - s / 2), (cx, cy), (cx, cy + s), (cx - s, cy + s / 2)]
    right = [(cx, cy), (cx + s, cy - s / 2), (cx + s, cy + s / 2), (cx, cy + s)]
    ic.put(poly(left), r, lambda px, py: 2, rim=False); ic.put(poly(right), r, lambda px, py: 3, rim=False); ic.put(poly(top), r, lambda px, py: 1, rim=False)
    ic.line(cx - s, cy - s / 2, cx, cy - s, r[0]); ic.line(cx, cy, cx, cy + s - 1, r[4]); ic.line(cx + s - 1, cy + s / 2, cx, cy + s - 1, r[4])

def pebbles(ic, r, spots):
    for (cx, cy, rad) in spots:
        ic.put(circle(cx, cy, rad), r, lambda px, py, cx=cx, cy=cy, rad=rad: 1 if math.hypot(px - cx + rad * .35, py - cy + rad * .35) < rad * .45 else 2)

def heap(ic, r, grains=None):
    """A powder heap."""
    ic.put(poly([(2, 27), (8, 17), (13, 11), (19, 11), (24, 17), (30, 27)]), r, lambda px, py: 1 if px < 15 + (py - 11) * .2 else 2)
    if grains:
        k = ic.letters([grains])[0]
        for (x, y) in ((9, 22), (14, 15), (20, 20), (25, 24), (16, 24), (12, 19)): ic.px(x, y, k)

def crystal(ic, r, cx, base, top, w):
    """An upright crystal prism with a pointed tip."""
    pts = [(cx - w, base), (cx - w, top + w), (cx, top), (cx + w, top + w), (cx + w, base)]
    ic.put(poly(pts), r, lambda px, py: 1 if px < cx else 3 if px > cx + w * .4 else 2)

def blob(ic, r, pts, spots=None, sheen=True):
    ic.put(poly(pts), r, lambda px, py: 2)
    if sheen:
        for (x, y) in ((10, 12), (11, 12), (12, 11), (13, 11)): ic.px(x, y, r[0])
    for (x, y) in (spots or []): ic.px(x, y, r[4]); ic.px(x + 1, y, r[4])

BLOB = [(3, 20), (6, 12), (12, 8), (20, 8), (27, 12), (29, 20), (25, 26), (15, 28), (7, 27)]

def item(name, col):
    def deco(fn):
        ic = Icon(); fn(ic, ic.ramp(col), col); ic.write(name); print("drafted", name)
        return fn
    return deco

# ---- ore minerals ----
@item("pentlandite", "b8a46a")
def _(ic, r, c): cube(ic, r, 13, 13, 10); cube(ic, ic.ramp(tone(c, -.08)), 22, 20, 7)
@item("sperrylite", "c8ccd2")
def _(ic, r, c): cube(ic, r, 15, 15, 12); ic.line(9, 12, 15, 15, r[1]); ic.line(21, 12, 15, 15, r[3])
@item("cobaltite", "a88fb8")
def _(ic, r, c):
    cube(ic, r, 12, 14, 9); cube(ic, r, 21, 18, 9)
    for k in (-3, 0, 3): ic.line(16 + k, 11, 21 + k, 13.5, r[3])                   # striations
@item("chromite", "4e4a52")
def _(ic, r, c):
    ic.put(poly([(16, 2), (29, 16), (16, 30), (3, 16)]), r, lambda px, py: (1 if px < 16 else 2) if py < 16 else (2 if px < 16 else 3))
    ic.line(16, 3, 16, 29, r[4]); ic.line(4, 16, 28, 16, r[3])
@item("wolframite", "4e4642")
def _(ic, r, c):
    for (x0, h) in ((6, 22), (13, 26), (21, 18)):
        ic.put(poly([(x0, 29), (x0, 29 - h), (x0 + 5, 27 - h), (x0 + 7, 25 - h), (x0 + 7, 29)]), r, lambda px, py, x0=x0: 1 if px < x0 + 2.5 else 2 if px < x0 + 5 else 3)
@item("molybdenite", "6f7686")
def _(ic, r, c):
    for (cx, cy) in ((13, 20), (19, 12)):
        ic.put(poly([(p[0], p[1] * .55 + cy * .45) for p in ngon(cx, cy, 12, 6, 0)]), r, lambda px, py, cy=cy: 1 if py < cy - 1 else 2)
@item("bauxite", "c4875a")
def _(ic, r, c): pebbles(ic, r, [(10, 21, 7), (21, 21, 6.5), (16, 11, 6.5), (25, 11, 4), (6, 11, 3.5)])
@item("crushed-ore", "8a7f72")
def _(ic, r, c):
    for i, (pts, col) in enumerate((([(3, 18), (8, 12), (14, 15), (12, 23), (5, 24)], c), ([(14, 8), (21, 4), (27, 9), (22, 15), (15, 14)], "9a8a7a"),
                                    ([(16, 18), (24, 15), (29, 22), (24, 29), (16, 27)], "6e6a7a"), ([(9, 25), (14, 24), (15, 29), (9, 30)], "a87f5a"))):
        ic.put(poly(pts), ic.ramp(col), lambda px, py: 2)
@item("red-mud", "9c4632")
def _(ic, r, c): blob(ic, r, BLOB, [(20, 18), (12, 21)])
@item("industrial-sludge", "5c5140")
def _(ic, r, c): blob(ic, r, BLOB, [(19, 17), (11, 21), (22, 23), (15, 14)])
@item("petroleum-coke", "3a3a42")
def _(ic, r, c):
    ic.put(poly([(4, 16), (9, 6), (19, 4), (28, 10), (29, 21), (21, 28), (9, 27)]), r, lambda px, py: 1 if px + py < 22 else 2)
    for (x, y) in ((13, 12), (20, 10), (18, 18), (10, 20), (24, 17), (15, 23)): ic.px(x, y, r[4]); ic.px(x + 1, y, r[4]); ic.px(x, y + 1, r[4])   # pores
@item("aetherite", "7fe0ff")
def _(ic, r, c):
    crystal(ic, ic.ramp(tone(c, -.12)), 9, 28, 12, 4); crystal(ic, ic.ramp(tone(c, -.06)), 23, 28, 10, 4); crystal(ic, r, 16, 29, 2, 5)

# ---- powders and chemicals ----
@item("sulfur", "e8d44d")
def _(ic, r, c):
    ic.put(poly([(13, 2), (24, 14), (13, 30), (3, 15)]), r, lambda px, py: 0 if px < 13 and py < 15 else 1 if py < 15 else 2 if px < 13 else 3)
    ic.put(poly([(24, 13), (30, 19), (24, 28), (19, 20)]), r, lambda px, py: 1 if py < 19 else 3)
@item("soda-ash", "f2f0ea")
def _(ic, r, c):
    for (cx, cy, s) in ((10, 20, 6), (21, 21, 6), (16, 11, 6), (25, 11, 4)): cube(ic, ic.ramp(tone(c, -.05)), cx, cy, s)
@item("alumina", "ece8e2")
def _(ic, r, c): heap(ic, r, "c8c0b4")
@item("ammonium-nitrate", "f2efe4")
def _(ic, r, c): pebbles(ic, r, [(9, 22, 5), (17, 23, 5), (25, 22, 5), (13, 15, 5), (21, 15, 5), (17, 8, 4.5)])
@item("fertilizer", "d8cfa8")
def _(ic, r, c):
    ic.put(poly([(6, 8), (12, 4), (20, 4), (26, 8), (28, 28), (4, 28)]), r, lambda px, py: 1 if px < 13 else 2)   # sack
    ic.put(rect(9, 3, 22, 6), r, lambda px, py: 3)
    g = ic.ramp("6fae4a"); ic.put(ellipse(16, 18, 5, 3.2), g, lambda px, py: 1 if py < 18 else 2)   # leaf print
@item("polyethylene", "e3ecf0")
def _(ic, r, c):
    for (cx, cy) in ((10, 21), (21, 22), (15, 12), (25, 12), (6, 12)): ic.put(ellipse(cx, cy, 5.5, 4), r, lambda px, py, cy=cy: 1 if py < cy else 2)
@item("synthetic-rubber", "3a3a42")
def _(ic, r, c):
    ic.put(poly(bar(16, 16, 30, 20, deg=-20, chamfer=6)), r, lambda px, py: 1 if px + py * .4 < 15 else 2)
    for t in (-4, 3): ic.line(8, 15 + t, 24, 9 + t, r[3])
@item("epoxy-resin", "e0a94a")
def _(ic, r, c):
    ic.put(union(circle(16, 20, 10), poly([(9, 15), (16, 1), (23, 15)])), r, lambda px, py: 1 if px < 14 else 2)
    for (x, y) in ((11, 16), (11, 17), (12, 15), (12, 21)): ic.px(x, y, r[0])
@item("carbon-fiber", "3a3c44")
def _(ic, r, c):
    m = ic.ramp("989aa4"); ic.put(rect(3, 4, 28, 8), m, lambda px, py: 1); ic.put(rect(3, 23, 28, 27), m, lambda px, py: 2)   # bobbin flanges
    ic.put(rect(6, 9, 25, 22), r, lambda px, py: 2)
    for y in range(10, 22, 2): ic.line(7, y, 24, y, r[3])
    for (x, y) in ((10, 11), (11, 11), (16, 15), (17, 15), (13, 19)): ic.px(x, y, r[0])   # fibre sheen
@item("carbon-composite", "4a5260")
def _(ic, r, c):
    pts = [(3, 9), (21, 3), (29, 21), (11, 29)]
    ic.put(poly(pts), r, lambda px, py: 1 if ((int(px) // 3) + (int(py) // 3)) % 2 == 0 else 3)   # twill weave
@item("biomass", "6fae4a")
def _(ic, r, c):
    for (cx, cy, rx, ry, col) in ((11, 18, 8, 6, tone(c, -.08)), (21, 19, 8, 6, c), (16, 11, 7, 6, tone(c, .06))):
        ic.put(ellipse(cx, cy, rx, ry), ic.ramp(col), lambda px, py, cy=cy: 1 if py < cy else 2)
@item("enzyme-extract", "e86fd0")
def _(ic, r, c):
    gl = ic.ramp("c8dce8"); ic.put(poly(bar(16, 16, 32, 12, deg=-45, chamfer=5)), gl, lambda px, py: 1)
    ic.put(both(poly(bar(16, 16, 26, 7, deg=-45, chamfer=3)), lambda px, py: px + py > 30), r, lambda px, py: 1 if px < py else 2)
    ic.put(poly(bar(27, 5, 6, 10, deg=-45, chamfer=1)), ic.ramp("8a5a3a"), lambda px, py: 2)   # stopper
@item("polymer-catalyst", "b07ad6")
def _(ic, r, c):
    for (cx, cy) in ((10, 20), (22, 20), (16, 9)):
        ic.put(rect(cx - 5, cy - 3, cx + 5, cy + 6), r, lambda px, py: 2); ic.put(ellipse(cx + .5, cy - 2, 6, 3), r, lambda px, py: 1)
@item("lead-acid-cell", "7f9a63")
def _(ic, r, c):
    ic.put(rect(3, 8, 28, 28), r, lambda px, py: 1 if px < 10 else 2)
    ic.put(rect(3, 8, 28, 11), ic.ramp("5a5c64"), lambda px, py: 1)
    ic.put(rect(7, 3, 11, 7), ic.ramp("d96b68"), lambda px, py: 1); ic.put(rect(20, 3, 24, 7), ic.ramp("4a4b53"), lambda px, py: 1)
    k = ic.letters(["f2f0ea"])[0]
    for x in range(12, 20): ic.px(x, 18, k)
@item("superconductor", "8fd0ff")
def _(ic, r, c):
    ic.put(minus(ellipse(16, 16, 14, 10), ellipse(16, 16, 7, 4.5)), r, lambda px, py: 1 if py < 14 else 2)   # tape coil
    ic.put(minus(ellipse(16, 16, 10.5, 7.5), ellipse(16, 16, 9.5, 6.5)), r, lambda px, py: 3, rim=False)
@item("experimental-core", "c88fff")
def _(ic, r, c):
    fr = ic.ramp("989aa4"); ic.put(minus(poly(ngon(16, 16, 15, 8, 22.5)), poly(ngon(16, 16, 10, 8, 22.5))), fr, lambda px, py: 1 if px + py < 30 else 2)
    ic.put(poly([(16, 6), (25, 16), (16, 26), (7, 16)]), r, lambda px, py: 0 if px < 14 and py < 14 else 1 if px < 16 else 2)

def rods(ic, col, dull=False):
    for i, x0 in enumerate((6, 13, 20)):
        rr = ic.ramp(col if not dull else tone(col, -.04 * i))
        ic.put(rect(x0, 6, x0 + 6, 27), rr, lambda px, py, x0=x0: 1 if px < x0 + 2.5 else 2)
        cap = ic.ramp("b8bcc4" if not dull else "6e7080"); ic.put(rect(x0, 3, x0 + 6, 6), cap, lambda px, py: 1); ic.put(rect(x0, 27, x0 + 6, 29), cap, lambda px, py: 2)
    if dull:
        k = ic.letters(["3a3238"])[0]
        for (x, y) in ((8, 12), (9, 13), (16, 18), (15, 19), (22, 10), (23, 22)): ic.px(x, y, k)
@item("thorium-fuel-rod", "c96fa0")
def _(ic, r, c): rods(ic, c)
@item("spent-fuel-rod", "6d5a63")
def _(ic, r, c): rods(ic, c, True)
