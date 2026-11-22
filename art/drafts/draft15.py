"""Air Separation Unit, Vacuum Freezer, Acid Leaching Vat, Gas Separator, Alumina Refinery (with their moving layers)."""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from blocklib import *

ORANGE = "color O d96b68\ncolor P fdb380\n"
COLD = "color C bfe6f2\ncolor c 7fb8cc\n"

def ring(r0, r1, cx=48, cy=48): return lambda px, py: r0 <= math.hypot(px - cx, py - cy) <= r1
def window(g, test): fill(g, test, '.')
def small(n): return [['.'] * n for _ in range(n)]
def write_small(s, name, header):
    with open(os.path.join(OUT, name + ".txt"), "w", newline="\n") as f:
        f.write(f"size {len(s)} {len(s)}\noutline 0\n" + header.strip() + "\ngrid\n")
        for row in s: f.write("".join(row) + "\n")
    print("drafted", name)
def paste(g, s, cx, cy):
    """A spinning part at rest in the main sprite (build-menu icon); the game draws the turning one over it."""
    n = len(s)
    for y in range(n):
        for x in range(n):
            if s[y][x] != '.': g[cy - n // 2 + y][cx - n // 2 + x] = s[y][x]
def fan(n, blades, r0, r1, width=.5):
    s = small(n); c = n / 2
    for y in range(n):
        for x in range(n):
            px, py = x + .5 - c, y + .5 - c
            r, t = math.hypot(px, py), math.atan2(py, px)
            if r <= r0: s[y][x] = 'M' if r > r0 - 1.2 else 'X'
            elif r <= r1:
                k = (t % (2 * math.pi / blades)) / (2 * math.pi / blades)
                if k < width: s[y][x] = 'A' if k < width / 2 else 'B'
    return s
def gauge(g, cx, cy):
    part(g, incircle(cx, cy, 9), cx, cy)
    fill(g, incircle(cx, cy, 6.5), 'X'); fill(g, incircle(cx, cy, 5.5), 'Z')
    for k in range(5):
        a = math.pi * (1.25 - k * .375)
        x, y = int(cx + 4.5 * math.cos(a)), int(cy - 4.5 * math.sin(a)); g[y][x] = 'H'
def needle(cx, cy):
    g = new()
    for t in range(5): g[cy - t][cx] = 'K'
    fill(g, incircle(cx, cy, 1.4), 'X')
    return g
FANHDR = "color A b0b9c0\ncolor B 6e7080\ncolor M 989aa4\ncolor X 4a4b53"

# ---------------- Air Separation Unit: intake fan, insulated cold box round the column, three product lines ----------------
g = new(); base(g)
for (x0, y0, x1, y1) in ((46, 3, 58, 17), (46, 79, 58, 93), (79, 42, 93, 54)):      # nitrogen up, oxygen down, argon right
    part(g, inrect(x0, y0, x1, y1), (x0 + x1) / 2, (y0 + y1) / 2)
fill(g, inrect(49, 6, 55, 9), 'C'); fill(g, inrect(49, 87, 55, 90), 'c'); fill(g, inrect(87, 45, 90, 51), 'Z')
part(g, incircle(16, 48, 12), 16, 48)                                                # air intake (fan spins in it)
fill(g, incircle(16, 48, 9.5), 'X')
part(g, inrect(26, 44, 32, 52), 29, 48)                                              # intake duct into the cold box
part(g, inrect(30, 18, 76, 78), 53, 48)                                              # insulated cold box
groove(g, minus(inrect(33, 21, 73, 75), inrect(34, 22, 72, 74)), 53, 48)
for y in range(25, 73, 6):                                                           # insulation quilting
    groove(g, inrect(36, y, 40, y)); groove(g, inrect(66, y, 70, y))
window(g, ring(13.5, 18, 53, 48))                                                    # cryofluid bath round the column
part(g, incircle(53, 48, 13), 53, 48)                                                # distillation column
diag(g, ring(9, 11, 53, 48), 'C', 'c', 53, 48)                                       # frosted rim
fill(g, incircle(53, 48, 8), 'X'); diag(g, incircle(53, 48, 6.5), 'C', 'c', 53, 48)
fill(g, inrect(64, 26, 67, 29), 'X'); fill(g, inrect(65, 27, 66, 28), 'L')
f = fan(20, 6, 3, 9, .45); paste(g, f, 16, 48)
write(g, "air-separation-unit", BLOCK + COLD + "color Z e8e8e0\ncolor L bfe6f2\nmask glow L C\nref cryofluid-mixer separator")
write_small(f, "air-separation-unit-fan", FANHDR)

# ---------------- Vacuum Freezer: cryo coil round a sealed chamber; -press: the chamber hatch; -pump: vacuum pump rotor ----------------
g = new(); base(g)
part(g, inrect(16, 16, 80, 80), 48, 48)                                              # coil frame
window(g, minus(inrect(20, 20, 76, 76), inrect(28, 28, 68, 68)))                     # cryofluid coil channel (shows the cryofluid)
for (x0, y0, x1, y1) in ((20, 46, 27, 49), (69, 46, 76, 49), (46, 20, 49, 27), (46, 69, 49, 76)):
    part(g, inrect(x0, y0, x1, y1), (x0 + x1) / 2, (y0 + y1) / 2)                   # chamber straps
part(g, inrect(28, 28, 68, 68), 48, 48)                                              # the vacuum chamber
fill(g, inrect(31, 31, 65, 65), 'X')
for (x0, y0, x1, y1) in ((4, 4, 14, 14), (82, 4, 92, 14)):                           # cryofluid in/out
    part(g, inrect(x0, y0, x1, y1), (x0 + x1) / 2, (y0 + y1) / 2); fill(g, inrect(x0 + 3, y0 + 3, x1 - 3, y1 - 3), 'C')
part(g, incircle(84, 84, 9), 84, 84); fill(g, incircle(84, 84, 4), 'X'); fill(g, incircle(84, 84, 2), 'L')   # frost indicator
part(g, incircle(12, 84, 10), 12, 84); fill(g, incircle(12, 84, 7.5), 'X')         # vacuum pump
p = fan(16, 4, 2.5, 7, .5); paste(g, p, 12, 84)
write(g, "vacuum-freezer", BLOCK + COLD + "color L bfe6f2\nmask glow L\nref cryofluid-mixer plastanium-compressor")
write_small(p, "vacuum-freezer-pump", FANHDR)
g = new()                                                                            # the hatch, slams shut at the end of each batch
part(g, inrect(32, 32, 64, 64), 48, 48)
groove(g, minus(inrect(36, 36, 60, 60), inrect(37, 37, 59, 59)))
for (x0, y0, x1, y1) in ((34, 46, 62, 49), (46, 34, 49, 62)):
    part(g, inrect(x0, y0, x1, y1), 48, 48)                                         # locking bars
part(g, incircle(48, 48, 5), 48, 48); fill(g, incircle(48, 48, 2), 'C')
write(g, "vacuum-freezer-press", BLOCK + COLD + "on vacuum-freezer")

# ---------------- Acid Leaching Vat: banded rubber-lined vat (acid shows), agitator, scrap hopper, metal bins, waste pump ----------------
g = new(); base(g)
part(g, inrect(5, 5, 27, 27), 16, 16)                                                # scrap feed hopper
fill(g, inrect(9, 9, 23, 23), 'X')
for (x, y, c) in ((11, 12, 'A'), (16, 10, 'B'), (19, 15, 'M'), (12, 18, 'G'), (17, 19, 'A'), (14, 14, 'H')):
    fill(g, inrect(x, y, x + 2, y + 1), c)
for i, (col, dk) in enumerate((('U', 'u'), ('Q', 'q'), ('V', 'v'))):                # titanium, copper, lead output bins
    y0 = 58 + i * 11
    part(g, inrect(80, y0, 92, y0 + 9), 86, y0 + 4.5); diag(g, inrect(83, y0 + 2, 89, y0 + 7), col, dk, 86, y0 + 4.5)
part(g, incircle(14, 82, 9), 14, 82); fill(g, incircle(14, 82, 4.5), 'X'); fill(g, incircle(14, 82, 2), 'W')   # acidic waste pump
part(g, incircle(48, 48, 31), 48, 48)                                                # the vat
diag(g, ring(22, 26), 'T', 't')                                                      # rubber lining
for k in range(4):                                                                   # steel bands round the vat
    a = k * math.pi / 2 + math.pi / 4
    for r in range(26, 31):
        for w in (-1, 0, 1):
            x = int(48 + r * math.cos(a) - w * math.sin(a)); y = int(48 + r * math.sin(a) + w * math.cos(a))
            g[y][x] = 'H'
window(g, incircle(48, 48, 21.5))
write(g, "acid-leaching-vat", BLOCK + "color T 5d6b62\ncolor t 3f4a43\ncolor U c3d0e6\ncolor u 8da1b8\ncolor Q d99d73\ncolor q a8664c\ncolor V 9a9cc8\ncolor v 6d6f9a\ncolor W c8d06a\nmask glow W\nref separator disassembler")
g = new()                                                                            # -rotator: agitator paddles
for k in range(4):
    a = k * math.pi / 2 + .2
    ca, sa = math.cos(a), math.sin(a)
    def pt(r, s): return (48 + ca * r - sa * s, 48 + sa * r + ca * s)
    diag(g, inpoly([pt(4, -2), pt(17, -3), pt(17, 3), pt(4, 2)]), 'A', 'B', *pt(10, 0), 'M')
part(g, incircle(48, 48, 5), 48, 48); fill(g, incircle(48, 48, 2), 'X')
write(g, "acid-leaching-vat-rotator", BLOCK + "on acid-leaching-vat")

# ---------------- Gas Separator: flue gas compressor, hollow-fibre membrane bank, nitrogen manifold up, CO2 manifold down ----------------
g = new(); base(g)
part(g, incircle(14, 48, 11), 14, 48); fill(g, incircle(14, 48, 8.5), 'X')         # flue gas compressor
part(g, inrect(24, 44, 30, 52), 27, 48)
part(g, inrect(28, 4, 84, 18), 56, 11); fill(g, inrect(31, 8, 81, 14), 'X'); diag(g, inrect(32, 9, 80, 13), 'C', 'c', 56, 11)    # nitrogen manifold
part(g, inrect(28, 78, 84, 92), 56, 85); fill(g, inrect(31, 82, 81, 88), 'X'); diag(g, inrect(32, 83, 80, 87), 'K', 'k', 56, 85)  # CO2 manifold
part(g, inrect(30, 21, 82, 75), 56, 48)                                              # membrane housing
window(g, inrect(34, 25, 78, 71))                                                    # flue gas shows between the fibres
for x in range(36, 78, 4):                                                           # hollow-fibre membrane modules
    diag(g, inrect(x, 25, x + 1, 71), 'M', 'G', x + .5, 48)
for y in (36, 60):                                                                   # support straps
    part(g, inrect(34, y, 78, y + 2), 56, y + 1)
gauge(g, 14, 16)
fill(g, inrect(10, 78, 18, 84), 'X'); fill(g, inrect(11, 79, 17, 83), 'L')
f = fan(18, 5, 2.5, 8, .5); paste(g, f, 14, 48)
write(g, "gas-separator", BLOCK + COLD + "color K 7a7c84\ncolor k 55575f\ncolor Z dfe3e8\ncolor L bfe6f2\nmask glow L\nref separator cryofluid-mixer")
write_small(f, "gas-separator-fan", FANHDR)
write(needle(14, 16), "gas-separator-needle", BLOCK + "color K c23b3b\non gas-separator")

# ---------------- Alumina Refinery: heated digester with agitator, bauxite in, alumina out, two red-mud thickeners with rakes ----------------
g = new(); base(g)
part(g, inrect(5, 5, 25, 25), 15, 15); fill(g, inrect(9, 9, 21, 21), 'X')           # bauxite hopper
for (x, y) in ((11, 11), (15, 13), (18, 10), (12, 16), (17, 17), (14, 19)):
    fill(g, inrect(x, y, x + 2, y + 1), 'R'); fill(g, inrect(x + 1, y + 1, x + 2, y + 1), 'r')
part(g, inrect(71, 5, 91, 25), 81, 15); fill(g, inrect(75, 9, 87, 21), 'X')         # alumina out
for (x, y) in ((77, 11), (81, 13), (84, 10), (78, 16), (83, 17), (80, 19)):
    fill(g, inrect(x, y, x + 2, y + 1), 'W'); fill(g, inrect(x + 1, y + 1, x + 2, y + 1), 'w')
for cx in (16, 80):                                                                  # thickeners: red mud settles, rakes turn
    part(g, incircle(cx, 80, 13), cx, 80)
    diag(g, incircle(cx, 80, 10.5), 'R', 'r', cx, 80)
part(g, incircle(48, 48, 25), 48, 48)                                                # the digester
diag(g, ring(19, 22), 'O', 'H')                                                      # heating jacket (warm)
fill(g, ring(18, 19), 'X')
window(g, incircle(48, 48, 17.5))
rake = small(22)
for y in range(22):
    for x in range(22):
        px, py = x + .5 - 11, y + .5 - 11
        if abs(py) <= 1.2 and abs(px) <= 9.5: rake[y][x] = 'A' if py < 0 else 'B'
        if abs(px) <= 1.2 and abs(py) <= 9.5: rake[y][x] = 'A' if px > 0 else 'B'
        if math.hypot(px, py) <= 2.5: rake[y][x] = 'M'
for cx in (16, 80): paste(g, rake, cx, 80)
write(g, "alumina-refinery", BLOCK + ORANGE + "color R 9a5545\ncolor r 6e3b31\ncolor W e8ecf0\ncolor w b9c0c8\nmask glow O\nref separator silicon-crucible")
write_small(rake, "alumina-refinery-rake", "color A b0b9c0\ncolor B 6e7080\ncolor M 989aa4")
g = new()                                                                            # -rotator: digester agitator
for k in range(3):
    a = k * 2 * math.pi / 3
    ca, sa = math.cos(a), math.sin(a)
    def pt(r, s): return (48 + ca * r - sa * s, 48 + sa * r + ca * s)
    diag(g, inpoly([pt(3, -2), pt(14, -3.5), pt(14, 3.5), pt(3, 2)]), 'A', 'B', *pt(8, 0), 'M')
part(g, incircle(48, 48, 4.5), 48, 48); fill(g, incircle(48, 48, 1.8), 'X')
write(g, "alumina-refinery-rotator", BLOCK + "on alumina-refinery")
