package eclipse.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.core.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

/**
 * ECLIPSE main menu backdrop (setting "eclipse-menu", on by default): a dark industrial grid with light pulses running
 * along it like conveyors, a solar eclipse right of centre (a corona fading from gold through amber to a hint of cyan,
 * two dashed rings turning opposite ways, the black disk and its gold rim), drifting embers, scanlines and a vignette.
 * The ECLIPSE logo sits top left, drawn above the menu; vanilla's background and logo are hidden and its buttons are
 * replaced by {@link EclipseMenu}.
 */
public class MenuBackdrop extends Element{
    public static final String setting = "eclipse-menu";

    static final Color background = Color.valueOf("08080a"), grid = Color.valueOf("2a2e393d"), gold = Color.valueOf("ffd37f"),
        amber = Color.valueOf("f59e0b"), cyan = Color.valueOf("38bdf8"), core = Color.valueOf("07080a"), shade = Color.valueOf("060709"),
        crater = Color.valueOf("0c0e12"), craterLight = Color.valueOf("12151b");
    static final int pulseCount = 14, emberCount = 55;

    /** Layout grid for the logo (16:9, scaled to cover the screen), as the menu column lines up with it. */
    static final int cols = 64, rows = 36;
    /** Logo centre and width in that grid. */
    static final float logoX = 15f, logoY = 30.2f, logoW = 22f;

    TextureRegion logo;
    /** The eclipse C's corona, pulsing slowly over the logo. */
    TextureRegion logoGlow;
    float ts, ox, oy;
    /** Pulses: horizontal?, start position 0-1, lane (grid cells), speed per tick. Embers: x, y (0-1), radius, speed x/y, alpha. */
    final float[] pulses = new float[pulseCount * 4], embers = new float[emberCount * 6];

    /** Puts the backdrop into the main menu; while it is on, vanilla's background and logo are hidden. Call on ClientLoadEvent. */
    public static void install(){
        ui.settings.graphics.checkPref(setting, true);
        if(ui.menuGroup == null || ui.menuGroup.getChildren().isEmpty() || !(ui.menuGroup.getChildren().first() instanceof Group menu) || menu.getChildren().isEmpty()){
            Log.warn("[ECLIPSE] menu backdrop: main menu not found");
            return;
        }
        Element vanilla = menu.getChildren().first();
        vanilla.visible(() -> !Core.settings.getBool(setting, true));
        //vanilla's logo and version line: the last plain drawing element of the menu (Group.fill(DrawRect))
        for(int i = menu.getChildren().size - 1; i > 0; i--){
            Element e = menu.getChildren().get(i);
            if(!(e instanceof Group) && e.getClass().getName().startsWith("arc.scene.Group$")){
                e.visible(() -> !Core.settings.getBool(setting, true));
                break;
            }
        }
        MenuBackdrop backdrop = new MenuBackdrop();
        backdrop.visible(() -> Core.settings.getBool(setting, true));
        //right after the vanilla background: the buttons stay on top
        menu.addChildAt(1, backdrop);
        //our own buttons and the logo on top (desktop)
        EclipseMenu.install(menu, backdrop);
        Log.info("[ECLIPSE] menu backdrop installed");
    }

    MenuBackdrop(){
        touchable = arc.scene.event.Touchable.disabled;
        logo = Core.atlas.find("eclipse-logo");
        logoGlow = Core.atlas.find("eclipse-logo-glow");
        Rand r = new Rand(7);
        for(int i = 0; i < pulseCount; i++){
            pulses[i * 4] = i % 2 == 0 ? 1f : 0f;
            pulses[i * 4 + 1] = r.random(1f);
            pulses[i * 4 + 2] = r.random(11);
            pulses[i * 4 + 3] = r.random(0.0008f, 0.0023f) * (r.chance(0.5) ? 1f : -1f);
        }
        for(int i = 0; i < emberCount; i++){
            embers[i * 6] = r.random(1f);
            embers[i * 6 + 1] = r.random(1f);
            embers[i * 6 + 2] = r.random(0.5f, 2.3f);
            embers[i * 6 + 3] = r.range(0.175f);
            embers[i * 6 + 4] = r.random(0.1f, 0.55f);
            embers[i * 6 + 5] = r.random(0.2f, 0.9f);
        }
    }

    float sx(float tx){
        return ox + tx * ts;
    }

    float sy(float ty){
        return oy + ty * ts;
    }

    @Override
    public void act(float delta){
        super.act(delta);
        setBounds(0f, 0f, Core.graphics.getWidth(), Core.graphics.getHeight());
    }

    /** Screen x of the left edge of the logo. */
    float logoLeftScreen(){
        if(logo == null || !logo.found() || ts <= 0f) return Core.graphics.getWidth() * 0.06f;
        return sx(logoX) - logoW * ts / 2f;
    }

    /** Screen y of the bottom of the logo and its version line (for the menu column under it). */
    float logoBottomScreen(){
        if(logo == null || !logo.found() || ts <= 0f) return Core.graphics.getHeight() * 0.7f;
        float lh = logoW * ts * logo.height / logo.width;
        return sy(logoY) - lh / 2f - Scl.scl(30f);
    }

    @Override
    public void draw(){
        float w = width, h = height, t = Time.globalTime;
        ts = Math.max(w / cols, h / rows);
        ox = x + (w - cols * ts) / 2f;
        oy = y + (h - rows * ts) / 2f;

        Draw.color(background);
        Fill.crect(x, y, w, h);
        drawGrid(w, h, t);
        drawEclipse(w, h, t);
        drawEmbers(w, h, t);
        drawAtmosphere(w, h);
        Draw.reset();
    }

    /** The factory floor grid, and light pulses running along its lanes like items on conveyors. */
    void drawGrid(float w, float h, float t){
        float cell = Scl.scl(80f);
        Draw.color(grid);
        Lines.stroke(Scl.scl(1f));
        for(float gx = x; gx < x + w; gx += cell) Lines.line(gx, y, gx, y + h);
        for(float gy = y + h; gy > y; gy -= cell) Lines.line(x, gy, x + w, gy);
        float len = Scl.scl(20f), thick = Scl.scl(2f);
        for(int i = 0; i < pulseCount; i++){
            boolean horizontal = pulses[i * 4] > 0f;
            float pos = ((pulses[i * 4 + 1] + pulses[i * 4 + 3] * t) % 1f + 1f) % 1f;
            float lane = pulses[i * 4 + 2] * cell + cell / 2f;
            Draw.color(i % 3 == 0 ? cyan : gold, 0.45f);
            if(horizontal) Fill.crect(x + pos * w - len / 2f, y + h - (lane % h) - thick / 2f, len, thick);
            else Fill.crect(x + (lane % w) - thick / 2f, y + h - pos * h - len / 2f, thick, len);
        }
    }

    /** The eclipse: corona, two dashed rings turning opposite ways, the black disk and its gold rim. */
    void drawEclipse(float w, float h, float t){
        boolean narrow = w < Scl.scl(768f);
        float cx = x + w * (narrow ? 0.5f : 0.68f), cy = y + h * (1f - (narrow ? 0.35f : 0.48f)), r = Math.min(w, h) * (narrow ? 0.16f : 0.19f);

        //corona: gold at the rim, through amber to a faint cyan, fading out at 2.6 radii (as rings of the gradient)
        float inner = r * 0.92f, outer = r * 2.6f;
        int steps = 64;
        float step = (outer - inner) / steps;
        Lines.stroke(step + 1f);
        for(int i = 0; i < steps; i++){
            float f = (i + 0.5f) / steps;
            if(f < 0.18f) Tmp.c1.set(gold).a(0.55f).lerp(Tmp.c2.set(amber).a(0.22f), f / 0.18f);
            else if(f < 0.5f) Tmp.c1.set(amber).a(0.22f).lerp(Tmp.c2.set(cyan).a(0.06f), (f - 0.18f) / 0.32f);
            else Tmp.c1.set(cyan).a(0.06f).lerp(Tmp.c2.set(background).a(0f), (f - 0.5f) / 0.5f);
            Draw.color(Tmp.c1);
            Lines.circle(cx, cy, inner + step * (i + 0.5f));
        }

        //rings: gold (dash 12, gap 18, dash 4, gap 18) turning one way, cyan (dash 6, gap 24) the other way, a little slower
        float angle = t * 0.0022f * Mathf.radDeg;
        Lines.stroke(Scl.scl(1.2f));
        Draw.color(gold, 0.24f);
        dashed(cx, cy, r * 1.38f, angle, new float[]{12f, 18f, 4f, 18f});
        Draw.color(cyan, 0.2f);
        dashed(cx, cy, r * 1.65f, -angle * 0.8f, new float[]{6f, 24f});

        Draw.color(core);
        Fill.circle(cx, cy, r);
        //the inner rim catching light on the upper left: gold rings, offset that way, fading inward
        for(int i = 0; i < 10; i++){
            float f = i / 10f;
            Draw.color(gold, 0.05f * (1f - f));
            Lines.stroke(r * 0.03f);
            Lines.circle(cx - r * 0.02f * (1f - f), cy + r * 0.02f * (1f - f), r * (0.985f - f * 0.09f));
        }
        Draw.color(core);
        Fill.circle(cx + r * 0.05f, cy - r * 0.05f, r * 0.86f);
        //lunar surface: a few barely-there maria and craters
        Rand rand = new Rand(31);
        for(int i = 0; i < 14; i++){
            float a = rand.random(360f), d = r * (float)Math.sqrt(rand.random(0.72f)), cr = r * rand.random(0.05f, i < 4 ? 0.3f : 0.12f);
            Draw.color(i < 4 ? crater : craterLight, i < 4 ? 0.55f : 0.35f);
            Fill.circle(cx + Angles.trnsx(a, d), cy + Angles.trnsy(a, d), cr);
        }
        Draw.color(gold, 0.85f);
        Lines.stroke(Scl.scl(2f));
        Lines.circle(cx, cy, r);
    }

    /** A dashed circle: the pattern (dash, gap, dash, gap... in screen pixels) repeated around it, rotated by an angle. */
    void dashed(float cx, float cy, float radius, float rotation, float[] pattern){
        float circumference = Mathf.PI2 * radius, at = 0f;
        int k = 0;
        while(at < circumference){
            float len = Scl.scl(pattern[k % pattern.length]);
            if(k % 2 == 0){
                float a1 = rotation + at / circumference * 360f, a2 = rotation + Math.min(at + len, circumference) / circumference * 360f;
                Lines.line(cx + Angles.trnsx(a1, radius), cy + Angles.trnsy(a1, radius), cx + Angles.trnsx(a2, radius), cy + Angles.trnsy(a2, radius));
            }
            at += len;
            k++;
        }
    }

    /** Gold embers drifting up and sideways, wrapping around the screen. */
    void drawEmbers(float w, float h, float t){
        for(int i = 0; i < emberCount; i++){
            float ex = ((embers[i * 6] * w + embers[i * 6 + 3] * Scl.scl(1f) * t) % w + w) % w;
            float ey = (embers[i * 6 + 1] * h + embers[i * 6 + 4] * Scl.scl(1f) * t) % h;
            Draw.color(gold, embers[i * 6 + 5]);
            Fill.circle(x + ex, y + ey, Scl.scl(embers[i * 6 + 2]));
        }
    }

    /** Scanlines over everything, and a vignette that darkens the edges around the eclipse. */
    void drawAtmosphere(float w, float h){
        float line = Scl.scl(2f), period = Scl.scl(4f);
        Draw.color(0f, 0f, 0f, 0.09f);
        for(float sy = y; sy < y + h; sy += period) Fill.crect(x, sy, w, line);

        float cx = x + w * 0.68f, cy = y + h * 0.55f, diag = Mathf.dst(w, h), from = diag * 0.15f, to = diag * 0.85f;
        int steps = 48;
        float step = (to - from) / steps;
        Lines.stroke(step + 1f);
        for(int i = 0; i < steps; i++){
            Draw.color(shade, 0.78f * (i + 0.5f) / steps);
            Lines.circle(cx, cy, from + step * (i + 0.5f));
        }
        //beyond the last ring (the far corners): fully shaded
        Draw.color(shade, 0.78f);
        Lines.stroke(diag);
        Lines.circle(cx, cy, to + diag / 2f);
    }

    void drawLogo(){
        if(logo == null || !logo.found()) return;
        float lw = logoW * ts, lh = lw * logo.height / logo.width;
        float cx = sx(logoX), cy = sy(logoY);
        Draw.color(0f, 0f, 0f, 0.6f);
        Draw.rect(logo, cx + Scl.scl(4f), cy - Scl.scl(4f), lw, lh);
        //in horizontal strips, each a little darker than the one above: a soft vertical gradient over the letters
        int strips = 24;
        float left = cx - lw / 2f, bottom = cy - lh / 2f;
        for(int i = 0; i < strips; i++){
            float v0 = logo.v + (logo.v2 - logo.v) * i / strips, v1 = logo.v + (logo.v2 - logo.v) * (i + 1) / strips;
            Tmp.tr1.set(logo.texture);
            Tmp.tr1.set(logo.u, v0, logo.u2, v1);
            float shade = 1f - 0.18f * (i + 0.5f) / strips;
            Draw.color(shade, shade, shade, 1f);
            float stripH = lh / strips;
            Draw.rect(Tmp.tr1, cx, bottom + lh - stripH * (i + 0.5f), lw, stripH);
        }
        if(logoGlow != null && logoGlow.found()){
            Draw.blend(Blending.additive);
            Draw.color(1f, 0.72f, 0.3f, 0.3f + Mathf.absin(Time.globalTime, 40f, 0.35f));
            Draw.rect(logoGlow, cx, cy, lw, lh);
            Draw.blend();
        }
        Draw.color();
        //what vanilla's hidden logo showed, the game and its version, on the logo's left edge like the buttons below
        Font font = Fonts.outline;
        float scale = font.getData().scaleX;
        font.getData().setScale(Scl.scl(1f));
        font.setColor(Color.white);
        float lineY = bottom - Scl.scl(10f);
        font.draw("[#ffffffba]for Mindustry " + Version.combined(), left, lineY, Align.left);
        font.setColor(Color.white);
        font.getData().setScale(scale);
        Draw.reset();
    }
}
