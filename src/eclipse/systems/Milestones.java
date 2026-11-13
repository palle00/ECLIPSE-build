package eclipse.systems;

import arc.*;
import arc.scene.style.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.type.*;

import static mindustry.Vars.*;

/**
 * One-time messages for key firsts in a game ("First superalloy cast!"): reached the first time any building of the
 * player's team holds the material. Reached milestones are stored in the rules (tag {@link #tag}), so each save shows
 * each message once. A save that predates milestones records what it already has without messages. Sandbox and the
 * editor show none.
 */
public class Milestones{
    public static final String tag = "eclipse-milestones";
    /** Milestone ids (bundle key eclipse-milestone.&lt;id&gt;) with the material that reaches them, in progression order. */
    public static final String[][] list = {
        {"gears", "eclipse-copper-gear"},
        {"titanium-parts", "eclipse-titanium-rod"},
        {"chemistry", "eclipse-sulfuric-acid"},
        {"plastic", "eclipse-polyethylene"},
        {"superalloy", "eclipse-superalloy"},
        {"carbide", "eclipse-tungsten-carbide"},
        {"microchip", "eclipse-microchip"},
        {"aetherite", "eclipse-aetherite"},
        {"surge-superalloy", "eclipse-surge-superalloy"},
        {"experimental-core", "eclipse-experimental-core"},
        {"superconductor", "eclipse-superconductor"},
    };

    static float timer;

    public static void init(){
        Events.run(Trigger.update, () -> {
            if(!state.isPlaying() || state.rules.editor || state.rules.infiniteResources || player == null) return;
            timer += Time.delta;
            if(timer < 120f) return;
            timer = 0f;
            for(String id : check(player.team())) announce(id);
        });
    }

    /**
     * Records milestones the team has reached and returns the new ones. The first call on a save without the tag
     * records everything already reached and returns nothing (no burst of messages on older saves).
     */
    public static Seq<String> check(Team team){
        Seq<String> fresh = new Seq<>();
        boolean firstLook = !state.rules.tags.containsKey(tag);
        ObjectSet<String> done = ObjectSet.with(state.rules.tags.get(tag, "").split(","));
        ObjectSet<UnlockableContent> held = held(team);
        for(String[] m : list){
            if(done.contains(m[0])) continue;
            UnlockableContent c = content(m[1]);
            if(c == null || !held.contains(c)) continue;
            done.add(m[0]);
            if(!firstLook) fresh.add(m[0]);
        }
        if(firstLook || fresh.any()) state.rules.tags.put(tag, String.join(",", done.toSeq().select(s -> !s.isEmpty())));
        return fresh;
    }

    /** Every item and liquid held by the team's buildings. */
    static ObjectSet<UnlockableContent> held(Team team){
        ObjectSet<UnlockableContent> out = new ObjectSet<>();
        for(Building b : team.data().buildings){
            if(b.items != null) b.items.each((item, amount) -> out.add(item));
            if(b.liquids != null) b.liquids.each((liquid, amount) -> {
                if(amount > 0.01f) out.add(liquid);
            });
        }
        return out;
    }

    static @Nullable UnlockableContent content(String name){
        Item item = mindustry.Vars.content.item(name);
        return item != null ? item : mindustry.Vars.content.liquid(name);
    }

    static void announce(String id){
        if(headless) return;
        UnlockableContent c = null;
        for(String[] m : list) if(m[0].equals(id)) c = content(m[1]);
        ui.hudfrag.showToast(c == null ? Icon.ok : new TextureRegionDrawable(c.uiIcon), Core.bundle.get("eclipse-milestone." + id));
        Sounds.uiUnlock.play();
    }
}
