package eclipse.ui;

import arc.*;
import arc.input.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.fragments.*;
import mindustry.world.*;

import static mindustry.Vars.*;

/**
 * A search box at the top of the build menu's block grid (the grid moves down to make room). Type part of a block's
 * name and the grid itself shows the matching blocks from every tab, in build-menu order (tab by tab, each in its menu
 * position); hovering one shows its info as usual. Click one (or press Enter for the first) to pick it: the menu jumps
 * to its tab with it selected. Escape clears the search.
 * The build menu rebuilds its whole frame now and then, so the box re-attaches itself whenever that happens: the grid's
 * cell gets a small table holding the box and the grid (Cell.setElement, the layout's own way to swap a cell's content).
 * It reads the menu's package-private fields (blockPane, blockTable, menuHoverBlock, selectedBlocks) by reflection.
 */
public class BlockSearch{
    static final String marker = "eclipse-search";
    static final int columns = 4; //as vanilla's grid
    static final float boxHeight = 34f, gridHeight = 194f; //vanilla gives the grid 194

    static TextField field;
    /** The build menu's fields, looked up once in install(). */
    static java.lang.reflect.Field blockPane, blockTable, menuHoverBlock, selectedBlocks;
    static ScrollPane attachedTo;
    static String shownQuery = "";

    /** Placeable blocks whose name contains every word of the query, in build-menu order. */
    public static Seq<Block> matches(String query){
        String q = query.trim().toLowerCase();
        if(q.isEmpty()) return new Seq<>();
        String[] words = q.split("\\s+");
        Seq<Block> found = content.blocks().select(b -> b.isPlaceable() && b.environmentBuildable() && b.unlockedNow() && matchesAll(b, words));
        //ids follow the menu order (ECLIPSE's MenuOrder renumbers them), so this is the order the tabs show them in
        found.sort(Structs.comps(Structs.comparingInt(b -> b.category.ordinal()), Structs.comparingInt(b -> b.id)));
        return found;
    }

    static boolean matchesAll(Block b, String[] words){
        String text = (b.localizedName + " " + b.name.replace('-', ' ')).toLowerCase();
        for(String w : words) if(!text.contains(w)) return false;
        return true;
    }

    /** Throws if a game update renamed the build menu's fields this relies on, so the caller can turn it off. */
    public static void install(){
        PlacementFragment frag = ui.hudfrag.blockfrag;
        blockPane = menuField("blockPane");
        blockTable = menuField("blockTable");
        menuHoverBlock = menuField("menuHoverBlock");
        selectedBlocks = menuField("selectedBlocks");

        field = new TextField("");
        field.setMessageText(Core.bundle.get("eclipse-search.blocks"));
        field.keyDown(KeyCode.enter, () -> {
            Seq<Block> found = matches(field.getText());
            if(found.any()) pick(found.first());
        });
        field.keyDown(KeyCode.escape, BlockSearch::clear);

        Events.run(Trigger.update, () -> {
            if(state.isMenu()) return;
            if(!EclipseSettings.blockSearch.on()){
                detach(frag);
                return;
            }
            ScrollPane pane = get(blockPane, frag);
            //the menu rebuilt its frame: put the box back on top of the new grid
            if(pane != attachedTo && pane != null && pane.parent instanceof Table column){
                Cell<?> cell = column.getCell(pane);
                if(cell == null) return;
                Table box = new Table();
                cell.setElement(box);
                field.remove();
                box.add(field).growX().height(boxHeight).padBottom(2f).row();
                box.add(pane).grow();
                cell.height(gridHeight + boxHeight + 2f); //the grid keeps its height; the menu grows a little
                attachedTo = pane;
                shownQuery = "";
            }
            Table grid = get(blockTable, frag);
            if(grid == null) return;
            String query = field.getText().trim();
            boolean ours = grid.find(marker) != null;
            if(!query.isEmpty() && (!ours || !query.equals(shownQuery))){
                fill(frag, grid, query);
            }else if(query.isEmpty() && ours){
                //search cleared: give the grid back to the current tab
                shownQuery = "";
                frag.rebuild();
            }
        });
    }

    /** Takes the search box away again (the Build menu search setting turned off): the grid gets its old place back. */
    static void detach(PlacementFragment frag){
        if(attachedTo == null) return;
        ScrollPane pane = attachedTo;
        attachedTo = null;
        field.setText("");
        if(field.parent instanceof Table box && box.parent instanceof Table column){
            Cell<?> cell = column.getCell(box);
            if(cell != null){
                cell.setElement(pane);
                cell.height(gridHeight);
            }
        }
        frag.rebuild();
    }

    /** Shows the matches in the menu's own grid, with buttons like vanilla's, from the first slot. */
    static void fill(PlacementFragment frag, Table grid, String query){
        shownQuery = query;
        grid.clear();
        grid.top().left().margin(5f);
        Seq<Block> found = matches(query);
        ButtonGroup<ImageButton> group = new ButtonGroup<>();
        group.setMinCheckCount(0);
        for(int i = 0; i < found.size; i++){
            Block b = found.get(i);
            ImageButton button = grid.button(new TextureRegionDrawable(b.uiIcon), Styles.selecti, () -> pick(b)).size(46f).group(group).name(marker).get();
            button.resizeImage(iconMed);
            button.update(() -> button.setChecked(control.input.block == b));
            button.hovered(() -> set(menuHoverBlock, frag, b));
            button.exited(() -> {
                if(get(menuHoverBlock, frag) == b) set(menuHoverBlock, frag, null);
            });
            if((i + 1) % columns == 0) grid.row();
        }
        if(found.isEmpty()) grid.add("[lightgray]" + Core.bundle.get("eclipse-search.none")).name(marker).pad(6f);
    }

    /** Picks a block for placing, as if chosen from its tab, and clears the search (the menu then shows that tab). */
    static void pick(Block b){
        PlacementFragment frag = ui.hudfrag.blockfrag;
        control.input.block = b;
        frag.currentCategory = b.category;
        ObjectMap<Category, Block> selected = get(selectedBlocks, frag);
        if(selected != null) selected.put(b.category, b);
        clear();
    }

    /** A field of the build menu, made accessible; throws if a game update renamed it. */
    static java.lang.reflect.Field menuField(String name){
        try{
            java.lang.reflect.Field f = PlacementFragment.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T get(java.lang.reflect.Field f, Object o){
        try{
            return (T)f.get(o);
        }catch(IllegalAccessException e){
            throw new RuntimeException(e);
        }
    }

    static void set(java.lang.reflect.Field f, Object o, Object value){
        try{
            f.set(o, value);
        }catch(IllegalAccessException e){
            throw new RuntimeException(e);
        }
    }

    static void clear(){
        field.setText("");
        Core.scene.setKeyboardFocus(null);
    }
}
