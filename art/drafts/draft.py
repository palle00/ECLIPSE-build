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

# ---------------- Heat Lance: heat-powered beam. Long focusing emitter, thick thermal housing, radiator fins. -------------
g = new()
# radiator fins on the outer flanks (heat structures), 4px fins with 2px seams
for y in range(48, 84, 6):
    ridge(g, 6, y, 19, y + 3, 12)
# thermal housing: broad block with long diagonal shoulders towards the emitter
poly(g, [(16, 52), (34, 30), (48, 30), (48, 90), (28, 90), (16, 80)], 'a')
poly(g, [(19, 54), (34, 36), (38, 36), (38, 86), (29, 86), (19, 78)], 'a')
poly(g, [(28, 43), (34, 36), (38, 36), (38, 86), (29, 86), (28, 85)], 'b')
rect(g, 39, 32, 40, 88, '.')
# heat vents in the side plates: glowing slots
for y in (52, 60, 68, 76):
    rect(g, 24, y, 33, y + 3, 'd')
    rect(g, 29, y, 33, y + 3, 'e')
# central spine and emitter barrel
rect(g, 41, 14, 47, 90, 'a')
# focusing collars
for y in (20, 31):
    rect(g, 36, y, 47, y + 4, 'a')
    rect(g, 36, y - 1, 47, y - 1, '.')
    rect(g, 36, y + 5, 47, y + 5, '.')
# emitter head: a wide focusing prism with the lens in its throat
poly(g, [(36, 14), (40, 4), (48, 4), (48, 17), (36, 17)], 'a')
rect(g, 43, 5, 47, 6, 'z')
rect(g, 44, 7, 47, 13, 'd')
# rear heat intake
rect(g, 30, 87, 40, 91, 'a')
rect(g, 33, 88, 37, 90, 'z')
write("heat-lance", g, TURRET + """
color d d96b68 e
color e fdb380 d
heat d e
ref meltdown lancer ripple
""")

# ---------------- Tremor: ground-slam. Square impact plate, four hydraulic rams, braced corner feet. ----------------------
g = new()
# corner feet and diagonal braces (reinforced outer support)
poly(g, [(6, 14), (14, 6), (24, 6), (24, 12), (12, 24), (6, 24)], 'a')
poly(g, [(15, 12), (21, 12), (12, 21), (12, 15)], 'b')
poly(g, [(6, 72), (12, 72), (24, 84), (24, 90), (14, 90), (6, 82)], 'a')
poly(g, [(12, 75), (15, 84), (21, 84), (12, 75)], 'b')
poly(g, [(16, 22), (22, 16), (34, 28), (28, 34)], 'b')
poly(g, [(16, 74), (22, 80), (34, 68), (28, 62)], 'a')
# hydraulic rams: a fat cylinder and a rod reaching into the plate. North/south straddle the axis.
rect(g, 38, 6, 47, 17, 'a'); rect(g, 38, 12, 47, 12, '.'); rect(g, 43, 18, 47, 27, 'a')
rect(g, 38, 78, 47, 89, 'a'); rect(g, 38, 83, 47, 83, '.'); rect(g, 43, 68, 47, 77, 'a')
ridge(g, 6, 38, 17, 57, 12); rect(g, 12, 38, 12, 57, '.'); ridge(g, 18, 43, 27, 52, 23)
# impact plate: heavy square with chamfered corners, a raised pressing face and a boss in the middle
poly(g, [(36, 27), (48, 27), (48, 69), (36, 69), (27, 60), (27, 36)], 'a')
poly(g, [(38, 32), (48, 32), (48, 64), (38, 64), (32, 58), (32, 38)], 'b')
poly(g, [(41, 37), (48, 37), (48, 59), (41, 59), (37, 55), (37, 41)], 'a')
# hammer face: a raised square boss in the middle of the plate
rect(g, 40, 40, 47, 55, 'b'); rect(g, 43, 43, 47, 52, 'a')
# chunky inward chevrons on the plate rim: the force goes into the ground (they glow when it slams)
poly(g, [(28, 38), (32, 38), (37, 48), (32, 58), (28, 58), (33, 48)], 'd')
poly(g, [(35, 28), (48, 34), (48, 38), (35, 32)], 'd')
poly(g, [(35, 68), (48, 62), (48, 58), (35, 64)], 'd')
write("tremor", g, TURRET + """
color d d96b68 e
color e fdb380 d
heat d e
ref ripple fuse
""")

# ---------------- Cloudbreaker: anti-air mine layer. Central hub with a mine magazine, six launch tubes around it. --------
g = new()
# arms from hub to tubes
for (x0, y0, x1, y1) in ((22, 21, 36, 28), (14, 44, 30, 51), (22, 68, 36, 75)):
    rect(g, x0, y0, x1, y1, 'a')
    rect(g, x0, y0 + (y1 - y0) // 2, x1, y0 + (y1 - y0) // 2, '.')
def tube(cx, cy):
    poly(g, [(cx - 10, cy - 5), (cx - 5, cy - 10), (cx + 5, cy - 10), (cx + 10, cy - 5), (cx + 10, cy + 5), (cx + 5, cy + 10), (cx - 5, cy + 10), (cx - 10, cy + 5)], 'a')
    poly(g, [(cx, cy - 10), (cx + 5, cy - 10), (cx + 10, cy - 5), (cx + 10, cy + 5), (cx + 5, cy + 10), (cx, cy + 10)], 'b')
    circle(g, cx, cy, 6.6, 'm'); circle(g, cx + .5, cy, 6.6, 'n')
    for y in range(H):
        for x in range(HALF):
            if g[y][x] in 'mn' and x + .5 < cx: g[y][x] = 'm'
    circle(g, cx, cy, 4.6, 'z')
    circle(g, cx + 1, cy - 1, 2, 'd'); circle(g, cx + 1.5, cy - 1.5, 1, 'e')
tube(17, 20); tube(13, 48); tube(17, 76)
# central hub: long plated body pointing forward
poly(g, [(38, 10), (48, 10), (48, 86), (36, 86), (28, 76), (28, 24)], 'a')
poly(g, [(31, 26), (38, 16), (40, 16), (40, 84), (37, 84), (31, 75)], 'b')
rect(g, 41, 16, 41, 84, '.')
rect(g, 42, 16, 47, 84, 'a')
# mine magazine down the spine: three cyan mines in their housings
for cy in (34, 48, 62):
    rect(g, 42, cy - 5, 47, cy + 5, 'z')
    circle(g, 48, cy, 4, 'd'); circle(g, 48.5, cy - 1, 1.6, 'e')
# forward director (targeting sensor)
poly(g, [(42, 4), (48, 4), (48, 12), (40, 12)], 'a'); rect(g, 45, 5, 47, 8, 'e')
write("cloudbreaker", g, TURRET + """
color d 4f9cc0 e
color e 8ad4ef d
color m 4d4e58 n
color n 7b7b7b m
heat m n
ref ripple fuse
""")
