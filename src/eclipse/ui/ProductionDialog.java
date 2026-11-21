package eclipse.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import eclipse.world.blocks.*;
import eclipse.world.blocks.RecipeCrafter.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;

import static mindustry.Vars.*;

/**
 * The production panel (O, or the wiki's Production button), in three tabs:
 * <ul>
 * <li>Resources: every material the base makes or uses, sortable and filterable, with a trend line; the selected one in
 * detail (rates, a one-minute graph, its production chain, totals and a hint).</li>
 * <li>Production Chains: the selected material's whole chain for a target rate, with machines needed against machines built.</li>
 * <li>Power: made, used, stored and satisfaction, a one-minute graph, and power by generator and consumer block.</li>
 * </ul>
 * All numbers come from {@link ProductionStats}; the panel refreshes once a second.
 */
public class ProductionDialog extends BaseDialog{
    public static @Nullable ProductionDialog instance;

    //the wiki's look: the game's translucent dialog, see-through dark panels and its yellow accent; blue/orange only for the graph lines
    static final Color accent = Pal.accent, madeColor = Color.valueOf("4aa8ff"), usedColor = Color.valueOf("ff9a3d"),
        good = Pal.heal, bad = Pal.remove, muted = Color.lightGray, grid = Color.valueOf("ffffff22"),
        //cards: dark, a little see-through, 1px frame; zebra rows; dimmed zeroes
        cardFill = Color.valueOf("121212d9"), cardBorder = Color.valueOf("2a2a2a"), rowA = Color.valueOf("0d0d0dcc"), rowB = Color.valueOf("151515cc"),
        rowSelected = Color.valueOf("ffd37f2e"), dim = Color.valueOf("555555"), tabFill = Color.valueOf("1a1a1ae6"), tabOver = Color.valueOf("2a2a2ae6");

    enum Tab{resources, chains, power}
    enum Sort{name, made, used, net, core}
    enum Filter{all, raw, intermediate, advanced, liquids}

    Tab tab = Tab.resources;
    Sort sort = Sort.used;
    boolean descending = true;
    Filter filter = Filter.all;
    String search = "";
    @Nullable UnlockableContent selected;
    float chainRate = -1f;
    float refresh;

    final Table body = new Table(), list = new Table(), right = new Table();
    final ObjectMap<UnlockableContent, Filter> categories = new ObjectMap<>();

    public ProductionDialog(){
        super("@eclipse-production");
        styles();
        addCloseButton();
        cont.margin(6f);
        header();
        cont.add(body).grow();
        shown(() -> {
            categories.clear(); //raw or not depends on the map, which may have changed
            rebuild();
        });
        update(() -> {
            if((refresh += Time.delta) < 60f) return;
            //don't rebuild under someone typing (search box, target rate): the field would lose focus
            if(Core.scene.getKeyboardFocus() instanceof TextField f && f.isDescendantOf(this)) return;
            refresh = 0f;
            refreshData();
        });
    }

    /** Tabs and filter pills share one look: dark fill, gold underline when active. Rows: zebra, gold bar when selected. */
    TextButton.TextButtonStyle tabStyle;
    Button.ButtonStyle rowEven, rowOdd;

    void styles(){
        tabStyle = new TextButton.TextButtonStyle(){{
            font = Fonts.def;
            fontColor = Color.white;
            up = filled(tabFill, null, false);
            over = filled(tabOver, null, false);
            down = filled(tabOver, accent, false);
            checked = filled(tabFill, accent, false);
        }};
        rowEven = rowStyle(rowA);
        rowOdd = rowStyle(rowB);
    }

    static Button.ButtonStyle rowStyle(Color fill){
        return new Button.ButtonStyle(){{
            up = filled(fill, null, true);
            over = filled(Color.valueOf("222222cc"), null, true);
            checked = filled(rowSelected, accent, true);
        }};
    }

    /** A fill with an optional accent line: along the bottom (tabs) or the left edge (rows). Keeps the caller's alpha. */
    static Drawable filled(Color fill, @Nullable Color line, boolean leftBar){
        return new BaseDrawable(){
            @Override
            public void draw(float x, float y, float w, float h){
                float a = Draw.getColor().a;
                Draw.color(fill, fill.a * a);
                Fill.crect(x, y, w, h);
                if(line != null){
                    Draw.color(line, a);
                    if(leftBar) Fill.crect(x, y, Scl.scl(3f), h);
                    else Fill.crect(x, y, w, Scl.scl(3f));
                }
                Draw.color(1f, 1f, 1f, a);
            }
        };
    }

    /** A card: dark fill with a 1px frame. */
    static Drawable framed(Color fill, Color border){
        return new BaseDrawable(){
            @Override
            public void draw(float x, float y, float w, float h){
                float a = Draw.getColor().a, t = Scl.scl(1f);
                Draw.color(fill, fill.a * a);
                Fill.crect(x, y, w, h);
                Draw.color(border, a);
                Fill.crect(x, y, w, t);
                Fill.crect(x, y + h - t, w, t);
                Fill.crect(x, y, t, h);
                Fill.crect(x + w - t, y, t, h);
                Draw.color(1f, 1f, 1f, a);
            }
        };
    }

    static Drawable panel(){
        return framed(cardFill, cardBorder);
    }

    static Drawable box(){
        return Styles.black6;
    }

    /** A section heading as in the wiki: accent text with a line under it. */
    static void heading(Table t, String key){
        t.add("[accent]" + Core.bundle.get(key)).left().padTop(6f).padBottom(2f).row();
        t.image().color(accent).height(2f).growX().padBottom(6f).row();
    }

    // ---- frame ----

    void header(){
        cont.table(h -> {
            h.left();
            tabButton(h, Tab.resources, Icon.box);
            tabButton(h, Tab.chains, Icon.tree);
            tabButton(h, Tab.power, Icon.power);
        }).growX().padBottom(8f).row();
    }

    void tabButton(Table h, Tab t, Drawable icon){
        TextButton b = h.button(Core.bundle.get("eclipse-production.tab." + t.name()), icon, tabStyle, () -> {
            tab = t;
            rebuild();
        }).height(46f).minWidth(190f).padRight(8f).get();
        b.update(() -> b.setChecked(tab == t));
        b.getLabelCell().padLeft(8f);
    }

    void rebuild(){
        body.clear();
        if(selected == null){
            Seq<UnlockableContent> rows = rows();
            if(rows.any()) selected = rows.first();
        }
        if(tab == Tab.power){
            body.add(right).grow();
        }else{
            body.table(panel(), this::leftPanel).width(Math.min(Core.graphics.getWidth() / Scl.scl(1f) * 0.47f, 760f)).growY().padRight(10f);
            body.add(right).grow();
        }
        refreshData();
    }

    void refreshData(){
        fillList();
        //the rebuild makes new scroll panes: remember where each was scrolled to and put it back (same layout, same order)
        Seq<ScrollPane> before = new Seq<>();
        panes(right, before);
        FloatSeq scrollX = new FloatSeq(), scrollY = new FloatSeq();
        for(ScrollPane p : before){
            scrollX.add(p.getScrollX());
            scrollY.add(p.getScrollY());
        }
        Tab shownTab = lastTab;
        UnlockableContent shownSelected = lastSelected;
        right.clear();
        right.top().left();
        right.background(panel());
        right.margin(14f);
        switch(tab){
            case resources -> resourceDetail();
            case chains -> chainView();
            case power -> powerView();
        }
        lastTab = tab;
        lastSelected = selected;
        if(shownTab != tab || shownSelected != selected) return; //another tab or material starts at the top
        Seq<ScrollPane> after = new Seq<>();
        panes(right, after);
        //set before the first layout: the pane clamps to its new size instead of resetting
        Runnable restore = () -> {
            for(int i = 0; i < after.size && i < scrollY.size; i++){
                after.get(i).setScrollXForce(scrollX.get(i));
                after.get(i).setScrollYForce(scrollY.get(i));
            }
        };
        restore.run();
        //and once more after the first layout, in case it clamped the panes while their content was still unmeasured
        Core.app.post(restore);
    }

    @Nullable Tab lastTab;
    @Nullable UnlockableContent lastSelected;

    static void panes(Element e, Seq<ScrollPane> out){
        if(e instanceof ScrollPane p) out.add(p);
        if(e instanceof Group g) for(Element child : g.getChildren()) panes(child, out);
    }

    // ---- left: the material list ----

    static final float colName = 190f, colNum = 92f, colCore = 74f, rowHeight = 42f;

    void leftPanel(Table t){
        t.margin(10f).top();
        //top-down: search, category pills, column headers, rows
        t.table(top -> {
            top.left();
            top.image(Icon.zoom).padRight(8f);
            TextField field = top.field(search, s -> {
                search = s.trim().toLowerCase();
                fillList();
            }).growX().height(42f).get();
            field.setMessageText(Core.bundle.get("eclipse-production.search"));
        }).growX().padBottom(6f).row();
        t.table(pills -> {
            pills.left();
            for(Filter f : Filter.values()){
                TextButton b = pills.button(Core.bundle.get("eclipse-production.filter." + f.name()), tabStyle, () -> {
                    filter = f;
                    fillList();
                }).height(38f).padRight(6f).get();
                b.getLabelCell().pad(0f, 12f, 0f, 12f);
                b.update(() -> b.setChecked(filter == f));
            }
        }).growX().padBottom(10f).row();
        if(tab == Tab.resources){
            t.table(head -> {
                head.left().defaults().left();
                sortHeader(head, Sort.name, "eclipse-production.col.material", colName);
                sortHeader(head, Sort.made, "eclipse-production.col.made", colNum);
                sortHeader(head, Sort.used, "eclipse-production.col.used", colNum);
                sortHeader(head, Sort.net, "eclipse-production.col.net", colNum);
                sortHeader(head, Sort.core, "eclipse-production.col.core", colCore);
            }).growX().padBottom(4f).row();
        }else{
            //chains: every material in the game, whether the base makes it or not
            t.add("[accent]" + Core.bundle.get("eclipse-production.chains.all")).left().height(32f).padBottom(4f).row();
        }
        t.image().color(accent).height(2f).growX().padBottom(4f).row();
        t.pane(Styles.smallPane, list).grow().get().setScrollingDisabledX(true);
    }

    /** A sortable column title, at the start of its column like the values under it. */
    void sortHeader(Table head, Sort s, String key, float width){
        head.button(b -> {
            b.left();
            b.label(() -> (sort == s ? "[accent]" + (descending ? "v " : "^ ") : "[lightgray]") + Core.bundle.get(key));
        }, Styles.cleart, () -> {
            if(sort == s) descending = !descending;
            else{
                sort = s;
                descending = s != Sort.name;
            }
            fillList();
        }).minWidth(width).growX().height(32f);
    }

    /** Every item (but upgrade modules, which have no chain) and liquid in the game. */
    Seq<UnlockableContent> allMaterials(){
        Seq<UnlockableContent> out = new Seq<>();
        for(Item i : content.items()) if(!i.isHidden() && !(i instanceof eclipse.augment.AugmentModule)) out.add(i);
        for(Liquid l : content.liquids()) if(!l.isHidden()) out.add(l);
        return out;
    }

    Seq<UnlockableContent> rows(){
        Seq<UnlockableContent> rows = (tab == Tab.chains ? allMaterials() : ProductionStats.tracked()).select(c -> (filter == Filter.all || category(c) == filter)
            && (search.isEmpty() || c.localizedName.toLowerCase().contains(search)));
        if(tab == Tab.chains) return rows.sort(Structs.comparing(c -> c.localizedName));
        Floatf<UnlockableContent> key = switch(sort){
            case made -> c -> ProductionStats.made.get(c, 0f);
            case used -> c -> ProductionStats.used.get(c, 0f);
            case net -> c -> ProductionStats.made.get(c, 0f) - ProductionStats.used.get(c, 0f);
            case core -> c -> c instanceof Item i && player != null && player.core() != null ? player.core().items.get(i) : -1f;
            case name -> c -> 0f;
        };
        if(sort == Sort.name) rows.sort(Structs.comparing(c -> c.localizedName));
        else rows.sort(Structs.comparingFloat(key));
        if(descending) rows.reverse();
        return rows;
    }

    void fillList(){
        list.clear();
        list.top().left();
        Seq<UnlockableContent> rows = rows();
        if(rows.isEmpty()){
            list.add("[lightgray]" + Core.bundle.get("eclipse-production.empty")).pad(10f);
            return;
        }
        Building core = player == null ? null : player.core();
        int index = 0;
        for(UnlockableContent c : rows){
            float m = ProductionStats.made.get(c, 0f), u = ProductionStats.used.get(c, 0f);
            Button row = new Button(index++ % 2 == 0 ? rowEven : rowOdd);
            row.left();
            if(tab == Tab.chains){
                //chains: just the material and its category
                row.image(c.uiIcon).size(26f).padLeft(8f).padRight(8f);
                row.add(c.localizedName).left().ellipsis(true).growX();
                row.add("[#" + dim + "]" + Core.bundle.get("eclipse-production.badge." + category(c).name())).fontScale(0.75f).right().padRight(12f);
                row.setChecked(selected == c);
                row.clicked(() -> {
                    selected = c;
                    chainRate = -1f;
                    refreshData();
                });
                list.add(row).growX().height(rowHeight).row();
                continue;
            }
            row.table(n -> {
                n.left();
                n.image(c.uiIcon).size(26f).padLeft(8f).padRight(8f);
                n.add(c.localizedName).left().ellipsis(true).width(colName - 48f);
            }).minWidth(colName).growX().left();
            row.add(dimmed(rate(m), m)).minWidth(colNum).growX().left();
            row.add(dimmed(rate(u), u)).minWidth(colNum).growX().left();
            //net with a thin bar under it: red = share of the demand not covered, green = share of the output left over
            Table netCell = new Table();
            netCell.left().add(Math.abs(m - u) < 0.005f ? dimmed(rate(0f), 0f) : net(m - u, Math.max(m, u)));
            row.stack(new NetBar(m, u), netCell).minWidth(colNum).growX().growY();
            int stock = c instanceof Item i && core != null ? core.items.get(i) : -1;
            row.add(stock < 0 ? "[#" + dim + "]-" : dimmed(String.valueOf(stock), stock)).minWidth(colCore).growX().left();
            row.setChecked(selected == c);
            row.clicked(() -> {
                selected = c;
                chainRate = -1f;
                refreshData();
            });
            list.add(row).growX().height(rowHeight).row();
        }
    }

    // ---- right: resource detail ----

    void resourceDetail(){
        UnlockableContent c = selected;
        if(c == null){
            right.add("[lightgray]" + Core.bundle.get("eclipse-production.empty"));
            return;
        }
        float m = ProductionStats.made.get(c, 0f), u = ProductionStats.used.get(c, 0f);
        right.table(top -> {
            top.left();
            top.image(c.uiIcon).size(80f).padRight(16f);
            top.table(n -> {
                n.left().top();
                n.add("[accent]" + c.localizedName).style(Styles.outlineLabel).fontScale(1.3f).left().row();
                badge(n, c);
                n.row();
                if(c.description != null) n.add("[lightgray]" + c.description).wrap().width(300f).left().padTop(4f);
            }).growX().left();
            statBox(top, "eclipse-production.col.made", rate(m), Color.white);
            statBox(top, "eclipse-production.col.used", rate(u), Color.white);
            float n = m - u;
            statBox(top, "eclipse-production.col.net", (n > 0 ? "+" : "") + rate(n), netColor(n, Math.max(m, u)));
        }).growX().padBottom(12f).row();

        right.table(box(), g -> {
            g.margin(10f);
            g.table(legend -> {
                legend.right();
                legend.image().color(madeColor).size(10f).padRight(4f);
                legend.add(Core.bundle.get("eclipse-production.col.made")).padRight(14f);
                legend.image().color(usedColor).size(10f).padRight(4f);
                legend.add(Core.bundle.get("eclipse-production.col.used"));
            }).growX().row();
            g.add(new LineGraph(() -> ProductionStats.history(c, true), () -> ProductionStats.history(c, false))).grow().minHeight(170f);
        }).growX().height(220f).padBottom(12f).row();


        heading(right, "eclipse-production.details");
        right.table(d -> {
            d.left().top();
            d.table(rows -> {
                rows.left().defaults().left().padBottom(4f);
                mindustry.world.blocks.storage.CoreBlock.CoreBuild core = player == null ? null : player.core();
                detailRow(rows, "eclipse-production.col.core", c instanceof Item i && core != null ? String.valueOf(core.items.get(i)) : "-");
                float avg = ProductionStats.averageNet(c);
                detailRow(rows, "eclipse-production.last-minute", net(avg, Math.max(m, u)));
                detailRow(rows, "eclipse-production.total-made", amount(ProductionStats.totalMade.get(c, 0f)));
                detailRow(rows, "eclipse-production.total-used", amount(ProductionStats.totalUsed.get(c, 0f)));
            }).width(300f).top().padRight(20f);
            d.table(box(), hint -> {
                hint.margin(12f).left();
                hint.image(Icon.infoCircle).size(28f).color(accent).top().padRight(10f);
                hint.add(advice(c, m, u)).wrap().growX().left();
            }).growX().top().minHeight(100f);
        }).growX().row();
    }

    void statBox(Table t, String key, String value, Color color){
        t.table(box(), b -> {
            b.margin(8f, 14f, 8f, 14f).left();
            b.add("[lightgray]" + Core.bundle.get(key)).left().row();
            b.add(value).fontScale(1.4f).color(color).left();
        }).minWidth(120f).padLeft(8f).top();
    }

    void detailRow(Table t, String key, String value){
        t.add(Core.bundle.get(key)).padRight(20f);
        t.add(value).right().row();
    }

    String advice(UnlockableContent c, float m, float u){
        float big = Math.max(m, u), n = ProductionStats.averageNet(c);
        String name = c.localizedName;
        if(u > 0.001f && m <= 0.001f) return Core.bundle.format("eclipse-production.advice.none", name);
        if(n < -0.02f * big) return Core.bundle.format("eclipse-production.advice.short", name);
        if(n > 0.02f * big){
            mindustry.world.blocks.storage.CoreBlock.CoreBuild core = player == null ? null : player.core();
            if(c instanceof Item i && core != null && core.items.get(i) >= core.storageCapacity * 0.95f) return Core.bundle.format("eclipse-production.advice.full", name);
            return Core.bundle.format("eclipse-production.advice.surplus", name);
        }
        if(big <= 0.001f) return Core.bundle.format("eclipse-production.advice.idle", name);
        return Core.bundle.format("eclipse-production.advice.balanced", name);
    }

    // ---- chains tab ----

    /** Icon, name, category badge, and core stock and net rate: the top of every chain page, raw materials too. */
    void headerCard(UnlockableContent c){
        float m = ProductionStats.made.get(c, 0f), u = ProductionStats.used.get(c, 0f);
        Building core = player == null ? null : player.core();
        right.table(framed(Color.valueOf("0d0d0dcc"), cardBorder), h -> {
            h.margin(10f).left();
            h.image(c.uiIcon).size(48f).padRight(14f);
            h.table(n -> {
                n.left();
                n.add("[accent]" + c.localizedName).style(Styles.outlineLabel).fontScale(1.3f).left().row();
                badge(n, c);
            }).left();
            h.add().growX();
            h.table(st -> {
                st.right().defaults().right();
                st.add("[lightgray]" + Core.bundle.get("eclipse-production.col.core") + "  [white]"
                    + (c instanceof Item i && core != null ? core.items.get(i) : "-")).row();
                st.add("[lightgray]" + Core.bundle.get("eclipse-production.col.net") + "  " + net(m - u, Math.max(m, u)));
            });
        }).growX().padBottom(12f).row();
    }

    /** A small framed category label: RAW MATERIAL, INTERMEDIATE, ADVANCED, LIQUID. */
    void badge(Table t, UnlockableContent c){
        t.table(framed(Color.valueOf("00000066"), accent), b -> b.add(Core.bundle.get("eclipse-production.badge." + category(c).name()))
            .fontScale(0.75f).color(accent).pad(2f, 8f, 2f, 8f)).left().padTop(4f);
    }

    void chainView(){
        UnlockableContent c = selected;
        if(c == null){
            right.add("[lightgray]" + Core.bundle.get("eclipse-production.empty"));
            return;
        }
        headerCard(c);
        if(!(c instanceof Item item) || RecipeTree.raw(item) || RecipeTree.sourceOf(item) == null){
            rawChain(c);
            return;
        }
        if(chainRate <= 0f) chainRate = Math.max(Math.max(ProductionStats.used.get(c, 0f), ProductionStats.made.get(c, 0f)), 1f);
        right.table(top -> {
            top.left();
            top.add(Core.bundle.get("eclipse-wiki.tree.rate") + " ").color(muted);
            top.field(Strings.autoFixed(chainRate, 2), TextField.TextFieldFilter.floatsOnly, text -> {
                float v = Strings.parseFloat(text, -1f);
                if(v > 0f && v <= 10000f) chainRate = v;
            }).width(100f);
            top.add(" /s").padRight(10f);
            top.button(Icon.refresh, Styles.cleari, this::refreshData).size(40f);
        }).growX().padBottom(10f).row();

        ObjectIntMap<String> have = builtCounts();
        RecipeTree.Step root = RecipeTree.plan(item, chainRate);
        Table tree = new Table().top().left();
        tree.defaults().left().padBottom(3f);
        tree.add("[accent]" + Core.bundle.get("eclipse-production.col.material")).padRight(20f);
        tree.add("[accent]" + Core.bundle.get("eclipse-production.chains.machine")).padRight(20f);
        tree.add("[accent]" + Core.bundle.get("eclipse-production.chains.need")).padRight(20f);
        tree.add("[accent]" + Core.bundle.get("eclipse-production.chains.have")).row();
        chainRows(tree, root, 0, have);
        right.pane(Styles.smallPane, tree).grow().row();
        int tier = RecipeTree.maxTier(root);
        right.add("[lightgray]" + Core.bundle.format("eclipse-wiki.tree.power", Strings.autoFixed(RecipeTree.power(root) * 60f, 0),
            tier == 0 ? "-" : RecipeCrafter.roman(tier), Strings.autoFixed(RecipeCrafter.tierPower[tier] * 60f, 0))).wrap().growX().left().padTop(8f).row();
        right.add("[lightgray]" + Core.bundle.get("eclipse-production.chains.hint")).wrap().growX().left().padTop(4f);
    }

    /** A raw material has no chain of its own: show what it goes into instead, each a jump to that chain. */
    void rawChain(UnlockableContent c){
        right.add("[lightgray]" + Core.bundle.format("eclipse-production.chains.raw", c.localizedName)).wrap().growX().left().padBottom(10f).row();
        Seq<UnlockableContent> products = new Seq<>();
        for(Block b : content.blocks()){
            if(!b.name.startsWith("eclipse-") || !b.isVisible() || !EclipseWiki.uses(b, c)) continue;
            if(b instanceof RecipeCrafter rc){
                for(Recipe r : rc.recipes){
                    boolean usesIt = (c instanceof Item i && (r.uses(i) || r.catalyst == i)) || (c instanceof Liquid l && r.usesLiquid(l));
                    if(!usesIt) continue;
                    for(ItemStack o : r.outputs) products.addUnique(o.item);
                    for(LiquidStack o : r.liquidOutputs) products.addUnique(o.liquid);
                }
            }else if(b instanceof GenericCrafter g){
                if(g.outputItems != null) for(ItemStack o : g.outputItems) products.addUnique(o.item);
                if(g.outputLiquids != null) for(LiquidStack o : g.outputLiquids) products.addUnique(o.liquid);
            }
        }
        products.remove(c);
        if(products.isEmpty()){
            right.add("[lightgray]" + Core.bundle.format("eclipse-production.chains.unused", c.localizedName)).wrap().growX().left();
            return;
        }
        heading(right, "eclipse-production.chains.used-in");
        Table grid = new Table().top().left();
        int n = 0;
        for(UnlockableContent p : products){
            grid.button(b -> {
                b.left();
                b.image(p.uiIcon).size(28f).padLeft(8f).padRight(8f);
                b.add(p.localizedName).left().ellipsis(true).width(150f);
            }, tabStyle, () -> {
                selected = p;
                chainRate = -1f;
                refreshData();
            }).size(210f, 44f).pad(3f);
            if(++n % 3 == 0) grid.row();
        }
        right.pane(Styles.smallPane, grid).grow().top();
    }

    void chainRows(Table t, RecipeTree.Step s, int depth, ObjectIntMap<String> have){
        t.table(n -> {
            n.left();
            n.add().width(depth * 20f);
            n.image(s.item.uiIcon).size(24f).padRight(6f);
            n.add(s.item.localizedName + " [lightgray]" + rate(s.perSecond));
        }).left().padRight(20f);
        if(s.source != null){
            Block b = s.source.block;
            int built = have.get(key(b, s.item), 0);
            float need = s.machines;
            t.table(n -> {
                n.left();
                n.image(b.uiIcon).size(24f).padRight(6f);
                n.add(b.localizedName);
            }).left().padRight(20f);
            t.add(Strings.autoFixed(need, 2)).padRight(20f);
            t.add((built >= Math.ceil(need - 0.001f) ? "[#" + good + "]" : "[#" + bad + "]") + built).row();
        }else{
            t.add("[lightgray]" + Core.bundle.get("eclipse-production.chains.raw-step")).colspan(3).row();
        }
        for(RecipeTree.Step in : s.inputs) chainRows(t, in, depth + 1, have);
    }

    /** Built machines per block (universal machines per block and recipe product). */
    ObjectIntMap<String> builtCounts(){
        ObjectIntMap<String> out = new ObjectIntMap<>();
        if(player == null) return out;
        for(Building b : player.team().data().buildings){
            if(!b.isValid()) continue;
            if(b instanceof RecipeBuild r){
                if(r.recipe != null) for(ItemStack s : r.recipe.outputs) out.increment(key(b.block, s.item));
            }else out.increment(key(b.block, null));
        }
        return out;
    }

    static String key(Block b, @Nullable UnlockableContent product){
        return b instanceof RecipeCrafter && product != null ? b.name + "/" + product.name : b.name + "/";
    }

    // ---- power tab ----

    void powerView(){
        float m = ProductionStats.powerMade, u = ProductionStats.powerUsed;
        right.table(top -> {
            top.left();
            top.image(Icon.power).size(64f).color(accent).padRight(16f);
            top.add("[accent]" + Core.bundle.get("eclipse-production.tab.power")).style(Styles.outlineLabel).fontScale(1.3f).growX().left();
            statBox(top, "eclipse-production.col.made", power(m), Color.white);
            statBox(top, "eclipse-production.col.used", power(u), Color.white);
            statBox(top, "eclipse-production.col.net", (m - u > 0 ? "+" : "") + power(m - u), netColor(m - u, Math.max(m, u)));
            statBox(top, "eclipse-production.stored", power(ProductionStats.powerStored).replace("/s", "") + " / " + power(ProductionStats.powerCapacity).replace("/s", ""), Color.white);
            statBox(top, "eclipse-production.satisfaction", (int)(ProductionStats.satisfaction * 100) + "%", ProductionStats.satisfaction < 0.999f ? bad : good);
        }).growX().padBottom(12f).row();
        right.table(box(), g -> {
            g.margin(10f);
            g.add(new LineGraph(() -> ProductionStats.powerHistory(true), () -> ProductionStats.powerHistory(false))).grow().minHeight(170f);
        }).growX().height(220f).padBottom(12f).row();
        right.table(lists -> {
            lists.top().left();
            lists.table(l -> blockList(l, "eclipse-production.generators", ProductionStats.powerByGenerator, ProductionStats.generatorCount, madeColor)).grow().top().padRight(16f);
            lists.table(l -> blockList(l, "eclipse-production.consumers", ProductionStats.powerByConsumer, ProductionStats.consumerCount, usedColor)).grow().top();
        }).grow();
    }

    void blockList(Table t, String key, ObjectFloatMap<Block> power, ObjectIntMap<Block> counts, Color color){
        t.top().left().defaults().left().padBottom(3f);
        t.add("[accent]" + Core.bundle.get(key)).left().padBottom(2f).row();
        t.image().color(accent).height(2f).growX().padBottom(6f).row();
        Seq<Block> blocks = power.keys().toSeq().sort(Structs.comparingFloat(b -> -power.get(b, 0f)));
        if(blocks.isEmpty()) t.add("[lightgray]-").row();
        Table rows = new Table().top().left();
        rows.defaults().left().padBottom(3f);
        for(Block b : blocks){
            rows.image(b.uiIcon).size(26f).padRight(8f);
            rows.add(b.localizedName + " [lightgray]x" + counts.get(b, 0)).growX().padRight(16f);
            rows.add(power(power.get(b, 0f))).color(color).right().row();
        }
        t.pane(Styles.smallPane, rows).grow().top();
    }

    // ---- formatting ----

    Filter category(UnlockableContent c){
        return categories.get(c, () -> {
            if(c instanceof Liquid) return Filter.liquids;
            if(!(c instanceof Item i) || RecipeTree.raw(i)) return Filter.raw;
            RecipeTree.Source s = RecipeTree.sourceOf(i);
            if(s == null) return Filter.raw;
            return s.tier >= 3 ? Filter.advanced : Filter.intermediate;
        });
    }

    /** Zeroes in a muted gray, so real production and deficits stand out. */
    static String dimmed(String text, float value){
        return Math.abs(value) < 0.005f ? "[#" + dim + "]" + text : text;
    }

    static String rate(float v){
        return Strings.autoFixed(v, Math.abs(v) < 10f ? 2 : 1) + "/s";
    }

    static String net(float v, float scale){
        return "[#" + netColor(v, scale) + "]" + (v > 0.0005f ? "+" : "") + rate(v);
    }

    static Color netColor(float v, float scale){
        return v < -0.02f * scale ? bad : v > 0.02f * scale ? good : Color.lightGray;
    }

    static String amount(float v){
        return v >= 10000f ? UI.formatAmount((long)v) : String.valueOf((int)v);
    }

    static String power(float v){
        return shortNumber(v) + "/s";
    }

    /** 950, 12.5k, 3.4mil, 1.2bil: short enough for graph scales and stat boxes at any power level. */
    static String shortNumber(float v){
        float a = Math.abs(v);
        String sign = v < 0f ? "-" : "";
        if(a >= 1e9f) return sign + Strings.autoFixed(a / 1e9f, 1) + "bil";
        if(a >= 1e6f) return sign + Strings.autoFixed(a / 1e6f, 1) + "mil";
        if(a >= 1e4f) return sign + Strings.autoFixed(a / 1e3f, 1) + "k";
        return sign + Strings.autoFixed(a, a >= 10f ? 0 : 2);
    }

    // ---- graphs ----

    /** A thin bar along the bottom edge of the Net cell (under the number): the deficit share in red, or the surplus share in green. */
    static class NetBar extends Element{
        final float made, used;

        NetBar(float made, float used){
            this.made = made;
            this.used = used;
        }

        @Override
        public void draw(){
            float h = Scl.scl(3f), w = width - Scl.scl(10f), y0 = y + Scl.scl(2f);
            if(made <= 0.005f && used <= 0.005f) return;
            float frac;
            if(used > made){
                frac = (used - made) / used;
                Draw.color(bad, parentAlpha);
            }else{
                frac = made <= 0f ? 0f : (made - used) / made;
                Draw.color(good, parentAlpha);
            }
            //fill from the left, where the numbers start
            Fill.crect(x, y0, w * frac, h);
            Draw.reset();
        }
    }

    /** Made and used over the last minute, with a 0..max scale on the left. */
    static class LineGraph extends Element{
        final Prov<float[]> a, b;

        LineGraph(Prov<float[]> a, Prov<float[]> b){
            this.a = a;
            this.b = b;
        }

        @Override
        public void draw(){
            float[] da = a.get(), db = b.get();
            float max = 0f;
            for(float v : da) max = Math.max(max, v);
            for(float v : db) max = Math.max(max, v);
            max = niceMax(max);
            float left = Scl.scl(44f), bottom = Scl.scl(8f), top = Scl.scl(8f);
            float px = x + left, pw = width - left - Scl.scl(6f), py = y + bottom, ph = height - bottom - top;
            for(int i = 0; i <= 4; i++){
                float yy = py + ph * i / 4f;
                Draw.color(grid, parentAlpha);
                Lines.stroke(Scl.scl(1f));
                Lines.line(px, yy, px + pw, yy);
                Fonts.outline.draw(shortNumber(max * i / 4f), px - Scl.scl(6f), yy + Scl.scl(6f), muted, 0.8f, false, Align.right);
            }
            series(da, madeColor, px, py, pw, ph, max);
            series(db, usedColor, px, py, pw, ph, max);
            Draw.reset();
        }

        void series(float[] d, Color c, float px, float py, float pw, float ph, float max){
            if(d.length < 2) return;
            Draw.color(c, parentAlpha);
            Lines.stroke(Scl.scl(2f));
            float step = pw / (ProductionStats.historySize - 1);
            float x0 = px + pw - step * (d.length - 1);
            for(int i = 1; i < d.length; i++){
                Lines.line(x0 + step * (i - 1), py + ph * d[i - 1] / max, x0 + step * i, py + ph * d[i] / max);
            }
        }

        static float niceMax(float v){
            if(v <= 0.0001f) return 1f;
            float mag = (float)Math.pow(10, Math.floor(Math.log10(v)));
            for(float m : new float[]{1f, 2f, 2.5f, 5f, 10f}) if(v <= m * mag) return m * mag;
            return 10f * mag;
        }
    }
}
