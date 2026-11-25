package eclipse.content;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.part.*;
import mindustry.game.*;
import mindustry.entities.part.DrawPart.*;
import mindustry.type.*;

import static mindustry.Vars.*;

/**
 * Animated parts for ECLIPSE units, on top of what the game already moves (treads, legs, engines, weapon recoil):
 * a slow additive pulse over each unit's accent lights ("-glow": eye slits, vents, cores, glowing sacs), spinning
 * rotors on the rotor drones, and the Wasp's fluttering wings. Runs after every unit exists.
 */
public class UnitPolish{
    /** Units with a "-glow" sprite (the Logistics Drone has none: its only bright parts are its cargo). */
    static final ObjectSet<String> glowing = ObjectSet.with(
        "dreadnought", "colossus", "leviathan", "annihilator", "tempest", "abyss", "behemoth", "maelstrom", "rampart", "delta",
        "epsilon", "valkyrie", "cinderwake", "repair-drone", "shield-drone", "bulwark", "brood", "barracuda", "interceptor",
        "frostwake", "spitter", "reaper", "slicker", "ew-drone", "wraith", "wasp", "lurker", "mite", "skiff", "scrabba", "lamprey");

    public static void load(){
        for(UnitType u : content.units()){
            if(!u.name.startsWith("eclipse-")) continue;
            String n = u.name.substring("eclipse-".length());
            if(glowing.contains(n)) u.parts.add(glow());
            switch(n){
                case "logistics-drone" -> rotors(u, 3.4f, 3.25f);
                case "repair-drone" -> rotors(u, 3f, 2f);
                case "wasp" -> u.parts.add(new RegionPart("-wing"){{
                    mirror = true;
                    outline = false;
                    clampProgress = false;
                    progress = PartProgress.time.sin(1.4f, 1f);
                    moveRot = 9f;
                }});
                default -> {}
            }
        }
        firingAndDeaths();
    }

    // ---- firing and death effects (user, 2026-11-25: units should feel as finished as the turrets) ----

    /** The Swarm's guts: violet flesh and acid. */
    public static final Effect swarmBurst = new Effect(50f, 120f, e -> {
        float size = Math.max(1f, e.rotation);
        Draw.color(Color.valueOf("b866d6"), Color.valueOf("6a3778"), e.fin());
        Angles.randLenVectors(e.id, (int)(8 + size * 4), 4f + e.finpow() * (10f + size * 9f), (x, y) -> Fill.circle(e.x + x, e.y + y, (1.2f + size * 0.5f) * e.fout()));
        Draw.color(Color.valueOf("b6e26a"), e.fout() * 0.8f);
        Angles.randLenVectors(e.id + 7, (int)(4 + size * 2), 2f + e.finpow() * (6f + size * 6f), (x, y) -> Fill.circle(e.x + x, e.y + y, (0.8f + size * 0.3f) * e.fout()));
        Draw.reset();
    });

    /** A Tide boat going down: a ring of water and spray. */
    public static final Effect tideSplash = new Effect(60f, 140f, e -> {
        float size = Math.max(1f, e.rotation);
        Draw.color(Color.valueOf("d8eeff"), Color.valueOf("5a8fd8"), e.fin());
        Lines.stroke((2f + size * 0.6f) * e.fout());
        Lines.circle(e.x, e.y, 4f + e.finpow() * (12f + size * 10f));
        Angles.randLenVectors(e.id, (int)(10 + size * 4), 3f + e.finpow() * (10f + size * 8f), (x, y) -> Fill.circle(e.x + x, e.y + y, (1f + size * 0.35f) * e.fout()));
        Draw.reset();
    });

    static ObjectSet<UnitType> swarmUnits, tideUnits;

    /**
     * Weapons: the plain default flash (the same tiny one on a Mite and on the Maelstrom's main gun) becomes one that fits
     * the weapon: lasers and lightning flash, sprays splash, missiles and torpedoes launch with a puff, heavy guns get a
     * big flash and smoke, Dominion guns throw casings, and big shots shake the screen (bosses more). Weapons that already
     * had their own effect keep it.
     * Deaths: the Swarm bursts into flesh and acid, the Tide's boats go down in a splash, the Dominion's big machines blow
     * up in a massive explosion; every death shakes the screen by the unit's size.
     */
    static void firingAndDeaths(){
        swarmUnits = new ObjectSet<>();
        tideUnits = new ObjectSet<>();
        for(SpawnGroup g : Factions.waves(Factions.swarm)) swarmUnits.add(g.type);
        for(SpawnGroup g : Factions.waves(Factions.tide)) tideUnits.add(g.type);
        for(SpawnGroup g : Factions.subGroups()) tideUnits.add(g.type);

        for(UnitType u : content.units()){
            if(!u.name.startsWith("eclipse-")) continue;
            boolean swarm = swarmUnits.contains(u), tide = tideUnits.contains(u), boss = u.health >= 9000f;
            for(Weapon w : u.weapons){
                BulletType b = w.bullet;
                if(b == null || !(b.shootEffect == Fx.shootSmall || b.shootEffect == Fx.none)) continue;
                float hit = b.damage + b.splashDamage;
                if(b instanceof LaserBulletType || b instanceof ContinuousBulletType) b.shootEffect = Fx.lancerLaserShoot;
                else if(b instanceof LightningBulletType) b.shootEffect = Fx.lightningShoot;
                else if(b instanceof LiquidBulletType) b.shootEffect = Fx.shootLiquid;
                else if(b instanceof MissileBulletType || b instanceof eclipse.type.TorpedoBulletType) b.shootEffect = Fx.shootSmokeSquare;
                else if(b instanceof eclipse.type.MineBulletType) continue; //dropped behind, not fired
                else if(b instanceof ArtilleryBulletType || hit >= 90f){
                    b.shootEffect = Fx.shootBig;
                    b.smokeEffect = Fx.shootBigSmoke;
                }else if(hit >= 35f) b.shootEffect = Fx.shootBig;
                else continue; //a light gun: the small flash fits
                //cartridge guns of the Dominion (and the player's tanks) throw spent casings
                if(!swarm && !tide && b instanceof BasicBulletType && !(b instanceof MissileBulletType) && w.ejectEffect == Fx.none)
                    w.ejectEffect = hit >= 120f ? Fx.casing3 : Fx.casing2;
                float shake = hit >= 250f ? 2f : hit >= 90f ? 0.8f : 0f;
                w.shake = Math.max(w.shake, boss ? shake * 1.5f : shake);
            }

            if(swarm) u.deathExplosionEffect = swarmBurst;
            else if(tide) u.deathExplosionEffect = tideSplash;
            else if(u.health >= 5000f) u.deathExplosionEffect = Fx.massiveExplosion;
            u.deathShake = Math.max(u.deathShake, boss ? Math.max(4f, u.hitSize / 8f) : u.hitSize / 8f * 0.6f);
        }
    }

    /** The accent lights, pulsing slowly in their own colours. */
    static RegionPart glow(){
        return new RegionPart("-glow"){{
            outline = false;
            blending = Blending.additive;
            color = new Color(1f, 1f, 1f, 0.12f);
            colorTo = new Color(1f, 1f, 1f, 0.75f);
            progress = PartProgress.time.absin(24f, 1f);
            layerOffset = 0.003f;
        }};
    }

    /** Two pairs of rotors (front and back, mirrored left/right), spinning over the rotor pods. */
    static void rotors(UnitType u, float px, float py){
        //(the loop value is named ry: inside the part's initializer a plain "y" would be the part's own field)
        for(float ry : new float[]{py, -py}){
            u.parts.add(new RegionPart("-rotor"){{
                mirror = true;
                outline = false;
                clampProgress = false;
                x = px;
                y = ry;
                progress = PartProgress.time;
                moveRot = -26f;
                layerOffset = 0.002f;
            }});
        }
    }
}
