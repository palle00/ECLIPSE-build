"""The Swarm, third pass (2026-11-22, after the user's reference sheet "Giftige insektoide svaerm - evolutionstrin"):
one creature in evolution stages, each as large as the Dominion unit of its tier (mite 48 = reaper, wasp 48 = wraith,
spitter 72 = bulwark, brood 96 = rampart, behemoth 128 = annihilator, scrabba 200 = colossus).
- The silhouette comes from long hairy jointed legs and the bone at the front: mandibles, a pair of horns, from the
  brood on a pair of mantis scythes.
- Charcoal chitin in overlapping gritty plates, a bone ridge of spikes down the spine.
- Toxic neon green as the only light: a star-shaped core, veins along the plate seams, slime dripping off the rear.
Every unit is designed on a 96 x 96 canvas (front at the top, the left half mirrored) and re-rendered at its own size,
so bigger units get finer detail rather than bigger pixels. Writes each unit with its -base and its legs (-leg, -leg-base,
-joint; see leg()); the Wasp's wings are lifted out by unitparts.py, the glow masks made by unitglow.py. Run: python -I art/drafts/swarm.py <out dir>"""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from unitlib import *
import unitlib

SWARM = """
color g 454b3d
color h 2c3027
color i 191c15
color k 060705
color P d6c899
color O a9966a
color N 66573a
color A 8ff03c
color B e4ff9c
color C 3d7d1c
color T ffa664
color U d06b53
mask cell T:ffffff U:dcc6c6
team e05438
outline 3 060705
"""
SWARMPART = chr(10).join(l for l in SWARM.split(chr(10)) if not l.startswith(("mask", "team")))
C = 48

def chitin(g, test, **kw): g.panel(test, light='g', mid='h', dark='i', seamch='k', **kw)

def _curve(pts, n=48):
    """Points along a Bezier curve through the control points."""
    out = []
    for s in range(n + 1):
        t = s / n
        p = list(pts)
        while len(p) > 1: p = [(p[i][0] + (p[i + 1][0] - p[i][0]) * t, p[i][1] + (p[i + 1][1] - p[i][1]) * t) for i in range(len(p) - 1)]
        out.append((p[0][0], p[0][1], t))
    return out

def stroke(pts, w0, w1, dx=0.0, dy=0.0, k=1.0):
    """A tapering stroke along a curve (w0 wide at its start, w1 at its tip), in design units."""
    samples = [(x + dx, y + dy, (w0 + (w1 - w0) * t) * k) for (x, y, t) in _curve(pts)]
    x0 = min(s[0] - s[2] for s in samples); x1 = max(s[0] + s[2] for s in samples)
    y0 = min(s[1] - s[2] for s in samples); y1 = max(s[1] + s[2] for s in samples)
    def t(px, py):
        if px < x0 or px > x1 or py < y0 or py > y1: return False
        return any((px - x) ** 2 + (py - y) ** 2 <= w * w for (x, y, w) in samples)
    return unitlib._wrap(t)

def bone(g, pts, w0, w1):
    """A curved bone claw or horn: dark underside, mid face, a highlight along its upper-left edge, a dark seam."""
    full = stroke(pts, w0, w1)
    g.fill(stroke(pts, w0 + 0.9, w1 + 0.7), 'k')
    g.fill(full, 'N')
    g.fill(both(full, stroke(pts, w0 * 0.75, w1 * 0.7, -0.6, -0.6)), 'O')
    g.fill(both(full, stroke(pts, w0 * 0.3, w1 * 0.3, -1.0, -1.0)), 'P')

def vein(g, pts, w=0.55, bright=False):
    g.fill(stroke(pts, w + 0.5, w * 0.5 + 0.3), 'C')
    g.fill(stroke(pts, w, w * 0.4), 'B' if bright else 'A')

def ellipse(cx, cy, rx, ry): return unitlib._wrap(lambda px, py: ((px - cx) / rx) ** 2 + ((py - cy) / ry) ** 2 <= 1)

def grit(g, seed=1):
    """Gritty chitin: a scatter of pixels a shade darker or lighter on the plates."""
    for y in range(g.h):
        for x in range(g.hw):
            c = g.g[y][x]
            if c not in "ghi": continue
            r = ((x * 73856093) ^ (y * 19349663) ^ (seed * 83492791)) % 100
            if r < 11: g.g[y][x] = {'g': 'h', 'h': 'i', 'i': 'k'}[c]
            elif r < 17: g.g[y][x] = {'g': 'g', 'h': 'g', 'i': 'h'}[c]

def creature(g, tier, scythes=None):
    """The Swarm body plan; tier 1 (mite) to 4 (behemoth) adds horns, veins and eggs. The legs are separate sprites
    (the game draws them jointed under the body), so the bone only grows from the head - plus, from tier 3, one pair
    of mantis scythes beside it (the Scrabba has its pincers instead)."""
    if scythes if scythes is not None else tier >= 3:
        bone(g, [(30, 40), (12, 36), (8, 22), (16, 12)], 3.6, 0.6)
    # the abdomen: overlapping plates from the rear forwards
    body = ellipse(C, 60, 22, 30)
    chitin(g, body, rim=3)
    for (yc, r) in ((96, 34), (84, 30), (72, 28), (60, 26)):                          # plate edges: arcs across the back
        g.fill(both(body, ring(C, yc - r - 6, r, r + 1.1)), 'k')
    # shoulder plates
    chitin(g, poly([(20, 40), (28, 30), (38, 32), (36, 48), (24, 52)]), rim=2)
    chitin(g, poly([(24, 58), (32, 52), (40, 58), (36, 72), (26, 74)]), rim=2)
    # head shield
    chitin(g, poly([(32, 34), (38, 22), (45, 17), (C, 16), (C, 40), (36, 42)]), rim=2)
    # mandibles: big crescents from the head, the tips turning in
    bone(g, [(39, 24), (26, 16), (28, 2), (40, 0)], 3.8 if tier >= 3 else 3.2, 0.5)
    bone(g, [(44, 24), (39, 13), (43, 5)], 2.4 if tier >= 3 else 2.0, 0.4)                 # a pair of horns on the head, forward
    # the spine: a bone ridge of vertebrae down the middle
    for y in range(18 if tier >= 2 else 22, 90, 6 if tier >= 2 else 8):
        g.fill(poly([(C - 3.2, y + 1), (C, y - 2.6), (C + 0.01, y + 3.6), (C - 1, y + 3)]), 'k')
        g.fill(poly([(C - 2.2, y + 1), (C, y - 1.6), (C + 0.01, y + 2.6)]), 'O')
        g.fill(poly([(C - 1.2, y + 0.4), (C, y - 1), (C + 0.01, y + 1)]), 'P')
    # veins from the core along the plate seams
    vein(g, [(C - 3, 50), (40, 48), (37, 44), (32, 45)], 0.5)
    vein(g, [(C - 3, 55), (42, 58), (40, 63), (35, 64)], 0.5)
    if tier >= 2:
        vein(g, [(C - 3, 60), (44, 67), (42, 72), (43, 76)], 0.45)
        vein(g, [(C - 3, 45), (44, 41), (43, 36), (40, 34)], 0.45)
    # the core: a star of light
    g.fill(poly([(C - 10, 52), (C - 4, 48), (C, 36), (C + 0.01, 68), (C - 4, 56)]), 'C')
    g.fill(poly([(C - 7.5, 52), (C - 3, 48.5), (C, 40), (C + 0.01, 64), (C - 3, 55.5)]), 'A')
    g.fill(poly([(C - 4, 52), (C - 1.4, 49.5), (C, 45), (C + 0.01, 59), (C - 1.4, 54.5)]), 'B')
    # glowing eyes under the head shield
    for (x, y) in ((42, 26), (40, 30), (44, 31)):
        g.fill(rect(x - 1, y - 1, x + 1, y + 1), 'k'); g.fill(rect(x, y, x, y), 'A')
    # egg clutches in the flanks
    if tier >= 3:
        for (cx, cy) in ((30, 60), (31, 70)):
            g.fill(circ(cx, cy, 3.6), 'k'); g.fill(circ(cx - 1, cy, 1.5), 'A'); g.fill(circ(cx + 1.4, cy + 1.2, 1.4), 'A')
            g.fill(circ(cx - 1.3, cy - 0.4, 0.5), 'B')
    # slime off the rear
    for (x, y0, ln) in ((44, 86, 6), (40, 84, 4)) + (((36, 80, 5),) if tier >= 3 else ()):
        g.fill(rect(x, y0, x, y0 + ln), 'C'); g.fill(rect(x, y0, x, y0 + ln - 2), 'A')
    # the team mark: a chevron on the rear plate
    g.fill(poly([(C - 7, 82), (C, 77), (C + 0.01, 80), (C - 7, 85)]), 'T'); g.fill(poly([(C - 7, 84), (C, 79), (C + 0.01, 80), (C - 7, 85)]), 'U')

def unit(name, size, tier, header=""):
    w, h = scaled(size / 96, 96, 96)
    g = Grid(w, h)
    creature(g, tier)
    unscaled()
    grit(g, size)
    g.mirror()
    g.write(name, SWARM + header + "size %d %d\non %s-base\nref crawler dagger" % (size, size, name))

LEGPART = SWARMPART + "color Y 5a553c\ncolor Z 9a8f66\n"

def limb(g, parts):
    """A round chitin limb from strokes (pts, w0, w1): dark underside, a mid band and a highlight along its upper edge."""
    def layer(k, dy):
        ts = [stroke(p, w0 * k, w1 * k, 0, dy * w0) for (p, w0, w1) in parts]
        return lambda px, py: any(t(px, py) for t in ts)
    g.fill(lambda px, py: any(stroke(p, w0 + 0.8, w1 + 0.8)(px, py) for (p, w0, w1) in parts), 'k', full=True)
    g.fill(layer(1, 0), 'i', full=True)
    g.fill(layer(0.72, -0.22), 'h', full=True)
    g.fill(layer(0.34, -0.5), 'g', full=True)

def hairs(g, x0, x1, cy, r0, r1, ln, step, seed):
    """Coarse bristles along a horizontal shaft (radius r0 at x0 to r1 at x1): out of both edges, uneven, bent and swept
    towards x1, with now and then a long one. Drawn over the shaft's edge so the silhouette is fuzzy."""
    x, i = x0 + step * 0.3, 0
    while x < x1:
        rnd = ((int(x * 7) * 73856093) ^ (seed * 19349663) ^ (i * 83492791)) % 1000 / 1000
        r = r0 + (r1 - r0) * (x - x0) / (x1 - x0)
        side = 1 if (i + (rnd > 0.8)) % 2 else -1
        l = ln * (0.5 + 0.5 * rnd) * (1.5 if rnd > 0.88 else 1)
        a = math.radians(28 + 50 * ((rnd * 13) % 1))
        root = (x, cy + side * r * 0.7)
        tip = (x + l * math.cos(a), cy + side * (r + l * math.sin(a)))
        bend = ((root[0] + tip[0]) / 2 + l * 0.25 * (rnd - 0.5), (root[1] + tip[1]) / 2 + side * l * 0.15)
        g.fill(stroke([root, bend, tip], 0.62, 0.5), 'Y', full=True)
        g.fill(stroke([bend, tip], 0.5, 0.45), 'Z', full=True)
        x += step * (0.5 + ((rnd * 7) % 1)); i += 1

def leg(name, L, T):
    """A long hairy leg for the game's jointed legs, as strips along their length (the game stretches each from end to
    end and mirrors it on the other side): NAME-leg the thigh from the body (left) to the knee (right), NAME-leg-base
    the shin from the knee to a hooked bone claw, NAME-joint a round knee (drawn unrotated). T is the shaft's width."""
    hl = T + 2                                                                                # bristle length
    h = int(T + 2 * hl + 2); cy = h / 2
    g = Grid(L, h); g.hw = L
    limb(g, [([(-1, cy), (L * 0.5, cy - 0.4), (L + 1, cy)], T / 2, T * 0.38),
             ([(L * 0.12, cy), (L * 0.4, cy - 0.6), (L * 0.62, cy)], T * 0.5, T * 0.62)])        # a swollen thigh
    for x in (L * 0.3, L * 0.68):                                                             # segment notches
        g.fill(rect(x, cy - T / 2, x, cy + T / 2), 'k')
    grit(g, L)
    hairs(g, 0, L, cy, T / 2, T * 0.38, hl, max(1.4, T * 0.4), len(name))
    if T >= 4:                                                                                # pus sacs
        for (x, s) in ((L * 0.45, -1), (L * 0.8, 1)):
            g.fill(circ(x, cy + s * T * 0.15, T * 0.3 + 0.4), 'C'); g.fill(circ(x - 0.3, cy + s * T * 0.15 - 0.3, T * 0.18 + 0.2), 'A')
    g.write(name + "-leg", LEGPART + "size %d %d" % (L, h))

    L2 = int(L * 1.15)
    g = Grid(L2, h); g.hw = L2
    end = L2 * 0.8
    limb(g, [([(-1, cy), (end * 0.5, cy + 0.3), (end, cy)], T * 0.42, T * 0.2)])
    g.fill(rect(end * 0.45, cy - T / 2, end * 0.45, cy + T / 2), 'k')
    grit(g, L2)
    hairs(g, 0, end, cy, T * 0.4, T * 0.2, hl * 1.15, max(1.2, T * 0.32), len(name) + 3)
    bone(g, [(end - 1, cy), (L2 - 3, cy - 0.5), (L2 - 1, cy + T * 0.6)], max(1.0, T * 0.28), 0.4)   # the claw, hooked
    g.write(name + "-leg-base", LEGPART + "size %d %d" % (L2, h))

    j = int(T * 1.6) + 2
    g = Grid(j, j); g.hw = j
    chitin(g, circ(j / 2, j / 2, T * 0.8), rim=1)
    g.write(name + "-joint", LEGPART + "size %d %d" % (j, j))

def base(name, size, r):
    g = Grid(size, size); chitin(g, circ(size / 2, size / 2, r), rim=3); grit(g, size + 7); g.mirror()
    g.write(name, SWARMPART + "size %d %d" % (size, size))

# ======================= the ground stages =======================
#   leg strips: the thigh is legLength * 2 px (half the leg at 4 px per world unit), see Factions.swarmSpider
unit("mite", 48, 1); leg("mite", 28, 3); base("mite-base", 34, 11)
unit("spitter", 72, 2); leg("spitter", 40, 4); base("spitter-base", 52, 17)
unit("brood", 96, 3); leg("brood", 56, 5); base("brood-base", 68, 23)
unit("behemoth", 128, 4); leg("behemoth", 76, 6); base("behemoth-base", 90, 30)

# ======================= Wasp (48): the flying stage - a hornet of the same chitin, bone and light =======================
w, h = scaled(48 / 96, 96, 96)
g = Grid(w, h)
g.fill(poly([(4, 22), (38, 30), (36, 42), (2, 36)]), 'W'); g.fill(poly([(8, 50), (38, 44), (34, 58), (14, 66)]), 'W')   # wings
for pts in ([(8, 26), (38, 32)], [(6, 32), (36, 38)], [(12, 54), (36, 48)]): g.fill(stroke(pts, 0.6, 0.6), 'X')
body = poly([(34, 46), (42, 40), (C, 40), (C, 88), (42, 84), (36, 70)])
chitin(g, body, rim=3)
for y in (54, 64, 74): g.fill(both(body, ring(C, y - 40, 40, 41.4)), 'C'); g.fill(both(body, ring(C, y - 40, 40.3, 41)), 'A')   # glowing bands
g.fill(rect(C - 1.2, 86, C - 0.01, 95), 'N'); g.fill(rect(C - 0.8, 86, C - 0.01, 92), 'P')                 # stinger
chitin(g, poly([(36, 24), (42, 20), (C, 20), (C, 46), (38, 44)]), rim=2)                                  # thorax
chitin(g, poly([(38, 12), (43, 8), (C, 8), (C, 22), (40, 22)]), rim=2)                                    # head
bone(g, [(41, 12), (34, 6), (36, 0), (42, 0)], 2.6, 0.5)                                                   # mandibles
g.fill(poly([(C - 5, 33), (C, 27), (C + 0.01, 39), (C - 3, 36)]), 'A'); g.fill(poly([(C - 2.5, 33), (C, 30), (C + 0.01, 36)]), 'B')   # core
for (x, y) in ((43, 14), (41, 17)): g.fill(rect(x - 1, y - 1, x + 1, y + 1), 'k'); g.fill(rect(x, y, x, y), 'A')
g.fill(poly([(C - 6, 82), (C, 77), (C + 0.01, 80), (C - 6, 85)]), 'T'); g.fill(poly([(C - 6, 84), (C, 79), (C + 0.01, 80), (C - 6, 85)]), 'U')
unscaled()
grit(g, 48)
g.mirror()
g.write("wasp", SWARM + "color W 1d2618\ncolor X 3d7d1c\nsize 48 48\nref flare horizon")

# ======================= Scrabba (200): the last stage, a scorpion - giant pincers, a tail over the back =======================
w, h = scaled(200 / 96, 96, 96)
g = Grid(w, h)
creature(g, 4, scythes=False)
# pincers: a heavy arm, a palm and two scythe fingers reaching forward
bone(g, [(30, 34), (18, 26), (14, 16)], 4.0, 3.0)
chitin(g, ellipse(12, 12, 8, 7), rim=2)
bone(g, [(8, 10), (2, 2), (8, -4)], 2.8, 0.5)
bone(g, [(16, 8), (20, 0), (16, -4)], 2.4, 0.5)
# the tail: segments up the spine from the rear, the stinger glowing over the core
for (cy, r) in ((92, 5.2), (84, 5.2), (76, 5), (69, 4.6)):
    chitin(g, circ(C, cy, r), rim=2)
    g.fill(both(circ(C, cy, r), rect(C - 1.2, cy - r, C - 0.01, cy + r)), 'C')
g.fill(circ(C, 62, 4), 'k'); g.fill(circ(C, 62, 3.2), 'A'); g.fill(circ(C - 1, 61, 1.2), 'B')
bone(g, [(C - 0.5, 59), (C - 2, 55), (C - 0.2, 51)], 1.6, 0.3)
unscaled()
grit(g, 200)
g.mirror()
g.write("scrabba", SWARM + "size 200 200\non scrabba-base\nref reign toxopid")
leg("scrabba", 104, 8); base("scrabba-base", 140, 44)
