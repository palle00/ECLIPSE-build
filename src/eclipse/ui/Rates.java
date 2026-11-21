package eclipse.ui;

import arc.struct.*;
import arc.util.*;
import eclipse.world.blocks.*;
import eclipse.world.blocks.RecipeCrafter.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.power.ConsumeGenerator.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.production.Drill.*;
import mindustry.world.consumers.*;

/**
 * Items and liquids per second a machine makes and uses: a running building's actual rate (speed, overclock, power
 * and modules included), or a block's base rate for a recipe. Used by the production panel and the placement ratio hint.
 */
public class Rates{
    /** Adds what a building makes and uses per second right now; nothing while it is stopped. */
    public static void of(Building b, ObjectFloatMap<UnlockableContent> in, ObjectFloatMap<UnlockableContent> out){
        if(b instanceof RecipeBuild r){
            if(r.recipe == null || !r.shouldConsume() || r.efficiency <= 0f) return;
            recipe(r.recipe, r.efficiency * r.timeScale() * RecipeCrafter.overclockSpeed[r.overclock], in, out);
        }else if(b instanceof GenericCrafter.GenericCrafterBuild g){
            if(!g.shouldConsume() || g.efficiency <= 0f) return;
            crafter((GenericCrafter)b.block, g.efficiency * g.timeScale(), in, out);
        }else if(b instanceof DrillBuild d){
            if(d.dominantItem != null && d.efficiency > 0f) out.increment(d.dominantItem, 0f, d.lastDrillSpeed * 60f);
        }else if(b instanceof ConsumeGeneratorBuild g && g.productionEfficiency > 0f && b.block instanceof mindustry.world.blocks.power.ConsumeGenerator cg){
            for(Consume c : b.block.consumers){
                if(c instanceof ConsumeItems ci) for(ItemStack s : ci.items) in.increment(s.item, 0f, s.amount * 60f / cg.itemDuration * g.timeScale());
                else if(c instanceof ConsumeItemFilter && b.items != null && b.items.first() != null) in.increment(b.items.first(), 0f, 60f / cg.itemDuration * g.timeScale());
                else if(c instanceof ConsumeLiquid cl && !cl.booster) in.increment(cl.liquid, 0f, cl.amount * 60f * g.efficiency * g.timeScale());
                else if(c instanceof ConsumeLiquids cls) for(LiquidStack s : cls.liquids) in.increment(s.liquid, 0f, s.amount * 60f * g.efficiency * g.timeScale());
            }
        }
    }

    /** A recipe at speed k (1 = base): items per craft over its time, liquids per tick. */
    public static void recipe(Recipe r, float k, ObjectFloatMap<UnlockableContent> in, ObjectFloatMap<UnlockableContent> out){
        float crafts = k * 60f / r.time;
        for(ItemStack s : r.inputs) in.increment(s.item, 0f, s.amount * crafts);
        for(LiquidStack s : r.liquidInputs) in.increment(s.liquid, 0f, s.amount * 60f * k);
        for(ItemStack s : r.outputs) out.increment(s.item, 0f, s.amount * crafts);
        for(LiquidStack s : r.liquidOutputs) out.increment(s.liquid, 0f, s.amount * 60f * k);
    }

    /** A fixed-recipe crafter at speed k (1 = base). */
    public static void crafter(GenericCrafter g, float k, ObjectFloatMap<UnlockableContent> in, ObjectFloatMap<UnlockableContent> out){
        float crafts = k * 60f / g.craftTime;
        for(Consume c : g.consumers){
            if(c.optional) continue;
            if(c instanceof ConsumeItems ci) for(ItemStack s : ci.items) in.increment(s.item, 0f, s.amount * crafts);
            else if(c instanceof ConsumeLiquid cl) in.increment(cl.liquid, 0f, cl.amount * 60f * k);
            else if(c instanceof ConsumeLiquids cls) for(LiquidStack s : cls.liquids) in.increment(s.liquid, 0f, s.amount * 60f * k);
        }
        if(g.outputItems != null) for(ItemStack s : g.outputItems) out.increment(s.item, 0f, s.amount * crafts);
        if(g.outputLiquids != null) for(LiquidStack s : g.outputLiquids) out.increment(s.liquid, 0f, s.amount * 60f * k);
    }

    /** A building's base rates for what it is set to make (its recipe, or its fixed one); false if it isn't set up. */
    public static boolean base(Building b, ObjectFloatMap<UnlockableContent> in, ObjectFloatMap<UnlockableContent> out){
        if(b instanceof RecipeBuild r){
            if(r.recipe == null) return false;
            recipe(r.recipe, RecipeCrafter.overclockSpeed[r.overclock], in, out);
            return true;
        }
        if(b.block instanceof GenericCrafter g && !(g instanceof RecipeCrafter)){
            crafter(g, 1f, in, out);
            return true;
        }
        return false;
    }

    /** A block's base rates for a recipe that is about to be placed (null: its fixed recipe). */
    public static boolean base(Block b, @Nullable Recipe r, ObjectFloatMap<UnlockableContent> in, ObjectFloatMap<UnlockableContent> out){
        if(b instanceof RecipeCrafter){
            if(r == null) return false;
            recipe(r, 1f, in, out);
            return true;
        }
        if(b instanceof GenericCrafter g){
            crafter(g, 1f, in, out);
            return true;
        }
        return false;
    }
}
