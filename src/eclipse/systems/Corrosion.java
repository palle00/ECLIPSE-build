package eclipse.systems;

import arc.*;
import arc.math.*;
import arc.util.*;
import eclipse.type.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * Corrosive liquids slowly destroy liquid blocks that are not corrosion-resistant.
 * Resistant: every ECLIPSE block (chemical equipment and polymer transport), vanilla plated conduits and the
 * sandbox-only creative blocks (liquid source, liquid void...).
 * Runs on the server / singleplayer only; damage is synced to clients by the game.
 */
public class Corrosion{
    /** Ticks between corrosion passes. */
    public static final float interval = 30f;
    /** Fraction of max health lost per second while holding a corrosive liquid. */
    public static final float damagePerSecond = 0.01f;

    static float timer;

    public static void init(){
        Events.run(Trigger.update, Corrosion::update);
    }

    public static boolean resistant(Block block){
        return block.name.startsWith("eclipse-") || block == Blocks.platedConduit || block.buildVisibility == BuildVisibility.sandboxOnly;
    }

    static void update(){
        if(!state.isPlaying() || net.client()) return;
        timer += Time.delta;
        if(timer < interval) return;
        timer = 0f;

        Groups.build.each(b -> {
            if(b.liquids == null || b.liquids.currentAmount() < 0.01f || !(b.liquids.current() instanceof CorrosiveLiquid) || resistant(b.block)) return;
            b.damage(b.maxHealth * damagePerSecond * interval / 60f);
            if(Mathf.chance(0.35)) eclipse.content.Polish.corrode.at(b.x + Mathf.range(b.block.size * 3f), b.y + Mathf.range(b.block.size * 3f), 0f, b.liquids.current().color);
        });
    }
}
