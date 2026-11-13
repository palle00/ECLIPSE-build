package eclipse.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

/**
 * ECLIPSE main menu backdrop (setting "eclipse-menu", on by default): a top-down Serpulo landscape at dusk, built from
 * the game's own floor, boulder and ore sprites. Grassland, a winding river, rocky ground with ECLIPSE ore veins, ECLIPSE
 * aircraft crossing over, and above the land a night sky with a cloud bank and the eclipse. The ECLIPSE logo sits top
 * left, drawn above the menu; vanilla's logo is hidden and its buttons are replaced by {@link EclipseMenu}. The scene is laid out once in tiles
 * and scaled to cover the screen.
 */
public class MenuBackdrop extends Element{
    public static final String setting = "eclipse-menu";

    /** Scene size in tiles (16:9). Row 0 is the bottom. */
    static final int cols = 64, rows = 36;
    static final String[] ores = {"ore-nickel", "ore-cobalt", "ore-chromium", "ore-tungsten"};
    static final String[] aircraft = {"wraith", "interceptor", "valkyrie", "wraith", "tempest"};

    static final Color skyTop = Color.valueOf("0b0a1c"), skyLow = Color.valueOf("2b2350"), corona = Color.valueOf("ff9a3c"), coronaHot = Color.valueOf("ffd9a0"),
        disk = Color.valueOf("120e20"), cloudShadow = Color.valueOf("3a3160"), cloudMid = Color.valueOf("5e5190"), cloudLight = Color.valueOf("8f80bf"), dusk = Color.valueOf("b9a8e6");

    /** Per tile: floor region (null = sky), whether it is water, ore overlay and decoration (a tree or a boulder). */
    final TextureRegion[] floorRegion = new TextureRegion[cols * rows], ore = new TextureRegion[cols * rows], deco = new TextureRegion[cols * rows];
    final boolean[] water = new boolean[cols * rows];
    final int[] top = new int[cols];
    /** Cloud puffs along the edge of the land: x, y, radius (tiles), drift speed. */
    final FloatSeq puffs = new FloatSeq();
    final Seq<UnitType> flyers = new Seq<>();
    TextureRegion logo;
    boolean built;

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
        MenuBackdrop backdrop = new MenuBackdrop(menu);
        backdrop.visible(() -> Core.settings.getBool(setting, true));
        //right after the vanilla background: the buttons stay on top
        menu.addChildAt(1, backdrop);
        //our own buttons and the logo on top (desktop)
        EclipseMenu.install(menu, backdrop);
        Log.info("[ECLIPSE] menu backdrop installed");
    }

    final Group menu;

    MenuBackdrop(Group menu){
        this.menu = menu;
        touchable = arc.scene.event.Touchable.disabled;
    }

    // ---- layout (once, in tiles) ----

    void build(){
        built = true;
        logo = Core.atlas.find("eclipse-logo");
        for(String n : aircraft){
            UnitType u = content.unit("eclipse-" + n);
            if(u != null) flyers.add(u);
        }

        int seed = 9;
        for(int c = 0; c < cols; c++) top[c] = 21 + (int)(Simplex.noise2d(seed, 2, 0.5, 1 / 7.0, c, 0) * 4f) + (c > 30 ? 2 : 0);
        Seq<OreBlock> oreBlocks = new Seq<>();
        for(String n : ores) if(content.block("eclipse-" + n) instanceof OreBlock o) oreBlocks.add(o);

        Rand rand = new Rand(17);
        for(int c = 0; c < cols; c++){
            for(int r = 0; r < top[c]; r++){
                int i = r * cols + c;
                //a river winding down the left third with sand banks, and a small lake on the right
                float river = 17f + 3.5f * Mathf.sin(r * 0.21f + 1f) + 1.5f * Mathf.sin(r * 0.5f);
                float d = Math.abs(c + 0.5f - river), lake = Mathf.dst(c, r * 1.3f, 52f, 8f) + Simplex.noise2d(seed + 5, 2, 0.5, 1 / 4.0, c, r) * 3f;
                float n = Simplex.noise2d(seed + 1, 3, 0.5, 1 / 6.0, c, r);
                //rocky ground away from the water, on the right half
                float rock = Simplex.noise2d(seed + 2, 3, 0.5, 1 / 9.0, c, r) + (c > 30 ? 0.04f : -0.1f);
                Floor f;
                d = Math.min(d, lake - 2.5f);
                if(d < 1.4f) f = (Floor)Blocks.deepwater;
                else if(d < 2.5f) f = (Floor)Blocks.water;
                else if(d < 3.1f) f = (Floor)Blocks.sandWater;
                else if(d < 4.2f + n * 1.5f) f = (Floor)Blocks.sand;
                else if(rock > 0.62f) f = n > 0.55f ? (Floor)Blocks.basalt : (Floor)Blocks.stone;
                else if(rock > 0.55f) f = (Floor)Blocks.darksand;
                else f = n > 0.72f ? (Floor)Blocks.sand : (Floor)Blocks.grass;
                water[i] = f.isLiquid;
                TextureRegion[] vars = f.variantRegions();
                floorRegion[i] = vars != null && vars.length > 0 ? vars[rand.random(vars.length - 1)] : f.fullIcon;

                //ECLIPSE ore veins in the rock
                if((f == Blocks.stone || f == Blocks.basalt || f == Blocks.darksand) && !oreBlocks.isEmpty()){
                    for(int k = 0; k < oreBlocks.size; k++){
                        if(Simplex.noise2d(seed + 20 + k, 2, 0.5, 1 / 3.5, c, r) > 0.8f){
                            TextureRegion[] ov = oreBlocks.get(k).variantRegions();
                            ore[i] = ov[rand.random(ov.length - 1)];
                            break;
                        }
                    }
                }
                //boulders on the rock
                if((f == Blocks.stone || f == Blocks.basalt) && ore[i] == null && rand.chance(0.07f)){
                    TextureRegion[] bv = Blocks.boulder.variantRegions();
                    deco[i] = bv[rand.random(bv.length - 1)];
                }
            }
        }

        //cloud puffs: a bank along the edge of the land
        Rand pr = new Rand(23);
        for(float c = -4f; c < cols + 4f; c += pr.random(1.2f, 2.4f)){
            int edge = top[Mathf.clamp((int)c, 0, cols - 1)];
            puffs.addAll(c, edge + pr.random(-0.5f, 1.5f), pr.random(1.6f, 3.2f), 0f);
            if(pr.chance(0.5f)) puffs.addAll(c + pr.random(-1f, 1f), edge + pr.random(1.5f, 3.5f), pr.random(1.1f, 2.2f), 0f);
        }
    }

    // ---- drawing ----

    float ts, ox, oy;

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

    /** Logo centre and width in tiles. */
    static final float logoX = 15f, logoY = 30.2f, logoW = 22f;

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
        if(!built) build();
        float w = width, h = height, t = Time.globalTime;
        ts = Math.max(w / cols, h / rows);
        ox = x + (w - cols * ts) / 2f;
        oy = y + (h - rows * ts) / 2f;

        drawSky(t);
        drawLand(t);
        drawClouds(t);
        drawFlyers(t);
        Draw.reset();
    }

    void drawSky(float t){
        float cTop = skyTop.toFloatBits(), cLow = skyLow.toFloatBits();
        Fill.quad(x, y, cLow, x + width, y, cLow, x + width, y + height, cTop, x, y + height, cTop);
        Rand rand = new Rand(3);
        for(int i = 0; i < 160; i++){
            float px = x + rand.random(width), py = y + height * rand.random(0.45f, 1f);
            Draw.color(1f, 0.93f, 0.85f, 0.2f + Mathf.absin(t + i * 41f, 25f + rand.random(30f), 0.6f));
            Fill.square(px, py, Scl.scl(rand.random(0.8f, 1.6f)));
        }
        //the eclipse: a soft orange glow, a thick bright ring and the dark disk, slowly breathing
        float cx = sx(46.5f), cy = sy(29.5f), r = ts * 4.4f;
        float pulse = Mathf.absin(t, 90f, 1f);
        for(int i = 7; i >= 1; i--){
            Draw.color(corona, 0.035f + 0.02f * (7 - i) + pulse * 0.01f);
            Fill.circle(cx, cy, r * (1f + i * 0.13f));
        }
        Draw.color(corona, 0.85f);
        Fill.circle(cx, cy, r * 1.09f);
        Draw.color(coronaHot);
        Fill.circle(cx, cy, r * 1.035f);
        Draw.color(disk);
        Fill.circle(cx, cy, r);
        //a faint crescent of light on the disk
        Draw.color(dusk, 0.12f);
        Fill.circle(cx - r * 0.25f, cy + r * 0.25f, r * 0.55f);
        Draw.color(disk);
        Fill.circle(cx - r * 0.15f, cy + r * 0.15f, r * 0.62f);
    }

    void drawLand(float t){
        Draw.color();
        for(int r = 0; r < rows; r++){
            for(int c = 0; c < cols; c++){
                int i = r * cols + c;
                if(floorRegion[i] == null) continue;
                Draw.rect(floorRegion[i], sx(c + 0.5f), sy(r + 0.5f), ts, ts);
                if(ore[i] != null) Draw.rect(ore[i], sx(c + 0.5f), sy(r + 0.5f), ts, ts);
            }
        }
        //water glints
        Rand gr = new Rand(5);
        for(int i = 0; i < 120; i++){
            int c = gr.random(cols - 1), r = gr.random(rows - 1);
            if(!water[r * cols + c]) continue;
            Draw.color(1f, 1f, 1f, Mathf.absin(t + i * 23f, 18f, 0.35f));
            Fill.crect(sx(c + gr.random(0.1f, 0.6f)), sy(r + gr.random(0.2f, 0.8f)), ts * 0.35f, ts * 0.08f);
        }
        //dusk: the land a little darker and cooler, so it sits under the night sky
        Draw.color(Color.valueOf("1a1440"), 0.28f);
        for(int c = 0; c < cols; c++) Fill.crect(sx(c), sy(0), ts, top[c] * ts);
        //boulders, back to front, with a soft shadow
        for(int r = rows - 1; r >= 0; r--){
            for(int c = 0; c < cols; c++){
                TextureRegion d = deco[r * cols + c];
                if(d == null) continue;
                float s = ts;
                Draw.color(0f, 0f, 0f, 0.3f);
                Draw.rect(d, sx(c + 0.6f), sy(r + 0.35f), s, s);
                Draw.color(0.85f, 0.82f, 0.95f, 1f);
                Draw.rect(d, sx(c + 0.5f), sy(r + 0.5f), s, s);
            }
        }
    }

    /** Soft cloud puffs, each a dark base with a lit top, drifting slowly. */
    void drawClouds(float t){
        float span = cols + 8f;
        for(int pass = 0; pass < 3; pass++){
            Color col = pass == 0 ? cloudShadow : pass == 1 ? cloudMid : cloudLight;
            Draw.color(col);
            for(int i = 0; i < puffs.size; i += 4){
                float px = puffs.items[i] + t * puffs.items[i + 3], py = puffs.items[i + 1], pr = puffs.items[i + 2];
                if(puffs.items[i + 3] > 0f) px = ((px + 4f) % span + span) % span - 4f;
                float drift = Mathf.sin(t * 0.01f + i, 1f, 0.15f);
                if(pass == 0) Fill.circle(sx(px + drift), sy(py - pr * 0.15f), pr * ts);
                else if(pass == 1) Fill.circle(sx(px + drift - pr * 0.08f), sy(py + pr * 0.1f), pr * ts * 0.82f);
                else Fill.circle(sx(px + drift - pr * 0.22f), sy(py + pr * 0.38f), pr * ts * 0.45f);
            }
        }
    }

    /** ECLIPSE strike aircraft in loose formations, crossing from the lower left to the upper right. */
    void drawFlyers(float t){
        if(flyers.isEmpty()) return;
        float angle = 28f;
        for(int k = 0; k < 3; k++){
            float period = 1500f + k * 400f, p = ((t + k * 700f) % period) / period;
            float travel = cols * 1.6f;
            float bx = -12f + Angles.trnsx(angle, p * travel), by = 2f + k * 7f + Angles.trnsy(angle, p * travel);
            UnitType type = flyers.get(k % flyers.size);
            float size = Mathf.clamp(type.hitSize / 8f, 1.2f, 3.5f) * 1.3f;
            for(int m = 0; m < 3 + (k == 1 ? 1 : 0); m++){
                float fx = bx - Angles.trnsx(angle, m * 2.2f) + (m % 2 == 0 ? 1f : -1f) * m * 0.9f, fy = by - Angles.trnsy(angle, m * 2.2f) + m * 0.5f;
                Draw.color(corona, 0.35f);
                Lines.stroke(ts * 0.12f);
                Lines.lineAngle(sx(fx), sy(fy), angle + 180f, ts * size * 1.6f);
                Draw.color(0f, 0f, 0f, 0.25f);
                Draw.rect(type.fullIcon, sx(fx + 1.4f), sy(fy - 1.4f), ts * size, ts * size, angle - 90f);
                Draw.color();
                Draw.rect(type.fullIcon, sx(fx), sy(fy), ts * size, ts * size, angle - 90f);
            }
        }
    }

    void drawLogo(){
        if(logo == null || !logo.found()) return;
        float lw = logoW * ts, lh = lw * logo.height / logo.width;
        float cx = sx(logoX), cy = sy(logoY);
        Draw.color(0f, 0f, 0f, 0.6f);
        Draw.rect(logo, cx + Scl.scl(4f), cy - Scl.scl(4f), lw, lh);
        Draw.color();
        Draw.rect(logo, cx, cy, lw, lh);
        //what vanilla's hidden logo showed: the game and its version
        Font font = Fonts.outline;
        float scale = font.getData().scaleX;
        font.getData().setScale(Scl.scl(1f));
        font.setColor(Color.white);
        font.draw("[#ffffffba]for Mindustry " + Version.combined(), cx, cy - lh / 2f - Scl.scl(10f), Align.center);
        font.getData().setScale(scale);
        Draw.reset();
    }
}
