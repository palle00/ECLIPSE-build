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
import mindustry.world.blocks.power.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * The World Engine: ECLIPSE's capstone mega project. After the frame is placed it is completed in {@link #stages},
 * each needing large deliveries from a different production branch (belts, pipes or Logistics Drones). It only accepts
 * what the current stage still needs. Once every stage is complete it comes online as a planetary power source.
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
    public Color glowColor = Color.valueOf("ffb648");
    public TextureRegion glowRegion;

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
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(stagesStat, stages.length);
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("eclipse-engine", (EngineBuild e) -> new Bar(
            () -> e.complete() ? Core.bundle.get("bar.eclipse-engine-online")
                : Core.bundle.format("bar.eclipse-engine-stage", e.stage + 1, stages.length, Core.bundle.get("eclipse-engine.stage-" + stages[e.stage].key)),
            () -> glowColor,
            e::progress));
    }

    public class EngineBuild extends GeneratorBuild{
        public int stage;
        /** Delivered amounts for the current stage: items, then liquids, in the stage's order. */
        public float[] delivered = new float[16];

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
                ui.showInfoToast(complete() ? Core.bundle.get("eclipse-engine.online") : Core.bundle.format("eclipse-engine.stage-done", stage, stages.length), 4f);
            }
        }

        @Override
        public void updateTile(){
            productionEfficiency = complete() ? 1f : 0f;
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
            if(!complete() || !glowRegion.found()) return;
            Draw.z(Layer.blockAdditive);
            Draw.blend(Blending.additive);
            Draw.color(glowColor, 0.55f + Mathf.absin(Time.time, 12f, 0.35f));
            Draw.rect(glowRegion, x, y);
            Draw.blend();
            Draw.reset();
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.b(stage);
            write.b(delivered.length);
            for(float f : delivered) write.f(f);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision < 1) return;
            stage = read.b();
            int n = read.b();
            delivered = new float[Math.max(16, n)];
            for(int i = 0; i < n; i++) delivered[i] = read.f();
        }
    }
}
