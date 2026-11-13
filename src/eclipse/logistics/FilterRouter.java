package eclipse.logistics;

import arc.*;
import arc.math.geom.*;
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
 * Routes up to {@link #slots} configured item types to chosen output sides; everything else goes to a fallback side
 * or is held on the input belt. Input is the back side. Used by the Three-Way Item Sorter (3 rules, 1x1) and the
 * Distribution Hub (4 rules, 2x2). A configured item whose side is blocked is held, never redirected or lost.
 * <p>
 * Config: Point2(slot, itemId or -1) sets a rule's item, Point2(10 + slot, side) its side, Point2(20, side or -1)
 * the fallback; Point2[] restores the whole configuration (copy/paste, schematics); null resets.
 */
public class FilterRouter extends RoutingBlock{
    public int slots = 3;
    /** Output sides: left, front, right. */
    public static final int[] outputs = {1, 0, 3};

    public FilterRouter(String name){
        super(name);
        config(Point2.class, (FilterBuild b, Point2 p) -> b.apply(p));
        config(Point2[].class, (FilterBuild b, Point2[] all) -> {
            b.reset();
            for(Point2 p : all) b.apply(p);
        });
        configClear((FilterBuild b) -> b.reset());
    }

    public class FilterBuild extends RoutingBuild{
        public final Item[] ruleItems = new Item[slots];
        public final int[] ruleSides = new int[slots];
        /** -1 = hold unconfigured items. */
        public int fallback = -1;
        /** Rule being edited in the configuration UI (client only). */
        int editing = -1;
        /** Round-robin pointer per side, for sides with more than one neighbour (2x2 hub). */
        final int[] sidePointer = new int[4];

        {
            reset();
        }

        public void reset(){
            for(int i = 0; i < slots; i++){
                ruleItems[i] = null;
                ruleSides[i] = outputs[i % outputs.length];
            }
            fallback = -1;
        }

        public void apply(Point2 p){
            if(p.x >= 0 && p.x < slots){
                ruleItems[p.x] = p.y < 0 ? null : content.item(p.y);
            }else if(p.x >= 10 && p.x < 10 + slots && validOutput(p.y)){
                ruleSides[p.x - 10] = p.y;
            }else if(p.x == 20 && (p.y == -1 || validOutput(p.y))){
                fallback = p.y;
            }
        }

        boolean validOutput(int side){
            return side == 0 || side == 1 || side == 3;
        }

        @Override
        public boolean acceptsFrom(Building source){
            return rel(source) == 2;
        }

        @Override
        public @Nullable Building route(@Nullable Building source, Item item, boolean commit){
            int side = fallback;
            for(int i = 0; i < slots; i++){
                if(ruleItems[i] == item){
                    side = ruleSides[i];
                    break;
                }
            }
            if(side < 0) return null;

            Seq<Building> candidates = sides[side];
            int n = candidates.size;
            for(int k = 0; k < n; k++){
                int index = (sidePointer[side] + k) % n;
                Building b = candidates.get(index);
                if(canPass(source, b, item)){
                    if(commit) sidePointer[side] = (index + 1) % n;
                    return b;
                }
            }
            if(commit) markBlocked(side);
            return null;
        }

        @Override
        public void draw(){
            super.draw();
            for(int i = 0; i < slots; i++){
                if(ruleItems[i] != null) drawItemOnSide(ruleItems[i], ruleSides[i]);
            }
        }

        @Override
        public void buildConfiguration(Table table){
            table.background(Styles.black6);
            for(int i = 0; i < slots; i++){
                int slot = i;
                table.button(b -> b.image(() -> ruleItems[slot] == null ? Icon.cancel.getRegion() : ruleItems[slot].uiIcon).size(24f), Styles.clearTogglei, () -> {
                    editing = editing == slot ? -1 : slot;
                    rebuild(table);
                }).size(40f).checked(b -> editing == slot);
                table.table(t -> sideButtons(t, outputs, () -> ruleSides[slot], side -> configure(new Point2(10 + slot, side)), false));
                table.row();
            }
            table.add(Core.bundle.get("eclipse-route-other")).padRight(4f);
            table.table(t -> sideButtons(t, outputs, () -> fallback, side -> configure(new Point2(20, side)), true));
            table.row();
            table.button(Core.bundle.get("eclipse-route-reset"), Styles.flatt, () -> configure(null)).colspan(2).height(36f).growX();
            if(editing >= 0){
                table.row();
                table.table(t -> ItemSelection.buildTable(t, content.items(), () -> ruleItems[editing],
                    item -> configure(new Point2(editing, item == null ? -1 : item.id)), 4)).colspan(2);
            }
        }

        void rebuild(Table table){
            table.clearChildren();
            buildConfiguration(table);
        }

        @Override
        public Object config(){
            Point2[] all = new Point2[slots * 2 + 1];
            for(int i = 0; i < slots; i++){
                all[i] = new Point2(i, ruleItems[i] == null ? -1 : ruleItems[i].id);
                all[slots + i] = new Point2(10 + i, ruleSides[i]);
            }
            all[slots * 2] = new Point2(20, fallback);
            return all;
        }

        @Override
        public byte version(){
            return 2;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.b(slots);
            for(int i = 0; i < slots; i++){
                write.s(ruleItems[i] == null ? -1 : ruleItems[i].id);
                write.b(ruleSides[i]);
            }
            write.b(fallback);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 2){
                int n = read.b();
                for(int i = 0; i < n; i++){
                    short id = read.s();
                    byte side = read.b();
                    if(i < slots){
                        ruleItems[i] = id < 0 ? null : content.item(id);
                        ruleSides[i] = side;
                    }
                }
                fallback = read.b();
            }
        }
    }
}
