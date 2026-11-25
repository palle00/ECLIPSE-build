package eclipse.content.blocks;

import arc.graphics.*;
import eclipse.content.*;
import eclipse.defense.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.part.*;
import mindustry.world.draw.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.payloads.*;

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
    /** 1.0 additions (player requests): the Bombard siege mortar ({@link Bombard}); the Supernova pair, Primer and Stardust Launcher ({@link Supernova}). */
    public static Block bombard, primer, stardustLauncher;
    /** 1.0 additions: the Payload Catapult and the Casing Fabricator that builds its empty shells ({@link PayloadCatapult}). */
    public static Block payloadCatapult, casingFabricator;
    /** 1.0 additions: the Sonar, which finds the Tide's submerged submarines (a {@link Jammer} that applies Sonar Contact). */
    public static Block sonar;

    public static void load(){
        acidMortar = new LiquidTurret("acid-mortar"){{
            drawer = new DrawTurret(){{ parts.add(new RegionPart("-tube"){{ progress = PartProgress.recoil; moveY = -1.5f; }}); }};
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
                acidicWaste, acidShell(acidicWaste, 15f, 1), //the leaching byproduct: weaker, but free (splash -25% across, balance audit 2026-11-25)
                hydrochloricAcid, acidShell(hydrochloricAcid, 22f, 1),
                sulfuricAcid, acidShell(sulfuricAcid, 26f, 1),
                nitricAcid, acidShell(nitricAcid, 30f, 1),
                aquaRegia, acidShell(aquaRegia, 41f, 2)
            );
        }};

        minelayer = new ItemTurret("minelayer"){{
            drawer = new DrawTurret(){{ parts.add(new RegionPart("-hatch"){{ progress = PartProgress.recoil; moveY = -2f; }}); }};
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
                Items.blastCompound, blasted(mineShell(70f, 32f, Color.valueOf("ff795e"), 3f))
            );
        }};

        heatLance = new Armory.HeatBeamTurret("heat-lance"){{
            drawer = new DrawTurret(){{ parts.add(new RegionPart("-emitter"){{ progress = PartProgress.warmup; moveY = 2f; heatColor = Color.valueOf("ff8a3d"); }}); }};
            requirements(Category.turret, with(Items.silicon, 120, Items.graphite, 100, Items.titanium, 80, reinforcedGlass, 30, superalloyFrame, 8, precisionComponent, 4));
            size = 3;
            health = 1600;
            range = 170f;
            shootY = 11f; //the emitter lens at the tip of the sprite
            rotateSpeed = 2.5f;
            heatRequirement = 12f;
            //balance audit 2026-11-25: extra heat never raised the damage (vanilla caps it at 100%), so 42 damage at the bare
            //12 heat was already twice a Fuse. Now 24 damage (about a Fuse) and up to 1.5x with 18 heat (HeatBeamTurret).
            maxHeatEfficiency = 1.5f;
            scaleDamageEfficiency = true;
            heatColor = Color.valueOf("ff8a3d");
            shootSound = Sounds.none;
            loopSound = Sounds.beamMeltdown;
            loopSoundVolume = 1f;
            shootType = new ContinuousLaserBulletType(24f){{
                damageInterval = 15f; //same damage per second as 8 every 5 ticks, in bigger hits that armour blunts less
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
            requirements(Category.turret, with(Items.silicon, 80, Items.lead, 60, Items.titanium, 40, circuitBoard, 6, insulatedCable, 10, emitter, 2));
            size = 2;
            health = 600;
            status = Munitions.jammed;
            consumePower(3f);
        }};

        // ---- second set: a line-breaker for armoured columns and three kinds of anti-air ----

        //Piledriver: a slow carbide slug that punches through up to three ground units in a line; no splash, no air
        piledriver = new ItemTurret("piledriver"){{
            drawer = new DrawTurret(){{ parts.add(new RegionPart("-ram"){{ progress = PartProgress.recoil; moveY = -3f; }}); }};
            requirements(Category.turret, with(Items.titanium, 160, Items.graphite, 120, tungstenPlate, 40, tungstenCarbide, 60, superalloyFrame, 6, piston, 4));
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
            ammo(tungstenCarbide, new BasicBulletType(4.5f, 600f){{
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
            drawer = new DrawTurret(){{ parts.add(new RegionPart("-barrel"){{ progress = PartProgress.recoil; moveY = -1.5f; }}); }};
            requirements(Category.turret, with(Items.titanium, 50, Items.silicon, 70, aluminium, 60, microchip, 6, titaniumGear, 6, precisionComponent, 2));
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
                ammoniumNitrate, interceptor(150f, 18f, 1.5f),
                Items.blastCompound, blasted(interceptor(210f, 26f, 1f))
            );
        }};

        //Thunderhead: a discharge that jumps between aircraft, weaker with every jump
        thunderhead = new PowerTurret("thunderhead"){{
            drawer = new DrawTurret(){{ parts.add(new RegionPart("-prong"){{ mirror = true; progress = PartProgress.warmup; moveX = 1.5f; moveRot = -6f; }}); }};
            requirements(Category.turret, with(Items.titanium, 120, Items.surgeAlloy, 40, leadAcidCell, 12, microchip, 10, superalloyFrame, 4, alnicoMagnet, 8));
            size = 3;
            health = 1800;
            range = 170f;
            reload = 50f;
            targetGround = false;
            rotateSpeed = 4f;
            shootY = 10f;
            shootSound = Sounds.shootArc;
            consumePower(5f);
            shootType = new Arsenal.ChainBulletType(9f, 130f){{
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
            drawer = new DrawTurret(){{ parts.add(new RegionPart("-spine"){{ progress = PartProgress.recoil; moveY = -2f; }}); }};
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
                Items.blastCompound, blasted(aerialMine(140f, 30f, 2f)),
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
            drawer = new DrawTurret(){{ parts.add(new RegionPart("-barrels"){{ progress = PartProgress.recoil; moveY = -1.5f; }}); }};
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
                Items.titanium, new Armory.BoomerangBulletType(5f, 96f){{
                    lifetime = 64f;
                    ammoMultiplier = 2f;
                }},
                tungstenCarbide, new Armory.BoomerangBulletType(5.5f, 180f){{
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
            drawer = new DrawTurret(){{ parts.add(new RegionPart("-arm"){{ mirror = true; progress = PartProgress.recoil; moveX = 2f; }}); }};
            requirements(Category.turret, with(Items.titanium, 120, Items.silicon, 60, Items.lead, 80, Items.thorium, 40, piston, 4, pressureCasing, 4));
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
            shootType = new Armory.ShockwaveBulletType(260f){{ //balance audit 2026-11-25: was 175, knockback threw units out before the next wave
                radius = 120f;
            }};
        }};

        //T4 Apiary: launches drones that circle over the enemy and shoot whatever is near, ground or air; at most 6 at once
        apiary = new Armory.DroneTurret("apiary"){{
            drawer = new DrawTurret(){{ parts.add(new RegionPart("-bay"){{ mirror = true; progress = PartProgress.warmup; moveX = 2f; }}); }};
            requirements(Category.turret, with(Items.titanium, 100, Items.silicon, 150, Items.plastanium, 60, microchip, 20, industrialBearing, 6, precisionComponent, 6));
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
                microchip, drone(14f, 22f, 2f) //balance audit 2026-11-25: 24 damage out-damaged a Spectre
            );
        }};

        //T5 Event Horizon: a singularity that pulls every enemy around it in, ground and air, then collapses
        eventHorizon = new ItemTurret("event-horizon"){{
            requirements(Category.turret, with(Items.thorium, 300, Items.phaseFabric, 200, Items.surgeAlloy, 400, experimentalCore, 12, aetherite, 150, superconductor, 120));
            size = 4;
            health = 5200;
            range = 300f;
            reload = 360f;
            rotateSpeed = 1.5f;
            shootY = 14f;
            heatColor = Color.valueOf("c88fff");
            drawer = new DrawTurret(){{
                parts.add(new RegionPart("-claw"){{
                    mirror = true; progress = PartProgress.warmup; moveX = 3f; moveRot = -10f;
                    heatColor = Color.valueOf("c88fff");
                }});
            }};
            recoil = 3f;
            shake = 4f;
            predictTarget = false;
            shootSound = Sounds.shootEclipse;
            consumePower(12f);
            coolant = consumeCoolant(0.5f);
            ammo(
                aetherite, new Armory.SingularityBulletType(2.2f, 2700f){{
                    tickDamage = 18f;
                }},
                Items.phaseFabric, new Armory.SingularityBulletType(2.2f, 1350f){{
                    pullRadius = 56f;
                    tickDamage = 12f;
                    glowColor = Color.valueOf("ffd59e");
                    lightColor = glowColor;
                }}
            );
        }};

        // ---- 1.0 additions ----

        //Bombard: a siege mortar that outranges everything on Serpulo, but has a 15-tile blind spot around itself
        bombard = new Bombard.BombardTurret("bombard"){{
            requirements(Category.turret, with(Items.thorium, 200, Items.graphite, 250, Items.titanium, 300, tungstenPlate, 60, superalloyFrame, 10, piston, 8));
            size = 4;
            health = 3600;
            range = 560f;
            minRange = 120f;
            reload = 60f * 7f;
            targetAir = false;
            rotateSpeed = 0.8f;
            recoil = 5f;
            shake = 6f;
            shootY = 18f;
            drawer = new DrawTurret(){{
                parts.add(new RegionPart("-tube"){{ progress = PartProgress.recoil; moveY = -3.5f; }});
            }};
            inaccuracy = 2f;
            velocityRnd = 0.05f;
            shootSound = Sounds.shootArtillerySapBig;
            shootEffect = Fx.shootBig2;
            smokeEffect = Fx.shootBigSmoke2;
            coolant = consumeCoolant(0.4f);
            ammo(
                Items.graphite, siegeShell(450f, 48f, Color.valueOf("bfbfd6"), Color.valueOf("7d7d93")),
                ammoniumNitrate, siegeShell(670f, 60f, Color.valueOf("f2efe4"), Color.valueOf("b9b49c")),
                Items.blastCompound, blasted(siegeShell(1050f, 76f, Color.valueOf("ff795e"), Color.valueOf("c4533d")))
            );
        }};
        //Supernova, part 1: the Primer leaves a mark that detonates after 2.5 s, pulling in nearby stardust clouds
        primer = new PowerTurret("primer"){{
            drawer = new DrawTurret(){{ parts.add(new RegionPart("-barrel"){{ progress = PartProgress.recoil; moveY = -2f; }}); }};
            requirements(Category.turret, with(Items.silicon, 150, Items.titanium, 120, Items.plastanium, 50, reinforcedGlass, 20, microchip, 10, precisionComponent, 4));
            size = 3;
            health = 1800;
            range = 240f;
            reload = 60f * 5f;
            rotateSpeed = 3f;
            shootY = 10f;
            recoil = 2f;
            shootSound = Sounds.shootLancer;
            consumePower(5f);
            shootType = new Supernova.PrimerBulletType(4f, 15f, new Supernova.FuseBulletType(60f * 2.5f){{
                fragment = new BasicBulletType(2.5f, 22f){{
                    width = 10f;
                    height = 12f;
                    lifetime = 30f;
                    frontColor = Pal.plastaniumFront;
                    backColor = Pal.plastaniumBack;
                    despawnEffect = Fx.plasticExplosion;
                }};
            }}){{
                lifetime = 60f;
            }};
        }};

        //Supernova, part 2: the Stardust Launcher scatters clouds of any of these items for the Primer to set off
        stardustLauncher = new ItemTurret("stardust-launcher"){{
            drawer = new DrawTurret(){{ parts.add(new RegionPart("-tubes"){{ progress = PartProgress.recoil; moveY = -2f; }}); }};
            requirements(Category.turret, with(Items.titanium, 120, Items.graphite, 80, Items.lead, 100, titaniumGear, 10, industrialBearing, 6, pressureCasing, 4));
            size = 3;
            health = 1500;
            range = 240f;
            reload = 12f;
            rotateSpeed = 6f;
            inaccuracy = 10f;
            velocityRnd = 0.2f;
            shootY = 9f;
            recoil = 1f;
            shootSound = Sounds.shoot;
            coolant = consumeCoolant(0.2f);
            Item[] items = stardustItems();
            Object[] list = new Object[items.length * 2];
            for(int i = 0; i < items.length; i++){
                list[i * 2] = items[i];
                list[i * 2 + 1] = new Supernova.DebrisShotBulletType(items[i], 8f){{
                    ammoMultiplier = 2f;
                }};
            }
            ammo(list);
        }};

        //Sonar: pings every 2 s; submerged submarines in range become targetable for 3 s
        sonar = new Jammer("sonar"){{
            requirements(Category.turret, with(Items.silicon, 60, Items.lead, 60, Items.metaglass, 40, circuitBoard, 4, emitter, 2));
            size = 2;
            health = 500;
            status = Munitions.sonarContact;
            range = 200f;
            interval = 120f;
            duration = 180f;
            bossFactor = 1f;
            color = Color.valueOf("7fe0ff");
            consumePower(1.5f);
        }};

        //Payload Catapult: throws whole blocks (filled containers, tanks, batteries); they blow up as if destroyed where they land
        payloadCatapult = new PayloadCatapult.CatapultTurret("payload-catapult"){{
            requirements(Category.turret, with(Items.thorium, 400, Items.plastanium, 200, Items.surgeAlloy, 150, superalloyFrame, 20, experimentalCore, 4, piston, 12));
            size = 5;
            health = 6000;
            range = 420f;
            minRange = 64f;
            reload = 60f * 4f;
            targetAir = false;
            rotateSpeed = 0.7f;
            shootCone = 2f; //one expensive shot: aim properly before letting go
            recoil = 6f;
            shake = 7f;
            shootY = 12f;
            drawer = new DrawTurret(){{
                parts.add(new RegionPart("-arm"){{ progress = PartProgress.recoil; moveY = 5f; }});
            }};
            shootSound = Sounds.shootArtillerySapBig;
            shootEffect = Fx.shootBig2;
            smokeEffect = Fx.shootBigSmoke2;
            consumePower(5f); //runs the arm, and charges battery shells
            shellType = new PayloadCatapult.PayloadShellBulletType(3f, 120f){{
                lifetime = 140f;
            }};
        }};

        //Casing Fabricator: Serpulo's answer to the Constructor, limited to the catapult's shells; builds them empty onto a payload conveyor
        casingFabricator = new Constructor("casing-fabricator"){{
            requirements(Category.units, with(Items.titanium, 120, Items.silicon, 80, Items.graphite, 80, industrialBearing, 6, precisionComponent, 4));
            size = 3;
            filter = PayloadCatapult.shells();
            minBlockSize = 1;
            maxBlockSize = 3;
            consumePower(2f);
        }};

        firingFeel();
    }

    /**
     * How each turret's shot looks and feels (user, 2026-11-25: turret shooting should feel finished). Most of them used the
     * bullet's default small flash, the same as a Duo's, whatever their size; none threw casings. Set on the turret, so
     * every ammo gets the same: a flash and smoke that fit the weapon (gunfire, a pneumatic puff for launchers, sparks for
     * energy weapons), spent casings for guns that fire cartridges, and screen shake that grows with size. The heat glow
     * after a shot comes from each turret's -heat sprite (all have one).
     */
    static void firingFeel(){
        feel(rivet, Fx.shootSmall, Fx.shootSmallSmoke, Fx.casing1, 0f);             //1x1 spinning gun: small muzzle flash, brass
        feel(sickle, Fx.sparkShoot, Fx.shootSmallSmoke, Fx.none, 1f);               //sawblade flung out: sparks off the rail
        feel(kestrel, Fx.shootBig, Fx.shootBigSmoke, Fx.casing2, 1.5f);             //flak cannon: big flash, big casings
        feel(minelayer, Fx.shootSmokeSquare, Fx.shootSmallSmoke, Fx.none, 1f);      //mines lobbed by compressed air
        feel(acidMortar, Fx.shootSmokeSquare, Fx.shootBigSmoke, Fx.none, 2f);       //acid shells: a gas-driven mortar, no fire
        feel(cloudbreaker, Fx.shootSmokeSquare, Fx.shootSmallSmoke, Fx.none, 1f);   //aerial mines puffed into the air
        feel(tremor, Fx.shootSmokeSquareBig, Fx.none, Fx.none, 2f);                 //shockwave: a ring of dust kicked up
        feel(thunderhead, Fx.lightningShoot, Fx.shootSmallSmoke, Fx.none, 1f);
        feel(piledriver, Fx.shootBig, Fx.shootBigSmoke, Fx.casing3, 3f);            //tungsten slugs: heavy casings
        feel(apiary, Fx.shootSmokeSquare, Fx.none, Fx.none, 0f);                    //drones launched by catapult
        feel(primer, Fx.sparkShoot, Fx.shootSmallSmoke, Fx.none, 1.5f);             //an energy mark: sparks
        feel(stardustLauncher, Fx.shootSmokeSquare, Fx.shootSmallSmoke, Fx.none, 0.5f);
        feel(eventHorizon, Fx.lancerLaserShoot, Fx.shootBigSmoke2, Fx.none, 4f);
        //Bombard and Payload Catapult had their own big effects already; the Bombard's shells now leave casings too
        ((Turret)bombard).ammoUseEffect = Fx.casing3;
    }

    static void feel(Block block, Effect shoot, Effect smoke, Effect casing, float shakeAmount){
        Turret t = (Turret)block;
        t.shootEffect = shoot;
        t.smokeEffect = smoke;
        t.ammoUseEffect = casing;
        t.shake = Math.max(t.shake, shakeAmount); //never less than a turret already had
    }

    /** Everything the Stardust Launcher fires: each item's properties set what its cloud adds to the blast. */
    public static Item[] stardustItems(){
        return new Item[]{Items.sand, Items.scrap, Items.coal, Items.copper, Items.lead, Items.graphite, Items.metaglass, Items.titanium, Items.silicon,
            Items.thorium, Items.sporePod, Items.pyratite, Items.blastCompound, Items.plastanium, Items.phaseFabric, Items.surgeAlloy, sulfur, ammoniumNitrate};
    }

    /**
     * Blast compound ammo marks what it hits with vanilla's Blasted, as vanilla's own blast ammo does (players' report,
     * 2026-11-25). A shell that only drops a mine (no damage of its own) puts it on the mine, which does the damage.
     */
    static BulletType blasted(BulletType b){
        BulletType hits = b.fragBullet != null && b.damage <= 0f && b.splashDamage <= 0f ? b.fragBullet : b;
        hits.status = StatusEffects.blasted;
        return b;
    }

    /** The Bombard's shell: a slow, high arc (about 3.5 s to full range), a marked landing spot and a big blast. */
    static BulletType siegeShell(float splash, float radius, Color front, Color back){
        return new Bombard.SiegeShellBulletType(2.8f, splash, radius){{
            lifetime = 205f;
            width = height = 20f;
            frontColor = front;
            backColor = back;
            trailEffect = Fx.artilleryTrailSmoke;
            trailInterval = 3f;
            hitEffect = Fx.massiveExplosion;
            despawnEffect = Fx.none;
            hitSound = Sounds.explosionArtilleryShockBig;
            hitShake = 6f;
            buildingDamageMultiplier = 1.5f; //a siege gun: it breaks enemy bases
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
    static BulletType aerialMine(float dmg, float radius, float ammoPerItem){
        //not "damage": inside {{ }} that would read the bullet's own field (dmg * 0.4)
        return new Arsenal.AerialMineBulletType(dmg * 0.4f){{
            splashDamage = dmg;
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
    public static eclipse.type.MineBulletType mine(float splash, float radius, float speed){
        return new eclipse.type.MineBulletType(speed, 0f, "eclipse-mine"){{
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
