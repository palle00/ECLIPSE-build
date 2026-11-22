"""Scrabba (Swarm boss scorpion: body, -leg, -base), the Tide's Lamprey, and the status icons for Exoskeleton,
Incubating and Latched. Run: python -I art/drafts/draft34.py <out dir>"""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from unitlib import *

SWARM = """
color g a6b464
color h 78873f
color i 4f5c2d
color k 2b3420
color A c9d36a
color B f0f79c
color T ffa664
color U d06b53
mask cell T:ffffff U:dcc6c6
team e05438
outline 3 2b3420
"""
SWARMPART = chr(10).join(l for l in SWARM.split(chr(10)) if not l.startswith(("mask", "team")))
def bug(g, test, **kw): g.panel(test, light='g', mid='h', dark='i', seamch='k', **kw)

# ======================= Scrabba (136): Swarm boss scorpion =======================
# left half drawn (x < 68), mirrored; the front is the top
g = Grid(136, 136)
C = 68
bug(g, poly([(44, 52), (50, 40), (60, 34), (C, 33), (C, 112), (58, 114), (48, 106), (42, 90), (40, 70)]))   # carapace
for y in range(62, 110, 10): g.fill(both(poly([(42, 52), (C, 52), (C, 112), (42, 112)]), rect(0, y, C - 1, y)), 'k')  # abdomen seams
bug(g, poly([(50, 40), (60, 33), (C, 32), (C, 50), (52, 50)]))                                                # head plate
g.fill(circ(58, 40, 2.6), 'A'); g.fill(circ(63, 45, 1.8), 'A'); g.fill(circ(57.5, 39.5, 1), 'B')            # eyes
# pincers: arm forward-outward from the head, a heavy palm, two fingers
bug(g, poly([(48, 44), (40, 34), (32, 30), (28, 34), (36, 40), (44, 50)]))                                   # forearm
bug(g, circ(26, 24, 12))                                                                                       # palm
bug(g, poly([(16, 18), (12, 4), (16, 0), (22, 8), (24, 16)]))                                                  # outer finger
bug(g, poly([(28, 14), (30, 3), (35, 2), (37, 10), (34, 18)]))                                                 # inner finger
g.fill(poly([(14, 4), (16, 1), (18, 4)]), 'A'); g.fill(poly([(31, 4), (34, 2.5), (35, 6)]), 'A')              # claw tips
# team stripe across the carapace
g.fill(poly([(44, 58), (C, 56), (C, 61), (44, 63)]), 'T'); g.fill(poly([(44, 61.5), (C, 59.5), (C, 61), (44, 63)]), 'U')
# tail: segments up the spine from the rear, arched over the back, the stinger pointing forward
for (cy, r) in ((124, 9), (112, 9), (100, 8.5), (89, 8), (79, 7.5)):
    bug(g, circ(C, cy, r))
g.fill(circ(C, 69, 7), 'k'); g.fill(circ(C, 69, 6), 'A'); g.fill(circ(C - 2, 67, 2.5), 'B')                  # venom bulb
g.fill(poly([(C - 2.5, 63), (C, 54), (C + 0.01, 63)]), 'i'); g.fill(poly([(C - 1.5, 62), (C, 56), (C + 0.01, 62)]), 'B')  # stinger
g.mirror()
g.write("scrabba", SWARM + """
size 136 136
on scrabba-base
ref reign toxopid
""")
# legs (the mech leg sprite: drawn on the right, the game mirrors it for the left step)
g = Grid(136, 136); g.hw = 136
for (y, dy) in ((54, -10), (66, -4), (78, 4), (90, 10)):
    bug(g, poly([(88, y), (110, y + dy), (114, y + dy + 4), (92, y + 6)]))
    bug(g, poly([(110, y + dy), (124, y + dy + 8), (122, y + dy + 12), (112, y + dy + 6)]))
g.write("scrabba-leg", SWARMPART + "size 136 136")
g = Grid(96, 96); bug(g, circ(48, 48, 30)); g.mirror(); g.write("scrabba-base", SWARMPART + "size 96 96")

# ======================= Lamprey (40x88): the Tide's robotic eel =======================
BOAT = UNIT + GLOW + "outline 3 2d2f39\ncolor C 3f7fa0\ncolor E 8fd8ff\ncolor W 3f6fa0\n"
g = Grid(40, 88)
H = 20
g.panel(poly([(H, 2), (12, 4), (8, 10), (8, 18), (11, 26), (11, 44), (12, 60), (14, 74), (17, 84), (H, 87)]), rim=2)   # body
for y in range(30, 84, 8): g.fill(both(poly([(6, 26), (H, 26), (H, 86), (6, 86)]), rect(0, y, H - 1, y)), 'K')     # segment rings
g.panel(poly([(10, 34), (4, 40), (5, 46), (11, 44)]), rim=1)                                                       # side fins
g.panel(poly([(12, 60), (7, 66), (8, 70), (13, 70)]), rim=1)
g.fill(circ(H, 10, 8), 'K'); g.fill(circ(H, 10, 6.5), 'W'); g.fill(circ(H, 10, 3.5), 'K')                        # sucker mouth
for a in range(0, 180, 30):                                                                                        # teeth ring
    x, y = H - 5 * math.sin(math.radians(a)), 10 - 5 * math.cos(math.radians(a))
    g.fill(circ(x, y, 1), 'L')
g.fill(rect(H - 1, 22, H - 1, 80), 'E'); g.fill(rect(H - 2, 24, H - 2, 78), 'C')                                   # electric spine
for y in (36, 52, 68): g.fill(circ(13.5, y, 1.4), 'E')                                                             # side lights
g.fill(poly([(12, 20), (H, 19), (H, 24), (12, 25)]), 'T'); g.fill(poly([(12, 23.5), (H, 22.5), (H, 24), (12, 25)]), 'U')  # team band
g.mirror()
g.write("lamprey", BOAT + "size 40 88\nref risso retusa")

# ======================= status icons (32px white silhouettes, tinted by the game) =======================
def sgrid(): return Grid(32, 32)
def full(g, test, ch='W'):
    for y in range(32):
        for x in range(32):
            if test(x + .5, y + .5): g.g[y][x] = ch
def raw(test): return test
def writeicon(g, name):
    with open(os.path.join(OUT, name + ".txt"), "w", newline="\n") as f:
        f.write("size 32 32\noutline 0\ncolor W ffffff\ngrid\n" + "\n".join("".join(r) for r in g.g) + "\n")
    print("drafted", name)
cc = lambda cx, cy, r: (lambda px, py: (px - cx) ** 2 + (py - cy) ** 2 <= r * r)

# exoskeleton: overlapping shell plates
g = sgrid()
for (cy, r) in ((9, 10), (17, 11), (25, 9)):
    full(g, lambda px, py, cy=cy, r=r: cc(16, cy, r)(px, py) and py >= cy - r * 0.55)
for y in (12, 20): full(g, lambda px, py, y=y: abs(py - y) < 0.8 and 5 < px < 27, '.')
writeicon(g, "exoskeleton")

# incubating: an egg with a crack
g = sgrid()
full(g, lambda px, py: ((px - 16) / 9.5) ** 2 + ((py - 17) / 13) ** 2 <= 1)
for (x0, y0, x1, y1) in ((11, 13, 15, 17), (15, 17, 18, 13), (18, 13, 21, 17)):
    L = math.hypot(x1 - x0, y1 - y0)
    full(g, lambda px, py, x0=x0, y0=y0, x1=x1, y1=y1, L=L: abs((px - x0) * (y1 - y0) - (py - y0) * (x1 - x0)) / L <= 0.8
         and -0.5 <= ((px - x0) * (x1 - x0) + (py - y0) * (y1 - y0)) / L <= L + 0.5, '.')
writeicon(g, "incubating")

# latched: a sucker mouth ring with teeth, biting a block edge
g = sgrid()
full(g, lambda px, py: 22 <= py <= 30 and 3 <= px <= 29)                                          # the block
full(g, lambda px, py: 6 ** 2 <= (px - 16) ** 2 + (py - 12) ** 2 <= 10 ** 2)                       # mouth ring
for a in range(0, 360, 45):
    full(g, cc(16 + 4 * math.cos(math.radians(a)), 12 + 4 * math.sin(math.radians(a)), 1.3))       # teeth
writeicon(g, "latched")
