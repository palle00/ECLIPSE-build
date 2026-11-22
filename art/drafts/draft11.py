"""3x3 support blocks in the vanilla block style: diagonal light/dark split (light top-right), bold shapes, few tones."""
import sys, os, math
OUT = sys.argv[1]
W = H = 96

def new(): return [['.'] * W for _ in range(H)]
def write(g, name, header):
    with open(os.path.join(OUT, name + ".txt"), "w", newline="\n") as f:
        f.write(header.strip() + "\ngrid\n")
        for row in g: f.write("".join(row) + "\n")
    print("drafted", name)

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
def both(a, b): return lambda px, py: a(px, py) and b(px, py)
def minus(a, b): return lambda px, py: a(px, py) and not b(px, py)
def octagon(cx, cy, r, c=None):
    c = c if c is not None else r * .42
    return [(cx - r + c, cy - r), (cx + r - c, cy - r), (cx + r, cy - r + c), (cx + r, cy + r - c), (cx + r - c, cy + r), (cx - r + c, cy + r), (cx - r, cy + r - c), (cx - r, cy - r + c)]
def diamond(cx, cy, r): return [(cx, cy - r), (cx + r, cy), (cx, cy + r), (cx - r, cy)]

def diag(g, test, light, dark, cx=48, cy=48, mid=None, band=1.2):
    """Block shading: top-right of the part's diagonal is light, bottom-left dark, optional mid band."""
    for y in range(H):
        for x in range(W):
            if not test(x + .5, y + .5): continue
            d = (x + .5 - cx) - (y + .5 - cy)
            g[y][x] = mid if (mid and abs(d) < band) else (light if d > 0 else dark)
def fill(g, test, ch):
    for y in range(H):
        for x in range(W):
            if test(x + .5, y + .5): g[y][x] = ch
def part(g, test, cx, cy, mid='M'):
    """A raised part: light top-right, dark bottom-left, mid on the ridge, and a 1px groove around it."""
    ring = lambda px, py: not test(px, py) and any(test(px + dx, py + dy) for dx in (-1, 0, 1) for dy in (-1, 0, 1))
    diag(g, ring, 'G', 'H', 48, 48)
    diag(g, test, 'A', 'B', cx, cy, mid)
def groove(g, test, cx=48, cy=48):
    diag(g, test, 'G', 'H', cx, cy)

BLOCK = """
size 96 96
outline 0
color A b0b9c0
color B 6e7080
color M 989aa4
color G 757887
color H 5c5e6c
color X 4a4b53
"""

def base(g):
    """The 3x3 body: diagonal split, a groove inset frame, chamfered corners."""
    body = inpoly([(4, 0), (92, 0), (96, 4), (96, 92), (92, 96), (4, 96), (0, 92), (0, 4)])
    diag(g, body, 'A', 'B', 48, 48, 'M')
    groove(g, minus(inrect(5, 5, 90, 90), inrect(7, 7, 88, 88)))

# ---------------- Emergency Repair Dispatch Bay: service pit, rod stock, arm with green emitter ----------------
g = new(); base(g)
part(g, inrect(12, 12, 70, 70), 41, 41)                                  # bay frame
fill(g, inrect(17, 17, 65, 65), 'X')                                      # the service pit
diag(g, inrect(19, 19, 63, 63), 'H', 'X', 41, 41)
fill(g, inrect(38, 22, 44, 60), 'G'); fill(g, inrect(22, 38, 60, 44), 'G')   # landing cross
for (cx, cy) in ((20, 20), (62, 20), (20, 62), (62, 62)):                 # corner status lights
    fill(g, inrect(cx - 1, cy - 1, cx, cy), 'R')
part(g, inrect(74, 12, 86, 70), 80, 41)                                   # repair-stock rack
for y in range(16, 67, 6):
    fill(g, inrect(76, y, 84, y + 3), 'X'); fill(g, inrect(77, y + 1, 83, y + 2), 'T')   # titanium rods
part(g, inrect(12, 74, 86, 86), 49, 80)                                   # arm rail along the bottom
fill(g, inrect(14, 79, 84, 80), 'X')
write(g, "emergency-repair-dispatch-bay", BLOCK + """
color R 62ae7e
color S 84f490
color T 8da1b8
ref mend-projector vault
""")
g = new()                                                                 # -arm: slides along the rail while repairing
part(g, inrect(37, 72, 45, 84), 41, 78)                                   # arm carriage
part(g, inrect(39, 44, 43, 74), 41, 59)                                   # arm reaching over the pit
part(g, inrect(33, 38, 45, 46), 39, 42)                                   # emitter head
fill(g, inrect(35, 40, 42, 43), 'R'); fill(g, inrect(39, 40, 42, 41), 'S')
write(g, "emergency-repair-dispatch-bay-arm", BLOCK + "color R 62ae7e\ncolor S 84f490\non emergency-repair-dispatch-bay")

# ---------------- Unit Shield Beacon: cyan crystal in an octagon, four focusing prongs, power socket ----------------
g = new(); base(g)
part(g, inpoly(octagon(48, 48, 30)), 48, 48)                               # support housing
groove(g, minus(inpoly(octagon(48, 48, 25)), inpoly(octagon(48, 48, 23))))
diag(g, inpoly(octagon(48, 48, 17)), 'M', 'M')
main = g; g = new()                                                       # -prongs: turn slowly round the crystal
for k in range(4):                                                        # focusing prongs, aimed at the crystal
    a = math.pi / 4 + k * math.pi / 2
    ca, sa = math.cos(a), math.sin(a)
    def pt(r, s): return (48 + ca * r - sa * s, 48 + sa * r + ca * s)
    prong = inpoly([pt(20, -4), pt(41, -6), pt(43, 0), pt(41, 6), pt(20, 4)])
    px, py = pt(31, 0)
    part(g, prong, px, py)
    tx, ty = pt(23, 0)
    fill(g, incircle(tx, ty, 2.2), 'K')
write(g, "unit-shield-beacon-prongs", BLOCK + "color K 4fa7c7\non unit-shield-beacon")
g = main
fill(g, inpoly(diamond(48, 48, 11)), 'X')
diag(g, inpoly(diamond(48, 48, 9)), 'D', 'C', 48, 48)                      # the shield crystal
fill(g, inpoly(diamond(51, 45, 3)), 'E')
part(g, inrect(40, 84, 56, 92), 48, 88); fill(g, inrect(43, 87, 53, 89), 'X')   # power socket
fill(g, inrect(45, 87, 46, 88), 'O'); fill(g, inrect(50, 87, 51, 88), 'O')
write(g, "unit-shield-beacon", BLOCK + """
color C 4fa7c7
color D 8ae4ff
color E d4f6ff
color O fdb380
mask crystal C D E
ref force-projector mend-projector
""")

# ---------------- Wave Disruption Array: central pulse emitter, four directional antennas ----------------
g = new(); base(g)
for (cx, cy) in ((14, 14), (82, 14), (14, 82), (82, 82)):                 # field coils, out of the antennas' sweep
    part(g, inpoly(diamond(cx, cy, 8)), cx, cy)
    fill(g, inpoly(diamond(cx, cy, 4.5)), 'X'); fill(g, inpoly(diamond(cx, cy, 2.5)), 'K')
main = g; g = new()                                                       # -antennas: sweep slowly round the emitter
for (x0, y0, x1, y1, cx, cy) in ((42, 12, 54, 30, 48, 21), (42, 66, 54, 84, 48, 75), (12, 42, 30, 54, 21, 48), (66, 42, 84, 54, 75, 48)):
    part(g, inrect(x0, y0, x1, y1), cx, cy)                                # antenna arms
for (cx, cy) in ((48, 13), (48, 83), (13, 48), (83, 48)):                 # antenna dishes with cyan tips
    part(g, incircle(cx, cy, 9), cx, cy)
    fill(g, incircle(cx, cy, 5), 'X'); fill(g, incircle(cx, cy, 2.5), 'C')
write(g, "wave-disruption-array-antennas", BLOCK + "color C 8ae4ff\non wave-disruption-array")
write(main, "wave-disruption-array", BLOCK + "color K d96b68\nmask glow K\nref radar force-projector")
g = new()                                                                 # -emitter: drawn over the antennas
part(g, incircle(48, 48, 23), 48, 48)                                      # pulse emitter housing
groove(g, minus(incircle(48, 48, 18), incircle(48, 48, 16)))
diag(g, incircle(48, 48, 16), 'M', 'M')
fill(g, incircle(48, 48, 11), 'X')
diag(g, incircle(48, 48, 8), 'P', 'O', 48, 48)                            # orange core (the pulse is drawn here)
fill(g, incircle(50, 46, 2.5), 'Q')
write(g, "wave-disruption-array-emitter", BLOCK + """
color O d96b68
color P fdb380
color Q ffe1c4
on wave-disruption-array
""")

# ---------------- Component Depot: four hatched component bays around a transfer hub ----------------
g = new(); base(g)
bays = ((10, 10, 42, 42), (54, 10, 86, 42), (10, 54, 42, 86), (54, 54, 86, 86))
for (x0, y0, x1, y1) in bays:
    part(g, inrect(x0, y0, x1, y1), (x0 + x1) / 2, (y0 + y1) / 2)          # bay hatch
    fill(g, inrect(x0 + 4, y0 + 4, x1 - 4, y1 - 4), 'X')
    diag(g, inrect(x0 + 5, y0 + 5, x1 - 5, y1 - 5), 'G', 'H', (x0 + x1) / 2, (y0 + y1) / 2)
def gear(cx, cy):                                                          # copper gear
    fill(g, incircle(cx, cy, 8), 'Q'); fill(g, incircle(cx, cy, 6), 'P')
    for k in range(8):
        a = k * math.pi / 4
        fill(g, incircle(cx + math.cos(a) * 8.5, cy + math.sin(a) * 8.5, 2), 'P')
    fill(g, incircle(cx, cy, 2.5), 'X')
gear(26, 26)
diag(g, inrect(62, 20, 78, 32), 'U', 'V', 70, 26)                          # titanium plate
fill(g, inrect(62, 25, 78, 26), 'V')
fill(g, incircle(26, 70, 9), 'V'); fill(g, incircle(26, 70, 7), 'U'); fill(g, incircle(26, 70, 3.5), 'X')   # bearing
fill(g, inrect(62, 62, 78, 64), 'W'); fill(g, inrect(62, 76, 78, 78), 'W')                                   # superalloy frame
fill(g, inrect(62, 62, 64, 78), 'W'); fill(g, inrect(76, 62, 78, 78), 'W'); fill(g, inrect(64, 64, 76, 65), 'Y')
fill(g, inrect(36, 46, 60, 50), 'X'); fill(g, inrect(46, 36, 50, 60), 'X')  # transfer channels
hub = new()                                                                # -hub: turns while the depot holds items
part(hub, inpoly(octagon(48, 48, 11)), 48, 48)
fill(hub, inrect(39, 46, 57, 50), 'X'); fill(hub, inrect(46, 39, 50, 57), 'X')
fill(hub, incircle(48, 48, 4), 'O')
write(hub, "component-depot-hub", BLOCK + "color O fdb380\non component-depot")
write(g, "component-depot", BLOCK + """
color P d99d73
color Q a8664c
color U c3d0e6
color V 8da1b8
color W 9aa3b5
color Y d4dbe8
color O fdb380
ref vault container
""")

# ---------------- Robot Dispatch Hub: four open docking pads around a charging spire ----------------
g = new(); base(g)
for (cx, cy) in ((24, 24), (72, 24), (24, 72), (72, 72)):
    part(g, inpoly(octagon(cx, cy, 16)), cx, cy)                            # pad rim
    fill(g, inpoly(octagon(cx, cy, 12)), 'X')                              # open docking space
    diag(g, inpoly(octagon(cx, cy, 11)), 'H', 'X', cx, cy)
    fill(g, inrect(cx - 7, cy - 1, cx + 7, cy), 'G'); fill(g, inrect(cx - 1, cy - 7, cx, cy + 7), 'G')   # landing cross
    lx, ly = (cx - 11 if cx < 48 else cx + 10), (cy - 11 if cy < 48 else cy + 10)
    fill(g, inrect(lx, ly, lx + 1, ly + 1), 'S')                           # pad indicator
for (x0, y0, x1, y1) in ((40, 22, 56, 26), (40, 70, 56, 74), (22, 40, 26, 56), (70, 40, 74, 56)):
    groove(g, inrect(x0, y0, x1, y1))                                      # charging conduits
ring = new()                                                               # -ring: charging coil turning round the spire
for k in range(4):
    a0 = k * math.pi / 2 + .25
    seg = lambda px, py, a0=a0: 14.5 <= math.hypot(px - 48, py - 48) <= 18 and (math.atan2(py - 48, px - 48) - a0) % (2 * math.pi) < 1.0
    part(ring, seg, 48 + 16 * math.cos(a0 + .5), 48 + 16 * math.sin(a0 + .5))
    fill(ring, both(seg, lambda px, py: 16 <= math.hypot(px - 48, py - 48) <= 16.8), 'R')
write(ring, "robot-dispatch-hub-ring", BLOCK + "color R 62ae7e\non robot-dispatch-hub")
part(g, inpoly(octagon(48, 48, 13)), 48, 48)                               # charging spire
fill(g, inpoly(octagon(48, 48, 8)), 'X')
diag(g, inpoly(octagon(48, 48, 6)), 'S', 'R', 48, 48)
fill(g, inrect(49, 45, 51, 46), 'T')
write(g, "robot-dispatch-hub", BLOCK + """
color R 62ae7e
color S 84f490
color T d8ffe0
mask glow S R T
ref ground-factory mend-projector
""")
