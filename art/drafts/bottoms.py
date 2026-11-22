"""Tank floors ("-bottom": seen through a machine's liquid windows when it is empty): dark plating with seams and rivets,
one design for every block size, kept subtle so the liquid drawn over it still reads.
The floor is clipped to the machine's outline (its windows stay, its chamfered corners stay clear).
Usage: python bottoms.py OUT_DIR name:size..."""
import sys, os
from PIL import Image
out = sys.argv[1]
for arg in sys.argv[2:]:
    name, n = arg.rsplit(":", 1); n = int(n)
    g = [['F'] * n for _ in range(n)]
    for y in range(n):
        for x in range(n):
            if (x + y) % 2 == 0 and (x // 4 + y // 4) % 3 == 0: g[y][x] = 'f'            # faint plate texture
    step = 16
    for k in range(0, n, step):
        for t in range(n):
            g[t][k] = 'S'; g[k][t] = 'S'                                                   # plate seams
    for yy in range(step // 2, n, step):
        for xx in range(step // 2, n, step): g[yy][xx] = 'R'                              # rivets
    for t in range(n): g[0][t] = g[n - 1][t] = g[t][0] = g[t][n - 1] = 'S'
    a = Image.open(os.path.join(os.path.dirname(__file__), "..", "..", "assets", "sprites", name + ".png")).convert("RGBA").getchannel("A").load()
    outside = set(); stack = [(x, y) for x in range(n) for y in (0, n - 1)] + [(x, y) for y in range(n) for x in (0, n - 1)]
    while stack:                                                                           # flood the clear area from the edges
        x, y = stack.pop()
        if (x, y) in outside or not (0 <= x < n and 0 <= y < n) or a[x, y] > 0: continue
        outside.add((x, y)); stack += [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]
    for (x, y) in outside: g[y][x] = '.'
    with open(os.path.join(out, name + "-bottom.txt"), "w", newline="\n") as f:
        f.write(f"size {n} {n}\noutline 0\ncolor F 2e3038\ncolor f 33353e\ncolor S 24252c\ncolor R 3e404a\ngrid\n" + "\n".join("".join(r) for r in g) + "\n")
    print("wrote", name)
