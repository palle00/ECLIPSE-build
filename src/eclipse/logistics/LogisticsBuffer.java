package eclipse.logistics;

import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * A small local buffer between uneven production and consumption. Takes items from every side except the front,
 * stores up to {@link #capacity} items in total, and exports through the front at {@link #itemsPerSecond}.
 * <ul>
 * <li><b>Reserve:</b> one item type can keep a minimum amount that is never exported.</li>
 * <li><b>Output filter</b> (industrial buffer only): only export one item type.</li>
 * </ul>
 * Config: Point2(0, itemId or -1) reserve item, Point2(1, amount) reserve amount, Point2(2, itemId or -1) filter,
 * Point2[] full state, null resets.
 */
public class LogisticsBuffer extends Block{
    /** Total items stored, across all types. */
    public int capacity = 60;
    public float itemsPerSecond = 60f;
    public boolean outputFilter = false;
    public static final int[] reserveSteps = {0, 10, 20, 50, 100, 200};

    public LogisticsBuffer(String name){
        super(name);
        update = true;
        solid = true;
        rotate = true;
        configurable = true;
        saveConfig = true;
        hasItems = true;
        unloadable = true;
        group = BlockGroup.transportation;
        envEnabled = Env.any;
        config(Point2.class, (BufferBuild b, Point2 p) -> b.apply(p));
        config(Point2[].class, (BufferBuild b, Point2[] all) -> {
            b.reset();
            for(Point2 p : all) b.apply(p);
        });
        configClear((BufferBuild b) -> b.reset());
    }

    @Override
    public void init(){
        itemCapacity = capacity;
        super.init();
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.itemsMoved, itemsPerSecond, StatUnit.itemsSecond);
    }

    public class BufferBuild extends Building{
        public @Nullable Item reserveItem, filter;
        public int reserveAmount;
        float budget;
        int dumpIndex;

        public void reset(){
            reserveItem = null;
            reserveAmount = 0;
            filter = null;
        }

        public void apply(Point2 p){
            if(p.x == 0) reserveItem = p.y < 0 ? null : content.item(p.y);
            else if(p.x == 1) reserveAmount = Math.max(0, Math.min(p.y, capacity));
            else if(p.x == 2 && outputFilter) filter = p.y < 0 ? null : content.item(p.y);
        }

        /** How many of this item may leave the buffer right now. */
        public int exportable(Item item){
            if(filter != null && item != filter) return 0;
            int have = items.get(item);
            return item == reserveItem ? Math.max(have - reserveAmount, 0) : have;
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return source.team == team && relativeTo(source) != rotation && items.total() < capacity;
        }

        @Override
        public void updateTile(){
            budget = Math.min(budget + itemsPerSecond / 60f * delta() * efficiency, Math.max(1f, itemsPerSecond / 60f * 2f));
            while(budget >= 1f && exportOne()) budget -= 1f;
        }

        /** Sends one exportable item to a building on the front side; items rotate so no type starves. */
        boolean exportOne(){
            int n = content.items().size;
            for(int k = 0; k < n; k++){
                Item item = content.item((dumpIndex + k) % n);
                if(exportable(item) <= 0) continue;
                for(Building b : proximity){
                    if(relativeTo(b) == rotation && b.team == team && b.acceptItem(this, item)){
                        items.remove(item, 1);
                        b.handleItem(this, item);
                        dumpIndex = (item.id + 1) % n;
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void buildConfiguration(Table table){
            table.background(Styles.black6);
            table.add(Core.bundle.get("eclipse-buffer-reserve")).left().row();
            table.table(t -> ItemSelection.buildTable(t, content.items(), () -> reserveItem, item -> configure(new Point2(0, item == null ? -1 : item.id)), 4)).row();
            table.table(t -> {
                for(int amount : reserveSteps){
                    t.button(String.valueOf(amount), Styles.flatTogglet, () -> configure(new Point2(1, amount))).size(44f, 32f).checked(b -> reserveAmount == amount);
                }
            }).row();
            if(outputFilter){
                table.add(Core.bundle.get("eclipse-buffer-filter")).left().row();
                table.table(t -> ItemSelection.buildTable(t, content.items(), () -> filter, item -> configure(new Point2(2, item == null ? -1 : item.id)), 4)).row();
            }
            table.button(Core.bundle.get("eclipse-route-reset"), Styles.flatt, () -> configure(null)).height(36f).growX();
        }

        @Override
        public Object config(){
            return new Point2[]{new Point2(0, reserveItem == null ? -1 : reserveItem.id), new Point2(1, reserveAmount), new Point2(2, filter == null ? -1 : filter.id)};
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.s(reserveItem == null ? -1 : reserveItem.id);
            write.i(reserveAmount);
            write.s(filter == null ? -1 : filter.id);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1){
                short r = read.s();
                reserveItem = r < 0 ? null : content.item(r);
                reserveAmount = read.i();
                short f = read.s();
                filter = f < 0 ? null : content.item(f);
            }
        }
    }
}
