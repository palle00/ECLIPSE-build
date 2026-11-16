package eclipse.content;

import arc.graphics.*;
import mindustry.type.*;

public class EclipseItems{
    public static Item sulfur, polyethylene, leadAcidCell, polymerCatalyst, ammoniumNitrate,
        nickel, chromium, cobalt, tungsten, lithium, superalloy, tungstenCarbide,
        alumina, aluminium, highPuritySilicon, microchip, reinforcedGlass, surgeSuperalloy,
        hotSuperalloy, hotTungstenCarbide, hotAluminium, hotSurgeSuperalloy, hotTitanium, hotSilicon, hotMetaglass, hotReinforcedGlass,
        molybdenum, vanadium, platinum, thoriumFuelRod, spentFuelRod, industrialSludge,
        syntheticRubber, epoxyResin, carbonFiber, carbonComposite,
        biomass, fertilizer, enzymeExtract,
        copperGear, titaniumRod, titaniumGear, pressureCasing, industrialBearing, superalloyFrame, precisionComponent, experimentalCore,
        copperWire, circuitBoard, controlUnit, alnicoMagnet, crushedOre,
        aetherite, superconductor,
        copperPlate, leadPlate, titaniumPlate, aluminiumPlate, superalloyPlate, tungstenPlate, copperFoil, aluminiumFoil,
        tungstenRod, aluminiumWire, titaniumBolt, titaniumScrew, titaniumRing, titaniumSpring, superalloyGear,
        magnetWire, resistor, capacitor, transistor, insulatedCable,
        electricMotor, permanentMagnetMotor, superconductingMotor, pumpUnit, piston, rotor, heatingCoil, sensor, emitter,
        sodaAsh, petroleumCoke, magnesium, bauxite, redMud;

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

        // ---- hot metals: fresh out of a furnace, cooled in the vacuum freezer before they can be used ----
        hotSuperalloy = new Item("hot-superalloy", Color.valueOf("ff9a4a")){{
            cost = 1.6f;
        }};

        hotTungstenCarbide = new Item("hot-tungsten-carbide", Color.valueOf("e0623a")){{
            cost = 1.6f;
        }};

        hotAluminium = new Item("hot-aluminium", Color.valueOf("ffb27a")){{
            cost = 1.1f;
        }};

        hotSurgeSuperalloy = new Item("hot-surge-superalloy", Color.valueOf("ffc04a")){{
            cost = 2f;
            charge = 0.5f;
        }};

        //tier II: only the ECLIPSE routes make these hot; vanilla titanium, silicon and metaglass stay as they are
        hotTitanium = new Item("hot-titanium", Color.valueOf("e8906a")){{
            cost = 1f;
        }};

        hotSilicon = new Item("hot-silicon", Color.valueOf("c8704a")){{
            cost = 0.8f;
        }};

        hotMetaglass = new Item("hot-metaglass", Color.valueOf("ffc89a")){{
            cost = 1.5f;
        }};

        hotReinforcedGlass = new Item("hot-reinforced-glass", Color.valueOf("f0aa7a")){{
            cost = 1.2f;
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

        // ---- forms and small parts (Metal Press, Extruder, Lathe, Gear Press): only what a part or component needs ----
        copperPlate = new Item("copper-plate", Color.valueOf("d99d73")){{ cost = 0.6f; }};
        leadPlate = new Item("lead-plate", Color.valueOf("8c7fa9")){{ cost = 0.6f; }};
        titaniumPlate = new Item("titanium-plate", Color.valueOf("8da1e3")){{ cost = 1.1f; }};
        aluminiumPlate = new Item("aluminium-plate", Color.valueOf("b9c4cf")){{ cost = 1.2f; }};
        superalloyPlate = new Item("superalloy-plate", Color.valueOf("cdb98a")){{ cost = 1.8f; }};
        tungstenPlate = new Item("tungsten-plate", Color.valueOf("8a8c96")){{ cost = 1.6f; }};
        copperFoil = new Item("copper-foil", Color.valueOf("e8a070")){{ cost = 0.6f; }};
        aluminiumFoil = new Item("aluminium-foil", Color.valueOf("d6dde3")){{ cost = 1.2f; }};
        tungstenRod = new Item("tungsten-rod", Color.valueOf("8a8c96")){{ cost = 1.5f; }};
        aluminiumWire = new Item("aluminium-wire", Color.valueOf("c9d2da")){{ cost = 1.1f; }};
        titaniumBolt = new Item("titanium-bolt", Color.valueOf("8da1e3")){{ cost = 1f; }};
        titaniumScrew = new Item("titanium-screw", Color.valueOf("a0b2ec")){{ cost = 1f; }};
        titaniumRing = new Item("titanium-ring", Color.valueOf("8da1e3")){{ cost = 1f; }};
        titaniumSpring = new Item("titanium-spring", Color.valueOf("9aaee8")){{ cost = 1f; }};
        superalloyGear = new Item("superalloy-gear", Color.valueOf("cdb98a")){{ cost = 1.8f; }};

        // ---- electronic parts (Circuit Assembler) and magnet wire (Extruder) ----
        magnetWire = new Item("magnet-wire", Color.valueOf("c4703a")){{ cost = 1f; }};
        resistor = new Item("resistor", Color.valueOf("d8c08a")){{ cost = 0.8f; }};
        capacitor = new Item("capacitor", Color.valueOf("5a7ab0")){{ cost = 1.2f; }};
        transistor = new Item("transistor", Color.valueOf("3a3c44")){{ cost = 1.4f; }};
        insulatedCable = new Item("insulated-cable", Color.valueOf("33333b")){{ cost = 1f; }};

        // ---- parts (Assembler): what machines are built from ----
        electricMotor = new Item("electric-motor", Color.valueOf("7a8a9c")){{ cost = 1.6f; }};
        permanentMagnetMotor = new Item("permanent-magnet-motor", Color.valueOf("b05454")){{ cost = 2.2f; }};
        superconductingMotor = new Item("superconducting-motor", Color.valueOf("8fd0ff")){{ cost = 3.5f; }};
        pumpUnit = new Item("pump-unit", Color.valueOf("5f8fb0")){{ cost = 1.8f; }};
        piston = new Item("piston", Color.valueOf("9aa3ad")){{ cost = 1.6f; }};
        rotor = new Item("rotor", Color.valueOf("8da1e3")){{ cost = 1.4f; }};
        heatingCoil = new Item("heating-coil", Color.valueOf("ff8a4a")){{ cost = 1.8f; }};
        sensor = new Item("sensor", Color.valueOf("6dd0f0")){{ cost = 2f; }};
        emitter = new Item("emitter", Color.valueOf("ff5a5a")){{ cost = 2.6f; }};

        // ---- realistic chemistry ----
        //sodium carbonate: sodium hydroxide carbonated with captured CO2; the flux of soda-lime glass, and lithium's precipitant
        sodaAsh = new Item("soda-ash", Color.valueOf("f2f0ea")){{ cost = 0.6f; }};
        //the heavy residue of crude oil, coked: graphitized into graphite, and the anodes of aluminium cells
        petroleumCoke = new Item("petroleum-coke", Color.valueOf("33333a")){{ cost = 0.7f; flammability = 0.8f; }};
        //won from brine (Dow process): reduces titanium chloride to titanium (Kroll), and burns fiercely
        magnesium = new Item("magnesium", Color.valueOf("d2d7dc")){{ cost = 1f; flammability = 0.6f; explosiveness = 0.2f; }};
        //aluminium ore (gibbsite and boehmite): refined into alumina with hot sodium hydroxide (Bayer)
        bauxite = new Item("bauxite", Color.valueOf("c4875a")){{ hardness = 2; cost = 0.7f; }};
        //the Bayer residue: iron and titanium oxides; must be taken away, and its titanium can be smelted out
        redMud = new Item("red-mud", Color.valueOf("9c4632")){{ cost = 0.3f; }};
    }
}
