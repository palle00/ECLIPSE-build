"""Unit drafts v3: wide light/dark facets (rim 4-5), aggressive silhouettes, heavier weapons, glowing slits."""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from unitlib import *

# ======================= Rampart (96): heavy shield tank =======================
g = Grid(96, 96)
# tread fenders, angled
g.panel(poly([(5, 16), (12, 6), (26, 6), (26, 20), (5, 22)]), rim=3)
g.panel(poly([(5, 76), (26, 76), (26, 91), (12, 91), (5, 84)]), rim=3)
# hull: a heavy brick
g.panel(poly([(22, 20), (30, 12), (48, 12), (48, 90), (26, 90), (22, 86)]), rim=5)
# side armour skirts, team chevrons
g.panel(poly([(21, 32), (32, 30), (32, 78), (21, 78)]), rim=4)
for y0 in (38, 50):
    g.fill(poly([(22, y0), (31, y0 - 3), (31, y0 + 2), (22, y0 + 5)]), 'T'); g.fill(poly([(22, y0 + 3), (31, y0), (31, y0 + 2), (22, y0 + 5)]), 'U')
# glacis with glowing vision slits (the eyes)
g.panel(poly([(32, 18), (48, 16), (48, 36), (32, 38)]), rim=4)
g.fill(poly([(35, 25), (44, 24), (44, 27), (35, 29)]), 'K'); g.fill(poly([(36, 26), (43, 25), (43, 26), (36, 28)]), 'O')
# engine deck: grille, twin exhaust stacks glowing
g.panel(rect(34, 64, 48, 86), rim=3)
for y in range(67, 84, 3): g.fill(rect(36, y, 47, y), 'K')
g.panel(rect(30, 84, 38, 93), rim=2)
g.fill(rect(32, 90, 36, 93), 'O')
# sponson housings for the twin cannons
g.panel(circ(22, 60, 12), rim=4)
g.fill(circ(22, 60, 6), 'K')
# the shield plough: a thick V of armour across the front with a cyan emitter edge and spikes
plough = poly([(2, 24), (10, 10), (48, 0), (48, 10), (14, 18), (8, 28)])
g.panel(plough, rim=3)
g.fill(poly([(10, 10), (48, 0), (48, 2), (11, 12)]), 'S')
g.panel(poly([(2, 24), (0, 12), (6, 14), (8, 28)]), rim=2)
g.panel(poly([(22, 6), (24, 0), (28, 4), (28, 8)]), rim=1)
g.mirror()
g.write("rampart", UNIT + """
color S 7fd6ff
""" + GLOW + """
size 96 96
outline 3 2d2f39
on rampart-treads
weapon rampart-cannon 6.5 -3
weapon rampart-cannon -6.5 -3 flip
ref fortress precept
""")

g = Grid(96, 96)
for y in range(8, 88):
    for x in range(10, 23):
        g.g[y][x] = 'K' if (y - 8) % 5 == 4 else ('D' if x in (10, 22) else ('L' if (y - 8) % 5 == 0 else 'M'))
g.mirror()
g.write("rampart-treads", COLORS + "size 96 96\noutline 0")

# twin heavy cannon on an armoured housing
g = Grid(56, 56)
g.panel(poly([(14, 26), (20, 18), (28, 18), (28, 46), (18, 46), (14, 40)]), rim=4)
g.panel(rect(19, 2, 24, 22), rim=2)
g.fill(rect(19, 2, 24, 5), 'K'); g.fill(rect(20, 3, 23, 4), 'D')
g.fill(rect(19, 12, 24, 12), 'K')
g.panel(circ(28, 34, 6), rim=2)
g.mirror()
g.write("rampart-cannon", COLORS + "size 56 56\noutline 3 2d2f39")

# ======================= Valkyrie (96): heavy gunship with forward claws =======================
g = Grid(96, 96)
# tail fins
g.panel(poly([(14, 76), (28, 64), (30, 66), (28, 92), (16, 90)]), rim=3)
# swept wings with a jagged trailing edge
g.panel(poly([(4, 46), (34, 26), (36, 58), (24, 56), (16, 62), (6, 60)]), rim=5)
g.fill(poly([(10, 52), (32, 40), (32, 45), (12, 56)]), 'T'); g.fill(poly([(11, 55), (32, 43), (32, 45), (12, 56)]), 'U')
# gun nacelles at the wing roots (guns at x = +/-8)
g.panel(poly([(10, 26), (16, 18), (24, 18), (24, 60), (12, 60), (10, 54)]), rim=4)
g.fill(rect(13, 56, 21, 60), 'K'); g.fill(rect(14, 58, 20, 59), 'O')
# forward claws
g.panel(poly([(26, 30), (28, 6), (32, 2), (34, 30)]), rim=3)
# armoured fuselage, sharp nose, canopy
g.panel(poly([(36, 22), (44, 6), (48, 6), (48, 92), (38, 92), (34, 82), (34, 30)]), rim=4)
g.fill(poly([(41, 14), (48, 10), (48, 26), (41, 24)]), 'K'); g.fill(poly([(42, 15), (48, 12), (48, 24), (42, 22)]), 'C'); g.fill(poly([(44, 15), (48, 13), (48, 17), (44, 18)]), 'E')
g.fill(rect(38, 86, 47, 93), 'K'); g.fill(rect(40, 88, 47, 92), 'O')
g.mirror()
g.write("valkyrie", UNIT + """
color C 3f7fa0
color E 8fd8ff
""" + GLOW + """
size 96 96
outline 3 565666
weapon valkyrie-gun 8 1
weapon valkyrie-gun -8 1 flip
ref zenith horizon
""")

g = Grid(40, 40)
g.panel(poly([(12, 18), (16, 14), (20, 14), (20, 34), (12, 34)]), rim=3)
g.panel(rect(14, 2, 17, 16), rim=1)
g.fill(rect(14, 2, 17, 4), 'K')
g.mirror()
g.write("valkyrie-gun", COLORS + "size 40 40\noutline 3 565666")

# ======================= Cinderwake (88x112): armoured slag warship =======================
g = Grid(*scaled(1.4, 88, 112))
hull = poly([(44, 0), (30, 12), (17, 36), (15, 86), (22, 104), (44, 110)])
g.panel(hull, rim=5)
deck = poly([(44, 16), (32, 28), (25, 46), (24, 84), (29, 98), (44, 102)])
g.fill(deck, 'D')
for y in range(30, 98, 6): g.fill(both(deck, rect(0, y, 43, y)), 'K')                      # deck plating
# ram bow: spiked armour wedge
g.panel(poly([(44, 0), (32, 14), (38, 20), (44, 14)]), rim=2)
g.panel(poly([(30, 12), (24, 10), (28, 20)]), rim=1)
# side armour belt with team colour
g.panel(poly([(16, 48), (24, 46), (24, 78), (16, 80)]), rim=3)
g.fill(rect(18, 54, 22, 72), 'T'); g.fill(rect(18, 66, 22, 72), 'U')
# stepped superstructure (pyramid), glowing bridge slit
g.panel(poly([(30, 36), (44, 28), (44, 72), (30, 72)]), rim=4)
g.panel(poly([(35, 40), (44, 34), (44, 66), (35, 66)]), rim=3)
g.fill(poly([(36, 44), (44, 40), (44, 43), (36, 47)]), 'K'); g.fill(poly([(37, 45), (44, 41), (44, 42), (37, 46)]), 'O')
# twin smokestacks aft, glowing
g.panel(rect(31, 88, 38, 100), rim=2); g.fill(rect(33, 91, 36, 95), 'O')
# mine rack at the stern
g.fill(rect(28, 75, 44, 85), 'K')
for cx in (32, 38): g.fill(circ(cx, 80, 2.4), 'O')
g.mirror()
# tesla coil on top of the superstructure (shock weapon at the centre)
g.fill(circ(44, 56, 10), 'D', full=True); g.fill(ring(44, 56, 6, 8), 'K', full=True); g.fill(circ(44, 56, 6), 'C', full=True); g.fill(circ(45, 55, 2.5), 'E', full=True)
# slag cannon turret on the right gunwale (spray at x = +6 -> 24 px right): housing, barrel, glowing muzzle
g.fill(circ(68, 58, 10), 'K', full=True); g.fill(circ(68, 58, 9), 'D', full=True); g.fill(circ(67, 57, 6), 'M', full=True); g.fill(circ(66, 56, 3), 'L', full=True)
g.fill(rect(65, 34, 71, 52), 'K', full=True); g.fill(rect(66, 36, 70, 52), 'D', full=True); g.fill(rect(66, 36, 67, 52), 'M', full=True)
g.fill(rect(65, 32, 71, 37), 'O', full=True); g.fill(rect(67, 32, 69, 35), 'P', full=True)
g.write("cinderwake", UNIT + """
color C 3f7fa0
color E 8fd8ff
""" + GLOW + """
size 88 112
outline 3 2d2f39
ref bryde minke
""")
unscaled()

# ======================= Delta (72): arrowhead builder =======================
g = Grid(72, 72)
g.panel(poly([(36, 2), (36, 60), (26, 56), (16, 66), (2, 64), (8, 50)]), rim=4)               # the delta wing, notched
g.fill(poly([(12, 54), (30, 40), (30, 45), (14, 58)]), 'T'); g.fill(poly([(13, 57), (30, 43), (30, 45), (14, 58)]), 'U')
g.panel(poly([(20, 24), (26, 14), (29, 16), (27, 32)]), rim=2)                                # canards
g.panel(poly([(30, 10), (36, 2), (36, 66), (30, 62)]), rim=3)                                 # spine
g.panel(poly([(22, 50), (28, 42), (30, 44), (30, 66), (24, 68)]), rim=2)                      # tail fins
g.fill(poly([(32, 14), (36, 11), (36, 24), (32, 22)]), 'K'); g.fill(poly([(33, 15), (36, 13), (36, 22), (33, 21)]), 'C')
g.fill(rect(32, 62, 36, 69), 'K'); g.fill(rect(33, 65, 36, 67), 'O')
g.mirror()
g.write("delta", UNIT + GLOW + """
color C ff9a3c
size 72 72
outline 3 565666
weapon v:small-mount-weapon 1 2 under
weapon v:small-mount-weapon -1 2 flip under
weapon v:repair-beam-weapon-center 0 -2
ref gamma zenith
""")

# ======================= Epsilon (80): trident builder =======================
g = Grid(80, 80)
crescent = both(lambda px, py: (px - 40) ** 2 / 37 ** 2 + (py - 38) ** 2 / 32 ** 2 <= 1,
                lambda px, py: (px - 40) ** 2 / 27 ** 2 + (py - 26) ** 2 / 27 ** 2 > 1 and py > 26)
g.panel(crescent, rim=4)
g.panel(poly([(4, 24), (10, 6), (18, 12), (21, 54), (8, 52)]), rim=4)                          # side prongs
g.fill(poly([(9, 18), (14, 12), (18, 16), (18, 24), (9, 26)]), 'T'); g.fill(poly([(9, 23), (18, 21), (18, 24), (9, 26)]), 'U')
g.fill(rect(9, 8, 13, 11), 'V')
g.panel(poly([(29, 14), (40, 0), (40, 70), (29, 64)]), rim=4)                                  # centre prong / body
g.fill(rect(36, 2, 40, 6), 'V')
g.fill(poly([(32, 18), (40, 11), (40, 34), (32, 32)]), 'K'); g.fill(poly([(33, 19), (40, 13), (40, 32), (33, 30)]), 'W')
g.fill(poly([(12, 56), (28, 62), (28, 66), (12, 60)]), 'V')                                      # purple flow lines
g.fill(rect(34, 68, 40, 74), 'K'); g.fill(rect(36, 70, 40, 72), 'V')
g.mirror()
g.write("epsilon", UNIT + """
color V b37aff
color W 7d4fc4
size 80 80
outline 3 565666
weapon v:small-mount-weapon 1 2 under
weapon v:small-mount-weapon -1 2 flip under
weapon v:repair-beam-weapon-center 0 -2
ref gamma zenith
""")
