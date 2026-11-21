package eclipse.defense;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.blocks.defense.turrets.*;

/**
 * The Bombard: a siege mortar with the longest reach on Serpulo and a wide blind spot (the turret's minRange) where it
 * cannot fire, since its own shell would land on it. Its shells fly slowly in a high arc, and a marker on the ground
 * shows where each will land, so both sides see it coming.
 */
public class Bombard{

    /**
     * An item turret with a real blind spot: it never picks a target within {@link #minRange} of itself and drops one that
     * comes that close. (Vanilla minRange only pushes the landing point outward; the turret would still aim at close enemies.)
     */
    public static class BombardTurret extends ItemTurret{
        public BombardTurret(String name){
            super(name);
        }

        public class BombardTurretBuild extends ItemTurretBuild{
            boolean outside(Position p){
                return !within(p, minRange);
            }

            @Override
            protected Posc findEnemy(float range){
                BulletType ammo = peekAmmo();
                boolean blocks = targetGround && targetBlocks && (ammo == null || ammo.targetBlocks);
                return Units.bestTarget(team, x, y, range,
                    u -> !u.dead() && unitFilter.get(u) && (u.isGrounded() || targetAir) && (!u.isGrounded() || targetGround) && outside(u),
                    b -> blocks && buildingFilter.get(b) && outside(b), unitSort);
            }

            @Override
            protected boolean validateTarget(){
                //a player or logic processor aiming it by hand may pick any spot; the shell still lands at least minRange away
                return super.validateTarget() && (isControlled() || logicControlled() || target == null || outside(target));
            }
        }
    }

    /** An artillery shell that draws a shrinking target ring where it will come down. */
    public static class SiegeShellBulletType extends ArtilleryBulletType{
        public Color markerColor = Color.valueOf("ff5845");

        public SiegeShellBulletType(float speed, float splashDamage, float splashRadius){
            super(speed, 0f);
            this.splashDamage = splashDamage;
            this.splashDamageRadius = splashRadius;
        }

        @Override
        public void draw(Bullet b){
            super.draw(b);
            //where it lands: artillery keeps its speed and stops at the end of its (aim-scaled) life
            float left = Math.max(b.lifetime - b.time, 0f);
            float lx = b.x + b.vel.x * left, ly = b.y + b.vel.y * left;
            float r = splashDamageRadius * (0.35f + 0.65f * b.fout());
            Draw.z(Layer.bullet - 1f);
            Draw.color(markerColor, 0.35f + 0.45f * Mathf.absin(Time.time, 4f, 1f));
            Lines.stroke(1.6f);
            Lines.circle(lx, ly, r);
            Lines.stroke(1f);
            Lines.circle(lx, ly, splashDamageRadius);
            for(int i = 0; i < 4; i++) Lines.lineAngleCenter(lx + Angles.trnsx(i * 90f, r), ly + Angles.trnsy(i * 90f, r), i * 90f, 6f);
            Draw.reset();
        }
    }
}
