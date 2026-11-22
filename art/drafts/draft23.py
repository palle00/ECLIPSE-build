"""2x2: Ore Washer, Basic Module Assembler, Module Recycling Facility, Industrial Battery, Lithium Battery, Aetherite Capacitor."""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from smalllib import *

CYAN = "color C 8ae4ff\ncolor c 4fa7c7\n"

def power(name, lamps):
    """-power-empty / -power-full: charge lamps (the game fades the full ones in with the stored charge)."""
    for suf, col in (("-power-empty", "3a3540"), ("-power-full", "8fffb0")):
        g = new()
        for (x0, y0, x1, y1) in lamps: fill(g, inrect(x0, y0, x1, y1), 'K')
        write(g, name + suf, BLOCK2 + f"color K {col}\non {name}")

# ---------------- Ore Washer: wash trough with riffles (water shows), paddle wheel (turns), mineral bins ----------------
g = new(); base2(g)
part(g, inrect(2, 14, 10, 24), 6, 19); fill(g, inrect(2, 17, 7, 21), 'N')           # water in
for i, (c1, c2) in enumerate((('R', 'r'), ('S', 's'), ('T', 't'))):                  # mineral collection bins
    x0 = 8 + i * 17
    part(g, inrect(x0, 46, x0 + 14, 60), x0 + 7, 53); fill(g, inrect(x0 + 3, 49, x0 + 11, 57), 'X')
    for (x, y) in ((x0 + 4, 51), (x0 + 7, 53), (x0 + 9, 50), (x0 + 5, 55)): g[y][x] = c1; g[y][x + 1] = c2
part(g, inrect(6, 6, 58, 43), 32, 24)                                                # wash trough
window(g, inrect(10, 10, 54, 39))
for x in range(14, 52, 6): part(g, inrect(x, 10, x + 1, 39), x, 24)                # riffles
main = g; g = new()                                                                 # -wheel: paddle wheel in the trough (turns)
for k in range(6):
    a = k * math.pi / 3
    ca, sa = math.cos(a), math.sin(a)
    def pt(r, s): return (32 + ca * r - sa * s, 24 + sa * r + ca * s)
    diag(g, inpoly([pt(2, -2), pt(11, -2.4), pt(11, 2.4), pt(2, 2)]), 'A', 'B', *pt(6, 0), 'M')
fill(g, ring(10.5, 12, 32, 24), 'H')                                                 # wheel rim
part(g, incircle(32, 24, 3.5), 32, 24)
w = small(26)
for y in range(26):
    for x in range(26): w[y][x] = g[24 - 13 + y][32 - 13 + x]
write_small(w, "ore-washer-wheel", "color A b0b9c0\ncolor B 6e7080\ncolor M 989aa4\ncolor G 757887\ncolor H 5c5e6c")
paste(main, w, 32, 24)
write(main, "ore-washer", BLOCK2 + "color N 9fb3c8\ncolor R 9a8a7a\ncolor r 6e6256\ncolor S c8b88a\ncolor s 9a8c66\ncolor T 8a96a6\ncolor t 5e6878\nref separator pulverizer")

# ---------------- Basic Module Assembler: platform (the module is drawn on it), tool rail, output tray; -head slides ----------------
g = new(); base2(g)
part(g, inrect(5, 4, 58, 11), 32, 7); fill(g, inrect(8, 6, 55, 8), 'X')            # tool rail
part(g, inrect(12, 15, 52, 51), 32, 33); fill(g, inrect(15, 18, 49, 48), 'X')       # assembly platform
diag(g, inrect(16, 19, 48, 47), 'H', 'X', 32, 33)
for (x, y) in ((17, 20), (46, 20), (17, 45), (46, 45)): fill(g, inrect(x, y, x + 1, y + 1), 'P')
part(g, inrect(18, 53, 46, 61), 32, 57); fill(g, inrect(21, 55, 43, 59), 'X')       # output tray
fill(g, inrect(55, 54, 57, 56), 'L')
write(g, "basic-module-assembler", BLOCK2 + ORANGE + "color L fdb380\nmask glow L P\nref multi-press pulverizer")
g = new()
part(g, inrect(26, 3, 38, 13), 32, 8); part(g, inrect(30, 13, 34, 19), 32, 16); fill(g, inrect(31, 19, 33, 20), 'P')
write(g, "basic-module-assembler-head", BLOCK2 + ORANGE + "on basic-module-assembler")

# ---------------- Module Recycling Facility: dismantling bed with a cutter disc (spins), sorting bins ----------------
g = new(); base2(g)
part(g, inrect(5, 5, 40, 40), 22, 22); fill(g, inrect(8, 8, 37, 37), 'X')           # dismantling bed
diag(g, inrect(9, 9, 36, 36), 'H', 'X', 22, 22)
for i, c in enumerate(('Q', 'U', 'C')):                                              # sorted material bins
    y0 = 6 + i * 13
    part(g, inrect(44, y0, 59, y0 + 11), 51, y0 + 5.5); fill(g, inrect(47, y0 + 3, 56, y0 + 8), 'X')
    for (x, y) in ((48, y0 + 4), (51, y0 + 6), (54, y0 + 4)): fill(g, inrect(x, y, x + 1, y + 1), c)
part(g, inrect(5, 44, 40, 59), 22, 51)                                               # conveyor to the bins
for x in range(8, 38, 4): fill(g, inrect(x, 47, x + 1, 56), 'H')
fill(g, inrect(55, 50, 57, 52), 'L')
saw = small(26)
for y in range(26):
    for x in range(26):
        px, py = x + .5 - 13, y + .5 - 13; r, t = math.hypot(px, py), math.atan2(py, px)
        k = (t % (math.pi / 6)) / (math.pi / 6)
        if r <= 12.5 - 2.5 * k: saw[y][x] = 'A' if px - py > 0 else 'B'
        if r <= 4: saw[y][x] = 'M'
        if r <= 1.8: saw[y][x] = 'X'
        if 6 < r <= 7 and (t % (math.pi / 2)) < .9: saw[y][x] = 'G'
paste(g, saw, 22, 22)
write(g, "module-recycling-facility", BLOCK2 + CYAN + "color Q d99d73\ncolor U c3d0e6\ncolor L 8ae4ff\nmask glow L\nref disassembler pulverizer")
write_small(saw, "module-recycling-facility-saw", "color A b0b9c0\ncolor B 6e7080\ncolor M 989aa4\ncolor G 757887\ncolor X 4a4b53")

# ---------------- Industrial Battery: six lead-acid cells in a frame, terminals and busbar, charge lamps ----------------
g = new(); base2(g)
part(g, inrect(4, 4, 59, 12), 32, 8); diag(g, inrect(7, 7, 56, 9), 'Q', 'q', 32, 8)   # busbar
for j in range(2):
    for i in range(3):
        x0, y0 = 6 + i * 18, 15 + j * 20
        part(g, inrect(x0, y0, x0 + 15, y0 + 17), x0 + 7.5, y0 + 8.5)              # cell
        fill(g, inrect(x0 + 3, y0 + 3, x0 + 12, y0 + 14), 'H')
        fill(g, inrect(x0 + 4, y0 + 4, x0 + 5, y0 + 5), 'O'); fill(g, inrect(x0 + 10, y0 + 4, x0 + 11, y0 + 5), 'M')   # terminals
        fill(g, inrect(x0 + 5, y0 + 9, x0 + 10, y0 + 9), 'X')
part(g, inrect(14, 55, 50, 61), 32, 58); fill(g, inrect(17, 57, 47, 59), 'X')       # charge lamp panel
write(g, "industrial-battery", BLOCK2 + ORANGE + "color Q d99d73\ncolor q a8664c\nref battery battery-large")
power("industrial-battery", [(x, 57, x + 3, 59) for x in range(18, 46, 6)])

# ---------------- Lithium Battery: dense pack of cylindrical cells round a power-management plate, charge lamps ----------------
g = new(); base2(g)
part(g, inrect(4, 4, 59, 59), 32, 32); fill(g, inrect(7, 7, 56, 56), 'X')
for j in range(6):
    for i in range(6):
        cx, cy = 11 + i * 8.4, 11 + j * 8.4
        if 2 <= i <= 3 and 2 <= j <= 3: continue
        fill(g, incircle(cx, cy, 3.6), 'M'); diag(g, incircle(cx, cy, 2.8), 'A', 'B', cx, cy); fill(g, incircle(cx, cy, 1), 'H')
part(g, inrect(24, 24, 40, 40), 32, 32); fill(g, inrect(27, 27, 37, 37), 'X')       # power-management plate
write(g, "lithium-battery", BLOCK2 + "ref battery-large battery")
power("lithium-battery", [(28, 28, 29, 36), (31, 28, 32, 36), (34, 28, 35, 36)])

# ---------------- Aetherite Capacitor: plate stacks round a caged aetherite crystal (it lights with the charge) ----------------
g = new(); base2(g)
for (x0, x1) in ((5, 19), (45, 59)):                                                 # capacitor plate stacks
    part(g, inrect(x0, 6, x1, 58), (x0 + x1) / 2, 32)
    for y in range(10, 56, 4): diag(g, inrect(x0 + 3, y, x1 - 3, y + 1), 'M', 'G', (x0 + x1) / 2, y)
part(g, inpoly(octagon(32, 32, 12)), 32, 32); fill(g, inpoly(octagon(32, 32, 8)), 'X')   # crystal cage
diag(g, inpoly(diamond(32, 32, 6)), 'c', 'H', 32, 32)
for (x0, y0, x1, y1) in ((31, 20, 32, 44), (20, 31, 44, 32)): fill(g, both(inrect(x0, y0, x1, y1), minus(inpoly(octagon(32, 32, 11)), inpoly(diamond(32, 32, 6)))), 'G')
part(g, inrect(24, 50, 40, 58), 32, 54); fill(g, inrect(26, 52, 38, 56), 'X')
write(g, "aetherite-capacitor", BLOCK2 + CYAN + "ref battery-large surge-wall")
for suf, c1, c2 in (("-power-empty", "3a4a52", "2a3a42"), ("-power-full", "8ae4ff", "e8fbff")):
    g = new()
    fill(g, inpoly(diamond(32, 32, 5)), 'K'); fill(g, inrect(32, 29, 33, 30), 'W')
    for x in range(27, 38, 3): fill(g, inrect(x, 53, x + 1, 55), 'K')
    write(g, "aetherite-capacitor" + suf, BLOCK2 + f"color K {c1}\ncolor W {c2}\non aetherite-capacitor")
