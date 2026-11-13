package eclipse.content;

import arc.graphics.*;
import eclipse.type.*;
import mindustry.content.*;
import mindustry.type.*;

/** New liquids and gases. Vanilla liquids (water, oil, hydrogen, nitrogen...) are reused, not duplicated. */
public class EclipseLiquids{
    public static Liquid purifiedWater, brine, hydrochloricAcid, oxygen, chlorine,
        methane, naphtha, ethylene, sulfuricAcid, sodiumHydroxide, acidicWaste,
        ammonia, nitricAcid, steam, liquidNitrogen, aquaRegia,
        flueGas, carbonDioxide, wastewater,
        methanol, propylene, acrylonitrile, biofuel, chilledWater;

    public static void load(){
        steam = new Liquid("steam", Color.valueOf("e8edf0")){{
            gas = true;
            temperature = 0.8f;
            coolant = false;
        }};

        // colder and with more heat capacity than cryofluid: the best turret coolant
        liquidNitrogen = new Liquid("liquid-nitrogen", Color.valueOf("bfe3ff")){{
            temperature = 0.05f;
            heatCapacity = 1.1f;
            effect = StatusEffects.freezing;
            lightColor = Color.valueOf("bfe3ff").a(0.3f);
            coolant = true;
        }};

        purifiedWater = new Liquid("purified-water", Color.valueOf("8fd3ff")){{
            heatCapacity = 0.45f;
            effect = StatusEffects.wet;
            coolant = true;
        }};

        brine = new Liquid("brine", Color.valueOf("a7c9b4")){{
            heatCapacity = 0.35f;
            viscosity = 0.6f;
            effect = StatusEffects.wet;
            coolant = false;
        }};

        hydrochloricAcid = new CorrosiveLiquid("hydrochloric-acid", Color.valueOf("dfe882")){{
            viscosity = 0.5f;
            effect = StatusEffects.corroded;
            coolant = false;
        }};

        oxygen = new Liquid("oxygen", Color.valueOf("d8efff")){{
            gas = true;
            barColor = Color.valueOf("a9d8ff");
            coolant = false;
        }};

        chlorine = new CorrosiveLiquid("chlorine", Color.valueOf("b6dc4c")){{
            gas = true;
            effect = StatusEffects.corroded;
            coolant = false;
        }};

        methane = new Liquid("methane", Color.valueOf("c4b3e6")){{
            gas = true;
            flammability = 1f;
            explosiveness = 0.6f;
            coolant = false;
        }};

        naphtha = new Liquid("naphtha", Color.valueOf("d9a45a")){{
            flammability = 1.1f;
            explosiveness = 0.5f;
            viscosity = 0.65f;
            effect = StatusEffects.tarred;
            coolant = false;
        }};

        ethylene = new Liquid("ethylene", Color.valueOf("aee3cf")){{
            gas = true;
            flammability = 1f;
            explosiveness = 0.8f;
            coolant = false;
        }};

        sulfuricAcid = new CorrosiveLiquid("sulfuric-acid", Color.valueOf("e8b84a")){{
            viscosity = 0.7f;
            effect = StatusEffects.corroded;
            coolant = false;
        }};

        sodiumHydroxide = new CorrosiveLiquid("sodium-hydroxide", Color.valueOf("d8dff0")){{
            viscosity = 0.55f;
            effect = StatusEffects.corroded;
            coolant = false;
        }};

        acidicWaste = new CorrosiveLiquid("acidic-waste", Color.valueOf("8f8a3a")){{
            viscosity = 0.75f;
            effect = StatusEffects.corroded;
            coolant = false;
        }};

        ammonia = new Liquid("ammonia", Color.valueOf("c9e6f2")){{
            gas = true;
            flammability = 0.2f;
            effect = StatusEffects.corroded;
            coolant = false;
        }};

        nitricAcid = new CorrosiveLiquid("nitric-acid", Color.valueOf("f0d98c")){{
            viscosity = 0.6f;
            effect = StatusEffects.corroded;
            coolant = false;
        }};

        // HCl + HNO3: the only acid that dissolves platinum
        aquaRegia = new CorrosiveLiquid("aqua-regia", Color.valueOf("e8873a")){{
            viscosity = 0.6f;
            effect = StatusEffects.corroded;
            coolant = false;
        }};

        // ---- waste streams ----

        // exhaust from burning methane: mostly nitrogen and CO2, with sulfur dioxide. Vented when not piped away.
        flueGas = new Liquid("flue-gas", Color.valueOf("8a8580")){{
            gas = true;
            temperature = 0.7f;
            coolant = false;
        }};

        carbonDioxide = new Liquid("carbon-dioxide", Color.valueOf("c8ccd0")){{
            gas = true;
            coolant = false;
        }};

        // dirty process water from scrubbing
        wastewater = new Liquid("wastewater", Color.valueOf("6f7f5a")){{
            viscosity = 0.6f;
            coolant = false;
        }};
    
        // ---- organic chemistry ----
        methanol = new Liquid("methanol", Color.valueOf("d6e4f0")){{
            flammability = 0.9f;
            explosiveness = 0.3f;
            coolant = false;
        }};

        propylene = new Liquid("propylene", Color.valueOf("e6d3a8")){{
            gas = true;
            flammability = 1f;
            explosiveness = 0.8f;
            coolant = false;
        }};

        // toxic monomer for carbon fiber; burns readily
        acrylonitrile = new Liquid("acrylonitrile", Color.valueOf("c9b8e8")){{
            flammability = 0.8f;
            explosiveness = 0.4f;
            effect = StatusEffects.corroded;
            coolant = false;
        }};
    
        // ---- biology ----
        //fermented ethanol
        biofuel = new Liquid("biofuel", Color.valueOf("f0e6a0")){{
            flammability = 1f;
            explosiveness = 0.4f;
            coolant = false;
        }};
    
        // ---- cooling ----
        //evaporatively cooled water: a cheap coolant between water and cryofluid
        chilledWater = new Liquid("chilled-water", Color.valueOf("7fd8ff")){{
            temperature = 0.25f;
            heatCapacity = 0.6f;
            effect = StatusEffects.wet;
            coolant = true;
        }};
    }
}
