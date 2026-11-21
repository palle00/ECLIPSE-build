package eclipse.type;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;

/**
 * A mine. Besides going off under the first ground unit it touches (a normal collision), it also goes off under a
 * submerged submarine, which bullets otherwise pass through, and its blast reaches submerged submarines too: mines
 * are the one weapon that works on a sub nobody can see.
 */
public class MineBulletType extends BasicBulletType{
    static final Color armed = Color.valueOf("ff5a3d");

    public MineBulletType(float speed, float damage, String sprite){
        super(speed, damage, sprite);
    }

    @Override
    public void update(Bullet b){
        super.update(b);
        if(!b.isAdded()) return;
        //nearbyEnemies, not closestEnemy: the latter skips untargetable units, which is exactly what a hidden sub is
        boolean[] under = {false};
        Units.nearbyEnemies(b.team, b.x, b.y, hitSize + 4f, u -> under[0] |= SubmarineUnitType.hidden(u));
        if(under[0]){
            hit(b);
            b.remove();
        }
    }

    /** The armed light in the middle of the mine blinks (each mine at its own pace). */
    @Override
    public void draw(Bullet b){
        super.draw(b);
        Draw.z(layer + 0.01f);
        Draw.blend(Blending.additive);
        Draw.color(armed, Mathf.absin(Time.time + b.id * 13f, 7f, 1f));
        Fill.circle(b.x, b.y, 1.1f);
        Draw.blend();
        Draw.reset();
    }

    @Override
    public void hit(Bullet b, float x, float y, boolean createFrags){
        super.hit(b, x, y, createFrags);
        //the normal blast skips units nothing can hit; a mine's reaches submerged subs as well
        Units.nearbyEnemies(b.team, x, y, splashDamageRadius, u -> {
            if(SubmarineUnitType.hidden(u)) u.damage(splashDamage);
        });
    }
}
