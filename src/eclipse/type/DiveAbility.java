package eclipse.type;

import arc.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import eclipse.content.*;
import mindustry.entities.abilities.*;
import mindustry.gen.*;

/**
 * A submarine dives whenever it is not attacking: it surfaces the moment it opens fire and stays up for
 * {@link #surfaceTime} after its last shot. A nuclear submarine ({@link #nuclear}) never has to surface: it fires
 * from below.
 */
public class DiveAbility extends Ability{
    public float surfaceTime = 120f;
    public boolean nuclear;
    protected float surfaced;

    public DiveAbility(boolean nuclear){
        this.nuclear = nuclear;
    }

    @Override
    public void update(Unit unit){
        if(!nuclear && unit.isShooting()){
            surfaced = surfaceTime;
            unit.unapply(Munitions.submerged);
            return;
        }
        surfaced -= Time.delta;
        if(nuclear || surfaced <= 0f) unit.apply(Munitions.submerged, 30f);
    }

    @Override
    public void addStats(Table t){
        super.addStats(t);
        t.add(nuclear ? Core.bundle.get("ability.eclipse-dive.nuclear") : Core.bundle.format("ability.eclipse-dive.stats", Strings.autoFixed(surfaceTime / 60f, 1)));
    }

    @Override
    public String localized(){
        return Core.bundle.get("ability.eclipse-dive");
    }
}
