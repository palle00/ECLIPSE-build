package eclipse.content.blocks;

import arc.graphics.*;
import eclipse.world.blocks.*;
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
    public static Block siliconRefinery, chipFabricator, circuitAssembler;

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
    
        // ---- wiring and control (wire comes from the Extruder) ----

        // Every circuit: circuit boards (copper-foil-clad polyethylene etched in hydrochloric acid; the spent
        // etchant leaves as acidic waste) and control units (microchip + circuit boards + an electric motor), and the electronic
        // parts that go on boards: resistors, capacitors, transistors, and insulated cable.
        // Its cost must not include circuit boards or later components (no circular costs).
        circuitAssembler = new RecipeCrafter("circuit-assembler"){{
            requirements(Category.crafting, with(Items.lead, 80, Items.silicon, 60, Items.metaglass, 30, titaniumGear, 8, pressureCasing, 5));
            size = 3;
            health = 500;
            itemCapacity = 20;
            liquidCapacity = 20f;
            recipe(2, 30f, with(Items.graphite, 1, copperWire, 1), with(resistor, 2));
            recipe(3, 30f, with(aluminiumFoil, 2, polyethylene, 1), with(capacitor, 2));
            recipe(3, 40f, with(highPuritySilicon, 1, copperWire, 1, polyethylene, 1), with(transistor, 2));
            recipe(3, 30f, with(copperWire, 2, syntheticRubber, 1), with(insulatedCable, 2));
            recipe(3, 60f, with(copperFoil, 2, polyethylene, 1, resistor, 2, capacitor, 1, transistor, 1), with(circuitBoard, 1))
                .liquidsIn(hydrochloricAcid, 2f / 60f).liquidsOut(acidicWaste, 1f / 60f);
            recipe(4, 90f, with(microchip, 1, circuitBoard, 2, electricMotor, 1, titaniumScrew, 2), with(controlUnit, 1));
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(hydrochloricAcid, 3f), new DrawDefault());
        }};
}
}
