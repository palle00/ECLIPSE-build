package eclipse.logistics;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * Base for pass-through item routers (sorter, hub, merger, splitter, balancer, filter).
 * <ul>
 * <li><b>No loss, no duplication:</b> an item is only accepted when {@link RoutingBuild#route} finds a destination that
 * accepts it right now; otherwise it stays on the incoming belt. If a destination refuses between accept and handle,
 * the item waits in a one-item safety buffer.</li>
 * <li><b>Finite throughput:</b> {@link #itemsPerSecond} tokens per second.</li>
 * <li><b>Direction:</b> sides are relative to rotation (0 front, 1 left, 2 back, 3 right), so rotating a building
 * rotates its routes.</li>
 * <li><b>Determinism:</b> {@code route(..., false)} only peeks; {@code route(..., true)} commits round-robin state, so
 * the server and clients make the same decisions.</li>
 * </ul>
 * Like vanilla sorters these transfer instantly, so two of them can't feed each other directly (a belt is needed between).
 */
public abstract class RoutingBlock extends Block{
    public float itemsPerSecond = 30f;

    public RoutingBlock(String name){
        super(name);
        update = true;
        solid = false;
        underBullets = true;
        rotate = true;
        configurable = true;
        saveConfig = true;
        hasItems = true;
        itemCapacity = 1;
        instantTransfer = true;
        unloadable = false;
        group = BlockGroup.transportation;
        envEnabled = Env.any;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.itemsMoved, itemsPerSecond, StatUnit.itemsSecond);
    }

    /** Relative side names, for UI: 0 front, 1 left, 2 back, 3 right. */
    public static final String[] sideKeys = {"front", "left", "back", "right"};

    public abstract class RoutingBuild extends Building{
        /** Handling tokens; one item costs one token. */
        public float budget;
        /** Safety buffer for an item whose destination refused it between accept and handle. */
        public @Nullable Item held;
        public int heldFrom = 2;
        /** Bitmask of relative sides that recently refused an item (for drawing). */
        public int blocked;
        float blockedTimer;

        /** Buildings adjacent on each relative side (1 for 1x1 blocks, up to `size` for larger ones). */
        @SuppressWarnings({"unchecked", "rawtypes"})
        public final Seq<Building>[] sides = new Seq[]{new Seq<Building>(), new Seq<Building>(), new Seq<Building>(), new Seq<Building>()};

        /** Relative side (0 front, 1 left, 2 back, 3 right) of an adjacent building, or -1. */
        public int rel(Building other){
            if(other == null) return -1;
            int abs = relativeTo(other);
            return abs < 0 ? -1 : Mathf.mod(abs - rotation, 4);
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();
            for(Seq<Building> s : sides) s.clear();
            for(Building b : proximity){
                int r = rel(b);
                if(r >= 0) sides[r].add(b);
            }
            //stable order along each side so round-robin is deterministic
            for(Seq<Building> s : sides) s.sort(b -> b.pos());
        }

        /** Can `to` take this item from us now? Respects vanilla's no-chaining rule for instant-transfer blocks. */
        public boolean canPass(@Nullable Building from, Building to, Item item){
            if(to == null || to.team != team || to == from) return false;
            if(from != null && from.block.instantTransfer && to.block.instantTransfer) return false;
            return to.acceptItem(this, item);
        }

        /** The first building on a relative side that accepts the item. */
        public @Nullable Building firstOnSide(@Nullable Building from, int side, Item item){
            for(Building b : sides[side]) if(canPass(from, b, item)) return b;
            return null;
        }

        protected void markBlocked(int side){
            blocked |= 1 << side;
            blockedTimer = 30f;
        }

        /** Whether items may enter from this neighbour. */
        public abstract boolean acceptsFrom(Building source);

        /** Destination for an item, or null to hold it on the incoming belt. commit = false only peeks. */
        public abstract @Nullable Building route(@Nullable Building source, Item item, boolean commit);

        @Override
        public void updateTile(){
            //efficiency is 1 for unpowered routers and the power satisfaction for powered ones
            budget = Math.min(budget + itemsPerSecond / 60f * delta() * efficiency, Math.max(1f, itemsPerSecond / 60f * 2f));
            if(blockedTimer > 0f && (blockedTimer -= delta()) <= 0f) blocked = 0;

            //retry an item stuck in the safety buffer
            if(held != null && budget >= 1f){
                Building source = sides[heldFrom].isEmpty() ? null : sides[heldFrom].first();
                Building to = route(source, held, true);
                if(to != null){
                    budget -= 1f;
                    Item item = held;
                    held = null;
                    items.remove(item, 1);
                    to.handleItem(this, item);
                }
            }
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return held == null && budget >= 1f && team == source.team && acceptsFrom(source) && route(source, item, false) != null;
        }

        @Override
        public void handleItem(Building source, Item item){
            Building to = route(source, item, true);
            if(to != null){
                budget -= 1f;
                to.handleItem(this, item);
            }else{
                //never lose an item: keep it until a destination frees up
                held = item;
                heldFrom = Math.max(rel(source), 0);
                items.add(item, 1);
            }
        }

        @Override
        public int acceptStack(Item item, int amount, Teamc source){
            return 0;
        }

        @Override
        public void draw(){
            super.draw();
            drawBlocked();
        }

        /** Red marks on sides that refused items recently. */
        protected void drawBlocked(){
            if(blocked == 0) return;
            Draw.color(Pal.remove, 0.8f);
            for(int s = 0; s < 4; s++){
                if((blocked & (1 << s)) == 0) continue;
                float angle = (rotation + s) * 90f;
                Tmp.v1.trns(angle, size * tilesize / 2f - 1.5f);
                Fill.square(x + Tmp.v1.x, y + Tmp.v1.y, 1.5f, 45f);
            }
            Draw.color();
        }

        /** Small item icon drawn toward a relative side (for showing configured routes). */
        protected void drawItemOnSide(Item item, int side){
            float angle = (rotation + side) * 90f;
            Tmp.v1.trns(angle, size * tilesize / 2f - 2.5f);
            Draw.rect(item.fullIcon, x + Tmp.v1.x, y + Tmp.v1.y, 4f, 4f);
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.s(held == null ? -1 : held.id);
            write.b(heldFrom);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1){
                short id = read.s();
                held = id < 0 ? null : content.item(id);
                heldFrom = read.b();
            }
        }
    }

    /** A direction picker row: one toggle button per allowed relative side. */
    protected static void sideButtons(arc.scene.ui.layout.Table t, int[] allowed, arc.func.Intp current, arc.func.Intc pick, boolean allowHold){
        if(allowHold){
            t.button(arc.Core.bundle.get("eclipse-route-hold"), mindustry.ui.Styles.flatTogglet, () -> pick.get(-1)).size(56f, 32f).checked(b -> current.get() == -1);
        }
        for(int side : allowed){
            t.button(arc.Core.bundle.get("eclipse-route-" + sideKeys[side]), mindustry.ui.Styles.flatTogglet, () -> pick.get(side)).size(56f, 32f).checked(b -> current.get() == side);
        }
    }

    static final Color tmpColor = new Color();
}
