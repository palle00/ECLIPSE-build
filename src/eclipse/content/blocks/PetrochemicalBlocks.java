package eclipse.content.blocks;

import arc.graphics.*;
import eclipse.world.blocks.*;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.production.*;
import mindustry.world.draw.*;

import static eclipse.content.EclipseItems.*;
import static eclipse.content.EclipseLiquids.*;
import static mindustry.type.ItemStack.*;

/** Organic chemistry: oil refining, cracking, catalysts, polymers, methanol chemistry and carbon composites. */
public class PetrochemicalBlocks{
    public static Block distillationColumn, steamCracker, catalystPlant, polymerizationReactor,
        methanolSynthesizer, olefinReactor, epoxyPlant, rubberPlant, acrylonitrileReactor, carbonFiberFurnace, compositePress,
        compositeWall, compositeWallLarge;

    public static void load(){
        // Oil -> naphtha + methane, sulfur recovered from the bottoms.
        distillationColumn = new GenericCrafter("distillation-column"){{
            requirements(Category.crafting, with(Items.lead, 100, Items.titanium, 60, Items.metaglass, 60, Items.silicon, 40, Items.graphite, 40, titaniumRod, 20, pressureCasing, 10));
            size = 3;
            health = 700;
            rotate = true;
            rotateDraw = false;
            liquidCapacity = 50f;
            itemCapacity = 10;
            craftTime = 60f;
            consumeLiquid(Liquids.oil, 12f / 60f);
            consumePower(1f);
            outputLiquids = LiquidStack.with(naphtha, 6f / 60f, methane, 4f / 60f);
            liquidOutputDirections = new int[]{1, 3};
            outputItem = new ItemStack(sulfur, 1);
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(Liquids.oil, 4f), new DrawDefault(), new DrawLiquidOutputs());
        }};

        // Naphtha + steam (purified water) + heat -> ethylene.
        steamCracker = new HeatCrafter("steam-cracker"){{
            requirements(Category.crafting, with(Items.lead, 80, Items.titanium, 80, Items.silicon, 60, Items.metaglass, 40, Items.graphite, 50, pressureCasing, 10, industrialBearing, 5));
            size = 3;
            health = 700;
            liquidCapacity = 40f;
            heatRequirement = 10f;
            maxEfficiency = 2f;
            consumeLiquids(LiquidStack.with(naphtha, 6f / 60f, purifiedWater, 2f / 60f));
            outputLiquid = new LiquidStack(ethylene, 5f / 60f);
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(naphtha, 4f), new DrawDefault(), new DrawHeatInput());
        }};

        // Titanium + chlorine -> Ziegler-Natta catalyst (titanium chloride based).
        catalystPlant = new GenericCrafter("catalyst-plant"){{
            requirements(Category.crafting, with(Items.lead, 60, Items.titanium, 50, Items.silicon, 50, Items.metaglass, 30, pressureCasing, 5, titaniumGear, 5));
            size = 2;
            health = 300;
            liquidCapacity = 20f;
            itemCapacity = 10;
            craftTime = 120f;
            consumeItem(Items.titanium, 1);
            consumeLiquid(chlorine, 3f / 60f);
            consumePower(1f);
            outputItem = new ItemStack(polymerCatalyst, 1);
            craftEffect = Fx.pulverizeSmall;
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(chlorine, 3f), new DrawDefault());
        }};

        // Ethylene -> polyethylene over a reusable catalyst.
        polymerizationReactor = new ChemicalReactor("polymerization-reactor"){{
            requirements(Category.crafting, with(Items.lead, 80, Items.titanium, 60, Items.silicon, 80, Items.metaglass, 40, pressureCasing, 10, titaniumGear, 10));
            size = 3;
            health = 650;
            liquidCapacity = 40f;
            itemCapacity = 10;
            craftTime = 60f;
            catalyst = polymerCatalyst;
            craftsPerCatalyst = 12;
            consumeLiquid(ethylene, 5f / 60f);
            consumePower(1.5f);
            outputItem = new ItemStack(polyethylene, 1);
            craftEffect = Fx.steam;
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(ethylene, 4f),
                new DrawBubbles(Color.valueOf("e3ecf0")){{ amount = 10; radius = 5f; spread = 9f; }},
                new DrawDefault());
        }};
    
        // ---- methanol chemistry: captured CO2 becomes the feedstock for the organic branch ----

        // CO2 + 3 H2 -> methanol (+ water). Runs at pressure over a copper-zinc bed.
        methanolSynthesizer = new ChemicalReactor("methanol-synthesizer"){{
            requirements(Category.crafting, with(Items.lead, 100, Items.titanium, 80, Items.silicon, 60, reinforcedGlass, 30, Items.copper, 80, superalloyFrame, 10, pressureCasing, 15));
            size = 3;
            health = 750;
            liquidCapacity = 40f;
            craftTime = 60f;
            pressureRequired = 0.6f;
            consumeLiquids(LiquidStack.with(carbonDioxide, 3f / 60f, Liquids.hydrogen, 9f / 60f));
            consumePower(1.5f);
            outputLiquid = new LiquidStack(methanol, 4f / 60f);
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(methanol, 4f), new DrawDefault());
        }};

        // Methanol -> propylene (methanol-to-olefins). The split-off water leaves as wastewater.
        olefinReactor = new ChemicalReactor("olefin-reactor"){{
            requirements(Category.crafting, with(Items.lead, 80, Items.titanium, 60, Items.silicon, 50, Items.metaglass, 40, Items.graphite, 40, pressureCasing, 10, titaniumRod, 10));
            size = 3;
            health = 650;
            rotate = true;
            rotateDraw = false;
            liquidCapacity = 40f;
            consumeLiquid(methanol, 8f / 60f);
            consumePower(1f);
            outputLiquids = LiquidStack.with(propylene, 4f / 60f, wastewater, 3f / 60f);
            liquidOutputDirections = new int[]{1, 3};
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(methanol, 4f), new DrawDefault(), new DrawLiquidOutputs());
        }};

        // Propylene + chlorine -> epichlorohydrin, cured with sodium hydroxide -> epoxy resin. Salty wastewater remains.
        epoxyPlant = new GenericCrafter("epoxy-plant"){{
            requirements(Category.crafting, with(Items.lead, 60, Items.titanium, 50, Items.metaglass, 40, Items.silicon, 30, polyethylene, 20, pressureCasing, 5, titaniumGear, 5));
            size = 2;
            health = 340;
            liquidCapacity = 30f;
            itemCapacity = 10;
            craftTime = 90f;
            consumeLiquids(LiquidStack.with(propylene, 3f / 60f, chlorine, 3f / 60f, sodiumHydroxide, 2f / 60f));
            consumePower(0.8f);
            outputItem = new ItemStack(epoxyResin, 1);
            outputLiquid = new LiquidStack(wastewater, 2f / 60f);
            craftEffect = Fx.pulverizeSmall;
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(propylene, 3f), new DrawDefault());
        }};

        // Ethylene + propylene over the Ziegler-Natta catalyst -> EPDM rubber, vulcanized with sulfur.
        rubberPlant = new ChemicalReactor("rubber-plant"){{
            requirements(Category.crafting, with(Items.lead, 80, Items.titanium, 60, Items.silicon, 60, Items.metaglass, 40, polyethylene, 30, pressureCasing, 10, industrialBearing, 5));
            size = 3;
            health = 650;
            liquidCapacity = 40f;
            itemCapacity = 10;
            craftTime = 60f;
            catalyst = polymerCatalyst;
            craftsPerCatalyst = 12;
            consumeItem(sulfur, 1);
            consumeLiquids(LiquidStack.with(ethylene, 3f / 60f, propylene, 3f / 60f));
            consumePower(1.2f);
            outputItem = new ItemStack(syntheticRubber, 2);
            craftEffect = Fx.steam;
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(propylene, 4f), new DrawDefault());
        }};

        // Propylene + ammonia + oxygen -> acrylonitrile (ammoxidation) over a molybdate catalyst.
        acrylonitrileReactor = new ChemicalReactor("acrylonitrile-reactor"){{
            requirements(Category.crafting, with(Items.lead, 80, Items.titanium, 80, Items.silicon, 60, reinforcedGlass, 30, syntheticRubber, 20, pressureCasing, 15, industrialBearing, 5));
            size = 3;
            health = 700;
            liquidCapacity = 40f;
            itemCapacity = 10;
            craftTime = 60f;
            catalyst = molybdenum;
            craftsPerCatalyst = 30;
            consumeLiquids(LiquidStack.with(propylene, 3f / 60f, ammonia, 3f / 60f, oxygen, 4f / 60f));
            consumePower(1.5f);
            outputLiquid = new LiquidStack(acrylonitrile, 4f / 60f);
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(acrylonitrile, 4f), new DrawDefault());
        }};

        // Acrylonitrile is spun into fiber, then oxidized and carbonized in a very hot furnace. Off-gases leave as flue gas.
        carbonFiberFurnace = new HeatCrafter("carbon-fiber-furnace"){{
            requirements(Category.crafting, with(Items.titanium, 80, Items.silicon, 60, Items.graphite, 60, superalloy, 20, syntheticRubber, 20, superalloyFrame, 10, industrialBearing, 10));
            size = 3;
            health = 700;
            liquidCapacity = 30f;
            itemCapacity = 10;
            craftTime = 60f;
            heatRequirement = 12f;
            maxEfficiency = 2f;
            consumeLiquid(acrylonitrile, 4f / 60f);
            consumePower(1f);
            outputItem = new ItemStack(carbonFiber, 1);
            outputLiquid = new LiquidStack(flueGas, 2f / 60f);
            ignoreLiquidFullness = true; //flue gas is vented when not piped away
            craftEffect = Fx.smeltsmoke;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("ff8a3d"); }}, new DrawHeatInput());
        }};

        // Carbon fiber laid up in epoxy and cured -> carbon composite (CFRP), light and very strong.
        compositePress = new GenericCrafter("composite-press"){{
            requirements(Category.crafting, with(Items.titanium, 60, Items.silicon, 40, Items.graphite, 40, syntheticRubber, 20, epoxyResin, 10, precisionComponent, 5, industrialBearing, 10));
            size = 2;
            health = 380;
            itemCapacity = 10;
            craftTime = 90f;
            consumeItems(with(carbonFiber, 2, epoxyResin, 1));
            consumePower(1.5f);
            outputItem = new ItemStack(carbonComposite, 1);
            craftEffect = Fx.pulverizeMedium;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("8fb8ff"); }});
        }};

        // Light, strong and partly deflects bullets. Sits between superalloy and surge-superalloy walls.
        compositeWall = new Wall("composite-wall"){{
            requirements(Category.defense, with(carbonComposite, 5));
            health = 250 * 4;
            chanceDeflect = 5f;
            absorbLasers = true;
        }};

        compositeWallLarge = new Wall("composite-wall-large"){{
            requirements(Category.defense, mult(compositeWall.requirements, 4));
            size = 2;
            health = 250 * 4 * 4;
            chanceDeflect = 5f;
            absorbLasers = true;
        }};
}
}
