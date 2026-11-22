"""Helper layers drawn over finished machines (they need the machine's PNGs in assets/sprites first):
  "-heat"   heat glowing in from the right edge (DrawHeatOutput / DrawHeatInput turn it to each side that gets heat)
  "-glow"   heat makers' unturned heat glow, over the block's own hot part (coils, flame, core)
  "-<liquid>-output1/2"  liquid output couplings (DrawLiquidOutputs turns them to the output side; 2 = flipped for left/down)
Run without python -I (uses Pillow from the user site). Usage: python helpers.py OUT_DIR"""
import sys, os
from PIL import Image
OUT = sys.argv[1]
SPR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "assets", "sprites")

def alpha(name):
    p = os.path.join(SPR, name + ".png")
    if not os.path.exists(p): return None
    im = Image.open(p).convert("RGBA")
    a = im.getchannel("A").load()
    return im.size[0], (lambda x, y: a[x, y] > 0)
def write(g, name, colors, on):
    with open(os.path.join(OUT, name + ".txt"), "w", newline="\n") as f:
        f.write(f"size {len(g[0])} {len(g)}\noutline 0\n" + colors + f"on {on}\ngrid\n" + "\n".join("".join(r) for r in g) + "\n")
    print("drafted", name)

# ---- -heat: a funnel of heat from the right edge, brightest at the edge, clipped to the machine (and its outlet) ----
HEAT_OUT = ["electric-heater", "industrial-heater", "thermal-storage", "sulfuric-acid-plant", "geothermal-heat-tap", "heat-exchanger", "fission-reactor"]
HEAT_IN = ["alumina-refinery", "blast-furnace", "carbon-fiber-furnace", "industrial-boiler", "methane-pyrolysis-reactor", "steam-cracker", "thermal-waste-processor"]
for name in HEAT_OUT + HEAT_IN:
    n, main = alpha(name)
    top = alpha(name + "-top1")
    solid = (lambda x, y: main(x, y) or top[1](x, y)) if top else main
    c = n / 2; x0 = n * 0.68
    g = [['.'] * n for _ in range(n)]
    for y in range(n):
        for x in range(int(x0), n):
            if not solid(x, y): continue
            u = (x + .5 - x0) / (n - x0)
            if u <= 0: continue
            hh = n * (0.10 + 0.24 * u)
            dy = abs(y + .5 - c)
            if dy > hh: continue
            i = u ** 1.4 * (1 - 0.65 * (dy / hh) ** 2)
            g[y][x] = 'W' if i > .62 else 'v' if i > .4 else 'u' if i > .22 else 't' if i > .08 else '.'
    write(g, name + "-heat", "color W ffffff\ncolor v b4b4b4\ncolor u 707070\ncolor t 363636\n", name + ("-top1" if top else ""))

# ---- -glow for heat makers: the block's own hot part, dimmer (it adds to that part's own glow when heated) ----
HOT = {"electric-heater": "-coils", "industrial-heater": "-flame", "thermal-storage": "-core", "sulfuric-acid-plant": "-flame",
       "geothermal-heat-tap": "-core", "heat-exchanger": "-core", "fission-reactor": "-core"}
for name, part in HOT.items():
    n, hot = alpha(name + part)
    g = [['w' if hot(x, y) else '.' for x in range(n)] for y in range(n)]
    write(g, name + "-glow", "color w 7a7a7a\n", name)

# ---- liquid output couplings: flange + stub with a window in the liquid's colour and a flow chevron ----
LIQ = {"nitrogen": "efe3ff", "eclipse-oxygen": "d8efff", "eclipse-argon": "c9d6ff", "eclipse-biofuel": "f0e6a0",
       "eclipse-carbon-dioxide": "c8ccd0", "eclipse-naphtha": "d9a45a", "eclipse-methane": "c4b3e6", "eclipse-diesel": "d8b25e",
       "eclipse-purified-water": "8fd3ff", "eclipse-brine": "a7c9b4"}
OUTS = {"air-separation-unit": ["nitrogen", "eclipse-oxygen", "eclipse-argon"], "biochemical-fermenter": ["eclipse-biofuel", "eclipse-carbon-dioxide"],
        "distillation-column": ["eclipse-naphtha", "eclipse-methane", "eclipse-diesel"], "gas-separator": ["nitrogen", "eclipse-carbon-dioxide"],
        "water-treatment-plant": ["eclipse-purified-water", "eclipse-brine"]}
def darker(h, f):
    r, g, b = int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)
    return "%02x%02x%02x" % (int(r * f), int(g * f), int(b * f))
for name, liquids in OUTS.items():
    n = alpha(name)[0]; c = n // 2
    big = n >= 96
    fl, st, hw = (4, 9, 9) if big else (3, 7, 7)            # flange width, stub length, flange half-height
    for liq in liquids:
        g = [['.'] * n for _ in range(n)]
        def put(x0, y0, x1, y1, ch):
            for y in range(y0, y1 + 1):
                for x in range(x0, x1 + 1): g[y][x] = ch
        sx = n - st
        put(sx - fl, c - hw, sx - 1, c + hw - 1, 'B'); put(sx - fl, c - hw, sx - 1, c - 1, 'A')         # flange, lit on top
        put(sx - fl, c - hw, sx - fl, c + hw - 1, 'X')
        for by in (c - hw + 2, c + hw - 3): put(sx - fl + 1, by, sx - fl + 2, by, 'X')                 # bolts
        sh = hw - 3
        put(sx, c - sh, n - 1, c + sh - 1, 'X')                                                        # stub frame
        put(sx + 1, c - sh + 1, n - 2, c + sh - 2, 'l'); put(sx + 1, c - sh + 1, n - 2, c - 1, 'L')     # liquid window
        put(sx, c - sh, n - 1, c - sh, 'M')                                                            # top rim catches the light
        k = sh - 3                                                                                     # flow chevron ">"
        for d in range(k + 1):
            x = sx + 2 + d
            if x < n - 2:
                g[c - k - 1 + d][x] = 'P'; g[c + k - d][x] = 'P'
        col = LIQ[liq]
        head = f"color A b0b9c0\ncolor B 6e7080\ncolor M 989aa4\ncolor X 4a4b53\ncolor L {col}\ncolor l {darker(col, .72)}\ncolor P ffffff\n"
        write(g, f"{name}-{liq}-output1", head, name)
        write(g[::-1], f"{name}-{liq}-output2", head, name)
