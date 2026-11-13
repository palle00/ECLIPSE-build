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

/**
 * Splits one input (back) across output branches.
 * <ul>
 * <li><b>Even:</b> branches take turns; a blocked branch is skipped.</li>
 * <li><b>Weighted:</b> ratios such as 2:1 or 3:1:1 (weights 0-3; 0 disables a branch), applied over time with smooth
 * weighted round-robin. Only branches that can accept are counted, so blocked branches don't stall the others.</li>
 * <li><b>Priority:</b> the first branch in the order gets everything it can take; later branches get the rest.</li>
 * </ul>
 * Used by the Controlled Splitter (1x1, branches = left/front/right) and the Line Balancer (2x2, up to 4 branches,
 * one per adjacent building on the output sides). Config: Integer mode, Point2(branch, weight), Point2(10 + rank, branch)
 * for the priority order, Point2[] full state, null resets.
 */
public class ControlledSplitter extends RoutingBlock{
    public static final int even = 0, weighted = 1, priority = 2;
    /** Maximum branches. */
    public int maxBranches = 3;
    /** Line Balancer mode: each adjacent building on the output sides is a branch, and every side but the front also accepts input. */
    public boolean perBuilding = false;

    public ControlledSplitter(String name){
        super(name);
        config(Integer.class, (SplitterBuild b, Integer mode) -> b.mode = Math.max(0, Math.min(mode, 2)));
        config(Point2.class, (SplitterBuild b, Point2 p) -> b.apply(p));
        config(Point2[].class, (SplitterBuild b, Point2[] all) -> {
            b.reset();
            for(Point2 p : all) b.apply(p);
        });
        configClear((SplitterBuild b) -> b.reset());
    }

    public class SplitterBuild extends RoutingBuild{
        public int mode = even;
        public final int[] weights = new int[maxBranches];
        public final int[] order = new int[maxBranches];
        /** Smooth weighted round-robin credits, and last time each branch was served (even mode). */
        final float[] credit = new float[maxBranches];
        final float[] lastServed = new float[maxBranches];
        public final int[] delivered = new int[maxBranches];
        final Seq<Building> branches = new Seq<>();

        {
            reset();
        }

        public void reset(){
            mode = even;
            for(int i = 0; i < maxBranches; i++){
                weights[i] = 1;
                order[i] = i;
                credit[i] = 0f;
            }
        }

        public void apply(Point2 p){
            if(p.x == 9) mode = Math.max(0, Math.min(p.y, 2));
            else if(p.x >= 0 && p.x < maxBranches) weights[p.x] = Math.max(0, Math.min(p.y, 3));
            else if(p.x >= 10 && p.x < 10 + maxBranches && p.y >= 0 && p.y < maxBranches){
                int rank = p.x - 10;
                for(int i = 0; i < maxBranches; i++){
                    if(order[i] == p.y){
                        order[i] = order[rank];
                        order[rank] = p.y;
                        break;
                    }
                }
            }
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();
            branches.clear();
            if(perBuilding){
                for(int side : new int[]{1, 0, 3}) for(Building b : sides[side]) if(branches.size < maxBranches) branches.add(b);
            }else{
                //1x1: one branch per side, kept in fixed positions (left, front, right) even when a side is empty
                for(int side : new int[]{1, 0, 3}) branches.add(sides[side].isEmpty() ? null : sides[side].first());
            }
        }

        @Override
        public boolean acceptsFrom(Building source){
            int r = rel(source);
            return perBuilding ? r >= 1 && !branches.contains(source) : r == 2;
        }

        /** Branch index side for drawing/marking. */
        int sideOf(int branch){
            Building b = branch < branches.size ? branches.get(branch) : null;
            return b == null ? new int[]{1, 0, 3}[Math.min(branch, 2)] : rel(b);
        }

        boolean open(Building source, int i, Item item){
            Building b = i < branches.size ? branches.get(i) : null;
            return b != null && canPass(source, b, item);
        }

        @Override
        public @Nullable Building route(@Nullable Building source, Item item, boolean commit){
            int chosen = -1;
            int n = Math.min(branches.size, maxBranches);
            if(mode == priority){
                for(int k = 0; k < n && chosen < 0; k++) if(open(source, order[k], item)) chosen = order[k];
            }else if(mode == weighted){
                float total = 0f, best = -Float.MAX_VALUE;
                for(int i = 0; i < n; i++){
                    if(weights[i] <= 0 || !open(source, i, item)) continue;
                    total += weights[i];
                    float c = credit[i] + weights[i];
                    if(c > best){
                        best = c;
                        chosen = i;
                    }
                }
                if(chosen >= 0 && commit){
                    for(int i = 0; i < n; i++) if(weights[i] > 0 && open(source, i, item)) credit[i] += weights[i];
                    credit[chosen] -= total;
                }
            }else{
                //even: the open branch served longest ago
                float oldest = Float.MAX_VALUE;
                for(int i = 0; i < n; i++){
                    if(open(source, i, item) && lastServed[i] < oldest){
                        oldest = lastServed[i];
                        chosen = i;
                    }
                }
            }

            if(chosen < 0){
                if(commit) for(int i = 0; i < n; i++) markBlocked(sideOf(i));
                return null;
            }
            if(commit){
                lastServed[chosen] = Time.time + chosen * 0.001f;
                delivered[chosen]++;
            }
            return branches.get(chosen);
        }

        @Override
        public void buildConfiguration(Table table){
            table.background(Styles.black6);
            String[] modes = {"eclipse-split-even", "eclipse-split-weighted", "eclipse-split-priority"};
            table.table(t -> {
                for(int m = 0; m < 3; m++){
                    int mm = m;
                    t.button(Core.bundle.get(modes[m]), Styles.flatTogglet, () -> configure(mm)).height(36f).growX().checked(b -> mode == mm);
                }
            }).growX().row();
            for(int i = 0; i < maxBranches; i++){
                int branch = i;
                table.table(t -> {
                    t.add(Core.bundle.format("eclipse-split-branch", branch + 1)).padRight(6f);
                    for(int w = 0; w <= 3; w++){
                        int ww = w;
                        t.button(String.valueOf(w), Styles.flatTogglet, () -> configure(new Point2(branch, ww))).size(36f).checked(b -> weights[branch] == ww);
                    }
                    t.button(Core.bundle.get("eclipse-split-first"), Styles.flatTogglet, () -> configure(new Point2(10, branch))).height(36f).padLeft(6f).checked(b -> order[0] == branch);
                }).left().row();
            }
            table.button(Core.bundle.get("eclipse-route-reset"), Styles.flatt, () -> configure(null)).height(36f).growX();
        }

        @Override
        public Object config(){
            Point2[] all = new Point2[1 + maxBranches * 2];
            all[0] = new Point2(9, mode);
            for(int i = 0; i < maxBranches; i++){
                all[1 + i] = new Point2(i, weights[i]);
                all[1 + maxBranches + i] = new Point2(10 + i, order[i]);
            }
            return all;
        }

        @Override
        public byte version(){
            return 2;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.b(mode);
            write.b(maxBranches);
            for(int i = 0; i < maxBranches; i++){
                write.b(weights[i]);
                write.b(order[i]);
            }
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 2){
                mode = read.b();
                int n = read.b();
                for(int i = 0; i < n; i++){
                    byte w = read.b(), o = read.b();
                    if(i < maxBranches){
                        weights[i] = w;
                        order[i] = o;
                    }
                }
            }
        }
    }
}
