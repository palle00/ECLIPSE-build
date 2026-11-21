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
                if(n != null) open.get(n.step.item);
                return n != null;
            }

            @Override
            public boolean mouseMoved(InputEvent event, float x, float y){
                hovered = hit(x, y);
                return false;
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Element toActor){
                hovered = null;
            }
        });
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
        return Scl.scl(cols * (nodeW + gapX) - gapX + 8f);
    }

    @Override
    public float getPrefHeight(){
        return Scl.scl(Math.max(1, rows) * (nodeH + gapY) + 8f);
    }

    /** Node position on screen: column from the left, row from the top. */
    float nx(Node n){
        return x + Scl.scl(n.nx + 4f);
    }

    float ny(Node n){
        return y + height - Scl.scl(n.ny + nodeH + 4f);
    }

    @Nullable Node hit(float lx, float ly){
        for(Node n : nodes){
            float ax = nx(n) - x, ay = ny(n) - y;
            if(lx >= ax && ly >= ay && lx <= ax + Scl.scl(nodeW) && ly <= ay + Scl.scl(nodeH)) return n;
        }
        return null;
    }

    @Override
    public void draw(){
        validate();
        float w = Scl.scl(nodeW), h = Scl.scl(nodeH);
        //edges: from each input's right side to its parent's left side
        Lines.stroke(Scl.scl(2.5f));
        for(Node n : nodes){
            if(n.parent == null) continue;
            float x1 = nx(n) + w, y1 = ny(n) + h / 2f, x2 = nx(n.parent), y2 = ny(n.parent) + h / 2f, mx = (x1 + x2) / 2f;
            Draw.color(Color.valueOf("45454f"), parentAlpha);
            Lines.curve(x1, y1, mx, y1, mx, y2, x2, y2, 20);
            Fill.tri(x2, y2, x2 - Scl.scl(7f), y2 + Scl.scl(4f), x2 - Scl.scl(7f), y2 - Scl.scl(4f));
        }
        Font font = Fonts.def;
        GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        for(Node n : nodes){
            float ax = nx(n), ay = ny(n);
            boolean product = n.parent == null, raw = n.step.source == null;
            Draw.color(Color.valueOf("101015"), parentAlpha);
            Fill.crect(ax, ay, w, h);
            Draw.color(product ? Pal.accent : n == hovered ? Color.valueOf("5a5a68") : Color.valueOf("30303b"), parentAlpha);
            Lines.stroke(Scl.scl(2f));
            Lines.rect(ax, ay, w, h);
            Draw.color(1f, 1f, 1f, parentAlpha);
            Draw.rect(n.step.item.uiIcon, ax + Scl.scl(22f), ay + h - Scl.scl(22f), Scl.scl(30f), Scl.scl(30f));
            font.getData().setScale(0.85f);
            font.setColor(product ? Pal.accent : Color.white);
            font.draw(fit(font, layout, n.step.item.localizedName, w - Scl.scl(48f)), ax + Scl.scl(42f), ay + h - Scl.scl(10f));
            font.getData().setScale(0.75f);
            font.setColor(WikiUI.cyan);
            font.draw(Strings.autoFixed(n.step.perSecond, 2) + "/s" + (raw ? "  " + Core.bundle.get("eclipse-wiki.chain.raw") : ""), ax + Scl.scl(42f), ay + h - Scl.scl(30f));
            if(n.step.source != null){
                Draw.color(1f, 1f, 1f, parentAlpha);
                Draw.rect(n.step.source.block.uiIcon, ax + Scl.scl(14f), ay + Scl.scl(13f), Scl.scl(18f), Scl.scl(18f));
                font.setColor(Color.lightGray);
                String liq = n.step.liquids.isEmpty() ? "" : "  + " + n.step.liquids.first().liquid.localizedName;
                font.draw(fit(font, layout, "×" + Strings.autoFixed(n.step.machines, 2) + " " + n.step.source.block.localizedName + liq, w - Scl.scl(30f)), ax + Scl.scl(27f), ay + Scl.scl(19f));
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
