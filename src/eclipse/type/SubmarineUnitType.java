package eclipse.type;

import arc.graphics.g2d.*;
import eclipse.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

/**
 * A submarine (the Tide's subs, enemy-only): a naval unit that can dive. While Submerged it can't be targeted or hit by
 * anything (turrets ignore it, bullets and blasts pass through), unless a Sonar has it on contact; mines still go off
 * under it ({@link MineBulletType}). It surfaces to attack ({@link DiveAbility}) and is then hittable like any boat.
 * Both states are status effects, so they save with the map and sync to clients.
 */
public class SubmarineUnitType extends UnitType{

    public SubmarineUnitType(String name){
        super(name);
        constructor = UnitWaterMove::create;
        naval = true;
        outlineColor = Pal.darkOutline;
    }

    /** Submerged and not on sonar: out of reach of everything but mines. */
    public static boolean hidden(Unit unit){
        return unit.hasEffect(Munitions.submerged) && !unit.hasEffect(Munitions.sonarContact);
    }

    @Override
    public boolean targetable(Unit unit, Team targeter){
        return super.targetable(unit, targeter) && !hidden(unit);
    }

    @Override
    public boolean hittable(Unit unit){
        return super.hittable(unit) && !hidden(unit);
    }

    @Override
    public void applyColor(Unit unit){
        super.applyColor(unit);
        //a dark shape under the surface; clearer when sonar has it
        if(unit.hasEffect(Munitions.submerged)) Draw.alpha(hidden(unit) ? 0.3f : 0.6f);
    }
}
