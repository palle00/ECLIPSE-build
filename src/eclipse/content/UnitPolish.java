package eclipse.content;

import arc.graphics.*;
import arc.struct.*;
import mindustry.entities.part.*;
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
        "frostwake", "spitter", "reaper", "slicker", "ew-drone", "wraith", "wasp", "lurker", "mite", "skiff");

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
