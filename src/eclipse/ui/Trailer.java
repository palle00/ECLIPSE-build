package eclipse.ui;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import eclipse.content.*;
import eclipse.systems.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.io.*;
import mindustry.maps.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;

import static mindustry.Vars.*;

/**
 * Developer trailer mode (photos.ps1 -Trailer): a 30-second teaser played in real time on the trailer arena
 * (arena.ps1), with the game's own sound and the boss music, recorded by OBS.
 * <ol>
 * <li>The eclipse forming; the fortress; the Swarm, the Dominion and the Tide coming in.</li>
 * <li>The turret line holding against a constant stream of enemies, in quick cuts.</li>
 * <li>The sky going dark, the bosses arriving stronger, a pull-back over the whole front; the end card.</li>
 * </ol>
 * Started by the photo flag "trailer:&lt;signal folder&gt;|&lt;arena map&gt;". After a warm-up the game writes "ready"
 * into the signal folder and waits for "go" (written once OBS records), plays the trailer, writes "done" and quits.
 */
public class Trailer{
    static final float length = 30f, warmup = 8f, musicStart = 12f;
    static final int night = 1, flash = 2;

    /** A shot: start time, camera from/to (tiles), zoom from/to, caption, flags. Each shot lasts until the next one starts. */
    record Shot(float at, float x0, float y0, float x1, float y1, float zoom0, float zoom1, String caption, int flags){}

    /** Filled in when the arena is loaded (turret spots come from its rule tags). */
    static Shot[] shots;
    static final float openerEnd = 3f, cardStart = 25f;

    static Fi signals;
    /** 0 loading, 1 warming up, 2 waiting for "go", 3 playing, 4 done. */
    static int phase;
    static float clock, t;
    static int shot = -1;
    static boolean blockStatus;
    static @Nullable arc.audio.Music music;
    static final Rand rand = new Rand(5);
    /** The timeline's one-off events, by time; each fires once. */
    static final FloatSeq eventTimes = new FloatSeq();
    static final Seq<Runnable> events = new Seq<>();
    static int nextEvent;

    public static void start(String args){
        String[] p = args.split("\\|");
        if(p.length < 2){
            Log.err("[ECLIPSE] trailer: expected signals|arena, got @", args);
            Core.app.exit();
            return;
        }
        signals = new Fi(p[0]);
        signals.mkdirs();
        signals.child("ready").delete();
        signals.child("go").delete();
        signals.child("done").delete();
        MenuBackdrop.skipIntro();
        PhotoShoot.running = true; //(keeps the welcome hint away)
        if(!Core.graphics.isFullscreen()) Core.graphics.setFullscreen(true);
        Time.runTask(120f, () -> {
            try{
                Map map = MapIO.createMap(new Fi(p[1]), true);
                control.playMap(map, map.rules());
            }catch(Throwable e){
                Log.err("[ECLIPSE] trailer: could not load the arena", e);
                Core.app.exit();
                return;
            }
            //no random or boss music: the trailer plays its own
            control.sound.musicChance = control.sound.musicWaveChance = 0f;
            control.sound.stop();
            blockStatus = Core.settings.getBool("blockstatus", true);
            Core.settings.put("blockstatus", false);
            script();
            phase = 1;
            Events.run(Trigger.update, Trailer::update);
            Core.app.addListener(new ApplicationListener(){
                @Override
                public void update(){
                    overlay();
                }
            });
        });
    }

    // ---- the script ----

    static void script(){
        String[] wall = state.rules.tags.get("trailer.wall", "106").split(",");
        float wx = Strings.parseFloat(wall[0], 106f);
        float[] thunder = turret("eclipse-thunderhead", wx - 12f, 60f), shadow = turret("foreshadow", wx - 22f, 70f),
            horizon = turret("eclipse-event-horizon", wx - 20f, 56f), tremor = turret("eclipse-tremor", wx - 10f, 48f);
        float riverY = waterY(140);

        shots = new Shot[]{
            new Shot(openerEnd, 30f, 86f, 40f, 72f, 3.4f, 3.0f, "FORGE AN INDUSTRY", 0),
            new Shot(5.6f, 152f, 74f, 142f, 72f, 3.6f, 3.9f, "THE SWARM", 0),
            new Shot(7.6f, 158f, 46f, 149f, 46f, 3.4f, 3.7f, "THE DOMINION", 0),
            new Shot(9.6f, 150f, riverY, 140f, riverY, 3.6f, 3.8f, "THE TIDE", 0),
            new Shot(11f, wx - 2f, 66f, wx + 2f, 64f, 2.6f, 2.8f, "HOLD THE LINE", 0),
            new Shot(12.6f, thunder[0] + 7f, thunder[1], thunder[0] + 9f, thunder[1] + 1f, 4.6f, 4.9f, "", 0),
            new Shot(13.8f, shadow[0] + 8f, shadow[1] - 1f, shadow[0] + 10f, shadow[1], 4.0f, 4.3f, "", 0),
            new Shot(15f, horizon[0] + 8f, horizon[1], horizon[0] + 9f, horizon[1] + 2f, 4.4f, 4.6f, "", 0),
            new Shot(16f, tremor[0] + 12f, tremor[1] + 4f, tremor[0] + 13f, tremor[1] + 6f, 3.8f, 3.5f, "", 0),
            new Shot(17f, wx + 4f, 64f, wx + 2f, 64f, 2.0f, 2.3f, "WHEN THE SKY GOES DARK", night | flash),
            new Shot(19.2f, wx + 6f, 62f, wx + 3f, 64f, 4.0f, 4.3f, "THEY GROW STRONGER", night),
            new Shot(21f, wx + 8f, 82f, wx + 4f, 80f, 3.6f, 3.9f, "", night),
            new Shot(22.6f, wx - 2f, 64f, wx - 8f, 62f, 3.6f, 1.7f, "", night),
        };

        //the Swarm, the Dominion and the Tide, each a moment before its shot, east of the camera, heading west
        at(5.3f, () -> {
            group(Factions.mite, 30, 152f, 172f, 60f, 86f);
            group(Factions.spitter, 8, 158f, 172f, 62f, 84f);
            group(Factions.scrabba, 2, 160f, 166f, 68f, 78f);
            group(Factions.brood, 2, 162f, 170f, 66f, 80f);
            group(Factions.wasp, 8, 160f, 178f, 60f, 88f);
        });
        at(7.3f, () -> {
            group(MilitaryUnits.bulwark, 4, 158f, 170f, 36f, 56f);
            group(MilitaryUnits.rampart, 3, 160f, 172f, 36f, 56f);
            group(MilitaryUnits.reaper, 4, 156f, 168f, 38f, 54f);
            group(MilitaryUnits.colossus, 1, 166f, 170f, 44f, 48f);
            group(MilitaryUnits.wraith, 3, 166f, 182f, 34f, 58f);
            group(MilitaryUnits.valkyrie, 2, 168f, 182f, 38f, 54f);
        });
        at(9.3f, () -> {
            naval(Factions.skiff, 3, 146f, 166f);
            naval(Factions.slicker, 3, 148f, 168f);
            naval(Factions.cinderwake, 1, 152f, 160f);
            naval(Factions.frostwake, 1, 156f, 164f);
        });
        //a constant stream at the wall from here on
        for(float s = 8.5f; s < 24f; s += 1f){
            at(s, () -> {
                group(Factions.mite, 4, wx + 14f, wx + 24f, 28f, 100f);
                group(Factions.spitter, 1, wx + 16f, wx + 24f, 30f, 98f);
                group(MilitaryUnits.reaper, 1, wx + 18f, wx + 26f, 30f, 98f);
                if(rand.chance(0.5)) group(Factions.wasp, 2, wx + 18f, wx + 30f, 30f, 98f);
            });
        }
        //the eclipse: the sky darkens, the bosses come in stronger
        at(17.4f, () -> EclipseEvent.start(100));
        at(18.4f, () -> {
            group(Factions.behemoth, 2, wx + 12f, wx + 18f, 54f, 70f);
            group(MilitaryUnits.annihilator, 1, wx + 14f, wx + 18f, 58f, 66f);
            group(MilitaryUnits.colossus, 2, wx + 14f, wx + 20f, 50f, 76f);
            group(Factions.brood, 4, wx + 12f, wx + 20f, 50f, 78f);
            group(Factions.mite, 20, wx + 10f, wx + 22f, 40f, 88f);
        });
        at(20.4f, () -> {
            group(MilitaryUnits.leviathan, 1, wx + 14f, wx + 18f, 78f, 84f);
            group(MilitaryUnits.tempest, 2, wx + 12f, wx + 20f, 74f, 88f);
            group(MilitaryUnits.valkyrie, 4, wx + 12f, wx + 22f, 72f, 90f);
        });
    }

    static void at(float time, Runnable r){
        int i = 0;
        while(i < eventTimes.size && eventTimes.get(i) <= time) i++;
        eventTimes.insert(i, time);
        events.insert(i, r);
    }

    /** A turret's position in tiles from the arena's tags ("name:x,y;..."), or a fallback. */
    static float[] turret(String name, float fx, float fy){
        for(String s : state.rules.tags.get("trailer.turrets", "").split(";")){
            String[] kv = s.split(":");
            if(kv.length == 2 && kv[0].equals(name)){
                String[] xy = kv[1].split(",");
                return new float[]{Strings.parseFloat(xy[0], fx), Strings.parseFloat(xy[1], fy)};
            }
        }
        Log.warn("[ECLIPSE] trailer: no @ in the arena", name);
        return new float[]{fx, fy};
    }

    /** The middle of the deep water in a column (the river), or 16. */
    static float waterY(int x){
        float sum = 0f;
        int n = 0;
        for(int y = 0; y < world.height(); y++){
            Tile t = world.tile(x, y);
            if(t != null && t.floor().isDeep()){
                sum += y;
                n++;
            }
        }
        return n == 0 ? 16f : sum / n;
    }

    static void group(@Nullable UnitType type, int count, float x0, float x1, float y0, float y1){
        if(type == null) return;
        for(int i = 0; i < count; i++) spawn(type, rand.random(x0, x1), rand.random(y0, y1));
    }

    /** Boats on deep water in a stretch of the river. */
    static void naval(@Nullable UnitType type, int count, float x0, float x1){
        if(type == null) return;
        for(int i = 0; i < count; i++){
            int x = (int)rand.random(x0, x1);
            spawn(type, x, waterY(x) + rand.range(1f));
        }
    }

    static void spawn(UnitType type, float tx, float ty){
        Unit u = type.create(state.rules.waveTeam);
        u.set(tx * tilesize, ty * tilesize);
        u.rotation = 180f;
        u.add();
    }

    // ---- running it ----

    static void update(){
        if(!state.isGame() || phase == 0 || phase >= 4) return;
        ui.hudfrag.shown = false;
        float dt = Time.delta / 60f;
        if(phase == 1){
            aim(shots[0], 0f);
            if((clock += dt) >= warmup){
                signals.child("ready").writeString("ready");
                phase = 2;
                Log.info("[ECLIPSE] trailer: ready, waiting for the recording to start");
            }
            return;
        }
        if(phase == 2){
            aim(shots[0], 0f);
            if(signals.child("go").exists()){
                phase = 3;
                t = 0f;
                music = Musics.boss1;
                music.setVolume(0.85f);
                music.setLooping(false);
                music.play();
                music.setPosition(musicStart);
                Log.info("[ECLIPSE] trailer: playing");
            }else if((clock += dt) >= warmup + 90f){
                Log.err("[ECLIPSE] trailer: the recording never started");
                finish();
            }
            return;
        }
        t += dt;
        while(nextEvent < events.size && eventTimes.get(nextEvent) <= t) events.get(nextEvent++).run();
        //music fades out over the end card
        if(music != null) music.setVolume(0.85f * (1f - smooth(length - 2f, length, t)));
        int current = -1;
        for(int i = 0; i < shots.length; i++) if(t >= shots[i].at) current = i;
        Shot s = shots[Math.max(current, 0)];
        if(current != shot){
            shot = current;
            //a cut: no zoom easing from the last shot
            renderer.targetscale = renderer.camerascale = s.zoom0;
            PhotoShoot.lighting((s.flags & night) != 0);
            state.rules.ambientLight.a = 0.5f;
        }
        float end = current + 1 < shots.length ? shots[current + 1].at : cardStart - 0.4f;
        aim(s, (t - s.at) / (end - s.at));
        if(t >= length) finish();
    }

    static void finish(){
        if(phase == 4) return;
        phase = 4;
        if(music != null) music.stop();
        EclipseEvent.end();
        Core.settings.put("blockstatus", blockStatus);
        signals.child("done").writeString("done");
        Log.info("[ECLIPSE] trailer: done");
        //a moment of black for the recording's tail, then quit
        Time.runTask(90f, Core.app::exit);
    }

    /** The camera between a shot's two points (tiles), eased, kept inside the map. */
    static void aim(Shot s, float f){
        if(control.input instanceof DesktopInput d) d.panning = true;
        float e = Interp.smooth.apply(Mathf.clamp(f)), zoom = Mathf.lerp(s.zoom0, s.zoom1, e);
        renderer.targetscale = renderer.camerascale = zoom;
        float halfW = Core.graphics.getWidth() / zoom / 2f, halfH = Core.graphics.getHeight() / zoom / 2f;
        float x = Mathf.lerp(s.x0, s.x1, e) * tilesize, y = Mathf.lerp(s.y0, s.y1, e) * tilesize;
        //six tiles in from the map's edge, where the game draws its dark border
        float margin = 6f * tilesize, maxX = world.width() * tilesize - tilesize / 2f, maxY = world.height() * tilesize - tilesize / 2f;
        x = (halfW + margin) * 2f >= maxX ? maxX / 2f : Mathf.clamp(x, halfW + margin, maxX - halfW - margin);
        y = (halfH + margin) * 2f >= maxY ? maxY / 2f : Mathf.clamp(y, halfH + margin, maxY - halfH - margin);
        Core.camera.position.set(x, y);
    }

    // ---- the overlay ----

    static void overlay(){
        if(phase == 0 || phase >= 5) return;
        float w = Core.graphics.getWidth(), h = Core.graphics.getHeight();
        Draw.proj(0f, 0f, w, h);
        if(phase < 3) black(w, h, 1f);
        else if(phase == 4) black(w, h, 1f);
        else if(t < openerEnd) opener(w, h, t);
        else if(t >= cardStart) endCard(w, h, t - cardStart);
        else{
            Shot s = shots[Math.max(shot, 0)];
            float end = shot + 1 < shots.length ? shots[shot + 1].at : cardStart - 0.4f;
            letterbox(w, h);
            caption(w, h, s, t - s.at, end - s.at);
            if((s.flags & flash) != 0){
                float f = t - 17.4f;
                if(f > 0f && f < 0.35f){
                    Draw.color(1f, 1f, 1f, 0.75f * (1f - f / 0.35f));
                    Fill.crect(0f, 0f, w, h);
                }
            }
            //into the opener's black and out to the end card
            black(w, h, 1f - smooth(openerEnd, openerEnd + 0.25f, t) + smooth(cardStart - 0.4f, cardStart - 0.15f, t));
        }
        Draw.flush();
        Draw.reset();
    }

    static float smooth(float from, float to, float v){
        return Interp.smooth.apply(Mathf.clamp((v - from) / (to - from)));
    }

    static void black(float w, float h, float alpha){
        if(alpha <= 0f) return;
        Draw.color(0f, 0f, 0f, Math.min(alpha, 1f));
        Fill.crect(0f, 0f, w, h);
    }

    static void letterbox(float w, float h){
        float bar = h * 0.07f;
        Draw.color(Color.black);
        Fill.crect(0f, 0f, w, bar);
        Fill.crect(0f, h - bar, w, bar);
    }

    static void text(String s, float x, float y, float scale, Color c, float alpha, int align){
        if(alpha <= 0.001f || s.isEmpty()) return;
        Font font = Fonts.outline;
        float old = font.getData().scaleX;
        font.getData().setScale(scale);
        font.setColor(c.r, c.g, c.b, alpha);
        font.draw(s, x, y, align);
        font.setColor(Color.white);
        font.getData().setScale(old);
    }

    /** The eclipse over the grid, the logo under it: the opener and the end card. */
    static void title(float w, float h, float ticks, float introSeconds, float logoAlpha){
        float px = h / 1080f, cx = w / 2f, cy = h * 0.6f, r = h * 0.19f;
        Draw.color(MenuBackdrop.background);
        Fill.crect(0f, 0f, w, h);
        MenuBackdrop.gridLines(0f, 0f, w, h);
        if(introSeconds < 2.6f) MenuBackdrop.drawIntro(cx, cy, r, ticks, introSeconds, px);
        else MenuBackdrop.drawEclipse(cx, cy, r, ticks, 1f, px);
        TextureRegion logo = Core.atlas.find("eclipse-logo"), glow = Core.atlas.find("eclipse-logo-glow");
        if(logoAlpha > 0f && logo.found()){
            float lw = w * 0.36f, lh = lw * logo.height / logo.width;
            MenuBackdrop.drawLogo(logo, glow.found() ? glow : null, cx, h * 0.24f, lw, lh, logoAlpha);
        }
    }

    /** The eclipse forming, fast, out of black. */
    static void opener(float w, float h, float t){
        title(w, h, t * 120f, t * 1.05f, 0f);
        black(w, h, 1f - smooth(0f, 0.3f, t) + smooth(openerEnd - 0.2f, openerEnd, t));
    }

    /** A big centred caption low on the screen, punching in and fading out; the shot's cut comes from black. */
    static void caption(float w, float h, Shot s, float time, float shotLength){
        float px = h / 1080f;
        if(!s.caption.isEmpty()){
            float in = smooth(0.05f, 0.3f, time), alpha = in * (1f - smooth(shotLength - 0.35f, shotLength - 0.1f, time));
            if(alpha > 0f){
                float scale = (2.9f + 0.5f * (1f - in)) * px, y = h * 0.2f;
                Draw.color(MenuBackdrop.gold, alpha);
                float bar = 240f * px * in;
                Fill.crect(w / 2f - bar / 2f, y - 74f * px, bar, 4f * px);
                text(s.caption, w / 2f, y, scale, Color.white, alpha, Align.center);
            }
        }
        black(w, h, 0.6f * (1f - smooth(0f, 0.12f, time)));
    }

    static void endCard(float w, float h, float t){
        title(w, h, (cardStart + t) * 120f, 9f, smooth(0.2f, 0.9f, t));
        float px = h / 1080f;
        text("VERSION 1.0", w / 2f, h * 0.165f, 2f * px, MenuBackdrop.gold, smooth(0.8f, 1.4f, t), Align.center);
        text("A mod for Mindustry  -  on the Steam Workshop", w / 2f, h * 0.1f, 1.4f * px, Color.lightGray, smooth(1.3f, 1.9f, t), Align.center);
        black(w, h, 1f - smooth(0f, 0.5f, t) + smooth(length - cardStart - 0.8f, length - cardStart, t));
    }
}
