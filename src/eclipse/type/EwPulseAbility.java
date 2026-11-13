package eclipse.type;

import arc.*;
import arc.graphics.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import eclipse.defense.*;
import mindustry.entities.abilities.*;
import mindustry.gen.*;
import mindustry.world.blocks.defense.turrets.*;

import static mindustry.Vars.*;

/**
 * Electronic Warfare Drone pulse (military M5): every interval, enemy turrets in range are slowed
 * (aim and fire) for a while. One bounded effect, as the defence spec asks: it never disables a turret,
 * never touches other buildings, units, cores or waves, and a slowed turret is not slowed further.
 */
public class EwPulseAbility extends Ability{
    public float range = 80f, interval = 300f, duration = 240f, slow = 0.5f;
    public Color color = Color.valueOf("7fd8ff");
    protected float timer;

    @Override
    public void update(Unit unit){
        timer += Time.delta;
        if(timer < interval) return;
        timer = 0f;
        indexer.allBuildings(unit.x, unit.y, range, b -> {
            if(b.team != unit.team && b.block instanceof Turret) b.applySlowdown(slow, duration);
        });
        Jammer.pulse.at(unit.x, unit.y, range, color);
    }

    @Override
    public void addStats(Table t){
        super.addStats(t);
        t.add(Core.bundle.format("ability.eclipse-ewpulse.stats", Strings.autoFixed(range / 8f, 1), Strings.autoFixed(interval / 60f, 1),
            Math.round((1f - slow) * 100), Strings.autoFixed(duration / 60f, 1)));
    }

    @Override
    public String localized(){
        return Core.bundle.get("ability.eclipse-ewpulse");
    }
}
