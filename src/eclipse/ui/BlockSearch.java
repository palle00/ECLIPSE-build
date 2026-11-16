package eclipse.ui;

import arc.*;
import arc.input.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.world.*;

import static mindustry.Vars.*;

/**
 * A search box right above the build menu. Type part of a block's name: matching blocks from every tab appear as icons;
 * click one (or press Enter for the first) to pick it for placing, as if chosen from its tab. Escape clears the search.
 * Vanilla's build menu can't be filtered from a mod (its block lists are package-private), so this sits next to it.
 */
public class BlockSearch{
    static final int maxResults = 24, columns = 6;

    /** Placeable blocks whose name contains every word of the query, best matches (name starts with it) first. */
    public static Seq<Block> matches(String query){
        String q = query.trim().toLowerCase();
        if(q.isEmpty()) return new Seq<>();
        String[] words = q.split("\\s+");
        Seq<Block> found = content.blocks().select(b -> b.isPlaceable() && b.environmentBuildable() && b.unlockedNow() && matchesAll(b, words));
        found.sort(Structs.comps(Structs.comparingBool(b -> !b.localizedName.toLowerCase().startsWith(q)), Structs.comparing(b -> b.localizedName)));
        return found;
    }

    static boolean matchesAll(Block b, String[] words){
        String text = (b.localizedName + " " + b.name.replace('-', ' ')).toLowerCase();
        for(String w : words) if(!text.contains(w)) return false;
        return true;
    }

    public static void install(){
        Table toggler = Reflect.get(ui.hudfrag.blockfrag, "toggler");
        if(toggler == null) return;

        Table root = new Table();
        root.touchable = Touchable.childrenOnly;
        Table results = new Table(Styles.black6);
        TextField[] field = {null};

        Runnable[] rebuild = {null};
        rebuild[0] = () -> {
            results.clear();
            Seq<Block> found = matches(field[0].getText());
            for(int i = 0; i < Math.min(found.size, maxResults); i++){
                Block b = found.get(i);
                results.button(new TextureRegionDrawable(b.uiIcon), Styles.selecti, 32f, () -> pick(b, field[0]))
                    .size(46f).tooltip(b.localizedName).checked(btn -> control.input.block == b);
                if((i + 1) % columns == 0) results.row();
            }
            if(found.size > maxResults) results.row().add("[lightgray]+" + (found.size - maxResults)).colspan(columns);
            if(found.isEmpty() && !field[0].getText().isBlank()) results.add("[lightgray]" + Core.bundle.get("eclipse-search.none")).pad(6f);
        };

        root.add(results).right().row();
        field[0] = root.field("", text -> rebuild[0].run()).growX().height(40f).get();
        field[0].setMessageText(Core.bundle.get("eclipse-search.blocks"));
        field[0].keyDown(KeyCode.enter, () -> {
            Seq<Block> found = matches(field[0].getText());
            if(found.any()) pick(found.first(), field[0]);
        });
        field[0].keyDown(KeyCode.escape, () -> clear(field[0]));
        results.visible(() -> !field[0].getText().isBlank());

        //sit on top of the build menu's frame, matching its width
        root.update(() -> {
            if(toggler.getChildren().isEmpty()) return;
            Element frame = toggler.getChildren().first();
            frame.localToStageCoordinates(Tmp.v1.set(frame.getWidth(), frame.getHeight()));
            root.getCell(field[0]).width(frame.getWidth());
            root.pack();
            root.setPosition(Tmp.v1.x - root.getWidth(), Tmp.v1.y);
        });
        root.visible(() -> ui.hudfrag.shown && toggler.visible && !toggler.getChildren().isEmpty() && toggler.getChildren().first().visible && !state.isMenu());
        ui.hudGroup.addChild(root);
    }

    static void pick(Block b, TextField field){
        control.input.block = b;
        ui.hudfrag.blockfrag.currentCategory = b.category;
        ui.hudfrag.blockfrag.rebuild();
        clear(field);
    }

    static void clear(TextField field){
        field.setText("");
        field.change();
        Core.scene.setKeyboardFocus(null);
    }
}
