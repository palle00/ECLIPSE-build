"""Casing Fabricator, Methane Pyrolysis Reactor, Assembler, Experimental Core Assembler, Circuit Assembler."""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from blocklib import *

ORANGE = "color O d96b68\ncolor P fdb380\n"

# ---------------- Casing Fabricator: stamping bed, sheet-stock rails; -top: gantry along the top edge ----------------
g = new(); base(g)
part(g, inrect(8, 10, 20, 86), 14, 48); part(g, inrect(76, 10, 88, 86), 82, 48)   # side rails
for y in range(14, 82, 6):                                                        # stacked sheet stock
    fill(g, inrect(10, y, 18, y + 3), 'U'); fill(g, inrect(10, y + 3, 18, y + 3), 'V')
    fill(g, inrect(78, y, 86, y + 3), 'U'); fill(g, inrect(78, y + 3, 86, y + 3), 'V')
part(g, inrect(23, 20, 73, 84), 48, 52)                                           # bed frame
fill(g, inrect(27, 24, 69, 80), 'X')
diag(g, inrect(29, 26, 67, 78), 'H', 'X', 48, 52)                                 # dark stamping bed (the shell is drawn here)
for (x0, y0, sx, sy) in ((27, 24, 1, 1), (69, 24, -1, 1), (27, 80, 1, -1), (69, 80, -1, -1)):   # orange corner markers
    fill(g, inpoly([(x0, y0), (x0 + 8 * sx, y0), (x0, y0 + 8 * sy)]), 'O')
    fill(g, inpoly([(x0, y0), (x0 + 5 * sx, y0), (x0, y0 + 5 * sy)]), 'P')
write(g, "casing-fabricator", BLOCK + ORANGE + "color U c3d0e6\ncolor V 8da1b8\nref constructor vault")
g = new()
part(g, inrect(4, 4, 91, 15), 48, 10)                                             # gantry beam
fill(g, inrect(6, 9, 89, 10), 'X')
part(g, inrect(38, 2, 57, 22), 48, 12)                                            # press head
fill(g, inrect(42, 17, 53, 20), 'X'); fill(g, inrect(44, 18, 51, 19), 'P')
write(g, "casing-fabricator-top", BLOCK + ORANGE + "ref constructor-top")

# ---------------- Methane Pyrolysis Reactor: dark reaction chamber, methane feed, gas and carbon zones ----------------
g = new(); base(g)
part(g, inpoly(octagon(46, 50, 31)), 46, 50)                                       # thick reactor walls
fill(g, inpoly(octagon(46, 50, 23)), 'X')
fill(g, minus(inpoly(octagon(46, 50, 21)), inpoly(octagon(46, 50, 17))), 'O')      # glowing chamber ring
fill(g, minus(inpoly(octagon(46, 50, 20)), inpoly(octagon(46, 50, 18))), 'P')
diag(g, inpoly(octagon(46, 50, 16)), 'H', 'X', 46, 50)                            # the dark reaction chamber
for (cx, cy) in ((24, 26), (68, 26), (24, 74), (68, 74)):                          # wall bolts
    fill(g, incircle(cx, cy, 2), 'X')
part(g, inrect(4, 44, 16, 56), 10, 50)                                            # methane feed pipe
fill(g, inrect(4, 47, 12, 53), 'N'); fill(g, inrect(4, 47, 12, 48), 'Z')
part(g, incircle(16, 50, 6), 16, 50); fill(g, incircle(16, 50, 2.5), 'N')        # feed valve
for (cx, cy) in ((80, 14), (88, 22), (80, 30)):                                    # hydrogen gas vents (top right); fans spin in them
    part(g, incircle(cx, cy, 6), cx, cy); fill(g, incircle(cx, cy, 4.5), 'X')
part(g, inpoly([(70, 70), (90, 70), (90, 90), (76, 90)]), 82, 80)                  # carbon black hopper (bottom right)
fill(g, inpoly([(74, 74), (86, 74), (86, 86), (78, 86)]), 'X')
for (cx, cy) in ((79, 78), (83, 81), (80, 83), (84, 77)): fill(g, inrect(cx, cy, cx, cy), 'K')
write(g, "methane-pyrolysis-reactor", BLOCK + ORANGE + "color N 9fb3c8\ncolor Z d8e4f0\ncolor K 2a2a30\nmask glow O P\nref surge-smelter separator")
fan = [['.'] * 12 for _ in range(12)]                                              # -fan: 12px vent fan, spun by the game
for k in range(3):
    a = k * 2 * math.pi / 3
    for y in range(12):
        for x in range(12):
            px, py = x + .5 - 6, y + .5 - 6
            r, t = math.hypot(px, py), math.atan2(py, px)
            if 1.2 < r <= 4.6 and (t - a) % (2 * math.pi) < .75: fan[y][x] = 'A' if (t - a) % (2 * math.pi) < .38 else 'B'
for y in range(12):
    for x in range(12):
        if math.hypot(x + .5 - 6, y + .5 - 6) <= 1.6: fan[y][x] = 'M'
with open(os.path.join(OUT, "methane-pyrolysis-reactor-fan.txt"), "w", newline="\n") as f:
    f.write("size 12 12\noutline 0\ncolor A b0b9c0\ncolor B 6e7080\ncolor M 989aa4\ngrid\n")
    for row in fan: f.write("".join(row) + "\n")
print("drafted methane-pyrolysis-reactor-fan")
for (cx, cy) in ((80, 14), (88, 22), (80, 30)):                                    # the fans at rest, for the build-menu icon
    for y in range(12):
        for x in range(12):
            if fan[y][x] != '.': g[cy - 6 + y][cx - 6 + x] = fan[y][x]
write(g, "methane-pyrolysis-reactor", BLOCK + ORANGE + "color N 9fb3c8\ncolor Z d8e4f0\ncolor K 2a2a30\nmask glow O P\nref surge-smelter separator")

# ---------------- Assembler: two robot arms over a work platform, component feed hoppers, output chute ----------------
g = new(); base(g)
for i, (cx, col) in enumerate(((20, 'Q'), (38, 'U'), (58, 'Q'), (76, 'U'))):        # component feed hoppers
    part(g, inrect(cx - 7, 8, cx + 7, 22), cx, 15)
    fill(g, inrect(cx - 4, 11, cx + 4, 19), 'X'); fill(g, inrect(cx - 2, 13, cx + 2, 17), col)
part(g, inpoly(octagon(48, 54, 20)), 48, 54)                                       # work platform
fill(g, inpoly(octagon(48, 54, 14)), 'X')
diag(g, inrect(40, 46, 56, 62), 'V', 'W', 48, 54)                                 # the part being assembled
fill(g, inrect(44, 50, 52, 58), 'X'); fill(g, incircle(48, 54, 2.5), 'W')
arms = {}
for sx in (1, -1):                                                                # robot arms: bases here, arms swing (-arm-l / -arm-r)
    bx = 48 + 33 * sx
    part(g, incircle(bx, 70, 8), bx, 70); fill(g, incircle(bx, 70, 3), 'X')        # arm base
    a = arms[sx] = new()
    seg = inpoly([(bx - 3, 66), (bx + 3, 66), (48 + 15 * sx + 3, 44), (48 + 15 * sx - 3, 44)])
    part(a, seg, (bx + 48 + 15 * sx) / 2, 55)
    part(a, incircle(48 + 15 * sx, 44, 4), 48 + 15 * sx, 44)
    fill(a, incircle(48 + 11 * sx, 47, 2), 'P')                                   # tool tip
    fill(a, incircle(bx, 70, 3), 'M'); fill(a, incircle(bx, 70, 1.5), 'X')         # shoulder pin
part(g, inrect(40, 78, 56, 90), 48, 84); fill(g, inrect(43, 81, 53, 90), 'X')      # output chute
write(g, "assembler", BLOCK + ORANGE + "color Q d99d73\ncolor U c3d0e6\ncolor V b8c2d0\ncolor W 7d8798\nmask glow Q U\nref multi-press disassembler")
write(arms[-1], "assembler-arm-l", BLOCK + ORANGE + "on assembler")
write(arms[1], "assembler-arm-r", BLOCK + ORANGE + "on assembler")

# ---------------- Experimental Core Assembler: enclosed cradle with a purple core, four precision arms ----------------
g = new(); base(g)
for (cx, cy) in ((14, 14), (82, 14), (14, 82), (82, 82)):                          # material feed ports
    part(g, inpoly(octagon(cx, cy, 8)), cx, cy); fill(g, inpoly(octagon(cx, cy, 4)), 'X')
    fill(g, inrect(cx - 1, cy - 1, cx, cy), 'P')
part(g, inpoly(octagon(48, 48, 32)), 48, 48)                                       # enclosure
groove(g, minus(inpoly(octagon(48, 48, 27)), inpoly(octagon(48, 48, 25))))
fill(g, inpoly(octagon(48, 48, 22)), 'X')                                          # enclosed centre
diag(g, inpoly(octagon(48, 48, 20)), 'H', 'X', 48, 48)
for k in range(4):                                                                # precision arms converging on the core
    a = math.pi / 4 + k * math.pi / 2
    ca, sa = math.cos(a), math.sin(a)
    def pt(r, s): return (48 + ca * r - sa * s, 48 + sa * r + ca * s)
    part(g, inpoly([pt(10, -3), pt(30, -4), pt(30, 4), pt(10, 3)]), *pt(20, 0))
    tx, ty = pt(10, 0); fill(g, incircle(tx, ty, 1.6), 'O')
for (x0, y0, x1, y1) in ((44, 30, 52, 34), (44, 62, 52, 66), (30, 44, 34, 52), (62, 44, 66, 52)):   # cradle prongs
    part(g, inrect(x0, y0, x1, y1), (x0 + x1) / 2, (y0 + y1) / 2)
fill(g, inpoly(octagon(48, 48, 8)), 'X')
write(g, "experimental-core-assembler", BLOCK + ORANGE + "mask glow O\nref disassembler surge-smelter")
g = new()                                                                          # -rotator: the experimental core turns in its cradle
diag(g, inpoly(diamond(48, 48, 7)), 'R', 'S', 48, 48)
fill(g, inpoly(diamond(48, 48, 3)), 'S'); fill(g, inrect(49, 44, 50, 45), 'T')
write(g, "experimental-core-assembler-rotator", BLOCK + "color R c88fff\ncolor S 8a5ad1\ncolor T f0e0ff\non experimental-core-assembler")

# ---------------- Circuit Assembler: acid-bath window with a print gantry, circuit board output; -bottom ----------------
g = new(); base(g)
part(g, inrect(14, 10, 82, 64), 48, 37)                                           # bath frame
for y in range(17, 58):                                                           # the window: the acid tile shows here
    for x in range(20, 76): g[y][x] = '.'
part(g, inrect(14, 34, 82, 40), 48, 37)                                           # print gantry over the bath
fill(g, inrect(18, 37, 78, 37), 'X')                                             # the print head's track (-head slides on it)
part(g, inrect(14, 68, 82, 88), 48, 78)                                           # output tray
fill(g, inrect(22, 71, 74, 85), 'C'); fill(g, inrect(22, 71, 74, 72), 'D')        # circuit board
for (x0, y0, x1, y1) in ((26, 75, 44, 76), (44, 75, 45, 81), (50, 78, 70, 79), (30, 81, 40, 82)):
    fill(g, inrect(x0, y0, x1, y1), 'Q')                                          # copper traces
fill(g, inrect(58, 73, 63, 77), 'X'); fill(g, inrect(30, 77, 33, 80), 'X')        # chips
write(g, "circuit-assembler", BLOCK + ORANGE + "color C 3f7f4f\ncolor D 5aa36a\ncolor Q d99d73\nmask glow Q\nref silicon-smelter plastanium-compressor")
g = new()                                                                          # -head: print head, slides along the gantry
part(g, inrect(43, 30, 53, 44), 48, 37); fill(g, inrect(46, 42, 50, 44), 'P'); fill(g, inrect(47, 33, 49, 35), 'X')
write(g, "circuit-assembler-head", BLOCK + ORANGE + "on circuit-assembler")
g = new()
diag(g, inrect(0, 0, 95, 95), 'H', 'X', 48, 48)
for x in range(8, 90, 8): fill(g, inrect(x, 4, x, 91), 'K')
write(g, "circuit-assembler-bottom", BLOCK + "color K 3a3b42")
