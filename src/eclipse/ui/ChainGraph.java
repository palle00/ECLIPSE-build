package eclipse.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import mindustry.ctype.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;

/**
 * The production calculator drawn as a flow: raw materials on the left, the product on the right; each node is an item
 * at its rate with the machines making it (how many), liquids it needs listed under it. Click a node to open its page.
 */
public class ChainGraph extends Element{
    static final float nodeW = 178f, nodeH = 62f, gapX = 56f, gapY = 14f;

    static class Node{
        RecipeTree.Step step;
        int col;
        float nx, ny;
        @Nullable Node parent;
    }

    final Seq<Node> nodes = new Seq<>();
    final Cons<UnlockableContent> open;
    int cols, rows;
    @Nullable Node hovered;
    /** Size factor of the whole graph. */
    public float zoom = 1f;
    /** Optional: set to let the mouse wheel zoom the graph (off in the wiki, where the wheel scrolls the page); told the new zoom. */
    public @Nullable Floatc onZoom;
    public static final float minZoom = 0.4f, maxZoom = 2.5f;

    public ChainGraph(RecipeTree.Step root, Cons<UnlockableContent> open){
        this.open = open;
        int depth = depth(root);
        cols = depth + 1;
        //columns by depth, product on the right; rows in the order the tree lists them
        IntIntMap perCol = new IntIntMap();
        add(root, null, 0, depth, perCol);
        for(var e : perCol) rows = Math.max(rows, e.value);
        //centre each column vertically
        for(Node n : nodes){
            int inCol = perCol.get(n.col);
            n.ny += (rows - inCol) * (nodeH + gapY) / 2f;
        }
        addListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                Node n = hit(x, y);
                //opening rebuilds the panel and removes this graph: do it after the touch event is done
                if(n != null) Core.app.post(() -> open.get(n.step.item));
                return n != null;
            }

            @Override
            public boolean mouseMoved(InputEvent event, float x, float y){
                hovered = hit(x, y);
                return false;
            }

            //the wheel goes to the scroll focus, not to what is under the mouse: take it while hovered, then give it back to the pane
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Element fromActor){
                if(onZoom != null && pointer == -1) Core.scene.setScrollFocus(ChainGraph.this);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Element toActor){
                hovered = null;
                if(pointer != -1 || toActor != null && toActor.isDescendantOf(ChainGraph.this)) return;
                if(Core.scene.getScrollFocus() == ChainGraph.this){
                    Element pane = parent;
                    while(pane != null && !(pane instanceof arc.scene.ui.ScrollPane)) pane = pane.parent;
                    Core.scene.setScrollFocus(pane);
                }
            }

            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY){
                if(onZoom == null || amountY == 0f) return false;
                setZoom(zoom * (amountY < 0f ? 1.15f : 1f / 1.15f));
                event.stop(); //the wheel zooms here instead of scrolling the pane
                return true;
            }
        });
    }

    @Override
    public void act(float delta){
        super.act(delta);
        //the production panel rebuilds every second, which drops the scroll focus: take it again while under the mouse
        if(onZoom == null || Core.scene == null) return;
        Element focus = Core.scene.getScrollFocus();
        if(focus == this || focus != null && !(focus instanceof arc.scene.ui.ScrollPane)) return;
        Vec2 m = screenToLocalCoordinates(Tmp.v1.set(Core.input.mouseX(), Core.input.mouseY()));
        if(hit(m.x, m.y, true) == this) Core.scene.setScrollFocus(this);
    }

    public void setZoom(float z){
        zoom = Mathf.clamp(z, minZoom, maxZoom);
        invalidateHierarchy();
        if(onZoom != null) onZoom.get(zoom);
    }

    /** A size in graph units to screen pixels, at the current zoom. */
    float s(float v){
        return Scl.scl(v * zoom);
    }

    static int depth(RecipeTree.Step s){
        int d = 0;
        for(RecipeTree.Step in : s.inputs) d = Math.max(d, depth(in) + 1);
        return d;
    }

    void add(RecipeTree.Step s, @Nullable Node parent, int level, int depth, IntIntMap perCol){
        Node n = new Node();
        n.step = s;
        n.parent = parent;
        n.col = depth - level;
        int row = perCol.get(n.col, 0);
        perCol.put(n.col, row + 1);
        n.nx = n.col * (nodeW + gapX);
        n.ny = row * (nodeH + gapY);
        nodes.add(n);
        for(RecipeTree.Step in : s.inputs) add(in, n, level + 1, depth, perCol);
    }

    @Override
    public float getPrefWidth(){
        return s(cols * (nodeW + gapX) - gapX + 8f);
    }

    @Override
    public float getPrefHeight(){
        return s(Math.max(1, rows) * (nodeH + gapY) + 8f);
    }

    /** Node position on screen: column from the left, row from the top. */
    float nx(Node n){
        return x + s(n.nx + 4f);
    }

    float ny(Node n){
        return y + height - s(n.ny + nodeH + 4f);
    }

    @Nullable Node hit(float lx, float ly){
        for(Node n : nodes){
            float ax = nx(n) - x, ay = ny(n) - y;
            if(lx >= ax && ly >= ay && lx <= ax + s(nodeW) && ly <= ay + s(nodeH)) return n;
        }
        return null;
    }

    @Override
    public void draw(){
        validate();
        float w = s(nodeW), h = s(nodeH);
        //edges: from each input's right side to its parent's left side
        Lines.stroke(s(2.5f));
        for(Node n : nodes){
            if(n.parent == null) continue;
            float x1 = nx(n) + w, y1 = ny(n) + h / 2f, x2 = nx(n.parent), y2 = ny(n.parent) + h / 2f, mx = (x1 + x2) / 2f;
            Draw.color(Color.valueOf("45454f"), parentAlpha);
            Lines.curve(x1, y1, mx, y1, mx, y2, x2, y2, 20);
            Fill.tri(x2, y2, x2 - s(7f), y2 + s(4f), x2 - s(7f), y2 - s(4f));
        }
        Font font = Fonts.def;
        GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        for(Node n : nodes){
            float ax = nx(n), ay = ny(n);
            boolean product = n.parent == null, raw = n.step.source == null;
            Draw.color(Color.valueOf("101015"), parentAlpha);
            Fill.crect(ax, ay, w, h);
            Draw.color(product ? Pal.accent : n == hovered ? Color.valueOf("5a5a68") : Color.valueOf("30303b"), parentAlpha);
            Lines.stroke(s(2f));
            Lines.rect(ax, ay, w, h);
            Draw.color(1f, 1f, 1f, parentAlpha);
            Draw.rect(n.step.item.uiIcon, ax + s(22f), ay + h - s(22f), s(30f), s(30f));
            font.getData().setScale(0.85f * zoom);
            font.setColor(product ? Pal.accent : Color.white);
            font.draw(fit(font, layout, n.step.item.localizedName, w - s(48f)), ax + s(42f), ay + h - s(10f));
            font.getData().setScale(0.75f * zoom);
            font.setColor(WikiUI.cyan);
            font.draw(Strings.autoFixed(n.step.perSecond, 2) + "/s" + (raw ? "  " + Core.bundle.get("eclipse-wiki.chain.raw") : ""), ax + s(42f), ay + h - s(30f));
            if(n.step.source != null){
                Draw.color(1f, 1f, 1f, parentAlpha);
                Draw.rect(n.step.source.block.uiIcon, ax + s(14f), ay + s(13f), s(18f), s(18f));
                font.setColor(Color.lightGray);
                String liq = n.step.liquids.isEmpty() ? "" : "  + " + n.step.liquids.first().liquid.localizedName;
                font.draw(fit(font, layout, "×" + Strings.autoFixed(n.step.machines, 2) + " " + n.step.source.block.localizedName + liq, w - s(30f)), ax + s(27f), ay + s(19f));
            }
        }
        font.getData().setScale(1f);
        font.setColor(Color.white);
        Pools.free(layout);
        Draw.reset();
    }

    /** Text shortened with "..." to fit a width at the font's current scale. */
    static String fit(Font font, GlyphLayout layout, String text, float width){
        layout.setText(font, text);
        if(layout.width <= width) return text;
        for(int n = text.length() - 1; n > 3; n--){
            String t = text.substring(0, n) + "...";
            layout.setText(font, t);
            if(layout.width <= width) return t;
        }
        return text;
    }
}
