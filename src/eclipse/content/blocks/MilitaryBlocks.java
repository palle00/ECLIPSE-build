package eclipse.content.blocks;

import arc.graphics.*;
import eclipse.content.*;
import eclipse.defense.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;

import static eclipse.content.EclipseItems.*;
import static eclipse.content.EclipseLiquids.*;
import static mindustry.type.ItemStack.*;

/**
 * Military phase M2: turrets with mechanics vanilla Serpulo lacks.
 * - Acid Mortar: long-range shells that leave corrosive acid pools (area denial); the blast etches armour.
 * - Minelayer: lobs mines that sit on the ground and go off under ground units.
 * - Heat Lance: a beam powered by the heat network instead of electricity; more heat, more damage.
 * - Jammer: a pulse that slows enemy units' fire rate for a while (see {@link Jammer}).
 */
public class MilitaryBlocks{
    public static Block acidMortar, minelayer, heatLance, jammer, piledriver, kestrel, thunderhead, cloudbreaker;
    /** Third set, one turret per tier from the first minutes to the endgame (mechanics in {@link Armory}). */
    public static Block rivet, sickle, tremor, apiary, eventHorizon;

    public static void load(){
        acidMortar = new LiquidTurret("acid-mortar"){{
            requirements(Category.turret, with(Items.titanium, 60, Items.metaglass, 40, Items.graphite, 40, polyethylene, 20, pressureCasing, 6, titaniumGear, 6));
            size = 2;
            health = 900;
            range = 260f;
            minRange = 40f;
            reload = 90f;
            targetAir = false;
            rotateSpeed = 2.5f;
            recoil = 2f;
            shootY = 10f;
            liquidCapacity = 40f;
            inaccuracy = 3f;
            velocityRnd = 0.15f;
            shootSound = Sounds.shootArtillerySap;
            ammo(
                acidicWaste, acidShell(acidicWaste, 20f, 1), //the leaching byproduct: weaker, but free
                hydrochloricAcid, acidShell(hydrochloricAcid, 30f, 1),
                sulfuricAcid, acidShell(sulfuricAcid, 34f, 1),
                nitricAcid, acidShell(nitricAcid, 40f, 1),
                aquaRegia, acidShell(aquaRegia, 55f, 2)
            );
        }};

        minelayer = new ItemTurret("minelayer"){{
            requirements(Category.turret, with(Items.copper, 80, Items.graphite, 50, Items.titanium, 40, copperGear, 10, industrialBearing, 4));
            size = 2;
            health = 800;
            range = 200f;
            minRange = 24f;
            reload = 100f;
            targetAir = false;
            rotateSpeed = 3f;
            inaccuracy = 14f;
            velocityRnd = 0.25f;
            shoot.shots = 2;
            shoot.shotDelay = 8f;
            shootSound = Sounds.shootArtillerySmall;
            coolant = consumeCoolant(0.2f);
            ammo(
                ammoniumNitrate, mineShell(48f, 26f, Color.valueOf("f2efe4"), 2f),
                Items.blastCompound, mineShell(70f, 32f, Color.valueOf("ff795e"), 3f)
            );
        }};

        heatLance = new ContinuousTurret("heat-lance"){{
            requirements(Category.turret, with(Items.silicon, 120, Items.graphite, 100, Items.titanium, 80, reinforcedGlass, 30, superalloyFrame, 8, precisionComponent, 4));
            size = 3;
            health = 1600;
            range = 170f;
            shootY = 30f;
            rotateSpeed = 2.5f;
            heatRequirement = 12f;
            maxHeatEfficiency = 2f;
            scaleDamageEfficiency = true;
            heatColor = Color.valueOf("ff8a3d");
            shootSound = Sounds.none;
            loopSound = Sounds.beamMeltdown;
            loopSoundVolume = 1f;
            shootType = new ContinuousLaserBulletType(14f){{
                length = 180f;
                width = 4f;
                colors = new Color[]{Color.valueOf("ff8a3d55"), Color.valueOf("ff8a3daa"), Color.valueOf("ffd29a"), Color.white};
                status = StatusEffects.melting;
                statusDuration = 90f;
                hitEffect = Fx.hitMeltdown;
                lightColor = Color.valueOf("ff8a3d");
                buildingDamageMultiplier = 0.4f;
            }};
        }};

        jammer = new Jammer("jammer"){{
            requirements(Category.turret, with(Items.silicon, 80, Items.lead, 60, Items.titanium, 40, circuitBoard, 6, copperWire, 20, alnicoMagnet, 4));
            size = 2;
            health = 600;
            status = Munitions.jammed;
            consumePower(3f);
        }};

        // ---- second set: a line-breaker for armoured columns and three kinds of anti-air ----

        //Piledriver: a slow carbide slug that punches through up to three ground units in a line; no splash, no air
        piledriver = new ItemTurret("piledriver"){{
            requirements(Category.turret, with(Items.titanium, 160, Items.graphite, 120, superalloy, 80, tungstenCarbide, 60, superalloyFrame, 6, industrialBearing, 8));
            size = 3;
            health = 2400;
            range = 240f;
            reload = 150f;
            targetAir = false;
            rotateSpeed = 1.6f;
            recoil = 4f;
            shake = 3f;
            shootY = 14f;
            shootSound = Sounds.shootSpectre;
            coolant = consumeCoolant(0.3f);
            ammo(tungstenCarbide, new BasicBulletType(4.5f, 420f){{
                pierce = true;
                pierceCap = 3;
                pierceBuilding = false;
                collidesAir = false;
                width = 13f;
                height = 24f;
                lifetime = 56f;
                knockback = 2.5f;
                frontColor = Color.valueOf("c8ccd8");
                backColor = Color.valueOf("50525e");
                trailLength = 8;
                trailWidth = 2.6f;
                trailColor = Color.valueOf("50525e");
                hitEffect = Fx.hitBulletBig;
                shootEffect = Fx.shootBig;
                smokeEffect = Fx.shootBigSmoke;
            }});
        }};

        //Kestrel: one guided missile at a time, at the strongest aircraft in range
        kestrel = new ItemTurret("kestrel"){{
            requirements(Category.turret, with(Items.silicon, 70, Items.titanium, 50, aluminium, 60, polyethylene, 20, microchip, 6, titaniumGear, 6, precisionComponent, 2));
            size = 2;
            health = 900;
            range = 300f;
            reload = 70f;
            targetGround = false;
            unitSort = UnitSorts.strongest;
            rotateSpeed = 6f;
            shootY = 6f;
            shootSound = Sounds.shootMissile;
            ammo(
                ammoniumNitrate, interceptor(110f, 18f, 1.5f),
                Items.blastCompound, interceptor(150f, 26f, 1f)
            );
        }};

        //Thunderhead: a discharge that jumps between aircraft, weaker with every jump
        thunderhead = new PowerTurret("thunderhead"){{
            requirements(Category.turret, with(Items.titanium, 120, Items.surgeAlloy, 40, cobalt, 80, microchip, 10, leadAcidCell, 12, alnicoMagnet, 8, superalloyFrame, 4));
            size = 3;
            health = 1800;
            range = 170f;
            reload = 50f;
            targetGround = false;
            rotateSpeed = 4f;
            shootY = 10f;
            shootSound = Sounds.shootArc;
            consumePower(5f);
            shootType = new Arsenal.ChainBulletType(9f, 90f){{
                lifetime = 20f;
                width = 7f;
                height = 12f;
                frontColor = Color.white;
                backColor = Color.valueOf("8fd8ff");
                status = Munitions.jammed;
                statusDuration = 60f;
                hitEffect = Fx.hitLancer;
                shootEffect = Fx.lightningShoot;
            }};
        }};

        //Cloudbreaker: floating mines around itself that dart at aircraft passing through; at most 8 at once
        cloudbreaker = new Arsenal.MineTurret("cloudbreaker"){{
            requirements(Category.turret, with(Items.titanium, 100, Items.plastanium, 40, aluminium, 80, carbonComposite, 40, microchip, 8, industrialBearing, 6));
            size = 3;
            health = 1600;
            range = 200f;
            reload = 90f;
            maxMines = 8;
            targetGround = false;
            rotateSpeed = 5f;
            inaccuracy = 35f;
            velocityRnd = 0.4f;
            shootY = 8f;
            shootSound = Sounds.shootArtillerySmall;
            ammo(
                Items.blastCompound, aerialMine(140f, 30f, 2f),
                ammoniumNitrate, aerialMine(90f, 24f, 2f)
            );
        }};

        // ---- third set: one turret per tier, from the first copper to the endgame ----

        //T1 Rivet: a 1x1 gatling that spins up to triple fire rate while it keeps shooting
        rivet = new Armory.SpinTurret("rivet"){{
            requirements(Category.turret, with(Items.copper, 45, Items.lead, 25, Items.graphite, 15, copperGear, 6));
            size = 1;
            health = 280;
            range = 115f;
            reload = 30f;
            maxSpeedup = 3f;
            spinUpTime = 180f;
            rotateSpeed = 9f;
            inaccuracy = 4f;
            shootY = 4f;
            recoil = 0.6f;
            shootCone = 25f;
            shootSound = Sounds.shootDuo;
            coolant = consumeCoolant(0.1f);
            ammo(
                Items.copper, rivetRound(2.8f, 9f, 2, Color.valueOf("eac2a9"), Color.valueOf("d39169"), 0f, 1f),
                Items.graphite, rivetRound(3.5f, 18f, 4, Color.valueOf("bfbfd6"), Color.valueOf("7d7d93"), 0f, 0.8f),
                Items.silicon, rivetRound(3f, 12f, 5, Color.valueOf("e4e4ea"), Color.valueOf("8a8ba1"), 0.35f, 1.2f)
            );
        }};

        //T2 Sickle: a sawblade that flies out, turns and comes back, cutting through ground units both ways
        sickle = new ItemTurret("sickle"){{
            requirements(Category.turret, with(Items.titanium, 70, Items.lead, 50, Items.graphite, 40, titaniumGear, 8, industrialBearing, 4));
            size = 2;
            health = 900;
            range = 150f;
            reload = 95f; //one blade in the air at a time: it is caught before the next is thrown
            targetAir = false;
            rotateSpeed = 4f;
            shootY = 6f;
            recoil = 1.5f;
            shootSound = Sounds.shootBreach;
            coolant = consumeCoolant(0.2f);
            ammo(
                Items.titanium, new Armory.BoomerangBulletType(5f, 32f){{
                    lifetime = 64f;
                    ammoMultiplier = 2f;
                }},
                tungstenCarbide, new Armory.BoomerangBulletType(5.5f, 60f){{
                    lifetime = 60f;
                    ammoMultiplier = 3f;
                    status = Munitions.etched;
                    statusDuration = 60f * 3f;
                    bladeColor = Color.valueOf("9ea3b5");
                    edgeColor = Color.valueOf("50525e");
                }}
            );
        }};

        //T3 Tremor: a powered ground shockwave around itself; knocks ground units back and slows them
        tremor = new PowerTurret("tremor"){{
            requirements(Category.turret, with(Items.titanium, 120, Items.silicon, 60, Items.lead, 80, Items.thorium, 40, industrialBearing, 8, pressureCasing, 4));
            size = 3;
            health = 1900;
            range = 120f;
            reload = 110f;
            targetAir = false;
            rotateSpeed = 30f;
            shootY = 0f;
            recoil = 0f;
            shake = 2f;
            shootCone = 360f;
            predictTarget = false;
            shootSound = Sounds.shockwaveTower;
            consumePower(4f);
            shootType = new Armory.ShockwaveBulletType(70f){{
                radius = 120f;
            }};
        }};

        //T4 Apiary: launches drones that circle over the enemy and shoot whatever is near, ground or air; at most 6 at once
        apiary = new Armory.DroneTurret("apiary"){{
            requirements(Category.turret, with(Items.silicon, 150, Items.titanium, 100, Items.plastanium, 60, aluminium, 60, microchip, 20, precisionComponent, 6, industrialBearing, 6));
            size = 3;
            health = 1700;
            range = 240f;
            reload = 70f;
            maxDrones = 6;
            rotateSpeed = 4f;
            shootY = 8f;
            inaccuracy = 20f;
            shootSound = Sounds.shootMissileSmall;
            coolant = consumeCoolant(0.3f);
            ammo(
                Items.silicon, drone(14f, 30f, 1f),
                microchip, drone(24f, 22f, 2f)
            );
        }};

        //T5 Event Horizon: a singularity that pulls every enemy around it in, ground and air, then collapses
        eventHorizon = new ItemTurret("event-horizon"){{
            requirements(Category.turret, with(Items.surgeAlloy, 400, Items.phaseFabric, 200, Items.thorium, 300, surgeSuperalloy, 200, superconductor, 120,
                aetherite, 150, superalloyFrame, 30, precisionComponent, 30, experimentalCore, 12));
            size = 4;
            health = 5200;
            range = 300f;
            reload = 360f;
            rotateSpeed = 1.5f;
            shootY = 14f;
            recoil = 3f;
            shake = 4f;
            predictTarget = false;
            shootSound = Sounds.shootEclipse;
            consumePower(12f);
            coolant = consumeCoolant(0.5f);
            ammo(
                aetherite, new Armory.SingularityBulletType(2.2f, 900f),
                Items.phaseFabric, new Armory.SingularityBulletType(2.2f, 450f){{
                    pullRadius = 56f;
                    tickDamage = 4f;
                    glowColor = Color.valueOf("ffd59e");
                    lightColor = glowColor;
                }}
            );
        }};
    }

    /** Rivet's round: small and quick; homing &gt; 0 makes it seek (silicon). */
    static BulletType rivetRound(float speed, float damage, int perItem, Color front, Color back, float homing, float reloadMul){
        return new BasicBulletType(speed, damage){{
            width = 6f;
            height = 9f;
            lifetime = 120f / speed;
            ammoMultiplier = perItem;
            frontColor = front;
            backColor = back;
            homingPower = homing;
            reloadMultiplier = reloadMul;
            hitEffect = Fx.hitBulletSmall;
        }};
    }

    /** Apiary's drone, firing small laser bolts every fireEvery ticks. */
    static BulletType drone(float boltDamage, float fireEvery, float perItem){
        return new Armory.DroneBulletType(new LaserBoltBulletType(5.2f, boltDamage){{
            lifetime = 20f;
            healPercent = 0f;
            collidesTeam = false;
            backColor = Color.valueOf("f8c166");
            frontColor = Color.white;
        }}){{
            fireInterval = fireEvery;
            ammoMultiplier = perItem;
        }};
    }

    /** Kestrel's interceptor: a fast homing missile for aircraft only, with a small blast. */
    static BulletType interceptor(float damage, float radius, float ammoPerItem){
        return new MissileBulletType(5.5f, damage){{
            collidesGround = false;
            homingPower = 0.25f;
            homingRange = 120f;
            lifetime = 62f;
            splashDamage = damage * 0.3f;
            splashDamageRadius = radius;
            width = 7f;
            height = 13f;
            ammoMultiplier = ammoPerItem;
            frontColor = Color.white;
            backColor = Color.valueOf("8fd8ff");
            trailColor = Color.valueOf("8fd8ff");
        }};
    }

    /** Cloudbreaker's floating mine: bursts against aircraft only. */
    static BulletType aerialMine(float damage, float radius, float ammoPerItem){
        return new Arsenal.AerialMineBulletType(damage * 0.4f){{
            splashDamage = damage;
            splashDamageRadius = radius;
            ammoMultiplier = ammoPerItem;
        }};
    }

    /** Artillery shell full of acid: its blast etches armour, and it leaves corrosive pools (the acid's own status) where it lands. */
    static BulletType acidShell(Liquid acid, float splash, int pool){
        return new ArtilleryBulletType(2.6f, 0f){{
            splashDamage = splash;
            splashDamageRadius = 24f;
            lifetime = 100f;
            width = height = 12f;
            frontColor = acid.color;
            backColor = acid.color.cpy().mul(0.7f);
            status = Munitions.etched;
            statusDuration = 60f * 6f;
            hitEffect = Fx.sapExplosion;
            ammoMultiplier = 0.1f; //10 units of acid per shell
            puddleLiquid = acid;
            puddles = pool == 1 ? 6 : 10;
            puddleRange = pool == 1 ? 14f : 20f;
            puddleAmount = 30f;
        }};
    }

    /**
     * A mine: waits on the ground (up to 30 s) and goes off under the first ground unit; flying units pass over.
     * speed > 0 scatters it a little before it settles (dropped from a bomber).
     */
    public static BasicBulletType mine(float splash, float radius, float speed){
        return new BasicBulletType(speed, 0f, "eclipse-mine"){{
            drag = speed > 0f ? 0.08f : 0f;
            width = height = 10f;
            shrinkY = 0f;
            lifetime = 60f * 30f;
            collidesAir = false;
            collidesTiles = false;
            hittable = false;
            reflectable = false;
            absorbable = false;
            keepVelocity = false;
            hitSize = 6f;
            splashDamage = splash;
            splashDamageRadius = radius;
            despawnHit = false;
            despawnEffect = Fx.none;
            hitEffect = Fx.blastExplosion;
            hitSound = Sounds.explosion;
            frontColor = Color.white;
            backColor = Color.white;
            layer = Layer.debris + 0.1f;
        }};
    }

    /** A lobbed shell that drops a mine where it lands. */
    static BulletType mineShell(float splash, float radius, Color color, float ammoPerItem){
        BasicBulletType mine = mine(splash, radius, 0f);
        return new ArtilleryBulletType(2.4f, 0f){{
            lifetime = 85f;
            width = height = 10f;
            frontColor = color;
            backColor = color.cpy().mul(0.7f);
            fragBullets = 1;
            fragBullet = mine;
            fragVelocityMin = fragVelocityMax = 0f;
            fragRandomSpread = 0f;
            hitEffect = Fx.none;
            despawnEffect = Fx.none;
            ammoMultiplier = ammoPerItem;
        }};
    }
}
