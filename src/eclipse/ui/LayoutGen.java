package eclipse.ui;

import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import eclipse.content.blocks.*;
import eclipse.type.*;
import eclipse.ui.BuildLayout.*;
import eclipse.world.blocks.*;
import eclipse.world.blocks.RecipeCrafter.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.heat.*;
import mindustry.world.blocks.production.*;
import mindustry.world.consumers.*;

/**
 * Builds the wiki's build examples from the game's own recipes, so they follow every balance change:
 * - {@link #minimal}: one machine with each input arriving on its own belt or pipe, outputs leaving on theirs (fixed
 *   liquid sides respected), the heaters its heat needs facing it, and a power node; acids get polymer conduits.
 * - {@link #hotLine}: a furnace recipe whose product comes out hot, as a full line: as many furnaces as one Vacuum
 *   Freezer keeps up with, fed by one ore line with a router over each furnace (what no furnace takes flows on, so a
 *   mixed line never jams), their output collected on one belt into the freezer, and cryofluid piped in.
 * The tests place every layout's schematic in a world, feed it and check it reaches the rate it states.
 */
public class LayoutGen{
    /** Belts and pipes between the machine and the edge of the minimal picture. */
    static final int margin = 3;

    /** What one machine does with one recipe, read from a universal machine's recipe or a fixed crafter's consumers. */
    public static class Job{
        public Block block;
        public @Nullable Recipe recipe;
        public final Seq<ItemStack> in = new Seq<>(), out = new Seq<>();
        public final Seq<LiquidStack> liqIn = new Seq<>(), liqOut = new Seq<>();
        public int[] sides;
        public float seconds, heat, powerUse;
        public @Nullable Item catalyst;
        public int catalystCrafts = 10;
        public boolean vented;
        public @Nullable Object config;

        public @Nullable UnlockableContent product(){
            if(out.any()) return out.first().item;
            if(liqOut.any()) return liqOut.first().liquid;
            return null;
        }

        /** Per second of the main product at full speed (a separator makes one random result per craft, by weight). */
        public float productRate(){
            if(block instanceof Separator s){
                int sum = 0;
                for(ItemStack st : s.results) sum += st.amount;
                return sum <= 0 ? 0f : s.results[0].amount / (float)sum / seconds;
            }
            if(out.any()) return out.first().amount / seconds;
            if(liqOut.any()) return liqOut.first().amount * 60f;
            return 0f;
        }

        public int itemsPerCraft(){
            int n = 0;
            for(ItemStack s : in) n += s.amount;
            return n;
        }
    }

    public static @Nullable Job job(Block b, @Nullable Recipe r){
        Job j = new Job();
        j.block = b;
        j.recipe = r;
        if(r != null){
            j.in.addAll(r.inputs);
            j.out.addAll(r.outputs);
            j.liqIn.addAll(r.liquidInputs);
            j.liqOut.addAll(r.liquidOutputs);
            j.sides = r.liquidSides;
            j.seconds = r.time / 60f;
            j.heat = r.heat;
            j.powerUse = r.power();
            j.catalyst = r.catalyst;
            j.catalystCrafts = r.craftsPerCatalyst;
            j.vented = r.vented;
            j.config = RecipeCrafter.pack(r, 0, (RecipeCrafter)b);
            return j;
        }
        if(!(b instanceof GenericCrafter) && !(b instanceof Separator)) return null;
        for(Consume c : b.consumers){
            if(c.optional) continue;
            if(c instanceof ConsumeItems ci) j.in.addAll(ci.items);
            else if(c instanceof ConsumeLiquid cl) j.liqIn.add(new LiquidStack(cl.liquid, cl.amount));
            else if(c instanceof ConsumeLiquids cls) j.liqIn.addAll(cls.liquids);
            else if(c instanceof ConsumeItemFilter || c instanceof ConsumeLiquidFilter || c instanceof ConsumeItemDynamic) return null; //fuel of any kind: no fixed setup
        }
        if(b.consPower != null) j.powerUse = b.consPower.usage;
        if(b instanceof GenericCrafter g){
            if(g.outputItems != null) j.out.addAll(g.outputItems);
            if(g.outputLiquids != null) j.liqOut.addAll(g.outputLiquids);
            j.sides = g.liquidOutputDirections != null && g.liquidOutputDirections.length > 0 && g.liquidOutputDirections[0] >= 0 ? g.liquidOutputDirections : new int[0];
            j.seconds = g.craftTime / 60f;
        }else{
            Separator s = (Separator)b;
            for(ItemStack st : s.results) j.out.add(new ItemStack(st.item, 1));
            j.seconds = s.craftTime / 60f;
            j.sides = new int[0];
        }
        if(b instanceof HeatCrafter hc) j.heat = hc.heatRequirement;
        if(b instanceof ChemicalReactor cr && cr.catalyst != null){
            j.catalyst = cr.catalyst;
            j.catalystCrafts = cr.craftsPerCatalyst;
        }
        if(j.in.isEmpty() && j.liqIn.isEmpty() && j.out.isEmpty() && j.liqOut.isEmpty()) return null;
        if(j.seconds <= 0f) j.seconds = 1f;
        return j;
    }

    // ---------------- helpers ----------------

    static Block heater(){
        return PowerBlocks.electricHeater;
    }

    static float heaterOutput(){
        return ((HeatProducer)heater()).heatOutput;
    }

    /** Titanium belts, plain ones for the first tier when they keep up. */
    static Block belt(@Nullable Recipe r, float perSecond){
        return r != null && r.tier <= 1 && perSecond <= 4.5f ? Blocks.conveyor : Blocks.titaniumConveyor;
    }

    static Block pipe(Liquid l){
        return l instanceof CorrosiveLiquid ? TransportBlocks.polymerConduit : Blocks.conduit;
    }

    static String n(float v){
        return Strings.autoFixed(Math.round(v * 100f) / 100f, 2);
    }

    static String stacks(Seq<ItemStack> s){
        StringBuilder b = new StringBuilder();
        for(int i = 0; i < s.size; i++){
            if(i > 0) b.append(" + ");
            b.append(s.get(i).amount).append(' ').append(s.get(i).item.localizedName);
        }
        return b.toString();
    }

    /** "Nickel 1.33/s, Chromium 0.67/s" for items at these per-craft amounts. */
    static String rates(Seq<ItemStack> s, float seconds, float machines){
        StringBuilder b = new StringBuilder();
        for(int i = 0; i < s.size; i++){
            if(i > 0) b.append(", ");
            b.append(s.get(i).item.localizedName).append(' ').append(n(s.get(i).amount / seconds * machines)).append("/s");
        }
        return b.toString();
    }

    static float total(Seq<ItemStack> s, float seconds){
        float t = 0f;
        for(ItemStack st : s) t += st.amount / seconds;
        return t;
    }

    /**
     * One craft's items in order (2 Ni, 1 Cr, 1 Co -> Ni Ni Cr Co), the catalyst once per its life of crafts. The catalyst
     * comes first: the machine can't start without it, and a belt full of other inputs would never let it through.
     */
    static Item[] cycle(Job j){
        Seq<Item> one = new Seq<>();
        for(ItemStack s : j.in) for(int i = 0; i < s.amount; i++) one.add(s.item);
        if(j.catalyst == null) return one.toArray(Item.class);
        Seq<Item> all = new Seq<>();
        all.add(j.catalyst);
        for(int c = 0; c < j.catalystCrafts; c++) all.addAll(one);
        return all.toArray(Item.class);
    }

    static float cycleInterval(Job j, Item[] cycle){
        //items per second of the whole cycle: one craft's items per craft time, plus the catalyst's share
        float perSecond = j.itemsPerCraft() / j.seconds + (j.catalyst == null ? 0f : 1f / (j.seconds * j.catalystCrafts));
        return perSecond <= 0f ? 1f : 1f / perSecond;
    }

    static String machineInfo(Job j){
        String title = j.block.localizedName + (j.recipe != null ? " · " + j.recipe.output().localizedName : "");
        StringBuilder s = new StringBuilder(title);
        String ins = stacks(j.in), outs = stacks(j.out);
        for(LiquidStack l : j.liqIn) ins += (ins.isEmpty() ? "" : " + ") + l.liquid.localizedName;
        for(LiquidStack l : j.liqOut) outs += (outs.isEmpty() ? "" : " + ") + l.liquid.localizedName;
        s.append('\n').append(Core.bundle.format("eclipse-build.info.recipe", ins.isEmpty() ? "-" : ins, outs.isEmpty() ? "-" : outs, n(j.seconds)));
        if(j.heat > 0f) s.append('\n').append(Core.bundle.format("eclipse-build.info.heat", n(j.heat)));
        if(j.powerUse > 0f) s.append('\n').append(Core.bundle.format("eclipse-build.info.power", n(j.powerUse * 60f)) + (j.recipe != null ? " (" + Core.bundle.format("eclipse-build.tier", RecipeCrafter.roman(j.recipe.tier)) + ")" : ""));
        return s.toString();
    }

    static String heaterInfo(){
        Block h = heater();
        return Core.bundle.format("eclipse-build.info.heater", h.localizedName, n(heaterOutput()), n(h.consPower.usage * 60f));
    }

    static String beltInfo(Block belt, String carries){
        return belt.localizedName + "\n" + carries;
    }

    // ---------------- minimal: one machine ----------------

    /** Grid tile of slot k on a side, d tiles out from the machine (1 = touching). Sides: 0 right, 1 up, 2 left, 3 down. */
    static int[] tile(int mx, int my, int n, int side, int slot, int d){
        return switch(side){
            case 0 -> new int[]{mx + n - 1 + d, my + slot};
            case 1 -> new int[]{mx + slot, my + n - 1 + d};
            case 2 -> new int[]{mx - d, my + slot};
            default -> new int[]{mx + slot, my - d};
        };
    }

    static int free(boolean[] side, int mid){
        for(int d = 0; d < side.length; d++){
            if(mid + d < side.length && !side[mid + d]) return mid + d;
            if(mid - d >= 0 && !side[mid - d]) return mid - d;
        }
        return -1;
    }

    static int pick(boolean[][] used, int... order){
        for(int side : order) if(free(used[side], used[side].length / 2) >= 0) return side;
        return -1;
    }

    /** A planned belt or pipe run on one side of the machine. */
    static class Run{
        int side, slot;
        boolean out;
        @Nullable Liquid liquid;
        float liquidPerSecond;
    }

    public static @Nullable BuildLayout minimal(Block b, @Nullable Recipe r){
        Job j = job(b, r);
        if(j == null) return null;
        int n = b.size, rotation = b.rotate ? 1 : 0;
        boolean[][] used = new boolean[4][n];
        //a heater only gives its full heat when its whole side touches the machine: with heat, belts and pipes keep to
        //the edge of each side, so a 2x2 heater fits beside them
        int mid = j.heat > 0f ? 0 : n / 2;
        Seq<Run> runs = new Seq<>();
        Run itemIn = null, itemOut = null;

        //fixed liquid output sides first (relative to the machine's facing; it faces up here), then belts, then the rest
        Seq<LiquidStack> anySide = new Seq<>();
        for(int i = 0; i < j.liqOut.size; i++){
            int dir = j.sides != null && i < j.sides.length ? j.sides[i] : -1;
            if(dir < 0){
                if(!(j.vented && r != null)) anySide.add(j.liqOut.get(i));
                continue;
            }
            int side = b.rotate ? (dir + rotation) % 4 : dir;
            int slot = free(used[side], mid);
            if(slot < 0) return null;
            used[side][slot] = true;
            Run run = new Run();
            run.side = side;
            run.slot = slot;
            run.out = true;
            run.liquid = j.liqOut.get(i).liquid;
            run.liquidPerSecond = j.liqOut.get(i).amount * 60f;
            runs.add(run);
        }
        if(j.in.any() || j.catalyst != null){
            int slot = free(used[2], mid);
            if(slot < 0) return null;
            used[2][slot] = true;
            itemIn = new Run();
            itemIn.side = 2;
            itemIn.slot = slot;
            runs.add(itemIn);
        }
        if(j.out.any()){
            int side = pick(used, 0, 3, 1);
            if(side < 0) return null;
            itemOut = new Run();
            itemOut.side = side;
            itemOut.slot = free(used[side], mid);
            itemOut.out = true;
            used[side][itemOut.slot] = true;
            runs.add(itemOut);
        }
        for(LiquidStack l : anySide){
            int side = pick(used, 0, 3, 1, 2);
            if(side < 0) return null;
            Run run = new Run();
            run.side = side;
            run.slot = free(used[side], mid);
            run.out = true;
            run.liquid = l.liquid;
            run.liquidPerSecond = l.amount * 60f;
            used[side][run.slot] = true;
            runs.add(run);
        }
        for(LiquidStack l : j.liqIn){
            int side = b.rotate ? pick(used, 3, 1, 2, 0) : pick(used, 1, 3, 2, 0);
            if(side < 0) return null;
            Run run = new Run();
            run.side = side;
            run.slot = free(used[side], mid);
            run.liquid = l.liquid;
            run.liquidPerSecond = l.amount * 60f;
            used[side][run.slot] = true;
            runs.add(run);
        }

        BuildLayout lay = new BuildLayout();
        lay.title = Core.bundle.get("eclipse-build.minimal");
        lay.recipe = r;
        int mx = margin, my = margin;
        lay.w = n + margin * 2;
        lay.h = n + margin * 2;
        boolean[][] occupied = new boolean[lay.w][lay.h];
        for(int x = mx; x < mx + n; x++) for(int y = my; y < my + n; y++) occupied[x][y] = true;
        Part machine = lay.add(b, mx + (n - 1) / 2, my + (n - 1) / 2, rotation, j.config, 1, machineInfo(j));

        //the runs: belts and pipes from the edge to the machine (inputs) or away from it (outputs)
        for(Run run : runs){
            int toward = (run.side + 2) % 4, dir = run.out ? run.side : toward;
            boolean items = run.liquid == null;
            float itemRate = items ? (run.out ? total(j.out, j.seconds) : 1f / cycleInterval(j, cycle(j))) : 0f;
            Block block = items ? belt(r, itemRate) : pipe(run.liquid);
            String carries = items ? (run.out ? rates(j.out, j.seconds, 1f) : rates(j.in, j.seconds, 1f) + (j.catalyst != null ? ", " + j.catalyst.localizedName : ""))
                : run.liquid.localizedName + " " + n(run.liquidPerSecond) + "/s";
            for(int d = 1; d <= margin; d++){
                int[] t = tile(mx, my, n, run.side, run.slot, d);
                occupied[t[0]][t[1]] = true;
                Part p = lay.add(block, t[0], t[1], dir, null, 3, beltInfo(block, carries));
                p.liquid = run.liquid;
            }
            int[] port = tile(mx, my, n, run.side, run.slot, margin + 1);
            BuildLayout.Port pt = lay.port(items ? (run.out ? BuildLayout.Port.Kind.output : BuildLayout.Port.Kind.items) : (run.out ? BuildLayout.Port.Kind.liquidOutput : BuildLayout.Port.Kind.liquid), port[0], port[1]);
            if(items && !run.out) pt.items = cycle(j);
            pt.liquid = run.liquid;
            //the label sits beside the run's outer end
            int[] end = tile(mx, my, n, run.side, run.slot, margin);
            String text = items ? n(itemRate) + "/s" : run.liquid.localizedName + " " + n(run.liquidPerSecond) + "/s";
            float lx = end[0] + 0.5f, ly = end[1] + 0.5f;
            if(run.side % 2 == 1){ lx += 0.65f; ly += run.side == 1 ? -0.2f : 0.2f; }else ly += 0.75f;
            lay.label(lx, ly, text, 0, run.side == 2 ? 1 : run.side == 0 ? -1 : 1, 3);
            if(items){
                Flow f = new Flow();
                f.items = run.out ? outCycle(j) : cycle(j);
                f.interval = run.out ? j.seconds / f.items.length : cycleInterval(j, f.items);
                f.step = 3;
                int[] a = tile(mx, my, n, run.side, run.slot, margin), z = tile(mx, my, n, run.side, run.slot, 1);
                float ox = Geometry.d4x(run.side) * 0.5f, oy = Geometry.d4y(run.side) * 0.5f;
                //outer edge to the machine's edge, or the machine's edge out past the picture
                float ax = a[0] + 0.5f + ox, ay = a[1] + 0.5f + oy, zx = z[0] + 0.5f - ox, zy = z[1] + 0.5f - oy;
                f.path = run.out ? new float[]{zx, zy, ax + ox * 2f, ay + oy * 2f} : new float[]{ax, ay, zx, zy};
                lay.flows.add(f);
            }
        }

        //heaters: as many as the heat needs, each facing the machine
        Seq<Part> heaters = new Seq<>();
        int need = j.heat > 0f ? Mathf.ceil(j.heat / heaterOutput() - 0.001f) : 0;
        if(need > 0){
            outer:
            for(int side : new int[]{3, 1, 2, 0}){
                for(int off = 0; off + 2 <= n; off++){
                    if(heaters.size >= need) break outer;
                    //the heater's lower left tile, touching the machine's side over at least one tile
                    int hx, hy;
                    if(side % 2 == 1){
                        hx = mx + off;
                        hy = side == 1 ? my + n : my - 2;
                    }else{
                        hy = my + off;
                        hx = side == 0 ? mx + n : mx - 2;
                    }
                    if(!freeRect(occupied, hx, hy, 2, 2)) continue;
                    for(int x = hx; x < hx + 2; x++) for(int y = hy; y < hy + 2; y++) occupied[x][y] = true;
                    heaters.add(lay.add(heater(), hx, hy, (side + 2) % 4, null, 1, heaterInfo()));
                }
            }
            if(heaters.size < need) return null;
            float got = heaters.size * heaterOutput();
            lay.label(mx + n / 2f, my + n + 0.05f, Core.bundle.format("eclipse-build.label.heat", n(got), n(j.heat)), 1, 0, 1);
        }

        //power: a node in a free tile near a corner, linking the machine and its heaters
        float power = j.powerUse * 60f + heaters.size * heater().consPower.usage * 60f;
        if(power > 0f){
            Part node = null;
            int[][] spots = {{mx - 2, my + n + 1}, {mx + n + 1, my + n + 1}, {mx - 2, my - 2}, {mx + n + 1, my - 2}, {mx - 1, my + n + 1}, {mx + n, my + n + 1}, {mx - 1, my - 2}, {mx + n, my - 2}};
            for(int[] s : spots){
                if(s[0] < 0 || s[1] < 0 || s[0] >= lay.w || s[1] >= lay.h || occupied[s[0]][s[1]]) continue;
                occupied[s[0]][s[1]] = true;
                node = lay.add(Blocks.powerNode, s[0], s[1], 0, null, 2, Core.bundle.format("eclipse-build.info.node", Blocks.powerNode.localizedName, n(power)));
                break;
            }
            if(node == null) return null;
            Seq<Part> linked = new Seq<>();
            linked.add(machine);
            linked.addAll(heaters);
            node.config = BuildLayout.links(node, linked);
            //the grid joins the node from the nearest free neighbour
            for(Point2 d : Geometry.d4){
                int px = node.x + d.x, py = node.y + d.y;
                if(px >= 0 && py >= 0 && px < lay.w && py < lay.h && occupied[px][py]) continue;
                lay.port(BuildLayout.Port.Kind.power, px, py);
                break;
            }
            lay.label(node.x + 0.5f, node.y + (node.y > my ? 1.25f : -0.3f), Core.bundle.format("eclipse-build.label.power", n(power)), 2, 0, 2);
        }

        lay.product = j.product();
        lay.productRate = j.productRate();
        String need3 = heaters.any() ? Core.bundle.format("eclipse-build.step.heaters", heaters.size, heater().localizedName, n(heaters.size * heaterOutput()), n(j.heat)) : "";
        lay.steps.add(Core.bundle.format("eclipse-build.step.machine", b.localizedName, need3).trim());
        if(power > 0f) lay.steps.add(Core.bundle.format("eclipse-build.step.power", n(power)) + (r != null ? " " + Core.bundle.format("eclipse-build.step.tier", RecipeCrafter.roman(r.tier)) : ""));
        else lay.steps.add(Core.bundle.get("eclipse-build.step.nopower"));
        lay.steps.add(Core.bundle.get(j.liqIn.any() || j.liqOut.any() ? "eclipse-build.step.feed-liquids" : "eclipse-build.step.feed"));
        if(lay.product != null) lay.totals.add(new String[]{Core.bundle.get("eclipse-build.total.makes"), n(lay.productRate) + "/s " + lay.product.localizedName});
        if(j.in.any()) lay.totals.add(new String[]{Core.bundle.get("eclipse-build.total.in"), n(total(j.in, j.seconds)) + "/s"});
        for(LiquidStack l : j.liqIn) lay.totals.add(new String[]{l.liquid.localizedName, n(l.amount * 60f) + "/s"});
        if(power > 0f) lay.totals.add(new String[]{Core.bundle.get("eclipse-build.total.power"), n(power) + "/s"});
        if(j.heat > 0f) lay.totals.add(new String[]{Core.bundle.get("eclipse-build.total.heat"), n(j.heat)});
        return lay;
    }

    static Item[] outCycle(Job j){
        Seq<Item> s = new Seq<>();
        for(ItemStack st : j.out) for(int i = 0; i < st.amount; i++) s.add(st.item);
        return s.toArray(Item.class);
    }

    static boolean freeRect(boolean[][] occ, int x, int y, int w, int h){
        if(x < 0 || y < 0 || x + w > occ.length || y + h > occ[0].length) return false;
        for(int i = x; i < x + w; i++) for(int k = y; k < y + h; k++) if(occ[i][k]) return false;
        return true;
    }

    // ---------------- full line: hot furnace products into a Vacuum Freezer ----------------

    /** The freezer recipe that cools this hot item, or null. */
    static @Nullable Recipe coolingFor(Item hot){
        RecipeCrafter vf = (RecipeCrafter)MetallurgyBlocks.vacuumFreezer;
        return vf.recipes.find(x -> x.inputs.length == 1 && x.inputs[0].item == hot);
    }

    /** True if this recipe makes a hot item that a full line can show (items in, at most 8 heat, a 3x3 machine). */
    public static boolean lineSupported(Block b, @Nullable Recipe r){
        if(!(b instanceof RecipeCrafter) || r == null || b.size != 3 || r.outputs.length == 0 || coolingFor(r.outputs[0].item) == null) return false;
        if(r.liquidInputs.length > 0 || (r.liquidOutputs.length > 0 && !r.vented) || r.catalyst != null) return false;
        return r.heat <= heaterOutput() * 2f + 0.001f;
    }

    public static @Nullable BuildLayout hotLine(RecipeCrafter rc, Recipe r){
        if(!lineSupported(rc, r)) return null;
        Job j = job(rc, r);
        Recipe cool = coolingFor(r.outputs[0].item);
        RecipeCrafter vf = (RecipeCrafter)MetallurgyBlocks.vacuumFreezer;
        Job jc = job(vf, cool);
        float made = r.outputs[0].amount / j.seconds, cooled = cool.inputs[0].amount / jc.seconds, inPerMachine = total(j.in, j.seconds);
        Block belt = Blocks.titaniumConveyor;
        float beltSpeed = ((Conveyor)belt).displayedSpeed;
        //as many furnaces as one freezer keeps up with, at most what one input belt can feed (and four, to fit the picture)
        int count = Math.max(1, Math.min(Math.min((int)(cooled / made + 0.001f), (int)(beltSpeed / inPerMachine)), 4));
        int heatersEach = r.heat > 0f ? Mathf.ceil(r.heat / heaterOutput() - 0.001f) : 0;

        BuildLayout lay = new BuildLayout();
        lay.recipe = r;
        lay.title = Core.bundle.format("eclipse-build.line", count, rc.localizedName, vf.localizedName);
        //rows from the bottom: 1 collector belt, 2-3 heaters below, 4-6 furnaces, 7-8 heaters above and feeders, 9 the ore line;
        //each furnace: feeder at its left column, heaters over its other two columns, output column on its right, a power
        //node in the gap on its left
        int yCollect = 1, yHeat = 2, yMachine = 4, yTop = 7, yLine = 9;
        Seq<Part> furnaces = new Seq<>(), heaters = new Seq<>(), nodes = new Seq<>();
        int lastOut = 0;
        float lineRate = inPerMachine * count;
        String lineCarries = rates(j.in, j.seconds, count);
        Seq<Integer> feeds = new Seq<>();
        for(int k = 0; k < count; k++){
            int minX = 1 + k * 5, cx = minX + 1;
            feeds.add(minX);
            furnaces.add(lay.add(rc, cx, yMachine + 1, 0, j.config, 1, machineInfo(j)));
            if(heatersEach >= 1) heaters.add(lay.add(heater(), minX + 1, yHeat, 1, null, 1, heaterInfo()));
            if(heatersEach >= 2) heaters.add(lay.add(heater(), minX + 1, yTop, 3, null, 1, heaterInfo()));
            //the feeders from the line's router down into the furnace
            for(int y = yLine - 1; y >= yTop; y--) lay.add(belt, minX, y, 3, null, 3, beltInfo(belt, rates(j.in, j.seconds, 1f)));
            //its output column down the right side to the collector
            int ox = minX + 3;
            for(int y = yMachine + 2; y >= yHeat; y--) lay.add(belt, ox, y, 3, null, 4, beltInfo(belt, rates(j.out, j.seconds, 1f)));
            lastOut = ox;
            nodes.add(lay.add(Blocks.powerNode, minX - 1, yMachine + 1, 0, null, 2, ""));
        }
        int lineEnd = feeds.peek() + 4;
        for(int x = 0; x <= lineEnd; x++){
            if(feeds.contains(x)) lay.add(Blocks.router, x, yLine, 0, null, 3, Core.bundle.format("eclipse-build.info.router", Blocks.router.localizedName));
            else lay.add(belt, x, yLine, 0, null, 3, beltInfo(belt, lineCarries));
        }
        int fx = lastOut + 3; //the freezer's centre
        for(int x = 1 + 3; x <= fx - 2; x++) lay.add(belt, x, yCollect, 0, null, 4, beltInfo(belt, rates(j.out, j.seconds, count)));
        Part freezer = lay.add(vf, fx, yCollect, 0, jc.config, 5, machineInfo(jc));
        Liquid cryo = Liquids.cryofluid;
        float cryoRate = jc.liqIn.first().amount * 60f * Math.min(1f, made * count / cooled);
        for(int y = yLine; y >= yCollect + 2; y--){
            Part p = lay.add(pipe(cryo), fx, y, 3, null, 5, beltInfo(pipe(cryo), cryo.localizedName + " " + n(cryoRate) + "/s"));
            p.liquid = cryo;
        }
        for(int x = fx + 2; x <= fx + 3; x++) lay.add(belt, x, yCollect, 0, null, 5, beltInfo(belt, cool.outputs[0].item.localizedName + " " + n(made * count) + "/s"));
        //one more node beside the freezer (too far from the last furnace's)
        Part freezerNode = lay.add(Blocks.powerNode, fx - 2, yMachine, 0, null, 5, "");
        nodes.add(freezerNode);
        lay.w = fx + 4;
        lay.h = yLine + 1;

        float power = (j.powerUse * count + heater().consPower.usage * heaters.size + jc.powerUse) * 60f;
        for(int k = 0; k < nodes.size; k++){
            Part node = nodes.get(k);
            node.info = Core.bundle.format("eclipse-build.info.node", Blocks.powerNode.localizedName, n(power));
            Seq<Part> linked = new Seq<>();
            if(node == freezerNode) linked.add(freezer);
            else{
                linked.add(furnaces.get(k));
                for(Part h : heaters) if(h.x == furnaces.get(k).x) linked.add(h);
            }
            //each node links the one placed before it, so the links find it
            if(k > 0) linked.add(nodes.get(k - 1));
            node.config = BuildLayout.links(node, linked);
        }

        //ports: the ore line in from the left, leftovers out on the right, cryofluid in from the top, the product out
        lay.port(BuildLayout.Port.Kind.items, -1, yLine).items = cycle(j);
        lay.port(BuildLayout.Port.Kind.excess, lineEnd + 1, yLine);
        lay.port(BuildLayout.Port.Kind.liquid, fx, yLine + 1).liquid = cryo;
        lay.port(BuildLayout.Port.Kind.output, fx + 4, yCollect);
        lay.port(BuildLayout.Port.Kind.power, -1, yMachine + 1);

        //item flows: the ore line, a set of inputs dropping into each furnace in turn; outputs down and along to the freezer
        Item[] one = cycle(j);
        Flow line = new Flow();
        line.items = one;
        line.interval = 1f / lineRate;
        line.perPath = one.length;
        line.paths = new float[count][];
        for(int k = 0; k < count; k++) line.paths[k] = new float[]{-0.5f, yLine + 0.5f, feeds.get(k) + 0.5f, yLine + 0.5f, feeds.get(k) + 0.5f, yMachine + 3f};
        line.step = 3;
        lay.flows.add(line);
        for(int k = 0; k < count; k++){
            int ox = 1 + k * 5 + 3;
            Flow f = new Flow();
            f.items = outCycle(j);
            f.interval = j.seconds / f.items.length;
            f.offset = 0.4f + k * j.seconds / count;
            f.path = new float[]{ox + 0.5f, yMachine + 1.5f, ox + 0.5f, yCollect + 0.5f, fx - 1f, yCollect + 0.5f};
            f.step = 4;
            lay.flows.add(f);
        }
        Flow cooledFlow = new Flow();
        cooledFlow.items = new Item[]{cool.outputs[0].item};
        cooledFlow.interval = 1f / (made * count);
        cooledFlow.offset = 3f;
        cooledFlow.path = new float[]{fx + 1.5f, yCollect + 0.5f, fx + 4.5f, yCollect + 0.5f};
        cooledFlow.step = 5;
        lay.flows.add(cooledFlow);

        //labels
        lay.label(0.2f, yLine + 0.95f, n(lineRate) + "/s · " + lineCarries, 0, 1, 3);
        if(heatersEach > 0) for(int k = 0; k < count; k++) lay.label(feeds.get(k) + 2f, yMachine + 0.2f, Core.bundle.format("eclipse-build.label.heat", n(heatersEach * heaterOutput()), n(r.heat)), 1, 0, 1);
        lay.label(4.2f, yCollect + 0.95f, n(made * count) + "/s " + r.outputs[0].item.localizedName, 0, 1, 4);
        lay.label(fx - 0.7f, yLine - 0.5f, cryo.localizedName + " " + n(cryoRate) + "/s", 0, -1, 5);
        lay.label(fx + 4f, yCollect + 1.55f, n(made * count) + "/s " + cool.outputs[0].item.localizedName, 0, -1, 5);
        lay.label(0.3f, 0.5f, Core.bundle.format("eclipse-build.label.power", n(power)), 2, 1, 2); //the empty bottom row

        lay.product = cool.outputs[0].item;
        lay.productRate = Math.min(made * count, cooled);
        lay.steps.add(heatersEach > 0
            ? Core.bundle.format("eclipse-build.line.step1-heat", count, rc.localizedName, heatersEach, heater().localizedName, n(heatersEach * heaterOutput()), n(r.heat))
            : Core.bundle.format("eclipse-build.line.step1", count, rc.localizedName));
        lay.steps.add(Core.bundle.format("eclipse-build.line.step2", nodes.size, n(power), RecipeCrafter.roman(r.tier), n(r.power() * 60f)));
        lay.steps.add(Core.bundle.format("eclipse-build.line.step3", Blocks.router.localizedName));
        lay.steps.add(Core.bundle.format("eclipse-build.line.step4", n(made), count, n(made * count)));
        lay.steps.add(Core.bundle.format("eclipse-build.line.step5", vf.localizedName, cryo.localizedName, n(lay.productRate), lay.product.localizedName));
        lay.totals.add(new String[]{Core.bundle.get("eclipse-build.total.makes"), n(lay.productRate) + "/s " + lay.product.localizedName});
        lay.totals.add(new String[]{Core.bundle.get("eclipse-build.total.in"), n(lineRate) + "/s"});
        lay.totals.add(new String[]{cryo.localizedName, n(cryoRate) + "/s"});
        lay.totals.add(new String[]{Core.bundle.get("eclipse-build.total.power"), n(power) + "/s"});
        if(heatersEach > 0) lay.totals.add(new String[]{Core.bundle.get("eclipse-build.total.heat"), n(heatersEach * heaterOutput()) + " × " + count});
        return lay;
    }

    /** Every example for a machine page: per recipe, the minimal one and, where a recipe supports it, the full line. */
    public static Seq<BuildLayout> examples(Block b){
        Seq<BuildLayout> out = new Seq<>();
        if(b instanceof RecipeCrafter rc){
            for(Recipe r : rc.recipes){
                BuildLayout min = minimal(b, r);
                if(min != null) out.add(min);
                BuildLayout line = hotLine(rc, r);
                if(line != null) out.add(line);
            }
        }else{
            BuildLayout min = minimal(b, null);
            if(min != null) out.add(min);
        }
        return out;
    }
}
