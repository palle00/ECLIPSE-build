package eclipse.world.blocks;

import arc.*;
import arc.graphics.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.ui.*;
import mindustry.world.blocks.heat.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * A heat conductor with a reservoir. While heat flows in, it passes up to {@link #maxOutput} heat out of the front
 * and charges. When the heaters stop, it keeps delivering the last heat level until the reservoir is empty,
 * so heat-driven machines ride out fuel shortages.
 */
public class ThermalStorage extends HeatConductor{
    public static final Stat capacityStat = new Stat("eclipse-heat-capacity", StatCat.crafting);

    /** Stored heat in heat-seconds (e.g. 600 = 20 heat for 30 seconds). */
    public float capacity = 600f;
    public float maxOutput = 20f;

    public ThermalStorage(String name){
        super(name);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(capacityStat, Core.bundle.format("stat.eclipse-heat-capacity-value", (int)capacity, (int)maxOutput));
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("eclipse-stored-heat", (ThermalBuild e) -> new Bar(
            () -> Core.bundle.format("bar.eclipse-stored-heat", (int)e.stored),
            () -> Color.valueOf("ff8a3d"),
            () -> e.stored / capacity));
    }

    public class ThermalBuild extends HeatConductorBuild{
        public float stored, level;

        @Override
        public void updateTile(){
            updateHeat();
        }

        @Override
        public void updateHeat(){
            if(lastHeatUpdate == state.updateId) return;
            lastHeatUpdate = state.updateId;
            float incoming = enabled ? calculateHeat(sideHeat, cameFrom) : 0f;
            if(incoming > 0.001f){
                level = Math.min(incoming, maxOutput);
                stored = Math.min(capacity, stored + level * Time.delta / 60f);
                heat = level;
            }else if(stored > 0f){
                stored = Math.max(0f, stored - level * Time.delta / 60f);
                heat = level;
            }else{
                heat = 0f;
            }
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(stored);
            write.f(level);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1){
                stored = read.f();
                level = read.f();
            }
        }
    }
}
