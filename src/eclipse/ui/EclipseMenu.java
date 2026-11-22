package eclipse.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.core.GameState.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Saves.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.mod.Mods.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.ui.fragments.MenuFragment.*;

import static mindustry.Vars.*;

/**
 * The ECLIPSE main menu (desktop, while the ECLIPSE backdrop is on), over the dusk scene and under the ECLIPSE logo:
 * <ul>
 * <li>Continue: the newest save or campaign sector, with a preview, the wave, time played, when, and the next wave
 * that may bring an eclipse. Enter starts it.</li>
 * <li>Play, large; it opens the Play panel ({@link PlayPanel}) beside the column.</li>
 * <li>The other buttons (the ECLIPSE wiki, vanilla's and other mods') in a two-column grid; Settings and Quit share
 * its next slot as two half-width buttons.</li>
 * <li>A What's New card under the eclipse, and a footer with a tip, the credits and the version.</li>
 * </ul>
 * Chamfered industrial frames that turn gold when hovered or chosen with the arrow keys. Every action is vanilla's
 * own (or the wiki); the logo is drawn by {@link MenuBackdrop}, and everything fades in after its intro.
 */
public class EclipseMenu{
    /** The ECLIPSE community server, opened from the news card or the footer. */
    public static final String discord = "https://discord.gg/tbeqDZed2";

    static final Color accent = Pal.accent, steel = Color.valueOf("282b36"), fill = Color.valueOf("101218f0"), fillLit = Color.valueOf("2a2214f5"),
        iconFill = Color.valueOf("282b3666"), chipFill = Color.valueOf("15171ee6"), footerFill = Color.valueOf("08080acc"), muted = Color.valueOf("a1a1aa"),
        light = Color.valueOf("c4c4cc"), cardFill = Color.valueOf("12141bf2"), panelFill = Color.valueOf("0c0d12f7"), continueFill = Color.valueOf("1d1810f2"),
        continueFrame = Color.valueOf("ffd37fbf"), continueLit = Color.valueOf("2c2312f7"), eclipseColor = Color.valueOf("ff9a3c"), dark = Color.valueOf("1d1608");
    static final float columnWidth = 600f, gap = 8f;
    static final String wikiSeen = "eclipse-menu-wiki-seen";

    /** The sizes a menu button comes in: Play; a grid cell; Settings and Quit; a row in another mod's submenu panel. */
    enum Kind{
        big(80f, 52f, 28f, 1.3f, true), cell(62f, 34f, 19f, 0.9f, false), small(62f, 30f, 17f, 0.8f, false), row(64f, 40f, 22f, 1f, true);

        final float height, box, icon, title;
        final boolean arrow;

        Kind(float height, float box, float icon, float title, boolean arrow){
            this.height = height;
            this.box = box;
            this.icon = icon;
            this.title = title;
            this.arrow = arrow;
        }
    }

    static Table column, panel, footer, news;
    static Element dim;
    /** The button whose panel is open (Play, or another mod's button with a submenu). */
    static @Nullable MenuButton open;
    /** The game Continue resumes. */
    static @Nullable SaveSlot last;
    /** Keyboard navigation: the column's buttons, the open panel's buttons, and the one chosen with the arrow keys. */
    static final Seq<Button> columnNav = new Seq<>(), panelNav = new Seq<>();
    static int focus = -1, quiet;
    static boolean newsFits = true;
    static int tip;
    static @Nullable Label tipLabel;
    static @Nullable BaseDialog whatsNew, credits;

    /** Hides vanilla's buttons and adds ours and the footer on top of the menu. Call after the backdrop is installed. */
    static void install(Group menu, MenuBackdrop backdrop){
        if(mobile) return;
        Element buttons = menu.find("buttons");
        if(buttons == null){
            Log.warn("[ECLIPSE] menu: vanilla buttons not found, keeping them");
            return;
        }
        Element vanillaPanel = buttons;
        while(vanillaPanel.parent != null && vanillaPanel.parent != menu) vanillaPanel = vanillaPanel.parent;
        vanillaPanel.visible(() -> !on());
        //vanilla's big Discord button (Mindustry's server) in the corner: ours is on the news card instead
        Element vanillaDiscord = menu.find("discord");
        if(vanillaDiscord != null) vanillaDiscord.visible(() -> !on() && !ui.consolefrag.shown());

        column = new Table();
        news = new Table();
        footer = new Table();
        panel = new Table();
        //behind an open panel: darkens the scene and closes the panel when clicked
        dim = new Element(){
            @Override
            public void draw(){
                Draw.color(0.024f, 0.027f, 0.035f, 0.62f * parentAlpha);
                Fill.crect(x, y, width, height);
                Draw.reset();
            }
        };
        dim.clicked(EclipseMenu::close);
        column.visible(EclipseMenu::on);
        news.visible(() -> on() && newsFits && open == null);
        footer.visible(EclipseMenu::on);
        dim.visible(() -> on() && open != null);
        panel.visible(() -> on() && open != null);
        menu.addChild(column);
        menu.addChild(news);
        menu.addChild(footer);
        menu.addChild(dim);
        menu.addChild(panel);
        Seq<String> tips = tips();
        tip = tips.isEmpty() ? 0 : Mathf.random(tips.size - 1);
        rebuild();
        buildNews();
        buildFooter();

        //the logo above everything, the layout of the parts around it, and the keys
        Element logo = new Element(){
            @Override
            public void draw(){
                backdrop.drawLogo(MenuBackdrop.logoAlpha());
            }

            @Override
            public void act(float delta){
                super.act(delta);
                arrange(backdrop);
                keys();
            }
        };
        logo.touchable = Touchable.disabled;
        logo.visible(EclipseMenu::on);
        menu.addChild(logo);
        //vanilla rebuilds its list on resize; follow it. Back in the menu, Continue may have a newer game
        Events.on(ResizeEvent.class, e -> Core.app.post(EclipseMenu::rebuild));
        Events.on(StateChangeEvent.class, e -> {
            if(e.to == State.menu) Core.app.post(EclipseMenu::rebuild);
        });
    }

    static boolean on(){
        return Core.settings.getBool(MenuBackdrop.setting, true);
    }

    /** Places the column under the logo, the news card bottom right, an open panel beside the column; fades all in after the intro. */
    static void arrange(MenuBackdrop backdrop){
        float w = Core.graphics.getWidth(), h = Core.graphics.getHeight(), footH = Scl.scl(44f);
        footer.setBounds(0f, 0f, w, footH);
        if(linksCell != null){
            float pad = backdrop.logoLeftScreen() / Scl.scl(1f);
            if(Math.abs(linksPad - pad) > 0.5f){
                linksPad = pad;
                linksCell.padLeft(pad);
                footer.invalidate();
            }
        }
        //the column starts right under the logo's version line and is scaled down if it would reach the footer
        float left = backdrop.logoLeftScreen(), top = backdrop.logoBottomScreen() - Scl.scl(20f), floor = footH + Scl.scl(20f);
        column.pack();
        float s = Math.min(1f, (top - floor) / column.getHeight());
        column.setTransform(s < 0.999f);
        column.setScale(s);
        column.setPosition((int)left, (int)(top - column.getHeight() * s));
        float columnRight = left + column.getWidth() * s;

        news.pack();
        float nx = w - news.getWidth() - Scl.scl(72f), ny = footH + Scl.scl(48f);
        boolean fits = nx > columnRight + Scl.scl(48f);
        news.setPosition((int)nx, (int)ny);
        if(fits != newsFits){
            newsFits = fits;
            buildFooter();
        }

        if(open != null){
            panel.pack();
            float pw = panel.getWidth(), ph = panel.getHeight(), px = columnRight + Scl.scl(32f), availW = w - px - Scl.scl(48f);
            //not enough room beside the column: over it
            boolean over = availW < pw * 0.8f;
            if(over){
                px = left;
                availW = w - px - Scl.scl(48f);
            }
            float ps = Math.min(1f, Math.min(availW / pw, (top - floor) / ph));
            panel.setTransform(ps < 0.999f);
            panel.setScale(ps);
            panel.setPosition((int)px, (int)(top - ph * ps));
            dim.setBounds(over ? 0f : columnRight + Scl.scl(16f), footH, w, h - footH);
        }

        //hidden during the intro, fading in at its end; no clicks until it is visible
        float alpha = MenuBackdrop.uiAlpha();
        Touchable touch = MenuBackdrop.introPlaying() ? Touchable.disabled : Touchable.childrenOnly;
        for(Table t : new Table[]{column, news, footer}){
            t.color.a = alpha;
            t.touchable = touch;
        }
    }

    /** Up and down choose a button, Enter presses it (or Continue, if none is chosen), Escape closes the panel. */
    static void keys(){
        //a few quiet frames first: not the key that closed a dialog or skipped the intro
        if(!on() || Core.scene.hasDialog() || MenuBackdrop.introPlaying() || Core.scene.getKeyboardFocus() instanceof TextField || !state.isMenu()){
            quiet = 0;
            return;
        }
        if(++quiet < 10) return;
        if(Core.input.deltaX() != 0f || Core.input.deltaY() != 0f) focus = -1; //the mouse takes over
        Seq<Button> nav = open != null ? panelNav : columnNav;
        if(Core.input.keyTap(KeyCode.down) || Core.input.keyTap(KeyCode.up)){
            if(nav.isEmpty()) return;
            int step = Core.input.keyTap(KeyCode.down) ? 1 : -1;
            focus = focus < 0 ? (step > 0 ? 0 : nav.size - 1) : (focus + step + nav.size) % nav.size;
        }else if(Core.input.keyTap(KeyCode.enter)){
            if(focus >= 0 && focus < nav.size) nav.get(focus).fireClick();
            else if(open == null && last != null) resume(last);
        }else if(Core.input.keyTap(KeyCode.escape) && open != null){
            close();
        }
    }

    static boolean focused(Button b){
        Seq<Button> nav = open != null ? panelNav : columnNav;
        return focus >= 0 && focus < nav.size && nav.get(focus) == b;
    }

    // ---- the column ----

    /** All main menu buttons: vanilla's, then those other mods added, then Quit. */
    static Seq<MenuButton> buttons(){
        Seq<MenuButton> all = new Seq<>();
        if(ui.menufrag.desktopButtons != null) all.addAll(ui.menufrag.desktopButtons);
        try{
            Seq<MenuButton> custom = Reflect.get(ui.menufrag, "customButtons");
            if(custom != null) all.addAll(custom);
        }catch(Throwable ignored){
        }
        all.add(new MenuButton("@quit", Icon.exit, Core.app::exit));
        //vanilla leaves a null in its list where a button doesn't apply (Workshop outside Steam)
        all.removeAll(b -> b == null);
        return all;
    }

    static void rebuild(){
        if(column == null) return;
        column.clear();
        columnNav.clear();
        close();
        column.defaults().left();
        last = newestSave();
        if(last != null) column.add(continueCard(last)).size(columnWidth, 136f).padBottom(16f).row();

        Seq<MenuButton> all = buttons();
        MenuButton play = all.find(b -> "play".equals(key(b)));
        if(play != null) column.add(button(play, Kind.big, columnWidth, null)).size(columnWidth, Kind.big.height).padBottom(16f).row();

        //the rest two by two: the wiki first, then vanilla's and other mods' buttons in their order
        Seq<MenuButton> cells = new Seq<>();
        if(EclipseWiki.instance != null) cells.add(new MenuButton("@eclipse-menu.wiki", Icon.book, () -> {
            Core.settings.put(wikiSeen, true);
            EclipseWiki.instance.show();
        }));
        for(MenuButton b : all){
            String k = key(b);
            if(b != play && !"settings".equals(k) && !"quit".equals(k)) cells.add(b);
        }
        float cellWidth = (columnWidth - gap) / 2f;
        Table grid = new Table();
        for(int i = 0; i < cells.size; i++){
            MenuButton b = cells.get(i);
            String tag = "eclipse-menu.wiki".equals(key(b)) && !Core.settings.getBool(wikiSeen, false) ? Core.bundle.get("eclipse-menu.new") : null;
            grid.add(button(b, Kind.cell, cellWidth, tag)).size(cellWidth, Kind.cell.height).padRight(i % 2 == 0 ? gap : 0f).padBottom(gap);
            if(i % 2 == 1) grid.row();
        }
        //Settings and Quit share the next grid slot, half a button each
        Table util = new Table();
        float halfWidth = (cellWidth - gap) / 2f;
        for(MenuButton b : all){
            String k = key(b);
            if("settings".equals(k) || "quit".equals(k)) util.add(button(b, Kind.small, halfWidth, null)).size(halfWidth, Kind.small.height).padRight(util.getChildren().isEmpty() ? gap : 0f);
        }
        if(util.hasChildren()) grid.add(util).size(cellWidth, Kind.cell.height).padRight(cells.size % 2 == 0 ? gap : 0f).padBottom(gap).left();
        column.add(grid).left().padBottom(16f - gap).row();

        column.add(Core.bundle.get("eclipse-menu.keys")).fontScale(0.65f).color(Color.valueOf("6e6e7e")).left();
    }

    /** The newest game to continue: a save or a campaign sector you still hold, by when it was saved. */
    static @Nullable SaveSlot newestSave(){
        SaveSlot best = null;
        for(SaveSlot s : control.saves.getSaveSlots()){
            try{
                if(s.meta == null || s.isSector() && (s.getSector() == null || !s.getSector().hasBase())) continue;
                if(best == null || s.getTimestamp() > best.getTimestamp()) best = s;
            }catch(Throwable ignored){ //a save made with content that is gone
            }
        }
        return best;
    }

    /** Loads a save, or resumes a campaign sector the way the planet view does. */
    static void resume(SaveSlot s){
        close();
        checkPlay(() -> {
            Sector sector = s.isSector() ? s.getSector() : null;
            if(sector == null) ui.load.runLoadSave(s);
            else if(sector.hasBase()) control.playSector(sector);
            else{
                ui.planet.state.planet = sector.planet;
                ui.planet.show();
            }
        });
    }

    /** As vanilla's menu: no game while a mod has content errors. */
    static void checkPlay(Runnable run){
        if(!mods.hasContentErrors()) run.run();
        else ui.showInfo("@mod.noerrorplay");
    }

    static Button continueCard(SaveSlot s){
        Button.ButtonStyle style = new Button.ButtonStyle(){{
            up = chamfer(continueFill, continueFrame, 10f);
            over = down = chamfer(continueLit, accent, 10f);
        }};
        Drawable normal = style.up;
        Button button = new Button(style){
            @Override
            public boolean isOver(){
                return super.isOver() && !Core.scene.hasDialog();
            }
        };
        button.left();
        Table preview = new Table(chamfer(Color.valueOf("0b0b0e"), Color.valueOf("3a3326"), 4f));
        Texture tex = null;
        try{
            tex = s.previewTexture();
        }catch(Throwable ignored){
        }
        if(tex != null){
            preview.add(new Image(cropped(tex, 160f / 98f))).size(160f, 98f);
        }else preview.add(new SizedIcon(s.isSector() ? Icon.planet : Icon.map)).size(40f).color(muted);
        button.add(preview).size(168f, 105f).padLeft(14f).padRight(18f);
        float textWidth = columnWidth - 14f - 168f - 18f - 22f - 34f;
        button.table(t -> {
            t.left().defaults().left();
            t.table(head -> {
                head.left();
                head.add(Core.bundle.get("eclipse-menu.continue")).fontScale(0.75f).color(accent);
                head.table(chamfer(chipFill, Color.valueOf("3a3d4a"), 3f), k -> k.add(Core.bundle.get("eclipse-menu.enter")).fontScale(0.6f).color(muted).pad(1f, 6f, 1f, 6f)).padLeft(10f);
            }).row();
            Label name = t.add(saveName(s)).fontScale(1.3f).width(textWidth).get();
            name.setEllipsis(true);
            t.row();
            t.add(saveMeta(s)).fontScale(0.75f).color(light).width(textWidth).get().setEllipsis(true);
            String eclipse = eclipseLine(s);
            if(eclipse != null){
                t.row();
                t.table(e -> {
                    e.left();
                    e.add(new EclipseMark()).size(14f).padRight(8f);
                    e.add(eclipse).fontScale(0.7f).color(eclipseColor).width(textWidth - 22f).get().setEllipsis(true);
                });
            }
        }).width(textWidth).left();
        Element arrow = button.add(new SizedIcon(Icon.rightOpen)).size(20f).padLeft(12f).color(accent).get();
        float[] slide = {0f};
        button.update(() -> {
            boolean lit = button.isOver() || focused(button);
            slide[0] = Mathf.lerpDelta(slide[0], lit ? 6f : 0f, 0.3f);
            arrow.translation.x = Scl.scl(slide[0]);
            style.up = lit ? style.over : normal;
        });
        button.clicked(() -> resume(s));
        columnNav.add(button);
        return button;
    }

    /** The middle of a texture at an aspect ratio (width / height): a map preview filling its box. */
    static TextureRegion cropped(Texture tex, float aspect){
        TextureRegion region = new TextureRegion(tex);
        float own = tex.width / (float)tex.height;
        if(own > aspect){
            float cut = (1f - aspect / own) / 2f;
            region.set(cut, 0f, 1f - cut, 1f);
        }else{
            float cut = (1f - own / aspect) / 2f;
            region.set(0f, cut, 1f, 1f - cut);
        }
        return region;
    }

    /** A tiny eclipse: the black disk with its gold rim and glow, before "may bring an eclipse". */
    static class EclipseMark extends Element{
        @Override
        public void draw(){
            float cx = x + width / 2f, cy = y + height / 2f, r = Math.min(width, height) / 2f - Scl.scl(2f);
            Draw.blend(Blending.additive);
            Fill.light(cx, cy, 24, r * 2.2f, Tmp.c1.set(1f, 0.67f, 0.27f, 0.55f * parentAlpha), Tmp.c2.set(1f, 0.6f, 0.2f, 0f));
            Draw.blend();
            Draw.color(Color.valueOf("07080a"), parentAlpha);
            Fill.circle(cx, cy, r);
            Draw.color(accent, parentAlpha);
            Lines.stroke(Scl.scl(1.5f));
            Lines.circle(cx, cy, r);
            Draw.reset();
        }
    }

    static String saveName(SaveSlot s){
        Sector sector = s.isSector() ? s.getSector() : null;
        return sector != null ? sector.planet.localizedName + " · " + sector.name() : s.getName();
    }

    /** "Wave 34 · 2 h 10 min played · yesterday" (without the wave where there are none). */
    static String saveMeta(SaveSlot s){
        String time = playtime(s.meta.timePlayed), when = ago(System.currentTimeMillis(), s.getTimestamp());
        return s.meta.rules != null && s.meta.rules.waves ? Core.bundle.format("eclipse-menu.continue.meta", s.getWave(), time, when)
            : Core.bundle.format("eclipse-menu.continue.meta-nowaves", time, when);
    }

    /** The next wave that may bring an eclipse and its chance, where eclipses happen (waves on, not PvP or the editor). */
    static @Nullable String eclipseLine(SaveSlot s){
        Rules r = s.meta.rules;
        if(r == null || !r.waves || r.pvp || r.editor) return null;
        int next = nextEclipseWave(s.getWave());
        return Core.bundle.format("eclipse-menu.continue.eclipse", next, Math.round(eclipse.systems.EclipseEvent.chance(next) * 100f));
    }

    /** The first wave from this one on (as the HUD counts them) that may bring an eclipse: every 5th, from 5. */
    public static int nextEclipseWave(int wave){
        int every = eclipse.systems.EclipseEvent.every;
        return Math.max(every, (wave + every - 1) / every * every);
    }

    /** Play time: "2 h 10 min", "45 min". */
    public static String playtime(long millis){
        long minutes = millis / 60000L;
        return minutes >= 60 ? Core.bundle.format("eclipse-menu.time.hm", minutes / 60, minutes % 60) : Core.bundle.format("eclipse-menu.time.m", minutes);
    }

    /** How long ago: "just now", "12 min ago", "3 h ago", "yesterday", "5 days ago". */
    public static String ago(long now, long then){
        long minutes = Math.max(0L, now - then) / 60000L;
        if(minutes < 1) return Core.bundle.get("eclipse-menu.ago.now");
        if(minutes < 60) return Core.bundle.format("eclipse-menu.ago.min", minutes);
        if(minutes < 60 * 24) return Core.bundle.format("eclipse-menu.ago.hours", minutes / 60);
        if(minutes < 60 * 48) return Core.bundle.get("eclipse-menu.ago.yesterday");
        return Core.bundle.format("eclipse-menu.ago.days", minutes / (60 * 24));
    }

    // ---- drawing ----

    /** An octagon with cut corners (Mindustry's chamfered look), framed: the frame colour, then the fill inset by it. */
    static Drawable chamfer(Color fillColor, Color frame, float cut){
        return new BaseDrawable(){
            @Override
            public void draw(float x, float y, float w, float h){
                float a = Draw.getColor().a, c = Scl.scl(cut), t = Scl.scl(1.5f);
                Draw.color(frame, frame.a * a);
                octagon(x, y, w, h, c);
                Draw.color(fillColor, fillColor.a * a);
                octagon(x + t, y + t, w - t * 2f, h - t * 2f, Math.max(c - t * 0.6f, 0f));
                Draw.color(1f, 1f, 1f, a);
            }
        };
    }

    static void octagon(float x, float y, float w, float h, float c){
        Fill.crect(x + c, y, w - c * 2f, h);
        Fill.crect(x, y + c, c, h - c * 2f);
        Fill.crect(x + w - c, y + c, c, h - c * 2f);
        Fill.tri(x, y + c, x + c, y + c, x + c, y);
        Fill.tri(x + w - c, y, x + w - c, y + c, x + w, y + c);
        Fill.tri(x, y + h - c, x + c, y + h, x + c, y + h - c);
        Fill.tri(x + w - c, y + h, x + w, y + h - c, x + w - c, y + h - c);
    }

    // ---- buttons ----

    /** Vanilla's bundle key of a button ("play" for "@play", "database" for "@database.button", "wiki" for ours), or null for a plain text (another mod's). */
    static final String[] knownKeys = {"play", "campaign", "customgame", "loadgame", "joingame", "tutorial", "database", "schematics", "editor",
        "workshop", "mods", "settings", "about", "quit", "be"};

    public static @Nullable String key(MenuButton b){
        if(b.text == null) return null;
        if(b.text.startsWith("@")){
            String k = b.text.substring(1);
            return k.endsWith(".button") ? k.substring(0, k.length() - 7) : k;
        }
        for(String k : knownKeys) if(Core.bundle.has(k) && Core.bundle.get(k).equals(b.text)) return k;
        return null;
    }

    static String text(MenuButton b){
        return b.text.startsWith("@") ? Core.bundle.get(b.text.substring(1)) : b.text;
    }

    public static @Nullable String subtitle(MenuButton b){
        String k = key(b);
        if(k == null) return null;
        if(k.startsWith("eclipse-menu.")) k = k.substring(13);
        return Core.bundle.has("eclipse-menu.sub." + k) ? Core.bundle.get("eclipse-menu.sub." + k) : null;
    }

    /** A menu button of a kind, at a width (or as wide as its text, below 0), with an optional tag after its title (NEW). */
    static Button button(MenuButton b, Kind kind, float width, @Nullable String tag){
        Button.ButtonStyle style = new Button.ButtonStyle(){{
            up = chamfer(fill, steel, 10f);
            over = down = checked = chamfer(fillLit, accent, 10f);
        }};
        Drawable normal = style.up;
        //no hover look while a dialog covers the menu: the button never sees the mouse leave
        Button button = new Button(style){
            @Override
            public boolean isOver(){
                return super.isOver() && !Core.scene.hasDialog();
            }
        };
        button.left();
        Table icon = new Table(chamfer(iconFill, steel, 5f));
        Element image = icon.add(new SizedIcon(b.icon)).size(kind.icon).get();
        button.add(icon).size(kind.box).padLeft(12f).padRight(kind == Kind.big ? 16f : 12f);
        String subtitle = kind == Kind.small ? null : subtitle(b);
        Label title = new Label(text(b).toUpperCase());
        title.setFontScale(kind.title);
        float textWidth = width < 0f ? -1f : width - 12f - kind.box - (kind == Kind.big ? 16f : 12f) - (kind.arrow ? 16f + 14f + 18f : 16f) - (tag != null ? 60f : 0f);
        //a half-width button: shrink a long title (or a long translation) to fit before cutting it off
        if(kind == Kind.small && textWidth > 0f){
            for(float scale = kind.title; title.getPrefWidth() > Scl.scl(textWidth) && scale > 0.55f; scale -= 0.05f) title.setFontScale(scale - 0.05f);
        }
        if(textWidth > 0f && tag == null) title.setEllipsis(true);
        button.table(texts -> {
            texts.left();
            texts.table(head -> {
                head.left();
                Cell<Label> c = head.add(title).left();
                if(textWidth > 0f && tag == null) c.width(textWidth);
                if(tag != null) head.table(((TextureRegionDrawable)Tex.whiteui).tint(accent), x -> x.add(tag).fontScale(0.55f).color(dark).pad(1f, 6f, 0f, 6f)).padLeft(10f);
            }).left().row();
            if(subtitle != null){
                Label s = texts.add(subtitle).fontScale(kind == Kind.cell ? 0.65f : 0.75f).color(muted).left().get();
                if(textWidth > 0f){
                    texts.getCell(s).width(textWidth + (tag != null ? 60f : 0f));
                    s.setEllipsis(true);
                }
            }
        }).left().padRight(kind.arrow ? 0f : 16f);
        Element arrow = kind.arrow ? button.add(new SizedIcon(Icon.rightOpen)).size(16f).padLeft(14f).padRight(18f).get() : null;
        boolean panelOpener = b.submenu != null && b.submenu.any() && kind != Kind.row;
        float[] slide = {0f};
        button.update(() -> {
            boolean chosen = panelOpener && open == b, lit = button.isOver() || chosen || focused(button);
            button.setChecked(chosen);
            if(arrow != null){
                slide[0] = Mathf.lerpDelta(slide[0], lit ? 6f : 0f, 0.3f);
                arrow.translation.x = Scl.scl(slide[0]); //only the arrow slides out; the button stays put
                arrow.setColor(lit ? accent : muted);
            }
            style.up = lit ? style.over : normal;
            title.setColor(lit ? accent : Color.white);
            image.setColor(lit || kind == Kind.big ? accent : Color.white);
        });
        button.clicked(() -> {
            if(panelOpener) toggle(b);
            else{
                close();
                if(b.runnable != null) b.runnable.run();
            }
        });
        (kind == Kind.row ? panelNav : columnNav).add(button);
        return button;
    }

    static void toggle(MenuButton b){
        if(open == b){
            close();
            return;
        }
        open = b;
        panel.clear();
        panelNav.clear();
        focus = -1;
        if("play".equals(key(b))) PlayPanel.build(panel, b);
        else PlayPanel.generic(panel, b);
        panel.color.a = 0f;
        panel.actions(arc.scene.actions.Actions.alpha(1f, 0.15f));
    }

    static void close(){
        if(open == null) return;
        open = null;
        panel.clear();
        panelNav.clear();
        focus = -1;
    }

    /** A small chamfered action button: icon (optional) and text; gold when hovered or chosen with the keys. */
    static Button action(@Nullable Drawable icon, String text, Runnable run, Seq<Button> nav){
        Button.ButtonStyle style = new Button.ButtonStyle(){{
            up = chamfer(fill, steel, 6f);
            over = down = chamfer(fillLit, accent, 6f);
        }};
        Drawable normal = style.up;
        Button b = new Button(style);
        Element image = icon == null ? null : b.add(new SizedIcon(icon)).size(16f).padLeft(14f).padRight(8f).get();
        Label label = b.add(text).fontScale(0.75f).padLeft(icon == null ? 14f : 0f).padRight(14f).get();
        b.update(() -> {
            boolean lit = b.isOver() || focused(b);
            style.up = lit ? style.over : normal;
            label.setColor(lit ? accent : Color.white);
            if(image != null) image.setColor(lit ? accent : Color.white);
        });
        b.clicked(run);
        nav.add(b);
        return b;
    }

    // ---- news card ----

    static void buildNews(){
        news.clear();
        news.background(chamfer(Color.valueOf("0c0d12ed"), steel, 10f));
        news.margin(18f, 20f, 18f, 20f);
        news.defaults().left();
        LoadedMod mod = mods.getMod("eclipse");
        news.add(Core.bundle.format("eclipse-menu.news.label", mod == null ? "" : mod.meta.version)).fontScale(0.7f).color(accent).row();
        news.add(Core.bundle.get("eclipse-menu.news.title")).wrap().width(384f).padTop(6f).row();
        for(int i = 1; Core.bundle.has("eclipse-menu.news." + i); i++){
            String line = Core.bundle.get("eclipse-menu.news." + i);
            news.table(li -> {
                li.left().top();
                li.image().color(accent).size(7f).padRight(10f).padTop(9f).top();
                li.add(line).fontScale(0.75f).color(light).wrap().width(364f);
            }).padTop(4f).row();
        }
        Seq<Button> none = new Seq<>(); //the news card is not part of the keyboard order
        news.table(row -> {
            row.left();
            row.add(action(Icon.book, Core.bundle.get("eclipse-menu.news.read"), EclipseMenu::showWhatsNew, none)).height(40f).padRight(gap);
            row.add(action(Icon.discord, Core.bundle.get("eclipse-menu.discord"), EclipseMenu::openDiscord, none)).height(40f);
        }).padTop(12f);
    }

    static void openDiscord(){
        if(!Core.app.openURI(discord)) ui.showInfoToast(Core.bundle.get("eclipse-menu.discord-failed"), 3f);
    }

    // ---- footer ----

    static Seq<String> tips(){
        Seq<String> out = new Seq<>();
        for(int i = 0; Core.bundle.has("eclipse-menu.tip." + i); i++) out.add(Core.bundle.get("eclipse-menu.tip." + i));
        return out;
    }

    static void buildFooter(){
        footer.clear();
        footer.background(((TextureRegionDrawable)Tex.whiteui).tint(footerFill));
        footer.image().color(steel).height(1f).growX().colspan(2).top().row();
        //a tip from the wiki, starting on the logo's left edge (kept in step in the layout)
        Seq<String> tips = tips();
        linksCell = footer.table(row -> {
            row.left();
            if(tips.isEmpty()) return;
            row.add(Core.bundle.get("eclipse-menu.tip")).fontScale(0.75f).color(accent).padRight(12f);
            tipLabel = row.add(tips.get(tip % tips.size)).fontScale(0.75f).color(light).maxWidth(Core.graphics.getWidth() / Scl.scl(1f) * 0.55f).get();
            tipLabel.setEllipsis(true);
            link(row, Core.bundle.get("eclipse-menu.tip.next") + " >", null, () -> {
                tip = (tip + 1) % tips.size;
                if(tipLabel != null) tipLabel.setText(tips.get(tip));
            });
        }).growX().padLeft(24f);
        footer.table(right -> {
            right.right();
            //what the news card holds, when there is no room for it
            if(!newsFits){
                link(right, Core.bundle.get("eclipse-menu.footer.whatsnew"), Icon.book, EclipseMenu::showWhatsNew);
                divider(right);
                link(right, Core.bundle.get("eclipse-menu.footer.discord"), Icon.discord, EclipseMenu::openDiscord);
                divider(right);
            }
            link(right, Core.bundle.get("eclipse-menu.footer.credits"), Icon.players, EclipseMenu::showCredits);
            LoadedMod mod = mods.getMod("eclipse");
            //the build date, written by build.ps1 into the mod's files
            String date = null;
            try{
                if(mod != null && mod.root.child("build-date.txt").exists()) date = mod.root.child("build-date.txt").readString().trim();
            }catch(Throwable ignored){
            }
            String version = "ECLIPSE " + (mod == null ? "" : "v" + mod.meta.version) + (date == null || date.isEmpty() ? "" : "  [#a1a1aa]// BUILD " + date);
            right.table(chamfer(chipFill, steel, 4f), v -> v.add(version).fontScale(0.75f).color(accent).pad(0f, 12f, 0f, 12f)).height(28f).padLeft(22f);
        }).padRight(24f);
    }

    static @Nullable Cell<Table> linksCell;
    static float linksPad = -1f;

    /** A footer link: a white icon (optional) and label, gold while hovered. No frame. */
    static void link(Table t, String text, @Nullable Drawable icon, Runnable run){
        Button b = new Button(new Button.ButtonStyle());
        Element image = null;
        if(icon != null){
            image = new SizedIcon(icon);
            b.add(image).size(15f).padRight(8f);
        }
        Label label = new Label(text);
        label.setFontScale(0.75f);
        b.add(label);
        b.clicked(run);
        Element fimage = image;
        b.update(() -> {
            Color c = b.isOver() ? accent : Color.white;
            if(fimage != null) fimage.setColor(c);
            label.setColor(c);
        });
        t.add(b).height(40f).padLeft(18f);
    }

    /** A thin vertical divider between footer links. */
    static void divider(Table t){
        t.image().color(steel).size(1.5f, 14f).padLeft(18f);
    }

    /** The people who helped make ECLIPSE (bundle key eclipse-menu.credits.text, so it can be edited without code). */
    static void showCredits(){
        if(credits == null){
            credits = new BaseDialog("@eclipse-menu.credits");
            credits.addCloseButton();
            credits.cont.pane(p -> p.add(Core.bundle.get("eclipse-menu.credits.text")).wrap().width(Math.min(Core.graphics.getWidth() / Scl.scl(1f) - 80f, 760f)).left())
                .grow();
        }
        credits.show();
    }

    static void showWhatsNew(){
        if(whatsNew == null){
            whatsNew = new BaseDialog("@eclipse-menu.whatsnew");
            whatsNew.addCloseButton();
            whatsNew.cont.pane(p -> p.add(Core.bundle.get("eclipse-menu.whatsnew.text")).wrap().width(Math.min(Core.graphics.getWidth() / Scl.scl(1f) - 80f, 760f)).left())
                .grow();
        }
        whatsNew.show();
    }

    /** Opens the Play panel (photo mode). */
    static void openPlay(){
        MenuButton play = buttons().find(b -> "play".equals(key(b)));
        if(play != null && open != play) toggle(play);
    }
}
