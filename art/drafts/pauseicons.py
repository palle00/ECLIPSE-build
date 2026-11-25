"""Outline icons for the ECLIPSE pause menu (user, 2026-11-25: "better icons", like the approved mockup).

White strokes on transparent, 48x48 (drawn 8x larger and scaled down, so the edges are smooth like the game's own icon
font); the game tints them (white, gold when hovered, grey when disabled). Written to assets/sprites/ui/pause-NAME.png,
loaded as the atlas region eclipse-pause-NAME.

Run without -I (uses Pillow from the user site): python art/drafts/pauseicons.py OUT_DIR
"""
import math, os, sys
from PIL import Image, ImageDraw

N, S = 48, 8          # output size, supersampling
W = 3.4 * S           # stroke width
WHITE = (255, 255, 255, 255)


def canvas():
    im = Image.new("RGBA", (N * S, N * S), (0, 0, 0, 0))
    return im, ImageDraw.Draw(im)


def P(x, y):
    """A point in 24-unit icon space (like the mockup's 24x24 viewBox) to supersampled pixels, with a small margin."""
    k = (N - 6) / 24 * S
    return (3 * S + x * k, 3 * S + y * k)


def line(d, pts, w=W):
    pts = [P(*p) for p in pts]
    d.line(pts, fill=WHITE, width=int(w), joint="curve")
    for p in (pts[0], pts[-1]):
        r = w / 2
        d.ellipse((p[0] - r, p[1] - r, p[0] + r, p[1] + r), fill=WHITE)


def poly(d, pts, w=W):
    line(d, pts + [pts[0]], w)


def circle(d, cx, cy, r, w=W, fill=False):
    (x0, y0), (x1, y1) = P(cx - r, cy - r), P(cx + r, cy + r)
    if fill: d.ellipse((x0, y0, x1, y1), fill=WHITE)
    else: d.ellipse((x0, y0, x1, y1), outline=WHITE, width=int(w))


def arc(d, cx, cy, r, a0, a1, w=W):
    (x0, y0), (x1, y1) = P(cx - r, cy - r), P(cx + r, cy + r)
    d.arc((x0, y0, x1, y1), a0, a1, fill=WHITE, width=int(w))


def dot(d, x, y, r=1.2):
    circle(d, x, y, r, fill=True)


def resume(d):
    poly(d, [(7, 4.5), (19.5, 12), (7, 19.5)])

def settings(d):
    # a gear: one outline with eight flat-topped teeth, a hole in the middle
    pts = []
    for i in range(8):
        a = i * math.pi / 4
        for da, r in ((-0.36, 7.4), (-0.2, 10), (0.2, 10), (0.36, 7.4)):
            pts.append((12 + math.cos(a + da) * r, 12 + math.sin(a + da) * r))
    poly(d, pts, W * 0.9)
    circle(d, 12, 12, 3.2)

def wiki(d):
    # an open book
    line(d, [(12, 6.5), (12, 20)])
    line(d, [(12, 6.5), (8, 5), (3, 5), (3, 18.5), (8, 18.5), (12, 20)])
    line(d, [(12, 6.5), (16, 5), (21, 5), (21, 18.5), (16, 18.5), (12, 20)])

def production(d):
    line(d, [(3, 20.5), (21, 20.5)])
    for x, top in ((6, 13), (12, 6), (18, 10)):
        line(d, [(x, 20.5), (x, top)], W * 1.35)

def stalls(d):
    poly(d, [(12, 3), (22, 20.5), (2, 20.5)])
    line(d, [(12, 9.5), (12, 14)])
    dot(d, 12, 17.3, 1.3)

def save(d):
    poly(d, [(4, 3.5), (17, 3.5), (20.5, 7), (20.5, 20.5), (4, 20.5)])
    line(d, [(8, 3.5), (8, 8.5), (15, 8.5), (15, 3.5)])
    poly(d, [(8, 20.5), (8, 14), (16.5, 14), (16.5, 20.5)])

def load(d):
    # a folder, its flap open
    line(d, [(3, 18.5), (3, 5), (9, 5), (11, 7.5), (19, 7.5), (19, 10)])
    poly(d, [(3, 18.5), (6, 10.5), (22, 10.5), (19, 18.5)])

def planet(d):
    circle(d, 12, 12, 6.8)
    # the ring: an ellipse tilted, its back half hidden behind the planet
    pts = []
    for i in range(0, 61):
        t = i / 60 * 2 * math.pi
        x, y = 11 * math.cos(t), 3.6 * math.sin(t)
        a = math.radians(-22)
        px, py = x * math.cos(a) - y * math.sin(a), x * math.sin(a) + y * math.cos(a)
        pts.append((12 + px, 12 + py, math.sin(t) > 0 or math.hypot(px, py) > 6.9))
    seg = []
    for x, y, vis in pts:
        if vis: seg.append((x, y))
        elif len(seg) > 1:
            line(d, seg, W * 0.85); seg = []
        else: seg = []
    if len(seg) > 1: line(d, seg, W * 0.85)

def research(d):
    # a flask
    line(d, [(9, 3), (15, 3)])
    line(d, [(10, 3), (10, 9.5), (4.5, 19), (5.5, 20.5), (18.5, 20.5), (19.5, 19), (14, 9.5), (14, 3)])
    line(d, [(7, 15), (17, 15)], W * 0.8)

def host(d):
    circle(d, 9, 8, 3.4)
    arc(d, 9, 20.5, 6.5, 180, 360)
    line(d, [(2.5, 20.5), (15.5, 20.5)])
    arc(d, 16, 8, 3.4, 270, 90 + 0.01)
    arc(d, 17, 20.5, 5.5, 270, 360)

def quit_(d):
    line(d, [(13, 3.5), (20, 3.5), (20, 20.5), (13, 20.5)])
    line(d, [(3.5, 12), (14.5, 12)])
    line(d, [(8, 7.5), (3.5, 12), (8, 16.5)])

def abandon(d):
    # a white flag on a pole
    line(d, [(5, 21), (5, 3)])
    line(d, [(5, 4), (19, 4), (15.5, 8.5), (19, 13), (5, 13)])

def more(d):
    line(d, [(9, 5), (16, 12), (9, 19)])


ICONS = {"resume": resume, "settings": settings, "wiki": wiki, "production": production, "stalls": stalls, "save": save,
         "load": load, "planet": planet, "research": research, "host": host, "quit": quit_, "abandon": abandon, "more": more}


def main():
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    sheet = Image.new("RGBA", (len(ICONS) * (N * 2 + 8), N * 2 + 8), (16, 18, 24, 255))
    for i, (name, draw) in enumerate(ICONS.items()):
        im, d = canvas()
        draw(d)
        small = im.resize((N, N), Image.LANCZOS)
        small.save(os.path.join(out, "pause-" + name + ".png"))
        sheet.alpha_composite(small.resize((N * 2, N * 2), Image.LANCZOS), (i * (N * 2 + 8) + 4, 4))
        print("icon", name)
    sheet.save(os.path.join(out, "..", "pause-icons-preview.png") if len(sys.argv) < 3 else sys.argv[2])


if __name__ == "__main__":
    main()
