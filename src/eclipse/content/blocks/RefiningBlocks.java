package eclipse.content.blocks;

import arc.graphics.*;
import eclipse.content.*;
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
    public static Block methanePyrolysisReactor;

    public static void load(){
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
            drawer = new DrawMulti(new DrawDefault(),
                new DrawParticles(){{ color = Color.valueOf("ffb060"); x = -0.5f; y = -0.5f; particles = 14; particleRad = 3.6f; particleSize = 0.9f; particleLife = 50f; reverse = true; alpha = 0.8f; }},
                Animations.spin("-fan", 8f, 8f, 8.5f), Animations.spin("-fan", 8f, 10f, 6.5f), Animations.spin("-fan", 8f, 8f, 4.5f),
                new DrawGlowRegion(){{ color = Color.valueOf("ff8a3d"); }}, new DrawHeatInput());
        }};

    }
}
