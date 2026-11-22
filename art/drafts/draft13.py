"""Distillation Column, Heat Exchanger, Steam Cracker, Carbon Fiber Furnace, Reprocessing Plant."""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from blocklib import *

ORANGE = "color O d96b68\ncolor P fdb380\n"

def window(g, test):
    """See-through window: the block's -bottom layer and liquid tile show here."""
    fill(g, test, '.')

# ---------------- Distillation Column: tray column (oil shows between the trays), reflux drums, three outlets, sulfur unit ----------------
g = new(); base(g)
for (x0, y0, x1, y1) in ((44, 3, 56, 18), (44, 78, 56, 93), (78, 42, 93, 54)):   # outlet flanges: naphtha up, methane down, diesel right
    part(g, inrect(x0, y0, x1, y1), (x0 + x1) / 2, (y0 + y1) / 2)
fill(g, inrect(47, 4, 53, 8), 'X'); fill(g, inrect(47, 88, 53, 92), 'X'); fill(g, inrect(88, 45, 92, 51), 'X')
for (cx, cy) in ((80, 17), (80, 79)):                                                # reflux drums
    part(g, incircle(cx, cy, 10), cx, cy)
    groove(g, minus(incircle(cx, cy, 6), incircle(cx, cy, 5)), cx, cy)
    fill(g, incircle(cx, cy, 2.2), 'X')
part(g, inrect(5, 28, 23, 68), 14, 48)                                               # sulfur recovery unit (Claus)
fill(g, inrect(8, 32, 20, 52), 'X')
diag(g, inrect(9, 33, 19, 51), 'H', 'X', 14, 42)
for (x, y) in ((10, 44), (14, 46), (12, 48), (16, 43), (11, 40), (15, 49), (17, 47)):  # sulfur pellets
    fill(g, inrect(x, y, x + 1, y + 1), 'Y'); fill(g, inrect(x + 1, y + 1, x + 1, y + 1), 'Z')
fill(g, inrect(8, 46, 20, 52), 'Y'); fill(g, inrect(8, 50, 20, 52), 'Z')
fill(g, inrect(10, 57, 18, 62), 'X'); fill(g, inrect(11, 58, 17, 61), 'L')         # status lamp (glows in the sulfur colour)
part(g, inpoly(octagon(50, 48, 27)), 50, 48)                                        # the column shell
groove(g, minus(inpoly(octagon(50, 48, 23)), inpoly(octagon(50, 48, 22))), 50, 48)
tray = inpoly(octagon(50, 48, 19))                                                   # top sieve tray: oil shows through the holes
diag(g, tray, 'M', 'G', 50, 48)
for y in range(36, 62, 6):
    for x in range(41 + (y // 6 % 2) * 3, 66, 6):
        if tray(x - 1.5, y - 1.5) and tray(x + 2.5, y + 2.5) and x > 39: fill(g, inrect(x - 1, y - 1, x + 1, y + 1), '.')
window(g, both(tray, inrect(30, 30, 37, 66)))                                        # downcomer: the open segment at the tray's edge
fill(g, both(tray, inrect(38, 30, 39, 66)), 'X')                                     # its weir
write(g, "distillation-column", BLOCK + "color Y e8d44d\ncolor Z b8a334\ncolor L f4e88a\nmask glow L\nref separator cultivator")

# ---------------- Heat Exchanger: finned core fed by four broad channels; -top1/-top2: the output collector (turns with the block) ----------------
g = new(); base(g)
for (x0, y0, x1, y1) in ((5, 39, 30, 57), (39, 5, 57, 30), (39, 66, 57, 91), (66, 39, 91, 57)):   # collection channels
    part(g, inrect(x0, y0, x1, y1), (x0 + x1) / 2, (y0 + y1) / 2)
    vertical = x1 - x0 < y1 - y0
    fill(g, inrect(x0 + 3, y0 + 3, x1 - 3, y1 - 3) if not vertical else inrect(x0 + 3, y0 + 3, x1 - 3, y1 - 3), 'X')
    if vertical:                                                                     # heat-pipe fins across the channel
        for y in range(y0 + 5, y1 - 3, 4): diag(g, inrect(x0 + 4, y, x1 - 4, y + 1), 'F', 'E', (x0 + x1) / 2, y)
    else:
        for x in range(x0 + 5, x1 - 3, 4): diag(g, inrect(x, y0 + 4, x + 1, y1 - 4), 'F', 'E', x, (y0 + y1) / 2)
for (cx, cy) in ((18, 18), (78, 18), (18, 78), (78, 78)):                            # insulated corner pads
    part(g, inrect(cx - 10, cy - 10, cx + 10, cy + 10), cx, cy)
    groove(g, minus(inrect(cx - 6, cy - 6, cx + 6, cy + 6), inrect(cx - 5, cy - 5, cx + 5, cy + 5)), cx, cy)
    fill(g, incircle(cx, cy, 2), 'X')
part(g, inpoly(octagon(48, 48, 22)), 48, 48)                                        # exchanger core
fill(g, inpoly(octagon(48, 48, 16)), 'X')
for x in range(34, 63, 4):                                                           # fins
    diag(g, both(inpoly(octagon(48, 48, 15)), inrect(x, 32, x + 1, 64)), 'F', 'E', 48, 48)
fill(g, inrect(44, 44, 51, 51), 'X'); diag(g, inrect(45, 45, 50, 50), 'P', 'O')    # hot core
write(g, "heat-exchanger", BLOCK + ORANGE + "color F 989aa4\ncolor E 757887\nmask core F E O P\nref heat-redirector heat-router")
top = new()
part(top, inpoly([(64, 37), (78, 33), (93, 33), (93, 63), (78, 63), (64, 59)]), 80, 48)
fill(top, inrect(85, 40, 93, 56), 'X'); fill(top, inrect(85, 40, 86, 56), 'O')     # outlet slot (the heat glow sits here)
for x0 in (68, 75):                                                                  # chevrons point out of the front
    for i in range(5):
        fill(top, inrect(x0 + i, 43 + i, x0 + i + 1, 43 + i), 'P'); fill(top, inrect(x0 + i, 53 - i, x0 + i + 1, 53 - i), 'O')
write(top, "heat-exchanger-top1", BLOCK + ORANGE + "on heat-exchanger")
write(top[::-1], "heat-exchanger-top2", BLOCK + ORANGE + "on heat-exchanger")

# ---------------- Steam Cracker: radiant coil over the naphtha bath, burner row, heavy thermal supports either side ----------------
g = new(); base(g)
for x0 in (5, 79):                                                                   # thermal support beams
    part(g, inrect(x0, 7, x0 + 12, 89), x0 + 6, 48)
    for y in range(17, 85, 11):
        groove(g, inrect(x0 + 2, y, x0 + 10, y), x0 + 6, y)
        fill(g, incircle(x0 + 6.5, y - 5, 1.6), 'X')
part(g, inrect(20, 7, 76, 89), 48, 48)                                               # furnace housing
fill(g, inrect(24, 13, 72, 68), 'X')
window(g, inrect(26, 15, 70, 66))
xs = list(range(29, 70, 8))                                                          # radiant tube coil (serpentine)
for i, x in enumerate(xs):                                                           # metal tubes, glowing in the hot zone over the burners
    fill(g, inrect(x, 19, x, 47), 'B'); fill(g, inrect(x + 1, 19, x + 2, 47), 'M'); fill(g, inrect(x + 3, 19, x + 3, 47), 'A')
    fill(g, inrect(x, 48, x, 62), 'O'); fill(g, inrect(x + 1, 48, x + 3, 62), 'P')
    if i + 1 < len(xs):
        y = 17 if i % 2 == 0 else 61
        if y < 40:
            fill(g, inrect(x, y, xs[i + 1] + 3, y), 'A'); fill(g, inrect(x, y + 1, xs[i + 1] + 3, y + 2), 'M')
            fill(g, inrect(x + 1, y + 3, xs[i + 1] + 2, y + 3), 'B'); fill(g, inrect(x, y + 3, x, y + 3), 'B')
        else: fill(g, inrect(x, y, xs[i + 1] + 3, y + 2), 'P'); fill(g, inrect(x, y + 3, xs[i + 1] + 3, y + 3), 'O')
fill(g, inrect(26, 72, 70, 84), 'X')                                                 # burner row under the firebox
for x in range(29, 68, 8):
    diag(g, inrect(x, 74, x + 5, 82), 'M', 'H', x + 2.5, 78); fill(g, inrect(x + 2, 77, x + 3, 79), 'P')
fill(g, inrect(40, 9, 56, 11), 'X'); fill(g, inrect(41, 10, 55, 10), 'L')           # product lamp (glows ethylene green)
write(g, "steam-cracker", BLOCK + ORANGE + "color L dff5ec\nmask glow L\nmask flame O P\nref silicon-crucible separator")

# ---------------- Carbon Fiber Furnace: white precursor spool -> insulated hot tunnel -> black carbon fibre spool ----------------
g = new(); base(g)
for (x0, y0, x1, y1, hole) in ((41, 4, 55, 18, 'N'), (41, 78, 55, 92, 'X')):         # nitrogen purge (top), flue vent (bottom)
    part(g, inrect(x0, y0, x1, y1), (x0 + x1) / 2, (y0 + y1) / 2)
    fill(g, inrect(x0 + 3, y0 + 3, x1 - 3, y1 - 3), 'X')
    if hole == 'N': fill(g, inrect(x0 + 5, y0 + 5, x1 - 5, y1 - 5), 'N')
    else:
        for x in range(x0 + 4, x1 - 2, 3): fill(g, inrect(x, y0 + 4, x, y1 - 4), 'H')
part(g, inrect(26, 21, 70, 75), 48, 48)                                              # insulated tunnel casing
for x in range(30, 68, 5):                                                           # insulation ribs, top and bottom bands
    groove(g, inrect(x, 24, x, 29)); groove(g, inrect(x, 67, x, 72))
for (x0, x1) in ((26, 33), (63, 70)):                                                # heavy end clamps
    part(g, inrect(x0, 19, x1, 77), (x0 + x1) / 2, 48)
fill(g, inrect(34, 32, 62, 64), 'X')                                                 # the hot section
for x in range(37, 60, 6):                                                           # heater elements
    diag(g, inrect(x, 34, x + 2, 62), 'P', 'O', x + 1, 48)
for y in (43, 48, 53):                                                               # fibre tows: white precursor in, black carbon fibre out
    fill(g, inrect(26, y, 47, y), 'W'); fill(g, inrect(48, y, 70, y), 'K')
    fill(g, inrect(26, y + 1, 47, y + 1), 'V'); fill(g, inrect(48, y + 1, 70, y + 1), 'J')
for (x0, x1) in ((26, 33), (63, 70)):                                                # guide rollers in the clamps
    for y in (43, 48, 53): fill(g, inrect(x0 + 2, y - 1, x1 - 2, y + 2), 'X'); fill(g, inrect(x0 + 3, y, x1 - 3, y), 'M')
for (cx, cy) in ((16, 48), (80, 48)):                                               # spool sockets (the spools turn on top)
    fill(g, incircle(cx, cy, 13), 'X')
write(g, "carbon-fiber-furnace", BLOCK + ORANGE + "color N 9fb3c8\ncolor W e3ecf0\ncolor V aab4bc\ncolor K 3a3b44\ncolor J 22232a\nmask glow O P\nref silicon-smelter kiln")
SPOOL = "size 26 26\noutline 0\ncolor A b0b9c0\ncolor B 6e7080\ncolor M 989aa4\ncolor G 757887\ncolor H 5c5e6c\ncolor X 4a4b53\ncolor Q fdb380\ncolor W e3ecf0\ncolor V aab4bc\ncolor K 3a3b44\ncolor J 22232a\n"
for name, c1, c2 in (("carbon-fiber-furnace-spool-in", 'W', 'V'), ("carbon-fiber-furnace-spool-out", 'K', 'J')):
    s = [['.'] * 26 for _ in range(26)]
    for y in range(26):
        for x in range(26):
            px, py = x + .5 - 13, y + .5 - 13
            r, t = math.hypot(px, py), math.atan2(py, px)
            if r > 12.2: continue
            if r > 9.6: s[y][x] = 'A' if px - py > 0 else 'B'                       # flange rim
            elif r > 4.2: s[y][x] = c1 if (int(r * 1.6) % 2 == 0) else c2              # wound fibre
            elif r > 2.2: s[y][x] = 'X' if (t % (2 * math.pi / 3)) < .5 else 'M'        # hub with three spokes
            else: s[y][x] = 'H'
    for y in range(26):                                                           # bright notch on the rim: shows the turn
        for x in range(26):
            px, py = x + .5 - 13, y + .5 - 13
            if 9.8 < math.hypot(px, py) <= 12.2 and abs(math.atan2(py, px) + math.pi / 2) < .22: s[y][x] = 'Q'
    with open(os.path.join(OUT, name + ".txt"), "w", newline="\n") as f:
        f.write(SPOOL + "grid\n")
        for row in s: f.write("".join(row) + "\n")
    print("drafted", name)
    cx = 16 if name.endswith("in") else 80                                         # the spool at rest, for the build-menu icon
    for y in range(26):
        for x in range(26):
            if s[y][x] != '.': g[48 - 13 + y][cx - 13 + x] = s[y][x]
write(g, "carbon-fiber-furnace", BLOCK + ORANGE + "color N 9fb3c8\ncolor W e3ecf0\ncolor V aab4bc\ncolor K 3a3b44\ncolor J 22232a\ncolor Q fdb380\nmask glow O P\nref silicon-smelter kiln")

# ---------------- Reprocessing Plant: dissolver (nitric acid shows, agitator turns), extraction stages, filter bank ----------------
g = new(); base(g)
fill(g, inrect(18, 12, 76, 14), 'X'); fill(g, inrect(18, 12, 76, 12), 'G')          # stage manifold
for cx in (14, 32, 50):                                                              # sealed extraction stages
    part(g, incircle(cx, 13, 8), cx, 13)
    fill(g, incircle(cx, 13, 4), 'X'); fill(g, inrect(cx - 1, 12, cx, 13), 'L')
for y0 in (8, 29, 50, 71):                                                           # filter bank
    part(g, inrect(77, y0, 90, y0 + 17), 83.5, y0 + 8.5)
    for y in range(y0 + 3, y0 + 16, 3): groove(g, inrect(80, y, 87, y), 83.5, y)
for (x0, y0, x1, y1) in ((44, 18, 52, 26), (70, 44, 78, 52), (44, 70, 52, 78), (10, 44, 26, 52)):   # transfer pipes
    diag(g, inrect(x0, y0, x1, y1), 'M', 'G', (x0 + x1) / 2, (y0 + y1) / 2); fill(g, inrect(x0, y0, x1, y0) if x1 - x0 > y1 - y0 else inrect(x0, y0, x0, y1), 'A')
part(g, incircle(12, 48, 7), 12, 48); fill(g, incircle(12, 48, 3), 'X'); fill(g, inrect(11, 47, 12, 48), 'Y')   # nitric acid inlet valve
fill(g, inrect(52, 76, 66, 78), 'G'); fill(g, inrect(64, 76, 66, 82), 'G')           # line to the thorium chute
part(g, inpoly(octagon(48, 48, 25)), 48, 48)                                         # dissolver vessel
groove(g, minus(inpoly(octagon(48, 48, 21)), inpoly(octagon(48, 48, 20))), 48, 48)
window(g, inpoly(octagon(48, 48, 17)))
part(g, inrect(5, 76, 27, 91), 16, 83.5)                                             # spent fuel input hatch
fill(g, inrect(8, 80, 24, 87), 'X'); diag(g, inrect(10, 82, 22, 85), 'S', 'T', 16, 83.5)
part(g, inrect(66, 79, 74, 91), 70, 85); fill(g, inrect(68, 82, 72, 91), 'X')         # thorium output chute
for (x, y) in ((68, 86), (70, 84), (71, 88), (69, 89)):
    fill(g, inrect(x, y, x + 1, y + 1), 'R'); fill(g, inrect(x + 1, y + 1, x + 1, y + 1), 'Q')
write(g, "reprocessing-plant", BLOCK + "color Y f0d98c\ncolor S 7f9a83\ncolor T 54685a\ncolor R f9a3c7\ncolor Q c27a9b\ncolor L f9c6db\nmask glow L\nref separator disassembler")
g = new()                                                                            # agitator (the game turns it about the block centre)
fill(g, incircle(48, 48, 5), 'H')
for k in range(3):
    a = k * 2 * math.pi / 3 + .3
    ca, sa = math.cos(a), math.sin(a)
    def pt(r, s): return (48 + ca * r - sa * s, 48 + sa * r + ca * s)
    blade = inpoly([pt(3, -2.5), pt(15, -3.5), pt(15, 3.5), pt(3, 2.5)])
    diag(g, blade, 'A', 'B', *pt(9, 0), 'M')
part(g, incircle(48, 48, 4), 48, 48); fill(g, incircle(48, 48, 1.5), 'X')
write(g, "reprocessing-plant-rotator", BLOCK + "on reprocessing-plant")
