"""The remaining high-effort units, v3 style: bigger sprites, faceted plates, aggressive shapes."""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from unitlib import *

TANK = UNIT + GLOW + "outline 3 2d2f39\n"
AIR = UNIT + GLOW + "outline 3 565666\n"
BOAT = UNIT + GLOW + "outline 3 2d2f39\ncolor C 3f7fa0\ncolor E 8fd8ff\n"
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
def bug(g, test, rim=None, **kw): g.panel(test, light='g', mid='h', dark='i', seamch='k', **kw)

def treads(name, size, x0, w, y0, h):
    g = Grid(size, size)
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            g.g[y][x] = 'K' if (y - y0) % 5 == 4 else ('D' if x in (x0, x0 + w - 1) else ('L' if (y - y0) % 5 == 0 else 'M'))
    g.mirror()
    g.write(name, COLORS + "size %d %d\noutline 0" % (size, size))

# ======================= Reaper (48): fast light tank, narrow chassis, long autocannon =======================
treads("reaper-treads", 48, 5, 6, 5, 38)
g = Grid(48, 48)
g.panel(poly([(4, 8), (8, 4), (12, 4), (12, 11), (4, 11)]), rim=2)                     # fenders
g.panel(poly([(4, 37), (12, 37), (12, 44), (8, 44), (4, 41)]), rim=2)
g.panel(poly([(11, 14), (18, 4), (24, 3), (24, 44), (14, 44), (11, 40)]), rim=3)       # wedge hull
g.fill(poly([(12, 22), (17, 20), (17, 24), (12, 26)]), 'T'); g.fill(poly([(12, 25), (17, 23), (17, 24), (12, 26)]), 'U')
g.fill(poly([(17, 11), (23, 10), (23, 12), (17, 13)]), 'O')                            # eye slit
for y in range(34, 42, 2): g.fill(rect(17, y, 23, y), 'K')                             # engine grille
g.mirror()
g.write("reaper", TANK + "size 48 48\non reaper-treads\nweapon reaper-gun 0 0\nref dagger locus")
g = Grid(32, 32)
g.panel(circ(16, 19, 7), rim=2)
g.panel(rect(14, 1, 15, 15), rim=1)
g.fill(rect(14, 1, 15, 2), 'K')
g.mirror()
g.write("reaper-gun", COLORS + "size 32 32\noutline 3 2d2f39")

# ======================= Bulwark (72): armoured tank, thick AP cannon =======================
treads("bulwark-treads", 72, 7, 10, 7, 58)
g = Grid(72, 72)
g.panel(poly([(5, 12), (10, 5), (19, 5), (19, 16), (5, 16)]), rim=2)
g.panel(poly([(5, 58), (19, 58), (19, 68), (10, 68), (5, 64)]), rim=2)
g.panel(poly([(16, 18), (24, 9), (36, 8), (36, 67), (21, 67), (16, 62)]), rim=4)        # hull
g.panel(poly([(16, 26), (24, 24), (24, 56), (16, 56)]), rim=3)                          # side armour
for y0 in (32, 42):
    g.fill(poly([(17, y0), (23, y0 - 2), (23, y0 + 2), (17, y0 + 4)]), 'T'); g.fill(poly([(17, y0 + 2), (23, y0), (23, y0 + 2), (17, y0 + 4)]), 'U')
g.panel(poly([(24, 12), (36, 10), (36, 22), (24, 24)]), rim=3)                          # glacis
g.fill(poly([(26, 16), (34, 15), (34, 17), (26, 18)]), 'O')
g.panel(rect(26, 52, 36, 66), rim=2)
for y in range(54, 65, 3): g.fill(rect(27, y, 35, y), 'K')
g.mirror()
g.write("bulwark", TANK + "size 72 72\non bulwark-treads\nweapon bulwark-cannon 0 0\nref mace locus")
g = Grid(48, 48)
g.panel(poly([(12, 22), (18, 16), (24, 16), (24, 38), (14, 38), (12, 34)]), rim=3)
g.panel(rect(20, 2, 23, 18), rim=2)
g.fill(rect(20, 2, 23, 4), 'K'); g.fill(rect(21, 2, 23, 3), 'D')
g.fill(rect(20, 10, 23, 10), 'K')
g.mirror()
g.write("bulwark-cannon", COLORS + "size 48 48\noutline 3 2d2f39")

# ======================= Wraith (48): fast recon, needle fuselage, small forward guns =======================
g = Grid(48, 48)
g.panel(poly([(4, 32), (19, 20), (22, 26), (10, 38)]), rim=2)                          # forward-swept wing
g.fill(poly([(8, 32), (18, 25), (19, 27), (9, 34)]), 'T')
g.panel(rect(11, 13, 12, 26), rim=1)                                                   # gun barrel on the wing
g.panel(poly([(15, 40), (20, 34), (21, 44), (16, 45)]), rim=1)                         # tail fin
g.panel(poly([(20, 12), (24, 2), (24, 45), (21, 44), (19, 30)]), rim=2)                # needle fuselage
g.fill(rect(22, 6, 24, 9), 'E')                                                        # sensor nose
g.mirror()
g.write("wraith", AIR + "color E 8fd8ff\nsize 48 48\nref flare horizon")

# ======================= Interceptor (64): sleek fighter, forward missile launchers =======================
g = Grid(64, 64)
g.panel(poly([(3, 46), (26, 22), (28, 42), (10, 52)]), rim=3)                          # swept wing
g.fill(poly([(9, 46), (25, 32), (26, 36), (11, 49)]), 'T'); g.fill(poly([(10, 48), (26, 34), (26, 36), (11, 49)]), 'U')
g.panel(poly([(18, 54), (25, 44), (27, 46), (26, 60), (19, 60)]), rim=2)               # tail fin
g.panel(poly([(24, 14), (32, 2), (32, 60), (26, 60), (23, 40)]), rim=3)                # fuselage
g.fill(poly([(28, 10), (32, 7), (32, 18), (28, 16)]), 'K'); g.fill(poly([(29, 11), (32, 9), (32, 16), (29, 15)]), 'E')
g.fill(rect(27, 56, 31, 60), 'K'); g.fill(rect(28, 58, 31, 59), 'O')
g.mirror()
g.write("interceptor", AIR + "color E 8fd8ff\nsize 64 64\nweapon interceptor-launcher 4 0\nweapon interceptor-launcher -4 0 flip\nref horizon zenith")
g = Grid(24, 24)
g.panel(rect(8, 6, 11, 20), rim=2)
g.fill(rect(10, 3, 11, 6), 'R'); g.fill(rect(10, 3, 11, 3), 'O')                       # missile nose
g.mirror()
g.write("interceptor-launcher", COLORS + GLOW + "color R d9584a\nsize 24 24\noutline 3 565666")

# ======================= Support robots (48) =======================
def rotor(g, cx, cy, r=6):
    g.panel(circ(cx, cy, r), rim=2)
    g.fill(circ(cx, cy, r - 2.5), 'K'); g.fill(circ(cx, cy, 1.4), 'D')

g = Grid(48, 48)                                                                       # logistics: cargo quadcopter
for (cx, cy) in ((10, 11), (10, 37)):
    g.panel(poly([(cx + 2, cy - 1), (22, 21), (22, 26), (cx + 2, cy + 2)]), rim=1)
    rotor(g, cx, cy)
g.panel(rect(14, 14, 24, 34), rim=3)
g.fill(rect(15, 17, 24, 30), 'W'); g.fill(rect(15, 17, 18, 30), 'X')                    # cargo crate
g.fill(rect(15, 23, 24, 24), 'K'); g.fill(rect(20, 17, 24, 19), 'T')
g.mirror()
g.write("logistics-drone", AIR + "color W c9a26f\ncolor X 9b7650\nsize 48 48\nref mono poly")

g = Grid(48, 48)                                                                       # repair: body, side thrusters, repair arm
for cy in (16, 32): rotor(g, 9, cy, 5)
g.panel(poly([(12, 22), (16, 16), (24, 14), (24, 38), (16, 36), (12, 30)]), rim=3)
g.panel(rect(21, 3, 24, 14), rim=1)                                                    # repair arm
g.fill(rect(20, 2, 24, 5), 'H'); g.fill(rect(22, 3, 24, 4), 'J')
g.fill(rect(18, 22, 24, 30), 'T'); g.fill(rect(18, 27, 24, 30), 'U')
g.mirror()
g.write("repair-drone", AIR + "color H 62ae7e\ncolor J 84f490\nsize 48 48\nref mono poly")

g = Grid(48, 48)                                                                       # shield: emitter dome with focusing fins
for (pts) in ([(6, 14), (12, 8), (16, 16), (11, 20)], [(4, 30), (12, 26), (13, 34), (6, 38)]):
    g.panel(poly(pts), rim=2)
g.panel(circ(24, 24, 13), rim=3)
g.fill(circ(24, 24, 8), 'K'); g.fill(circ(24, 24, 6.5), 'S'); g.fill(circ(22.5, 22.5, 2.5), 'V')
g.fill(rect(22, 36, 24, 39), 'T')
g.mirror()
g.write("shield-drone", AIR + "color S 4fa7c7\ncolor V 9be8ff\nsize 48 48\nref mono poly")

g = Grid(48, 48)                                                                       # electronic warfare: dish array and antennas
g.panel(poly([(4, 10), (8, 6), (16, 16), (12, 20)]), rim=1)
g.panel(poly([(4, 38), (12, 28), (16, 32), (8, 42)]), rim=1)
g.fill(rect(3, 5, 6, 8), 'R'); g.fill(rect(3, 40, 6, 43), 'R')
g.panel(poly([(12, 18), (18, 10), (24, 10), (24, 40), (16, 38), (12, 32)]), rim=3)
g.fill(circ(24, 24, 8), 'K'); g.panel(circ(24, 24, 6.5), rim=2); g.fill(circ(24, 24, 2), 'R')
g.fill(rect(20, 34, 24, 37), 'T')
g.mirror()
g.write("ew-drone", AIR + "color R e05ad1\nsize 48 48\nref mono oct")

# ======================= The Swarm: bio-mechanical bugs =======================
def claws(name, size, foot):
    g = Grid(size, size)
    hw = size // 2
    g.hw = size                                                                        # draw the right side only; the game mirrors legs
    bug(g, poly(foot), rim=2)
    g.write(name, SWARMPART + "size %d %d" % (size, size))

# Mite (40): beetle with mandibles and a glowing acid sac
g = Grid(40, 40)
bug(g, poly([(8, 14), (14, 7), (20, 6), (20, 34), (12, 34), (7, 26)]), rim=3)          # carapace
bug(g, poly([(10, 26), (20, 24), (20, 35), (14, 36)]), rim=2)                          # rear segment
g.fill(circ(20, 30, 4), 'A'); g.fill(circ(19, 29, 1.5), 'B')                            # acid sac
bug(g, poly([(13, 8), (14, 2), (18, 5), (17, 9)]), rim=1)                              # mandibles
g.fill(poly([(12, 16), (18, 13), (18, 16), (12, 19)]), 'T'); g.fill(poly([(12, 18), (18, 15), (18, 16), (12, 19)]), 'U')
g.fill(rect(17, 9, 18, 10), 'A')                                                       # eyes
g.mirror()
g.write("mite", SWARM + "size 40 40\non mite-base\nref crawler dagger")
claws("mite-leg", 40, [(24, 14), (34, 10), (37, 16), (34, 18), (26, 20)])
g = Grid(28, 28); bug(g, circ(14, 14, 9), rim=2); g.mirror(); g.write("mite-base", SWARMPART + "size 28 28")

# Wasp (44): flyer, two wing pairs, striped abdomen, sting forward
g = Grid(44, 44)
g.fill(poly([(3, 14), (18, 16), (16, 22), (4, 22)]), 'W'); g.fill(poly([(5, 26), (18, 24), (16, 30), (6, 32)]), 'W')   # wings
g.fill(poly([(3, 14), (18, 16), (18, 17), (3, 16)]), 'X')
bug(g, poly([(18, 8), (22, 4), (22, 18), (17, 16)]), rim=2)                            # head with sting forward
g.fill(rect(20, 2, 22, 5), 'A')
bug(g, poly([(16, 18), (22, 16), (22, 42), (17, 38)]), rim=2)                          # thorax/abdomen
for y in (26, 32, 38): g.fill(rect(17, y, 22, y + 1), 'A')
g.fill(rect(19, 19, 22, 23), 'T'); g.fill(rect(19, 22, 22, 23), 'U')
g.mirror()
g.write("wasp", SWARM + "color W c9d6b0\ncolor X e8f0d8\nsize 44 44\nref flare horizon")

# Spitter (56): bulky, big acid sac on its back, spitting snout
g = Grid(56, 56)
bug(g, poly([(10, 20), (18, 10), (28, 8), (28, 46), (16, 46), (9, 36)]), rim=4)
g.fill(both(circ(28, 32, 11), lambda px, py: True), 'k'); g.fill(circ(28, 32, 9.5), 'A'); g.fill(circ(26, 29, 4), 'B')
bug(g, poly([(22, 10), (25, 2), (28, 2), (28, 12)]), rim=1)                            # snout
g.fill(rect(26, 2, 28, 3), 'k')
bug(g, poly([(12, 12), (16, 4), (20, 8), (17, 14)]), rim=1)                            # mandibles
g.fill(poly([(12, 22), (22, 18), (22, 22), (12, 26)]), 'T'); g.fill(poly([(12, 25), (22, 21), (22, 22), (12, 26)]), 'U')
g.mirror()
g.write("spitter", SWARM + "size 56 56\non spitter-base\nref dagger mace")
claws("spitter-leg", 56, [(34, 20), (48, 14), (52, 22), (48, 25), (36, 28)])
g = Grid(40, 40); bug(g, circ(20, 20, 13), rim=3); g.mirror(); g.write("spitter-base", SWARMPART + "size 40 40")

# Brood (80): the brood mother - wide armoured carapace, spore pods, spikes
g = Grid(80, 80)
bug(g, poly([(8, 30), (18, 14), (30, 8), (40, 8), (40, 70), (26, 74), (12, 64), (6, 46)]), rim=5)
bug(g, poly([(4, 30), (10, 22), (12, 36)]), rim=1); bug(g, poly([(4, 52), (10, 46), (12, 58)]), rim=1)    # spikes
for (cx, cy) in ((22, 34), (20, 50), (30, 60)):
    g.fill(circ(cx, cy, 6), 'k'); g.fill(circ(cx, cy, 5), 'S'); g.fill(circ(cx - 1.5, cy - 1.5, 2), 'Q')   # spore pods
bug(g, poly([(28, 12), (34, 2), (40, 4), (40, 16)]), rim=2)                            # head / mandibles
g.fill(rect(34, 10, 36, 12), 'A')
g.fill(poly([(30, 22), (40, 20), (40, 26), (30, 28)]), 'T'); g.fill(poly([(30, 26), (40, 24), (40, 26), (30, 28)]), 'U')
g.mirror()
g.write("brood", SWARM + "color S 7457ce\ncolor Q b48cff\nsize 80 80\non brood-base\nref mace fortress")
claws("brood-leg", 80, [(48, 28), (68, 18), (75, 30), (68, 34), (50, 40)])
g = Grid(56, 56); bug(g, circ(28, 28, 19), rim=4); g.mirror(); g.write("brood-base", SWARMPART + "size 56 56")

# ======================= The Tide: boats and submarines =======================
# Skiff (48x56): fast boat, water cannon at the bow, two light guns (x = +/-3 -> 12 px)
g = Grid(*scaled(1.35, 48, 56))
g.panel(poly([(24, 2), (15, 12), (9, 28), (10, 46), (14, 53), (24, 54)]), rim=3)
g.fill(poly([(24, 10), (17, 18), (14, 30), (15, 46), (24, 48)]), 'M')
g.panel(rect(18, 26, 24, 40), rim=2)
g.fill(rect(19, 28, 24, 31), 'K'); g.fill(rect(20, 29, 24, 30), 'E')                  # windshield
g.fill(rect(11, 30, 13, 40), 'T'); g.fill(rect(11, 36, 13, 40), 'U')
g.panel(rect(11, 18, 13, 27), rim=1)                                                  # light gun
g.mirror()
g.fill(rect(22, 4, 25, 10), 'W', full=True); g.fill(rect(23, 4, 24, 6), 'E', full=True)   # water cannon
g.write("skiff", BOAT + "color W 3f6fa0\nsize 48 56\nref risso retusa")
unscaled()

# Slicker (60x72): oil sprayer at the bow, incendiary guns at +/-4 (16 px), oil tanks
g = Grid(*scaled(1.4, 60, 72))
g.panel(poly([(30, 2), (18, 14), (11, 34), (12, 60), (18, 69), (30, 70)]), rim=4)
g.fill(poly([(30, 12), (21, 22), (16, 38), (17, 58), (30, 62)]), 'M')
for cy in (40, 52):
    g.fill(circ(23, cy, 6), 'K'); g.fill(circ(23, cy, 5), 'Y'); g.fill(circ(21.5, cy - 1.5, 2), 'Z')    # oil drums
g.panel(rect(24, 22, 30, 34), rim=2)
g.fill(rect(25, 24, 30, 26), 'K'); g.fill(rect(26, 25, 30, 25), 'O')
g.panel(rect(13, 22, 16, 34), rim=1); g.fill(rect(13, 21, 16, 23), 'O')               # incendiary gun
g.fill(rect(12, 40, 15, 54), 'T'); g.fill(rect(12, 48, 15, 54), 'U')
g.mirror()
g.fill(rect(28, 4, 31, 12), 'Y', full=True); g.fill(rect(29, 4, 30, 6), 'O', full=True)
g.write("slicker", BOAT + "color Y 3b3329\ncolor Z 6b5d4a\nsize 60 72\nref oxynoe minke")
unscaled()

# Frostwake (72x88): icebreaker, cryo spray at the bow, cryo tanks, mine rack 24 px aft
g = Grid(*scaled(1.4, 72, 88))
g.panel(poly([(36, 1), (22, 14), (14, 36), (14, 74), (20, 84), (36, 86)]), rim=4)
g.panel(poly([(36, 1), (26, 12), (32, 16), (36, 10)]), rim=2)                          # ice ram
g.fill(poly([(36, 14), (26, 24), (20, 40), (20, 72), (36, 76)]), 'M')
for cy in (34, 48):
    g.fill(circ(26, cy, 7), 'K'); g.fill(circ(26, cy, 6), 'F'); g.fill(circ(24, cy - 2, 2.5), 'H')     # cryo tanks
g.panel(poly([(30, 26), (36, 22), (36, 56), (30, 56)]), rim=3)
g.fill(rect(31, 30, 36, 32), 'K'); g.fill(rect(32, 31, 36, 31), 'H')
g.fill(rect(16, 40, 19, 62), 'T'); g.fill(rect(16, 54, 19, 62), 'U')
g.fill(rect(26, 64, 36, 72), 'K')
for cx in (29, 34): g.fill(circ(cx, 68, 2.2), 'O')                                     # mine rack
g.mirror()
g.fill(rect(34, 4, 37, 12), 'F', full=True); g.fill(rect(35, 4, 36, 6), 'H', full=True)
g.write("frostwake", BOAT + "color F 4fa7c7\ncolor H a6f0ff\nsize 72 88\nref minke bryde")
unscaled()

# Lurker (48x60): small submarine, torpedo tube at the bow
g = Grid(*scaled(1.35, 48, 60))
g.panel(poly([(24, 3), (16, 12), (13, 30), (14, 50), (24, 57)]), rim=3)
g.panel(poly([(19, 22), (24, 18), (24, 40), (19, 38)]), rim=2)                         # conning tower
g.fill(rect(21, 24, 24, 25), 'E')
g.fill(rect(14, 30, 16, 44), 'T'); g.fill(rect(14, 38, 16, 44), 'U')
g.fill(rect(22, 3, 24, 7), 'K')
g.mirror()
g.write("lurker", BOAT + "size 48 60\nref risso retusa")
unscaled()

# Barracuda (72x96): attack submarine, twin torpedo tubes (x = +/-4 -> 16 px), deck gun 12 px forward
g = Grid(*scaled(1.45, 72, 96))
g.panel(poly([(36, 3), (24, 16), (18, 40), (19, 78), (28, 90), (36, 92)]), rim=4)
g.panel(poly([(27, 52), (36, 46), (36, 72), (28, 70)]), rim=3)                         # sail
g.fill(rect(30, 54, 36, 55), 'E')
for x0 in (19,):
    g.fill(rect(x0, 10, x0 + 3, 16), 'K')
g.panel(poly([(16, 64), (19, 58), (19, 80), (14, 82)]), rim=1)                         # dive planes
g.fill(rect(20, 44, 23, 66), 'T'); g.fill(rect(20, 58, 23, 66), 'U')
g.mirror()
g.fill(circ(36, 36, 7), 'K', full=True); g.fill(circ(36, 36, 6), 'D', full=True); g.fill(circ(35, 35, 3.5), 'M', full=True)   # deck gun
g.fill(rect(35, 22, 37, 32), 'L', full=True); g.fill(rect(35, 22, 37, 23), 'K', full=True)
g.fill(rect(19, 6, 22, 12), 'K', full=True); g.fill(rect(49, 6, 52, 12), 'K', full=True)
g.write("barracuda", BOAT + "size 72 96\nref minke bryde")
unscaled()
