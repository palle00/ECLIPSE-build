package eclipse.input;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.liquid.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.*;

/**
 * Upgrade Section (desktop): with a belt, conduit or wall selected in the build menu, hold the upgrade key (Alt by
 * default, rebindable in Controls) over an existing block of the same kind. The connected section lights up with a count
 * and a cost check; click to queue a normal replacement plan for every block in it. Nothing is replaced instantly:
 * builders build the plans with normal costs, and vanilla carries belt contents over.
 * <ul>
 * <li>Belts and conduits: follows one line, forward and backward, through straight parts and corners. It stops at gaps,
 * at other block types, and where lines merge, so a line that joins from the side is never included.</li>
 * <li>Walls: every wall of the same type touching the section.</li>
 * </ul>
 * Only blocks of exactly the clicked type are included, and only when the selected block can replace it (same kind and size).
 */
public class UpgradePlanner{
    /** Most plans one click can queue. */
    public static final int maxPlans = 300;
    public static @Nullable KeyBind key;

    public enum Family{belt, conduit, wall}

    static @Nullable Building hovered;
    static @Nullable Block hoveredTarget;
    static int hoveredPlans = -1;
    static final Seq<Building> section = new Seq<>();

    /** Registers the key binding and the input / drawing hooks. Desktop clients only. */
    public static void init(){
        if(headless || mobile) return;
        key = KeyBind.add("eclipse_upgrade_section", KeyCode.altLeft, "eclipse");
        Events.run(Trigger.update, UpgradePlanner::update);
        Events.run(Trigger.draw, UpgradePlanner::draw);
    }

    public static @Nullable Family family(Block b){
        if(b instanceof Conveyor) return Family.belt;
        if(b instanceof Conduit) return Family.conduit;
        if(b instanceof Wall) return Family.wall;
        return null;
    }

    /** Whether the selected block can upgrade a section of the other: same kind and size, and the game allows the replacement. */
    public static boolean canUpgrade(Block from, Block target){
        Family f = family(from);
        return f != null && from != target && f == family(target) && from.size == target.size && target.canReplace(from);
    }

    /** The connected section an upgrade would cover, starting from one block (at most {@link #maxPlans} blocks). */
    public static Seq<Building> section(@Nullable Building start, Block target){
        Seq<Building> out = new Seq<>();
        if(start == null || !canUpgrade(start.block, target)) return out;
        IntSet seen = new IntSet();
        seen.add(start.pos());
        out.add(start);

        if(family(start.block) == Family.wall){
            for(int i = 0; i < out.size && out.size < maxPlans; i++){
                for(Building n : out.get(i).proximity){
                    if(n.block == start.block && n.team == start.team && out.size < maxPlans && seen.add(n.pos())) out.add(n);
                }
            }
            return out;
        }

        //belts and conduits: one line, forward then backward
        Building cur = start;
        while(out.size < maxPlans){
            Building next = cur.front();
            if(next == null || !same(next, start) || previous(next, start) != cur || !seen.add(next.pos())) break;
            out.add(next);
            cur = next;
        }
        cur = start;
        while(out.size < maxPlans){
            Building prev = previous(cur, start);
            if(prev == null || !seen.add(prev.pos())) break;
            out.insert(0, prev);
            cur = prev;
        }
        return out;
    }

    /**
     * The block before b in its line: the one feeding b's back (straight), or, with nothing behind b, the single one
     * feeding its side (a corner). Two feeders, or a back feeder plus a side one, mean lines merge here: then a side
     * feeder is another line and is not followed.
     */
    static @Nullable Building previous(Building b, Building start){
        Building back = b.back();
        if(back != null && same(back, start) && back.front() == b) return back;
        Building only = null;
        for(int side : new int[]{1, 3}){
            Building s = b.nearby(Mathf.mod(b.rotation + side, 4));
            if(s != null && same(s, start) && s.front() == b){
                if(only != null) return null;
                only = s;
            }
        }
        return only;
    }

    static boolean same(Building b, Building start){
        return b.block == start.block && b.team == start.team;
    }

    /** Replacement plans for a section, keeping each block's rotation; skips blocks already planned or not replaceable right now. */
    public static Seq<BuildPlan> plans(Seq<Building> blocks, Block target, @Nullable Unit builder){
        Seq<BuildPlan> out = new Seq<>();
        for(Building b : blocks){
            if(planned(builder, b, target) || !Build.validPlace(target, b.team, b.tileX(), b.tileY(), b.rotation)) continue;
            out.add(new BuildPlan(b.tileX(), b.tileY(), b.rotation, target));
        }
        return out;
    }

    /** Queues the plans on a builder; they are built like any other plan. Returns how many were queued. */
    public static int queue(Unit builder, Seq<Building> blocks, Block target){
        Seq<BuildPlan> plans = plans(blocks, target, builder);
        for(BuildPlan p : plans) builder.addBuild(p);
        return plans.size;
    }

    static boolean planned(@Nullable Unit builder, Building b, Block target){
        if(builder == null) return false;
        for(BuildPlan p : builder.plans) if(!p.breaking && p.x == b.tileX() && p.y == b.tileY() && p.block == target) return true;
        return false;
    }

    /** Whether the team's core holds the materials for this many replacement blocks. */
    public static boolean affordable(Block target, int count, @Nullable CoreBuild core){
        if(state.rules.infiniteResources || (core != null && core.team.rules().infiniteResources)) return true;
        if(core == null) return false;
        for(ItemStack s : target.requirements){
            if(core.items.get(s.item) < Math.round(s.amount * state.rules.buildCostMultiplier) * count) return false;
        }
        return true;
    }

    // ---- desktop input and preview ----

    static boolean active(){
        return key != null && state.isGame() && player != null && player.unit() != null && player.unit().canBuild()
            && control.input.block != null && Core.input.keyDown(key) && !Core.scene.hasMouse();
    }

    static void update(){
        if(!active()){
            hovered = null;
            section.clear();
            return;
        }
        Vec2 mouse = Core.input.mouseWorld();
        Building b = world.buildWorld(mouse.x, mouse.y);
        if(b != null && b.team != player.team()) b = null;
        Block target = control.input.block;
        //recompute only when the hovered block, the selection or the plan queue changes
        if(b != hovered || target != hoveredTarget || player.unit().plans.size != hoveredPlans){
            hovered = b;
            hoveredTarget = target;
            hoveredPlans = player.unit().plans.size;
            section.set(section(b, target));
        }
        if(!section.isEmpty() && Core.input.keyTap(Binding.select)){
            queue(player.unit(), section, target);
            hoveredPlans = -1;
        }
    }

    static void draw(){
        if(!active() || section.isEmpty() || hovered == null) return;
        Block target = hoveredTarget;
        int planned = 0, blocked = 0;
        Draw.z(Layer.overlayUI);
        for(Building b : section){
            boolean isPlanned = planned(player.unit(), b, target);
            boolean valid = Build.validPlace(target, b.team, b.tileX(), b.tileY(), b.rotation);
            if(isPlanned) planned++;
            else if(!valid) blocked++;
            Color color = isPlanned ? Pal.gray : valid ? Pal.accent : Pal.remove;
            if(!isPlanned && valid){
                Draw.color(Color.white, 0.55f);
                Draw.rect(target.fullIcon, b.x, b.y, target.rotate ? b.rotation * 90f : 0f);
            }
            Lines.stroke(1f, color);
            Lines.square(b.x, b.y, b.block.size * tilesize / 2f);
        }
        Draw.reset();

        int count = section.size - planned - blocked;
        boolean enough = affordable(target, count, player.team().core());
        StringBuilder text = new StringBuilder(Core.bundle.format("eclipse-upgrade.preview", count, target.localizedName));
        if(planned > 0) text.append("  [lightgray]").append(Core.bundle.format("eclipse-upgrade.planned", planned)).append("[]");
        if(blocked > 0) text.append("  [scarlet]").append(Core.bundle.format("eclipse-upgrade.blocked", blocked)).append("[]");
        if(section.size >= maxPlans) text.append("  [lightgray]").append(Core.bundle.format("eclipse-upgrade.limit", maxPlans)).append("[]");
        if(!enough) text.append("  [scarlet]").append(Core.bundle.get("eclipse-upgrade.short")).append("[]");
        target.drawPlaceText(text.toString(), hovered.tileX(), hovered.tileY(), count > 0);
        Draw.reset();
    }
}
