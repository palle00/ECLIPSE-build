"""Blast Furnace, Electric Furnace, Cooling Tower, Electrolyzer, Pressure Reactor (with their moving layers)."""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from blocklib import *

ORANGE = "color O d96b68\ncolor P fdb380\n"
BRICK = "color R 8a5a4a\ncolor S 6b4538\n"

def ring(r0, r1, cx=48, cy=48): return lambda px, py: r0 <= math.hypot(px - cx, py - cy) <= r1
def window(g, test): fill(g, test, '.')
def bricks(g, r0, r1, n, cx=48, cy=48):
    """Refractory brick ring: alternating brick tones with dark mortar joints."""
    for y in range(H):
        for x in range(W):
            px, py = x + .5 - cx, y + .5 - cy
            r = math.hypot(px, py)
            if not (r0 <= r <= r1): continue
            k = (math.atan2(py, px) % (2 * math.pi)) / (2 * math.pi) * n
            g[y][x] = 'X' if k % 1 < .12 else ('R' if int(k) % 2 == 0 else 'S')

# ---------------- Blast Furnace: hot-blast stoves, bustle pipe with tuyeres, glowing mouth; -rotator: the charging chute ----------------
g = new(); base(g)
for (cx, cy) in ((15, 15), (15, 81)):                                                # hot-blast stoves
    part(g, incircle(cx, cy, 10), cx, cy)
    groove(g, ring(5.5, 6.5, cx, cy), cx, cy); fill(g, incircle(cx, cy, 2.5), 'X')
part(g, inrect(11, 24, 19, 72), 15, 48)                                              # hot-blast main from the stoves
part(g, inrect(4, 44, 16, 52), 10, 48)
part(g, incircle(81, 15, 9), 81, 15); fill(g, incircle(81, 15, 5), 'X')             # gas uptake
part(g, inrect(42, 80, 54, 93), 48, 86)                                              # tap-hole runner
fill(g, inrect(45, 80, 51, 93), 'X'); fill(g, inrect(47, 80, 49, 93), 'O')
part(g, ring(29, 35), 48, 48)                                                        # bustle pipe
groove(g, ring(31.5, 32.5))
for k in range(8):                                                                   # tuyeres into the furnace
    a = k * math.pi / 4 + math.pi / 8
    ca, sa = math.cos(a), math.sin(a)
    def pt(r, s): return (48 + ca * r - sa * s, 48 + sa * r + ca * s)
    part(g, inpoly([pt(24, -2.5), pt(30, -3), pt(30, 3), pt(24, 2.5)]), *pt(27, 0))
part(g, incircle(48, 48, 27), 48, 48)                                                # furnace shell
bricks(g, 19, 24, 20)                                                                # refractory lining
fill(g, incircle(48, 48, 18.5), 'X')                                                 # the mouth: burden glowing through
for (cx, cy, r) in ((44, 44, 7), (53, 50, 6), (46, 54, 5), (52, 41, 4)):
    fill(g, incircle(cx, cy, r), 'O')
for (cx, cy, r) in ((44, 44, 3.5), (53, 50, 3), (46, 54, 2), (52, 41, 1.6)):
    fill(g, incircle(cx, cy, r), 'P')
write(g, "blast-furnace", BLOCK + ORANGE + BRICK + "mask glow O P\nref smelter silicon-crucible")
g = new()
diag(g, inpoly([(46, 48), (50, 48), (52, 31), (44, 31)]), "A", "B", 48, 40, "M")       # charging chute (turns over the burden)
fill(g, inrect(45, 31, 51, 32), "X")
part(g, incircle(48, 48, 4.5), 48, 48); fill(g, incircle(48, 48, 1.8), 'X')
write(g, "blast-furnace-rotator", BLOCK + "on blast-furnace")

# ---------------- Electric Furnace: molten bath, three graphite electrodes on boom arms, power cables ----------------
g = new(); base(g)
part(g, inrect(6, 8, 24, 30), 15, 19)                                               # transformer
for y in range(12, 28, 4): fill(g, inrect(9, y, 21, y + 1), 'X')
part(g, inrect(6, 66, 24, 88), 15, 77); fill(g, inrect(10, 70, 20, 84), 'X'); fill(g, inrect(12, 72, 18, 82), 'q')   # tapping spout
part(g, incircle(44, 48, 29), 44, 48)                                                # furnace shell
groove(g, ring(25.5, 26.5, 44, 48))
bricks(g, 20, 24.5, 24, 44, 48)
fill(g, incircle(44, 48, 19.5), 'X')                                                 # molten bath
diag(g, incircle(44, 48, 15), 'P', 'O', 44, 48)
fill(g, incircle(44, 48, 6), 'P')
part(g, inrect(78, 24, 92, 72), 85, 48)                                              # electrode mast
fill(g, inrect(81, 28, 89, 68), 'X'); fill(g, inrect(83, 30, 87, 66), 'G')
els = [(49, 40), (38, 48), (49, 56)]                                                 # electrodes in a triangle, arms run parallel from the mast
for (ex, ey) in els:
    part(g, inrect(ex, ey - 3, 80, ey + 3), (ex + 80) / 2, ey)
    fill(g, inrect(ex + 6, ey, 79, ey), 'q')                                         # power cable along the arm
for (ex, ey) in els:                                                                 # graphite electrodes
    fill(g, incircle(ex, ey, 5.5), 'M'); fill(g, incircle(ex, ey, 4.2), 'K'); fill(g, incircle(ex - .8, ey - .8, 1.6), 'J')
write(g, "electric-furnace", BLOCK + ORANGE + BRICK + "color Q d99d73\ncolor q a8664c\ncolor K 3a3b44\ncolor J 55565f\nmask glow O P\nref arc-furnace silicon-smelter")

# ---------------- Cooling Tower: fan stack over a water basin ring, spray headers, water in/out; -rotator: the fan ----------------
g = new(); base(g)
for (cx, cy) in ((17, 17), (79, 17), (17, 79), (79, 79)):                            # spray distribution headers
    part(g, inrect(cx - 11, cy - 11, cx + 11, cy + 11), cx, cy)
    for k in range(-6, 7, 4):
        fill(g, inrect(cx - 7, cy + k, cx + 7, cy + k + 1), 'X')
part(g, inrect(3, 42, 16, 54), 9, 48); fill(g, inrect(3, 46, 12, 50), 'N')          # warm water in
part(g, inrect(80, 42, 93, 54), 87, 48); fill(g, inrect(84, 46, 93, 50), 'N')       # chilled water out
part(g, incircle(48, 48, 32), 48, 48)                                                # basin wall
window(g, ring(23.5, 28.5))                                                          # the basin: chilled water shows here
for k in range(8):                                                                   # distribution arms over the basin
    a = k * math.pi / 4
    ca, sa = math.cos(a), math.sin(a)
    arm = lambda px, py, ca=ca, sa=sa: abs((px - 48) * -sa + (py - 48) * ca) <= 1.6 and 23 <= (px - 48) * ca + (py - 48) * sa <= 29
    diag(g, arm, 'M', 'G')
part(g, ring(18.5, 23), 48, 48)                                                      # fan stack
fill(g, incircle(48, 48, 18.5), 'X')
diag(g, incircle(48, 48, 17), 'H', 'X')
fill(g, inrect(84, 84, 88, 87), 'X'); fill(g, inrect(85, 85, 87, 86), 'L')          # status lamp
write(g, "cooling-tower", BLOCK + "color N 9fb3c8\ncolor L bfe6f2\nmask glow L\nref cultivator water-extractor")
g = new()
for k in range(5):                                                                   # five-blade fan
    a = k * 2 * math.pi / 5
    for y in range(H):
        for x in range(W):
            px, py = x + .5 - 48, y + .5 - 48
            r, t = math.hypot(px, py), (math.atan2(py, px) - a) % (2 * math.pi)
            if 4 < r <= 16.5 and t < .5: g[y][x] = 'A' if t < .25 else 'B'
part(g, incircle(48, 48, 4.5), 48, 48); fill(g, incircle(48, 48, 1.8), 'X')
write(g, "cooling-tower-rotator", BLOCK + "on cooling-tower")

# ---------------- Electrolyzer: three cells with electrode plates, copper busbar, separate H2 / O2 gas domes ----------------
g = new(); base(g)
part(g, inrect(12, 9, 84, 19), 48, 14)                                               # copper busbar
diag(g, inrect(14, 12, 82, 16), 'Q', 'q', 48, 14)
for (cx, col) in ((22, 'L'), (74, 'L')):
    fill(g, inrect(cx - 2, 3, cx + 1, 6), 'X'); fill(g, inrect(cx - 1, 4, cx, 5), col)   # power lamps
for x0 in (16, 39, 62):                                                              # electrolysis cells
    part(g, inrect(x0, 22, x0 + 18, 72), x0 + 9, 47)
    window(g, inrect(x0 + 3, 25, x0 + 15, 69))
    for i, dx in enumerate((4, 8, 12)):                                              # electrode plates (anode copper, cathode steel)
        c1, c2 = ('Q', 'q') if i % 2 == 0 else ('A', 'B')
        fill(g, inrect(x0 + dx, 25, x0 + dx, 69), c1); fill(g, inrect(x0 + dx + 1, 25, x0 + dx + 1, 69), c2)
        fill(g, inrect(x0 + dx, 19, x0 + dx + 1, 24), 'q')                         # hangers from the busbar
for (cx, c1, c2) in ((30, 'N', 'n'), (66, 'W', 'w')):                               # gas domes: hydrogen left, oxygen right
    part(g, inrect(cx - 13, 76, cx + 13, 91), cx, 83)
    fill(g, inrect(cx - 1, 72, cx + 1, 76), 'G')
    diag(g, both(incircle(cx, 84, 8), inrect(0, 0, 95, 95)), c1, c2, cx, 84)
    fill(g, incircle(cx, 84, 2.5), 'X')
write(g, "electrolyzer", BLOCK + "color Q d99d73\ncolor q a8664c\ncolor N d8e4f0\ncolor n 9fb3c8\ncolor W bfe6f2\ncolor w 7fb8cc\ncolor L 8fd8ff\nmask glow L\nref electrolyzer separator")

# ---------------- Pressure Reactor: thick sealed vessel with a sight glass, gas inlets, catalyst hatch, pressure gauge ----------------
g = new(); base(g)
for (x0, y0, x1, y1) in ((3, 42, 20, 54), (42, 3, 54, 20)):                          # gas inlets
    part(g, inrect(x0, y0, x1, y1), (x0 + x1) / 2, (y0 + y1) / 2)
for (x0, y0, x1, y1) in ((8, 40, 11, 56), (40, 8, 56, 11)):                          # inlet flanges
    part(g, inrect(x0, y0, x1, y1), (x0 + x1) / 2, (y0 + y1) / 2)
part(g, inrect(6, 70, 28, 90), 17, 80)                                               # catalyst service hatch
fill(g, inrect(10, 74, 24, 86), 'X'); diag(g, inrect(12, 77, 22, 83), 'P', 'O', 17, 80)
part(g, incircle(80, 16, 10), 80, 16)                                                # pressure gauge (the needle swings)
fill(g, incircle(80, 16, 7), 'X'); fill(g, incircle(80, 16, 6), 'Z')
for k in range(5):
    a = math.pi * (1.25 - k * .375)
    fill(g, inrect(80 + 5 * math.cos(a), 16 - 5 * math.sin(a), 80 + 5 * math.cos(a), 16 - 5 * math.sin(a)), 'H')
fill(g, inrect(86, 26, 89, 29), 'X'); fill(g, inrect(87, 27, 88, 28), 'L')          # pressure lamp
part(g, incircle(48, 48, 31), 48, 48)                                                # the vessel
groove(g, ring(24.5, 25.5))
diag(g, incircle(48, 48, 24), 'M', 'G')
groove(g, ring(15, 16))
fill(g, incircle(48, 48, 11), 'X')                                                   # sight glass: the reaction shows here
window(g, incircle(48, 48, 8.5))
write(g, "pressure-reactor", BLOCK + ORANGE + "color Z dfe3e8\ncolor L ff8a6a\nmask glow L\nref separator plastanium-compressor")
g = new()                                                                            # -press: the clamp ring, bears down at the end of each batch
part(g, ring(17, 21), 48, 48)                                                        # lid flange
for k in range(12):
    a = k * math.pi / 6
    fill(g, incircle(48 + 19 * math.cos(a), 48 + 19 * math.sin(a), 1.1), 'X')       # flange bolts
for k in range(4):                                                                   # four hydraulic clamps
    a = k * math.pi / 2 + math.pi / 4
    ca, sa = math.cos(a), math.sin(a)
    def pt(r, s): return (48 + ca * r - sa * s, 48 + sa * r + ca * s)
    part(g, inpoly([pt(16, -5), pt(35, -7), pt(37, 0), pt(35, 7), pt(16, 5)]), *pt(26, 0))
    groove(g, inpoly([pt(24, -4), pt(25, -4), pt(25, 4), pt(24, 4)]))
    bx, by = pt(31, 0); fill(g, incircle(bx, by, 2.2), 'X'); fill(g, incircle(bx, by, 1), 'M')
write(g, "pressure-reactor-press", BLOCK + "on pressure-reactor")
g = new()                                                                            # -needle: swings about the gauge centre
for t in range(6):
    g[16 - t][80] = 'K'; g[16 - t][79] = 'K' if t < 2 else g[16 - t][79]
fill(g, incircle(80, 16, 1.5), 'X')
write(g, "pressure-reactor-needle", BLOCK + "color K c23b3b\non pressure-reactor")
