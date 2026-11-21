package eclipse.content;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;

import static eclipse.content.EclipseItems.*;

/**
 * Military phase M1: ECLIPSE materials as ammunition for vanilla turrets. Each adds an effect vanilla ammo lacks,
 * rather than more damage:
 * - tungsten carbide armour-piercing rounds (Duo, Fuse, Salvo, Spectre) ignore armour;
 * - sulfur caustic shells (Hail, Ripple) splash Etched, which halves armour so every other turret hits harder;
 * - lead-acid cell EMP shells (Hail, Ripple) Jam everything in the blast: the Jammer's effect at artillery range.
 */
public class Munitions{
    public static StatusEffect etched, jammed;
    /** Tide submarines: Submerged (untargetable and unhittable, see SubmarineUnitType) and Sonar Contact (a Sonar has it). */
    public static StatusEffect submerged, sonarContact;
    /** The Swarm's exoskeleton (see ExoskeletonAbility): while it holds, the unit takes 20% less damage. */
    public static StatusEffect exoskeleton;
    /** A freshly laid Swarm egg (the Behemoth's Mites): it can't move or attack and takes double damage until it hatches. */
    public static StatusEffect incubating;
    /** A Tide Lamprey clamped onto a building: it stays put and drains the building (see LatchAbility). */
    public static StatusEffect latched;

    static final Color carbideFront = Color.valueOf("c8ccd8"), carbideBack = Color.valueOf("50525e");
    static final Color sulfurFront = Color.valueOf("f2e27a"), sulfurBack = Color.valueOf("b8a032");

    public static void load(){
        etched = new StatusEffect("etched"){{
            color = Color.valueOf("e8d44d");
            dynamic = true; //lets update() set the armour override on its own status entry
            effect = Fx.sapped;
            effectChance = 0.08f;
        }

            @Override
            public void update(Unit unit, StatusEntry entry){
                super.update(unit, entry);
                entry.damageMultiplier = entry.healthMultiplier = entry.speedMultiplier = entry.reloadMultiplier = 1f;
                entry.buildSpeedMultiplier = entry.dragMultiplier = 1f;
                entry.armorOverride = unit.armor * 0.5f;
            }
        };

        //Tide submarines: under water, out of reach of everything but mines, until a Sonar picks them up
        submerged = new StatusEffect("submerged"){{
            color = Color.valueOf("3e6fb0");
            speedMultiplier = 0.85f;
        }};
        sonarContact = new StatusEffect("sonar-contact"){{
            color = Color.valueOf("7fe0ff");
        }};

        //the Swarm: exoskeleton (tougher while it holds), eggs that must hatch first
        exoskeleton = new StatusEffect("exoskeleton"){{
            color = Color.valueOf("d8c48a");
            healthMultiplier = 1.25f; //damage taken / 1.25: 20% less
        }};
        incubating = new StatusEffect("incubating"){{
            color = Color.valueOf("e8d8a0");
            speedMultiplier = 0f;
            disarm = true;
            healthMultiplier = 0.5f; //takes double damage: a soft target until it hatches
        }

            @Override
            public void draw(Unit unit){
                //the egg: a pale shell over the unit, its spots darkening as it is about to hatch
                float z = Draw.z();
                Draw.z(Layer.groundUnit + 1f);
                float r = unit.hitSize * 0.85f;
                Draw.color(Color.valueOf("8a7a4a"));
                Fill.circle(unit.x, unit.y, r + 1f);
                Draw.color(Color.valueOf("e8d8a0"));
                Fill.circle(unit.x, unit.y, r);
                Draw.color(Color.valueOf("fff2c4"));
                Fill.circle(unit.x - r * 0.3f, unit.y + r * 0.3f, r * 0.35f);
                Draw.color(Color.valueOf("a6b464"), 0.5f + Mathf.absin(Time.time, 6f, 0.4f));
                Fill.circle(unit.x + r * 0.35f, unit.y - r * 0.2f, r * 0.18f);
                Fill.circle(unit.x - r * 0.2f, unit.y - r * 0.4f, r * 0.13f);
                Draw.reset();
                Draw.z(z);
            }
        };
        latched = new StatusEffect("latched"){{
            color = Color.valueOf("8fd8ff");
            speedMultiplier = 0f;
            disarm = true; //all its damage goes into the building it holds
        }};

        //Jammer: enemy weapons cycle slower while it lasts
        jammed = new StatusEffect("jammed"){{
            color = Color.valueOf("b37aff");
            reloadMultiplier = 0.6f;
            effect = Fx.overclocked;
            effectChance = 0.05f;
        }};

        armourPiercing((ItemTurret)Blocks.duo, new BasicBulletType(3.5f, 15){{
            width = 8f;
            height = 11f;
            ammoMultiplier = 3f;
            reloadMultiplier = 0.9f;
            lifetime = 52f;
        }});
        armourPiercing((ItemTurret)Blocks.salvo, new BasicBulletType(3.8f, 24){{
            width = 8f;
            height = 12f;
            ammoMultiplier = 4f;
            lifetime = 55f;
        }});
        armourPiercing((ItemTurret)Blocks.spectre, new BasicBulletType(7.5f, 85){{ //+40%: at 60 it lost to graphite even against armour
            width = 15f;
            height = 22f;
            ammoMultiplier = 3f;
            pierceCap = 2;
            pierceBuilding = true;
            lifetime = 37f;
        }});
        ShrapnelBulletType fuseAp = new ShrapnelBulletType(){{
            length = 100f;
            damage = 112f; //+40%: at 80 it lost to thorium even against armour
            ammoMultiplier = 4f;
            reloadMultiplier = 1.1f;
            toColor = carbideFront;
            shootEffect = smokeEffect = Fx.thoriumShoot;
        }};
        fuseAp.pierceArmor = true;
        ((ItemTurret)Blocks.fuse).ammoTypes.put(tungstenCarbide, fuseAp);

        caustic((ItemTurret)Blocks.hail, new ArtilleryBulletType(3f, 15){{
            splashDamage = 24f;
            splashDamageRadius = 22f;
            ammoMultiplier = 3f;
            width = height = 11f;
            lifetime = 80f;
        }});
        caustic((ItemTurret)Blocks.ripple, new ArtilleryBulletType(3f, 30){{
            splashDamage = 50f;
            splashDamageRadius = 26f;
            ammoMultiplier = 3f;
            width = 12f;
            height = 14f;
            lifetime = 80f;
        }});

        emp((ItemTurret)Blocks.hail, new ArtilleryBulletType(3f, 10){{
            splashDamage = 14f;
            splashDamageRadius = 26f;
            ammoMultiplier = 2f;
            width = height = 11f;
            lifetime = 80f;
        }});
        emp((ItemTurret)Blocks.ripple, new ArtilleryBulletType(3f, 18){{
            splashDamage = 26f;
            splashDamageRadius = 32f;
            ammoMultiplier = 2f;
            width = 12f;
            height = 14f;
            lifetime = 80f;
        }});
    }

    /** EMP shells from lead-acid cells: little damage, but everything in the blast is Jammed (weapons 40% slower) for 4 s. */
    static void emp(ItemTurret turret, ArtilleryBulletType b){
        b.status = jammed;
        b.statusDuration = 60f * 4f;
        b.frontColor = Color.white;
        b.backColor = Color.valueOf("8fd8ff");
        b.hitEffect = Fx.hitLancer;
        b.lightning = 2;
        b.lightningDamage = 6f;
        b.lightningLength = 5;
        b.lightningColor = Color.valueOf("8fd8ff");
        turret.ammoTypes.put(leadAcidCell, b);
    }

    static void armourPiercing(ItemTurret turret, BasicBulletType b){
        b.pierceArmor = true;
        b.frontColor = carbideFront;
        b.backColor = carbideBack;
        b.hitEffect = Fx.hitBulletBig;
        turret.ammoTypes.put(tungstenCarbide, b);
    }

    static void caustic(ItemTurret turret, ArtilleryBulletType b){
        b.status = etched;
        b.statusDuration = 60f * 6f;
        b.frontColor = sulfurFront;
        b.backColor = sulfurBack;
        b.hitEffect = Fx.sapExplosion;
        b.knockback = 0.8f;
        turret.ammoTypes.put(sulfur, b);
    }

    /** Vanilla turrets that take ECLIPSE ammunition (for tests and the showcase). */
    public static Block[] turrets(){
        return new Block[]{Blocks.duo, Blocks.fuse, Blocks.salvo, Blocks.spectre, Blocks.hail, Blocks.ripple};
    }
}
