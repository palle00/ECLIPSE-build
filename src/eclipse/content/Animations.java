package eclipse.content;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.draw.*;

import static mindustry.Vars.*;

/**
 * Production animation for every ECLIPSE crafter, active only while the block is producing:
 * its "-glow" layer pulses in the colour of what it makes, its "-rotator" layer (agitators, fans, gears,
 * centrifuges, crushers), where the sprite generator drew one, turns around the block centre, and its "-press" layer
 * (press plates, mould clamps) presses down at the end of every production cycle.
 * Crafters that already had a glow keep their own colour; heat-output blocks keep "-glow" for their heat port.
 */
public class Animations{

    public static void load(){
        for(Block b : content.blocks()){
            if(!b.name.startsWith("eclipse-") || !(b instanceof GenericCrafter c)) continue;
            Seq<DrawBlock> parts = c.drawer instanceof DrawMulti m ? Seq.with(m.drawers) : Seq.with(c.drawer);
            int main = parts.indexOf(d -> d instanceof DrawDefault);
            parts.insert(main + 1, new DrawRotor());
            parts.insert(main + 1, new DrawPress());
            boolean hasGlow = parts.contains(d -> d instanceof DrawGlowRegion || d instanceof DrawHeatOutput);
            if(!hasGlow){
                DrawGlowRegion glow = new DrawGlowRegion();
                glow.color = productColor(c);
                glow.alpha = 0.75f;
                parts.add(glow);
            }
            c.drawer = new DrawMulti(parts.toArray(DrawBlock.class));
        }
    }

    static Color productColor(GenericCrafter c){
        if(c.outputItem != null) return c.outputItem.item.color;
        if(c.outputItems != null && c.outputItems.length > 0) return c.outputItems[0].item.color;
        if(c.outputLiquid != null) return c.outputLiquid.liquid.color;
        if(c.outputLiquids != null && c.outputLiquids.length > 0) return c.outputLiquids[0].liquid.color;
        return Pal.accent;
    }

    /**
     * Draws "&lt;block&gt;-press": a press plate seen from above. Over the last 30% of each production cycle it goes
     * down and comes back up; going down, it looks a little smaller and darker (further from the camera).
     * Does nothing for blocks that have no press sprite.
     */
    public static class DrawPress extends DrawBlock{
        public float depth = 0.07f;
        public TextureRegion region;

        @Override
        public void load(Block block){
            region = Core.atlas != null && Core.atlas.has(block.name + "-press") ? Core.atlas.find(block.name + "-press") : null;
        }

        @Override
        public void draw(Building build){
            if(region == null) return;
            float down = Mathf.slope(Mathf.curve(build.progress(), 0.7f, 1f)) * Mathf.clamp(build.warmup() * 2f);
            float scale = 1f - depth * down;
            Draw.color(Tmp.c1.set(Color.white).mul(1f - 0.18f * down));
            Draw.rect(region, build.x, build.y, region.width * region.scl() * scale, region.height * region.scl() * scale);
            Draw.color();
        }

        @Override
        public TextureRegion[] icons(Block block){
            return region == null ? new TextureRegion[0] : new TextureRegion[]{region};
        }
    }

    /** Draws "<block>-rotator" spinning with production progress; does nothing for blocks that have no rotator sprite. */
    public static class DrawRotor extends DrawBlock{
        public float speed = 2.5f;
        public TextureRegion region;

        @Override
        public void load(Block block){
            region = Core.atlas != null && Core.atlas.has(block.name + "-rotator") ? Core.atlas.find(block.name + "-rotator") : null;
        }

        @Override
        public void draw(Building build){
            if(region != null) Draw.rect(region, build.x, build.y, build.totalProgress() * speed);
        }

        @Override
        public TextureRegion[] icons(Block block){
            return region == null ? new TextureRegion[0] : new TextureRegion[]{region};
        }
    }
}
