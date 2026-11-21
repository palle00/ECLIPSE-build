package eclipse.defense;

import arc.graphics.g2d.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.payloads.*;

import static mindustry.Vars.*;

/**
 * The Payload Catapult: a late-game turret that throws whole blocks. It takes an empty shell (a container, vault,
 * liquid container, liquid tank, battery or large battery) from a payload conveyor, fills it from belts, pipes and its
 * own power, and once nothing more has gone in for 2 seconds (full, or the supply has stopped) it hurls the block at
 * the enemy. Where it lands it blows up exactly as that block does when destroyed (the game's own rules:
 * {@link Building#onDestroyed}): a container of blast compound is a huge blast, oil or other flammables start fires,
 * a charged battery throws lightning, slag and other liquids splash out as puddles.
 * The empty shells come from the Casing Fabricator (Serpulo has no Constructor): see {@link #shells()}.
 */
public class PayloadCatapult{

    /** The blocks it accepts as shells (and the Casing Fabricator builds): storage, tanks and batteries up to 3x3. */
    public static Seq<Block> shells(){
        return Seq.with(Blocks.container, Blocks.vault, Blocks.liquidContainer, Blocks.liquidTank, Blocks.battery, Blocks.batteryLarge);
    }

    public static class CatapultTurret extends Turret{
        public BulletType shellType;
        /** It fires once nothing has gone into the loaded shell for this long. */
        public float settleTime = 120f;
        /** Power per tick that goes into a battery shell. */
        public float chargeRate = 5f;

        public CatapultTurret(String name){
            super(name);
            hasItems = true;
            hasLiquids = true;
            acceptsPayload = true;
        }

        public class CatapultBuild extends TurretBuild{
            public @Nullable BuildPayload shell;
            /** The shell that was just fired, handed to its bullet right after the turret creates it. */
            @Nullable BuildPayload launching;
            float settle;

            @Override
            public boolean acceptPayload(Building source, Payload payload){
                return shell == null && launching == null && payload instanceof BuildPayload p && shells().contains(p.block());
            }

            @Override
            public void handlePayload(Building source, Payload payload){
                shell = (BuildPayload)payload;
                settle = 0f;
            }

            @Override
            public boolean acceptItem(Building source, Item item){
                return shell != null && shell.block().hasItems && shell.build.acceptItem(this, item);
            }

            @Override
            public void handleItem(Building source, Item item){
                shell.build.handleItem(this, item);
                settle = 0f;
            }

            @Override
            public boolean acceptLiquid(Building source, Liquid liquid){
                return shell != null && shell.block().hasLiquids && shell.build.acceptLiquid(this, liquid);
            }

            @Override
            public void handleLiquid(Building source, Liquid liquid, float amount){
                shell.build.handleLiquid(this, liquid, amount);
                settle = 0f;
            }

            boolean chargingShell(){
                return shell != null && shell.block().consPower != null && shell.block().consPower.buffered && shell.build.power != null && shell.build.power.status < 1f;
            }

            @Override
            public boolean shouldConsume(){
                return chargingShell() || super.shouldConsume();
            }

            @Override
            public void updateTile(){
                settle += delta();
                if(chargingShell() && efficiency > 0f){
                    shell.build.power.status = Math.min(1f, shell.build.power.status + chargeRate * delta() * efficiency / shell.block().consPower.capacity);
                    settle = 0f;
                }
                super.updateTile();
                //the shot was just created: give it the block (BulletType.create sets a bullet's data after init, so not there)
                if(launching != null){
                    Bullet shot = Groups.bullet.find(b -> b.owner == this && b.type == shellType && b.data == null);
                    if(shot != null){
                        shot.data = launching;
                        launching = null;
                    }
                }
            }

            @Override
            public boolean hasAmmo(){
                return shell != null && settle >= settleTime;
            }

            @Override
            public BulletType peekAmmo(){
                return shellType;
            }

            @Override
            public BulletType useAmmo(){
                launching = shell;
                shell = null;
                return shellType;
            }

            @Override
            public void draw(){
                super.draw();
                if(shell == null) return;
                //the loaded shell sits in the cradle and turns with the turret
                Draw.z(Layer.turret + 0.1f);
                TextureRegion icon = shell.icon();
                float s = shell.block().size * tilesize * 0.85f;
                Draw.rect(icon, x + recoilOffset.x, y + recoilOffset.y, s, s, drawrot());
            }

            @Override
            public void write(Writes write){
                super.write(write);
                Payload.write(shell, write);
                write.f(settle);
            }

            @Override
            public void read(Reads read, byte revision){
                super.read(read, revision);
                shell = Payload.read(read);
                settle = read.f();
            }
        }
    }

    /** The thrown block: it tumbles along a high arc and, where it lands, explodes as that block does when destroyed. */
    public static class PayloadShellBulletType extends ArtilleryBulletType{

        public PayloadShellBulletType(float speed, float impactDamage){
            super(speed, 0f);
            splashDamage = impactDamage;
            splashDamageRadius = 24f;
            hitEffect = Fx.none;
            despawnEffect = Fx.none;
        }

        @Override
        public void draw(Bullet b){
            if(!(b.data instanceof BuildPayload p)){
                super.draw(b);
                return;
            }
            //bigger at the top of the arc, as if closer to the camera; it tumbles as it flies
            float scale = 1f + b.fslope() * 0.7f, s = p.block().size * tilesize * scale;
            Draw.z(Layer.bullet);
            Draw.color(0f, 0f, 0f, 0.3f);
            Draw.rect(p.icon(), b.x - 6f * b.fslope(), b.y - 6f * b.fslope(), s * 0.9f, s * 0.9f, b.time * 3f);
            Draw.color();
            Draw.rect(p.icon(), b.x, b.y, s, s, b.time * 3f);
        }

        @Override
        public void despawned(Bullet b){
            super.despawned(b);
            if(!(b.data instanceof BuildPayload p)) return;
            b.data = null;
            Tile tile = world.tileWorld(b.x, b.y);
            if(tile == null) return;
            //the game's own building explosion, from what this block holds: items, liquids and stored power
            Building build = p.build;
            build.tile = tile;
            build.set(b.x, b.y);
            build.team = b.team;
            build.onDestroyed();
        }
    }
}
