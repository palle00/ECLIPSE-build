package eclipse.content;

import eclipse.robotics.*;
import eclipse.robotics.RobotType.*;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.units.*;
import mindustry.world.blocks.units.UnitFactory.*;

import static eclipse.content.EclipseItems.*;
import static mindustry.type.ItemStack.*;

/**
 * Specialist robotics (ECLIPSE_Strategic_Defense_Robotics_Systems.md, Part III): utility robots built in the vanilla
 * Air Factory (unit caps and research apply) and run from a Robot Dispatch Hub.
 */
public class RoboticsContent{
    public static RobotType logisticsDrone, repairDrone, shieldDrone;
    public static Block robotHub;

    public static void load(){
        logisticsDrone = new RobotType("logistics-drone"){{
            role = Role.logistics;
            health = 180f;
            speed = 1.8f;
            itemCapacity = 20;
        }};

        repairDrone = new RobotType("repair-drone"){{
            role = Role.repair;
            health = 220f;
            speed = 2f;
            itemCapacity = 0;
        }};

        //slow and fragile on purpose: an escort trade-off
        shieldDrone = new RobotType("shield-drone"){{
            role = Role.shield;
            health = 260f;
            speed = 1.3f;
            itemCapacity = 0;
        }};

        robotHub = new RobotHub("robot-dispatch-hub"){{
            requirements(Category.units, with(Items.titanium, 100, Items.silicon, 80, microchip, 20, controlUnit, 5, circuitBoard, 10, industrialBearing, 5));
            size = 3;
            health = 700;
            repairStock = titaniumRod;
            consumePower(2f);
        }};

        //built in the vanilla Air Factory, next to the flare and mono
        UnitFactory air = (UnitFactory)Blocks.airFactory;
        air.plans.add(
            new UnitPlan(logisticsDrone, 60f * 40f, with(aluminium, 20, microchip, 5, leadAcidCell, 5, alnicoMagnet, 4)),
            new UnitPlan(repairDrone, 60f * 50f, with(aluminium, 25, microchip, 8, controlUnit, 2, titaniumRod, 10)),
            new UnitPlan(shieldDrone, 60f * 60f, with(carbonComposite, 15, microchip, 10, alnicoMagnet, 8, precisionComponent, 3))
        );
    }
}
