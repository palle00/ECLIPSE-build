package eclipse.ui;

import arc.*;
import arc.graphics.*;
import arc.scene.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.fragments.MenuFragment.*;

import static mindustry.Vars.*;

/**
 * The ECLIPSE main menu buttons (desktop, while the ECLIPSE backdrop is on): a column of separate dark buttons under the
 * ECLIPSE logo (starting where the logo starts, halfway down the space below it), replacing vanilla's button strip. The buttons themselves come from vanilla's own list
 * ({@code MenuFragment.desktopButtons} plus buttons other mods added), so every action and submenu stays vanilla's;
 * a submenu (Play) opens as a second column beside it. The logo is drawn on top of everything.
 */
public class EclipseMenu{
    static final Color up = new Color(0.07f, 0.06f, 0.13f, 0.88f), over = new Color(0.22f, 0.16f, 0.38f, 0.95f),
        down = Color.valueOf("ff9a3c").a(0.9f), edge = Color.valueOf("ff9a3c");
    static TextButton.TextButtonStyle style;

    static Table column, sub;
    static @Nullable MenuButton open;

    /** Hides vanilla's buttons and adds ours and the logo on top of the menu. Call after the backdrop is installed. */
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

        TextureRegionDrawable white = (TextureRegionDrawable)Tex.whiteui;
        style = new TextButton.TextButtonStyle(){{
            this.up = white.tint(EclipseMenu.up);
            this.over = white.tint(EclipseMenu.over);
            this.down = white.tint(EclipseMenu.down);
            font = Fonts.def;
            fontColor = Color.white;
            disabledFontColor = Color.gray;
        }};

        column = new Table();
        sub = new Table();
        column.visible(EclipseMenu::on);
        sub.visible(EclipseMenu::on);
        rebuild();
        menu.addChild(column);
        menu.addChild(sub);

        //the logo and version line above everything else
        Element logo = new Element(){
            @Override
            public void draw(){
                backdrop.drawLogo();
            }

            @Override
            public void act(float delta){
                super.act(delta);
                setBounds(0f, 0f, Core.graphics.getWidth(), Core.graphics.getHeight());
                //the column: lined up with the logo's left edge, and halfway between the logo's bottom and the bottom of the screen
                column.pack();
                float h = column.getHeight(), left = backdrop.logoLeftScreen(), below = backdrop.logoBottomScreen() - Scl.scl(12f);
                float topY = Math.min(below / 2f + h / 2f, below);
                column.setPosition((int)left, (int)(topY - h));
                //a submenu opens beside it, level with the button that opened it
                sub.pack();
                sub.setPosition((int)(column.x + column.getWidth() + Scl.scl(10f)), (int)(topY - sub.getHeight()));
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
    @SuppressWarnings("unchecked")
    static Seq<MenuButton> buttons(){
        Seq<MenuButton> all = new Seq<>();
        if(ui.menufrag.desktopButtons != null) all.addAll(ui.menufrag.desktopButtons);
        try{
            Seq<MenuButton> custom = Reflect.get(ui.menufrag, "customButtons");
            if(custom != null) all.addAll(custom);
        }catch(Throwable ignored){
        }
        all.add(new MenuButton("@quit", Icon.exit, Core.app::exit));
        return all;
    }

    static void rebuild(){
        column.clear();
        sub.clear();
        open = null;
        column.defaults().width(300f).height(58f).padBottom(8f).left();
        for(MenuButton b : buttons()) column.add(button(b, false)).row();
    }

    static TextButton button(MenuButton b, boolean inSub){
        //no hover look while a dialog covers the menu: the button never sees the mouse leave
        TextButton button = new TextButton(b.text, style){
            @Override
            public boolean isOver(){
                return super.isOver() && !Core.scene.hasDialog();
            }
        };
        button.clearChildren();
        button.left();
        button.image(b.icon).size(30f).padLeft(16f).padRight(18f).color(Color.white);
        button.add(button.getLabel()).left().growX();
        button.getLabel().setAlignment(Align.left);
        //an orange edge on the left while hovered or open
        button.fill((x, y, w, h) -> {
            boolean lit = button.isOver() || (open == b && !inSub);
            if(!lit) return;
            arc.graphics.g2d.Draw.color(edge);
            arc.graphics.g2d.Fill.crect(x, y, Scl.scl(4f), h);
            arc.graphics.g2d.Draw.reset();
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
        sub.defaults().width(260f).height(58f).padBottom(8f).left();
        for(MenuButton s : b.submenu) sub.add(button(s, true)).row();
        sub.color.a = 0f;
        sub.actions(arc.scene.actions.Actions.alpha(1f, 0.15f));
    }

    static void close(){
        open = null;
        sub.clear();
    }
}
