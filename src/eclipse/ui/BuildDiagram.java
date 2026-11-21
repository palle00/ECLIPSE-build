package eclipse.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.pooling.*;
import eclipse.ui.BuildLayout.*;
import mindustry.content.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.liquid.*;
import mindustry.world.blocks.power.*;

/**
 * Draws a {@link BuildLayout} as an animated picture with the game's own sprites: belts running, the real items
 * riding them in the recipe's ratio (at a quarter of game speed), liquids flowing in their pipes, machines working,
 * heaters glowing on the side they feed, power lasers, and a rate on every belt. Hovering any tile shows what it is
 * and what it carries. In step mode only the parts up to the current step are drawn, the newest ones outlined.
 * Uses Time.globalTime, so it also moves while the game is paused.
 */
public class BuildDiagram extends Element{
    /** Item speed along belts, tiles per (game) second; the picture runs at a quarter of game speed. */
    static final float speed = 8f, slow = 0.25f;

    public final BuildLayout lay;
    /** 0: everything; otherwise the step being shown. */
    public int step;
    public boolean rates = true;
    float ts;

    public BuildDiagram(BuildLayout lay, float maxWidth){
        this.lay = lay;
        ts = Mathf.clamp(maxWidth / lay.w, Scl.scl(16f), Scl.scl(48f));
    }

    @Override
    public float getPrefWidth(){
        return lay.w * ts;
    }

    @Override
    public float getPrefHeight(){
        return lay.h * ts;
    }

    boolean shown(int s){
        return step == 0 || s <= step;
    }

    float tx(float lx){
        return x + lx * ts;
    }

    float ty(float ly){
        return y + ly * ts;
    }

    @Override
    public void draw(){
        validate();
        ts = Math.min(width / lay.w, height / lay.h);
        float t = Time.globalTime, sim = t / 60f * slow;
        Draw.color(Color.valueOf("16161b"), parentAlpha);
        Fill.crect(x, y, width, height);
        Draw.color(Color.valueOf("1e1e25"), parentAlpha);
        for(int i = 0; i <= lay.w; i++) Fill.crect(x + i * ts - 0.5f, y, 1f, height);
        for(int i = 0; i <= lay.h; i++) Fill.crect(x, y + i * ts - 0.5f, width, 1f);
        if(!clipBegin(x, y, width, height)) return;

        //pipes, then belts (animated), then what rides them, then buildings on top
        for(Part p : lay.parts){
            if(!shown(p.step) || !(p.block instanceof Conduit)) continue;
            drawPipe(p, t);
        }
        int frame = (int)(t / 4f) % 4;
        for(Part p : lay.parts){
            if(!shown(p.step) || !(p.block instanceof Conveyor)) continue;
            TextureRegion r = Core.atlas.find(p.block.name + "-0-" + frame);
            Draw.color(1f, 1f, 1f, parentAlpha);
            Draw.rect(r, tx(p.x + 0.5f), ty(p.y + 0.5f), ts, ts, p.rotation * 90f);
        }
        for(Flow f : lay.flows) if(shown(f.step)) drawFlow(f, sim);
        for(Part p : lay.parts){
            if(!shown(p.step) || p.block instanceof Conveyor || p.block instanceof Conduit) continue;
            drawBlock(p, t);
        }
        //power lasers from each node to what it links
        for(Part p : lay.parts){
            if(!shown(p.step) || !(p.block instanceof PowerNode) || !(p.config instanceof Point2[] links)) continue;
            for(Point2 l : links){
                Part o = lay.at(p.x + l.x, p.y + l.y);
                if(o == null || !shown(o.step)) continue;
                Draw.color(Pal.powerLight, (0.55f + Mathf.absin(t, 6f, 0.25f)) * parentAlpha);
                Lines.stroke(Math.max(1.5f, ts * 0.12f));
                Lines.line(tx(p.x + 0.5f), ty(p.y + 0.5f), tx(o.minX() + o.block.size / 2f), ty(o.minY() + o.block.size / 2f));
            }
        }
        //the newest parts in step mode
        if(step > 0){
            Draw.color(Pal.accent, (0.5f + Mathf.absin(t, 5f, 0.45f)) * parentAlpha);
            Lines.stroke(2f);
            for(Part p : lay.parts) if(p.step == step) Lines.rect(tx(p.minX()) + 1f, ty(p.minY()) + 1f, p.block.size * ts - 2f, p.block.size * ts - 2f);
        }
        if(rates) for(Label l : lay.labels) if(shown(l.step)) drawLabel(l);
        clipEnd();
        drawHover();
        Draw.reset();
    }

    void drawPipe(Part p, float t){
        float cx = tx(p.x + 0.5f), cy = ty(p.y + 0.5f);
        TextureRegion bottom = Core.atlas.find("conduit-bottom"), top = Core.atlas.find(p.block.name + "-top-0", "conduit-top-0");
        Draw.color(1f, 1f, 1f, parentAlpha);
        Draw.rect(bottom, cx, cy, ts, ts, p.rotation * 90f);
        if(p.liquid != null){
            boolean across = p.rotation % 2 == 0;
            Draw.color(p.liquid.color, 0.9f * parentAlpha);
            Fill.crect(cx - ts * (across ? 0.5f : 0.15f), cy - ts * (across ? 0.15f : 0.5f), ts * (across ? 1f : 0.3f), ts * (across ? 0.3f : 1f));
            //bubbles running the way it flows
            Draw.color(Tmp.c1.set(p.liquid.color).lerp(Color.white, 0.55f), 0.85f * parentAlpha);
            float f = (t * 0.03f) % 1f, dx = Geometry.d4x(p.rotation), dy = Geometry.d4y(p.rotation);
            Fill.square(cx + dx * (f - 0.5f) * ts, cy + dy * (f - 0.5f) * ts, ts * 0.08f, 45f);
        }
        Draw.color(1f, 1f, 1f, parentAlpha);
        Draw.rect(top, cx, cy, ts, ts, p.rotation * 90f);
    }

    void drawFlow(Flow f, float sim){
        //item i leaves at offset + i * interval and moves at a fixed speed; draw the ones on their path now
        float maxLen = 0f;
        int paths = f.paths == null ? 1 : f.paths.length * f.perPath;
        for(int k = 0; k < Math.max(1, paths); k += Math.max(1, f.perPath)) maxLen = Math.max(maxLen, length(f.pathOf(k)));
        int first = Math.max(0, Mathf.floor((sim - maxLen / speed - f.offset) / f.interval)), last = Mathf.floor((sim - f.offset) / f.interval);
        for(int i = first; i <= last && i - first < 400; i++){
            float[] path = f.pathOf(i);
            float d = (sim - f.offset - i * f.interval) * speed, len = length(path);
            if(d < 0f || d > len) continue;
            Item item = f.items[i % f.items.length];
            float[] pos = at(path, d);
            float s = ts * 0.55f;
            Draw.color(1f, 1f, 1f, parentAlpha);
            Draw.rect(item.fullIcon, tx(pos[0]), ty(pos[1]), s, s);
        }
    }

    static float length(float[] p){
        float l = 0f;
        for(int i = 2; i < p.length; i += 2) l += Mathf.dst(p[i - 2], p[i - 1], p[i], p[i + 1]);
        return l;
    }

    static float[] at(float[] p, float d){
        for(int i = 2; i < p.length; i += 2){
            float seg = Mathf.dst(p[i - 2], p[i - 1], p[i], p[i + 1]);
            if(d <= seg){
                float f = seg <= 0f ? 0f : d / seg;
                return new float[]{Mathf.lerp(p[i - 2], p[i], f), Mathf.lerp(p[i - 1], p[i + 1], f)};
            }
            d -= seg;
        }
        return new float[]{p[p.length - 2], p[p.length - 1]};
    }

    void drawBlock(Part p, float t){
        Block b = p.block;
        float size = b.size * ts, cx = tx(p.minX() + b.size / 2f), cy = ty(p.minY() + b.size / 2f);
        float rot = b.rotate && b.rotateDraw ? p.rotation * 90f : 0f;
        Draw.color(1f, 1f, 1f, parentAlpha);
        Draw.rect(b.fullIcon, cx, cy, size, size, rot);
        TextureRegion rotator = region(b.name + "-rotator"), press = region(b.name + "-press"), glow = region(b.name + "-glow"), heat = region(b.name + "-heat");
        if(rotator != null) Draw.rect(rotator, cx, cy, size, size, t * 2f);
        if(press != null){
            float down = Mathf.slope(Mathf.curve((t % 120f) / 120f, 0.7f, 1f));
            Draw.color(Tmp.c1.set(Color.white).mul(1f - 0.18f * down), parentAlpha);
            Draw.rect(press, cx, cy, size * (1f - 0.07f * down), size * (1f - 0.07f * down));
        }
        if(glow != null){
            Draw.blend(Blending.additive);
            Draw.color(Pal.accent, (0.35f + Mathf.absin(t, 8f, 0.35f)) * parentAlpha);
            Draw.rect(glow, cx, cy, size, size);
            Draw.blend();
        }
        if(heat != null && b instanceof mindustry.world.blocks.heat.HeatProducer){
            Draw.blend(Blending.additive);
            Draw.color(Color.valueOf("ff9a3c"), (0.55f + Mathf.absin(t, 10f, 0.3f)) * parentAlpha);
            Draw.rect(heat, cx, cy, size, size, rot);
            Draw.blend();
        }
        //rotating machines face up in the picture: mark the front
        if(b.rotate && !b.rotateDraw && !(b instanceof Conveyor)){
            float fy = cy + size / 2f + ts * 0.12f;
            Draw.color(Pal.accent, parentAlpha);
            Fill.tri(cx - ts * 0.25f, fy, cx + ts * 0.25f, fy, cx, fy + ts * 0.3f);
        }
    }

    static @Nullable TextureRegion region(String name){
        return Core.atlas.has(name) ? Core.atlas.find(name) : null;
    }

    void drawLabel(Label l){
        Font font = Fonts.outline;
        GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        float scale = Mathf.clamp(ts / Scl.scl(40f), 0.55f, 0.85f);
        font.getData().setScale(scale * Scl.scl(1f) / Scl.scl(1f));
        layout.setText(font, l.text);
        float w = layout.width + ts * 0.35f, h = layout.height + ts * 0.3f;
        float lx = l.align < 0 ? tx(l.x) - w : l.align == 0 ? tx(l.x) - w / 2f : tx(l.x), ly = ty(l.y) - h / 2f;
        lx = Mathf.clamp(lx, x + 2f, x + width - w - 2f);
        ly = Mathf.clamp(ly, y + 2f, y + height - h - 2f);
        Color c = l.kind == 1 ? WikiUI.heat : l.kind == 2 ? Pal.accent : WikiUI.cyan;
        Draw.color(0.04f, 0.04f, 0.06f, 0.86f * parentAlpha);
        Fill.crect(lx, ly, w, h);
        Draw.color(c, parentAlpha);
        Lines.stroke(1.5f);
        Lines.rect(lx, ly, w, h);
        font.setColor(c);
        font.draw(l.text, lx + ts * 0.17f, ly + h / 2f + layout.height / 2f);
        font.getData().setScale(1f);
        font.setColor(Color.white);
        Pools.free(layout);
    }

    /** The box for the tile under the mouse: what it is and what it carries. */
    void drawHover(){
        if(!hasMouse()) return;
        Vec2 m = screenToLocalCoordinates(Tmp.v1.set(Core.input.mouseX(), Core.input.mouseY()));
        int lx = Mathf.floor(m.x / ts), ly = Mathf.floor(m.y / ts);
        Part p = lay.at(lx, ly);
        if(p == null || !shown(p.step) || p.info.isEmpty()) return;
        Font font = Fonts.def;
        GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        String[] lines = p.info.split("\n");
        font.getData().setScale(0.85f);
        float w = 0f, lh = font.getLineHeight();
        for(String s : lines){
            layout.setText(font, s);
            w = Math.max(w, layout.width);
        }
        float pad = Scl.scl(8f), bw = w + pad * 2f, bh = lines.length * lh + pad * 2f;
        float bx = x + m.x + Scl.scl(16f), by = y + m.y - bh - Scl.scl(8f);
        if(bx + bw > x + width) bx = x + m.x - bw - Scl.scl(12f);
        if(by < y) by = y + m.y + Scl.scl(12f);
        Draw.color(0.04f, 0.04f, 0.06f, 0.95f * parentAlpha);
        Fill.crect(bx, by, bw, bh);
        Draw.color(Pal.accent, parentAlpha);
        Lines.stroke(2f);
        Lines.rect(bx, by, bw, bh);
        //the tile itself, outlined
        Lines.rect(tx(p.minX()), ty(p.minY()), p.block.size * ts, p.block.size * ts);
        for(int i = 0; i < lines.length; i++){
            font.setColor(i == 0 ? Pal.accent : Color.white);
            font.draw(lines[i], bx + pad, by + bh - pad - i * lh);
        }
        font.getData().setScale(1f);
        font.setColor(Color.white);
        Pools.free(layout);
    }
}
