package eclipse.content.blocks;

import eclipse.defense.*;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.*;

import static eclipse.content.EclipseItems.*;
import static mindustry.type.ItemStack.*;

/** Strategic defence and support (ECLIPSE_Strategic_Defense_Robotics_Systems.md, Parts I-II). */
public class DefenseBlocks{
    public static Block waveScanner, waveDisruptionArray, unitShieldBeacon, repairDispatchBay;

    public static void load(){
        // Stage 4-5: wave intelligence.
        waveScanner = new WaveScanner("wave-intelligence-scanner"){{
            requirements(Category.logic, with(Items.silicon, 60, Items.titanium, 40, reinforcedGlass, 20, circuitBoard, 5, copperWire, 20));
            size = 2;
            health = 300;
            consumePower(0.5f);
        }};

        // Stage 6-7: one controlled wave delay per lockout.
        waveDisruptionArray = new WaveDisruptionArray("wave-disruption-array"){{
            requirements(Category.effect, with(microchip, 30, aluminium, 60, leadAcidCell, 20, reinforcedGlass, 40, controlUnit, 5, superalloyFrame, 5));
            size = 3;
            health = 800;
            consumePower(4f);
        }};

        // Stage 7: shared shield pool for friendly units only.
        unitShieldBeacon = new UnitShieldBeacon("unit-shield-beacon"){{
            requirements(Category.effect, with(microchip, 20, aluminium, 40, highPuritySilicon, 30, reinforcedGlass, 30, precisionComponent, 10, alnicoMagnet, 10));
            size = 3;
            health = 700;
            consumePower(3f);
        }};

        // Stage 6-7: targeted repair from titanium-rod stock.
        repairDispatchBay = new RepairDispatchBay("emergency-repair-dispatch-bay"){{
            requirements(Category.effect, with(Items.titanium, 80, Items.silicon, 50, industrialBearing, 10, circuitBoard, 5, titaniumGear, 10));
            size = 3;
            health = 600;
            stock = titaniumRod;
            consumePower(2f);
        }};
    }
}
