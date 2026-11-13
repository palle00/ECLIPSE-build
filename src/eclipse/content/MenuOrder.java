package eclipse.content;

import arc.struct.*;
import eclipse.content.blocks.*;
import mindustry.content.*;
import mindustry.ctype.*;

import static eclipse.content.EclipseItems.*;
import static eclipse.content.EclipseLiquids.*;
import static eclipse.content.blocks.BiologyBlocks.*;
import static eclipse.content.blocks.ChemicalBlocks.*;
import static eclipse.content.blocks.ComponentBlocks.*;
import static eclipse.content.blocks.DefenseBlocks.*;
import static eclipse.content.blocks.ExplorationBlocks.*;
import static eclipse.content.blocks.ElectronicsBlocks.*;
import static eclipse.content.blocks.EndgameBlocks.*;
import static eclipse.content.blocks.EnvironmentBlocks.*;
import static eclipse.content.blocks.LogisticsBlocks.*;
import static eclipse.content.blocks.MetallurgyBlocks.*;
import static eclipse.content.blocks.NuclearBlocks.*;
import static eclipse.content.blocks.PetrochemicalBlocks.*;
import static eclipse.content.blocks.PowerBlocks.*;
import static eclipse.content.blocks.RefiningBlocks.*;
import static eclipse.content.blocks.TransportBlocks.*;
import static eclipse.content.AugmentContent.*;
import static mindustry.Vars.*;

/**
 * Places ECLIPSE content next to the vanilla content it extends, in progression order, instead of at the end of the lists.
 * The build menu, core inventory and database all list content in id order, so this reorders the content lists and renumbers ids.
 * Saves and maps store a name map, so existing saves load correctly with the new ids.
 * Must run at the end of loadContent, before content.init().
 */
public class MenuOrder{
    /** anchor -> content placed directly after it, in order. Placed content can itself be an anchor. */
    static final ObjectMap<Content, Seq<Content>> placements = new ObjectMap<>();
    static final ObjectSet<Content> placed = new ObjectSet<>();

    public static void load(){
        //---- distribution: vanilla conveyors -> ECLIPSE conveyor upgrades, sorters -> advanced sorters, routers -> hubs ----
        after(Blocks.armoredConveyor, aluminiumConveyor, chromiumConveyor, superalloyConveyor, carbideConveyor, surgeConveyor);
        after(Blocks.invertedSorter, threeWaySorter, advancedItemFilter, batchGate, flowMeter);
        after(Blocks.distributor, distributionHub, controlledSplitter, lineBalancer);
        after(Blocks.underflowGate, priorityMerger, smallBuffer, industrialBuffer);
        after(Blocks.unloader, smartUnloader);
        after(Blocks.container, componentDepot);

        //---- liquid: pipes after vanilla pipes, routers/valves after the liquid router, tank and bridge after theirs ----
        after(Blocks.platedConduit, aluminiumConduit, chromiumConduit, superalloyConduit, carbideConduit, surgeConduit, polymerConduit);
        after(Blocks.liquidRouter, polymerLiquidRouter, liquidOverflowValve, liquidBalancer, reserveValve);
        after(Blocks.liquidTank, polymerLiquidTank);
        after(Blocks.bridgeConduit, polymerBridgeConduit);

        //---- power ----
        after(Blocks.batteryLarge, industrialBattery, lithiumBattery, flowBattery, aetheriteCapacitor);
        after(Blocks.steamGenerator, hydrogenFuelCell, methaneTurbine, biofuelEngine, industrialBoiler, steamTurbine);
        after(Blocks.thoriumReactor, fissionReactor);
        after(Blocks.impactReactor, worldEngine);

        //---- turrets ----
        after(Blocks.hail, MilitaryBlocks.minelayer);
        after(Blocks.wave, MilitaryBlocks.acidMortar);
        after(Blocks.duo, MilitaryBlocks.rivet, MilitaryBlocks.sickle);
        after(Blocks.lancer, MilitaryBlocks.heatLance, MilitaryBlocks.tremor);
        after(Blocks.cyclone, MilitaryBlocks.apiary);
        after(Blocks.meltdown, MilitaryBlocks.eventHorizon);
        after(Blocks.arc, MilitaryBlocks.jammer, MilitaryBlocks.thunderhead);
        after(Blocks.ripple, MilitaryBlocks.piledriver);
        after(Blocks.scatter, MilitaryBlocks.kestrel);
        after(MilitaryBlocks.minelayer, MilitaryBlocks.cloudbreaker);
        after(Blocks.foreshadow, MilitaryEndgame.orbitalPlatform);
        after(Blocks.tetrativeReconstructor, MilitaryEndgame.titanComplex);

        //---- production / defense / effect ----
        after(Blocks.blastDrill, carbideDrill, EndgameBlocks.latticeDrill);
        after(Blocks.cultivator, biomassCultivator, algaePhotobioreactor);
        after(Blocks.thoriumWallLarge, superalloyWall, superalloyWallLarge, compositeWall, compositeWallLarge);
        after(Blocks.surgeWallLarge, surgeSuperalloyWall, surgeSuperalloyWallLarge);
        after(Blocks.mendProjector, repairDispatchBay);
        after(Blocks.forceProjector, unitShieldBeacon, waveDisruptionArray, temporalArray);
        //it reads the next wave and draws it on logic displays: next to the displays in the Logic tab
        after(Blocks.largeLogicDisplay, waveScanner);
        after(Blocks.airFactory, RoboticsContent.robotHub);
        after(Blocks.overdriveDome, augmentationCoupler, advancedCoupler, chemicalInterface, militaryInterface, augmentationHub);

        //---- crafting: each ECLIPSE production chain follows the vanilla machine it grows out of ----
        after(Blocks.multiPress, methanePyrolysisReactor, gearPress, metalExtruder, heavyGearPress, casingForge, bearingPress, frameForge,
            precisionAssembler, coreAssembler); //mechanical manufacturing
        after(Blocks.siliconSmelter, siliconReductionFurnace, siliconRefinery, chipFabricator, wireDrawer, circuitPrinter, controlUnitAssembler);
        after(Blocks.kiln, sodaGlassFurnace, glassKiln);
        after(Blocks.plastaniumCompressor, //oil -> petrochemicals and heat
            distillationColumn, industrialHeater, electricHeater, geothermalHeatTap, heatExchanger, thermalStorage, steamCracker, catalystPlant,
            polymerizationReactor, methanolSynthesizer, olefinReactor, epoxyPlant, rubberPlant, acrylonitrileReactor,
            carbonFiberFurnace, compositePress, sulfuricAcidPlant, batteryAssemblyPlant);
        after(Blocks.phaseWeaver, fuelRodFabricator, reprocessingPlant); //thorium -> nuclear fuel
        after(Blocks.surgeSmelter, alloyFurnace, inductionFurnace, arcFurnace);
        after(Blocks.pulverizer, oreCrusher, oreWasher);
        after(Blocks.cryofluidMixer, //water -> chemical industry
            coolingTower, waterTreatmentPlant, electrolysisPlant, airSeparationUnit, cryogenicCondenser, highPressureReactor, nitricAcidPlant,
            chlorAlkaliCell, acidSynthesizer, acidLeachingVat, industrialNeutralizer, industrialScrubber, gasSeparator,
            wastewaterTreatmentPlant, thermalWasteProcessor, lithiumExtractor, aluminaRefinery, aluminiumSmelter,
            aquaRegiaMixer, platinumRefinery, synthesisComplex);
        after(Blocks.blastMixer, ammoniumNitratePlant, explosivesPlant);
        after(Blocks.separator, chlorideTitaniumPlant, electrowinningCell, mineralSeparator, sinteringFurnace);
        after(Blocks.sporePress, fertilizerPlant, enzymeExtractor, biochemicalFermenter, anaerobicDigester); //biology
        after(Blocks.incinerator, basicModuleAssembler, advancedModuleAssembler, experimentalFacility, aetheriteForge, moduleRecycler); //augmentation section

        //---- environment (map editor) ----
        after(Blocks.oreThorium, oreNickel, oreChromium, oreCobalt, oreTungsten, oreMolybdenum, orePlatinum, oreAetherite);

        //---- items (core inventory, item pickers); augmentation modules stay at the end as their own group ----
        after(Items.metaglass, reinforcedGlass);
        after(Items.coal, sulfur);
        after(Items.copper, copperGear, copperWire);
        after(Items.titanium, titaniumRod, titaniumGear, pressureCasing, industrialBearing, nickel, chromium, cobalt, molybdenum, vanadium, tungsten, tungstenCarbide, alumina, aluminium, lithium, superalloy, superalloyFrame, alnicoMagnet, platinum);
        after(Items.thorium, thoriumFuelRod, spentFuelRod);
        after(Items.scrap, crushedOre, industrialSludge);
        after(Items.silicon, highPuritySilicon, microchip, circuitBoard, precisionComponent, controlUnit);
        after(Items.plastanium, polyethylene, polymerCatalyst, leadAcidCell, syntheticRubber, epoxyResin, carbonFiber, carbonComposite);
        after(Items.surgeAlloy, surgeSuperalloy, experimentalCore);
        after(Items.phaseFabric, aetherite, superconductor);
        after(Items.sporePod, biomass, fertilizer, enzymeExtract);
        after(Items.blastCompound, ammoniumNitrate);

        //---- liquids ----
        after(Liquids.water, chilledWater, purifiedWater, EclipseLiquids.steam, brine, sodiumHydroxide, chlorine, hydrochloricAcid, sulfuricAcid, acidicWaste,
            wastewater, ammonia, nitricAcid, aquaRegia);
        after(Liquids.oil, methane, naphtha, ethylene, flueGas, carbonDioxide, methanol, propylene, acrylonitrile, biofuel);
        after(Liquids.cryofluid, oxygen, liquidNitrogen);

        apply(ContentType.block);
        apply(ContentType.item);
        apply(ContentType.liquid);
    }

    /** Whether this content has a fixed place (used by the test to catch new content that was never placed). */
    public static boolean isPlaced(Content c){
        return placed.contains(c);
    }

    static void after(Content anchor, Content... list){
        for(Content c : list){
            if(c == null || !placed.add(c)) throw new IllegalStateException("ECLIPSE: content placed twice or null after " + anchor);
        }
        placements.get(anchor, Seq::new).addAll(list);
    }

    static void apply(ContentType type){
        Seq<Content> all = content.getBy(type);
        Seq<Content> result = new Seq<>(all.size);
        for(Content c : all){
            if(!placed.contains(c)) add(result, c);
        }
        if(result.size != all.size) throw new IllegalStateException("ECLIPSE: menu order lost content of type " + type + " (anchor missing?)");
        all.set(result);
        for(int i = 0; i < all.size; i++) all.get(i).id = (short)i;
    }

    static void add(Seq<Content> result, Content c){
        result.add(c);
        Seq<Content> next = placements.get(c);
        if(next != null) for(Content n : next) add(result, n);
    }
}
