package eclipse.type;

import arc.graphics.*;
import mindustry.type.*;
import mindustry.world.meta.*;

/** A liquid that corrodes ordinary liquid blocks. See {@link eclipse.systems.Corrosion}. */
public class CorrosiveLiquid extends Liquid{
    public static final Stat corrosiveStat = new Stat("eclipse-corrosive");

    public CorrosiveLiquid(String name, Color color){
        super(name, color);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(corrosiveStat, true);
    }
}
