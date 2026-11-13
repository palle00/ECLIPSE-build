package eclipse.robotics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * An ECLIPSE utility robot: a small flying unit with one job ({@link Role}), run by {@link RobotAI} from a
 * Robot Dispatch Hub. Built in the vanilla Air Factory, so unit caps and research apply as usual. No weapons.
 */
public class RobotType extends UnitType{
    public enum Role{logistics, repair, shield}

    public static final Stat roleStat = new Stat("eclipse-robot-role", StatCat.function);

    public Role role = Role.logistics;

    public RobotType(String name){
        super(name);
        constructor = UnitEntity::create;
        controller = u -> new RobotAI();
        flying = true;
        lowAltitude = true;
        drag = 0.08f;
        accel = 0.12f;
        hitSize = 9f;
        engineOffset = 5f;
        engineSize = 2f;
        logicControllable = false;
        playerControllable = false;
        isEnemy = false;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(roleStat, Core.bundle.get("eclipse-robot.role-" + role.name()));
    }

    /** Shows the job: a repair beam to the target, or the shield radius around an escorting drone. */
    @Override
    public void draw(Unit unit){
        super.draw(unit);
        RobotHub.Slot s = RobotHub.slotFor(unit);
        if(s == null || s.state != RobotAI.State.servicing) return;
        Draw.z(Layer.flyingUnit + 0.1f);
        if(role == Role.repair){
            Building t = s.target < 0 ? null : world.build(s.target);
            if(t != null){
                Draw.color(Pal.heal, 0.6f + Mathf.absin(Time.time, 4f, 0.3f));
                Lines.stroke(1.5f);
                Lines.line(unit.x, unit.y, t.x, t.y);
            }
        }else if(role == Role.shield){
            Draw.color(Color.valueOf("7fd8ff"), 0.45f);
            Lines.stroke(1f);
            Lines.circle(unit.x, unit.y, RobotAI.shieldRadius);
        }
        Draw.reset();
    }
}
