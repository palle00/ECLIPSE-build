package eclipse.logistics;

import arc.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.*;

/**
 * A list-based filter: items that pass (allow list: listed items; deny list: everything else) go straight on,
 * the rest alternate left and right. Unlike the vanilla sorter it filters a whole list of items.
 * Config: Item toggles an item in the list, Boolean sets deny mode, Integer[] (ids) restores the list, null resets.
 */
public class AdvancedItemFilter extends RoutingBlock{
    public int maxItems = 6;

    public AdvancedItemFilter(String name){
        super(name);
        config(Item.class, (FilterListBuild b, Item item) -> b.toggle(item));
        config(Boolean.class, (FilterListBuild b, Boolean deny) -> b.deny = deny);
        config(int[].class, (FilterListBuild b, int[] state) -> {
            b.reset();
            if(state.length == 0) return;
            b.deny = state[0] == 1;
            for(int i = 1; i < state.length; i++) b.toggle(content.item(state[i]));
        });
        configClear((FilterListBuild b) -> b.reset());
    }

    public class FilterListBuild extends RoutingBuild{
        public final OrderedSet<Item> list = new OrderedSet<>();
        public boolean deny;
        int sideTurn;

        public void reset(){
            list.clear();
            deny = false;
        }

        public void toggle(@Nullable Item item){
            if(item == null) return;
            if(!list.remove(item) && list.size < maxItems) list.add(item);
        }

        public boolean passes(Item item){
            return list.contains(item) != deny;
        }

        @Override
        public boolean acceptsFrom(Building source){
            return rel(source) == 2;
        }

        @Override
        public @Nullable Building route(@Nullable Building source, Item item, boolean commit){
            if(passes(item)){
                Building out = firstOnSide(source, 0, item);
                if(out == null && commit) markBlocked(0);
                return out;
            }
            for(int k = 0; k < 2; k++){
                int side = (sideTurn + k) % 2 == 0 ? 1 : 3;
                Building out = firstOnSide(source, side, item);
                if(out != null){
                    if(commit) sideTurn = (sideTurn + k + 1) % 2;
                    return out;
                }
            }
            if(commit){
                markBlocked(1);
                markBlocked(3);
            }
            return null;
        }

        @Override
        public void draw(){
            super.draw();
            if(!list.isEmpty()) drawItemOnSide(list.first(), 0);
        }

        @Override
        public void buildConfiguration(Table table){
            table.background(Styles.black6);
            table.button(Core.bundle.get("eclipse-filter-allow"), Styles.flatTogglet, () -> configure(false)).height(36f).growX().checked(b -> !deny);
            table.button(Core.bundle.get("eclipse-filter-deny"), Styles.flatTogglet, () -> configure(true)).height(36f).growX().checked(b -> deny);
            table.row();
            table.table(t -> {
                int i = 0;
                for(Item item : content.items()){
                    t.button(b -> b.image(item.uiIcon).size(24f), Styles.clearTogglei, () -> configure(item)).size(36f).checked(b -> list.contains(item));
                    if(++i % 6 == 0) t.row();
                }
            }).colspan(2).row();
            table.button(Core.bundle.get("eclipse-route-reset"), Styles.flatt, () -> configure(null)).colspan(2).height(36f).growX();
        }

        @Override
        public Object config(){
            int[] state = new int[list.size + 1];
            state[0] = deny ? 1 : 0;
            int i = 1;
            for(Item item : list) state[i++] = item.id;
            return state;
        }

        @Override
        public byte version(){
            return 2;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.bool(deny);
            write.b(list.size);
            for(Item item : list) write.s(item.id);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 2){
                list.clear();
                deny = read.bool();
                int n = read.b();
                for(int i = 0; i < n; i++) list.add(content.item(read.s()));
            }
        }
    }
}
