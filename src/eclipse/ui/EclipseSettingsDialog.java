package eclipse.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.ui.dialogs.*;
import mindustry.ui.dialogs.SettingsMenuDialog.SettingsTable.*;

import static eclipse.ui.EclipseMenu.*;
import static mindustry.Vars.*;

/**
 * The settings screen of the ECLIPSE pause menu (mockup approved by the user 2026-11-25): a tab rail on the left with the
 * ECLIPSE tab first, then vanilla's own Game, Graphics and Sound tables shown in the ECLIPSE frame (the same tables
 * vanilla's settings dialog shows; it takes them back when it opens), and Controls, Language and Data, which open vanilla's
 * dialogs. The ECLIPSE tab lists {@link EclipseSettings}, a search box filters it.
 */
public class EclipseSettingsDialog extends BaseDialog{
    public static EclipseSettingsDialog instance;

    static final Color rail = Color.valueOf("08080a"), ground = Color.valueOf("0a0b0f"), line = Color.valueOf("1b1d25"),
        description = Color.valueOf("9a9aa6"), offTrack = Color.valueOf("3a3d4a"), knob = Color.valueOf("0c0d12");
    static final String[] tabs = {"eclipse", "game", "graphics", "sound", "controls", "language", "data"};

    String tab = "eclipse";
    final Table nav = new Table(), content = new Table();
    final Label title = new Label(""), subtitle = new Label("");
    final TextField search = new TextField("");

    public EclipseSettingsDialog(){
        super("");
        titleTable.remove();
        closeOnBack();
        cont.clear();
        cont.background(tint(ground));

        nav.background(tint(rail));
        cont.add(nav).width(260f).growY();
        cont.table(main -> {
            main.top().left();
            main.table(head -> {
                head.left();
                head.table(t -> {
                    t.left();
                    title.setColor(accent);
                    EclipseFonts.crisp(title, 1.3f);
                    t.add(title).left().row();
                    subtitle.setColor(muted);
                    EclipseFonts.crisp(subtitle, 0.75f);
                    t.add(subtitle).left();
                }).growX().left();
                search.setMessageText(Core.bundle.get("eclipse-settings.search"));
                search.changed(this::rebuildContent);
                head.add(search).width(300f).height(44f);
            }).growX().pad(32f, 44f, 14f, 52f).row();
            ScrollPane pane = new ScrollPane(content, mindustry.ui.Styles.smallPane);
            pane.setFadeScrollBars(false);
            main.add(pane).grow().padLeft(44f).padRight(36f).padBottom(20f);
        }).grow();
        FitDialog.install(this);

        shown(() -> {
            search.setText("");
            show(tab);
        });
    }

    public static void install(){
        if(headless) return;
        instance = new EclipseSettingsDialog();
    }

    static Drawable tint(Color c){
        return ((TextureRegionDrawable)Tex.whiteui).tint(c);
    }

    void show(String id){
        if(!id.equals(tab)) search.setText(""); //a new tab starts unfiltered
        tab = id;
        rebuildNav();
        boolean eclipse = id.equals("eclipse");
        title.setText(eclipse ? Core.bundle.get("eclipse-settings.title") : Core.bundle.get("eclipse-settings.tab." + id));
        subtitle.setText(Core.bundle.get(eclipse ? "eclipse-settings.subtitle" : "eclipse-settings.vanilla"));
        rebuildContent();
    }

    void rebuildNav(){
        nav.clear();
        nav.top().left().margin(36f, 36f, 24f, 18f);
        EclipseFonts.crisp(nav.add(Core.bundle.get("eclipse-settings.rail")), 0.65f).color(muted).left().padBottom(10f).row();
        for(String id : tabs){
            boolean opensDialog = id.equals("controls") || id.equals("language") || id.equals("data");
            String hint = id.equals("eclipse") ? Core.bundle.format("eclipse-settings.count", EclipseSettings.all.size) : opensDialog ? "↗" : null;
            nav.add(tabButton(Core.bundle.get("eclipse-settings.tab." + id), hint, tab.equals(id), () -> {
                if(id.equals("controls")) ui.controls.show();
                else if(id.equals("language")) ui.language.show();
                else if(id.equals("data")) showData();
                else show(id);
            })).growX().height(50f).padBottom(8f).row();
        }
        nav.add().growY().row();
        nav.add(tabButton(Core.bundle.get("back"), "Esc", false, this::hide)).growX().height(50f);
    }

    static void showData(){
        try{
            BaseDialog data = Reflect.get(SettingsMenuDialog.class, ui.settings, "dataDialog");
            data.show();
        }catch(Throwable t){
            ui.settings.show(); //a game update renamed the field: vanilla's own settings still reach it
        }
    }

    /** A rail button: chamfered, gold when chosen or hovered, with a small hint on the right. */
    static Button tabButton(String text, @Nullable String hint, boolean chosen, Runnable run){
        Button.ButtonStyle style = new Button.ButtonStyle(){{
            up = chamfer(fill, steel, 10f);
            over = down = checked = chamfer(fillLit, accent, 10f);
        }};
        Drawable normal = style.up;
        Button b = new Button(style);
        b.left();
        Label label = EclipseFonts.crisp(b.add(text), 0.9f).left().growX().padLeft(16f).get();
        if(hint != null) EclipseFonts.crisp(b.add(hint), 0.6f).color(muted).padRight(14f);
        b.update(() -> {
            boolean lit = chosen || b.isOver();
            style.up = lit ? style.over : normal;
            label.setColor(lit ? accent : Color.white);
        });
        b.clicked(run);
        return b;
    }

    void rebuildContent(){
        content.clear();
        content.top().left();
        switch(tab){
            case "game" -> buildVanilla(ui.settings.game);
            case "graphics" -> buildVanilla(ui.settings.graphics);
            case "sound" -> buildVanilla(ui.settings.sound);
            default -> buildEclipse();
        }
    }

    // ---- vanilla's tabs, drawn as ECLIPSE rows (user, 2026-11-25) ----

    /**
     * Vanilla's Game, Graphics or Sound settings as ECLIPSE rows: name and description, a switch or a slider. Built from
     * the settings vanilla's own table lists, with their defaults, ranges, value labels and change handlers (package-private
     * in vanilla, read by reflection), so every option behaves exactly as in vanilla's dialog.
     */
    void buildVanilla(SettingsMenuDialog.SettingsTable table){
        String q = search.getText().trim().toLowerCase();
        int shown = 0;
        for(Setting s : table.getSettings()){
            String title = s.title == null ? s.name : s.title, desc = s.description == null ? "" : s.description;
            if(!q.isEmpty() && !(title + " " + desc).toLowerCase().contains(q)) continue;
            shown++;
            try{
                if(s instanceof CheckSetting c){
                    boolean def = Reflect.get(c, "def");
                    Boolc changed = Reflect.get(c, "changed");
                    row(title, desc, t -> t.add(new Switch(() -> Core.settings.getBool(s.name, def), v -> {
                        Core.settings.put(s.name, v);
                        if(changed != null) changed.get(v);
                    })).size(56f, 30f));
                }else if(s instanceof SliderSetting sl){
                    int def = Reflect.get(sl, "def"), min = Reflect.get(sl, "min"), max = Reflect.get(sl, "max"), step = Reflect.get(sl, "step");
                    SettingsMenuDialog.StringProcessor sp = Reflect.get(sl, "sp");
                    Intc changed = Reflect.get(sl, "changed");
                    row(title, desc, t -> slider(t, min, max, step, Core.settings.getInt(s.name, def), v -> sp == null ? String.valueOf(v) : sp.get(v), v -> {
                        Core.settings.put(s.name, v);
                        if(changed != null) changed.get(v);
                    }));
                }else if(s instanceof TextSetting ts){
                    String def = Reflect.get(TextSetting.class, ts, "def");
                    Cons<String> changed = Reflect.get(TextSetting.class, ts, "changed");
                    row(title, desc, t -> {
                        TextField field = new TextField(Core.settings.getString(s.name, def));
                        field.changed(() -> {
                            Core.settings.put(s.name, field.getText());
                            if(changed != null) changed.get(field.getText());
                        });
                        t.add(field).width(280f).height(40f);
                    });
                }else{
                    //a kind of setting this screen does not know (another mod's): vanilla's own row
                    vanillaRow(s);
                }
            }catch(Throwable e){
                //a game update renamed a field: fall back to vanilla's own row for this setting
                Log.err("[ECLIPSE] settings: could not read " + s.name, e);
                vanillaRow(s);
            }
        }
        if(shown == 0){
            content.add(Core.bundle.get("eclipse-settings.none")).color(muted).left().padTop(20f).row();
            return;
        }
        if(q.isEmpty()){
            section("defaults");
            actionRow("eclipse-settings.vanilla-reset", Core.bundle.get("eclipse-settings.reset.button"), () ->
                ui.showConfirm("@eclipse-settings.vanilla-reset.name", "@eclipse-settings.vanilla-reset.confirm", () -> {
                    //as vanilla's own "Reset to Defaults": every setting of this tab back to its registered default
                    for(Setting s : table.getSettings()){
                        Object def = Core.settings.getDefault(s.name);
                        if(def != null) Core.settings.put(s.name, def);
                    }
                    rebuildContent();
                }));
        }
    }

    static @Nullable Slider.SliderStyle sliderStyle;

    /** The ECLIPSE slider (as in the mockup): a thin steel track, gold up to the knob, a gold chamfered knob. */
    static Slider.SliderStyle sliderStyle(){
        if(sliderStyle != null) return sliderStyle;
        BaseDrawable track = bar(offTrack), filled = bar(accent);
        BaseDrawable knobUp = (BaseDrawable)chamfer(accent, accent, 3f), knobLit = (BaseDrawable)chamfer(Color.white, accent, 3f);
        for(BaseDrawable d : new BaseDrawable[]{knobUp, knobLit}){
            d.setMinWidth(14f);
            d.setMinHeight(24f);
        }
        sliderStyle = new Slider.SliderStyle(){{
            background = track;
            knobBefore = filled;
            knob = knobUp;
            knobOver = knobDown = knobLit;
        }};
        return sliderStyle;
    }

    /** A thin bar centred in whatever height the slider gives it (the engine stretches a track to the slider's height). */
    static BaseDrawable bar(Color color){
        BaseDrawable d = new BaseDrawable(){
            @Override
            public void draw(float x, float y, float w, float h){
                float t = Scl.scl(6f), a = Draw.getColor().a;
                Draw.color(color, color.a * a);
                Fill.crect(x, y + (h - t) / 2f, w, t);
                Draw.color(1f, 1f, 1f, a);
            }
        };
        d.setMinHeight(6f);
        return d;
    }

    /** A setting drawn by vanilla itself, in a table of its own. */
    void vanillaRow(Setting s){
        SettingsMenuDialog.SettingsTable own = new SettingsMenuDialog.SettingsTable();
        s.add(own);
        content.add(own).growX().left().row();
    }

    /** A slider with its value on the right; onChange gets the new value (already rounded to the step). */
    static void slider(Table t, int min, int max, int step, int value, Func<Integer, String> label, Intc onChange){
        Slider slider = new Slider(min, max, step, false, sliderStyle());
        slider.setValue(value);
        Label text = new Label(label.get(value));
        text.setColor(light);
        slider.moved(v -> {
            int i = Math.round(v);
            text.setText(label.get(i));
            onChange.get(i);
        });
        t.add(slider).width(220f).padRight(12f);
        t.add(text).minWidth(52f).right();
    }

    // ---- the ECLIPSE tab ----

    void buildEclipse(){
        String q = search.getText().trim().toLowerCase();
        boolean filtering = !q.isEmpty();
        int shown = 0;
        for(String section : EclipseSettings.sections){
            Seq<EclipseSettings.Setting> rows = EclipseSettings.all.select(s -> s.section.equals(section)
                && (!filtering || (s.name() + " " + s.description()).toLowerCase().contains(q)));
            boolean welcome = section.equals("notifications") && matches(q, "eclipse-settings.welcome");
            if(rows.isEmpty() && !welcome) continue;
            section(section);
            for(EclipseSettings.Setting s : rows) settingRow(s);
            if(welcome) actionRow("eclipse-settings.welcome", Core.bundle.get("eclipse-settings.welcome.button"), () -> {
                EclipseSettings.showWelcomeAgain();
                ui.showInfoToast(Core.bundle.get("eclipse-settings.welcome.done"), 3f);
            });
            shown += rows.size;
        }
        if(!filtering){
            keysSection();
            section("gameplay");
            row(Core.bundle.get("eclipse-settings.factions"), Core.bundle.get("eclipse-settings.factions.description"),
                t -> t.table(chamfer(chipFill, steel, 6f), c -> EclipseFonts.crisp(c.add(Core.bundle.get("eclipse-settings.factions.always")), 0.75f).color(muted).pad(6f, 12f, 6f, 12f)));
            section("data");
            actionRow("eclipse-settings.wiki-history", Core.bundle.get("eclipse-settings.wiki-history.button"), () -> {
                EclipseSettings.clearWikiHistory();
                ui.showInfoToast(Core.bundle.get("eclipse-settings.wiki-history.done"), 3f);
            });
            actionRow("eclipse-settings.reset", Core.bundle.get("eclipse-settings.reset.button"), () ->
                ui.showConfirm("@eclipse-settings.reset.name", "@eclipse-settings.reset.confirm", () -> {
                    EclipseSettings.resetAll();
                    rebuildContent();
                }));
        }else if(shown == 0){
            content.add(Core.bundle.get("eclipse-settings.none")).color(muted).left().padTop(20f);
        }
    }

    static boolean matches(String q, String key){
        return q.isEmpty() || (Core.bundle.get(key + ".name") + " " + Core.bundle.get(key + ".description")).toLowerCase().contains(q);
    }

    void section(String id){
        EclipseFonts.crisp(content.add(Core.bundle.get("eclipse-settings.section." + id)), 0.7f).color(muted).left().padTop(content.hasChildren() ? 26f : 4f).padBottom(4f).row();
    }

    /** One setting: name and description on the left, its control on the right, a thin line under it. */
    void row(String name, String text, arc.func.Cons<Table> control){
        content.table(r -> {
            r.left();
            r.table(t -> {
                t.left();
                t.add(name).left().row();
                Label d = EclipseFonts.crisp(t.add(text), 0.72f).color(description).left().wrap().growX().get();
                d.setAlignment(Align.left);
            }).growX().left().padRight(24f);
            r.table(control::get).right();
        }).growX().padTop(10f).padBottom(10f).row();
        content.image(Tex.whiteui).color(line).height(1f).growX().row();
    }

    void settingRow(EclipseSettings.Setting s){
        row(s.name(), s.description(), t -> {
            if(s.kind == EclipseSettings.Kind.toggle){
                t.add(new Switch(s::on, v -> {
                    Core.settings.put(s.key, v);
                    EclipseSettings.apply();
                })).size(56f, 30f);
            }else{
                slider(t, 0, 100, 10, Math.round(s.fraction() * 100f), v -> v + "%", v -> {
                    Core.settings.put(s.key, v);
                    EclipseSettings.apply();
                });
            }
        });
    }

    void actionRow(String key, String button, Runnable run){
        row(Core.bundle.get(key + ".name"), Core.bundle.get(key + ".description"), t -> t.add(action(null, button, run, new Seq<>())).height(40f));
    }

    void keysSection(){
        section("keys");
        content.table(r -> {
            r.left();
            Object[][] keys = {{ProductionStats.key, "eclipse-settings.key.production"}, {StallOverlay.key, "eclipse-settings.key.stalls"},
                {EclipseWiki.entryKey, "eclipse-settings.key.wiki"}, {eclipse.input.UpgradePlanner.key, "eclipse-settings.key.upgrade"}};
            for(Object[] k : keys){
                if(k[0] == null) continue;
                r.table(chamfer(chipFill, steel, 5f), c -> EclipseFonts.crisp(c.add(EclipseWiki.keyName((KeyBind)k[0])), 0.75f).color(accent).pad(3f, 9f, 3f, 9f)).padRight(8f);
                EclipseFonts.crisp(r.add(Core.bundle.get((String)k[1])), 0.8f).padRight(26f);
            }
            r.add().growX();
            r.add(action(null, Core.bundle.get("eclipse-settings.keys.change"), () -> ui.controls.show(), new Seq<>())).height(40f);
        }).growX().padTop(10f).padBottom(10f).row();
        content.image(Tex.whiteui).color(line).height(1f).growX().row();
    }

    /** An on/off switch: a gold track with the knob on the right when on. */
    static class Switch extends Element{
        final Boolp get;
        float slide;

        Switch(Boolp get, Boolc set){
            this.get = get;
            slide = get.get() ? 1f : 0f;
            clicked(() -> set.get(!get.get()));
        }

        @Override
        public void act(float delta){
            super.act(delta);
            slide = Mathf.approachDelta(slide, get.get() ? 1f : 0f, 0.2f);
        }

        @Override
        public void draw(){
            Color track = Tmp.c1.set(offTrack).lerp(accent, slide);
            Draw.color(track, parentAlpha);
            octagon(x, y, width, height, Scl.scl(5f));
            float pad = Scl.scl(4f), size = height - pad * 2f;
            Draw.color(knob, parentAlpha);
            Fill.crect(x + pad + (width - pad * 2f - size) * slide, y + pad, size, size);
            Draw.reset();
        }
    }
}
