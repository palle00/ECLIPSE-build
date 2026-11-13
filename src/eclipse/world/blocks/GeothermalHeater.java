package eclipse.world.blocks;

import arc.*;
import arc.math.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.world.*;
import mindustry.world.blocks.heat.*;
import mindustry.world.meta.*;

/**
 * Geothermal Heat Tap: a heat producer that needs no fuel but only works on hot floors. Its heat is
 * {@link #heatOutput} times the floor's summed heat attribute (hot rock, magma rock), like the vanilla thermal
 * generator but feeding the heat network instead of power.
 */
public class GeothermalHeater extends HeatProducer{
    public Attribute attribute = Attribute.heat;

    public GeothermalHeater(String name){
        super(name);
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        return sumAttribute(attribute, tile.x, tile.y) > 0.01f;
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        drawPlaceText(Core.bundle.format("eclipse-geothermal.heat", Strings.autoFixed(sumAttribute(attribute, x, y) * heatOutput, 1)), x, y, valid);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.remove(Stat.output);
        stats.add(Stat.output, Core.bundle.format("stat.eclipse-geothermal-value", Strings.autoFixed(heatOutput, 1)));
    }

    public class GeothermalBuild extends HeatProducerBuild{
        public float attrSum;

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();
            attrSum = sumAttribute(attribute, tile.x, tile.y);
        }

        @Override
        public void updateTile(){
            heat = Mathf.approachDelta(heat, enabled ? heatOutput * attrSum : 0f, warmupRate);
        }
    }
}
