"""Unit glow masks: a copy of each unit grid keeping only its bright, saturated accent pixels (eye slits, vents, cores,
sacs) in their own colours, for a slowly pulsing additive RegionPart ("-glow"). Team-colour cell letters are left out.
Usage: python unitglow.py SRC_DIR OUT_DIR name..."""
import sys, os, colorsys

src, out, names = sys.argv[1], sys.argv[2], sys.argv[3:]
BODY = {"a6b464", "78873f", "4f5c2d", "2b3420"}   # the Swarm's green body: never glows
for name in names:
    lines = open(os.path.join(src, name + ".txt"), encoding="utf-8").read().split("\n")
    i = lines.index("grid")
    head, grid = lines[:i], [l for l in lines[i + 1:] if l.strip()]
    cols, cell = {}, set()
    for l in head:
        t = l.split()
        if len(t) >= 3 and t[0] == "color": cols[t[1]] = t[2]
        if len(t) >= 2 and t[0] == "mask": cell |= {x[0] for x in t[2:]}
    def accent(c):
        if c not in cols or c in cell: return False
        r, g, b = (int(cols[c][k:k + 2], 16) / 255 for k in (0, 2, 4))
        h, s, v = colorsys.rgb_to_hsv(r, g, b)
        if cols[c].lower() in BODY: return False
        return (s > 0.38 and v > 0.62) or (s > 0.2 and v > 0.9)
    keep = {c for c in cols if accent(c)}
    g = ["".join(c if c in keep else '.' for c in row) for row in grid]
    n = sum(row.count(c) for row in g for c in keep)
    h2 = [l for l in head if l.startswith(("size", "mirror", "outline")) or (l.startswith("color") and l.split()[1] in keep)]
    with open(os.path.join(out, name + "-glow.txt"), "w", newline="\n") as f:
        f.write("\n".join(h2) + "\noutline 0\ngrid\n" + "\n".join(g) + "\n")
    print(f"{name}: {n} glow pixels, letters {''.join(sorted(keep))}")
