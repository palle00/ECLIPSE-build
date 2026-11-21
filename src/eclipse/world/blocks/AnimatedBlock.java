package eclipse.world.blocks;

import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.draw.*;

/**
 * A plain block that draws through a {@link DrawBlock} like a crafter does, so its sprite can have moving layers
 * (spinning, swinging, glowing parts). Its build warms up while {@link AnimatedBuild#animate} is told it is working,
 * and its progress (what spinning and swinging parts follow) only runs while warm.
 */
public class AnimatedBlock extends Block{
    public DrawBlock drawer = new DrawDefault();

    public AnimatedBlock(String name){
        super(name);
    }

    @Override
    public void load(){
        super.load();
        drawer.load(this);
    }

    @Override
    protected TextureRegion[] icons(){
        return drawer.finalIcons(this);
    }

    public class AnimatedBuild extends Building{
        /** Named apart from the fields subclasses already have (a module assembler's progress). */
        public float animWarmup, animTime;

        /** Call once per update with whether the block is working. */
        public void animate(boolean working){
            animWarmup = Mathf.approachDelta(animWarmup, working ? 1f : 0f, 0.02f);
            animTime += animWarmup * delta();
        }

        @Override
        public float warmup(){
            return animWarmup;
        }

        @Override
        public float totalProgress(){
            return animTime;
        }

        @Override
        public void draw(){
            drawer.draw(this);
        }

        @Override
        public void drawLight(){
            super.drawLight();
            drawer.drawLight(this);
        }
    }
}
