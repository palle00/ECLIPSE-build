package eclipse.logistics;

import arc.*;
import arc.scene.ui.layout.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * Collects a configured batch of items from the back, then releases the whole batch through the front before
 * collecting again. Items wait inside while collecting; no production bonus. Config: Integer batch size, null resets.
 */
public class BatchGate extends Block{
    public static final int[] sizes = {2, 5, 10, 20};
    public float itemsPerSecond = 30f;

    public BatchGate(String name){
        super(name);
        update = true;
        solid = false;
        underBullets = true;
        rotate = true;
        configurable = true;
        saveConfig = true;
        hasItems = true;
        itemCapacity = sizes[sizes.length - 1];
        unloadable = false;
        group = BlockGroup.transportation;
        envEnabled = Env.any;
        config(Integer.class, (GateBuild b, Integer size) -> b.batch = Math.max(1, Math.min(size, itemCapacity)));
        configClear((GateBuild b) -> b.batch = 10);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.itemsMoved, itemsPerSecond, StatUnit.itemsSecond);
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("eclipse-batch", (GateBuild e) -> new Bar(() -> Core.bundle.format("eclipse-batch-bar", e.items.total(), e.batch),
            () -> e.releasing ? Pal.accent : Pal.items, () -> e.items.total() / (float)e.batch));
    }

    public class GateBuild extends Building{
        public int batch = 10;
        /** True while emptying a completed batch. */
        public boolean releasing;
        float budget;
        public int batchesReleased;

        @Override
        public boolean acceptItem(Building source, Item item){
            return !releasing && source.team == team && relativeTo(source) == (rotation + 2) % 4 && items.total() < batch;
        }

        @Override
        public void handleItem(Building source, Item item){
            super.handleItem(source, item);
            if(items.total() >= batch) releasing = true;
        }

        @Override
        public void updateTile(){
            budget = Math.min(budget + itemsPerSecond / 60f * delta(), Math.max(1f, itemsPerSecond / 60f * 2f));
            //the batch size may have been lowered below what is already inside
            if(!releasing && items.total() >= batch) releasing = true;
            if(!releasing) return;
            Building front = front();
            while(budget >= 1f && items.total() > 0 && front != null && front.team == team){
                Item item = items.first();
                if(!front.acceptItem(this, item)) break;
                items.remove(item, 1);
                front.handleItem(this, item);
                budget -= 1f;
            }
            if(items.total() == 0){
                releasing = false;
                batchesReleased++;
            }
        }

        @Override
        public void buildConfiguration(Table table){
            table.background(Styles.black6);
            for(int size : sizes){
                table.button(String.valueOf(size), Styles.flatTogglet, () -> configure(size)).size(44f, 36f).checked(b -> batch == size);
            }
        }

        @Override
        public Object config(){
            return batch;
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.b(batch);
            write.bool(releasing);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1){
                batch = read.b();
                releasing = read.bool();
            }
        }
    }
}
