package eclipse.content.blocks;

import eclipse.augment.*;
import eclipse.world.blocks.*;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.draw.*;

import static eclipse.content.EclipseItems.*;
import static mindustry.type.ItemStack.*;

/** Aetherite (Tier V modules, power storage) and floor-based environment blocks. */
public class ExplorationBlocks{
    public static Block aetheriteForge, aetheriteCapacitor, geothermalHeatTap;

    public static void load(){
        // ---- aetherite ----
        aetheriteForge = new ModuleAssembler("aetherite-module-forge"){{
            requirements(Category.crafting, with(aetherite, 60, Items.phaseFabric, 40, experimentalCore, 10, controlUnit, 10, surgeSuperalloy, 30));
            size = 3;
            health = 1100;
            minTier = maxTier = 5;
            itemCapacity = 80;
            consumePower(6f);
        }};

        aetheriteCapacitor = new Battery("aetherite-capacitor"){{
            requirements(Category.power, with(aetherite, 30, lithium, 20, carbonFiber, 10, circuitBoard, 10, precisionComponent, 5));
            size = 2;
            health = 500;
            consumePowerBuffered(120000f);
            baseExplosiveness = 6f;
            drawer = new DrawMulti(new DrawDefault(), new DrawPower(), new DrawRegion("-top"));
        }};

        // ---- environment: free heat on hot floors ----
        geothermalHeatTap = new GeothermalHeater("geothermal-heat-tap"){{
            requirements(Category.crafting, with(Items.titanium, 40, Items.graphite, 40, Items.metaglass, 20, titaniumRod, 10, pressureCasing, 5));
            size = 2;
            health = 400;
            heatOutput = 3f; //per unit of floor heat: about 6 on full hot rock, 9 on full magma rock
            drawer = new DrawMulti(new DrawDefault(), new DrawHeatOutput());
        }};
    }
}
