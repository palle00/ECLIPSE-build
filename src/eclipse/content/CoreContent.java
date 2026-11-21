package eclipse.content;

import arc.graphics.*;
import eclipse.world.blocks.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.entities.pattern.*;
import mindustry.type.*;
import mindustry.type.weapons.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;

import static eclipse.content.EclipseItems.*;
import static mindustry.type.ItemStack.*;

/**
 * Two core upgrades after vanilla's Nucleus, each placed over the previous core like vanilla's Shard -> Foundation ->
 * Nucleus: the Bastion (6x6) and the Citadel (7x7). More storage, a higher unit cap, more health and armour, a built-in
 * power node ({@link EclipseCore}) and a better core unit. The units are Gamma's successors (same model, recoloured):
 * faster builders and miners with a repair beam that fixes damaged blocks around them by itself. Delta fires 2 more
 * bullets than Gamma and they home in; Epsilon fires 2 more again, and its bullets explode on impact.
 * Stats are set relative to the vanilla Nucleus and Gamma, so they keep their place if vanilla is rebalanced.
 */
public class CoreContent{
    public static UnitType delta, epsilon;
    public static Block coreBastion, coreCitadel;

    public static void load(){
        delta = coreUnit("delta", Color.valueOf("ff9a3c"), 1.8f, 1.4f, 1, 40, 2, false, 0.5f, 0.001f);
        epsilon = coreUnit("epsilon", Color.valueOf("b37aff"), 2.8f, 1.9f, 2, 90, 4, true, 0.9f, 0.002f);
        //their sprites are bigger than Gamma's: the engine glow sits at their tails
        delta.engineOffset = 8f;
        epsilon.engineOffset = 8.5f;

        CoreBlock nucleus = (CoreBlock)Blocks.coreNucleus;

        coreBastion = new EclipseCore("core-bastion"){{
            requirements(Category.effect, with(Items.copper, 12000, Items.lead, 12000, Items.silicon, 8000, Items.thorium, 6000, superalloyFrame, 400, controlUnit, 150));
            size = 6;
            health = nucleus.health * 2;
            armor = nucleus.armor + 4f;
            itemCapacity = 30000;
            unitCapModifier = nucleus.unitCapModifier + 8;
            unitType = delta;
            thrusterLength = nucleus.thrusterLength * 1.2f;
            laserRange = 12f;
            maxNodes = 10;
        }};

        coreCitadel = new EclipseCore("core-citadel"){{
            requirements(Category.effect, with(Items.copper, 20000, Items.silicon, 12000, Items.surgeAlloy, 4000, superalloyFrame, 800, experimentalCore, 100, superconductor, 300));
            size = 7;
            health = nucleus.health * 7 / 2;
            armor = nucleus.armor + 8f;
            itemCapacity = 60000;
            unitCapModifier = nucleus.unitCapModifier + 16;
            unitType = epsilon;
            thrusterLength = nucleus.thrusterLength * 1.4f;
            laserRange = 16f;
            maxNodes = 15;
        }};
    }

    /**
     * A core unit built on Gamma: its movement, building and weapon, scaled up, recoloured, with more shots per volley,
     * stronger homing, optionally exploding bullets, and a repair beam that heals damaged blocks nearby on its own.
     */
    static UnitType coreUnit(String name, Color color, float healthScale, float buildScale, int tierUp, int extraCargo, int extraShots,
                             boolean explode, float repair, float repairFraction){
        UnitType g = UnitTypes.gamma;
        UnitType t = new UnitType(name){{
            constructor = g.constructor;
            aiController = g.aiController;
            controller = g.controller;
            isEnemy = false;
            coreUnitDock = true;
            flying = g.flying;
            lowAltitude = g.lowAltitude;
            canHeal = true;
            fogRadius = g.fogRadius;
            drag = g.drag;
            accel = g.accel;
            rotateSpeed = g.rotateSpeed;
            speed = g.speed * (1f + 0.08f * tierUp);
            health = g.health * healthScale;
            armor = g.armor + 2f * tierUp;
            hitSize = g.hitSize + tierUp;
            engineOffset = g.engineOffset;
            engineSize = g.engineSize;
            buildSpeed = g.buildSpeed * buildScale;
            mineTier = g.mineTier + tierUp;
            mineSpeed = g.mineSpeed * (1f + 0.3f * tierUp);
            mineWalls = g.mineWalls;
            mineFloor = g.mineFloor;
            itemCapacity = g.itemCapacity + extraCargo;
            healColor = color;
        }};

        for(Weapon base : g.weapons){
            Weapon w = base.copy();
            //the extra bullets are shared between the two guns of a mirrored pair
            int shots = w.shoot.shots + (w.mirror ? extraShots / 2 : extraShots);
            w.shoot = new ShootSpread(shots, 5f);
            BulletType b = w.bullet.copy();
            b.homingPower = Math.max(b.homingPower, 0.08f);
            b.homingRange = Math.max(b.homingRange, 80f);
            if(explode){
                b.splashDamage = b.damage * 1.2f;
                b.splashDamageRadius = 22f;
                b.hitEffect = Fx.blastExplosion;
                b.despawnHit = true;
            }
            w.bullet = b;
            t.weapons.add(w);
        }

        //repairs damaged blocks around the unit by itself, like a small mender that flies with the player
        t.weapons.add(new RepairBeamWeapon("repair-beam-weapon-center"){{
            x = 0f;
            y = -2f;
            shootY = 3f;
            mirror = false;
            beamWidth = 0.6f;
            repairSpeed = repair;
            fractionRepairSpeed = repairFraction;
            targetUnits = false;
            targetBuildings = true;
            autoTarget = true;
            controllable = false;
            laserColor = color;
            healColor = color;
            bullet = new BulletType(){{
                maxRange = 70f;
            }};
        }});
        return t;
    }
}
