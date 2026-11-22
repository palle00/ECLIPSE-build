"""Walls (1x1 + large) and the 2x2 logistics blocks: Distribution Hub, Line Balancer, Logistics Buffers, Wave Intelligence Scanner.
Routing blocks turn with their rotation: the front (output) side is the right edge."""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import blocklib
from smalllib import *

def size(n):
    blocklib.W = blocklib.H = n

# ---------------- walls: vanilla bevel (light top-right, dark bottom-left) round a raised plate with the material's mark ----------------
WALLS = {  # name: plate light, plate dark, plate mid, bevel light, bevel dark, mark colour
    "superalloy": ("e6d6a8", "a8946a", "c9b788", "f2e8c8", "7e6c48", "6e5c3c"),
    "composite": ("5c606c", "34363e", "464a54", "7a7e8a", "24262c", "8fb8ff"),
    "surge-superalloy": ("ffd86a", "c4862a", "e8b04a", "fff0b0", "8a5a14", "d96b68"),
}
def wall_header(c):
    return f"outline 0\ncolor A {c[0]}\ncolor B {c[1]}\ncolor M {c[2]}\ncolor E {c[3]}\ncolor D {c[4]}\ncolor K {c[5]}\ncolor X 3a3b42\ncolor G {c[1]}\ncolor H {c[4]}\n"
def plate(g, x0, y0, x1, y1, mat):
    """One wall plate: bevel ring, plate face (diagonal split), the material's mark."""
    diag(g, inrect(x0, y0, x1, y1), 'E', 'D', (x0 + x1) / 2, (y0 + y1) / 2)
    diag(g, inrect(x0 + 3, y0 + 3, x1 - 3, y1 - 3), 'A', 'B', (x0 + x1) / 2, (y0 + y1) / 2, 'M')
    cx, cy, w = (x0 + x1) / 2, (y0 + y1) / 2, x1 - x0
    if mat == "superalloy":                                                          # riveted plate
        for (dx, dy) in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
            fill(g, inrect(cx + dx * (w / 2 - 6) - .5, cy + dy * (w / 2 - 6) - .5, cx + dx * (w / 2 - 6) + .5, cy + dy * (w / 2 - 6) + .5), 'K')
        groove(g, inrect(x0 + 6, cy, x1 - 6, cy))
    elif mat == "composite":                                                         # carbon weave
        for y in range(int(y0 + 4), int(y1 - 3)):
            for x in range(int(x0 + 4), int(x1 - 3)):
                if ((x // 3) + (y // 3)) % 2 == 0 and (x + y) % 3 == 0: g[y][x] = 'D'
        fill(g, inrect(cx - 1, cy - 1, cx, cy), 'K')
    else:                                                                            # surge: a charge line between two contacts
        fill(g, inrect(x0 + 6, cy, x1 - 6, cy), 'B')
        for x in (x0 + 6, x1 - 7): fill(g, inrect(x, cy - 1, x + 1, cy + 1), 'K')

for mat, c in WALLS.items():
    size(32); g = new()
    plate(g, 0, 0, 31, 31, mat)
    with open(os.path.join(OUT, mat + "-wall.txt"), "w", newline="\n") as f:
        f.write("size 32 32\n" + wall_header(c) + "ref titanium-wall plastanium-wall\ngrid\n" + "\n".join("".join(r) for r in g) + "\n")
    size(64); g = new()
    for (x0, y0) in ((0, 0), (32, 0), (0, 32), (32, 32)): plate(g, x0, y0, x0 + 31, y0 + 31, mat)
    with open(os.path.join(OUT, mat + "-wall-large.txt"), "w", newline="\n") as f:
        f.write("size 64 64\n" + wall_header(c) + "ref titanium-wall-large plastanium-wall-large\ngrid\n" + "\n".join("".join(r) for r in g) + "\n")
    print("drafted", mat, "walls")
size(64)

def chevron(g, cx, cy, d, c='P'):
    """A small arrow pointing in direction d (0 right, 1 up, 2 left, 3 down)."""
    for i in range(4):
        for j in (-i, i):
            x, y = [(cx - i, cy + j), (cx + j, cy + i), (cx + i, cy + j), (cx + j, cy - i)][d]
            g[int(y)][int(x)] = c

# ---------------- Distribution Hub (2x2 filter router): sorting turntable and four chutes ----------------
g = new(); base2(g)
for (x0, y0, x1, y1) in ((26, 2, 38, 22), (26, 42, 38, 62), (2, 26, 22, 38), (42, 26, 62, 38)):   # chutes on all four sides
    part(g, inrect(x0, y0, x1, y1), (x0 + x1) / 2, (y0 + y1) / 2); fill(g, inrect(x0 + 3, y0 + 3, x1 - 3, y1 - 3), 'X')
chevron(g, 32, 8, 1); chevron(g, 32, 56, 3); chevron(g, 56, 32, 0); chevron(g, 8, 32, 2, 'H')
for (cx, cy) in ((12, 12), (52, 12), (12, 52), (52, 52)):
    part(g, inrect(cx - 7, cy - 7, cx + 7, cy + 7), cx, cy); fill(g, incircle(cx, cy, 1.5), 'X')
part(g, incircle(32, 32, 12), 32, 32); fill(g, incircle(32, 32, 9), 'X')            # sorting turntable
for k in range(4):
    a = k * math.pi / 2 + math.pi / 4
    fill(g, incircle(32 + 5 * math.cos(a), 32 + 5 * math.sin(a), 2.2), 'M')
fill(g, incircle(32, 32, 2), 'P')
write(g, "distribution-hub", BLOCK2 + ORANGE + "ref distributor sorter")

# ---------------- Line Balancer (2x2 controlled splitter): intake on the back, split vanes, two outputs ----------------
g = new(); base2(g)
part(g, inrect(2, 24, 24, 40), 13, 32); fill(g, inrect(2, 28, 22, 36), 'X')         # intake
for (y0, y1) in ((6, 22), (42, 58)):                                                 # the two balanced lines out the front
    part(g, inrect(36, y0, 62, y1), 49, (y0 + y1) / 2); fill(g, inrect(40, y0 + 4, 62, y1 - 4), 'X')
    chevron(g, 56, (y0 + y1) / 2, 0)
part(g, inrect(20, 14, 42, 50), 31, 32)                                              # splitter body with flow vanes
fill(g, inrect(24, 18, 38, 46), 'X')
for k in range(4): g[32 - k][28 + k] = g[32 + k][28 + k] = 'M'
fill(g, inrect(26, 31, 28, 33), 'P')
write(g, "line-balancer", BLOCK2 + ORANGE + "ref overflow-gate distributor")

# ---------------- Logistics Buffers (2x2): crate grid with an output gate on the front ----------------
for name, n, accent in (("small-logistics-buffer", 2, "c3d0e6"), ("industrial-logistics-buffer", 3, "fdb380")):
    g = new(); base2(g)
    part(g, inrect(5, 5, 46, 58), 25, 31); fill(g, inrect(8, 8, 43, 55), 'X')       # storage rack
    cw = 34 // n
    for j in range(n):
        for i in range(n):
            x0, y0 = 9 + i * cw, 9 + j * (46 // n)
            diag(g, inrect(x0 + 1, y0 + 1, x0 + cw - 2, y0 + 46 // n - 2), 'A', 'B', x0 + cw / 2, y0 + 23 / n, 'M')   # crates
            fill(g, inrect(x0 + 2, y0 + 23 // n, x0 + cw - 3, y0 + 23 // n), 'G')
    part(g, inrect(48, 18, 60, 46), 54, 32); fill(g, inrect(51, 22, 60, 42), 'X')    # output gate (front)
    fill(g, inrect(52, 24, 53, 40), 'Q'); chevron(g, 57, 32, 0, 'Q')
    write(g, name, BLOCK2 + f"color Q {accent}\nref container vault")

# ---------------- Wave Intelligence Scanner (2x2): radar pedestal and console; -dish turns while it is linked ----------------
g = new(); base2(g)
part(g, inrect(44, 6, 60, 58), 52, 32); fill(g, inrect(47, 10, 57, 54), 'X')       # console
for y in range(13, 52, 5): fill(g, inrect(49, y, 55, y + 1), 'S')
fill(g, inrect(49, 12, 55, 12), 'Y')
part(g, incircle(24, 32, 19), 24, 32); fill(g, incircle(24, 32, 15), 'X')           # radar pedestal
dish = small(30)
for y in range(30):
    for x in range(30):
        px, py = x + .5 - 15, y + .5 - 15; r = math.hypot(px, py)
        if r <= 13.5 and px > -5: dish[y][x] = 'A' if py < 0 else 'B'                 # parabolic dish facing the sweep
        if r <= 13.5 and -5 < px <= -3: dish[y][x] = 'H'
        if abs(py) <= 1 and -9 < px <= 0: dish[y][x] = 'M'                           # feed arm
        if r <= 2.5: dish[y][x] = 'X'
        if 9 < px <= 12 and abs(py) <= 1: dish[y][x] = 'C'
paste(g, dish, 24, 32)
write(g, "wave-intelligence-scanner", BLOCK2 + "color S 4a7a5a\ncolor Y ffd37f\ncolor C 8ae4ff\nref radar logic-display")
write_small(dish, "wave-intelligence-scanner-dish", "color A b0b9c0\ncolor B 6e7080\ncolor M 989aa4\ncolor H 5c5e6c\ncolor X 4a4b53\ncolor C 8ae4ff")
