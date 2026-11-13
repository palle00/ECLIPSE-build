package eclipse.defense;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * Orbital Weapons Platform (military M5). Tap it, then tap any spot within range to set a target.
 * It charges for a long time on power, then spends its strike charge (items) and calls down one strike:
 * a visible warning at the target, and after a few seconds heavy damage to enemy units and buildings in a large radius.
 * Bounded on purpose: one strike per charge, long charge time, items per strike, fixed range.
 */
public class OrbitalPlatform extends Block{
    public float range = 150f * 8f, chargeTime = 60f * 120f, strikeDelay = 180f, strikeRadius = 10f * 8f, strikeDamage = 3000f;
    public Color color = Color.valueOf("ff8a3d");
    public TextureRegion dishRegion;

    public static final Effect warning = new Effect(40f, 2000f, e -> {
        Draw.color(Color.scarlet, e.fout());
        Lines.stroke(2f);
        Lines.circle(e.x, e.y, e.rotation);
        Lines.circle(e.x, e.y, e.rotation * e.fin());
        Lines.line(e.x - e.rotation, e.y, e.x + e.rotation, e.y);
        Lines.line(e.x, e.y - e.rotation, e.x, e.y + e.rotation);
    });

    public static final Effect blast = new Effect(90f, 2000f, e -> {
        Draw.color(Color.white, Color.valueOf("ff8a3d"), e.fin());
        Lines.stroke(10f * e.fout());
        Lines.circle(e.x, e.y, e.rotation * e.fin(Interp.pow3Out));
        Fill.circle(e.x, e.y, e.rotation * 0.5f * e.fout());
    });

    public OrbitalPlatform(String name){
        super(name);
        update = true;
        solid = true;
        configurable = true;
        hasPower = true;
        hasItems = true;
        itemCapacity = 100;
        group = BlockGroup.turrets;
        config(Integer.class, (OrbitalBuild b, Integer v) -> b.target = v);
        //copies (schematics, copy/paste) carry the target relative to the platform
        config(Point2.class, (OrbitalBuild b, Point2 p) -> b.target = Point2.pack(b.tile.x + p.x, b.tile.y + p.y));
        configClear((OrbitalBuild b) -> b.target = -1);
    }

    @Override
    public void load(){
        super.load();
        dishRegion = Core.atlas.find(name + "-dish");
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.range, range / 8f, StatUnit.blocks);
        stats.add(Stat.reload, chargeTime / 60f, StatUnit.seconds);
        stats.add(Stat.damage, strikeDamage);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        Drawf.dashCircle(x * 8f + offset, y * 8f + offset, range, color);
    }

    @Override
    protected TextureRegion[] icons(){
        return new TextureRegion[]{region, dishRegion};
    }

    public class OrbitalBuild extends Building{
        /** Packed tile position of the target, or -1. */
        public int target = -1;
        public float charge, aim = 90f, strikeTimer = -1f, strikeX, strikeY;

        public boolean validTarget(){
            if(target < 0) return false;
            Tile t = world.tile(target);
            return t != null && within(t.worldx(), t.worldy(), range);
        }

        public float chargeFraction(){
            return charge / chargeTime;
        }

        @Override
        public void updateTile(){
            if(validTarget()){
                Tile t = world.tile(target);
                aim = Angles.moveToward(aim, angleTo(t.worldx(), t.worldy()), delta());
            }
            if(strikeTimer >= 0f){
                strikeTimer -= delta();
                if(timer(timerDump, 30f)) warning.at(strikeX, strikeY, strikeRadius);
                if(strikeTimer < 0f){
                    Damage.damage(team, strikeX, strikeY, strikeRadius, strikeDamage, true, true, true);
                    blast.at(strikeX, strikeY, strikeRadius);
                    Effect.shake(8f, 80f, strikeX, strikeY);
                }
                return;
            }
            if(!validTarget() || efficiency <= 0f) return;
            charge += delta() * efficiency;
            if(charge >= chargeTime){
                charge = 0f;
                consume();
                Tile t = world.tile(target);
                strikeX = t.worldx();
                strikeY = t.worldy();
                strikeTimer = strikeDelay;
            }
        }

        /** Fires the charged strike now (tests): the same path as a full charge. */
        public void forceCharge(){
            charge = chargeTime;
        }

        @Override
        public boolean shouldConsume(){
            return validTarget() && strikeTimer < 0f;
        }

        @Override
        public boolean onConfigureTapped(float x, float y){
            Tile t = world.tileWorld(x, y);
            if(t == null || !within(x, y, range) || t.build == this) return false;
            configure(t.pos());
            return true;
        }

        @Override
        public boolean onConfigureBuildTapped(Building other){
            if(other == this){
                configure(-1);
                return false;
            }
            if(!within(other, range)) return true;
            configure(other.tile.pos());
            return false;
        }

        @Override
        public void drawConfigure(){
            Drawf.dashCircle(x, y, range, color);
            if(validTarget()){
                Tile t = world.tile(target);
                Drawf.dashCircle(t.worldx(), t.worldy(), strikeRadius, Color.scarlet);
                Drawf.dashLine(color, x, y, t.worldx(), t.worldy());
            }
        }

        @Override
        public void draw(){
            Draw.rect(block.region, x, y);
            Draw.rect(dishRegion, x, y, aim - 90f);
            if(charge > 0f){
                Draw.z(Layer.effect);
                Draw.color(color, chargeFraction() * 0.8f);
                Fill.circle(x + Angles.trnsx(aim, 6f), y + Angles.trnsy(aim, 6f), 3f * chargeFraction());
                Draw.reset();
            }
        }

        @Override
        public Object config(){
            return target < 0 ? null : Point2.unpack(target).sub(tile.x, tile.y);
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.i(target);
            write.f(charge);
            write.f(strikeTimer);
            write.f(strikeX);
            write.f(strikeY);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            target = read.i();
            charge = read.f();
            strikeTimer = read.f();
            strikeX = read.f();
            strikeY = read.f();
        }
    }
}
