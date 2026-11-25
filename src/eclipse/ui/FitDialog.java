package eclipse.ui;

import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;

/**
 * Shrinks a full-screen dialog on a screen too small for it (laptops, phones; players' reports, 2026-11-25): when the
 * dialog needs more room than the screen has, it is laid out on a larger virtual screen and drawn scaled down to fill
 * the real one, so nothing is cut off. On screens big enough it is left exactly as it was.
 * <p>
 * The room it needs: its own minimum size, plus whatever is cut off inside its vertical-only scroll panes that widen
 * with the dialog (their rows can't scroll sideways). A pane in a fixed-width column doesn't count: a wider dialog would
 * not give it more room. Nor do panes that scroll sideways on purpose (diagrams, graphs), or anything inside them.
 */
class FitDialog{
    /** Never smaller than this: below it the text can't be read anyway. */
    static final float minScale = 0.5f;

    static void install(Dialog d){
        //a child rather than d.update(), which would replace a dialog's own update hook
        d.addChild(new Element(){
            @Override
            public void act(float delta){
                super.act(delta);
                fit(d);
            }
        });
    }

    static void fit(Dialog d){
        if(d.parent == null) return;
        float w = d.parent.getWidth(), h = d.parent.getHeight();
        float cut = cutOff(d, d, Float.NEGATIVE_INFINITY);
        //the width it lays out at now, plus the most a pane cuts off (or minus the least spare room any pane has)
        float needW = cut == Float.NEGATIVE_INFINITY ? d.getMinWidth() : Math.max(d.getMinWidth(), d.getWidth() + cut);
        float k = Math.max(minScale, Math.min(1f, Math.min(w / needW, h / d.getMinHeight())));
        if(k >= 0.99f){
            if(!d.fillParent){
                d.setFillParent(true);
                d.setScale(1f);
                d.setKeepWithinStage(true);
                d.invalidate();
            }
            return;
        }
        if(d.fillParent || Math.abs(k - d.scaleX) > 0.005f){
            d.setFillParent(false);
            d.setKeepWithinStage(false); //it measures the unscaled size and would push the dialog off-centre
            d.setTransform(true);
            d.setScale(k);
        }
        //every frame: Dialog.show() packs the dialog to its preferred size
        d.setSize(w / k, h / k);
        d.setOrigin(Align.center); //scaled about its centre, and Dialog centres it: it covers the screen exactly
        d.centerWindow();
    }

    /** The most width cut off inside a widening vertical-only scroll pane under e (negative: the least room to spare), or max if none. */
    static float cutOff(Dialog d, Element e, float max){
        if(e instanceof ScrollPane p){
            Element content = p.getWidget();
            return p.isScrollingDisabledX() && content != null && widens(d, p) ? Math.max(max, content.getMinWidth() - content.getWidth()) : max;
        }
        if(e instanceof Group g) for(Element child : g.getChildren()) max = cutOff(d, child, max);
        return max;
    }

    /** Whether e gets wider with the dialog: every cell between them stretches sideways and has no fixed width. */
    static boolean widens(Dialog d, Element e){
        for(; e != d && e.parent != null; e = e.parent){
            if(!(e.parent instanceof Table t)) continue;
            Cell<?> c = t.getCell(e);
            if(c == null) return false;
            float maxWidth = Reflect.get(c, "maxWidth"), fillX = Reflect.get(c, "fillX");
            int expandX = Reflect.get(c, "expandX");
            if(maxWidth > 0f || (expandX == 0 && fillX <= 0f)) return false;
        }
        return true;
    }
}
