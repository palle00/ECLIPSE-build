package eclipse.content.blocks;

import arc.graphics.*;
import arc.struct.*;
import eclipse.world.blocks.*;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.draw.*;

import static eclipse.content.EclipseItems.*;
import static eclipse.content.EclipseLiquids.*;
import static mindustry.type.ItemStack.*;

/**
 * Mechanical manufacturing: the universal machines ({@link RecipeCrafter}) for forms, parts and components. Components go into the build costs of
 * ECLIPSE buildings, module recipes and a few late vanilla buildings (see {@link #patchVanilla()}).
 * Each producer only needs earlier components to build, so there are no circular costs (checked by the test).
 */
public class ComponentBlocks{
    public static Block gearPress, extruder, metalPress, lathe, assembler, frameForge, coreAssembler,
        componentDepot;

    public static void load(){
        // Every gear: copper gears (tier I) for basic machinery, pumps, sorters and tier I modules;
        // titanium gears (tier II) for industrial machinery, reactors and tier II modules.
        gearPress = new RecipeCrafter("gear-press"){{
            requirements(Category.crafting, with(Items.copper, 40, Items.lead, 25));
            size = 2;
            health = 220;
            itemCapacity = 10;
            recipe(1, 30f, with(Items.copper, 2), with(copperGear, 1));
            recipe(2, 60f, with(Items.titanium, 3), with(titaniumGear, 2));
            recipe(3, 60f, with(superalloy, 3), with(superalloyGear, 2));
            craftEffect = Fx.pulverizeSmall;
        }};

        // Every rod and wire: copper wire (tier I), titanium rods (tier II, graphite as die lubricant),
        // tungsten rods and aluminium wire (tier III). Its cost must not need copper wire, rods or later components.
        extruder = new RecipeCrafter("extruder"){{
            requirements(Category.crafting, with(Items.copper, 60, Items.lead, 40, Items.graphite, 20, copperGear, 10));
            size = 2;
            health = 260;
            itemCapacity = 10;
            recipe(1, 30f, with(Items.copper, 1), with(copperWire, 2));
            recipe(2, 60f, with(Items.titanium, 2, Items.graphite, 1), with(titaniumRod, 2));
            recipe(3, 60f, with(tungsten, 2, Items.graphite, 1), with(tungstenRod, 2));
            recipe(3, 30f, with(aluminium, 1), with(aluminiumWire, 2));
            recipe(3, 40f, with(copperWire, 2, epoxyResin, 1), with(magnetWire, 2)); //enamelled winding wire
            craftEffect = Fx.pulverizeSmall;
        }};

        // Every plate, rolled from metal, and foil, rolled thin from plate. Tier I for copper and lead,
        // II for titanium, III for aluminium, superalloy and tungsten.
        metalPress = new RecipeCrafter("metal-press"){{
            requirements(Category.crafting, with(Items.copper, 60, Items.lead, 40, Items.graphite, 20, copperGear, 10));
            size = 2;
            health = 260;
            itemCapacity = 10;
            recipe(1, 40f, with(Items.copper, 2), with(copperPlate, 1));
            recipe(1, 40f, with(Items.lead, 2), with(leadPlate, 1));
            recipe(2, 50f, with(Items.titanium, 2), with(titaniumPlate, 1));
            recipe(3, 40f, with(aluminium, 2), with(aluminiumPlate, 1));
            recipe(3, 60f, with(superalloy, 2), with(superalloyPlate, 1));
            recipe(3, 60f, with(tungsten, 2), with(tungstenPlate, 1));
            recipe(1, 30f, with(copperPlate, 1), with(copperFoil, 2));
            recipe(3, 30f, with(aluminiumPlate, 1), with(aluminiumFoil, 2));
            craftEffect = Fx.pulverizeSmall;
        }};

        // Small titanium parts turned from rods: bolts, screws (threaded bolts), rings and springs. All tier II.
        lathe = new RecipeCrafter("lathe"){{
            requirements(Category.crafting, with(Items.titanium, 40, Items.lead, 30, Items.graphite, 20, copperGear, 10));
            size = 2;
            health = 260;
            itemCapacity = 10;
            recipe(2, 30f, with(titaniumRod, 1), with(titaniumBolt, 2));
            recipe(2, 20f, with(titaniumBolt, 1), with(titaniumScrew, 1));
            recipe(2, 40f, with(titaniumRod, 1), with(titaniumRing, 1));
            recipe(2, 40f, with(titaniumRod, 1), with(titaniumSpring, 2));
            craftEffect = Fx.pulverizeSmall;
        }};

        // Parts and components: motors, pumps, pistons, rotors, heating coils, sensors and emitters, and the components
        // built from them (pressure casings, bearings, precision components, lead-acid cells, fuel rods).
        // Its cost must not need pressure casings or later components (no circular costs).
        assembler = new RecipeCrafter("assembler"){{
            requirements(Category.crafting, with(Items.titanium, 80, Items.lead, 60, Items.silicon, 40, Items.graphite, 40, titaniumGear, 10, titaniumRod, 10));
            size = 3;
            health = 600;
            itemCapacity = 20;
            liquidCapacity = 20f;
            //parts
            recipe(2, 60f, with(copperWire, 4, titaniumRod, 1, titaniumPlate, 1, titaniumBolt, 2), with(electricMotor, 1));
            recipe(3, 90f, with(magnetWire, 2, alnicoMagnet, 1, titaniumRod, 1, titaniumBolt, 2), with(permanentMagnetMotor, 1));
            recipe(5, 120f, with(superconductor, 2, superalloyPlate, 2, titaniumRod, 1), with(superconductingMotor, 1))
                .liquidsIn(liquidNitrogen, 2f / 60f);
            recipe(2, 40f, with(titaniumPlate, 2, titaniumRing, 1, titaniumScrew, 2), with(rotor, 1));
            recipe(2, 60f, with(electricMotor, 1, rotor, 1, titaniumRing, 2, titaniumPlate, 1), with(pumpUnit, 1));
            recipe(2, 60f, with(electricMotor, 1, titaniumPlate, 1, titaniumRod, 1, titaniumSpring, 2), with(piston, 1));
            recipe(3, 60f, with(tungstenRod, 2, molybdenum, 1), with(heatingCoil, 1));
            recipe(3, 60f, with(transistor, 1, resistor, 2, highPuritySilicon, 1, reinforcedGlass, 1), with(sensor, 1));
            recipe(4, 90f, with(sensor, 1, transistor, 2, platinum, 1, reinforcedGlass, 1), with(emitter, 1));
            //components
            recipe(2, 90f, with(titaniumPlate, 2, titaniumBolt, 2, Items.metaglass, 2, Items.graphite, 1), with(pressureCasing, 1));
            recipe(2, 60f, with(titaniumRing, 2, Items.copper, 2, Items.graphite, 1), with(industrialBearing, 2));
            recipe(3, 90f, with(titaniumGear, 1, titaniumScrew, 2, sensor, 1), with(precisionComponent, 1));
            recipe(3, 90f, with(leadPlate, 2, polyethylene, 1), with(leadAcidCell, 1)).liquidsIn(sulfuricAcid, 3f / 60f);
            recipe(3, 120f, with(Items.thorium, 4, superalloy, 1), with(thoriumFuelRod, 1));
            craftEffect = Fx.pulverizeMedium;
        }};

        // Superalloy plates + titanium rods, hot-formed and bolted -> superalloy machine frames for heavy and hot machinery.
        frameForge = new HeatCrafter("frame-forge"){{
            requirements(Category.crafting, with(Items.titanium, 60, Items.silicon, 40, Items.graphite, 40, industrialBearing, 10, pressureCasing, 10));
            size = 3;
            health = 650;
            itemCapacity = 10;
            craftTime = 120f;
            heatRequirement = 8f;
            maxEfficiency = 2f;
            consumeItems(with(superalloyPlate, 2, titaniumRod, 2, titaniumBolt, 2));
            consumePower(1f);
            outputItem = new ItemStack(superalloyFrame, 1);
            craftEffect = Fx.smeltsmoke;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("ffb46b"); }}, new DrawHeatInput());
        }};

        // Precision components + surge superalloy + platinum + phase fabric -> experimental cores for endgame machinery.
        coreAssembler = new GenericCrafter("experimental-core-assembler"){{
            requirements(Category.crafting, with(Items.titanium, 120, Items.silicon, 100, Items.surgeAlloy, 40, superalloyFrame, 10, precisionComponent, 20, controlUnit, 5));
            size = 3;
            health = 800;
            itemCapacity = 10;
            craftTime = 180f;
            consumeItems(with(precisionComponent, 2, surgeSuperalloy, 1, platinum, 1, Items.phaseFabric, 1));
            consumePower(4f);
            outputItem = new ItemStack(experimentalCore, 1);
            craftEffect = Fx.smeltsmoke;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("c88fff"); }});
        }};
    }

    /** Components never need to be belted to the core: depots store them anywhere and keep the core stocked. */
    public static void loadDepot(){
        componentDepot = new ComponentDepot("component-depot"){{
            requirements(Category.effect, with(Items.titanium, 100, Items.lead, 100, copperGear, 30, titaniumGear, 10));
            size = 3;
            health = 600;
            itemCapacity = 1000;
            accepted.addAll(copperGear, titaniumRod, titaniumGear, pressureCasing, industrialBearing, superalloyFrame, precisionComponent, experimentalCore,
                copperWire, circuitBoard, controlUnit,
                //forms and parts: building costs use them too
                copperPlate, leadPlate, titaniumPlate, aluminiumPlate, superalloyPlate, tungstenPlate, copperFoil, aluminiumFoil,
                tungstenRod, aluminiumWire, magnetWire, titaniumBolt, titaniumScrew, titaniumRing, titaniumSpring, superalloyGear,
                resistor, capacitor, transistor, insulatedCable,
                electricMotor, permanentMagnetMotor, superconductingMotor, pumpUnit, piston, rotor, heatingCoil, sensor, emitter);
        }};
    }

    /** A few late vanilla buildings now need ECLIPSE components. Research costs stay vanilla, so the campaign can't soft-lock. */
    public static void patchVanilla(){
        addCost(Blocks.laserDrill, industrialBearing, 10);
        addCost(Blocks.blastDrill, industrialBearing, 20, titaniumGear, 20);
        addCost(Blocks.thoriumReactor, pressureCasing, 20, superalloyFrame, 10);
        addCost(Blocks.impactReactor, superalloyFrame, 20, experimentalCore, 10, alnicoMagnet, 20); //magnetic confinement
        addCost(Blocks.largeSolarPanel, precisionComponent, 5);
    }

    static void addCost(Block block, Object... extra){
        Seq<ItemStack> cost = new Seq<>(block.requirements);
        cost.addAll(with(extra));
        block.requirements = cost.toArray(ItemStack.class);
    }
}
