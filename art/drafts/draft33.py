"""Status-effect icons (white silhouettes like vanilla's: the game tints them with the effect colour) and the Minelayer's
mine (40px, drawn in its own colours: front and back tint are white)."""
import sys, os, math
OUT = sys.argv[1]

def grid(n): return [['.'] * n for _ in range(n)]
def put(g, test, ch):
    n = len(g)
    for y in range(n):
        for x in range(n):
            if test(x + .5, y + .5): g[y][x] = ch
def circ(cx, cy, r): return lambda px, py: (px - cx) ** 2 + (py - cy) ** 2 <= r * r
def ring(cx, cy, r0, r1): return lambda px, py: r0 * r0 <= (px - cx) ** 2 + (py - cy) ** 2 <= r1 * r1
def rect(x0, y0, x1, y1): return lambda px, py: x0 <= px <= x1 + 1 and y0 <= py <= y1 + 1
def poly(pts):
    def t(px, py):
        inside = False; j = len(pts) - 1
        for i in range(len(pts)):
            xi, yi = pts[i]; xj, yj = pts[j]
            if (yi > py) != (yj > py) and px < (xj - xi) * (py - yi) / (yj - yi) + xi: inside = not inside
            j = i
        return inside
    return t
def line(g, x0, y0, x1, y1, w, ch):
    dx, dy = x1 - x0, y1 - y0; L = math.hypot(dx, dy)
    put(g, lambda px, py: abs((px - x0) * dy - (py - y0) * dx) / L <= w / 2 and -w / 2 <= ((px - x0) * dx + (py - y0) * dy) / L <= L + w / 2, ch)
def write(g, name, head):
    with open(os.path.join(OUT, name + ".txt"), "w", newline="\n") as f:
        f.write(f"size {len(g)} {len(g)}\noutline 0\n" + head + "grid\n" + "\n".join("".join(r) for r in g) + "\n")
    print("drafted", name)
WHITE = "color W ffffff\n"

# etched: an armour plate eaten by acid - pits through it and a drop running off its edge
g = grid(32)
put(g, poly([(4, 13), (28, 13), (28, 25), (24, 29), (8, 29), (4, 25)]), 'W')             # armour plate
for (cx, r) in ((9, 3.2), (16.5, 4), (24, 3)): put(g, circ(cx, 13, r), '.')               # its top edge eaten away
put(g, circ(16.5, 6.5, 3.2), 'W'); put(g, poly([(14, 5.5), (16.5, 0.5), (19, 5.5)]), 'W')  # the acid drop falling onto it
write(g, "etched", WHITE)

# jammed: signal waves with a slash through them
g = grid(32)
for r in (7, 13, 19):
    put(g, lambda px, py, r=r: r - 3 <= math.hypot(px - 16, py - 27) <= r and abs(math.atan2(px - 16, 27 - py)) < .85 and py < 27, 'W')
put(g, circ(16, 26, 2.6), 'W')
line(g, 5, 28, 27, 5, 3.2, '.'); line(g, 6, 27, 26, 6, 1.8, 'W')
write(g, "jammed", WHITE)

# sonar-contact: a pinged target - a dot in a ring with four ticks and an outer ping arc
g = grid(32)
put(g, ring(16, 16, 6.5, 10), "W"); put(g, circ(16, 16, 3.5), "W")
for (x0, y0, x1, y1) in ((14, 1, 17, 6), (14, 26, 17, 31), (1, 14, 6, 17), (26, 14, 31, 17)): put(g, rect(x0, y0, x1, y1), "W")
put(g, lambda px, py: 12.5 <= math.hypot(px - 16, py - 16) <= 14 and px > 16 and py < 16, 'W')
write(g, "sonar-contact", WHITE)

# submerged: water surface waves over a hull going down
g = grid(32)
for k, y0 in enumerate((5, 11)):
    put(g, lambda px, py, y0=y0: abs(py - (y0 + 1.6 * math.sin(px / 2.6))) <= 1.2 and 3 <= px <= 29, 'W')
put(g, poly([(8, 20), (24, 20), (27, 23), (24, 26), (8, 26), (5, 23)]), 'W')               # hull
put(g, rect(14, 16, 18, 20), 'W')                                                          # sail
write(g, "submerged", WHITE)

# the mine: octagonal casing, six detonator prongs, pressure plate, armed light (the game blinks it)
g = grid(40)
C = 20
for k in range(6):
    a = k * math.pi / 3 + math.pi / 6
    line(g, C + 13 * math.cos(a), C + 13 * math.sin(a), C + 18 * math.cos(a), C + 18 * math.sin(a), 3.2, 'P')
    put(g, circ(C + 18 * math.cos(a), C + 18 * math.sin(a), 1.8), 'p')
oct_ = [(C + 15 * math.cos(math.radians(22.5 + 45 * i)), C + 15 * math.sin(math.radians(22.5 + 45 * i))) for i in range(8)]
put(g, poly(oct_), 'B')
put(g, lambda px, py: poly(oct_)(px, py) and (px - C) + (py - C) < -4, 'A')
put(g, ring(C, C, 9, 10.5), 'K')
put(g, circ(C, C, 8.5), 'M'); put(g, lambda px, py: circ(C, C, 8.5)(px, py) and (px - C) + (py - C) < -3, 'L')
put(g, circ(C, C, 3), 'K'); put(g, circ(C, C, 1.8), 'O')
write(g, "mine", "color A 8a8f9c\ncolor B 5a5e6c\ncolor M 6e7280\ncolor L 9a9fac\ncolor K 2c2d38\ncolor P 4a4b53\ncolor p 6e7080\ncolor O d96b68\n")
b = grid(40)                                                                                # back: a dark shadow silhouette under it
for y in range(40):
    for x in range(40):
        if g[y][x] != '.':
            for (dx, dy) in ((1, 1), (2, 2), (0, 1), (1, 0)):
                if 0 <= x + dx < 40 and 0 <= y + dy < 40: b[y + dy][x + dx] = 'S'
write(b, "mine-back", "color S 1a1b22\n")
