package eclipse.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import eclipse.world.blocks.*;
import eclipse.world.blocks.RecipeCrafter.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.*;

/**
 * The production panel (O, or the wiki's Production button), in the wiki's look, in four tabs:
 * <ul>
 * <li>Overview: the base at a glance (power, shortages, stopped machines, the core filling up), what needs attention
 * (stopped machines grouped by block and reason, each a jump to why and how to fix it) and what runs short.</li>
 * <li>Resources: every material the base makes or uses, sortable and filterable; the selected one in detail: rates,
 * a one-minute graph, which of your machines make and use it, totals and advice.</li>
 * <li>Chains: a material's whole production chain for a target rate, drawn as a flow filling the panel.</li>
 * <li>Power: made, used, stored and satisfaction, a one-minute graph, and every generator and consumer as a share.</li>
 * </ul>
 * All numbers come from {@link ProductionStats}; the panel refreshes once a second.
 */
public class ProductionDialog extends BaseDialog{
    public static @Nullable ProductionDialog instance;

    static final Color accent = Pal.accent, madeColor = Color.valueOf("4aa8ff"), usedColor = Color.valueOf("ff9a3d"),
        good = WikiUI.good, bad = WikiUI.bad, muted = Color.lightGray, grid = Color.valueOf("ffffff22"),
        rowA = Color.valueOf("0f0f13d9"), rowB = Color.valueOf("15151bd9"), rowSelected = Color.valueOf("ffd37f26"), dim = Color.valueOf("5a5a66");

    enum Tab{overview, resources, chains, power}
    enum Sort{name, made, used, net, core}
    enum Filter{all, raw, intermediate, advanced, liquids}

    Tab tab = Tab.overview;
    Sort sort = Sort.used;
    boolean descending = true;
    Filter filter = Filter.all;
    String search = "";
    @Nullable UnlockableContent selected;
    float chainRate = -1f;
    /** The chain graph's zoom, kept across refreshes and materials. */
    float chainZoom = 1f;
    float refresh;

    final Table tabsBar = new Table(), body = new Table(), list = new Table(), right = new Table();
    final ObjectMap<UnlockableContent, Filter> categories = new ObjectMap<>();
    Button.ButtonStyle rowEven, rowOdd;

    public ProductionDialog(){
        super("@eclipse-production");
        FitDialog.install(this);
        rowEven = rowStyle(rowA);
        rowOdd = rowStyle(rowB);
        addCloseButton();
        buttons.button("@eclipse-wiki", Icon.book, () -> {
            if(EclipseWiki.instance != null) EclipseWiki.instance.show();
        }).size(210f, 64f);
        cont.margin(6f);
        cont.add(tabsBar).growX().row();
        cont.image().color(WikiUI.edge).height(3f).growX().padBottom(8f).row();
        cont.add(body).grow();
        buildTabs();
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

    static Button.ButtonStyle rowStyle(Color fill){
        return new Button.ButtonStyle(){{
            up = WikiUI.box(fill, Color.clear, 0f, 0f);
            over = WikiUI.box(Color.valueOf("1f1f27e6"), Color.clear, 0f, 0f);
            checked = WikiUI.box(rowSelected, accent, 2f, 0f);
        }};
    }

    // ---------------- frame ----------------

    void buildTabs(){
        tabsBar.clear();
        tabsBar.left();
        tabButton(Tab.overview, Icon.home);
        tabButton(Tab.resources, Icon.box);
        tabButton(Tab.chains, Icon.tree);
        tabButton(Tab.power, Icon.power);
    }

    void tabButton(Tab t, Drawable icon){
        TextButton b = tabsBar.button(Core.bundle.get("eclipse-production.tab." + t.name()), icon, Styles.flatTogglet, () -> {
            tab = t;
            rebuild();
        }).height(46f).padRight(4f).get();
        b.update(() -> b.setChecked(tab == t));
        b.getLabelCell().pad(0f, 10f, 0f, 14f);
        b.getCells().first().padLeft(10f);
    }

    /** Opens a tab with a material selected (photo mode). */
    void photo(String tabName, @Nullable String material){
        tab = Tab.valueOf(tabName);
        if(material != null){
            UnlockableContent c = content.item(material);
            selected = c != null ? c : content.liquid(material);
        }
        if(!isShown()) show();
        rebuild();
    }

    void rebuild(){
        body.clear();
        if(selected == null){
            Seq<UnlockableContent> rows = rows();
            if(rows.any()) selected = rows.first();
        }
        if(tab == Tab.overview || tab == Tab.power){
            body.add(right).grow();
        }else{
            //never narrower than the resources table's columns (small screens): FitDialog then shrinks the panel to fit
            float least = tab == Tab.resources ? colName + 3f * colNum + colCore + 40f : 0f;
            body.table(WikiUI.card(), this::leftPanel).width(Math.max(Math.min(Core.graphics.getWidth() / Scl.scl(1f) * 0.42f, 700f), least)).growY().padRight(10f);
            body.add(right).grow();
        }
        refreshData();
    }

    void select(UnlockableContent c, Tab to){
        selected = c;
        chainRate = -1f;
        tab = to;
        rebuild();
    }

    void refreshData(){
        if(tab == Tab.resources || tab == Tab.chains) fillList();
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
        right.background(WikiUI.box(Color.valueOf("101015d9"), WikiUI.edge, 2f, 14f));
        switch(tab){
            case overview -> overviewView();
            case resources -> resourceDetail();
            case chains -> chainView();
            case power -> powerView();
        }
        lastTab = tab;
        lastSelected = selected;
        if(shownTab != tab || shownSelected != selected) return; //another tab or material starts at the top
        Seq<ScrollPane> after = new Seq<>();
        panes(right, after);
        Runnable restore = () -> {
            for(int i = 0; i < after.size && i < scrollY.size; i++){
                after.get(i).setScrollXForce(scrollX.get(i));
                after.get(i).setScrollYForce(scrollY.get(i));
            }
        };
        restore.run();
        Core.app.post(restore);
    }

    @Nullable Tab lastTab;
    @Nullable UnlockableContent lastSelected;

    static void panes(Element e, Seq<ScrollPane> out){
        if(e instanceof ScrollPane p) out.add(p);
        if(e instanceof Group g) for(Element child : g.getChildren()) panes(child, out);
    }

    // ---------------- shared pieces ----------------

    /** A key number: icon, title, the value large in its colour, a line under it, and an optional bar (0-1). */
    void tile(Table t, Drawable icon, String title, String value, Color color, String sub, float bar, Color barColor){
        t.table(WikiUI.card(), b -> {
            b.top().left();
            b.table(h -> {
                h.left();
                h.image(icon).size(22f).color(color).padRight(8f);
                h.add(title).color(muted).left();
            }).left().row();
            b.add(value).color(color).fontScale(1.6f).left().padTop(4f).row();
            b.add(sub).color(muted).fontScale(0.8f).left().wrap().growX().row();
            if(bar >= 0f) b.add(new Bar(Mathf.clamp(bar), barColor)).growX().height(6f).padTop(6f);
        }).growX().uniformX().minWidth(150f).pad(0f, 0f, 0f, 10f).top();
    }

    /** A thin horizontal bar, filled to a fraction. */
    static class Bar extends Element{
        final float value;
        final Color color;

        Bar(float value, Color color){
            this.value = value;
            this.color = color;
        }

        @Override
        public void draw(){
            Draw.color(Color.valueOf("26262f"), parentAlpha);
            Fill.crect(x, y, width, height);
            Draw.color(color, parentAlpha);
            Fill.crect(x, y, width * value, height);
            Draw.reset();
        }
    }

    // ---------------- overview ----------------

    void overviewView(){
        float pm = ProductionStats.powerMade, pu = ProductionStats.powerUsed;
        Seq<UnlockableContent> shortages = ProductionStats.tracked().select(c -> net(c) < -0.02f * Math.max(made(c), used(c)) && used(c) > 0.001f)
            .sort(Structs.comparingFloat(this::net));
        CoreBuild core = player == null ? null : player.core();
        Seq<Item> filling = new Seq<>();
        if(core != null) for(Item i : content.items()) if(core.items.get(i) >= core.storageCapacity * 0.9f && net(i) > 0.001f) filling.add(i);
        int stopped = ProductionStats.crafters - ProductionStats.running;

        right.table(tiles -> {
            tiles.left().top();
            float sat = ProductionStats.satisfaction;
            tile(tiles, Icon.power, Core.bundle.get("eclipse-production.tile.power"), (pm - pu >= 0 ? "+" : "") + power(pm - pu),
                sat < 0.999f ? bad : pm > 0f ? good : muted, Core.bundle.format("eclipse-production.tile.power.sub", power(pm), power(pu), (int)(sat * 100)), sat, sat < 0.999f ? bad : good);
            tile(tiles, Icon.warning, Core.bundle.get("eclipse-production.tile.short"), String.valueOf(shortages.size), shortages.any() ? bad : good,
                Core.bundle.get(shortages.any() ? "eclipse-production.tile.short.sub" : "eclipse-production.tile.short.none"), -1f, bad);
            tile(tiles, Icon.cancel, Core.bundle.get("eclipse-production.tile.stopped"), stopped + " / " + ProductionStats.crafters, stopped > 0 ? Color.orange : good,
                Core.bundle.format("eclipse-production.tile.stopped.sub", ProductionStats.running), ProductionStats.crafters == 0 ? -1f : ProductionStats.running / (float)ProductionStats.crafters, good);
            tile(tiles, Icon.box, Core.bundle.get("eclipse-production.tile.full"), String.valueOf(filling.size), filling.any() ? Color.orange : good,
                Core.bundle.get(filling.any() ? "eclipse-production.tile.full.sub" : "eclipse-production.tile.full.none"), -1f, good);
        }).growX().padBottom(14f).row();

        right.table(cols -> {
            cols.top().left();
            cols.table(this::attention).grow().top().padRight(16f);
            cols.table(r -> {
                r.top().left();
                shortages(r, shortages);
                fillingUp(r, filling, core);
            }).width(Math.min(Core.graphics.getWidth() / Scl.scl(1f) * 0.32f, 520f)).top();
        }).grow();
    }

    /** Stopped machines, grouped by block and reason: why (the wiki's fix) and where (the K overlay). */
    void attention(Table t){
        t.top().left();
        WikiUI.header(t, Core.bundle.get("eclipse-production.attention"));
        Seq<ProductionStats.Stall> stalls = ProductionStats.stalls;
        if(stalls.isEmpty()){
            t.add(WikiUI.callout(1, Core.bundle.get("eclipse-production.attention.none"), Core.bundle.get("eclipse-production.attention.none.text"), 560f)).growX().left().row();
            return;
        }
        Table rows = new Table().top().left();
        int shown = 0;
        for(ProductionStats.Stall s : stalls){
            if(++shown > 14) break;
            rows.table(WikiUI.card(), r -> {
                r.left();
                r.image(s.block.uiIcon).size(36f).padRight(10f);
                r.table(txt -> {
                    txt.left().defaults().left();
                    txt.add("×" + s.count + "  " + s.block.localizedName).row();
                    txt.add(s.status.text()).color(StallOverlay.color(s.status.kind)).fontScale(0.85f);
                }).growX();
                if(s.status.content != null) r.image(s.status.content.uiIcon).size(26f).padRight(10f);
                if(EclipseWiki.isEclipse(s.block)) r.button("@eclipse-production.why", Icon.book, Styles.flatt, () -> EclipseWiki.openStopped(s.block, s.status)).height(38f).padRight(4f);
                r.button("@eclipse-production.show", Icon.eye, Styles.flatt, () -> {
                    StallOverlay.shown = true;
                    hide();
                    ui.showInfoToast(Core.bundle.get("eclipse-production.show.toast"), 4f);
                }).height(38f);
            }).growX().padBottom(4f).row();
        }
        if(stalls.size > 14) rows.add(Core.bundle.format("eclipse-production.attention.more", stalls.size - 14)).color(muted).left().row();
        t.pane(Styles.smallPane, rows).grow().top().get().setScrollingDisabledX(true);
    }

    void shortages(Table t, Seq<UnlockableContent> shortages){
        WikiUI.header(t, Core.bundle.get("eclipse-production.short"));
        if(shortages.isEmpty()){
            t.add(Core.bundle.get("eclipse-production.short.none")).color(muted).left().padBottom(6f).row();
            return;
        }
        int n = 0;
        for(UnlockableContent c : shortages){
            if(++n > 6) break;
            float m = made(c), u = used(c);
            t.button(b -> {
                b.left();
                b.image(c.uiIcon).size(28f).padRight(8f);
                b.table(txt -> {
                    txt.left().defaults().left();
                    txt.add(c.localizedName).row();
                    txt.add(Core.bundle.format("eclipse-production.made-used", rate(m), rate(u))).color(muted).fontScale(0.8f);
                }).growX();
                b.add(net(m - u, Math.max(m, u))).right().padLeft(8f);
            }, rowEven, () -> select(c, Tab.resources)).growX().height(48f).padBottom(2f).row();
        }
    }

    void fillingUp(Table t, Seq<Item> filling, @Nullable CoreBuild core){
        if(core == null) return;
        WikiUI.header(t, Core.bundle.get("eclipse-production.filling"));
        if(filling.isEmpty()){
            t.add(Core.bundle.get("eclipse-production.filling.none")).color(muted).left().row();
            return;
        }
        int n = 0;
        for(Item i : filling){
            if(++n > 4) break;
            float frac = core.items.get(i) / (float)core.storageCapacity;
            t.button(b -> {
                b.left();
                b.image(i.uiIcon).size(28f).padRight(8f);
                b.table(txt -> {
                    txt.left().defaults().left();
                    txt.add(i.localizedName + "  [lightgray]" + (int)(frac * 100) + "%").row();
                    txt.add(new Bar(frac, frac >= 0.999f ? bad : Color.orange)).width(180f).height(5f).padTop(3f);
                }).growX();
                b.add("+" + rate(net(i))).color(good).right().padLeft(8f);
            }, rowEven, () -> select(i, Tab.resources)).growX().height(48f).padBottom(2f).row();
        }
    }

    // ---------------- left: the material list ----------------

    static final float colName = 190f, colNum = 86f, colCore = 74f, rowHeight = 42f;

    void leftPanel(Table t){
        t.top();
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
                pills.button(Core.bundle.get("eclipse-production.filter." + f.name()), WikiUI.chipStyle(), () -> {
                    filter = f;
                    fillList();
                }).checked(b -> filter == f).height(34f).padRight(6f);
            }
        }).growX().padBottom(8f).row();
        if(tab == Tab.resources){
            t.table(head -> {
                head.left().defaults().left();
                sortHeader(head, Sort.name, "eclipse-production.col.material", colName);
                sortHeader(head, Sort.made, "eclipse-production.col.made", colNum);
                sortHeader(head, Sort.used, "eclipse-production.col.used", colNum);
                sortHeader(head, Sort.net, "eclipse-production.col.net", colNum);
                sortHeader(head, Sort.core, "eclipse-production.col.core", colCore);
            }).growX().padBottom(2f).row();
        }else{
            //chains: every material in the game, whether the base makes it or not
            t.add(Core.bundle.get("eclipse-production.chains.all")).color(accent).left().height(32f).row();
        }
        t.image().color(WikiUI.edge).height(2f).growX().padBottom(4f).row();
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
            case made -> this::made;
            case used -> this::used;
            case net -> this::net;
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
            float m = made(c), u = used(c);
            Button row = new Button(index++ % 2 == 0 ? rowEven : rowOdd);
            row.left();
            if(tab == Tab.chains){
                row.image(c.uiIcon).size(26f).padLeft(8f).padRight(8f);
                row.add(c.localizedName).left().ellipsis(true).growX();
                row.add(Core.bundle.get("eclipse-production.badge." + category(c).name())).color(dim).fontScale(0.75f).right().padRight(12f);
                row.setChecked(selected == c);
                row.clicked(() -> select(c, Tab.chains));
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
            row.clicked(() -> select(c, Tab.resources));
            list.add(row).growX().height(rowHeight).row();
        }
    }

    // ---------------- resources: the selected material ----------------

    /** Icon, name, tags (category, hot), and buttons to its wiki page and its chain. */
    void materialHeader(UnlockableContent c, boolean chainButton){
        right.table(top -> {
            top.left();
            top.table(WikiUI.box(WikiUI.well, WikiUI.edge, 2f, 6f), i -> i.image(c.uiIcon).size(64f)).padRight(14f);
            top.table(n -> {
                n.left().top().defaults().left();
                n.add(c.localizedName).style(Styles.outlineLabel).color(accent).fontScale(1.3f).row();
                n.table(tags -> {
                    tags.left();
                    WikiUI.tag(tags, Core.bundle.get("eclipse-production.badge." + category(c).name()), muted);
                    if(c.name.startsWith("eclipse-hot-")) WikiUI.tag(tags, Core.bundle.get("eclipse-wiki.tag.hot"), WikiUI.heat);
                }).padTop(4f);
            }).growX().left();
            if(EclipseWiki.isEclipse(c)) top.button("@eclipse-production.wiki", Icon.book, Styles.flatt, () -> EclipseWiki.openAt(c, "overview", null)).height(44f).padLeft(8f);
            if(chainButton && c instanceof Item i && !RecipeTree.raw(i) && RecipeTree.sourceOf(i) != null) top.button("@eclipse-production.chain", Icon.tree, Styles.flatt, () -> select(c, Tab.chains)).height(44f).padLeft(4f);
        }).growX().padBottom(10f).row();
    }

    void resourceDetail(){
        UnlockableContent c = selected;
        if(c == null){
            right.add("[lightgray]" + Core.bundle.get("eclipse-production.empty"));
            return;
        }
        float m = made(c), u = used(c), n = m - u;
        materialHeader(c, true);
        CoreBuild core = player == null ? null : player.core();
        right.table(tiles -> {
            tiles.left();
            tile(tiles, Icon.upload, Core.bundle.get("eclipse-production.col.made"), rate(m), madeColor, Core.bundle.format("eclipse-production.total", amount(ProductionStats.totalMade.get(c, 0f))), -1f, madeColor);
            tile(tiles, Icon.download, Core.bundle.get("eclipse-production.col.used"), rate(u), usedColor, Core.bundle.format("eclipse-production.total", amount(ProductionStats.totalUsed.get(c, 0f))), -1f, usedColor);
            tile(tiles, Icon.chartBar, Core.bundle.get("eclipse-production.col.net"), (n > 0 ? "+" : "") + rate(n), netColor(n, Math.max(m, u)),
                Core.bundle.format("eclipse-production.minute", rate(ProductionStats.averageNet(c))), -1f, good);
            if(c instanceof Item i && core != null) tile(tiles, Icon.box, Core.bundle.get("eclipse-production.col.core"), String.valueOf(core.items.get(i)), Color.white,
                Core.bundle.format("eclipse-production.of-capacity", (int)(core.items.get(i) * 100f / core.storageCapacity)), core.items.get(i) / (float)core.storageCapacity, Color.orange);
        }).growX().padBottom(10f).row();

        right.table(WikiUI.card(), g -> {
            g.table(legend -> {
                legend.right();
                legend.image().color(madeColor).size(10f).padRight(4f);
                legend.add(Core.bundle.get("eclipse-production.col.made")).padRight(14f);
                legend.image().color(usedColor).size(10f).padRight(4f);
                legend.add(Core.bundle.get("eclipse-production.col.used"));
            }).growX().row();
            g.add(new LineGraph(() -> ProductionStats.history(c, true), () -> ProductionStats.history(c, false))).grow().minHeight(150f);
        }).growX().height(200f).padBottom(10f).row();

        //which of your machines make and use it
        right.table(cols -> {
            cols.top().left();
            cols.table(l -> shares(l, Core.bundle.get("eclipse-production.made-by"), ProductionStats.madeBy.get(c), ProductionStats.madeByCount.get(c), madeColor)).growX().top().padRight(14f);
            cols.table(l -> shares(l, Core.bundle.get("eclipse-production.used-by"), ProductionStats.usedBy.get(c), ProductionStats.usedByCount.get(c), usedColor)).growX().top();
        }).growX().padBottom(8f).row();

        int kind = advice(c, m, u);
        right.add(WikiUI.callout(kind == 0 ? 3 : kind, "", adviceText(c, m, u), 640f)).growX().left().padTop(6f).row();
    }

    /** Blocks and their share of a rate, as bars: who makes (or uses) a material in your base. */
    void shares(Table t, String title, @Nullable ObjectFloatMap<Block> rates, @Nullable ObjectIntMap<Block> counts, Color color){
        t.top().left();
        WikiUI.header(t, title);
        if(rates == null || rates.size == 0){
            t.add(Core.bundle.get("eclipse-production.nobody")).color(muted).left().row();
            return;
        }
        float total = 0f, max = 0f;
        for(var e : rates){
            total += e.value;
            max = Math.max(max, e.value);
        }
        float fmax = max;
        Seq<Block> blocks = rates.keys().toSeq().sort(Structs.comparingFloat(b -> -rates.get(b, 0f)));
        int n = 0;
        for(Block b : blocks){
            if(++n > 5) break;
            float v = rates.get(b, 0f);
            t.table(r -> {
                r.left();
                r.image(b.uiIcon).size(26f).padRight(8f);
                r.table(txt -> {
                    txt.left().defaults().left();
                    txt.add(b.localizedName + "  [lightgray]×" + (counts == null ? 0 : counts.get(b, 0))).fontScale(0.9f).row();
                    txt.add(new Bar(fmax <= 0f ? 0f : v / fmax, color)).growX().height(5f).padTop(3f);
                }).growX();
                r.add(rate(v)).color(color).right().padLeft(8f);
            }).growX().padBottom(4f).row();
        }
        if(blocks.size > 5) t.add(Core.bundle.format("eclipse-production.attention.more", blocks.size - 5)).color(muted).left().row();
        t.add(Core.bundle.format("eclipse-production.share-total", rate(total))).color(muted).fontScale(0.8f).left().padTop(2f).row();
    }

    /** 0 balanced or idle, 1 surplus (tip), 2 short or none made (warning). */
    int advice(UnlockableContent c, float m, float u){
        float big = Math.max(m, u), n = ProductionStats.averageNet(c);
        if(u > 0.001f && m <= 0.001f) return 2;
        if(n < -0.02f * big) return 2;
        if(n > 0.02f * big) return 1;
        return 0;
    }

    String adviceText(UnlockableContent c, float m, float u){
        float big = Math.max(m, u), n = ProductionStats.averageNet(c);
        String name = c.localizedName;
        if(u > 0.001f && m <= 0.001f) return Core.bundle.format("eclipse-production.advice.none", name);
        if(n < -0.02f * big) return Core.bundle.format("eclipse-production.advice.short", name);
        if(n > 0.02f * big){
            CoreBuild core = player == null ? null : player.core();
            if(c instanceof Item i && core != null && core.items.get(i) >= core.storageCapacity * 0.95f) return Core.bundle.format("eclipse-production.advice.full", name);
            return Core.bundle.format("eclipse-production.advice.surplus", name);
        }
        if(big <= 0.001f) return Core.bundle.format("eclipse-production.advice.idle", name);
        return Core.bundle.format("eclipse-production.advice.balanced", name);
    }

    // ---------------- chains ----------------

    void chainView(){
        UnlockableContent c = selected;
        if(c == null){
            right.add("[lightgray]" + Core.bundle.get("eclipse-production.empty"));
            return;
        }
        materialHeader(c, false);
        if(!(c instanceof Item item) || RecipeTree.raw(item) || RecipeTree.sourceOf(item) == null){
            rawChain(c);
            return;
        }
        if(chainRate <= 0f) chainRate = Math.max(Math.max(used(c), made(c)), 1f);
        RecipeTree.Step root = RecipeTree.plan(item, chainRate);
        ChainGraph graph = new ChainGraph(root, x -> {
            if(x != c) select(x, Tab.chains);
        });
        graph.zoom = chainZoom;
        graph.onZoom = z -> chainZoom = z;
        right.table(top -> {
            top.left();
            top.add(Core.bundle.get("eclipse-wiki.tree.rate") + " ").color(muted);
            top.field(Strings.autoFixed(chainRate, 2), TextField.TextFieldFilter.floatsOnly, text -> {
                float v = Strings.parseFloat(text, -1f);
                if(v > 0f && v <= 10000f) chainRate = v;
            }).width(100f);
            top.add(" /s").padRight(10f);
            top.button(Icon.refresh, Styles.cleari, this::refreshData).size(40f);
            top.add(Core.bundle.get("eclipse-production.chains.rate-hint")).color(muted).fontScale(0.8f).padLeft(10f);
        }).growX().padBottom(8f).row();

        right.pane(Styles.smallPane, graph).grow().left().row();
        int tier = RecipeTree.maxTier(root);
        right.table(t -> {
            t.left();
            WikiUI.tag(t, Core.bundle.format("eclipse-wiki.chain.power", Strings.autoFixed(RecipeTree.power(root) * 60f, 0)), accent);
            if(tier > 0) WikiUI.tag(t, Core.bundle.format("eclipse-wiki.tag.tier-power", RecipeCrafter.roman(tier), Strings.autoFixed(RecipeCrafter.tierPower[tier] * 60f, 0)), WikiUI.tierColor(tier));
        }).left().padTop(8f).row();
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
        WikiUI.header(right, Core.bundle.get("eclipse-production.chains.used-in"));
        Table grid = new Table().top().left();
        int n = 0;
        for(UnlockableContent p : products){
            grid.button(b -> {
                b.left();
                b.image(p.uiIcon).size(28f).padLeft(8f).padRight(8f);
                b.add(p.localizedName).left().ellipsis(true).width(150f);
            }, rowEven, () -> select(p, Tab.chains)).size(210f, 44f).pad(3f);
            if(++n % 3 == 0) grid.row();
        }
        right.pane(Styles.smallPane, grid).grow().top();
    }

    // ---------------- power ----------------

    void powerView(){
        float m = ProductionStats.powerMade, u = ProductionStats.powerUsed, sat = ProductionStats.satisfaction;
        right.table(tiles -> {
            tiles.left();
            tile(tiles, Icon.upload, Core.bundle.get("eclipse-production.col.made"), power(m), madeColor, Core.bundle.format("eclipse-production.generators-n", count(ProductionStats.generatorCount)), -1f, madeColor);
            tile(tiles, Icon.download, Core.bundle.get("eclipse-production.col.used"), power(u), usedColor, Core.bundle.format("eclipse-production.consumers-n", count(ProductionStats.consumerCount)), -1f, usedColor);
            tile(tiles, Icon.chartBar, Core.bundle.get("eclipse-production.col.net"), (m - u > 0 ? "+" : "") + power(m - u), netColor(m - u, Math.max(m, u)), "", -1f, good);
            float cap = ProductionStats.powerCapacity;
            tile(tiles, Icon.box, Core.bundle.get("eclipse-production.stored"), shortNumber(ProductionStats.powerStored), Color.white,
                Core.bundle.format("eclipse-production.of-capacity-power", shortNumber(cap)), cap <= 0f ? 0f : ProductionStats.powerStored / cap, accent);
            tile(tiles, Icon.power, Core.bundle.get("eclipse-production.satisfaction"), (int)(sat * 100) + "%", sat < 0.999f ? bad : good,
                Core.bundle.get(sat < 0.999f ? "eclipse-production.satisfaction.low" : "eclipse-production.satisfaction.ok"), sat, sat < 0.999f ? bad : good);
        }).growX().padBottom(10f).row();
        right.table(WikiUI.card(), g -> {
            g.table(legend -> {
                legend.right();
                legend.image().color(madeColor).size(10f).padRight(4f);
                legend.add(Core.bundle.get("eclipse-production.col.made")).padRight(14f);
                legend.image().color(usedColor).size(10f).padRight(4f);
                legend.add(Core.bundle.get("eclipse-production.col.used"));
            }).growX().row();
            g.add(new LineGraph(() -> ProductionStats.powerHistory(true), () -> ProductionStats.powerHistory(false))).grow().minHeight(150f);
        }).growX().height(200f).padBottom(10f).row();
        right.table(lists -> {
            lists.top().left();
            lists.table(l -> powerShares(l, "eclipse-production.generators", ProductionStats.powerByGenerator, ProductionStats.generatorCount, madeColor)).grow().top().padRight(16f);
            lists.table(l -> powerShares(l, "eclipse-production.consumers", ProductionStats.powerByConsumer, ProductionStats.consumerCount, usedColor)).grow().top();
        }).grow();
    }

    static int count(ObjectIntMap<Block> counts){
        int n = 0;
        for(var e : counts) n += e.value;
        return n;
    }

    void powerShares(Table t, String key, ObjectFloatMap<Block> power, ObjectIntMap<Block> counts, Color color){
        t.top().left();
        WikiUI.header(t, Core.bundle.get(key));
        Seq<Block> blocks = power.keys().toSeq().sort(Structs.comparingFloat(b -> -power.get(b, 0f)));
        if(blocks.isEmpty()){
            t.add("[lightgray]-").left().row();
            return;
        }
        float total = 0f;
        for(var e : power) total += e.value;
        Table rows = new Table().top().left();
        for(Block b : blocks){
            float v = power.get(b, 0f), share = total <= 0f ? 0f : v / total;
            rows.table(r -> {
                r.left();
                r.image(b.uiIcon).size(26f).padRight(8f);
                r.table(txt -> {
                    txt.left().defaults().left();
                    txt.add(b.localizedName + "  [lightgray]×" + counts.get(b, 0) + "  " + (int)(share * 100) + "%").fontScale(0.9f).row();
                    txt.add(new Bar(share, color)).growX().height(5f).padTop(3f);
                }).growX();
                r.add(power(v)).color(color).right().padLeft(8f);
            }).growX().padBottom(4f).row();
        }
        t.pane(Styles.smallPane, rows).grow().top().get().setScrollingDisabledX(true);
    }

    // ---------------- numbers ----------------

    float made(UnlockableContent c){
        return ProductionStats.made.get(c, 0f);
    }

    float used(UnlockableContent c){
        return ProductionStats.used.get(c, 0f);
    }

    float net(UnlockableContent c){
        return made(c) - used(c);
    }

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

    /** 950, 12.5k, 3.4mil, 1.2bil: short enough for graph scales and tiles at any power level. */
    static String shortNumber(float v){
        float a = Math.abs(v);
        String sign = v < 0f ? "-" : "";
        if(a >= 1e9f) return sign + Strings.autoFixed(a / 1e9f, 1) + "bil";
        if(a >= 1e6f) return sign + Strings.autoFixed(a / 1e6f, 1) + "mil";
        if(a >= 1e4f) return sign + Strings.autoFixed(a / 1e3f, 1) + "k";
        return sign + Strings.autoFixed(a, a >= 10f ? 0 : 2);
    }

    // ---------------- graphs ----------------

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
            Fill.crect(x, y0, w * frac, h);
            Draw.reset();
        }
    }

    /** Made and used over the last minute, with a 0..max scale on the left and a soft fill under each line. */
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
            Font font = EclipseFonts.get(EclipseFonts.Kind.outline, EclipseFonts.basePx() * 0.8f);
            for(int i = 0; i <= 4; i++){
                float yy = py + ph * i / 4f;
                Draw.color(grid, parentAlpha);
                Lines.stroke(Scl.scl(1f));
                Lines.line(px, yy, px + pw, yy);
                font.draw(shortNumber(max * i / 4f), px - Scl.scl(6f), yy + Scl.scl(6f), muted, 1f, true, Align.right);
            }
            series(da, madeColor, px, py, pw, ph, max);
            series(db, usedColor, px, py, pw, ph, max);
            Draw.reset();
        }

        void series(float[] d, Color c, float px, float py, float pw, float ph, float max){
            if(d.length < 2) return;
            float step = pw / (ProductionStats.historySize - 1);
            float x0 = px + pw - step * (d.length - 1);
            //a faint fill under the line, then the line
            Draw.color(c, 0.12f * parentAlpha);
            for(int i = 1; i < d.length; i++){
                float xa = x0 + step * (i - 1), xb = x0 + step * i, ya = py + ph * d[i - 1] / max, yb = py + ph * d[i] / max;
                Fill.quad(xa, py, xa, ya, xb, yb, xb, py);
            }
            Draw.color(c, parentAlpha);
            Lines.stroke(Scl.scl(2f));
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
