package eclipse.logistics;

import arc.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

/**
 * Liquid control buildings for existing pipes; liquid enters from the back. Uses Mindustry's normal liquid transfer,
 * no pressure simulation.
 * <ul>
 * <li><b>Overflow:</b> everything to the front; only what the front can't take goes left, then right.</li>
 * <li><b>Balancer:</b> divides the flow evenly across the open outputs (left, front, right).</li>
 * <li><b>Reserve:</b> only draws from the tank behind it while that tank is above a configured fill level
 * (Integer config, percent), keeping a reserve in the tank.</li>
 * </ul>
 */
public class LiquidValve extends Block{
    public enum Mode{ overflow, balancer, reserve }

    public Mode mode = Mode.overflow;
    public static final int[] reserveSteps = {0, 25, 50, 75, 90};

    public LiquidValve(String name){
        super(name);
        update = true;
        solid = false;
        underBullets = true;
        rotate = true;
        hasLiquids = true;
        liquidCapacity = 20f;
        outputsLiquid = true;
        group = BlockGroup.transportation;
        envEnabled = Env.any;
        config(Integer.class, (ValveBuild b, Integer percent) -> b.reservePercent = Math.max(0, Math.min(percent, 95)));
        configClear((ValveBuild b) -> b.reservePercent = 50);
    }

    @Override
    public void init(){
        configurable = mode == Mode.reserve;
        saveConfig = configurable;
        super.init();
    }

    public class ValveBuild extends Building{
        public int reservePercent = 50;
        final Seq<Building> open = new Seq<>();

        Building side(int rel){
            return nearby(Mathf.mod(rotation + rel, 4));
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            if(source == null || source.team != team || relativeTo(source) != Mathf.mod(rotation + 2, 4)) return false;
            if(liquids.currentAmount() > 0.2f && liquids.current() != liquid) return false;
            if(liquids.get(liquid) >= liquidCapacity) return false;
            if(mode == Mode.reserve){
                //only draw while the tank behind stays above its reserve
                if(source.liquids == null || source.block.liquidCapacity <= 0f) return false;
                return source.liquids.get(liquid) / source.block.liquidCapacity > reservePercent / 100f;
            }
            return true;
        }

        @Override
        public void updateTile(){
            Liquid liquid = liquids.current();
            if(liquids.currentAmount() <= 0.0001f) return;
            switch(mode){
                case overflow -> {
                    for(int rel : new int[]{0, 1, 3}){
                        Building b = side(rel);
                        if(b != null && b.team == team) moveLiquid(b, liquid);
                        if(liquids.currentAmount() <= 0.0001f) break;
                    }
                }
                case balancer -> {
                    open.clear();
                    for(int rel : new int[]{1, 0, 3}){
                        Building b = side(rel);
                        if(b != null && b.team == team && b.acceptLiquid(this, liquid)) open.add(b);
                    }
                    if(open.isEmpty()) return;
                    float share = liquids.currentAmount() / open.size;
                    for(Building b : open){
                        float room = b.block.liquidCapacity - b.liquids.get(liquid);
                        float amount = Math.min(share, room);
                        if(amount > 0f) transferLiquid(b, amount, liquid);
                    }
                }
                case reserve -> {
                    Building front = side(0);
                    if(front != null && front.team == team) moveLiquid(front, liquid);
                }
            }
        }

        @Override
        public void buildConfiguration(Table table){
            if(mode != Mode.reserve) return;
            table.background(Styles.black6);
            table.add(Core.bundle.get("eclipse-valve-reserve")).padRight(6f);
            for(int p : reserveSteps){
                table.button(p + "%", Styles.flatTogglet, () -> configure(p)).size(52f, 36f).checked(b -> reservePercent == p);
            }
        }

        @Override
        public Object config(){
            return reservePercent;
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.b(reservePercent);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1) reservePercent = read.b();
        }
    }
}
