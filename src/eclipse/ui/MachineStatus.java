package eclipse.ui;

import arc.*;
import arc.graphics.*;
import arc.util.*;
import eclipse.world.blocks.ChemicalReactor.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.production.GenericCrafter.*;
import mindustry.world.blocks.production.HeatCrafter.*;
import mindustry.world.consumers.*;

import static mindustry.Vars.*;

/**
 * Why an ECLIPSE machine is not working, in words: a status bar on every ECLIPSE crafter, e.g. "Stopped: Acidic Waste has
 * nowhere to go" or "No heat (needs 10)". Vanilla only shows a coloured dot, which is not enough for chemistry that can
 * stall on a full waste output, missing heat, a missing catalyst or pressure.
 */
public class MachineStatus{
    public enum Kind{working, disabled, outputFull, noPower, missing, noCatalyst, noHeat, pressurizing, lowHeat}

    public static class Status{
        public Kind kind = Kind.working;
        public @Nullable UnlockableContent content;
        /** Heat required (noHeat), or a fraction 0-1 (lowHeat speed, pressurizing progress). */
        public float value;

        Status set(Kind kind, @Nullable UnlockableContent content, float value){
            this.kind = kind;
            this.content = content;
            this.value = value;
            return this;
        }

        public boolean problem(){
            return kind != Kind.working && kind != Kind.lowHeat && kind != Kind.pressurizing;
        }

        public String text(){
            String name = content == null ? "" : content.localizedName;
            return switch(kind){
                case working -> Core.bundle.get("eclipse-status.working");
                case disabled -> Core.bundle.get("eclipse-status.disabled");
                case outputFull -> Core.bundle.format("eclipse-status.output-full", name);
                case noPower -> Core.bundle.get("eclipse-status.no-power");
                case missing -> content == null ? Core.bundle.get("eclipse-status.missing-fuel") : Core.bundle.format("eclipse-status.missing", name);
                case noCatalyst -> Core.bundle.format("eclipse-status.no-catalyst", name);
                case noHeat -> Core.bundle.format("eclipse-status.no-heat", Strings.autoFixed(value, 1));
                case pressurizing -> Core.bundle.format("eclipse-status.pressurizing", Math.round(value * 100));
                case lowHeat -> Core.bundle.format("eclipse-status.low-heat", Math.round(value * 100));
            };
        }

        public Color color(){
            return kind == Kind.working ? Pal.heal : problem() ? Pal.remove : Pal.accent;
        }
    }

    /** What is holding a machine back, checked in the order a player should fix it. */
    public static Status of(Building b){
        Status s = new Status();
        if(!b.enabled) return s.set(Kind.disabled, null, 0f);

        //outputs that have nowhere to go stop the machine
        if(b.block instanceof GenericCrafter g && b instanceof GenericCrafterBuild){
            if(g.outputItems != null){
                for(ItemStack out : g.outputItems) if(b.items.get(out.item) + out.amount > g.itemCapacity) return s.set(Kind.outputFull, out.item, 0f);
            }
            if(g.outputLiquids != null && !g.ignoreLiquidFullness){
                LiquidStack full = null;
                boolean allFull = true;
                for(LiquidStack out : g.outputLiquids){
                    if(b.liquids.get(out.liquid) >= g.liquidCapacity - 0.001f){
                        if(full == null) full = out;
                    }else allFull = false;
                }
                //a crafter that dumps extra liquid only stops when every output is full
                if(full != null && (!g.dumpExtraLiquid || allFull)) return s.set(Kind.outputFull, full.liquid, 0f);
            }
        }

        //required inputs
        for(Consume c : b.block.consumers){
            if(c.optional || c.efficiency(b) > 0.0001f) continue;
            if(c instanceof ConsumePower) return s.set(Kind.noPower, null, 0f);
            return s.set(Kind.missing, missingOf(b, c), 0f);
        }

        if(b instanceof ReactorBuild r){
            eclipse.world.blocks.ChemicalReactor block = (eclipse.world.blocks.ChemicalReactor)b.block;
            if(!r.catalystSatisfied()) return s.set(Kind.noCatalyst, block.catalyst, 0f);
            if(block.pressureRequired > 0f && r.pressure < block.pressureRequired) return s.set(Kind.pressurizing, null, r.pressure / block.pressureRequired);
        }
        if(b instanceof HeatCrafterBuild h){
            float need = h.heatRequirement();
            if(need > 0f && h.heat <= 0.001f) return s.set(Kind.noHeat, null, need);
            if(need > 0f && h.heat < need) return s.set(Kind.lowHeat, null, h.heat / need);
        }
        return s;
    }

    /** The input a consumer is waiting for (null for "any fuel"). */
    static @Nullable UnlockableContent missingOf(Building b, Consume c){
        if(c instanceof ConsumeItems ci){
            for(ItemStack st : ci.items) if(b.items.get(st.item) < st.amount) return st.item;
        }
        if(c instanceof ConsumeLiquid cl) return cl.liquid;
        if(c instanceof ConsumeLiquids cls){
            for(LiquidStack st : cls.liquids) if(b.liquids.get(st.liquid) < st.amount) return st.liquid;
            return cls.liquids.length > 0 ? cls.liquids[0].liquid : null;
        }
        return null;
    }

    /** A status bar on every ECLIPSE crafter. Call on ClientLoadEvent. */
    public static void install(){
        Events.on(ClientLoadEvent.class, e -> {
            for(Block block : content.blocks()){
                if(!(block instanceof GenericCrafter) || !block.name.startsWith("eclipse-")) continue;
                block.addBar("eclipse-status", (GenericCrafterBuild b) -> {
                    Status[] last = {of(b)};
                    return new Bar(() -> (last[0] = of(b)).text(), () -> last[0].color(), () -> 1f);
                });
            }
        });
    }
}
