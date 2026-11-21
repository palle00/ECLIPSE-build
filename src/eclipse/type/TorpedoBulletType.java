package eclipse.type;

import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.world.*;

/**
 * A cavitation torpedo: it runs under the surface, so nothing can shoot it down, absorb or reflect it, but it only
 * travels through water. Where its path reaches land it detonates against the shore.
 */
public class TorpedoBulletType extends BasicBulletType{

    public TorpedoBulletType(float speed, float damage){
        super(speed, damage);
        hittable = false;
        absorbable = false;
        reflectable = false;
        collidesAir = false;
        keepVelocity = false;
        width = 5f;
        height = 14f;
        shrinkY = 0f;
        trailEffect = Fx.bubble;
        trailChance = 0.4f;
        despawnHit = true;
    }

    @Override
    public void update(Bullet b){
        super.update(b);
        Tile tile = b.tileOn();
        if(b.isAdded() && tile != null && !tile.floor().isLiquid){
            hit(b);
            b.remove();
        }
    }
}
