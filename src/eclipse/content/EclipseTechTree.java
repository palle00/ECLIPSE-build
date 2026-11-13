package eclipse.content;

import eclipse.augment.*;
import eclipse.content.blocks.*;
import mindustry.content.*;
import mindustry.content.TechTree.*;
import mindustry.ctype.*;
import mindustry.game.Objectives.*;
import mindustry.type.*;
import mindustry.world.*;

import static eclipse.content.EclipseItems.*;
import static eclipse.content.EclipseLiquids.*;
import static eclipse.content.blocks.BiologyBlocks.*;
import static eclipse.content.blocks.ChemicalBlocks.*;
import static eclipse.content.blocks.ComponentBlocks.*;
import static eclipse.content.blocks.DefenseBlocks.*;
import static eclipse.content.blocks.ExplorationBlocks.*;
import static eclipse.content.blocks.ElectronicsBlocks.*;
import static eclipse.content.blocks.EndgameBlocks.*;
import static eclipse.content.blocks.MetallurgyBlocks.*;
import static eclipse.content.blocks.NuclearBlocks.*;
import static eclipse.content.blocks.PetrochemicalBlocks.*;
import static eclipse.content.blocks.PowerBlocks.*;
import static eclipse.content.blocks.RefiningBlocks.*;
import static eclipse.content.blocks.TransportBlocks.*;

/** Attaches ECLIPSE research to the Serpulo tech tree. */
public class EclipseTechTree{

    public static void load(){
        //mechanical manufacturing
        TechNode gears = child(find(Blocks.graphitePress), gearPress);
        TechNode heavyGears = child(child(gears, metalExtruder), heavyGearPress);
        child(heavyGears, casingForge);
        TechNode bearings = child(heavyGears, bearingPress);
        child(bearings, frameForge);
        child(child(bearings, precisionAssembler), coreAssembler);
        child(heavyGears, componentDepot);
        TechNode gearItem = child(find(Items.copper), copperGear);
        TechNode rodItem = child(find(Items.titanium), titaniumRod);
        child(rodItem, titaniumGear);
        child(rodItem, pressureCasing);
        child(rodItem, industrialBearing);
        child(gearItem, precisionComponent);

        //chemical industry
        TechNode treatment = child(find(Blocks.mechanicalPump), waterTreatmentPlant);
        TechNode electrolysis = child(treatment, electrolysisPlant);
        child(electrolysis, hydrogenFuelCell);
        TechNode airSeparation = child(electrolysis, airSeparationUnit);
        TechNode haber = child(airSeparation, highPressureReactor);
        TechNode nitric = child(haber, nitricAcidPlant);
        TechNode aquaRegiaNode = child(nitric, aquaRegiaMixer);
        child(aquaRegiaNode, platinumRefinery);
        TechNode nitrate = child(nitric, ammoniumNitratePlant);
        child(nitrate, explosivesPlant);
        TechNode chlorAlkali = child(treatment, chlorAlkaliCell);
        child(chlorAlkali, catalystPlant);
        TechNode acid = child(chlorAlkali, acidSynthesizer);
        TechNode leaching = child(acid, acidLeachingVat);
        child(leaching, industrialNeutralizer);

        TechNode distillation = child(treatment, distillationColumn);
        child(distillation, methaneTurbine);
        TechNode heater = child(distillation, industrialHeater);
        TechNode cracker = child(heater, steamCracker);
        TechNode polymerization = child(cracker, polymerizationReactor);

        //augmentation
        TechNode coupler = child(find(Blocks.overdriveProjector), AugmentContent.augmentationCoupler);
        TechNode basicAssembler = child(coupler, AugmentContent.basicModuleAssembler);
        TechNode advancedAssembler = child(basicAssembler, AugmentContent.advancedModuleAssembler);
        child(basicAssembler, AugmentContent.productionAccelerator1);
        child(basicAssembler, AugmentContent.powerOptimizer1);
        child(child(advancedAssembler, AugmentContent.productionAccelerator2), AugmentContent.productionAccelerator3);
        child(child(advancedAssembler, AugmentContent.powerOptimizer2), AugmentContent.powerOptimizer3);
        child(child(coupler, AugmentContent.advancedCoupler), AugmentContent.augmentationHub);
        child(basicAssembler, AugmentContent.outputAmplifier1);
        child(basicAssembler, AugmentContent.precisionFabricator1);
        child(basicAssembler, AugmentContent.materialRecycler1);
        child(child(advancedAssembler, AugmentContent.outputAmplifier2), AugmentContent.outputAmplifier3);
        child(child(advancedAssembler, AugmentContent.precisionFabricator2), AugmentContent.precisionFabricator3);
        child(advancedAssembler, AugmentContent.materialRecycler2);

        //chemical augmentation
        child(coupler, AugmentContent.chemicalInterface);
        AugmentModule[][] chemicalFamilies = {
            {AugmentContent.reactionAccelerator1, AugmentContent.reactionAccelerator2, AugmentContent.reactionAccelerator3},
            {AugmentContent.thermalController1, AugmentContent.thermalController2, AugmentContent.thermalController3},
            {AugmentContent.pressureStabilizer1, AugmentContent.pressureStabilizer2},
            {AugmentContent.catalystOptimizer1, AugmentContent.catalystOptimizer2},
            {AugmentContent.purificationModule1, AugmentContent.purificationModule2, AugmentContent.purificationModule3},
            {AugmentContent.wasteRecovery1, AugmentContent.wasteRecovery2}
        };
        for(AugmentModule[] family : chemicalFamilies){
            TechNode node = child(basicAssembler, family[0]);
            for(int i = 1; i < family.length; i++) node = child(i == 1 ? find(AugmentContent.advancedModuleAssembler) : node, family[i]);
        }

        //energy, mining and military augmentation
        AugmentModule[][] moreFamilies = {
            {AugmentContent.thermalRecovery1, AugmentContent.thermalRecovery2, AugmentContent.thermalRecovery3},
            {AugmentContent.energyOvercharger1, AugmentContent.energyOvercharger2},
            {AugmentContent.capacitorModule1, AugmentContent.capacitorModule2},
            {AugmentContent.deepMining1, AugmentContent.deepMining2, AugmentContent.deepMining3},
            {AugmentContent.advancedDrillHead1, AugmentContent.advancedDrillHead2},
            {AugmentContent.geologicalAnalyzer},
            {AugmentContent.rapidFireController1, AugmentContent.rapidFireController2, AugmentContent.rapidFireController3},
            {AugmentContent.coolingSystem1, AugmentContent.coolingSystem2},
            {AugmentContent.advancedAmmunition1, AugmentContent.advancedAmmunition2, AugmentContent.advancedAmmunition3},
            {AugmentContent.targetingComputer1, AugmentContent.targetingComputer2}
        };
        for(AugmentModule[] family : moreFamilies){
            TechNode node = child(basicAssembler, family[0]);
            for(int i = 1; i < family.length; i++) node = child(i == 1 ? find(AugmentContent.advancedModuleAssembler) : node, family[i]);
        }
        child(advancedAssembler, AugmentContent.ballisticComputer);
        child(coupler, AugmentContent.militaryInterface);
        child(basicAssembler, AugmentContent.moduleRecycler);

        //experimental tier, configurations and legendary modules
        TechNode experimental = child(advancedAssembler, AugmentContent.experimentalFacility);
        for(AugmentModule m : new AugmentModule[]{AugmentContent.productionAccelerator4, AugmentContent.powerOptimizer4, AugmentContent.outputAmplifier4,
            AugmentContent.precisionFabricator4, AugmentContent.reactionAccelerator4, AugmentContent.deepMining4,
            AugmentContent.quantumAccelerator, AugmentContent.matterReplicator, AugmentContent.zeroPointEfficiencyCore, AugmentContent.temporalProcessingCore}){
            child(experimental, m);
        }
        for(AugmentModule m : new AugmentModule[]{AugmentContent.massProduction, AugmentContent.closedLoopProcessing, AugmentContent.emergencyOverdrive}){
            child(advancedAssembler, m);
        }

        //high-speed belts and pipes, one tier per material
        TechNode belt = find(Blocks.titaniumConveyor);
        for(Block b : new Block[]{aluminiumConveyor, chromiumConveyor, superalloyConveyor, carbideConveyor, surgeConveyor}) belt = child(belt, b);
        TechNode pipeTier = find(Blocks.platedConduit);
        for(Block b : new Block[]{aluminiumConduit, chromiumConduit, superalloyConduit, carbideConduit, surgeConduit}) pipeTier = child(pipeTier, b);

        //advanced logistics: control and routing (early-mid -> mid -> mid-late)
        child(find(Blocks.conveyor), LogisticsBlocks.flowMeter);
        TechNode sorterNode = child(find(Blocks.sorter), LogisticsBlocks.threeWaySorter);
        child(sorterNode, LogisticsBlocks.advancedItemFilter);
        child(sorterNode, LogisticsBlocks.batchGate);
        child(sorterNode, LogisticsBlocks.distributionHub);
        child(find(Blocks.overflowGate), LogisticsBlocks.priorityMerger);
        TechNode splitterNode = child(find(Blocks.router), LogisticsBlocks.controlledSplitter);
        child(splitterNode, LogisticsBlocks.lineBalancer);
        TechNode bufferNode = child(find(Blocks.container), LogisticsBlocks.smallBuffer);
        child(bufferNode, LogisticsBlocks.industrialBuffer);
        child(find(Blocks.unloader), LogisticsBlocks.smartUnloader);
        TechNode valve = child(find(Blocks.liquidRouter), LogisticsBlocks.liquidOverflowValve);
        child(valve, LogisticsBlocks.liquidBalancer);
        child(valve, LogisticsBlocks.reserveValve);

        //corrosion-resistant logistics
        TechNode pipe = child(polymerization, polymerConduit);
        child(pipe, polymerLiquidRouter);
        child(pipe, polymerBridgeConduit);
        child(pipe, polymerLiquidTank);
        TechNode sulfuric = child(distillation, sulfuricAcidPlant);
        TechNode batteryPlant = child(sulfuric, batteryAssemblyPlant);
        TechNode industrialBatteryNode = child(batteryPlant, industrialBattery);
        TechNode lithiumBatteryNode = child(industrialBatteryNode, lithiumBattery);
        child(lithiumBatteryNode, flowBattery);
        child(heater, electricHeater);

        //waste and emissions
        TechNode scrubber = child(heater, industrialScrubber);
        TechNode separatorNode = child(heater, gasSeparator);

        //organic chemistry and composites
        TechNode olefin = child(child(separatorNode, methanolSynthesizer), olefinReactor);
        child(olefin, epoxyPlant);
        TechNode rubber = child(olefin, rubberPlant);
        TechNode press = child(child(child(rubber, acrylonitrileReactor), carbonFiberFurnace), compositePress);
        child(child(press, compositeWall), compositeWallLarge);
        TechNode wastewaterNode = child(scrubber, wastewaterTreatmentPlant);
        child(wastewaterNode, thermalWasteProcessor);

        //biology: fertilizer from the nitrogen industry, biomass, enzymes, fermentation, digestion
        TechNode fertilizerNode = child(haber, fertilizerPlant);
        TechNode cultivator = child(fertilizerNode, biomassCultivator);
        child(cultivator, algaePhotobioreactor);
        TechNode enzymes = child(cultivator, enzymeExtractor);
        child(child(enzymes, biochemicalFermenter), biofuelEngine);
        child(wastewaterNode, anaerobicDigester);

        //nuclear
        TechNode fab = child(find(Blocks.thoriumReactor), fuelRodFabricator);
        TechNode fission = child(fab, fissionReactor);
        child(fission, reprocessingPlant);

        //metallurgy
        TechNode mineralSep = child(find(Blocks.separator), mineralSeparator);
        child(mineralSep, sinteringFurnace);
        child(heater, alloyFurnace);
        child(treatment, lithiumExtractor);
        child(child(find(Blocks.laserDrill), carbideDrill), latticeDrill);
        TechNode wall = child(find(Blocks.thoriumWall), superalloyWall);
        child(wall, superalloyWallLarge);
        TechNode refinery = child(chlorAlkali, aluminaRefinery);
        TechNode smelter = child(refinery, aluminiumSmelter);
        child(heater, glassKiln);
        child(airSeparation, cryogenicCondenser);
        child(smelter, arcFurnace);
        TechNode surgeWall = child(find(Blocks.surgeWall), surgeSuperalloyWall);
        child(surgeWall, surgeSuperalloyWallLarge);

        //electronics
        TechNode siliconRefineryNode = child(find(Blocks.siliconSmelter), siliconRefinery);
        child(siliconRefineryNode, chipFabricator);

        //heat recovery
        TechNode exchanger = child(sulfuric, heatExchanger);
        TechNode boiler = child(exchanger, industrialBoiler);
        child(boiler, steamTurbine);

        //materials
        TechNode sulfurNode = child(find(Items.coal), sulfur);
        TechNode poly = child(sulfurNode, polyethylene);
        child(poly, leadAcidCell);
        TechNode rubberItem = child(poly, syntheticRubber);
        child(poly, epoxyResin);
        child(child(rubberItem, carbonFiber), carbonComposite);
        child(sulfurNode, polymerCatalyst);
        child(sulfurNode, ammoniumNitrate);

        TechNode titanium = find(Items.titanium);
        TechNode nickelNode = child(titanium, nickel);
        child(nickelNode, chromium);
        child(nickelNode, cobalt);
        TechNode tungstenNode = child(nickelNode, tungsten);
        child(tungstenNode, tungstenCarbide);
        child(child(nickelNode, superalloy), superalloyFrame);
        child(nickelNode, molybdenum);
        child(nickelNode, vanadium);
        child(tungstenNode, platinum);
        TechNode rod = child(find(Items.thorium), thoriumFuelRod);
        child(rod, spentFuelRod);
        child(find(Items.scrap), industrialSludge);
        child(titanium, lithium);
        TechNode aluminaNode = child(titanium, alumina);
        child(aluminaNode, aluminium);
        TechNode hpSilicon = child(find(Items.silicon), highPuritySilicon);
        child(hpSilicon, microchip);
        child(find(Items.metaglass), reinforcedGlass);
        child(child(find(Items.surgeAlloy), surgeSuperalloy), experimentalCore);
        TechNode biomassNode = child(find(Items.sporePod), biomass);
        child(biomassNode, fertilizer);
        child(biomassNode, enzymeExtract);
        child(biomassNode, biofuel);

        TechNode pure = child(find(Liquids.water), purifiedWater);
        child(pure, oxygen);
        TechNode brineNode = child(pure, brine);
        TechNode chlorineNode = child(brineNode, chlorine);
        child(chlorineNode, hydrochloricAcid);
        child(brineNode, sodiumHydroxide);
        child(pure, acidicWaste);
        child(pure, wastewater);
        child(pure, sulfuricAcid);
        TechNode ammoniaNode = child(pure, ammonia);
        TechNode nitricNode = child(ammoniaNode, nitricAcid);
        child(nitricNode, aquaRegia);
        child(pure, steam);
        child(find(Liquids.cryofluid), liquidNitrogen);

        TechNode oil = find(Liquids.oil);
        TechNode flue = child(child(oil, methane), flueGas);
        TechNode propyleneNode = child(child(child(flue, carbonDioxide), methanol), propylene);
        child(propyleneNode, acrylonitrile);
        TechNode naphthaNode = child(oil, naphtha);
        child(naphthaNode, ethylene);

        //electronics, ore processing, heat storage and cooling
        TechNode wire = child(find(gearPress), wireDrawer);
        child(child(wire, circuitPrinter), controlUnitAssembler);
        child(child(find(Blocks.pulverizer), oreCrusher), oreWasher);
        child(find(alloyFurnace), inductionFurnace);
        child(find(heatExchanger), thermalStorage);
        child(find(waterTreatmentPlant), coolingTower);
        child(child(child(find(copperGear), copperWire), circuitBoard), controlUnit);
        child(find(cobalt), alnicoMagnet);
        child(find(Items.scrap), crushedOre);
        child(find(Liquids.water), chilledWater);

        //aetherite and environment
        TechNode aetheriteBranch = child(find(AugmentContent.experimentalFacility), aetheriteForge);
        for(AugmentModule m : new AugmentModule[]{AugmentContent.productionAccelerator5, AugmentContent.powerOptimizer5, AugmentContent.outputAmplifier5, AugmentContent.deepMining5}){
            child(aetheriteBranch, m);
        }
        child(find(lithiumBattery), aetheriteCapacitor);
        child(find(industrialHeater), geothermalHeatTap);
        child(find(Items.phaseFabric), aetherite);

        //endgame: exotic chemistry, the long wave delay, the World Engine
        TechNode synthesis = child(find(platinumRefinery), synthesisComplex);
        child(find(Blocks.impactReactor), worldEngine);
        child(find(aetherite), superconductor);

        //specialist robotics: built in the vanilla air factory, run from the dispatch hub
        TechNode hubNode = child(find(Blocks.airFactory), RoboticsContent.robotHub);
        child(child(hubNode, RoboticsContent.logisticsDrone), RoboticsContent.repairDrone);
        child(find(RoboticsContent.repairDrone), RoboticsContent.shieldDrone);

        //military: each new turret under the vanilla turret whose role it extends
        child(find(Blocks.wave), MilitaryBlocks.acidMortar);
        child(find(Blocks.hail), MilitaryBlocks.minelayer);
        child(find(Blocks.lancer), MilitaryBlocks.heatLance);
        child(find(Blocks.arc), MilitaryBlocks.jammer);
        child(find(Blocks.ripple), MilitaryBlocks.piledriver);
        child(find(Blocks.scatter), MilitaryBlocks.kestrel);
        child(find(Blocks.arc), MilitaryBlocks.thunderhead);
        child(find(MilitaryBlocks.minelayer), MilitaryBlocks.cloudbreaker);
        //third set, one per tier: the Rivet right after the Duo, the Event Horizon after the Meltdown
        child(child(find(Blocks.duo), MilitaryBlocks.rivet), MilitaryBlocks.sickle);
        child(find(Blocks.lancer), MilitaryBlocks.tremor);
        child(find(Blocks.cyclone), MilitaryBlocks.apiary);
        child(find(Blocks.meltdown), MilitaryBlocks.eventHorizon);

        //unit lines: T1 under the vanilla factory, each upgrade also needs its reconstructor researched (as vanilla units do)
        unitLine(Blocks.groundFactory, MilitaryUnits.reaper, MilitaryUnits.bulwark, MilitaryUnits.rampart, MilitaryUnits.annihilator, MilitaryUnits.colossus);
        unitLine(Blocks.airFactory, MilitaryUnits.wraith, MilitaryUnits.interceptor, MilitaryUnits.valkyrie, MilitaryUnits.tempest, MilitaryUnits.leviathan);

        //military endgame: the titan after the last reconstructor, the orbital platform after Foreshadow, the EW drone after the Jammer
        child(child(find(Blocks.tetrativeReconstructor), MilitaryEndgame.titanComplex), MilitaryEndgame.dreadnought);
        child(find(Blocks.foreshadow), MilitaryEndgame.orbitalPlatform);
        child(find(MilitaryBlocks.jammer), MilitaryEndgame.ewDrone);

        //strategic defence and support
        child(child(find(Blocks.mender), waveScanner), waveDisruptionArray);
        child(find(waveDisruptionArray), temporalArray);
        child(find(Blocks.forceProjector), unitShieldBeacon);
        child(find(Blocks.mendProjector), repairDispatchBay);

        //upgraded production of the vanilla materials ECLIPSE consumes most, each under the vanilla building it upgrades
        child(find(Blocks.siliconSmelter), siliconReductionFurnace);
        child(find(Blocks.graphitePress), methanePyrolysisReactor);
        child(find(Blocks.kiln), sodaGlassFurnace);
        child(find(Blocks.separator), chlorideTitaniumPlant);
        child(find(Blocks.separator), electrowinningCell);
    }

    static void unitLine(Block factory, UnitType... tiers){
        Block[] reconstructors = {Blocks.additiveReconstructor, Blocks.multiplicativeReconstructor, Blocks.exponentialReconstructor, Blocks.tetrativeReconstructor};
        TechNode node = child(find(factory), tiers[0]);
        for(int i = 1; i < tiers.length; i++){
            node = child(node, tiers[i]);
            node.objectives.add(new Research(reconstructors[i - 1]));
        }
    }

    static TechNode find(UnlockableContent content){
        TechNode node = TechTree.all.find(n -> n.content == content);
        if(node == null) throw new IllegalStateException("ECLIPSE: tech node not found for " + content.name);
        return node;
    }

    static TechNode child(TechNode parent, UnlockableContent content){
        TechNode node = new TechNode(parent, content, content.researchRequirements());
        //resources are unlocked by producing them
        if(content instanceof Liquid || content instanceof Item) node.objectives.add(new Produce(content));
        return node;
    }
}
