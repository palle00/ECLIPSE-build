package eclipse.content;

import arc.*;
import arc.graphics.*;
import arc.struct.*;
import eclipse.type.*;
import mindustry.content.*;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

import static eclipse.content.MilitaryUnits.*;
import static mindustry.Vars.*;

/**
 * Military phase M4: enemy factions that join the waves of every new custom game. The map's own (vanilla) waves keep
 * running every wave; the factions come on top of them in their own rhythms, so they take turns instead of stacking:
 * - The Swarm, every 2nd wave: many cheap self-repairing biological machines, acid and spores, units that split or breed;
 *   at full health their regeneration grows an exoskeleton (ExoskeletonAbility); Scrabba, a melee scorpion, is its late boss;
 * - The Dominion, every 3rd wave: few heavy, armoured units from the ECLIPSE tank and strike lines, shields on later waves;
 * - The Tide, every 5th wave: amphibious boats that cross water and land (slow on land), spraying water, oil, cryofluid
 *   or slag to set up status reactions for each other's guns and lightning, and laying mines (1.0 addition; enemy-only);
 *   the Lamprey leaps from water onto turrets and clamps on, freezing them (LatchAbility).
 * - every 6th wave the Swarm and the Dominion arrive together (a joint assault); every 10th the Tide joins the Swarm.
 * Applied once per game, on a new game or the first load of an older save (tag "eclipse-faction" in the rules, so the save
 * keeps it; an older save with only the first two factions gets the Tide added). PvP and the editor are never touched.
 */
public class Factions{
    public static final String tag = "eclipse-faction";
    public static final int swarm = 1, dominion = 2, tide = 3;
    /** Wave rhythm of each faction: the Swarm on waves 2, 4, 6...; the Dominion on waves 3, 6, 9... (0-based wave indices). */
    public static final int swarmSpacing = 2, swarmPhase = 1, dominionSpacing = 3, dominionPhase = 2, tideSpacing = 5, tidePhase = 4;

    public static UnitType mite, wasp, spitter, brood, behemoth, scrabba;
    /** The Tide's amphibious boats (enemy-only). */
    public static UnitType skiff, slicker, frostwake, cinderwake, maelstrom, lamprey;
    /** The Tide's submarines (enemy-only); they only come from spawn points on water. */
    public static UnitType lurker, barracuda, abyss;

    static final Color acid = Color.valueOf("8f8a3a");

    public static void load(){
        mite = swarmMech("mite", t -> {
            t.health = 70f;
            t.speed = 0.9f;
            t.hitSize = 8f;
            t.abilities.add(regen(0.1f), new ExoskeletonAbility(30f, 0.1f));
            t.weapons.add(new Weapon(){{
                mirror = false;
                x = 0f;
                reload = 24f;
                shootCone = 30f;
                shootSound = Sounds.none;
                bullet = new LiquidBulletType(EclipseLiquids.acidicWaste){{
                    damage = 7f;
                    speed = 2.5f;
                    lifetime = 20f;
                    puddleSize = 3f;
                    orbSize = 3f;
                }};
            }});
        });
        wasp = swarmFlyer("wasp", t -> {
            t.health = 140f;
            t.speed = 3.2f;
            t.hitSize = 9f;
            t.accel = 0.12f;
            t.drag = 0.05f;
            t.abilities.add(regen(0.15f), new ExoskeletonAbility(50f, 0.15f));
            t.weapons.add(new Weapon(){{
                mirror = false;
                x = 0f;
                reload = 30f;
                shootSound = Sounds.shoot;
                bullet = new BasicBulletType(3.5f, 10f){{
                    lifetime = 35f;
                    width = 5f;
                    height = 9f;
                    status = StatusEffects.corroded;
                    statusDuration = 60f * 2f;
                    frontColor = Color.valueOf("c9d36a");
                    backColor = acid;
                }};
            }});
        });
        spitter = swarmMech("spitter", t -> {
            t.health = 320f;
            t.speed = 0.55f;
            t.armor = 2f;
            t.hitSize = 12f;
            t.abilities.add(regen(0.25f), new ExoskeletonAbility(120f, 0.25f));
            t.weapons.add(new Weapon(){{
                mirror = false;
                x = 0f;
                reload = 70f;
                shootSound = Sounds.shootArtillerySap;
                bullet = new ArtilleryBulletType(2.2f, 10f){{
                    splashDamage = 25f;
                    splashDamageRadius = 20f;
                    lifetime = 90f;
                    width = height = 10f;
                    frontColor = Color.valueOf("c9d36a");
                    backColor = acid;
                    status = StatusEffects.corroded;
                    statusDuration = 60f * 3f;
                    puddleLiquid = EclipseLiquids.acidicWaste;
                    puddles = 4;
                    puddleRange = 10f;
                    puddleAmount = 20f;
                }};
            }});
        });
        brood = swarmMech("brood", t -> {
            t.health = 950f;
            t.speed = 0.45f;
            t.armor = 5f;
            t.hitSize = 18f;
            t.abilities.add(regen(0.5f), new ExoskeletonAbility(350f, 0.5f), new SpawnDeathAbility(mite, 4, 12f));
            t.weapons.add(new Weapon(){{
                mirror = false;
                x = 0f;
                reload = 40f;
                shoot.shots = 3;
                inaccuracy = 15f;
                shootSound = Sounds.shootSap;
                bullet = new BasicBulletType(2.5f, 14f){{
                    lifetime = 40f;
                    width = height = 8f;
                    status = StatusEffects.sporeSlowed;
                    statusDuration = 60f * 3f;
                    frontColor = Color.valueOf("b37aff");
                    backColor = Color.valueOf("7457ce");
                }};
            }});
        });
        behemoth = swarmMech("behemoth", t -> {
            t.health = 9000f;
            t.speed = 0.35f;
            t.armor = 10f;
            t.hitSize = 30f;
            //its Mites are laid as eggs that hatch after 3 s: splash can wipe a clutch before it hatches
            t.abilities.add(regen(2f), new ExoskeletonAbility(2500f, 2f), new EggSpawnAbility(mite, 60f * 6f, 0f, -8f));
            t.weapons.add(new Weapon(){{
                mirror = false;
                x = 0f;
                reload = 90f;
                shootSound = Sounds.shootArtillerySapBig;
                bullet = new ArtilleryBulletType(2.4f, 30f){{
                    splashDamage = 80f;
                    splashDamageRadius = 34f;
                    lifetime = 100f;
                    width = height = 16f;
                    frontColor = Color.valueOf("c9d36a");
                    backColor = acid;
                    status = StatusEffects.corroded;
                    statusDuration = 60f * 4f;
                    puddleLiquid = EclipseLiquids.acidicWaste;
                    puddles = 8;
                    puddleRange = 18f;
                    puddleAmount = 30f;
                }};
            }});
        });

        //Scrabba: the Swarm's late boss, a giant scorpion and the first melee unit. Its claws crush everything in a 3x3 area
        //in front of it (walls included); its stinger saps (heals it by part of the damage) and leaves the target Sapped
        scrabba = swarmMech("scrabba", t -> {
            t.health = 16000f;
            t.speed = 0.42f;
            t.armor = 14f;
            t.hitSize = 34f;
            t.rotateSpeed = 2.2f;
            t.abilities.add(regen(2.5f), new ExoskeletonAbility(3000f, 2.5f));
            t.weapons.add(new Weapon(){{
                x = 11f;
                y = 14f;
                alternate = true;
                reload = 50f;
                shootCone = 45f;
                shootY = 11f; //the crush lands on the block in front of the claw (pressed against a wall: its centre)
                shootSound = Sounds.shootSmite;
                bullet = new ExplosionBulletType(160f, 13f){{ //radius 13 around a block: it and its 8 neighbours
                    killShooter = false;
                    splashDamagePierce = true; //every block in the area, not only the first one facing it
                    rangeOverride = 30f;
                    buildingDamageMultiplier = 2f;
                    hitEffect = Fx.dynamicExplosion;
                    hitColor = Color.valueOf("c9d36a");
                    shootEffect = Fx.none;
                    smokeEffect = Fx.none;
                }};
            }}, new Weapon(){{
                mirror = false;
                x = 0f;
                y = -4f;
                rotate = true;
                rotateSpeed = 4f;
                reload = 45f;
                shootSound = Sounds.shootSap;
                bullet = new SapBulletType(){{
                    sapStrength = 0.6f;
                    length = 72f;
                    damage = 70f;
                    status = StatusEffects.sapped;
                    statusDuration = 60f * 4f;
                    color = Color.valueOf("c9d36a");
                    width = 0.6f;
                }};
            }});
        });

        // ---- the Tide: amphibious boats. Their liquids set up reactions (wet + shock, tarred + fire, freezing) for each other ----
        skiff = tideBoat("skiff", t -> {
            t.health = 160f;
            t.speed = 1.5f;
            t.hitSize = 9f;
            t.weapons.add(spray(Liquids.water, 5f, 30f, 0f), new Weapon(){{
                x = 4f;
                reload = 20f;
                shootSound = Sounds.shoot;
                bullet = new BasicBulletType(2.6f, 9f){{
                    lifetime = 40f;
                    width = 6f;
                    height = 8f;
                }};
            }});
        });
        slicker = tideBoat("slicker", t -> {
            t.health = 420f;
            t.speed = 1.2f;
            t.armor = 2f;
            t.hitSize = 12f;
            t.weapons.add(spray(Liquids.oil, 7f, 34f, 0f), new Weapon(){{
                x = 5.6f;
                reload = 40f;
                shootSound = Sounds.shootFlame;
                bullet = new BasicBulletType(2.4f, 12f){{
                    lifetime = 45f;
                    width = 7f;
                    height = 9f;
                    status = StatusEffects.burning;
                    statusDuration = 60f * 3f;
                    makeFire = true;
                    frontColor = Color.valueOf("ffd29a");
                    backColor = Color.valueOf("ff8a3d");
                }};
            }});
        });
        frostwake = tideBoat("frostwake", t -> {
            t.health = 900f;
            t.speed = 1f;
            t.armor = 4f;
            t.hitSize = 15f;
            t.weapons.add(spray(Liquids.cryofluid, 9f, 36f, 0f), mineDropper(180f, 60f, 28f));
            t.weapons.get(1).y = -8.4f; //the mine rack 34 px behind the centre of its 123 px hull
        });
        cinderwake = tideBoat("cinderwake", t -> {
            t.health = 3000f;
            t.speed = 0.8f;
            t.armor = 8f;
            t.hitSize = 20f;
            t.weapons.add(spray(Liquids.slag, 14f, 40f, 8.4f), mineDropper(150f, 80f, 30f), shock(14f, 12, 90f));
            t.weapons.get(1).y = -8.4f; //the mine rack 34 px behind the centre of its 157 px hull
        });
        maelstrom = tideBoat("maelstrom", t -> {
            t.health = 14000f;
            t.speed = 0.6f;
            t.armor = 12f;
            t.hitSize = 34f;
            t.abilities.add(regen(1f));
            t.weapons.add(spray(Liquids.water, 18f, 44f, -12.4f), spray(Liquids.cryofluid, 18f, 44f, 12.4f), mineDropper(90f, 120f, 34f), shock(24f, 16, 60f));
            t.weapons.get(2).y = -16.3f; //the mine racks at the stern of its 236 px hull
        });

        //the Lamprey: a robotic eel that leaps from the water onto a turret and clamps on, freezing and draining it until killed
        lamprey = tideBoat("lamprey", t -> {
            t.health = 520f;
            t.speed = 1.7f;
            t.armor = 3f;
            t.hitSize = 10f;
            t.abilities.add(new LatchAbility());
            t.weapons.add(shock(8f, 6, 45f));
        });

        // ---- the Tide's submarines: they dive when not attacking (only mines and sonar reach them then) ----
        lurker = sub("lurker", false, t -> {
            t.health = 300f;
            t.speed = 1.1f;
            t.hitSize = 10f;
            t.weapons.add(torpedoes(45f, 90f, 0f, 160f));
            t.weapons.get(0).y = 8.4f; //the torpedo tube at the bow of its 81 px hull
        });
        barracuda = sub("barracuda", false, t -> {
            t.health = 1600f;
            t.speed = 0.9f;
            t.armor = 5f;
            t.hitSize = 16f;
            t.weapons.add(torpedoes(70f, 70f, 5.8f, 200f), new Weapon(){{
                y = 4.35f;
                rotate = true;
                mirror = false;
                x = 0f;
                reload = 25f;
                shootSound = Sounds.shootDuo;
                bullet = new BasicBulletType(3f, 14f){{
                    lifetime = 50f;
                    width = 7f;
                    height = 9f;
                }};
            }});
            t.weapons.get(0).y = 14f; //the torpedo tubes at the bow of its 139 px hull
        });
        //the nuclear submarine: never has to surface, launches long-range missiles from below
        abyss = sub("abyss", true, t -> {
            t.health = 18000f;
            t.speed = 0.6f;
            t.armor = 14f;
            t.hitSize = 36f;
            t.weapons.add(torpedoes(120f, 60f, 16f, 220f), new Weapon(){{
                mirror = false;
                x = 0f;
                y = 9.6f;
                reload = 60f * 6f;
                shootCone = 180f;
                inaccuracy = 4f;
                shootSound = Sounds.shootMissile;
                bullet = new MissileBulletType(3.6f, 80f){{
                    lifetime = 140f;
                    width = 10f;
                    height = 16f;
                    homingPower = 0.08f;
                    splashDamage = 500f;
                    splashDamageRadius = 56f;
                    status = StatusEffects.burning;
                    statusDuration = 60f * 4f;
                    hitEffect = Fx.massiveExplosion;
                    hitShake = 4f;
                    frontColor = Color.valueOf("fff1d6");
                    backColor = Color.valueOf("ff795e");
                    trailColor = backColor;
                }};
            }});
        });

        Events.on(EventType.PlayEvent.class, e -> apply());
        //custom-game saves from before the factions get them the first time they load (once: the tag marks it)
        Events.on(EventType.WorldLoadEndEvent.class, e -> apply());
    }

    /** Enemy-only units: never researched or built; the database still shows them. */
    static UnitType swarmMech(String name, arc.func.Cons<UnitType> setup){
        UnitType t = new UnitType(name){{
            constructor = MechUnit::create;
            mechStepParticles = true;
            mechLegColor = Color.valueOf("4a5a3a");
            shieldColor = ExoskeletonAbility.chitin; //the exoskeleton's hit flash
        }};
        setup.get(t);
        return t;
    }

    static UnitType swarmFlyer(String name, arc.func.Cons<UnitType> setup){
        UnitType t = new UnitType(name){{
            constructor = UnitEntity::create;
            flying = true;
            circleTarget = true;
            shieldColor = ExoskeletonAbility.chitin;
        }};
        setup.get(t);
        return t;
    }

    /** A Tide boat: a hovering ground unit, so it crosses water and land (slow on land; see {@link AmphibiousAbility}). Enemy-only. */
    static UnitType tideBoat(String name, arc.func.Cons<UnitType> setup){
        UnitType t = new UnitType(name){{
            constructor = UnitEntity::create;
            hovering = true; //no drowning, no floor effects, and the game's hover path cost: over water and land alike
            canDrown = false;
            omniMovement = false;
            rotateSpeed = 3f;
            outlineColor = Pal.darkOutline;
            abilities.add(new AmphibiousAbility());
        }};
        setup.get(t);
        return t;
    }

    /** A Tide submarine (enemy-only): dives when not attacking; a nuclear one never surfaces. */
    static UnitType sub(String name, boolean nuclear, arc.func.Cons<UnitType> setup){
        UnitType t = new SubmarineUnitType(name){{
            rotateSpeed = 2.5f;
            abilities.add(new DiveAbility(nuclear));
        }};
        setup.get(t);
        return t;
    }

    /** Cavitation torpedoes: they run under the surface (nothing can shoot them down) and only through water. */
    static Weapon torpedoes(float dmg, float reloadTicks, float offsetX, float travel){
        //parameter names differ from Weapon/BulletType fields: in {{ }} an inherited field would shadow them
        return new Weapon(){{
            mirror = offsetX != 0f;
            x = offsetX;
            reload = reloadTicks;
            shootCone = 20f;
            shootSound = Sounds.shootMissileSmall;
            bullet = new TorpedoBulletType(2.4f, dmg){{
                lifetime = travel / 2.4f;
                splashDamage = dmg * 0.4f;
                splashDamageRadius = 16f;
                frontColor = Color.valueOf("d8f4ff");
                backColor = Color.valueOf("3e6fb0");
            }};
        }};
    }

    /** A liquid jet in front of the boat; the liquid's status (wet, tarred, freezing, melting) sets up reactions. */
    static Weapon spray(Liquid liquid, float dmg, float travel, float offsetX){
        return new Weapon(){{
            mirror = false;
            x = offsetX;
            reload = 6f;
            shootCone = 30f;
            inaccuracy = 6f;
            shootSound = Sounds.none;
            bullet = new LiquidBulletType(liquid){{
                damage = dmg;
                speed = 3f;
                lifetime = travel / 3f;
                puddleSize = 4f;
                orbSize = 3f;
                knockback = 0.6f;
            }};
        }};
    }

    /** Drops a mine behind the boat every so often: a trap for ground units following it. */
    static Weapon mineDropper(float reloadTicks, float splash, float radius){
        return new Weapon(){{
            mirror = false;
            x = 0f;
            y = -6f;
            reload = reloadTicks;
            shootCone = 360f;
            ignoreRotation = true;
            shootSound = Sounds.none;
            bullet = eclipse.content.blocks.MilitaryBlocks.mine(splash, radius, 0f);
        }};
    }

    /** An electric arc: extra damage on wet targets (vanilla's wet + shocked reaction). */
    static Weapon shock(float dmg, int length, float reloadTicks){
        return new Weapon(){{
            mirror = false;
            x = 0f;
            reload = reloadTicks;
            shootCone = 40f;
            shootSound = Sounds.shootArc;
            bullet = new LightningBulletType(){{
                damage = dmg;
                lightningLength = length;
                lightningColor = Color.valueOf("8fd8ff");
                status = StatusEffects.shocked;
            }};
        }};
    }

    static RegenAbility regen(float perTick){
        RegenAbility r = new RegenAbility();
        r.amount = perTick;
        return r;
    }

    /**
     * A faction's own world: only that faction attacks (user, 2026-11-18). Checked on every load, not once: the game
     * writes fresh vanilla waves when it invades a captured sector, and those are replaced too.
     */
    static boolean applyFactionWorld(){
        Planet planet = state.rules.planet;
        if(planet != EclipsePlanets.tide && planet != EclipsePlanets.hive && planet != EclipsePlanets.ironhold) return false;
        Seq<SpawnGroup> base = planet == EclipsePlanets.tide ? tideWorldWaves() : planet == EclipsePlanets.hive ? swarmWorldWaves() : dominionWorldWaves();
        Seq<SpawnGroup> own = planet == EclipsePlanets.tide ? new Seq<>(base).addAll(subWaves()) : base;
        ObjectSet<UnitType> ownTypes = new ObjectSet<>(), baseTypes = new ObjectSet<>();
        base.each(g -> baseTypes.add(g.type));
        own.each(g -> ownTypes.add(g.type));
        String worldTag = planet == EclipsePlanets.tide ? "tide-world" : planet == EclipsePlanets.hive ? "swarm-world" : "dominion-world";
        //already only this faction (and tagged): leave it, so a saved wave list keeps its exact groups
        //(an older save missing a unit type added since, like Scrabba or the Lamprey, gets the current list)
        if(worldTag.equals(state.rules.tags.get(tag)) && !state.rules.spawns.contains(g -> !ownTypes.contains(g.type))
            && !baseTypes.toSeq().contains(t -> !state.rules.spawns.contains(g -> g.type == t))) return true;
        state.rules.spawns = own;
        state.rules.tags.put(tag, worldTag);
        if(!headless) ui.showInfoToast(Core.bundle.get("eclipse-factions-" + worldTag), 6f);
        return true;
    }

    /**
     * Whether shared waves are fair to change: always outside PvP; in PvP only when the wave team is nobody's side
     * (owns no core), since the waves then attack every team alike. Also used by the ECLIPSE wave event.
     */
    public static boolean neutralWaves(){
        return !state.rules.pvp || state.rules.waveTeam.cores().isEmpty();
    }

    /** New custom game, PvP map with waves or campaign sector: the map's waves stay, and the factions join them in their own rhythms. */
    public static void apply(){
        if(net.client() || state.rules.editor || !state.rules.waves || !neutralWaves()) return;
        //the factions' own worlds: only that faction
        if(applyFactionWorld()) return;
        String done = state.rules.tags.get(tag);
        if(done == null){
            //a new list, so the map's own wave list is never changed
            state.rules.spawns = new Seq<>(state.rules.spawns).addAll(waves(swarm)).addAll(waves(dominion)).addAll(waves(tide)).addAll(subWaves());
            state.rules.tags.put(tag, "swarm+dominion+tide+scrabba+lamprey");
            if(!headless) ui.showInfoToast(Core.bundle.get("eclipse-factions-start"), 6f);
            return;
        }
        if(!done.contains("tide")){
            //an older save that has the Swarm and the Dominion: the Tide joins (with its Lampreys)
            state.rules.spawns = new Seq<>(state.rules.spawns).addAll(waves(tide)).addAll(subWaves());
            done += "+tide+lamprey";
            state.rules.tags.put(tag, done);
            if(!headless) ui.showInfoToast(Core.bundle.get("eclipse-factions-tide"), 6f);
        }
        if(!done.contains("scrabba")){
            //an older save from before Scrabba and the Lamprey: they join their factions' rhythms
            Seq<SpawnGroup> newcomers = waves(swarm).select(g -> g.type == scrabba);
            if(!done.contains("lamprey")) newcomers.addAll(waves(tide).select(g -> g.type == lamprey));
            state.rules.spawns = new Seq<>(state.rules.spawns).addAll(newcomers);
            state.rules.tags.put(tag, done + "+scrabba" + (done.contains("lamprey") ? "" : "+lamprey"));
            if(!headless) ui.showInfoToast(Core.bundle.get("eclipse-factions-newcomers"), 6f);
        }
    }

    /**
     * One faction's groups. They join the map's waves, so they are smaller than a full wave set: the Swarm grows a little
     * on each of its waves, the Dominion sends a few heavy units. Every begin is on the faction's rhythm and every spacing
     * a multiple of it, so a faction never shows up outside its own waves. Sizes grow per appearance, not per wave.
     */
    public static Seq<SpawnGroup> waves(int faction){
        Seq<SpawnGroup> s = new Seq<>();
        if(faction == swarm){
            //the Swarm (waves 2, 4, 6...): mites from the start, flyers and spitters soon, breeders mid-game, a Behemoth every 12 waves
            s.add(group(mite, 1, -1, 2, 2, 1f, 0f, 16));
            s.add(group(wasp, 5, -1, 2, 1, 2f, 0f, 10));
            //no wave shields: the Swarm grows its own exoskeleton
            s.add(group(spitter, 9, -1, 4, 1, 2f, 0f, 6));
            s.add(group(brood, 15, -1, 4, 1, 3f, 0f, 4));
            SpawnGroup boss = group(behemoth, 23, -1, 12, 1, 0f, 0f, 2), scrabbaBoss = group(scrabba, 39, -1, 20, 1, 0f, 0f, 1);
            boss.effect = scrabbaBoss.effect = StatusEffects.boss;
            s.add(boss, scrabbaBoss);
        }else if(faction == tide){
            //the Tide (waves 5, 10, 15...): skiffs first, then oil and cryofluid boats, slag boats late, a Maelstrom every 20 waves from wave 35
            s.add(group(skiff, 4, -1, 5, 2, 1.5f, 0f, 12));
            s.add(group(slicker, 9, -1, 5, 1, 2f, 0f, 8));
            s.add(group(frostwake, 14, -1, 5, 1, 2.5f, 5f, 6));
            s.add(group(lamprey, 14, -1, 5, 1, 2.5f, 0f, 6));
            s.add(group(cinderwake, 24, -1, 10, 1, 3f, 10f, 4));
            SpawnGroup boss = group(maelstrom, 34, -1, 20, 1, 0f, 30f, 2);
            boss.effect = StatusEffects.boss;
            s.add(boss);
        }else{
            //the Dominion (waves 3, 6, 9...): few, heavy, armoured; shields from mid-game; Colossus and Leviathan bosses late
            s.add(group(reaper, 2, -1, 3, 1, 2f, 0f, 6));
            s.add(group(wraith, 5, -1, 3, 1, 3f, 0f, 5));
            s.add(group(bulwark, 8, -1, 6, 1, 3f, 5f, 4));
            s.add(group(interceptor, 11, -1, 6, 1, 3f, 5f, 4));
            s.add(group(rampart, 14, -1, 6, 1, 3f, 10f, 3));
            s.add(group(valkyrie, 17, -1, 6, 1, 3f, 10f, 3));
            s.add(group(annihilator, 26, -1, 9, 1, 3f, 20f, 2));
            s.add(group(tempest, 32, -1, 9, 1, 3f, 20f, 2));
            SpawnGroup colossusBoss = group(colossus, 38, -1, 15, 1, 0f, 40f, 2), leviathanBoss = group(leviathan, 47, -1, 15, 1, 0f, 40f, 2);
            colossusBoss.effect = leviathanBoss.effect = StatusEffects.boss;
            s.add(colossusBoss, leviathanBoss);
        }
        return s;
    }

    /** The Swarm on its own world (Hive): every wave, breeders and Behemoths sooner than elsewhere. */
    public static Seq<SpawnGroup> swarmWorldWaves(){
        Seq<SpawnGroup> s = new Seq<>();
        s.add(group(mite, 0, -1, 1, 3, 1f, 0f, 20));
        s.add(group(wasp, 2, -1, 2, 1, 2f, 0f, 12));
        s.add(group(spitter, 4, -1, 3, 1, 2f, 0f, 8));
        s.add(group(brood, 10, -1, 4, 1, 3f, 0f, 5));
        SpawnGroup boss = group(behemoth, 20, -1, 15, 1, 0f, 0f, 2), scrabbaBoss = group(scrabba, 29, -1, 20, 1, 0f, 0f, 1);
        boss.effect = scrabbaBoss.effect = StatusEffects.boss;
        s.add(boss, scrabbaBoss);
        return s;
    }

    /** The Dominion on its own world (Ironhold): every wave, heavy units and both bosses sooner than elsewhere. */
    public static Seq<SpawnGroup> dominionWorldWaves(){
        Seq<SpawnGroup> s = new Seq<>();
        s.add(group(reaper, 0, -1, 1, 1, 2f, 0f, 8));
        s.add(group(wraith, 1, -1, 2, 1, 2.5f, 0f, 6));
        s.add(group(bulwark, 5, -1, 3, 1, 3f, 5f, 5));
        s.add(group(interceptor, 6, -1, 3, 1, 3f, 5f, 5));
        s.add(group(rampart, 10, -1, 4, 1, 3f, 10f, 4));
        s.add(group(valkyrie, 12, -1, 4, 1, 3f, 10f, 4));
        s.add(group(annihilator, 18, -1, 6, 1, 3f, 20f, 2));
        s.add(group(tempest, 21, -1, 6, 1, 3f, 20f, 2));
        SpawnGroup colossusBoss = group(colossus, 29, -1, 15, 1, 0f, 40f, 2), leviathanBoss = group(leviathan, 37, -1, 15, 1, 0f, 40f, 2);
        colossusBoss.effect = leviathanBoss.effect = StatusEffects.boss;
        s.add(colossusBoss, leviathanBoss);
        return s;
    }

    /** The Tide on its own world: a wave every wave, the heavier boats on shorter rhythms than on other planets. */
    public static Seq<SpawnGroup> tideWorldWaves(){
        Seq<SpawnGroup> s = new Seq<>();
        s.add(group(skiff, 0, -1, 1, 2, 2f, 0f, 14));
        s.add(group(slicker, 2, -1, 2, 1, 2f, 0f, 8));
        s.add(group(frostwake, 6, -1, 3, 1, 2.5f, 5f, 6));
        s.add(group(lamprey, 4, -1, 2, 1, 2.5f, 0f, 8));
        s.add(group(cinderwake, 14, -1, 5, 1, 3f, 10f, 4));
        SpawnGroup boss = group(maelstrom, 24, -1, 15, 1, 0f, 30f, 2);
        boss.effect = StatusEffects.boss;
        s.add(boss);
        return s;
    }

    /**
     * The Tide's submarines, on the Tide's rhythm, pinned to each spawn point that is on water (a sub from a land spawn
     * would be stuck there). A map without a water spawn gets none.
     */
    public static Seq<SpawnGroup> subWaves(){
        Seq<SpawnGroup> s = new Seq<>();
        for(mindustry.world.Tile spawn : spawner.getSpawns()){
            if(!spawn.floor().isLiquid) continue;
            for(SpawnGroup g : subGroups()){
                g.spawn = spawn.pos();
                s.add(g);
            }
        }
        return s;
    }

    /** The submarine groups one water spawn point gets (not yet pinned to it); also what the wiki's unit pages read. */
    public static Seq<SpawnGroup> subGroups(){
        SpawnGroup lurkers = group(lurker, 9, -1, 5, 1, 2f, 0f, 6), barracudas = group(barracuda, 19, -1, 10, 1, 3f, 5f, 4), boss = group(abyss, 49, -1, 25, 1, 0f, 40f, 1);
        boss.effect = StatusEffects.boss;
        return Seq.with(lurkers, barracudas, boss);
    }

    static SpawnGroup group(UnitType type, int begin, int end, int spacing, int amount, float scaling, float shieldScaling, int max){
        SpawnGroup g = new SpawnGroup(type);
        g.begin = begin;
        g.end = end < 0 ? SpawnGroup.never : end;
        g.spacing = spacing;
        g.unitAmount = amount;
        g.unitScaling = scaling <= 0f ? SpawnGroup.never : scaling;
        g.shieldScaling = shieldScaling;
        g.max = max;
        return g;
    }
}
