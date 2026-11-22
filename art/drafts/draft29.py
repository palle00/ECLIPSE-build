"""Item icons, family 3: mechanical parts, stock forms (plates, foils, wires, rods), motors and electronics."""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from itemlib import *

def item(name, col):
    def deco(fn):
        ic = Icon(); fn(ic, ic.ramp(col), col); ic.write(name); print("drafted", name)
        return fn
    return deco

def gear(ic, r, teeth, rout, rin, hole, spokes=0):
    def t(px, py):
        d, a = math.hypot(px - 16, py - 16), math.atan2(py - 16, px - 16)
        k = (a % (2 * math.pi / teeth)) / (2 * math.pi / teeth)
        return d <= (rout if .2 < k < .7 else rin) and d >= hole
    ic.put(t, r, lambda px, py: 1 if (px - 16) + (py - 16) < -2 else 2)
    if spokes:
        for k in range(spokes):
            a = k * 2 * math.pi / spokes
            ic.line(16 + hole * math.cos(a), 16 + hole * math.sin(a), 16 + (rin - 3) * math.cos(a), 16 + (rin - 3) * math.sin(a), r[3])
    ic.put(ring(16, 16, hole, hole + 1.5), r, lambda px, py: 3, rim=False)

def plates(ic, col, thick=3):
    """Two stacked sheets lying flat (isometric)."""
    for i, oy in enumerate((6, -2)):
        r = ic.ramp(tone(col, -.06 * (1 - i)))
        top = [(3, 15 + oy), (17, 8 + oy), (29, 14 + oy), (15, 21 + oy)]
        ic.put(poly([(3, 15 + oy), (15, 21 + oy), (29, 14 + oy), (29, 14 + oy + thick), (15, 21 + oy + thick), (3, 15 + oy + thick)]), r, lambda px, py: 3)
        ic.put(poly(top), r, lambda px, py: 1 if px < 13 else 2)

def foil(ic, col):
    """A foil roll lying on its side, its sheet unrolling toward the viewer."""
    r = ic.ramp(col)
    ic.put(poly([(6, 16), (26, 16), (29, 29), (3, 29)]), r, lambda px, py: 1 if px < 10 else 2)    # unrolled sheet
    ic.put(rect(4, 5, 27, 17), r, lambda px, py: 1 if py < 9 else 3 if py > 14 else 2)        # the roll
    ic.put(ellipse(4.5, 11, 3, 6), r, lambda px, py: 1)
    ic.put(ellipse(4.5, 11, 1.4, 2.6), ic.ramp(tone(col, -.3)), lambda px, py: 3, rim=False)

def spool(ic, col, flange="989aa4"):
    f = ic.ramp(flange)
    ic.put(ellipse(16, 6, 13, 4), f, lambda px, py: 1); ic.put(ellipse(16, 26, 13, 4), f, lambda px, py: 2)
    r = ic.ramp(col)
    ic.put(rect(5, 6, 26, 25), r, lambda px, py: 1 if px < 11 else 3 if px > 22 else 2)
    for y in range(8, 25, 3): ic.line(6, y, 25, y + 1, r[3])
    ic.put(ellipse(16, 6, 13, 4), f, lambda px, py: 1)

def rods2(ic, col):
    r = ic.ramp(col)
    for o in (-5, 5):
        ic.put(poly(bar(16 + o * .7, 16 + o * .7, 34, 7, deg=-45, chamfer=1)), r, lambda px, py: 1 if px + py < 32 else 2)

def motor(ic, col, band=None, glow=None):
    r = ic.ramp(col)
    ic.put(rect(24, 14, 30, 18), ic.ramp("b8bcc4"), lambda px, py: 1)                # shaft
    ic.put(rect(3, 7, 25, 25), r, lambda px, py: 1 if py < 12 else 3 if py > 21 else 2)   # body
    for x in range(6, 22, 3): ic.line(x, 8, x, 24, r[3])                            # cooling fins
    ic.put(rect(3, 7, 6, 25), ic.ramp("d99d73"), lambda px, py: 1 if py < 14 else 2)  # end windings
    if band: ic.put(rect(13, 7, 17, 25), ic.ramp(band), lambda px, py: 1 if py < 14 else 2)
    if glow:
        k = ic.letters([glow])[0]
        for x in range(7, 23, 4): ic.px(x, 16, k); ic.px(x + 1, 16, k)

@item("copper-gear", "d99d73")
def _(ic, r, c): gear(ic, r, 8, 15, 11.5, 4)
@item("titanium-gear", "8da1e3")
def _(ic, r, c): gear(ic, r, 10, 15, 12, 4.5, 5)
@item("superalloy-gear", "cdb98a")
def _(ic, r, c): gear(ic, r, 14, 15, 12.5, 4, 6)

for n, col, thick in (("copper-plate", "d99d73", 3), ("lead-plate", "8c7fa9", 3), ("titanium-plate", "8da1e3", 3), ("aluminium-plate", "b9c4cf", 2),
                      ("superalloy-plate", "cdb98a", 3), ("tungsten-plate", "8a8c96", 4)):
    ic = Icon(); plates(ic, col, thick); ic.write(n); print("drafted", n)
for n, col in (("copper-foil", "e8a070"), ("aluminium-foil", "d6dde3")):
    ic = Icon(); foil(ic, col); ic.write(n); print("drafted", n)
for n, col in (("copper-wire", "e8a070"), ("aluminium-wire", "c9d2da"), ("magnet-wire", "b85a2e")):
    ic = Icon(); spool(ic, col); ic.write(n); print("drafted", n)
for n, col in (("titanium-rod", "8da1e3"), ("tungsten-rod", "8a8c96")):
    ic = Icon(); rods2(ic, col); ic.write(n); print("drafted", n)

@item("insulated-cable", "3a3a42")
def _(ic, r, c):
    ic.put(minus(circle(15, 15, 13), circle(15, 15, 6)), r, lambda px, py: 1 if px + py < 26 else 2)
    ic.put(ring(15, 15, 9, 9.8), r, lambda px, py: 3, rim=False)
    ic.put(poly(bar(25, 25, 10, 5, deg=-45, chamfer=1)), r, lambda px, py: 2)
    ic.put(circle(28.5, 28.5, 2.2), ic.ramp("e8a070"), lambda px, py: 1)           # copper core at the cut end
@item("titanium-bolt", "8da1e3")
def _(ic, r, c):
    ic.put(rect(12, 11, 19, 30), r, lambda px, py: 1 if px < 15 else 2)              # shank
    for y in range(14, 30, 3): ic.line(12, y, 19, y - 1, r[3])                     # thread
    ic.put(poly([(5, 4), (10, 1), (21, 1), (26, 4), (26, 10), (21, 13), (10, 13), (5, 10)]), r, lambda px, py: 1 if px < 13 else 2 if px < 20 else 3)   # hex head
@item("titanium-screw", "a0b2ec")
def _(ic, r, c):
    ic.put(poly([(12, 11), (19, 11), (19, 25), (15.5, 31), (12, 25)]), r, lambda px, py: 1 if px < 15 else 2)   # pointed shank
    for y in range(13, 27, 3): ic.line(11, y + 1, 20, y - 1, r[3])
    ic.put(ellipse(15.5, 7, 11, 6), r, lambda px, py: 1 if py < 6 else 2)             # round head
    ic.line(10, 7, 21, 7, r[4])                                                       # slot
@item("titanium-ring", "8da1e3")
def _(ic, r, c): ic.put(minus(circle(16, 16, 14), circle(16, 16, 7.5)), r, lambda px, py: 1 if (px - 16) + (py - 16) < -3 else 2)
@item("titanium-spring", "9aaee8")
def _(ic, r, c):
    for i in range(6):
        y = 6 + i * 4
        ic.put(ellipse(16, y + 2, 12, 2.6), r, lambda px, py: 1 if px < 14 else 2)
        ic.put(ellipse(16, y + 2, 9, 1.2), ic.ramp(tone(c, -.3)), lambda px, py: 3, rim=False)
@item("rotor", "8da1e3")
def _(ic, r, c):
    for k in range(4):
        a = math.radians(k * 90 + 20)
        pts = [(16 + 4 * math.cos(a - .6), 16 + 4 * math.sin(a - .6)), (16 + 15 * math.cos(a - .15), 16 + 15 * math.sin(a - .15)),
               (16 + 15 * math.cos(a + .25), 16 + 15 * math.sin(a + .25)), (16 + 4 * math.cos(a + .6), 16 + 4 * math.sin(a + .6))]
        ic.put(poly(pts), r, lambda px, py: 1 if (px - 16) + (py - 16) < 0 else 2)
    ic.put(circle(16, 16, 4.5), ic.ramp("b8bcc4"), lambda px, py: 1)
@item("electric-motor", "7a8a9c")
def _(ic, r, c): motor(ic, c)
@item("permanent-magnet-motor", "8a8c96")
def _(ic, r, c): motor(ic, c, band="c04a4a")
@item("superconducting-motor", "8fb8d8")
def _(ic, r, c): motor(ic, c, glow="e8fbff")
@item("pump-unit", "5f8fb0")
def _(ic, r, c):
    ic.put(union(circle(14, 17, 12), rect(14, 5, 29, 11)), r, lambda px, py: 1 if px + py < 24 else 2)   # volute with its outlet
    ic.put(circle(14, 17, 5.5), ic.ramp("b8bcc4"), lambda px, py: 1)
    ic.put(circle(14, 17, 2), r, lambda px, py: 4, rim=False)
@item("piston", "9aa3ad")
def _(ic, r, c):
    ic.put(poly([(13, 16), (19, 16), (21, 30), (11, 30)]), ic.ramp("c8ccd2"), lambda px, py: 1 if px < 16 else 2)   # connecting rod
    ic.put(circle(16, 29, 3), ic.ramp("c8ccd2"), lambda px, py: 2)
    ic.put(rect(5, 2, 26, 18), r, lambda px, py: 1 if px < 11 else 3 if px > 22 else 2)   # piston head
    for y in (5, 8): ic.line(5, y, 26, y, r[4])                                        # ring grooves
    ic.put(circle(16, 14, 2.4), ic.ramp("5a5c64"), lambda px, py: 2, rim=False)       # wrist pin
@item("pressure-casing", "9ab0b8")
def _(ic, r, c):
    ic.put(union(rect(5, 8, 26, 26), ellipse(15.5, 8, 10.5, 5)), r, lambda px, py: 1 if px < 11 else 2)   # domed vessel
    for x in (8, 15, 22): ic.put(circle(x, 23, 1.6), r, lambda px, py: 4, rim=False)
    ic.put(rect(5, 17, 26, 18), r, lambda px, py: 3, rim=False)
@item("industrial-bearing", "b8bcc4")
def _(ic, r, c):
    ic.put(minus(circle(16, 16, 14.5), circle(16, 16, 5.5)), r, lambda px, py: 2)
    ic.put(ring(16, 16, 8, 12), ic.ramp("4a4b53"), lambda px, py: 2, rim=False)      # ball race
    for k in range(8):
        a = k * math.pi / 4
        ic.put(circle(16 + 10 * math.cos(a), 16 + 10 * math.sin(a), 1.9), r, lambda px, py: 0)
@item("superalloy-frame", "cdb98a")
def _(ic, r, c):
    ic.put(minus(rect(3, 3, 28, 28), rect(9, 9, 22, 22)), r, lambda px, py: 1 if px + py < 31 else 2)
    ic.put(poly([(8, 10), (10, 8), (23, 21), (21, 23)]), r, lambda px, py: 2)       # cross brace
@item("alnico-magnet", "c04a4a")
def _(ic, r, c):
    ic.put(minus(union(circle(16, 14, 13), rect(3, 14, 29, 28)), union(circle(16, 14, 6), rect(10, 14, 21, 30))), r, lambda px, py: 1 if px < 12 else 2)
    s = ic.ramp("c8ccd2"); ic.put(rect(3, 24, 9, 28), s, lambda px, py: 1); ic.put(rect(22, 24, 28, 28), s, lambda px, py: 1)
@item("heating-coil", "ff8a4a")
def _(ic, r, c):
    core = ic.ramp("d8d0c0"); ic.put(rect(2, 12, 29, 19), core, lambda px, py: 1 if py < 15 else 2)   # ceramic former
    for x in range(3, 29, 4): ic.put(ellipse(x + 1.5, 15.5, 2, 10), r, lambda px, py: 1 if py < 13 else 2)
@item("sensor", "6dd0f0")
def _(ic, r, c):
    b = ic.ramp("5a5c64"); ic.put(rect(4, 6, 27, 25), b, lambda px, py: 1 if px < 10 else 2)
    ic.put(circle(15.5, 15.5, 7), ic.ramp("2a2b32"), lambda px, py: 2); ic.put(circle(15.5, 15.5, 4.5), r, lambda px, py: 1 if px + py < 30 else 2)
    for (x0, x1) in ((8, 9), (22, 23)): ic.put(rect(x0, 25, x1, 29), ic.ramp("c8ccd2"), lambda px, py: 1)
@item("emitter", "ff5a5a")
def _(ic, r, c):
    b = ic.ramp("8a8c96"); ic.put(rect(2, 8, 19, 24), b, lambda px, py: 1 if py < 13 else 2)
    for x in (5, 9, 13): ic.line(x, 9, x, 23, b[3])
    ic.put(rect(18, 10, 23, 22), ic.ramp("5a5c64"), lambda px, py: 2)
    ic.put(circle(24, 16, 7), r, lambda px, py: 0 if px < 23 and py < 15 else 1 if px < 25 else 2)   # the lens
@item("resistor", "d8c08a")
def _(ic, r, c):
    leads = ic.ramp("c8ccd2"); ic.line(2, 29, 9, 22, leads[1]); ic.line(22, 9, 29, 2, leads[1])
    ic.put(poly(bar(16, 16, 26, 13, deg=-45, chamfer=4)), r, lambda px, py: 1 if px + py < 32 else 2)
    for (o, col) in ((-7, "c04a4a"), (-2, "3a3a42"), (3, "e8a070"), (8, "e8d44d")):
        k = ic.ramp(col)[2]
        for t in range(-5, 6): ic.px(16 + o * .707 + t * .707, 16 - o * .707 + t * .707, k)
@item("capacitor", "5a7ab0")
def _(ic, r, c):
    leads = ic.ramp("c8ccd2"); ic.put(rect(11, 24, 12, 30), leads, lambda px, py: 1); ic.put(rect(19, 24, 20, 30), leads, lambda px, py: 1)
    ic.put(union(rect(6, 6, 25, 24), ellipse(15.5, 6, 9.5, 3)), r, lambda px, py: 1 if px < 11 else 3 if px > 21 else 2)
    ic.put(rect(20, 7, 22, 23), ic.ramp("c8d0e0"), lambda px, py: 1)                # polarity stripe
@item("transistor", "3a3c44")
def _(ic, r, c):
    t = ic.ramp("b8bcc4"); ic.put(rect(7, 2, 24, 9), t, lambda px, py: 1); ic.put(circle(15.5, 5.5, 2), t, lambda px, py: 4, rim=False)   # metal tab
    ic.put(rect(7, 9, 24, 21), r, lambda px, py: 1 if px < 11 else 2)
    for x in (9, 15, 21): ic.put(rect(x, 21, x + 1, 30), t, lambda px, py: 1)
@item("microchip", "3f9a5c")
def _(ic, r, c):
    pins = ic.ramp("c8ccd2")
    for p in range(8, 25, 4):
        ic.put(rect(p, 2, p + 1, 5), pins, lambda px, py: 1); ic.put(rect(p, 27, p + 1, 30), pins, lambda px, py: 1)
        ic.put(rect(2, p, 5, p + 1), pins, lambda px, py: 1); ic.put(rect(27, p, 30, p + 1), pins, lambda px, py: 1)
    ic.put(rect(5, 5, 26, 26), ic.ramp("2a2b32"), lambda px, py: 1 if px + py < 24 else 2)
    ic.put(rect(11, 11, 20, 20), r, lambda px, py: 1 if px + py < 31 else 2)        # the die
@item("circuit-board", "3f9a5c")
def _(ic, r, c):
    ic.put(poly([(2, 10), (20, 3), (30, 21), (12, 29)]), r, lambda px, py: 1 if px + py < 28 else 2)
    cu = ic.ramp("e8a070")[1]
    ic.line(8, 12, 19, 8, cu); ic.line(10, 17, 24, 12, cu); ic.line(14, 23, 22, 20, cu); ic.line(19, 8, 22, 14, cu)
    ic.put(poly([(12, 12), (17, 10), (19, 14), (14, 16)]), ic.ramp("2a2b32"), lambda px, py: 2)
    ic.put(poly([(18, 19), (22, 17), (24, 20), (20, 22)]), ic.ramp("2a2b32"), lambda px, py: 2)
@item("control-unit", "5ab8ff")
def _(ic, r, c):
    b = ic.ramp("6e7080"); ic.put(rect(3, 4, 28, 27), b, lambda px, py: 1 if px + py < 18 else 2)
    ic.put(rect(6, 7, 25, 17), ic.ramp("1d2a36"), lambda px, py: 2)                 # display
    k = r[1]
    ic.line(8, 14, 12, 10, k); ic.line(12, 10, 16, 13, k); ic.line(16, 13, 22, 9, k)
    for (x, col) in ((7, "6dff9a"), (13, "ffd37f"), (19, "ff5a5a")): ic.put(rect(x, 21, x + 3, 24), ic.ramp(col), lambda px, py: 1)
@item("precision-component", "6dd0a0")
def _(ic, r, c):
    m = ic.ramp("b8bcc4")
    ic.put(union(circle(16, 16, 13), rect(13, 1, 18, 31)), m, lambda px, py: 1 if px + py < 30 else 2)
    ic.put(ring(16, 16, 9, 12.5), m, lambda px, py: 3, rim=False)
    for k in range(16):                                                              # knurled ring
        a = k * math.pi / 8; ic.px(16 + 11 * math.cos(a), 16 + 11 * math.sin(a), m[0])
    ic.put(circle(16, 16, 6), r, lambda px, py: 1 if px + py < 31 else 2)
