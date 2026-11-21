package eclipse.ui;

import arc.graphics.g2d.*;
import arc.scene.*;
import arc.scene.style.*;

/**
 * An icon drawn at exactly its cell's size, keeping its shape. The game's icons are font glyphs that draw at the
 * font's size whatever size their image is given, so a small icon box would show a 28-pixel icon spilling out of it.
 */
public class SizedIcon extends Element{
    final Drawable drawable;
    final @arc.util.Nullable TextureRegion region;

    public SizedIcon(Drawable drawable){
        this.drawable = drawable;
        region = drawable instanceof TextureRegionDrawable t ? t.getRegion() : null;
    }

    @Override
    public void draw(){
        Draw.color(color.r, color.g, color.b, color.a * parentAlpha);
        if(region == null || region.width <= 0 || region.height <= 0){
            drawable.draw(x, y, width, height);
        }else{
            float k = Math.min(width / region.width, height / region.height);
            Draw.rect(region, x + width / 2f, y + height / 2f, region.width * k, region.height * k);
        }
        Draw.reset();
    }
}
