package eclipse.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.input.*;
import mindustry.ui.fragments.*;

import static mindustry.Vars.*;

/**
 * The map screen (M) turns live when zoomed in: past the zoom where the map is as large as the game's own furthest
 * zoom, the map picture fades out and the real game shows through, tinted and with scrolling old-TV scanlines.
 * <p>
 * Vanilla's map element always paints the whole screen black, so while the live view shows, it is hidden and this
 * element takes over its input (scroll and pinch zoom, drag to pan, Escape). The camera is only moved to the map's
 * view while the world is drawn and put back right after, so the player's unit, detached cameras and building input
 * never see it.
 */
public class LiveMap{
    /** How far past the live threshold (as a camera scale factor) the map takes to fade out completely. */
    static final float fadeRange = 1.6f;
    static final Color tint = Color.valueOf("5fd3ff");

    static MinimapFragment frag;
    /** 0 = map picture only, 1 = fully live; below 0 the vanilla map element is in charge. */
    static float live = -1f;
    static final arc.math.geom.Vec2 savedPos = new arc.math.geom.Vec2();
    static float savedW, savedH;
    static boolean moved;

    /** Call on ClientLoadEvent. */
    public static void install(){
        frag = ui.minimapfrag;
        Element vanilla = frag.elem;
        vanilla.visible(() -> frag.shown() && live < 0f);

        Element overlay = new Element(){
            @Override
            public void draw(){
                drawOverlay();
            }
        };
        overlay.touchable = Touchable.enabled;
        overlay.visible(() -> frag.shown() && live >= 0f);
        overlay.update(() -> {
            overlay.setBounds(0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
            if(!ui.chatfrag.shown() && !(Core.scene.getKeyboardFocus() instanceof arc.scene.ui.TextField)){
                overlay.requestKeyboard();
                overlay.requestScroll();
            }
            if(Core.input.keyTap(Binding.menu)) frag.hide();
        });
        overlay.addListener(new InputListener(){
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY){
                float zoom = zoom();
                setZoom(zoom - amountY / 10f * zoom);
                return true;
            }
        });
        overlay.addListener(new ElementGestureListener(){
            float lastZoom = -1f;

            @Override
            public void zoom(InputEvent event, float initialDistance, float distance){
                if(lastZoom < 0f) lastZoom = LiveMap.zoom();
                setZoom(distance / initialDistance * lastZoom);
            }

            @Override
            public void pan(InputEvent event, float x, float y, float deltaX, float deltaY){
                float zoom = LiveMap.zoom();
                Reflect.set(frag, "panx", (float)Reflect.get(frag, "panx") + deltaX / zoom);
                Reflect.set(frag, "pany", (float)Reflect.get(frag, "pany") + deltaY / zoom);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                lastZoom = -1f;
            }
        });
        vanilla.parent.addChildAfter(vanilla, overlay);

        Events.run(Trigger.update, LiveMap::updateLive);
        Events.run(Trigger.preDraw, LiveMap::moveCamera);
        Events.run(Trigger.postDraw, LiveMap::restoreCamera);
    }

    static float zoom(){
        return Reflect.get(frag, "zoom");
    }

    /** Same limits as vanilla's map zoom. */
    static void setZoom(float zoom){
        Reflect.set(frag, "zoom", Mathf.clamp(zoom, 0.25f, 10f));
    }

    /** Screen pixels per world unit the map is drawn at (vanilla draws one tile as 5 scaled pixels per zoom step). */
    static float mapScale(){
        return Scl.scl(5f) * zoom() / tilesize;
    }

    static void updateLive(){
        if(frag == null || !frag.shown() || !state.isGame() || renderer.minimap.getTexture() == null){
            live = -1f;
            return;
        }
        float start = renderer.minScale();
        float s = mapScale();
        live = s < start ? -1f : Mathf.clamp((s - start) / (start * (fadeRange - 1f)));
    }

    /** The world point at the centre of the map screen, from vanilla's pan and zoom. */
    static void mapCenter(arc.math.geom.Vec2 out){
        float zoom = zoom();
        float size = Scl.scl(5f) * zoom * world.width();
        float ratio = (float)renderer.minimap.getTexture().height / renderer.minimap.getTexture().width;
        float panx = Reflect.get(frag, "panx"), pany = Reflect.get(frag, "pany");
        out.set((size / 2f - panx * zoom) / size * world.unitWidth() - tilesize / 2f,
            (size * ratio / 2f - pany * zoom) / (size * ratio) * world.unitHeight() - tilesize / 2f);
    }

    static void moveCamera(){
        moved = live >= 0f && frag.shown();
        if(!moved) return;
        savedPos.set(Core.camera.position);
        savedW = Core.camera.width;
        savedH = Core.camera.height;
        float s = mapScale();
        mapCenter(Core.camera.position);
        Core.camera.width = Core.graphics.getWidth() / s;
        Core.camera.height = Core.graphics.getHeight() / s;
    }

    static void restoreCamera(){
        if(!moved) return;
        moved = false;
        Core.camera.position.set(savedPos);
        Core.camera.width = savedW;
        Core.camera.height = savedH;
        Core.camera.update();
    }

    static void drawOverlay(){
        float w = Core.graphics.getWidth(), h = Core.graphics.getHeight();
        float t = Mathf.clamp(live);

        //the map picture, fading out over the live world
        if(t < 1f && renderer.minimap.getTexture() != null){
            float zoom = zoom();
            float size = Scl.scl(5f) * zoom * world.width();
            float ratio = (float)renderer.minimap.getTexture().height / renderer.minimap.getTexture().width;
            float panx = Reflect.get(frag, "panx"), pany = Reflect.get(frag, "pany");
            Draw.color(Color.black, 1f - t);
            Fill.crect(0, 0, w, h);
            Draw.color(1f, 1f, 1f, 1f - t);
            Draw.rect(Draw.wrap(renderer.minimap.getTexture()), w / 2f + panx * zoom, h / 2f + pany * zoom, size, size * ratio);
        }

        //colour tint
        Draw.color(tint, 0.14f * t);
        Fill.crect(0, 0, w, h);

        //old-TV scanlines: fine dark lines plus a soft bright band rolling down the screen
        float gap = Math.max(Scl.scl(3f), 2f);
        Draw.color(0f, 0f, 0f, 0.22f * t);
        for(float y = 0; y < h; y += gap) Fill.crect(0, y, w, gap / 3f);

        float band = h * 0.18f, bandY = h - (Time.globalTime * Scl.scl(1.2f)) % (h + band);
        for(int i = 0; i < 12; i++){
            float f = i / 11f;
            Draw.color(tint, 0.05f * t * Mathf.slope(f));
            Fill.crect(0, bandY - band * f, w, band / 12f);
        }

        //a faint flicker
        Draw.color(1f, 1f, 1f, 0.015f * t * Mathf.absin(Time.globalTime, 2.3f, 1f));
        Fill.crect(0, 0, w, h);

        Draw.reset();
    }
}
