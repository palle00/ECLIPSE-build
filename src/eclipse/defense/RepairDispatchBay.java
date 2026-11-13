package eclipse.defense;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * Emergency Repair Dispatch Bay: repairs ONE chosen friendly building in range, fast but from finite stock.
 * Select the bay, then tap the building to service (tap the bay again to clear). Every {@link #healthPerStock} health
 * repaired uses one {@link #stock} item. It never builds, never repairs enemy blocks and never repairs an area,
 * so it is a deliberate service, not a better mender.
 */
public class RepairDispatchBay extends Block{
    public static final Stat serviceStat = new Stat("eclipse-repair-service", StatCat.function);

    public Item stock;
    public float range = 15f * 8f;
    public float healthPerStock = 120f;
    /** Repair per second: this fraction of the target's max health, capped at {@link #maxRepairPerSecond}. */
    public float repairFraction = 0.05f, maxRepairPerSecond = 150f;
    public Color beamColor = Pal.heal;

    public RepairDispatchBay(String name){
        super(name);
        update = true;
        solid = true;
        configurable = true;
        hasItems = true;
        hasPower = true;
        itemCapacity = 40;
        saveConfig = false;
        config(Integer.class, (BayBuild b, Integer pos) -> b.target = pos);
        //copies (schematics, copy/paste) carry the target relative to the bay
        config(Point2.class, (BayBuild b, Point2 p) -> b.target = Point2.pack(b.tile.x + p.x, b.tile.y + p.y));
        configClear((BayBuild b) -> b.target = -1);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.range, range / tilesize, StatUnit.blocks);
        stats.add(serviceStat, Core.bundle.format("stat.eclipse-repair-service-value", (int)healthPerStock, stock.emoji(), stock.localizedName, (int)maxRepairPerSecond));
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, beamColor);
    }

    public class BayBuild extends Building{
        public int target = -1;
        /** Health repaired that has not been paid for with stock yet. */
        public float debt;
        public boolean repairing;

        public @Nullable Building targetBuild(){
            if(target == -1) return null;
            Building b = world.build(target);
            if(b == null || b == this || b.team != team || b instanceof ConstructBuild || !b.isValid()) return null;
            if(!within(b, range + b.block.size * tilesize / 2f)) return null;
            return b;
        }

        /** Why the bay is idle, or null if it is repairing. */
        public @Nullable String idleReason(){
            Building b = targetBuild();
            if(target == -1) return Core.bundle.get("eclipse-bay.no-target");
            if(b == null) return Core.bundle.get("eclipse-bay.invalid");
            if(b.health >= b.maxHealth) return Core.bundle.get("eclipse-bay.full");
            if(efficiency <= 0f) return Core.bundle.get("eclipse-bay.no-power");
            if(items.get(stock) <= 0 && debt <= 0f) return Core.bundle.format("eclipse-bay.no-stock", stock.localizedName);
            return null;
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return item == stock && items.get(stock) < itemCapacity;
        }

        @Override
        public void updateTile(){
            repairing = false;
            Building b = targetBuild();
            if(b == null || b.health >= b.maxHealth || efficiency <= 0f) return;
            //pay for the next batch up front, so stock is never overdrawn
            if(debt <= 0f){
                if(items.get(stock) <= 0) return;
                items.remove(stock, 1);
                debt += healthPerStock;
            }
            float amount = Math.min(Math.min(b.maxHealth * repairFraction, maxRepairPerSecond) * efficiency * delta() / 60f, debt);
            amount = Math.min(amount, b.maxHealth - b.health);
            if(amount <= 0f) return;
            b.heal(amount);
            debt -= amount;
            repairing = true;
            if(Mathf.chanceDelta(0.05)) Fx.healBlockFull.at(b.x, b.y, b.block.size, beamColor, b.block);
        }

        @Override
        public boolean onConfigureBuildTapped(Building other){
            if(other == this){
                configure(null);
                return false;
            }
            if(other.team == team && within(other, range + other.block.size * tilesize / 2f)){
                configure(other.pos());
                return false;
            }
            return true;
        }

        @Override
        public void drawConfigure(){
            super.drawConfigure();
            Drawf.dashCircle(x, y, range, beamColor);
            Building b = targetBuild();
            if(b != null) Drawf.select(b.x, b.y, b.block.size * tilesize / 2f + 2f, beamColor);
        }

        @Override
        public void draw(){
            super.draw();
            Building b = targetBuild();
            if(repairing && b != null){
                Draw.z(Layer.effect);
                Draw.color(beamColor, 0.6f + Mathf.absin(Time.time, 4f, 0.3f));
                Lines.stroke(1.5f);
                Lines.line(x, y, b.x, b.y);
                Draw.reset();
            }
        }

        @Override
        public void buildConfiguration(Table table){
            table.background(Styles.black6);
            table.margin(6f);
            table.label(() -> {
                Building b = targetBuild();
                String head = b == null ? "" : b.block.localizedName + "  " + (int)(b.health / b.maxHealth * 100) + "%\n";
                String reason = idleReason();
                return head + (reason == null ? Core.bundle.get("eclipse-bay.repairing") : "[lightgray]" + reason) + "\n[lightgray]" + Core.bundle.get("eclipse-bay.hint");
            }).minWidth(240f);
        }

        @Override
        public Object config(){
            return target < 0 ? null : Point2.unpack(target).sub(tile.x, tile.y);
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.i(target);
            write.f(debt);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1){
                target = read.i();
                debt = read.f();
            }
        }
    }
}
