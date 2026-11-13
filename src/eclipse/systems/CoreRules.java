package eclipse.systems;

import eclipse.augment.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

/** Vanilla cores refuse augmentation modules: they are machine parts, not building materials. */
public class CoreRules{

    /** Must run during loadContent, after vanilla content exists. Cores from other mods are left alone. */
    public static void patchVanillaCores(){
        for(Block b : content.blocks()){
            if(!(b instanceof CoreBlock core) || b.minfo.mod != null) continue;
            core.buildType = () -> core.new CoreBuild(){
                @Override
                public boolean acceptItem(Building source, Item item){
                    return !(item instanceof AugmentModule) && super.acceptItem(source, item);
                }

                @Override
                public int acceptStack(Item item, int amount, Teamc source){
                    return item instanceof AugmentModule ? 0 : super.acceptStack(item, amount, source);
                }
            };
        }
    }
}
