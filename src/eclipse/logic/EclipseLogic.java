package eclipse.logic;

import arc.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import eclipse.augment.*;
import eclipse.augment.AugmentationCoupler.*;
import eclipse.defense.*;
import eclipse.defense.OrbitalPlatform.*;
import eclipse.defense.WaveScanner.*;
import eclipse.ui.*;
import eclipse.world.blocks.*;
import eclipse.world.blocks.ChemicalReactor.*;
import eclipse.world.blocks.RecipeCrafter.*;
import eclipse.world.blocks.WorldEngine.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.heat.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.production.HeatCrafter.*;

import static mindustry.Vars.*;

/**
 * ECLIPSE's logic instructions. Vanilla sensors (@heat, @progress, @config...) cover what fits them; these cover the rest:
 * <ul>
 * <li>{@code esensor result block field}: reads an ECLIPSE value ({@link Field}) from a block.</li>
 * <li>{@code econtrol block field value [value2]}: sets a machine's overclock or recipe, or aims the Orbital Platform.
 * Like vanilla control, the block must be linked to the processor.</li>
 * <li>{@code recipe result item field index}: what the ECLIPSE machine making an item needs (machine, time, power...).</li>
 * </ul>
 * {@link #fields(Block)} lists what a block answers; the wiki shows it on every block page.
 */
public class EclipseLogic{
    /** What esensor reads. Blocks that don't have a value return null (and 0 when used as a number). */
    public enum Field{
        /** Why a crafter is stopped, as a number: 0 working (see the wiki for the rest). */
        status,
        /** 1 if the crafter is stopped by a problem (not just warming up), else 0. */
        stalled,
        /** The item or liquid a stopped crafter waits for or can't get rid of. */
        missing,
        /** The product of the chosen recipe (universal machines). */
        recipe,
        /** Overclock level 0-3 (universal machines). */
        overclock,
        /** Heat the block has or receives. */
        heat,
        /** Pressure built up, 0-1. */
        pressure,
        /** Crafts left before the catalyst in the machine runs out. */
        catalyst,
        /** Charge of the Orbital Platform, or World Engine build progress, 0-1. */
        charge,
        /** World Engine: energy in its store. */
        stored,
        /** World Engine: construction stage (6 when complete). */
        stage,
        /** Orbital Platform: target tile, or -1. */
        targetX, targetY,
        /** Wave Scanner (powered): next wave's ground units, aircraft, bosses and seconds until it comes. */
        waveGround, waveAir, waveBoss, waveTime,
        /** Augmentation couplers: working modules. */
        modules
    }

    /** What econtrol sets. */
    public enum Setting{
        /** Overclock level 0-3 (universal machines). */
        overclock,
        /** Recipe by its product item or liquid (universal machines); null clears it. */
        recipe,
        /** Orbital Platform target tile (x, y); a negative x clears it. */
        target
    }

    /** What the recipe instruction reports. */
    public enum RecipeField{
        /** The ECLIPSE machine that makes it. */
        machine,
        /** Seconds per craft, at base speed. */
        time,
        /** Amount made per craft. */
        amount,
        /** Power per second while running. */
        power,
        /** Power tier 1-5 (0 for machines outside the tier system). */
        tier,
        /** Number of item and liquid inputs. */
        inputs,
        /** The index-th input (items first, then liquids). */
        input,
        /** Amount of the index-th input per craft. */
        inputAmount
    }

    public static void register(){
        LAssembler.customParsers.put("esensor", t -> {
            ESensorStatement s = new ESensorStatement();
            if(t.length > 1) s.result = t[1];
            if(t.length > 2) s.target = t[2];
            if(t.length > 3) s.field = parse(Field.class, t[3], Field.status);
            s.afterRead();
            return s;
        });
        LAssembler.customParsers.put("econtrol", t -> {
            EControlStatement s = new EControlStatement();
            if(t.length > 1) s.setting = parse(Setting.class, t[1], Setting.overclock);
            if(t.length > 2) s.target = t[2];
            if(t.length > 3) s.p1 = t[3];
            if(t.length > 4) s.p2 = t[4];
            s.afterRead();
            return s;
        });
        LAssembler.customParsers.put("recipe", t -> {
            RecipeStatement s = new RecipeStatement();
            if(t.length > 1) s.result = t[1];
            if(t.length > 2) s.item = t[2];
            if(t.length > 3) s.field = parse(RecipeField.class, t[3], RecipeField.machine);
            if(t.length > 4) s.index = t[4];
            s.afterRead();
            return s;
        });
        LogicIO.allStatements.add(ESensorStatement::new);
        LogicIO.allStatements.add(EControlStatement::new);
        LogicIO.allStatements.add(RecipeStatement::new);
    }

    static <T extends Enum<T>> T parse(Class<T> type, String name, T def){
        for(T t : type.getEnumConstants()) if(t.name().equals(name)) return t;
        return def;
    }

    // ---- what each block answers ----

    /** The esensor fields a block answers, for the wiki. */
    public static Seq<Field> fields(Block b){
        Seq<Field> out = new Seq<>();
        if(b instanceof GenericCrafter) out.addAll(Field.status, Field.stalled, Field.missing);
        if(b instanceof RecipeCrafter) out.addAll(Field.recipe, Field.overclock);
        if(b instanceof HeatProducer || b instanceof HeatCrafter || b instanceof HeatConductor || (b instanceof RecipeCrafter rc && rc.usesHeat)) out.add(Field.heat);
        if((b instanceof ChemicalReactor r && r.pressureRequired > 0f) || (b instanceof RecipeCrafter rc && rc.usesPressure)) out.add(Field.pressure);
        if((b instanceof ChemicalReactor r && r.catalyst != null) || (b instanceof RecipeCrafter rc && rc.usesCatalyst)) out.add(Field.catalyst);
        if(b instanceof OrbitalPlatform) out.addAll(Field.charge, Field.targetX, Field.targetY);
        if(b instanceof WorldEngine) out.addAll(Field.charge, Field.stored, Field.stage);
        if(b instanceof WaveScanner) out.addAll(Field.waveGround, Field.waveAir, Field.waveBoss, Field.waveTime);
        if(b instanceof AugmentationCoupler) out.add(Field.modules);
        return out;
    }

    /** The econtrol settings a block accepts, for the wiki. */
    public static Seq<Setting> settings(Block b){
        Seq<Setting> out = new Seq<>();
        if(b instanceof RecipeCrafter) out.addAll(Setting.overclock, Setting.recipe);
        if(b instanceof OrbitalPlatform) out.add(Setting.target);
        return out;
    }

    /** An ECLIPSE value of a building: a number, a content, or null when the block doesn't have it. */
    public static @Nullable Object sense(Building b, Field f){
        switch(f){
            case status -> {
                return b.block instanceof GenericCrafter ? (double)MachineStatus.of(b).kind.ordinal() : null;
            }
            case stalled -> {
                return b.block instanceof GenericCrafter ? (MachineStatus.of(b).problem() ? 1d : 0d) : null;
            }
            case missing -> {
                return b.block instanceof GenericCrafter ? MachineStatus.of(b).content : null;
            }
            case recipe -> {
                return b instanceof RecipeBuild r && r.recipe != null ? r.recipe.output() : null;
            }
            case overclock -> {
                return b instanceof RecipeBuild r ? (double)r.level : null;
            }
            case heat -> {
                if(b instanceof RecipeBuild r) return (double)r.heat;
                if(b instanceof HeatCrafterBuild h) return (double)h.heat;
                if(b instanceof HeatBlock h) return (double)h.heat();
                return null;
            }
            case pressure -> {
                if(b instanceof RecipeBuild r) return (double)r.pressure;
                if(b instanceof ReactorBuild r) return (double)r.pressure;
                return null;
            }
            case catalyst -> {
                if(b instanceof RecipeBuild r){
                    if(r.recipe == null || r.recipe.catalyst == null) return 0d;
                    return (double)Math.max(r.items.get(r.recipe.catalyst) * r.recipe.craftsPerCatalyst - r.catalystUses, 0);
                }
                if(b instanceof ReactorBuild r && r.block instanceof ChemicalReactor c && c.catalyst != null){
                    return (double)Math.max(r.items.get(c.catalyst) * c.craftsPerCatalyst - r.catalystUses, 0);
                }
                return null;
            }
            case charge -> {
                if(b instanceof OrbitalBuild o) return (double)o.chargeFraction();
                if(b instanceof EngineBuild e) return (double)e.progress();
                return null;
            }
            case stored -> {
                return b instanceof EngineBuild e ? (double)e.stored : null;
            }
            case stage -> {
                return b instanceof EngineBuild e ? (double)e.stage : null;
            }
            case targetX, targetY -> {
                if(!(b instanceof OrbitalBuild o)) return null;
                if(!o.validTarget()) return -1d;
                Tile t = world.tile(o.target);
                return (double)(f == Field.targetX ? t.x : t.y);
            }
            case waveGround, waveAir, waveBoss, waveTime -> {
                if(!(b instanceof ScannerBuild s)) return null;
                if(!s.online() || !state.rules.waves) return -1d;
                if(f == Field.waveTime) return (double)(state.wavetime / 60f);
                int n = 0;
                for(WaveInfo.Entry e : nextWave()){
                    if(f == Field.waveBoss ? e.boss : (f == Field.waveAir) == e.type.flying) n += e.count;
                }
                return (double)n;
            }
            case modules -> {
                return b instanceof CouplerBuild c ? (double)c.workingModules().size : null;
            }
        }
        return null;
    }

    static Seq<WaveInfo.Entry> waveCache = new Seq<>();
    static int waveCacheWave = -1;
    static float waveCacheTime = -1f;

    /** The next wave, computed once per wave and game second however many processors ask. */
    static Seq<WaveInfo.Entry> nextWave(){
        if(waveCacheWave != state.wave || Math.abs(Time.time - waveCacheTime) > 60f){
            waveCache = WaveInfo.nextWave();
            waveCacheWave = state.wave;
            waveCacheTime = Time.time;
        }
        return waveCache;
    }

    /** Applies an econtrol setting; false if the block doesn't take it. */
    public static boolean control(Building b, Setting s, @Nullable Object v1, double n1, double n2){
        switch(s){
            case overclock -> {
                if(!(b instanceof RecipeBuild r)) return false;
                int level = Math.max(0, Math.min((int)n1, RecipeCrafter.overclockSpeed.length - 1));
                if(level != r.level) b.configureAny(RecipeCrafter.pack(r.fixed, level, (RecipeCrafter)b.block));
                return true;
            }
            case recipe -> {
                if(!(b instanceof RecipeBuild r)) return false;
                RecipeCrafter rc = (RecipeCrafter)b.block;
                Recipe target = v1 instanceof UnlockableContent c ? rc.recipeFor(c) : null;
                if(target != r.fixed) b.configureAny(RecipeCrafter.pack(target, r.level, rc));
                return true;
            }
            case target -> {
                if(!(b instanceof OrbitalBuild o)) return false;
                Tile t = n1 < 0 ? null : world.tile((int)n1, (int)n2);
                int pos = t == null ? -1 : t.pos();
                if(pos != o.target) b.configureAny(pos);
                return true;
            }
        }
        return false;
    }

    /** A value of the recipe making an item, or null when no ECLIPSE machine makes it. */
    public static @Nullable Object recipe(@Nullable Object item, RecipeField f, int index){
        if(!(item instanceof Item it)) return null;
        RecipeTree.Source s = RecipeTree.sourceOf(it);
        if(s == null) return null;
        int items = s.inputs.length, all = items + s.liquids.length;
        return switch(f){
            case machine -> s.block;
            case time -> (double)s.seconds;
            case amount -> (double)s.amount;
            case power -> (double)(s.power * 60f);
            case tier -> (double)s.tier;
            case inputs -> (double)all;
            case input -> index < 0 || index >= all ? null : index < items ? s.inputs[index].item : s.liquids[index - items].liquid;
            case inputAmount -> index < 0 || index >= all ? null
                : index < items ? (double)s.inputs[index].amount : (double)(s.liquids[index - items].amount * 60f * s.seconds);
        };
    }

    static void set(LVar out, @Nullable Object v){
        if(v instanceof Double d) out.setnum(d);
        else out.setobj(v);
    }

    // ---- statements ----

    public static class ESensorStatement extends LStatement{
        public String result = "result", target = "block1";
        public Field field = Field.status;

        @Override
        public void build(Table table){
            table.clearChildren();
            field(table, result, s -> result = s);
            table.add(" = ");
            table.button(b -> {
                b.label(() -> field.name());
                b.clicked(() -> showSelect(b, Field.values(), field, f -> field = f, 2, c -> c.width(150f)));
            }, mindustry.ui.Styles.logict, () -> {}).size(160f, 40f).color(table.color);
            table.add(" in ");
            field(table, target, s -> target = s);
        }

        @Override
        public LExecutor.LInstruction build(LAssembler b){
            LVar out = b.var(result), block = b.var(target);
            Field f = field;
            return exec -> set(out, block.obj() instanceof Building build ? sense(build, f) : null);
        }

        @Override
        public LCategory category(){
            return LCategory.block;
        }

        @Override
        public void write(StringBuilder b){
            b.append("esensor ").append(result).append(' ').append(target).append(' ').append(field.name());
        }
    }

    public static class EControlStatement extends LStatement{
        public Setting setting = Setting.overclock;
        public String target = "block1", p1 = "0", p2 = "0";

        @Override
        public void build(Table table){
            table.clearChildren();
            table.add(" set ");
            table.button(b -> {
                b.label(() -> setting.name());
                b.clicked(() -> showSelect(b, Setting.values(), setting, s -> {
                    setting = s;
                    build(table);
                }));
            }, mindustry.ui.Styles.logict, () -> {}).size(130f, 40f).color(table.color);
            table.add(" of ");
            field(table, target, s -> target = s);
            table.add(" to ");
            fields(table, setting == Setting.target ? "x" : "value", p1, s -> p1 = s);
            if(setting == Setting.target) fields(table, "y", p2, s -> p2 = s);
        }

        @Override
        public LExecutor.LInstruction build(LAssembler b){
            LVar block = b.var(target), v1 = b.var(p1), v2 = b.var(p2);
            Setting s = setting;
            return exec -> {
                if(!(block.obj() instanceof Building build)) return;
                if(!exec.privileged && (exec.build == null || !exec.build.validLink(build))) return;
                control(build, s, v1.obj(), v1.num(), v2.num());
            };
        }

        @Override
        public LCategory category(){
            return LCategory.block;
        }

        @Override
        public void write(StringBuilder b){
            b.append("econtrol ").append(setting.name()).append(' ').append(target).append(' ').append(p1).append(' ').append(p2);
        }
    }

    public static class RecipeStatement extends LStatement{
        public String result = "result", item = "@eclipse-copper-gear", index = "0";
        public RecipeField field = RecipeField.machine;

        @Override
        public void build(Table table){
            table.clearChildren();
            field(table, result, s -> result = s);
            table.add(" = ");
            table.button(b -> {
                b.label(() -> field.name());
                b.clicked(() -> showSelect(b, RecipeField.values(), field, f -> {
                    field = f;
                    build(table);
                }, 2, c -> c.width(150f)));
            }, mindustry.ui.Styles.logict, () -> {}).size(150f, 40f).color(table.color);
            table.add(" of ");
            field(table, item, s -> item = s);
            if(field == RecipeField.input || field == RecipeField.inputAmount) fields(table, "#", index, s -> index = s);
        }

        @Override
        public LExecutor.LInstruction build(LAssembler b){
            LVar out = b.var(result), it = b.var(item), idx = b.var(index);
            RecipeField f = field;
            return exec -> set(out, recipe(it.obj(), f, (int)idx.num()));
        }

        @Override
        public LCategory category(){
            return LCategory.operation;
        }

        @Override
        public void write(StringBuilder b){
            b.append("recipe ").append(result).append(' ').append(item).append(' ').append(field.name()).append(' ').append(index);
        }
    }
}
