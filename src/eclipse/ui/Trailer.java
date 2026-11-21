package eclipse.ui;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import eclipse.content.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.io.*;
import mindustry.maps.*;
import mindustry.type.*;
import mindustry.ui.*;

import java.io.*;

import static mindustry.Vars.*;

/**
 * Developer trailer mode (photos.ps1 -Trailer): plays a scripted sequence on the photo scene and records it with
 * ffmpeg, one frame per game step at a fixed 30 fps, so the video is smooth however slow the capture is.
 * <ol>
 * <li>The opener: the eclipse forming over the factory grid (the menu's intro, slower), then the logo and "1.0".</li>
 * <li>Slow camera moves over the factories, each with a caption.</li>
 * <li>A Swarm and Dominion attack on the turret line, then an eclipse darkening the sky over the same fight.</li>
 * <li>The wiki's animated build example, and the end card.</li>
 * </ol>
 * Started by the photo flag "trailer:&lt;ffmpeg&gt;|&lt;scene map&gt;|&lt;output video&gt;"; the game quits when the video is written.
 */
public class Trailer{
    static final int fps = 30;
    static final int night = 1, enemies = 2, darken = 4, wiki = 8, right = 16;

    /** A shot: anchor (as in photo mode), camera offset in tiles and zoom at its start and end, length, caption, flags. */
    record Shot(String anchor, float dx0, float dy0, float dx1, float dy1, float zoom0, float zoom1, float seconds, String caption, String sub, int flags){}

    static final Shot[] shots = {
        new Shot("!core-shard", 14f, 9f, 21f, 12f, 2.1f, 2.45f, 4.5f, "A deeper industry", "Machine parts, chemistry, new metals and power", 0),
        new Shot("blast-furnace", -9f, 0f, -4f, 0f, 3.6f, 4.1f, 4f, "Heat and new metals", "Blast furnaces, heaters and alloys", 0),
        new Shot("electrolyzer", 0f, -4f, 4f, -4f, 4f, 4.4f, 4f, "Real chemistry", "Brine, acids, gases and plastics", 0),
        new Shot("component-depot", -9f, -3f, -4f, -3f, 4f, 4.4f, 3.5f, "One machine for every part", "Universal machines, power tiers and overclock", 0),
        new Shot("fission-reactor", 0f, -2f, 5f, -2f, 4f, 4.4f, 3.5f, "From steam to fusion", "New generators for every stage", 0),
        new Shot("lattice-drill", 8f, 1f, 13f, 1f, 3.1f, 3.5f, 3f, "Deeper ores", "Ore minerals with real byproducts", 0),
        new Shot("fission-reactor", -7f, 8f, -2f, 8f, 2.8f, 3.1f, 4f, "", "", night),
        new Shot("thunderhead", -6f, -6f, -6f, -2f, 3.6f, 3.9f, 6f, "Three enemy factions", "The Swarm, the Dominion and the Tide", enemies),
        new Shot("thunderhead", -6f, -2f, -6f, 0f, 3.9f, 3.4f, 6f, "Every fifth wave, the sky may go dark", "and the enemy grows stronger", darken),
        new Shot("!core-shard", 0f, 0f, 0f, 0f, 3f, 3f, 5f, "A wiki that shows you how", "Animated build examples you can place in one click", wiki | right),
    };
    static final float openerSeconds = 5.5f, endSeconds = 6f, warmup = 60f * 40f;

    static String ffmpeg, output;
    static @Nullable Process process;
    static @Nullable OutputStream pipe;
    static int width, height;
    /** 0 warming up, 1 opener, 2 shots, 3 end card, 4 done. */
    static int phase;
    static float warm;
    static int frame, shot, shotFrame;
    static boolean shotStarted;

    public static void start(String args){
        String[] p = args.split("\\|");
        if(p.length < 3){
            Log.err("[ECLIPSE] trailer: expected ffmpeg|scene|output, got @", args);
            Core.app.exit();
            return;
        }
        ffmpeg = p[0];
        output = p[2];
        MenuBackdrop.skipIntro();
        PhotoShoot.running = true; //(keeps the welcome hint away)
        Time.runTask(120f, () -> {
            try{
                Map map = MapIO.createMap(new Fi(p[1]), true);
                control.playMap(map, map.rules());
            }catch(Throwable e){
                Log.err("[ECLIPSE] trailer: could not load the scene", e);
                Core.app.exit();
                return;
            }
            Events.run(Trigger.update, Trailer::update);
            Core.app.addListener(new ApplicationListener(){
                @Override
                public void update(){
                    afterFrame();
                }
            });
        });
    }

    /** Before the frame is drawn: the camera, lighting and what happens in the shot. */
    static void update(){
        if(!state.isGame() || phase >= 4) return;
        ui.hudfrag.shown = false;
        if(phase == 0){
            aim(shots[0], 0f);
            if((warm += Time.delta) >= warmup) begin();
            return;
        }
        if(phase == 1){
            aim(shots[0], 0f);
            return;
        }
        if(phase == 2){
            Shot s = shots[shot];
            if(!shotStarted){
                shotStarted = true;
                PhotoShoot.lighting((s.flags & night) != 0);
                state.rules.ambientLight.a = 0.62f;
                if((s.flags & enemies) != 0) attack(s.anchor);
                if((s.flags & darken) != 0) eclipse.systems.EclipseEvent.start(60);
                if((s.flags & wiki) != 0){
                    Content furnace = content.getByName(ContentType.block, "eclipse-blast-furnace");
                    EclipseWiki wikiDialog = EclipseWiki.instance;
                    if(furnace instanceof UnlockableContent u && wikiDialog != null){
                        wikiDialog.show();
                        wikiDialog.pendingBuild = 1; //the full line (three furnaces into a freezer), not the single machine
                        wikiDialog.open(u, "build", true);
                    }
                }
                //a cut: no zoom easing from the last shot
                renderer.targetscale = renderer.camerascale = s.zoom0;
            }
            aim(s, shotFrame / (s.seconds * fps));
        }
    }

    /** Starts recording: ffmpeg reads raw frames from a pipe; the game steps 1/30 s per frame from now on. */
    static void begin(){
        width = Core.graphics.getWidth();
        height = Core.graphics.getHeight();
        try{
            new Fi(output).parent().mkdirs();
            ProcessBuilder b = new ProcessBuilder(ffmpeg, "-y", "-f", "rawvideo", "-pix_fmt", "rgba", "-s", width + "x" + height, "-r", String.valueOf(fps),
                "-i", "-", "-vf", "vflip,scale=1920:1080:flags=lanczos", "-c:v", "libx264", "-preset", "slow", "-crf", "17", "-pix_fmt", "yuv420p",
                "-movflags", "+faststart", output);
            b.redirectErrorStream(true);
            b.redirectOutput(new File(output + ".log"));
            process = b.start();
            pipe = new BufferedOutputStream(process.getOutputStream(), 1 << 22);
        }catch(Throwable e){
            Log.err("[ECLIPSE] trailer: could not start ffmpeg (" + ffmpeg + ")", e);
            Core.app.exit();
            return;
        }
        Time.setDeltaProvider(() -> 60f / fps);
        statusBefore = Core.settings.getBool("blockstatus", true);
        Core.settings.put("blockstatus", false);
        phase = 1;
        frame = 0;
        Log.info("[ECLIPSE] trailer: recording @x@ to @", width, height, output);
    }

    /** After the frame is drawn: the overlay (opener, captions, end card, fades), then the frame goes to ffmpeg. */
    static void afterFrame(){
        if(phase < 1 || phase >= 4 || pipe == null) return;
        float w = Core.graphics.getWidth(), h = Core.graphics.getHeight();
        if((int)w != width || (int)h != height){
            Log.err("[ECLIPSE] trailer: the window changed size while recording; stopping");
            finish();
            return;
        }
        Draw.proj(0f, 0f, w, h);
        float t = frame / (float)fps;
        if(phase == 1) opener(w, h, t);
        else if(phase == 2) caption(w, h, shots[shot], shotFrame / (float)fps);
        else endCard(w, h, t);
        Draw.flush();
        Draw.reset();
        try{
            pipe.write(ScreenUtils.getFrameBufferPixels(0, 0, width, height, false));
        }catch(Throwable e){
            Log.err("[ECLIPSE] trailer: ffmpeg stopped taking frames", e);
            finish();
            return;
        }
        frame++;
        if(phase == 1 && t >= openerSeconds){
            phase = 2;
            frame = 0;
            shot = shotFrame = 0;
            shotStarted = false;
        }else if(phase == 2){
            Shot s = shots[shot];
            if(++shotFrame >= s.seconds * fps){
                if((s.flags & wiki) != 0 && EclipseWiki.instance != null) EclipseWiki.instance.hide();
                shotFrame = 0;
                shotStarted = false;
                if(++shot >= shots.length){
                    phase = 3;
                    frame = 0;
                }
            }
        }else if(phase == 3 && t >= endSeconds){
            finish();
        }
    }

    static boolean statusBefore = true;

    static void finish(){
        phase = 4;
        Core.settings.put("blockstatus", statusBefore);
        Time.setDeltaProvider(() -> Math.min(Core.graphics.getDeltaTime() * 60f, 3f));
        try{
            if(pipe != null) pipe.close();
            if(process != null) process.waitFor();
            Log.info("[ECLIPSE] trailer written: @", output);
        }catch(Throwable e){
            Log.err("[ECLIPSE] trailer: ffmpeg did not finish", e);
        }
        pipe = null;
        Core.app.exit();
    }

    // ---- camera and action ----

    /** The camera on a shot's anchor, eased from its start to its end offset and zoom, kept inside the map. */
    static void aim(Shot s, float f){
        if(control.input instanceof DesktopInput d) d.panning = true;
        float e = Interp.smooth.apply(Mathf.clamp(f)), zoom = Mathf.lerp(s.zoom0, s.zoom1, e);
        renderer.targetscale = renderer.camerascale = zoom;
        Position at = PhotoShoot.anchor(s.anchor);
        if(at == null) return;
        float halfW = Core.graphics.getWidth() / zoom / 2f, halfH = Core.graphics.getHeight() / zoom / 2f;
        float x = at.getX() + Mathf.lerp(s.dx0, s.dx1, e) * tilesize, y = at.getY() + Mathf.lerp(s.dy0, s.dy1, e) * tilesize;
        //six tiles in from the map's edge, where the game draws its dark border
        float margin = 6f * tilesize, maxX = world.width() * tilesize - tilesize / 2f, maxY = world.height() * tilesize - tilesize / 2f;
        x = (halfW + margin) * 2f >= maxX ? maxX / 2f : Mathf.clamp(x, halfW + margin, maxX - halfW - margin);
        y = (halfH + margin) * 2f >= maxY ? maxY / 2f : Mathf.clamp(y, halfH + margin, maxY - halfH - margin);
        Core.camera.position.set(x, y);
    }

    /** The Swarm and the Dominion coming up from the south at the turret line: mites, spitters, wasps, a Scrabba, tanks. */
    static void attack(String anchorName){
        Position at = PhotoShoot.anchor(anchorName);
        if(at == null) return;
        Rand rand = new Rand(11);
        UnitType[] ground = {Factions.mite, Factions.mite, Factions.mite, Factions.mite, Factions.mite, Factions.mite, Factions.spitter, Factions.spitter,
            Factions.spitter, Factions.scrabba, MilitaryUnits.bulwark, MilitaryUnits.bulwark, MilitaryUnits.rampart};
        UnitType[] air = {Factions.wasp, Factions.wasp, Factions.wasp, Factions.wasp};
        for(UnitType type : ground) PhotoShoot.spawn(type, at.getX() + rand.range(10f) * tilesize, at.getY() - (8f + rand.random(3f)) * tilesize);
        for(UnitType type : air) PhotoShoot.spawn(type, at.getX() + rand.range(12f) * tilesize, at.getY() - (13f + rand.random(3f)) * tilesize);
    }

    // ---- the overlay ----

    static float smooth(float from, float to, float v){
        return Interp.smooth.apply(Mathf.clamp((v - from) / (to - from)));
    }

    static void black(float w, float h, float alpha){
        if(alpha <= 0f) return;
        Draw.color(0f, 0f, 0f, Math.min(alpha, 1f));
        Fill.crect(0f, 0f, w, h);
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

    static void opener(float w, float h, float t){
        //the eclipse forms (the menu's intro at 80% speed), then the logo and the version
        title(w, h, frame * 2f, t * 0.8f, smooth(3.1f, 3.8f, t));
        text("VERSION 1.0", w / 2f, h * 0.24f - h * 0.075f, 2f * h / 1080f, MenuBackdrop.gold, smooth(3.6f, 4.2f, t), Align.center);
        black(w, h, 1f - smooth(0f, 0.5f, t) + smooth(openerSeconds - 0.5f, openerSeconds, t));
    }

    /** A caption over the bottom left, on a soft dark band, fading in and out; a quick fade through black at the cuts. */
    static void caption(float w, float h, Shot s, float t){
        float px = h / 1080f, alpha = smooth(0.4f, 0.9f, t) * (1f - smooth(s.seconds - 0.8f, s.seconds - 0.35f, t));
        if(!s.caption.isEmpty() && alpha > 0f){
            float band = 320f * px, a = 0.72f * alpha;
            float bottom = Color.toFloatBits(0f, 0f, 0f, a), top = Color.toFloatBits(0f, 0f, 0f, 0f);
            Fill.quad(0f, 0f, bottom, w, 0f, bottom, w, band, top, 0f, band, top);
            boolean onRight = (s.flags & right) != 0;
            float barX = onRight ? w - 115f * px : 110f * px, textX = onRight ? w - 136f * px : 136f * px;
            int align = onRight ? Align.right : Align.left;
            //on the right (the wiki shot) lower down, under the wiki's page
            float lift = onRight ? -70f * px : 0f;
            Draw.color(MenuBackdrop.gold, alpha);
            Fill.crect(barX, 140f * px + lift, 5f * px, 100f * px);
            text(s.caption, textX, 240f * px + lift, 2.1f * px, Color.white, alpha, align);
            text(s.sub, textX + (onRight ? 0f : 2f * px), 176f * px + lift, 1.15f * px, Color.lightGray, alpha, align);
        }
        black(w, h, 1f - smooth(0f, 0.3f, t) + smooth(s.seconds - 0.3f, s.seconds, t));
    }

    static void endCard(float w, float h, float t){
        title(w, h, frame * 2f, 9f, 1f);
        float px = h / 1080f;
        text("VERSION 1.0", w / 2f, h * 0.165f, 2f * px, MenuBackdrop.gold, smooth(0.6f, 1.2f, t), Align.center);
        text("A mod for Mindustry  -  on the Steam Workshop", w / 2f, h * 0.1f, 1.4f * px, Color.lightGray, smooth(1f, 1.6f, t), Align.center);
        black(w, h, 1f - smooth(0f, 0.6f, t) + smooth(endSeconds - 0.8f, endSeconds, t));
    }
}
