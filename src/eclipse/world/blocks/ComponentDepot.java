package eclipse.world.blocks;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * Storage for mechanical components only. It never merges with the core, but keeps the team's core stocked
 * with up to {@link #coreReserve} of each component, from anywhere on the map. Components can live next to
 * their production lines and still be used for building.
 */
public class ComponentDepot extends StorageBlock{
    public static final Stat reserveStat = new Stat("eclipse-core-reserve", StatCat.items);

    public Seq<Item> accepted = new Seq<>();
    /** The depot tops the core up to this many of each component. */
    public int coreReserve = 200;
    /** Items of each kind sent to the core per transfer, and ticks between transfers. */
    public int transferAmount = 2;
    public float transferInterval = 10f;
    /** "-hub": the transfer hub, turning while components are sent to the core. */
    public TextureRegion hubRegion;

    public ComponentDepot(String name){
        super(name);
        coreMerge = false;
        update = true;
    }

    @Override
    public void load(){
        super.load();
        hubRegion = Core.atlas.find(name + "-hub");
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(reserveStat, Core.bundle.format("stat.eclipse-core-reserve-value", coreReserve));
    }

    public class DepotBuild extends StorageBuild{
        float timer, sinceSent = 999f, warmup, spin;

        @Override
        public boolean acceptItem(Building source, Item item){
            return accepted.contains(item) && items.get(item) < getMaximumAccepted(item);
        }

        @Override
        public void updateTile(){
            sinceSent += delta();
            warmup = Mathf.approachDelta(warmup, sinceSent < 60f ? 1f : 0f, 0.02f);
            spin += warmup * delta() * 2f;
            timer += delta();
            if(timer < transferInterval) return;
            timer = 0f;
            CoreBuild core = team.core();
            if(core == null) return;
            for(Item item : accepted){
                int n = Math.min(Math.min(items.get(item), coreReserve - core.items.get(item)), transferAmount);
                if(n <= 0) continue;
                items.remove(item, n);
                core.items.add(item, n);
                sinceSent = 0f;
                if(!headless && Mathf.chance(0.3)) Fx.itemTransfer.at(x, y, 0f, item.color, core);
            }
        }

        @Override
        public void draw(){
            super.draw();
            if(hubRegion.found()) Drawf.spinSprite(hubRegion, x, y, spin);
        }
    }
}
