package eclipse.type;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import eclipse.content.*;
import mindustry.content.*;
import mindustry.entities.abilities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * The Tide's Lamprey: it clamps onto an enemy building next to it (not walls; turrets first) and holds on until it is
 * killed. The building it holds is frozen (no reload, no turning, no production) and drained by {@link #drain} per second.
 * From water it leaps onto the nearest enemy turret within {@link #leapRange}: airborne for the leap, so it clears walls
 * (and only anti-air can hit it in the air), and it lands on a free tile beside the turret.
 * The leap is decided on the server (position and elevation sync to clients); the hold is recomputed from position on
 * every client, so the frozen turret looks the same everywhere.
 */
public class LatchAbility extends Ability{
    public float leapRange = 10f * tilesize, leapTime = 40f, cooldown = 60f * 8f, drain = 15f;
    public Color color = Color.valueOf("8fd8ff");

    /** Leap progress in ticks (-1 when not leaping), its start and landing point; time until the next leap. */
    protected float leapT = -1f, fromX, fromY, toX, toY, wait, zap;

    @Override
    public void update(Unit unit){
        if(leapT >= 0f){
            leapT += Time.delta;
            float f = Math.min(leapT / leapTime, 1f);
            unit.set(Mathf.lerp(fromX, toX, f), Mathf.lerp(fromY, toY, f));
            unit.vel.setZero();
            //in the air (over walls, hittable by anti-air) until it touches down
            unit.elevation = f < 1f ? Math.max(0.1f, Mathf.slope(f)) : 0f;
            if(f >= 1f){
                leapT = -1f;
                Fx.landShock.at(toX, toY);
            }
            return;
        }
        wait -= Time.delta;

        Building held = held(unit);
        if(held != null){
            unit.apply(Munitions.latched, 20f);
            unit.lookAt(held);
            held.applySlowdown(0f, 20f);
            if(!net.client()) held.damage(drain / 60f * Time.delta);
            if((zap += Time.delta) >= 24f){
                zap = 0f;
                Fx.chainLightning.at(unit.x, unit.y, 0f, color, held);
            }
            wait = cooldown;
            return;
        }

        if(net.client() || wait > 0f || unit.floorOn() == null || !unit.floorOn().isLiquid) return;
        Building turret = Geometry.findClosest(unit.x, unit.y, indexer.getEnemy(unit.team, BlockFlag.turret));
        if(turret == null || !unit.within(turret, leapRange)) return;
        Tile landing = landing(unit, turret);
        if(landing == null) return;
        fromX = unit.x;
        fromY = unit.y;
        toX = landing.worldx();
        toY = landing.worldy();
        leapT = 0f;
        Fx.ripple.at(unit.x, unit.y, unit.hitSize / 4f, unit.floorOn().mapColor);
    }

    /** The enemy building the unit is clamped onto: the nearest one touching it, turrets first; never walls. */
    public static Building held(Unit unit){
        Building best = null;
        float bestScore = Float.MAX_VALUE;
        int tx = unit.tileX(), ty = unit.tileY();
        for(int dx = -4; dx <= 4; dx++){
            for(int dy = -4; dy <= 4; dy++){
                Building b = world.build(tx + dx, ty + dy);
                if(b == null || b.team == unit.team || b.team == Team.derelict || b.block instanceof Wall) continue;
                if(!unit.within(b, b.block.size * tilesize / 2f + unit.hitSize / 2f + 4f)) continue;
                float score = unit.dst(b) - (b.block.flags.contains(BlockFlag.turret) ? 1000f : 0f);
                if(score < bestScore){
                    best = b;
                    bestScore = score;
                }
            }
        }
        return best;
    }

    /** A free tile right beside the building, the nearest one to the unit; null if it is boxed in. */
    static Tile landing(Unit unit, Building b){
        int size = b.block.size, x0 = b.tile.x - (size - 1) / 2 - 1, y0 = b.tile.y - (size - 1) / 2 - 1, x1 = x0 + size + 1, y1 = y0 + size + 1;
        Tile best = null;
        for(int x = x0; x <= x1; x++){
            for(int y = y0; y <= y1; y++){
                if(x != x0 && x != x1 && y != y0 && y != y1) continue;
                Tile t = world.tile(x, y);
                if(t == null || t.solid() || t.legSolid()) continue;
                if(best == null || unit.dst2(t.worldx(), t.worldy()) < unit.dst2(best.worldx(), best.worldy())) best = t;
            }
        }
        return best;
    }

    @Override
    public void addStats(Table t){
        super.addStats(t);
        t.add(Core.bundle.format("ability.eclipse-latch.stats", Strings.autoFixed(leapRange / tilesize, 0), Strings.autoFixed(drain, 0),
            Strings.autoFixed(cooldown / 60f, 0)));
    }

    @Override
    public String localized(){
        return Core.bundle.get("ability.eclipse-latch");
    }
}
