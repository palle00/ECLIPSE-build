package eclipse.content.blocks;

import arc.graphics.*;
import eclipse.exploration.*;
import eclipse.world.blocks.*;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.heat.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import mindustry.world.draw.*;

import static eclipse.content.EclipseItems.*;
import static eclipse.content.EclipseLiquids.*;
import static mindustry.type.ItemStack.*;

/** Energy sector: generators, storage and heat. */
public class PowerBlocks{
    public static Block thermalStorage, coolingTower,
        hydrogenFuelCell, methaneTurbine, industrialBattery, lithiumBattery, industrialHeater,
        heatExchanger, industrialBoiler, steamTurbine, electricHeater, flowBattery;

    public static void load(){
        // ---- heat recovery: waste heat -> exchanger -> boiler -> steam turbine ----

        // Collects heat from the back and sides and passes it out of the front.
        heatExchanger = new HeatConductor("heat-exchanger"){{
            requirements(Category.crafting, with(aluminium, 30, Items.copper, 50, Items.graphite, 30, titaniumRod, 10));
            size = 3;
            health = 500;
            drawer = new DrawMulti(new DrawDefault(), new DrawHeatOutput(), new DrawHeatInput("-heat"));
            regionRotated1 = 1;
        }};

        // Heat + purified water -> steam.
        industrialBoiler = new HeatCrafter("industrial-boiler"){{
            requirements(Category.power, with(reinforcedGlass, 30, Items.titanium, 60, Items.lead, 60, Items.graphite, 40, pressureCasing, 15));
            size = 3;
            health = 650;
            liquidCapacity = 60f;
            heatRequirement = 10f;
            maxEfficiency = 4f; //one fission reactor drives one boiler at full output
            consumeLiquid(purifiedWater, 6f / 60f);
            outputLiquid = new LiquidStack(steam, 12f / 60f);
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(purifiedWater, 4f),
                new DrawBubbles(Color.valueOf("ffffff")){{ amount = 16; radius = 5f; spread = 10f; }},
                new DrawDefault(), new DrawHeatInput());
        }};

        // Power -> heat through molybdenum disilicide elements. No fuel needed, but costs a lot of power.
        electricHeater = new HeatProducer("electric-heater"){{
            requirements(Category.crafting, with(molybdenum, 20, Items.silicon, 30, Items.lead, 30, Items.graphite, 20, titaniumRod, 10, copperWire, 20));
            size = 2;
            health = 300;
            heatOutput = 4f;
            consumePower(3f);
            drawer = new DrawMulti(new DrawDefault(), new DrawHeatOutput());
        }};

        // Vanadium redox flow battery: the largest energy store.
        flowBattery = new Battery("flow-battery"){{
            requirements(Category.power, with(vanadium, 60, polyethylene, 40, aluminium, 30, microchip, 10, pressureCasing, 10, precisionComponent, 5, carbonFiber, 10));
            size = 3;
            health = 700;
            consumePowerBuffered(120000f);
            baseExplosiveness = 2f;
            drawer = new DrawMulti(new DrawDefault(), new DrawPower(), new DrawRegion("-top"));
        }};

        // Steam -> power; condensate is returned as purified water.
        steamTurbine = new ConsumeGenerator("steam-turbine"){{
            requirements(Category.power, with(aluminium, 60, microchip, 10, Items.titanium, 40, Items.silicon, 40, industrialBearing, 20, superalloyFrame, 5, cobalt, 20, alnicoMagnet, 10));
            size = 3;
            health = 600;
            liquidCapacity = 40f;
            powerProduction = 3f;
            consumeLiquid(steam, 12f / 60f);
            outputLiquid = new LiquidStack(purifiedWater, 5f / 60f);
            generateEffect = Fx.steam;
            effectChance = 0.04f;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("9fd8ff"); }});
        }};

        // H2 + O2 -> power + purified water. ~80% round trip vs. electrolysis; free power from chlor-alkali hydrogen.
        hydrogenFuelCell = new ConsumeGenerator("hydrogen-fuel-cell"){{
            requirements(Category.power, with(Items.copper, 50, Items.lead, 40, Items.silicon, 40, Items.graphite, 30, Items.metaglass, 20, copperGear, 10, copperWire, 15));
            size = 2;
            health = 300;
            liquidCapacity = 20f;
            powerProduction = 0.9f;
            consumeLiquids(LiquidStack.with(Liquids.hydrogen, 6f / 60f, oxygen, 3f / 60f));
            outputLiquid = new LiquidStack(purifiedWater, 3f / 60f);
            generateEffect = Fx.none;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("7fc8ff"); }});
        }};

        // Methane -> power.
        methaneTurbine = new ConsumeGenerator("methane-turbine"){{
            requirements(Category.power, with(Items.copper, 60, Items.lead, 50, Items.titanium, 40, Items.silicon, 30, Items.graphite, 30, industrialBearing, 10, titaniumGear, 5));
            size = 2;
            health = 320;
            liquidCapacity = 20f;
            powerProduction = 4f;
            consumeLiquid(methane, 6f / 60f);
            outputLiquid = new LiquidStack(flueGas, 12f / 60f); //vented when not piped away
            generateEffect = Fx.generatespark;
            effectChance = 0.05f;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("ff9a4a"); }});
        }};

        // Lead-acid bank: better storage density than the large battery, costs processed cells.
        industrialBattery = new Battery("industrial-battery"){{
            requirements(Category.power, with(leadAcidCell, 20, Items.lead, 40, Items.titanium, 20, Items.silicon, 10, titaniumRod, 5, copperWire, 10));
            size = 2;
            health = 400;
            consumePowerBuffered(25000f);
            baseExplosiveness = 3f;
            drawer = new DrawMulti(new DrawDefault(), new DrawPower(), new DrawRegion("-top"));
        }};

        // Twice the industrial battery's capacity in the same footprint.
        lithiumBattery = new Battery("lithium-battery"){{
            requirements(Category.power, with(lithium, 30, polyethylene, 20, Items.silicon, 20, Items.graphite, 20, titaniumRod, 5, cobalt, 10, copperWire, 10));
            size = 2;
            health = 400;
            consumePowerBuffered(50000f);
            baseExplosiveness = 4f;
            drawer = new DrawMulti(new DrawDefault(), new DrawPower(), new DrawRegion("-top"));
        }};

        // Burns methane to produce heat for heat-driven processes (shares the vanilla heat system).
        industrialHeater = new HeatProducer("industrial-heater"){{
            requirements(Category.crafting, with(Items.lead, 40, Items.graphite, 30, Items.silicon, 20, Items.metaglass, 20, titaniumRod, 5));
            size = 2;
            health = 300;
            liquidCapacity = 20f;
            heatOutput = 5f;
            consumeLiquid(methane, 2f / 60f);
            outputLiquid = new LiquidStack(flueGas, 4f / 60f);
            ignoreLiquidFullness = true; //flue gas is vented when not piped away
            drawer = new DrawMulti(new DrawDefault(), new DrawHeatOutput());
        }};
    
        // ---- heat storage and cooling ----

        // Charges from incoming heat and keeps passing it on after the heaters stop.
        thermalStorage = new ThermalStorage("thermal-storage"){{
            requirements(Category.crafting, with(Items.titanium, 40, Items.graphite, 40, Items.metaglass, 20, pressureCasing, 5, titaniumRod, 10));
            size = 2;
            health = 400;
            capacity = 600f;
            maxOutput = 20f;
            drawer = new DrawMulti(new DrawDefault(), new DrawHeatOutput(), new DrawHeatInput("-heat"));
            regionRotated1 = 1;
        }};

        // Water -> chilled water by evaporative cooling. Some water is lost to evaporation.
        coolingTower = new AttributeCrafter("cooling-tower"){{
            attribute = Biomes.cold; //faster on ice and snow
            baseEfficiency = 1f;
            boostScale = 0.15f;
            maxBoost = 0.6f;
            requirements(Category.crafting, with(Items.copper, 60, Items.lead, 60, Items.metaglass, 40, copperGear, 15, titaniumRod, 10));
            size = 3;
            health = 500;
            liquidCapacity = 40f;
            consumeLiquid(Liquids.water, 12f / 60f);
            consumePower(0.4f);
            outputLiquid = new LiquidStack(chilledWater, 10f / 60f);
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(chilledWater, 4f), new DrawDefault());
        }};
}
}
