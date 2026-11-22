"""2x2: Augmentation Hub, Gear Press, Extruder, Metal Press, Lathe, Silicon Refinery (with their moving layers)."""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from smalllib import *

CYAN = "color C 8ae4ff\ncolor c 4fa7c7\n"
COPPER = "color Q d99d73\ncolor q a8664c\n"

def cogs(g, cx, cy, r, teeth, c1, c2):
    fill(g, incircle(cx, cy, r), c2); fill(g, incircle(cx, cy, r - 1.2), c1)
    for k in range(teeth):
        a = k * 2 * math.pi / teeth
        fill(g, incircle(cx + math.cos(a) * (r + .6), cy + math.sin(a) * (r + .6), 1.3), c1)
    fill(g, incircle(cx, cy, r * .35), 'X')

# ---------------- Augmentation Hub: coupler core with four module slots; -connector: arm to the machine; -light: tinted by the module ----------------
g = new(); base2(g)
for (cx, cy) in ((15, 15), (49, 15), (15, 49), (49, 49)):                            # module slots
    part(g, inrect(cx - 9, cy - 9, cx + 9, cy + 9), cx, cy)
    fill(g, inrect(cx - 6, cy - 6, cx + 6, cy + 6), 'X'); fill(g, inrect(cx - 4, cy - 4, cx + 4, cy + 4), 'H')
part(g, inpoly(octagon(32, 32, 11)), 32, 32); fill(g, inpoly(octagon(32, 32, 7)), 'X')   # coupler core
fill(g, inpoly(diamond(32, 32, 4)), 'M')
write(g, "augmentation-hub", BLOCK2 + "ref overdrive-projector mender")
g = new()                                                                            # -connector: points at the augmented machine (drawn turned)
part(g, inrect(43, 28, 63, 36), 53, 32); fill(g, inrect(58, 30, 63, 34), 'X'); fill(g, inrect(60, 31, 63, 33), 'C')
write(g, "augmentation-hub-connector", BLOCK2 + CYAN + "on augmentation-hub")
g = new()                                                                            # -light: core and slot lamps
fill(g, inpoly(diamond(32, 32, 4)), 'W')
for (cx, cy) in ((15, 15), (49, 15), (15, 49), (49, 49)): fill(g, inrect(cx - 2, cy - 2, cx + 1, cy + 1), 'W')
write(g, "augmentation-hub-light", BLOCK2 + "color W ffffff\non augmentation-hub")

# ---------------- Gear Press: forming bed with a gear die, side supports, feed throat, output chute; -press: the ram ----------------
g = new(); base2(g)
for x0 in (5, 51):                                                                   # side supports
    part(g, inrect(x0, 8, x0 + 8, 56), x0 + 4, 32)
part(g, inrect(24, 3, 40, 10), 32, 6); fill(g, inrect(27, 5, 37, 9), 'X')          # feed throat
part(g, inrect(26, 54, 38, 61), 32, 57); fill(g, inrect(28, 56, 36, 61), 'X')       # output chute
part(g, inrect(15, 12, 49, 52), 32, 32); fill(g, inrect(18, 15, 46, 49), 'X')       # forming bed
diag(g, inrect(19, 16, 45, 48), 'H', 'X', 32, 32)
cogs(g, 32, 32, 9, 10, 'Q', 'q')                                                     # copper gear in the die
fill(g, inrect(47, 50, 49, 52), 'L')
write(g, "gear-press", BLOCK2 + COPPER + "color L fdb380\nmask glow L\nref multi-press pulverizer")
g = new()                                                                            # -press: ram plate on two cylinders
part(g, inrect(15, 29, 49, 35), 32, 32)                                             # a narrow ram beam: the gear shows above and below
fill(g, inrect(28, 30, 36, 34), 'X'); fill(g, inrect(30, 31, 34, 33), 'M')
for cx in (14, 50):
    part(g, incircle(cx, 32, 4.5), cx, 32); fill(g, incircle(cx, 32, 1.8), 'X')
write(g, "gear-press-press", BLOCK2 + "on gear-press")

# ---------------- Extruder: ram cylinder, die block, wire spool (turns); -ram slides ----------------
g = new(); base2(g)
part(g, inrect(5, 22, 28, 42), 16, 32); fill(g, inrect(8, 26, 26, 38), 'X')         # ram guide
part(g, inrect(27, 18, 37, 46), 32, 32); fill(g, incircle(32, 32, 3), 'X'); fill(g, incircle(32, 32, 1.5), 'Q')   # die block
fill(g, inrect(37, 31, 45, 32), 'Q')                                                 # extruded wire leaving the die
part(g, incircle(48, 32, 12), 48, 32); fill(g, incircle(48, 32, 10), 'X')           # spool housing
spool = small(20)
for y in range(20):
    for x in range(20):
        px, py = x + .5 - 10, y + .5 - 10; r, t = math.hypot(px, py), math.atan2(py, px)
        if r <= 9.5:
            if r > 3.5: spool[y][x] = 'Q' if int(r * 1.5) % 2 == 0 else 'q'
            elif r > 2: spool[y][x] = 'M' if (t % (math.pi * 2 / 3)) > .6 else 'X'
            else: spool[y][x] = 'X'
for y in range(20):
    for x in range(20):
        if 7 < math.hypot(x + .5 - 10, y + .5 - 10) <= 9.5 and abs(math.atan2(y + .5 - 10, x + .5 - 10) + math.pi / 2) < .3: spool[y][x] = 'W'
paste(g, spool, 48, 32)
fill(g, inrect(6, 54, 9, 57), 'L')
write(g, "extruder", BLOCK2 + COPPER + "color W f4e0c8\ncolor L fdb380\nmask glow L\nref plastanium-compressor pulverizer")
write_small(spool, "extruder-spool", "color Q d99d73\ncolor q a8664c\ncolor M 989aa4\ncolor X 4a4b53\ncolor W f4e0c8")
g = new()                                                                            # -ram: billet ram, pushes into the die
part(g, inrect(10, 27, 27, 37), 18, 32); fill(g, inrect(12, 30, 18, 34), 'M')
write(g, "extruder-ram", BLOCK2 + "on extruder")

# ---------------- Metal Press: heavy platen bed, plate stock out; -press: the platen ----------------
g = new(); base2(g)
for (cx, cy) in ((10, 10), (54, 10), (10, 54), (54, 54)):                            # guide columns
    part(g, incircle(cx, cy, 5), cx, cy); fill(g, incircle(cx, cy, 2), 'X')
part(g, inrect(14, 14, 50, 50), 32, 32); fill(g, inrect(17, 17, 47, 47), 'X')       # press bed with a plate on it
diag(g, inrect(21, 21, 43, 43), 'U', 'u', 32, 32)
part(g, inrect(52, 20, 61, 44), 56, 32)                                              # finished plate stack
for y in range(23, 42, 3): fill(g, inrect(54, y, 59, y + 1), 'U'); fill(g, inrect(54, y + 2, 59, y + 2), 'u')
fill(g, inrect(3, 30, 5, 33), 'L')
write(g, "metal-press", BLOCK2 + "color U c3d0e6\ncolor u 8da1b8\ncolor L fdb380\nmask glow L\nref multi-press plastanium-compressor")
g = new()                                                                            # -press: the platen, slams down at the end of each batch
part(g, inrect(15, 15, 49, 49), 32, 32)
groove(g, minus(inrect(19, 19, 45, 45), inrect(20, 20, 44, 44)))
for (cx, cy) in ((24, 24), (40, 24), (24, 40), (40, 40)):                            # hydraulic cylinder heads
    part(g, incircle(cx, cy, 4), cx, cy); fill(g, incircle(cx, cy, 1.5), 'X')
for (cx, cy) in ((10, 10), (54, 10), (10, 54), (54, 54)):
    part(g, both(incircle(cx, cy, 6), minus(inrect(0, 0, 63, 63), incircle(cx, cy, 3.5))), cx, cy)
write(g, "metal-press-press", BLOCK2 + "on metal-press")

# ---------------- Lathe: bed with headstock; -chuck turns; -carriage (tool post) slides along the work ----------------
g = new(); base2(g)
part(g, inrect(5, 46, 58, 58), 32, 52)                                               # lathe bed
fill(g, inrect(8, 50, 55, 51), 'X')
part(g, inrect(5, 8, 15, 42), 10, 25)                                                # headstock
part(g, inrect(50, 16, 59, 38), 55, 27); fill(g, incircle(54, 27, 1.8), 'X')       # tailstock
fill(g, inrect(30, 24, 50, 30), 'X')                                                 # the workpiece
diag(g, inrect(30, 25, 50, 29), 'U', 'u', 40, 27)
for x in range(33, 50, 4): fill(g, inrect(x, 25, x, 29), 'u')
chuck = small(20)
for y in range(20):
    for x in range(20):
        px, py = x + .5 - 10, y + .5 - 10; r, t = math.hypot(px, py), math.atan2(py, px)
        if r <= 9.5: chuck[y][x] = 'H' if r > 8.3 else 'G'                            # dark rim, mid face
        for k in range(3):                                                          # three light jaws
            a = k * 2 * math.pi / 3
            along, across = px * math.cos(a) + py * math.sin(a), -px * math.sin(a) + py * math.cos(a)
            if 2.5 < along <= 8 and abs(across) <= 1.6: chuck[y][x] = 'A' if across < 0 else 'M'
        if r <= 2.5: chuck[y][x] = 'X'
paste(g, chuck, 22, 27)
fill(g, inrect(7, 36, 9, 38), 'L')
write(g, "lathe", BLOCK2 + "color U c3d0e6\ncolor u 8da1b8\ncolor L fdb380\nmask glow L\nref pulverizer multi-press")
write_small(chuck, "lathe-chuck", "color A b0b9c0\ncolor M 989aa4\ncolor G 757887\ncolor H 5c5e6c\ncolor X 4a4b53")
g = new()                                                                            # -carriage: tool post riding the bed
part(g, inrect(34, 44, 46, 58), 40, 51)                                             # saddle on the bed
part(g, inrect(37, 31, 43, 44), 40, 37); fill(g, inrect(39, 30, 41, 32), 'K')       # tool post, cutter at the work
write(g, "lathe-carriage", BLOCK2 + "color K 343a4a\non lathe")

# ---------------- Silicon Refinery: sealed bell jar (acid shows), glowing silicon rods, H2 and HCl connections ----------------
g = new(); base2(g)
part(g, inrect(3, 26, 12, 38), 7, 32); fill(g, inrect(3, 29, 9, 35), 'C')           # hydrogen in
part(g, inrect(52, 26, 61, 38), 56, 32); fill(g, inrect(55, 29, 61, 35), 'Y')       # acid in
part(g, incircle(32, 32, 21), 32, 32)                                                # bell jar flange
groove(g, ring(17, 17.8))
window(g, incircle(32, 32, 16))
for x0 in (24, 36):                                                                  # U-shaped silicon rods
    fill(g, inrect(x0, 22, x0 + 1, 40), 'R'); fill(g, inrect(x0 + 3, 22, x0 + 4, 40), 'R'); fill(g, inrect(x0, 21, x0 + 4, 22), 'R')
    fill(g, inrect(x0 + 2, 23, x0 + 2, 40), '.')
    for x in (x0, x0 + 3): fill(g, inrect(x - 1, 41, x + 2, 43), 'M')
part(g, inrect(24, 52, 40, 60), 32, 56); fill(g, inrect(27, 55, 37, 58), 'X')       # output hatch
write(g, "silicon-refinery", BLOCK2 + COLD + "color Y f4f7c8\ncolor R c9ccd4\nmask rods R\nref silicon-smelter cryofluid-mixer")
