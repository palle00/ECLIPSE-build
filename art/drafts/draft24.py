"""2x2: Hydrogen Fuel Cell, Methane Turbine, Biofuel Engine, Biomass Cultivator."""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from smalllib import *

CYAN = "color C 8ae4ff\ncolor c 4fa7c7\n"

# ---------------- Hydrogen Fuel Cell: plate stack (membranes glow), H2 / O2 feeds, water out, blower fan ----------------
g = new(); base2(g)
part(g, inrect(2, 18, 10, 30), 6, 24); fill(g, inrect(2, 21, 7, 27), 'N')           # hydrogen in
part(g, inrect(30, 2, 42, 10), 36, 6); fill(g, inrect(33, 2, 39, 7), 'W')           # oxygen in
part(g, inrect(30, 54, 42, 62), 36, 58); fill(g, inrect(33, 57, 39, 62), 'b')       # water out
part(g, inrect(14, 12, 58, 52), 36, 32); fill(g, inrect(17, 15, 55, 49), 'X')       # cell stack
for x in range(19, 54, 4):
    diag(g, inrect(x, 16, x + 1, 48), 'M', 'G', x, 32); fill(g, inrect(x + 2, 17, x + 2, 47), 'C')   # bipolar plates, membranes
for y in (15, 49): part(g, inrect(14, y - 1, 58, y + 1), 36, y)                      # end plates
part(g, incircle(10, 50, 8), 10, 50); fill(g, incircle(10, 50, 5.5), 'X')           # blower
f = fan(12, 5, 1.6, 5, .5); paste(g, f, 10, 50)
write(g, "hydrogen-fuel-cell", BLOCK2 + CYAN + "color N d8e4f0\ncolor W bfe6f2\ncolor b 7fb8cc\nmask glow C\nref differential-generator battery")
write_small(f, "hydrogen-fuel-cell-fan", FANHDR)

# ---------------- Methane Turbine: combustor (glows), turbine chamber with rotor (spins), exhaust ----------------
g = new(); base2(g)
part(g, inrect(2, 28, 9, 36), 5, 32); fill(g, inrect(2, 30, 6, 34), 'Z')            # methane in
part(g, inrect(8, 20, 22, 44), 15, 32); fill(g, inrect(11, 24, 19, 40), 'X')         # combustor can
for y in range(26, 39, 4): fill(g, inrect(12, y, 18, y + 1), 'O')
part(g, incircle(48, 12, 7), 48, 12); fill(g, incircle(48, 12, 4.5), 'X')           # exhaust
part(g, incircle(38, 34, 19), 38, 34); fill(g, incircle(38, 34, 15), 'X')           # turbine chamber
rotor = small(30)
for y in range(30):
    for x in range(30):
        px, py = x + .5 - 15, y + .5 - 15; r, t = math.hypot(px, py), math.atan2(py, px)
        if r <= 14:
            k = (t + r * .07) % (2 * math.pi / 14) / (2 * math.pi / 14)
            rotor[y][x] = ('A' if k < .35 else 'M' if k < .6 else 'B') if r > 5 else ('G' if r > 2.5 else 'X')
paste(g, rotor, 38, 34)
write(g, "methane-turbine", BLOCK2 + ORANGE + "color Z c4b3e6\nmask glow O\nref differential-generator steam-engine")
write_small(rotor, "methane-turbine-rotor", "color A b0b9c0\ncolor B 6e7080\ncolor M 989aa4\ncolor G 757887\ncolor X 4a4b53")

# ---------------- Biofuel Engine: four-cylinder block, rocker covers, flywheel (spins), exhaust manifold, fuel gauge ----------------
g = new(); base2(g)
part(g, inrect(6, 8, 40, 56), 23, 32)                                                # engine block
for i in range(4):                                                                   # cylinder heads
    y0 = 11 + i * 11
    part(g, inrect(10, y0, 36, y0 + 8), 23, y0 + 4)
    fill(g, incircle(17, y0 + 4, 2), 'X'); fill(g, incircle(29, y0 + 4, 2), 'X')
part(g, inrect(40, 10, 48, 54), 44, 32)                                              # exhaust manifold
for y in (16, 27, 38, 49): fill(g, inrect(41, y - 1, 47, y + 1), 'H')
part(g, incircle(52, 14, 6), 52, 14); fill(g, incircle(52, 14, 3.5), 'X')           # exhaust stack
part(g, incircle(51, 44, 11), 51, 44)                                                # flywheel housing
fw = small(20)
for y in range(20):
    for x in range(20):
        px, py = x + .5 - 10, y + .5 - 10; r, t = math.hypot(px, py), math.atan2(py, px)
        if r <= 9.5:
            fw[y][x] = 'H' if r > 8 else ('A' if px - py > 0 else 'B') if r > 3 else 'M'
            if 3 < r <= 8 and (t % (2 * math.pi / 3)) < .8: fw[y][x] = 'X'
            if r <= 1.5: fw[y][x] = 'X'
paste(g, fw, 51, 44)
part(g, inrect(6, 57, 22, 62), 14, 59); fill(g, inrect(8, 58, 20, 61), 'X'); fill(g, inrect(9, 59, 16, 60), 'Y')   # fuel gauge
write(g, "biofuel-engine", BLOCK2 + "color Y f0c64a\nmask glow Y\nref combustion-generator steam-engine")
write_small(fw, "biofuel-engine-flywheel", "color A b0b9c0\ncolor B 6e7080\ncolor M 989aa4\ncolor H 5c5e6c\ncolor X 4a4b53")

# ---------------- Biomass Cultivator: growth tank (water + culture show), grow lights, paddle (turns) ----------------
g = new(); base2(g)
for (x0, y0, x1, y1) in ((6, 4, 58, 9), (6, 55, 58, 60)):                            # grow-light bars
    part(g, inrect(x0, y0, x1, y1), 32, (y0 + y1) / 2); fill(g, inrect(x0 + 3, y0 + 2, x1 - 3, y1 - 2), 'L')
part(g, inrect(4, 12, 60, 52), 32, 32)
window(g, inrect(8, 16, 56, 48))
write(g, "biomass-cultivator", BLOCK2 + "color L f0c8ff\nmask lights L\nref cultivator")
g = new()                                                                            # -rotator: slow paddle
diag(g, inrect(16, 30, 48, 34), 'A', 'B', 32, 32, 'M')
part(g, incircle(32, 32, 3.5), 32, 32)
write(g, "biomass-cultivator-rotator", BLOCK2 + "on biomass-cultivator")
mid = small(16)                                                                      # -middle: the culture cell DrawCultivator scatters (tinted)
for y in range(16):
    for x in range(16):
        r = math.hypot(x + .5 - 8, y + .5 - 8)
        if r <= 6.5: mid[y][x] = 'W' if r <= 4.5 else 'w'
write_small(mid, "biomass-cultivator-middle", "color W ffffff\ncolor w c8c8c8")
