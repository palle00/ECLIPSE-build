package eclipse.ui;

import arc.*;
import arc.graphics.*;
import arc.input.*;
import arc.scene.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import eclipse.content.*;
import eclipse.systems.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;

import static eclipse.ui.EclipseMenu.*;
import static mindustry.Vars.*;

/**
 * The ECLIPSE pause menu (desktop; mockup approved by the user 2026-11-25), in the main menu's look:
 * <ul>
 * <li>Resume, large; then a grid: Settings (ECLIPSE's {@link EclipseSettingsDialog}), the wiki, the production panel, the
 * stopped-machine marks, Save / Load and Planet map / Research (greyed where they don't apply); then Host / invite and
 * Quit. One centred column, scaled with the screen.</li>
 * </ul>
 * Vanilla builds its pause menu each time it opens; this replaces it right after. Every vanilla button this menu does not
 * draw itself (objectives, abandon, world processors, other mods' buttons) is kept, small, at the bottom left.
 * Phones, the editor and map playtests keep vanilla's menu (with the wiki button added).
 */
public class PauseMenu{
    /** Bundle keys of the vanilla buttons this menu replaces; any other button is carried over. */
    static final String[] replaced = {"back", "settings", "savegame", "loadgame", "save", "load", "research", "planetmap", "hostserver",
        "hostserver.mobile", "quit", "save.quit", "eclipse-wiki"};
    static final Color eclipseOrange = Color.valueOf("ff9a3c");

    public static void install(){
        //vanilla's own shown() listener (registered in its constructor) rebuilds the menu first; this runs after it
        ui.paused.shown(() -> {
            if(mobile || state.rules.editor || state.playtestingMap != null) addWikiButton(ui.paused.cont);
            else build();
        });
    }

    static void build(){
        PausedDialog dialog = ui.paused;
        Table cont = dialog.cont;
        Seq<TextButton> extras = new Seq<>();
        @Nullable TextButton[] quit = {null};
        collect(cont, extras, quit);
        dialog.titleTable.visible = false;
        cont.clear();
        //vanilla gives every cell of its menu a button's size (220 x 55): this page must fill the screen instead
        cont.defaults().reset();
        cont.center(); //the page is scaled about its centre, so its cell must be centred too

        //the approved mockup is a 1280 x 720 screen (user, 2026-11-25: "make it read as this"): the page is laid out at
        //exactly its positions (72 px margins, 56 from the top, the footer 28 from the
        //bottom) and then scaled as a whole to fill the real screen, centred, so it reads the same at any resolution
        Table page = new Table();
        page.setTransform(true);
        page.top().left();
        //the button column alone, in the middle of the page (user, 2026-11-25: the "This game" card removed)
        page.table(root -> root.table(left -> column(left, quit[0])).width(560f)).grow().pad(56f, 72f, 0f, 72f).row();
        page.table(f -> {
            f.left().bottom();
            //vanilla's situational buttons (abandon, objectives, world processors) and other mods': small, bottom left
            //buttons first, then check boxes (Steam's "friends only" is hidden unless hosting, but still takes its place)
            for(TextButton b : extras){
                if(b instanceof CheckBox) continue;
                String text = b.getText().toString();
                f.add(row(icon(is(text, "abandon") ? "abandon" : "more"), text, b::fireClick)).height(40f).padRight(10f);
            }
            for(TextButton b : extras) if(b instanceof CheckBox) f.add(b).height(40f).padRight(10f);
            f.add().growX();
            EclipseFonts.crisp(f.add("ECLIPSE " + modVersion()), 0.6f).color(Color.valueOf("8f8f9c")).right().bottom();
        }).growX().pad(0f, 72f, 28f, 72f);
        cont.add(page).size(1280f, 720f);
        page.update(() -> {
            //cont is the whole dialog (in pixels); the page's own size is 1280 x 720 UI units
            float k = Math.min(cont.getWidth() / Scl.scl(1280f), cont.getHeight() / Scl.scl(720f));
            page.setOrigin(Align.center);
            page.setScale(k);
        });
    }

    static String modVersion(){
        var mod = mods.getMod("eclipse");
        return mod == null ? "" : mod.meta.version;
    }

    /** The vanilla (and other mods') buttons this menu does not draw itself, and vanilla's quit button. */
    static void collect(Group group, Seq<TextButton> extras, TextButton[] quit){
        for(Element e : group.getChildren()){
            if(e instanceof TextButton b){
                String text = b.getText().toString();
                if(is(text, "quit") || is(text, "save.quit")) quit[0] = b;
                boolean ours = false;
                for(String k : replaced) if(is(text, k)) ours = true;
                if(!ours) extras.add(b);
            }else if(e instanceof Group g){
                collect(g, extras, quit);
            }
        }
    }

    static boolean is(String text, String key){
        return Core.bundle.has(key) && Core.bundle.get(key).equals(text);
    }

    // ---- the button column ----

    /** Width of a half-width tile in the 560-wide column (two per row, 10 apart). */
    static final float half = 275f;

    static void column(Table t, @Nullable TextButton quit){
        t.top().left();
        t.table(head -> {
            head.left();
            head.table(((TextureRegionDrawable)Tex.whiteui).tint(accent), c -> EclipseFonts.crisp(c.add(Core.bundle.get("eclipse-pause.paused")), 0.65f).color(dark).pad(3f, 10f, 2f, 10f));
            EclipseFonts.crisp(head.add(headline()), 0.7f).color(muted).padLeft(12f);
        }).left().padBottom(14f).row();

        t.add(tile(icon("resume"), Core.bundle.get("eclipse-pause.resume"), Core.bundle.get("eclipse-pause.resume.sub"), "Esc", true, () -> ui.paused.hide()))
            .width(half * 2f + 10f).height(80f).padBottom(10f).row();

        Table grid = new Table();
        Seq<Button> cells = new Seq<>();
        //always the same eight tiles, as in the mockup; ones that don't apply here (save and load in the campaign, which
        //saves by itself; planet map and research outside it) are greyed out, so the grid never changes shape
        boolean campaign = state.isCampaign();
        cells.add(tile(icon("settings"), Core.bundle.get("settings"), Core.bundle.get("eclipse-pause.settings.sub"), null, false, () -> EclipseSettingsDialog.instance.show()));
        cells.add(tile(icon("wiki"), Core.bundle.get("eclipse-wiki"), Core.bundle.get("eclipse-menu.sub.wiki"), key(EclipseWiki.entryKey), false, () -> EclipseWiki.instance.show()));
        cells.add(tile(icon("production"), Core.bundle.get("eclipse-production"), Core.bundle.get("eclipse-pause.production.sub"), key(ProductionStats.key), false, ProductionStats::show));
        cells.add(stallTile());
        cells.add(disabled(tile(icon("save"), Core.bundle.get("savegame"), campaign ? Core.bundle.get("eclipse-pause.save.auto") : saveLine(), null, false,
            () -> vanillaDialog("save")), campaign || net.client()));
        cells.add(disabled(tile(icon("load"), Core.bundle.get("loadgame"), campaign ? Core.bundle.get("eclipse-pause.load.campaign") : Core.bundle.get("eclipse-pause.load.sub"), null, false,
            () -> vanillaDialog("load")), campaign || net.client()));
        cells.add(disabled(tile(icon("planet"), Core.bundle.get("planetmap"), campaign ? Core.bundle.get("eclipse-pause.planet.sub") : Core.bundle.get("eclipse-pause.campaign-only"), null, false, () -> {
            ui.paused.hide();
            ui.planet.show();
        }), !campaign));
        cells.add(disabled(tile(icon("research"), Core.bundle.get("research"), campaign ? researchLine() : Core.bundle.get("eclipse-pause.campaign-only"), null, false,
            () -> ui.research.show()), !campaign));
        //fixed half widths: long subtitles would otherwise widen the grid past the column
        for(int i = 0; i < cells.size; i++){
            grid.add(cells.get(i)).width(half).height(62f).pad(0f, 0f, 10f, i % 2 == 0 ? 10f : 0f);
            if(i % 2 == 1) grid.row();
        }
        t.add(grid).left().row();

        Table bottom = new Table();
        if(!net.client()){
            boolean invite = net.server() && steam;
            bottom.add(row(icon("host"), Core.bundle.get(invite ? "eclipse-pause.invite" : "eclipse-pause.host"), () -> {
                if(invite) platform.inviteFriends();
                else ui.host.show();
            })).width(half).height(52f).padRight(10f);
        }
        String quitText = quit != null ? quit.getText().toString() : Core.bundle.get("quit");
        bottom.add(row(icon("quit"), quitText, () -> {
            if(quit != null) quit.fireClick();
            else Reflect.invoke(PausedDialog.class, ui.paused, "showQuitConfirm", new Object[0]);
        })).width(net.client() ? half * 2f + 10f : half).height(52f);
        t.add(bottom).left().row();
    }

    /** "Saved 4 min ago" for the current save, else what the button does. */
    static String saveLine(){
        var slot = control.saves.getCurrent();
        return slot == null ? Core.bundle.get("eclipse-pause.save.sub") : Core.bundle.format("eclipse-pause.save.saved", ago(System.currentTimeMillis(), slot.getTimestamp()));
    }

    /** A one-line tile: icon and text on the left, gold when hovered (the bottom row and vanilla's extra buttons). */
    static Button row(Drawable icon, String text, Runnable run){
        Button.ButtonStyle style = new Button.ButtonStyle(){{
            up = chamfer(fill, steel, 10f);
            over = down = chamfer(fillLit, accent, 10f);
        }};
        Drawable normal = style.up;
        Button b = new Button(style);
        b.left();
        Element image = b.add(new SizedIcon(icon)).size(20f).padLeft(14f).padRight(12f).get();
        Label label = EclipseFonts.crisp(b.add(text), 0.85f).left().padRight(18f).get();
        b.update(() -> {
            boolean lit = b.isOver();
            style.up = lit ? style.over : normal;
            label.setColor(lit ? accent : Color.white);
            image.setColor(lit ? accent : muted);
        });
        b.clicked(run);
        return b;
    }

    /** The pause menu's own outline icons (art/drafts/pauseicons.py), white so the tiles can tint them. */
    static Drawable icon(String name){
        return new TextureRegionDrawable(Core.atlas.find("eclipse-pause-" + name));
    }

    static Button disabled(Button b, boolean off){
        b.setDisabled(off);
        return b;
    }

    static @Nullable String key(@Nullable KeyBind bind){
        return bind == null ? null : EclipseWiki.keyName(bind);
    }

    static void vanillaDialog(String field){
        BaseDialog d = Reflect.get(PausedDialog.class, ui.paused, field);
        d.show();
    }

    static Button stallTile(){
        int stopped = 0;
        if(player != null){
            StallOverlay.scan(player.team());
            stopped = StallOverlay.stalled.size;
        }
        String sub = stopped == 0 ? Core.bundle.get("eclipse-pause.stalls.none") : Core.bundle.format("eclipse-pause.stalls.some", stopped);
        Button b = tile(icon("stalls"), Core.bundle.get("eclipse-pause.stalls"), sub, key(StallOverlay.key), false, () -> {
            StallOverlay.shown = true;
            ui.paused.hide();
        });
        ((Color[])b.userObject)[0] = eclipseOrange; //the mockup's warning icon is orange
        if(stopped > 0) b.find(e -> "sub".equals(e.name)).setColor(eclipseOrange);
        return b;
    }

    static String researchLine(){
        int total = 0, done = 0;
        for(Block b : content.blocks()){
            if(!b.name.startsWith("eclipse-") || b.techNode == null) continue;
            total++;
            if(b.unlocked()) done++;
        }
        return Core.bundle.format("eclipse-pause.research.sub", done, total);
    }

    static String headline(){
        StringBuilder s = new StringBuilder();
        if(state.rules.sector != null) s.append(state.rules.sector.name()).append(" · ").append(state.rules.sector.planet.localizedName);
        else if(state.map != null) s.append(state.map.name());
        if(state.rules.waves) s.append(" · ").append(Core.bundle.format("eclipse-pause.wave", state.wave));
        s.append(" · ").append(Core.bundle.format("eclipse-pause.played", playtime(control.saves.getTotalPlaytime())));
        return s.toString();
    }

    /** A chamfered tile: icon, title, subtitle and an optional key chip; gold when hovered. The big one is Resume. */
    static Button tile(Drawable icon, String title, String sub, @Nullable String keyName, boolean big, Runnable run){
        Button.ButtonStyle style = new Button.ButtonStyle(){{
            up = big ? chamfer(continueFill, continueFrame, 10f) : chamfer(fill, steel, 10f);
            over = down = big ? chamfer(continueLit, accent, 10f) : chamfer(fillLit, accent, 10f);
            disabled = chamfer(fill, steel, 10f);
        }};
        Drawable normal = style.up;
        Button b = new Button(style);
        b.left();
        Element image = b.add(new SizedIcon(icon)).size(big ? 30f : 22f).padLeft(big ? 22f : 14f).padRight(big ? 16f : 12f).get();
        Label[] title1 = {null};
        Color[] iconColor = {accent};
        b.userObject = iconColor; //stallTile turns it orange
        b.table(t -> {
            t.left();
            title1[0] = EclipseFonts.crisp(t.add(big ? title.toUpperCase() : title), big ? 1.35f : 0.9f).left().get();
            t.row();
            Cell<Label> c = EclipseFonts.crisp(t.add(sub), big ? 0.7f : 0.62f).color(big ? Color.valueOf("c9b58a") : Color.valueOf("8f8f9c")).left().name("sub");
            //a long subtitle (or translation) is cut with "..." instead of widening the tile
            if(!big) c.width(half - (keyName != null ? 110f : 70f)).get().setEllipsis(true);
        }).growX().left();
        if(keyName != null) b.table(chamfer(chipFill, steel, 5f), c -> EclipseFonts.crisp(c.add(keyName), 0.6f).color(muted).pad(2f, 8f, 2f, 8f)).padRight(16f);
        b.update(() -> {
            boolean lit = b.isOver() && !b.isDisabled();
            style.up = lit ? style.over : normal;
            title1[0].setColor(b.isDisabled() ? muted : big || lit ? accent : Color.white);
            image.setColor(b.isDisabled() ? muted : iconColor[0]);
        });
        b.clicked(() -> {
            if(!b.isDisabled()) run.run();
        });
        return b;
    }

    // ---- phones, the editor, playtests: vanilla's menu with the wiki button ----

    /** Puts the wiki button where "Save & Quit" (or "Quit") was, and moves that button to a new row right below. */
    static void addWikiButton(Table cont){
        TextButton button = new Table().button("@eclipse-wiki", Icon.book, () -> EclipseWiki.instance.show()).get();
        button.remove();
        Cell<?> quitCell = cont.getCells().find(c -> c.get() instanceof TextButton b && (is(b.getText().toString(), "save.quit") || is(b.getText().toString(), "quit")));
        if(quitCell != null){
            Element quit = quitCell.get();
            Cell<?> widest = quitCell;
            for(Cell<?> c : cont.getCells()) if(c.get() instanceof TextButton && c.minWidth() > widest.minWidth()) widest = c;
            Cell<?> quitLayout = new Cell<>().set(quitCell);
            quitCell.setElement(button);
            if(widest != quitCell) quitCell.set(widest);
            cont.row();
            cont.add(quit).set(quitLayout);
        }else{
            cont.row();
            cont.add(button).colspan(2).width(mobile ? 220f : 450f).height(55f).pad(5f);
        }
        ui.paused.titleTable.visible = true;
    }
}
