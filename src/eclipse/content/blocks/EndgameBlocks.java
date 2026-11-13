package eclipse.content.blocks;

import arc.graphics.*;
import eclipse.defense.*;
import eclipse.world.blocks.*;
import eclipse.world.blocks.WorldEngine.*;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.draw.*;

import static eclipse.content.EclipseItems.*;
import static eclipse.content.EclipseLiquids.*;
import static mindustry.type.ItemStack.*;

/** Endgame: exotic chemistry (superconductors), the Temporal Interference Array and the World Engine. */
public class EndgameBlocks{
    public static Block synthesisComplex, temporalArray, worldEngine, latticeDrill;

    public static void load(){
        // Copper wire + aetherite, dissolved in nitric acid and sintered under pressure over platinum, cooled with
        // liquid nitrogen -> superconductor. The exotic end of the chemical industry.
        synthesisComplex = new ChemicalReactor("chemical-synthesis-complex"){{
            requirements(Category.crafting, with(Items.titanium, 200, Items.silicon, 150, superalloyFrame, 20, pressureCasing, 30, controlUnit, 10, platinum, 40));
            size = 4;
            health = 1600;
            itemCapacity = 20;
            liquidCapacity = 60f;
            craftTime = 120f;
            pressureRequired = 0.8f;
            catalyst = platinum;
            craftsPerCatalyst = 20;
            consumeItems(with(copperWire, 4, aetherite, 1));
            consumeLiquids(LiquidStack.with(nitricAcid, 4f / 60f, liquidNitrogen, 6f / 60f));
            consumePower(6f);
            outputItem = new ItemStack(superconductor, 2);
            craftEffect = Fx.smeltsmoke;
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(liquidNitrogen, 5f), new DrawDefault());
        }};

        // Stage 9: one long wave delay; shares the Disruption Array's lockout, so the two never add up.
        temporalArray = new WaveDisruptionArray("temporal-interference-array"){{
            requirements(Category.effect, with(Items.phaseFabric, 100, platinum, 60, surgeSuperalloy, 60, superconductor, 40, experimentalCore, 10, carbonComposite, 60));
            size = 4;
            health = 1800;
            delay = 50f * 60f;
            lockWaves = 4;
            chargeTime = 90f * 60f;
            fieldColor = Color.valueOf("7fe0ff");
            consumePower(12f);
        }};

        // The capstone: five stages, each fed by a different production branch, then a planetary power source.
        worldEngine = new WorldEngine("world-engine"){{
            requirements(Category.power, with(Items.titanium, 2000, Items.silicon, 1500, superalloyFrame, 100, superconductor, 50, Items.thorium, 1000));
            size = 6;
            health = 12000;
            powerProduction = 500f; //30,000 power per second once online
            stages = new Stage[]{
                new Stage("foundation", with(superalloyFrame, 300, pressureCasing, 400, carbonComposite, 400, Items.titanium, 4000)),
                new Stage("energy", with(aetherite, 300, superconductor, 200, lithium, 600), new LiquidStack(liquidNitrogen, 2000f)),
                new Stage("chemistry", with(polyethylene, 1000, epoxyResin, 500, syntheticRubber, 500), new LiquidStack(sulfuricAcid, 2000f)),
                new Stage("control", with(controlUnit, 150, precisionComponent, 300, microchip, 800, circuitBoard, 600)),
                new Stage("activation", with(experimentalCore, 50, surgeSuperalloy, 400, Items.phaseFabric, 600, superconductor, 200))
            };
        }};

        //Lattice Drill: grows a mining lattice over the whole ore patch it stands on
        latticeDrill = new LatticeDrill("lattice-drill"){{
            requirements(Category.production, with(Items.thorium, 200, Items.phaseFabric, 40, tungstenCarbide, 150, surgeSuperalloy, 120,
                superalloyFrame, 20, precisionComponent, 20, experimentalCore, 6));
            size = 3;
            health = 2200;
        }};
    }
}
