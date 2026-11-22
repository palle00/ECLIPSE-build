"""Unit drafts v2: bigger sprites, heavy bevelled armour plates. Shapes are given in full-canvas coordinates for the
LEFT half (x < w/2) plus the centre line; the right half is the plain mirror image. A panel gets a dark seam around it
(over whatever is below), a light rim on its top/outer edge, a dark rim on its bottom/inner edge and a mid face."""
import sys, os, math

OUT = sys.argv[1] if len(sys.argv) > 1 else "."

class Grid:
    def __init__(self, w, h):
        self.w, self.h, self.hw = w, h, w // 2
        self.g = [['.'] * w for _ in range(h)]
    def inside(self, test, x, y):
        if x < 0 or y < 0 or x >= self.w or y >= self.h: return False
        if x >= self.hw: x = self.w - 1 - x
        return test(x + .5, y + .5)
    def fill(self, test, ch, full=False):
        for y in range(self.h):
            for x in range(self.w if full else self.hw):
                if test(x + .5, y + .5): self.g[y][x] = ch
    def panel(self, test, rim=2, seam=1, light='L', mid='M', dark='D', seamch='K'):
        pts = [(x, y) for y in range(self.h) for x in range(self.hw) if test(x + .5, y + .5)]
        inside = set(pts)
        if seam:
            for (x, y) in pts:
                for dy in range(-seam, seam + 1):
                    for dx in range(-seam, seam + 1):
                        nx, ny = x + dx, y + dy
                        if 0 <= nx < self.hw and 0 <= ny < self.h and (nx, ny) not in inside and not self.inside(test, nx, ny):
                            self.g[ny][nx] = seamch
        for (x, y) in pts:
            up_out = not self.inside(test, x, y - rim) or not self.inside(test, x - rim, y)
            dn_out = not self.inside(test, x, y + rim) or not self.inside(test, x + rim, y)
            self.g[y][x] = light if up_out and not dn_out else dark if dn_out else mid
    def mirror(self):
        for y in range(self.h):
            for x in range(self.hw):
                self.g[y][self.w - 1 - x] = self.g[y][x]
    def write(self, name, header):
        import re
        header = re.sub(r"(?m)^size \d+ \d+$", "size %d %d" % (self.w, self.h), header)
        with open(os.path.join(OUT, name + ".txt"), "w", newline="\n") as f:
            f.write(header.strip() + "\ngrid\n")
            for row in self.g: f.write("".join(row) + "\n")
        print("drafted", name)

# Optional re-render at another scale: (s, w0, h0, w, h) maps the new canvas back onto the original design,
# anchored at the centre (weapon mounts are measured from the unit's centre), so shapes stay crisp - nothing is resampled.
XF = None
def scaled(s, w0, h0):
    global XF
    w = int(round(w0 * s / 2)) * 2
    h = int(round(h0 * s))
    XF = (s, w0, h0, w, h)
    return w, h
def unscaled():
    global XF
    XF = None
def _xf(px, py):
    if XF is None: return px, py
    s, w0, h0, w, h = XF
    return (px - w / 2) / s + w0 / 2, (py - h / 2) / s + h0 / 2

def _poly(pts):
    def t(px, py):
        inside = False; j = len(pts) - 1
        for i in range(len(pts)):
            xi, yi = pts[i]; xj, yj = pts[j]
            if (yi > py) != (yj > py) and px < (xj - xi) * (py - yi) / (yj - yi) + xi: inside = not inside
            j = i
        return inside
    return t
def _rect(x0, y0, x1, y1): return lambda px, py: x0 <= px < x1 + 1 and y0 <= py < y1 + 1
def _circ(cx, cy, r): return lambda px, py: (px - cx) ** 2 + (py - cy) ** 2 <= r * r
def _ring(cx, cy, r0, r1): return lambda px, py: r0 * r0 < (px - cx) ** 2 + (py - cy) ** 2 <= r1 * r1
def _wrap(t): return lambda px, py: t(*_xf(px, py))
def poly(pts): return _wrap(_poly(pts))
def rect(x0, y0, x1, y1): return _wrap(_rect(x0, y0, x1, y1))
def circ(cx, cy, r): return _wrap(_circ(cx, cy, r))
def ring(cx, cy, r0, r1): return _wrap(_ring(cx, cy, r0, r1))
def both(a, b): return lambda px, py: a(px, py) and b(px, py)
def minus(a, b): return lambda px, py: a(px, py) and not b(px, py)

COLORS = """
color L b0b9c0
color M 989aa4
color D 6e7080
color K 565666
"""
UNIT = COLORS + """
color T ffa664
color U d06b53
mask cell T:ffffff U:dcc6c6
team ffd37f
"""
GLOW = """
color O ff7a3d
color P ffb27a
"""


def _facet(self, test, rim=None, seam=1, band=1.5, cx=None, cy=None, light='L', mid='M', dark='D', seamch='K'):
    """Vanilla unit plate: split along a diagonal ridge through its centre - light facet top-left (outer), dark facet
    bottom-right (inner), a narrow mid band on the ridge, and a dark seam around it."""
    pts = [(x, y) for y in range(self.h) for x in range(self.hw) if test(x + .5, y + .5)]
    if not pts: return
    inside = set(pts)
    if cx is None: cx = sum(p[0] for p in pts) / len(pts) + .5
    if cy is None: cy = sum(p[1] for p in pts) / len(pts) + .5
    if seam:
        for (x, y) in pts:
            for dy in range(-seam, seam + 1):
                for dx in range(-seam, seam + 1):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < self.hw and 0 <= ny < self.h and (nx, ny) not in inside and not self.inside(test, nx, ny):
                        self.g[ny][nx] = seamch
    for (x, y) in pts:
        s = (cx - x - .5) + (cy - y - .5)
        self.g[y][x] = light if s > band else dark if s < -band else mid
Grid.panel = _facet
