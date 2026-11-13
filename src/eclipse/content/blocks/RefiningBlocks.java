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
    public static Block siliconReductionFurnace, methanePyrolysisReactor, sodaGlassFurnace, chlorideTitaniumPlant, electrowinningCell;

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
            outputItem = new ItemStack(Items.silicon, 4);
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

        // Sand fluxed with sodium hydroxide (soda glass) -> metaglass without lead, leaving lead free for building costs.
        sodaGlassFurnace = new HeatCrafter("soda-glass-furnace"){{
            requirements(Category.crafting, with(Items.lead, 60, Items.graphite, 40, Items.titanium, 30, titaniumRod, 10, pressureCasing, 5));
            size = 2;
            health = 340;
            itemCapacity = 15;
            liquidCapacity = 20f;
            craftTime = 60f;
            heatRequirement = 6f;
            maxEfficiency = 2f;
            consumeItem(Items.sand, 3);
            consumeLiquid(sodiumHydroxide, 2f / 60f);
            consumePower(0.4f);
            outputItem = new ItemStack(Items.metaglass, 3);
            craftEffect = Fx.smeltsmoke;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("9fd6ff"); }}, new DrawHeatInput());
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
            outputItem = new ItemStack(Items.titanium, 2);
            craftEffect = Fx.smeltsmoke;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{ color = Color.valueOf("8da1e3"); }}, new DrawHeatInput());
        }};

        // Scrap dissolved in sulfuric acid and plated out electrically -> copper and lead. The spent electrolyte leaves as acidic waste.
        electrowinningCell = new GenericCrafter("electrowinning-cell"){{
            requirements(Category.crafting, with(Items.lead, 80, Items.metaglass, 40, Items.silicon, 40, copperWire, 30, pressureCasing, 10));
            size = 3;
            health = 600;
            itemCapacity = 20;
            liquidCapacity = 30f;
            craftTime = 60f;
            consumeItem(Items.scrap, 2);
            consumeLiquid(sulfuricAcid, 4f / 60f);
            consumePower(2.5f);
            outputItems = with(Items.copper, 2, Items.lead, 1);
            outputLiquid = new LiquidStack(acidicWaste, 1f / 60f);
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(sulfuricAcid, 4f),
                new DrawBubbles(Color.valueOf("e8b84a")){{ amount = 10; radius = 4f; spread = 9f; }},
                new DrawDefault());
        }};
    }
}
