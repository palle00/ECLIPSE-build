package eclipse.content;

import arc.*;
import arc.graphics.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;

import static eclipse.content.MilitaryUnits.*;
import static mindustry.Vars.*;

/**
 * Military phase M4: enemy factions that join the waves of every new custom game. The map's own (vanilla) waves keep
 * running every wave; the factions come on top of them in their own rhythms, so they take turns instead of stacking:
 * - The Swarm, every 2nd wave: many cheap self-repairing biological machines, acid and spores, units that split or breed;
 * - The Dominion, every 3rd wave: few heavy, armoured units from the ECLIPSE tank and strike lines, shields on later waves;
 * - every 6th wave both arrive together (a joint assault).
 * Applied once per game, on a new game or the first load of an older custom-game save (tag "eclipse-faction" in the rules,
 * so the save keeps it). Campaign, PvP and the editor are never touched.
 */
public class Factions{
    public static final String tag = "eclipse-faction";
    public static final int swarm = 1, dominion = 2;
    /** Wave rhythm of each faction: the Swarm on waves 2, 4, 6...; the Dominion on waves 3, 6, 9... (0-based wave indices). */
    public static final int swarmSpacing = 2, swarmPhase = 1, dominionSpacing = 3, dominionPhase = 2;

    public static UnitType mite, wasp, spitter, brood, behemoth;

    static final Color acid = Color.valueOf("8f8a3a");

    public static void load(){
        mite = swarmMech("mite", t -> {
            t.health = 70f;
            t.speed = 0.9f;
            t.hitSize = 8f;
            t.abilities.add(regen(0.1f));
            t.weapons.add(new Weapon(){{
                mirror = false;
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
            t.abilities.add(regen(0.15f));
            t.weapons.add(new Weapon(){{
                mirror = false;
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
            t.abilities.add(regen(0.25f));
            t.weapons.add(new Weapon(){{
                mirror = false;
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
            t.abilities.add(regen(0.5f), new SpawnDeathAbility(mite, 4, 12f));
            t.weapons.add(new Weapon(){{
                mirror = false;
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
            t.abilities.add(regen(2f), new UnitSpawnAbility(mite, 60f * 6f, 0f, -8f));
            t.weapons.add(new Weapon(){{
                mirror = false;
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
        }};
        setup.get(t);
        return t;
    }

    static UnitType swarmFlyer(String name, arc.func.Cons<UnitType> setup){
        UnitType t = new UnitType(name){{
            constructor = UnitEntity::create;
            flying = true;
            circleTarget = true;
        }};
        setup.get(t);
        return t;
    }

    static RegenAbility regen(float perTick){
        RegenAbility r = new RegenAbility();
        r.amount = perTick;
        return r;
    }

    /** New custom game: the map's waves stay, and both factions join them in their own rhythms. */
    public static void apply(){
        if(net.client() || state.isCampaign() || state.rules.editor || state.rules.pvp || !state.rules.waves || state.rules.tags.containsKey(tag)) return;
        //a new list, so the map's own wave list is never changed
        state.rules.spawns = new Seq<>(state.rules.spawns).addAll(waves(swarm)).addAll(waves(dominion));
        state.rules.tags.put(tag, "swarm+dominion");
        if(!headless) ui.showInfoToast(Core.bundle.get("eclipse-factions-start"), 6f);
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
            s.add(group(spitter, 9, -1, 4, 1, 2f, 3f, 6));
            s.add(group(brood, 15, -1, 4, 1, 3f, 8f, 4));
            SpawnGroup boss = group(behemoth, 23, -1, 12, 1, 0f, 20f, 2);
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
