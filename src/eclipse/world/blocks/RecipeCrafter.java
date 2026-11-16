package eclipse.world.blocks;

import arc.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.production.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * A universal machine: one block, many recipes (INDUSTRY_OVERHAUL.md §4). The player picks the recipe (tap the block, or
 * a logic processor's {@code control config} with the output item); schematics copy it.
 *
 * Power tiers: each recipe has a tier I-V that sets its power draw ({@link #tierPower}). The machine only runs while its
 * grid covers that draw in full ({@link #minSatisfaction}); below it the machine stops instead of slowing down.
 *
 * Overclocking: level 0-III, set next to the recipe. Each level doubles speed for a steeply rising power cost
 * ({@link #overclockSpeed}, {@link #overclockPower}); recipe tier + level may not go above tier V for now.
 *
 * Declare a machine as a list of recipes:
 * <pre>
 * gearPress = new RecipeCrafter("gear-press"){{
 *     requirements(...); size = 2;
 *     recipe(1, 30f, with(Items.copper, 2), with(copperGear, 1));
 *     recipe(2, 60f, with(Items.titanium, 3), with(titaniumGear, 2));
 * }};
 * </pre>
 * Each recipe's first output names it and must be unique within the machine. Liquids, per tick like vanilla consumeLiquid:
 * <pre>
 *     recipe(3, 60f, with(copperWire, 2, polyethylene, 1), with(circuitBoard, 1))
 *         .liquidsIn(hydrochloricAcid, 2f / 60f).liquidsOut(acidicWaste, 1f / 60f);
 * </pre>
 * A full liquid output stops the machine (waste must be removed), like the other ECLIPSE chemistry.
 *
 * {@link #autoSelect}: with no recipe chosen, the machine runs whichever recipe its inputs complete, so a mixed belt never
 * jams it (e.g. the Vacuum Freezer cooling any hot metal that arrives).
 */
public class RecipeCrafter extends GenericCrafter{
    /** Power per tick while running, by tier (index = tier): 30, 120, 480, 1920 and 7680 per second. */
    public static final float[] tierPower = {0f, 0.5f, 2f, 8f, 32f, 128f};
    public static final int maxTier = 5;
    /** Speed and power multipliers per overclock level 0-III. */
    public static final float[] overclockSpeed = {1f, 2f, 4f, 8f}, overclockPower = {1f, 5f, 30f, 200f};
    /** Below this grid satisfaction a machine on a tier recipe stops. To tune in playtesting (e.g. 0.9). */
    public static float minSatisfaction = 0.999f;

    public final Seq<Recipe> recipes = new Seq<>();
    /** With no recipe chosen, accept every recipe's inputs and run whichever recipe they complete. */
    public boolean autoSelect = false;

    public static class Recipe{
        public final int tier;
        /** Ticks per craft at overclock 0. */
        public final float time;
        public final ItemStack[] inputs, outputs;
        /** Liquids per tick at overclock 0. */
        public LiquidStack[] liquidInputs = {}, liquidOutputs = {};

        public Recipe(int tier, float time, ItemStack[] inputs, ItemStack[] outputs){
            if(tier < 1 || tier > maxTier) throw new IllegalArgumentException("recipe tier must be 1-" + maxTier + ", got " + tier);
            if(outputs.length == 0) throw new IllegalArgumentException("recipe without outputs");
            this.tier = tier;
            this.time = time;
            this.inputs = inputs;
            this.outputs = outputs;
        }

        /** Liquid inputs as liquid/amount-per-tick pairs. */
        public Recipe liquidsIn(Object... stacks){
            liquidInputs = LiquidStack.with(stacks);
            return this;
        }

        /** Liquid outputs as liquid/amount-per-tick pairs. */
        public Recipe liquidsOut(Object... stacks){
            liquidOutputs = LiquidStack.with(stacks);
            return this;
        }

        public boolean usesLiquid(Liquid liquid){
            return Structs.contains(liquidInputs, s -> s.liquid == liquid);
        }

        public boolean makesLiquid(Liquid liquid){
            return Structs.contains(liquidOutputs, s -> s.liquid == liquid);
        }

        /** The item that names this recipe. */
        public Item output(){
            return outputs[0].item;
        }

        /** Power per tick at overclock 0. */
        public float power(){
            return tierPower[tier];
        }

        public int maxOverclock(){
            return Math.min(overclockSpeed.length - 1, maxTier - tier);
        }

        public boolean uses(Item item){
            return Structs.contains(inputs, s -> s.item == item);
        }

        /** Shown in the recipe menu once every ingredient is unlocked (always, outside the campaign). */
        public boolean available(){
            return !Structs.contains(inputs, s -> !s.item.unlockedNow());
        }
    }

    public RecipeCrafter(String name){
        super(name);
        configurable = true;
        saveConfig = true;
        clearOnDoubleTap = true;
        logicConfigurable = true;

        consume(new ConsumeItemDynamic((RecipeBuild b) -> b.recipe == null ? ItemStack.empty : b.recipe.inputs));
        consumePowerDynamic((RecipeBuild b) -> b.powerUse());

        //logic and the recipe menu: pick by output item, keep the overclock level
        config(Item.class, (RecipeBuild b, Item item) -> b.select(recipeFor(item), b.overclock));
        //schematics and copying: recipe and overclock packed together
        config(Integer.class, (RecipeBuild b, Integer v) -> b.select(unpackRecipe(v), v >> 8));
        configClear((RecipeBuild b) -> b.select(null, 0));
    }

    /** Adds a recipe: tier 1-5, ticks per craft, inputs, outputs (the first output names the recipe). */
    public Recipe recipe(int tier, float time, ItemStack[] inputs, ItemStack[] outputs){
        Recipe r = new Recipe(tier, time, inputs, outputs);
        recipes.add(r);
        return r;
    }

    public @Nullable Recipe recipeFor(@Nullable Item output){
        return output == null ? null : recipes.find(r -> r.output() == output);
    }

    public boolean usesItem(Item item){
        return recipes.contains(r -> r.uses(item));
    }

    public boolean usesLiquid(Liquid liquid){
        return recipes.contains(r -> r.usesLiquid(liquid));
    }

    public boolean makesLiquid(Liquid liquid){
        return recipes.contains(r -> r.makesLiquid(liquid));
    }

    public static int pack(@Nullable Recipe r, int overclock, RecipeCrafter block){
        return (overclock << 8) | (r == null ? 0 : block.recipes.indexOf(r) + 1);
    }

    @Nullable Recipe unpackRecipe(int v){
        int i = (v & 0xff) - 1;
        return i >= 0 && i < recipes.size ? recipes.get(i) : null;
    }

    public static String roman(int n){
        return new String[]{"0", "I", "II", "III", "IV", "V"}[n];
    }

    @Override
    public void init(){
        if(recipes.isEmpty()) throw new IllegalStateException(name + " has no recipes");
        //every product, so generic code (wiki, augmentation filters, animations) sees what this machine can make
        Seq<ItemStack> outs = new Seq<>();
        ObjectSet<Item> names = new ObjectSet<>();
        for(Recipe r : recipes){
            if(!names.add(r.output())) throw new IllegalStateException(name + " has two recipes for " + r.output().name);
            for(ItemStack s : r.outputs) if(!outs.contains(o -> o.item == s.item)) outs.add(s);
            for(ItemStack s : r.inputs) itemCapacity = Math.max(itemCapacity, s.amount * 2);
            for(ItemStack s : r.outputs) itemCapacity = Math.max(itemCapacity, s.amount * 2);
        }
        outputItems = outs.toArray(ItemStack.class);
        craftTime = recipes.first().time;
        //liquid handling only for machines that have liquid recipes, so dry machines don't count as chemical plants
        if(recipes.contains(r -> r.liquidInputs.length > 0 || r.liquidOutputs.length > 0)){
            hasLiquids = true;
            outputsLiquid = recipes.contains(r -> r.liquidOutputs.length > 0);
            consume(new ConsumeLiquidsDynamic((RecipeBuild b) -> b.liquidInputs));
        }
        super.init();
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.remove(Stat.output);
        stats.remove(Stat.productionTime);
        stats.add(Stat.output, table -> {
            table.row();
            for(Recipe r : recipes){
                table.table(Styles.grayPanel, t -> {
                    t.table(in -> {
                        for(ItemStack s : r.inputs) in.add(StatValues.displayItem(s.item, s.amount, r.time, true)).pad(2f);
                        for(LiquidStack s : r.liquidInputs) in.add(StatValues.displayLiquid(s.liquid, s.amount * 60f, true)).pad(2f);
                    }).left();
                    t.add(" -> ").pad(4f);
                    t.table(out -> {
                        for(ItemStack s : r.outputs) out.add(StatValues.displayItem(s.item, s.amount, r.time, true)).pad(2f);
                        for(LiquidStack s : r.liquidOutputs) out.add(StatValues.displayLiquid(s.liquid, s.amount * 60f, true)).pad(2f);
                    }).left().growX();
                    t.add(Core.bundle.format("eclipse-recipe.tier", roman(r.tier), Strings.autoFixed(r.power() * 60f, 1))).right().pad(4f);
                }).growX().pad(4f).row();
            }
        });
    }

    public class RecipeBuild extends GenericCrafterBuild{
        /** The recipe the player chose (null: none, or automatic on autoSelect machines). */
        public @Nullable Recipe fixed;
        /** The recipe running now: the chosen one, or the one picked automatically. */
        public @Nullable Recipe recipe;
        /** Overclock level the player chose (0-III), and the level in effect for the running recipe (capped by its tier). */
        public int level, overclock;
        /** The recipe's liquid inputs scaled by the overclock speed (what the liquid consumer takes per tick). */
        public LiquidStack[] liquidInputs = {};

        public void select(@Nullable Recipe r, int lvl){
            fixed = r;
            level = Math.max(0, Math.min(lvl, overclockSpeed.length - 1));
            activate(r);
        }

        /** Whether the machine picks its recipe from its inputs right now. */
        public boolean auto(){
            return autoSelect && fixed == null;
        }

        void activate(@Nullable Recipe r){
            if(r != recipe) progress = 0f;
            recipe = r;
            overclock = r == null ? 0 : Math.min(level, r.maxOverclock());
            liquidInputs = r == null ? new LiquidStack[0] : new LiquidStack[r.liquidInputs.length];
            for(int i = 0; i < liquidInputs.length; i++){
                liquidInputs[i] = new LiquidStack(r.liquidInputs[i].liquid, r.liquidInputs[i].amount * overclockSpeed[overclock]);
            }
        }

        boolean ready(Recipe r){
            return items.has(r.inputs);
        }

        /** The next recipe after the running one that has its inputs (round robin, so no input waits forever). */
        @Nullable Recipe nextReady(){
            int start = recipe == null ? -1 : recipes.indexOf(recipe);
            for(int i = 1; i <= recipes.size; i++){
                Recipe r = recipes.get((start + i) % recipes.size);
                if(ready(r)) return r;
            }
            return null;
        }

        /** Power per tick wanted right now: the recipe's tier power times the overclock factor. */
        public float powerUse(){
            return recipe == null || !shouldConsume() ? 0f : recipe.power() * overclockPower[overclock];
        }

        /** Power per tick the current recipe needs to run (0 without a recipe). */
        public float powerNeeded(){
            return recipe == null ? 0f : recipe.power() * overclockPower[overclock];
        }

        /** Has power, but not enough of it: the machine stops instead of slowing down. */
        public boolean underpowered(){
            return recipe != null && power != null && !cheating() && power.status < minSatisfaction;
        }

        @Override
        public float efficiencyScale(){
            return underpowered() ? 0f : super.efficiencyScale();
        }

        @Override
        public boolean shouldConsume(){
            if(recipe == null || !enabled) return false;
            for(ItemStack out : recipe.outputs) if(items.get(out.item) + out.amount > itemCapacity) return false;
            for(LiquidStack out : recipe.liquidOutputs) if(liquids.get(out.liquid) >= liquidCapacity - 0.001f) return false;
            return true;
        }

        @Override
        public void updateTile(){
            //automatic: keep the running recipe while it has its inputs, otherwise switch to one that does
            if(auto() && (recipe == null || !ready(recipe))){
                Recipe next = nextReady();
                if(next != null) activate(next);
            }
            super.updateTile();
            if(recipe == null) return;
            if(efficiency > 0f){
                float scale = edelta() * overclockSpeed[overclock];
                for(LiquidStack out : recipe.liquidOutputs){
                    handleLiquid(this, out.liquid, Math.min(out.amount * scale, liquidCapacity - liquids.get(out.liquid)));
                }
            }
            for(LiquidStack out : recipe.liquidOutputs) dumpLiquid(out.liquid, 2f);
            //liquid left from a previous recipe leaves too
            if(hasLiquids){
                liquids.each((liquid, amount) -> {
                    boolean needed = auto() ? usesLiquid(liquid) : recipe.usesLiquid(liquid);
                    if(amount > 0.001f && !needed && !recipe.makesLiquid(liquid)) dumpLiquid(liquid, 2f);
                });
            }
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            boolean wanted = auto() ? usesLiquid(liquid) : recipe != null && recipe.usesLiquid(liquid);
            return wanted && liquids.get(liquid) < liquidCapacity;
        }

        @Override
        public float getProgressIncrease(float base){
            return recipe == null ? 0f : super.getProgressIncrease(recipe.time / overclockSpeed[overclock]);
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            boolean wanted = auto() ? usesItem(item) : recipe != null && recipe.uses(item);
            return wanted && items.get(item) < itemCapacity;
        }

        @Override
        public void craft(){
            if(recipe == null) return;
            consume();
            for(ItemStack out : recipe.outputs) for(int i = 0; i < out.amount; i++) offload(out.item);
            if(wasVisible) craftEffect.at(x, y);
            progress %= 1f;
            //automatic: take turns, so one busy input can't starve the others
            if(auto()){
                Recipe next = nextReady();
                if(next != null) activate(next);
            }
        }

        /** Products leave; so do leftovers from a previous recipe, so a recipe change never jams the machine. */
        @Override
        public void dumpOutputs(){
            dump();
        }

        @Override
        public boolean canDump(Building to, Item item){
            return auto() ? !usesItem(item) : recipe == null || !recipe.uses(item);
        }

        @Override
        public void buildConfiguration(Table table){
            Seq<Item> outputs = recipes.select(Recipe::available).map(Recipe::output);
            ItemSelection.buildTable(RecipeCrafter.this, table, outputs, () -> fixed == null ? null : fixed.output(),
                item -> configure(pack(recipeFor(item), level, RecipeCrafter.this)));
            table.row();
            table.table(Styles.black6, t -> {
                if(autoSelect){
                    t.add("[lightgray]" + Core.bundle.get("eclipse-recipe.auto")).colspan(overclockSpeed.length + 1).left().wrap().width(240f).row();
                }
                t.add(Core.bundle.get("eclipse-recipe.overclock")).padRight(6f);
                for(int i = 0; i < overclockSpeed.length; i++){
                    int lvl = i;
                    t.button(roman(lvl), Styles.flatTogglet, () -> configure(pack(fixed, lvl, RecipeCrafter.this))).size(40f)
                        .disabled(b -> fixed == null ? !autoSelect : lvl > fixed.maxOverclock()).checked(b -> level == lvl);
                }
                t.row();
                t.label(() -> recipe == null ? "" : Core.bundle.format("eclipse-recipe.overclock-info", Strings.autoFixed(overclockSpeed[overclock], 1),
                    Strings.autoFixed(powerNeeded() * 60f, 1))).colspan(overclockSpeed.length + 1).left();
            }).growX();
        }

        @Override
        public Object config(){
            return fixed == null && level == 0 ? null : pack(fixed, level, RecipeCrafter.this);
        }

        @Override
        public void draw(){
            super.draw();
            if(recipe != null) Draw.rect(recipe.output().fullIcon, x, y, 4f + size * 2f, 4f + size * 2f);
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.s(fixed == null ? -1 : fixed.output().id);
            write.b(level);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1){
                short id = read.s();
                int lvl = read.b();
                float saved = progress; //selecting resets progress; a loaded machine keeps it
                select(id < 0 ? null : recipeFor(content.item(id)), lvl);
                progress = saved;
            }
        }
    }
}
