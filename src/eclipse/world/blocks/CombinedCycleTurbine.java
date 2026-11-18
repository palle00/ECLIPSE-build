package eclipse.world.blocks;

import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.blocks.heat.*;
import mindustry.world.blocks.power.*;
import mindustry.world.meta.*;

/**
 * A gas turbine whose hot exhaust also gives heat out of its front: the "combined cycle". Put an Industrial Boiler there
 * and its Steam Turbines turn the exhaust into more power. Heat follows the turbine's output.
 */
public class CombinedCycleTurbine extends ConsumeGenerator{
    /** Heat out of the front at full output. */
    public float heatOutput = 10f;

    public CombinedCycleTurbine(String name){
        super(name);
        rotate = true;
        rotateDraw = false;
        drawArrow = true;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.output, heatOutput, StatUnit.heatUnits);
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("heat", (TurbineBuild e) -> new Bar("bar.heat", Pal.lightOrange, e::heatFrac));
    }

    public class TurbineBuild extends ConsumeGeneratorBuild implements HeatBlock{
        @Override
        public float heat(){
            return heatOutput * productionEfficiency;
        }

        @Override
        public float heatFrac(){
            return productionEfficiency;
        }
    }
}
