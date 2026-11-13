package eclipse.defense;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * Unit Shield Beacon: a finite, shared shield pool for friendly units in range. Buildings get nothing.
 * It works through the units' own shield value (the mechanism vanilla's shield-regen units use), but only tops a unit
 * up to {@link #unitCap} and records what it granted, so it never overwrites a unit's native shield, overlapping
 * beacons can't stack past the cap, and the granted part is taken back when a unit leaves the field.
 * Recharging needs power and slows while the field is under pressure. Only the server changes unit shields.
 */
public class UnitShieldBeacon extends Block{
    public static final Stat poolStat = new Stat("eclipse-shield-pool", StatCat.function);

    public float range = 12f * 8f;
    /** Shared pool, its recharge per second at full power, and the slowdown while shields were granted recently. */
    public float poolCapacity = 1500f, rechargePerSecond = 15f, pressureRecharge = 0.3f;
    /** Most shield the beacon tops a unit up to, and how much it grants per unit per pass. */
    public float unitCap = 80f, grantPerPass = 20f;
    public float passInterval = 15f;
    public Color fieldColor = Color.valueOf("7fd8ff");

    public UnitShieldBeacon(String name){
        super(name);
        update = true;
        solid = true;
        hasPower = true;
        sync = true;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.range, range / tilesize, StatUnit.blocks);
        stats.add(poolStat, Core.bundle.format("stat.eclipse-shield-pool-value", (int)poolCapacity, (int)unitCap, (int)rechargePerSecond));
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("eclipse-pool", (BeaconBuild e) -> new Bar(
            () -> Core.bundle.format("bar.eclipse-shield-pool", (int)e.pool, e.covered),
            () -> fieldColor,
            () -> e.pool / poolCapacity));
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, fieldColor);
    }

    public class BeaconBuild extends Building{
        public float pool, timer, pressure;
        public int covered;
        /** unit id -> shield this beacon granted and the unit still has. Saved (unit ids survive saving), so it can still be taken back. */
        final IntFloatMap granted = new IntFloatMap();
        final IntSet seen = new IntSet();

        @Override
        public void updateTile(){
            pressure = Math.max(0f, pressure - delta());
            float rate = rechargePerSecond * efficiency * (pressure > 0f ? pressureRecharge : 1f);
            pool = Math.min(poolCapacity, pool + rate * delta() / 60f);

            timer += delta();
            if(timer < passInterval) return;
            timer = 0f;
            if(net.client()) return;

            seen.clear();
            covered = 0;
            Units.nearby(team, x, y, range, u -> {
                if(u.dead) return;
                seen.add(u.id);
                covered++;
                //what this beacon granted that the unit still has (damage eats shield first)
                float mine = Math.min(granted.get(u.id, 0f), u.shield);
                float add = Math.min(Math.min(unitCap - u.shield, grantPerPass), pool);
                if(add > 0.01f){
                    u.shield += add;
                    mine += add;
                    pool -= add;
                    pressure = 120f;
                }
                if(mine > 0f) granted.put(u.id, mine); else granted.remove(u.id, 0f);
            });

            //units that left the field or died lose what this beacon granted
            IntSeq gone = new IntSeq();
            for(IntFloatMap.Entry e : granted.entries()){
                if(seen.contains(e.key)) continue;
                Unit u = Groups.unit.getByID(e.key);
                if(u != null && !u.dead) u.shield = Math.max(0f, u.shield - Math.min(e.value, u.shield));
                gone.add(e.key);
            }
            for(int i = 0; i < gone.size; i++) granted.remove(gone.get(i), 0f);
        }

        @Override
        public void onRemoved(){
            //a destroyed or removed beacon takes its shields with it
            if(!net.client()){
                for(IntFloatMap.Entry e : granted.entries()){
                    Unit u = Groups.unit.getByID(e.key);
                    if(u != null && !u.dead) u.shield = Math.max(0f, u.shield - Math.min(e.value, u.shield));
                }
            }
            granted.clear();
            super.onRemoved();
        }

        @Override
        public void drawSelect(){
            super.drawSelect();
            Drawf.dashCircle(x, y, range, fieldColor);
        }

        @Override
        public void draw(){
            super.draw();
            if(efficiency > 0f && pool > 1f){
                Draw.z(Layer.shields);
                Draw.color(fieldColor, 0.08f + 0.08f * (pool / poolCapacity));
                Fill.circle(x, y, range);
                Draw.color(fieldColor, 0.5f);
                Lines.stroke(1f);
                Lines.circle(x, y, range);
                Draw.reset();
            }
        }

        @Override
        public byte version(){
            return 2;
        }

        /** Total shield currently granted (tests). */
        public float grantedTotal(){
            float sum = 0f;
            for(IntFloatMap.Entry e : granted.entries()) sum += e.value;
            return sum;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(pool);
            write.i(granted.size);
            for(IntFloatMap.Entry e : granted.entries()){
                write.i(e.key);
                write.f(e.value);
            }
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1) pool = read.f();
            granted.clear();
            if(revision >= 2){
                int n = read.i();
                for(int i = 0; i < n; i++) granted.put(read.i(), read.f());
            }
        }
    }

}
