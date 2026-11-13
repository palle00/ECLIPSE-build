package eclipse.content.blocks;

import eclipse.logistics.*;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.*;

import static eclipse.content.EclipseItems.*;
import static mindustry.type.ItemStack.*;

/**
 * Advanced logistics (ECLIPSE_Advanced_Logistics_Design.md): control and routing, not raw speed.
 * Handling rates: 1x1 buildings 30 items/s, 2x2 buildings 60 items/s.
 */
public class LogisticsBlocks{
    public static Block threeWaySorter, priorityMerger, controlledSplitter, distributionHub, smallBuffer, industrialBuffer, lineBalancer,
        advancedItemFilter, batchGate, smartUnloader, liquidOverflowValve, liquidBalancer, reserveValve, flowMeter;

    public static void load(){
        threeWaySorter = new FilterRouter("three-way-sorter"){{
            requirements(Category.distribution, with(Items.lead, 4, Items.copper, 4, Items.graphite, 2, Items.silicon, 2, copperGear, 2));
            health = 80;
            slots = 3;
            itemsPerSecond = 30f;
        }};

        priorityMerger = new PriorityMerger("priority-merger"){{
            requirements(Category.distribution, with(Items.lead, 4, Items.copper, 4, Items.graphite, 2, copperGear, 2));
            health = 80;
            itemsPerSecond = 30f;
        }};

        controlledSplitter = new ControlledSplitter("controlled-splitter"){{
            requirements(Category.distribution, with(Items.lead, 4, Items.copper, 4, Items.graphite, 2, Items.silicon, 2, copperGear, 2));
            health = 80;
            maxBranches = 3;
            itemsPerSecond = 30f;
        }};

        distributionHub = new FilterRouter("distribution-hub"){{
            requirements(Category.distribution, with(Items.titanium, 30, Items.silicon, 30, Items.lead, 30, microchip, 4, titaniumGear, 4, precisionComponent, 2, circuitBoard, 2));
            size = 2;
            health = 400;
            slots = 4;
            itemsPerSecond = 60f;
            consumePower(0.3f);
        }};

        smallBuffer = new LogisticsBuffer("small-logistics-buffer"){{
            requirements(Category.distribution, with(Items.titanium, 20, Items.lead, 30, Items.graphite, 10, copperGear, 4));
            size = 2;
            health = 300;
            capacity = 60;
            itemsPerSecond = 60f;
        }};

        industrialBuffer = new LogisticsBuffer("industrial-logistics-buffer"){{
            requirements(Category.distribution, with(Items.titanium, 40, Items.silicon, 30, aluminium, 20, microchip, 4, industrialBearing, 4, precisionComponent, 2));
            size = 2;
            health = 500;
            capacity = 200;
            itemsPerSecond = 60f;
            outputFilter = true;
            consumePower(0.2f);
        }};

        lineBalancer = new ControlledSplitter("line-balancer"){{
            requirements(Category.distribution, with(superalloy, 10, Items.titanium, 30, Items.silicon, 30, microchip, 6, precisionComponent, 2, industrialBearing, 2, controlUnit, 1));
            size = 2;
            health = 400;
            maxBranches = 4;
            perBuilding = true;
            itemsPerSecond = 60f;
            consumePower(0.3f);
        }};

        advancedItemFilter = new AdvancedItemFilter("advanced-item-filter"){{
            requirements(Category.distribution, with(Items.titanium, 6, Items.silicon, 6, microchip, 1, precisionComponent, 1, circuitBoard, 1));
            health = 80;
            itemsPerSecond = 30f;
        }};

        batchGate = new BatchGate("batch-gate"){{
            requirements(Category.distribution, with(Items.titanium, 6, Items.lead, 6, Items.silicon, 4, copperGear, 2));
            health = 80;
        }};

        //cheap on purpose: a measuring tool everyone should be able to place from the start
        flowMeter = new FlowMeter("flow-meter"){{
            requirements(Category.distribution, with(Items.copper, 4, Items.lead, 2));
            health = 45;
        }};

        smartUnloader = new SmartUnloader("smart-unloader"){{
            requirements(Category.distribution, with(Items.titanium, 30, Items.silicon, 30, microchip, 3, precisionComponent, 2, titaniumGear, 4, circuitBoard, 2));
            health = 120;
        }};

        liquidOverflowValve = new LiquidValve("liquid-overflow-valve"){{
            requirements(Category.liquid, with(Items.metaglass, 8, Items.lead, 6, Items.graphite, 4, copperGear, 2));
            health = 80;
            mode = Mode.overflow;
        }};

        liquidBalancer = new LiquidValve("liquid-balancer"){{
            requirements(Category.liquid, with(Items.metaglass, 10, Items.titanium, 6, Items.silicon, 4, titaniumGear, 2));
            health = 80;
            mode = Mode.balancer;
        }};

        reserveValve = new LiquidValve("reserve-valve"){{
            requirements(Category.liquid, with(Items.metaglass, 10, Items.titanium, 6, Items.silicon, 6, titaniumGear, 2));
            health = 80;
            mode = Mode.reserve;
        }};
    }
}
