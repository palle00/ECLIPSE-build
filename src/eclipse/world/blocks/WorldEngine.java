package eclipse.world.blocks;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.core.*;
import mindustry.world.blocks.power.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * The World Engine: ECLIPSE's capstone mega project. After the frame is placed it is completed in {@link #stages},
 * each needing large deliveries from a different production branch (belts, pipes or Logistics Drones). It only accepts
 * what the current stage still needs. Once every stage is complete it comes online:
 * <ul>
 * <li>a planetary power source ({@link #powerProduction});</li>
 * <li>a built-in energy store of {@link #storeCapacity}: it keeps back what its grid doesn't use, and adds it on top
 * when the grid runs short (the grid's own batteries charge once the store is full);</li>
 * <li>a planetary uplink: it also produces {@link #generationBonus} of everything the team's other generators on the map
 * produce (one engine per team gives the bonus; the power comes out of the engine, so it reaches the grids linked to it);</li>
 * <li>an overdrive field: buildings within {@link #overdriveRange} work {@link #overdriveSpeed} times as fast, like an
 * overdrive projector (the strongest boost applies; boosts never stack).</li>
 * </ul>
 */
public class WorldEngine extends PowerGenerator{
    public static final Stat stagesStat = new Stat("eclipse-world-engine-stages", StatCat.function);

    public static class Stage{
        public final String key;
        public final ItemStack[] items;
        public final LiquidStack[] liquids;

        public Stage(String key, ItemStack[] items, LiquidStack... liquids){
            this.key = key;
            this.items = items;
            this.liquids = liquids;
        }
    }

    public Stage[] stages = {};
    public float storeCapacity = 5_000_000f;
    public float generationBonus = 0.15f;
    public float overdriveRange = 40f * 8f, overdriveSpeed = 1.5f;
    public static final Stat storeStat = new Stat("eclipse-engine-store", StatCat.power), bonusStat = new Stat("eclipse-engine-bonus", StatCat.power),
        overdriveStat = new Stat("eclipse-engine-overdrive", StatCat.function);
    public Color glowColor = Color.valueOf("ffb648");
    public TextureRegion glowRegion;
    /** "-stage1".."-stage5": what each finished stage adds to the frame (foundation braces, energy coils, chemistry tanks,
     * control consoles, confinement magnets); the last stage, activation, lights the eclipse itself. */
    public TextureRegion[] stageRegions = new TextureRegion[0];
    /** Where each stage's work goes, in world units from the centre (x, y, width, height); drawn as a pulsing marker. */
    static final float[][][] stageMarks = {
        {{-15f, 15f, 6f, 6f}, {15f, 15f, 6f, 6f}, {-15f, -15f, 6f, 6f}, {15f, -15f, 6f, 6f}},
        {{-19f, 0f, 7f, 18f}, {19f, 0f, 7f, 18f}},
        {{0f, -19f, 18f, 7f}},
        {{0f, 19f, 18f, 7f}},
        {},
        {}
    };

    public WorldEngine(String name){
        super(name);
        hasItems = true;
        hasLiquids = true;
        configurable = true;
        sync = true;
        itemCapacity = 0;
        liquidCapacity = 1000f;
    }

    @Override
    public void load(){
        super.load();
        glowRegion = Core.atlas.find(name + "-glow");
        stageRegions = new TextureRegion[5];
        for(int i = 0; i < stageRegions.length; i++) stageRegions[i] = Core.atlas.find(name + "-stage" + (i + 1));
    }

    /** The build menu shows the finished engine. */
    @Override
    public TextureRegion[] icons(){
        arc.struct.Seq<TextureRegion> out = arc.struct.Seq.with(region);
        for(TextureRegion r : stageRegions) if(r != null && r.found()) out.add(r);
        return out.toArray(TextureRegion.class);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(stagesStat, stages.length);
        stats.add(storeStat, Core.bundle.format("stat.eclipse-engine-store-value", UI.formatAmount((long)storeCapacity)));
        stats.add(bonusStat, Core.bundle.format("stat.eclipse-engine-bonus-value", Math.round(generationBonus * 100)));
        stats.add(overdriveStat, Core.bundle.format("stat.eclipse-engine-overdrive-value", Math.round((overdriveSpeed - 1f) * 100), (int)(overdriveRange / tilesize)));
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, overdriveRange, glowColor);
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("eclipse-engine", (EngineBuild e) -> new Bar(
            () -> e.complete() ? Core.bundle.get("bar.eclipse-engine-online")
                : Core.bundle.format("bar.eclipse-engine-stage", e.stage + 1, stages.length, Core.bundle.get("eclipse-engine.stage-" + stages[e.stage].key)),
            () -> glowColor,
            e::progress));
        addBar("eclipse-engine-store", (EngineBuild e) -> new Bar(
            () -> Core.bundle.format("bar.eclipse-engine-store", UI.formatAmount((long)e.stored), UI.formatAmount((long)storeCapacity)),
            () -> Pal.powerBar,
            () -> e.stored / storeCapacity));
    }

    public class EngineBuild extends GeneratorBuild{
        public int stage;
        /** Delivered amounts for the current stage: items, then liquids, in the stage's order. */
        public float[] delivered = new float[16];
        /** Energy in the built-in store; power out per tick right now; the uplink bonus per tick (0 unless this is the team's lead engine). */
        public float stored, output, bonus;
        float lastContribution, scanTimer, overdriveTimer;

        public boolean complete(){
            return stage >= stages.length;
        }

        /** Overall progress, 0..1 across all stages. */
        public float progress(){
            if(complete()) return 1f;
            Stage s = stages[stage];
            float need = 0f, have = 0f;
            for(int i = 0; i < s.items.length; i++){ need += s.items[i].amount; have += delivered[i]; }
            for(int i = 0; i < s.liquids.length; i++){ need += s.liquids[i].amount; have += delivered[s.items.length + i]; }
            return (stage + (need <= 0f ? 1f : have / need)) / stages.length;
        }

        int itemIndex(Item item){
            if(complete()) return -1;
            ItemStack[] items = stages[stage].items;
            for(int i = 0; i < items.length; i++) if(items[i].item == item) return i;
            return -1;
        }

        int liquidIndex(Liquid liquid){
            if(complete()) return -1;
            LiquidStack[] liquids = stages[stage].liquids;
            for(int i = 0; i < liquids.length; i++) if(liquids[i].liquid == liquid) return i;
            return -1;
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            int i = itemIndex(item);
            return i >= 0 && delivered[i] < stages[stage].items[i].amount;
        }

        @Override
        public int acceptStack(Item item, int amount, Teamc source){
            int i = itemIndex(item);
            return i < 0 ? 0 : Math.max(0, Math.min(amount, stages[stage].items[i].amount - (int)delivered[i]));
        }

        @Override
        public void handleItem(Building source, Item item){
            int i = itemIndex(item);
            if(i >= 0) delivered[i]++;
            checkStage();
        }

        @Override
        public void handleStack(Item item, int amount, Teamc source){
            int i = itemIndex(item);
            if(i >= 0) delivered[i] += amount;
            checkStage();
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            int i = liquidIndex(liquid);
            return i >= 0 && delivered[stages[stage].items.length + i] < stages[stage].liquids[i].amount;
        }

        @Override
        public void handleLiquid(Building source, Liquid liquid, float amount){
            int i = liquidIndex(liquid);
            if(i >= 0){
                int k = stages[stage].items.length + i;
                delivered[k] = Math.min(stages[stage].liquids[i].amount, delivered[k] + amount);
            }
            checkStage();
        }

        void checkStage(){
            if(complete()) return;
            Stage s = stages[stage];
            for(int i = 0; i < s.items.length; i++) if(delivered[i] < s.items[i].amount) return;
            for(int i = 0; i < s.liquids.length; i++) if(delivered[s.items.length + i] < s.liquids[i].amount - 0.01f) return;
            stage++;
            delivered = new float[16];
            if(!headless){
                Fx.spawnShockwave.at(x, y, block.size * tilesize * 1.5f);
                if(eclipse.ui.EclipseSettings.announce()) ui.showInfoToast(complete() ? Core.bundle.get("eclipse-engine.online") : Core.bundle.format("eclipse-engine.stage-done", stage, stages.length), 4f);
            }
        }

        @Override
        public void updateTile(){
            productionEfficiency = complete() ? 1f : 0f;
            if(!complete()){
                output = lastContribution = 0f;
                return;
            }
            if((scanTimer -= delta()) <= 0f){
                scanTimer = 60f;
                bonus = uplinkBonus();
            }
            updateStore();
            if((overdriveTimer += delta()) >= 60f){
                overdriveTimer = 0f;
                indexer.eachBlock(this, overdriveRange, b -> b != this && b.block.canOverdrive, b -> b.applyBoost(overdriveSpeed, 70f));
            }
        }

        /** What the rest of the team's generators on the map produce, times the bonus; 0 if another engine of the team leads. */
        public float uplinkBonus(){
            float others = 0f;
            for(Building b : team.data().buildings){
                if(b instanceof EngineBuild e){
                    if(e != this && e.complete() && e.id < id) return 0f;
                    continue;
                }
                if(b.block instanceof PowerGenerator && b.enabled) others += b.getPowerProduction();
            }
            return others * generationBonus;
        }

        /** Keeps back what the grid doesn't use (up to the store's room) and adds the store on top when the grid runs short. */
        void updateStore(){
            float full = powerProduction + bonus, dt = delta();
            output = full;
            if(power == null || power.graph == null || dt <= 0.0001f) return;
            //last update's balance, as if this engine had given its full output
            float others = power.graph.getLastPowerProduced() - lastContribution;
            float surplus = others + full * dt - power.graph.getLastPowerNeeded();
            if(surplus > 0f){
                float charge = Math.min(Math.min(surplus, full * dt), storeCapacity - stored);
                stored += charge;
                output = full - charge / dt;
            }else{
                float give = Math.min(-surplus, stored);
                stored -= give;
                output = full + give / dt;
            }
            lastContribution = output * dt;
        }

        @Override
        public float getPowerProduction(){
            return complete() && enabled ? output : 0f;
        }

        @Override
        public void drawSelect(){
            super.drawSelect();
            if(complete()) Drawf.dashCircle(x, y, overdriveRange, glowColor);
        }

        @Override
        public void buildConfiguration(Table table){
            table.background(Styles.black6);
            table.margin(6f);
            table.defaults().left();
            for(int i = 0; i < stages.length; i++){
                int index = i;
                Stage s = stages[i];
                table.add((i < stage ? "[lime]" : i == stage ? "[accent]" : "[lightgray]") + (i + 1) + ". " + Core.bundle.get("eclipse-engine.stage-" + s.key)).row();
                if(i != stage) continue;
                table.table(t -> {
                    t.left().defaults().left().padRight(6f);
                    for(int k = 0; k < s.items.length; k++){
                        int slot = k;
                        t.image(s.items[k].item.uiIcon).size(20f);
                        t.label(() -> (int)delivered[slot] + "/" + s.items[slot].amount).minWidth(80f);
                        if(k % 2 == 1) t.row();
                    }
                    t.row();
                    for(int k = 0; k < s.liquids.length; k++){
                        int slot = s.items.length + k;
                        LiquidStack ls = s.liquids[k];
                        t.image(ls.liquid.uiIcon).size(20f);
                        t.label(() -> (int)delivered[slot] + "/" + (int)ls.amount).minWidth(80f);
                    }
                }).padLeft(12f).row();
            }
            if(complete()) table.add("[accent]" + Core.bundle.get("eclipse-engine.online")).row();
        }

        @Override
        public void draw(){
            super.draw();
            //what the finished stages have added to the frame
            for(int i = 0; i < Math.min(stage, stageRegions.length); i++){
                if(stageRegions[i].found()) Draw.rect(stageRegions[i], x, y);
            }
            if(!complete()){
                drawStageMarker();
                return;
            }
            //online: the eclipse lights up - a pulsing corona, two rings turning opposite ways, a slow outward pulse
            Draw.z(Layer.blockAdditive);
            Draw.blend(Blending.additive);
            if(glowRegion.found()){
                Draw.color(glowColor, 0.6f + Mathf.absin(Time.time, 14f, 0.35f));
                Draw.rect(glowRegion, x, y);
            }
            float t = Time.time;
            Lines.stroke(0.7f);
            Draw.color(glowColor, 0.55f);
            dashedRing(11.4f, t * 0.35f, 10);
            Draw.color(Pal.accent, 0.35f);
            dashedRing(13.2f, -t * 0.25f, 6);
            float pulse = (t % 240f) / 240f;
            Draw.color(glowColor, 0.4f * (1f - pulse));
            Lines.stroke(1.4f * (1f - pulse));
            Lines.circle(x, y, 9f + pulse * 30f);
            Draw.blend();
            Draw.reset();
        }

        /** A ring of n dashes (half of the circle drawn), turned by rotation degrees. */
        void dashedRing(float radius, float rotation, int n){
            float step = 360f / n;
            for(int i = 0; i < n; i++){
                float a = rotation + i * step;
                Lines.arc(x, y, radius, 0.5f / n, a);
            }
        }

        /** Outlines where the current stage's work goes (bays, braces, the magnet ring or the core), pulsing gently. */
        void drawStageMarker(){
            if(stage >= stageMarks.length) return;
            Draw.z(Layer.blockOver);
            Draw.color(Pal.accent, 0.25f + Mathf.absin(Time.time, 10f, 0.35f));
            Lines.stroke(1f);
            for(float[] m : stageMarks[stage]) Lines.rect(x + m[0] - m[2] / 2f, y + m[1] - m[3] / 2f, m[2], m[3]);
            if(stage == 4) Lines.circle(x, y, 10.8f);
            if(stage == 5) Lines.circle(x, y, 8.2f);
            Draw.reset();
        }

        @Override
        public byte version(){
            return 2;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.b(stage);
            write.b(delivered.length);
            for(float f : delivered) write.f(f);
            write.f(stored);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision < 1) return;
            stage = read.b();
            int n = read.b();
            delivered = new float[Math.max(16, n)];
            for(int i = 0; i < n; i++) delivered[i] = read.f();
            if(revision >= 2) stored = read.f();
            //saves from before the Confinement stage (inserted 5th): an engine past Control keeps its progress and skips it
            else if(stage >= 4) stage++;
        }
    }
}
