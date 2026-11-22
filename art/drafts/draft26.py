"""1x1 blocks: augmentation couplers and interfaces, item routing blocks, flow meter, liquid valves.
Routing blocks and valves turn with their rotation: the back (input) is the left edge, the front the right edge."""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import blocklib
blocklib.W = blocklib.H = 32
from blocklib import new, write, inpoly, incircle, inrect, both, minus, octagon, diamond, diag, fill, part, groove

B1 = """
size 32 32
outline 0
color A b0b9c0
color B 6e7080
color M 989aa4
color G 757887
color H 5c5e6c
color X 4a4b53
"""
ORANGE = "color O d96b68\ncolor P fdb380\n"
CYAN = "color C 8ae4ff\ncolor c 4fa7c7\n"

def base1(g):
    body = inpoly([(1, 0), (31, 0), (32, 1), (32, 31), (31, 32), (1, 32), (0, 31), (0, 1)])
    diag(g, body, 'A', 'B', 16, 16, 'M')
    groove(g, minus(inrect(2, 2, 29, 29), inrect(3, 3, 28, 28)))
def chev(g, cx, cy, d, c='P'):
    for i in range(3):
        for j in (-i, i):
            x, y = [(cx - i, cy + j), (cx + j, cy + i), (cx + i, cy + j), (cx + j, cy - i)][d]
            g[int(y)][int(x)] = c
def channel(g, x0, y0, x1, y1):
    part(g, inrect(x0, y0, x1, y1), (x0 + x1) / 2, (y0 + y1) / 2); fill(g, inrect(x0 + 1, y0 + 1, x1 - 1, y1 - 1), 'X')

# ---------------- augmentation couplers and interfaces: socket(s); -connector arm (turns to the machine); -light ----------------
COUPLERS = {"augmentation-coupler": [(16, 16, 7)], "advanced-coupler": [(16, 10, 5), (16, 22, 5)],
            "chemical-augmentation-interface": [(10, 10, 4.5), (22, 10, 4.5), (10, 22, 4.5)], "military-augmentation-interface": [(10, 9, 4.5), (10, 22, 4.5)]}
for name, slots in COUPLERS.items():
    g = new(); base1(g)
    for (cx, cy, r) in slots:
        part(g, inpoly(octagon(cx, cy, r)), cx, cy); fill(g, inpoly(octagon(cx, cy, r - 2)), 'X')
    if name.startswith("chemical"):
        part(g, incircle(22, 22, 5), 22, 22); fill(g, incircle(22, 22, 3), 'X'); fill(g, incircle(22, 22, 1.5), 'Y')   # reagent port
    if name.startswith("military"):
        part(g, inrect(18, 6, 27, 26), 22, 16); fill(g, inrect(20, 9, 25, 12), 'R')    # targeting module
        for y in (15, 18, 21): fill(g, inrect(20, y, 25, y), 'X')
    write(g, name, B1 + "color Y c8d06a\ncolor R d96b68\nref overdrive-projector mender")
    g = new()
    part(g, inrect(24, 13, 31, 19), 28, 16); fill(g, inrect(29, 14, 31, 18), 'C')
    write(g, name + "-connector", B1 + CYAN + "on " + name)
    g = new()
    for (cx, cy, r) in slots: fill(g, inpoly(octagon(cx, cy, r - 2.5)), 'W')
    write(g, name + "-light", B1 + "color W ffffff\non " + name)

# ---------------- Three-Way Item Sorter: selector over a cross of channels (back in; front, left, right out) ----------------
g = new(); base1(g)
channel(g, 0, 12, 31, 20); channel(g, 12, 0, 20, 31)
part(g, inpoly(diamond(16, 16, 8)), 16, 16); fill(g, inpoly(diamond(16, 16, 4)), 'X'); fill(g, inrect(15, 15, 16, 16), 'P')
chev(g, 28, 16, 0); chev(g, 16, 3, 1); chev(g, 16, 28, 3)
write(g, "three-way-sorter", B1 + ORANGE + "ref sorter router")

# ---------------- Advanced Item Filter: straight channel through a filter comb, reject port to the side ----------------
g = new(); base1(g)
channel(g, 0, 11, 31, 21); channel(g, 13, 0, 19, 11)
part(g, inrect(12, 8, 19, 24), 15.5, 16)
for y in range(10, 23, 3): fill(g, inrect(14, y, 17, y), 'X')
chev(g, 27, 16, 0, 'C'); chev(g, 16, 4, 1, 'O')
write(g, "advanced-item-filter", B1 + ORANGE + CYAN + "ref sorter overflow-gate")

# ---------------- Batch Gate: channel with a sliding gate door and a batch counter lamp ----------------
g = new(); base1(g)
channel(g, 0, 11, 31, 21)
part(g, inrect(14, 4, 19, 27), 16.5, 16); fill(g, inrect(15, 12, 18, 20), 'V')     # gate door
for (x, y) in ((5, 5), (9, 5)): fill(g, inrect(x, y, x + 2, y + 2), 'X')
fill(g, inrect(6, 6, 6, 6), 'L'); fill(g, inrect(10, 6, 10, 6), 'L')
chev(g, 26, 16, 0)
write(g, "batch-gate", B1 + ORANGE + "color V a07fd0\ncolor L 8fffb0\nref overflow-gate sorter")

# ---------------- Controlled Splitter: one line in, split by a ratio dial to front and sides ----------------
g = new(); base1(g)
channel(g, 0, 12, 16, 20); channel(g, 16, 12, 31, 20); channel(g, 16, 0, 20, 31)
part(g, incircle(16, 16, 7), 16, 16); fill(g, incircle(16, 16, 4.5), 'X')          # ratio dial
for k in range(3):
    a = -math.pi / 2 + k * math.pi / 2
    fill(g, incircle(16 + 3 * math.cos(a), 16 + 3 * math.sin(a), 1), 'P')
chev(g, 27, 16, 0); chev(g, 18, 4, 1); chev(g, 18, 27, 3)
write(g, "controlled-splitter", B1 + ORANGE + "ref overflow-gate router")

# ---------------- Priority Merger: three inputs merge into the front, priority pips ----------------
g = new(); base1(g)
channel(g, 0, 12, 31, 20); channel(g, 12, 0, 20, 16); channel(g, 12, 16, 20, 31)
part(g, inrect(18, 10, 25, 22), 21.5, 16)                                           # merge gate
chev(g, 28, 16, 0)
for i, (x, y) in enumerate(((4, 16), (16, 4), (16, 27))):                           # priority 1-2-3
    for k in range(i + 1): fill(g, inrect(x - 1 + k * 0, y - 1 + (k - i / 2) * 0, x, y), 'L')
fill(g, inrect(3, 15, 4, 16), 'L'); fill(g, inrect(14, 3, 15, 4), 'L'); fill(g, inrect(17, 3, 18, 4), 'L')
fill(g, inrect(12, 27, 13, 28), 'L'); fill(g, inrect(15, 27, 16, 28), 'L'); fill(g, inrect(18, 27, 19, 28), 'L')
write(g, "priority-merger", B1 + ORANGE + "color L 8ae4ff\nref router overflow-gate")

# ---------------- Smart Unloader: unloading chute with a sensor eye ----------------
g = new(); base1(g)
part(g, inpoly(octagon(16, 16, 12)), 16, 16); fill(g, inpoly(octagon(16, 16, 9)), 'X')
fill(g, inpoly(diamond(16, 16, 6)), 'G'); fill(g, inpoly(diamond(16, 16, 3)), 'C'); fill(g, inrect(16, 15, 16, 15), 'W')   # sensor eye
chev(g, 27, 16, 0)
write(g, "smart-unloader", B1 + ORANGE + CYAN + "color W e8fbff\nref unloader sorter")

# ---------------- Flow Meter: LCD screen (the game writes the rate on it); -ports: in/out ports (turn with the block) ----------------
g = new(); base1(g)
fill(g, inrect(3, 3, 28, 25), 'X'); fill(g, inrect(4, 4, 27, 24), 'K')               # screen: x 4-27, y 4-24
part(g, inrect(3, 26, 28, 29), 16, 27.5)
for x in (6, 10, 14): fill(g, inrect(x, 27, x + 1, 28), 'H')
write(g, "flow-meter", B1 + "color K 16241c\nref logic-display sorter")
g = new()
for (x0, x1) in ((0, 2), (29, 31)): part(g, inrect(x0, 12, x1, 20), (x0 + x1) / 2, 16)
fill(g, inrect(30, 15, 31, 17), 'P')
write(g, "flow-meter-ports", B1 + ORANGE + "on flow-meter")

# ---------------- liquid valves: pipe through, sight glass in the middle (the game fills it), each with its own valve gear ----------------
for name in ("liquid-overflow-valve", "liquid-balancer", "reserve-valve"):
    g = new(); base1(g)
    part(g, inrect(0, 10, 31, 22), 16, 16)                                           # pipe through
    if name == "liquid-overflow-valve":
        part(g, inrect(12, 0, 20, 31), 16, 16)                                       # overflow outlets to the sides
        chev(g, 16, 3, 1, 'N'); chev(g, 16, 28, 3, 'N')
    elif name == "liquid-balancer":
        part(g, incircle(16, 16, 11), 16, 16)                                        # balancing chamber
        for k in range(4):
            a = k * math.pi / 2 + math.pi / 4
            fill(g, incircle(16 + 8 * math.cos(a), 16 + 8 * math.sin(a), 1), 'N')
    else:
        part(g, incircle(16, 16, 11), 16, 16)                                        # handwheel
        for k in range(3):
            a = k * 2 * math.pi / 3
            for r in range(7, 11): g[int(16 + r * math.sin(a))][int(16 + r * math.cos(a))] = 'O'
    fill(g, inrect(9, 9, 22, 22), 'H'); fill(g, inrect(10, 10, 21, 21), 'X')         # sight glass (x/y 10-21)
    chev(g, 28, 16, 0, 'N')
    write(g, name, B1 + ORANGE + "color N 9fb3c8\nref overflow-gate liquid-router")
