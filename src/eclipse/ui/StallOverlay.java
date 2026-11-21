package eclipse.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.struct.*;
import arc.util.*;
import eclipse.ui.MachineStatus.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.blocks.production.*;

import static mindustry.Vars.*;

/**
 * Toggle (K, rebindable) that marks every stopped machine of the player's team: a frame coloured by the reason and the
 * icon of the item it is waiting for or can't get rid of. The scan runs a few times a second, not every frame.
 */
public class StallOverlay{
    public static @Nullable KeyBind key;
    public static boolean shown;
    static final Seq<Building> stalled = new Seq<>();
    static final Seq<Status> reasons = new Seq<>();
    static float timer;

    public static void registerKey(){
        if(headless || mobile) return;
        key = KeyBind.add("eclipse_stall_overlay", KeyCode.k, "eclipse");
        Events.run(Trigger.update, () -> {
            if(!state.isGame()) return;
            if(Core.input.keyTap(key) && !(Core.scene.getKeyboardFocus() instanceof TextField)){
                shown = !shown;
                ui.showInfoToast(Core.bundle.get(shown ? "eclipse-stall.on" : "eclipse-stall.off"), 2f);
            }
            if(shown && (timer += Time.delta) >= 20f){
                timer = 0f;
                if(player != null) scan(player.team());
            }
        });
        Events.run(Trigger.draw, () -> {
            if(shown && state.isGame()) draw();
        });
    }

    /** Every stopped crafter of a team, with why. */
    public static void scan(mindustry.game.Team team){
        stalled.clear();
        reasons.clear();
        for(Building b : team.data().buildings){
            if(!(b.block instanceof GenericCrafter) || !b.isValid()) continue;
            Status s = MachineStatus.of(b);
            if(!s.problem()) continue;
            stalled.add(b);
            reasons.add(s);
        }
    }

    /** Whether the last scan marked this building. */
    public static boolean stalledContains(Building b){
        return stalled.contains(b);
    }

    public static Color color(Kind k){
        return switch(k){
            case noPower, underpowered -> Pal.remove;
            case outputFull -> Color.orange;
            case missing, noCatalyst -> Pal.accent;
            case noHeat, heatShort -> Color.valueOf("ff795e");
            default -> Color.lightGray;
        };
    }

    static void draw(){
        Draw.z(Layer.overlayUI);
        for(int i = 0; i < stalled.size; i++){
            Building b = stalled.get(i);
            if(!b.isValid()) continue;
            Status s = reasons.get(i);
            float half = b.block.size * tilesize / 2f;
            Lines.stroke(1.5f, color(s.kind));
            Draw.alpha(0.6f + Mathf.absin(10f, 0.4f));
            Lines.rect(b.x - half, b.y - half, half * 2f, half * 2f);
            UnlockableContent c = s.content;
            if(c != null){
                Draw.color();
                Draw.rect(c.fullIcon, b.x, b.y, 6f, 6f);
            }
        }
        Draw.reset();
    }
}
