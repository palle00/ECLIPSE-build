package eclipse.world.blocks;

import arc.*;
import arc.util.*;
import arc.util.io.*;
import eclipse.augment.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.production.*;
import mindustry.world.meta.*;

/**
 * A crafter with two optional chemical-engineering mechanics:
 * <ul>
 * <li><b>Catalyst</b>: an item held in the inventory that enables the reaction and is only used up
 * once every {@link #craftsPerCatalyst} crafts (deterministic, so it stays in sync in multiplayer).</li>
 * <li><b>Pressure</b>: the reactor must pressurize to {@link #pressureRequired} using power before it
 * produces anything, and slowly loses pressure while it is idle.</li>
 * </ul>
 */
public class ChemicalReactor extends GenericCrafter{
    public static final Stat
        catalystStat = new Stat("eclipse-catalyst", StatCat.crafting),
        pressureStat = new Stat("eclipse-pressure", StatCat.crafting);

    public @Nullable Item catalyst;
    public int craftsPerCatalyst = 10;
    /** If true, the reactor also runs without the catalyst; with it, it runs {@link #catalystBoost} times faster. */
    public boolean catalystOptional = false;
    public float catalystBoost = 1f;

    /** Operating pressure as a fraction of maximum; 0 disables the mechanic. */
    public float pressureRequired = 0f;
    /** Pressure gained per tick at full power / lost per tick while idle. */
    public float pressurizeSpeed = 1f / 300f, depressurizeSpeed = 1f / 600f;

    public ChemicalReactor(String name){
        super(name);
    }

    @Override
    public void init(){
        if(catalyst != null) hasItems = true;
        super.init();
    }

    @Override
    public void setStats(){
        super.setStats();
        if(catalyst != null){
            String extra = catalystOptional ? " " + Core.bundle.format("stat.eclipse-catalyst-optional", Strings.autoFixed(catalystBoost, 1)) : "";
            stats.add(catalystStat, StatValues.string("@ @ [lightgray]" + Core.bundle.format("stat.eclipse-catalyst-life", craftsPerCatalyst) + extra,
                catalyst.emoji(), catalyst.localizedName));
        }
        if(pressureRequired > 0f){
            stats.add(pressureStat, StatValues.string(Core.bundle.format("stat.eclipse-pressure-value",
                (int)(pressureRequired * 100), Strings.autoFixed(pressureRequired / pressurizeSpeed / 60f, 1))));
        }
    }

    @Override
    public void setBars(){
        super.setBars();
        if(pressureRequired > 0f){
            addBar("eclipse-pressure", (ReactorBuild e) -> new Bar(
                () -> Core.bundle.format("bar.eclipse-pressure", (int)(e.pressure * 100)),
                () -> e.pressure >= pressureRequired ? Pal.accent : Pal.lightOrange,
                () -> e.pressure));
        }
        if(catalyst != null){
            addBar("eclipse-catalyst", (ReactorBuild e) -> new Bar(
                () -> catalyst.localizedName + " " + e.items.get(catalyst),
                () -> Pal.items,
                () -> e.items.has(catalyst) ? 1f - e.catalystUses / (float)craftsPerCatalyst : 0f));
        }
    }

    public class ReactorBuild extends GenericCrafterBuild{
        public float pressure;
        public int catalystUses;

        public boolean hasCatalyst(){
            return catalyst == null || items.has(catalyst);
        }

        /** Whether the reaction may run at all; an optional catalyst only changes the speed. */
        public boolean catalystSatisfied(){
            return catalystOptional || hasCatalyst();
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            if(item == catalyst) return items.get(item) < itemCapacity;
            return super.acceptItem(source, item);
        }

        @Override
        public boolean shouldConsume(){
            return super.shouldConsume() && catalystSatisfied();
        }

        @Override
        public void updateTile(){
            if(pressureRequired > 0f){
                float powered = power == null ? 1f : power.status;
                //Pressure Stabilizer modules: faster pressurizing, slower pressure loss
                float stabilizer = 1f + Augmentation.effects(this).pressure;
                if(enabled && shouldConsume() && powered > 0f){
                    pressure = Math.min(pressure + pressurizeSpeed * stabilizer * powered * delta(), 1f);
                }else{
                    pressure = Math.max(pressure - depressurizeSpeed / stabilizer * delta(), 0f);
                }
            }
            super.updateTile();
        }

        @Override
        public float efficiencyScale(){
            if(pressure < pressureRequired) return 0f;
            float boost = catalyst != null && catalystOptional && items.has(catalyst) ? catalystBoost : 1f;
            return super.efficiencyScale() * boost;
        }

        @Override
        public void craft(){
            super.craft();
            //Catalyst Optimizer modules: more crafts per catalyst
            int life = Math.round(craftsPerCatalyst * (1f + Augmentation.effects(this).catalystLife));
            if(catalyst != null && items.has(catalyst) && ++catalystUses >= life){
                catalystUses = 0;
                items.remove(catalyst, 1);
            }
        }

        /** Waste Recovery modules: part of each batch of waste is turned back into the acid this reactor consumes. */
        @Override
        public void handleLiquid(Building source, Liquid liquid, float amount){
            if(source == this && AugmentationCoupler.wasteLiquids.contains(liquid)){
                float fraction = Augmentation.effects(this).wasteRecovery;
                Liquid acid = fraction > 0f ? AugmentationCoupler.consumedAcid(block) : null;
                if(acid != null){
                    float recovered = Math.min(amount * fraction, liquidCapacity - liquids.get(acid));
                    if(recovered > 0f){
                        liquids.add(acid, recovered);
                        amount -= recovered;
                    }
                }
            }
            super.handleLiquid(source, liquid, amount);
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(pressure);
            write.i(catalystUses);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1){
                pressure = read.f();
                catalystUses = read.i();
            }
        }
    }
}
