"""2x2: Lithium Extractor, Platinum Refinery, Mineral Separator, Enzyme Extractor, Ore Crusher, Flotation Cell."""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from smalllib import *

def agitator(name, blades=3, r=10):
    g = new()
    for k in range(blades):
        a = k * 2 * math.pi / blades + .4
        ca, sa = math.cos(a), math.sin(a)
        def pt(rr, s): return (32 + ca * rr - sa * s, 32 + sa * rr + ca * s)
        diag(g, inpoly([pt(2.5, -1.6), pt(r, -2.4), pt(r, 2.4), pt(2.5, 1.6)]), 'A', 'B', *pt(r / 2, 0), 'M')
    part(g, incircle(32, 32, 3.2), 32, 32); fill(g, incircle(32, 32, 1.3), 'X')
    write(g, name + "-rotator", BLOCK2 + "on " + name)

# ---------------- Lithium Extractor: stirred precipitation tank (brine shows), filter press, lithium crystal tray ----------------
g = new(); base2(g)
part(g, inrect(2, 26, 10, 38), 6, 32); fill(g, inrect(2, 29, 7, 35), 'N')           # brine in
part(g, inrect(6, 50, 58, 61), 32, 55)                                               # filter press
for x in range(10, 55, 4): diag(g, inrect(x, 52, x + 2, 59), 'M', 'G', x + 1, 55)
part(g, inrect(46, 4, 60, 18), 53, 11); fill(g, inrect(49, 7, 57, 15), 'X')        # lithium crystals
for (x, y) in ((50, 8), (53, 10), (55, 8), (51, 12), (54, 13)): fill(g, inrect(x, y, x + 1, y + 1), 'W'); g[y + 1][x + 1] = 'w'
part(g, incircle(32, 32, 16), 32, 32)                                                # precipitation tank
groove(g, ring(12.5, 13.3))
window(g, incircle(32, 32, 11.5))
fill(g, inrect(5, 5, 10, 10), 'X'); fill(g, inrect(6, 6, 9, 9), 'L')   # status lamp (glows in the product colour)
write(g, "lithium-extractor", BLOCK2 + "color L f0f4f8\nmask glow L\ncolor N 9fb3c8\ncolor W f0f4f8\ncolor w c0c8d0\nref separator cryofluid-mixer")
g = new()                                                                            # -rotator: slow stirrer (block centre)
for k in range(2):
    a = k * math.pi + .3
    ca, sa = math.cos(a), math.sin(a)
    def pt(rr, s): return (32 + ca * rr - sa * s, 32 + sa * rr + ca * s)
    diag(g, inpoly([pt(2, -1.5), pt(9, -2), pt(9, 2), pt(2, 1.5)]), 'A', 'B', *pt(5, 0), 'M')
part(g, incircle(32, 32, 3), 32, 32)
write(g, "lithium-extractor-rotator", BLOCK2 + "on lithium-extractor")

# ---------------- Platinum Refinery: sealed acid chamber (aqua regia shows), recovery filter with platinum sponge ----------------
g = new(); base2(g)
part(g, inrect(4, 4, 60, 60), 32, 32)                                                # acid-resistant shell
groove(g, minus(inrect(7, 7, 57, 57), inrect(8, 8, 56, 56)))
for (cx, cy) in ((10, 10), (54, 10), (10, 54), (54, 54)): fill(g, incircle(cx, cy, 1.6), 'X')   # seal bolts
part(g, inrect(14, 12, 50, 38), 32, 25)
window(g, inrect(17, 15, 47, 35))                                                    # the leaching chamber
part(g, inrect(14, 42, 50, 54), 32, 48); fill(g, inrect(17, 45, 47, 51), 'X')       # recovery filter
for (x, y) in ((20, 46), (25, 48), (30, 46), (35, 48), (40, 46), (44, 48)): fill(g, inrect(x, y, x + 2, y + 1), 'W'); g[y + 1][x + 2] = 'w'
fill(g, inrect(52, 44, 54, 46), 'L')
write(g, "platinum-refinery", BLOCK2 + "color W e4e6ee\ncolor w aeb2c0\ncolor L f0d98c\nmask glow L W\nref separator plastanium-compressor")

# ---------------- Mineral Separator: centrifuge (spins) in a slag bath, feed, acid in, output ports ----------------
g = new(); base2(g)
part(g, inrect(2, 26, 10, 38), 6, 32); fill(g, inrect(2, 29, 7, 35), 'O')           # molten slag feed
part(g, inrect(26, 2, 38, 10), 32, 6); fill(g, inrect(29, 3, 35, 7), 'Y')           # sulfuric acid in
for (x0, y0) in ((52, 14), (52, 28), (52, 42)):                                      # output ports
    part(g, inrect(x0, y0, x0 + 9, y0 + 7), x0 + 4.5, y0 + 3.5); fill(g, inrect(x0 + 3, y0 + 2, x0 + 9, y0 + 5), 'X')
part(g, incircle(32, 32, 19), 32, 32)
window(g, ring(12, 15.5))                                                            # the slag bath round the rotor
fill(g, incircle(32, 32, 12), 'X')
write(g, "mineral-separator", BLOCK2 + ORANGE + "color Y e8d44d\nref separator disassembler")
g = new()                                                                            # -rotator: centrifuge rotor (spins about the block centre)
for y in range(64):
    for x in range(64):
        px, py = x + .5 - 32, y + .5 - 32; r, t = math.hypot(px, py), math.atan2(py, px)
        if r <= 11.5:
            k = (t % (math.pi / 3)) / (math.pi / 3)
            g[y][x] = ('A' if k < .5 else 'B') if r > 4 else ('M' if r > 2 else 'X')
            if r > 4 and .45 < k < .55: g[y][x] = 'H'
write(g, "mineral-separator-rotator", BLOCK2 + "on mineral-separator")

# ---------------- Enzyme Extractor: biomass hopper, small centrifuge (spins), enzyme vials (glow) ----------------
g = new(); base2(g)
part(g, inrect(4, 4, 24, 24), 14, 14); fill(g, inrect(7, 7, 21, 21), 'X')          # biomass hopper
for (x, y) in ((9, 9), (13, 11), (16, 9), (10, 14), (15, 16), (12, 18)): fill(g, inrect(x, y, x + 2, y + 1), 'E'); g[y + 1][x + 2] = 'e'
part(g, incircle(38, 24, 15), 38, 24); fill(g, incircle(38, 24, 11), 'X')          # centrifuge housing
for i, x0 in enumerate((8, 20, 32, 44)):                                             # extraction vials
    part(g, inrect(x0, 44, x0 + 9, 59), x0 + 4.5, 51)
    fill(g, inrect(x0 + 2, 46, x0 + 7, 57), 'X'); fill(g, inrect(x0 + 3, 50 - i, x0 + 6, 56), 'V')
c = small(20)
for y in range(20):
    for x in range(20):
        px, py = x + .5 - 10, y + .5 - 10; r, t = math.hypot(px, py), math.atan2(py, px)
        if r <= 9.5:
            if r > 3: c[y][x] = ('A' if px - py > 0 else 'B') if (t % (math.pi / 2)) < 1 else 'G'
            else: c[y][x] = 'M'
            if 6 < r <= 8 and (t % (math.pi / 2)) < .5: c[y][x] = 'V'
paste(g, c, 38, 24)
write(g, "enzyme-extractor", BLOCK2 + "color E 8fc77a\ncolor e 5b8a4c\ncolor V e86fd0\nmask glow V\nref separator cultivator")
write_small(c, "enzyme-extractor-centrifuge", "color A b0b9c0\ncolor B 6e7080\ncolor M 989aa4\ncolor G 757887\ncolor V e86fd0")

# ---------------- Ore Crusher: feed hopper, two crushing rollers (turn against each other), fragments out ----------------
g = new(); base2(g)
part(g, inpoly([(14, 2), (50, 2), (44, 14), (20, 14)]), 32, 8); fill(g, inpoly([(19, 4), (45, 4), (41, 12), (23, 12)]), 'X')   # feed hopper
for (x, y) in ((24, 6), (29, 8), (34, 5), (38, 8)): fill(g, inrect(x, y, x + 2, y + 2), 'R'); g[y + 2][x + 2] = 'r'
part(g, inrect(4, 16, 60, 48), 32, 32); fill(g, inrect(7, 19, 57, 45), 'X')         # crusher housing, dark gap round the rollers
part(g, inrect(18, 50, 46, 61), 32, 55); fill(g, inrect(21, 53, 43, 61), 'X')       # crushed ore out
for (x, y) in ((23, 55), (27, 57), (31, 54), (35, 57), (39, 55), (25, 59), (37, 59)): g[y][x] = 'R'; g[y][x + 1] = 'r'
roll = small(26)
for y in range(26):
    for x in range(26):
        px, py = x + .5 - 13, y + .5 - 13; r, t = math.hypot(px, py), math.atan2(py, px)
        if r <= 12.5:
            k = (t % (math.pi / 6)) / (math.pi / 6)
            roll[y][x] = 'H' if r > 11 and k < .5 else ('A' if px - py > 0 else 'B') if r > 4 else 'M'
            if 5 < r <= 11 and k < .18: roll[y][x] = 'G'
            if r <= 2: roll[y][x] = 'X'
paste(g, roll, 19, 32); paste(g, roll, 45, 32)
fill(g, inrect(53, 53, 58, 58), 'X'); fill(g, inrect(54, 54, 57, 57), 'L')   # status lamp (glows in the product colour)
write(g, "ore-crusher", BLOCK2 + "color L fdb380\nmask glow L\ncolor R 9a8a7a\ncolor r 6e6256\nref pulverizer multi-press")
write_small(roll, "ore-crusher-roller", "color A b0b9c0\ncolor B 6e7080\ncolor M 989aa4\ncolor G 757887\ncolor H 5c5e6c\ncolor X 4a4b53")
write(new(), "ore-crusher-press", BLOCK2)                                            # no press layer any more (the old jaws)

# ---------------- Flotation Cell: froth tank (water shows), froth lip, concentrate launder; -rotator: impeller ----------------
g = new(); base2(g)
part(g, inrect(52, 6, 61, 58), 56, 32)                                               # concentrate launder
fill(g, inrect(54, 9, 58, 55), 'X')
for y in range(12, 54, 5): fill(g, inrect(55, y, 57, y + 1), 'F')
part(g, inrect(2, 26, 9, 38), 5, 32); fill(g, inrect(2, 29, 6, 35), 'N')
part(g, incircle(32, 32, 18), 32, 32)
fill(g, ring(13.5, 15), 'F')                                                         # froth lip
window(g, incircle(32, 32, 13))
fill(g, inrect(4, 51, 9, 56), 'X'); fill(g, inrect(5, 52, 8, 55), 'L')   # status lamp (glows in the product colour)
write(g, "flotation-cell", BLOCK2 + "color L e8e8e0\nmask glow L\ncolor F e8e8e0\ncolor N 9fb3c8\nref separator cultivator")
agitator("flotation-cell", 4, 9)
