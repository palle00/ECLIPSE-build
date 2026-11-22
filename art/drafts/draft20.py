"""2x2: Chip Fabricator, Industrial Heater, Electric Heater, Geothermal Heat Tap, Thermal Storage, Composite Press."""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from smalllib import *

CYAN = "color C 8ae4ff\ncolor c 4fa7c7\n"

def outlet(name, heavy=False):
    """-top1/-top2: the heat outlet on the output face (the game turns it with the block; the heat glow sits in its slot)."""
    t = new()
    pts = [(46, 20), (63, 16), (63, 48), (46, 44)] if heavy else [(48, 22), (63, 19), (63, 45), (48, 42)]
    part(t, inpoly(pts), 55, 32)
    fill(t, inrect(55, 24, 63, 40), 'X'); fill(t, inrect(55, 24, 55, 40), 'O')
    for y in (26, 31, 36): groove(t, inrect(49, y, 53, y + 1), 51, y)
    write(t, name + "-top1", BLOCK2 + ORANGE + "on " + name)
    write(t[::-1], name + "-top2", BLOCK2 + ORANGE + "on " + name)

# ---------------- Chip Fabricator: wafer bed, gantry rail; -head slides; traces glow ----------------
g = new(); base2(g)
part(g, inrect(5, 7, 58, 15), 32, 11); fill(g, inrect(8, 10, 55, 12), 'X')          # gantry rail
part(g, inrect(10, 18, 54, 50), 32, 34); fill(g, inrect(13, 21, 51, 47), 'X')       # wafer chuck
fill(g, incircle(32, 34, 13), 'w'); fill(g, incircle(32, 34, 12), 'W')              # the wafer, diced into chips
for k in range(-10, 11, 5):
    fill(g, both(incircle(32, 34, 12), inrect(32 + k, 22, 32 + k, 46)), 'w')
    fill(g, both(incircle(32, 34, 12), inrect(20, 34 + k, 44, 34 + k)), 'w')
fill(g, inrect(29, 31, 34, 36), 'C')                                                 # the chip being etched
part(g, inrect(5, 52, 58, 59), 32, 55); fill(g, inrect(8, 54, 55, 57), 'X')         # control panel with traces
for (x0, x1, y) in ((10, 22, 55), (22, 22, 54), (26, 40, 56), (44, 53, 55)): fill(g, inrect(x0, y, x1, y), 'T')
write(g, "chip-fabricator", BLOCK2 + CYAN + "color W c8cdd8\ncolor w 9aa0ae\ncolor T 6dff9a\nmask glow T\nref silicon-smelter pulverizer")
g = new()                                                                            # -head: etching head riding the rail
part(g, inrect(26, 6, 38, 17), 32, 11); part(g, inrect(30, 17, 34, 25), 32, 21); fill(g, inrect(31, 25, 33, 26), 'C')
write(g, "chip-fabricator-head", BLOCK2 + CYAN + "on chip-fabricator")

# ---------------- Industrial Heater: methane burner firebox (flame flickers), flue stack; outlet on the output face ----------------
g = new(); base2(g)
part(g, inrect(3, 28, 11, 36), 7, 32); fill(g, inrect(3, 30, 8, 34), 'Z')           # methane in
part(g, incircle(13, 12, 7), 13, 12); fill(g, incircle(13, 12, 4.5), 'X')           # flue stack
part(g, incircle(26, 34, 15), 26, 34)                                                # firebox
fill(g, incircle(26, 34, 11), 'X')
for y in range(26, 43, 4): fill(g, both(incircle(26, 34, 10), inrect(16, y, 36, y + 1)), 'O')   # flame through the grate
fill(g, both(incircle(26, 34, 6), inrect(20, 28, 32, 40)), 'P')
part(g, inrect(38, 20, 48, 46), 43, 33)                                              # heat duct to the outlet
write(g, "industrial-heater", BLOCK2 + ORANGE + "color Z c4b3e6\nmask flame O P\nref combustion-generator smelter")
outlet("industrial-heater", True)

# ---------------- Electric Heater: three heating coils in a recess (glow with output), cyan terminals; outlet ----------------
g = new(); base2(g)
part(g, inrect(3, 22, 10, 42), 6, 32); fill(g, inrect(5, 26, 8, 28), 'C'); fill(g, inrect(5, 36, 8, 38), 'C')   # power terminals
part(g, inrect(11, 12, 48, 52), 30, 32); fill(g, inrect(14, 15, 45, 49), 'X')       # element bay
for y0 in (19, 30, 41):                                                              # zig-zag heating coils
    for x in range(16, 44):
        y = y0 + (2 if (x // 3) % 2 else 0)
        g[y][x] = 'R'; g[y + 1][x] = 'r'
write(g, "electric-heater", BLOCK2 + ORANGE + CYAN + "color R c9ccd4\ncolor r 8a8d96\nmask coils R r\nref thermal-generator smelter")
outlet("electric-heater")

# ---------------- Geothermal Heat Tap: anchored base plate, finned collector over a hot core (glows with heat); outlet ----------------
g = new(); base2(g)
for (cx, cy) in ((9, 9), (55, 9), (9, 55), (55, 55)):                                # ground anchors
    fill(g, incircle(cx, cy, 5), 'X'); fill(g, incircle(cx, cy, 3), 'K'); fill(g, incircle(cx - 1, cy - 1, 1), 'M')
part(g, incircle(30, 32, 20), 30, 32)                                                # collector
for k in range(12):                                                                  # radial fins
    a = k * math.pi / 6
    for r in range(11, 19):
        x, y = int(30 + r * math.cos(a)), int(32 + r * math.sin(a)); g[y][x] = 'G' if x - y > 0 else 'H'
fill(g, incircle(30, 32, 10), 'X'); diag(g, incircle(30, 32, 8), 'P', 'O', 30, 32)   # the hot core
write(g, "geothermal-heat-tap", BLOCK2 + ORANGE + "color K 343a4a\nmask core O P\nref thermal-generator vent-condenser")
outlet("geothermal-heat-tap")

# ---------------- Thermal Storage: layered insulation round a heat core (glows with what it stores), intake chevrons; outlet ----------------
g = new(); base2(g)
part(g, inrect(10, 10, 50, 54), 30, 32)                                              # insulation layers
for i, c in enumerate(('G', 'M', 'H')):
    m = 3 + i * 3
    fill(g, minus(inrect(10 + m, 10 + m, 50 - m, 54 - m), inrect(11 + m, 11 + m, 49 - m, 53 - m)), c)
fill(g, inrect(21, 23, 39, 41), 'X'); diag(g, inrect(23, 25, 37, 39), 'P', 'O', 30, 32)   # the heat core
for i in range(2):                                                                   # intake chevrons on the back face
    x0 = 3 + i * 4
    for k in range(4):
        g[28 + k][x0 + k] = 'O'; g[36 - k][x0 + k] = 'O'
write(g, "thermal-storage", BLOCK2 + ORANGE + "mask core O P\nref battery thermal-generator")
outlet("thermal-storage")

# ---------------- Composite Press: carbon/resin layup bed, resin feed; -press: pressure plate ----------------
g = new(); base2(g)
for (cx, cy) in ((10, 10), (54, 10), (10, 54), (54, 54)):
    part(g, incircle(cx, cy, 5), cx, cy); fill(g, incircle(cx, cy, 2), 'X')
part(g, inrect(14, 14, 50, 50), 32, 32); fill(g, inrect(16, 16, 48, 48), 'X')
for i, y in enumerate(range(18, 47, 4)):                                             # the layup: carbon fibre and resin layers
    fill(g, inrect(18, y, 46, y + 1), 'K' if i % 2 == 0 else 'Y'); fill(g, inrect(18, y + 2, 46, y + 3), 'k')
part(g, inrect(52, 26, 61, 38), 56, 32); fill(g, inrect(55, 29, 61, 35), 'Y')       # epoxy feed
fill(g, inrect(3, 30, 5, 33), 'L')
write(g, "composite-press", BLOCK2 + "color K 3a3b44\ncolor k 2a2b32\ncolor Y d8c890\ncolor L 8fb8ff\nmask glow L\nref plastanium-compressor multi-press")
g = new()                                                                            # -press: pressure plate (the layup shows at its ends)
part(g, inrect(17, 23, 47, 41), 32, 32)
for x in (24, 32, 40): groove(g, inrect(x, 26, x, 38), x, 32)
for cx in (10, 54): part(g, inrect(cx - 3 if cx < 32 else 47, 29, 17 if cx < 32 else cx + 3, 35), 32, 32)
write(g, "composite-press-press", BLOCK2 + "on composite-press")
