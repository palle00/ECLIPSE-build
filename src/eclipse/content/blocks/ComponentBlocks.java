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
 * Mechanical manufacturing: one fixed-recipe machine per component. Components go into the build costs of
 * ECLIPSE buildings, module recipes and a few late vanilla buildings (see {@link #patchVanilla()}).
 * Each producer only needs earlier components to build, so there are no circular costs (checked by the test).
 */
public class ComponentBlocks{
    public static Block gearPress, metalExtruder, heavyGearPress, casingForge, bearingPress, frameForge, precisionAssembler, coreAssembler,
        componentDepot;

    public static void load(){
        // Copper -> copper gears. Early game: basic machinery, pumps, sorters, tier I modules.
        gearPress = new GenericCrafter("gear-press"){{
            requirements(Category.crafting, with(Items.copper, 40, Items.lead, 25));
            size = 2;
            health = 220;
            itemCapacity = 10;
            craftTime = 30f;
            consumeItem(Items.copper, 2);
            consumePower(0.25f);
            outputItem = new ItemStack(copperGear, 1);
            craftEffect = Fx.pulverizeSmall;
        }};

        // Titanium + graphite die lubricant -> titanium rods. Structural frames, pressure vessels.
        metalExtruder = new GenericCrafter("metal-extruder"){{
            requirements(Category.crafting, with(Items.copper, 60, Items.lead, 40, Items.graphite, 20, copperGear, 10));
            size = 2;
            health = 260;
            itemCapacity = 10;
            craftTime = 60f;
            consumeItems(with(Items.titanium, 2, Items.graphite, 1));
            consumePower(0.6f);
            outputItem = new ItemStack(titaniumRod, 2);
            craftEffect = Fx.pulverizeSmall;
        }};

        // Titanium -> titanium gears. Industrial machinery, reactors, tier II modules.
        heavyGearPress = new GenericCrafter("heavy-gear-press"){{
            requirements(Category.crafting, with(Items.titanium, 40, Items.lead, 40, Items.graphite, 20, copperGear, 15));
            size = 2;
            health = 300;
            itemCapacity = 10;
            craftTime = 60f;
            consumeItem(Items.titanium, 3);
            consumePower(0.8f);
            outputItem = new ItemStack(titaniumGear, 2);
            craftEffect = Fx.pulverizeSmall;
        }};

        // Titanium rods + metaglass + graphite seals -> pressure casings for vessels and reactors.
        casingForge = new GenericCrafter("casing-forge"){{
            requirements(Category.crafting, with(Items.titanium, 50, Items.lead, 40, Items.metaglass, 30, titaniumGear, 10, titaniumRod, 10));
            size = 2;
            health = 320;
            itemCapacity = 10;
            craftTime = 90f;
            consumeItems(with(titaniumRod, 2, Items.metaglass, 2, Items.graphite, 1));
            consumePower(1f);
            outputItem = new ItemStack(pressureCasing, 1);
            craftEffect = Fx.pulverizeMedium;
        }};

        // Titanium rod races + copper cages + graphite lubricant -> industrial bearings.
        bearingPress = new GenericCrafter("bearing-press"){{
            requirements(Category.crafting, with(Items.titanium, 50, Items.silicon, 30, titaniumGear, 10, copperGear, 10));
            size = 2;
            health = 300;
            itemCapacity = 10;
            craftTime = 60f;
            consumeItems(with(titaniumRod, 1, Items.copper, 2, Items.graphite, 1));
            consumePower(1f);
            outputItem = new ItemStack(industrialBearing, 2);
            craftEffect = Fx.pulverizeSmall;
        }};

        // Superalloy + titanium rods, hot-formed -> superalloy machine frames for heavy and hot machinery.
        frameForge = new HeatCrafter("frame-forge"){{
            requirements(Category.crafting, with(Items.titanium, 60, Items.silicon, 40, Items.graphite, 40, industrialBearing, 10, pressureCasing, 10));
            size = 3;
            health = 650;
            itemCapacity = 10;
            craftTime = 120f;
            heatRequirement = 8f;
            maxEfficiency = 2f;
            consumeItems(with(superalloy, 2, titaniumRod, 2));
            consumePower(1f);
            outputItem = new ItemStack(superalloyFrame, 1);
            craftEffect = Fx.smeltsmoke;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("ffb46b"); }}, new DrawHeatInput());
        }};

        // Titanium gears + silicon + high-purity silicon -> precision components for electronics and smart blocks.
        precisionAssembler = new GenericCrafter("precision-assembler"){{
            requirements(Category.crafting, with(Items.titanium, 60, Items.silicon, 60, Items.lead, 40, industrialBearing, 10, titaniumGear, 10));
            size = 2;
            health = 320;
            itemCapacity = 10;
            craftTime = 90f;
            consumeItems(with(titaniumGear, 1, Items.silicon, 2, highPuritySilicon, 1));
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
