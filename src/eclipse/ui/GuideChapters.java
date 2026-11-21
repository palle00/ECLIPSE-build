package eclipse.ui;

import arc.*;
import arc.struct.*;
import arc.util.*;
import eclipse.content.*;
import mindustry.game.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;

/**
 * Plain data behind the wiki's guides and unit pages, kept out of the UI so the tests can check it:
 * - guide texts split into chapters at lines starting with "## " (the text before the first one is the first chapter);
 * - each paragraph's kind: a callout ("!tip Title|text", "!warn …", "!next …"), a heading line, or plain text;
 * - the Getting Started checklists, which tick themselves from the player's base;
 * - when each enemy unit comes, read from the faction wave lists.
 */
public class GuideChapters{
    public static class Chapter{
        public String title, body;
    }

    /** Checklist rows per guide and chapter (block names without "eclipse-"; vanilla names have no prefix). */
    public static final ObjectMap<String, String[][]> checks = ObjectMap.of(
        "start", new String[][]{
            {},
            {"gear-press", "extruder", "rivet", "flow-meter"},
            {"metal-press", "lathe", "assembler", "component-depot", "water-treatment-plant"},
            {"ore-crusher", "electric-heater", "blast-furnace", "vacuum-freezer", "augmentation-coupler"},
            {"fission-reactor", "steam-turbine", "robot-dispatch-hub", "lattice-drill"},
            {}
        });

    public static Seq<Chapter> parse(String title, String text){
        Seq<Chapter> out = new Seq<>();
        Chapter cur = new Chapter();
        cur.title = title;
        StringBuilder body = new StringBuilder();
        for(String line : text.split("\n", -1)){
            if(line.startsWith("## ")){
                cur.body = body.toString().trim();
                if(!cur.body.isEmpty() || out.any()) out.add(cur);
                cur = new Chapter();
                cur.title = line.substring(3).trim();
                body.setLength(0);
                continue;
            }
            body.append(line).append('\n');
        }
        cur.body = body.toString().trim();
        out.add(cur);
        return out;
    }

    /** A paragraph of guide or page text: 0 plain, 1 tip, 2 warning, 3 what comes next. */
    public static int calloutKind(String paragraph){
        if(paragraph.startsWith("!tip ")) return 1;
        if(paragraph.startsWith("!warn ")) return 2;
        if(paragraph.startsWith("!next ")) return 3;
        return 0;
    }

    /** Title and text of a callout paragraph ("!tip Title|text"). */
    public static String[] callout(String paragraph){
        String rest = paragraph.substring(paragraph.indexOf(' ') + 1);
        int bar = rest.indexOf('|');
        return bar < 0 ? new String[]{"", rest} : new String[]{rest.substring(0, bar), rest.substring(bar + 1)};
    }

    public static @Nullable Block checkBlock(String name){
        Block b = content.block("eclipse-" + name);
        return b != null ? b : content.block(name);
    }

    /** How many of this block the player's team has (-1 outside a game). */
    public static int built(Block b){
        if(!state.isGame() || player == null) return -1;
        return player.team().data().getBuildings(b).size;
    }

    // ---- when an enemy unit comes ----

    /** "Custom games and Serpulo: from wave 40, every 20 waves (boss)" lines for a faction unit; empty for others. */
    public static Seq<String> schedule(UnitType u){
        Seq<String> out = new Seq<>();
        add(out, Core.bundle.get("eclipse-wiki.waves.everywhere"), u, Factions.waves(Factions.swarm), Factions.waves(Factions.dominion), Factions.waves(Factions.tide));
        add(out, Core.bundle.get("eclipse-wiki.waves.sea"), u, Factions.subGroups());
        add(out, EclipsePlanets.hive.localizedName, u, Factions.swarmWorldWaves());
        add(out, EclipsePlanets.ironhold.localizedName, u, Factions.dominionWorldWaves());
        add(out, EclipsePlanets.tide.localizedName, u, Factions.tideWorldWaves(), Factions.subGroups());
        return out;
    }

    @SafeVarargs
    static void add(Seq<String> out, String where, UnitType u, Seq<SpawnGroup>... lists){
        for(Seq<SpawnGroup> list : lists){
            for(SpawnGroup g : list){
                if(g.type != u) continue;
                String every = g.spacing <= 1 ? Core.bundle.get("eclipse-wiki.waves.each") : Core.bundle.format("eclipse-wiki.waves.every", g.spacing);
                out.add(Core.bundle.format("eclipse-wiki.waves.line", where, g.begin + 1, every)
                    + (g.effect == mindustry.content.StatusEffects.boss ? " " + Core.bundle.get("eclipse-wiki.waves.boss") : ""));
                return;
            }
        }
    }
}
