package eclipse.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.*;

/**
 * The ECLIPSE deployment sequence, played instead of vanilla's core landing whenever a new game starts (not PvP, not
 * loading a save). Its own setting turns it off; it plays even when vanilla's "skip core animation" is on. About 7 seconds:
 * <ol>
 * <li>Awakening: black screen, an orange point grows into the eclipse of the main menu, its corona lighting up.</li>
 * <li>Scan: an orange scanning ring expands out of the eclipse.</li>
 * <li>Through the eclipse: the eclipse grows past the screen and its dark disc opens onto the planet far below.</li>
 * <li>Deployment: the core pod descends onto the landing zone on an orange thruster trail while the camera dives in.</li>
 * <li>Activation: touchdown, and a pulse of orange energy spreads across the terrain.</li>
 * </ol>
 * Everything is drawn in world space over the map; the core building itself is already in place under the pod.
 */
public class DeploymentIntro implements LaunchAnimator{
    public static final String setting = "eclipse-intro";
    /** Ticks: the whole sequence, and the moments each phase starts. */
    static final float duration = 420f, tEclipse = 60f, tScan = 110f, tOpen = 165f, tOpened = 245f, tLand = 335f;
    static final Color dark = Color.valueOf("060709"), disk = Color.valueOf("07080a"), gold = Color.valueOf("ffd37f"),
        amber = Color.valueOf("f59e0b"), orange = Color.valueOf("ff8a3d"), cyan = Color.valueOf("38bdf8");

    final CoreBuild core;
    float time;
    boolean scanned, touched;

    DeploymentIntro(CoreBuild core){
        this.core = core;
    }

    public static void install(){
        //vanilla starts its landing (unless "skip core animation" is on) in the same trigger; this listener is registered
        //later, so it runs after: it takes over vanilla's landing, or starts the intro itself when vanilla skipped it
        Events.run(Trigger.newGame, () -> {
            if(headless || player == null || state.rules.pvp || !Core.settings.getBool(setting, true)) return;
            CoreBuild core = player.bestCore();
            if(core == null) return;
            Core.camera.position.set(core);
            renderer.showLanding(new DeploymentIntro(core));
            player.deathTimer = Player.deathDelay - duration; //no respawn before the core is down (as vanilla's landing)
            Log.info("[ECLIPSE] deployment intro playing (vanilla landing @)", Core.settings.getBool("skipcoreanimation") ? "was skipped" : "replaced");
        });
        Log.info("[ECLIPSE] deployment intro installed (setting @ = @)", setting, Core.settings.getBool(setting, true));
    }

    // ---- LaunchAnimator ----

    @Override
    public void beginLaunch(boolean launching){
        time = 0f;
    }

    @Override
    public void endLaunch(){
    }

    @Override
    public float launchDuration(){
        return duration;
    }

    @Override
    public void updateLaunch(){
        time += Time.delta;
        if(!scanned && time >= tScan){
            scanned = true;
            Sounds.shieldWave.play(0.6f);
        }
        if(!touched && time >= tLand){
            touched = true;
            Sounds.coreLand.at(core.x, core.y, 1f, 1f);
            Effect.shake(5f, 40f, core);
            Fx.launch.at(core);
        }
    }

    /** Camera zoom: far out while the planet is revealed, diving to the normal zoom as the pod comes down. */
    @Override
    public float zoomLaunch(){
        float near = renderer.targetscale, far = Math.max(renderer.minScale(), near * 0.3f);
        return Mathf.lerp(far, near, Interp.pow3InInverse.apply(Mathf.clamp((time - tOpened + 30f) / (tLand - tOpened + 30f))));
    }

    @Override
    public void drawLaunch(){
    }

    @Override
    public void drawLaunchGlobalZ(){
        float cx = Core.camera.position.x, cy = Core.camera.position.y, w = Core.camera.width, h = Core.camera.height;
        float span = Math.max(w, h), r = Math.min(w, h) * 0.17f;

        drawPod(cx, cy);
        if(time >= tLand) drawPulse();

        //awakening to the opening: everything outside the eclipse is dark
        if(time < tOpened){
            float open = Interp.pow3In.apply(Mathf.clamp((time - tOpen) / (tOpened - tOpen)));
            float er = time < tEclipse ? r * Interp.pow2Out.apply(time / tEclipse) : r;
            er = Mathf.lerp(er, span * 1.3f, open);
            //darkness around the eclipse (a thick ring, so the disc can open onto the world)
            Draw.color(dark);
            Lines.stroke(span * 2f);
            Lines.circle(cx, cy, er + span);
            //the disc itself stays dark until it opens
            Draw.color(disk, 1f - open);
            Fill.circle(cx, cy, er);
            drawEclipse(cx, cy, er, Mathf.clamp((time - tEclipse * 0.5f) / (tScan - tEclipse * 0.5f)) * (1f - open * 0.6f), time < tEclipse);
            if(time >= tScan && time < tOpen + 40f) drawScan(cx, cy, r, span);
            drawTitle(cx, cy - r * 1.9f, "ECLIPSE  //  DEPLOYMENT PROTOCOL", Mathf.clamp((time - tEclipse) / 30f) * (1f - Mathf.clamp((time - tOpen) / 30f)));
        }else if(time < tLand){
            drawTitle(core.x, core.y - core.block.size * 4f - 10f, "DEPLOYMENT ZONE CONFIRMED", Mathf.slope(Mathf.clamp((time - tOpened) / (tLand - tOpened))) * 1.4f);
        }
        //a flash at touchdown
        if(time >= tLand){
            Draw.color(gold, 0.45f * (1f - Mathf.clamp((time - tLand) / 25f)));
            Fill.crect(cx - w, cy - h, w * 2f, h * 2f);
        }
        Draw.reset();
    }

    /** The menu's eclipse: a corona (gold through amber to cyan), the dark disc with a gold rim; while forming, a growing point. */
    void drawEclipse(float cx, float cy, float r, float light, boolean forming){
        if(forming){
            Draw.blend(Blending.additive);
            Draw.color(orange, 0.9f);
            Fill.circle(cx, cy, Math.max(r * 0.25f, 1.5f));
            Draw.color(gold, 0.35f);
            Fill.circle(cx, cy, r * 0.6f);
            Draw.blend();
        }
        int steps = 40;
        float inner = r * 0.94f, outer = r * 2.6f, step = (outer - inner) / steps;
        Lines.stroke(step * 1.15f);
        for(int i = 0; i < steps; i++){
            float f = (i + 0.5f) / steps;
            if(f < 0.18f) Tmp.c1.set(gold).a(0.55f).lerp(Tmp.c2.set(amber).a(0.22f), f / 0.18f);
            else if(f < 0.5f) Tmp.c1.set(amber).a(0.22f).lerp(Tmp.c2.set(cyan).a(0.06f), (f - 0.18f) / 0.32f);
            else Tmp.c1.set(cyan).a(0.06f).lerp(Tmp.c2.set(dark).a(0f), (f - 0.5f) / 0.5f);
            Tmp.c1.a *= light;
            Draw.color(Tmp.c1);
            Lines.circle(cx, cy, inner + step * (i + 0.5f));
        }
        //two rings turning opposite ways
        float angle = time * 0.4f;
        Lines.stroke(r * 0.012f);
        Draw.color(gold, 0.35f * light);
        Lines.dashCircle(cx, cy, r * 1.38f);
        Draw.color(cyan, 0.25f * light);
        Lines.poly(cx, cy, 6, r * 1.62f, angle);
        Lines.poly(cx, cy, 6, r * 1.62f, -angle * 0.8f + 30f);
        Draw.color(gold, 0.9f * light);
        Lines.stroke(r * 0.03f);
        Lines.circle(cx, cy, r);
    }

    /** Scanning rings expanding out of the eclipse. */
    void drawScan(float cx, float cy, float r, float span){
        for(int i = 0; i < 2; i++){
            float f = Mathf.clamp((time - tScan - i * 18f) / 70f);
            if(f <= 0f) continue;
            Draw.color(orange, 0.75f * (1f - f));
            Lines.stroke(r * 0.04f * (1f - f * 0.6f));
            Lines.circle(cx, cy, Mathf.lerp(r * 1.1f, span * 0.75f, Interp.pow2Out.apply(f)));
        }
    }

    /** The core pod: falls from high above (drawn large) onto the core, on an orange thruster trail. */
    void drawPod(float cx, float cy){
        if(time < tOpen + 20f) return;
        float f = Mathf.clamp((time - tOpen - 20f) / (tLand - tOpen - 20f)), fall = Interp.pow2In.apply(f);
        float size = core.block.size * 8f, scale = time >= tLand ? 1f : Mathf.lerp(5f, 1f, fall);
        float s = size * scale;
        //shadow, offset more the higher the pod is
        Draw.color(0f, 0f, 0f, 0.35f * f);
        Fill.rect(core.x + (scale - 1f) * 6f, core.y - (scale - 1f) * 6f, size * 1.05f, size * 1.05f);
        //thruster trail streaming back up from the pod
        if(time < tLand + 20f){
            float trail = time >= tLand ? 1f - (time - tLand) / 20f : 1f;
            Draw.blend(Blending.additive);
            for(int i = 0; i < 6; i++){
                float k = i / 6f;
                Draw.color(orange, 0.28f * (1f - k) * trail);
                Fill.circle(core.x, core.y + s * 0.3f + k * s * 1.4f, s * (0.42f - k * 0.05f));
            }
            Draw.color(gold, 0.6f * trail);
            Fill.circle(core.x, core.y, s * 0.36f);
            Draw.blend();
        }
        //the pod: the core in an orange-rimmed deployment frame
        Draw.color();
        Draw.rect(core.block.fullIcon, core.x, core.y, s, s);
        Draw.color(orange, time >= tLand ? 1f - Mathf.clamp((time - tLand) / 40f) : 0.9f);
        Lines.stroke(Math.max(1f, s * 0.04f));
        Lines.square(core.x, core.y, s * 0.56f);
        for(int i = 0; i < 4; i++){
            float a = 45f + i * 90f;
            Fill.circle(core.x + Angles.trnsx(a, s * 0.75f), core.y + Angles.trnsy(a, s * 0.75f), s * 0.05f);
        }
    }

    /** Activation: an energy pulse spreading over the terrain from the core. */
    void drawPulse(){
        for(int i = 0; i < 3; i++){
            float f = Mathf.clamp((time - tLand - i * 10f) / 75f);
            if(f <= 0f || f >= 1f) continue;
            float rad = Interp.pow2Out.apply(f) * 38f * 8f;
            Draw.color(orange, 0.6f * (1f - f));
            Lines.stroke(4f * (1f - f) + 0.6f);
            Lines.circle(core.x, core.y, rad);
            Draw.color(gold, 0.12f * (1f - f));
            Lines.stroke(rad * 0.15f);
            Lines.circle(core.x, core.y, rad * 0.93f);
        }
    }

    /** A line of text in world space at a fixed size on screen. */
    void drawTitle(float x, float y, String text, float alpha){
        if(alpha <= 0.01f) return;
        Font font = Fonts.outline;
        boolean ints = font.usesIntegerPositions();
        font.setUseIntegerPositions(false);
        float scale = 0.9f / Math.max(renderer.getScale(), 0.0001f); //UI-sized on screen at any zoom
        font.draw(text, x, y, Tmp.c1.set(gold).a(alpha), scale, false, Align.center);
        font.setUseIntegerPositions(ints);
        font.setColor(Color.white);
    }
}
