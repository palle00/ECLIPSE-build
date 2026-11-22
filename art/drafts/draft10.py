"""max units: Colossus (200), Leviathan (224), Dreadnought (256). Modules on struts, strong negative spaces,
many medium plates rather than a few huge ones, glowing slits/vents, weapons visually separated."""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from unitlib import *

TANK = UNIT + GLOW + "outline 3 2d2f39\n"
AIR = UNIT + GLOW + "outline 3 565666\n"
ACID = "color A 9fb83a\ncolor B d6ec6a\n"
JAM = "color V b37aff\ncolor W e2ccff\n"
SHIELD = "color S 7fd6ff\ncolor Z c4f1ff\n"

def treads(name, size, x0, w, y0, h, step=8):
    g = Grid(size, size)
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            k = (y - y0) % step
            g.g[y][x] = 'K' if k >= step - 2 else ('D' if x in (x0, x0 + 1, x0 + w - 1, x0 + w - 2) else ('L' if k == 0 else 'M'))
    g.mirror()
    g.write(name, COLORS + "size %d %d\noutline 0" % (size, size))

def grille(g, x0, y0, x1, y1, step=4):
    for y in range(y0 + 2, y1 - 1, step): g.fill(rect(x0 + 2, y, x1, y), 'K')

def slit(g, pts, inner):
    g.fill(poly(pts), 'K'); g.fill(poly(inner), 'O')

def chevron(g, x0, y0, w, h):
    g.fill(poly([(x0, y0 + h), (x0 + w, y0), (x0 + w, y0 + h), (x0, y0 + 2 * h)]), 'T')
    g.fill(poly([(x0, y0 + 1.6 * h), (x0 + w, y0 + .6 * h), (x0 + w, y0 + h), (x0, y0 + 2 * h)]), 'U')

def mount(g, cx, cy, r):
    g.panel(circ(cx, cy, r))
    g.fill(ring(cx, cy, r * .62, r * .74), 'K')

# =============================== Colossus (200) ===============================
treads("colossus-treads", 200, 16, 28, 16, 168)
g = Grid(200, 200)
g.panel(poly([(12, 32), (20, 14), (48, 14), (48, 42), (12, 42)]))                         # front track guard
g.panel(poly([(12, 160), (48, 160), (48, 188), (22, 188), (12, 176)]))                    # rear track guard
g.panel(poly([(44, 46), (60, 26), (100, 22), (100, 192), (58, 192), (44, 178)]))          # hull
g.panel(poly([(44, 64), (64, 60), (64, 104), (44, 106)]))                                 # side armour, front
g.panel(poly([(44, 110), (64, 108), (64, 154), (44, 156)]))                               # side armour, rear
chevron(g, 47, 70, 14, 8); chevron(g, 47, 116, 14, 8)
g.panel(poly([(60, 28), (100, 24), (100, 58), (62, 64)]))                                 # upper glacis
g.panel(poly([(62, 64), (100, 58), (100, 70), (64, 76)]), band=1)                         # lower glacis
slit(g, [(70, 40), (94, 36), (94, 44), (70, 48)], [(72, 42), (94, 38), (94, 42), (72, 46)])
g.panel(poly([(4, 48), (14, 28), (100, 6), (100, 22), (22, 38), (14, 56)]))               # dozer ram
g.panel(poly([(4, 48), (2, 30), (10, 32), (14, 56)]), band=1)                             # ram spikes
g.panel(poly([(38, 24), (42, 10), (52, 18)]), band=1)
g.panel(poly([(88, 10), (100, 3), (100, 12)]), band=1)
mount(g, 100, 100, 42)                                                                    # main turret ring
mount(g, 44, 140, 18)                                                                     # mortar sponsons (x +/-14, y -10)
for cy in (122, 158):                                                                     # acid canisters
    g.fill(rect(60, cy - 7, 70, cy + 7), 'K'); g.fill(rect(61, cy - 6, 69, cy + 6), 'A'); g.fill(rect(61, cy - 6, 63, cy + 6), 'B')
g.panel(rect(72, 150, 100, 186)); grille(g, 72, 150, 100, 186)                            # engine deck
for x0 in (62, 80):
    g.panel(rect(x0, 184, x0 + 12, 196), band=1); g.fill(rect(x0 + 2, 192, x0 + 10, 196), 'O')
g.mirror()
g.write("colossus", TANK + ACID + """
size 200 200
on colossus-treads
weapon colossus-cannon 0 0
weapon colossus-mortar 14 -10
weapon colossus-mortar -14 -10 flip
ref conquer reign
""")

g = Grid(176, 176)                                                                        # penetrator cannon turret
g.panel(poly([(48, 98), (58, 74), (74, 64), (88, 62), (88, 138), (62, 138), (48, 124)]))   # housing
g.panel(poly([(40, 90), (50, 74), (60, 92), (50, 114)]), band=1)                          # cheek armour
g.panel(rect(62, 124, 88, 142), band=1); grille(g, 62, 124, 88, 142, 3)                   # rear bustle
g.panel(poly([(68, 92), (78, 86), (88, 86), (88, 110), (68, 110)]), band=1)               # commander's hatch
g.panel(rect(72, 52, 88, 76))                                                             # mantlet
g.panel(rect(80, 12, 88, 74), band=1)                                                     # long barrel
g.panel(rect(76, 32, 88, 42), band=1)                                                     # bore evacuator
g.panel(rect(74, 4, 88, 16), band=1)                                                      # muzzle brake
g.fill(rect(83, 4, 88, 7), 'K'); g.fill(rect(74, 9, 78, 11), 'K')
g.mirror()
g.write("colossus-cannon", COLORS + "size 176 176\noutline 3 2d2f39")

g = Grid(56, 56)                                                                          # acid mortar (Colossus and Dreadnought)
g.panel(circ(28, 32, 17))
g.panel(rect(22, 8, 28, 28), band=1)                                                      # fat tube
g.panel(rect(20, 4, 28, 10), band=1)                                                      # muzzle ring
g.fill(rect(24, 4, 28, 6), 'K')
g.fill(circ(28, 34, 7), 'K'); g.fill(circ(28, 34, 5.5), 'A'); g.fill(circ(26.5, 32.5, 2), 'B')
g.mirror()
g.write("colossus-mortar", COLORS + ACID + "size 56 56\noutline 3 2d2f39")

# =============================== Leviathan (224) ===============================
g = Grid(224, 224)
wing = poly([(88, 54), (6, 146), (4, 172), (22, 166), (32, 184), (50, 172), (62, 192), (88, 182)])
g.panel(wing)                                                                             # broad swept wing
g.panel(poly([(88, 54), (6, 146), (10, 154), (88, 66)]), band=1)                          # armoured leading edge
g.panel(poly([(80, 96), (36, 148), (48, 156), (84, 116)]), band=1)                        # inner wing plate
g.fill(poly([(22, 158), (60, 124), (62, 130), (26, 164)]), 'T'); g.fill(poly([(24, 162), (61, 128), (62, 130), (26, 164)]), 'U')
for (x0, y0) in ((42, 142), (62, 152)):                                                   # engine nacelles
    g.panel(rect(x0, y0, x0 + 12, y0 + 40)); g.fill(rect(x0 + 2, y0 + 36, x0 + 10, y0 + 42), 'O')
g.panel(poly([(4, 124), (8, 104), (14, 98), (20, 104), (22, 124), (22, 176), (4, 176)]))  # pointed jammer pod
g.fill(poly([(8, 106), (14, 100), (20, 106), (20, 112), (8, 112)]), 'V'); g.fill(rect(12, 102, 15, 105), 'W')
g.fill(ring(13, 146, 3, 5), 'V')
mount(g, 48, 136, 13)                                                                     # point defence mounts (x +/-16, y -6)
g.panel(poly([(70, 72), (74, 22), (82, 6), (88, 22), (88, 84)]), band=1)                  # forward prongs
g.fill(poly([(76, 22), (82, 10), (86, 22)]), 'V')
g.panel(poly([(88, 36), (102, 10), (112, 4), (112, 216), (94, 214), (84, 198), (84, 90)]))  # arrow hull
g.panel(poly([(84, 100), (94, 96), (94, 186), (86, 182)]), band=1)                        # hull side armour
mount(g, 112, 64, 21)                                                                     # lance housing (y +12)
g.panel(poly([(94, 94), (112, 88), (112, 132), (94, 128)]))                               # command bridge
for y in (100, 110): g.fill(rect(97, y, 112, y + 3), 'K'); g.fill(rect(98, y + 1, 112, y + 2), 'E')
g.fill(poly([(98, 24), (108, 16), (108, 20), (98, 28)]), 'K'); g.fill(poly([(99, 25), (108, 18), (108, 19), (99, 26)]), 'O')
for x0 in (96, 104):                                                                      # dorsal jamming fins
    g.panel(poly([(x0, 152), (x0 + 4, 140), (x0 + 7, 146), (x0 + 7, 188), (x0, 186)]), band=1)
    g.fill(rect(x0 + 3, 140, x0 + 6, 144), 'V')
g.panel(rect(100, 198, 112, 218), band=1); g.fill(rect(103, 212, 112, 218), 'K')           # tail nozzle
g.mirror()
g.write("leviathan", AIR + JAM + """
color E 8fd8ff
size 224 224
weapon leviathan-point-defense 16 -6
weapon leviathan-point-defense -16 -6 flip
weapon leviathan-lance 0 12
ref eclipse antumbra
""")

g = Grid(72, 72)                                                                          # heavy lance emitter
g.panel(poly([(20, 46), (26, 32), (36, 28), (36, 64), (24, 64), (20, 58)]))                 # angular housing
for y in (36, 42, 48, 54): g.panel(rect(12, y, 20, y + 3), band=1)                          # radiator fins
g.panel(rect(31, 6, 36, 32), band=1)                                                      # emitter barrel
for y in (9, 16, 23):                                                                     # focusing coils
    g.panel(rect(27, y, 36, y + 3), band=1); g.fill(rect(29, y + 1, 36, y + 2), 'V')
g.fill(rect(32, 2, 36, 6), 'K'); g.fill(rect(33, 2, 36, 5), 'V'); g.fill(rect(34, 2, 36, 3), 'W')   # lens
g.panel(rect(28, 60, 36, 68), band=1)
g.mirror()
g.write("leviathan-lance", COLORS + JAM + "size 72 72\noutline 3 565666")

g = Grid(36, 36)                                                                          # twin point-defence turret
g.panel(circ(18, 21, 10))
g.panel(rect(14, 4, 16, 16), band=1)
g.fill(rect(14, 4, 16, 5), 'K')
g.fill(circ(18, 22, 3), 'V')
g.mirror()
g.write("leviathan-point-defense", COLORS + JAM + "size 36 36\noutline 3 565666")

# =============================== Dreadnought (256) ===============================
treads("dreadnought-treads", 256, 18, 36, 20, 216, 10)
g = Grid(256, 256)
g.panel(poly([(14, 40), (24, 22), (60, 22), (60, 52), (14, 52)]))                         # front track guard
g.panel(rect(14, 114, 60, 142))                                                           # middle track guard
g.panel(poly([(14, 212), (60, 212), (60, 244), (26, 244), (14, 232)]))                    # rear track guard
g.panel(poly([(60, 56), (76, 40), (128, 36), (128, 244), (74, 244), (60, 230)]))          # chassis
g.panel(poly([(58, 62), (80, 58), (80, 112), (58, 114)]))                                 # side armour slabs
g.panel(poly([(58, 144), (80, 142), (80, 206), (58, 208)]))
chevron(g, 62, 68, 14, 9); chevron(g, 62, 150, 14, 9)
# frontal shield assembly: a broad projector bar on struts, cyan emitters, pylons at the ends
g.panel(poly([(8, 44), (18, 20), (128, 6), (128, 20), (26, 32), (18, 52)]))
g.fill(poly([(18, 20), (128, 6), (128, 9), (19, 23)]), 'S'); g.fill(poly([(60, 13), (128, 6), (128, 7), (60, 14)]), 'Z')
g.panel(poly([(2, 46), (4, 22), (14, 18), (22, 46), (12, 60)]), band=1)                   # shield pylons
g.fill(rect(6, 24, 12, 30), 'S'); g.fill(rect(7, 25, 9, 27), 'Z')
for x0 in (70, 104): g.panel(rect(x0, 20, x0 + 8, 40), band=1)                            # struts (gaps between them)
# command block with the jammer dish
g.panel(poly([(94, 44), (128, 40), (128, 92), (94, 92)]))
slit(g, [(98, 52), (122, 48), (122, 54), (98, 58)], [(100, 53), (122, 50), (122, 52), (100, 56)])
g.fill(circ(128, 76, 12), 'K'); g.fill(circ(128, 76, 10), 'D'); g.fill(ring(128, 76, 4, 7), 'V'); g.fill(circ(128, 76, 3), 'W')
g.panel(poly([(98, 62), (104, 56), (108, 60), (108, 88), (98, 88)]), band=1); g.fill(rect(100, 58, 104, 62), 'V')   # antenna
mount(g, 68, 104, 25)                                                                     # cannon sponsons (x +/-15, y +6)
mount(g, 128, 168, 24)                                                                    # heat lance mount (y -10)
for y in (150, 158): g.fill(rect(96, y, 104, y + 3), 'K'); g.fill(rect(97, y + 1, 104, y + 2), 'O')
mount(g, 68, 200, 17)                                                                     # mortar mounts (x +/-15, y -18)
for cy in (184, 220):
    g.fill(rect(84, cy - 7, 94, cy + 7), 'K'); g.fill(rect(85, cy - 6, 93, cy + 6), 'A'); g.fill(rect(85, cy - 6, 87, cy + 6), 'B')
g.panel(rect(98, 198, 128, 240)); grille(g, 98, 198, 128, 240)                            # engine deck
for x0 in (76, 100):
    g.panel(rect(x0, 236, x0 + 14, 250), band=1); g.fill(rect(x0 + 2, 246, x0 + 12, 250), 'O')
g.mirror()
g.write("dreadnought", TANK + ACID + JAM + SHIELD + """
size 256 256
on dreadnought-treads
weapon dreadnought-cannon 15 6
weapon dreadnought-cannon -15 6 flip
weapon dreadnought-lance 0 -10
weapon colossus-mortar 15 -18
weapon colossus-mortar -15 -18 flip
ref conquer reign
""")

g = Grid(144, 144)                                                                        # heavy AP cannon
g.panel(poly([(38, 84), (46, 62), (60, 54), (72, 52), (72, 114), (50, 114), (38, 102)]))   # housing
g.panel(poly([(30, 76), (40, 60), (48, 78), (40, 98)]), band=1)                           # cheek armour
g.panel(rect(52, 102, 72, 118), band=1); grille(g, 52, 102, 72, 118, 3)                   # rear bustle
g.panel(poly([(56, 76), (64, 72), (72, 72), (72, 92), (56, 92)]), band=1)                 # hatch
g.panel(rect(60, 42, 72, 62))                                                             # mantlet
g.panel(rect(66, 10, 72, 60), band=1)                                                     # long barrel
for y in (22, 34): g.fill(rect(66, y, 72, y), 'K')
g.panel(rect(62, 4, 72, 12), band=1); g.fill(rect(69, 4, 72, 6), 'K')                     # muzzle
g.mirror()
g.write("dreadnought-cannon", COLORS + "size 144 144\noutline 3 2d2f39")

g = Grid(80, 80)                                                                          # heat lance emitter
g.panel(poly([(14, 48), (22, 32), (40, 26), (40, 72), (22, 72), (14, 62)]))
for y in range(36, 66, 6): g.panel(rect(6, y, 14, y + 3), band=1)                         # radiator fins
for y in (40, 48, 56): g.fill(rect(24, y, 32, y + 3), 'K'); g.fill(rect(25, y + 1, 32, y + 2), 'O')   # heat vents
g.panel(rect(34, 8, 40, 30), band=1)
for y in (12, 20): g.panel(rect(30, y, 40, y + 3), band=1)
g.fill(rect(34, 3, 40, 8), 'K'); g.fill(rect(35, 4, 40, 7), 'O'); g.fill(rect(37, 4, 40, 5), 'P')
g.mirror()
g.write("dreadnought-lance", COLORS + GLOW + "size 80 80\noutline 3 2d2f39")
