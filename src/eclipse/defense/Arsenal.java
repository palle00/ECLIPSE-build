package eclipse.defense;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.blocks.defense.turrets.*;

/** Custom bullets and turret behaviour for the ECLIPSE anti-air set (Thunderhead, Cloudbreaker). */
public class Arsenal{

    /**
     * Thunderhead's discharge: hits one aircraft, then jumps to the nearest aircraft not hit yet (up to {@link #chains}
     * times within {@link #chainRange}), each jump dealing {@link #falloff} of the previous. Each hit applies {@link #status}.
     * Jump hits are reported like normal hits, so turret statistics count them.
     */
    public static class ChainBulletType extends BasicBulletType{
        public int chains = 4;
        public float chainRange = 64f, falloff = 0.75f;
        public Color chainColor = Color.valueOf("8fd8ff");

        public ChainBulletType(float speed, float damage){
            super(speed, damage);
            collidesGround = false;
            collidesTiles = false;
        }

        @Override
        public void hitEntity(Bullet b, Hitboxc entity, float health){
            super.hitEntity(b, entity, health);
            if(!(entity instanceof Unit first)) return;
            IntSet hit = new IntSet();
            hit.add(first.id);
            Unit from = first;
            float damage = b.damage;
            for(int i = 0; i < chains; i++){
                damage *= falloff;
                Unit next = Units.closestEnemy(b.team, from.x, from.y, chainRange, u -> u.isFlying() && !u.dead && !hit.contains(u.id));
                if(next == null) break;
                hit.add(next.id);
                Fx.chainLightning.at(from.x, from.y, 0f, chainColor, next);
                next.damage(damage);
                if(status != StatusEffects.none) next.apply(status, statusDuration);
                Events.fire(new UnitDamageEvent().set(next, b));
                if(next.health <= 0f) Events.fire(new UnitBulletDestroyEvent(next, b));
                from = next;
            }
        }
    }

    /**
     * Cloudbreaker's mine: thrown a short way, it brakes to a stop and floats. After {@link #armTime} it watches for
     * enemy aircraft within {@link #triggerRange}, then accelerates at the nearest one and bursts on contact (air only).
     * Unused mines fizzle out when their lifetime ends.
     */
    public static class AerialMineBulletType extends BasicBulletType{
        public float armTime = 45f, triggerRange = 44f, accel = 0.45f, maxSpeed = 6f;
        public Color lightColor = Color.valueOf("ff795e");

        public AerialMineBulletType(float damage){
            super(3.2f, damage);
            drag = 0.055f;
            lifetime = 60f * 20f;
            collidesGround = false;
            collidesTiles = false;
            collidesAir = true;
            hittable = false;
            absorbable = false;
            reflectable = false;
            keepVelocity = false;
            despawnHit = false;
            despawnEffect = Fx.smokePuff;
            hitEffect = Fx.flakExplosion;
            shrinkY = 0f;
        }

        @Override
        public void update(Bullet b){
            super.update(b);
            if(b.time < armTime) return;
            Unit target = Units.closestEnemy(b.team, b.x, b.y, triggerRange, Unit::isFlying);
            if(target == null) return;
            Tmp.v1.set(target).sub(b).setLength(accel * Time.delta);
            b.vel.add(Tmp.v1).limit(maxSpeed);
        }

        @Override
        public void draw(Bullet b){
            boolean armed = b.time >= armTime;
            Draw.color(Color.valueOf("4d4e58"));
            Fill.circle(b.x, b.y, 2.6f);
            Draw.color(Color.valueOf("8a8b94"));
            Fill.circle(b.x, b.y, 1.8f);
            //a blinking light once armed
            Draw.color(lightColor, armed ? 0.5f + Mathf.absin(Time.time + b.id, 6f, 0.5f) : 0.25f);
            Fill.circle(b.x, b.y, 0.9f);
            Draw.reset();
        }
    }

    /** An item turret that keeps at most {@link #maxMines} of its mines floating at once (Cloudbreaker). */
    public static class MineTurret extends ItemTurret{
        public int maxMines = 8;

        public MineTurret(String name){
            super(name);
        }

        public class MineTurretBuild extends ItemTurretBuild{
            int activeMines;
            float countTimer;

            /** Mines of this turret still in the air (recounted a few times a second). */
            public int activeMines(){
                return Groups.bullet.count(b -> b.owner == this && b.type instanceof AerialMineBulletType);
            }

            @Override
            protected void updateShooting(){
                if((countTimer -= Time.delta) <= 0f){
                    countTimer = 10f;
                    activeMines = activeMines();
                }
                if(activeMines >= maxMines) return;
                super.updateShooting();
            }

            @Override
            protected void shoot(BulletType type){
                super.shoot(type);
                activeMines += shoot.shots;
            }
        }
    }
}
