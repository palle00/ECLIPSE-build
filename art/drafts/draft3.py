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


W = H = 64
HALF = 32
T2 = """
size 64 64
mirror
color a 4d4e58 b
color b 7b7b7b a
color z 2c2d38
color m 4d4e58 n
color n 7b7b7b m
base 2
"""
ORANGE = """
color d d96b68 e
color e fdb380 d
"""

# ---------------- Sickle: boomerang sawblade. Forked cradle holding the blade, body, rear catch magnet. -----------------
g = new()
# body
poly(g, [(12, 34), (20, 26), (32, 26), (32, 59), (17, 59), (12, 54)], 'a')
ridge(g, 15, 36, 24, 55, 20)
rect(g, 25, 30, 26, 57, '.')
# forked cradle: two prongs either side of the blade, orange tips
poly(g, [(14, 10), (21, 6), (24, 6), (24, 30), (14, 30)], 'm')
poly(g, [(19, 7.5), (21, 6), (24, 6), (24, 30), (19, 30)], 'n')
rect(g, 14, 6, 24, 9, 'm'); rect(g, 19, 6, 24, 9, 'n')
rect(g, 16, 6, 18, 8, 'd'); rect(g, 19, 6, 22, 8, 'e')
# the blade sitting in its recessed housing between the prongs
circle(g, 32, 20, 10, 'z')
circle(g, 32, 20, 8, 'k')
for (x, y) in ((24, 18), (26, 13), (30, 11)): rect(g, x, y, x + 1, y + 1, 'z')
circle(g, 32, 20, 2.5, 'z')
# rear catch magnet
rect(g, 27, 54, 31, 60, 'a'); rect(g, 28, 56, 31, 58, 'd')
write("sickle", g, T2 + ORANGE + """
color k 8c939e l
color l b4bcc4 k
heat m n d e
ref lancer salvo wave
""")

# ---------------- Kestrel: guided AA interceptor. One missile tube, twin sensor pods, slim angular hull. -----------------
g = new()
# slim angular hull
poly(g, [(13, 40), (22, 28), (32, 28), (32, 59), (19, 59), (13, 52)], 'a')
poly(g, [(18, 41), (24, 33), (26, 33), (26, 55), (18, 55)], 'b')
rect(g, 27, 30, 27, 57, '.')
# twin sensor pods with cyan lenses, on short struts
rect(g, 8, 18, 17, 30, 'a'); rect(g, 13, 18, 17, 30, 'b')
rect(g, 10, 20, 12, 24, 'd'); rect(g, 13, 20, 15, 24, 'e')
rect(g, 14, 31, 20, 34, 'a')
# the missile tube, its bore and the nose of the missile inside
rect(g, 26, 6, 32, 46, 'm')
rect(g, 26, 16, 32, 16, '.'); rect(g, 26, 36, 32, 36, '.')
rect(g, 29, 6, 32, 9, 'z'); rect(g, 30, 7, 32, 8, 'd')
write("kestrel", g, T2 + """
color d 4f9cc0 e
color e 8ad4ef d
heat m n
ref lancer salvo wave
""")

# ---------------- Minelayer: lobs mines. Short fat mortar tube, mine magazine with two mines, heavy body. -----------------
g = new()
# heavy body with side plates
poly(g, [(8, 32), (20, 20), (32, 20), (32, 60), (16, 60), (8, 52)], 'a')
ridge(g, 11, 30, 20, 56, 16)
rect(g, 21, 24, 22, 58, '.')
# mine magazine in the rear: two mines waiting in the loading bay
rect(g, 23, 40, 32, 58, 'z')
for cy in (44, 53): circle(g, 32, cy, 3.6, 'd')
# short fat mortar tube with a round bore
rect(g, 23, 6, 32, 30, 'm')
rect(g, 23, 18, 32, 18, '.')
circle(g, 32, 11, 4.5, 'z')
write("minelayer", g, T2 + ORANGE + """
heat m n
ref lancer salvo wave
""")

# ---------------- Acid Mortar: wide armoured body with side acid tanks, short fat banded tube. -----------------------------
g = new()
# wide body, angled shoulders up to the tube
poly(g, [(7, 34), (18, 22), (32, 22), (32, 60), (7, 60)], 'a')
# side acid tanks built into the body: long and rectangular, with a long acid window
ridge(g, 9, 32, 18, 57, 14)
rect(g, 11, 35, 16, 54, 'z'); rect(g, 12, 37, 13, 52, 'g'); rect(g, 14, 37, 15, 52, 'h')
rect(g, 19, 30, 20, 58, '.')
# feed line from the tank top into the breech
rect(g, 13, 27, 24, 30, 'a'); rect(g, 13, 31, 16, 31, 'a')
# forward armour cheeks beside the tube
poly(g, [(9, 24), (13, 12), (19, 9), (19, 24)], 'a')
poly(g, [(14, 24), (14, 11.5), (19, 9), (19, 24)], 'b')
rect(g, 9, 25, 19, 25, '.')
# breech plate
ridge(g, 21, 32, 31, 57, 26)
rect(g, 23, 40, 29, 43, 'g'); rect(g, 26, 40, 29, 43, 'h')
# short fat tube with a muzzle collar and the bore
rect(g, 22, 8, 32, 30, 'm')
rect(g, 22, 18, 32, 18, '.')
rect(g, 20, 4, 32, 9, 'a'); rect(g, 20, 10, 32, 10, '.')
rect(g, 27, 4, 32, 6, 'z')
write("acid-mortar", g, T2 + """
color g 7fa83d h
color h b4dc5a g
heat m n
ref lancer salvo wave
""")
