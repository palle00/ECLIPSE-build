package eclipse.ui;

import arc.*;
import arc.input.*;
import arc.scene.ui.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;

import static mindustry.Vars.*;

/**
 * Data for the production panel ({@link ProductionDialog}): for every item and liquid, how much the player's machines,
 * drills and generators make and use per second, the last minute of that as history, and totals since the world
 * loaded; and the same for power, by generator and consumer block. Sampled twice a second on every client.
 */
public class ProductionStats{
    /** Samples per second, and how many are kept: one minute of history. */
    public static final float interval = 30f;
    public static final int historySize = 120;

    /** Smoothed rates per second (about the last three seconds). */
    public static final ObjectFloatMap<UnlockableContent> made = new ObjectFloatMap<>(), used = new ObjectFloatMap<>();
    /** Amounts made and used since the world loaded. */
    public static final ObjectFloatMap<UnlockableContent> totalMade = new ObjectFloatMap<>(), totalUsed = new ObjectFloatMap<>();
    static final ObjectMap<UnlockableContent, float[]> histMade = new ObjectMap<>(), histUsed = new ObjectMap<>();
    static int head, filled;

    /** Power, per second: made, wanted, in batteries, battery capacity, and how much of the demand is met (0-1). */
    public static float powerMade, powerUsed, powerStored, powerCapacity, satisfaction = 1f;
    static final float[] histPowerMade = new float[historySize], histPowerUsed = new float[historySize];
    /** Power per second by block, and how many of each block are running. */
    public static final ObjectFloatMap<Block> powerByGenerator = new ObjectFloatMap<>(), powerByConsumer = new ObjectFloatMap<>();
    public static final ObjectIntMap<Block> generatorCount = new ObjectIntMap<>(), consumerCount = new ObjectIntMap<>();

    static final ObjectFloatMap<UnlockableContent> sampleIn = new ObjectFloatMap<>(), sampleOut = new ObjectFloatMap<>();
    static final ObjectFloatMap<UnlockableContent> oneIn = new ObjectFloatMap<>(), oneOut = new ObjectFloatMap<>();

    /** Per material, per block: what each kind of building makes or uses per second right now, and how many of them do. */
    public static final ObjectMap<UnlockableContent, ObjectFloatMap<Block>> madeBy = new ObjectMap<>(), usedBy = new ObjectMap<>();
    public static final ObjectMap<UnlockableContent, ObjectIntMap<Block>> madeByCount = new ObjectMap<>(), usedByCount = new ObjectMap<>();

    /** Machines that make things (crafters), how many of them are running, and the stopped ones grouped by block and reason. */
    public static int crafters, running;
    public static final Seq<Stall> stalls = new Seq<>();

    /** Stopped machines of one block for one reason (and the same missing or blocked material). */
    public static class Stall{
        public Block block;
        public MachineStatus.Status status;
        public int count;
    }
    static final float smoothing = 0.35f;
    static float timer;
    public static @Nullable KeyBind key;

    /** Registers the key early (mod constructor) so the Controls menu lists it. Desktop clients only. */
    public static void registerKey(){
        if(headless || mobile) return;
        key = KeyBind.add("eclipse_production", KeyCode.o, "eclipse");
        Events.run(Trigger.update, () -> {
            if(ProductionDialog.instance == null || !state.isGame() || !Core.input.keyTap(key) || Core.scene.getKeyboardFocus() instanceof TextField) return;
            if(ProductionDialog.instance.isShown()) ProductionDialog.instance.hide();
            else ProductionDialog.instance.show();
        });
    }

    /** Sampling (any client) and the panel. Call on ClientLoadEvent. */
    public static void install(){
        Events.run(Trigger.update, () -> {
            if(!state.isGame() || player == null) return;
            if((timer += Time.delta) < interval) return;
            timer = 0f;
            sample(player.team());
        });
        Events.on(WorldLoadEvent.class, e -> clear());
        ProductionDialog.instance = new ProductionDialog();
    }

    public static void clear(){
        made.clear();
        used.clear();
        totalMade.clear();
        totalUsed.clear();
        histMade.clear();
        histUsed.clear();
        head = filled = 0;
        java.util.Arrays.fill(histPowerMade, 0f);
        java.util.Arrays.fill(histPowerUsed, 0f);
        powerMade = powerUsed = powerStored = powerCapacity = 0f;
        satisfaction = 1f;
        madeBy.clear();
        usedBy.clear();
        madeByCount.clear();
        usedByCount.clear();
        stalls.clear();
        crafters = running = 0;
    }

    /** One sample of a team's rates: blended into the averages, added to the history and the totals. */
    public static void sample(Team team){
        sampleIn.clear();
        sampleOut.clear();
        powerByGenerator.clear();
        powerByConsumer.clear();
        generatorCount.clear();
        consumerCount.clear();
        madeBy.clear();
        usedBy.clear();
        madeByCount.clear();
        usedByCount.clear();
        crafters = running = 0;
        ObjectMap<String, Stall> groups = new ObjectMap<>();
        //power made: every generator's output; used: what consumers ask for, times how much of it their grid can give
        ObjectSet<PowerGraph> graphs = new ObjectSet<>();
        float pm = 0f, pu = 0f, wanted = 0f;
        for(Building b : team.data().buildings){
            if(!b.isValid()) continue;
            //what this building makes and uses, added to the totals and to its block's share
            oneIn.clear();
            oneOut.clear();
            Rates.of(b, oneIn, oneOut);
            for(var e : oneOut){
                sampleOut.increment(e.key, 0f, e.value);
                madeBy.get(e.key, ObjectFloatMap::new).increment(b.block, 0f, e.value);
                madeByCount.get(e.key, ObjectIntMap::new).increment(b.block);
            }
            for(var e : oneIn){
                sampleIn.increment(e.key, 0f, e.value);
                usedBy.get(e.key, ObjectFloatMap::new).increment(b.block, 0f, e.value);
                usedByCount.get(e.key, ObjectIntMap::new).increment(b.block);
            }
            //crafters: running, or stopped and why (grouped)
            if(b.block instanceof mindustry.world.blocks.production.GenericCrafter && !(b.block instanceof mindustry.world.blocks.heat.HeatProducer)){
                crafters++;
                MachineStatus.Status s = MachineStatus.of(b);
                if(!s.problem()) running++;
                else{
                    Stall st = groups.get(b.block.name + "/" + s.kind + "/" + (s.content == null ? "" : s.content.name), () -> {
                        Stall n = new Stall();
                        n.block = b.block;
                        n.status = new MachineStatus.Status().copy(s);
                        return n;
                    });
                    st.count++;
                }
            }
            if(b.power == null) continue;
            graphs.add(b.power.graph);
            if(b instanceof PowerGenerator.GeneratorBuild g && g.getPowerProduction() > 0f){
                float out = g.getPowerProduction() * 60f * g.timeScale();
                powerByGenerator.increment(b.block, 0f, out);
                generatorCount.increment(b.block);
                pm += out;
            }else if(b.block.consPower != null && !b.block.consPower.buffered && b.shouldConsumePower){ //as the grid: idle machines draw nothing
                float want = b.block.consPower.requestedPower(b) * 60f;
                if(want > 0f){
                    powerByConsumer.increment(b.block, 0f, want);
                    consumerCount.increment(b.block);
                    wanted += want;
                    pu += want * b.power.graph.getSatisfaction();
                }
            }
        }
        stalls.clear();
        for(Stall st : groups.values()) stalls.add(st);
        stalls.sort(st -> -st.count);
        blend(made, sampleOut);
        blend(used, sampleIn);
        float seconds = interval / 60f;
        for(var e : sampleOut) totalMade.increment(e.key, 0f, e.value * seconds);
        for(var e : sampleIn) totalUsed.increment(e.key, 0f, e.value * seconds);

        float ps = 0f, pc = 0f;
        for(PowerGraph g : graphs){
            ps += g.getLastPowerStored();
            pc += g.getLastCapacity();
        }
        powerMade = powerMade * (1f - smoothing) + pm * smoothing;
        powerUsed = powerUsed * (1f - smoothing) + pu * smoothing;
        powerStored = ps;
        powerCapacity = pc;
        satisfaction = wanted <= 0f ? 1f : Math.min(pu / wanted, 1f);

        //history: one value per tracked material and for power at the same index
        for(var e : made) if(!histMade.containsKey(e.key)) histMade.put(e.key, new float[historySize]);
        for(var e : used) if(!histUsed.containsKey(e.key)) histUsed.put(e.key, new float[historySize]);
        for(var e : histMade) e.value[head] = made.get(e.key, 0f);
        for(var e : histUsed) e.value[head] = used.get(e.key, 0f);
        histPowerMade[head] = powerMade;
        histPowerUsed[head] = powerUsed;
        head = (head + 1) % historySize;
        filled = Math.min(filled + 1, historySize);
    }

    static void blend(ObjectFloatMap<UnlockableContent> avg, ObjectFloatMap<UnlockableContent> now){
        ObjectSet<UnlockableContent> keys = new ObjectSet<>();
        for(var e : avg) keys.add(e.key);
        for(var e : now) keys.add(e.key);
        for(UnlockableContent c : keys){
            float v = avg.get(c, 0f) * (1f - smoothing) + now.get(c, 0f) * smoothing;
            //forget what has stopped for good
            if(v < 0.001f) avg.remove(c, 0f);
            else avg.put(c, v);
        }
    }

    /** The recorded history of a material's made (or used) rate, oldest first; empty before the first sample. */
    public static float[] history(UnlockableContent c, boolean madeSide){
        float[] ring = (madeSide ? histMade : histUsed).get(c);
        return ordered(ring);
    }

    public static float[] powerHistory(boolean madeSide){
        return ordered(madeSide ? histPowerMade : histPowerUsed);
    }

    static float[] ordered(@Nullable float[] ring){
        float[] out = new float[filled];
        if(ring == null) return out;
        for(int i = 0; i < filled; i++) out[i] = ring[(head - filled + i + historySize) % historySize];
        return out;
    }

    /** Average net rate (made - used) over the recorded history. */
    public static float averageNet(UnlockableContent c){
        float[] m = history(c, true), u = history(c, false);
        if(m.length == 0) return made.get(c, 0f) - used.get(c, 0f);
        float sum = 0f;
        for(int i = 0; i < m.length; i++) sum += m[i] - u[i];
        return sum / m.length;
    }

    /** Everything that has been made or used since the world loaded. */
    public static Seq<UnlockableContent> tracked(){
        ObjectSet<UnlockableContent> all = new ObjectSet<>();
        for(var e : made) all.add(e.key);
        for(var e : used) all.add(e.key);
        for(var e : totalMade) all.add(e.key);
        for(var e : totalUsed) all.add(e.key);
        return all.toSeq();
    }

    public static void show(){
        if(ProductionDialog.instance != null) ProductionDialog.instance.show();
    }
}
