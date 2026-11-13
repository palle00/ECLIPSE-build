package eclipse.robotics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import eclipse.robotics.RobotType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * Robot Dispatch Hub: a local home for up to {@link #slots} ECLIPSE robots within {@link #range}.
 * Robots register themselves with the nearest hub that has a free slot. Each slot stores the robot's job, set by the
 * player (tap a building to pick a source, destination or repair target; pick an item from a list), plus its charge.
 * The hub recharges docked robots with power and turns its titanium-rod stock into repair charge. It controls only
 * ECLIPSE robots, never other units, and nothing reaches beyond its radius.
 */
public class RobotHub extends Block{
    /** Every hub in the world, so robots can find one without scanning the map. */
    public static final Seq<HubBuild> all = new Seq<>();
    public static final Stat slotsStat = new Stat("eclipse-robot-slots", StatCat.function);

    public int slots = 4;
    public float range = 25f * 8f;
    public float dockRange = 3f * 8f;
    /** Charge gained per second while docked at full power (charge goes 0..1). */
    public float chargeRate = 1f / 15f;
    public Item repairStock;
    public float healthPerStock = 120f;
    public Color color = Color.valueOf("98ffa9");

    //config operations: IntSeq(op, slot, value)
    public static final int opSource = 0, opDest = 1, opItem = 2, opTarget = 3, opClear = 4;

    public RobotHub(String name){
        super(name);
        update = true;
        solid = true;
        configurable = true;
        hasPower = true;
        hasItems = true;
        itemCapacity = 40;
        sync = true;
        saveConfig = false;
        config(IntSeq.class, (HubBuild b, IntSeq s) -> b.apply(s.get(0), s.get(1), s.get(2)));
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.range, range / tilesize, StatUnit.blocks);
        stats.add(slotsStat, slots);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, color);
    }

    /** The slot a robot is registered in, on any hub (for drawing; hubs are few). */
    public static @Nullable Slot slotFor(Unit u){
        for(HubBuild b : all){
            if(!b.isValid()) continue;
            for(Slot s : b.slotData) if(s.unitId == u.id) return s;
        }
        return null;
    }

    /** One robot's job and status. Positions are building positions (-1 = unset). */
    public static class Slot{
        public @Nullable Role role;
        public int unitId = -1;
        public int source = -1, dest = -1, target = -1, item = -1;
        public float charge = 1f, repairCharge, shieldPool;
        public RobotAI.State state = RobotAI.State.idle;
    }

    public class HubBuild extends Building{
        public Slot[] slotData = new Slot[slots];
        /** Client-side: which slot field the next building tap sets. */
        int pickOp = -1, pickSlot = -1, itemSlot = -1;

        {
            for(int i = 0; i < slots; i++) slotData[i] = new Slot();
        }

        @Override
        public void created(){
            super.created();
            if(!all.contains(this)) all.add(this);
        }

        @Override
        public void onRemoved(){
            all.remove(this);
            super.onRemoved();
        }

        public boolean inRange(Building b){
            return b != null && b.isValid() && b.team == team && within(b, range + b.block.size * tilesize / 2f);
        }

        /** The slot this robot is registered in, or -1. */
        public int slotOf(Unit u){
            for(int i = 0; i < slots; i++) if(slotData[i].unitId == u.id) return i;
            return -1;
        }

        /** Registers a robot: prefers a free slot already set up for its role, then any free slot. Server only. */
        public int register(Unit u, Role role){
            int existing = slotOf(u);
            if(existing != -1) return existing;
            int pick = -1;
            for(int i = 0; i < slots; i++){
                Slot s = slotData[i];
                if(occupied(s)) continue;
                if(s.role == role){ pick = i; break; }
                if(pick == -1 && s.role == null) pick = i;
            }
            if(pick == -1) return -1;
            Slot s = slotData[pick];
            if(s.role != role){
                //a different robot type takes over an empty slot: start from a clean job
                slotData[pick] = s = new Slot();
                s.role = role;
            }
            s.unitId = u.id;
            return pick;
        }

        boolean occupied(Slot s){
            if(s.unitId == -1) return false;
            Unit u = Groups.unit.getByID(s.unitId);
            if(u == null || u.dead || !(u.type instanceof RobotType)){
                s.unitId = -1;
                return false;
            }
            return true;
        }

        public boolean hasFreeSlot(Role role){
            for(Slot s : slotData) if(!occupied(s) && (s.role == null || s.role == role)) return true;
            return false;
        }

        void apply(int op, int slot, int value){
            if(slot < 0 || slot >= slots) return;
            Slot s = slotData[slot];
            switch(op){
                case opSource -> s.source = value;
                case opDest -> s.dest = value;
                case opTarget -> s.target = value;
                case opItem -> s.item = value;
                case opClear -> { s.source = s.dest = s.target = s.item = -1; }
            }
        }

        /** Docked robots recharge with the hub's power; repair charge is bought with titanium rods. */
        public void service(Unit u, Slot s, float delta){
            if(efficiency > 0f) s.charge = Math.min(1f, s.charge + chargeRate * efficiency * delta / 60f);
            if(s.role == Role.repair && repairStock != null && s.repairCharge <= RobotAI.repairCapacity - healthPerStock && items.has(repairStock)){
                items.remove(repairStock, 1);
                s.repairCharge += healthPerStock;
            }
            if(s.role == Role.shield && efficiency > 0f) s.shieldPool = Math.min(RobotAI.shieldCapacity, s.shieldPool + RobotAI.shieldRecharge * efficiency * delta / 60f);
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return item == repairStock && items.get(item) < itemCapacity;
        }

        @Override
        public boolean onConfigureBuildTapped(Building other){
            if(pickOp != -1 && other != this){
                if(inRange(other)) configure(IntSeq.with(pickOp, pickSlot, other.pos()));
                else if(!headless) ui.showInfoToast(Core.bundle.get("eclipse-hub.out-of-range"), 2f);
                pickOp = -1;
                rebuildConfig();
                return false;
            }
            return other != this;
        }

        void rebuildConfig(){
            if(!headless && control.input.config.getSelected() == this) control.input.config.showConfig(this);
        }

        @Override
        public void buildConfiguration(Table table){
            table.background(Styles.black6);
            table.margin(6f);
            table.defaults().left();
            for(int i = 0; i < slots; i++){
                int slot = i;
                Slot s = slotData[i];
                table.table(row -> {
                    row.left().defaults().left().padRight(4f);
                    Unit u = s.unitId == -1 ? null : Groups.unit.getByID(s.unitId);
                    String who = s.role == null ? Core.bundle.get("eclipse-hub.empty")
                        : Core.bundle.get("eclipse-robot.role-" + s.role.name()) + (u == null ? " [lightgray](" + Core.bundle.get("eclipse-hub.no-robot") + ")[]" : "");
                    row.add((slot + 1) + ". " + who).minWidth(180f);
                    row.label(() -> s.role == null ? "" : "[lightgray]" + Core.bundle.get("eclipse-robot.state-" + s.state.name()) + "  " + (int)(s.charge * 100) + "%");
                }).row();
                if(s.role == null) continue;
                table.table(row -> {
                    row.left().defaults().height(36f).padRight(4f);
                    if(s.role == Role.logistics){
                        pickButton(row, slot, opSource, "eclipse-hub.source", s.source);
                        pickButton(row, slot, opDest, "eclipse-hub.dest", s.dest);
                        Item item = s.item < 0 ? null : content.item(s.item);
                        row.button(b -> {
                            if(item != null) b.image(item.uiIcon).size(20f).padRight(4f);
                            b.add(Core.bundle.get("eclipse-hub.item"));
                        }, Styles.flatt, () -> {
                            itemSlot = itemSlot == slot ? -1 : slot;
                            rebuildConfig();
                        }).width(110f);
                    }else if(s.role == Role.repair){
                        pickButton(row, slot, opTarget, "eclipse-hub.target", s.target);
                        row.add("[lightgray]" + Core.bundle.format("eclipse-hub.repair-charge", (int)s.repairCharge));
                    }else{
                        row.add("[lightgray]" + Core.bundle.format("eclipse-hub.shield-pool", (int)s.shieldPool));
                    }
                }).padLeft(16f).row();
                if(itemSlot == slot){
                    table.table(t -> ItemSelection.buildTable(t, content.items().select(it -> !it.isHidden()),
                        () -> s.item < 0 ? null : content.item(s.item),
                        it -> {
                            configure(IntSeq.with(opItem, slot, it == null ? -1 : it.id));
                            itemSlot = -1;
                            rebuildConfig();
                        }, 6)).padLeft(16f).row();
                }
            }
            if(pickOp != -1) table.add("[accent]" + Core.bundle.get("eclipse-hub.pick-hint")).padTop(4f).row();
        }

        void pickButton(Table row, int slot, int op, String key, int pos){
            Building b = pos < 0 ? null : world.build(pos);
            row.button((pickOp == op && pickSlot == slot ? "[accent]" : "") + Core.bundle.get(key) + (b == null ? "" : ": " + b.block.localizedName), Styles.flatt, () -> {
                pickOp = op;
                pickSlot = slot;
                rebuildConfig();
            }).width(150f);
        }

        @Override
        public void drawConfigure(){
            super.drawConfigure();
            Drawf.dashCircle(x, y, range, color);
            for(Slot s : slotData){
                for(int pos : new int[]{s.source, s.dest, s.target}){
                    Building b = pos < 0 ? null : world.build(pos);
                    if(b != null) Drawf.select(b.x, b.y, b.block.size * tilesize / 2f + 2f, color);
                }
            }
        }

        @Override
        public void drawSelect(){
            super.drawSelect();
            Drawf.dashCircle(x, y, range, color);
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.b(slots);
            for(Slot s : slotData){
                write.b(s.role == null ? -1 : s.role.ordinal());
                write.i(s.source);
                write.i(s.dest);
                write.i(s.target);
                write.s(s.item);
                write.f(s.charge);
                write.f(s.repairCharge);
                write.f(s.shieldPool);
                write.b(s.state.ordinal());
                write.i(s.unitId);
            }
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision < 1) return;
            int n = read.b();
            for(int i = 0; i < n; i++){
                Slot s = i < slots ? slotData[i] : new Slot();
                int role = read.b();
                s.role = role < 0 ? null : Role.values()[role];
                s.source = read.i();
                s.dest = read.i();
                s.target = read.i();
                s.item = read.s();
                s.charge = read.f();
                s.repairCharge = read.f();
                s.shieldPool = read.f();
                s.state = RobotAI.State.values()[read.b()];
                //unit ids hold within a session (and sync to clients); RobotAI clears them when a world loads
                s.unitId = read.i();
            }
        }
    }
}
