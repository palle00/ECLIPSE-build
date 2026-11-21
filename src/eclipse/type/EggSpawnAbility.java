package eclipse.type;

import arc.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import eclipse.content.*;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.type.*;

import static mindustry.Vars.*;

/**
 * Vanilla's unit spawning (the Behemoth breeding Mites), but each new unit is laid as an egg: Incubating for
 * {@link #incubation} ticks, so it can't move or attack and takes double damage. Splash can wipe a clutch before it hatches.
 */
public class EggSpawnAbility extends UnitSpawnAbility{
    public float incubation = 60f * 3f;

    public EggSpawnAbility(UnitType unit, float spawnTime, float spawnX, float spawnY){
        super(unit, spawnTime, spawnX, spawnY);
    }

    public EggSpawnAbility(){
    }

    @Override
    public void update(Unit unit){
        timer += Time.delta * state.rules.unitBuildSpeed(unit.team);
        if(timer < spawnTime || !Units.canCreate(unit.team, this.unit)) return;
        timer = 0f;
        float x = unit.x + Angles.trnsx(unit.rotation, spawnY, spawnX), y = unit.y + Angles.trnsy(unit.rotation, spawnY, spawnX);
        spawnEffect.at(x, y, 0f, parentizeEffects ? unit : null);
        Unit egg = this.unit.create(unit.team);
        egg.set(x, y);
        egg.rotation = unit.rotation;
        egg.apply(Munitions.incubating, incubation);
        Events.fire(new UnitCreateEvent(egg, null, unit));
        if(!net.client()) egg.add();
    }

    @Override
    public void addStats(Table t){
        super.addStats(t);
        t.row();
        t.add(Core.bundle.format("ability.eclipse-eggs.stats", Strings.autoFixed(incubation / 60f, 1)));
    }
}
