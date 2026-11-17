package eclipse.content.blocks;

import arc.graphics.*;
import eclipse.defense.*;
import eclipse.world.blocks.*;
import eclipse.world.blocks.WorldEngine.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.draw.*;

import static eclipse.content.EclipseItems.*;
import static eclipse.content.EclipseLiquids.*;
import static mindustry.type.ItemStack.*;

/** Endgame: exotic chemistry (superconductors), the Temporal Interference Array and the World Engine. */
public class EndgameBlocks{
    public static Block synthesisComplex, temporalArray, worldEngine, latticeDrill, fusionReactor;

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

        // Tokamak: deuterium-tritium fusion. The lithium blanket breeds the tritium; superconducting magnets, kept cold with
        // liquid nitrogen, hold the plasma. Like the vanilla impact reactor it needs power to ignite and warms up slowly.
        fusionReactor = new ImpactReactor("fusion-reactor"){{
            requirements(Category.power, with(superconductor, 120, superconductingMotor, 20, experimentalCore, 15, tungstenCarbide, 300, Items.surgeAlloy, 300, Items.thorium, 400));
            size = 5;
            health = 4000;
            itemCapacity = 20;
            liquidCapacity = 60f;
            powerProduction = 450f; //27,000/s at full warmup, 24,000/s after its own 3,000/s
            itemDuration = 180f;
            consumePower(50f);
            consumeItem(lithium, 1);
            consumeLiquids(LiquidStack.with(deuterium, 2f / 60f, liquidNitrogen, 4f / 60f));
            explosionRadius = 26;
            explosionDamage = 2500 * 5;
            ambientSound = Sounds.loopPulse;
            ambientSoundVolume = 0.08f;
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawPlasma(){{ plasma1 = Color.valueOf("ff9ab6"); plasma2 = Color.valueOf("8fd0ff"); }}, new DrawDefault());
        }};

        // The capstone: six stages, each fed by a different production branch, then a planetary power source with a huge
        // energy store, an uplink that adds 15% to the team's other generation, and an overdrive field.
        worldEngine = new WorldEngine("world-engine"){{
            requirements(Category.power, with(Items.titanium, 4000, Items.silicon, 3000, superalloyFrame, 200, superconductor, 100, Items.thorium, 2000, Items.surgeAlloy, 500));
            size = 6;
            health = 24000;
            powerProduction = 5000f; //300,000 power per second once online
            stages = new Stage[]{
                new Stage("foundation", with(superalloyFrame, 600, pressureCasing, 800, carbonComposite, 800, Items.titanium, 8000)),
                new Stage("energy", with(aetherite, 600, superconductor, 400, lithium, 1200), new LiquidStack(liquidNitrogen, 4000f)),
                new Stage("chemistry", with(polyethylene, 2000, epoxyResin, 1000, syntheticRubber, 1000), new LiquidStack(sulfuricAcid, 4000f)),
                new Stage("control", with(controlUnit, 300, precisionComponent, 600, microchip, 1600, circuitBoard, 1200)),
                //the plasma core: the fusion industry's fuel and magnets
                new Stage("confinement", with(superconductingMotor, 120, tungstenCarbide, 1500, superconductor, 400), new LiquidStack(deuterium, 6000f)),
                new Stage("activation", with(experimentalCore, 100, surgeSuperalloy, 800, Items.phaseFabric, 1200, superconductor, 400))
            };
        }};

        //Lattice Drill: grows a mining lattice over the whole ore patch it stands on
        latticeDrill = new LatticeDrill("lattice-drill"){{
            requirements(Category.production, with(Items.thorium, 200, Items.phaseFabric, 40, tungstenCarbide, 150, surgeSuperalloy, 120, experimentalCore, 6, superconductingMotor, 6));
            size = 3;
            health = 2200;
        }};
    }
}
