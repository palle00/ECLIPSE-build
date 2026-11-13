package eclipse.logistics;

import arc.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;

/**
 * Merges up to three input lines (back, left, right) into the front.
 * <ul>
 * <li><b>Priority:</b> items from a side are only taken when no higher-priority side has offered an item recently,
 * so the top line always goes first and lower lines fill the gaps.</li>
 * <li><b>Round-robin:</b> sides take turns; a side that has nothing to offer is skipped.</li>
 * </ul>
 * Items that aren't taken stay on their own belts. Config: Integer mode (0 priority, 1 round-robin),
 * Point2(rank, side) for the priority order, Point2[] full state, null resets.
 */
public class PriorityMerger extends RoutingBlock{
    public static final int[] inputs = {2, 1, 3};
    /** A side counts as waiting if it offered an item within this many ticks. */
    public float offerWindow = 3f;

    public PriorityMerger(String name){
        super(name);
        config(Integer.class, (MergerBuild b, Integer mode) -> b.roundRobin = mode == 1);
        config(Point2.class, (MergerBuild b, Point2 p) -> b.setRank(p.x, p.y));
        config(Point2[].class, (MergerBuild b, Point2[] all) -> {
            b.reset();
            for(Point2 p : all){
                if(p.x == 9) b.roundRobin = p.y == 1;
                else b.setRank(p.x, p.y);
            }
        });
        configClear((MergerBuild b) -> b.reset());
    }

    public class MergerBuild extends RoutingBuild{
        public boolean roundRobin;
        /** Sides in priority order, highest first. */
        public final int[] order = {2, 1, 3};
        public int turn;
        final float[] lastOffer = {-999f, -999f, -999f, -999f};

        public void reset(){
            roundRobin = false;
            order[0] = 2;
            order[1] = 1;
            order[2] = 3;
            turn = 0;
        }

        /** Puts a side at a rank, swapping with whatever was there. */
        public void setRank(int rank, int side){
            if(rank < 0 || rank > 2 || (side != 1 && side != 2 && side != 3)) return;
            for(int i = 0; i < 3; i++){
                if(order[i] == side){
                    order[i] = order[rank];
                    order[rank] = side;
                    return;
                }
            }
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            //record the offer before any budget check, so a busy high-priority line always counts as waiting
            if(!acceptsFrom(source)) return false;
            return super.acceptItem(source, item);
        }

        boolean waiting(int side){
            return Time.time - lastOffer[side] <= offerWindow;
        }

        @Override
        public boolean acceptsFrom(Building source){
            int r = rel(source);
            if(r < 1) return false;
            //remember who is offering, even when refused, so priorities and turns can see waiting lines
            lastOffer[r] = Time.time;
            return true;
        }

        @Override
        public @Nullable Building route(@Nullable Building source, Item item, boolean commit){
            int from = rel(source);
            if(from < 1) return null;

            if(roundRobin){
                //skip turns of sides that have nothing waiting
                for(int k = 0; k < 3 && order[turn] != from && !waiting(order[turn]); k++) turn = (turn + 1) % 3;
                if(order[turn] != from) return null;
            }else{
                for(int side : order){
                    if(side == from) break;
                    if(waiting(side)) return null;
                }
            }

            Building out = firstOnSide(source, 0, item);
            if(out == null){
                if(commit) markBlocked(0);
                return null;
            }
            if(commit && roundRobin) turn = (turn + 1) % 3;
            return out;
        }

        @Override
        public void buildConfiguration(Table table){
            table.background(Styles.black6);
            table.button(Core.bundle.get("eclipse-merge-priority"), Styles.flatTogglet, () -> configure(0)).height(36f).growX().checked(b -> !roundRobin);
            table.button(Core.bundle.get("eclipse-merge-roundrobin"), Styles.flatTogglet, () -> configure(1)).height(36f).growX().checked(b -> roundRobin);
            table.row();
            for(int rank = 0; rank < 3; rank++){
                int r = rank;
                table.add((rank + 1) + ".").padRight(4f);
                table.table(t -> sideButtons(t, inputs, () -> order[r], side -> configure(new Point2(r, side)), false));
                table.row();
            }
            table.button(Core.bundle.get("eclipse-route-reset"), Styles.flatt, () -> configure(null)).colspan(2).height(36f).growX();
        }

        @Override
        public Object config(){
            return new Point2[]{new Point2(9, roundRobin ? 1 : 0), new Point2(0, order[0]), new Point2(1, order[1]), new Point2(2, order[2])};
        }

        @Override
        public byte version(){
            return 2;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.bool(roundRobin);
            for(int s : order) write.b(s);
            write.b(turn);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 2){
                roundRobin = read.bool();
                for(int i = 0; i < 3; i++) order[i] = read.b();
                turn = read.b();
            }
        }
    }
}
