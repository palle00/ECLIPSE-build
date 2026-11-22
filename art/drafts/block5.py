"""The 5x5 turret base (vanilla has bases only up to block-4; the game looks for the mod's eclipse-block-5 next).
Same language as vanilla's block-2..4: light rim top/right, dark rim left/bottom split diagonally, a mid panel with a
light octagon ring, diagonal slots and dots. Usage: python block5.py OUT_DIR"""
import sys, os, math
from PIL import Image, ImageDraw

N = 160; RIM = 12
LIGHT, DARK, MID = (176, 185, 192, 255), (110, 112, 128, 255), (152, 154, 164, 255)
im = Image.new("RGBA", (N, N), MID); d = ImageDraw.Draw(im)
d.polygon([(0, 0), (N, 0), (N, N), (N - RIM, N - RIM), (N - RIM, RIM), (RIM, RIM)], fill=LIGHT)      # top + right rim
d.polygon([(0, 0), (RIM, RIM), (RIM, N - RIM), (N - RIM, N - RIM), (N, N), (0, N)], fill=DARK)        # left + bottom rim
c = 9                                                                                               # chamfered inner panel
d.polygon([(RIM + c, RIM), (N - RIM - c, RIM), (N - RIM, RIM + c), (N - RIM, N - RIM - c), (N - RIM - c, N - RIM), (RIM + c, N - RIM), (RIM, N - RIM - c), (RIM, RIM + c)], fill=MID)
for (x, y) in ((RIM, RIM), (N - RIM, RIM), (RIM, N - RIM), (N - RIM, N - RIM)):                   # the panel corners show the rim
    pass
def octagon(cx, cy, r, k=.41):
    return [(cx - r + r * k, cy - r), (cx + r - r * k, cy - r), (cx + r, cy - r + r * k), (cx + r, cy + r - r * k),
            (cx + r - r * k, cy + r), (cx - r + r * k, cy + r), (cx - r, cy + r - r * k), (cx - r, cy - r + r * k)]
d.polygon(octagon(80, 80, 52), fill=LIGHT); d.polygon(octagon(80, 80, 44), fill=MID)               # octagon ring
d.polygon(octagon(80, 80, 22), fill=LIGHT); d.polygon(octagon(80, 80, 17), fill=MID)               # inner ring
def slot(cx, cy, ang, length, width):
    a = math.radians(ang); dx, dy = math.cos(a) * length / 2, math.sin(a) * length / 2
    nx, ny = -math.sin(a) * width / 2, math.cos(a) * width / 2
    d.polygon([(cx - dx + nx, cy - dy + ny), (cx + dx + nx, cy + dy + ny), (cx + dx - nx, cy + dy - ny), (cx - dx - nx, cy - dy - ny)], fill=LIGHT)
for (sx, sy) in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
    ang = 45 if sx * sy > 0 else -45
    slot(80 + sx * 22, 80 + sy * 22, ang, 18, 7)                                                   # slots inside the ring
    slot(80 + sx * 56, 80 + sy * 56, ang, 20, 8)                                                   # corner slots
    for (ox, oy) in ((sx * 38, sy * 62), (sx * 62, sy * 38)):                                      # dots beside them
        d.ellipse([80 + ox - 4, 80 + oy - 4, 80 + ox + 4, 80 + oy + 4], fill=LIGHT)
im.save(os.path.join(sys.argv[1], "block-5.png")); print("wrote block-5")
