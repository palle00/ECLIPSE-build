package eclipse.ui;

import arc.*;
import arc.struct.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.*;

/**
 * Every ECLIPSE setting in one place (user, 2026-11-25: "add all the settings the mod needs"): defined once here, shown on
 * the ECLIPSE tab of {@link EclipseSettingsDialog} (opened from the pause menu) and, for the main menu and phones, as an
 * ECLIPSE section in vanilla's settings. The four older settings keep their keys, so players keep their choices.
 */
public class EclipseSettings{
    public enum Kind{toggle, percent}

    public static class Setting{
        public final String key, section;
        public final Kind kind;
        public final int def;

        Setting(String key, String section, Kind kind, int def){
            this.key = key;
            this.section = section;
            this.kind = kind;
            this.def = def;
        }

        public String name(){
            return Core.bundle.get("setting." + key + ".name");
        }

        public String description(){
            return Core.bundle.get("setting." + key + ".description");
        }

        public boolean on(){
            return Core.settings == null ? def != 0 : Core.settings.getBool(key, def != 0);
        }

        /** 0-1 for a percent setting. */
        public float fraction(){
            return (Core.settings == null ? def : Core.settings.getInt(key, def)) / 100f;
        }

        public void reset(){
            if(kind == Kind.toggle) Core.settings.put(key, def != 0);
            else Core.settings.put(key, def);
        }
    }

    /** Sections in the order they are shown; each has a bundle key eclipse-settings.section.NAME. */
    public static final String[] sections = {"interface", "notifications", "visuals", "sound"};
    public static final Seq<Setting> all = new Seq<>();

    public static final Setting
        compactCore = toggle(CompactCoreDisplay.setting, "interface"),
        machineStatus = toggle("eclipse-machine-status", "interface"),
        placementHints = toggle("eclipse-placement-hints", "interface"),
        blockSearch = toggle("eclipse-block-search", "interface"),
        liveMap = toggle("eclipse-live-map", "interface"),
        milestones = toggle("eclipse-milestone-popups", "notifications"),
        announcements = toggle("eclipse-announcements", "notifications"),
        menuBackdrop = toggle(MenuBackdrop.setting, "visuals"),
        menuIntro = toggle(MenuBackdrop.introSetting, "visuals"),
        deployIntro = toggle(DeploymentIntro.setting, "visuals"),
        darkness = percent("eclipse-darkness", "visuals"),
        terrainParticles = toggle("eclipse-terrain-particles", "visuals"),
        machineSounds = percent("eclipse-machine-sounds", "sound");

    static Setting toggle(String key, String section){
        Setting s = new Setting(key, section, Kind.toggle, 1);
        all.add(s);
        return s;
    }

    static Setting percent(String key, String section){
        Setting s = new Setting(key, section, Kind.percent, 100);
        all.add(s);
        return s;
    }

    /** Machine sounds' own volume per block before the Machine sounds setting scales it (filled by Polish). */
    public static final ObjectFloatMap<Block> baseSoundVolume = new ObjectFloatMap<>();

    /** Defaults, the vanilla ECLIPSE section, and the settings that act on content. Call on ClientLoadEvent. */
    public static void install(){
        for(Setting s : all){
            if(s.kind == Kind.toggle) Core.settings.defaults(s.key, s.def != 0);
            else Core.settings.defaults(s.key, s.def);
        }
        ui.settings.addCategory(Core.bundle.get("eclipse-settings.title"), Icon.settings, t -> {
            for(Setting s : all){
                if(s.kind == Kind.toggle) t.checkPref(s.key, s.def != 0, v -> apply());
                else t.sliderPref(s.key, s.def, 0, 100, 10, v -> {
                    apply();
                    return v + "%";
                });
            }
        });
        apply();
    }

    /** Puts settings that change content into effect: the status bar on ECLIPSE machines and the machine sound volume. */
    public static void apply(){
        MachineStatus.showBars(machineStatus.on());
        float volume = machineSounds.fraction();
        for(var e : baseSoundVolume) e.key.ambientSoundVolume = e.value * volume;
    }

    /** Everything on the ECLIPSE tab back to its default. */
    public static void resetAll(){
        for(Setting s : all) s.reset();
        apply();
    }

    /** Shows the one-time welcome again in the next game. */
    public static void showWelcomeAgain(){
        Core.settings.put(WelcomeHint.shownKey, false);
    }

    /** Forgets the wiki's pinned and recently opened pages. */
    public static void clearWikiHistory(){
        Core.settings.remove(EclipseWiki.pinsKey);
        Core.settings.remove(EclipseWiki.recentKey);
    }

    /** Whether ECLIPSE should announce faction arrivals, eclipses and the World Engine with a toast. */
    public static boolean announce(){
        return announcements.on();
    }
}
