package eclipse.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.mod.Mods.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.ui.fragments.MenuFragment.*;

import static mindustry.Vars.*;

/**
 * The ECLIPSE main menu (desktop, while the ECLIPSE backdrop is on), over the dusk scene and under the ECLIPSE logo:
 * vanilla's own menu buttons (and other mods') as chamfered industrial buttons with an icon box, a title and a short
 * subtitle; hovered or open, the frame turns gold and the arrow slides out. A submenu (Play) opens beside them. A
 * footer holds What's New, the Discord link and the version. Every action is vanilla's own; the logo is drawn by
 * {@link MenuBackdrop}.
 */
public class EclipseMenu{
    /** The ECLIPSE community server, opened from the footer. */
    public static final String discord = "https://discord.gg/tbeqDZed2";

    static final Color accent = Pal.accent, steel = Color.valueOf("282b36"), fill = Color.valueOf("101218f0"), fillLit = Color.valueOf("2a2214f5"),
        iconFill = Color.valueOf("282b3666"), chipFill = Color.valueOf("15171ee6"), footerFill = Color.valueOf("08080acc"), muted = Color.valueOf("a1a1aa");
    static final float buttonWidth = 460f, subWidth = 380f, buttonHeight = 64f;

    static Table column, sub, footer;
    static @Nullable MenuButton open;
    static @Nullable BaseDialog whatsNew;

    /** Hides vanilla's buttons and adds ours and the footer on top of the menu. Call after the backdrop is installed. */
    static void install(Group menu, MenuBackdrop backdrop){
        if(mobile) return;
        Element buttons = menu.find("buttons");
        if(buttons == null){
            Log.warn("[ECLIPSE] menu: vanilla buttons not found, keeping them");
            return;
        }
        Element panel = buttons;
        while(panel.parent != null && panel.parent != menu) panel = panel.parent;
        panel.visible(() -> !on());
        //vanilla's big Discord button (Mindustry's server) in the corner: ours sits in the footer instead
        Element vanillaDiscord = menu.find("discord");
        if(vanillaDiscord != null) vanillaDiscord.visible(() -> !on() && !ui.consolefrag.shown());

        column = new Table();
        sub = new Table();
        footer = new Table();
        for(Table t : new Table[]{column, sub, footer}){
            t.visible(EclipseMenu::on);
            menu.addChild(t);
        }
        rebuild();
        buildFooter();

        //the logo above everything, and the layout of the parts around it
        Element logo = new Element(){
            @Override
            public void draw(){
                backdrop.drawLogo();
            }

            @Override
            public void act(float delta){
                super.act(delta);
                float w = Core.graphics.getWidth(), h = Core.graphics.getHeight();
                setBounds(0f, 0f, w, h);
                footer.setBounds(0f, 0f, w, Scl.scl(44f));
                if(linksCell != null){
                    float pad = backdrop.logoLeftScreen() / Scl.scl(1f);
                    if(Math.abs(linksPad - pad) > 0.5f){
                        linksPad = pad;
                        linksCell.padLeft(pad);
                        footer.invalidate();
                    }
                }
                //the column: lined up with the logo's left edge, halfway between the logo's bottom and the footer
                column.pack();
                float ch = column.getHeight(), left = backdrop.logoLeftScreen(), below = backdrop.logoBottomScreen() - Scl.scl(12f), floor = Scl.scl(56f);
                float topY = Math.min((below + floor) / 2f + ch / 2f, below);
                column.setPosition((int)left, (int)(topY - ch));
                //a submenu opens beside it, level with the top of the column
                sub.pack();
                sub.setPosition((int)(column.x + column.getWidth() + Scl.scl(16f)), (int)(topY - sub.getHeight()));
            }
        };
        logo.touchable = arc.scene.event.Touchable.disabled;
        logo.visible(EclipseMenu::on);
        menu.addChild(logo);
        //vanilla rebuilds its list on resize; follow it
        Events.on(mindustry.game.EventType.ResizeEvent.class, e -> Core.app.post(EclipseMenu::rebuild));
    }

    static boolean on(){
        return Core.settings.getBool(MenuBackdrop.setting, true);
    }

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
        column.clear();
        sub.clear();
        open = null;
        column.defaults().width(buttonWidth).height(buttonHeight).padBottom(9f).left();
        for(MenuButton b : buttons()) column.add(button(b, false, buttonWidth)).row();
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

    /** Vanilla's bundle key of a button ("play" for "@play"), or null for a button with a plain text (another mod's). */
    static final String[] knownKeys = {"play", "campaign", "customgame", "loadgame", "joingame", "tutorial", "database", "schematics", "editor",
        "workshop", "mods", "settings", "about", "quit", "be"};

    static @Nullable String key(MenuButton b){
        if(b.text == null) return null;
        if(b.text.startsWith("@")) return b.text.substring(1);
        for(String k : knownKeys) if(Core.bundle.has(k) && Core.bundle.get(k).equals(b.text)) return k;
        return null;
    }

    static String text(MenuButton b){
        return b.text.startsWith("@") ? Core.bundle.get(b.text.substring(1)) : b.text;
    }

    static Button button(MenuButton b, boolean inSub, float width){
        String key = key(b);
        Button.ButtonStyle style = new Button.ButtonStyle(){{
            up = chamfer(fill, steel, 10f);
            over = chamfer(fillLit, accent, 10f);
            down = chamfer(fillLit, accent, 10f);
            checked = chamfer(fillLit, accent, 10f);
        }};
        //no hover look while a dialog covers the menu: the button never sees the mouse leave
        boolean primary = "play".equals(key) && !inSub;
        Button button = new Button(style){
            @Override
            public boolean isOver(){
                return super.isOver() && !Core.scene.hasDialog();
            }

            @Override
            public void draw(){
                super.draw();
                //PLAY: a gold bar on its left edge, always, so the main action anchors the stack
                if(primary){
                    float c = Scl.scl(10f);
                    Draw.color(accent, parentAlpha * color.a);
                    Fill.crect(x + translation.x + Scl.scl(1.5f), y + translation.y + c, Scl.scl(3f), height - c * 2f);
                    Draw.reset();
                }
            }
        };
        button.left();
        Table icon = new Table(chamfer(iconFill, steel, 5f));
        Image image = icon.image(b.icon).size(22f).get();
        button.add(icon).size(40f).padLeft(12f).padRight(14f);
        //title and subtitle get the room between the icon and the arrow; a long subtitle is cut with "..." instead of running under the arrow
        float textWidth = width - 12f - 40f - 14f - 16f - 18f - 14f;
        String subtitle = key != null && Core.bundle.has("eclipse-menu.sub." + key) ? Core.bundle.get("eclipse-menu.sub." + key) : null;
        Label title = new Label(text(b).toUpperCase());
        title.setEllipsis(true);
        button.table(texts -> {
            texts.left();
            texts.add(title).width(textWidth).left().row();
            if(subtitle != null){
                Label s = texts.add(subtitle).fontScale(0.75f).color(muted).width(textWidth).left().get();
                s.setEllipsis(true);
            }
        }).width(textWidth).left();
        Image arrow = button.image(Icon.rightOpen).size(16f).padLeft(14f).padRight(18f).get();
        float[] slide = {0f};
        button.update(() -> {
            boolean lit = button.isOver() || (open == b && !inSub);
            button.setChecked(open == b && !inSub);
            slide[0] = Mathf.lerpDelta(slide[0], lit ? 6f : 0f, 0.3f);
            arrow.translation.x = Scl.scl(slide[0]); //only the arrow slides out; the button stays put
            title.setColor(lit ? accent : Color.white);
            image.setColor(lit ? accent : Color.white);
            arrow.setColor(lit ? accent : muted);
        });
        button.clicked(() -> {
            if(b.submenu != null && b.submenu.any() && !inSub){
                toggle(b);
            }else{
                close();
                if(b.runnable != null) b.runnable.run();
            }
        });
        return button;
    }

    static void toggle(MenuButton b){
        if(open == b){
            close();
            return;
        }
        open = b;
        sub.clear();
        sub.defaults().width(subWidth).height(buttonHeight).padBottom(9f).left();
        for(MenuButton s : b.submenu) if(s != null) sub.add(button(s, true, subWidth)).row();
        sub.color.a = 0f;
        sub.actions(arc.scene.actions.Actions.alpha(1f, 0.15f));
    }

    static void close(){
        open = null;
        sub.clear();
    }

    // ---- footer ----

    static void buildFooter(){
        footer.clear();
        footer.background(((TextureRegionDrawable)Tex.whiteui).tint(footerFill));
        footer.image().color(steel).height(1f).growX().colspan(2).top().row();
        //plain links (icon + label) separated by thin dividers; the version badge stays the bar's only framed element.
        //They start on the left edge of the logo and button column (kept in step in the layout).
        linksCell = footer.table(links -> {
            links.left();
            link(links, "eclipse-menu.footer.whatsnew", Icon.book, EclipseMenu::showWhatsNew);
            divider(links);
            link(links, "eclipse-menu.footer.discord", Icon.discord, () -> {
                if(!Core.app.openURI(discord)) ui.showInfoToast(Core.bundle.get("eclipse-menu.discord-failed"), 3f);
            });
            divider(links);
            link(links, "eclipse-menu.footer.credits", Icon.players, EclipseMenu::showCredits);
        }).growX().padLeft(24f);
        LoadedMod mod = mods.getMod("eclipse");
        //the build date, written by build.ps1 into the mod's files
        String date = null;
        try{
            if(mod != null && mod.root.child("build-date.txt").exists()) date = mod.root.child("build-date.txt").readString().trim();
        }catch(Throwable ignored){
        }
        String version = "ECLIPSE " + (mod == null ? "" : "v" + mod.meta.version) + (date == null || date.isEmpty() ? "" : "  [#a1a1aa]// BUILD " + date);
        footer.table(chamfer(chipFill, steel, 4f), v -> v.add(version).fontScale(0.75f).color(accent).pad(0f, 12f, 0f, 12f))
            .height(28f).padRight(24f);
    }

    static @Nullable Cell<Table> linksCell;
    static float linksPad = -1f;

    /** A footer link: a white icon and label, gold while hovered. No frame. */
    static void link(Table t, String key, Drawable icon, Runnable run){
        Button b = new Button(new Button.ButtonStyle());
        Image image = new Image(icon);
        image.setScaling(Scaling.fit);
        Label label = new Label(Core.bundle.get(key));
        label.setFontScale(0.75f);
        b.add(image).size(14f).padRight(8f);
        b.add(label);
        b.clicked(run);
        b.update(() -> {
            Color c = b.isOver() ? accent : Color.white;
            image.setColor(c);
            label.setColor(c);
        });
        t.add(b).height(40f);
    }

    /** A thin vertical divider between footer links. */
    static void divider(Table t){
        t.image().color(steel).size(1.5f, 14f).padLeft(22f).padRight(22f);
    }

    static @Nullable BaseDialog credits;

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
}
