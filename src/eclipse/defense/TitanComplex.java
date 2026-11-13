package eclipse.defense;

import mindustry.world.blocks.units.*;

/**
 * Titan Manufacturing Complex (military M5): a vanilla unit factory for one titan-class unit, limited to
 * one titan alive per team. While the team's titan lives, the complex pauses and uses nothing.
 */
public class TitanComplex extends UnitFactory{

    public TitanComplex(String name){
        super(name);
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
        public boolean shouldConsume(){
            return super.shouldConsume() && !titanAlive();
        }
    }
}
