"""Biochemical Fermenter, Anaerobic Digester, Advanced Module Assembler, Aetherite Module Forge, Vanadium Flow Battery."""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from blocklib import *
sys.argv = sys.argv[:2]
exec(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "draft15.py"), encoding="utf-8").read().split("# ---------------- Air Separation Unit")[0].split("from blocklib import *")[1])

CYAN = "color C 8ae4ff\ncolor c 4fa7c7\n"

# ---------------- Biochemical Fermenter: sealed vessel (biofuel shows), cooling jacket, stirrer, enzyme dosing, outlets up/down ----------------
g = new(); base(g)
for (x0, y0, x1, y1, c) in ((42, 3, 54, 17, 'Y'), (42, 79, 54, 93, 'K')):           # biofuel out (up), CO2 out (down)
    part(g, inrect(x0, y0, x1, y1), (x0 + x1) / 2, (y0 + y1) / 2); fill(g, inrect(x0 + 3, y0 + 4, x1 - 3, y1 - 4), c)
part(g, inrect(3, 42, 17, 54), 10, 48); fill(g, inrect(3, 46, 13, 50), 'N')         # purified water in
part(g, inrect(76, 26, 92, 70), 84, 48)                                              # enzyme dosing unit
for y0 in (30, 44, 58):
    fill(g, inrect(80, y0, 88, y0 + 9), 'X'); diag(g, inrect(81, y0 + 3, 87, y0 + 8), 'E', 'e', 84, y0 + 5)
    fill(g, inrect(82, y0 + 1, 86, y0 + 2), 'M')
part(g, incircle(48, 48, 29), 48, 48)                                                # the vessel
for k in range(16):                                                                  # cooling jacket coils
    a = k * math.pi / 8
    for r in range(20, 26):
        x, y = int(48 + r * math.cos(a)), int(48 + r * math.sin(a)); g[y][x] = 'e'
groove(g, ring(19, 19.8))
window(g, incircle(48, 48, 18.5))
fill(g, inrect(18, 18, 21, 21), 'X'); fill(g, inrect(19, 19, 20, 20), 'L')          # status lamp
write(g, "biochemical-fermenter", BLOCK + "color Y f0e6a0\ncolor K 7a7c84\ncolor N 9fb3c8\ncolor E 8fc77a\ncolor e 5b8a4c\ncolor L c9f0a0\nmask glow L E\nref cultivator separator")
g = new()                                                                            # -rotator: slow two-blade stirrer
diag(g, inrect(34, 46, 62, 50), 'A', 'B', 48, 48, 'M')
part(g, incircle(48, 48, 4.5), 48, 48); fill(g, incircle(48, 48, 1.8), 'X')
write(g, "biochemical-fermenter-rotator", BLOCK + "on biochemical-fermenter")

# ---------------- Anaerobic Digester: big tank under a gas-holder roof (-press: it settles as gas is drawn off), flare, sludge in, digestate out ----------------
g = new(); base(g)
part(g, inrect(4, 70, 26, 92), 15, 81); fill(g, inrect(8, 74, 22, 88), 'X')          # sludge hopper
for (x, y) in ((10, 76), (15, 79), (19, 75), (11, 83), (17, 85)):
    fill(g, inrect(x, y, x + 3, y + 2), 'S'); fill(g, inrect(x + 1, y + 2, x + 3, y + 2), 's')
part(g, inrect(70, 70, 92, 92), 81, 81); fill(g, inrect(74, 74, 88, 88), 'X')       # digestate (fertilizer) out
for (x, y) in ((76, 76), (81, 79), (85, 75), (77, 83), (83, 85)):
    fill(g, inrect(x, y, x + 2, y + 1), 'F'); fill(g, inrect(x + 1, y + 1, x + 2, y + 1), 'f')
part(g, incircle(15, 15, 10), 15, 15); fill(g, incircle(15, 15, 6), 'X')            # gas flare stack
fill(g, incircle(15, 15, 4), 'O'); fill(g, incircle(15, 15, 2.2), 'P')
part(g, inrect(70, 6, 90, 18), 80, 12); fill(g, inrect(73, 10, 87, 14), 'Z')       # biogas take-off
part(g, incircle(48, 48, 32), 48, 48)                                                # digestion tank
window(g, ring(24, 27.5))                                                            # gas gap round the roof (methane shows)
fill(g, inrect(86, 86, 89, 89), 'X'); fill(g, inrect(87, 87, 88, 88), 'L')
write(g, "anaerobic-digester", BLOCK + ORANGE + "color S 7a5a40\ncolor s 5a412e\ncolor F e8e2c8\ncolor f b9b39c\ncolor Z c4b3e6\ncolor L c4b3e6\nmask glow L\nmask flame O P\nref separator cultivator")
g = new()                                                                            # -press: gas-holder roof with radial seams
diag(g, incircle(48, 48, 23.5), 'A', 'B', 48, 48, 'M')
for k in range(8):
    a = k * math.pi / 4
    for r in range(6, 23):
        x, y = int(48 + r * math.cos(a)), int(48 + r * math.sin(a)); g[y][x] = 'G' if (x - y) > 0 else 'H'
part(g, incircle(48, 48, 5), 48, 48); fill(g, incircle(48, 48, 2), 'X')
write(g, "anaerobic-digester-press", BLOCK + "on anaerobic-digester")

# ---------------- Advanced Module Assembler: precision bed (the module is drawn on it), two gantry heads, component bays ----------------
g = new(); base(g)
for y0 in (6, 82):                                                                   # gantry rails
    part(g, inrect(10, y0, 86, y0 + 8), 48, y0 + 4); fill(g, inrect(13, y0 + 3, 83, y0 + 5), 'X')
for x0 in (5, 79):                                                                   # component bays
    part(g, inrect(x0, 20, x0 + 12, 76), x0 + 6, 48)
    for y in range(24, 72, 8):
        fill(g, inrect(x0 + 3, y, x0 + 9, y + 5), 'X'); diag(g, inrect(x0 + 4, y + 1, x0 + 8, y + 4), 'U', 'u', x0 + 6, y + 2.5)
part(g, inrect(21, 21, 75, 75), 48, 48)                                              # precision bed
fill(g, inrect(25, 25, 71, 71), 'X'); diag(g, inrect(26, 26, 70, 70), 'H', 'X', 48, 48)
for (x0, y0, sx, sy) in ((26, 26, 1, 1), (70, 26, -1, 1), (26, 70, 1, -1), (70, 70, -1, -1)):   # cyan alignment marks
    fill(g, inrect(min(x0, x0 + 6 * sx), y0, max(x0, x0 + 6 * sx), y0), 'C'); fill(g, inrect(x0, min(y0, y0 + 6 * sy), x0, max(y0, y0 + 6 * sy)), 'C')
write(g, "advanced-module-assembler", BLOCK + CYAN + "color U c3d0e6\ncolor u 8da1b8\nmask glow C\nref multi-press disassembler")
for name, y0, tip in (("advanced-module-assembler-head-a", 4, 1), ("advanced-module-assembler-head-b", 80, -1)):
    g = new()
    part(g, inrect(40, y0, 56, y0 + 12), 48, y0 + 6)
    stem = inrect(46, y0 + 12, 50, y0 + 18) if tip > 0 else inrect(46, y0 - 6, 50, y0)
    part(g, stem, 48, y0 + 15 if tip > 0 else y0 - 3)
    fill(g, inrect(46, y0 + 18, 50, y0 + 19) if tip > 0 else inrect(46, y0 - 7, 50, y0 - 6), 'C')
    fill(g, inrect(44, y0 + 4, 52, y0 + 8), 'X')
    write(g, name, BLOCK + CYAN + "on advanced-module-assembler")

# ---------------- Aetherite Module Forge: heavy frame, forge bed, crystal chamber with a turning containment ring ----------------
g = new(); base(g)
for (cx, cy) in ((12, 12), (84, 12), (12, 84), (84, 84)):                            # frame pillars
    part(g, inpoly(octagon(cx, cy, 8)), cx, cy); fill(g, incircle(cx, cy, 2.5), 'X')
part(g, inrect(20, 34, 76, 90), 48, 62)                                              # forge bed housing
fill(g, inrect(25, 39, 71, 85), 'X'); diag(g, inrect(26, 40, 70, 84), 'H', 'X', 48, 62)
for x in (32, 64):                                                                   # energy conduits from the chamber
    fill(g, inrect(x - 1, 26, x + 1, 39), 'X'); fill(g, inrect(x, 26, x, 39), 'C')
for (x0, x1) in ((5, 17), (79, 91)):                                                 # clamp pads either side of the bed
    part(g, inrect(x0, 46, x1, 78), (x0 + x1) / 2, 62)
    for y in range(50, 76, 6): groove(g, inrect(x0 + 3, y, x1 - 3, y), (x0 + x1) / 2, y)
fill(g, inrect(28, 42, 68, 42), 'c'); fill(g, inrect(28, 82, 68, 82), 'c')         # energy rails along the bed
fill(g, inrect(28, 42, 28, 82), 'c'); fill(g, inrect(68, 42, 68, 82), 'c')
for (x, y) in ((28, 42), (68, 42), (28, 82), (68, 82)): fill(g, inrect(x - 1, y - 1, x + 1, y + 1), 'C')
part(g, inpoly(octagon(48, 18, 15)), 48, 18)                                         # crystal chamber
fill(g, inpoly(octagon(48, 18, 10)), 'X')
diag(g, inpoly(diamond(48, 18, 7)), 'C', 'c', 48, 18); fill(g, inrect(49, 14, 50, 15), 'W')   # aetherite crystal
ring_s = small(30)                                                                   # containment ring (turns)
for y in range(30):
    for x in range(30):
        px, py = x + .5 - 15, y + .5 - 15
        r, t = math.hypot(px, py), math.atan2(py, px)
        if 10.5 <= r <= 14 and (t % (math.pi / 2)) < 1.15: ring_s[y][x] = 'H' if px - py > 0 else 'X'
        if 11.8 <= r <= 12.7 and (t % (math.pi / 2)) < 1.15: ring_s[y][x] = 'C'
paste(g, ring_s, 48, 18)
write(g, "aetherite-module-forge", BLOCK + CYAN + "color W e8fbff\nmask crystal C c W\nref disassembler surge-smelter")
write_small(ring_s, "aetherite-module-forge-ring", "color H 5c5e6c\ncolor X 4a4b53\ncolor C 8ae4ff")

# ---------------- Vanadium Flow Battery: paired electrolyte tanks, pumps, central cell stack; -power-empty/-power-full: charge lamps ----------------
g = new(); base(g)
for (cx, c1, c2) in ((20, 'V', 'v'), (76, 'Y', 'y')):                                # V2+/V3+ (violet) and V4+/V5+ (yellow) electrolyte
    part(g, incircle(cx, 24, 17), cx, 24)
    diag(g, incircle(cx, 24, 12), c1, c2, cx, 24)
    groove(g, ring(6, 7, cx, 24), cx, 24); fill(g, incircle(cx, 24, 3), 'X')
for (x0, x1) in ((12, 16), (24, 28), (68, 72), (80, 84)):                            # pipe loops down to the stack
    part(g, inrect(x0, 40, x1, 60), (x0 + x1) / 2, 50)
for cx in (20, 76):                                                                  # circulation pumps
    part(g, incircle(cx, 66, 8), cx, 66); fill(g, incircle(cx, 66, 5.5), 'X')
part(g, inrect(32, 44, 64, 92), 48, 68)                                              # cell stack
for y in range(52, 88, 4):
    diag(g, inrect(36, y, 60, y + 1), 'M', 'G', 48, y)                               # membrane cells
part(g, inrect(36, 38, 60, 47), 48, 42); fill(g, inrect(38, 40, 58, 45), 'X')       # charge lamp panel
p = fan(14, 4, 2.2, 6, .5)
for cx in (20, 76): paste(g, p, cx, 66)
write(g, "flow-battery", BLOCK + "color V 9a7fd0\ncolor v 6a4fa0\ncolor Y d8d070\ncolor y a8a040\nref battery-large separator")
write_small(p, "flow-battery-pump", FANHDR)
for name, col in (("flow-battery-power-empty", "3a3540"), ("flow-battery-power-full", "8fffb0")):
    g = new()
    for x in range(39, 57, 4): fill(g, inrect(x, 41, x + 2, 44), 'K')
    write(g, name, BLOCK + f"color K {col}\non flow-battery")
