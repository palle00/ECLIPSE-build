"""Upgrade module icons: ONE base cartridge for every module; only the family glyph and the level pips change.
Base: chamfered casing, connector pins on the left, a band in the category colour, a dark window with the glyph,
and 5 level pips along the bottom (lit up to the tier). Legendary modules: lilac-gold casing and a gem instead of pips.
Usage: python modules.py OUT_DIR MODULES.txt   (lines "Display Name|id")"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from itemlib import *

CATEGORY = {"production": "ff9a3c", "energy": "5ab8ff", "industrial": "ffd84a", "mining": "c47a3c", "chemical": "b37aff",
            "military": "ff5a5a", "legendary": "e8d8ff"}
FAMILY = {  # family id: category, glyph
    "production-accelerator": "production", "output-amplifier": "production", "precision-fabricator": "production",
    "material-recycler": "industrial", "mass-production": "industrial", "closed-loop-processing": "industrial", "emergency-overdrive": "industrial",
    "power-optimizer": "energy", "thermal-recovery": "energy", "energy-overcharger": "energy", "capacitor-module": "energy",
    "deep-mining": "mining", "advanced-drill-head": "mining", "geological-analyzer": "mining",
    "reaction-accelerator": "chemical", "thermal-controller": "chemical", "pressure-stabilizer": "chemical", "catalyst-optimizer": "chemical",
    "purification-module": "chemical", "waste-recovery": "chemical",
    "rapid-fire-controller": "military", "cooling-system": "military", "advanced-ammunition": "military", "targeting-computer": "military",
    "ballistic-computer": "military",
    "quantum-accelerator": "legendary", "matter-replicator": "legendary", "ancient-efficiency-core": "legendary", "temporal-processing-core": "legendary",
}
SINGLE_TIER = {"geological-analyzer": 1, "ballistic-computer": 2, "mass-production": 3, "closed-loop-processing": 3, "emergency-overdrive": 3}

# 12x10 glyphs: '#' glyph colour, '+' glyph highlight, '-' glyph shade
G = {
"production-accelerator": ["            ", "  #   #     ", "  ##  ##    ", "   ##  ##   ", "    ++  ++  ", "    ++  ++  ", "   ##  ##   ", "  ##  ##    ", "  #   #     ", "            "],
"output-amplifier": ["     ++     ", "    ++++    ", "   ++++++   ", "     ##     ", "  # ## ## # ", "  # ## ## # ", "  # ## ## # ", "  # ## ## # ", " ########## ", "            "],
"precision-fabricator": ["     #      ", "     #      ", "   #####    ", "  #  +  #   ", "### +++ ### ", "  #  +  #   ", "   #####    ", "     #      ", "     #      ", "            "],
"material-recycler": ["   +++++    ", "  +     +   ", " +       ## ", " +      ### ", "            ", " ###        ", " ##       # ", "   #     #  ", "    #####   ", "            "],
"mass-production": ["            ", " ++++  ++++ ", " +--+  +--+ ", " ++++  ++++ ", "            ", "    ++++    ", "    +--+    ", "    ++++    ", " ########## ", "            "],
"closed-loop-processing": ["            ", "  ###  ###  ", " #   ##   # ", "#    ++    #", "#    ++    #", " #   ##   # ", "  ###  ###  ", "            ", "            ", "            "],
"emergency-overdrive": ["    +++     ", "   +++      ", "  +++   ### ", " ######  ## ", "   ###  #   ", "  ###       ", " ##    +++  ", "##    ++    ", "#    ++     ", "            "],
"power-optimizer": ["     ++     ", "    ++      ", "   ++       ", "  ######    ", "     ##     ", "    ##   #  ", "   ##    #  ", "  #    #####", "        ### ", "         #  "],
"thermal-recovery": ["     #      ", "    ##      ", "    ###  #  ", "   ######## ", "  ###+++### ", "  ##+++++## ", "  ##++-++## ", "   ##---##  ", "    #####   ", "            "],
"energy-overcharger": ["      ++    ", "     ++     ", "    ++   #  ", "   ######## ", "     ##  #  ", "    ##      ", "   ##       ", "  #         ", "            ", "            "],
"capacitor-module": ["    #  #    ", "    #  #    ", "    #  #    ", "  ++#  #++  ", "  ++#  #++  ", "  ++#  #++  ", "    #  #    ", "    #  #    ", "    #  #    ", "            "],
"deep-mining": ["  ++++++++  ", "  ########  ", "   ######   ", "   ##++##   ", "    ####    ", "    #++#    ", "     ##     ", "     ##     ", "  -  --  -  ", " ---------- "],
"advanced-drill-head": [" ########## ", " #++++++++# ", "  #++++++#  ", "  ##----##  ", "   #++++#   ", "   ##--##   ", "    #++#    ", "    ##-#    ", "     ##     ", "            "],
"geological-analyzer": ["            ", " ++++++++++ ", "            ", " ###  ##### ", "            ", " ----#----- ", "    ###     ", " ----#----- ", "            ", " ########## "],
"reaction-accelerator": ["    ####    ", "     ##     ", "     ##     ", "    #  #    ", "   #    #   ", "  #  ++  #  ", " #  ++++  # ", " # ++++++ # ", "  ########  ", "            "],
"thermal-controller": ["     ##     ", "    #  # ## ", "    #  #    ", "    #++# ## ", "    #++#    ", "    #++# ## ", "   #++++#   ", "   #++++#   ", "    ####    ", "            "],
"pressure-stabilizer": ["   ######   ", "  #      #  ", " #  +  -  # ", " #   + -  # ", " #    #   # ", " #        # ", "  #      #  ", "   ######   ", "     ##     ", "   ######   "],
"catalyst-optimizer": ["     ##     ", "   ##  ##   ", "   #    #   ", "   #    #   ", "   ##  ##   ", "  # +##+ #  ", " ++      ++ ", " ++      ++ ", "            ", "            "],
"purification-module": ["     +      ", "    +++     ", "   +++++    ", "  +++++++   ", "  ++++#++   ", "  +++###+   ", "   ++#++    ", "    +++     ", "            ", "  ########  "],
"waste-recovery": [" ########## ", "  #++++++#  ", "   #++++#   ", "    #++#    ", "     ##     ", "     ##     ", "  ##    ##  ", " ##      ## ", "  ########  ", "            "],
"rapid-fire-controller": ["            ", " ++  ++  ++ ", " ##  ##  ## ", " ##  ##  ## ", " ##  ##  ## ", " ##  ##  ## ", " --  --  -- ", "            ", "############", "            "],
"cooling-system": ["     ##     ", "  #  ##  #  ", "   # ## #   ", "    ++++    ", "####++++####", "    ++++    ", "   # ## #   ", "  #  ##  #  ", "     ##     ", "            "],
"advanced-ammunition": ["     ++     ", "    ++++    ", "    ++++    ", "    ####    ", "    ####    ", "    ####    ", "    ####    ", "   ------   ", "   ######   ", "            "],
"targeting-computer": ["     #      ", "   #####    ", "  #  #  #   ", " #   #   #  ", "#### + ####", " #   #   #  ", "  #  #  #   ", "   #####    ", "     #      ", "            "],
"ballistic-computer": ["      ++++  ", "    ++    + ", "   +       +", "  #        ", " #         ", " #         ", "#    ++    ", "#   ++++   ", "#########  ", "            "],
"quantum-accelerator": ["   ##  ##   ", "  #  ##  #  ", " #  #  #  # ", " # #    # # ", "  #  ++  #  ", "  #  ++  #  ", " # #    # # ", " #  #  #  # ", "  #  ##  #  ", "   ##  ##   "],
"matter-replicator": [" #######    ", " #+++++#    ", " #+---+#    ", " #+-#######", " #+-#+++++#", " ###+#---+#", "    #+---+#", "    #+---+#", "    #######", "            "],
"ancient-efficiency-core": ["   ######   ", "  #      #  ", " #  ++++  # ", " # +    + # ", " # + ## + # ", " # + ## + # ", " # +    + # ", " #  ++++  # ", "  #      #  ", "   ######   "],
"temporal-processing-core": [" ########## ", "  #++++++#  ", "   #++++#   ", "    #++#    ", "     ##     ", "     ##     ", "    #  #    ", "   # -- #   ", "  #------#  ", " ########## "],
}

def family_of(mid):
    for f in sorted(FAMILY, key=len, reverse=True):
        if mid == f or mid.startswith(f + "-"): return f
    raise KeyError(mid)

def module_icon(mid):
    fam = family_of(mid); cat = FAMILY[fam]; col = CATEGORY[cat]
    legendary = cat == "legendary"
    tier = int(mid.rsplit("-", 1)[1]) if mid[-1].isdigit() else SINGLE_TIER.get(fam, 4)
    ic = Icon()
    casing = ic.ramp("a89cc8" if legendary else "8a8f9c")
    pins = ic.ramp("e8c070")
    for y in (9, 14, 19): ic.put(rect(1, y, 3, y + 2), pins, lambda px, py: 1, rim=False)          # connector pins
    body = poly([(6, 3), (26, 3), (29, 6), (29, 26), (26, 29), (6, 29), (3, 26), (3, 6)])
    ic.put(body, casing, lambda px, py: 1 if px + py < 30 else 2)
    band = ic.ramp(col)
    ic.put(rect(6, 4, 25, 6), band, lambda px, py: 1, rim=False)                                     # category band
    win = ic.ramp("23252c")
    ic.put(rect(8, 9, 24, 21), win, lambda px, py: 2, rim=False)                                     # glyph window
    ic.put(rect(8, 9, 24, 9), win, lambda px, py: 4, rim=False)
    gl, gh, gs = ic.letters([col, mix(col, "ffffff", .55), tone(col, -.25)])
    for j, row in enumerate(G[fam]):
        for i, ch in enumerate(row):
            if ch != ' ': ic.px(10 + i, 10 + j, {'#': gl, '+': gh, '-': gs}[ch])
    if legendary:                                                                                    # a gem instead of level pips
        gem = ic.ramp("ffd37f")
        ic.put(poly([(16, 22), (19.5, 25), (16, 28), (12.5, 25)]), gem, lambda px, py: 1 if px < 16 else 2)
    else:                                                                                            # level pips I-V
        on, off = ic.letters([mix(col, "ffffff", .25), "2a2c33"])
        for k in range(5):
            x0 = 7 + k * 4
            for y in (24, 25):
                for x in (x0, x0 + 1, x0 + 2): ic.px(x, y, on if k < tier else off)
    return ic

if __name__ == "__main__":
    for line in open(sys.argv[2]).read().split("\n"):
        if not line.strip(): continue
        name, mid = line.split("|")
        if mid == "zero-point-efficiency-core": mid = "ancient-efficiency-core"
        module_icon(mid).write(mid); print("drafted", mid)
