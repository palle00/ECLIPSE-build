"""The big blocks: Core: Bastion (6x6), Core: Citadel (7x7), Titan Manufacturing Complex (7x7), Fusion Reactor (5x5),
Temporal Interference Array, Chemical Synthesis Complex, Experimental Augmentation Facility, Combined-Cycle Turbine and
Breeder Reactor (4x4), each with its moving layers."""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import blocklib
from blocklib import inpoly, incircle, inrect, both, minus, octagon, diamond, diag, fill, part, groove
OUT = sys.argv[1]

def canvas(n):
    blocklib.W = blocklib.H = n
    return [['.'] * n for _ in range(n)]
def header(n, extra=""):
    return f"size {n} {n}\noutline 0\ncolor A b0b9c0\ncolor B 6e7080\ncolor M 989aa4\ncolor G 757887\ncolor H 5c5e6c\ncolor X 4a4b53\n" + extra
def write(g, name, head):
    with open(os.path.join(OUT, name + ".txt"), "w", newline="\n") as f:
        f.write(head.strip() + "\ngrid\n" + "\n".join("".join(r) for r in g) + "\n")
    print("drafted", name)
def body(g, n, c=6):
    diag(g, inpoly([(c, 0), (n - c, 0), (n, c), (n, n - c), (n - c, n), (c, n), (0, n - c), (0, c)]), 'A', 'B', n / 2, n / 2, 'M')
    groove(g, minus(inrect(c + 1, c + 1, n - c - 2, n - c - 2), inrect(c + 2, c + 2, n - c - 3, n - c - 3)), n / 2, n / 2)
def ring(cx, cy, r0, r1): return lambda px, py: r0 * r0 <= (px - cx) ** 2 + (py - cy) ** 2 <= r1 * r1
def window(g, test): fill(g, test, '.')
def small(n): return [['.'] * n for _ in range(n)]
def pt(cx, cy, a, r, s=0):
    ca, sa = math.cos(a), math.sin(a)
    return (cx + ca * r - sa * s, cy + sa * r + ca * s)
def radial(cx, cy, a, r0, r1, w0, w1=None):
    w1 = w0 if w1 is None else w1
    return inpoly([pt(cx, cy, a, r0, -w0), pt(cx, cy, a, r1, -w1), pt(cx, cy, a, r1, w1), pt(cx, cy, a, r0, w0)])
def outlet(n, name, extra=""):
    """-top1/-top2: heat outlet on the output face (turns with the block; the heat glow sits in its slot)."""
    t = canvas(n); c = n / 2
    part(t, inpoly([(n - 26, c - 16), (n - 1, c - 20), (n - 1, c + 20), (n - 26, c + 16)]), n - 12, c)
    fill(t, inrect(n - 10, c - 13, n - 1, c + 12), 'X'); fill(t, inrect(n - 10, c - 13, n - 9, c + 12), 'O')
    for k in (-8, 0, 8): groove(t, inrect(n - 23, c + k - 1, n - 14, c + k), n - 18, c + k)
    hd = header(n, "color O d96b68\n" + extra + f"on {name}")
    write(t, name + "-top1", hd); write(t[::-1], name + "-top2", hd)
    h = canvas(n); fill(h, inrect(n - 9, c - 12, n - 2, c + 11), 'W'); fill(h, inrect(n - 12, c - 9, n - 10, c + 8), 'w')
    write(h, name + "-heat", header(n, "color W ffffff\ncolor w 8a8a8a\n" + f"on {name}-top1"))   # the outlet slot glows with heat

ECL = "color K 07080b\ncolor Y 8a6a32\ncolor y 5a4520\n"
TEAM = "color T ffffff\ncolor U dcc6c6\n"

# ======================= cores: fortress walls and towers round a keep with the eclipse emblem =======================
def core(n, towers, keep_r, emblem_r, name):
    C = n / 2
    g = canvas(n); body(g, n, 8); team = canvas(n); glow = canvas(n)
    w = n * 0.12                                                                    # curtain walls along the edges
    for (x0, y0, x1, y1) in ((n * .22, 6, n * .78, 6 + w), (n * .22, n - 7 - w, n * .78, n - 7), (6, n * .22, 6 + w, n * .78), (n - 7 - w, n * .22, n - 7, n * .78)):
        part(g, inrect(x0, y0, x1, y1), (x0 + x1) / 2, (y0 + y1) / 2)
        horiz = x1 - x0 > y1 - y0
        for k in range(1, 7):                                                       # crenels
            if horiz: x = x0 + (x1 - x0) * k / 7; fill(g, inrect(x - 2, y0 + 3, x + 2, y1 - 3), 'H')
            else: y = y0 + (y1 - y0) * k / 7; fill(g, inrect(x0 + 3, y - 2, x1 - 3, y + 2), 'H')
        fill(team, inrect(x0 + 4, (y0 + y1) / 2 - 1, x1 - 4, (y0 + y1) / 2) if horiz else inrect((x0 + x1) / 2 - 1, y0 + 4, (x0 + x1) / 2, y1 - 4), 'T')
    for (tx, ty, tr) in towers:                                                     # towers
        part(g, inpoly(octagon(tx, ty, tr)), tx, ty)
        groove(g, ring(tx, ty, tr * .62, tr * .62 + 1), tx, ty)
        fill(g, inpoly(octagon(tx, ty, tr * .45)), 'X')
        fill(team, inpoly(diamond(tx, ty, tr * .48)), 'T'); fill(team, both(inpoly(diamond(tx, ty, tr * .48)), lambda px, py, tx=tx, ty=ty: px - py > tx - ty), 'U')
        fill(team, inpoly(diamond(tx, ty, tr * .2)), '.')
    groove(g, ring(C, C, keep_r + 6, keep_r + 8))                                   # courtyard ring
    for k in range(8):                                                              # buttresses round the keep
        a = k * math.pi / 4
        part(g, radial(C, C, a, keep_r * .8, keep_r + 9, 6, 8), *pt(C, C, a, keep_r + 3))
    part(g, inpoly(octagon(C, C, keep_r)), C, C)                                    # the keep
    groove(g, ring(C, C, keep_r * .78, keep_r * .78 + 1.2))
    for k in range(8):                                                              # team-coloured spokes and a band on the keep
        a = k * math.pi / 4 + math.pi / 8
        fill(team, radial(C, C, a, emblem_r + 9, keep_r * .92, 3, 4), 'T')
        fill(team, both(radial(C, C, a, emblem_r + 9, keep_r * .92, 3, 4), lambda px, py, a=a: (px - C) * math.sin(a) - (py - C) * math.cos(a) < 0), 'U')
    for k in range(16):
        a = k * math.pi / 8
        fill(team, both(ring(C, C, keep_r * .8, keep_r * .86), lambda px, py, a=a: abs(((math.atan2(py - C, px - C) - a + math.pi) % (2 * math.pi)) - math.pi) < .12), 'T')
    fill(g, incircle(C, C, emblem_r + 6), 'X')                                      # the vault, with the eclipse emblem
    fill(g, incircle(C, C, emblem_r + 4), 'H')
    fill(g, ring(C, C, emblem_r, emblem_r + 2.5), 'Y'); fill(g, both(ring(C, C, emblem_r, emblem_r + 2.5), lambda px, py: px - py > 4), 'y')
    fill(g, incircle(C, C, emblem_r), 'K')
    fill(glow, ring(C, C, emblem_r, emblem_r + 2.5), 'W'); fill(glow, ring(C, C, emblem_r + 2.5, emblem_r + 5), 'w')
    write(g, name, header(n, ECL + "ref core-nucleus core-foundation"))
    write(team, name + "-team", header(n, TEAM + f"on {name}"))
    write(glow, name + "-glow", header(n, f"color W ffffff\ncolor w 6a6a6a\non {name}"))

core(192, [(30, 30, 22), (162, 30, 22), (30, 162, 22), (162, 162, 22)], 50, 20, "core-bastion")
core(224, [(32, 32, 24), (192, 32, 24), (32, 192, 24), (192, 192, 24), (112, 20, 15), (112, 204, 15), (20, 112, 15), (204, 112, 15)], 64, 27, "core-citadel")

# ======================= Titan Manufacturing Complex: vehicle cradle, staging bays, gantry; -top frame; -crane =======================
N = 224; C = 112
g = canvas(N); body(g, N, 8)
fill(g, inrect(48, 28, 176, 196), 'X')                                              # the assembly bay (the titan is built here)
diag(g, inrect(50, 30, 174, 194), 'H', 'X', C, C)
for y in range(40, 192, 12): fill(g, inrect(50, y, 174, y), 'G')                   # floor grid
for x in range(60, 174, 12): fill(g, inrect(x, 30, x, 194), 'G')
for x0 in (68, 150):                                                                # vehicle cradle rails with clamps
    part(g, inrect(x0, 40, x0 + 6, 184), x0 + 3, C)
    for y in range(52, 180, 32): part(g, inrect(x0 - 4, y, x0 + 10, y + 6), x0 + 3, y + 3)
for (x0, y0, sx, sy) in ((52, 32, 1, 1), (172, 32, -1, 1), (52, 192, 1, -1), (172, 192, -1, -1)):   # alignment marks
    fill(g, inpoly([(x0, y0), (x0 + 12 * sx, y0), (x0, y0 + 12 * sy)]), 'O')
STOCK = (("Q", "q"), ("S", "s"), ("R", "r"), ("V", "v"))
for x0 in (10, 182):                                                                # material staging bays
    for i, (c1, c2) in enumerate(STOCK):
        y0 = 30 + i * 42
        part(g, inrect(x0, y0, x0 + 32, y0 + 36), x0 + 16, y0 + 18)
        fill(g, inrect(x0 + 4, y0 + 4, x0 + 28, y0 + 32), 'X')
        for k in range(4): diag(g, inrect(x0 + 6, y0 + 6 + k * 6, x0 + 26, y0 + 9 + k * 6), c1, c2, x0 + 16, y0 + 8 + k * 6)
write(g, "titan-manufacturing-complex", header(N, "color O d96b68\ncolor Q 8a8c96\ncolor q 5c5e6c\ncolor S cdb98a\ncolor s 8a7a50\ncolor R 6a6e80\ncolor r 3e4150\ncolor V d99d73\ncolor v a8664c\nref tetrative-reconstructor ground-factory"))
t = canvas(N)                                                                       # -top: corner gantry towers and the end beams (drawn over the titan)
for (cx, cy) in ((24, 18), (200, 18), (24, 206), (200, 206)):
    part(t, inrect(cx - 16, cy - 12, cx + 16, cy + 12), cx, cy); fill(t, incircle(cx, cy, 4), 'X')
for y0 in (8, 204):
    part(t, inrect(40, y0, 184, y0 + 12), C, y0 + 6)
    for x in range(48, 180, 10): fill(t, inrect(x, y0 + 3, x + 4, y0 + 9), 'H')
write(t, "titan-manufacturing-complex-top", header(N, "on titan-manufacturing-complex"))
t = canvas(N)                                                                       # -crane: travels along the titan as it is built
part(t, inrect(34, C - 7, 190, C + 7), C, C)
for x in range(40, 186, 8): fill(t, inrect(x, C - 3, x + 3, C + 3), 'H')
part(t, inrect(C - 16, C - 14, C + 16, C + 14), C, C); fill(t, inrect(C - 8, C - 6, C + 8, C + 6), 'X'); fill(t, incircle(C, C, 3), 'O')
write(t, "titan-manufacturing-complex-crane", header(N, "color O fdb380\non titan-manufacturing-complex"))

# ======================= Fusion Reactor: torus with field coils round the plasma window, cryo plants, injectors =======================
N = 160; C = 80
g = canvas(N); body(g, N, 6); glow = canvas(N)
for (cx, cy) in ((24, 24), (136, 24), (24, 136), (136, 136)):                       # cryo plants (liquid nitrogen) and injectors
    part(g, inpoly(octagon(cx, cy, 16)), cx, cy)
    fill(g, inpoly(octagon(cx, cy, 9)), 'X'); fill(g, inpoly(diamond(cx, cy, 6)), 'c' if cx == cy else 'P')
part(g, incircle(C, C, 66), C, C)                                                   # the torus (vacuum vessel)
groove(g, ring(C, C, 54, 55))
fill(g, ring(C, C, 46, 50), 'X')
for k in range(12):                                                                 # toroidal field coils
    a = k * math.pi / 6
    part(g, radial(C, C, a, 50, 68, 5, 7), *pt(C, C, a, 59))
    for r in range(53, 67, 3): fill(g, radial(C, C, a, r, r + 1, 4, 5), 'C')
    for r in range(53, 67, 3): fill(glow, radial(C, C, a, r, r + 1, 4, 5), 'W')
window(g, incircle(C, C, 46))                                                       # the plasma shows here
write(g, "fusion-reactor", header(N, "color C 8ae4ff\ncolor c 4fa7c7\ncolor P ffb3c8\nref impact-reactor thorium-reactor"))
write(glow, "fusion-reactor-glow", header(N, "color W ffffff\non fusion-reactor"))

# ======================= Temporal Interference Array: pylons round two counter-turning rings and a purple core =======================
N = 128; C = 64
g = canvas(N); body(g, N, 6); glow = canvas(N)
for k in range(4):                                                                  # focusing pylons on the diagonals
    a = k * math.pi / 2 + math.pi / 4
    part(g, radial(C, C, a, 40, 60, 7, 9), *pt(C, C, a, 50))
    tx, ty = pt(C, C, a, 54)
    fill(g, inpoly(diamond(tx, ty, 4)), 'V'); fill(glow, inpoly(diamond(tx, ty, 4)), 'W')
for (cx, cy, col) in ((C, 10, 'O'), (C, 118, 'O'), (10, C, 'c'), (118, C, 'c')):  # support nodes
    part(g, incircle(cx, cy, 6), cx, cy); fill(g, incircle(cx, cy, 2.5), col)
part(g, incircle(C, C, 42), C, C)
fill(g, incircle(C, C, 34), 'X'); diag(g, incircle(C, C, 32), 'H', 'X', C, C)
fill(g, inpoly(diamond(C, C, 9)), 'V'); fill(g, inpoly(diamond(C, C, 5)), 'v'); fill(glow, inpoly(diamond(C, C, 9)), 'W')   # the core
write(g, "temporal-interference-array", header(N, "color V b37aff\ncolor v e2ccff\ncolor O d96b68\ncolor c 8ae4ff\nref force-projector overdrive-dome"))
write(glow, "temporal-interference-array-glow", header(N, "color W ffffff\non temporal-interference-array"))
for name, r0, r1, gaps, node in (("-ring-a", 25, 31, 4, 'O'), ("-ring-b", 14, 19, 3, 'c')):
    t = canvas(N)
    for y in range(N):
        for x in range(N):
            px, py = x + .5 - C, y + .5 - C; r = math.hypot(px, py)
            a = math.atan2(py, px) % (2 * math.pi)
            if r0 <= r <= r1 and (a % (2 * math.pi / gaps)) > .35: t[y][x] = 'A' if px - py > 0 else 'B'
    for k in range(gaps):
        a = k * 2 * math.pi / gaps + .7
        nx, ny = pt(C, C, a, (r0 + r1) / 2); fill(t, incircle(nx, ny, 2), node)
    write(t, "temporal-interference-array" + name, header(N, "color O fdb380\ncolor c 8ae4ff\non temporal-interference-array"))

# ======================= Chemical Synthesis Complex: pressure chamber, cryo section, catalyst bank, manifolds, control =======================
N = 128; C = 64
g = canvas(N); body(g, N, 6)
part(g, inrect(10, 6, 118, 18), C, 12)                                              # feed manifolds
for x in range(18, 112, 16): part(g, inrect(x, 3, x + 8, 21), x + 4, 12); fill(g, inrect(x + 2, 7, x + 6, 17), 'X')
part(g, inrect(10, 108, 118, 122), C, 115)                                          # control housing
fill(g, inrect(18, 111, 50, 119), 'X'); diag(g, inrect(19, 112, 49, 118), 'U', 'u', 34, 115)
for x in range(58, 110, 8): fill(g, inrect(x, 113, x + 4, 117), 'L')
fill(g, inrect(104, 112, 108, 116), 'E')                                            # the exotic accent
part(g, inrect(20, 26, 42, 102), 31, 64)                                            # cryogenic section (liquid nitrogen shows)
window(g, inrect(24, 30, 38, 98))
for y in range(34, 96, 7): part(g, inrect(24, y, 38, y + 2), 31, y + 1)            # cooling fins over it
part(g, inrect(88, 26, 108, 102), 98, 64)                                           # catalyst bank
for y0 in (32, 54, 76):
    fill(g, inrect(91, y0, 105, y0 + 16), 'X'); diag(g, inrect(93, y0 + 2, 103, y0 + 14), 'O', 'o', 98, y0 + 8)
    fill(g, inrect(95, y0 + 5, 101, y0 + 6), 'L')
part(g, incircle(C, C, 22), C, C)                                                   # the pressure synthesis chamber
groove(g, ring(C, C, 17, 18))
window(g, incircle(C, C, 14))
for (x0, x1) in ((42, 46), (82, 86)): part(g, inrect(x0, 61, x1, 67), (x0 + x1) / 2, 64)
write(g, "chemical-synthesis-complex", header(N, "color U 8ae4ff\ncolor u 3a7fa0\ncolor L 8ae4ff\ncolor E c88fff\ncolor O d96b68\ncolor o 8a3a2a\nmask glow L E\nref cryofluid-mixer plastanium-compressor"))
t = canvas(N)
for k in range(3):
    a = k * 2 * math.pi / 3 + .3
    diag(t, radial(C, C, a, 3, 12, 1.8, 2.6), 'A', 'B', *pt(C, C, a, 7), 'M')
part(t, incircle(C, C, 3.5), C, C)
write(t, "chemical-synthesis-complex-rotator", header(N, "on chemical-synthesis-complex"))

# ======================= Experimental Augmentation Facility: four stations round the precision bed, two swinging arms =======================
N = 128; C = 64
g = canvas(N); body(g, N, 6)
part(g, inrect(36, 6, 92, 24), C, 15)                                               # station: material feed (top)
for x in range(42, 88, 11): fill(g, inrect(x, 10, x + 7, 20), 'X'); fill(g, inrect(x + 2, 13, x + 5, 17), 'Q')
part(g, inrect(36, 104, 92, 122), C, 113)                                           # station: inspection / output (bottom)
fill(g, inrect(40, 108, 88, 118), 'X'); fill(g, inrect(44, 112, 84, 113), 'U')
for (x0, col) in ((6, 'U'), (104, 'O')):                                            # stations: laser etcher (left), forge (right)
    part(g, inrect(x0, 36, x0 + 18, 92), x0 + 9, 64)
    fill(g, inrect(x0 + 3, 40, x0 + 15, 88), 'X')
    for y in range(44, 86, 10): fill(g, inrect(x0 + 5, y, x0 + 13, y + 4), col)
part(g, inrect(30, 30, 98, 98), C, C)                                               # precision assembly bed (the module is drawn here)
fill(g, inrect(34, 34, 94, 94), 'X'); diag(g, inrect(35, 35, 93, 93), 'H', 'X', C, C)
for (x0, y0, sx, sy) in ((36, 36, 1, 1), (92, 36, -1, 1), (36, 92, 1, -1), (92, 92, -1, -1)):
    fill(g, inrect(min(x0, x0 + 7 * sx), y0, max(x0, x0 + 7 * sx), y0), 'V'); fill(g, inrect(x0, min(y0, y0 + 7 * sy), x0, max(y0, y0 + 7 * sy)), 'V')
for (cx, cy) in ((26, 26), (102, 26)): part(g, incircle(cx, cy, 7), cx, cy); fill(g, incircle(cx, cy, 2.5), 'X')   # arm bases
write(g, "experimental-augmentation-facility", header(N, "color Q d99d73\ncolor U 8ae4ff\ncolor O fdb380\ncolor V c88fff\nmask glow V U O\nref multi-press surge-smelter"))
for name, cx, sx in (("-arm-l", 26, 1), ("-arm-r", 102, -1)):
    t = canvas(N)
    part(t, inpoly([(cx - 3, 24), (cx + 3, 28), (C - 14 * sx + 3, 52), (C - 14 * sx - 3, 48)]) if sx > 0 else inpoly([(cx + 3, 24), (cx - 3, 28), (C + 14 + 3, 48), (C + 14 - 3, 52)]), (cx + C) / 2, 38)
    tx = C - 14 * sx
    part(t, incircle(tx, 50, 4), tx, 50); fill(t, incircle(tx + 2 * sx, 53, 1.6), 'V')
    fill(t, incircle(cx, 26, 3), 'M'); fill(t, incircle(cx, 26, 1.5), 'X')
    write(t, "experimental-augmentation-facility" + name, header(N, "color V e2ccff\non experimental-augmentation-facility"))

# ======================= Combined-Cycle Turbine: intake, combustor, turbine rotor, heat-recovery section; outlet =======================
N = 128; C = 64
g = canvas(N); body(g, N, 6)
part(g, incircle(20, C, 15), 20, C); fill(g, incircle(20, C, 12), 'X')            # compressor intake
for k in range(10):
    a = k * math.pi / 5; fill(g, radial(20, C, a, 3, 11, .8, 1.4), 'G')
part(g, inrect(34, 40, 52, 88), 43, C)                                              # combustor
for y in range(46, 84, 8): fill(g, inrect(38, y, 48, y + 4), 'X'); fill(g, inrect(40, y + 1, 46, y + 3), 'F')
part(g, incircle(72, C, 20), 72, C); fill(g, incircle(72, C, 16), 'X')            # turbine housing (the rotor spins in it)
part(g, inrect(94, 22, 116, 106), 105, C)                                           # heat-recovery section
for y in range(28, 102, 6): diag(g, inrect(98, y, 112, y + 2), 'M', 'G', 105, y + 1)
part(g, incircle(105, 14, 7), 105, 14); fill(g, incircle(105, 14, 4), 'X')         # exhaust stack
rotor = small(34)
for y in range(34):
    for x in range(34):
        px, py = x + .5 - 17, y + .5 - 17; r, a = math.hypot(px, py), math.atan2(py, px)
        if r <= 15.5:
            k = ((a + r * .07) % (2 * math.pi / 14)) / (2 * math.pi / 14)
            rotor[y][x] = ('A' if k < .35 else 'M' if k < .6 else 'B') if r > 5 else ('G' if r > 2.5 else 'X')
for y in range(34):
    for x in range(34):
        if rotor[y][x] != '.': g[C - 17 + y][72 - 17 + x] = rotor[y][x]
write(g, "combined-cycle-turbine", header(N, "color F fdb380\nmask flame F\nref differential-generator steam-engine"))
glow = canvas(N)                                                                    # heated: the heat-recovery fins warm up
for y in range(28, 102, 6): fill(glow, inrect(98, y, 112, y + 2), 'w')
write(glow, "combined-cycle-turbine-glow", header(N, "color w 9a9a9a\non combined-cycle-turbine"))
with open(os.path.join(OUT, "combined-cycle-turbine-rotor.txt"), "w", newline="\n") as f:
    f.write("size 34 34\noutline 0\ncolor A b0b9c0\ncolor B 6e7080\ncolor M 989aa4\ncolor G 757887\ncolor X 4a4b53\ngrid\n" + "\n".join("".join(r) for r in rotor) + "\n")
print("drafted combined-cycle-turbine-rotor")
outlet(N, "combined-cycle-turbine")

# ======================= Breeder Reactor: sodium pool, hex fuel core inside a breeding blanket, pumps; outlet =======================
N = 128; C = 64
g = canvas(N); body(g, N, 6); core = canvas(N)
for (x0, y0, x1, y1) in ((10, 10, 34, 30), (94, 98, 118, 118)):                    # intermediate heat exchangers
    part(g, inrect(x0, y0, x1, y1), (x0 + x1) / 2, (y0 + y1) / 2)
    for y in range(y0 + 4, y1 - 2, 4): fill(g, inrect(x0 + 4, y, x1 - 4, y + 1), 'H')
part(g, incircle(C, C, 48), C, C)                                                   # reactor vessel
fill(g, incircle(C, C, 41), 'X'); diag(g, incircle(C, C, 39), 'Z', 'z', C, C)      # the liquid sodium pool
def hexes(rad, spacing):
    out = []
    for q in range(-4, 5):
        for r_ in range(-4, 5):
            x = C + spacing * (q + r_ / 2); y = C + spacing * r_ * .866
            if math.hypot(x - C, y - C) <= rad: out.append((x, y))
    return out
for (x, y) in hexes(25, 7):                                                         # breeding blanket
    fill(g, inpoly(octagon(x, y, 3.2)), 'W')
for (x, y) in hexes(13, 7):                                                         # fuel core (glows)
    fill(g, inpoly(octagon(x, y, 3.2)), 'F'); fill(core, inpoly(octagon(x, y, 3.2)), 'W')
for (cx, cy) in ((34, 92), (94, 34)):                                               # sodium pumps (impellers spin)
    part(g, incircle(cx, cy, 9), cx, cy); fill(g, incircle(cx, cy, 6.5), 'X')
imp = small(14)
for y in range(14):
    for x in range(14):
        px, py = x + .5 - 7, y + .5 - 7; r, a = math.hypot(px, py), math.atan2(py, px)
        if r <= 6 and ((a % (math.pi / 2)) < .6 or r < 2): imp[y][x] = 'A' if r >= 2 else 'X'
for (cx, cy) in ((34, 92), (94, 34)):
    for y in range(14):
        for x in range(14):
            if imp[y][x] != '.': g[cy - 7 + y][cx - 7 + x] = imp[y][x]
write(g, "breeder-reactor", header(N, "color Z 3d4a5c\ncolor z 2e3846\ncolor W 9a8a7a\ncolor F e07ab8\nref thorium-reactor impact-reactor"))
write(core, "breeder-reactor-core", header(N, "color W ffffff\non breeder-reactor"))
glow = canvas(N)                                                                    # heated: the breeding blanket warms up
for (x, y) in hexes(25, 7): fill(glow, inpoly(octagon(x, y, 3.2)), 'w')
write(glow, "breeder-reactor-glow", header(N, "color w 8a8a8a\non breeder-reactor"))
with open(os.path.join(OUT, "breeder-reactor-pump.txt"), "w", newline="\n") as f:
    f.write("size 14 14\noutline 0\ncolor A b0b9c0\ncolor X 4a4b53\ngrid\n" + "\n".join("".join(r) for r in imp) + "\n")
print("drafted breeder-reactor-pump")
outlet(N, "breeder-reactor")
