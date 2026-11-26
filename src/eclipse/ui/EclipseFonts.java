package eclipse.ui;

import arc.Core;
import arc.freetype.FreeTypeFontGenerator;
import arc.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import arc.graphics.Color;
import arc.graphics.Texture.TextureFilter;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.PixmapPacker;
import arc.scene.ui.Label;
import arc.scene.ui.Label.LabelStyle;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Scl;
import arc.struct.IntMap;
import arc.util.Log;
import mindustry.ui.Fonts;

/** Sizing helpers for crisp fonts; no GL or native state. */
public class EclipseFonts{
    public enum Kind{ plain, outline }

    private static final IntMap<Font> plainCache = new IntMap<>();
    private static final IntMap<Font> outlineCache = new IntMap<>();

    private static FreeTypeFontGenerator generator;
    private static PixmapPacker packer;

    /** Baseline UI font size in screen pixels. */
    public static float basePx(){
        return Scl.scl(18f);
    }

    /** Clamps to 8..128 and rounds to an even pixel size so glyphs stay crisp. */
    public static int px(float target){
        return (int)Math.max(8, Math.min(128, Math.round(target / 2f) * 2f));
    }

    /** Bakes a font on demand at an even pixel size; one generator and packer are shared. */
    public static Font get(Kind kind, float targetPx){
        int px = px(targetPx);
        IntMap<Font> cache = kind == Kind.outline ? outlineCache : plainCache;
        Font cached = cache.get(px);
        if(cached != null) return cached;
        Log.debug("EclipseFont baked kind=@ px=@", kind, px);

        if(generator == null){
            generator = new FreeTypeFontGenerator(Core.files.internal("fonts/font.woff"));
            packer = new PixmapPacker(Math.min(FreeTypeFontGenerator.getMaxTextureSize(), 4096), 2048, 2, true);
        }

        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = px;
        p.incremental = true;
        p.characters = "\0";
        p.minFilter = TextureFilter.linear;
        p.magFilter = TextureFilter.linear;
        p.packer = packer;
        if(kind == Kind.outline){
            p.borderColor = Color.darkGray;
            p.borderWidth = px / 9f;
            p.spaceX -= p.borderWidth;
        }else{
            p.shadowColor = Color.darkGray;
            p.shadowOffsetY = Math.max(1, Math.round(px / 9f));
        }

        Font font = generator.generateFont(p);
        assert font.getData().scaleX == 1f;
        cache.put(px, font);
        return font;
    }

    public static <T extends Label> Cell<T> crisp(Cell<T> cell, float scale){
        crisp(cell.get(), scale);
        return cell;
    }

    public static void crisp(Label label, float scale){
        LabelStyle old = label.getStyle();
        LabelStyle style = new LabelStyle(old);
        style.font = get(old.font == Fonts.outline ? Kind.outline : Kind.plain, basePx() * scale);
        label.setStyle(style);
        label.setFontScale(1f);
        label.invalidate();
    }
}
