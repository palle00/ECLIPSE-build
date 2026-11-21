package eclipse.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.content.*;
import mindustry.game.Saves.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.fragments.MenuFragment.*;
import mindustry.world.*;

import static eclipse.ui.EclipseMenu.*;
import static mindustry.Vars.*;

/**
 * The main menu's Play panel, beside the button column: a Campaign card with every planet you can play (its enemy
 * faction, the sectors you hold, where your last game is) and ECLIPSE research progress, then cards for Custom Game,
 * Load Game (the three newest saves, one click to load) and Join Game. Each runs vanilla's own submenu action; entries
 * it doesn't know (another mod's) get a button at the bottom. Other mods' buttons with a submenu get a plain list.
 */
class PlayPanel{
    /** The panel's inner width, and the gap between cards. */
    static final float width = 1020f, cardGap = 12f;
    static final Color planetFill = Color.valueOf("0c0d12e6"), hereFrame = Color.valueOf("ffd37f99"), dim = Color.valueOf("6e6e7e");

    static void build(Table t, MenuButton play){
        frame(t, text(play));
        MenuButton campaign = find(play, "campaign"), custom = find(play, "customgame"), load = find(play, "loadgame"), join = find(play, "joingame");
        if(campaign != null) t.add(campaign(campaign)).width(width).padBottom(cardGap).row();
        Seq<MenuButton> cards = Seq.with(custom, load, join).select(b -> b != null);
        if(cards.any()){
            float w = (width - cardGap * (cards.size - 1)) / cards.size;
            Table row = new Table();
            row.top().left();
            for(int i = 0; i < cards.size; i++){
                MenuButton b = cards.get(i);
                Table card = b == load ? loadCard(b, w) : b == custom ? textCard(b, Icon.map, w, "eclipse-menu.custom.tip", "eclipse-menu.custom.go")
                    : textCard(b, Icon.players, w, "eclipse-menu.join.pvp", "eclipse-menu.join.go");
                row.add(card).width(w).growY().padRight(i < cards.size - 1 ? cardGap : 0f);
            }
            t.add(row).width(width).row();
        }
        //anything else in the submenu: another mod's entries, the tutorial
        Seq<MenuButton> rest = play.submenu.select(s -> s != null && s != campaign && s != custom && s != load && s != join);
        if(rest.any()){
            t.table(more -> {
                more.left();
                for(MenuButton s : rest) more.add(action(s.icon, text(s), () -> run(s), panelNav)).height(40f).padRight(8f);
            }).left().padTop(cardGap).row();
        }
    }

    /** Another mod's button with a submenu: its entries as a list of menu buttons. */
    static void generic(Table t, MenuButton b){
        frame(t, text(b));
        for(MenuButton s : b.submenu) if(s != null) t.add(button(s, Kind.row, 380f, null)).size(380f, Kind.row.height).padBottom(8f).row();
    }

    static @Nullable MenuButton find(MenuButton b, String key){
        return b.submenu.find(s -> s != null && key.equals(key(s)));
    }

    static void run(MenuButton s){
        close();
        if(s.runnable != null) s.runnable.run();
    }

    /** The panel's frame and header: its title, the Escape hint and a close button. */
    static void frame(Table t, String title){
        t.background(chamfer(panelFill, steel, 12f));
        t.margin(20f, 22f, 22f, 22f);
        t.top().left();
        t.defaults().left();
        t.table(h -> {
            h.left();
            h.add(title.toUpperCase()).fontScale(1.3f).color(accent);
            h.add().growX();
            h.add(Core.bundle.get("eclipse-menu.panel.esc")).fontScale(0.7f).color(muted).padRight(14f);
            h.button(Icon.cancel, Styles.cleari, EclipseMenu::close).size(40f);
        }).width(width).padBottom(14f).row();
    }

    /** A card: an icon box, the title and (optional) a line under it. */
    static Table card(Drawable icon, String title, @Nullable String sub, float w){
        Table c = new Table(chamfer(cardFill, steel, 10f));
        c.top().left().margin(16f, 18f, 16f, 18f);
        c.defaults().left();
        float inner = w - 36f;
        c.table(h -> {
            h.left();
            h.table(chamfer(iconFill, steel, 5f), i -> i.add(new SizedIcon(icon)).size(20f).color(accent)).size(36f).padRight(12f);
            h.table(tx -> {
                tx.left().defaults().left();
                tx.add(title.toUpperCase());
                if(sub != null) tx.row().add(sub).fontScale(0.7f).color(muted).wrap().width(inner - 48f);
            }).growX();
        }).width(inner).row();
        return c;
    }

    // ---- campaign ----

    static Table campaign(MenuButton campaign){
        Table c = card(Icon.planet, text(campaign), subtitle(campaign), width);
        float inner = width - 36f;
        //Serpulo and Erekir first, then the others (ECLIPSE's, other mods') in their order
        Seq<Planet> planets = content.planets().select(p -> p.accessible && p.visible() && p.hasGrid() && p.sectors.any())
            .sort(p -> p == Planets.serpulo ? 0 : p == Planets.erekir ? 1 : 2);
        Sector here = last != null && last.isSector() ? last.getSector() : null;
        int perRow = 5;
        float w = (inner - 10f * (perRow - 1)) / perRow;
        Table grid = new Table();
        grid.left();
        for(int i = 0; i < planets.size; i++){
            Planet p = planets.get(i);
            grid.add(planet(p, campaign, here != null && here.planet == p)).size(w, 190f).padRight(i % perRow < perRow - 1 ? 10f : 0f).padBottom(10f);
            if(i % perRow == perRow - 1) grid.row();
        }
        c.add(grid).left().padTop(12f).row();
        //research is shared between planets: one bar for all of ECLIPSE's blocks in the tech tree
        int total = 0, done = 0;
        for(Block b : content.blocks()){
            if(!b.name.startsWith("eclipse-") || b.techNode == null) continue;
            total++;
            if(b.unlocked()) done++;
        }
        if(total > 0){
            int ft = total, fd = done;
            c.table(r -> {
                r.left();
                r.add(Core.bundle.get("eclipse-menu.research")).fontScale(0.75f).color(light).padRight(14f);
                r.add(new ProductionDialog.Bar(fd / (float)ft, WikiUI.good)).size(420f, 8f).padRight(14f);
                r.add(Core.bundle.format("eclipse-menu.research.count", fd, ft)).fontScale(0.75f).color(light);
            }).left().padTop(2f);
        }
        return c;
    }

    /** A planet: drawn small, its name, its enemy faction, and how many sectors you hold (or that your last game is there). */
    static Button planet(Planet p, MenuButton campaign, boolean here){
        Button.ButtonStyle style = new Button.ButtonStyle(){{
            up = chamfer(planetFill, here ? hereFrame : steel, 8f);
            over = down = chamfer(fillLit, accent, 8f);
        }};
        Drawable normal = style.up;
        Button b = new Button(style);
        b.top();
        b.add(new Image(pixelPlanet(p))).size(96f).padTop(10f).row();
        Label name = b.add(p.localizedName).fontScale(0.95f).padTop(6f).get();
        b.row();
        b.add(Core.bundle.has("eclipse-menu.planet." + p.name) ? Core.bundle.get("eclipse-menu.planet." + p.name) : "").fontScale(0.65f).color(muted).row();
        if(here){
            b.table(((TextureRegionDrawable)Tex.whiteui).tint(accent), x -> x.add(Core.bundle.get("eclipse-menu.planet.here")).fontScale(0.55f).color(dark).pad(1f, 6f, 0f, 6f)).padTop(4f);
        }else{
            int held = p.sectors.count(Sector::hasBase);
            String text = held == 0 ? Core.bundle.get("eclipse-menu.planet.none") : held == 1 ? Core.bundle.get("eclipse-menu.planet.one") : Core.bundle.format("eclipse-menu.planet.held", held);
            b.add(text).fontScale(0.65f).color(held > 0 ? WikiUI.good : dim).padTop(2f);
        }
        b.update(() -> {
            boolean lit = b.isOver() || focused(b);
            style.up = lit ? style.over : normal;
            name.setColor(lit ? accent : Color.white);
        });
        //the planet view, opened on this planet (vanilla's Campaign button, with its own checks)
        b.clicked(() -> {
            close();
            ui.planet.state.planet = p;
            if(campaign.runnable != null) campaign.runnable.run();
        });
        panelNav.add(b);
        return b;
    }

    /**
     * Each planet's look as 32x32 pixel art: noise mapped to the planet's colours (land and sea, rust, spores, slag and
     * lava, islands), lit from the upper right. Planets without colours here (another mod's) use shades of their icon colour.
     */
    static final ObjectMap<String, Object[]> palettes = ObjectMap.of(
        "serpulo", new Object[]{3, 0.11f, new float[]{0.46f, 0.52f, 0.6f, 0.7f, 0.8f, 1f}, new String[]{"2f5a8a", "3b6fa0", "5e8b4a", "4c7a3c", "7f8a8f", "a4aab0"}},
        "erekir", new Object[]{9, 0.12f, new float[]{0.4f, 0.55f, 0.68f, 0.8f, 1f}, new String[]{"4a2622", "6e3528", "8f4630", "b45c38", "d07a48"}},
        "eclipse-hive", new Object[]{12, 0.13f, new float[]{0.38f, 0.5f, 0.6f, 0.7f, 0.82f, 1f}, new String[]{"2c1f3d", "4b2f66", "6e4590", "3f6b33", "5a8a3c", "9cc25a"}},
        "eclipse-ironhold", new Object[]{21, 0.125f, new float[]{0.35f, 0.5f, 0.62f, 0.72f, 0.78f, 1f}, new String[]{"211c1e", "332a2c", "4a3530", "6b3e2c", "b0542a", "f08a3a"}},
        "eclipse-tide", new Object[]{5, 0.1f, new float[]{0.5f, 0.6f, 0.66f, 0.7f, 0.8f, 1f}, new String[]{"123257", "1b4673", "26608f", "d2c290", "4f8a4f", "3c6e3c"}}
    );
    static final ObjectMap<Planet, TextureRegion> pixelPlanets = new ObjectMap<>();

    static TextureRegion pixelPlanet(Planet p){
        return pixelPlanets.get(p, () -> {
            Object[] pal = palettes.get(p.name);
            int seed = pal == null ? p.id : (int)pal[0];
            float scale = pal == null ? 0.12f : (float)pal[1];
            float[] stops = pal == null ? new float[]{0.4f, 0.5f, 0.6f, 0.7f, 1f} : (float[])pal[2];
            Color[] colors = new Color[stops.length];
            Color base = p.iconColor == null ? Color.gray : p.iconColor;
            for(int i = 0; i < colors.length; i++){
                colors[i] = pal == null ? base.cpy().mul(0.35f + 0.65f * i / (colors.length - 1f)).a(1f) : Color.valueOf(((String[])pal[3])[i]);
            }
            Pixmap pix = new Pixmap(32, 32);
            for(int y = 0; y < 32; y++){
                for(int x = 0; x < 32; x++){
                    float dx = (x + 0.5f - 16f) / 15f, dy = (y + 0.5f - 16f) / 15f, d2 = dx * dx + dy * dy;
                    if(d2 > 1f){
                        pix.set(x, y, 0);
                        continue;
                    }
                    float v = Simplex.noise2d(seed, 3, 0.5, scale, x, y);
                    Color c = colors[colors.length - 1];
                    for(int i = 0; i < stops.length; i++){
                        if(v < stops[i]){
                            c = colors[i];
                            break;
                        }
                    }
                    //the pixmap's y runs down: light from the upper right
                    float nz = (float)Math.sqrt(1f - d2), light = Math.max(0.18f, Math.min(1.15f, 0.25f + 0.9f * (dx * 0.55f - dy * 0.45f + nz * 0.7f)));
                    pix.set(x, y, Color.rgba8888(Math.min(1f, c.r * light), Math.min(1f, c.g * light), Math.min(1f, c.b * light), 1f));
                }
            }
            Texture tex = new Texture(pix);
            tex.setFilter(Texture.TextureFilter.nearest);
            pix.dispose();
            return new TextureRegion(tex);
        });
    }

    // ---- the other cards ----

    /** Custom Game and Join Game: what it is, one more line, and the button that runs it. */
    static Table textCard(MenuButton b, Drawable icon, float w, String noteKey, String goKey){
        Table c = card(icon, text(b), null, w);
        float inner = w - 36f;
        String sub = subtitle(b);
        if(sub != null) c.add(sub).fontScale(0.75f).color(light).wrap().width(inner).padTop(12f).row();
        c.add(Core.bundle.get(noteKey)).fontScale(0.75f).color(light).wrap().width(inner).padTop(8f).row();
        c.add().growY().row();
        c.add(action(null, Core.bundle.get(goKey) + " >", () -> run(b), panelNav)).height(40f);
        return c;
    }

    /** Load Game: the three newest saves (campaign sectors are on the planet cards), then the full list. */
    static Table loadCard(MenuButton load, float w){
        Table c = card(Icon.folder, text(load), null, w);
        float inner = w - 36f;
        Seq<SaveSlot> recent = control.saves.getSaveSlots().select(s -> !s.isHidden() && s.meta != null)
            .sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        if(recent.isEmpty()) c.add(Core.bundle.get("eclipse-menu.load.none")).fontScale(0.75f).color(muted).padTop(12f).row();
        for(int i = 0; i < Math.min(3, recent.size); i++) c.add(saveRow(recent.get(i), inner)).size(inner, 50f).padTop(i == 0 ? 12f : 4f).row();
        c.add().growY().row();
        c.add(action(null, Core.bundle.get("eclipse-menu.load.all") + " >", () -> run(load), panelNav)).height(40f).padTop(8f);
        return c;
    }

    static Button saveRow(SaveSlot s, float w){
        Button.ButtonStyle style = new Button.ButtonStyle(){{
            up = ((TextureRegionDrawable)Tex.whiteui).tint(1f, 1f, 1f, 0.03f);
            over = down = ((TextureRegionDrawable)Tex.whiteui).tint(1f, 0.83f, 0.5f, 0.1f);
        }};
        Drawable normal = style.up;
        Button b = new Button(style);
        b.left().margin(4f, 8f, 4f, 8f);
        Label name = new Label(s.getName());
        name.setFontScale(0.75f);
        name.setEllipsis(true);
        String when = ago(System.currentTimeMillis(), s.getTimestamp()), time = playtime(s.meta.timePlayed);
        String detail = s.meta.rules != null && s.meta.rules.waves ? Core.bundle.format("eclipse-menu.load.row", s.getWave(), time) : time;
        b.table(t -> {
            t.left();
            t.add(name).width(w - 16f - 110f).left();
            t.add(when).fontScale(0.6f).color(dim).right().growX();
            t.row();
            t.add(detail).fontScale(0.6f).color(muted).left().colspan(2);
        }).grow();
        b.update(() -> {
            boolean lit = b.isOver() || focused(b);
            style.up = lit ? style.over : normal;
            name.setColor(lit ? accent : Color.white);
        });
        b.clicked(() -> {
            close();
            checkPlay(() -> ui.load.runLoadSave(s));
        });
        panelNav.add(b);
        return b;
    }
}
