"""Unit parts: drone rotors (spun by the game) and the Wasp's wings (lifted out of its grid, they flutter).
Usage: python unitparts.py PIXEL_DIR OUT_DIR"""
import sys, os, math
src, out = sys.argv[1], sys.argv[2]

# rotor: two crossed blades with a hub, 16px (drawn over each rotor pod)
R = 16
g = [['.'] * R for _ in range(R)]
for y in range(R):
    for x in range(R):
        px, py = x + .5 - 8, y + .5 - 8
        r = math.hypot(px, py)
        if r <= 7.6 and (abs(px - py) <= 1.1 or abs(px + py) <= 1.1): g[y][x] = 'A' if px + py < 0 else 'B'
        if r <= 2.2: g[y][x] = 'H'
head = "size 16 16\noutline 0\ncolor A d6dde4\ncolor B 8f97a4\ncolor H 3b3e4c\n"
for n in ("logistics-drone-rotor", "repair-drone-rotor"):
    with open(os.path.join(out, n + ".txt"), "w", newline="\n") as f:
        f.write(head + "grid\n" + "\n".join("".join(r) for r in g) + "\n")
    print("wrote", n)

# Wasp: lift the wings (pale letters W, X) out of its mirrored grid into a full-canvas right-wing part
lines = open(os.path.join(src, "wasp.txt"), encoding="utf-8").read().split("\n")
i = lines.index("grid"); head, grid = lines[:i], [list(l) for l in lines[i + 1:] if l.strip()]
W = int(next(l for l in head if l.startswith("size")).split()[1]); half = len(grid[0])
wing = [['.'] * W for _ in grid]
for y, row in enumerate(grid):
    for x, c in enumerate(row):
        if c in "WX":
            wing[y][W - 1 - x] = c; row[x] = '.'
with open(os.path.join(out, "wasp.txt"), "w", newline="\n") as f:
    f.write("\n".join(head) + "\ngrid\n" + "\n".join("".join(r) for r in grid) + "\n")
wh = [l for l in head if l.startswith(("size", "outline")) or (l.startswith("color") and l.split()[1] in "WX")]
with open(os.path.join(out, "wasp-wing.txt"), "w", newline="\n") as f:
    f.write("\n".join(wh) + "\noutline 0\ngrid\n" + "\n".join("".join(r) for r in wing) + "\n")
print("wrote wasp, wasp-wing")
