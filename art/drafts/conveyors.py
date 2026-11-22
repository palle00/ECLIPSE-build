"""ECLIPSE conveyor tiers from vanilla titanium conveyor frames: only the blue trim is recoloured, the dark belt and its
moving tread stay exactly as vanilla (so every frame still animates like vanilla).
Usage: python conveyors.py VANILLA_DIR OUT_DIR   (VANILLA_DIR holds titanium-conveyor-S-F.png dumped from the game atlas)"""
import sys, os
from PIL import Image
van, out = sys.argv[1], sys.argv[2]
TIERS = {"aluminium": ("8a96a4", "d6dde4"), "chromium": ("6f88b0", "c8dcf2"), "superalloy": ("9a7c48", "e6d09a"),
         "carbide": ("4a4458", "8a82a0"), "surge": ("c4862a", "ffd86a")}
def hexrgb(h): return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))
for tier, (dark, light) in TIERS.items():
    cols = (hexrgb(dark), hexrgb(light))
    for s in range(5):
        for f in range(4):
            im = Image.open(os.path.join(van, f"titanium-conveyor-{s}-{f}.png")).convert("RGBA")
            px = im.load()
            for y in range(im.height):
                for x in range(im.width):
                    r, g, b, a = px[x, y]
                    if a and b > r + 40 and b > g + 20:                          # any blue trim pixel (edges included), by brightness
                        t = max(0., min(1., (g - 128) / 36))                       # dark blue (g 128) -> tier dark, light blue (g 164) -> tier light
                        px[x, y] = tuple(int(cols[0][i] + (cols[1][i] - cols[0][i]) * t) for i in range(3)) + (a,)
            im.save(os.path.join(out, f"{tier}-conveyor-{s}-{f}.png"))
            if s == 0 and f == 0: im.save(os.path.join(out, f"{tier}-conveyor.png"))
    print("wrote", tier)
