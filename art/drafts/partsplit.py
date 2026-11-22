"""Lift a moving piece out of an approved mirrored turret grid into its own part grid(s), leaving the art identical at rest.
  sym: a centred piece (barrel, tubes, emitter): one mirrored part grid NAME-SUFFIX (the game draws it unmirrored)
  rl:  a side piece the game mirrors (RegionPart mirror = true): NAME-SUFFIX-r (right side, as PixelArt's mirror shades it)
       and NAME-SUFFIX-l (the same, letters swapped back: the game flips it onto the left)
Usage: python partsplit.py SRC_DIR OUT_DIR SPEC.py  (SPEC: PARTS = [(name, suffix, mode, [(x0, y0, x1, y1), ...]), ...],
rects in full-canvas pixels; only their left-half part matters since the grids are left halves)"""
import sys, os, runpy

src, out, spec = sys.argv[1:4]
PARTS = runpy.run_path(spec)["PARTS"]

def read(name):
    lines = open(os.path.join(src, name + ".txt"), encoding="utf-8").read().split("\n")
    i = lines.index("grid")
    head, grid = lines[:i], [list(l) for l in lines[i + 1:] if l.strip()]
    return head, grid

def partners(head):
    p = {}
    for l in head:
        t = l.split()
        if len(t) >= 4 and t[0] == "color": p[t[1]] = t[3]
    return p

def write(name, head, grid):
    with open(os.path.join(out, name + ".txt"), "w", newline="\n") as f:
        f.write("\n".join(head) + "\ngrid\n")
        for row in grid: f.write("".join(row) + "\n")
    print("wrote", name)

mains = {}
for (name, suffix, mode, rects) in PARTS:
    head, grid = mains.get(name) or read(name)
    size = next(l for l in head if l.startswith("size")).split()
    W = int(size[1]); half = len(grid[0])
    inside = lambda x, y: any(x0 <= x <= x1 and y0 <= y <= y1 for (x0, y0, x1, y1) in rects)
    piece = [['.'] * half for _ in grid]
    for y, row in enumerate(grid):
        for x, c in enumerate(row):
            if c != '.' and inside(x, y): piece[y][x] = c; row[x] = '.'
    mains[name] = (head, grid)
    phead = [l for l in head if not l.startswith(("ref", "base", "weapon", "team"))] + [f"on {name}"]
    if mode == "sym":
        write(f"{name}{suffix}", phead, piece)
    else:
        pr = partners(head)
        rhead = [l for l in phead if l != "mirror" and not l.startswith("heat")]
        r = [['.'] * W for _ in grid]; l = [['.'] * W for _ in grid]
        for y, row in enumerate(piece):
            for x, c in enumerate(row):
                if c != '.':
                    r[y][W - 1 - x] = pr.get(c, c)                                   # right side, shaded as the mirror shades it
                    l[y][W - 1 - x] = c                                              # flipped by the game onto the left
        write(f"{name}{suffix}-r", rhead, r); write(f"{name}{suffix}-l", rhead, l)
for name, (head, grid) in mains.items(): write(name, head, grid)
