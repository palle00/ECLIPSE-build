package eclipse.defense;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.graphics.*;
import mindustry.world.blocks.units.*;

/**
 * Titan Manufacturing Complex (military M5): a vanilla unit factory for one titan-class unit, limited to
 * one titan alive per team. While the team's titan lives, the complex pauses and uses nothing.
 */
public class TitanComplex extends UnitFactory{

    /** "-crane": the gantry crane over the assembly bay, travelling along the titan as it is built. */
    public TextureRegion craneRegion;

    public TitanComplex(String name){
        super(name);
    }

    @Override
    public void load(){
        super.load();
        craneRegion = Core.atlas.find(name + "-crane");
    }

    public class TitanBuild extends UnitFactoryBuild{
        public boolean titanAlive(){
            return currentPlan >= 0 && team.data().countType(plans.get(currentPlan).unit) > 0;
        }

        @Override
        public void created(){
            super.created();
            if(currentPlan < 0) currentPlan = 0;
        }

        @Override
        public void draw(){
            super.draw();
            if(!craneRegion.found()) return;
            boolean building = currentPlan >= 0 && efficiency > 0f && !titanAlive();
            float travel = building ? Mathf.lerp(-18f, 18f, fraction()) : 0f;
            Draw.z(Layer.blockOver + 0.2f);
            Draw.rect(craneRegion, x, y + travel);
            Draw.z(Layer.block);
        }

        @Override
        public boolean shouldConsume(){
            return super.shouldConsume() && !titanAlive();
        }
    }
}
