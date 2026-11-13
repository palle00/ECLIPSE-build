package eclipse.type;

import arc.*;
import arc.graphics.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import eclipse.content.*;
import eclipse.defense.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
import mindustry.gen.*;

/**
 * The Jammer's pulse carried by a unit (Leviathan): every interval, enemy units in range are Jammed
 * (weapons 40% slower); bosses for half as long. Refreshes, never stacks; never affects buildings.
 */
public class JamPulseAbility extends Ability{
    public float range = 140f, interval = 180f, duration = 240f;
    public Color color = Color.valueOf("b37aff");
    protected float timer;

    @Override
    public void update(Unit unit){
        timer += Time.delta;
        if(timer < interval) return;
        timer = 0f;
        Units.nearbyEnemies(unit.team, unit.x, unit.y, range, u -> u.apply(Munitions.jammed, u.hasEffect(StatusEffects.boss) ? duration / 2f : duration));
        Jammer.pulse.at(unit.x, unit.y, range, color);
    }

    @Override
    public void addStats(Table t){
        super.addStats(t);
        t.add(Core.bundle.format("ability.eclipse-jampulse.stats", Strings.autoFixed(range / 8f, 1), Strings.autoFixed(interval / 60f, 1), Strings.autoFixed(duration / 60f, 1)));
    }

    @Override
    public String localized(){
        return Core.bundle.get("ability.eclipse-jampulse");
    }
}
