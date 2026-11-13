package eclipse.augment;

import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/** Dismantles unwanted modules, recovering a fraction of their recipe. */
public class ModuleRecycler extends Block{
    public float recoveryFraction = 0.5f;
    public float craftTime = 300f;
    public Effect craftEffect = Fx.pulverizeMedium;

    public ModuleRecycler(String name){
        super(name);
        update = true;
        solid = true;
        hasItems = true;
        hasPower = true;
        itemCapacity = 60;
        group = BlockGroup.transportation;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.productionTime, craftTime / 60f, StatUnit.seconds);
        stats.add(recoveredStat, StatValues.string("@%", Math.round(recoveryFraction * 100)));
    }

    public static final Stat recoveredStat = new Stat("eclipse-recycler-recovered");

    @Override
    public void setBars(){
        super.setBars();
        addBar("progress", (RecyclerBuild e) -> new Bar("bar.progress", Pal.ammo, () -> e.progress));
    }

    public class RecyclerBuild extends Building{
        public float progress;

        public @Nullable AugmentModule module(){
            for(Item item : content.items()) if(item instanceof AugmentModule m && items.has(m)) return m;
            return null;
        }

        /** Room for everything a dismantled module returns. */
        boolean hasRoom(AugmentModule m){
            for(ItemStack s : m.recipe) if(items.get(s.item) + recovered(s) > itemCapacity) return false;
            return true;
        }

        int recovered(ItemStack s){
            return Math.max(1, (int)(s.amount * recoveryFraction));
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return item instanceof AugmentModule && module() == null;
        }

        @Override
        public boolean shouldConsume(){
            AugmentModule m = module();
            return m != null && hasRoom(m) && enabled;
        }

        @Override
        public void updateTile(){
            AugmentModule m = module();
            if(m != null && efficiency > 0f){
                progress += getProgressIncrease(craftTime);
                if(progress >= 1f){
                    progress = 0f;
                    items.remove(m, 1);
                    for(ItemStack s : m.recipe) items.add(s.item, recovered(s));
                    craftEffect.at(x, y);
                }
            }
            dumpRecovered();
        }

        void dumpRecovered(){
            for(Item item : content.items()){
                if(!(item instanceof AugmentModule) && items.has(item)) dump(item);
            }
        }

        @Override
        public boolean canDump(Building to, Item item){
            return !(item instanceof AugmentModule);
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(progress);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1) progress = read.f();
        }
    }
}
