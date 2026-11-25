package eclipse.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.core.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

/**
 * ECLIPSE main menu backdrop (setting "eclipse-menu", on by default): a dark industrial grid with light pulses running
 * along its lines like items on conveyors, a solar eclipse right of centre (a corona fading from gold through amber to
 * a hint of cyan, two dashed rings turning opposite ways, the black disk and its gold rim), drifting embers, scanlines
 * and a vignette. The ECLIPSE logo sits top left, drawn above the menu; vanilla's background and logo are hidden and
 * its buttons are replaced by {@link EclipseMenu}.
 * <p>
 * The first menu of a launch opens with the intro (setting "eclipse-menu-intro"): the moon crosses the sun, a
 * diamond-ring flash, the corona blooms, then the menu fades in. Any key or click skips it.
 */
public class MenuBackdrop extends Element{
    public static final String setting = "eclipse-menu", introSetting = "eclipse-menu-intro";

    static final Color background = Color.valueOf("08080a"), grid = Color.valueOf("2a2e393d"), gold = Color.valueOf("ffd37f"),
        amber = Color.valueOf("f59e0b"), cyan = Color.valueOf("38bdf8"), core = Color.valueOf("07080a"), shade = Color.valueOf("060709"),
        crater = Color.valueOf("0c0e12"), craterLight = Color.valueOf("12151b"), sunLight = Color.valueOf("fff4d8"), sunEdge = Color.valueOf("ffd890");
    static final int pulseCount = 14, emberCount = 55;

    /** Layout grid for the logo (16:9, scaled to cover the screen), as the menu column lines up with it. */
    static final int cols = 64, rows = 36;
    /** Logo centre and width in that grid. */
    static final float logoX = 15f, logoY = 30.2f, logoW = 22f;

    /** The intro's length in ticks, and when the menu starts fading in (2.6 s and 1.85 s). */
    static final float introLength = 156f, uiFrom = 111f;
    /** Ticks into the intro, or -1 while it isn't playing. */
    static float intro = -1f;
    static boolean introStarted, fromLoading;

    TextureRegion logo;
    /** The eclipse C's corona, pulsing slowly over the logo. */
    TextureRegion logoGlow;
    float ts, ox, oy;
    /** Pulses: horizontal?, start position 0-1, lane (grid line), speed per tick. Embers: x, y (0-1), radius, speed x/y, alpha. */
    final float[] pulses = new float[pulseCount * 4], embers = new float[emberCount * 6];

    /** Puts the backdrop into the main menu; while it is on, vanilla's background and logo are hidden. Call on ClientLoadEvent. */
    public static void install(){
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

    /** How visible the menu over the backdrop is: 0 during the intro, fading in at its end, else 1. */
    public static float uiAlpha(){
        return intro < 0f ? 1f : Mathf.clamp((intro - uiFrom) / 30f);
    }

    /** The logo stays in place after the loading screen (it showed there); otherwise it fades in with the menu. */
    public static float logoAlpha(){
        return fromLoading ? 1f : uiAlpha();
    }

    public static boolean introPlaying(){
        return intro >= 0f;
    }

    /** Plays the intro now (photo mode). */
    public static void playIntro(){
        introStarted = true;
        fromLoading = false;
        intro = 0f;
    }

    /** Ends the intro, or keeps it from starting (photo mode). */
    public static void skipIntro(){
        introStarted = true;
        intro = -1f;
    }

    MenuBackdrop(){
        touchable = arc.scene.event.Touchable.disabled;
        logo = Core.atlas.find("eclipse-logo");
        logoGlow = Core.atlas.find("eclipse-logo-glow");
        Rand r = new Rand(7);
        for(int i = 0; i < pulseCount; i++){
            pulses[i * 4] = i % 2 == 0 ? 1f : 0f;
            pulses[i * 4 + 1] = r.random(1f);
            pulses[i * 4 + 2] = 1 + (int)r.random(11f);
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
        //the first menu of a launch: the intro, unless turned off (or the backdrop is)
        if(!introStarted){
            introStarted = true;
            if(Core.settings.getBool(setting, true) && Core.settings.getBool(introSetting, true) && state.isMenu()){
                //after the ECLIPSE loading screen the moon is already almost over the sun: go on from there
                fromLoading = EclipseLoading.drawn;
                intro = fromLoading ? 60f : 0f;
            }
        }
        if(intro >= 0f){
            intro += Time.delta;
            //any key or click skips it (not in the first frames: the click or key that got here)
            if(intro >= introLength || (intro > 6f && (Core.input.justTouched() || Core.input.keyTap(KeyCode.anyKey)))) intro = -1f;
        }
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

    /** The factory floor grid, and light pulses running along its lines like items on conveyors, a fading trail behind each. */
    void drawGrid(float w, float h, float t){
        float cell = Scl.scl(80f);
        gridLines(x, y, w, h);
        float seg = Scl.scl(11f), thick = Scl.scl(2f), head = Scl.scl(6f);
        for(int i = 0; i < pulseCount; i++){
            boolean horizontal = pulses[i * 4] > 0f;
            float speed = pulses[i * 4 + 3], pos = ((pulses[i * 4 + 1] + speed * t) % 1f + 1f) % 1f;
            float lane = pulses[i * 4 + 2] * cell, dir = Math.signum(speed);
            Color c = i % 3 == 0 ? cyan : gold;
            //on a grid line: horizontal ones on the lines counted down from the top, vertical ones from the left
            float px = horizontal ? x + pos * w : x + (lane % w), py = horizontal ? y + h - (lane % h) : y + h - pos * h;
            //the trail points back the way the pulse came: left of a pulse moving right, above one moving down
            float bx = horizontal ? -dir : 0f, by = horizontal ? 0f : dir;
            for(int s = 0; s < 6; s++){
                Draw.color(c, 0.55f * (1f - s / 6f));
                float cx = px + bx * (s + 0.5f) * seg, cy = py + by * (s + 0.5f) * seg;
                if(horizontal) Fill.crect(cx - seg / 2f, cy - thick / 2f, seg, thick);
                else Fill.crect(cx - thick / 2f, cy - seg / 2f, thick, seg);
            }
            Draw.color(c, 0.9f);
            if(horizontal) Fill.crect(px - head / 2f, py - thick * 0.75f, head, thick * 1.5f);
            else Fill.crect(px - thick * 0.75f, py - head / 2f, thick * 1.5f, head);
        }
    }

    static void gridLines(float x, float y, float w, float h){
        float cell = Scl.scl(80f);
        Draw.color(grid);
        Lines.stroke(Scl.scl(1f));
        for(float gx = x; gx < x + w; gx += cell) Lines.line(gx, y, gx, y + h);
        for(float gy = y + h; gy > y; gy -= cell) Lines.line(x, gy, x + w, gy);
    }

    /** The eclipse's centre and radius on a screen of this size: right of centre, or higher and centred on a narrow one. */
    static float eclipseX(float w, float h){
        return w * (w < Scl.scl(768f) ? 0.5f : 0.68f);
    }

    static float eclipseY(float w, float h){
        return h * (1f - (w < Scl.scl(768f) ? 0.35f : 0.48f));
    }

    static float eclipseR(float w, float h){
        return Math.min(w, h) * (w < Scl.scl(768f) ? 0.16f : 0.19f);
    }

    /** The eclipse (or, during the intro, the moon crossing the sun). */
    void drawEclipse(float w, float h, float t){
        float cx = x + eclipseX(w, h), cy = y + eclipseY(w, h), r = eclipseR(w, h);
        if(intro < 0f) drawEclipse(cx, cy, r, t, 1f, Scl.scl(1f));
        else drawIntro(cx, cy, r, t, intro / 60f, Scl.scl(1f));
    }

    /**
     * The ECLIPSE's eclipse, shared by the main menu and the in-game wave event ({@link eclipse.systems.EclipseEvent}):
     * centre and radius, time, opacity (for fading in and out) and the size of one screen pixel in the units drawn in.
     */
    public static void drawEclipse(float cx, float cy, float r, float t, float alpha, float px){
        corona(cx, cy, r, alpha, px);
        rings(cx, cy, r, t, alpha, px);
        disk(cx, cy, r, alpha);
        rim(cx, cy, r, alpha, px);
    }

    /** Corona: gold at the rim, through amber to a faint cyan, fading out at 2.6 radii (as rings of the gradient). */
    static void corona(float cx, float cy, float r, float alpha, float px){
        float inner = r * 0.92f, outer = r * 2.6f;
        int steps = 64;
        float step = (outer - inner) / steps;
        Lines.stroke(step + px);
        for(int i = 0; i < steps; i++){
            float f = (i + 0.5f) / steps;
            if(f < 0.18f) Tmp.c1.set(gold).a(0.55f).lerp(Tmp.c2.set(amber).a(0.22f), f / 0.18f);
            else if(f < 0.5f) Tmp.c1.set(amber).a(0.22f).lerp(Tmp.c2.set(cyan).a(0.06f), (f - 0.18f) / 0.32f);
            else Tmp.c1.set(cyan).a(0.06f).lerp(Tmp.c2.set(background).a(0f), (f - 0.5f) / 0.5f);
            Draw.color(Tmp.c1.a(Tmp.c1.a * alpha));
            Lines.circle(cx, cy, inner + step * (i + 0.5f));
        }
    }

    /** Rings: gold (dash 12, gap 18, dash 4, gap 18) turning one way, cyan (dash 6, gap 24) the other way, a little slower. */
    static void rings(float cx, float cy, float r, float t, float alpha, float px){
        float angle = t * 0.0022f * Mathf.radDeg;
        Lines.stroke(1.2f * px);
        Draw.color(gold, 0.24f * alpha);
        dashed(cx, cy, r * 1.38f, angle, new float[]{12f, 18f, 4f, 18f}, px);
        Draw.color(cyan, 0.2f * alpha);
        dashed(cx, cy, r * 1.65f, -angle * 0.8f, new float[]{6f, 24f}, px);
    }

    /** The moon: the black disk, its inner rim catching light on the upper left, and a few barely-there craters. */
    static void disk(float cx, float cy, float r, float alpha){
        Draw.color(core, alpha);
        circle(cx, cy, r);
        for(int i = 0; i < 10; i++){
            float f = i / 10f;
            Draw.color(gold, 0.05f * (1f - f) * alpha);
            Lines.stroke(r * 0.03f);
            Lines.circle(cx - r * 0.02f * (1f - f), cy + r * 0.02f * (1f - f), r * (0.985f - f * 0.09f));
        }
        Draw.color(core, alpha);
        circle(cx + r * 0.05f, cy - r * 0.05f, r * 0.86f);
        Rand rand = new Rand(31);
        for(int i = 0; i < 14; i++){
            float a = rand.random(360f), d = r * (float)Math.sqrt(rand.random(0.72f)), cr = r * rand.random(0.05f, i < 4 ? 0.3f : 0.12f);
            Draw.color(i < 4 ? crater : craterLight, (i < 4 ? 0.55f : 0.35f) * alpha);
            circle(cx + Angles.trnsx(a, d), cy + Angles.trnsy(a, d), cr);
        }
    }

    /** Set while the loading screen draws: the atlas may be there but not ready yet (its circle shows as a black square). */
    static boolean loadingScreen;

    /** A filled circle: the atlas's smooth one, or a polygon on the loading screen (before the game's sprites are ready). */
    static void circle(float x, float y, float r){
        if(!loadingScreen && Core.atlas != null && Core.atlas.has("circle")) Fill.circle(x, y, r);
        else Fill.poly(x, y, Math.max(12, Math.min(96, (int)(r / 3f))), r);
    }

    static void rim(float cx, float cy, float r, float alpha, float px){
        Draw.color(gold, 0.85f * alpha);
        Lines.stroke(2f * px);
        Lines.circle(cx, cy, r);
    }

    /**
     * The intro at T seconds: until 1.3 s the moon slides over the sun from the lower left while the sun's glow fades
     * with the part still showing; around 1.3 s the last bead of light flashes on the rim (the diamond ring); from 1.2 s
     * the corona, rings and gold rim bloom.
     */
    static void drawIntro(float cx, float cy, float r, float t, float T, float px){
        float e = Interp.pow3.apply(Mathf.clamp(T / 1.3f)), dx = -1.45f * r * (1f - e), dy = -0.35f * r * (1f - e);
        float visible = 1f - overlap(Mathf.len(dx, dy), r) / (Mathf.PI * r * r), glow = (float)Math.pow(Mathf.clamp(visible), 0.7f);
        Draw.blend(Blending.additive);
        if(glow > 0.002f) Fill.light(cx, cy, 64, r * 3.4f, Tmp.c1.set(1f, 0.93f, 0.75f, 0.6f * glow), Tmp.c2.set(1f, 0.67f, 0.24f, 0f));
        Draw.blend();
        float bloom = smooth(1.2f, 2.3f, T);
        if(bloom > 0f){
            corona(cx, cy, r, bloom, px);
            rings(cx, cy, r, t, bloom, px);
        }
        if(visible > 0.0005f) Fill.light(cx, cy, 64, r, Tmp.c1.set(sunLight), Tmp.c2.set(sunEdge));
        disk(cx + dx, cy + dy, r, 1f);
        //the diamond ring, where the moon is heading (right and up)
        float k = smooth(1.1f, 1.3f, T) * (1f - smooth(1.42f, 1.8f, T));
        if(k > 0f){
            float a = Angles.angle(1.45f, 0.35f), bx = cx + Angles.trnsx(a, r), by = cy + Angles.trnsy(a, r), len = r * 2.4f, half = r * 0.06f;
            Draw.blend(Blending.additive);
            Fill.light(bx, by, 40, r * 0.6f, Tmp.c1.set(1f, 1f, 1f, k), Tmp.c2.set(1f, 0.78f, 0.43f, 0f));
            float bright = Tmp.c1.set(1f, 0.98f, 0.92f, 0.7f * k).toFloatBits(), clear = Tmp.c2.set(1f, 0.86f, 0.6f, 0f).toFloatBits();
            Fill.quad(bx - len, by, clear, bx, by - half, bright, bx, by + half, bright, bx - len, by, clear);
            Fill.quad(bx + len, by, clear, bx, by + half, bright, bx, by - half, bright, bx + len, by, clear);
            Draw.blend();
        }
        float rimIn = smooth(1.25f, 1.7f, T);
        if(rimIn > 0f) rim(cx, cy, r, rimIn, px);
    }

    /** The area two discs of radius r overlap when their centres are d apart. */
    static float overlap(float d, float r){
        if(d >= 2f * r) return 0f;
        return 2f * r * r * (float)Math.acos(d / (2f * r)) - d / 2f * (float)Math.sqrt(4f * r * r - d * d);
    }

    static float smooth(float from, float to, float v){
        return Interp.smooth.apply(Mathf.clamp((v - from) / (to - from)));
    }

    /** A dashed circle: the pattern (dash, gap, dash, gap... in screen pixels of size px) repeated around it, rotated by an angle. */
    static void dashed(float cx, float cy, float radius, float rotation, float[] pattern, float px){
        float circumference = Mathf.PI2 * radius, at = 0f;
        int k = 0;
        while(at < circumference){
            float len = pattern[k % pattern.length] * px;
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
        atmosphere(x, y, w, h);
    }

    static void atmosphere(float x, float y, float w, float h){
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

    /** The logo, its pulsing glow and the game's version under it, at an opacity (it fades in with the menu after the intro). */
    void drawLogo(float alpha){
        if(logo == null || !logo.found() || alpha <= 0.001f) return;
        float lw = logoW * ts, lh = lw * logo.height / logo.width;
        float cx = sx(logoX), cy = sy(logoY);
        drawLogo(logo, logoGlow != null && logoGlow.found() ? logoGlow : null, cx, cy, lw, lh, alpha);
        //what vanilla's hidden logo showed, the game and its version, on the logo's left edge like the buttons below
        Font font = Fonts.outline;
        float scale = font.getData().scaleX;
        font.getData().setScale(Scl.scl(1f));
        font.setColor(1f, 1f, 1f, 0.73f * alpha);
        font.draw("for Mindustry " + Version.combined(), cx - lw / 2f, cy - lh / 2f - Scl.scl(10f), Align.left);
        font.setColor(Color.white);
        font.getData().setScale(scale);
        Draw.reset();
    }

    /** A screen's logo layout: the 64x36 grid scaled to cover it. Returns centre x, centre y and width of the logo. */
    static float[] logoPlace(float w, float h){
        float ts = Math.max(w / cols, h / rows), ox = (w - cols * ts) / 2f, oy = (h - rows * ts) / 2f;
        return new float[]{ox + logoX * ts, oy + logoY * ts, logoW * ts};
    }

    /** The logo with its shadow, a soft vertical gradient over the letters and its pulsing glow (also on the loading screen). */
    static void drawLogo(TextureRegion logo, @Nullable TextureRegion logoGlow, float cx, float cy, float lw, float lh, float alpha){
        Draw.color(0f, 0f, 0f, 0.6f * alpha);
        Draw.rect(logo, cx + Scl.scl(4f), cy - Scl.scl(4f), lw, lh);
        //in horizontal strips, each a little darker than the one above: a soft vertical gradient over the letters
        int strips = 24;
        float bottom = cy - lh / 2f;
        for(int i = 0; i < strips; i++){
            float v0 = logo.v + (logo.v2 - logo.v) * i / strips, v1 = logo.v + (logo.v2 - logo.v) * (i + 1) / strips;
            Tmp.tr1.set(logo.texture);
            Tmp.tr1.set(logo.u, v0, logo.u2, v1);
            float shade = 1f - 0.18f * (i + 0.5f) / strips;
            Draw.color(shade, shade, shade, alpha);
            float stripH = lh / strips;
            Draw.rect(Tmp.tr1, cx, bottom + lh - stripH * (i + 0.5f), lw, stripH);
        }
        if(logoGlow != null){
            Draw.blend(Blending.additive);
            Draw.color(1f, 0.72f, 0.3f, (0.3f + Mathf.absin(Time.globalTime, 40f, 0.35f)) * alpha);
            Draw.rect(logoGlow, cx, cy, lw, lh);
            Draw.blend();
        }
        Draw.color();
    }
}
