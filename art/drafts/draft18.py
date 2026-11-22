"""Lattice Drill, Algae Photobioreactor, Polymer Liquid Tank (with their moving layers)."""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from blocklib import *
sys.argv = sys.argv[:2]
exec(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "draft15.py"), encoding="utf-8").read().split("# ---------------- Air Separation Unit")[0].split("from blocklib import *")[1])

CYAN = "color C 8ae4ff\ncolor c 4fa7c7\n"

# ---------------- Lattice Drill: base with four arm sockets, -core: lattice core (turns), -head: drill head, -node: lattice node ----------------
g = new(); base(g)
for (cx, cy) in ((14, 14), (82, 14), (14, 82), (82, 82)):                            # corner pylons
    part(g, inpoly(octagon(cx, cy, 9)), cx, cy); fill(g, inpoly(diamond(cx, cy, 3.5)), 'c')
for (x0, y0, x1, y1) in ((40, 3, 56, 24), (40, 72, 56, 93), (3, 40, 24, 56), (72, 40, 93, 56)):   # arm sockets the heads swing out of
    part(g, inrect(x0, y0, x1, y1), (x0 + x1) / 2, (y0 + y1) / 2)
    fill(g, inrect(x0 + 4, y0 + 4, x1 - 4, y1 - 4), 'X')
    if x1 - x0 < y1 - y0: fill(g, inrect(47, y0 + 4, 48, y1 - 4), 'C')
    else: fill(g, inrect(x0 + 4, 47, x1 - 4, 48), 'C')
part(g, inpoly(octagon(48, 48, 22)), 48, 48)                                         # core housing
fill(g, inpoly(octagon(48, 48, 16)), 'X')
core = small(30)
for y in range(30):
    for x in range(30):
        px, py = x + .5 - 15, y + .5 - 15
        r = math.hypot(px, py)
        if abs(px) + abs(py) <= 13 and (abs(px) <= 2.5 or abs(py) <= 2.5 or abs(abs(px) - abs(py)) <= 1.5 and r > 5):
            g_ = 'A' if px - py > 0 else 'B'
            core[y][x] = g_
        if abs(px) + abs(py) <= 6: core[y][x] = 'C' if px - py > 0 else 'c'
        if abs(px) + abs(py) <= 2.5: core[y][x] = 'W'
paste(g, core, 48, 48)
write(g, "lattice-drill", BLOCK + CYAN + "color W e8fbff\nref laser-drill blast-drill")
write_small(core, "lattice-drill-core", "color A b0b9c0\ncolor B 6e7080\n" + CYAN + "color W e8fbff")
head = small(32)                                                                     # drill head: three carbide cutters round a cyan bit
for k in range(3):
    a = k * 2 * math.pi / 3
    for y in range(32):
        for x in range(32):
            px, py = x + .5 - 16, y + .5 - 16
            r, t = math.hypot(px, py), (math.atan2(py, px) - a) % (2 * math.pi)
            if 4 < r <= 12 and t < .55: head[y][x] = 'A' if t < .27 else 'B'
            if 10 <= r <= 12.5 and .1 < t < .55: head[y][x] = 'K'
for y in range(32):
    for x in range(32):
        r = math.hypot(x + .5 - 16, y + .5 - 16)
        if r <= 5: head[y][x] = 'M' if r > 3 else 'C'
write_small(head, "lattice-drill-head", "color A b0b9c0\ncolor B 6e7080\ncolor M 989aa4\ncolor K 343a4a\n" + CYAN)
node = small(32)                                                                     # lattice node: a small cross of struts
for y in range(32):
    for x in range(32):
        px, py = x + .5 - 16, y + .5 - 16
        if (abs(px) <= .9 and abs(py) <= 9) or (abs(py) <= .9 and abs(px) <= 9): node[y][x] = 'W'
        if abs(px) + abs(py) <= 3: node[y][x] = 'W'
write_small(node, "lattice-drill-node", "color W ffffff")

# ---------------- Algae Photobioreactor: five lit glass tubes (water + algae bubbles show), grow-light strips, CO2 sparger, harvest ----------------
g = new(); base(g)
part(g, inrect(8, 6, 88, 18), 48, 12)                                                # harvest header
fill(g, inrect(12, 10, 84, 14), 'X'); diag(g, inrect(13, 11, 83, 13), 'E', 'e', 48, 12)
part(g, inrect(8, 78, 88, 90), 48, 84)                                               # CO2 sparger manifold
fill(g, inrect(12, 82, 84, 86), 'X')
for x in range(16, 84, 6): fill(g, inrect(x, 83, x + 1, 85), 'Z')
for i, x0 in enumerate(range(18, 79, 13)):                                           # glass tubes
    part(g, inrect(x0 - 1, 18, x0 + 8, 78), x0 + 3.5, 48)
    window(g, inrect(x0 + 1, 21, x0 + 6, 75))
    if i < 4:                                                                        # grow-light strips between the tubes
        fill(g, inrect(x0 + 9, 24, x0 + 11, 72), 'X'); fill(g, inrect(x0 + 10, 25, x0 + 10, 71), 'L')
fill(g, inrect(84, 86, 87, 89), 'X'); fill(g, inrect(85, 87, 86, 88), 'L')
write(g, "algae-photobioreactor", BLOCK + "color E 8fc77a\ncolor e 5b8a4c\ncolor Z c8cfd8\ncolor L f0c8ff\nmask lights L\nref cultivator separator")
g = new()                                                                            # -algae: the culture greening the tubes while it grows
for x0 in range(18, 79, 13):
    fill(g, inrect(x0 + 1, 21, x0 + 6, 75), 'W')
    for y in range(24, 74, 7): fill(g, inrect(x0 + 2 + (y // 7) % 3, y, x0 + 3 + (y // 7) % 3, y + 1), '.')
write(g, "algae-photobioreactor-algae", BLOCK + "color W ffffff\non algae-photobioreactor")

# ---------------- Polymer Liquid Tank: banded polymer-lined tank, big sight window, four flanged connections ----------------
g = new(); base(g)
for (x0, y0, x1, y1) in ((40, 2, 56, 12), (40, 84, 56, 94), (2, 40, 12, 56), (84, 40, 94, 56)):   # connections
    part(g, inrect(x0, y0, x1, y1), (x0 + x1) / 2, (y0 + y1) / 2)
    fill(g, inrect(x0 + 3, y0 + 3, x1 - 3, y1 - 3), 'X')
for (cx, cy) in ((15, 15), (81, 15), (15, 81), (81, 81)):                            # support feet
    part(g, inrect(cx - 7, cy - 7, cx + 7, cy + 7), cx, cy); fill(g, incircle(cx, cy, 2), 'X')
part(g, incircle(48, 48, 36), 48, 48)                                                # tank shell
diag(g, ring(27, 31), 'T', 't')                                                      # polymer lining
for k in range(8):                                                                   # shell bands
    a = k * math.pi / 4 + math.pi / 8
    for r in range(31, 36):
        for w in (0, 1):
            x = int(48 + r * math.cos(a) - w * math.sin(a)); y = int(48 + r * math.sin(a) + w * math.cos(a)); g[y][x] = 'H'
fill(g, ring(25, 27), 'X')
window(g, incircle(48, 48, 25))
for (x0, y0, x1, y1) in ((28, 47, 68, 49), (47, 28, 49, 68)):                        # window bars
    part(g, both(incircle(48, 48, 25), inrect(x0, y0, x1, y1)), 48, 48)
write(g, "polymer-liquid-tank", BLOCK + "color T 9cb8a8\ncolor t 6d8a7a\nref liquid-tank liquid-container")
