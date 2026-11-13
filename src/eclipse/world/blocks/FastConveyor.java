package eclipse.world.blocks;

import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.distribution.*;

/**
 * A conveyor whose throughput is exactly proportional to its speed.
 * <p>
 * Vanilla conveyors place an incoming item at the very start of the tile, throwing away the distance the item ahead
 * has already moved past the minimum spacing. That rounds throughput to 60 / (whole ticks per item): 60, 30, 20, 15...
 * so 40 or 50 items/s are impossible. This conveyor places the item exactly one spacing behind the item ahead,
 * keeping that distance, so items/s = speed * 60 / {@link #itemSpace}.
 */
public class FastConveyor extends Conveyor{
    /** Must match the vanilla conveyor's (private) item spacing. */
    public static final float itemSpace = 0.4f;

    public FastConveyor(String name){
        super(name);
    }

    /** Speed for a target throughput in items per second. */
    public static float speedFor(float itemsPerSecond){
        return itemsPerSecond * itemSpace / 60f;
    }

    public class FastConveyorBuild extends ConveyorBuild{

        @Override
        public void handleItem(Building source, Item item){
            int before = len;
            float carry = len > 0 ? ys[0] - itemSpace : 0f;
            super.handleItem(source, item);

            //an item entering from behind is inserted at index 0, position 0; move it up to keep the spacing exact
            if(len > before && before > 0 && ys[0] == 0f && carry > 0f){
                ys[0] = Math.min(carry, 1f);
                minitem = ys[0];
            }
        }
    }
}
