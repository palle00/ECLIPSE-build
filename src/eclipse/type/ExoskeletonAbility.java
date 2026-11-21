package eclipse.type;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import eclipse.content.*;
import mindustry.entities.abilities.*;
import mindustry.gen.*;
import mindustry.graphics.*;

/**
 * The Swarm's exoskeleton: while the unit is at full health, its regeneration grows a hard outer layer instead (the
 * unit's shield, up to {@link #max}) that takes damage first. While any of it holds the unit is Exoskeleton (20% less
 * damage). Unlike a vanilla shield it only grows at full health, so steady fire keeps it from coming back; acid
 * (Etched) stops it growing at all.
 */
public class ExoskeletonAbility extends Ability{
    public static final Color chitin = Color.valueOf("d8c48a");

    /** The thickest the exoskeleton gets, and how fast it grows per tick at full health. */
    public float max, growth;

    public ExoskeletonAbility(float max, float growth){
        this.max = max;
        this.growth = growth;
    }

    public ExoskeletonAbility(){
    }

    @Override
    public void update(Unit unit){
        if(unit.health >= unit.maxHealth - 0.001f && unit.shield < max && !unit.hasEffect(Munitions.etched)){
            unit.shield = Math.min(max, unit.shield + growth * Time.delta);
        }
        if(unit.shield > 0f) unit.apply(Munitions.exoskeleton, 10f);
    }

    @Override
    public void draw(Unit unit){
        if(unit.shield <= 0f) return;
        //chitin plates around the body, more of them (and brighter) the thicker it is
        float f = Mathf.clamp(unit.shield / max), r = unit.hitSize * 0.62f;
        int plates = 8, shown = Math.max(1, Mathf.ceil(plates * f));
        float z = Draw.z();
        Draw.z(Layer.groundUnit + 0.9f);
        Lines.stroke(Math.max(1f, unit.hitSize / 14f));
        Draw.color(chitin, 0.35f + 0.4f * f);
        for(int i = 0; i < shown; i++){
            float a = unit.rotation + 360f / plates * i + 360f / plates * 0.15f;
            Lines.arc(unit.x, unit.y, r, 1f / plates * 0.7f, a);
        }
        Draw.reset();
        Draw.z(z);
    }

    @Override
    public void addStats(Table t){
        super.addStats(t);
        t.add(Core.bundle.format("ability.eclipse-exoskeleton.stats", (int)max, Strings.autoFixed(growth * 60f, 1)));
    }

    @Override
    public String localized(){
        return Core.bundle.get("ability.eclipse-exoskeleton");
    }
}
