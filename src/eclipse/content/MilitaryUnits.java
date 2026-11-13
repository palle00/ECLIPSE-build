package eclipse.content;

import arc.graphics.*;
import arc.math.geom.*;
import eclipse.content.blocks.*;
import eclipse.type.*;
import mindustry.content.*;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.type.unit.*;
import mindustry.type.weapons.*;
import mindustry.world.blocks.units.*;
import mindustry.world.blocks.units.UnitFactory.*;
import mindustry.world.meta.*;

import static eclipse.content.EclipseItems.*;
import static mindustry.type.ItemStack.*;

/**
 * Military phase M3: two unit lines built in the vanilla factories and reconstructors (no parallel factory).
 * Each tier carries an M1/M2 mechanic instead of only more health:
 * - Tank line (Serpulo has no tanks): Reaper, Bulwark, Rampart, Annihilator, Colossus. Armour-piercing carbide guns;
 *   the Rampart adds a frontal shield arc, the Annihilator etching siege shells, the Colossus a piercing cannon and acid mortars.
 * - Strike line: Wraith (marks targets with Etched), Interceptor (anti-air only), Valkyrie (armour-piercing gunship),
 *   Tempest (bomber that scatters mines), Leviathan (command ship: jamming pulse and point defence).
 */
public class MilitaryUnits{
    public static UnitType reaper, bulwark, rampart, annihilator, colossus;
    public static UnitType wraith, interceptor, valkyrie, tempest, leviathan;

    static final Color apFront = Color.valueOf("c8ccd8"), apBack = Color.valueOf("50525e");

    public static void load(){
        // ---- tank line ----
        reaper = tank("reaper", 32, 6, 26, t -> {
            t.health = 170f;
            t.armor = 3f;
            t.speed = 1.05f;
            t.hitSize = 9f;
            t.rotateSpeed = 3.5f;
            t.weapons.add(new Weapon("eclipse-reaper-gun"){{
                mirror = false;
                rotate = true;
                rotateSpeed = 5f;
                reload = 22f;
                shootY = 5f;
                recoil = 1f;
                shootSound = Sounds.shootDuo;
                bullet = ap(3.6f, 12f, 38f, 7f, 10f);
                bullet.buildingDamageMultiplier = 1.6f; //sabotage: rips into buildings
            }});
        });
        bulwark = tank("bulwark", 48, 9, 38, t -> {
            t.health = 620f;
            t.armor = 9f;
            t.speed = 0.72f;
            t.hitSize = 13f;
            t.weapons.add(new Weapon("eclipse-bulwark-cannon"){{
                mirror = false;
                rotate = true;
                rotateSpeed = 3f;
                reload = 45f;
                shootY = 9f;
                recoil = 2f;
                shake = 1f;
                shootSound = Sounds.shootSalvo;
                bullet = ap(4.5f, 34f, 42f, 10f, 14f);
            }});
        });
        rampart = tank("rampart", 64, 12, 52, t -> {
            t.health = 1150f;
            t.armor = 11f;
            t.speed = 0.6f;
            t.hitSize = 17f;
            t.abilities.add(new ShieldArcAbility(){{
                radius = 34f;
                angle = 90f;
                regen = 0.6f;
                cooldown = 60f * 6f;
                max = 700f;
                width = 6f;
            }});
            t.weapons.add(new Weapon("eclipse-rampart-cannon"){{
                x = 7f;
                y = -2f;
                rotate = true;
                rotateSpeed = 2.5f;
                alternate = true;
                reload = 40f;
                shootY = 8f;
                recoil = 2f;
                shootSound = Sounds.shootSalvo;
                bullet = ap(4.5f, 32f, 44f, 10f, 14f);
            }});
        });
        annihilator = tank("annihilator", 96, 18, 80, t -> {
            t.health = 9000f;
            t.armor = 14f;
            t.speed = 0.5f;
            t.hitSize = 26f;
            t.crushDamage = 2f;
            t.weapons.add(new Weapon("eclipse-annihilator-mortar"){{
                mirror = false;
                rotate = true;
                rotateSpeed = 1.5f;
                reload = 90f;
                shootY = 14f;
                recoil = 3f;
                shake = 3f;
                shootSound = Sounds.shootRipple;
                bullet = new ArtilleryBulletType(2.6f, 30f){{
                    splashDamage = 85f;
                    splashDamageRadius = 36f;
                    lifetime = 115f;
                    width = height = 16f;
                    status = Munitions.etched;
                    statusDuration = 60f * 6f;
                    frontColor = Color.valueOf("f2e27a");
                    backColor = Color.valueOf("b8a032");
                    hitEffect = Fx.sapExplosion;
                    collidesAir = false;
                }};
            }}, new Weapon("eclipse-annihilator-gun"){{
                x = 10f;
                y = -6f;
                rotate = true;
                reload = 15f;
                shootSound = Sounds.shootDuo;
                bullet = ap(4f, 24f, 45f, 8f, 11f);
            }});
        });
        colossus = tank("colossus", 128, 24, 108, t -> {
            t.health = 24000f;
            t.armor = 18f;
            t.speed = 0.4f;
            t.hitSize = 34f;
            t.crushDamage = 4f;
            t.weapons.add(new Weapon("eclipse-colossus-cannon"){{
                mirror = false;
                rotate = true;
                rotateSpeed = 1f;
                reload = 110f;
                shootY = 26f;
                recoil = 5f;
                shake = 5f;
                shootSound = Sounds.shootSpectre;
                bullet = new BasicBulletType(9f, 260f){{
                    pierceArmor = true;
                    pierce = true;
                    pierceCap = 4;
                    pierceBuilding = true;
                    width = 16f;
                    height = 26f;
                    lifetime = 32f;
                    frontColor = apFront;
                    backColor = apBack;
                    trailLength = 8;
                    trailWidth = 3f;
                    trailColor = apBack;
                    hitEffect = Fx.hitBulletBig;
                }};
            }}, new Weapon("eclipse-colossus-mortar"){{
                x = 12f;
                y = -8f;
                rotate = true;
                rotateSpeed = 2f;
                reload = 70f;
                shootSound = Sounds.shootArtillerySap;
                bullet = new ArtilleryBulletType(2.6f, 0f){{
                    splashDamage = 45f;
                    splashDamageRadius = 26f;
                    lifetime = 100f;
                    width = height = 12f;
                    status = Munitions.etched;
                    statusDuration = 60f * 6f;
                    frontColor = EclipseLiquids.sulfuricAcid.color;
                    backColor = EclipseLiquids.sulfuricAcid.color.cpy().mul(0.7f);
                    puddleLiquid = EclipseLiquids.sulfuricAcid;
                    puddles = 8;
                    puddleRange = 16f;
                    puddleAmount = 30f;
                    collidesAir = false;
                }};
            }});
        });

        // ---- strike line ----
        wraith = flyer("wraith", t -> {
            t.health = 80f;
            t.speed = 3.4f;
            t.hitSize = 8f;
            t.accel = 0.15f;
            t.drag = 0.05f;
            t.weapons.add(new Weapon(){{
                x = 3f;
                reload = 18f;
                shootSound = Sounds.shoot;
                bullet = new BasicBulletType(3f, 7f){{
                    lifetime = 40f;
                    width = 6f;
                    height = 8f;
                    status = Munitions.etched; //marks targets for heavier guns
                    statusDuration = 60f * 2f;
                    frontColor = Color.valueOf("f2e27a");
                    backColor = Color.valueOf("b8a032");
                }};
            }});
        });
        interceptor = flyer("interceptor", t -> {
            t.health = 330f;
            t.speed = 3f;
            t.armor = 2f;
            t.hitSize = 11f;
            t.accel = 0.1f;
            t.drag = 0.04f;
            t.targetGround = false;
            t.weapons.add(new Weapon("eclipse-interceptor-launcher"){{
                x = 4f;
                reload = 30f;
                shootSound = Sounds.shootMissileSmall;
                bullet = new MissileBulletType(5f, 22f){{
                    lifetime = 48f;
                    homingPower = 0.15f;
                    splashDamage = 16f;
                    splashDamageRadius = 18f;
                    collidesGround = false;
                    width = 7f;
                    height = 10f;
                }};
            }});
        });
        valkyrie = flyer("valkyrie", t -> {
            t.health = 780f;
            t.speed = 1.9f;
            t.armor = 5f;
            t.hitSize = 15f;
            t.accel = 0.06f;
            t.drag = 0.03f;
            t.weapons.add(new Weapon("eclipse-valkyrie-gun"){{
                x = 5f;
                y = 1f;
                reload = 8f;
                shootSound = Sounds.shootDuo;
                bullet = ap(4f, 14f, 40f, 6f, 9f);
            }});
        });
        tempest = flyer("tempest", t -> {
            t.health = 7200f;
            t.speed = 1.1f;
            t.armor = 9f;
            t.hitSize = 26f;
            t.accel = 0.04f;
            t.drag = 0.02f;
            t.targetAir = false;
            t.weapons.add(new Weapon(){{
                mirror = false;
                x = 0f;
                shootCone = 180f;
                ignoreRotation = true;
                reload = 40f;
                shootSound = Sounds.none;
                bullet = new BombBulletType(40f, 30f){{
                    width = height = 14f;
                    fragBullets = 4;
                    fragBullet = MilitaryBlocks.mine(50f, 26f, 1.6f);
                    fragVelocityMin = 0.4f;
                    fragVelocityMax = 1f;
                }};
            }});
        });
        leviathan = flyer("leviathan", t -> {
            t.health = 22000f;
            t.speed = 0.7f;
            t.armor = 13f;
            t.hitSize = 34f;
            t.accel = 0.03f;
            t.drag = 0.02f;
            t.abilities.add(new JamPulseAbility());
            t.weapons.add(new PointDefenseWeapon("eclipse-leviathan-point-defense"){{
                x = 11f;
                y = -4f;
                reload = 8f;
                targetInterval = 6f;
                targetSwitchInterval = 6f;
                bullet = new BulletType(){{
                    shootEffect = Fx.sparkShoot;
                    hitEffect = Fx.pointHit;
                    maxRange = 120f;
                    damage = 25f;
                }};
            }}, new Weapon("eclipse-leviathan-lance"){{
                mirror = false;
                y = 8f;
                rotate = true;
                rotateSpeed = 2f;
                reload = 70f;
                shootY = 10f;
                shootSound = Sounds.shootLancer;
                bullet = new LaserBulletType(120f){{
                    length = 220f;
                    colors = new Color[]{Color.valueOf("b37aff66"), Color.valueOf("b37aff"), Color.white};
                }};
            }});
        });

        // ---- production: vanilla factories and reconstructors ----
        ((UnitFactory)Blocks.groundFactory).plans.add(new UnitPlan(reaper, 60f * 15f, with(Items.silicon, 10, Items.lead, 10, copperGear, 5)));
        ((UnitFactory)Blocks.airFactory).plans.add(new UnitPlan(wraith, 60f * 15f, with(Items.silicon, 15, copperWire, 5)));
        upgrade(Blocks.additiveReconstructor, reaper, bulwark, wraith, interceptor);
        upgrade(Blocks.multiplicativeReconstructor, bulwark, rampart, interceptor, valkyrie);
        upgrade(Blocks.exponentialReconstructor, rampart, annihilator, valkyrie, tempest);
        upgrade(Blocks.tetrativeReconstructor, annihilator, colossus, tempest, leviathan);
    }

    /** A Serpulo tank: Erekir's tread system with Serpulo outlines and research costs. Treads are one strip per side. */
    public static UnitType tank(String name, int sprite, int treadX, int treadH, arc.func.Cons<UnitType> setup){
        UnitType t = new TankUnitType(name){{
            constructor = TankUnit::create;
            outlineColor = Pal.darkOutline;
            researchCostMultiplier = 1f;
            envDisabled = Env.none;
            //left tread strip in sprite pixels from the centre; the game mirrors it and animates it
            int w = Math.max(4, sprite / 7);
            treadRects = new Rect[]{new Rect(treadX - sprite / 2f, (sprite - treadH) / 2f - sprite / 2f, w, treadH)};
        }};
        setup.get(t);
        return t;
    }

    public static UnitType flyer(String name, arc.func.Cons<UnitType> setup){
        UnitType t = new UnitType(name){{
            constructor = UnitEntity::create;
            flying = true;
            lowAltitude = true;
            circleTarget = false;
        }};
        setup.get(t);
        return t;
    }

    /** Armour-piercing carbide round. */
    static BasicBulletType ap(float speed, float damage, float lifetime, float width, float height){
        BasicBulletType b = new BasicBulletType(speed, damage);
        b.pierceArmor = true;
        b.lifetime = lifetime;
        b.width = width;
        b.height = height;
        b.frontColor = apFront;
        b.backColor = apBack;
        b.hitEffect = Fx.hitBulletBig;
        return b;
    }

    static void upgrade(mindustry.world.Block reconstructor, UnitType... pairs){
        Reconstructor r = (Reconstructor)reconstructor;
        for(int i = 0; i < pairs.length; i += 2) r.upgrades.add(new UnitType[]{pairs[i], pairs[i + 1]});
    }
}
