# Sprite drafts

Python scripts that draw the hand-made pixel sprites as letter grids into `art/pixel/NAME.txt`
(then `tools/PixelArt.java` turns those into `assets/sprites/*.png`).

- `draft.py` turret helpers + Cloudbreaker/Heat Lance/Tremor; `draft2.py` 3x3 turrets; `draft3.py` 2x2 turrets;
  `draft4.py` Jammer/Sonar/Rivet
- `unitlib.py` unit helpers (faceted plates, mirror, `scaled()` re-render); `draft7.py` Rampart/Valkyrie/Cinderwake/Delta/Epsilon;
  `draft8.py` the 17 high units; `draft9.py` Annihilator/Tempest/Abyss/Behemoth/Maelstrom; `draft10.py` Colossus/Leviathan/Dreadnought
- `blocklib.py` block helpers (diagonal split, grooved parts); `draft11.py` 3x3 support blocks; `draft12.py` 3x3 crafters; `draft13.py` distillation, heat exchanger, steam cracker, carbon fibre furnace, reprocessing;
  `draft14-18.py` the other 3x3 machines (rows 75-97); `smalllib.py` 2x2 helpers, `draft19-24.py` the 2x2 machines (rows 98-131)
- Every machine draft also writes its moving layers (-rotator, -press, small -fan/-spool/... parts, glow masks); the block code
  adds the drawers. `anim_preview.py OUT_SPRITES build/stage/sprites out.png spec.py` shows the layers at three moments.

Run: `python -I art/drafts/draftN.py <out dir>` - write into a scratch dir first and copy only the grids you mean to change:
some approved grids were hand-adjusted after drafting (draft.py / draft2-4 regenerate their whole batch).
- Item icons (32x32): `itemlib.py` (bevelled shapes, 5-tone ramps from the item colour, vanilla item style) and
  `draft27.py` metals + hot forms, `draft28.py` minerals/powders/chemicals/organics, `draft29.py` parts and electronics.
- Logistics: `draft25.py` walls + 2x2 logistics, `draft26.py` 1x1 couplers/routing/valves, `conveyors.py` belt tiers
  (needs vanilla titanium-conveyor frames dumped from the game atlas).
- `modules.py` all 70 upgrade modules from ONE base cartridge (category band, family glyph, level pips; legendary = gem);
  `fluids.py` liquids (drops/blobs/splashes) and gases (vanilla swirl); `ores.py` ore overlays -> art/pixel-ores, rendered
  into assets/sprites/blocks/environment (NOT top level: the generator puts ores there), then `ores.py DIR --shadow`.
- `draft31.py` Bombard / Event Horizon / Payload Catapult / Orbital Weapons Platform + World Engine (eclipse centre, 5 stages).
- `draft32.py` the big blocks: Core: Bastion + Citadel (with -team, -glow), Titan Complex (-top, -crane), Fusion Reactor,
  Temporal Array (-ring-a/-ring-b), Chemical Synthesis Complex, Experimental Augmentation Facility (-arm-l/-arm-r),
  Combined-Cycle Turbine (-rotor, -flame) and Breeder Reactor (-core, -pump); both heat makers get -top1/-top2/-heat/-glow.
- `draft33.py` status icons (white silhouettes, the game tints them) and the Minelayer mine (+ mine-back).
- `bottoms.py OUT name:size...` tank floors ("-bottom") clipped to the machine's outline; needs the machine's PNG in
  assets/sprites first. Run it without `python -I` (it uses Pillow from the user site).
- `helpers.py OUT` helper layers over finished machines: "-heat" (heat funnel from the outlet edge), heat makers' "-glow"
  (their hot part), liquid output couplings "-<liquid>-output1/2". Needs the machines' PNGs in assets/sprites; no `-I`.
- `logo.py` the ECLIPSE logo (sprite "logo") + "logo-glow" (the eclipse C's corona, pulsed by MenuBackdrop).
- `terrain.py` (+ `terrainlib.py`) the three planets' own terrain -> `art/pixel-terrain`, rendered into
  `assets/sprites/blocks/environment` (floors must be on the environment page): land floors (3 variants, every shape inside
  its tile like vanilla), single wrapping liquid tiles, cliff walls (2 variants + `-large`; shapes touching a tile edge are
  shared by all variants so walls join without seams), trees and boulders. Shallow-water blends and floor edges are made
  by the game. Run with `python -I terrain.py OUT [name...]`. Also each planet's oil (tar), heat floors (hot rock / magma
  rock), metal decks (metal floor / dark panels) and metal wall (dark metal), so no Serpulo terrain is left on the planets.
