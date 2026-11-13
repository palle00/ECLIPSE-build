package eclipse.robotics;

import arc.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import eclipse.robotics.RobotHub.*;
import eclipse.robotics.RobotType.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

/**
 * Shared AI for ECLIPSE robots (runs on the server / in singleplayer, like every unit AI). A robot registers with the
 * nearest Robot Dispatch Hub that has a free slot, then works the job stored in its slot:
 * - logistics: carry up to its cargo capacity of one item from the source building to the destination, only what the
 *   destination accepts; a full destination makes it wait with its cargo, never delete it;
 * - repair: fly to the one target building and repair it from a finite repair charge bought with titanium rods at the hub;
 * - shield: escort the largest friendly group in the hub's area and top units up to {@link #shieldCap} from a finite pool,
 *   refilled only at the hub; the shield it granted disappears when the drone is destroyed.
 * Every robot drains charge while working and flies home to recharge. Invalid hubs, routes or targets make it stop
 * safely and wait at the hub. Nothing is transferred without the robot physically flying there.
 */
public class RobotAI extends AIController{
    public enum State{idle, toSource, toTarget, waiting, servicing, returning, charging, noHub, noStock}

    public static final float repairCapacity = 600f, repairRate = 40f, repairRange = 3f * 8f,
        shieldCapacity = 400f, shieldRecharge = 40f, shieldRadius = 5f * 8f, shieldCap = 40f, shieldPerPass = 10f,
        //charge used per second while working: a full charge lasts two minutes
        drain = 1f / 120f, lowCharge = 0.15f, hubSearchRange = 60f * 8f, transferRange = 2.5f * 8f;

    /** Shield granted by each shield drone: drone id -> (unit id -> amount). Removed when the drone dies. */
    static final IntMap<IntFloatMap> grants = new IntMap<>();

    public @Nullable HubBuild hub;
    public int slot = -1;
    boolean recharging, refillingShield;
    float searchTimer, passTimer, escortTimer;
    int escortId = -1;

    public static void registerEvents(){
        Events.on(UnitDestroyEvent.class, e -> {
            if(e.unit.type instanceof RobotType) revokeShields(e.unit.id);
        });
        Events.on(WorldLoadEvent.class, e -> {
            grants.clear();
            //a hub from the previous map still looks valid to its own old tile, so check it is in the loaded world
            RobotHub.all.removeAll(b -> !b.isValid() || world.build(b.pos()) != b);
            //unit ids are not stable across a load: every robot registers again
            if(!net.client()) for(HubBuild b : RobotHub.all) for(RobotHub.Slot s : b.slotData) s.unitId = -1;
        });
    }

    RobotType type(){
        return (RobotType)unit.type;
    }

    @Override
    public void updateTargeting(){
        //no weapons
    }

    @Override
    public void updateMovement(){
        if(!validHub()){
            findHub();
            if(!validHub()){
                unit.vel.scl(0.9f);
                return;
            }
        }
        RobotHub.Slot s = hub.slotData[slot];
        RobotHub block = (RobotHub)hub.block;
        boolean docked = unit.within(hub, block.dockRange);
        if(docked) hub.service(unit, s, Time.delta);

        if(s.charge < lowCharge) recharging = true;
        if(recharging){
            if(s.charge >= 0.99f) recharging = false;
            else{
                goHome(s, docked ? State.charging : State.returning);
                return;
            }
        }

        switch(type().role){
            case logistics -> logistics(s, block);
            case repair -> repair(s, block, docked);
            case shield -> shield(s, block, docked);
        }
    }

    boolean validHub(){
        return hub != null && hub.isValid() && hub.team == unit.team && slot >= 0 && hub.slotData[slot].unitId == unit.id;
    }

    void findHub(){
        hub = null;
        slot = -1;
        searchTimer -= Time.delta;
        if(searchTimer > 0f) return;
        searchTimer = 60f;
        HubBuild best = null;
        for(HubBuild b : RobotHub.all){
            if(!b.isValid() || b.team != unit.team || !unit.within(b, hubSearchRange)) continue;
            if(b.slotOf(unit) == -1 && !b.hasFreeSlot(type().role)) continue;
            if(best == null || unit.dst(b) < unit.dst(best)) best = b;
        }
        if(best == null) return;
        int s = best.register(unit, type().role);
        if(s != -1){
            hub = best;
            slot = s;
        }
    }

    void goHome(RobotHub.Slot s, State state){
        s.state = state;
        moveTo(hub, ((RobotHub)hub.block).dockRange * 0.5f);
    }

    void work(RobotHub.Slot s){
        s.charge = Math.max(0f, s.charge - drain * Time.delta / 60f);
    }

    @Nullable Building building(int pos, RobotHub.Slot s){
        if(pos < 0) return null;
        Building b = world.build(pos);
        return hub.inRange(b) ? b : null;
    }

    // ---- logistics: one item, one route, finite cargo ----

    void logistics(RobotHub.Slot s, RobotHub block){
        Building src = building(s.source, s), dst = building(s.dest, s);
        Item item = s.item < 0 ? null : content.item(s.item);
        if(src == null || dst == null || item == null || src == dst){
            goHome(s, State.idle);
            return;
        }
        if(unit.stack.amount <= 0){
            s.state = State.toSource;
            moveTo(src, transferRange * 0.6f);
            work(s);
            if(unit.within(src, transferRange + src.block.size * 4f)){
                int amount = Math.min(src.items == null ? 0 : src.items.get(item), unit.type.itemCapacity);
                if(amount > 0 && src.block.unloadable && src.canUnload()) Call.takeItems(src, item, amount, unit);
                else s.state = State.waiting; //source empty: wait here
            }
        }else{
            s.state = State.toTarget;
            moveTo(dst, transferRange * 0.6f);
            work(s);
            if(unit.within(dst, transferRange + dst.block.size * 4f)){
                int accepted = wants(dst, unit.stack.item) ? dst.acceptStack(unit.stack.item, unit.stack.amount, unit) : 0;
                if(accepted > 0) Call.transferItemTo(unit, unit.stack.item, accepted, unit.x, unit.y, dst);
                else s.state = State.waiting; //destination full or refuses the item: keep the cargo and wait
            }
        }
    }

    /** Only deliver to storage, cores, turrets (ammo) and machines that actually use the item. */
    static boolean wants(Building b, Item item){
        return b instanceof StorageBlock.StorageBuild || b instanceof CoreBlock.CoreBuild || b instanceof Turret.TurretBuild || b.block.consumesItem(item);
    }

    // ---- repair: one target, finite repair charge ----

    void repair(RobotHub.Slot s, RobotHub block, boolean docked){
        Building target = building(s.target, s);
        if(target == null || target.health >= target.maxHealth - 0.01f){
            goHome(s, State.idle);
            return;
        }
        if(s.repairCharge <= 0f){
            goHome(s, docked && !hub.items.has(block.repairStock) ? State.noStock : State.returning);
            return;
        }
        moveTo(target, repairRange * 0.6f);
        work(s);
        s.state = State.toTarget;
        if(unit.within(target, repairRange + target.block.size * 4f)){
            float amount = Math.min(Math.min(repairRate * Time.delta / 60f, s.repairCharge), target.maxHealth - target.health);
            target.heal(amount);
            s.repairCharge -= amount;
            s.state = State.servicing;
        }
    }

    // ---- shield: escort a group, shared finite pool ----

    void shield(RobotHub.Slot s, RobotHub block, boolean docked){
        escortTimer -= Time.delta;
        Unit escort = escortId == -1 ? null : Groups.unit.getByID(escortId);
        if(escortTimer <= 0f || escort == null || escort.dead || !hub.within(escort, block.range)){
            escortTimer = 180f;
            escort = pickEscort(block);
            escortId = escort == null ? -1 : escort.id;
        }
        //refill at the hub when empty, and only leave again nearly full (no back-and-forth on a trickle of shield)
        if(s.shieldPool < shieldPerPass) refillingShield = true;
        if(refillingShield && s.shieldPool >= shieldCapacity * 0.9f) refillingShield = false;
        if(escort == null || refillingShield){
            goHome(s, escort == null ? State.idle : docked ? State.charging : State.returning);
            return;
        }
        moveTo(escort, shieldRadius * 0.4f);
        work(s);
        s.state = State.servicing;
        passTimer += Time.delta;
        if(passTimer < 15f) return;
        passTimer = 0f;

        IntFloatMap mine = grants.get(unit.id, IntFloatMap::new);
        Units.nearby(unit.team, unit.x, unit.y, shieldRadius, u -> {
            if(u.dead || u.type instanceof RobotType) return;
            float have = Math.min(mine.get(u.id, 0f), u.shield); //damage eats shield first
            //top up to the cap only: a unit already shielded by a beacon (or natively) gets nothing extra
            float add = Math.min(Math.min(shieldCap - u.shield, shieldPerPass), s.shieldPool);
            if(add > 0.01f){
                u.shield += add;
                have += add;
                s.shieldPool -= add;
            }
            if(have > 0f) mine.put(u.id, have); else mine.remove(u.id, 0f);
        });
        //granted shield stays on a unit until damage uses it up or this drone is destroyed (then it is revoked)
    }

    /** The friendly non-robot unit in the hub's area with the most allies around it. */
    @Nullable Unit pickEscort(RobotHub block){
        Unit[] best = {null};
        int[] bestCount = {0};
        Units.nearby(unit.team, hub.x, hub.y, block.range, u -> {
            if(u.dead || u.type instanceof RobotType) return;
            int[] n = {0};
            Units.nearby(unit.team, u.x, u.y, shieldRadius, o -> {
                if(!(o.type instanceof RobotType)) n[0]++;
            });
            if(n[0] > bestCount[0]){
                bestCount[0] = n[0];
                best[0] = u;
            }
        });
        return best[0];
    }

    /** Takes back the shield a drone granted (it died or stopped escorting). */
    static void revokeShields(int droneId){
        IntFloatMap mine = grants.remove(droneId);
        if(mine == null || net.client()) return;
        for(IntFloatMap.Entry e : mine.entries()){
            Unit u = Groups.unit.getByID(e.key);
            if(u != null && !u.dead) u.shield = Math.max(0f, u.shield - Math.min(e.value, u.shield));
        }
    }
}
