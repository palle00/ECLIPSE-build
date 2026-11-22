"""Item icons (32x32) in the vanilla item style: one bold shape filling the icon, shaded with a 5-tone ramp of the item's
colour - light rim top-left, dark rim bottom-right, a lighter facet - and no outline (vanilla items have none)."""
import sys, os, math, colorsys
OUT = sys.argv[1] if len(sys.argv) > 1 else "."
N = 32
POOL = "ABCDEFGHIJKLMNOPQRSTUVWYZabcdefghijklmnopqrstuvwxyz0123456789"   # 'X' kept free

def hexrgb(h): return tuple(int(h[i:i + 2], 16) / 255 for i in (0, 2, 4))
def rgbhex(c): return "".join(f"{max(0, min(255, round(v * 255))):02x}" for v in c)
def tone(h, dv, ds=0., hue=None):
    r, g, b = hexrgb(h); hh, s, v = colorsys.rgb_to_hsv(r, g, b)
    if hue is not None: hh = hue
    return rgbhex(colorsys.hsv_to_rgb(hh, max(0, min(1, s + ds)), max(0, min(1, v + dv))))
def mix(h1, h2, t):
    a, b = hexrgb(h1), hexrgb(h2)
    return rgbhex(tuple(a[i] + (b[i] - a[i]) * t for i in range(3)))

# ---- shape tests (pixel centres) ----
def poly(pts):
    def t(px, py):
        inside = False; j = len(pts) - 1
        for i in range(len(pts)):
            xi, yi = pts[i]; xj, yj = pts[j]
            if (yi > py) != (yj > py) and px < (xj - xi) * (py - yi) / (yj - yi) + xi: inside = not inside
            j = i
        return inside
    return t
def circle(cx, cy, r): return lambda px, py: (px - cx) ** 2 + (py - cy) ** 2 <= r * r
def ellipse(cx, cy, rx, ry): return lambda px, py: ((px - cx) / rx) ** 2 + ((py - cy) / ry) ** 2 <= 1
def rect(x0, y0, x1, y1): return lambda px, py: x0 <= px <= x1 + 1 and y0 <= py <= y1 + 1
def ring(cx, cy, r0, r1): return lambda px, py: r0 * r0 <= (px - cx) ** 2 + (py - cy) ** 2 <= r1 * r1
def both(a, b): return lambda px, py: a(px, py) and b(px, py)
def minus(a, b): return lambda px, py: a(px, py) and not b(px, py)
def union(*ts): return lambda px, py: any(t(px, py) for t in ts)
def rot(pts, cx, cy, deg):
    a = math.radians(deg); c, s = math.cos(a), math.sin(a)
    return [(cx + (x - cx) * c - (y - cy) * s, cy + (x - cx) * s + (y - cy) * c) for (x, y) in pts]
def bar(cx, cy, length, width, deg=-45, chamfer=3):
    """A chamfered bar (ingot / slab), rotated (vanilla metals lie at 45 degrees)."""
    l, w, c = length / 2, width / 2, chamfer
    pts = [(cx - l + c, cy - w), (cx + l - c, cy - w), (cx + l, cy - w + c), (cx + l, cy + w - c), (cx + l - c, cy + w), (cx - l + c, cy + w), (cx - l, cy + w - c), (cx - l, cy - w + c)]
    return rot(pts, cx, cy, deg)
def ngon(cx, cy, r, n, deg=0): return [(cx + r * math.cos(math.radians(deg + i * 360 / n)), cy + r * math.sin(math.radians(deg + i * 360 / n))) for i in range(n)]

class Icon:
    def __init__(self):
        self.g = [['.'] * N for _ in range(N)]
        self.cols = {}
        self.pool = iter(POOL)

    def letters(self, hexes):
        out = []
        for h in hexes:
            for k, v in self.cols.items():
                if v == h: out.append(k); break
            else:
                k = next(self.pool); self.cols[k] = h; out.append(k)
        return out

    def ramp(self, h, hot=False, flat=False):
        """5 letters: highlight, light, base, shade, dark. hot: highlights run to yellow-white, darks to deep red."""
        if hot:
            hexes = [mix(h, "fff4c0", .75), mix(h, "ffe08a", .4), h, mix(h, "b8341c", .45), mix(h, "6e1a10", .65)]
        else:
            r, g, b = hexrgb(h); v = max(r, g, b)
            up = .3 if v < .4 else .22
            hexes = [tone(h, up + .08, -.12), tone(h, up * .5, -.05), h, tone(h, -.17, .04), tone(h, -.33, .08)]
            if flat: hexes[0] = hexes[1]
        return self.letters(hexes)

    def put(self, test, ramp, facet=None, rim=True):
        """Fill a shape: light rim on its top/left edges, dark rim bottom/right, a shade band inside the dark rim,
        and facet(px, py) -> 0..4 (ramp index) for the interior (default: base, a lighter upper-left half)."""
        a, b, c, d, e = ramp
        m = {(x, y) for y in range(N) for x in range(N) if test(x + .5, y + .5)}
        for (x, y) in m:
            lo = (x - 1, y) not in m or (x, y - 1) not in m
            hi = (x + 1, y) not in m or (x, y + 1) not in m
            if rim and lo and not hi: ch = a
            elif rim and hi and not lo: ch = e
            elif rim and ((x + 2, y) not in m or (x, y + 2) not in m or (x + 1, y + 1) not in m): ch = d
            else:
                k = facet(x + .5, y + .5) if facet else 2
                ch = ramp[k]
            self.g[y][x] = ch
        return m

    def px(self, x, y, ch):
        if 0 <= x < N and 0 <= y < N: self.g[int(y)][int(x)] = ch

    def line(self, x0, y0, x1, y1, ch):
        n = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
        for i in range(n + 1):
            t = i / n
            self.px(round(x0 + (x1 - x0) * t), round(y0 + (y1 - y0) * t), ch)

    def write(self, name):
        with open(os.path.join(OUT, name + ".txt"), "w", newline="\n") as f:
            f.write(f"size {N} {N}\noutline 0\n")
            for k, v in self.cols.items(): f.write(f"color {k} {v}\n")
            f.write("grid\n")
            for row in self.g: f.write("".join(row) + "\n")

# ---- common facet patterns ----
def diag_light(cx=16, cy=16, k=0): return lambda px, py: 1 if (px - cx) + (py - cy) < k else 2
def stripe(cx, cy, deg, off, width=1.6):
    """A highlight stripe along a bar at angle deg, offset from its axis."""
    a = math.radians(deg); nx, ny = -math.sin(a), math.cos(a)
    return lambda px, py: 0 if abs((px - cx) * nx + (py - cy) * ny - off) < width / 2 else 2
