"""2x2 (64 px) block helpers: blocklib switched to a 64 px canvas, plus the part helpers the 3x3 drafts use."""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import blocklib
blocklib.W = blocklib.H = 64
from blocklib import new, write, inpoly, incircle, inrect, both, minus, octagon, diamond, diag, fill, part, groove, OUT
S = 64
C = 32

BLOCK2 = """
size 64 64
outline 0
color A b0b9c0
color B 6e7080
color M 989aa4
color G 757887
color H 5c5e6c
color X 4a4b53
"""
ORANGE = "color O d96b68\ncolor P fdb380\n"
COLD = "color C bfe6f2\ncolor c 7fb8cc\n"
FANHDR = "color A b0b9c0\ncolor B 6e7080\ncolor M 989aa4\ncolor X 4a4b53"

def base2(g):
    """The 2x2 body: diagonal split, a groove inset frame, chamfered corners."""
    body = inpoly([(3, 0), (61, 0), (64, 3), (64, 61), (61, 64), (3, 64), (0, 61), (0, 3)])
    diag(g, body, 'A', 'B', C, C, 'M')
    groove(g, minus(inrect(4, 4, 59, 59), inrect(5, 5, 58, 58)))

def ring(r0, r1, cx=C, cy=C): return lambda px, py: r0 <= math.hypot(px - cx, py - cy) <= r1
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
def gauge(g, cx, cy, r=7):
    part(g, incircle(cx, cy, r), cx, cy)
    fill(g, incircle(cx, cy, r - 2.2), 'X'); fill(g, incircle(cx, cy, r - 3), 'Z')
def needle(cx, cy, n=3):
    g = new()
    for t in range(n + 1): g[cy - t][cx] = 'K'
    fill(g, incircle(cx, cy, 1), 'X')
    return g
def world(px, py):
    """Pixel position on a 2x2 sprite -> world offset from the block centre (x right, y up)."""
    return ((px - C) / 4, -(py - C) / 4)
