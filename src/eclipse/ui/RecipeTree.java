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
 * The full production chain of an item down to raw materials, for a target rate: which machine makes each step, how
 * many of them it takes, and what goes in. Raw materials are what the loaded map lets drills mine and anything no
 * machine here makes; liquids are listed per second but not followed further. Machines run at base speed (no overclock, modules
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
        /** Power per tick while running, and the recipe's power tier (0: a fixed crafter outside the tier system). */
        public float power;
        public int tier;
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

    /** Items that can be mined on the loaded map (ore overlays, ore walls, floors such as sand), and the map they were read from. */
    static final ObjectSet<Item> minable = new ObjectSet<>();
    static @Nullable Object scannedTiles;

    /**
     * Raw materials: what can be mined on the loaded map, and what no machine available here makes. So graphite is raw on
     * Erekir (graphitic walls) but made from coal on Serpulo, and titanium on a map without its ore comes from a process.
     * With no map loaded (or one without any ore), vanilla items and ECLIPSE ore drops count as raw.
     */
    public static boolean raw(Item item){
        scan();
        if(!minable.isEmpty()) return minable.contains(item) || sourceOf(item) == null;
        if(!item.name.startsWith("eclipse-")) return true;
        for(Block b : content.blocks()) if(b instanceof OreBlock o && o.itemDrop == item) return true;
        return false;
    }

    /** Reads which items the loaded map can give drills, once per map. */
    static void scan(){
        if(world == null || world.tiles == null || scannedTiles == world.tiles) return;
        scannedTiles = world.tiles;
        minable.clear();
        for(Tile t : world.tiles){
            if(t.drop() != null) minable.add(t.drop());
            if(t.wallDrop() != null) minable.add(t.wallDrop());
        }
    }

    /** A machine the player can build here: visible, fits this planet and environment, not banned by the map. */
    static boolean available(Block b){
        if(!b.isVisible()) return false;
        if(!state.isGame()) return true;
        return b.environmentBuildable() && (state.rules.planet == null || b.isOnPlanet(state.rules.planet)) && !state.rules.isBanned(b);
    }

    /**
     * The machine that makes an item, among those available here. Vanilla items come from vanilla machines first (the
     * Graphite Press, the Silicon Smelter), ECLIPSE items from ECLIPSE universal machines, then fixed-recipe crafters.
     */
    public static @Nullable Source sourceOf(Item item){
        Source recipe = null, eclipseFixed = null, vanillaFixed = null;
        for(Block b : content.blocks()){
            if(!(b instanceof GenericCrafter) || !available(b)) continue;
            boolean eclipse = b.name.startsWith("eclipse-");
            if(b instanceof RecipeCrafter rc){
                if(recipe != null) continue;
                for(RecipeCrafter.Recipe r : rc.recipes){
                    ItemStack out = Structs.find(r.outputs, o -> o.item == item);
                    if(out == null || r.uses(item)) continue;
                    Source s = new Source();
                    s.block = b;
                    s.inputs = r.inputs;
                    s.liquids = r.liquidInputs;
                    s.amount = out.amount;
                    s.seconds = r.time / 60f;
                    s.power = r.power();
                    s.tier = r.tier;
                    recipe = s;
                    break;
                }
            }else if((eclipse ? eclipseFixed : vanillaFixed) == null && b instanceof GenericCrafter g && g.outputItems != null){
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
                s.power = b.consPower == null ? 0f : b.consPower.usage;
                if(eclipse) eclipseFixed = s;
                else vanillaFixed = s;
            }
        }
        if(!item.name.startsWith("eclipse-") && vanillaFixed != null) return vanillaFixed;
        return recipe != null ? recipe : eclipseFixed != null ? eclipseFixed : vanillaFixed;
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

    /** Power per tick for every machine in the chain. */
    public static float power(Step s){
        float p = s.source == null ? 0f : s.source.power * s.machines;
        for(Step in : s.inputs) p += power(in);
        return p;
    }

    /** The highest power tier any recipe in the chain needs (0: none on the tier system). */
    public static int maxTier(Step s){
        int t = s.source == null ? 0 : s.source.tier;
        for(Step in : s.inputs) t = Math.max(t, maxTier(in));
        return t;
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
