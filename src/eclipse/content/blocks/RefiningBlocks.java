package eclipse.content.blocks;

import arc.graphics.*;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.draw.*;

import static eclipse.content.EclipseItems.*;
import static eclipse.content.EclipseLiquids.*;
import static mindustry.type.ItemStack.*;

/**
 * Upgraded production for the vanilla materials ECLIPSE consumes most (silicon, graphite, metaglass, titanium,
 * copper and lead). Each uses a different industrial process from its vanilla counterpart, fed by ECLIPSE
 * chemistry and heat. On Serpulo, Erekir's silicon arc furnace and electrolyzer can't be built, so nothing is duplicated.
 */
public class RefiningBlocks{
    public static Block siliconReductionFurnace, methanePyrolysisReactor, chlorideTitaniumPlant;

    public static void load(){
        // Sand reduced with methane as the carbon source, in a heated retort -> silicon. No coal; up to 8 silicon/s at full heat.
        siliconReductionFurnace = new HeatCrafter("silicon-reduction-furnace"){{
            requirements(Category.crafting, with(Items.titanium, 80, Items.metaglass, 40, Items.silicon, 60, pressureCasing, 10, titaniumGear, 10, copperWire, 20));
            size = 3;
            health = 650;
            itemCapacity = 20;
            liquidCapacity = 30f;
            craftTime = 60f;
            heatRequirement = 8f;
            maxEfficiency = 2f;
            consumeItem(Items.sand, 4);
            consumeLiquid(methane, 3f / 60f);
            consumePower(1f);
            outputItem = new ItemStack(hotSilicon, 4); //cooled in the vacuum freezer
            craftEffect = Fx.smeltsmoke;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("ffb46b"); }}, new DrawHeatInput());
        }};

        // CH4 -> C + 2 H2 (methane pyrolysis): graphite without coal, with hydrogen as a by-product.
        methanePyrolysisReactor = new HeatCrafter("methane-pyrolysis-reactor"){{
            requirements(Category.crafting, with(Items.titanium, 60, Items.graphite, 40, Items.metaglass, 40, pressureCasing, 10, titaniumRod, 10));
            size = 3;
            health = 650;
            itemCapacity = 10;
            liquidCapacity = 30f;
            craftTime = 60f;
            heatRequirement = 10f;
            maxEfficiency = 2f;
            consumeLiquid(methane, 6f / 60f);
            consumePower(0.5f);
            outputItem = new ItemStack(Items.graphite, 2);
            outputLiquid = new LiquidStack(Liquids.hydrogen, 8f / 60f);
            craftEffect = Fx.smeltsmoke;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("ff8a3d"); }}, new DrawHeatInput());
        }};

        // Mineral sands chlorinated with coal as reductant, then reduced (chloride process) -> titanium from sand instead of ore.
        chlorideTitaniumPlant = new HeatCrafter("chloride-titanium-plant"){{
            requirements(Category.crafting, with(Items.titanium, 80, Items.silicon, 60, Items.metaglass, 50, pressureCasing, 15, industrialBearing, 5, copperWire, 20));
            size = 3;
            health = 700;
            itemCapacity = 20;
            liquidCapacity = 30f;
            craftTime = 60f;
            heatRequirement = 10f;
            maxEfficiency = 2f;
            consumeItems(with(Items.sand, 4, Items.coal, 2));
            consumeLiquid(chlorine, 4f / 60f);
            consumePower(2f);
            outputItem = new ItemStack(hotTitanium, 2); //cooled in the vacuum freezer
            craftEffect = Fx.smeltsmoke;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("8da1e3"); }}, new DrawHeatInput());
        }};

    }
}
