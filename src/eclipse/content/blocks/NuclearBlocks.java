package eclipse.content.blocks;

import arc.graphics.*;
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

/** Nuclear fuel cycle: fabrication -> fission heat -> reprocessing. */
public class NuclearBlocks{
    public static Block fuelRodFabricator, fissionReactor, reprocessingPlant;

    public static void load(){
        // Thorium pellets sealed in superalloy cladding.
        fuelRodFabricator = new GenericCrafter("fuel-rod-fabricator"){{
            requirements(Category.crafting, with(Items.titanium, 80, Items.silicon, 60, Items.lead, 80, superalloy, 20, titaniumGear, 10, superalloyFrame, 5));
            size = 2;
            health = 400;
            itemCapacity = 20;
            craftTime = 120f;
            consumeItems(with(Items.thorium, 4, superalloy, 1));
            consumePower(1.5f);
            outputItem = new ItemStack(thoriumFuelRod, 1);
            craftEffect = Fx.pulverizeSmall;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("ff7ad1"); }});
        }};

        // Fuel rods -> a large amount of heat out of the front (drives the boiler / steam turbine chain).
        fissionReactor = new HeatProducer("fission-reactor"){{
            requirements(Category.power, with(reinforcedGlass, 60, superalloy, 60, Items.thorium, 80, Items.lead, 200, Items.silicon, 100, microchip, 20, superalloyFrame, 30, pressureCasing, 30, precisionComponent, 10, carbonComposite, 20, tungsten, 40, controlUnit, 10));
            size = 3;
            health = 1400;
            itemCapacity = 10;
            craftTime = 600f;
            heatOutput = 40f;
            warmupRate = 0.05f;
            consumeItem(thoriumFuelRod, 1);
            outputItem = new ItemStack(spentFuelRod, 1);
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("ff7ad1"); }}, new DrawHeatOutput());
        }};

        // Spent fuel + nitric acid -> recovered thorium (PUREX), leaving acidic waste.
        reprocessingPlant = new ChemicalReactor("reprocessing-plant"){{
            requirements(Category.crafting, with(polyethylene, 40, reinforcedGlass, 30, Items.titanium, 60, Items.silicon, 40, pressureCasing, 20, industrialBearing, 5));
            size = 3;
            health = 700;
            itemCapacity = 10;
            liquidCapacity = 30f;
            craftTime = 120f;
            consumeItem(spentFuelRod, 1);
            consumeLiquid(nitricAcid, 4f / 60f);
            consumePower(2f);
            outputItem = new ItemStack(Items.thorium, 2);
            outputLiquid = new LiquidStack(acidicWaste, 3f / 60f);
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(nitricAcid, 4f), new DrawDefault());
        }};
    }
}
