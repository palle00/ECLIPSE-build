package eclipse.systems;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.defense.turrets.Turret.*;

import static mindustry.Vars.*;

/**
 * Combat statistics for every turret, vanilla and modded: damage per second (last {@link #window} seconds), enemies
 * killed and total damage dealt to enemy units, shown as bars in the turret's info panel.
 * <p>
 * The game reports each bullet hit (direct or splash) after the damage is applied, but not the amount. So every unit's
 * health + shield is noted at the start of each update; a hit's damage is how much that dropped since the last note.
 * Fragments, mines and lightning a turret's shots release count for that turret. Counted since the map was loaded.
 */
public class TurretStats{
    public static final int window = 5;
    static final float slotTicks = 30f;
    static final int slots = (int)(window * 60f / slotTicks);

    public static class Stats{
        public float damage;
        public int kills;
        final float[] recent = new float[slots];
        long lastSlot = -1;

        void add(float amount){
            advance();
            recent[(int)(lastSlot % slots)] += amount;
            damage += amount;
        }

        /** Clears the slots that went by since the last hit. */
        void advance(){
            long now = (long)(Time.time / slotTicks);
            if(lastSlot < 0) lastSlot = now;
            for(long s = lastSlot + 1; s <= now && s <= lastSlot + slots; s++) recent[(int)(s % slots)] = 0f;
            lastSlot = Math.max(lastSlot, now);
        }

        public float dps(){
            advance();
            float sum = 0f;
            for(float v : recent) sum += v;
            return sum / window;
        }
    }

    static final ObjectMap<Building, Stats> stats = new ObjectMap<>();
    /** Each unit's health + shield, noted at the start of the update and after every hit. */
    static final IntFloatMap hp = new IntFloatMap();

    /** Call after anything else that adds damage on hits (Augmentation), so that damage is counted too. */
    public static void init(){
        Events.on(WorldLoadEvent.class, e -> {
            stats.clear();
            hp.clear();
        });
        Events.run(Trigger.update, () -> {
            hp.clear();
            if(state.isPlaying()) Groups.unit.each(u -> hp.put(u.id, life(u)));
        });
        Events.on(UnitDamageEvent.class, e -> {
            float now = life(e.unit), before = hp.get(e.unit.id, now);
            hp.put(e.unit.id, now);
            Stats s = of(e.bullet);
            if(s != null && before > now) s.add(before - now);
        });
        Events.on(UnitBulletDestroyEvent.class, e -> {
            Stats s = of(e.bullet);
            if(s != null) s.kills++;
        });
        Events.on(ClientLoadEvent.class, e -> addBars());
    }

    static float life(Unit u){
        return Math.max(u.health, 0f) + Math.max(u.shield, 0f);
    }

    static @Nullable Stats of(@Nullable Bullet b){
        if(b == null || !(b.owner instanceof TurretBuild t)) return null;
        return stats.get(t, Stats::new);
    }

    /** A turret's statistics (empty if it has not hit anything yet). */
    public static Stats get(Building turret){
        Stats s = stats.get(turret);
        return s == null ? new Stats() : s;
    }

    /** DPS, kills and damage bars on every turret's info panel. */
    public static void addBars(){
        for(Block block : content.blocks()){
            if(!(block instanceof Turret turret)) continue;
            turret.addBar("eclipse-dps", (TurretBuild b) -> new Bar(
                () -> Core.bundle.format("bar.eclipse-turret-dps", UI.formatAmount((long)get(b).dps())), () -> Pal.remove, () -> get(b).dps() > 0f ? 1f : 0f));
            turret.addBar("eclipse-kills", (TurretBuild b) -> new Bar(
                () -> Core.bundle.format("bar.eclipse-turret-kills", get(b).kills), () -> Pal.accent, () -> 0f));
            turret.addBar("eclipse-damage", (TurretBuild b) -> new Bar(
                () -> Core.bundle.format("bar.eclipse-turret-damage", UI.formatAmount((long)get(b).damage)), () -> Pal.accent, () -> 0f));
        }
    }
}
