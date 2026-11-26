package eclipse.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import eclipse.augment.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;

/** The wiki's look: framed panels, coloured tags, tier colours, callouts, recipe cards and links with hover cards. */
public class WikiUI{
    public static final Color cyan = Color.valueOf("8fd8ff"), heat = Color.valueOf("ff9a3c"), good = Color.valueOf("84f491"),
        bad = Color.valueOf("ff7b6b"), panel = Color.valueOf("14141a"), edge = Color.valueOf("2a2a34"), well = Color.valueOf("0f0f13"),
        dim = Color.valueOf("6e6e7e");
    static final Color[] tiers = {Color.valueOf("b8c0c8"), Color.valueOf("b8c0c8"), Color.valueOf("84f491"), Color.valueOf("8fd8ff"), Color.valueOf("c8a2ff"), Color.valueOf("ff9a5a")};

    public static Color tierColor(int tier){
        return tiers[Math.max(0, Math.min(tier, 5))];
    }

    public static String hex(Color c){
        return "[#" + c.toString().substring(0, 6) + "]";
    }

    /** A flat panel with a frame, padded inside. */
    public static Drawable box(Color fill, Color frame, float stroke, float pad){
        BaseDrawable d = new BaseDrawable(){
            @Override
            public void draw(float x, float y, float width, float height){
                float a = Draw.getColor().a, s = Scl.scl(stroke);
                Draw.color(fill.r, fill.g, fill.b, fill.a * a);
                Fill.crect(x, y, width, height);
                if(s > 0f){
                    Draw.color(frame.r, frame.g, frame.b, frame.a * a);
                    Fill.crect(x, y, width, s);
                    Fill.crect(x, y + height - s, width, s);
                    Fill.crect(x, y, s, height);
                    Fill.crect(x + width - s, y, s, height);
                }
                Draw.color(1f, 1f, 1f, a);
            }
        };
        float p = Scl.scl(pad);
        d.setLeftWidth(p);
        d.setRightWidth(p);
        d.setTopHeight(p);
        d.setBottomHeight(p);
        return d;
    }

    public static Drawable card(){
        return box(panel, edge, 2f, 8f);
    }

    /** A small framed tag in a colour ("Tier III · 480/s", "Heat 8"). */
    public static void tag(Table t, String text, Color color){
        t.table(box(Color.clear, Tmp.c1.set(color).a(0.55f).cpy(), 2f, 0f), b -> EclipseFonts.crisp(b.add(text).color(color), 0.85f).pad(1f, 6f, 1f, 6f)).pad(2f);
    }

    /** An accent heading with a rule after it. */
    public static void header(Table t, String text){
        t.table(h -> {
            h.left();
            h.add(text).color(Pal.accent).padRight(8f);
            h.image().color(edge).height(2f).growX();
        }).growX().padTop(10f).padBottom(4f).row();
    }

    /** A callout: kind 1 tip (green), 2 warning (red), 3 what comes next (blue). */
    public static Table callout(int kind, String title, String text, float width){
        Color c = kind == 2 ? bad : kind == 1 ? good : cyan;
        //the game font has no tick: the game's own icons
        Drawable mark = kind == 2 ? mindustry.gen.Icon.warning : kind == 1 ? mindustry.gen.Icon.ok : mindustry.gen.Icon.right;
        Table t = new Table(card());
        t.top().left();
        t.table(box(Color.clear, c, 2f, 0f), m -> m.image(mark).size(16f).color(c)).size(28f).top().padRight(10f);
        t.table(body -> {
            body.top().left().defaults().left();
            if(!title.isEmpty()) body.add(title).color(Color.white).wrap().width(width - 60f).padBottom(2f).row();
            body.add(text).color(Color.lightGray).wrap().width(width - 60f);
        }).growX();
        return t;
    }

    /** Guide or page text: paragraphs, headings on their own line, and "!tip|!warn|!next Title|text" callouts. */
    public static void text(Table t, String text, float width){
        for(String para : text.split("\n\n")){
            String p = para.trim();
            if(p.isEmpty()) continue;
            int kind = GuideChapters.calloutKind(p);
            if(kind > 0){
                String[] c = GuideChapters.callout(p);
                t.add(callout(kind, c[0], c[1], width)).width(width).left().padTop(4f).padBottom(6f).row();
                continue;
            }
            //a paragraph that opens with a heading line ("[accent]Stage 2: titanium[]")
            int nl = p.indexOf('\n');
            String first = nl < 0 ? p : p.substring(0, nl);
            if(first.startsWith("[accent]") && first.endsWith("[]") && first.indexOf("[]") == first.length() - 2){
                t.add(first.substring(8, first.length() - 2)).color(Pal.accent).left().padTop(8f).padBottom(2f).row();
                if(nl < 0) continue;
                p = p.substring(nl + 1);
            }
            t.add(p).wrap().width(width).left().padBottom(8f).row();
        }
    }

    // ---- links ----

    /** A clickable icon + name (+ amount) with a hover card; ECLIPSE content opens in the wiki, vanilla in the database. */
    public static void link(Table t, UnlockableContent c, @Nullable String amount, Cons<UnlockableContent> open){
        Button b = t.button(bt -> {
            bt.left();
            bt.image(c.uiIcon).size(24f).padRight(4f);
            bt.add((amount == null ? "" : "[lightgray]" + amount + "[] ") + c.localizedName).left();
        }, Styles.flatt, () -> open.get(c)).height(32f).pad(1f).padRight(8f).left().get();
        hover(b, c);
    }

    /** The hover card: icon, name, kind and the first sentence of its description. */
    public static void hover(arc.scene.Element e, UnlockableContent c){
        if(mindustry.Vars.mobile) return;
        e.addListener(new Tooltip(t -> {
            t.background(box(Color.valueOf("0b0b0f"), Pal.accent, 2f, 10f));
            t.table(row -> {
                row.left();
                row.image(c.uiIcon).size(48f).padRight(10f);
                row.table(n -> {
                    n.left().defaults().left();
                    n.add(c.localizedName).color(Pal.accent).row();
                    EclipseFonts.crisp(n.add(kindLine(c)).color(Color.lightGray), 0.85f);
                });
            }).left().row();
            String d = firstSentence(c);
            if(!d.isEmpty()) t.add(d).wrap().width(300f).left().padTop(6f).row();
            EclipseFonts.crisp(t.add(Core.bundle.get(EclipseWiki.isEclipse(c) ? "eclipse-wiki.hover.open" : "eclipse-wiki.hover.database")).color(dim), 0.8f).left().padTop(4f);
        }){{
            allowMobile = false;
        }});
    }

    static String firstSentence(UnlockableContent c){
        String key = EclipseWiki.textKey(c);
        String text = Core.bundle.has(key) ? Core.bundle.get(key) : c.description;
        if(text == null) return "";
        text = text.replace("\n", " ");
        int dot = text.indexOf(". ");
        return dot > 0 && dot < 220 ? text.substring(0, dot + 1) : text.length() > 220 ? text.substring(0, 217) + "..." : text;
    }

    public static String kindLine(UnlockableContent c){
        if(c instanceof AugmentModule m) return Core.bundle.format("eclipse-wiki.kind.module", m.romanTier());
        if(c instanceof Block b) return b.size + "×" + b.size + " · " + Core.bundle.get(b instanceof mindustry.world.blocks.environment.OreBlock ? "eclipse-wiki.ores" : "eclipse-wiki.cat." + b.category.name());
        if(c instanceof Item) return Core.bundle.get("eclipse-wiki.kind.item");
        if(c instanceof Liquid) return Core.bundle.get("eclipse-wiki.kind.liquid");
        return Core.bundle.get("eclipse-wiki.kind.unit");
    }

    // ---- recipe cards ----

    /** One input or output: icon, "2 Nickel" and its rate per second underneath. */
    static void chip(Table t, UnlockableContent c, String amount, String perSecond, Cons<UnlockableContent> open){
        Button b = t.button(bt -> {
            bt.left();
            bt.image(c.uiIcon).size(28f).padRight(6f);
            bt.table(txt -> {
                txt.left().defaults().left();
                EclipseFonts.crisp(txt.add(amount + c.localizedName), 0.9f).row();
                EclipseFonts.crisp(txt.add(perSecond).color(cyan), 0.75f);
            });
        }, new Button.ButtonStyle(){{
            up = box(well, Color.valueOf("22222a"), 2f, 4f);
            over = box(Color.valueOf("1a1a21"), Color.valueOf("3a3a46"), 2f, 4f);
            down = over;
        }}, () -> open.get(c)).pad(2f).left().get();
        hover(b, c);
    }

    /**
     * A recipe as a card: inputs, an arrow with the craft time, outputs, then tags (tier, heat…) and optional buttons.
     * Amounts are per craft; liquids per second.
     */
    public static Table recipeCard(Seq<ItemStack> in, Seq<LiquidStack> liqIn, Seq<ItemStack> out, Seq<LiquidStack> liqOut, float seconds,
                                   Cons<Table> tags, @Nullable Cons<Table> buttons, Cons<UnlockableContent> open, float width){
        Table card = new Table(card());
        card.left();
        int per = width > 900f ? 3 : 2;
        Table ins = new Table().left(), outs = new Table().left();
        int n = 0;
        for(ItemStack s : in){ chip(ins, s.item, s.amount + " ", rate(s.amount / seconds), open); if(++n % per == 0) ins.row(); }
        for(LiquidStack s : liqIn){ chip(ins, s.liquid, "", rate(s.amount * 60f), open); if(++n % per == 0) ins.row(); }
        n = 0;
        for(ItemStack s : out){ chip(outs, s.item, s.amount + " ", rate(s.amount / seconds), open); if(++n % 2 == 0) outs.row(); }
        for(LiquidStack s : liqOut){ chip(outs, s.liquid, "", rate(s.amount * 60f), open); if(++n % 2 == 0) outs.row(); }
        card.add(ins).left();
        card.table(a -> {
            EclipseFonts.crisp(a.add("→").color(Color.lightGray), 1.4f).row();
            EclipseFonts.crisp(a.add(Strings.autoFixed(seconds, 2) + " s").color(Color.lightGray), 0.8f);
        }).pad(0f, 10f, 0f, 10f);
        card.add(outs).left().growX();
        card.row();
        card.table(bottom -> {
            bottom.left();
            bottom.table(tags).left().growX();
            if(buttons != null) bottom.table(buttons).right().padLeft(8f);
        }).colspan(3).growX().padTop(4f);
        return card;
    }

    static String rate(float perSecond){
        return Strings.autoFixed(perSecond, 2) + "/s";
    }

    /** A flat toggle-looking button for small controls (Rates, Steps, recipe chips). */
    public static TextButton.TextButtonStyle chipStyle(){
        return new TextButton.TextButtonStyle(){{
            font = Fonts.def;
            fontColor = Color.lightGray;
            checkedFontColor = Pal.accent;
            up = box(Color.clear, Color.valueOf("34343f"), 2f, 4f);
            over = box(Color.valueOf("1a1a21"), Color.valueOf("4a4a58"), 2f, 4f);
            checked = box(Color.valueOf("221d10"), Pal.accent, 2f, 4f);
            down = checked;
        }};
    }
}
