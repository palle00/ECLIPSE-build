"""2x2: Sulfuric Acid Plant, Water Treatment Plant, Chemical Reactor, Cryogenic Condenser, Wastewater Treatment Plant, Thermal Waste Processor."""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from smalllib import *

def outlet(name):
    t = new()
    part(t, inpoly([(48, 22), (63, 19), (63, 45), (48, 42)]), 55, 32)
    fill(t, inrect(55, 24, 63, 40), 'X'); fill(t, inrect(55, 24, 55, 40), 'O')
    for y in (26, 31, 36): groove(t, inrect(49, y, 53, y + 1), 51, y)
    write(t, name + "-top1", BLOCK2 + ORANGE + "on " + name)
    write(t[::-1], name + "-top2", BLOCK2 + ORANGE + "on " + name)

# ---------------- Sulfuric Acid Plant: sulfur burner (flame flickers), absorption tower (acid shows), liquid outlet; heat outlet ----------------
g = new(); base2(g)
part(g, incircle(15, 15, 10), 15, 15); fill(g, incircle(15, 15, 6.5), 'X')         # sulfur burner
fill(g, incircle(15, 15, 5), 'O'); fill(g, incircle(15, 15, 2.5), 'P')
part(g, inrect(21, 12, 30, 18), 25, 15)                                              # SO2 duct to the tower
part(g, inrect(26, 52, 38, 61), 32, 56); fill(g, inrect(29, 55, 35, 61), 'Y')       # acid out
part(g, incircle(32, 34, 16), 32, 34)                                                # absorption tower
groove(g, ring(12, 12.8, 32, 34))
window(g, incircle(32, 34, 11))
for y in (29, 39): part(g, both(incircle(32, 34, 11), inrect(20, y, 44, y + 1)), 32, y)   # packing supports
write(g, "sulfuric-acid-plant", BLOCK2 + ORANGE + "color Y e8d44d\nmask flame O P\nref separator kiln")
outlet("sulfuric-acid-plant")

# ---------------- Water Treatment Plant: inlet pump (spins), two filter beds (water shows), clean water up, brine down ----------------
g = new(); base2(g)
part(g, inrect(26, 2, 38, 10), 32, 6); fill(g, inrect(29, 3, 35, 7), 'N')           # purified water out (up)
part(g, inrect(26, 54, 38, 62), 32, 58); fill(g, inrect(29, 57, 35, 61), 'S')       # brine out (down)
part(g, incircle(10, 32, 8), 10, 32); fill(g, incircle(10, 32, 5.5), 'X')           # inlet pump
for y0 in (14, 34):                                                                  # filter beds: media over the water
    part(g, inrect(18, y0, 56, y0 + 16), 37, y0 + 8)
    window(g, inrect(21, y0 + 3, 53, y0 + 13))
    for x in range(21, 54):                                                          # filter media bed (sand over gravel)
        for y in range(y0 + 8, y0 + 14):
            if (x * 7 + y * 13) % 9 != 0: g[y][x] = 'D' if (x * 3 + y) % 4 else 'd'
p = fan(12, 4, 1.8, 5, .5); paste(g, p, 10, 32)
fill(g, inrect(54, 54, 59, 59), 'X'); fill(g, inrect(55, 55, 58, 58), 'L')   # status lamp (glows in the product colour)
write(g, "water-treatment-plant", BLOCK2 + "color L bfe6f2\nmask glow L\ncolor N 9fb3c8\ncolor S e0e0d8\ncolor D c8b88a\ncolor d 9a8c66\nref separator water-extractor")
write_small(p, "water-treatment-plant-pump", FANHDR)

# ---------------- Chemical Reactor: mixing vessel (contents show), four ports; -rotator: agitator ----------------
g = new(); base2(g)
for (x0, y0, x1, y1) in ((26, 2, 38, 10), (26, 54, 38, 62), (2, 26, 10, 38), (54, 26, 62, 38)):
    part(g, inrect(x0, y0, x1, y1), (x0 + x1) / 2, (y0 + y1) / 2); fill(g, inrect(x0 + 2, y0 + 2, x1 - 2, y1 - 2), 'X')
part(g, incircle(32, 32, 21), 32, 32)
groove(g, ring(16.5, 17.3))
window(g, incircle(32, 32, 15.5))
fill(g, inrect(54, 54, 57, 57), 'X'); fill(g, inrect(55, 55, 56, 56), 'L')
write(g, "chemical-reactor", BLOCK2 + "color L 7fe0a0\nmask glow L\nref separator cryofluid-mixer")
g = new()
for k in range(3):
    a = k * 2 * math.pi / 3 + .4
    ca, sa = math.cos(a), math.sin(a)
    def pt(r, s): return (32 + ca * r - sa * s, 32 + sa * r + ca * s)
    diag(g, inpoly([pt(2.5, -1.6), pt(12, -2.6), pt(12, 2.6), pt(2.5, 1.6)]), 'A', 'B', *pt(7, 0), 'M')
part(g, incircle(32, 32, 3.5), 32, 32); fill(g, incircle(32, 32, 1.4), 'X')
write(g, "chemical-reactor-rotator", BLOCK2 + "on chemical-reactor")

# ---------------- Cryogenic Condenser: insulated housing, condenser coil over the liquid nitrogen, compressor fan ----------------
g = new(); base2(g)
part(g, inrect(5, 5, 58, 58), 32, 32)                                                # insulated housing
for k in range(8, 56, 6):                                                            # insulation gaps
    fill(g, inrect(k, 6, k, 8), 'X'); fill(g, inrect(k, 55, k, 57), 'X')
part(g, incircle(14, 14, 7), 14, 14); fill(g, incircle(14, 14, 5), 'X')             # compressor
part(g, incircle(34, 36, 18), 34, 36)
window(g, incircle(34, 36, 13.5))                                                    # liquid nitrogen pools round the coil
for r in (5, 8.5, 12):                                                               # condenser coil
    fill(g, ring(r - 1, r + 1, 34, 36), 'G')
    fill(g, both(ring(r - 1, r - .2, 34, 36), lambda px, py: px - py > 0), 'C')
fill(g, incircle(34, 36, 2), 'C')
f = fan(10, 4, 1.5, 4.2, .5); paste(g, f, 14, 14)
write(g, "cryogenic-condenser", BLOCK2 + COLD + "mask glow C\nref cryofluid-mixer vent-condenser")
write_small(f, "cryogenic-condenser-fan", FANHDR)

# ---------------- Wastewater Treatment Plant: clarifier (water shows), sludge chamber, recovered water channel; -rotator: rake bridge ----------------
g = new(); base2(g)
part(g, inrect(2, 26, 10, 38), 6, 32); fill(g, inrect(2, 29, 7, 35), 'W')          # dirty water in
part(g, inrect(46, 3, 61, 13), 53, 8); fill(g, inrect(49, 6, 61, 10), 'N')          # recovered water channel
part(g, inrect(46, 50, 61, 61), 53, 55); fill(g, inrect(49, 53, 58, 58), 'K')       # sludge chamber
fill(g, inrect(51, 54, 53, 55), 'k'); fill(g, inrect(55, 56, 57, 57), 'k')
part(g, incircle(32, 32, 22), 32, 32)                                                # clarifier
groove(g, ring(18.5, 19.3))
window(g, incircle(32, 32, 17.5))
fill(g, incircle(32, 32, 4), 'K')                                                    # sediment well in the middle
fill(g, inrect(5, 53, 10, 58), 'X'); fill(g, inrect(6, 54, 9, 57), 'L')   # status lamp (glows in the product colour)
write(g, "wastewater-treatment-plant", BLOCK2 + "color L bfe6f2\nmask glow L\ncolor W 8a8270\ncolor N 9fb3c8\ncolor K 4a3e30\ncolor k 6a5a44\nref separator water-extractor")
g = new()                                                                            # -rotator: the rake bridge sweeping the clarifier
part(g, inrect(32, 30, 50, 34), 41, 32)
for x in range(36, 50, 4): fill(g, inrect(x, 34, x + 1, 36), 'H')
part(g, incircle(32, 32, 3.5), 32, 32)
write(g, "wastewater-treatment-plant-rotator", BLOCK2 + "on wastewater-treatment-plant")

# ---------------- Thermal Waste Processor: dark combustion chamber (embers, warm grate), feed hopper, scrap and sand trays ----------------
g = new(); base2(g)
part(g, inrect(4, 4, 22, 22), 13, 13); fill(g, inrect(7, 7, 19, 19), 'X')          # waste feed
for (x, y, c) in ((9, 9, 'M'), (13, 11, 'B'), (16, 14, 'A'), (10, 15, 'G')): fill(g, inrect(x, y, x + 2, y + 1), c)
for (x0, c1, c2) in ((42, 'A', 'B'), (52, 'D', 'd')):                                # scrap and sand trays
    part(g, inrect(x0 - 1, 46, x0 + 8, 60), x0 + 3.5, 53)
    for (x, y) in ((x0 + 1, 49), (x0 + 4, 52), (x0 + 2, 55), (x0 + 5, 57)): fill(g, inrect(x, y, x + 1, y + 1), c1); g[y + 1][x + 1] = c2
part(g, incircle(30, 32, 17), 30, 32)                                                # combustion chamber
fill(g, incircle(30, 32, 13), 'X')
diag(g, ring(10, 12.5, 30, 32), 'O', 'H', 30, 32)                                    # warm lining
for y in range(26, 40, 4): fill(g, both(incircle(30, 32, 9), inrect(20, y, 40, y)), 'H')   # grate
write(g, "thermal-waste-processor", BLOCK2 + ORANGE + "color D e8d8a8\ncolor d b8a878\nmask glow O\nref kiln silicon-smelter")
