"""Bombard (4x4), Event Horizon (4x4) and Payload Catapult (5x5): turret sprites + their moving parts.
Coordinates are full-canvas (turret points up); half grids keep x < W/2 and PixelArt mirrors them."""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from turretlib import *
OUT = sys.argv[1]
ORANGE = "color d d96b68 e\ncolor e fdb380 d\n"
PURPLE = "color d 8a5ad1 e\ncolor e c88fff d\n"
COPPER = "color q a8664c r\ncolor r dc9b74 q\n"

# ======================= Bombard =======================
# main: bolted turntable, mount block, fixed piston cylinders, rear shell hopper with hot warning stripes
g = Grid(128)
for (cx, cy) in ((24, 34), (20, 108)):                                       # heavy mounting lugs on the turntable
    g.poly([(cx - 10, cy - 6), (cx + 4, cy - 12), (cx + 12, cy), (cx + 4, cy + 12), (cx - 10, cy + 6)], 'a')
    g.ridge(cx - 6, cy - 4, cx + 6, cy + 4, cx, 'k', 'l')
g.ring(64, 70, 46, 55, 'a')                                                # turntable rim
for k in range(12):                                                         # rim bolts
    a = math.radians(k * 30 + 15); g.circle(64 + 50.5 * math.cos(a), 70 + 50.5 * math.sin(a), 1.6, 'z')
g.circle(64, 70, 45, 'k')                                                   # turntable deck
g.ring(64, 70, 44, 45.2, '.')
for (y0, y1) in ((52, 59), (64, 71), (76, 83)):                              # deck tread plates
    g.ridge(14, y0, 30, y1, 22, 'k', 'l')
g.poly([(38, 62), (46, 54), (64, 54), (64, 104), (38, 104)], 'a')           # mount block under the tube
g.poly([(42, 64), (48, 58), (64, 58), (64, 100), (42, 100)], 'b')
g.rect(42, 80, 63, 81, '.')
for y in (86, 90, 94): g.rect(48, y, 63, y + 1, 'm')                       # one cooling grille across the mount (glows after a shot)
g.rect(22, 44, 34, 96, 'a'); g.rect(28, 44, 34, 96, 'b')                    # piston cylinders (fixed)
g.rect(20, 44, 36, 48, 'k'); g.rect(20, 92, 36, 96, 'k')                     # cylinder end caps
g.rect(22, 96, 34, 100, '.')
g.poly([(30, 104), (40, 100), (64, 100), (64, 126), (34, 126), (30, 120)], 'a')   # shell hopper at the rear
g.poly([(34, 106), (42, 104), (64, 104), (64, 112), (34, 112)], 'b')
for i in range(8):                                                          # hot warning stripes across its rear face
    x0 = 32 + i * 5
    g.poly([(x0, 124), (x0 + 3, 114), (x0 + 6, 114), (x0 + 3, 124)], 'd' if i % 2 == 0 else 'z')
g.replace(lambda px, py: px > 64, 'dz', 'a')
g.write(OUT, "bombard", turret_header(128, 4, ORANGE + "heat m n", refs="ripple foreshadow"))

# part -tube: fat mortar tube with a wide dark bore, reinforcing bands, a yoke and the piston rods (recoils back)
g = Grid(128)
g.rect(24, 30, 32, 52, 'b'); g.rect(24, 30, 26, 52, 'a')                    # piston rods
g.poly([(20, 28), (44, 26), (44, 40), (20, 40)], 'a')                       # yoke across to the rods
g.rect(20, 28, 44, 30, 'b')
g.rect(42, 30, 64, 76, 'a')                                                 # tube body
g.ridge(42, 46, 63, 52, 54)                                                 # reinforcing bands
g.ridge(42, 62, 63, 68, 54)
g.seam(42, 45, 63, 45); g.seam(42, 53, 63, 53); g.seam(42, 61, 63, 61); g.seam(42, 69, 63, 69)
g.circle(64, 28, 25, 'a')                                                   # muzzle collar
g.ring(64, 28, 20, 25, 'b')
g.ring(64, 28, 20, 21.2, '.')
g.ring(64, 28, 15.5, 20, 'm')                                               # bore rim (glows with heat)
g.circle(64, 28, 15.5, 'z')                                                 # the wide dark bore
g.ring(64, 28, 9, 10, 'k')
g.write(OUT, "bombard-tube", turret_header(128, 4, ORANGE + "heat m n\non bombard", refs="ripple"))

# ======================= Event Horizon =======================
g = Grid(128)
g.poly([(6, 96), (6, 66), (16, 46), (30, 40), (30, 108), (16, 112)], 'a')   # stepped side armour
g.poly([(10, 92), (10, 68), (18, 52), (26, 49), (26, 102), (17, 106)], 'b')
g.seam(10, 78, 26, 79); g.seam(10, 92, 26, 93)
g.poly([(30, 92), (38, 86), (64, 86), (64, 124), (36, 124), (30, 118)], 'a')   # rear capacitor block
for y in range(94, 122, 6): g.ridge(36, y, 63, y + 3, 50, 'k', 'l')
g.circle(64, 60, 33, 'a')                                                   # chamber housing
for (a0, a1) in ((185, 265), (275, 355), (95, 175)):                        # containment segments
    g.arc(64, 60, 24, 33, a0, a1, 'b')
g.ring(64, 60, 23, 24.5, '.')
g.ring(64, 60, 17, 23, 'd')                                                 # purple only round the aperture
g.ring(64, 60, 19.5, 23, 'e')
g.circle(64, 60, 17, 'z')                                                   # the dark, recessed chamber
g.ring(64, 60, 10, 11.5, 'k')
g.circle(64, 60, 4, 'k')
g.poly([(56, 22), (60, 18), (64, 18), (64, 30), (56, 30)], 'a')             # focusing prong toward the muzzle
g.rect(60, 18, 63, 22, 'e')
g.rect(28, 50, 38, 70, 'k')                                                 # claw pivot housing (the claws are a part)
g.ridge(30, 52, 37, 68, 34, 'a', 'b')
g.write(OUT, "event-horizon", turret_header(128, 4, PURPLE + "heat d e", refs="foreshadow meltdown"))

# part -claw: ONE containment claw on the right; the game mirrors it and opens both while the turret warms up
g = Grid(128, half=False)
claw = [(92, 66), (100, 52), (102, 34), (96, 16), (88, 6), (82, 10), (88, 22), (90, 36), (86, 52), (80, 62)]
g.poly(claw, 'a')
g.poly([(92, 62), (98, 50), (99, 35), (94, 20), (90, 13), (88, 14), (92, 24), (94, 36), (90, 52), (86, 60)], 'b')
g.seam(84, 40, 104, 41)                                                     # joint between the two segments
g.poly([(82, 10), (88, 6), (90, 9), (85, 13)], 'e')                         # purple-lit emitter tip
g.write(OUT, "event-horizon-claw-r", turret_header(128, 4, PURPLE + "heat e\non event-horizon", mirror=False, refs="fuse"))
# -l: the game draws it flipped onto the left side, so it is the same claw with light and dark swapped (turret shading)
SWAP = str.maketrans("abklmnde", "balknmed")
g.g = [[c.translate(SWAP) for c in row] for row in g.g]
g.write(OUT, "event-horizon-claw-l", turret_header(128, 4, PURPLE + "heat d\non event-horizon", mirror=False, refs="fuse"))

# ======================= Payload Catapult =======================
g = Grid(160)
g.ring(80, 84, 62, 72, 'a')                                                 # turntable rim
for k in range(16):
    a = math.radians(k * 22.5 + 11); g.circle(80 + 67 * math.cos(a), 84 + 67 * math.sin(a), 1.8, 'z')
g.circle(80, 84, 61, 'k')
g.ring(80, 84, 60, 61.2, '.')
g.rect(38, 38, 79, 122, 'z')                                                # the open cradle floor (the block sits here)
for x in range(44, 80, 9): g.rect(x, 40, x + 1, 120, 'k')                   # floor lattice
for y in range(46, 122, 9): g.rect(38, y, 79, y + 1, 'k')
g.poly([(26, 128), (40, 124), (80, 124), (80, 156), (36, 156), (26, 146)], 'a')   # counterweight plates at the rear
g.poly([(30, 132), (42, 128), (80, 128), (80, 136), (30, 136)], 'b')
g.seam(30, 141, 80, 141); g.seam(30, 149, 80, 149)
for x in range(38, 80, 10):
    g.circle(x, 145, 1.5, 'z'); g.circle(x, 153, 1.5, 'z')
g.rect(20, 118, 80, 124, 'l')                                               # the axle
g.rect(14, 112, 30, 130, 'a'); g.rect(22, 112, 30, 130, 'b')               # square bearing blocks (no round lumps)
g.rect(17, 119, 26, 123, 'm')                                               # winch motor (warm after a throw)
g.write(OUT, "payload-catapult", turret_header(160, 5, COPPER + "heat m n", refs="ripple foreshadow"))

# part -arm: side beams with copper guide rails and the front crossbar (lunges forward when it throws)
g = Grid(160)
g.rect(26, 22, 38, 120, 'a'); g.rect(32, 22, 38, 120, 'b')                 # side beam
g.rect(39, 26, 42, 116, 'q')                                                # copper guide rail along the cradle
g.rect(40, 26, 41, 116, 'r')
for y in (40, 64, 88, 110): g.rect(26, y, 38, y + 2, 'k')                   # beam cross-ties
g.poly([(22, 10), (34, 4), (80, 4), (80, 24), (26, 24), (22, 20)], 'a')    # front crossbar / lip
g.poly([(28, 10), (36, 7), (80, 7), (80, 14), (28, 14)], 'b')
g.seam(26, 24, 80, 25)
g.rect(70, 14, 80, 22, 'z')                                                 # release latch
g.write(OUT, "payload-catapult-arm", turret_header(160, 5, COPPER + "on payload-catapult", refs="ripple"))
