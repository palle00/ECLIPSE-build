package eclipse.content;

import arc.func.*;
import arc.graphics.*;
import arc.util.*;
import eclipse.augment.*;
import eclipse.world.blocks.*;
import eclipse.augment.AugmentModule.*;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.heat.*;
import mindustry.world.blocks.production.*;
import mindustry.world.consumers.*;

import static eclipse.content.EclipseItems.*;
import static mindustry.type.ItemStack.*;

/** Augmentation system: couplers, module assemblers and modules (phase 1: speed and power; phase 2: efficiency and multi-slot). */
public class AugmentContent{
    public static AugmentModule productionAccelerator1, productionAccelerator2, productionAccelerator3,
        powerOptimizer1, powerOptimizer2, powerOptimizer3,
        outputAmplifier1, outputAmplifier2, outputAmplifier3,
        precisionFabricator1, precisionFabricator2, precisionFabricator3,
        materialRecycler1, materialRecycler2,
        reactionAccelerator1, reactionAccelerator2, reactionAccelerator3,
        thermalController1, thermalController2, thermalController3,
        pressureStabilizer1, pressureStabilizer2,
        catalystOptimizer1, catalystOptimizer2,
        purificationModule1, purificationModule2, purificationModule3,
        wasteRecovery1, wasteRecovery2,
        //phase 4: energy & mining
        thermalRecovery1, thermalRecovery2, thermalRecovery3, energyOvercharger1, energyOvercharger2, capacitorModule1, capacitorModule2,
        deepMining1, deepMining2, deepMining3, advancedDrillHead1, advancedDrillHead2, geologicalAnalyzer,
        //phase 5: military
        rapidFireController1, rapidFireController2, rapidFireController3, coolingSystem1, coolingSystem2,
        advancedAmmunition1, advancedAmmunition2, advancedAmmunition3, targetingComputer1, targetingComputer2, ballisticComputer,
        //phase 6: tier IV, configurations, legendary
        productionAccelerator4, powerOptimizer4, outputAmplifier4, precisionFabricator4, reactionAccelerator4, deepMining4,
        productionAccelerator5, powerOptimizer5, outputAmplifier5, deepMining5,
        massProduction, closedLoopProcessing, emergencyOverdrive,
        quantumAccelerator, matterReplicator, zeroPointEfficiencyCore, temporalProcessingCore;
    public static Block augmentationCoupler, advancedCoupler, augmentationHub, chemicalInterface, militaryInterface,
        basicModuleAssembler, advancedModuleAssembler, experimentalFacility, moduleRecycler;

    /** Hot machines whose waste heat can be recovered: heat producers, heat-driven machines and smelters. */
    public static final Boolf<Block> hotMachines = b -> b instanceof HeatProducer || b instanceof HeatCrafter
        || Polish.smelters.contains(b); //smelters, recorded by their original smelting smoke before Polish changes effects

    public static final Boolf<Block> drills = b -> b instanceof Drill;

    public static final Boolf<Block> turrets = b -> b instanceof Turret;

    /** Chemical machines: crafters that consume or produce liquids (vanilla and ECLIPSE). */
    public static final Boolf<Block> chemicalMachines = b -> b instanceof GenericCrafter && b.hasLiquids;

    /** Heat-driven machines. */
    public static final Boolf<Block> heatConsumers = b -> b instanceof HeatCrafter;

    public static final Boolf<Block> pressurized = b -> b instanceof ChemicalReactor r && r.pressureRequired > 0f;

    public static final Boolf<Block> catalytic = b -> b instanceof ChemicalReactor r && r.catalyst != null;

    /** Crafters with a product liquid (waste is not a product). */
    public static final Boolf<Block> liquidProducers = b -> b instanceof GenericCrafter g && AugmentationCoupler.primaryLiquid(g) != null;

    /** Reactors that turn an acid into acidic waste (recovery happens inside ChemicalReactor as the waste is produced). */
    public static final Boolf<Block> wasteProducers = b -> b instanceof ChemicalReactor g && g.outputLiquids != null
        && Structs.contains(g.outputLiquids, s -> AugmentationCoupler.wasteLiquids.contains(s.liquid));

    /** Machines with item output per cycle: item crafters (vanilla and ECLIPSE) and drills. */
    public static final Boolf<Block> itemProducers = b ->
        (b instanceof GenericCrafter g && g.outputItems != null) || b instanceof Drill;

    /** Crafters that consume items each cycle. */
    public static final Boolf<Block> itemConsumers = b ->
        b instanceof GenericCrafter && b.findConsumer(c -> c instanceof ConsumeItems) != null;

    /** Machines that make things: crafters (vanilla and ECLIPSE), separators, drills and solid pumps. */
    public static final Boolf<Block> producers = b ->
        b instanceof GenericCrafter || b instanceof Separator || b instanceof Drill || b instanceof SolidPump;

    /** Machines with a normal (non-battery) power draw. */
    public static final Boolf<Block> powered = b ->
        b.consPower != null && !b.consPower.buffered && !(b instanceof AugmentationCoupler);

    public static void load(){
        productionAccelerator1 = accelerator(1, 0.10f, 0.25f, 0.3f, 360f, with(Items.silicon, 20, Items.lead, 20, Items.graphite, 10));
        productionAccelerator2 = accelerator(2, 0.20f, 0.5f, 0.5f, 600f, with(microchip, 5, aluminium, 10, Items.silicon, 20));
        productionAccelerator3 = accelerator(3, 0.35f, 1f, 0.8f, 900f, with(microchip, 10, superalloy, 10, Items.surgeAlloy, 5));

        powerOptimizer1 = optimizer(1, 0.10f, 360f, with(Items.silicon, 15, Items.metaglass, 10, Items.lead, 20));
        powerOptimizer2 = optimizer(2, 0.20f, 600f, with(microchip, 5, lithium, 10, aluminium, 10));
        powerOptimizer3 = optimizer(3, 0.30f, 900f, with(microchip, 10, platinum, 5, vanadium, 10));

        // ---- phase 2: efficiency modules. Amplifier and Fabricator are mutually exclusive on one machine. ----
        outputAmplifier1 = amplifier(1, 0.10f, 0.10f, 0.3f, 360f, with(Items.silicon, 20, Items.titanium, 15, Items.graphite, 15));
        outputAmplifier2 = amplifier(2, 0.20f, 0.15f, 0.6f, 600f, with(microchip, 8, superalloy, 5, aluminium, 10));
        outputAmplifier3 = amplifier(3, 0.30f, 0.20f, 1f, 900f, with(microchip, 12, surgeSuperalloy, 3, platinum, 5));

        precisionFabricator1 = fabricator(1, 0.10f, 0.3f, 360f, with(Items.silicon, 20, Items.copper, 20, Items.metaglass, 10));
        precisionFabricator2 = fabricator(2, 0.20f, 0.6f, 600f, with(microchip, 8, tungstenCarbide, 5, chromium, 10));
        precisionFabricator3 = fabricator(3, 0.30f, 1f, 900f, with(microchip, 12, tungstenCarbide, 10, highPuritySilicon, 10));

        materialRecycler1 = recycler(1, 0.10f, 0.2f, 360f, with(Items.silicon, 15, Items.lead, 20, Items.titanium, 10));
        materialRecycler2 = recycler(2, 0.20f, 0.4f, 600f, with(microchip, 6, aluminium, 10, nickel, 10));

        // ---- phase 3: chemical modules ----
        AugmentationCoupler.wasteLiquids.add(EclipseLiquids.acidicWaste);

        reactionAccelerator1 = chemical("reaction-accelerator", 1, 0.3f, 360f, with(polyethylene, 10, Items.silicon, 15, Items.metaglass, 10), m -> {
            m.speedBonus = 0.15f;
            m.coolantUse = 3f / 60f;
            m.compatible = chemicalMachines;
            m.compatibleKey = "chemical";
        });
        reactionAccelerator2 = chemical("reaction-accelerator", 2, 0.6f, 600f, with(microchip, 6, reinforcedGlass, 8, polyethylene, 10), m -> {
            m.speedBonus = 0.30f;
            m.coolantUse = 6f / 60f;
            m.compatible = chemicalMachines;
            m.compatibleKey = "chemical";
        });
        reactionAccelerator3 = chemical("reaction-accelerator", 3, 1f, 900f, with(microchip, 10, platinum, 5, superalloy, 8), m -> {
            m.speedBonus = 0.50f;
            m.coolantUse = 9f / 60f;
            m.compatible = chemicalMachines;
            m.compatibleKey = "chemical";
        });

        thermalController1 = chemical("thermal-controller", 1, 0.5f, 360f, with(Items.copper, 20, Items.graphite, 15, Items.silicon, 10), m -> {
            m.thermalSupply = 0.15f;
            m.compatible = heatConsumers;
            m.compatibleKey = "heat";
        });
        thermalController2 = chemical("thermal-controller", 2, 1f, 600f, with(microchip, 5, molybdenum, 10, aluminium, 10), m -> {
            m.thermalSupply = 0.30f;
            m.compatible = heatConsumers;
            m.compatibleKey = "heat";
        });
        thermalController3 = chemical("thermal-controller", 3, 1.5f, 900f, with(microchip, 10, molybdenum, 15, tungstenCarbide, 5), m -> {
            m.thermalSupply = 0.45f;
            m.compatible = heatConsumers;
            m.compatibleKey = "heat";
        });

        pressureStabilizer1 = chemical("pressure-stabilizer", 1, 0.2f, 360f, with(Items.titanium, 15, reinforcedGlass, 10, Items.silicon, 10), m -> {
            m.pressureBoost = 0.5f;
            m.compatible = pressurized;
            m.compatibleKey = "pressurized";
        });
        pressureStabilizer2 = chemical("pressure-stabilizer", 2, 0.4f, 600f, with(microchip, 6, superalloy, 8, reinforcedGlass, 10), m -> {
            m.pressureBoost = 1f;
            m.compatible = pressurized;
            m.compatibleKey = "pressurized";
        });

        catalystOptimizer1 = chemical("catalyst-optimizer", 1, 0.2f, 360f, with(platinum, 3, Items.silicon, 15, polyethylene, 10), m -> {
            m.catalystLife = 0.5f;
            m.compatible = catalytic;
            m.compatibleKey = "catalytic";
        });
        catalystOptimizer2 = chemical("catalyst-optimizer", 2, 0.4f, 600f, with(microchip, 6, platinum, 6, highPuritySilicon, 5), m -> {
            m.catalystLife = 1f;
            m.compatible = catalytic;
            m.compatibleKey = "catalytic";
        });

        purificationModule1 = chemical("purification-module", 1, 0.3f, 360f, with(polyethylene, 10, Items.metaglass, 15, Items.silicon, 10), m -> {
            m.liquidYield = 0.10f;
            m.compatible = liquidProducers;
            m.compatibleKey = "liquid-producers";
        });
        purificationModule2 = chemical("purification-module", 2, 0.6f, 600f, with(microchip, 5, reinforcedGlass, 10, polyethylene, 10), m -> {
            m.liquidYield = 0.20f;
            m.compatible = liquidProducers;
            m.compatibleKey = "liquid-producers";
        });
        purificationModule3 = chemical("purification-module", 3, 1f, 900f, with(microchip, 10, platinum, 4, reinforcedGlass, 10), m -> {
            m.liquidYield = 0.30f;
            m.compatible = liquidProducers;
            m.compatibleKey = "liquid-producers";
        });

        wasteRecovery1 = chemical("waste-recovery", 1, 0.3f, 360f, with(polyethylene, 15, Items.titanium, 10, Items.silicon, 10), m -> {
            m.wasteRecovery = 0.25f;
            m.compatible = wasteProducers;
            m.compatibleKey = "waste-producers";
        });
        wasteRecovery2 = chemical("waste-recovery", 2, 0.6f, 600f, with(microchip, 6, polyethylene, 15, platinum, 3), m -> {
            m.wasteRecovery = 0.50f;
            m.compatible = wasteProducers;
            m.compatibleKey = "waste-producers";
        });

        // ---- phase 4: energy ----
        thermalRecovery1 = module("thermal-recovery-1", ModuleCategory.energy, 1, 0f, 360f, with(Items.copper, 20, Items.graphite, 15, Items.metaglass, 10),
            m -> { m.thermalRecovery = 0.10f; m.compatible = hotMachines; m.compatibleKey = "hot"; });
        thermalRecovery2 = module("thermal-recovery-2", ModuleCategory.energy, 2, 0f, 600f, with(microchip, 5, aluminium, 10, Items.copper, 20),
            m -> { m.thermalRecovery = 0.20f; m.compatible = hotMachines; m.compatibleKey = "hot"; });
        thermalRecovery3 = module("thermal-recovery-3", ModuleCategory.energy, 3, 0f, 900f, with(microchip, 10, vanadium, 8, superalloy, 8),
            m -> { m.thermalRecovery = 0.30f; m.compatible = hotMachines; m.compatibleKey = "hot"; });
        energyOvercharger1 = module("energy-overcharger-1", ModuleCategory.energy, 1, 0.8f, 360f, with(lithium, 10, Items.silicon, 20, Items.graphite, 10),
            m -> { m.overchargeBonus = 0.6f; m.compatible = producers; m.compatibleKey = "production"; });
        energyOvercharger2 = module("energy-overcharger-2", ModuleCategory.energy, 2, 1.5f, 600f, with(microchip, 8, lithium, 15, Items.surgeAlloy, 5),
            m -> { m.overchargeBonus = 1f; m.compatible = producers; m.compatibleKey = "production"; });
        capacitorModule1 = module("capacitor-module-1", ModuleCategory.energy, 1, 0f, 360f, with(leadAcidCell, 5, Items.silicon, 15, Items.copper, 20),
            m -> { m.capacitorCapacity = 2000f; m.compatible = powered; m.compatibleKey = "powered"; });
        capacitorModule2 = module("capacitor-module-2", ModuleCategory.energy, 2, 0f, 600f, with(lithium, 15, microchip, 5, vanadium, 5),
            m -> { m.capacitorCapacity = 6000f; m.compatible = powered; m.compatibleKey = "powered"; });

        // ---- phase 4: mining ----
        deepMining1 = module("deep-mining-1", ModuleCategory.mining, 1, 0.4f, 360f, with(Items.graphite, 15, Items.silicon, 15, Items.titanium, 10),
            m -> { m.speedBonus = 0.2f; m.compatible = drills; m.compatibleKey = "drills"; });
        deepMining2 = module("deep-mining-2", ModuleCategory.mining, 2, 0.8f, 600f, with(tungstenCarbide, 5, microchip, 5, Items.titanium, 15),
            m -> { m.speedBonus = 0.4f; m.compatible = drills; m.compatibleKey = "drills"; });
        deepMining3 = module("deep-mining-3", ModuleCategory.mining, 3, 1.2f, 900f, with(tungstenCarbide, 10, microchip, 10, superalloy, 8),
            m -> { m.speedBonus = 0.6f; m.compatible = drills; m.compatibleKey = "drills"; });
        advancedDrillHead1 = module("advanced-drill-head-1", ModuleCategory.mining, 1, 0.5f, 360f, with(tungstenCarbide, 5, Items.silicon, 15, Items.graphite, 10),
            m -> { m.yieldBonus = 0.15f; m.exclusiveGroup = "efficiency"; m.compatible = drills; m.compatibleKey = "drills"; });
        advancedDrillHead2 = module("advanced-drill-head-2", ModuleCategory.mining, 2, 1f, 600f, with(tungstenCarbide, 12, microchip, 6, chromium, 10),
            m -> { m.yieldBonus = 0.3f; m.exclusiveGroup = "efficiency"; m.compatible = drills; m.compatibleKey = "drills"; });
        geologicalAnalyzer = module("geological-analyzer", ModuleCategory.mining, 1, 0.1f, 360f, with(Items.silicon, 20, Items.lead, 15, Items.metaglass, 10),
            m -> { m.analyzer = true; m.compatible = drills; m.compatibleKey = "drills"; });

        // ---- phase 5: military (vanilla and future turrets) ----
        rapidFireController1 = module("rapid-fire-controller-1", ModuleCategory.military, 1, 0.3f, 360f, with(Items.silicon, 20, Items.graphite, 15, Items.titanium, 10),
            m -> { m.speedBonus = 0.2f; m.compatible = turrets; m.compatibleKey = "turrets"; });
        rapidFireController2 = module("rapid-fire-controller-2", ModuleCategory.military, 2, 0.6f, 600f, with(microchip, 6, superalloy, 6, Items.titanium, 15),
            m -> { m.speedBonus = 0.35f; m.compatible = turrets; m.compatibleKey = "turrets"; });
        rapidFireController3 = module("rapid-fire-controller-3", ModuleCategory.military, 3, 1f, 900f, with(microchip, 10, tungstenCarbide, 8, Items.surgeAlloy, 6),
            m -> { m.speedBonus = 0.5f; m.compatible = turrets; m.compatibleKey = "turrets"; });
        coolingSystem1 = module("cooling-system-1", ModuleCategory.military, 1, 0.2f, 360f, with(Items.metaglass, 15, Items.titanium, 10, Items.silicon, 10),
            m -> { m.coolingBonus = 0.5f; m.compatible = turrets; m.compatibleKey = "turrets"; });
        coolingSystem2 = module("cooling-system-2", ModuleCategory.military, 2, 0.4f, 600f, with(reinforcedGlass, 10, aluminium, 10, microchip, 4),
            m -> { m.coolingBonus = 1f; m.compatible = turrets; m.compatibleKey = "turrets"; });
        advancedAmmunition1 = module("advanced-ammunition-1", ModuleCategory.military, 1, 0.3f, 360f, with(Items.graphite, 20, Items.silicon, 15, Items.titanium, 10),
            m -> { m.damageBonus = 0.1f; m.compatible = turrets; m.compatibleKey = "turrets"; });
        advancedAmmunition2 = module("advanced-ammunition-2", ModuleCategory.military, 2, 0.6f, 600f, with(tungstenCarbide, 6, microchip, 5, Items.thorium, 10),
            m -> { m.damageBonus = 0.2f; m.compatible = turrets; m.compatibleKey = "turrets"; });
        advancedAmmunition3 = module("advanced-ammunition-3", ModuleCategory.military, 3, 1f, 900f, with(tungstenCarbide, 10, microchip, 10, Items.surgeAlloy, 5),
            m -> { m.damageBonus = 0.3f; m.compatible = turrets; m.compatibleKey = "turrets"; });
        targetingComputer1 = module("targeting-computer-1", ModuleCategory.military, 1, 0.2f, 360f, with(Items.silicon, 25, Items.copper, 20, Items.metaglass, 10),
            m -> { m.accuracy = 0.5f; m.compatible = turrets; m.compatibleKey = "turrets"; });
        targetingComputer2 = module("targeting-computer-2", ModuleCategory.military, 2, 0.4f, 600f, with(microchip, 8, highPuritySilicon, 5, aluminium, 10),
            m -> { m.accuracy = 1f; m.compatible = turrets; m.compatibleKey = "turrets"; });
        ballisticComputer = module("ballistic-computer", ModuleCategory.military, 2, 0.4f, 600f, with(microchip, 8, superalloy, 6, Items.titanium, 15),
            m -> { m.projectileSpeed = 0.5f; m.compatible = turrets; m.compatibleKey = "turrets"; });

        // ---- tier V: aetherite-powered, made in the Aetherite Module Forge ----
        productionAccelerator5 = module("production-accelerator-5", ModuleCategory.production, 5, 2f, 1200f, with(aetherite, 15, Items.phaseFabric, 10, microchip, 20),
            m -> { m.speedBonus = 0.7f; m.relativePowerCost = 0.8f; m.compatible = producers; m.compatibleKey = "production"; });
        powerOptimizer5 = module("power-optimizer-5", ModuleCategory.energy, 5, 0f, 1200f, with(aetherite, 15, Items.phaseFabric, 10, platinum, 10),
            m -> { m.powerReduction = 0.5f; m.compatible = powered; m.compatibleKey = "powered"; });
        outputAmplifier5 = module("output-amplifier-5", ModuleCategory.production, 5, 1.5f, 1200f, with(aetherite, 15, Items.phaseFabric, 10, platinum, 8),
            m -> { m.yieldBonus = 0.5f; m.speedPenalty = 0.1f; m.exclusiveGroup = "efficiency"; m.compatible = itemProducers; m.compatibleKey = "item-producers"; });
        deepMining5 = module("deep-mining-5", ModuleCategory.mining, 5, 2f, 1200f, with(aetherite, 15, Items.phaseFabric, 10, tungstenCarbide, 20),
            m -> { m.speedBonus = 1f; m.compatible = drills; m.compatibleKey = "drills"; });

        // ---- phase 6: tier IV (experimental) ----
        productionAccelerator4 = module("production-accelerator-4", ModuleCategory.production, 4, 2f, 1200f, with(microchip, 20, Items.phaseFabric, 10, surgeSuperalloy, 6),
            m -> { m.speedBonus = 0.5f; m.relativePowerCost = 1.2f; m.compatible = producers; m.compatibleKey = "production"; });
        powerOptimizer4 = module("power-optimizer-4", ModuleCategory.energy, 4, 0f, 1200f, with(microchip, 20, Items.phaseFabric, 10, platinum, 10),
            m -> { m.powerReduction = 0.4f; m.compatible = powered; m.compatibleKey = "powered"; });
        outputAmplifier4 = module("output-amplifier-4", ModuleCategory.production, 4, 1.5f, 1200f, with(microchip, 20, Items.phaseFabric, 10, platinum, 8),
            m -> { m.yieldBonus = 0.4f; m.speedPenalty = 0.2f; m.exclusiveGroup = "efficiency"; m.compatible = itemProducers; m.compatibleKey = "item-producers"; });
        precisionFabricator4 = module("precision-fabricator-4", ModuleCategory.production, 4, 1.5f, 1200f, with(microchip, 20, Items.phaseFabric, 10, tungstenCarbide, 15),
            m -> { m.inputSaving = 0.4f; m.exclusiveGroup = "efficiency"; m.compatible = itemConsumers; m.compatibleKey = "item-consumers"; });
        reactionAccelerator4 = module("reaction-accelerator-4", ModuleCategory.chemical, 4, 1.5f, 1200f, with(microchip, 20, Items.phaseFabric, 10, platinum, 10),
            m -> { m.speedBonus = 0.7f; m.coolantUse = 12f / 60f; m.compatible = chemicalMachines; m.compatibleKey = "chemical"; });
        deepMining4 = module("deep-mining-4", ModuleCategory.mining, 4, 2f, 1200f, with(microchip, 20, Items.phaseFabric, 10, tungstenCarbide, 20),
            m -> { m.speedBonus = 0.8f; m.compatible = drills; m.compatibleKey = "drills"; });

        // ---- phase 6: specialized configurations ----
        massProduction = module("mass-production", ModuleCategory.industrial, 3, 1f, 900f, with(microchip, 12, superalloy, 10, Items.plastanium, 10),
            m -> { m.speedBonus = 0.4f; m.yieldBonus = 0.2f; m.recycleFraction = 0f; m.relativePowerCost = 1.5f; m.exclusiveGroup = "efficiency";
                m.unique = true; m.compatible = itemProducers; m.compatibleKey = "item-producers"; });
        closedLoopProcessing = module("closed-loop-processing", ModuleCategory.industrial, 3, 0.8f, 900f, with(microchip, 12, polyethylene, 20, platinum, 4),
            m -> { m.inputSaving = 0.15f; m.recycleFraction = 0.1f; m.wasteRecovery = 0.5f; m.unique = true;
                m.compatible = itemConsumers; m.compatibleKey = "item-consumers"; });
        emergencyOverdrive = module("emergency-overdrive", ModuleCategory.industrial, 3, 2.5f, 900f, with(microchip, 10, Items.surgeAlloy, 10, lithium, 15),
            m -> { m.overchargeBonus = 1.5f; m.unique = true; m.compatible = producers; m.compatibleKey = "production"; });

        // ---- phase 6: legendary (one per machine, extremely expensive) ----
        quantumAccelerator = module("quantum-accelerator", ModuleCategory.legendary, 4, 6f, 3600f,
            with(Items.phaseFabric, 60, surgeSuperalloy, 40, microchip, 60, platinum, 30),
            m -> { m.speedBonus = 1.5f; m.relativePowerCost = 2f; m.legendary = true; m.compatible = producers; m.compatibleKey = "production"; });
        matterReplicator = module("matter-replicator", ModuleCategory.legendary, 4, 4f, 3600f,
            with(Items.phaseFabric, 60, surgeSuperalloy, 30, microchip, 60, highPuritySilicon, 40),
            m -> { m.yieldBonus = 0.5f; m.legendary = true; m.exclusiveGroup = "efficiency"; m.compatible = itemProducers; m.compatibleKey = "item-producers"; });
        //internal name kept from before the rename so existing saves keep their modules
        zeroPointEfficiencyCore = module("ancient-efficiency-core", ModuleCategory.legendary, 4, 0f, 3600f,
            with(Items.phaseFabric, 50, platinum, 40, vanadium, 40, microchip, 50),
            m -> { m.powerReduction = 0.5f; m.inputSaving = 0.25f; m.legendary = true; m.compatible = itemConsumers; m.compatibleKey = "item-consumers"; });
        temporalProcessingCore = module("temporal-processing-core", ModuleCategory.legendary, 4, 5f, 3600f,
            with(Items.phaseFabric, 60, surgeSuperalloy, 40, lithium, 60, microchip, 50),
            m -> { m.overchargeBonus = 3f; m.legendary = true; m.compatible = producers; m.compatibleKey = "production"; });

        militaryInterface = new AugmentationCoupler("military-augmentation-interface"){{
            requirements(Category.effect, with(Items.titanium, 40, microchip, 5, Items.silicon, 30, Items.graphite, 20, precisionComponent, 5, titaniumGear, 10, circuitBoard, 4));
            health = 260;
            slots = 3;
            onlyCategory = ModuleCategory.military;
        }};

        experimentalFacility = new ModuleAssembler("experimental-augmentation-facility"){{
            requirements(Category.crafting, with(Items.phaseFabric, 40, surgeSuperalloy, 30, microchip, 60, Items.titanium, 120, Items.silicon, 120, precisionComponent, 20, superalloyFrame, 20, carbonFiber, 20));
            size = 4;
            health = 1200;
            minTier = maxTier = 4;
            itemCapacity = 80;
            consumePower(8f);
        }};

        moduleRecycler = new ModuleRecycler("module-recycling-facility"){{
            requirements(Category.crafting, with(Items.copper, 60, Items.lead, 60, Items.silicon, 40, Items.graphite, 30, copperGear, 20, titaniumGear, 10));
            size = 2;
            health = 320;
            consumePower(1f);
        }};

        chemicalInterface = new AugmentationCoupler("chemical-augmentation-interface"){{
            requirements(Category.effect, with(polyethylene, 20, reinforcedGlass, 15, microchip, 5, Items.titanium, 20, pressureCasing, 10, circuitBoard, 4));
            health = 200;
            slots = 3;
            onlyCategory = ModuleCategory.chemical;
            liquidCapacity = 60f;
        }};

        augmentationCoupler = new AugmentationCoupler("augmentation-coupler"){{
            requirements(Category.effect, with(Items.copper, 30, Items.lead, 30, Items.silicon, 20, copperGear, 10));
            health = 120;
        }};

        advancedCoupler = new AugmentationCoupler("advanced-coupler"){{
            requirements(Category.effect, with(microchip, 5, Items.titanium, 20, Items.silicon, 20, aluminium, 10, precisionComponent, 2, titaniumGear, 5, circuitBoard, 2));
            health = 180;
            slots = 2;
        }};

        augmentationHub = new AugmentationCoupler("augmentation-hub"){{
            requirements(Category.effect, with(microchip, 20, superalloy, 20, Items.titanium, 60, Items.silicon, 40, superalloyFrame, 10, precisionComponent, 10, carbonFiber, 10, controlUnit, 5));
            size = 2;
            health = 500;
            slots = 4;
        }};

        basicModuleAssembler = new ModuleAssembler("basic-module-assembler"){{
            requirements(Category.crafting, with(Items.copper, 80, Items.lead, 60, Items.silicon, 50, Items.graphite, 30, copperGear, 20));
            size = 2;
            health = 320;
            minTier = maxTier = 1;
            consumePower(1f);
        }};

        advancedModuleAssembler = new ModuleAssembler("advanced-module-assembler"){{
            requirements(Category.crafting, with(microchip, 30, Items.titanium, 80, Items.silicon, 80, superalloy, 20, precisionComponent, 10, industrialBearing, 10, controlUnit, 5));
            size = 3;
            health = 700;
            minTier = 2;
            maxTier = 3;
            consumePower(3f);
        }};

        //mechanical components by tier: gears (I), titanium gears (II), precision components (III), experimental cores (IV)
        for(Item item : mindustry.Vars.content.items()){
            if(!(item instanceof AugmentModule m)) continue;
            Item part = m.tier <= 1 ? copperGear : m.tier == 2 ? titaniumGear : m.tier == 3 ? precisionComponent : experimentalCore;
            int amount = m.tier <= 1 ? 4 : m.tier == 2 ? 3 : m.tier == 3 ? 2 : 1;
            ItemStack[] r = java.util.Arrays.copyOf(m.recipe, m.recipe.length + 1);
            r[r.length - 1] = new ItemStack(part, amount);
            m.recipe = r;
        }
    }

    //note: parameters must not share names with AugmentModule fields, or the initializer blocks below assign fields to themselves
    static AugmentModule accelerator(int t, float bonus, float flatCost, float relativeCost, float time, ItemStack[] inputs){
        return new AugmentModule("production-accelerator-" + t, Color.valueOf("ff9a3c")){{
            category = ModuleCategory.production;
            tier = t;
            speedBonus = bonus;
            flatPowerCost = flatCost; //per tick, like consumePower()
            relativePowerCost = relativeCost;
            compatible = producers;
            compatibleKey = "production";
            craftTime = time;
            recipe = inputs;
            cost = 1f + t;
        }};
    }

    static AugmentModule amplifier(int t, float yield, float slowdown, float flatCost, float time, ItemStack[] inputs){
        return new AugmentModule("output-amplifier-" + t, Color.valueOf("ff9a3c")){{
            category = ModuleCategory.production;
            tier = t;
            yieldBonus = yield;
            speedPenalty = slowdown;
            flatPowerCost = flatCost;
            exclusiveGroup = "efficiency";
            compatible = itemProducers;
            compatibleKey = "item-producers";
            craftTime = time;
            recipe = inputs;
            cost = 1f + t;
        }};
    }

    static AugmentModule fabricator(int t, float saving, float flatCost, float time, ItemStack[] inputs){
        return new AugmentModule("precision-fabricator-" + t, Color.valueOf("ff9a3c")){{
            category = ModuleCategory.production;
            tier = t;
            inputSaving = saving;
            flatPowerCost = flatCost;
            exclusiveGroup = "efficiency";
            compatible = itemConsumers;
            compatibleKey = "item-consumers";
            craftTime = time;
            recipe = inputs;
            cost = 1f + t;
        }};
    }

    static AugmentModule recycler(int t, float fraction, float flatCost, float time, ItemStack[] inputs){
        return new AugmentModule("material-recycler-" + t, Color.valueOf("ffd84a")){{
            category = ModuleCategory.industrial;
            tier = t;
            recycleFraction = fraction;
            flatPowerCost = flatCost;
            compatible = itemConsumers;
            compatibleKey = "item-consumers";
            craftTime = time;
            recipe = inputs;
            cost = 1f + t;
        }};
    }

    /** A chemical module; `setup` sets its effect and compatibility. */
    static AugmentModule chemical(String family, int t, float flatCost, float time, ItemStack[] inputs, Cons<AugmentModule> setup){
        return module(family + "-" + t, ModuleCategory.chemical, t, flatCost, time, inputs, setup);
    }

    /** Any module; `setup` sets its effect and compatibility. (Parameter names deliberately differ from AugmentModule fields.) */
    static AugmentModule module(String id, ModuleCategory cat, int t, float flatCost, float time, ItemStack[] inputs, Cons<AugmentModule> setup){
        AugmentModule m = new AugmentModule(id, cat.color){{
            category = cat;
            tier = t;
            flatPowerCost = flatCost;
            craftTime = time;
            recipe = inputs;
            cost = 1f + t;
        }};
        setup.get(m);
        return m;
    }

    static AugmentModule optimizer(int t, float reduction, float time, ItemStack[] inputs){
        return new AugmentModule("power-optimizer-" + t, Color.valueOf("5ab8ff")){{
            category = ModuleCategory.energy;
            tier = t;
            powerReduction = reduction;
            compatible = powered;
            compatibleKey = "powered";
            craftTime = time;
            recipe = inputs;
            cost = 1f + t;
        }};
    }
}
