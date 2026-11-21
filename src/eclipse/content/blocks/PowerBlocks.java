package eclipse.content.blocks;

import arc.graphics.*;
import eclipse.exploration.*;
import eclipse.world.blocks.*;
import mindustry.content.*;
import eclipse.content.*;
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
        heatExchanger, industrialBoiler, steamTurbine, electricHeater, flowBattery, combinedCycleTurbine, solarFarm;

    public static void load(){
        // ---- heat recovery: waste heat -> exchanger -> boiler -> steam turbine ----

        // Collects heat from the back and sides and passes it out of the front.
        heatExchanger = new HeatConductor("heat-exchanger"){{
            requirements(Category.crafting, with(aluminiumPlate, 15, Items.copper, 50, Items.graphite, 30, titaniumRod, 10));
            size = 3;
            health = 500;
            drawer = new DrawMulti(new DrawDefault(), new DrawHeatRegion("-core"){{ color = Color.valueOf("ff8a3d"); pulse = 0.25f; }},
                new DrawHeatOutput(), new DrawHeatInput("-heat"));
            regionRotated1 = 1;
        }};

        // Heat + purified water -> steam.
        industrialBoiler = new HeatCrafter("industrial-boiler"){{
            requirements(Category.power, with(reinforcedGlass, 30, Items.titanium, 60, pumpUnit, 2, Items.graphite, 40, pressureCasing, 15));
            size = 3;
            health = 650;
            liquidCapacity = 60f;
            heatRequirement = 10f;
            maxEfficiency = 10f; //up to 100 heat: a fission reactor (40) drives it at 4x, a breeder reactor (100) at 10x
            consumeLiquid(purifiedWater, 6f / 60f);
            outputLiquid = new LiquidStack(steam, 12f / 60f);
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(purifiedWater, 4f),
                new DrawBubbles(Color.valueOf("ffffff")){{ amount = 20; radius = 1.5f; spread = 6f; timeScl = 20f; }},
                new DrawDefault(), new Animations.DrawMove("-needle"){{ pivotX = 9f; pivotY = -9.5f; swing = 40f; period = 120f; }}, new DrawHeatInput());
        }};

        // Power -> heat through molybdenum disilicide elements. No fuel needed, but costs a lot of power.
        electricHeater = new HeatProducer("electric-heater"){{
            requirements(Category.crafting, with(molybdenum, 20, Items.silicon, 30, Items.lead, 30, Items.graphite, 20, heatingCoil, 4, copperWire, 20));
            size = 2;
            health = 300;
            heatOutput = 4f;
            consumePower(3f);
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion("-coils"){{ color = Color.valueOf("ff6a3d"); alpha = 0.9f; glowScale = 5f; glowIntensity = 0.3f; }}, new DrawHeatOutput());
        }};

        // Vanadium redox flow battery: the largest energy store.
        flowBattery = new Battery("flow-battery"){{
            requirements(Category.power, with(vanadium, 60, polyethylene, 40, pressureCasing, 10, pumpUnit, 4, aluminiumWire, 30, carbonFiber, 10));
            size = 3;
            health = 700;
            consumePowerBuffered(120000f);
            baseExplosiveness = 2f;
            drawer = new DrawMulti(new DrawDefault(), Animations.spin("-pump", 4f, -7f, -4.5f), Animations.spin("-pump", 4f, 7f, -4.5f), new DrawPower());
        }};

        // Steam -> power; condensate is returned as purified water.
        steamTurbine = new ConsumeGenerator("steam-turbine"){{
            requirements(Category.power, with(aluminium, 60, cobalt, 20, industrialBearing, 20, superalloyFrame, 5, titaniumRing, 20, permanentMagnetMotor, 4));
            size = 3;
            health = 600;
            liquidCapacity = 40f;
            powerProduction = 15f; //900/s: a fission reactor + boiler + 4 turbines gives 3600/s, twice a thorium reactor
            consumeLiquid(steam, 12f / 60f);
            outputLiquid = new LiquidStack(purifiedWater, 5f / 60f);
            generateEffect = Fx.steam;
            effectChance = 0.04f;
            drawer = new DrawMulti(new DrawDefault(), new DrawRegion("-rotor", 8f, true), new DrawGlowRegion(){{ color = Color.valueOf("9fd8ff"); }});
        }};

        // H2 + O2 -> power + purified water. ~80% round trip vs. electrolysis; free power from chlor-alkali hydrogen.
        hydrogenFuelCell = new ConsumeGenerator("hydrogen-fuel-cell"){{
            requirements(Category.power, with(Items.copper, 50, Items.lead, 40, Items.metaglass, 20, Items.graphite, 30, Items.silicon, 40, copperWire, 15));
            size = 2;
            health = 300;
            liquidCapacity = 20f;
            powerProduction = 1.5f; //90/s
            consumeLiquids(LiquidStack.with(Liquids.hydrogen, 6f / 60f, oxygen, 3f / 60f));
            outputLiquid = new LiquidStack(purifiedWater, 3f / 60f);
            generateEffect = Fx.none;
            drawer = new DrawMulti(new DrawDefault(), Animations.spin("-fan", 8f, -5.5f, -4.5f), new DrawGlowRegion(){{ color = Color.valueOf("7fc8ff"); }});
        }};

        // Methane -> power.
        methaneTurbine = new ConsumeGenerator("methane-turbine"){{
            requirements(Category.power, with(Items.lead, 50, Items.titanium, 40, Items.silicon, 30, industrialBearing, 10, magnetWire, 20, rotor, 4));
            size = 2;
            health = 320;
            liquidCapacity = 20f;
            powerProduction = 6f; //360/s
            consumeLiquid(methane, 6f / 60f);
            outputLiquid = new LiquidStack(flueGas, 12f / 60f); //vented when not piped away
            generateEffect = Fx.generatespark;
            effectChance = 0.05f;
            drawer = new DrawMulti(new DrawDefault(), Animations.spin("-rotor", 8f, 1.5f, -0.5f), new DrawGlowRegion(){{ color = Color.valueOf("ff9a4a"); }});
        }};

        // Combined cycle: a large gas turbine (about 1.7x the Methane Turbine's power per methane) whose hot exhaust
        // also gives heat out of the front, so a boiler and steam turbine there add even more power.
        combinedCycleTurbine = new CombinedCycleTurbine("combined-cycle-turbine"){{
            requirements(Category.power, with(Items.titanium, 200, superalloyFrame, 20, pressureCasing, 20, industrialBearing, 20, rotor, 10, permanentMagnetMotor, 6));
            size = 4;
            health = 1100;
            liquidCapacity = 80f;
            powerProduction = 40f; //2400/s
            heatOutput = 10f; //drives one Industrial Boiler -> one Steam Turbine (+900/s)
            consumeLiquid(methane, 20f / 60f); //120 power per methane: twice the Methane Turbine, beating heater + boiler (90)
            outputLiquid = new LiquidStack(flueGas, 23f / 60f); //vented when not piped away
            generateEffect = Fx.generatespark;
            effectChance = 0.08f;
            drawer = new DrawMulti(new DrawDefault(), Animations.spin("-rotor", 8f, 2f, 0f),
                new DrawGlowRegion("-flame"){{ color = Color.valueOf("ff9a4a"); alpha = 0.9f; glowScale = 2f; glowIntensity = 0.5f; }}, new DrawHeatOutput());
        }};

        // Photovoltaic field of high-purity silicon cells: no fuel, follows the map's light like vanilla solar panels.
        solarFarm = new SolarGenerator("solar-farm"){{
            requirements(Category.power, with(highPuritySilicon, 60, reinforcedGlass, 30, aluminiumPlate, 30, copperWire, 40, Items.lead, 60));
            size = 3;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion("-glint"){{ color = Color.white; alpha = 0.45f; glowScale = 30f; glowIntensity = 0.8f; }});
            health = 500;
            powerProduction = 5f; //300/s in full light
        }};

        // Lead-acid bank: better storage density than the large battery, costs processed cells.
        industrialBattery = new Battery("industrial-battery"){{
            requirements(Category.power, with(leadAcidCell, 20, Items.lead, 40, Items.titanium, 20, Items.silicon, 10, titaniumRod, 5, insulatedCable, 10));
            size = 2;
            health = 400;
            consumePowerBuffered(25000f);
            baseExplosiveness = 3f;
            drawer = new DrawMulti(new DrawDefault(), new DrawPower());
        }};

        // Twice the industrial battery's capacity in the same footprint.
        lithiumBattery = new Battery("lithium-battery"){{
            requirements(Category.power, with(lithium, 30, cobalt, 10, Items.graphite, 20, polyethylene, 20, copperFoil, 10, aluminiumFoil, 10));
            size = 2;
            health = 400;
            consumePowerBuffered(50000f);
            baseExplosiveness = 4f;
            drawer = new DrawMulti(new DrawDefault(), new DrawPower());
        }};

        // Burns methane to produce heat for heat-driven processes (shares the vanilla heat system).
        industrialHeater = new HeatProducer("industrial-heater"){{
            requirements(Category.crafting, with(Items.lead, 40, Items.graphite, 30, Items.silicon, 20, Items.metaglass, 20, titaniumRod, 5));
            size = 2;
            health = 300;
            liquidCapacity = 20f;
            heatOutput = 5f;
            consumeLiquid(methane, 5f / 60f); //90 power per methane through a boiler: below the Combined-Cycle Turbine
            outputLiquid = new LiquidStack(flueGas, 10f / 60f);
            ignoreLiquidFullness = true; //flue gas is vented when not piped away
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion("-flame"){{ color = Color.valueOf("ff8a3d"); alpha = 0.9f; glowScale = 2f; glowIntensity = 0.5f; }}, new DrawHeatOutput());
        }};
    
        // ---- heat storage and cooling ----

        // Charges from incoming heat and keeps passing it on after the heaters stop.
        thermalStorage = new ThermalStorage("thermal-storage"){{
            requirements(Category.crafting, with(Items.titanium, 40, Items.graphite, 40, Items.metaglass, 20, pressureCasing, 5, titaniumRod, 10));
            size = 2;
            health = 400;
            capacity = 600f;
            maxOutput = 20f;
            drawer = new DrawMulti(new DrawDefault(), new DrawHeatRegion("-core"){{ color = Color.valueOf("ff8a3d"); pulse = 0.2f; }}, new DrawHeatOutput(), new DrawHeatInput("-heat"));
            regionRotated1 = 1;
        }};

        // Water -> chilled water by evaporative cooling. Some water is lost to evaporation.
        coolingTower = new AttributeCrafter("cooling-tower"){{
            attribute = Biomes.cold; //faster on ice and snow
            baseEfficiency = 1f;
            boostScale = 0.15f;
            maxBoost = 0.6f;
            requirements(Category.crafting, with(Items.copper, 60, Items.lead, 60, Items.metaglass, 40, copperGear, 15, rotor, 2));
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
