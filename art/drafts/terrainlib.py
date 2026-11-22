"""Helpers for terrain sprites (floors, cliff walls, trees, boulders) in the vanilla environment style: flat colour areas
with smooth blob edges (one in-between tone where two colours meet, as vanilla floors have), no outlines.

Shapes are painted in order onto a supersampled canvas; floors and walls wrap around their edges (a shape that leaves the
tile on one side comes back on the other) so neighbouring tiles join without seams. write() turns the result into a
PixelArt letter grid."""
import math, random, os, zlib

POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!$%&*+-/:;<=>?@^_~"


def hexrgb(h): return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))
def rgbhex(c): return "".join(f"{max(0, min(255, round(v))):02x}" for v in c)
def mix(a, b, t=.5):
    x, y = hexrgb(a), hexrgb(b)
    return rgbhex(tuple(x[i] + (y[i] - x[i]) * t for i in range(3)))


# ---- shapes: (test(px, py) -> bool, bbox (x0, y0, x1, y1)) in pixel units, pixel centres at +0.5 ----
def ellipse(cx, cy, rx, ry, deg=0.):
    a = math.radians(deg); c, s = math.cos(a), math.sin(a)
    def t(px, py):
        dx, dy = px - cx, py - cy
        u, v = dx * c + dy * s, -dx * s + dy * c
        return (u / rx) ** 2 + (v / ry) ** 2 <= 1
    r = max(rx, ry)
    return t, (cx - r, cy - r, cx + r, cy + r)

def circle(cx, cy, r): return ellipse(cx, cy, r, r)

def ring(cx, cy, r0, r1):
    return (lambda px, py: r0 * r0 <= (px - cx) ** 2 + (py - cy) ** 2 <= r1 * r1), (cx - r1, cy - r1, cx + r1, cy + r1)

def blob(cx, cy, r, rnd, wobble=.25, lobes=3, sy=1., deg=0.):
    """A rounded lump: radius r wobbling by `wobble` with a few lobes; squashed by sy and turned by deg."""
    ph = [rnd.uniform(0, 6.3) for _ in range(3)]
    a = math.radians(deg); c, s = math.cos(a), math.sin(a)
    def t(px, py):
        dx, dy = px - cx, py - cy
        u, v = dx * c + dy * s, (-dx * s + dy * c) / sy
        th = math.atan2(v, u)
        rr = r * (1 + wobble * (.6 * math.sin(lobes * th + ph[0]) + .4 * math.sin((lobes + 2) * th + ph[1])))
        return u * u + v * v <= rr * rr
    R = r * (1 + wobble) * max(1, sy)
    return t, (cx - R, cy - R, cx + R, cy + R)

def capsule(x0, y0, x1, y1, w):
    dx, dy = x1 - x0, y1 - y0; L2 = dx * dx + dy * dy or 1e-9
    def t(px, py):
        k = max(0, min(1, ((px - x0) * dx + (py - y0) * dy) / L2))
        qx, qy = x0 + k * dx - px, y0 + k * dy - py
        return qx * qx + qy * qy <= w * w
    return t, (min(x0, x1) - w, min(y0, y1) - w, max(x0, x1) + w, max(y0, y1) + w)

def poly(pts):
    def t(px, py):
        inside = False; j = len(pts) - 1
        for i in range(len(pts)):
            xi, yi = pts[i]; xj, yj = pts[j]
            if (yi > py) != (yj > py) and px < (xj - xi) * (py - yi) / (yj - yi) + xi: inside = not inside
            j = i
        return inside
    xs, ys = [p[0] for p in pts], [p[1] for p in pts]
    return t, (min(xs), min(ys), max(xs), max(ys))

def path(pts, w):
    """A stroke along a polyline (capsules joined)."""
    caps = [capsule(pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1], w) for i in range(len(pts) - 1)]
    return union(*caps)

def wavy(x0, y0, x1, y1, w, amp, waves, phase=0., steps=12):
    """A wavy stroke from (x0, y0) to (x1, y1)."""
    dx, dy = x1 - x0, y1 - y0; L = math.hypot(dx, dy) or 1; nx, ny = -dy / L, dx / L
    pts = []
    for i in range(steps + 1):
        k = i / steps; o = amp * math.sin(k * waves * 2 * math.pi + phase)
        pts.append((x0 + dx * k + nx * o, y0 + dy * k + ny * o))
    return path(pts, w)

def walk(x, y, rnd, steps, step, w, turn=.6, deg=None, lo=None, hi=None):
    """A wandering stroke (veins, cracks); with lo/hi it turns back before leaving that square."""
    a = math.radians(deg) if deg is not None else rnd.uniform(0, 2 * math.pi)
    pts = [(x, y)]
    for _ in range(steps):
        a += rnd.uniform(-turn, turn)
        nx, ny = x + math.cos(a) * step, y + math.sin(a) * step
        if lo is not None and not (lo <= nx <= hi and lo <= ny <= hi):
            a = math.atan2((lo + hi) / 2 - y, (lo + hi) / 2 - x) + rnd.uniform(-.5, .5)
            nx, ny = x + math.cos(a) * step, y + math.sin(a) * step
        x, y = nx, ny; pts.append((x, y))
    return path(pts, w), pts

def union(*shapes):
    ts = [s[0] for s in shapes]
    bb = (min(s[1][0] for s in shapes), min(s[1][1] for s in shapes), max(s[1][2] for s in shapes), max(s[1][3] for s in shapes))
    return (lambda px, py: any(t(px, py) for t in ts)), bb

def clip(a, b):
    """Shape a, only where b is too."""
    return (lambda px, py: a[0](px, py) and b[0](px, py)), a[1]

def minus(a, b):
    return (lambda px, py: a[0](px, py) and not b[0](px, py)), a[1]

def shifted(s, dx, dy):
    return (lambda px, py: s[0](px - dx, py - dy)), (s[1][0] + dx, s[1][1] + dy, s[1][2] + dx, s[1][3] + dy)


class Canvas:
    """A w x h sprite, supersampled S x S. Colour 0 is the background (transparent unless base is given)."""

    def __init__(self, w, h, base=None, wrap=True, S=4, var=0):
        self.w, self.h, self.S, self.wrap, self.var = w, h, S, wrap, var
        self.cols = [base]  # index -> hex (None = transparent)
        self.sub = [[0] * (w * S) for _ in range(h * S)]

    def col(self, h):
        if h not in self.cols: self.cols.append(h)
        return self.cols.index(h)

    def paint(self, shape, color, where=None):
        """Fills shape with color; `where(old_index)` limits it to pixels that already hold given colours."""
        t, (x0, y0, x1, y1) = shape
        S, W, H = self.S, self.w * self.S, self.h * self.S
        ci = self.col(color) if color is not None else 0
        sx0, sy0, sx1, sy1 = math.floor(x0 * S) - 1, math.floor(y0 * S) - 1, math.ceil(x1 * S) + 1, math.ceil(y1 * S) + 1
        if not self.wrap: sx0, sy0, sx1, sy1 = max(0, sx0), max(0, sy0), min(W - 1, sx1), min(H - 1, sy1)
        for sy in range(sy0, sy1 + 1):
            py = (sy + .5) / S
            row = self.sub[sy % H]
            for sx in range(sx0, sx1 + 1):
                if t((sx + .5) / S, py):
                    k = sx % W
                    if where is None or where(row[k]): row[k] = ci

    def on(self, *hexes):
        """A `where` filter: only over these colours."""
        idx = {self.col(h) for h in hexes}
        return lambda i: i in idx

    def pixels(self, aa=.7):
        """Resolves each pixel: its main colour, or (where two colours share it about evenly) their mix."""
        S, out = self.S, []
        for y in range(self.h):
            row = []
            for x in range(self.w):
                cnt = {}
                for sy in range(y * S, y * S + S):
                    r = self.sub[sy]
                    for sx in range(x * S, x * S + S): cnt[r[sx]] = cnt.get(r[sx], 0) + 1
                top = sorted(cnt.items(), key=lambda kv: -kv[1])
                a = top[0]
                if a[1] >= aa * S * S or len(top) == 1: row.append(self.cols[a[0]])
                else:
                    b = top[1]
                    ca, cb = self.cols[a[0]], self.cols[b[0]]
                    if ca is None or cb is None: row.append(ca if a[1] * 2 >= S * S else cb)
                    else: row.append(mix(ca, cb))  # one in-between tone per colour pair, as vanilla floors have
            out.append(row)
        return out


def quantize(px, step=6):
    """Snaps in-between tones to a coarse grid so a sprite needs few letters (vanilla floors use a handful)."""
    q = lambda h: rgbhex(tuple(round(v / step) * step for v in hexrgb(h)))
    return [[None if c is None else q(c) for c in row] for row in px]


def write(out_dir, name, px, refs=()):
    letters, cols = {}, []
    pool = iter(POOL)
    h, w = len(px), len(px[0])
    rows = []
    for row in px:
        s = ""
        for c in row:
            if c is None: s += "."; continue
            if c not in letters:
                letters[c] = next(pool); cols.append(c)
            s += letters[c]
        rows.append(s)
    with open(os.path.join(out_dir, name + ".txt"), "w", newline="\n") as f:
        f.write(f"size {w} {h}\noutline 0\n")
        if refs: f.write("ref " + " ".join(refs) + "\n")
        for c in cols: f.write(f"color {letters[c]} {c}\n")
        f.write("grid\n" + "\n".join(rows) + "\n")


def stable(*parts):
    """A seed that is the same on every run (Python's hash() of strings is not)."""
    return zlib.crc32("|".join(map(str, parts)).encode())


def seeded(name, v):
    return random.Random(sum(ord(ch) * (i + 1) for i, ch in enumerate(name)) * 31 + v)


def inside(rnd, r, n=32, pad=1.):
    """A random centre for a shape of radius r that keeps it inside the tile (vanilla floors never cross the edge)."""
    return rnd.uniform(r + pad, n - r - pad), rnd.uniform(r + pad, n - r - pad)


def scatter(cv, name, key, per32, m, draw):
    """Scatters per32 shapes per 32x32 over a wall tile so that tiles join without seams: shapes near the edge (centre
    within m of it) come from a seed shared by every variant and size, repeating every 32 px, so any two wall tiles
    (and the 64x64 large one) show the same shapes at their common edge; shapes further in vary per variant. draw(cv, x, y,
    rnd) must stay within m - 0.5 of (x, y)."""
    n = cv.w
    sh = random.Random(stable(name, key, "shared"))
    for i in range(per32):
        x, y, s = sh.uniform(0, 32), sh.uniform(0, 32), sh.randrange(1 << 30)
        for ox in range(0, n, 32):
            for oy in range(0, n, 32):
                X, Y = x + ox, y + oy
                if min(X, n - X, Y, n - Y) < m: draw(cv, X, Y, random.Random(s))
    vr = random.Random(stable(name, key, cv.var, n))
    k = per32 * ((n - 2 * m) / 32) ** 2
    for i in range(int(k) + (vr.random() < k % 1)):
        draw(cv, vr.uniform(m, n - m), vr.uniform(m, n - m), random.Random(vr.randrange(1 << 30)))


def voronoi(cv, name, per32, shade, sx=1., sy=1., gap=None, rim=1.2, m=7):
    """Plates (basalt columns, carapace) as Voronoi cells. Cells along the tile edge come from points shared by every
    variant (as in scatter), and the variant's own points count less near the edge, so walls join without seams.
    shade(cell, side) gives the colour: side 'l' near the cell's top-left border, 'd' near its bottom-right, 'm' inside."""
    n, S = cv.w, cv.S
    sh = random.Random(stable(name, "plates"))
    pts = []
    for i in range(per32):
        x, y = sh.uniform(0, 32), sh.uniform(0, 32)
        for ox in range(0, n, 32):
            for oy in range(0, n, 32):
                X, Y = x + ox, y + oy
                if min(X, n - X, Y, n - Y) < m: pts.append((X, Y, False, len(pts)))
    vr = random.Random(stable(name, "plates", cv.var, n))
    k = per32 * ((n - 2 * m) / 32) ** 2
    for i in range(int(k) + (vr.random() < k % 1)): pts.append((vr.uniform(m, n - m), vr.uniform(m, n - m), True, len(pts)))
    for syy in range(cv.h * S):
        py = (syy + .5) / S
        for sxx in range(cv.w * S):
            px = (sxx + .5) / S
            e = min(px, n - px, py, n - py)
            weight = 1 + 4 * max(0., 1 - e / m)
            best = []
            for (x, y, own, idx) in pts:
                dx = (px - x + n / 2) % n - n / 2; dy = (py - y + n / 2) % n - n / 2
                d = math.sqrt((dx * sx) ** 2 + (dy * sy) ** 2) * (weight if own else 1)
                best.append((d, idx, dx, dy))
            best.sort()
            d0, i0, dx, dy = best[0]; d1 = best[1][0]
            edge = d1 - d0
            if gap and edge < .7: c = gap
            elif edge < rim + .7: c = shade(i0, 'l' if dx + dy < 0 else 'd')
            else: c = shade(i0, 'm')
            cv.sub[syy][sxx] = cv.col(c)
