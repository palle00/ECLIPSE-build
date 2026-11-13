package eclipse.content;

import arc.graphics.*;
import mindustry.type.*;

public class EclipseItems{
    public static Item sulfur, polyethylene, leadAcidCell, polymerCatalyst, ammoniumNitrate,
        nickel, chromium, cobalt, tungsten, lithium, superalloy, tungstenCarbide,
        alumina, aluminium, highPuritySilicon, microchip, reinforcedGlass, surgeSuperalloy,
        molybdenum, vanadium, platinum, thoriumFuelRod, spentFuelRod, industrialSludge,
        syntheticRubber, epoxyResin, carbonFiber, carbonComposite,
        biomass, fertilizer, enzymeExtract,
        copperGear, titaniumRod, titaniumGear, pressureCasing, industrialBearing, superalloyFrame, precisionComponent, experimentalCore,
        copperWire, circuitBoard, controlUnit, alnicoMagnet, crushedOre,
        aetherite, superconductor;

    public static void load(){
        sulfur = new Item("sulfur", Color.valueOf("e8d44d")){{
            flammability = 0.6f;
            explosiveness = 0.2f;
            cost = 1f;
        }};

        polyethylene = new Item("polyethylene", Color.valueOf("e3ecf0")){{
            flammability = 0.3f;
            cost = 1.2f;
        }};

        leadAcidCell = new Item("lead-acid-cell", Color.valueOf("7f9a63")){{
            explosiveness = 0.1f;
            cost = 1.5f;
        }};

        polymerCatalyst = new Item("polymer-catalyst", Color.valueOf("b07ad6")){{
            cost = 1.5f;
        }};

        ammoniumNitrate = new Item("ammonium-nitrate", Color.valueOf("f2efe4")){{
            flammability = 0.2f;
            explosiveness = 0.8f;
            cost = 1f;
        }};

        // ---- metals: mined from ore, or separated from slag ----
        nickel = new Item("nickel", Color.valueOf("b4c2a6")){{
            hardness = 2;
            cost = 0.8f;
        }};

        chromium = new Item("chromium", Color.valueOf("c3cedb")){{
            hardness = 3;
            cost = 1f;
        }};

        cobalt = new Item("cobalt", Color.valueOf("4f6fd1")){{
            hardness = 3;
            cost = 1.1f;
        }};

        tungsten = new Item("tungsten", Color.valueOf("8a8c96")){{
            hardness = 4;
            cost = 1.4f;
        }};

        // extracted from brine, not mined
        lithium = new Item("lithium", Color.valueOf("d9cfe9")){{
            flammability = 0.4f;
            explosiveness = 0.3f;
            cost = 1.2f;
        }};

        // ---- alloys ----
        superalloy = new Item("superalloy", Color.valueOf("cdb98a")){{
            cost = 1.6f;
        }};

        tungstenCarbide = new Item("tungsten-carbide", Color.valueOf("50525e")){{
            cost = 1.6f;
        }};

        // ---- aluminium ----
        alumina = new Item("alumina", Color.valueOf("ece8e2")){{
            cost = 0.8f;
        }};

        aluminium = new Item("aluminium", Color.valueOf("b9c4cf")){{
            cost = 1.1f;
        }};

        // ---- electronics ----
        highPuritySilicon = new Item("high-purity-silicon", Color.valueOf("6d7a99")){{
            cost = 1.2f;
        }};

        microchip = new Item("microchip", Color.valueOf("3f9a5c")){{
            cost = 1.6f;
        }};

        // ---- advanced vanilla combinations ----
        reinforcedGlass = new Item("reinforced-glass", Color.valueOf("9fc7d6")){{
            cost = 1.2f;
        }};

        surgeSuperalloy = new Item("surge-superalloy", Color.valueOf("f2c45a")){{
            cost = 2f;
            charge = 0.5f;
        }};

        // ---- more metals ----
        molybdenum = new Item("molybdenum", Color.valueOf("8c9bb0")){{
            hardness = 3;
            cost = 1.1f;
        }};

        // only recovered from slag
        vanadium = new Item("vanadium", Color.valueOf("7a5fa8")){{
            cost = 1.2f;
        }};

        platinum = new Item("platinum", Color.valueOf("e4e6ea")){{
            hardness = 4;
            cost = 1.8f;
        }};

        // ---- nuclear ----
        thoriumFuelRod = new Item("thorium-fuel-rod", Color.valueOf("c96fa0")){{
            radioactivity = 1.2f;
            explosiveness = 0.3f;
            cost = 2f;
        }};

        spentFuelRod = new Item("spent-fuel-rod", Color.valueOf("6d5a63")){{
            radioactivity = 0.8f;
            cost = 1f;
        }};

        // ---- waste ----
        industrialSludge = new Item("industrial-sludge", Color.valueOf("5c5140")){{
            cost = 0.5f;
        }};
    
        // ---- organic chemistry and composites ----
        syntheticRubber = new Item("synthetic-rubber", Color.valueOf("3a3a40")){{
            flammability = 0.4f;
            cost = 1.3f;
        }};

        epoxyResin = new Item("epoxy-resin", Color.valueOf("e0a94a")){{
            flammability = 0.3f;
            cost = 1.3f;
        }};

        carbonFiber = new Item("carbon-fiber", Color.valueOf("2b2d33")){{
            cost = 1.6f;
        }};

        carbonComposite = new Item("carbon-composite", Color.valueOf("4a5260")){{
            cost = 2f;
        }};
    
        // ---- biology ----
        //burns in vanilla combustion generators
        biomass = new Item("biomass", Color.valueOf("6fae4a")){{
            flammability = 0.8f;
            cost = 0.6f;
        }};

        fertilizer = new Item("fertilizer", Color.valueOf("e8e2c8")){{
            cost = 0.8f;
        }};

        enzymeExtract = new Item("enzyme-extract", Color.valueOf("e86fd0")){{
            cost = 1.4f;
        }};
    
        // ---- mechanical components (made in ComponentBlocks) ----
        copperGear = new Item("copper-gear", Color.valueOf("d99d73")){{ cost = 0.6f; }};
        titaniumRod = new Item("titanium-rod", Color.valueOf("8da1e3")){{ cost = 1f; }};
        titaniumGear = new Item("titanium-gear", Color.valueOf("8da1e3")){{ cost = 1.2f; }};
        pressureCasing = new Item("pressure-casing", Color.valueOf("9ab0b8")){{ cost = 1.5f; }};
        industrialBearing = new Item("industrial-bearing", Color.valueOf("b8bcc4")){{ cost = 1.4f; }};
        superalloyFrame = new Item("superalloy-frame", Color.valueOf("cdb98a")){{ cost = 2f; }};
        precisionComponent = new Item("precision-component", Color.valueOf("6dd0a0")){{ cost = 2f; }};
        experimentalCore = new Item("experimental-core", Color.valueOf("c88fff")){{ cost = 3f; }};
    
        // ---- electronics ----
        copperWire = new Item("copper-wire", Color.valueOf("e8a070")){{ cost = 0.5f; }};
        circuitBoard = new Item("circuit-board", Color.valueOf("3f9a5c")){{ cost = 1.3f; }};
        controlUnit = new Item("control-unit", Color.valueOf("5ab8ff")){{ cost = 2.2f; }};

        // ---- metallurgy ----
        //aluminium-nickel-cobalt permanent magnet alloy, cast in the induction furnace
        alnicoMagnet = new Item("alnico-magnet", Color.valueOf("c04a4a")){{ cost = 1.6f; }};
        crushedOre = new Item("crushed-ore", Color.valueOf("8a7f72")){{ cost = 0.5f; }};
    
        // ---- aetherite ----
        //rare energy crystal: deep ore far from the core, or a rare ore-washer result
        aetherite = new Item("aetherite", Color.valueOf("7fe0ff")){{ cost = 2.5f; hardness = 4; radioactivity = 0.1f; }};
    
        // ---- endgame ----
        //made in the Chemical Synthesis Complex; consumed by the World Engine and the Temporal Interference Array
        superconductor = new Item("superconductor", Color.valueOf("8fd0ff")){{ cost = 3f; }};
    }
}
