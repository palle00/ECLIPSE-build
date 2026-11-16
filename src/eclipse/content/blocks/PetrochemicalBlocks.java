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
    public static Block distillationColumn, steamCracker, carbonFiberFurnace, compositePress,
        compositeWall, compositeWallLarge, pressureReactor;

    public static void load(){
        // Oil -> naphtha + methane, sulfur recovered from the bottoms.
        distillationColumn = new GenericCrafter("distillation-column"){{
            requirements(Category.crafting, with(Items.lead, 100, Items.metaglass, 60, Items.titanium, 60, Items.silicon, 40, pressureCasing, 10, pumpUnit, 2));
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
            requirements(Category.crafting, with(Items.lead, 80, Items.metaglass, 40, Items.titanium, 80, Items.silicon, 60, pressureCasing, 10, industrialBearing, 5));
            size = 3;
            health = 700;
            liquidCapacity = 40f;
            heatRequirement = 10f;
            maxEfficiency = 2f;
            consumeLiquids(LiquidStack.with(naphtha, 6f / 60f, purifiedWater, 2f / 60f));
            outputLiquid = new LiquidStack(ethylene, 5f / 60f);
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(naphtha, 4f), new DrawDefault(), new DrawHeatInput());
        }};

    
        // ---- methanol chemistry: captured CO2 becomes the feedstock for the organic branch ----

        // Catalytic and high-pressure synthesis: ammonia (Haber-Bosch, N2 + 3 H2, under pressure), methanol (CO2 + 3 H2, under
        // pressure), olefins from methanol (propylene left, wastewater right), nitric acid over platinum gauze (Ostwald),
        // acrylonitrile over a molybdenum catalyst (SOHIO), and polyethylene and synthetic rubber over the titanium
        // chloride polymerization catalyst. Catalysts wear out slowly; pressure is built up with power.
        pressureReactor = new RecipeCrafter("pressure-reactor"){{
            requirements(Category.crafting, with(Items.lead, 80, titaniumPlate, 40, Items.silicon, 60, Items.metaglass, 40, pressureCasing, 15, piston, 4));
            size = 3;
            health = 700;
            itemCapacity = 20;
            liquidCapacity = 40f;
            recipe(3, 60f, ItemStack.empty, ItemStack.empty).liquidsIn(Liquids.nitrogen, 3f / 60f, Liquids.hydrogen, 9f / 60f)
                .liquidsOut(ammonia, 6f / 60f).pressure(0.8f);
            recipe(3, 60f, ItemStack.empty, ItemStack.empty).liquidsIn(carbonDioxide, 3f / 60f, Liquids.hydrogen, 9f / 60f)
                .liquidsOut(methanol, 4f / 60f).pressure(0.6f);
            recipe(3, 60f, ItemStack.empty, ItemStack.empty).liquidsIn(methanol, 8f / 60f)
                .liquidsOut(propylene, 4f / 60f, wastewater, 3f / 60f).sides(1, 3);
            recipe(3, 60f, ItemStack.empty, ItemStack.empty).liquidsIn(ammonia, 4f / 60f, oxygen, 8f / 60f)
                .liquidsOut(nitricAcid, 4f / 60f).catalyst(platinum, 60);
            recipe(3, 60f, ItemStack.empty, ItemStack.empty).liquidsIn(propylene, 3f / 60f, ammonia, 3f / 60f, oxygen, 4f / 60f)
                .liquidsOut(acrylonitrile, 4f / 60f).catalyst(molybdenum, 30);
            recipe(3, 60f, ItemStack.empty, with(polyethylene, 1)).liquidsIn(ethylene, 5f / 60f).catalyst(polymerCatalyst, 12);
            recipe(3, 60f, with(sulfur, 1), with(syntheticRubber, 2)).liquidsIn(ethylene, 3f / 60f, propylene, 3f / 60f).catalyst(polymerCatalyst, 12);
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(), new DrawDefault());
        }};

        // Acrylonitrile is spun into fiber, then oxidized and carbonized in a very hot furnace. Off-gases leave as flue gas.
        carbonFiberFurnace = new HeatCrafter("carbon-fiber-furnace"){{
            requirements(Category.crafting, with(Items.graphite, 60, Items.titanium, 80, Items.silicon, 60, superalloyPlate, 10, industrialBearing, 10, superalloyFrame, 10));
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
            requirements(Category.crafting, with(Items.graphite, 40, Items.titanium, 60, Items.silicon, 40, syntheticRubber, 20, piston, 4, precisionComponent, 5));
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
