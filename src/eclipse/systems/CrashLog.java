package eclipse.systems;

import arc.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import eclipse.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.Mods.*;
import mindustry.world.*;

import java.util.*;

import static mindustry.Vars.*;

/**
 * ECLIPSE's own lines in the game log (saves\last_log.txt), so a crash can be traced back to what ECLIPSE was doing:
 * <ul>
 * <li>Startup: ECLIPSE and game versions, and how much content ECLIPSE loaded.</li>
 * <li>Each world load: map, mode, wave and how many ECLIPSE buildings it has.</li>
 * <li>A trail of the last {@link #trailSize} ECLIPSE events (world loads, waves, ECLIPSE blocks built, destroyed or
 * configured), kept in memory only.</li>
 * <li>Crash: the game shuts down without its normal exit. ECLIPSE then writes a context block (game state, ECLIPSE
 * buildings and units, the trail) to the log, right before Mindustry logs the error and writes its crash report.</li>
 * </ul>
 * It never changes the game, and every part is guarded so logging can neither cause a crash nor hide the real one.
 */
public class CrashLog{
    public static final int trailSize = 40;

    /** One trail entry. Preallocated and reused, so processors configuring blocks every tick make no garbage. */
    static class Entry{
        long millis;
        double tick;
        String what;
        @Nullable Block block;
        int x, y;
        @Nullable Object value;
    }

    static final Entry[] trail = new Entry[trailSize];
    static int next, count;
    static String phase = "starting";
    static boolean exiting;

    static{
        for(int i = 0; i < trailSize; i++) trail[i] = new Entry();
    }

    /** Call from the mod constructor, before content loads, so a crash while loading is covered too. */
    public static void init(){
        //first listener, so it runs before the game's own shutdown while the world and the log are still intact.
        //A normal quit calls exit() on every listener before dispose(); a crash goes straight to dispose().
        Core.app.getListeners().insert(0, new ApplicationListener(){
            @Override
            public void exit(){
                exiting = true;
            }

            @Override
            public void dispose(){
                if(!exiting) Log.err(crashContext());
            }
        });

        Events.on(ClientLoadEvent.class, e -> phase = "running");
        Events.on(WorldLoadEndEvent.class, e -> guard("world summary", () -> {
            Log.info("[ECLIPSE] world loaded: @", worldSummary());
            note("world loaded", null, -1, -1, state.map == null ? null : state.map.name());
        }));
        Events.on(WaveEvent.class, e -> note("wave", null, -1, -1, state.wave));
        Events.on(BlockBuildEndEvent.class, e -> {
            if(!e.breaking && e.tile != null && isEclipse(e.tile.block())) note("built", e.tile.block(), e.tile.x, e.tile.y, e.config);
        });
        Events.on(BlockDestroyEvent.class, e -> {
            if(e.tile != null && isEclipse(e.tile.block())) note("destroyed", e.tile.block(), e.tile.x, e.tile.y, null);
        });
        Events.on(ConfigEvent.class, e -> {
            if(e.tile != null && isEclipse(e.tile.block)) note("configured", e.tile.block, e.tile.tileX(), e.tile.tileY(), e.value);
        });
    }

    public static void contentLoading(){
        phase = "loading content";
    }

    /** Logs what ECLIPSE added, once its content has loaded. */
    public static void contentLoaded(long startMillis){
        phase = "content loaded";
        guard("startup summary", () -> {
            int[] n = new int[4];
            ContentType[] types = {ContentType.item, ContentType.liquid, ContentType.block, ContentType.unit};
            for(int i = 0; i < types.length; i++){
                for(Content c : content.getBy(types[i])) if(c instanceof UnlockableContent u && u.name.startsWith("eclipse-")) n[i]++;
            }
            Log.info("[ECLIPSE] @ on Mindustry @: loaded @ items, @ liquids, @ blocks, @ units in @ ms",
                version(), Version.combined(), n[0], n[1], n[2], n[3], Time.timeSinceMillis(startMillis));
        });
    }

    /** Adds an event to the trail (overwriting the oldest). */
    static void note(String what, @Nullable Block block, int x, int y, @Nullable Object value){
        Entry e = trail[next];
        e.millis = System.currentTimeMillis();
        e.tick = state == null ? 0 : state.tick;
        e.what = what;
        e.block = block;
        e.x = x;
        e.y = y;
        e.value = value;
        next = (next + 1) % trailSize;
        count = Math.min(count + 1, trailSize);
    }

    static boolean isEclipse(@Nullable Block block){
        return block != null && block.name.startsWith("eclipse-");
    }

    /** The block written to the log on a crash; public so the headless test can check it. */
    public static String crashContext(){
        StringBuilder s = new StringBuilder("[ECLIPSE] ===== crash context (Mindustry's own error report follows) =====");
        section(s, "versions", () -> "ECLIPSE " + version() + " on Mindustry " + Version.combined() + ", Java " + System.getProperty("java.version")
            + ", " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        section(s, "phase", CrashLog::phase);
        if(state != null && !state.isMenu() && world != null && world.tiles != null){
            section(s, "world", CrashLog::worldSummary);
            section(s, "player", () -> player == null || player.unit() == null ? "none"
                : "team " + player.team().name + ", " + player.unit().type.name + " at tile (" + player.tileX() + ", " + player.tileY() + ")");
            section(s, "ECLIPSE buildings", () -> top(eclipseBuildings()));
            section(s, "ECLIPSE units", () -> {
                ObjectIntMap<String> units = new ObjectIntMap<>();
                Groups.unit.each(u -> {
                    if(u.type.name.startsWith("eclipse-")) units.increment(u.type.name);
                });
                return top(units) + " (all units: " + Groups.unit.size() + ", bullets: " + Groups.bullet.size() + ")";
            });
            section(s, "rule tags", () -> {
                StringBuilder t = new StringBuilder();
                for(ObjectMap.Entry<String, String> e : state.rules.tags) if(e.key.startsWith("eclipse-")) t.append(t.length() == 0 ? "" : ", ").append(e.key).append('=').append(e.value);
                return t.length() == 0 ? "none" : t.toString();
            });
        }
        s.append("\n  recent ECLIPSE events, oldest first:");
        if(count == 0) s.append("\n    none");
        for(int i = 0; i < count; i++){
            Entry e = trail[(next - count + i + trailSize) % trailSize];
            s.append("\n    ");
            try{
                s.append(new java.text.SimpleDateFormat("HH:mm:ss").format(new Date(e.millis))).append(" tick ").append((long)e.tick).append(": ").append(e.what);
                if(e.block != null) s.append(' ').append(e.block.name).append(" at (").append(e.x).append(", ").append(e.y).append(')');
                if(e.value != null) s.append(" -> ").append(describe(e.value));
            }catch(Throwable t){
                s.append("(unreadable: ").append(t).append(')');
            }
        }
        s.append("\n[ECLIPSE] ===== end of crash context =====");
        return s.toString();
    }

    static String phase(){
        if(!phase.equals("running")) return phase;
        if(state.isMenu()) return "main menu";
        if(state.isEditor()) return "map editor";
        return state.isPaused() ? "in game (paused)" : "in game";
    }

    static String worldSummary(){
        int all = Groups.build.size(), ours = 0;
        for(Building b : Groups.build) if(isEclipse(b.block)) ours++;
        return "map '" + (state.map == null ? "?" : state.map.name()) + "' " + world.width() + "x" + world.height()
            + ", mode " + state.rules.mode().name()
            + (state.rules.sector == null ? "" : ", sector " + state.rules.sector.name())
            + (state.rules.planet == null ? "" : ", planet " + state.rules.planet.name)
            + (state.rules.waves ? ", wave " + state.wave : ", no waves")
            + ", tick " + (long)state.tick
            + ", " + (net.client() ? "multiplayer client" : net.server() ? "multiplayer host" : "singleplayer")
            + ", ECLIPSE buildings " + ours + " of " + all;
    }

    static ObjectIntMap<String> eclipseBuildings(){
        ObjectIntMap<String> out = new ObjectIntMap<>();
        for(Building b : Groups.build) if(isEclipse(b.block)) out.increment(b.block.name);
        return out;
    }

    /** "name x count" for the most common entries. */
    static String top(ObjectIntMap<String> counts){
        if(counts.size == 0) return "none";
        Seq<ObjectIntMap.Entry<String>> list = new Seq<>();
        for(ObjectIntMap.Entry<String> e : counts){
            ObjectIntMap.Entry<String> copy = new ObjectIntMap.Entry<>();
            copy.key = e.key;
            copy.value = e.value;
            list.add(copy);
        }
        list.sort(e -> -e.value);
        StringBuilder s = new StringBuilder();
        for(int i = 0; i < Math.min(list.size, 15); i++) s.append(i == 0 ? "" : ", ").append(list.get(i).key.substring("eclipse-".length())).append(" x").append(list.get(i).value);
        if(list.size > 15) s.append(", +").append(list.size - 15).append(" more kinds");
        return s.toString();
    }

    static String describe(Object value){
        if(value instanceof UnlockableContent c) return c.name;
        if(value instanceof Object[] a) return Arrays.toString(a);
        if(value instanceof int[] a) return Arrays.toString(a);
        return String.valueOf(value);
    }

    static String version(){
        LoadedMod mod = mods == null ? null : mods.getMod(EclipseMod.class);
        return mod == null ? "(version unknown)" : mod.meta.version;
    }

    /** One line of the crash context; a part that fails is reported in place, so the rest is still written. */
    static void section(StringBuilder s, String name, Prov<String> line){
        s.append("\n  ").append(name).append(": ");
        try{
            s.append(line.get());
        }catch(Throwable t){
            s.append("(could not read: ").append(t).append(')');
        }
    }

    /** An install step that may throw anything, including linkage errors when a game update changed what it calls. */
    public interface Install{
        void run() throws Throwable;
    }

    /**
     * Installs an optional extra (search box, live map...). These hook into vanilla UI internals that a game update can
     * change; if that happened, the extra stays off and the log says why, instead of the game crashing at startup.
     * Returns whether it installed.
     */
    public static boolean optional(String feature, Install install){
        try{
            install.run();
            return true;
        }catch(Throwable t){
            Log.err("[ECLIPSE] @ is turned off: it does not fit this game version (@). The rest of ECLIPSE works normally.\n@",
                feature, Strings.getSimpleMessage(t), Strings.getStackTrace(t));
            note("feature off: " + feature, null, -1, -1, Strings.getSimpleMessage(t));
            return false;
        }
    }

    /** Runs a logging step; if it fails, says so in the log instead of crashing the game. */
    static void guard(String what, Runnable r){
        try{
            r.run();
        }catch(Throwable t){
            Log.err("[ECLIPSE] could not write the " + what + ": @", Strings.getSimpleMessage(t));
        }
    }
}
