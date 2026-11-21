package eclipse.type;

import arc.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.abilities.*;
import mindustry.gen.*;
import mindustry.world.blocks.environment.*;

/**
 * An amphibious hull (the Tide's boats): fast on water, slow on land. Off water the unit is kept Slowed (vanilla's
 * status, speed x0.4); on water it leaves ripples behind. The unit itself is a hovering ground unit, so it crosses
 * deep water without drowning and paths over water and land alike (the game's hover path cost).
 */
public class AmphibiousAbility extends Ability{
    @Override
    public void update(Unit unit){
        Floor floor = unit.floorOn();
        if(floor == null) return;
        if(!floor.isLiquid){
            unit.apply(StatusEffects.slow, 10f);
        }else if(unit.moving() && Mathf.chanceDelta(0.2f)){
            Fx.ripple.at(unit.x, unit.y, unit.hitSize / 8f, floor.mapColor);
        }
    }

    @Override
    public void addStats(Table t){
        super.addStats(t);
        t.add(Core.bundle.format("ability.eclipse-amphibious.stats", Strings.autoFixed(StatusEffects.slow.speedMultiplier * 100f, 0)));
    }

    @Override
    public String localized(){
        return Core.bundle.get("ability.eclipse-amphibious");
    }
}
