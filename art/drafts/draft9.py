"""xhigh units: Annihilator, Tempest, Abyss, Behemoth, Maelstrom - boss-scale, v3 style."""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from unitlib import *

TANK = UNIT + GLOW + "outline 3 2d2f39\n"
AIR = UNIT + GLOW + "outline 3 565666\n"
BOAT = UNIT + GLOW + "outline 3 2d2f39\ncolor C 3f7fa0\ncolor E 8fd8ff\n"
ACID = "color A 9fb83a\ncolor B d6ec6a\n"
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

def treads(name, size, x0, w, y0, h):
    g = Grid(size, size)
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            g.g[y][x] = 'K' if (y - y0) % 6 == 5 else ('D' if x in (x0, x0 + w - 1) else ('L' if (y - y0) % 6 == 0 else 'M'))
    g.mirror()
    g.write(name, COLORS + "size %d %d\noutline 0" % (size, size))

# ======================= Annihilator (128): siege tank, caustic mortar, AP sponsons =======================
treads("annihilator-treads", 128, 10, 18, 10, 108)
g = Grid(128, 128)
g.panel(poly([(8, 18), (16, 8), (32, 8), (32, 26), (8, 26)]))                          # front fenders
g.panel(poly([(8, 104), (32, 104), (32, 120), (16, 120), (8, 112)]))                    # rear fenders
g.panel(poly([(28, 30), (40, 14), (64, 12), (64, 122), (34, 122), (28, 114)]))           # hull
g.panel(poly([(28, 40), (40, 38), (40, 98), (28, 98)]))                                  # flank armour
for cy in (52, 72):                                                                      # caustic ammo tanks
    g.fill(rect(30, cy - 7, 38, cy + 7), 'K'); g.fill(rect(31, cy - 6, 37, cy + 6), 'A'); g.fill(rect(31, cy - 6, 33, cy + 6), 'B')
g.fill(poly([(29, 88), (39, 84), (39, 90), (29, 94)]), 'T'); g.fill(poly([(29, 92), (39, 88), (39, 90), (29, 94)]), 'U')
g.panel(poly([(40, 16), (64, 14), (64, 34), (40, 38)]))                                  # glacis
g.fill(poly([(44, 23), (58, 21), (58, 25), (44, 27)]), 'K'); g.fill(poly([(46, 24), (58, 22), (58, 24), (46, 26)]), 'O')
g.panel(circ(64, 58, 25))                                                                # mortar turret ring
g.fill(ring(64, 58, 20, 22), 'K')
g.panel(circ(24, 88, 13))                                                                # AP gun sponsons (x = +/-10, y = -6)
g.fill(circ(24, 88, 7), 'K')
g.panel(rect(46, 92, 64, 118))                                                           # engine deck
for y in range(95, 116, 4): g.fill(rect(48, y, 63, y), 'K')
g.panel(rect(40, 116, 50, 125))
g.fill(rect(42, 122, 48, 125), 'O')
g.mirror()
g.write("annihilator", TANK + ACID + """
size 128 128
on annihilator-treads
weapon annihilator-mortar 0 0
weapon annihilator-gun 10 -6
weapon annihilator-gun -10 -6 flip
ref scepter conquer
""")

g = Grid(72, 72)                                                                         # caustic mortar turret
g.panel(poly([(8, 34), (16, 22), (28, 18), (36, 18), (36, 62), (20, 62), (8, 54)]))          # wide angular housing
g.panel(poly([(8, 34), (14, 28), (18, 40), (12, 50)]), rim=2)                                # cheek armour
for cy in (36, 48):                                                                          # acid feed canisters
    g.fill(rect(20, cy - 4, 27, cy + 4), 'K'); g.fill(rect(21, cy - 3, 26, cy + 3), 'A'); g.fill(rect(21, cy - 3, 22, cy + 3), 'B')
g.panel(rect(29, 8, 36, 30), rim=2)                                                          # long thick barrel
for y in (14, 20, 26): g.fill(rect(29, y, 36, y), 'K')                                       # ribs
g.panel(rect(25, 2, 36, 9), rim=2)                                                           # big muzzle brake
g.fill(rect(31, 2, 36, 5), 'K'); g.fill(rect(25, 5, 28, 6), 'K')
g.mirror()
g.write("annihilator-mortar", COLORS + ACID + "size 72 72\noutline 3 2d2f39")

g = Grid(40, 40)                                                                         # AP gun
g.panel(circ(20, 24, 10))
g.panel(rect(18, 3, 19, 20), rim=1)
g.fill(rect(18, 3, 19, 4), 'K')
g.mirror()
g.write("annihilator-gun", COLORS + "size 40 40\noutline 3 2d2f39")

# ======================= Tempest (128): flying-wing bomber with lit bomb bays =======================
g = Grid(128, 128)
wing = poly([(4, 76), (44, 36), (64, 26), (64, 112), (52, 118), (40, 104), (28, 112), (16, 98), (6, 96)])
g.panel(wing)
g.fill(poly([(14, 80), (44, 52), (46, 58), (18, 86)]), 'T'); g.fill(poly([(16, 84), (45, 56), (46, 58), (18, 86)]), 'U')
g.panel(poly([(4, 76), (44, 36), (48, 40), (8, 80)]), rim=2)                             # armoured leading edge
g.panel(poly([(4, 76), (2, 64), (10, 70), (10, 92)]), rim=2)                              # wingtip fins
for x0 in (18, 36):                                                                       # raised engine nacelles
    g.panel(poly([(x0, 64 - (x0 - 18) // 2), (x0 + 10, 58 - (x0 - 18) // 2), (x0 + 10, 100), (x0, 100)]), rim=3)
for (cx, cy) in ((23, 98), (41, 102)):                                                   # trailing-edge engines
    g.fill(rect(cx - 4, cy - 2, cx + 4, cy + 6), 'K'); g.fill(rect(cx - 3, cy + 2, cx + 3, cy + 5), 'O')
g.panel(poly([(48, 30), (56, 14), (64, 8), (64, 116), (50, 112)]))                       # central body
g.fill(poly([(56, 18), (64, 13), (64, 30), (56, 28)]), 'K'); g.fill(poly([(57, 19), (64, 15), (64, 28), (57, 27)]), 'C')
g.fill(poly([(58, 20), (64, 17), (64, 21), (58, 23)]), 'E')
for y0 in (48, 66, 84):                                                                  # bomb bay doors
    g.fill(rect(53, y0, 64, y0 + 14), 'K'); g.fill(rect(55, y0 + 2, 63, y0 + 12), 'D')
    g.fill(rect(55, y0 + 6, 63, y0 + 7), 'O')
g.mirror()
g.write("tempest", AIR + """
color C 3f7fa0
color E 8fd8ff
size 128 128
ref antumbra quell
""")

# ======================= Abyss (120x160): nuclear submarine =======================
g = Grid(*scaled(1.6, 120, 160))
g.panel(poly([(60, 2), (40, 22), (24, 56), (20, 100), (28, 136), (44, 152), (60, 156)]))  # hull
for y in range(26, 150, 9): g.fill(both(poly([(60, 2), (40, 22), (24, 56), (20, 100), (28, 136), (44, 152), (60, 156)]), rect(0, y, 59, y)), 'K')
g.panel(poly([(24, 104), (34, 100), (34, 132), (28, 136)]), rim=3)                        # ballast tanks
g.panel(poly([(28, 30), (40, 22), (40, 52), (26, 56)]), rim=3)
g.panel(poly([(14, 70), (24, 62), (24, 92), (14, 98)]))                                  # torpedo bulges (x = +/-10)
g.fill(rect(18, 72, 22, 82), 'K'); g.fill(rect(19, 74, 21, 77), 'O')
g.fill(rect(26, 100, 31, 128), 'T'); g.fill(rect(26, 118, 31, 128), 'U')
g.panel(rect(40, 36, 60, 68))                                                            # missile hatch block (x = 0, y = 6)
for cy in (39, 48, 57):
    for cx in (43, 51):
        g.fill(rect(cx, cy, cx + 6, cy + 6), 'K'); g.fill(rect(cx + 1, cy + 1, cx + 5, cy + 5), 'D'); g.fill(rect(cx + 2, cy + 2, cx + 4, cy + 4), 'O')
g.panel(poly([(48, 70), (60, 62), (60, 104), (48, 102)]))                                # sail
g.fill(rect(52, 74, 60, 76), 'K'); g.fill(rect(53, 75, 60, 75), 'E')
g.panel(poly([(16, 118), (30, 110), (30, 132), (14, 136)]))                              # dive planes
g.fill(circ(60, 124, 9), 'K'); g.fill(circ(60, 124, 7), 'N'); g.fill(circ(60, 124, 3.5), 'R')   # reactor
g.panel(rect(52, 140, 60, 156))
g.mirror()
g.fill(rect(57, 2, 62, 8), 'K', full=True)
g.write("abyss", BOAT + """
color N 2f7a5a
color R 8ff0b0
size 120 160
ref sei omura
""")
unscaled()

# ======================= Behemoth (120): Swarm boss beetle =======================
g = Grid(120, 120)
shell = poly([(12, 40), (24, 22), (40, 14), (60, 12), (60, 110), (44, 114), (26, 104), (14, 88), (8, 64)])
bug(g, shell)                                                                                # carapace
for y in range(30, 108, 12): g.fill(both(shell, rect(0, y, 59, y)), 'k')                     # segment seams
for pts in ([(6, 40), (14, 26), (18, 42)], [(2, 58), (10, 48), (12, 64)], [(2, 78), (10, 70), (12, 84)], [(8, 96), (16, 88), (20, 100)], [(22, 112), (24, 102), (32, 110)]):
    bug(g, poly(pts))                                                                         # rim spikes
bug(g, poly([(22, 62), (40, 52), (60, 52), (60, 104), (42, 106), (26, 96)]))                  # rear shell
g.fill(circ(60, 80, 20), 'k'); g.fill(circ(60, 80, 18), 'A'); g.fill(circ(55, 74, 7), 'B')   # acid gland
for (cx, cy) in ((32, 66), (28, 84), (40, 98)):
    g.fill(circ(cx, cy, 6), 'k'); g.fill(circ(cx, cy, 5), 'E'); g.fill(circ(cx - 1.5, cy - 1.5, 2), 'F')   # egg sacs
bug(g, poly([(26, 18), (32, 4), (42, 2), (48, 10), (44, 20), (38, 12), (34, 24)]))           # pincers
bug(g, poly([(42, 18), (52, 12), (60, 14), (60, 36), (46, 36)]))                              # head
g.fill(rect(50, 22, 54, 25), 'A'); g.fill(rect(46, 28, 49, 30), 'A')
g.fill(poly([(28, 36), (46, 32), (46, 40), (28, 44)]), 'T'); g.fill(poly([(28, 42), (46, 38), (46, 40), (28, 44)]), 'U')
g.mirror()
g.write("behemoth", SWARM + """
color E d8c48a
color F fff2c4
size 120 120
on behemoth-base
ref reign scepter
""")
g = Grid(120, 120); g.hw = 120
bug(g, poly([(70, 44), (98, 30), (110, 48), (100, 54), (74, 60)]))
bug(g, poly([(100, 54), (112, 48), (116, 62), (106, 64)]))
g.write("behemoth-leg", SWARMPART + "size 120 120")
g = Grid(84, 84); bug(g, circ(42, 42, 28)); g.mirror(); g.write("behemoth-base", SWARMPART + "size 84 84")

# ======================= Maelstrom (120x152): Tide flagship =======================
g = Grid(*scaled(1.55, 120, 152))
g.panel(poly([(60, 2), (40, 20), (24, 50), (20, 110), (28, 138), (60, 148)]))            # hull
g.panel(poly([(60, 2), (46, 18), (54, 22), (60, 14)]))                                    # armoured ram
g.panel(poly([(36, 12), (28, 20), (36, 26)]))                                             # bow spike
g.panel(poly([(30, 34), (42, 22), (46, 26), (34, 40)]), rim=2)                              # bow armour plates
g.panel(poly([(24, 50), (34, 40), (38, 44), (28, 56)]), rim=2)
deck = poly([(60, 22), (46, 34), (32, 58), (30, 108), (36, 128), (60, 134)])
g.fill(deck, 'D')
for y in range(36, 130, 7): g.fill(both(deck, rect(0, y, 59, y)), 'K')
g.panel(poly([(20, 66), (30, 62), (30, 104), (20, 108)]))                                 # armour belt
g.fill(rect(22, 72, 27, 98), 'T'); g.fill(rect(22, 88, 27, 98), 'U')
g.panel(poly([(42, 40), (60, 30), (60, 110), (42, 110)]))                                 # superstructure
g.panel(poly([(48, 46), (60, 40), (60, 100), (48, 100)]))
g.fill(poly([(48, 52), (60, 47), (60, 51), (48, 56)]), 'K'); g.fill(poly([(50, 53), (60, 49), (60, 50), (50, 54)]), 'O')
g.fill(rect(40, 112, 60, 126), 'K')
for cx in (45, 53): g.fill(circ(cx, 119, 3), 'O')                                         # mine racks (y = -6)
g.panel(rect(46, 128, 56, 142)); g.fill(rect(48, 132, 54, 136), 'O')                     # funnel
g.mirror()
# tesla tower at the centre (shock)
g.fill(circ(60, 76, 13), 'D', full=True); g.fill(ring(60, 76, 8, 11), 'K', full=True); g.fill(circ(60, 76, 8), 'C', full=True); g.fill(circ(61, 74, 3.5), 'E', full=True)
# sprayer turrets: water on the left (x = -8), cryo on the right (x = +8)
for (cx, wet, hi) in ((28, 'W', 'X'), (92, 'F', 'H')):
    g.fill(circ(cx, 76, 11), 'K', full=True); g.fill(circ(cx, 76, 10), 'D', full=True); g.fill(circ(cx - 1, 75, 7), 'M', full=True)
    g.fill(circ(cx, 76, 4), wet, full=True)
    g.fill(rect(cx - 3, 52, cx + 3, 70), 'K', full=True); g.fill(rect(cx - 2, 52, cx + 2, 70), 'D', full=True); g.fill(rect(cx - 2, 52, cx - 1, 70), 'M', full=True)
    g.fill(rect(cx - 3, 50, cx + 3, 55), wet, full=True); g.fill(rect(cx - 1, 50, cx + 1, 52), hi, full=True)
g.write("maelstrom", BOAT + """
color W 3f6fa0
color X 8fbfff
color F 4fa7c7
color H a6f0ff
size 120 152
ref sei omura
""")
unscaled()
