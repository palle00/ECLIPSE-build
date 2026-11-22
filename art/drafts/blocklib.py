"""3x3 support blocks in the vanilla block style: diagonal light/dark split (light top-right), bold shapes, few tones."""
import sys, os, math
OUT = sys.argv[1] if len(sys.argv) > 1 else "."
W = H = 96

def new(): return [['.'] * W for _ in range(H)]
def write(g, name, header):
    with open(os.path.join(OUT, name + ".txt"), "w", newline="\n") as f:
        f.write(header.strip() + "\ngrid\n")
        for row in g: f.write("".join(row) + "\n")
    print("drafted", name)

def inpoly(pts):
    def t(px, py):
        inside = False; j = len(pts) - 1
        for i in range(len(pts)):
            xi, yi = pts[i]; xj, yj = pts[j]
            if (yi > py) != (yj > py) and px < (xj - xi) * (py - yi) / (yj - yi) + xi: inside = not inside
            j = i
        return inside
    return t
def incircle(cx, cy, r): return lambda px, py: (px - cx) ** 2 + (py - cy) ** 2 <= r * r
def inrect(x0, y0, x1, y1): return lambda px, py: x0 <= px < x1 + 1 and y0 <= py < y1 + 1
def both(a, b): return lambda px, py: a(px, py) and b(px, py)
def minus(a, b): return lambda px, py: a(px, py) and not b(px, py)
def octagon(cx, cy, r, c=None):
    c = c if c is not None else r * .42
    return [(cx - r + c, cy - r), (cx + r - c, cy - r), (cx + r, cy - r + c), (cx + r, cy + r - c), (cx + r - c, cy + r), (cx - r + c, cy + r), (cx - r, cy + r - c), (cx - r, cy - r + c)]
def diamond(cx, cy, r): return [(cx, cy - r), (cx + r, cy), (cx, cy + r), (cx - r, cy)]

def diag(g, test, light, dark, cx=48, cy=48, mid=None, band=1.2):
    """Block shading: top-right of the part's diagonal is light, bottom-left dark, optional mid band."""
    for y in range(H):
        for x in range(W):
            if not test(x + .5, y + .5): continue
            d = (x + .5 - cx) - (y + .5 - cy)
            g[y][x] = mid if (mid and abs(d) < band) else (light if d > 0 else dark)
def fill(g, test, ch):
    for y in range(H):
        for x in range(W):
            if test(x + .5, y + .5): g[y][x] = ch
def part(g, test, cx, cy, mid='M'):
    """A raised part: light top-right, dark bottom-left, mid on the ridge, and a 1px groove around it."""
    ring = lambda px, py: not test(px, py) and any(test(px + dx, py + dy) for dx in (-1, 0, 1) for dy in (-1, 0, 1))
    diag(g, ring, 'G', 'H', 48, 48)
    diag(g, test, 'A', 'B', cx, cy, mid)
def groove(g, test, cx=48, cy=48):
    diag(g, test, 'G', 'H', cx, cy)

BLOCK = """
size 96 96
outline 0
color A b0b9c0
color B 6e7080
color M 989aa4
color G 757887
color H 5c5e6c
color X 4a4b53
"""

def base(g):
    """The 3x3 body: diagonal split, a groove inset frame, chamfered corners."""
    body = inpoly([(4, 0), (92, 0), (96, 4), (96, 92), (92, 96), (4, 96), (0, 92), (0, 4)])
    diag(g, body, 'A', 'B', 48, 48, 'M')
    groove(g, minus(inrect(5, 5, 90, 90), inrect(7, 7, 88, 88)))

