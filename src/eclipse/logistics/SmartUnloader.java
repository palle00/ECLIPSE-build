package eclipse.logistics;

import arc.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * Unloads up to three selected item types from adjacent inventories that allow unloading, taking turns between them,
 * and passes them to adjacent non-storage buildings (like the vanilla unloader, it never loads into storage).
 * Same speed as the vanilla unloader; its advantage is multi-item filtering. Config: Item toggles a selected item,
 * int[] restores the selection, null clears it.
 */
public class SmartUnloader extends Block{
    public int maxItems = 3;
    public float itemsPerSecond = 11f;

    public SmartUnloader(String name){
        super(name);
        update = true;
        solid = true;
        configurable = true;
        saveConfig = true;
        hasItems = true;
        itemCapacity = 0;
        unloadable = false;
        group = BlockGroup.transportation;
        envEnabled = Env.any;
        config(Item.class, (UnloaderBuild b, Item item) -> b.toggle(item));
        config(int[].class, (UnloaderBuild b, int[] ids) -> {
            b.selected.clear();
            for(int id : ids) b.toggle(content.item(id));
        });
        configClear((UnloaderBuild b) -> b.selected.clear());
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.itemsMoved, itemsPerSecond, StatUnit.itemsSecond);
    }

    public class UnloaderBuild extends Building{
        public final OrderedSet<Item> selected = new OrderedSet<>();
        float timer;
        int turn;

        public void toggle(@Nullable Item item){
            if(item == null) return;
            if(!selected.remove(item) && selected.size < maxItems) selected.add(item);
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return false;
        }

        @Override
        public void updateTile(){
            if(selected.isEmpty()) return;
            timer += delta() * itemsPerSecond / 60f;
            while(timer >= 1f){
                if(!moveOne()) {
                    timer = Math.min(timer, 1f);
                    return;
                }
                timer -= 1f;
            }
        }

        /** Moves one selected item from an unloadable neighbour to an accepting non-storage neighbour. */
        boolean moveOne(){
            Seq<Item> order = selected.orderedItems();
            for(int k = 0; k < order.size; k++){
                Item item = order.get((turn + k) % order.size);
                Building from = null, to = null;
                for(Building b : proximity){
                    if(b.team != team || b.items == null) continue;
                    if(from == null && b.canUnload() && b.items.has(item)) from = b;
                }
                if(from == null) continue;
                for(Building b : proximity){
                    if(b != from && b.team == team && !(b.block instanceof StorageBlock) && b.acceptItem(this, item)){
                        to = b;
                        break;
                    }
                }
                if(to == null) continue;
                from.removeStack(item, 1);
                to.handleItem(this, item);
                turn = (turn + k + 1) % order.size;
                return true;
            }
            return false;
        }

        @Override
        public void buildConfiguration(Table table){
            table.background(Styles.black6);
            table.table(t -> {
                int i = 0;
                for(Item item : content.items()){
                    t.button(b -> b.image(item.uiIcon).size(24f), Styles.clearTogglei, () -> configure(item)).size(36f).checked(b -> selected.contains(item));
                    if(++i % 6 == 0) t.row();
                }
            }).row();
            table.button(Core.bundle.get("eclipse-route-reset"), Styles.flatt, () -> configure(null)).height(36f).growX();
        }

        @Override
        public Object config(){
            int[] ids = new int[selected.size];
            int i = 0;
            for(Item item : selected) ids[i++] = item.id;
            return ids;
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.b(selected.size);
            for(Item item : selected) write.s(item.id);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1){
                selected.clear();
                int n = read.b();
                for(int i = 0; i < n; i++) selected.add(content.item(read.s()));
            }
        }
    }
}
