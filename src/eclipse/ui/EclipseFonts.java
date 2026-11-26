package eclipse.ui;

import arc.scene.ui.layout.Scl;

/** Sizing helpers for crisp fonts; no GL or native state. */
public class EclipseFonts{
    public enum Kind{ plain, outline }

    /** Baseline UI font size in screen pixels. */
    public static float basePx(){
        return Scl.scl(18f);
    }

    /** Clamps to 8..128 and rounds to an even pixel size so glyphs stay crisp. */
    public static int px(float target){
        return (int)Math.max(8, Math.min(128, Math.round(target / 2f) * 2f));
    }
}
