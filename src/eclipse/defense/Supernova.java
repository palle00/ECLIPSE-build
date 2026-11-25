package eclipse.defense;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

import static mindustry.Vars.*;

/**
 * The Supernova: two turrets that work together.
 * - The Stardust Launcher fires any of a range of items. Each shot is weak, but where it lands it leaves a cloud of
 *   stardust in the item's colour that hangs there for a few seconds (so it can't pile up forever).
 * - The Primer fires a weak shot that leaves a mark; a few seconds later the mark detonates, pulls in every friendly
 *   stardust cloud near it and explodes. The more clouds, the bigger and stronger the blast, and each item adds its own
 *   traits, read from the item's properties: explosive items hit harder, flammable ones start fires, charged ones throw
 *   lightning, radioactive ones add damage; plastanium bursts into fragments and spore pods slow whatever survives.
 * A Primer alone only makes a small bang; it is the Launchers around it that make it strong.
 */
public class Supernova{
    /** How long a stardust cloud lasts, in ticks. */
    public static final float cloudLife = 60f * 4f;

    /** The pull-in streak from a cloud to the detonation; data is the cloud's position, colour is the item's. */
    public static final Effect streak = new Effect(22f, 300f, e -> {
        if(!(e.data instanceof float[] from)) return;
        Draw.color(e.color, Color.white, e.fin() * 0.5f);
        float px = Mathf.lerp(from[0], e.x, e.finpow()), py = Mathf.lerp(from[1], e.y, e.finpow());
        Lines.stroke(2.2f * e.fout());
        Lines.line(px, py, Mathf.lerp(px, e.x, 0.35f), Mathf.lerp(py, e.y, 0.35f));
        Fill.circle(px, py, 2.4f * e.fout());
    }).layer(Layer.bullet + 1f);

    /** The blast: a white core, a ring in the debris colours and a fading nebula; rotation is the radius. */
    public static final Effect blast = new Effect(70f, 500f, e -> {
        float r = e.rotation;
        Draw.color(Color.white, e.color, e.fin());
        Fill.circle(e.x, e.y, r * 0.35f * e.fout());
        Draw.color(e.color, Color.white, 0.25f * e.fout());
        Draw.alpha(0.55f * e.fout());
        Fill.circle(e.x, e.y, r * (0.4f + 0.6f * e.finpow()));
        Draw.color(e.color);
        Lines.stroke(4f * e.fout() + 0.5f);
        Lines.circle(e.x, e.y, r * e.finpow());
        Angles.randLenVectors(e.id, 24, r * e.finpow(), (x, y) -> {
            Draw.color(e.color, Color.white, Mathf.randomSeed(e.id + (long)(x * 31f), 0f, 0.6f));
            Fill.circle(e.x + x, e.y + y, 2.2f * e.fout());
        });
        Drawf.light(e.x, e.y, r * 1.6f, e.color, 0.8f * e.fout());
    });

    // ---- Stardust Launcher ----

    /** A weak shot that leaves a stardust cloud of its item wherever it ends: on a hit, or when it runs out. */
    public static class DebrisShotBulletType extends BasicBulletType{
        public final StardustBulletType cloud;

        public DebrisShotBulletType(Item item, float damage){
            super(3.4f, damage);
            cloud = new StardustBulletType(item);
            lifetime = 70f;
            width = 7f;
            height = 9f;
            despawnHit = true;
            frontColor = item.color.cpy().lerp(Color.white, 0.4f);
            backColor = item.color;
            hitColor = item.color;
            trailColor = item.color;
            trailLength = 5;
            trailWidth = 1.6f;
            hitEffect = Fx.hitBulletSmall;
            despawnEffect = Fx.none;
        }

        @Override
        public void hit(Bullet b, float x, float y, boolean createFrags){
            super.hit(b, x, y, createFrags);
            cloud.create(b.owner, b.team, x, y, 0f);
        }
    }

    /** A drifting cloud of one item's dust; inert on its own, fuel for the Primer's blast. */
    public static class StardustBulletType extends BulletType{
        public final Item item;

        public StardustBulletType(Item item){
            super(0f, 0f);
            this.item = item;
            lifetime = cloudLife;
            collides = false;
            collidesGround = false;
            collidesAir = false;
            collidesTiles = false;
            hittable = false;
            absorbable = false;
            reflectable = false;
            keepVelocity = false;
            despawnHit = false;
            hitEffect = Fx.none;
            despawnEffect = Fx.none;
            shootEffect = Fx.none;
            smokeEffect = Fx.none;
            layer = Layer.bullet - 2f;
        }

        @Override
        public void draw(Bullet b){
            //a sparkling cloud in the item's colour that fades in fast and out slowly
            float alpha = Math.min(b.time / 12f, 1f) * Mathf.clamp(b.fout() * 3f);
            Mathf.rand.setSeed(b.id);
            for(int i = 0; i < 9; i++){
                float ang = Mathf.rand.random(360f) + b.time * Mathf.rand.range(0.6f), len = Mathf.rand.random(2f, 9f);
                float size = Mathf.rand.random(1f, 2.4f) * (0.7f + 0.3f * Mathf.absin(b.time + i * 9f, 5f, 1f));
                Draw.color(item.color, Color.white, Mathf.rand.random(0.5f));
                Draw.alpha(alpha * 0.9f);
                Fill.circle(b.x + Angles.trnsx(ang, len), b.y + Angles.trnsy(ang, len), size);
            }
            Draw.color(item.color);
            Draw.alpha(alpha * 0.18f);
            Fill.circle(b.x, b.y, 10f);
            Draw.reset();
        }
    }

    // ---- Primer ----

    /** The Primer's shot: weak, and it leaves a fuse mark where it ends. */
    public static class PrimerBulletType extends BasicBulletType{
        public final FuseBulletType fuse;

        public PrimerBulletType(float speed, float damage, FuseBulletType fuse){
            super(speed, damage);
            this.fuse = fuse;
            despawnHit = true;
            width = 8f;
            height = 12f;
            frontColor = Color.white;
            backColor = Color.valueOf("ffb46b");
            trailColor = backColor;
            trailLength = 8;
            trailWidth = 2f;
            despawnEffect = Fx.none;
        }

        @Override
        public void hit(Bullet b, float x, float y, boolean createFrags){
            super.hit(b, x, y, createFrags);
            fuse.create(b.owner, b.team, x, y, 0f);
        }
    }

    /** The mark the Primer leaves: counts down, then gathers every friendly stardust cloud in reach and explodes. */
    public static class FuseBulletType extends BulletType{
        /** How far it reaches for clouds, and the most clouds one blast takes. */
        public float pullRadius = 88f;
        public int maxClouds = 60;
        /** The blast with no clouds, and how much each cloud adds to the radius (capped). */
        public float baseDamage = 100f, baseRadius = 24f, radiusPerCloud = 3f, maxRadius = 128f;
        public BulletType fragment;

        public FuseBulletType(float fuseTime){
            super(0f, 0f);
            lifetime = fuseTime;
            collides = false;
            collidesGround = false;
            collidesAir = false;
            collidesTiles = false;
            hittable = false;
            absorbable = false;
            reflectable = false;
            keepVelocity = false;
            despawnHit = false;
            hitEffect = Fx.none;
            despawnEffect = Fx.none;
            despawnSound = Sounds.explosionArtilleryShockBig;
            layer = Layer.bullet - 1f;
        }

        /** What one cloud of this item adds to the blast's damage. */
        public static float cloudDamage(Item item){
            return 2.5f * (10f + 60f * item.explosiveness + 20f * item.flammability + 30f * item.radioactivity + 25f * item.charge + 3f * item.hardness);
        }

        @Override
        public void despawned(Bullet b){
            super.despawned(b);
            Seq<Bullet> clouds = new Seq<>();
            Groups.bullet.intersect(b.x - pullRadius, b.y - pullRadius, pullRadius * 2f, pullRadius * 2f, o -> {
                if(o.team == b.team && o.type instanceof StardustBulletType && o.within(b, pullRadius) && clouds.size < maxClouds) clouds.add(o);
            });

            float damage = baseDamage, fire = 0f, charge = 0f;
            boolean frag = false, spores = false, blasted = false;
            Color mix = new Color(0f, 0f, 0f, 1f);
            for(Bullet c : clouds){
                Item item = ((StardustBulletType)c.type).item;
                damage += cloudDamage(item);
                fire += item.flammability;
                charge += item.charge;
                frag |= item == Items.plastanium;
                spores |= item == Items.sporePod;
                blasted |= item == Items.blastCompound;
                mix.r += item.color.r;
                mix.g += item.color.g;
                mix.b += item.color.b;
                streak.at(b.x, b.y, 0f, item.color, new float[]{c.x, c.y});
                c.remove();
            }
            if(clouds.isEmpty()) mix.set(Color.valueOf("ffb46b"));
            else mix.mul(1f / clouds.size, 1f / clouds.size, 1f / clouds.size, 1f);
            mix.a = 1f;

            float radius = Math.min(baseRadius + clouds.size * radiusPerCloud, maxRadius);
            Damage.damage(b.team, b.x, b.y, radius, damage, false, true, true, true, b);
            blast.at(b.x, b.y, radius, mix);
            Effect.shake(Math.min(2f + clouds.size * 0.3f, 10f), 30f, b);

            //traits
            if(spores) Units.nearbyEnemies(b.team, b.x, b.y, radius, u -> u.apply(StatusEffects.sporeSlowed, 60f * 4f));
            //blast compound marks with Blasted, as vanilla's blast ammo (players' report, 2026-11-25)
            if(blasted) Units.nearbyEnemies(b.team, b.x, b.y, radius, u -> u.apply(StatusEffects.blasted));
            if(!net.client()){
                for(int i = 0; i < Math.min((int)(fire * 1.5f), 14); i++){
                    Tmp.v1.rnd(Mathf.random(radius));
                    Fires.create(world.tileWorld(b.x + Tmp.v1.x, b.y + Tmp.v1.y));
                }
            }
            for(int i = 0; i < Math.min((int)(charge * 2f), 10); i++) Lightning.create(b, Color.valueOf("f3e979"), 18f, b.x, b.y, Mathf.random(360f), (int)(radius / 6f));
            if(frag && fragment != null){
                for(int i = 0; i < 10; i++) fragment.create(b, b.x, b.y, i * 36f + Mathf.range(10f));
            }
        }

        @Override
        public void draw(Bullet b){
            //a ticking mark: a ring that tightens and blinks faster as the fuse runs down, and the reach it pulls from
            float blink = Mathf.absin(Time.time, 2f + 6f * b.fout(), 1f);
            Draw.color(Color.valueOf("ffb46b"), Color.white, blink);
            Lines.stroke(1.8f);
            Lines.circle(b.x, b.y, 4f + 8f * b.fout());
            Fill.circle(b.x, b.y, 2.2f);
            Draw.alpha(0.25f);
            Lines.stroke(1f);
            Lines.dashCircle(b.x, b.y, pullRadius);
            Draw.reset();
        }
    }
}
