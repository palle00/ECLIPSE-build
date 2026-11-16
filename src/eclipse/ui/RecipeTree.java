package eclipse.ui;

import arc.struct.*;
import arc.util.*;
import eclipse.world.blocks.*;
import mindustry.ctype.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.production.*;
import mindustry.world.consumers.*;

import static mindustry.Vars.*;

/**
 * The full production chain of an item down to raw materials, for a target rate: which ECLIPSE machine makes each
 * step, how many of them it takes, and what goes in. Raw materials are vanilla items, ore drops and anything nothing
 * makes; liquids are listed per second but not followed further. Machines run at base speed (no overclock, modules
 * or heat bonus). Shown in the ECLIPSE Wiki on every item page.
 */
public class RecipeTree{
    /** One way to make an item: a machine, what it takes per craft, and how long a craft takes. */
    public static class Source{
        public Block block;
        public ItemStack[] inputs = {};
        public LiquidStack[] liquids = {};
        public int amount;
        public float seconds;
    }

    /** One node: an item at a rate, the machines making it (none for raw materials) and its inputs. */
    public static class Step{
        public Item item;
        public float perSecond, machines;
        public @Nullable Source source;
        public final Seq<Step> inputs = new Seq<>();
        /** Liquid inputs, per second for all machines of this step. */
        public final Seq<LiquidStack> liquids = new Seq<>();
    }

    /** How to make {@code perSecond} of an item per second. */
    public static Step plan(Item item, float perSecond){
        return plan(item, perSecond, new Seq<>());
    }

    static Step plan(Item item, float rate, Seq<Item> path){
        Step s = new Step();
        s.item = item;
        s.perSecond = rate;
        if(raw(item) || path.contains(item)) return s;
        Source src = sourceOf(item);
        if(src == null) return s;
        s.source = src;
        float crafts = rate / src.amount;
        s.machines = crafts * src.seconds;
        path.add(item);
        for(ItemStack in : src.inputs) s.inputs.add(plan(in.item, crafts * in.amount, path));
        for(LiquidStack l : src.liquids) s.liquids.add(new LiquidStack(l.liquid, l.amount * 60f * s.machines));
        path.pop();
        return s;
    }

    /** Raw materials: vanilla items (vanilla machines and ores make them) and ECLIPSE ore drops. */
    public static boolean raw(Item item){
        if(!item.name.startsWith("eclipse-")) return true;
        for(Block b : content.blocks()) if(b instanceof OreBlock o && o.itemDrop == item) return true;
        return false;
    }

    /** The ECLIPSE machine that makes an item: universal machines first, then fixed-recipe crafters. */
    public static @Nullable Source sourceOf(Item item){
        Source fixed = null;
        for(Block b : content.blocks()){
            if(!b.name.startsWith("eclipse-")) continue;
            if(b instanceof RecipeCrafter rc){
                for(RecipeCrafter.Recipe r : rc.recipes){
                    ItemStack out = Structs.find(r.outputs, o -> o.item == item);
                    if(out == null || r.uses(item)) continue;
                    Source s = new Source();
                    s.block = b;
                    s.inputs = r.inputs;
                    s.liquids = r.liquidInputs;
                    s.amount = out.amount;
                    s.seconds = r.time / 60f;
                    return s;
                }
            }else if(fixed == null && b instanceof GenericCrafter g && g.outputItems != null){
                ItemStack out = Structs.find(g.outputItems, o -> o.item == item);
                if(out == null) continue;
                Source s = new Source();
                s.block = b;
                ConsumeItems items = b.findConsumer(c -> c instanceof ConsumeItems);
                if(items != null) s.inputs = items.items;
                if(items != null && Structs.contains(items.items, i -> i.item == item)) continue;
                Seq<LiquidStack> liquids = new Seq<>();
                for(Consume c : b.consumers){
                    if(c.optional) continue;
                    if(c instanceof ConsumeLiquid cl) liquids.add(new LiquidStack(cl.liquid, cl.amount));
                    if(c instanceof ConsumeLiquids cls) liquids.addAll(cls.liquids);
                }
                s.liquids = liquids.toArray(LiquidStack.class);
                s.amount = out.amount;
                s.seconds = g.craftTime / 60f;
                fixed = s;
            }
        }
        return fixed;
    }

    /** Total raw materials per second: leaf items and every liquid in the chain. */
    public static ObjectFloatMap<UnlockableContent> totals(Step root){
        ObjectFloatMap<UnlockableContent> out = new ObjectFloatMap<>();
        add(root, out);
        return out;
    }

    static void add(Step s, ObjectFloatMap<UnlockableContent> out){
        if(s.source == null) out.increment(s.item, 0f, s.perSecond);
        for(LiquidStack l : s.liquids) out.increment(l.liquid, 0f, l.amount);
        for(Step in : s.inputs) add(in, out);
    }

    /** Total machines per block in the chain. */
    public static ObjectFloatMap<Block> machines(Step root){
        ObjectFloatMap<Block> out = new ObjectFloatMap<>();
        countMachines(root, out);
        return out;
    }

    static void countMachines(Step s, ObjectFloatMap<Block> out){
        if(s.source != null) out.increment(s.source.block, 0f, s.machines);
        for(Step in : s.inputs) countMachines(in, out);
    }
}
