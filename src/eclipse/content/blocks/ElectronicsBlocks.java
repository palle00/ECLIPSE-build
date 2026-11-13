package eclipse.content.blocks;

import arc.graphics.*;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.draw.*;

import static eclipse.content.EclipseItems.*;
import static eclipse.content.EclipseLiquids.*;
import static mindustry.type.ItemStack.*;

/** Semiconductors. */
public class ElectronicsBlocks{
    public static Block siliconRefinery, chipFabricator, wireDrawer, circuitPrinter, controlUnitAssembler;

    public static void load(){
        // Silicon + HCl + H2 -> high-purity silicon (Siemens process).
        siliconRefinery = new GenericCrafter("silicon-refinery"){{
            requirements(Category.crafting, with(Items.lead, 60, Items.titanium, 40, Items.metaglass, 40, Items.silicon, 30, pressureCasing, 10, titaniumRod, 10));
            size = 2;
            health = 320;
            itemCapacity = 10;
            liquidCapacity = 30f;
            craftTime = 60f;
            consumeItem(Items.silicon, 2);
            consumeLiquids(LiquidStack.with(hydrochloricAcid, 3f / 60f, Liquids.hydrogen, 3f / 60f));
            consumePower(1.5f);
            outputItem = new ItemStack(highPuritySilicon, 1);
            craftEffect = Fx.smeltsmoke;
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(hydrochloricAcid, 3f), new DrawDefault());
        }};

        // High-purity silicon + copper traces + polyethylene packaging -> microchips.
        chipFabricator = new GenericCrafter("chip-fabricator"){{
            requirements(Category.crafting, with(Items.copper, 80, Items.lead, 60, Items.silicon, 60, Items.titanium, 40, precisionComponent, 10, industrialBearing, 5));
            size = 2;
            health = 320;
            itemCapacity = 10;
            craftTime = 90f;
            consumeItems(with(highPuritySilicon, 1, Items.copper, 2, polyethylene, 1));
            consumePower(2f);
            outputItem = new ItemStack(microchip, 2);
            craftEffect = Fx.pulverizeSmall;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("6dff9a"); }});
        }};
    
        // ---- wiring and control ----

        // Copper -> copper wire.
        wireDrawer = new GenericCrafter("wire-drawing-machine"){{
            requirements(Category.crafting, with(Items.copper, 40, Items.lead, 30, copperGear, 10));
            size = 2;
            health = 220;
            itemCapacity = 10;
            craftTime = 30f;
            consumeItem(Items.copper, 1);
            consumePower(0.3f);
            outputItem = new ItemStack(copperWire, 2);
        }};

        // Copper wire traces on a polyethylene substrate, etched in hydrochloric acid -> circuit boards. The spent etchant leaves as acidic waste.
        circuitPrinter = new GenericCrafter("circuit-board-printer"){{
            requirements(Category.crafting, with(Items.lead, 60, Items.silicon, 40, Items.metaglass, 20, titaniumGear, 5, pressureCasing, 5));
            size = 2;
            health = 280;
            itemCapacity = 10;
            liquidCapacity = 20f;
            craftTime = 60f;
            consumeItems(with(copperWire, 2, polyethylene, 1));
            consumeLiquid(hydrochloricAcid, 2f / 60f);
            consumePower(0.6f);
            outputItem = new ItemStack(circuitBoard, 1);
            outputLiquid = new LiquidStack(acidicWaste, 1f / 60f);
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(hydrochloricAcid, 3f), new DrawDefault());
        }};

        // Microchip + circuit boards + an AlNiCo motor magnet -> control unit for automated machinery.
        controlUnitAssembler = new GenericCrafter("control-unit-assembler"){{
            requirements(Category.crafting, with(Items.titanium, 60, Items.silicon, 50, Items.lead, 40, precisionComponent, 10, industrialBearing, 10, circuitBoard, 10));
            size = 2;
            health = 300;
            itemCapacity = 10;
            craftTime = 90f;
            consumeItems(with(microchip, 1, circuitBoard, 2, alnicoMagnet, 1));
            consumePower(2f);
            outputItem = new ItemStack(controlUnit, 1);
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("5ab8ff"); }});
        }};
}
}
