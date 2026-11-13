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
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.meta.*;

/**
 * Turret mechanics of the third turret set, one per tier, each something vanilla Serpulo has no turret for:
 * Rivet (spins up), Sickle (returning sawblade), Tremor (ground shockwave), Apiary (hovering drones),
 * Event Horizon (a singularity that pulls units in).
 */
public class Armory{
    public static final Stat spinStat = new Stat("eclipse-spin"), dronesStat = new Stat("eclipse-drones");

    /** Damages a unit as a bullet hit would, so turret statistics and kill credit count it. */
    static void hurt(Bullet b, Unit u, float damage){
        u.damage(damage);
        Events.fire(new UnitDamageEvent().set(u, b));
        if(u.health <= 0f && !u.dead) Events.fire(new UnitBulletDestroyEvent(u, b));
    }

    // ---- Rivet ----

    /**
     * An item turret that spins up: while it keeps firing, its fire rate climbs to {@link #maxSpeedup} times the base
     * over {@link #spinUpTime} ticks; with nothing to shoot it winds down again over {@link #spinDownTime}.
     */
    public static class SpinTurret extends ItemTurret{
        public float maxSpeedup = 3f, spinUpTime = 180f, spinDownTime = 120f;

        public SpinTurret(String name){
            super(name);
        }

        @Override
        public void setStats(){
            super.setStats();
            stats.add(spinStat, Core.bundle.format("stat.eclipse-spin-value", Strings.autoFixed(maxSpeedup, 1), Strings.autoFixed(spinUpTime / 60f, 1)));
        }

        public class SpinTurretBuild extends ItemTurretBuild{
            /** 0 = idle, 1 = full speed. */
            public float spin;

            @Override
            public void updateTile(){
                boolean firing = isShooting() && hasAmmo() && efficiency > 0f;
                spin = firing ? Math.min(spin + delta() / spinUpTime, 1f) : Math.max(spin - delta() / spinDownTime, 0f);
                super.updateTile();
            }

            @Override
            protected float baseReloadSpeed(){
                return super.baseReloadSpeed() * (1f + spin * (maxSpeedup - 1f));
            }

            @Override
            public void draw(){
                super.draw();
                //barrel blur and a hot glow that grow with the spin
                if(spin > 0.05f){
                    Tmp.v1.trns(rotation, shootY - 2f - curRecoil);
                    Draw.z(Layer.turret + 0.1f);
                    Draw.color(Pal.turretHeat, spin * 0.6f);
                    Lines.stroke(1.2f);
                    Lines.circle(x + recoilOffset.x + Tmp.v1.x, y + recoilOffset.y + Tmp.v1.y, 1.5f + spin * 1.5f + Mathf.absin(Time.time, 1.5f, 0.6f));
                    Draw.reset();
                }
            }

            @Override
            public void write(arc.util.io.Writes write){
                super.write(write);
                write.f(spin);
            }

            @Override
            public void read(arc.util.io.Reads read, byte revision){
                super.read(read, revision);
                if(revision >= 3) spin = read.f();
            }

            @Override
            public byte version(){
                return 3;
            }
        }
    }

    // ---- Sickle ----

    /**
     * A spinning sawblade: flies out to {@link #turnAt} of its lifetime, then curves back to the turret that threw it and
     * is caught there. It cuts through every ground unit on its path, both ways (each unit once per pass).
     */
    public static class BoomerangBulletType extends BasicBulletType{
        public float turnAt = 0.45f, returnAccel = 0.55f, radius = 5f;
        public Color bladeColor = Color.valueOf("c8ccd8"), edgeColor = Color.valueOf("7a7c88");

        public BoomerangBulletType(float speed, float damage){
            super(speed, damage);
            pierce = true;
            pierceBuilding = false;
            collidesAir = false;
            collidesTiles = false;
            hittable = false;
            reflectable = false;
            absorbable = false;
            keepVelocity = false;
            despawnEffect = Fx.none;
            hitEffect = Fx.hitBulletSmall;
            hitSize = 9f;
            shrinkY = 0f;
        }

        @Override
        public void update(Bullet b){
            super.update(b);
            if(b.time < b.lifetime * turnAt) return;
            //the turn: forget who was cut on the way out, so the way back cuts them again
            if(b.data != Boolean.TRUE){
                b.data = Boolean.TRUE;
                b.collided.clear();
            }
            if(!(b.owner instanceof Building home) || !home.isValid()) return;
            float speed = b.vel.len();
            Tmp.v1.set(home.x - b.x, home.y - b.y);
            if(Tmp.v1.len() < Math.max(speed, 6f)){
                b.remove();
                return;
            }
            //steer home; never slower than it was thrown, and never runs out of life on the way
            b.vel.add(Tmp.v1.setLength(returnAccel * Time.delta)).limit(this.speed * 1.2f);
            if(b.vel.len() < this.speed * 0.6f) b.vel.setLength(this.speed * 0.6f);
            b.time = Math.min(b.time, b.lifetime - 2f);
        }

        @Override
        public void draw(Bullet b){
            float rot = b.time * 18f;
            Draw.color(edgeColor);
            for(int i = 0; i < 6; i++) Drawf.tri(b.x, b.y, 3.5f, radius + 3f, rot + i * 60f);
            Draw.color(bladeColor);
            Fill.circle(b.x, b.y, radius);
            Draw.color(edgeColor);
            Fill.circle(b.x, b.y, radius * 0.35f);
            Draw.reset();
        }
    }

    // ---- Tremor ----

    /**
     * A ground shockwave from the turret's centre: a ring that grows to {@link #radius} over the bullet's lifetime and
     * hits every enemy ground unit it passes once, pushing it away from the turret and slowing it. Aircraft, buildings
     * and bullets are unaffected.
     */
    public static class ShockwaveBulletType extends BulletType{
        /** knockback is an impulse: a dagger is thrown about two tiles, heavier units less. */
        public float radius = 120f, knockback = 1200f, ringWidth = 10f;
        public Color waveColor = Color.valueOf("d9b88a");

        public ShockwaveBulletType(float damage){
            super(0f, damage);
            lifetime = 40f;
            collides = false;
            collidesAir = false;
            collidesTiles = false;
            hittable = false;
            absorbable = false;
            reflectable = false;
            keepVelocity = false;
            despawnEffect = Fx.none;
            hitEffect = Fx.none;
            shootEffect = Fx.none;
            smokeEffect = Fx.none;
            status = StatusEffects.slow;
            statusDuration = 120f;
        }

        @Override
        protected float calculateRange(){
            return radius;
        }

        @Override
        public void init(Bullet b){
            super.init(b);
            b.data = new IntSet();
        }

        @Override
        public void update(Bullet b){
            if(!(b.data instanceof IntSet hit)) return;
            float r = radius * b.fin();
            Units.nearbyEnemies(b.team, b.x, b.y, r, u -> {
                if(u.isFlying() || !u.checkTarget(false, true) || hit.contains(u.id) || u.dst(b) < r - ringWidth - u.hitSize / 2f) return;
                hit.add(u.id);
                hurt(b, u, b.damage);
                u.apply(status, statusDuration);
                u.impulseNet(Tmp.v1.set(u.x - b.x, u.y - b.y).setLength(knockback));
                Fx.hitBulletSmall.at(u.x, u.y, 0f, waveColor);
            });
        }

        @Override
        public void draw(Bullet b){
            float r = radius * b.fin();
            Draw.color(waveColor, 0.75f * b.fout());
            Lines.stroke(3.5f * b.fout() + 0.5f);
            Lines.circle(b.x, b.y, r);
            Draw.color(waveColor, 0.25f * b.fout());
            Lines.stroke(ringWidth * b.fout());
            Lines.circle(b.x, b.y, Math.max(r - ringWidth / 2f, 0f));
            Draw.reset();
        }
    }

    // ---- Apiary ----

    /**
     * A drone: flies to the turret's target, then circles over it for its lifetime, firing {@link #bolt} at the nearest
     * enemy (ground or air) within {@link #fireRange} every {@link #fireInterval} ticks. It cannot be shot down or hit
     * anything itself. When its target dies it moves on to the nearest enemy within {@link #seekRange}.
     */
    public static class DroneBulletType extends BulletType{
        public BulletType bolt;
        public float fireInterval = 30f, fireRange = 90f, seekRange = 160f, orbitRadius = 22f, accel = 0.25f, maxSpeed = 3.2f;
        public Color lightColor = Color.valueOf("f8c166");

        public DroneBulletType(BulletType bolt){
            super(3f, 0f);
            this.bolt = bolt;
            lifetime = 60f * 10f;
            collides = false;
            collidesAir = false;
            collidesGround = false;
            collidesTiles = false;
            hittable = false;
            absorbable = false;
            reflectable = false;
            keepVelocity = false;
            despawnEffect = Fx.smokePuff;
            hitEffect = Fx.none;
            shootEffect = Fx.shootSmall;
            drag = 0f;
        }

        @Override
        protected float calculateRange(){
            return seekRange;
        }

        @Override
        public void update(Bullet b){
            Teamc target = b.data instanceof Teamc t && (!(t instanceof Healthc h) || h.isValid()) ? t : null;
            if(target == null || b.timer(1, 20f) && !target.within(b, seekRange)){
                target = Units.closestTarget(b.team, b.x, b.y, seekRange, u -> u.checkTarget(true, true));
                b.data = target;
            }
            //circle the target (or hang around where it was)
            float cx = target == null ? b.aimX : target.getX(), cy = target == null ? b.aimY : target.getY();
            float ang = b.time * 2.5f + b.id * 47f;
            Tmp.v1.set(cx + Angles.trnsx(ang, orbitRadius) - b.x, cy + Angles.trnsy(ang, orbitRadius) - b.y);
            b.vel.add(Tmp.v1.limit(1f).scl(accel * Time.delta)).limit(maxSpeed);
            b.vel.scl(1f - 0.02f * Time.delta);

            if(b.timer(2, fireInterval)){
                Unit shot = Units.closestEnemy(b.team, b.x, b.y, fireRange, u -> u.checkTarget(true, true));
                if(shot != null) bolt.create(b.owner, b.team, b.x, b.y, b.angleTo(shot));
            }
        }

        @Override
        public void draw(Bullet b){
            float rot = b.vel.angle() - 90f;
            Draw.color(Color.valueOf("4d4e58"));
            Fill.poly(b.x, b.y, 4, 4.5f, rot + 45f);
            Draw.color(Color.valueOf("8a8b94"));
            Fill.poly(b.x, b.y, 4, 3f, rot + 45f);
            //four rotor blurs
            Draw.color(Color.valueOf("c8ccd8"), 0.45f);
            for(int i = 0; i < 4; i++){
                Tmp.v1.trns(rot + 45f + i * 90f, 4.5f);
                Fill.circle(b.x + Tmp.v1.x, b.y + Tmp.v1.y, 2f + Mathf.absin(Time.time + i * 3f, 1f, 0.5f));
            }
            Draw.color(b.team.color.cpy().lerp(lightColor, 0.4f));
            Fill.circle(b.x, b.y, 1.3f);
            Draw.reset();
        }
    }

    /** An item turret that keeps at most {@link #maxDrones} of its drones in the air at once (Apiary). */
    public static class DroneTurret extends ItemTurret{
        public int maxDrones = 6;

        public DroneTurret(String name){
            super(name);
        }

        @Override
        public void setStats(){
            super.setStats();
            stats.add(dronesStat, maxDrones);
        }

        public class DroneTurretBuild extends ItemTurretBuild{
            int active;
            float countTimer;

            public int activeDrones(){
                return Groups.bullet.count(b -> b.owner == this && b.type instanceof DroneBulletType);
            }

            @Override
            protected void updateShooting(){
                if((countTimer -= Time.delta) <= 0f){
                    countTimer = 10f;
                    active = activeDrones();
                }
                if(active >= maxDrones) return;
                super.updateShooting();
            }

            @Override
            protected void shoot(mindustry.entities.bullet.BulletType type){
                super.shoot(type);
                active += shoot.shots;
            }
        }
    }

    // ---- Event Horizon ----

    /**
     * A singularity: drifts to the point it was aimed at and stops there, pulling every enemy unit (ground and air)
     * within {@link #pullRadius} toward it and hurting them, then collapses at the end of its life in a blast
     * (splash damage). Heavier units are pulled less.
     */
    public static class SingularityBulletType extends BulletType{
        /** Pull per tick at the centre (impulse, so heavier units move less), and damage every tickInterval ticks at the centre. */
        public float pullRadius = 72f, pullForce = 30f, tickDamage = 6f, tickInterval = 10f;
        public Color glowColor = Color.valueOf("7fe0ff");

        public SingularityBulletType(float speed, float collapseDamage){
            super(speed, 0f);
            splashDamage = collapseDamage;
            splashDamageRadius = 64f;
            lifetime = 60f * 5f;
            collides = false;
            collidesTiles = false;
            hittable = false;
            absorbable = false;
            reflectable = false;
            keepVelocity = false;
            despawnHit = true;
            hitEffect = Fx.none;
            despawnEffect = Fx.none;
            hitSound = Sounds.explosionArtilleryShockBig;
            hitShake = 6f;
            lightRadius = 60f;
            lightColor = glowColor;
            shootEffect = Fx.lancerLaserShoot;
            smokeEffect = Fx.none;
            scaledSplashDamage = true;
        }

        @Override
        protected float calculateRange(){
            return speed * lifetime * 0.5f;
        }

        @Override
        public void update(Bullet b){
            //travel to the aim point, then hold there
            if(b.vel.len() > 0.01f && (b.within(b.aimX, b.aimY, b.vel.len() * 1.5f) || b.time > lifetime * 0.5f)) b.vel.setZero();
            float grow = Mathf.clamp(b.time / 40f);
            boolean tick = b.timer(3, tickInterval);
            Units.nearbyEnemies(b.team, b.x, b.y, pullRadius * grow, u -> {
                if(!u.checkTarget(true, true)) return;
                float strength = 1f - u.dst(b) / (pullRadius + 1f);
                u.impulseNet(Tmp.v1.set(b.x - u.x, b.y - u.y).setLength(pullForce * strength * Time.delta));
                if(tick) hurt(b, u, tickDamage * Mathf.lerp(0.4f, 1f, strength));
            });
            if(Mathf.chanceDelta(0.4f)){
                Tmp.v1.rnd(pullRadius * grow * Mathf.random(0.6f, 1f));
                Fx.hitLaserBlast.at(b.x + Tmp.v1.x, b.y + Tmp.v1.y, 0f, glowColor);
            }
        }

        @Override
        public void despawned(Bullet b){
            super.despawned(b);
            Fx.dynamicExplosion.at(b.x, b.y, splashDamageRadius / 8f / 2f);
            Fx.shockwave.at(b.x, b.y);
        }

        @Override
        public void draw(Bullet b){
            float grow = Mathf.clamp(b.time / 40f), r = 4f + 6f * grow;
            //accretion swirl
            Draw.color(glowColor, 0.35f);
            Lines.stroke(1.5f);
            for(int i = 0; i < 3; i++) Lines.arc(b.x, b.y, r + 6f + i * 5f, 0.3f, -b.time * (4f - i) + i * 120f);
            Draw.color(glowColor);
            Fill.circle(b.x, b.y, r + 1.5f + Mathf.absin(Time.time, 3f, 1f));
            Draw.color(Color.black);
            Fill.circle(b.x, b.y, r);
            Draw.reset();
            Drawf.light(b.x, b.y, lightRadius * grow, glowColor, 0.7f);
        }
    }
}
