"""The ECLIPSE logo (sprite "logo" -> eclipse-logo, main menu and cover art) and "logo-glow" (the corona, pulsed by
MenuBackdrop). Blocky 5x7 metal letters like the generated logo it replaces (bevelled metal, dark outline, orange rim), with
bevelled outer corners, and the C drawn as an eclipse: a glowing crescent round a dark moon."""
import sys, os, math
OUT = sys.argv[1]
LET = {
    "E": ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
    "L": ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
    "I": ["11111", "00100", "00100", "00100", "00100", "00100", "11111"],
    "P": ["11111", "10001", "10001", "11111", "10000", "10000", "10000"],
    "S": ["11111", "10000", "10000", "11111", "00001", "00001", "11111"],
}
CELL, GAP, PAD, OUTL, RIM = 12, 12, 10, 5, 2
LH = 7 * CELL
CW = LH                                                                          # the eclipse C is round: as wide as it is tall
word = ["E", "C", "L", "I", "P", "S", "E"]
widths = [CW if c == "C" else 5 * CELL for c in word]
W = sum(widths) + GAP * (len(word) - 1) + PAD * 2; H = LH + PAD * 2
face = [[False] * W for _ in range(H)]
cres = [[False] * W for _ in range(H)]; moon = [[False] * W for _ in range(H)]
x = PAD
for c, w in zip(word, widths):
    if c == "C":
        cx, cy, R = x + w / 2, PAD + LH / 2, LH / 2
        mx, mr = cx + R * 0.38, R * 0.86                                         # the moon, offset right: the sun shows as a crescent
        for py in range(H):
            for px in range(W):
                X, Y = px + .5, py + .5
                inside = (X - cx) ** 2 + (Y - cy) ** 2 <= R * R
                inmoon = (X - mx) ** 2 + (Y - cy) ** 2 <= mr * mr
                if inside and not inmoon: cres[py][px] = True
                elif inside and inmoon: moon[py][px] = True
    else:
        rows = LET[c]
        def on(r, q): return 0 <= r < 7 and 0 <= q < 5 and rows[r][q] == '1'
        for r in range(7):
            for q in range(5):
                if not on(r, q): continue
                for py in range(CELL):
                    for px in range(CELL):
                        # bevel convex corners: cut a 45 degree triangle where both neighbours on that corner are empty
                        cut = False
                        for (dx, dy, fx, fy) in ((-1, -1, px, py), (1, -1, CELL - 1 - px, py), (-1, 1, px, CELL - 1 - py), (1, 1, CELL - 1 - px, CELL - 1 - py)):
                            if not on(r + dy, q) and not on(r, q + dx) and fx + fy < 4: cut = True
                        if not cut: face[PAD + r * CELL + py][x + q * CELL + px] = True
    x += w + GAP
solid = [[face[y][x] or cres[y][x] or moon[y][x] for x in range(W)] for y in range(H)]
# enclosed holes (the P's bowl): filled dark, no rim inside them
outside = [[False] * W for _ in range(H)]
stack = [(x, y) for x in range(W) for y in (0, H - 1)] + [(x, y) for y in range(H) for x in (0, W - 1)]
while stack:
    x, y = stack.pop()
    if not (0 <= x < W and 0 <= y < H) or outside[y][x] or solid[y][x]: continue
    outside[y][x] = True; stack += [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]

# metal ramp top -> bottom, white catch-light under top edges, dark lip over bottom edges
METAL = ["f2f3f8", "e2e4eb", "d2d4dd", "c2c4cf", "b2b5c1", "a2a5b3", "9396a6", "8d8fa0"]
CORONA = ["fff1c4", "ffd98a", "ffbf5c", "ff9a3c", "e77a2c", "c75e22"]
g = [['.'] * W for _ in range(H)]
for y in range(H):
    for x in range(W):
        f = min(1, max(0, (y - PAD) / LH))
        if face[y][x]:
            ch = "abcdefgh"[min(7, int(f * 8))]
            if (y > 0 and not face[y - 1][x]) or (y > 1 and not face[y - 2][x]): ch = 'W'
            elif (y < H - 1 and not face[y + 1][x]) or (y < H - 2 and not face[y + 2][x]): ch = 'Z'
            g[y][x] = ch
        elif cres[y][x]:
            g[y][x] = "pqrstu"[min(5, int(f * 6))]
        elif moon[y][x]:
            edge = any(not moon[y + dy][x + dx] and not cres[y + dy][x + dx] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if 0 <= y + dy < H and 0 <= x + dx < W)
            near = any(cres[y + dy][x + dx] for dx in (-2, -1, 0, 1, 2) for dy in (-2, -1, 0, 1, 2) if 0 <= y + dy < H and 0 <= x + dx < W)
            g[y][x] = 'm' if near else 'K'                                       # a faint lit rim where the sun shows past it
        else:
            d = 99
            for dy in range(-OUTL - RIM, OUTL + RIM + 1):
                for dx in range(-OUTL - RIM, OUTL + RIM + 1):
                    ny, nx = y + dy, x + dx
                    if 0 <= ny < H and 0 <= nx < W and solid[ny][nx]: d = min(d, max(abs(dx), abs(dy)))
            if d <= OUTL or not outside[y][x]: g[y][x] = 'O'
            elif d <= OUTL + RIM: g[y][x] = 'R'
cols = "".join(f"color {k} {v}\n" for k, v in zip("abcdefgh", METAL)) + "".join(f"color {k} {v}\n" for k, v in zip("pqrstu", CORONA))
cols += "color W ffffff\ncolor Z 6a6c78\ncolor K 0b0c10\ncolor m 3a2a1c\ncolor O 16161d\ncolor R ff9a3c\n"
with open(os.path.join(OUT, "logo.txt"), "w", newline="\n") as f:
    f.write(f"size {W} {H}\noutline 0\n" + cols + "grid\n" + "\n".join("".join(r) for r in g) + "\n")
print("drafted logo", W, H)

# the corona glow: the crescent bright, a halo round it fading out over the outline
glow = [['.'] * W for _ in range(H)]
for y in range(H):
    for x in range(W):
        if cres[y][x]: glow[y][x] = 'W'; continue
        d = 99
        for dy in range(-6, 7):
            for dx in range(-6, 7):
                ny, nx = y + dy, x + dx
                if 0 <= ny < H and 0 <= nx < W and cres[ny][nx]: d = min(d, math.hypot(dx, dy))
        if d <= 2.5 and not moon[y][x]: glow[y][x] = 'v'
        elif d <= 4.5 and not moon[y][x]: glow[y][x] = 'u'
        elif d <= 6.5 and not moon[y][x]: glow[y][x] = 't'
with open(os.path.join(OUT, "logo-glow.txt"), "w", newline="\n") as f:
    f.write(f"size {W} {H}\noutline 0\ncolor W ffffff\ncolor v a0a0a0\ncolor u 5a5a5a\ncolor t 2a2a2a\non logo\ngrid\n" + "\n".join("".join(r) for r in glow) + "\n")
print("drafted logo-glow")
