"""Orbital Weapons Platform (5x5 base + rotating emitter head + charge glow) and the World Engine (6x6 frame with the
eclipse at its heart, one layer per construction stage, and the corona glow it lights when it comes online)."""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import blocklib
from blocklib import inpoly, incircle, inrect, both, minus, octagon, diamond, diag, fill, part, groove
from turretlib import Grid
OUT = sys.argv[1]

def canvas(n):
    blocklib.W = blocklib.H = n
    return [['.'] * n for _ in range(n)]

def header(n, extra=""):
    return (f"size {n} {n}\noutline 0\ncolor A b0b9c0\ncolor B 6e7080\ncolor M 989aa4\ncolor G 757887\ncolor H 5c5e6c\ncolor X 4a4b53\n" + extra)

def write(g, name, head):
    with open(os.path.join(OUT, name + ".txt"), "w", newline="\n") as f:
        f.write(head.strip() + "\ngrid\n")
        for row in g: f.write("".join(row) + "\n")
    print("drafted", name)

def body(g, n, c=6):
    """A big block body: diagonal light/dark split with a mid ridge, chamfered corners, a groove inset frame."""
    diag(g, inpoly([(c, 0), (n - c, 0), (n, c), (n, n - c), (n - c, n), (c, n), (0, n - c), (0, c)]), 'A', 'B', n / 2, n / 2, 'M')
    groove(g, minus(inrect(c + 1, c + 1, n - c - 2, n - c - 2), inrect(c + 2, c + 2, n - c - 3, n - c - 3)), n / 2, n / 2)

def ring(cx, cy, r0, r1): return lambda px, py: r0 * r0 <= (px - cx) ** 2 + (py - cy) ** 2 <= r1 * r1

# ============================== Orbital Weapons Platform ==============================
N = 160; C = 80
g = canvas(N); body(g, N)
CAPS = ((30, 30), (130, 30), (30, 130), (130, 130))
for (cx, cy) in CAPS:                                                              # power conduits to the centre
    for t in range(0, 101, 2):
        x, y = cx + (C - cx) * t / 100, cy + (C - cy) * t / 100
        fill(g, incircle(x, y, 3.2), 'X')
    for t in range(22, 70, 2):
        x, y = cx + (C - cx) * t / 100, cy + (C - cy) * t / 100
        fill(g, incircle(x, y, 1), 'c')
for (x0, y0, x1, y1) in ((62, 4, 98, 22), (62, 138, 98, 156), (4, 62, 22, 98), (138, 62, 156, 98)):   # emitter pylons
    part(g, inrect(x0, y0, x1, y1), (x0 + x1) / 2, (y0 + y1) / 2)
    vertical = y1 - y0 > x1 - x0
    for k in range(3):
        if vertical: fill(g, inrect(x0 + 4, y0 + 8 + k * 9, x1 - 4, y0 + 11 + k * 9), 'X'); fill(g, inrect(x0 + 6, y0 + 9 + k * 9, x1 - 6, y0 + 10 + k * 9), 'O')
        else: fill(g, inrect(x0 + 8 + k * 9, y0 + 4, x0 + 11 + k * 9, y1 - 4), 'X'); fill(g, inrect(x0 + 9 + k * 9, y0 + 6, x0 + 10 + k * 9, y1 - 6), 'O')
for (cx, cy) in CAPS:                                                              # capacitor banks (they light up as it charges)
    part(g, inpoly(octagon(cx, cy, 22)), cx, cy)
    for r in (17, 12):
        groove(g, ring(cx, cy, r, r + 1), cx, cy)
    fill(g, inpoly(octagon(cx, cy, 8)), 'X'); fill(g, inpoly(diamond(cx, cy, 5)), 'C'); fill(g, inrect(cx - 1, cy - 3, cx, cy - 2), 'W')
part(g, incircle(C, C, 46), C, C)                                                  # the central mount
groove(g, ring(C, C, 41, 42))
fill(g, incircle(C, C, 38), 'X')
diag(g, incircle(C, C, 36), 'H', 'X', C, C)
for k in range(8):                                                                 # mount bolts
    a = k * math.pi / 4 + math.pi / 8
    fill(g, incircle(C + 43.5 * math.cos(a), C + 43.5 * math.sin(a), 1.6), 'X')
write(g, "orbital-weapons-platform", header(N, "color C 8ae4ff\ncolor c 4fa7c7\ncolor W e8fbff\ncolor O d96b68\nmask glow C c W\nref foreshadow impact-reactor"))

# -dish (the aim layer, turns to the target): a compact emitter head; its lens sits 24px forward, where the charge glows
t = Grid(N)
t.rect(40, 70, 54, 104, 'a'); t.rect(47, 70, 54, 104, 'b')                         # side capacitor pods
for y in range(74, 102, 6): t.rect(42, y, 52, y + 2, 'c')
t.circle(80, 84, 30, 'a')                                                          # turret head
t.circle(80, 84, 22, 'b')                                                          # its raised top
t.ring(80, 84, 22, 23.2, '.')
t.rect(56, 82, 60, 88, 'a')                                                        # pod mounts
t.rect(66, 30, 80, 82, 'a'); t.rect(73, 30, 80, 82, 'b')                           # focusing barrel
for y in (34, 62):                                                                 # collars
    t.rect(62, y, 80, y + 6, 'k'); t.rect(71, y, 80, y + 6, 'l'); t.seam(62, y - 1, 80, y - 1); t.seam(62, y + 7, 80, y + 7)
t.circle(80, 56, 8, 'z'); t.ring(80, 56, 5, 8, 'd'); t.circle(80, 56, 2.5, 'e')    # the lens, 24px forward: the charge glows here
t.rect(70, 106, 80, 118, 'a'); t.rect(75, 106, 80, 118, 'b'); t.seam(70, 111, 80, 111)   # rear power coupling
t.circle(80, 84, 7, 'k'); t.circle(80, 84, 3, 'z')
t.outline('o', 3)
write_h = ("size 160 160\nmirror\ncolor a 4d4e58 b\ncolor b 7b7b7b a\ncolor z 2c2d38\ncolor k 3b3e4c l\ncolor l 5d6273 k\ncolor c 4fa7c7\n"
           "color d d96b68 e\ncolor e fdb380 d\ncolor o 404049\noutline 0\non orbital-weapons-platform\n")
t.write(OUT, "orbital-weapons-platform-dish", write_h)

# ============================== World Engine ==============================
N = 192; C = 96
EC = "color K 07080b\ncolor Y 8a6a32\ncolor y 5a4520\ncolor Z 1d2233\ncolor z 262c42\n"
PYLONS = ((26, 26), (166, 26), (26, 166), (166, 166))
BAYS = {"top": (60, 6, 132, 34), "bottom": (60, 158, 132, 186), "left": (6, 60, 34, 132), "right": (158, 60, 186, 132)}

g = canvas(N); body(g, N)
for (cx, cy) in PYLONS:                                                            # strut slots from the pylons (the foundation fills them)
    for t_ in range(17, 42, 2):
        x, y = cx + (C - cx) * t_ / 100, cy + (C - cy) * t_ / 100
        fill(g, incircle(x, y, 5), 'X')
for (x0, y0, x1, y1) in BAYS.values():                                             # empty bays waiting for their stage
    part(g, inrect(x0, y0, x1, y1), (x0 + x1) / 2, (y0 + y1) / 2)
    fill(g, inrect(x0 + 3, y0 + 3, x1 - 3, y1 - 3), 'X')
    diag(g, inrect(x0 + 4, y0 + 4, x1 - 4, y1 - 4), 'H', 'X', (x0 + x1) / 2, (y0 + y1) / 2)
    for (bx, by) in ((x0 + 6, y0 + 6), (x1 - 6, y0 + 6), (x0 + 6, y1 - 6), (x1 - 6, y1 - 6)): fill(g, incircle(bx, by, 1.4), 'G')
for (cx, cy) in PYLONS:                                                            # corner pylons
    part(g, inpoly(octagon(cx, cy, 17)), cx, cy)
    groove(g, ring(cx, cy, 10, 11), cx, cy)
    fill(g, inpoly(diamond(cx, cy, 5)), 'X')
part(g, incircle(C, C, 60), C, C)                                                  # the chamber housing
groove(g, ring(C, C, 54, 55))
fill(g, incircle(C, C, 50), 'X')
fill(g, incircle(C, C, 48), 'Z')                                                   # the deep blue sky round the eclipse
fill(g, both(incircle(C, C, 48), lambda px, py: (px - C) + (py - C) < -40), 'z')
fill(g, ring(C, C, 30, 32.5), 'Y')                                                 # the eclipse: unlit rim
fill(g, incircle(C, C, 30), 'K')                                                   # and the black disc
fill(g, both(ring(C, C, 30, 32.5), lambda px, py: (px - C) - (py - C) > 8), 'y')
write(g, "world-engine", header(N, EC + "ref impact-reactor core-nucleus"))

# stage 1, foundation: superalloy braces from the pylons, a riveted ring on the housing
g = canvas(N)
for (cx, cy) in PYLONS:
    for t_ in range(16, 41, 1):
        x, y = cx + (C - cx) * t_ / 100, cy + (C - cy) * t_ / 100
        fill(g, incircle(x, y, 4.2), 'S')
    for t_ in range(16, 41, 1):
        x, y = cx + (C - cx) * t_ / 100, cy + (C - cy) * t_ / 100
        fill(g, incircle(x - 1.2, y - 1.2, 1.6), 'T')
    for t_ in (22, 30, 37):
        x, y = cx + (C - cx) * t_ / 100, cy + (C - cy) * t_ / 100
        fill(g, incircle(x, y, 2), 's')
for k in range(24):
    a = k * math.pi / 12
    fill(g, incircle(C + 57.5 * math.cos(a), C + 57.5 * math.sin(a), 1.5), 's')
write(g, "world-engine-stage1", header(N, "color S c9b788\ncolor s 8a7a50\ncolor T e6d6a8\non world-engine"))

# stage 2, energy: superconductor coil banks in the side bays, cryo lines
g = canvas(N)
for (x0, y0, x1, y1) in (BAYS["left"], BAYS["right"]):
    for k in range(3):
        yy = y0 + 8 + k * 22
        part(g, inrect(x0 + 5, yy, x1 - 5, yy + 15), (x0 + x1) / 2, yy + 7.5)
        for x in range(x0 + 8, x1 - 6, 3): fill(g, inrect(x, yy + 3, x, yy + 12), 'C')
        fill(g, inrect(x0 + 7, yy + 6, x1 - 7, yy + 9), 'c')
    fill(g, inrect((x0 + x1) // 2 - 1, y0 - 4, (x0 + x1) // 2 + 1, y0 + 4), 'N')
write(g, "world-engine-stage2", header(N, "color C 8ae4ff\ncolor c 4fa7c7\ncolor N d8e4f0\non world-engine"))

# stage 3, chemistry: three process tanks in the bottom bay
g = canvas(N)
x0, y0, x1, y1 = BAYS["bottom"]
for k, (col, dk) in enumerate((("Q", "q"), ("P", "p"), ("R", "r"))):
    cx = x0 + 14 + k * 22
    part(g, incircle(cx, (y0 + y1) / 2, 11), cx, (y0 + y1) / 2)
    diag(g, incircle(cx, (y0 + y1) / 2, 7.5), col, dk, cx, (y0 + y1) / 2)
    fill(g, incircle(cx - 2, (y0 + y1) / 2 - 2, 1.8), 'W')
for k in range(2): fill(g, inrect(x0 + 25 + k * 22, (y0 + y1) / 2 - 1, x0 + 31 + k * 22, (y0 + y1) / 2 + 1), 'H')
write(g, "world-engine-stage3", header(N, "color Q e8b84a\ncolor q a8802a\ncolor P e3ecf0\ncolor p a8b4c0\ncolor R 6a6e7a\ncolor r 3a3c46\ncolor W ffffff\non world-engine"))

# stage 4, control: a console with three screens in the top bay
g = canvas(N)
x0, y0, x1, y1 = BAYS["top"]
part(g, inrect(x0 + 4, y0 + 3, x1 - 4, y1 - 3), (x0 + x1) / 2, (y0 + y1) / 2)
for k, (col, dk) in enumerate((("V", "v"), ("U", "u"), ("V", "v"))):
    sx = x0 + 10 + k * 19
    fill(g, inrect(sx, y0 + 7, sx + 14, y0 + 19), 'X'); diag(g, inrect(sx + 1, y0 + 8, sx + 13, y0 + 18), col, dk, sx + 7, y0 + 13)
for x in range(x0 + 10, x1 - 10, 4): fill(g, inrect(x, y1 - 9, x + 2, y1 - 7), 'L')
write(g, "world-engine-stage4", header(N, "color V 6dff9a\ncolor v 2f8a52\ncolor U 8ae4ff\ncolor u 3a7fa0\ncolor L ffd37f\non world-engine"))

# stage 5, confinement: four heavy magnets on the diagonals, coil faces turned to the core
g = canvas(N)
MAGNETS = [k * math.pi / 2 + math.pi / 4 for k in range(4)]
for a in MAGNETS:
    ca, sa = math.cos(a), math.sin(a)
    def pt(r, s_): return (C + ca * r - sa * s_, C + sa * r + ca * s_)
    part(g, inpoly([pt(34, -10), pt(52, -13), pt(52, 13), pt(34, 10)]), *pt(43, 0))
    for r in range(38, 51, 3): fill(g, inpoly([pt(r, -10), pt(r + 1, -10), pt(r + 1, 10), pt(r, 10)]), 'H')   # windings
    fill(g, inpoly([pt(34, -7), pt(36.5, -7), pt(36.5, 7), pt(34, 7)]), 'O')                              # coil face
write(g, "world-engine-stage5", header(N, "color O d96b68\non world-engine"))

# -glow (online, additive, tinted gold): a smooth graded corona round the disc and the four coil faces
g = canvas(N)
fill(g, ring(C, C, 32, 35), 'W'); fill(g, ring(C, C, 35, 39), 'w'); fill(g, ring(C, C, 39, 44), 'v')
for a in MAGNETS:
    ca, sa = math.cos(a), math.sin(a)
    def pt(r, s_): return (C + ca * r - sa * s_, C + sa * r + ca * s_)
    fill(g, inpoly([pt(34, -7), pt(36.5, -7), pt(36.5, 7), pt(34, 7)]), 'W')
write(g, "world-engine-glow", header(N, "color W ffffff\ncolor w 9a9a9a\ncolor v 4a4a4a\non world-engine"))
