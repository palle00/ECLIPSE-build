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


ORANGE = """
color d d96b68 e
color e fdb380 d
"""
HEATBODY = """
color m 4d4e58 n
color n 7b7b7b m
"""
COPPER = """
color k a8664c l
color l dc9b74 k
"""

def shade_split(g, cx, chars=('m', 'n')):
    """Inside a shape drawn with chars[0], pixels right of cx become chars[1] (ridge shading for an off-axis part)."""
    for y in range(H):
        for x in range(HALF):
            if g[y][x] == chars[0] and x + .5 >= cx: g[y][x] = chars[1]

# ---------------- Thunderhead: chain-lightning AA. Five electrodes on a capacitor housing. -------------------------------
g = new()
# capacitor housing at the back, insulator ribs on its side plates
poly(g, [(16, 58), (26, 46), (48, 46), (48, 89), (24, 89), (16, 81)], 'a')
ridge(g, 20, 54, 37, 85, 29)
for y in (60, 66, 72, 78): rect(g, 20, y, 37, y, '.')
rect(g, 38, 50, 39, 87, '.')
# side electrodes: a forward prong and a sideways prong per side, cyan tips
poly(g, [(19, 52), (31, 52), (19, 22), (9, 26)], 'a')
poly(g, [(25, 52), (31, 52), (19, 22), (14, 24)], 'b')
rect(g, 10, 17, 18, 24, 'a'); rect(g, 14, 17, 18, 24, 'b')
rect(g, 11, 18, 13, 22, 'd'); rect(g, 14, 18, 16, 22, 'e')
ridge(g, 6, 62, 17, 69, 12)
rect(g, 4, 60, 9, 71, 'a'); rect(g, 6, 61, 8, 70, 'd'); rect(g, 9, 61, 9, 70, 'e')
# central discharge column with charged coil collars and the main electrode
rect(g, 42, 12, 47, 89, 'a')
for y in (22, 31, 40):
    rect(g, 37, y, 47, y + 4, 'm')
    rect(g, 37, y - 1, 47, y - 1, '.')
rect(g, 39, 4, 47, 12, 'a'); rect(g, 41, 13, 47, 13, '.')
rect(g, 42, 6, 47, 10, 'd')
write("thunderhead", g, TURRET + ORANGE.replace("d96b68", "4f9cc0").replace("fdb380", "8ad4ef") + HEATBODY + """
heat m n
ref lancer ripple fuse
""")

# ---------------- Piledriver: heavy kinetic cannon. Long thick barrel, breech with a carbide slug, recoil cylinders. ------
g = new()
# recoil cylinders flanking the breech, warning bands
ridge(g, 17, 50, 26, 91, 22)
for y in (56, 84): rect(g, 17, y, 26, y + 2, 'd'); rect(g, 22, y, 26, y + 2, 'e')
# breech block and rear plate
poly(g, [(28, 51), (35, 44), (48, 44), (48, 76), (28, 76)], 'a')
poly(g, [(36, 44), (41, 38), (48, 38), (48, 44)], 'a')
ridge(g, 30, 50, 39, 72, 35)
rect(g, 40, 46, 41, 74, '.')
rect(g, 32, 78, 47, 90, 'a'); rect(g, 32, 77, 47, 77, '.')
# carbide slug window in the breech side
rect(g, 32, 56, 37, 66, 'z'); rect(g, 33, 58, 36, 64, 'k'); rect(g, 35, 58, 36, 64, 'l')
# long thick barrel with bands and a reinforced muzzle (it heats after firing)
rect(g, 41, 12, 47, 44, 'm')
for y in (22, 33): rect(g, 41, y, 47, y, '.')
rect(g, 37, 4, 47, 12, 'a'); rect(g, 37, 13, 47, 13, '.')
rect(g, 45, 4, 47, 7, 'z')
write("piledriver", g, TURRET + ORANGE + HEATBODY + """
color k 3b3e4c l
color l 5d6273 k
heat m n
ref ripple fuse spectre
""")

# ---------------- Apiary: drone carrier. Honeycomb launch bays, central dispatch housing, amber launch lights. ------------
g = new()
def hexagon(cx, cy, r): return [(cx, cy - r), (cx + .87 * r, cy - r / 2), (cx + .87 * r, cy + r / 2), (cx, cy + r), (cx - .87 * r, cy + r / 2), (cx - .87 * r, cy - r / 2)]
def bay(cx, cy):
    poly(g, hexagon(cx, cy, 11), 'a')
    poly(g, [(cx, cy - 11), (cx + 9.6, cy - 5.5), (cx + 9.6, cy + 5.5), (cx, cy + 11)], 'b')
    poly(g, hexagon(cx, cy, 6.5), 'z')
    rect(g, cx - 1, cy - 10, cx, cy - 8, 'd'); rect(g, cx + 1, cy - 10, cx + 2, cy - 8, 'e')
# frame between the bays and the housing
for cy in (24, 48, 72):
    rect(g, 22, cy - 3, 33, cy + 3, 'a'); rect(g, 22, cy, 33, cy, '.')
bay(15, 24); bay(15, 48); bay(15, 72)
# central dispatch housing: hexagonal, with a control dome and forward sensor
poly(g, [(32, 20), (48, 10), (48, 86), (32, 76)], 'a')
poly(g, [(36, 24), (40, 21.5), (40, 74.5), (36, 72)], 'b')
rect(g, 41, 14, 41, 82, '.')
poly(g, hexagon(48, 48, 9), 'd'); poly(g, hexagon(48, 48, 5), 'z')
rect(g, 43, 4, 47, 12, 'a'); rect(g, 45, 5, 47, 8, 'e')
for y in (26, 66): rect(g, 42, y, 47, y + 3, 'z')
write("apiary", g, TURRET + ORANGE + """
heat d e
ref ripple fuse
""")

# ---------------- Primer: precise electrical emitter. Slim barrel, copper capacitor rings, lit core in a round housing. ---
g = new()
# round housing at the back with power couplings
circle(g, 48, 66, 22, 'a')
circle(g, 48, 66, 16, 'b')
circle(g, 48, 66, 13, 'a')
circle(g, 48, 66, 7, 'd')
rect(g, 22, 62, 27, 70, 'a'); rect(g, 24, 62, 27, 70, 'b'); rect(g, 22, 61, 27, 61, '.'); rect(g, 22, 71, 27, 71, '.')
rect(g, 40, 88, 47, 91, 'a')
# slim emitter barrel, copper capacitor rings, orange-lit tip
rect(g, 44, 10, 47, 46, 'a')
for y in (16, 25, 34):
    rect(g, 39, y, 47, y + 4, 'k'); rect(g, 39, y - 1, 47, y - 1, '.')
rect(g, 42, 4, 47, 9, 'a'); rect(g, 44, 5, 47, 8, 'd')
write("primer", g, TURRET + ORANGE + COPPER + """
heat d e
ref lancer ripple
""")

# ---------------- Stardust Launcher: scatter gun. Rotary drum with coloured windows, three short tubes. -------------------
g = new()
# drum magazine
circle(g, 48, 58, 25, 'a')
circle(g, 48, 58, 19, 'b')
circle(g, 48, 58, 16, 'a')
circle(g, 48, 58, 6, 'b'); circle(g, 48, 58, 3, 'z')
# coloured glass windows around the drum (sand, titanium, thorium, surge, plastanium, spore across both halves)
for (wx, wy, c) in ((26, 56, 'p'), (31, 41, 'r'), (31, 71, 't'), (40, 34, 'p'), (40, 79, 'r')):
    rect(g, wx - 3, wy - 3, wx + 2, wy + 2, 'z'); rect(g, wx - 2, wy - 2, wx + 1, wy + 1, c)
# three short launch tubes with copper bands
for (x0, x1, y0) in ((31, 38, 14), (42, 47, 8)):
    rect(g, x0, y0, x1, 36, 'm')
    rect(g, x0, y0 + 9, x1, y0 + 11, 'k')
    rect(g, x0 + 2, y0, x1 - 2 if x1 < 47 else 47, y0 + 1, 'z')
shade_split(g, 34.5)
rect(g, 39, 12, 41, 34, '.')
write("stardust-launcher", g, TURRET + COPPER + HEATBODY + """
color p f7cba4 q
color q 8da1e3 p
color r f9a3c7 s
color s f3e979 r
color t cbd97f u
color u 9a7ce0 t
heat m n
ref ripple fuse
""")
