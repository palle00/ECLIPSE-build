"""Industrial Boiler, Steam Turbine, Solar Farm, Fission Reactor, Carbide Drill (with their moving layers)."""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from blocklib import *
sys.argv = sys.argv[:2]
exec(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "draft15.py"), encoding="utf-8").read().split("# ---------------- Air Separation Unit")[0].split("from blocklib import *")[1])

# ---------------- Industrial Boiler: fire-tube shell, water-level window, tube sheet, steam dome, gauge ----------------
g = new(); base(g)
part(g, inrect(3, 44, 12, 52), 7, 48); fill(g, inrect(3, 47, 9, 49), 'N')           # purified water in
part(g, inrect(64, 3, 92, 13), 78, 8); fill(g, inrect(67, 6, 92, 9), 'Z')           # steam out
part(g, inpoly([(14, 18), (82, 18), (88, 24), (88, 74), (82, 80), (14, 80), (8, 74), (8, 24)]), 48, 49)   # pressure shell
for x in (20, 76):                                                                   # shell rings
    groove(g, inrect(x, 20, x, 78))
part(g, inrect(19, 25, 77, 47), 48, 36); window(g, inrect(23, 29, 73, 43))           # water-level window (boiling water shows)
part(g, inrect(19, 50, 77, 76), 48, 63); fill(g, inrect(22, 53, 74, 73), 'X')       # tube sheet
for row, y in enumerate((57, 63, 69)):
    for x in range(27 + (row % 2) * 4, 72, 8):
        fill(g, incircle(x, y, 2.8), 'M'); fill(g, incircle(x, y, 1.5), 'H')
part(g, inrect(38, 5, 58, 17), 48, 11); fill(g, incircle(48, 11, 3.5), 'X'); fill(g, incircle(48, 11, 2), 'O')   # steam dome + safety valve
gauge(g, 84, 86)
write(g, "industrial-boiler", BLOCK + ORANGE + "color N 9fb3c8\ncolor Z e8eef2\nmask glow O\nref separator steam-engine")
write(needle(84, 86), "industrial-boiler-needle", BLOCK + "color K c23b3b\non industrial-boiler")

# ---------------- Steam Turbine: tapered casing, bladed rotor (spins with output), generator windings, condensate return ----------------
g = new(); base(g)
part(g, inrect(3, 42, 16, 54), 9, 48); fill(g, inrect(3, 45, 12, 51), 'Z')          # steam in
part(g, inrect(18, 80, 66, 92), 42, 86); fill(g, inrect(21, 84, 63, 88), 'N')       # condensate return
part(g, inpoly([(14, 34), (30, 18), (66, 12), (66, 84), (30, 78), (14, 62)]), 40, 48)   # turbine casing, widening downstream
for x in (24, 34, 58):
    groove(g, both(inpoly([(14, 34), (30, 18), (66, 12), (66, 84), (30, 78), (14, 62)]), inrect(x, 0, x, 95)))
part(g, inrect(68, 14, 92, 82), 80, 48)                                              # generator
for y in range(20, 78, 5):
    diag(g, inrect(72, y, 88, y + 2), 'Q', 'q', 80, y + 1)                           # copper windings
for y in (17, 79): fill(g, inrect(78, y - 1, 82, y + 1), 'X'); fill(g, inrect(79, y, 81, y), 'L')
part(g, incircle(48, 48, 22), 48, 48); fill(g, incircle(48, 48, 19.5), 'X')         # rotor housing
write(g, "steam-turbine", BLOCK + "color Z e8eef2\ncolor N 9fb3c8\ncolor Q d99d73\ncolor q a8664c\ncolor L 9fd8ff\nmask glow L\nref steam-engine differential-generator")
g = new()                                                                            # -rotor: bladed disc
for y in range(H):
    for x in range(W):
        px, py = x + .5 - 48, y + .5 - 48
        r, t = math.hypot(px, py), math.atan2(py, px)
        if r <= 18.5:
            k = (t + r * .06) % (2 * math.pi / 16) / (2 * math.pi / 16)
            if r > 7: g[y][x] = ('A' if k < .3 else 'M' if k < .55 else 'B') if k < .8 else 'H'
            elif r > 5: g[y][x] = 'G'
            else: g[y][x] = 'M' if r > 2 else 'X'
write(g, "steam-turbine-rotor", BLOCK + "on steam-turbine")

# ---------------- Solar Farm: eight PV panels round a central inverter; -glint: sunlight glint ----------------
g = new()
diag(g, inpoly([(4, 0), (92, 0), (96, 4), (96, 92), (92, 96), (4, 96), (0, 92), (0, 4)]), 'G', 'H', 48, 48)    # low frame
for j in range(3):
    for i in range(3):
        x0, y0 = 4 + i * 30, 4 + j * 30
        if i == 1 and j == 1:                                                        # inverter
            part(g, inrect(x0 + 2, y0 + 2, x0 + 25, y0 + 25), x0 + 13.5, y0 + 13.5)
            for y in range(y0 + 6, y0 + 22, 3): fill(g, inrect(x0 + 6, y, x0 + 21, y), 'X')
            fill(g, inrect(x0 + 18, y0 + 20, x0 + 21, y0 + 22), 'L')
            continue
        fill(g, inrect(x0, y0, x0 + 27, y0 + 27), 'M')                               # panel frame
        diag(g, inrect(x0 + 1, y0 + 1, x0 + 26, y0 + 26), 'S', 's', x0 + 13.5, y0 + 13.5)
        for k in range(1, 4):                                                        # cell grid
            fill(g, inrect(x0 + 1, y0 + k * 6.5, x0 + 26, y0 + k * 6.5), 'g')
            fill(g, inrect(x0 + k * 6.5, y0 + 1, x0 + k * 6.5, y0 + 26), 'g')
        for t in range(0, 9):                                                        # glint streak
            fill(g, inrect(x0 + 15 + t, y0 + 2 + t, x0 + 16 + t, y0 + 2 + t), 'W')
write(g, "solar-farm", BLOCK + "color S 3a5a9a\ncolor s 263d70\ncolor g 1d2c52\ncolor W 8fb0e8\ncolor L 7fe0a0\nmask glint W\nref solar-panel-large battery")

# ---------------- Fission Reactor: heavy containment, fuel lattice (glows), fuel-handling bridge; -top1/-top2: heat outlet ----------------
g = new(); base(g)
part(g, inpoly(octagon(48, 48, 41)), 48, 48)                                         # containment housing
for k in range(8):                                                                   # segment joints with bolts
    a = k * math.pi / 4
    x0, y0 = 48 + 34 * math.cos(a), 48 + 34 * math.sin(a)
    fill(g, incircle(x0, y0, 2.2), 'X'); fill(g, incircle(x0, y0, 1), 'M')
part(g, incircle(48, 48, 28), 48, 48)                                                # pressure vessel
groove(g, ring(24, 25))
fill(g, incircle(48, 48, 22), 'X'); diag(g, incircle(48, 48, 20.5), 'D', 'd')        # coolant pool
rods = [(48, 48)] + [(48 + 10 * math.cos(k * math.pi / 3), 48 + 10 * math.sin(k * math.pi / 3)) for k in range(6)]
for (x, y) in rods:                                                                  # fuel assemblies
    fill(g, incircle(x, y, 4), 'X'); diag(g, incircle(x, y, 3), 'F', 'f', x, y); fill(g, incircle(x + .5, y - .5, 1), 'E')
write(g, "fission-reactor", BLOCK + "color D 3d4a5c\ncolor d 2e3846\ncolor F e07ab8\ncolor f a0507f\ncolor E ffd0ec\nmask core F f E\nref thorium-reactor impact-reactor")
g = new()                                                                            # -bridge: fuel-handling bridge, turns slowly over the core
part(g, inrect(48, 46, 72, 49), 60, 47.5)                                           # crane arm from the centre post
part(g, inrect(59, 43, 67, 52), 63, 47.5); fill(g, inrect(62, 46, 64, 49), 'X')      # grab trolley
part(g, incircle(48, 48, 3.5), 48, 48)
write(g, "fission-reactor-bridge", BLOCK + "on fission-reactor")
top = new()
part(top, inpoly([(70, 36), (93, 30), (93, 66), (70, 60)]), 82, 48)
fill(top, inrect(85, 39, 93, 57), 'X'); fill(top, inrect(85, 39, 86, 57), 'O')
for y in (40, 46, 52):
    groove(top, inrect(73, y, 82, y + 2), 77, y + 1)
write(top, "fission-reactor-top1", BLOCK + ORANGE + "on fission-reactor")
write(top[::-1], "fission-reactor-top2", BLOCK + ORANGE + "on fission-reactor")

# ---------------- Carbide Drill: frame with carbide armour, water ports; -rotator: cutting head; -top: hub cap; -rim ----------------
g = new(); base(g)
for (cx, cy) in ((14, 14), (82, 14), (14, 82), (82, 82)):                            # carbide armour blocks
    part(g, inrect(cx - 10, cy - 10, cx + 10, cy + 10), cx, cy)
    diag(g, inrect(cx - 6, cy - 6, cx + 6, cy + 6), 'K', 'k', cx, cy)
for (x0, y0, x1, y1) in ((40, 3, 56, 12), (40, 84, 56, 93), (3, 40, 12, 56), (84, 40, 93, 56)):   # water ports
    part(g, inrect(x0, y0, x1, y1), (x0 + x1) / 2, (y0 + y1) / 2); fill(g, inrect(x0 + 3, y0 + 3, x1 - 3, y1 - 3), 'N')
part(g, incircle(48, 48, 33), 48, 48)
fill(g, incircle(48, 48, 26), 'X'); diag(g, incircle(48, 48, 24), 'H', 'X')         # drill pit
write(g, "carbide-drill", BLOCK + "color K 4a5266\ncolor k 343a4a\ncolor N 9fb3c8\nref laser-drill blast-drill")
g = new()                                                                            # -rotator: four cutting arms with carbide teeth
for k in range(4):
    a = k * math.pi / 2
    ca, sa = math.cos(a), math.sin(a)
    def pt(r, s): return (48 + ca * r - sa * s, 48 + sa * r + ca * s)
    part(g, inpoly([pt(6, -4), pt(22, -5), pt(23, 0), pt(22, 5), pt(6, 4)]), *pt(14, 0))
    for r in (12, 17, 21):
        tx, ty = pt(r, -4.5); fill(g, incircle(tx, ty, 1.7), 'K')
write(g, "carbide-drill-rotator", BLOCK + "color K 343a4a\non carbide-drill")
g = new()                                                                            # -top: hub cap
part(g, inpoly(octagon(48, 48, 9)), 48, 48); fill(g, inpoly(octagon(48, 48, 4)), 'X'); fill(g, inrect(47, 47, 48, 48), 'O')
write(g, "carbide-drill-top", BLOCK + ORANGE + "on carbide-drill")
g = new()                                                                            # -rim: glows in the boost colour
fill(g, ring(24.5, 26.5), 'R')
write(g, "carbide-drill-rim", BLOCK + "color R ffffff\non carbide-drill")
