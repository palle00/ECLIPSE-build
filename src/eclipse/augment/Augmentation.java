package eclipse.augment;

import arc.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import eclipse.augment.AugmentationCoupler.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.defense.turrets.Turret.*;

import static mindustry.Vars.*;

/**
 * Central augmentation controller. Tracks which couplers serve which machine and combines all their modules into
 * one {@link Effects} per machine, so bonuses are calculated once instead of being stacked independently per coupler.
 * <p>
 * Stacking rules: the strongest module of a kind counts fully, each further one half as much as the previous
 * (diminishing returns), up to a cap. Speed penalties add up without diminishing. Within an exclusive group only one
 * module family may be active per machine (the first attached wins); unique and legendary modules work once per
 * machine. Blocked modules are reported as conflicting.
 * <p>
 * Links change only on events (target selected, coupler or machine removed, world loaded); there is no map scan.
 * Military bullet effects are applied in one pass over new bullets per tick, only while armed turrets exist.
 */
public class Augmentation{
    public static final float diminishing = 0.5f;
    public static final float maxSpeedBonus = 1.5f, maxPowerReduction = 0.5f, maxYield = 0.5f, maxSaving = 0.5f, maxRecycle = 0.3f,
        maxThermal = 0.75f, maxPressure = 2f, maxCatalystLife = 2f, maxLiquidYield = 0.5f, maxWasteRecovery = 0.6f,
        maxThermalRecovery = 0.5f, maxDamage = 0.6f, maxAccuracy = 1f, maxProjectileSpeed = 1f, maxCooling = 3f;

    static final ObjectMap<Building, Seq<CouplerBuild>> links = new ObjectMap<>();
    static final ObjectMap<Building, Effects> effects = new ObjectMap<>();
    /** Turrets whose bullets need adjusting. */
    static final ObjectSet<Building> armed = new ObjectSet<>();
    /** Ids of augmented turrets' bullets already adjusted (a pooled bullet gets a new id for every life). */
    static IntSet adjusted = new IntSet(), alive = new IntSet();
    /** Bullets adjusted per turret (building id); for diagnostics and tests. */
    public static final IntIntMap adjustedBullets = new IntIntMap();

    /** Combined effect of every active module on one machine. */
    public static class Effects{
        /** Net speed change (bonuses minus penalties), e.g. 0.15 = +15%, -0.2 = -20%. */
        public float speed, powerReduction, yield, inputSaving, recycle;
        public float thermal, pressure, catalystLife, liquidYield, wasteRecovery;
        public float thermalRecovery, capacitor;
        public float damage, accuracy, projectileSpeed, cooling;
        public boolean analyzer;
        /** Modules that are attached but blocked (exclusive group or unique). */
        public final ObjectSet<AugmentModule> conflicting = new ObjectSet<>();

        public boolean military(){
            return damage > 0f || accuracy > 0f || projectileSpeed > 0f;
        }
    }

    static final Effects none = new Effects();

    public static void init(){
        //buildings from the previous world are gone; couplers re-link themselves from their saved target
        Events.on(WorldLoadEvent.class, e -> {
            links.clear();
            effects.clear();
            armed.clear();
            adjusted.clear();
        });
        Events.run(Trigger.update, Augmentation::updateBullets);
        //lasers and point-blank shots hit before updateBullets sees them: add their damage bonus on the hit itself
        Events.on(UnitDamageEvent.class, e -> {
            float extra = instantBonus(e.bullet);
            if(extra <= 0f || e.unit.dead) return;
            //armor counts once, as if the boosted shot had hit (same armor rules as the bullet's own hit)
            if(!e.bullet.type.pierceArmor){
                float base = e.bullet.damage, armor = (e.unit.armorOverride >= 0f ? e.unit.armorOverride : e.unit.armor) * e.bullet.type.armorMultiplier;
                extra = Damage.applyArmor(base + extra, armor) - Damage.applyArmor(base, armor);
            }
            e.unit.damagePierce(extra);
        });
        Events.on(BuildDamageEvent.class, e -> {
            float extra = instantBonus(e.source);
            if(extra <= 0f || e.build.dead) return;
            float mult = e.source.type.buildingDamageMultiplier, base = e.source.damage * mult;
            extra *= mult;
            if(!e.source.type.pierceArmor) extra = Damage.applyArmor(base + extra, e.build.block.armor) - Damage.applyArmor(base, e.build.block.armor);
            e.build.damage(extra);
        });
    }

    /** Extra damage for a hit by an augmented turret's shot that has not been adjusted yet (lasers deal all their damage when fired). */
    static float instantBonus(Bullet b){
        if(armed.isEmpty() || b == null || !(b.owner instanceof TurretBuild t) || !armed.contains(t) || adjusted.contains(b.id) || !firedBy(t, b.type)) return 0f;
        return b.damage * effects(t).damage;
    }

    public static void link(CouplerBuild coupler, Building target){
        Seq<CouplerBuild> list = links.get(target, Seq::new);
        if(!list.contains(coupler)) list.add(coupler);
    }

    public static void unlink(CouplerBuild coupler, Building target){
        Seq<CouplerBuild> list = links.get(target);
        if(list == null) return;
        list.remove(coupler);
        if(list.isEmpty()){
            links.remove(target);
            effects.remove(target);
            armed.remove(target);
        }
    }

    public static Seq<CouplerBuild> couplers(Building target){
        Seq<CouplerBuild> list = links.get(target);
        return list == null ? Seq.with() : list;
    }

    /** The coupler that applies the combined effects for this machine (the first linked one). */
    public static boolean isLead(CouplerBuild coupler, Building target){
        Seq<CouplerBuild> list = links.get(target);
        return list != null && list.first() == coupler;
    }

    /** Last calculated effects for a machine (recalculated by its lead coupler every update). */
    public static Effects effects(Building target){
        return effects.get(target, none);
    }

    /** Whether this turret's bullets are currently being adjusted by military modules. */
    public static boolean isArmed(Building turret){
        return armed.contains(turret);
    }

    public static float speedBonus(Building target){
        return effects(target).speed;
    }

    public static float powerReduction(Building target){
        return effects(target).powerReduction;
    }

    static final ObjectMap<String, FloatSeq> values = new ObjectMap<>();
    static final ObjectMap<String, String> groupOwner = new ObjectMap<>();
    static final ObjectSet<String> uniqueSeen = new ObjectSet<>();

    static void add(String key, float value){
        if(value > 0f) values.get(key, FloatSeq::new).add(value);
    }

    /** Recalculates the combined effects for a machine. Called by its lead coupler. */
    public static Effects recalculate(Building target){
        Effects e = effects.get(target, Effects::new);
        e.conflicting.clear();
        for(FloatSeq s : values.values()) s.clear();
        groupOwner.clear();
        uniqueSeen.clear();
        float penalty = 0f;
        boolean analyzer = false;

        for(CouplerBuild c : couplers(target)){
            for(AugmentModule m : c.workingModules()){
                if(m.unique || m.legendary){
                    if(!uniqueSeen.add(m.family)){
                        e.conflicting.add(m);
                        continue;
                    }
                }
                if(m.exclusiveGroup != null){
                    String owner = groupOwner.get(m.exclusiveGroup);
                    if(owner == null){
                        groupOwner.put(m.exclusiveGroup, m.family);
                    }else if(!owner.equals(m.family)){
                        e.conflicting.add(m);
                        continue;
                    }
                }
                add("speed", m.speedBonus);
                if(c.overchargeActive()) add("speed", m.overchargeBonus);
                add("power", m.powerReduction);
                add("yield", m.yieldBonus);
                add("saving", m.inputSaving);
                add("recycle", m.recycleFraction);
                add("thermal", m.thermalSupply);
                add("pressure", m.pressureBoost);
                add("catalyst", m.catalystLife);
                add("liquid", m.liquidYield);
                add("waste", m.wasteRecovery);
                add("recovery", m.thermalRecovery);
                add("capacitor", m.capacitorCapacity);
                add("damage", m.damageBonus);
                add("accuracy", m.accuracy);
                add("projectile", m.projectileSpeed);
                add("cooling", m.coolingBonus);
                penalty += m.speedPenalty;
                analyzer |= m.analyzer;
            }
        }

        e.speed = combine("speed", maxSpeedBonus) - penalty;
        e.powerReduction = combine("power", maxPowerReduction);
        e.yield = combine("yield", maxYield);
        e.inputSaving = combine("saving", maxSaving);
        e.recycle = combine("recycle", maxRecycle);
        e.thermal = combine("thermal", maxThermal);
        e.pressure = combine("pressure", maxPressure);
        e.catalystLife = combine("catalyst", maxCatalystLife);
        e.liquidYield = combine("liquid", maxLiquidYield);
        e.wasteRecovery = combine("waste", maxWasteRecovery);
        e.thermalRecovery = combine("recovery", maxThermalRecovery);
        e.damage = combine("damage", maxDamage);
        e.accuracy = combine("accuracy", maxAccuracy);
        e.projectileSpeed = combine("projectile", maxProjectileSpeed);
        e.cooling = combine("cooling", maxCooling);
        e.analyzer = analyzer;
        //capacitors simply add up
        FloatSeq caps = values.get("capacitor");
        e.capacitor = caps == null ? 0f : caps.sum();

        if(e.military()) armed.add(target);
        else armed.remove(target);
        return e;
    }

    static float combine(String key, float cap){
        FloatSeq seq = values.get(key);
        if(seq == null || seq.isEmpty()) return 0f;
        seq.sort();
        float total = 0f, weight = 1f;
        for(int i = seq.size - 1; i >= 0; i--){
            total += seq.get(i) * weight;
            weight *= diminishing;
        }
        return Math.min(total, cap);
    }

    /**
     * Applies damage / accuracy / projectile speed to each bullet once, right after an augmented turret fires it.
     * Only the turret's own shots are adjusted: fragments, split bullets and mines they release keep their spread and timing.
     * Accuracy and projectile speed only apply to moving projectiles; beams and lasers have nothing to speed up or aim.
     */
    static void updateBullets(){
        if(armed.isEmpty() || !state.isPlaying()) return;

        Groups.bullet.each(b -> {
            if(!(b.owner instanceof TurretBuild t) || !armed.contains(t) || !firedBy(t, b.type)) return;
            alive.add(b.id);
            if(!adjusted.add(b.id)) return;
            adjustedBullets.increment(t.id);
            Effects e = effects(t);
            if(e.damage > 0f) b.damage *= 1f + e.damage;
            if(b.type.speed <= 0.01f) return;
            if(e.accuracy > 0f){
                float angle = b.vel.angle();
                b.vel.setAngle(Angles.moveToward(angle, t.rotation, Angles.angleDist(angle, t.rotation) * e.accuracy));
            }
            if(e.projectileSpeed > 0f){
                b.vel.scl(1f + e.projectileSpeed);
                b.lifetime /= 1f + e.projectileSpeed;
            }
        });

        //keep only the ids of bullets still in flight
        IntSet swap = adjusted;
        adjusted = alive;
        alive = swap;
        alive.clear();
    }

    /** Whether a bullet type is one of the turret's own shots (an ammo type or its fixed shot), not a fragment or mine. */
    static boolean firedBy(TurretBuild t, BulletType type){
        if(t.block instanceof ItemTurret it) return it.ammoTypes.containsValue(type, true);
        if(t.block instanceof LiquidTurret lt) return lt.ammoTypes.containsValue(type, true);
        if(t.block instanceof PowerTurret pt) return pt.shootType == type;
        if(t.block instanceof ContinuousTurret ct) return ct.shootType == type;
        return t.peekAmmo() == type;
    }
}
