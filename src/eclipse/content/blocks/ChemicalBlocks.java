package eclipse.content.blocks;

import arc.graphics.*;
import eclipse.exploration.*;
import eclipse.world.blocks.*;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.heat.*;
import mindustry.world.blocks.production.*;
import mindustry.world.draw.*;

import static eclipse.content.EclipseItems.*;
import static eclipse.content.EclipseLiquids.*;
import static mindustry.type.ItemStack.*;

/** Inorganic chemical industry: water chemistry, chlor-alkali, acids, nitrogen chemistry, electrochemistry and waste treatment. */
public class ChemicalBlocks{
    public static Block
    waterTreatmentPlant, electrolysisPlant, chlorAlkaliCell, acidSynthesizer, acidLeachingVat,
    sulfuricAcidPlant, batteryAssemblyPlant, industrialNeutralizer,
    airSeparationUnit, highPressureReactor, cryogenicCondenser, nitricAcidPlant, aquaRegiaMixer, ammoniumNitratePlant, explosivesPlant,
    industrialScrubber, gasSeparator, wastewaterTreatmentPlant, thermalWasteProcessor;

    public static void load(){
        // Water -> purified water + brine concentrate (reverse osmosis style).
        waterTreatmentPlant = new GenericCrafter("water-treatment-plant"){{
            requirements(Category.crafting, with(Items.copper, 60, Items.lead, 50, Items.metaglass, 40, Items.graphite, 25, copperGear, 10));
            size = 2;
            health = 320;
            rotate = true;
            rotateDraw = false;
            liquidCapacity = 30f;
            consumeLiquid(Liquids.water, 18f / 60f);
            consumePower(0.5f);
            outputLiquids = LiquidStack.with(purifiedWater, 13f / 60f, brine, 5f / 60f);
            liquidOutputDirections = new int[]{1, 3};
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(purifiedWater, 3f), new DrawDefault(), new DrawLiquidOutputs());
        }};

        // Purified water -> hydrogen + oxygen (2:1).
        electrolysisPlant = new GenericCrafter("electrolysis-plant"){{
            requirements(Category.crafting, with(Items.copper, 80, Items.lead, 60, Items.silicon, 50, Items.graphite, 40, Items.metaglass, 40, copperGear, 15, copperWire, 20));
            size = 3;
            health = 600;
            rotate = true;
            rotateDraw = false;
            liquidCapacity = 40f;
            consumeLiquid(purifiedWater, 12f / 60f);
            consumePower(1.5f);
            outputLiquids = LiquidStack.with(Liquids.hydrogen, 8f / 60f, oxygen, 4f / 60f);
            liquidOutputDirections = new int[]{1, 3};
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(purifiedWater, 4f),
                new DrawBubbles(Color.valueOf("ffffff")){{ amount = 14; radius = 6f; spread = 10f; }},
                new DrawDefault(), new DrawLiquidOutputs());
        }};

        // Brine -> chlorine + hydrogen + sodium hydroxide.
        chlorAlkaliCell = new GenericCrafter("chlor-alkali-cell"){{
            requirements(Category.crafting, with(Items.lead, 80, Items.silicon, 60, Items.titanium, 40, Items.metaglass, 50, pressureCasing, 10, titaniumRod, 10, copperWire, 20));
            size = 3;
            health = 600;
            rotate = true;
            rotateDraw = false;
            liquidCapacity = 40f;
            consumeLiquid(brine, 6f / 60f);
            consumePower(2f);
            outputLiquids = LiquidStack.with(chlorine, 3f / 60f, Liquids.hydrogen, 3f / 60f, sodiumHydroxide, 3f / 60f);
            liquidOutputDirections = new int[]{1, 3, 0};
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(brine, 4f),
                new DrawBubbles(Color.valueOf("d4ef7a")){{ amount = 10; radius = 5f; spread = 9f; }},
                new DrawDefault(), new DrawLiquidOutputs());
        }};

        // H2 + Cl2 -> HCl.
        acidSynthesizer = new GenericCrafter("acid-synthesizer"){{
            requirements(Category.crafting, with(Items.lead, 50, Items.titanium, 30, Items.metaglass, 40, Items.silicon, 20, pressureCasing, 5));
            size = 2;
            health = 300;
            liquidCapacity = 30f;
            consumeLiquids(LiquidStack.with(Liquids.hydrogen, 3f / 60f, chlorine, 3f / 60f));
            consumePower(0.3f);
            outputLiquid = new LiquidStack(hydrochloricAcid, 6f / 60f);
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(hydrochloricAcid, 3f), new DrawDefault());
        }};

        // Scrap + HCl -> titanium + copper + lead, leaving acidic waste.
        acidLeachingVat = new ChemicalReactor("acid-leaching-vat"){{
            requirements(Category.crafting, with(Items.lead, 60, Items.titanium, 60, Items.silicon, 50, Items.metaglass, 50, pressureCasing, 10, titaniumGear, 5));
            size = 3;
            health = 600;
            itemCapacity = 12;
            liquidCapacity = 30f;
            craftTime = 90f;
            consumeItems(with(Items.scrap, 3));
            consumeLiquid(hydrochloricAcid, 6f / 60f);
            consumePower(1f);
            outputItems = with(Items.titanium, 1, Items.copper, 1, Items.lead, 1);
            outputLiquid = new LiquidStack(acidicWaste, 4f / 60f);
            craftEffect = Fx.smeltsmoke;
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(hydrochloricAcid, 4f),
                new DrawBubbles(Color.valueOf("f4f7c8")){{ amount = 8; radius = 4f; spread = 8f; }},
                new DrawDefault());
        }};

        // Sulfur + oxygen + water -> sulfuric acid (contact process). Strongly exothermic: waste heat leaves the front.
        sulfuricAcidPlant = new HeatProducer("sulfuric-acid-plant"){{
            requirements(Category.crafting, with(Items.lead, 60, Items.titanium, 40, Items.metaglass, 50, Items.silicon, 30, pressureCasing, 10));
            size = 2;
            health = 320;
            rotateDraw = false;
            liquidCapacity = 30f;
            itemCapacity = 10;
            craftTime = 60f;
            heatOutput = 4f;
            consumeItem(sulfur, 1);
            consumeLiquids(LiquidStack.with(oxygen, 4f / 60f, purifiedWater, 4f / 60f));
            consumePower(0.5f);
            outputLiquid = new LiquidStack(sulfuricAcid, 8f / 60f);
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(sulfuricAcid, 3f), new DrawDefault(), new DrawHeatOutput());
        }};

        // Lead + polyethylene casing + sulfuric acid electrolyte -> lead-acid cell.
        batteryAssemblyPlant = new GenericCrafter("battery-assembly-plant"){{
            requirements(Category.crafting, with(Items.copper, 80, Items.lead, 60, Items.silicon, 60, Items.graphite, 40, copperGear, 15, titaniumGear, 5));
            size = 2;
            health = 320;
            liquidCapacity = 20f;
            itemCapacity = 10;
            craftTime = 90f;
            consumeItems(with(Items.lead, 2, polyethylene, 1));
            consumeLiquid(sulfuricAcid, 3f / 60f);
            consumePower(1f);
            outputItem = new ItemStack(leadAcidCell, 1);
            craftEffect = Fx.pulverizeSmall;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("b7e07a"); }});
        }};

        // Acidic waste + sodium hydroxide -> water.
        industrialNeutralizer = new GenericCrafter("industrial-neutralizer"){{
            requirements(Category.crafting, with(Items.lead, 50, Items.titanium, 30, Items.metaglass, 40, pressureCasing, 5));
            size = 2;
            health = 320;
            liquidCapacity = 30f;
            consumeLiquids(LiquidStack.with(acidicWaste, 6f / 60f, sodiumHydroxide, 3f / 60f));
            consumePower(0.2f);
            outputLiquid = new LiquidStack(Liquids.water, 8f / 60f);
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(acidicWaste, 3f), new DrawDefault());
        }};

        // ---- nitrogen chemistry ----

        // Air + cryogenic cooling -> nitrogen + oxygen.
        airSeparationUnit = new GenericCrafter("air-separation-unit"){{
            requirements(Category.crafting, with(Items.lead, 100, Items.titanium, 80, Items.silicon, 60, reinforcedGlass, 30, Items.graphite, 40, industrialBearing, 10, pressureCasing, 10));
            size = 3;
            health = 700;
            rotate = true;
            rotateDraw = false;
            liquidCapacity = 40f;
            consumeLiquid(Liquids.cryofluid, 1f / 60f);
            consumePower(3f);
            outputLiquids = LiquidStack.with(Liquids.nitrogen, 8f / 60f, oxygen, 2f / 60f);
            liquidOutputDirections = new int[]{1, 3};
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(Liquids.cryofluid, 4f), new DrawDefault(), new DrawLiquidOutputs());
        }};

        // N2 + 3 H2 -> 2 NH3 (Haber process). Must reach operating pressure first.
        highPressureReactor = new ChemicalReactor("high-pressure-reactor"){{
            requirements(Category.crafting, with(Items.lead, 120, Items.titanium, 100, Items.thorium, 40, Items.silicon, 60, reinforcedGlass, 40, superalloyFrame, 10, industrialBearing, 10, pressureCasing, 20));
            size = 3;
            health = 900;
            liquidCapacity = 40f;
            craftTime = 60f;
            pressureRequired = 0.8f;
            consumeLiquids(LiquidStack.with(Liquids.nitrogen, 3f / 60f, Liquids.hydrogen, 9f / 60f));
            consumePower(3f);
            outputLiquid = new LiquidStack(ammonia, 6f / 60f);
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(ammonia, 4f), new DrawDefault());
        }};

        // Nitrogen gas + cryofluid refrigeration -> liquid nitrogen, a premium turret coolant.
        cryogenicCondenser = new AttributeCrafter("cryogenic-condenser"){{
            attribute = Biomes.cold; //faster on ice and snow
            baseEfficiency = 1f;
            boostScale = 0.15f;
            maxBoost = 0.5f;
            requirements(Category.crafting, with(Items.lead, 60, Items.titanium, 40, Items.silicon, 30, reinforcedGlass, 20, industrialBearing, 5, pressureCasing, 5));
            size = 2;
            health = 320;
            liquidCapacity = 30f;
            consumeLiquids(LiquidStack.with(Liquids.nitrogen, 6f / 60f, Liquids.cryofluid, 1f / 60f));
            consumePower(2f);
            outputLiquid = new LiquidStack(liquidNitrogen, 4f / 60f);
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(liquidNitrogen, 3f), new DrawDefault());
        }};

        // NH3 + O2 -> HNO3 (Ostwald process). Runs twice as fast over platinum gauze.
        nitricAcidPlant = new ChemicalReactor("nitric-acid-plant"){{
            requirements(Category.crafting, with(Items.lead, 60, Items.titanium, 50, Items.metaglass, 50, Items.silicon, 30, pressureCasing, 10));
            size = 2;
            health = 320;
            liquidCapacity = 30f;
            itemCapacity = 10;
            craftTime = 60f;
            catalyst = platinum;
            catalystOptional = true;
            catalystBoost = 2f;
            craftsPerCatalyst = 60;
            consumeLiquids(LiquidStack.with(ammonia, 4f / 60f, oxygen, 8f / 60f));
            consumePower(0.5f);
            outputLiquid = new LiquidStack(nitricAcid, 4f / 60f);
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(nitricAcid, 3f), new DrawDefault());
        }};

        // 3 HCl + 1 HNO3 -> aqua regia.
        aquaRegiaMixer = new GenericCrafter("aqua-regia-mixer"){{
            requirements(Category.crafting, with(polyethylene, 30, Items.titanium, 30, Items.metaglass, 30, pressureCasing, 5));
            size = 2;
            health = 300;
            liquidCapacity = 30f;
            consumeLiquids(LiquidStack.with(hydrochloricAcid, 6f / 60f, nitricAcid, 2f / 60f));
            consumePower(0.2f);
            outputLiquid = new LiquidStack(aquaRegia, 8f / 60f);
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(aquaRegia, 3f), new DrawDefault());
        }};

        // NH3 + HNO3 -> ammonium nitrate.
        ammoniumNitratePlant = new GenericCrafter("ammonium-nitrate-plant"){{
            requirements(Category.crafting, with(Items.lead, 50, Items.titanium, 40, Items.metaglass, 30, Items.silicon, 30, titaniumGear, 5, pressureCasing, 5));
            size = 2;
            health = 320;
            liquidCapacity = 30f;
            itemCapacity = 10;
            craftTime = 60f;
            consumeLiquids(LiquidStack.with(ammonia, 3f / 60f, nitricAcid, 3f / 60f));
            consumePower(0.3f);
            outputItem = new ItemStack(ammoniumNitrate, 1);
            craftEffect = Fx.pulverizeSmall;
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(nitricAcid, 3f), new DrawDefault());
        }};

        // Ammonium nitrate + coal -> blast compound (an alternative to the vanilla blast mixer).
        explosivesPlant = new GenericCrafter("explosives-plant"){{
            requirements(Category.crafting, with(Items.lead, 60, Items.titanium, 40, Items.silicon, 30, Items.graphite, 30, titaniumGear, 10));
            size = 2;
            health = 280;
            itemCapacity = 10;
            craftTime = 60f;
            consumeItems(with(ammoniumNitrate, 2, Items.coal, 1));
            consumePower(0.4f);
            outputItem = new ItemStack(Items.blastCompound, 2);
            craftEffect = Fx.pulverizeSmall;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("ff795e"); }});
        }};
    
        // ---- waste and emissions (flue gas comes from methane turbines and industrial heaters) ----

        // Flue gas + sodium hydroxide -> recovered sulfur + wastewater (wet flue-gas desulfurization).
        industrialScrubber = new GenericCrafter("industrial-scrubber"){{
            requirements(Category.crafting, with(Items.lead, 60, Items.titanium, 40, Items.metaglass, 40, Items.silicon, 20, pressureCasing, 10));
            size = 2;
            health = 320;
            liquidCapacity = 30f;
            itemCapacity = 10;
            craftTime = 120f;
            consumeLiquids(LiquidStack.with(flueGas, 12f / 60f, sodiumHydroxide, 2f / 60f));
            consumePower(0.5f);
            outputItem = new ItemStack(sulfur, 1);
            outputLiquid = new LiquidStack(wastewater, 4f / 60f);
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(flueGas, 3f),
                new DrawBubbles(Color.valueOf("d8dff0")){{ amount = 8; radius = 4f; spread = 7f; }},
                new DrawDefault());
        }};

        // Flue gas -> nitrogen + carbon dioxide through selective membranes. Nitrogen without cryofluid.
        gasSeparator = new GenericCrafter("gas-separator"){{
            requirements(Category.crafting, with(Items.lead, 80, Items.titanium, 60, Items.silicon, 50, Items.metaglass, 40, Items.plastanium, 20, pressureCasing, 10, industrialBearing, 5));
            size = 3;
            health = 600;
            rotate = true;
            rotateDraw = false;
            liquidCapacity = 40f;
            consumeLiquid(flueGas, 12f / 60f);
            consumePower(1.5f);
            outputLiquids = LiquidStack.with(Liquids.nitrogen, 8f / 60f, carbonDioxide, 3f / 60f);
            liquidOutputDirections = new int[]{1, 3};
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(flueGas, 4f), new DrawDefault(), new DrawLiquidOutputs());
        }};

        // Wastewater -> purified water + industrial sludge (settling and filtration).
        wastewaterTreatmentPlant = new GenericCrafter("wastewater-treatment-plant"){{
            requirements(Category.crafting, with(Items.copper, 60, Items.lead, 60, Items.metaglass, 40, Items.graphite, 30, copperGear, 15));
            size = 2;
            health = 320;
            liquidCapacity = 30f;
            itemCapacity = 10;
            craftTime = 120f;
            consumeLiquid(wastewater, 8f / 60f);
            consumePower(0.6f);
            outputItem = new ItemStack(industrialSludge, 1);
            outputLiquid = new LiquidStack(purifiedWater, 6f / 60f);
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(wastewater, 3f), new DrawDefault());
        }};

        // Sludge + heat -> scrap and sand back into the economy. Burning the organics gives off flue gas.
        thermalWasteProcessor = new HeatCrafter("thermal-waste-processor"){{
            requirements(Category.crafting, with(Items.lead, 60, Items.graphite, 40, Items.titanium, 30, Items.metaglass, 20, titaniumRod, 10));
            size = 2;
            health = 340;
            itemCapacity = 10;
            liquidCapacity = 20f;
            craftTime = 90f;
            heatRequirement = 5f;
            maxEfficiency = 2f;
            consumeItem(industrialSludge, 2);
            outputItems = with(Items.scrap, 1, Items.sand, 1);
            outputLiquid = new LiquidStack(flueGas, 2f / 60f);
            ignoreLiquidFullness = true; //flue gas is vented when not piped away
            craftEffect = Fx.smeltsmoke;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("ff8a3d"); }}, new DrawHeatInput());
        }};
    }
}
