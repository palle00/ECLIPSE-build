package eclipse.systems;

import arc.*;
import arc.graphics.*;
import arc.graphics.Camera;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.game.Rules.*;
import mindustry.gen.*;
import mindustry.graphics.*;

import static mindustry.Vars.*;

/**
 * The ECLIPSE: once the first boss of a game has spawned, every 5th wave has a chance to bring an eclipse. While it
 * lasts (until the next wave arrives) the wave team's units are tougher and hit harder, by a factor that grows with
 * the wave: x1.3 at wave 30, x2 at wave 200 (see {@link #multiplier}). The chance grows the same way: 30% at wave 30, certain
 * from wave 200 ({@link #chance}). The sky darkens and an eclipsed sun hangs over
 * the map.
 * The server decides and applies it (team rules, so it saves with the map); clients see it through the synced rules.
 */
public class EclipseEvent{
    public static final String bossTag = "eclipse-boss-seen", activeTag = "eclipse-eclipse";
    public static final int every = 5;

    /** Client side: how dark the sky is now (0-1), fading in and out; and whether the last frame had an eclipse. */
    static float shade;
    static boolean wasActive;

    public static void init(){
        Events.on(WaveEvent.class, e -> onWave());
        Events.on(WorldLoadEvent.class, e -> shade = (wasActive = active()) ? 1f : 0f);
        Events.run(Trigger.update, () -> {
            boolean now = state.isGame() && active();
            shade = Mathf.approachDelta(shade, now ? 1f : 0f, 1f / 180f);
            if(now != wasActive && !headless && state.isGame()){
                ui.showInfoToast(now ? Core.bundle.format("eclipse-eclipse.start", Strings.autoFixed(strength(), 2)) : Core.bundle.get("eclipse-eclipse.end"), 6f);
            }
            wasActive = now;
        });
        Events.run(Trigger.draw, EclipseEvent::draw);
    }

    /** How much stronger enemies are during an eclipse on this wave: 1 + 1% per wave up to 30 (x1.3), then on to x2 at wave 200. */
    public static float multiplier(int wave){
        return wave <= 30 ? 1f + wave * 0.01f : 1.3f + (wave - 30) * (0.7f / 170f);
    }

    /** The chance of an eclipse on an eligible wave: the same curve as its strength, so 10% at wave 10, 30% at wave 30, 100% from wave 200. */
    public static float chance(int wave){
        return Math.min(multiplier(wave) - 1f, 1f);
    }

    public static boolean active(){
        return state.rules.tags.containsKey(activeTag);
    }

    /** The current eclipse's strength, or 1 when there is none. */
    public static float strength(){
        return active() ? Strings.parseFloat(state.rules.tags.get(activeTag).split(";")[0], 1f) : 1f;
    }

    static void onWave(){
        if(net.client() || state.rules.pvp || state.rules.editor) return;
        int wave = state.wave - 1; //the wave that just arrived (as the HUD counted it); its spawn index is wave - 1
        end(); //an eclipse lasts until the next wave
        if(!state.rules.tags.containsKey(bossTag) && state.rules.spawns.contains(g -> g.effect == StatusEffects.boss && g.getSpawned(wave - 1) > 0)){
            state.rules.tags.put(bossTag, "true");
        }
        if(state.rules.tags.containsKey(bossTag) && wave % every == 0 && Mathf.chance(chance(wave))) start(wave);
    }

    /** Starts an eclipse for this wave: the wave team's units get tougher and hit harder until the next wave. */
    public static void start(int wave){
        if(active()) return;
        float m = multiplier(wave);
        TeamRule r = state.rules.teams.get(state.rules.waveTeam);
        //the team's own values are kept, so they come back exactly afterwards
        state.rules.tags.put(activeTag, m + ";" + r.unitHealthMultiplier + ";" + r.unitDamageMultiplier);
        r.unitHealthMultiplier *= m;
        r.unitDamageMultiplier *= m;
        sync();
    }

    public static void end(){
        if(!active()) return;
        String[] v = state.rules.tags.get(activeTag).split(";");
        TeamRule r = state.rules.teams.get(state.rules.waveTeam);
        r.unitHealthMultiplier = Strings.parseFloat(v[1], 1f);
        r.unitDamageMultiplier = Strings.parseFloat(v[2], 1f);
        state.rules.tags.remove(activeTag);
        sync();
    }

    static void sync(){
        if(net.server()) Call.setRules(state.rules);
    }

    static final Color sky = new Color(0.05f, 0.01f, 0.09f, 1f), corona = Color.valueOf("ffd9a0"), rim = Color.valueOf("fff1d6");

    /** A darkened sky and an eclipsed sun high over the view, with a slowly turning corona. */
    static void draw(){
        if(shade < 0.01f) return;
        Camera cam = Core.camera;
        Draw.z(Layer.weather + 1f);
        Draw.color(sky, 0.5f * shade);
        Fill.rect(cam.position.x, cam.position.y, cam.width, cam.height);

        float r = Math.min(cam.width, cam.height) * 0.07f;
        float cx = cam.position.x + cam.width * 0.32f, cy = cam.position.y + cam.height * 0.3f;
        Draw.color(corona, 0.12f * shade);
        Fill.circle(cx, cy, r * 2.2f);
        Draw.color(corona, 0.22f * shade);
        Fill.circle(cx, cy, r * 1.5f);
        Draw.color(rim, 0.6f * shade);
        for(int i = 0; i < 18; i++){
            float ang = i * 20f + Time.time * 0.15f, len = r * (0.35f + 0.35f * Mathf.absin(Time.time + i * 13f, 9f, 1f));
            Lines.stroke(r * 0.05f);
            Lines.lineAngle(cx + Angles.trnsx(ang, r * 1.02f), cy + Angles.trnsy(ang, r * 1.02f), ang, len);
        }
        Lines.stroke(r * 0.1f);
        Lines.circle(cx, cy, r * 1.02f);
        Draw.color(Color.black, 0.95f * shade);
        Fill.circle(cx, cy, r);
        Draw.reset();
    }
}
