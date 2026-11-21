package eclipse.content.blocks;

import arc.graphics.*;
import eclipse.world.blocks.*;
import mindustry.content.*;
import eclipse.content.*;
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
    public static Block fissionReactor, reprocessingPlant, breederReactor;

    public static void load(){
        // Fuel rods -> a large amount of heat out of the front (drives the boiler / steam turbine chain).
        fissionReactor = new HeatProducer("fission-reactor"){{
            requirements(Category.power, with(Items.lead, 200, Items.thorium, 80, superalloyFrame, 30, pressureCasing, 30, tungstenPlate, 20, controlUnit, 10));
            size = 3;
            health = 1400;
            itemCapacity = 10;
            craftTime = 600f;
            heatOutput = 40f;
            warmupRate = 0.05f;
            consumeItem(thoriumFuelRod, 1);
            outputItem = new ItemStack(spentFuelRod, 1);
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion("-core"){{ color = Color.valueOf("ff7ad1"); glowScale = 8f; }},
                new DrawRegion("-bridge", 0.3f, true), new DrawHeatOutput());
        }};

        // Fast breeder: a thorium blanket around the core breeds new fissile fuel, so each fuel rod comes out as two spent
        // rods (four thorium after reprocessing). 2.5x the fission reactor's heat; cooled by liquid sodium, which keeps
        // the neutrons fast. Its heat drives one Industrial Boiler at full output.
        breederReactor = new HeatProducer("breeder-reactor"){{
            requirements(Category.power, with(Items.thorium, 300, Items.lead, 400, tungstenPlate, 60, pressureCasing, 60, superalloyFrame, 40, controlUnit, 20));
            size = 4;
            health = 2200;
            itemCapacity = 10;
            liquidCapacity = 40f;
            craftTime = 600f;
            heatOutput = 100f;
            warmupRate = 0.04f;
            consumeItem(thoriumFuelRod, 1);
            consumeLiquid(liquidSodium, 2f / 60f); //coolant lost to the cover gas and to activation
            outputItem = new ItemStack(spentFuelRod, 2);
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion("-core"){{ color = Color.valueOf("ff7ad1"); glowScale = 8f; }},
                Animations.spin("-pump", 9f, -7.5f, -7f), Animations.spin("-pump", 9f, 7.5f, 7.5f), new DrawHeatOutput());
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
            consumeLiquids(LiquidStack.with(nitricAcid, 4f / 60f, diesel, 1f / 60f)); //PUREX: the extractant is carried in a kerosene-like diluent
            consumePower(2f);
            outputItem = new ItemStack(Items.thorium, 2);
            outputLiquid = new LiquidStack(acidicWaste, 3f / 60f);
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(nitricAcid, 4f),
                new DrawBubbles(Color.valueOf("fff4cc")){{ amount = 10; radius = 1.4f; spread = 3.5f; timeScl = 30f; }}, new DrawDefault());
        }};
    }
}
