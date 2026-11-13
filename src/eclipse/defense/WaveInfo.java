package eclipse.defense;

import arc.struct.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.type.*;

import static mindustry.Vars.*;

/**
 * Reads the next scheduled wave from the map's spawn groups, using the same formula the game uses to spawn it
 * (SpawnGroup.getSpawned(state.wave - 1) per ground or flyer spawn point). Nothing here changes the wave.
 */
public class WaveInfo{
    public static class Entry{
        public UnitType type;
        public int count;
        public float shields;
        public boolean boss;
    }

    /** Whether the map runs waves on a timer, so a next wave exists to report or delay. */
    public static boolean timedWaves(){
        return state.rules.waves && state.rules.waveTimer;
    }

    /** True in the campaign, where difficulty scales the counts; the report then shows them as approximate. */
    public static boolean approximate(){
        return state.isCampaign();
    }

    public static Seq<Entry> nextWave(){
        Seq<Entry> out = new Seq<>();
        if(!state.rules.waves) return out;
        int index = state.wave - 1;
        for(SpawnGroup group : state.rules.spawns){
            if(group.type == null) continue;
            int n = group.getSpawned(index);
            if(n <= 0) continue;
            int points = group.spawn != -1 ? 1 : group.type.flying ? spawner.countFlyerSpawns() : spawner.countGroundSpawns();
            if(points <= 0) continue;
            Entry e = out.find(x -> x.type == group.type && x.boss == (group.effect == StatusEffects.boss));
            if(e == null){
                e = new Entry();
                e.type = group.type;
                e.boss = group.effect == StatusEffects.boss;
                out.add(e);
            }
            e.count += n * points;
            e.shields = Math.max(e.shields, group.getShield(index));
        }
        out.sort(e -> -e.count);
        return out;
    }
}
