package eclipse.ui;

import arc.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import eclipse.augment.*;
import mindustry.core.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.*;

/**
 * Replaces the HUD's core resource list. With the "compact core display" setting on (default), it only lists
 * vanilla items and ECLIPSE items used in building costs; process intermediates stay in the core but are not listed.
 * Augmentation modules are never listed (cores refuse them).
 */
public class CompactCoreDisplay extends Table{
    public static final String setting = "eclipse-compact-core";

    final ObjectSet<Item> used = new ObjectSet<>();
    static ObjectSet<Item> buildMaterials;
    CoreBuild core;
    boolean compact;

    /** Swaps the vanilla display out of the HUD. Call on ClientLoadEvent. */
    public static void install(){
        ui.settings.game.checkPref(setting, true);
        Table old = ui.hudfrag.coreItems;
        if(old == null || !(old.parent instanceof Table parent)) return;
        Cell<?> cell = parent.getCell(old);
        if(cell == null) return;
        cell.setElement(new CompactCoreDisplay());
    }

    public CompactCoreDisplay(){
        rebuild();
    }

    static boolean shown(Item item, boolean compact){
        if(item instanceof AugmentModule) return false;
        if(!compact || !item.name.startsWith("eclipse-")) return true;
        if(buildMaterials == null){
            buildMaterials = new ObjectSet<>();
            for(Block b : content.blocks()){
                if(b.requirements != null) for(ItemStack s : b.requirements) buildMaterials.add(s.item);
            }
        }
        return buildMaterials.contains(item);
    }

    void rebuild(){
        clear();
        compact = Core.settings.getBool(setting, true);
        if(used.size > 0){
            background(Styles.black6);
            margin(4f);
        }

        update(() -> {
            CoreBuild current = player == null ? null : player.core();
            boolean nowCompact = Core.settings.getBool(setting, true);
            if(current != core || nowCompact != compact){
                core = current;
                used.clear();
                rebuild();
                return;
            }
            if(core != null && content.items().contains(i -> shown(i, compact) && core.items.get(i) > 0 && used.add(i))) rebuild();
        });

        int i = 0;
        for(Item item : content.items()){
            if(!used.contains(item)) continue;
            image(item.uiIcon).size(24f).padRight(3f).tooltip(t -> t.background(Styles.black6).margin(4f).add(item.localizedName).style(Styles.outlineLabel));
            label(() -> core == null ? "0" : UI.formatAmount(core.items.get(item))).padRight(3f).minWidth(52f).left();
            if(++i % 4 == 0) row();
        }
    }
}
