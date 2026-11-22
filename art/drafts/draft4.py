"""Draft rasteriser: turns shape lists into left-half letter grids for tools/PixelArt.java.
Full-canvas coordinates (96x96, turret points up); only x <= 47 is kept (the right half is mirrored by PixelArt).
Letters: a dark body / b light body, d/e accent pair, z barrel hole, '.' empty (becomes a dark outline seam in game)."""
import sys, os

W = H = 96
HALF = W // 2

def new(): return [['.'] * HALF for _ in range(H)]

def rect(g, x0, y0, x1, y1, ch):
    for y in range(max(0, y0), min(H - 1, y1) + 1):
        for x in range(max(0, x0), min(HALF - 1, x1) + 1):
            g[y][x] = ch

def poly(g, pts, ch):
    """Fill pixels whose centre is inside the polygon."""
    for y in range(H):
        for x in range(HALF):
            px, py = x + .5, y + .5
            inside = False
            j = len(pts) - 1
            for i in range(len(pts)):
                xi, yi = pts[i]; xj, yj = pts[j]
                if (yi > py) != (yj > py) and px < (xj - xi) * (py - yi) / (yj - yi) + xi:
                    inside = not inside
                j = i
            if inside: g[y][x] = ch

def circle(g, cx, cy, r, ch):
    for y in range(H):
        for x in range(HALF):
            if (x + .5 - cx) ** 2 + (y + .5 - cy) ** 2 <= r * r: g[y][x] = ch

def ridge(g, x0, y0, x1, y1, split=None):
    """A plate shaded the vanilla way: dark left part, lit right part."""
    s = split if split is not None else (x0 + x1 + 1) // 2
    rect(g, x0, y0, s - 1, y1, 'a'); rect(g, s, y0, x1, y1, 'b')

def write(name, g, header):
    path = os.path.join(sys.argv[1], name + ".txt")
    with open(path, "w", newline="\n") as f:
        f.write(header.strip() + "\ngrid\n")
        for row in g: f.write("".join(row) + "\n")
    print("drafted", path)

TURRET = """
size 96 96
mirror
color a 4d4e58 b
color b 7b7b7b a
color z 2c2d38
base 3
"""


import math

def diag(g, test, light, dark, cx, cy, mid=None):
    """Block shading: pixels of a shape top-right of its centre diagonal are light, bottom-left dark (optional mid band)."""
    for y in range(H):
        for x in range(HALF):
            if not test(x + .5, y + .5): continue
            d = (x + .5 - cx) - (y + .5 - cy)
            g[y][x] = mid if (mid and abs(d) < 1.2) else (light if d > 0 else dark)

def inpoly(pts):
    def t(px, py):
        inside = False; j = len(pts) - 1
        for i in range(len(pts)):
            xi, yi = pts[i]; xj, yj = pts[j]
            if (yi > py) != (yj > py) and px < (xj - xi) * (py - yi) / (yj - yi) + xi: inside = not inside
            j = i
        return inside
    return t
def incircle(cx, cy, r): return lambda px, py: (px - cx) ** 2 + (py - cy) ** 2 <= r * r
def inrect(x0, y0, x1, y1): return lambda px, py: x0 <= px < x1 + 1 and y0 <= py < y1 + 1
def octagon(cx, cy, r, c=None):
    c = c if c is not None else r * .42
    return [(cx - r + c, cy - r), (cx + r - c, cy - r), (cx + r, cy - r + c), (cx + r, cy + r - c), (cx + r - c, cy + r), (cx - r + c, cy + r), (cx - r, cy + r - c), (cx - r, cy - r + c)]
def rotpoly(pts, cx, cy, ang):
    ca, sa = math.cos(ang), math.sin(ang)
    return [(cx + (x - cx) * ca - (y - cy) * sa, cy + (x - cx) * sa + (y - cy) * ca) for (x, y) in pts]

BLOCK = """
size 64 64
outline 0
color A b0b9c0
color B 6e7080
color M 989aa4
color F a8b1b8
color G 757887
color H 5c5e6c
color X 4a4b53
"""
W = H = HALF = 64

def projector_base(g):
    diag(g, inrect(0, 0, 63, 63), 'A', 'B', 32, 32, 'M')
    # thin octagon frame lines (inverted shading reads as a groove); the diagonal split shows through between them
    ring = lambda px, py: inpoly(octagon(32, 32, 24))(px, py) and not inpoly(octagon(32, 32, 22))(px, py)
    diag(g, ring, 'G', 'H', 32, 32)
    diag(g, inpoly(octagon(32, 32, 17)), 'M', 'M', 32, 32)
    # socket for the spinning dish
    circle(g, 32, 32, 12.5, 'X')

# ---------------- Jammer: purple emitter posts in the corners, outward chevrons, socket for the spinning emitter. ----------
g = new()
projector_base(g)
for (cx, cy) in ((7, 7), (57, 7), (7, 57), (57, 57)):
    diag(g, inpoly(octagon(cx, cy, 6, 2)), 'A', 'B', cx, cy)
    diag(g, inrect(cx - 2, cy - 2, cx + 1, cy + 1), 'P', 'Q', cx, cy)
def chevron_out(ax, ay, dx, dy):
    # a 4px thick chevron whose point faces (dx, dy), centred on (ax, ay)
    px, py = -dy, dx
    pts = [(ax - px * 8 - dx * 3, ay - py * 8 - dy * 3), (ax + dx * 2, ay + dy * 2), (ax + px * 8 - dx * 3, ay + py * 8 - dy * 3),
           (ax + px * 8 - dx * 7, ay + py * 8 - dy * 7), (ax - dx * 2, ay - dy * 2), (ax - px * 8 - dx * 7, ay - py * 8 - dy * 7)]
    diag(g, inpoly(pts), 'P', 'Q', ax, ay)
chevron_out(32, 4, 0, -1); chevron_out(32, 60, 0, 1); chevron_out(4, 32, -1, 0); chevron_out(60, 32, 1, 0)
write("jammer", g, BLOCK + """
color P c49cff
color Q 8657cf
ref mend-projector overdrive-projector
""")

# jammer dish: three emitter blades round a purple core (spins; the glow pulses purple)
g = new()
blade = [(30, 32), (34, 32), (35.5, 18), (32, 16), (28.5, 18)]
for k in range(3):
    pts = rotpoly(blade, 32, 32, k * 2 * math.pi / 3)
    diag(g, inpoly(pts), 'A', 'B', sum(p[0] for p in pts) / 5, sum(p[1] for p in pts) / 5, 'M')
    tip = rotpoly([(32, 18.5)], 32, 32, k * 2 * math.pi / 3)[0]
    circle(g, tip[0], tip[1], 2.3, 'P')
diag(g, incircle(32, 32, 7), 'A', 'B', 32, 32, 'M')
circle(g, 32, 32, 4.5, 'X')
circle(g, 32, 32, 3, 'P'); circle(g, 32.8, 31.2, 1.3, 'L')
write("jammer-dish", g, BLOCK + """
color P b37aff
color L e2ccff
mask =jammer-glow P L
on jammer
""")

# ---------------- Sonar: hydrophone pods on the edges, cyan lights, socket for the spinning transducer. -------------------
g = new()
projector_base(g)
for (cx, cy) in ((32, 6), (32, 58), (6, 32), (58, 32)):
    diag(g, incircle(cx, cy, 5.6), 'A', 'B', cx, cy)
    circle(g, cx, cy, 3.4, 'X')
    diag(g, incircle(cx, cy, 2.2), 'C', 'D', cx, cy)
for (cx, cy) in ((7, 7), (57, 7), (7, 57), (57, 57)):
    diag(g, inpoly(octagon(cx, cy, 5, 2)), 'A', 'B', cx, cy)
    rect(g, cx - 1, cy - 1, cx, cy, 'X')
write("sonar", g, BLOCK + """
color C 8ae4ff
color D 4fa7c7
ref mend-projector overdrive-projector
""")

# sonar dish: a transducer ring with a sweep arm and a cyan emitter head, counterweight behind
g = new()
diag(g, incircle(32, 32, 11), 'A', 'B', 32, 32, 'M')
circle(g, 32, 32, 8, 'X')
diag(g, incircle(32, 32, 6), 'A', 'B', 32, 32, 'M')
diag(g, inrect(30, 8, 33, 24), 'A', 'B', 32, 16)
diag(g, inrect(26, 4, 37, 10), 'A', 'B', 32, 7)
rect(g, 28, 5, 35, 8, 'C')
diag(g, inrect(28, 42, 35, 49), 'A', 'B', 32, 46, 'M')
circle(g, 32, 32, 2.5, 'C')
write("sonar-dish", g, BLOCK + """
color C 7fe0ff
mask =sonar-glow C
on sonar
""")

# ---------------- Rivet: copper T1 gatling. Copper body, three steel barrels, clamp ring (barrels glow as it spins up). ---
W = H = 32
HALF = 16
g = new()
# copper body with a white steel highlight stripe, like the vanilla copper turrets
poly(g, [(6, 19), (10, 14), (16, 14), (16, 28), (9, 28), (6, 25)], 'a')
poly(g, [(11, 17), (13, 16), (16, 16), (16, 26), (11, 26)], 'b')
rect(g, 14, 18, 16, 26, 'w')
# barrels: two side, one centre pair, coming out of a round drum
for x0 in (10, 14):
    rect(g, x0, 4, x0 + 1, 15, 'm')
rect(g, 9, 9, 16, 10, 's')
circle(g, 16, 18, 4.6, 's')
circle(g, 16, 18, 2, 'z')
write("rivet", g, """
size 32 32
mirror
color a 8f665b b
color b c8a58f a
color s 4d4e58 t
color t 7b7b7b s
color m b0b9c0 n
color n f4f4f4 m
color z 2c2d38
color w b0b9c0 v
color v f4f4f4 w
base 1
heat m n
ref duo hail arc
""")
