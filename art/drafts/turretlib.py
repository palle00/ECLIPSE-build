"""Turret drafting for any size (the approved 3x3 turrets used draft.py's 96px version of the same idea).
Turrets point up. A grid is either a LEFT HALF (x < W/2; PixelArt mirrors it with dark<->light swapped, the vanilla
ridge shading) or a FULL canvas (parts drawn on one side only, e.g. a claw the game mirrors itself).
Letters: a/b dark/light body, k/l second metal, m/n body that is in the heat mask, d/e accent pair, z bore/hole,
'.' empty (in game the turret outline #404049 fills the gaps between parts)."""
import sys, os, math

class Grid:
    def __init__(self, w, h=None, half=True):
        self.W, self.H = w, h or w
        self.half = half
        self.gw = self.W // 2 if half else self.W
        self.g = [['.'] * self.gw for _ in range(self.H)]

    def _each(self, test, ch):
        for y in range(self.H):
            for x in range(self.gw):
                if test(x + .5, y + .5): self.g[y][x] = ch

    def rect(self, x0, y0, x1, y1, ch):
        for y in range(max(0, int(y0)), min(self.H - 1, int(y1)) + 1):
            for x in range(max(0, int(x0)), min(self.gw - 1, int(x1)) + 1):
                self.g[y][x] = ch

    def poly(self, pts, ch):
        def t(px, py):
            inside = False; j = len(pts) - 1
            for i in range(len(pts)):
                xi, yi = pts[i]; xj, yj = pts[j]
                if (yi > py) != (yj > py) and px < (xj - xi) * (py - yi) / (yj - yi) + xi: inside = not inside
                j = i
            return inside
        self._each(t, ch)

    def circle(self, cx, cy, r, ch):
        self._each(lambda px, py: (px - cx) ** 2 + (py - cy) ** 2 <= r * r, ch)

    def ring(self, cx, cy, r0, r1, ch):
        self._each(lambda px, py: r0 * r0 <= (px - cx) ** 2 + (py - cy) ** 2 <= r1 * r1, ch)

    def arc(self, cx, cy, r0, r1, a0, a1, ch):
        """Ring sector between angles a0..a1 (degrees, 0 = right, 90 = down)."""
        def t(px, py):
            d = (px - cx) ** 2 + (py - cy) ** 2
            a = math.degrees(math.atan2(py - cy, px - cx)) % 360
            return r0 * r0 <= d <= r1 * r1 and (a0 <= a <= a1 if a0 <= a1 else (a >= a0 or a <= a1))
        self._each(t, ch)

    def seam(self, x0, y0, x1, y1):
        """An empty seam line (the outline shows through)."""
        self.rect(x0, y0, x1, y1, '.')

    def ridge(self, x0, y0, x1, y1, split=None, dark='a', light='b'):
        """A plate shaded the vanilla way: dark on its left part, lit on its right part."""
        s = split if split is not None else (x0 + x1 + 1) // 2
        self.rect(x0, y0, s - 1, y1, dark); self.rect(s, y0, x1, y1, light)

    def replace(self, test, frm, to):
        for y in range(self.H):
            for x in range(self.gw):
                if self.g[y][x] in frm and test(x + .5, y + .5): self.g[y][x] = to

    def outline(self, ch='o', r=2):
        """Bake an outline round everything (for layers the game does not outline itself)."""
        filled = {(x, y) for y in range(self.H) for x in range(self.gw) if self.g[y][x] != '.'}
        add = set()
        for (x, y) in filled:
            for dy in range(-r, r + 1):
                for dx in range(-r, r + 1):
                    if dx * dx + dy * dy <= r * r + 1:
                        q = (x + dx, y + dy)
                        if 0 <= q[0] < self.gw and 0 <= q[1] < self.H and q not in filled: add.add(q)
        for (x, y) in add: self.g[y][x] = ch

    def write(self, out, name, header):
        with open(os.path.join(out, name + ".txt"), "w", newline="\n") as f:
            f.write(header.strip() + "\ngrid\n")
            for row in self.g: f.write("".join(row) + "\n")
        print("drafted", name)

def turret_header(size_px, base, extra="", mirror=True, refs="ripple"):
    return (f"size {size_px} {size_px}\n" + ("mirror\n" if mirror else "") +
            "color a 4d4e58 b\ncolor b 7b7b7b a\ncolor z 2c2d38\n"
            "color m 4d4e58 n\ncolor n 7b7b7b m\n"
            "color k 3b3e4c l\ncolor l 5d6273 k\n" + extra.strip() + "\n" + f"base {base}\nref {refs}\n")
