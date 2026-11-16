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
import static mindustry.type.ItemStack.*;

/**
 * Mechanical manufacturing: the universal Gear Press ({@link RecipeCrafter}) and one fixed-recipe machine per other component. Components go into the build costs of
 * ECLIPSE buildings, module recipes and a few late vanilla buildings (see {@link #patchVanilla()}).
 * Each producer only needs earlier components to build, so there are no circular costs (checked by the test).
 */
public class ComponentBlocks{
    public static Block gearPress, extruder, metalPress, lathe, casingForge, bearingPress, frameForge, precisionAssembler, coreAssembler,
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

        // Titanium plates bolted round metaglass, sealed with graphite -> pressure casings for vessels and reactors.
        casingForge = new GenericCrafter("casing-forge"){{
            requirements(Category.crafting, with(Items.titanium, 50, Items.lead, 40, Items.metaglass, 30, titaniumGear, 10, titaniumRod, 10));
            size = 2;
            health = 320;
            itemCapacity = 10;
            craftTime = 90f;
            consumeItems(with(titaniumPlate, 2, titaniumBolt, 2, Items.metaglass, 2, Items.graphite, 1));
            consumePower(1f);
            outputItem = new ItemStack(pressureCasing, 1);
            craftEffect = Fx.pulverizeMedium;
        }};

        // Titanium ring races + copper cages + graphite lubricant -> industrial bearings.
        bearingPress = new GenericCrafter("bearing-press"){{
            requirements(Category.crafting, with(Items.titanium, 50, Items.silicon, 30, titaniumGear, 10, copperGear, 10));
            size = 2;
            health = 300;
            itemCapacity = 10;
            craftTime = 60f;
            consumeItems(with(titaniumRing, 2, Items.copper, 2, Items.graphite, 1));
            consumePower(1f);
            outputItem = new ItemStack(industrialBearing, 2);
            craftEffect = Fx.pulverizeSmall;
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

        // Titanium gears + screws + silicon + high-purity silicon -> precision components for electronics and smart blocks.
        precisionAssembler = new GenericCrafter("precision-assembler"){{
            requirements(Category.crafting, with(Items.titanium, 60, Items.silicon, 60, Items.lead, 40, industrialBearing, 10, titaniumGear, 10));
            size = 2;
            health = 320;
            itemCapacity = 10;
            craftTime = 90f;
            consumeItems(with(titaniumGear, 1, titaniumScrew, 2, Items.silicon, 2, highPuritySilicon, 1));
            consumePower(1.5f);
            outputItem = new ItemStack(precisionComponent, 1);
            craftEffect = Fx.pulverizeSmall;
        }};

        // Precision components + surge superalloy + platinum + phase fabric -> experimental cores for endgame machinery.
        coreAssembler = new GenericCrafter("experimental-core-assembler"){{
            requirements(Category.crafting, with(Items.titanium, 120, Items.silicon, 100, Items.surgeAlloy, 40, precisionComponent, 20, superalloyFrame, 10, carbonComposite, 10, controlUnit, 5));
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
                copperWire, circuitBoard, controlUnit);
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
