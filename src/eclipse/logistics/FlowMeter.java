package eclipse.logistics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

/**
 * Flow Meter: placed inline in a belt, it passes items from its back straight out of its front and shows how many
 * items per second went through, averaged over the last {@link #window} seconds, on a small LCD drawn on the block:
 * the last item's icon, the rate in green digits and "IPS". The screen stays upright; only the ports turn.
 * It never holds items back: it only accepts an item when the belt in front can take it, and allows more per second
 * than the fastest belt, so it does not slow the line down. Like other instant routers, it needs a belt (not another
 * router) on both sides.
 */
public class FlowMeter extends RoutingBlock{
    /** Averaging window in seconds, split into slots of {@link #slotTicks}. */
    public static final int window = 5;
    public static final float slotTicks = 30f;
    static final int slots = (int)(window * 60f / slotTicks);

    public TextureRegion portsRegion;

    public FlowMeter(String name){
        super(name);
        configurable = false;
        saveConfig = false;
        itemsPerSecond = 120f;
    }

    @Override
    public void load(){
        super.load();
        portsRegion = Core.atlas.find(name + "-ports");
    }

    @Override
    protected TextureRegion[] icons(){
        return new TextureRegion[]{region, portsRegion};
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("eclipse-flow", (FlowBuild e) -> new Bar(
            () -> Core.bundle.format("bar.eclipse-flow", Strings.autoFixed(e.rate(), 1)),
            () -> Pal.items,
            () -> Math.min(e.rate() / 60f, 1f)));
    }

    public class FlowBuild extends RoutingBuild{
        final int[] counts = new int[slots];
        /** Per item type (by item id), per slot; created when the first item passes. */
        @Nullable int[][] itemCounts;
        int slot;
        float slotTimer, elapsed;
        /** Items passed since the meter was built (not saved). */
        public int total;
        /** The last item that passed, shown on the screen. */
        public @Nullable Item lastItem;

        /** Items per second over the last few seconds (or since placement, if shorter). */
        public float rate(){
            int sum = 0;
            for(int c : counts) sum += c;
            return sum / seconds();
        }

        /** Items per second of one item type: a meter on a mixed belt reports each material separately. */
        public float rate(Item item){
            if(itemCounts == null || item.id >= itemCounts[0].length) return 0f;
            int sum = 0;
            for(int[] s : itemCounts) sum += s[item.id];
            return sum / seconds();
        }

        float seconds(){
            return Math.max(Math.min(elapsed, (slots - 1) * slotTicks + slotTimer), slotTicks) / 60f;
        }

        // ---- logic: "sensor r meter1 @copper" = copper per second, "@totalItems" = all items per second, "@firstItem" = last item ----

        @Override
        public double sense(Content content){
            return content instanceof Item item ? rate(item) : 0d;
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.totalItems) return rate();
            return super.sense(sensor);
        }

        @Override
        public Object senseObject(LAccess sensor){
            if(sensor == LAccess.firstItem) return lastItem;
            return super.senseObject(sensor);
        }

        @Override
        public boolean acceptsFrom(Building source){
            return rel(source) == 2;
        }

        @Override
        public @Nullable Building route(@Nullable Building source, Item item, boolean commit){
            Building out = firstOnSide(source, 0, item);
            if(out == null && commit) markBlocked(0);
            return out;
        }

        @Override
        public void handleItem(Building source, Item item){
            counts[slot]++;
            if(itemCounts == null) itemCounts = new int[slots][content.items().size];
            itemCounts[slot][item.id]++;
            total++;
            lastItem = item;
            super.handleItem(source, item);
        }

        @Override
        public void updateTile(){
            super.updateTile();
            //real seconds, not the block's own time scale (overdrive must not change the reading)
            elapsed = Math.min(elapsed + Time.delta, slots * slotTicks);
            slotTimer += Time.delta;
            while(slotTimer >= slotTicks){
                slotTimer -= slotTicks;
                slot = (slot + 1) % slots;
                counts[slot] = 0;
                if(itemCounts != null) java.util.Arrays.fill(itemCounts[slot], 0);
            }
        }

        @Override
        public void draw(){
            Draw.rect(block.region, x, y);
            Draw.rect(portsRegion, x, y, rotdeg());
            drawBlocked();
            drawScreen();
        }

        /** The LCD: the rate in large digits on top, a small icon of the last item, then "IPS"; unlit segments glow faintly. */
        void drawScreen(){
            float r = rate();
            boolean active = r > 0f;
            String text = r < 9.95f ? Strings.autoFixed(r, 1) : String.valueOf(Math.round(r));

            if(active){
                Draw.color(lit, 0.07f);
                Fill.crect(px(screenX), py(screenY + screenH), screenW * pixel, screenH * pixel);
            }
            //number: rows 4-13, centred over the full screen width
            text(text, screenX + (screenW - textWidth(text, 2)) / 2, 4, 2, active ? lit : dim, true);
            //item: a small 4-pixel icon in rows 15-18, a pixel clear of the number and the label
            if(lastItem != null){
                Draw.color(Color.white, active ? 1f : 0.45f);
                Draw.rect(lastItem.fullIcon, px(screenX + screenW / 2f), py(17f), 4 * pixel, 4 * pixel);
            }
            //label: rows 20-24
            text("IPS", screenX + (screenW - textWidth("IPS", 1)) / 2, 20, 1, label, false);
            Draw.reset();
        }

        /** Draws text left to right from sprite pixel (sx, sy); spacing matches {@link #textWidth}. */
        void text(String text, int sx, int sy, int scale, Color color, boolean segments){
            for(int i = 0; i < text.length(); i++){
                char c = text.charAt(i);
                sx += gapBefore(text, i);
                if(c == '.'){
                    //a narrow point: one pixel wide, as tall as one font cell, on the baseline
                    Draw.color(color);
                    Fill.crect(px(sx), py(sy + 5 * scale), pixel, scale * pixel);
                }else{
                    if(segments) glyph('8', sx, sy, scale, ghost);
                    glyph(c, sx, sy, scale, color);
                }
                sx += glyphWidth(c, scale);
            }
        }

        /** World x / y of a sprite pixel edge (sprite pixels in image coordinates: x right, y down from the top-left). */
        float px(float spriteX){
            return x - 4f + spriteX * pixel;
        }

        float py(float spriteY){
            return y + 4f - spriteY * pixel;
        }

        void glyph(char c, int sx, int sy, int scale, Color color){
            int[] rows = font(c);
            if(rows == null) return;
            Draw.color(color);
            for(int row = 0; row < 5; row++){
                for(int col = 0; col < 3; col++){
                    if((rows[row] >> (2 - col) & 1) == 0) continue;
                    Fill.crect(px(sx + col * scale), py(sy + (row + 1) * scale), scale * pixel, scale * pixel);
                }
            }
        }
    }

    // ---- the meter's pixel font: 3x5 cells, drawn on the sprite's own pixel grid so it stays sharp ----

    /** One sprite pixel in world units (32 pixels per 8-unit tile). */
    static final float pixel = 0.25f;
    /** Screen area in sprite pixels (see SpriteGen.flowMeter). */
    static final int screenX = 4, screenY = 4, screenW = 24, screenH = 21;
    static final Color lit = Color.valueOf("5dff7a"), dim = Color.valueOf("3a8c4e"), ghost = Color.valueOf("173823"), label = Color.valueOf("3fae5a");

    /** Width in sprite pixels: 3 cells for letters and digits, a single pixel for the decimal point. */
    static int glyphWidth(char c, int scale){
        return c == '.' ? 1 : 3 * scale;
    }

    /** One pixel between characters. */
    static int gapBefore(String text, int i){
        return i > 0 ? 1 : 0;
    }

    static int textWidth(String text, int scale){
        int w = 0;
        for(int i = 0; i < text.length(); i++) w += glyphWidth(text.charAt(i), scale) + gapBefore(text, i);
        return w;
    }

    /** Rows top to bottom, 3 bits each (left = 4). */
    static @Nullable int[] font(char c){
        return switch(c){
            case '0' -> new int[]{7, 5, 5, 5, 7};
            case '1' -> new int[]{2, 6, 2, 2, 7};
            case '2' -> new int[]{7, 1, 7, 4, 7};
            case '3' -> new int[]{7, 1, 7, 1, 7};
            case '4' -> new int[]{5, 5, 7, 1, 1};
            case '5' -> new int[]{7, 4, 7, 1, 7};
            case '6' -> new int[]{7, 4, 7, 5, 7};
            case '7' -> new int[]{7, 1, 1, 1, 1};
            case '8' -> new int[]{7, 5, 7, 5, 7};
            case '9' -> new int[]{7, 5, 7, 1, 7};
            case 'I' -> new int[]{7, 2, 2, 2, 7};
            case 'P' -> new int[]{6, 5, 6, 4, 4};
            case 'S' -> new int[]{3, 4, 2, 1, 6};
            default -> null;
        };
    }
}
