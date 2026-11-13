package eclipse.content.blocks;

import eclipse.world.blocks.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.liquid.*;

import static eclipse.content.EclipseItems.*;
import static mindustry.type.ItemStack.*;

/** Logistics. Polymer liquid blocks are corrosion-resistant (see eclipse.systems.Corrosion). */
public class TransportBlocks{
    public static Block polymerConduit, polymerLiquidRouter, polymerBridgeConduit, polymerLiquidTank,
        aluminiumConveyor, chromiumConveyor, superalloyConveyor, carbideConveyor, surgeConveyor,
        aluminiumConduit, chromiumConduit, superalloyConduit, carbideConduit, surgeConduit;
    public static void load(){
        polymerConduit = new Conduit("polymer-conduit"){{
            requirements(Category.liquid, with(polyethylene, 1, Items.metaglass, 1));
            health = 90;
            liquidCapacity = 16f;
            liquidPressure = 1.025f;
        }};

        polymerLiquidRouter = new LiquidRouter("polymer-liquid-router"){{
            requirements(Category.liquid, with(polyethylene, 4, Items.metaglass, 2));
            health = 90;
            liquidCapacity = 30f;
            solid = false;
            underBullets = true;
        }};

        polymerBridgeConduit = new LiquidBridge("polymer-bridge-conduit"){{
            requirements(Category.liquid, with(polyethylene, 6, Items.metaglass, 8));
            health = 90;
            range = 4;
            liquidCapacity = 16f;
            fadeIn = moveArrows = false;
            arrowSpacing = 6f;
            ((Conduit)polymerConduit).bridgeReplacement = this;
        }};

        polymerLiquidTank = new LiquidRouter("polymer-liquid-tank"){{
            requirements(Category.liquid, with(polyethylene, 40, Items.titanium, 25, Items.metaglass, 40));
            size = 3;
            health = 700;
            liquidCapacity = 1800f;
            solid = true;
        }};

        // ---- high-speed belts: 20 / 30 / 40 / 50 / 60 items per second (measured by the headless test) ----
        aluminiumConveyor = conveyor("aluminium-conveyor", 20f, 70, with(aluminium, 1, Items.titanium, 1, copperGear, 1));
        chromiumConveyor = conveyor("chromium-conveyor", 30f, 80, with(chromium, 1, aluminium, 1, Items.titanium, 1, titaniumGear, 1));
        superalloyConveyor = conveyor("superalloy-conveyor", 40f, 95, with(superalloy, 1, aluminium, 1, titaniumGear, 1));
        carbideConveyor = conveyor("carbide-conveyor", 50f, 110, with(tungstenCarbide, 1, superalloy, 1, polyethylene, 1, industrialBearing, 1));
        surgeConveyor = conveyor("surge-conveyor", 60f, 130, with(surgeSuperalloy, 1, tungstenCarbide, 1, industrialBearing, 1));

        // ---- high-capacity pipes: 3600 / 4200 / 4800 / 5400 / 6000 liquid per second (vanilla plated conduit: 3000) ----
        aluminiumConduit = conduit("aluminium-conduit", 3600f, 90, with(aluminium, 1, Items.metaglass, 1));
        chromiumConduit = conduit("chromium-conduit", 4200f, 110, with(chromium, 1, reinforcedGlass, 1));
        superalloyConduit = conduit("superalloy-conduit", 4800f, 140, with(superalloy, 1, reinforcedGlass, 1));
        carbideConduit = conduit("carbide-conduit", 5400f, 170, with(tungstenCarbide, 1, reinforcedGlass, 1));
        surgeConduit = conduit("surge-conduit", 6000f, 200, with(surgeSuperalloy, 1, reinforcedGlass, 1));
    }

    /** FastConveyor makes throughput exactly proportional to speed. The headless test verifies every tier. */
    //note: parameters must not share names with Block fields (requirements, health), or the initializers assign fields to themselves
    static Block conveyor(String name, float itemsPerSecond, int hp, ItemStack[] cost){
        return new FastConveyor(name){{
            requirements(Category.distribution, cost);
            health = hp;
            speed = speedFor(itemsPerSecond);
            displayedSpeed = itemsPerSecond;
        }};
    }

    /** Measured: a conduit moves up to its liquid capacity every tick, so flow/s = capacity * 60. Verified by the headless test. */
    static Block conduit(String name, float perSecond, int hp, ItemStack[] cost){
        return new Conduit(name){{
            requirements(Category.liquid, cost);
            health = hp;
            liquidCapacity = perSecond / 60f;
            liquidPressure = 1.025f;
        }};
    }

    /**
     * Vanilla routing blocks that would choke the fast ECLIPSE belts (up to 60 items/s) get more headroom, with the same
     * cost, size and range: the junction (was ~25/s), the bridge conveyor (~22/s) and the phase conveyor (~30/s).
     * Routers, sorters and gates already pass a full belt. Tested in LogisticsTests.vanillaOnFastBelts.
     */
    public static void patchVanilla(){
        Junction junction = (Junction)Blocks.junction;
        junction.speed = 6f;
        junction.capacity = 12;

        BufferedItemBridge bridge = (BufferedItemBridge)Blocks.itemBridge;
        bridge.speed = 8f;
        bridge.bufferCapacity = 30;
        bridge.transportTime = 0.4f;
        //vanilla hands items on with a fixed 4-tick timer (30/s at most) that no setting reaches; its transport step reads
        //that timer through the building's time scale, so run just this step 4x faster: one item per tick, 60/s
        bridge.buildType = () -> bridge.new BufferedItemBridgeBuild(){
            @Override
            public void updateTransport(Building other){
                float scale = timeScale;
                timeScale = scale * 4f;
                super.updateTransport(other);
                timeScale = scale;
            }
        };

        ((ItemBridge)Blocks.phaseConveyor).transportTime = 0.8f;
    }
}
