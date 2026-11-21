package eclipse.ui;

import arc.*;
import arc.graphics.*;
import arc.input.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import eclipse.augment.*;
import eclipse.world.blocks.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.abilities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.heat.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.units.*;
import mindustry.world.consumers.*;

import static mindustry.Vars.*;

/**
 * The ECLIPSE wiki, opened from the pause menu or with I over a block. Every page has a header that answers the first
 * questions at a glance (kind, tier, heat, research, key numbers) and tabs instead of one long scroll:
 * - machines: Overview, Recipes (cards with rates), Build examples (animated, ratio-correct, buildable: {@link BuildDiagram},
 *   {@link LayoutGen}), Logic;
 * - materials: Overview (where to get it), Chain (the production calculator as a flow, {@link ChainGraph}), Uses;
 * - units: Overview with when they come and how to counter them;
 * - guides: one tab per chapter, with callouts and, in Getting Started, checklists that read the player's base.
 * The sidebar has search that knows recipes ({@link WikiSearch}), pinned and recent pages, and collapsible groups.
 * Back and forward work like a browser (Alt+arrows); Ctrl+F searches, P pins, B builds the example shown, 1-9 pick a tab.
 * Everything numeric is read from the game, so it can't go out of date.
 */
public class EclipseWiki extends BaseDialog{
    /** Guide pages, in menu order: id (bundle key eclipse-wiki.guide.&lt;id&gt;.name / .text) and related content names. */
    public static final String[][] guides = {
        {"start", "eclipse-gear-press", "eclipse-extruder", "eclipse-rivet", "eclipse-flow-meter", "eclipse-metal-press", "eclipse-component-depot", "eclipse-water-treatment-plant", "eclipse-augmentation-coupler", "eclipse-ore-crusher"},
        {"industry", "eclipse-gear-press", "eclipse-assembler", "eclipse-circuit-assembler", "eclipse-blast-furnace", "eclipse-electric-furnace", "eclipse-electrolyzer", "eclipse-chemical-reactor", "eclipse-pressure-reactor", "eclipse-vacuum-freezer", "eclipse-flotation-cell"},
        {"qol", "eclipse-flow-meter", "eclipse-component-depot", "eclipse-wave-intelligence-scanner"},
        {"components", "eclipse-titanium-gear", "eclipse-industrial-bearing", "eclipse-precision-component", "eclipse-superalloy-frame", "eclipse-experimental-core", "eclipse-gear-press", "eclipse-component-depot"},
        {"metallurgy", "eclipse-ore-nickel", "eclipse-ore-chromium", "eclipse-ore-cobalt", "eclipse-ore-tungsten", "eclipse-ore-molybdenum", "eclipse-ore-platinum", "eclipse-ore-bauxite", "eclipse-ore-crusher", "eclipse-carbide-drill"},
        {"heat", "eclipse-industrial-heater", "eclipse-electric-heater", "eclipse-geothermal-heat-tap", "eclipse-thermal-storage", "eclipse-heat-exchanger"},
        {"chemistry", "eclipse-sulfuric-acid", "eclipse-acidic-waste", "eclipse-polymer-conduit"},
        {"electronics", "eclipse-copper-wire", "eclipse-circuit-board", "eclipse-microchip", "eclipse-control-unit"},
        {"power", "eclipse-steam-turbine", "eclipse-combined-cycle-turbine", "eclipse-fission-reactor", "eclipse-breeder-reactor", "eclipse-fusion-reactor", "eclipse-industrial-battery", "eclipse-world-engine"},
        {"logistics", "eclipse-three-way-sorter", "eclipse-distribution-hub", "eclipse-priority-merger", "eclipse-controlled-splitter", "eclipse-line-balancer", "eclipse-smart-unloader", "eclipse-batch-gate", "eclipse-liquid-overflow-valve"},
        {"augmentation", "eclipse-augmentation-coupler", "eclipse-basic-module-assembler", "eclipse-production-accelerator-1", "eclipse-module-recycling-facility"},
        {"robotics", "eclipse-robot-dispatch-hub", "eclipse-logistics-drone", "eclipse-repair-drone", "eclipse-shield-drone"},
        {"defense", "eclipse-wave-intelligence-scanner", "eclipse-wave-disruption-array", "eclipse-unit-shield-beacon", "eclipse-emergency-repair-dispatch-bay"},
        {"military", "eclipse-acid-mortar", "eclipse-heat-lance", "eclipse-minelayer", "eclipse-jammer", "eclipse-sonar", "eclipse-piledriver", "eclipse-bombard", "eclipse-payload-catapult", "eclipse-casing-fabricator", "eclipse-kestrel", "eclipse-thunderhead", "eclipse-cloudbreaker", "eclipse-rivet", "eclipse-sickle", "eclipse-tremor", "eclipse-apiary", "eclipse-primer", "eclipse-stardust-launcher", "eclipse-event-horizon", "eclipse-reaper", "eclipse-wraith", "eclipse-orbital-weapons-platform", "eclipse-titan-manufacturing-complex"},
        {"factions", "eclipse-mite", "eclipse-wasp", "eclipse-spitter", "eclipse-brood", "eclipse-behemoth", "eclipse-scrabba", "eclipse-skiff", "eclipse-slicker", "eclipse-frostwake", "eclipse-lamprey", "eclipse-cinderwake", "eclipse-maelstrom", "eclipse-lurker", "eclipse-barracuda", "eclipse-abyss", "eclipse-colossus", "eclipse-leviathan"},
        {"exploration", "eclipse-aetherite", "eclipse-ore-aetherite", "eclipse-aetherite-module-forge", "eclipse-cooling-tower", "eclipse-biomass-cultivator"},
        {"logic", "eclipse-assembler", "eclipse-orbital-weapons-platform", "eclipse-wave-intelligence-scanner", "eclipse-world-engine", "eclipse-flow-meter", "eclipse-augmentation-coupler"},
        {"compare", "eclipse-steam-turbine", "eclipse-fusion-reactor", "eclipse-apiary", "eclipse-colossus"},
    };

    /** The icon of each guide in the menu: an ECLIPSE content name whose icon stands for the topic (start uses a vanilla icon). */
    public static final String[][] guideIcons = {
        {"industry", "eclipse-assembler"},
        {"qol", "eclipse-flow-meter"},
        {"components", "eclipse-titanium-gear"},
        {"metallurgy", "eclipse-superalloy"},
        {"heat", "eclipse-industrial-heater"},
        {"chemistry", "eclipse-sulfuric-acid"},
        {"electronics", "eclipse-microchip"},
        {"power", "eclipse-industrial-battery"},
        {"logistics", "eclipse-three-way-sorter"},
        {"augmentation", "eclipse-production-accelerator-1"},
        {"robotics", "eclipse-logistics-drone"},
        {"defense", "eclipse-unit-shield-beacon"},
        {"military", "eclipse-acid-mortar"},
        {"factions", "eclipse-brood"},
        {"exploration", "eclipse-aetherite"},
        {"logic", "micro-processor"},
        {"compare", "eclipse-steam-turbine"},
    };

    static final String pinsKey = "eclipse-wiki-pins", recentKey = "eclipse-wiki-recent";

    /** A page and the tab shown on it. */
    static class Place{
        final Object target;
        @Nullable String tab;

        Place(Object target, @Nullable String tab){
            this.target = target;
            this.tab = tab;
        }
    }

    /** One tab of a page. */
    static class Tab{
        final String id, label;
        final Runnable build;

        Tab(String id, String label, Runnable build){
            this.id = id;
            this.label = label;
            this.build = build;
        }
    }

    final Seq<Place> back = new Seq<>(), forward = new Seq<>();
    @Nullable Place current;
    Seq<Tab> tabs = new Seq<>();
    String query = "";
    /** Catalogue shows only what the player has unlocked (campaign); everything is unlocked elsewhere. */
    boolean unlockedOnly;
    /** Target rate of the production calculator on item pages, per second. */
    float treeRate = 1f;
    final Table list = new Table(), head = new Table(), tabBar = new Table(), page = new Table();
    final ObjectSet<String> openGroups = ObjectSet.with("eclipse-wiki.guides");
    ScrollPane pageScroll, listScroll;
    TextField search;

    //build tab state
    final ObjectMap<Block, Seq<BuildLayout>> examples = new ObjectMap<>();
    int buildIndex;
    int buildStep;
    boolean buildRates = true;
    @Nullable BuildDiagram diagram;
    /** An example asked for before its page opens (recipe menu, photo mode); applied after the page resets. */
    int pendingBuild = -1, pendingStep = -1;
    @Nullable Table caption;
    /** Set when the wiki was opened with I over a stopped machine: why it stopped, shown on its page. */
    @Nullable MachineStatus.Status stopped;
    @Nullable Block stoppedBlock;

    /** Adds the wiki button to the pause menu. Call on ClientLoadEvent. */
    public static @Nullable EclipseWiki instance;
    /** Opens the wiki on whatever ECLIPSE block or unit is under the cursor (or selected for building). I by default. */
    public static @Nullable KeyBind entryKey;

    /** Registers the key early (mod constructor) so the Controls menu lists it. Desktop clients only. */
    public static void registerKey(){
        if(headless || mobile) return;
        entryKey = KeyBind.add("eclipse_wiki_entry", KeyCode.i, "eclipse");
        Events.run(mindustry.game.EventType.Trigger.update, () -> {
            if(instance == null || !Core.input.keyTap(entryKey) || Core.scene.getKeyboardFocus() instanceof TextField) return;
            if(instance.isShown()){
                instance.hide();
                return;
            }
            //a stopped machine under the cursor opens with why it stopped
            Building b = state.isGame() ? world.buildWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y) : null;
            instance.stopped = null;
            instance.stoppedBlock = null;
            if(b != null && isEclipse(b.block)){
                MachineStatus.Status s = MachineStatus.of(b);
                if(s.problem()){
                    instance.stopped = new MachineStatus.Status().copy(s);
                    instance.stoppedBlock = b.block;
                }
            }
            UnlockableContent c = hoveredContent();
            instance.show();
            if(c != null) instance.open(c, null, true);
        });
    }

    /** Opens the wiki on a page and tab (the recipe menu's "?" opens a machine's recipes or its build example). */
    public static void openAt(UnlockableContent c, String tab, @Nullable Object recipe){
        if(instance == null) return;
        instance.show();
        if(recipe != null && c instanceof Block b){
            Seq<BuildLayout> ex = instance.examplesOf(b);
            int i = ex.indexOf(l -> l.recipe == recipe);
            if(i >= 0) instance.pendingBuild = i;
        }
        instance.open(c, tab, true);
    }

    /** The ECLIPSE block or unit under the cursor, else the ECLIPSE block selected in the build menu. */
    static @Nullable UnlockableContent hoveredContent(){
        if(!state.isGame()) return null;
        arc.math.geom.Vec2 m = Core.input.mouseWorld();
        Unit unit = Groups.unit.find(u -> u.within(m, u.hitSize / 2f + 2f) && isEclipse(u.type));
        if(unit != null) return unit.type;
        Building b = world.buildWorld(m.x, m.y);
        if(b != null && isEclipse(b.block)) return b.block;
        Block selected = control.input.block;
        return selected != null && isEclipse(selected) ? selected : null;
    }

    public static void install(){
        EclipseWiki wiki = instance = new EclipseWiki();
        //the pause menu rebuilds its buttons every time it opens; this listener runs after that rebuild
        ui.paused.shown(() -> {
            Table cont = ui.paused.cont;
            TextButton button = new Table().button("@eclipse-wiki", Icon.book, wiki::show).get();
            button.remove();
            //take the place of "Save & Quit" (or "Quit"), and move that button to a new row right below with the same layout
            Cell<?> quitCell = cont.getCells().find(c -> c.get() instanceof TextButton b && isQuit(b));
            if(quitCell != null){
                arc.scene.Element quit = quitCell.get();
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
        });
    }

    static boolean isQuit(TextButton b){
        String text = b.getText().toString();
        return text.equals(Core.bundle.get("save.quit")) || text.equals(Core.bundle.get("quit"));
    }

    public EclipseWiki(){
        super("@eclipse-wiki.title");
        addCloseButton();
        buttons.button("@eclipse-production", Icon.chartBar, ProductionStats::show).disabled(b -> !state.isGame()).size(210f, 64f);

        cont.table(left -> {
            left.top();
            left.table(s -> {
                s.image(Icon.zoom).padRight(6f);
                search = s.field("", text -> {
                    query = text.trim().toLowerCase();
                    buildList();
                }).growX().get();
                search.setMessageText(Core.bundle.get("eclipse-wiki.search"));
            }).growX().padBottom(6f).row();
            left.table(f -> {
                f.left();
                f.button("@eclipse-wiki.filter.all", WikiUI.chipStyle(), () -> { unlockedOnly = false; buildList(); }).checked(b -> !unlockedOnly).height(32f).padRight(6f);
                f.button("@eclipse-wiki.filter.unlocked", WikiUI.chipStyle(), () -> { unlockedOnly = true; buildList(); }).checked(b -> unlockedOnly).height(32f);
            }).left().padBottom(6f).row();
            listScroll = left.pane(Styles.smallPane, list).grow().get();
            listScroll.setScrollingDisabledX(true);
        }).width(mobile ? 250f : 330f).growY().padRight(12f);

        cont.table(right -> {
            right.top().left();
            right.table(nav -> {
                nav.left();
                nav.button(Icon.left, Styles.clearNonei, this::back).size(40f).disabled(b -> back.isEmpty()).tooltip("@eclipse-wiki.back");
                nav.button(Icon.right, Styles.clearNonei, this::goForward).size(40f).disabled(b -> forward.isEmpty()).tooltip("@eclipse-wiki.forward").padRight(8f);
                nav.label(this::crumbs).color(Color.lightGray).left().growX();
                nav.button("", WikiUI.chipStyle(), this::togglePin).update(b -> {
                    boolean pinned = current != null && pinned(current.target);
                    b.setText(Core.bundle.get(pinned ? "eclipse-wiki.pinned-on" : "eclipse-wiki.pin"));
                    b.setChecked(pinned);
                }).height(34f).minWidth(110f);
            }).growX().padBottom(6f).row();
            right.add(head).growX().padBottom(6f).row();
            right.add(tabBar).growX().row();
            right.image().color(WikiUI.edge).height(3f).growX().padBottom(8f).row();
            pageScroll = right.pane(page).grow().get();
            pageScroll.setScrollingDisabledX(true);
        }).grow();

        //keys: Ctrl+F search, Alt+arrows back/forward, P pin, B build this, 1-9 tabs
        keyDown(key -> {
            boolean typing = Core.scene.getKeyboardFocus() instanceof TextField;
            if(key == KeyCode.f && Core.input.ctrl()){
                Core.scene.setKeyboardFocus(search);
                search.selectAll();
            }else if(key == KeyCode.left && Core.input.alt()) back();
            else if(key == KeyCode.right && Core.input.alt()) goForward();
            else if(!typing && key == KeyCode.p) togglePin();
            else if(!typing && key == KeyCode.b && current != null && "build".equals(current.tab)) buildThis();
            else if(!typing && key.ordinal() >= KeyCode.num1.ordinal() && key.ordinal() <= KeyCode.num9.ordinal()){
                int i = key.ordinal() - KeyCode.num1.ordinal();
                if(current != null && i < tabs.size) open(current.target, tabs.get(i).id, false);
            }
        });

        shown(() -> {
            buildList();
            if(current == null) open(guides[0][0], null, false);
            else open(current.target, current.tab, false);
        });
    }

    // ---------------- navigation ----------------

    /** Fills the search box (photo mode). */
    public void searchFor(String text){
        search.setText(text);
        query = text.trim().toLowerCase();
        buildList();
    }

    /** Opens a guide (String id) or a content entry on its first tab. */
    public void open(Object target, boolean remember){
        open(target, null, remember);
    }

    public void open(Object target, @Nullable String tab, boolean remember){
        if(remember && current != null && !(current.target == target && (tab == null || tab.equals(current.tab)))){
            back.add(current);
            forward.clear();
        }
        if(current == null || current.target != target){
            if(!(target instanceof Block b && b == stoppedBlock)){
                stopped = null;
                stoppedBlock = null;
            }
            if(!(current != null && current.target == target)) diagramReset();
        }
        if(pendingBuild >= 0){
            buildIndex = pendingBuild;
            buildStep = 0;
            pendingBuild = -1;
        }
        if(pendingStep >= 0){
            buildStep = pendingStep;
            pendingStep = -1;
        }
        tabs = tabsOf(target);
        String id = tab != null && tabs.contains(t -> t.id.equals(tab)) ? tab : tabs.any() ? tabs.first().id : "overview";
        current = new Place(target, id);
        remember(target);
        buildHead(target);
        buildTabs();
        page.clear();
        page.top().left().defaults().left();
        Tab t = tabs.find(x -> x.id.equals(id));
        if(t != null) t.build.run();
        pageScroll.setScrollY(0f);
        buildList();
    }

    void diagramReset(){
        buildIndex = 0;
        buildStep = 0;
    }

    void back(){
        if(back.isEmpty() || current == null) return;
        forward.add(current);
        Place p = back.pop();
        open(p.target, p.tab, false);
    }

    void goForward(){
        if(forward.isEmpty() || current == null) return;
        back.add(current);
        Place p = forward.pop();
        open(p.target, p.tab, false);
    }

    String crumbs(){
        if(current == null) return "";
        Object t = current.target;
        String trail;
        if(t instanceof String id) trail = Core.bundle.get("eclipse-wiki.guides") + "  ›  " + Core.bundle.get("eclipse-wiki.guide." + id + ".name");
        else if(t instanceof UnlockableContent c) trail = section(c) + "  ›  " + c.localizedName;
        else trail = "";
        Tab tab = tabs.find(x -> x.id.equals(current.tab));
        return tab != null && tabs.size > 1 ? trail + "  ›  " + tab.label.replaceAll(" \\[lightgray\\]\\d+$", "") : trail;
    }

    static String section(UnlockableContent c){
        if(c instanceof AugmentModule) return Core.bundle.get("eclipse-wiki.modules");
        if(c instanceof Item) return Core.bundle.get("eclipse-wiki.items");
        if(c instanceof Liquid) return Core.bundle.get("eclipse-wiki.liquids");
        if(c instanceof UnitType) return Core.bundle.get("eclipse-wiki.units");
        if(c instanceof OreBlock) return Core.bundle.get("eclipse-wiki.ores");
        if(c instanceof Block b) return Core.bundle.get("eclipse-wiki.cat." + b.category.name());
        return "";
    }

    // ---------------- pins and recent pages (kept in the settings) ----------------

    static String key(Object target){
        return target instanceof String id ? "guide:" + id : ((UnlockableContent)target).name;
    }

    static @Nullable Object target(String key){
        if(key.startsWith("guide:")){
            String id = key.substring(6);
            for(String[] g : guides) if(g[0].equals(id)) return id;
            return null;
        }
        return byName(key);
    }

    static Seq<String> keys(String setting){
        String s = Core.settings.getString(setting, "");
        return s.isEmpty() ? new Seq<>() : Seq.with(s.split(","));
    }

    boolean pinned(Object target){
        return keys(pinsKey).contains(key(target));
    }

    void togglePin(){
        if(current == null) return;
        Seq<String> pins = keys(pinsKey);
        String k = key(current.target);
        if(!pins.remove(k)) pins.add(k);
        Core.settings.put(pinsKey, pins.toString(","));
        buildList();
    }

    void remember(Object target){
        Seq<String> recent = keys(recentKey);
        String k = key(target);
        recent.remove(k);
        recent.insert(0, k);
        while(recent.size > 6) recent.pop();
        Core.settings.put(recentKey, recent.toString(","));
    }

    // ---------------- left: search, pinned, recent and the catalogue ----------------

    void buildList(){
        list.clear();
        list.top().left().defaults().left().growX();

        if(!query.isEmpty()){
            Seq<WikiSearch.Group> groups = WikiSearch.search(query, unlockedOnly);
            if(groups.isEmpty()) list.add("[lightgray]" + Core.bundle.get("eclipse-wiki.search.none")).wrap().width(280f).padTop(8f).row();
            for(WikiSearch.Group g : groups){
                groupHeader(g.key, g.hits.size, false, null);
                for(WikiSearch.Hit h : g.hits){
                    if(h.guide != null) guideRow(h.guide, h.reason);
                    else entryRow(h.content, h.reason);
                }
            }
            return;
        }

        Seq<String> pins = keys(pinsKey), recent = keys(recentKey);
        if(pins.any()){
            groupHeader("eclipse-wiki.pinned", -1, false, null);
            for(String k : pins) row(target(k));
        }
        if(recent.size > 1){
            groupHeader("eclipse-wiki.recent", -1, false, null);
            for(String k : recent) if(current == null || !k.equals(key(current.target))) row(target(k));
        }

        boolean guidesOpen = openGroups.contains("eclipse-wiki.guides") || current != null && current.target instanceof String;
        groupHeader("eclipse-wiki.guides", guides.length, true, guidesOpen);
        if(guidesOpen) for(String[] g : guides) guideRow(g[0], null);

        section("eclipse-wiki.items", all(c -> c instanceof Item && !(c instanceof AugmentModule)));
        section("eclipse-wiki.modules", all(c -> c instanceof AugmentModule));
        section("eclipse-wiki.liquids", all(c -> c instanceof Liquid));
        for(Category cat : Category.all){
            section("eclipse-wiki.cat." + cat.name(), all(c -> c instanceof Block b && !(b instanceof OreBlock) && b.category == cat && b.isVisible()));
        }
        section("eclipse-wiki.ores", all(c -> c instanceof OreBlock));
        section("eclipse-wiki.units", all(c -> c instanceof UnitType));
    }

    void row(@Nullable Object target){
        if(target instanceof String id) guideRow(id, null);
        else if(target instanceof UnlockableContent c) entryRow(c, null);
    }

    /** A group heading; a foldable one toggles open and closed. */
    void groupHeader(String key, int count, boolean foldable, @Nullable Boolean open){
        list.button(b -> {
            b.left();
            if(foldable) b.image(open ? Icon.downOpen : Icon.rightOpen).size(14f).color(Color.lightGray).padRight(6f);
            b.add(Core.bundle.get(key)).color(Pal.accent).left().growX();
            if(count >= 0) b.add(String.valueOf(count)).color(WikiUI.dim).right().padRight(4f);
        }, Styles.flatt, () -> {
            if(!foldable) return;
            if(!openGroups.remove(key)) openGroups.add(key);
            buildList();
        }).height(34f).padTop(6f).row();
        list.image().color(WikiUI.edge).height(2f).growX().padBottom(3f).row();
    }

    /** All ECLIPSE content matching the filter (and the unlocked filter). */
    Seq<UnlockableContent> all(arc.func.Boolf<UnlockableContent> filter){
        Seq<UnlockableContent> out = new Seq<>();
        for(ContentType type : new ContentType[]{ContentType.item, ContentType.liquid, ContentType.block, ContentType.unit}){
            for(Content c : content.getBy(type)){
                if(c instanceof UnlockableContent u && isEclipse(u) && filter.get(u) && (!unlockedOnly || u.unlockedNow())) out.add(u);
            }
        }
        return out;
    }

    void section(String key, Seq<UnlockableContent> entries){
        if(entries.isEmpty()) return;
        boolean open = openGroups.contains(key) || current != null && current.target instanceof UnlockableContent c && entries.contains(c);
        groupHeader(key, entries.size, true, open);
        if(open) for(UnlockableContent c : entries) entryRow(c, null);
    }

    void entryRow(UnlockableContent c, @Nullable String reason){
        Button b = list.button(bt -> {
            bt.left();
            bt.image(c.uiIcon).size(24f).padRight(8f);
            bt.add(c.localizedName).left().growX().wrap();
            if(reason != null && !reason.isEmpty()) bt.add(reason).color(WikiUI.dim).fontScale(0.75f).right().padLeft(6f);
            else tierBadge(bt, c);
        }, Styles.flatTogglet, () -> open(c, null, true)).checked(x -> current != null && current.target == c).height(36f).padLeft(4f).get();
        list.row();
        WikiUI.hover(b, c);
    }

    void guideRow(String id, @Nullable String reason){
        list.button(b -> {
            b.left();
            b.image(guideIcon(id)).size(24f).padRight(8f);
            b.add(Core.bundle.get("eclipse-wiki.guide." + id + ".name")).left().growX().wrap();
            if(reason != null && !reason.isEmpty()) b.add(reason).color(WikiUI.dim).fontScale(0.75f).right().padLeft(6f);
        }, Styles.flatTogglet, () -> open(id, null, true)).checked(x -> current != null && id.equals(current.target)).height(36f).padLeft(4f).row();
    }

    /** "II–V" in the colour of the highest tier, for universal machines. */
    static void tierBadge(Table t, UnlockableContent c){
        if(!(c instanceof RecipeCrafter rc) || rc.recipes.isEmpty()) return;
        int lo = 9, hi = 0;
        for(RecipeCrafter.Recipe r : rc.recipes){
            lo = Math.min(lo, r.tier);
            hi = Math.max(hi, r.tier);
        }
        String text = lo == hi ? RecipeCrafter.roman(hi) : RecipeCrafter.roman(lo) + "–" + RecipeCrafter.roman(hi);
        WikiUI.tag(t, text, WikiUI.tierColor(hi));
    }

    // ---------------- the page header ----------------

    void buildHead(Object target){
        head.clear();
        head.background(WikiUI.card());
        head.left();
        if(target instanceof String id){
            head.image(guideIcon(id)).size(56f).padRight(14f);
            head.table(n -> {
                n.left().defaults().left();
                n.add(Core.bundle.get("eclipse-wiki.guide." + id + ".name")).style(Styles.outlineLabel).color(Pal.accent).fontScale(1.25f).row();
                n.add(Core.bundle.format("eclipse-wiki.guide.kind", tabs.size)).color(Color.lightGray);
            }).growX();
            return;
        }
        UnlockableContent c = (UnlockableContent)target;
        head.table(WikiUI.box(WikiUI.well, WikiUI.edge, 2f, 6f), i -> i.image(c.uiIcon).size(mobile ? 52f : 72f)).padRight(14f);
        head.table(n -> {
            n.left().defaults().left();
            n.add(c.localizedName).style(Styles.outlineLabel).color(Pal.accent).fontScale(1.3f).row();
            n.add(WikiUI.kindLine(c)).color(Color.lightGray).row();
            n.table(tags -> {
                tags.left();
                tagsOf(tags, c);
            }).left().padTop(4f);
        }).growX();
        Table stats = new Table();
        stats.right().defaults().padLeft(10f);
        statsOf(stats, c);
        if(stats.hasChildren() && !mobile){
            head.image().color(WikiUI.edge).width(2f).growY().padLeft(10f).padRight(10f);
            head.add(stats).right();
        }
    }

    void tagsOf(Table t, UnlockableContent c){
        if(c instanceof RecipeCrafter rc){
            boolean[] tierUsed = new boolean[6];
            float lo = Float.MAX_VALUE, hi = 0f;
            for(RecipeCrafter.Recipe r : rc.recipes){
                tierUsed[r.tier] = true;
                if(r.heat > 0f){
                    lo = Math.min(lo, r.heat);
                    hi = Math.max(hi, r.heat);
                }
            }
            for(int i = 1; i < 6; i++) if(tierUsed[i]) WikiUI.tag(t, Core.bundle.format("eclipse-wiki.tag.tier", RecipeCrafter.roman(i)), WikiUI.tierColor(i));
            if(hi > 0f) WikiUI.tag(t, Core.bundle.format("eclipse-wiki.tag.heat", Strings.autoFixed(lo, 1) + (hi > lo ? "–" + Strings.autoFixed(hi, 1) : "")), WikiUI.heat);
            WikiUI.tag(t, Core.bundle.format("eclipse-wiki.tag.recipes", rc.recipes.size), Color.lightGray);
        }else if(c instanceof HeatCrafter hc && hc.heatRequirement > 0f){
            WikiUI.tag(t, Core.bundle.format("eclipse-wiki.tag.heat", Strings.autoFixed(hc.heatRequirement, 1)), WikiUI.heat);
        }
        if(c instanceof Item i && i.name.startsWith("eclipse-hot-")) WikiUI.tag(t, Core.bundle.get("eclipse-wiki.tag.hot"), WikiUI.heat);
        if(c instanceof Item i && RecipeTree.raw(i)) WikiUI.tag(t, Core.bundle.get("eclipse-wiki.tag.raw"), Color.lightGray);
        if(c instanceof UnitType u && GuideChapters.schedule(u).any()) WikiUI.tag(t, Core.bundle.get("eclipse-wiki.tag.enemy"), WikiUI.bad);
        if(c.techNode != null) WikiUI.tag(t, Core.bundle.get(c.unlockedNow() ? "eclipse-wiki.tag.researched" : "eclipse-wiki.tag.locked"), c.unlockedNow() ? WikiUI.good : WikiUI.dim);
    }

    void statsOf(Table t, UnlockableContent c){
        if(c instanceof Block b && !(b instanceof OreBlock)){
            String power = null;
            if(b instanceof RecipeCrafter rc){
                float lo = Float.MAX_VALUE, hi = 0f;
                for(RecipeCrafter.Recipe r : rc.recipes){
                    lo = Math.min(lo, r.power() * 60f);
                    hi = Math.max(hi, r.power() * 60f);
                }
                power = Strings.autoFixed(lo, 0) + (hi > lo ? "–" + Strings.autoFixed(hi, 0) : "") + "/s";
            }else if(b instanceof PowerGenerator g && g.powerProduction > 0f) power = "+" + Strings.autoFixed(g.powerProduction * 60f, 0) + "/s";
            else if(b.consPower != null && b.consPower.usage > 0f) power = Strings.autoFixed(b.consPower.usage * 60f, 0) + "/s";
            if(power != null) stat(t, "eclipse-wiki.stat.power", power);
            stat(t, "eclipse-wiki.stat.health", String.valueOf((int)b.health));
            if(b.hasItems && b.itemCapacity > 0 && !(b instanceof Turret)) stat(t, "eclipse-wiki.stat.holds", Core.bundle.format("eclipse-wiki.stat.items", b.itemCapacity));
            if(b instanceof Turret tu) stat(t, "eclipse-wiki.stat.range", Strings.autoFixed(tu.range / tilesize, 1));
        }else if(c instanceof UnitType u){
            stat(t, "eclipse-wiki.stat.health", String.valueOf((int)u.health));
            stat(t, "eclipse-wiki.stat.armor", Strings.autoFixed(u.armor, 1));
            stat(t, "eclipse-wiki.stat.speed", Strings.autoFixed(u.speed * 60f / tilesize, 1));
        }else if(c instanceof Item || c instanceof Liquid){
            int made = 0, used = 0;
            for(Block b : content.blocks()){
                if(!b.isVisible()) continue;
                if(makes(b, c)) made++;
                if(uses(b, c)) used++;
            }
            stat(t, "eclipse-wiki.stat.made-by", String.valueOf(made));
            stat(t, "eclipse-wiki.stat.used-by", String.valueOf(used));
        }
    }

    static void stat(Table t, String key, String value){
        t.add(Core.bundle.get(key)).color(Color.lightGray).right();
        t.add(value).right().row();
    }

    // ---------------- tabs ----------------

    void buildTabs(){
        tabBar.clear();
        tabBar.left();
        if(tabs.size <= 1 && !(current != null && current.target instanceof String)) return;
        for(int i = 0; i < tabs.size; i++){
            Tab tab = tabs.get(i);
            Cell<TextButton> c = tabBar.button(tab.label, Styles.flatTogglet, () -> open(current.target, tab.id, true))
                .checked(b -> current != null && tab.id.equals(current.tab)).height(42f).padRight(4f);
            c.get().getLabelCell().pad(0f, 14f, 0f, 14f);
            if(!mobile && i < 9) c.tooltip(String.valueOf(i + 1));
        }
    }

    Seq<Tab> tabsOf(Object target){
        Seq<Tab> out = new Seq<>();
        if(target instanceof String id){
            String name = Core.bundle.get("eclipse-wiki.guide." + id + ".name");
            Seq<GuideChapters.Chapter> chapters = GuideChapters.parse(name, guideText(id));
            for(int i = 0; i < chapters.size; i++){
                int index = i;
                GuideChapters.Chapter ch = chapters.get(i);
                out.add(new Tab("c" + i, chapters.size == 1 ? Core.bundle.get("eclipse-wiki.tab.guide") : (i + 1) + ". " + ch.title, () -> chapterPage(id, chapters, index)));
            }
            return out;
        }
        UnlockableContent c = (UnlockableContent)target;
        out.add(new Tab("overview", Core.bundle.get("eclipse-wiki.tab.overview"), () -> overview(c)));
        if(c instanceof RecipeCrafter rc && rc.recipes.size > 0) out.add(new Tab("recipes", Core.bundle.get("eclipse-wiki.tab.recipes") + " [lightgray]" + rc.recipes.size, () -> recipesPage(rc)));
        if(c instanceof Block b && !(b instanceof OreBlock) && examplesOf(b).any()) out.add(new Tab("build", Core.bundle.get("eclipse-wiki.tab.build") + " [lightgray]" + examplesOf(b).size, () -> buildPage(b)));
        if(c instanceof Item item && !(item instanceof AugmentModule) && !RecipeTree.raw(item) && RecipeTree.sourceOf(item) != null) out.add(new Tab("chain", Core.bundle.get("eclipse-wiki.tab.chain"), () -> chainPage(item)));
        if((c instanceof Item || c instanceof Liquid) && hasUses(c)) out.add(new Tab("uses", Core.bundle.get("eclipse-wiki.tab.uses"), () -> usesPage(c)));
        if(c instanceof Block b && hasLogic(b)) out.add(new Tab("logic", Core.bundle.get("eclipse-wiki.tab.logic"), () -> logicFacts(b)));
        return out;
    }

    Seq<BuildLayout> examplesOf(Block b){
        if(!(b instanceof GenericCrafter) && !(b instanceof Separator)) return new Seq<>();
        Seq<BuildLayout> ex = examples.get(b);
        if(ex == null){
            ex = LayoutGen.examples(b);
            examples.put(b, ex);
        }
        return ex;
    }

    /** Width for running text (kept readable). */
    float textWidth(){
        return Math.min(pageWidth(), 980f);
    }

    /** Width for cards, tables and pictures: the whole page. */
    float pageWidth(){
        return Math.max(Math.min(Core.graphics.getWidth() / Scl.scl(1f) - (mobile ? 320f : 440f), 1440f), 240f);
    }

    // ---------------- guides ----------------

    static String guideText(String id){
        String text = Core.bundle.get("eclipse-wiki.guide." + id + ".text");
        //the QOL page names the keys as currently bound, so a rebound key shows correctly
        if(id.equals("qol")) text = Core.bundle.format("eclipse-wiki.guide.qol.text", keyName(entryKey), keyName(eclipse.input.UpgradePlanner.key),
            keyName(ProductionStats.key), keyName(StallOverlay.key));
        return text;
    }

    void chapterPage(String id, Seq<GuideChapters.Chapter> chapters, int index){
        float w = textWidth();
        GuideChapters.Chapter ch = chapters.get(index);
        if(chapters.size > 1) page.add(ch.title).color(Pal.accent).fontScale(1.15f).padBottom(6f).row();
        WikiUI.text(page, ch.body, w);

        //the checklist: rows tick themselves from the player's base
        String[][] checks = GuideChapters.checks.get(id);
        if(checks != null && index < checks.length && checks[index].length > 0){
            int done = 0;
            Seq<Block> blocks = new Seq<>();
            for(String n : checks[index]){
                Block b = GuideChapters.checkBlock(n);
                if(b == null) continue;
                blocks.add(b);
                if(GuideChapters.built(b) > 0) done++;
            }
            WikiUI.header(page, state.isGame() ? Core.bundle.format("eclipse-wiki.checklist.progress", done, blocks.size) : Core.bundle.get("eclipse-wiki.checklist"));
            for(Block b : blocks) checkRow(b, w);
        }
        if(id.equals("logic") && index == chapters.size - 1) logicGuide();
        if(id.equals("compare") && index == chapters.size - 1) WikiTables.build(this, page);
        if(index < chapters.size - 1){
            page.button(Core.bundle.format("eclipse-wiki.next-chapter", chapters.get(index + 1).title), Icon.right, Styles.flatt, () -> open(id, "c" + (index + 1), true))
                .height(44f).padTop(10f).left().row();
        }else{
            for(String[] g : guides){
                if(!g[0].equals(id) || g.length < 2) continue;
                WikiUI.header(page, Core.bundle.get("eclipse-wiki.related"));
                Table links = new Table().left();
                for(int i = 1; i < g.length; i++){
                    UnlockableContent c = byName(g[i]);
                    if(c != null) link(links, c, null);
                    if(i % 3 == 0) links.row();
                }
                page.add(links).left().row();
            }
        }
    }

    void checkRow(Block b, float width){
        int built = GuideChapters.built(b);
        boolean done = built > 0;
        String key = "eclipse-wiki.check." + b.name.replace("eclipse-", "");
        String[] text = Core.bundle.has(key) ? Core.bundle.get(key).split("\\|", 2) : new String[]{Core.bundle.format("eclipse-wiki.check.build", b.localizedName), b.description == null ? "" : b.description};
        page.table(WikiUI.card(), r -> {
            r.left();
            r.table(WikiUI.box(Color.clear, done ? WikiUI.good : Color.valueOf("45454f"), 3f, 0f), box -> { if(done) box.image(Icon.ok).size(16f).color(WikiUI.good); }).size(26f).padRight(10f);
            r.image(b.uiIcon).size(40f).padRight(10f);
            r.table(tt -> {
                tt.left().defaults().left();
                tt.add(text[0]).row();
                if(text.length > 1) tt.add(text[1]).color(Color.lightGray).fontScale(0.85f).wrap().width(width - 300f);
            }).growX();
            if(built >= 0 && done) r.add(Core.bundle.format("eclipse-wiki.checklist.built", built)).color(WikiUI.good).right().padLeft(10f);
            else r.button("@eclipse-wiki.checklist.show", Styles.flatt, () -> open(b, null, true)).height(36f).right().padLeft(10f);
        }).width(width).left().padBottom(4f).row();
    }

    // ---------------- overview ----------------

    void overview(UnlockableContent c){
        float w = textWidth();
        //opened with I over a stopped machine: why, and how to fix it
        if(stopped != null && c == stoppedBlock){
            MachineStatus.Kind k = stopped.kind;
            Table box = WikiUI.callout(2, Core.bundle.format("eclipse-wiki.stopped", stopped.text().replace("[", "[[")), Core.bundle.get("eclipse-wiki.fix." + k.name()), w);
            page.add(box).width(w).left().padBottom(6f).row();
            String guide = fixGuide(k);
            if(guide != null) page.button(Core.bundle.format("eclipse-wiki.fix.guide", Core.bundle.get("eclipse-wiki.guide." + guide + ".name")), Icon.book, Styles.flatt, () -> open(guide, null, true)).height(40f).left().padBottom(8f).row();
        }

        String key = textKey(c);
        String text = Core.bundle.has(key) ? Core.bundle.get(key) : c.description;
        if(text != null) WikiUI.text(page, text, w);
        //the short in-game description of a module tier, under its family's explanation
        if(c instanceof AugmentModule && c.description != null && !key.equals("wiki." + c.name)) page.add("[lightgray]" + c.description).wrap().width(w).padBottom(8f).row();

        if(c instanceof Block b) blockOverview(b, w);
        if(c instanceof Item || c instanceof Liquid) materialOverview(c, w);
        if(c instanceof UnitType u) unitOverview(u, w);

        if(c.techNode != null && c.techNode.parent != null){
            WikiUI.header(page, Core.bundle.get("eclipse-wiki.research"));
            Table t = new Table().left();
            link(t, c.techNode.parent.content, null);
            page.add(t).left().row();
        }
        page.button("@eclipse-wiki.stats", Icon.info, Styles.flatt, () -> ui.content.show(c)).size(240f, 44f).padTop(14f).row();
    }

    static @Nullable String fixGuide(MachineStatus.Kind k){
        return switch(k){
            case noHeat, lowHeat, heatShort -> "heat";
            case noPower, underpowered -> "power";
            case noCatalyst, pressurizing -> "chemistry";
            case outputFull, missing -> "logistics";
            case noRecipe, autoWaiting -> "industry";
            default -> null;
        };
    }

    void blockOverview(Block b, float w){
        if(b instanceof OreBlock ore && ore.itemDrop != null){
            WikiUI.header(page, Core.bundle.get("eclipse-wiki.yields"));
            row(page, ore.itemDrop, null);
            return;
        }
        if(b.requirements != null && b.requirements.length > 0){
            WikiUI.header(page, Core.bundle.get("eclipse-wiki.cost"));
            stacks(page, b.requirements);
        }
        if(b instanceof RecipeCrafter rc && rc.recipes.any()){
            float cw = Math.min(pageWidth(), 1200f);
            WikiUI.header(page, Core.bundle.get("eclipse-wiki.main-recipe"));
            page.add(recipeCard(rc, rc.recipes.first(), true, cw)).width(cw).left().row();
            if(rc.recipes.size > 1) page.button(Core.bundle.format("eclipse-wiki.more-recipes", rc.recipes.size - 1), Icon.right, Styles.flatt, () -> open(b, "recipes", true)).height(40f).left().padTop(4f).row();
            return;
        }
        LayoutGen.Job job = (b instanceof GenericCrafter || b instanceof Separator) ? LayoutGen.job(b, null) : null;
        if(job != null && (job.in.any() || job.liqIn.any()) && (job.out.any() || job.liqOut.any())){
            WikiUI.header(page, Core.bundle.get("eclipse-wiki.recipe"));
            page.add(WikiUI.recipeCard(job.in, job.liqIn, job.out, job.liqOut, job.seconds, t -> {
                if(job.powerUse > 0f) WikiUI.tag(t, Strings.autoFixed(job.powerUse * 60f, 1) + "/s", WikiUI.cyan);
                if(job.heat > 0f) WikiUI.tag(t, Core.bundle.format("eclipse-wiki.tag.heat", Strings.autoFixed(job.heat, 1)), WikiUI.heat);
                if(job.catalyst != null) WikiUI.tag(t, Core.bundle.format("eclipse-wiki.tag.catalyst", job.catalyst.localizedName, job.catalystCrafts), Color.lightGray);
            }, examplesOf(b).any() ? bt -> bt.button("@eclipse-wiki.see-built", WikiUI.chipStyle(), () -> open(b, "build", true)).height(30f) : null, this::open, w)).width(w).left().row();
        }else{
            facts(b);
        }
        //ammunition
        Table ammo = new Table().left();
        if(b instanceof ItemTurret it) for(Item i : it.ammoTypes.keys()) link(ammo, i, null);
        if(b instanceof LiquidTurret lt) for(Liquid l : lt.ammoTypes.keys()) link(ammo, l, null);
        if(ammo.hasChildren()){
            WikiUI.header(page, Core.bundle.get("eclipse-wiki.ammo"));
            page.add(ammo).left().row();
        }
    }

    /** Needs and makes for blocks that aren't simple crafters (generators, drills, factories, heaters with fuel). */
    void facts(Block b){
        Table needs = new Table().left();
        float seconds = b instanceof GenericCrafter g ? g.craftTime / 60f : 0f;
        for(Consume cons : b.consumers){
            if(cons instanceof ConsumeItems ci) for(ItemStack s : ci.items) link(needs, s.item, perCraft(s.amount, seconds));
            else if(cons instanceof ConsumeLiquid cl) link(needs, cl.liquid, perSecond(cl.amount * 60f));
            else if(cons instanceof ConsumeLiquids cls) for(LiquidStack s : cls.liquids) link(needs, s.liquid, perSecond(s.amount * 60f));
            else if(cons instanceof ConsumeItemFilter f) for(Item i : content.items().select(f.filter::get)) link(needs, i, null);
            else if(cons instanceof ConsumeLiquidFilter f) for(Liquid l : content.liquids().select(f.filter::get)) link(needs, l, null);
            else continue;
            needs.row();
        }
        if(b.consPower != null && b.consPower.usage > 0f) needs.add("[lightgray]" + Core.bundle.format("eclipse-wiki.power-use", Strings.autoFixed(b.consPower.usage * 60f, 1))).left().row();
        if(b instanceof HeatCrafter hc && hc.heatRequirement > 0f) needs.add("[lightgray]" + Core.bundle.format("eclipse-wiki.heat-use", Strings.autoFixed(hc.heatRequirement, 1))).left().row();
        if(needs.hasChildren()){
            WikiUI.header(page, Core.bundle.get("eclipse-wiki.needs"));
            page.add(needs).left().row();
        }
        Table makes = new Table().left();
        if(b instanceof GenericCrafter g){
            if(g.outputItems != null) for(ItemStack s : g.outputItems){ link(makes, s.item, perCraft(s.amount, seconds)); makes.row(); }
            if(g.outputLiquids != null) for(LiquidStack s : g.outputLiquids){ link(makes, s.liquid, perSecond(s.amount * 60f)); makes.row(); }
        }
        if(b instanceof HeatProducer hp) makes.add("[lightgray]" + Core.bundle.format("eclipse-wiki.heat-out", Strings.autoFixed(hp.heatOutput, 1))).left().row();
        if(b instanceof PowerGenerator pg && pg.powerProduction > 0f) makes.add("[lightgray]" + Core.bundle.format("eclipse-wiki.power-out", Strings.autoFixed(pg.powerProduction * 60f, 1))).left().row();
        if(b instanceof Drill d) makes.add("[lightgray]" + Core.bundle.format("eclipse-wiki.drill", d.tier)).left().row();
        if(b instanceof UnitFactory f) for(UnitFactory.UnitPlan p : f.plans){ link(makes, p.unit, null); makes.row(); }
        if(b instanceof ModuleAssembler a) for(AugmentModule m : a.modules()){ link(makes, m, null); makes.row(); }
        if(makes.hasChildren()){
            WikiUI.header(page, Core.bundle.get("eclipse-wiki.makes"));
            page.add(makes).left().row();
        }
    }

    // ---------------- recipes ----------------

    Table recipeCard(RecipeCrafter rc, RecipeCrafter.Recipe r, boolean buttons, float w){
        boolean built = examplesOf(rc).contains(l -> l.recipe == r);
        Item chainItem = r.outputs.length > 0 && !RecipeTree.raw(r.outputs[0].item) ? r.outputs[0].item : null;
        return WikiUI.recipeCard(Seq.with(r.inputs), Seq.with(r.liquidInputs), Seq.with(r.outputs), Seq.with(r.liquidOutputs), r.time / 60f, t -> {
            WikiUI.tag(t, Core.bundle.format("eclipse-wiki.tag.tier-power", RecipeCrafter.roman(r.tier), Strings.autoFixed(r.power() * 60f, 0)), WikiUI.tierColor(r.tier));
            if(r.heat > 0f) WikiUI.tag(t, Core.bundle.format("eclipse-wiki.tag.heat", Strings.autoFixed(r.heat, 1)), WikiUI.heat);
            if(r.pressure > 0f) WikiUI.tag(t, Core.bundle.format("eclipse-wiki.tag.pressure", (int)(r.pressure * 100)), WikiUI.cyan);
            if(r.catalyst != null) WikiUI.tag(t, Core.bundle.format("eclipse-wiki.tag.catalyst", r.catalyst.localizedName, r.craftsPerCatalyst), Color.lightGray);
            if(r.outputs.length > 0 && r.outputs[0].item.name.startsWith("eclipse-hot-")) WikiUI.tag(t, Core.bundle.get("eclipse-wiki.tag.comes-hot"), WikiUI.heat);
        }, !buttons ? null : bt -> {
            if(built) bt.button("@eclipse-wiki.see-built", WikiUI.chipStyle(), () -> {
                buildIndex = Math.max(0, examplesOf(rc).indexOf(l -> l.recipe == r));
                buildStep = 0;
                open(rc, "build", true);
            }).height(30f).padRight(4f);
            if(chainItem != null) bt.button("@eclipse-wiki.chain-button", WikiUI.chipStyle(), () -> open(chainItem, "chain", true)).height(30f);
        }, this::open, w);
    }

    void recipesPage(RecipeCrafter rc){
        float w = Math.min(pageWidth(), 1200f);
        for(RecipeCrafter.Recipe r : rc.recipes) page.add(recipeCard(rc, r, true, w)).width(w).left().padBottom(6f).row();
        page.add("[lightgray]" + Core.bundle.get("eclipse-wiki.recipes.hint")).wrap().width(w).left().padTop(4f).row();
    }

    // ---------------- build examples ----------------

    void buildPage(Block b){
        float w = pageWidth();
        Seq<BuildLayout> ex = examplesOf(b);
        if(ex.isEmpty()){
            page.add("[lightgray]" + Core.bundle.get("eclipse-build.none")).wrap().width(w).row();
            return;
        }
        buildIndex = Math.min(buildIndex, ex.size - 1);
        BuildLayout lay = ex.get(buildIndex);

        //which example: one chip per recipe, then minimal or full line for that recipe
        if(b instanceof RecipeCrafter rc && rc.recipes.size > 1){
            page.table(chips -> {
                chips.left();
                int n = 0;
                for(RecipeCrafter.Recipe r : rc.recipes){
                    int first = ex.indexOf(l -> l.recipe == r);
                    if(first < 0) continue;
                    ImageButton bt = chips.button(new arc.scene.style.TextureRegionDrawable(r.output().uiIcon), Styles.clearNoneTogglei, 32f, () -> {
                        buildIndex = first;
                        buildStep = 0;
                        open(b, "build", false);
                    }).size(44f).checked(x -> ex.get(buildIndex).recipe == r).get();
                    WikiUI.hover(bt, r.output());
                    if(++n % 14 == 0) chips.row();
                }
            }).left().padBottom(4f).row();
        }
        page.table(kinds -> {
            kinds.left();
            for(int i = 0; i < ex.size; i++){
                BuildLayout l = ex.get(i);
                if(l.recipe != lay.recipe) continue;
                int index = i;
                kinds.button(l.title, WikiUI.chipStyle(), () -> {
                    buildIndex = index;
                    buildStep = 0;
                    open(b, "build", false);
                }).checked(x -> buildIndex == index).height(34f).padRight(6f);
            }
            kinds.add().growX();
            kinds.button("@eclipse-build.rates", WikiUI.chipStyle(), () -> {
                buildRates = !buildRates;
                if(diagram != null) diagram.rates = buildRates;
            }).checked(x -> buildRates).height(34f).padRight(6f);
            kinds.button("@eclipse-build.steps", WikiUI.chipStyle(), () -> {
                buildStep = buildStep == 0 ? 1 : 0;
                if(diagram != null) diagram.step = buildStep;
                fillCaption(lay);
            }).checked(x -> buildStep > 0).height(34f).padRight(6f);
            Cell<TextButton> bt = kinds.button("@eclipse-build.build", Icon.copy, Styles.flatt, this::buildThis).disabled(x -> !state.isGame()).height(34f);
            if(!mobile) bt.tooltip("@eclipse-build.build-key");
        }).width(w).left().padBottom(6f).row();

        boolean wide = w > 760f;
        float picW = wide ? w - 270f : w;
        diagram = new BuildDiagram(lay, picW);
        diagram.rates = buildRates;
        diagram.step = buildStep;
        Table side = new Table();
        side.top().left().defaults().left().growX();
        side.table(WikiUI.card(), box -> {
            box.left().defaults().left();
            box.add(Core.bundle.get("eclipse-build.need")).color(Pal.accent).padBottom(4f).row();
            for(var e : lay.bill()){
                box.table(r -> {
                    r.left();
                    r.image(e.key.uiIcon).size(22f).padRight(6f);
                    r.add(e.key.localizedName).fontScale(0.85f).left().growX();
                    r.add("×" + e.value).color(WikiUI.cyan).fontScale(0.85f).right().padLeft(8f);
                }).growX().row();
            }
        }).growX().padBottom(6f).row();
        side.table(WikiUI.card(), box -> {
            box.left().defaults().left();
            box.add(Core.bundle.get("eclipse-build.this-line")).color(Pal.accent).padBottom(4f).row();
            for(String[] tot : lay.totals){
                box.table(r -> {
                    r.left();
                    r.add(tot[0]).color(Color.lightGray).fontScale(0.85f).left().growX();
                    r.add(tot[1]).color(WikiUI.cyan).fontScale(0.85f).right().padLeft(8f);
                }).growX().row();
            }
        }).growX();
        caption = new Table(WikiUI.card());
        fillCaption(lay);
        if(wide){
            page.table(row -> {
                row.top().left();
                row.add(diagram).top().left();
                row.add(side).width(254f).top().padLeft(14f);
            }).left().row();
            page.add(caption).width(diagram.getPrefWidth() + 268f).left().padTop(6f).row();
        }else{
            page.add(diagram).left().row();
            page.add(caption).growX().padTop(6f).row();
            page.add(side).growX().padTop(6f).row();
        }
    }

    void fillCaption(BuildLayout lay){
        if(caption == null) return;
        caption.clear();
        caption.left();
        if(buildStep == 0){
            caption.add(Core.bundle.get("eclipse-build.hover")).color(Color.lightGray).wrap().growX();
            return;
        }
        caption.add(Core.bundle.format("eclipse-build.step", buildStep, lay.steps.size)).color(Pal.accent).padRight(12f);
        caption.add(lay.steps.get(Math.min(buildStep, lay.steps.size) - 1)).wrap().growX();
        caption.button(Icon.left, Styles.clearNonei, () -> {
            buildStep = Math.max(1, buildStep - 1);
            if(diagram != null) diagram.step = buildStep;
            fillCaption(lay);
        }).size(40f).disabled(b -> buildStep <= 1);
        caption.button(buildStep < lay.steps.size ? Core.bundle.get("eclipse-build.next") : Core.bundle.get("eclipse-build.done"), Styles.flatt, () -> {
            buildStep = buildStep >= lay.steps.size ? 0 : buildStep + 1;
            if(diagram != null) diagram.step = buildStep;
            fillCaption(lay);
        }).height(40f).minWidth(90f);
    }

    /** Puts the example shown on the cursor as a schematic and closes the wiki. */
    void buildThis(){
        if(!state.isGame() || current == null || !(current.target instanceof Block b)) return;
        Seq<BuildLayout> ex = examplesOf(b);
        if(ex.isEmpty()) return;
        BuildLayout lay = ex.get(Math.min(buildIndex, ex.size - 1));
        control.input.useSchematic(lay.schematic());
        hide();
        StringBuilder parts = new StringBuilder();
        for(var e : lay.bill()){
            if(parts.length() > 0) parts.append(", ");
            parts.append(e.value).append(' ').append(e.key.localizedName);
        }
        ui.showInfoToast(Core.bundle.format("eclipse-build.built", parts), 5f);
    }

    // ---------------- materials ----------------

    void materialOverview(UnlockableContent mat, float w){
        if(mat instanceof AugmentModule m && m.recipe.length > 0){
            WikiUI.header(page, Core.bundle.get("eclipse-wiki.recipe"));
            stacks(page, m.recipe);
        }
        //where to get it: ores that drop it, then every recipe that makes it, as cards
        Table ores = new Table().left();
        for(Block b : content.blocks()) if(b instanceof OreBlock o && o.itemDrop == mat) link(ores, o, null);
        boolean header = false;
        if(ores.hasChildren()){
            WikiUI.header(page, Core.bundle.get("eclipse-wiki.where"));
            header = true;
            page.add(ores).left().row();
        }
        Table vanilla = new Table().left();
        for(Block b : content.blocks()){
            if(!b.isVisible() || !makes(b, mat)) continue;
            if(!header){
                WikiUI.header(page, Core.bundle.get("eclipse-wiki.where"));
                header = true;
            }
            if(b instanceof RecipeCrafter rc){
                for(RecipeCrafter.Recipe r : rc.recipes){
                    boolean out = false;
                    for(ItemStack s : r.outputs) if(s.item == mat) out = true;
                    for(LiquidStack s : r.liquidOutputs) if(s.liquid == mat) out = true;
                    if(!out) continue;
                    Table t = new Table().left();
                    link(t, rc, null);
                    page.add(t).left().row();
                    page.add(recipeCard(rc, r, true, w)).width(w).left().padBottom(4f).row();
                }
            }else vanilla.add(new Table(){{ link(this, b, null); }}).left();
        }
        if(vanilla.hasChildren()) page.add(vanilla).left().row();
        if(mat instanceof Item i && i.name.startsWith("eclipse-hot-")){
            page.add(WikiUI.callout(3, Core.bundle.get("eclipse-wiki.hot.title"), Core.bundle.get("eclipse-wiki.hot.text"), w)).width(w).left().padTop(6f).row();
        }
    }

    boolean hasUses(UnlockableContent mat){
        for(Block b : content.blocks()){
            if(b.isVisible() && uses(b, mat)) return true;
            if(mat instanceof Item i && b.requirements != null && b.isVisible() && contains(b.requirements, i)) return true;
        }
        return false;
    }

    void usesPage(UnlockableContent mat){
        //used by: machine inputs, ammunition, module recipes, unit costs
        Table usedBy = new Table().left();
        int n = 0;
        ObjectSet<UnlockableContent> seen = new ObjectSet<>();
        for(Block b : content.blocks()){
            if(uses(b, mat) && seen.add(b)){
                link(usedBy, b, null);
                if(++n % 3 == 0) usedBy.row();
            }
            if(b instanceof UnitFactory f) for(UnitFactory.UnitPlan p : f.plans){
                if(mat instanceof Item i && contains(p.requirements, i) && seen.add(p.unit)){
                    link(usedBy, p.unit, null);
                    if(++n % 3 == 0) usedBy.row();
                }
            }
        }
        for(Item item : content.items()){
            if(item instanceof AugmentModule m && mat instanceof Item i && contains(m.recipe, i) && seen.add(m)){
                link(usedBy, m, null);
                if(++n % 3 == 0) usedBy.row();
            }
        }
        if(usedBy.hasChildren()){
            WikiUI.header(page, Core.bundle.get("eclipse-wiki.used-by"));
            page.add(usedBy).left().row();
        }
        //build costs
        if(mat instanceof Item i){
            Table builds = new Table().left();
            n = 0;
            for(Block b : content.blocks()){
                if(b.requirements != null && b.isVisible() && contains(b.requirements, i)){
                    ImageButton bt = builds.button(new arc.scene.style.TextureRegionDrawable(b.uiIcon), Styles.clearNonei, 32f, () -> open(b)).size(44f).get();
                    WikiUI.hover(bt, b);
                    if(++n % 14 == 0) builds.row();
                }
            }
            if(builds.hasChildren()){
                WikiUI.header(page, Core.bundle.format("eclipse-wiki.build-cost-of-n", n));
                page.add(builds).left().row();
            }
        }
    }

    /** The production calculator: the chain for a target rate (typed in), drawn as a flow, then raw totals, machines and power. */
    void chainPage(Item item){
        page.table(r -> {
            r.left();
            r.add(Core.bundle.get("eclipse-wiki.tree.rate") + " ").padRight(4f);
            r.field(Strings.autoFixed(treeRate, 2), TextField.TextFieldFilter.floatsOnly, text -> {
                float v = Strings.parseFloat(text, -1f);
                if(v <= 0f || v > 10000f) return;
                treeRate = v;
                Core.app.post(() -> open(item, "chain", false));
            }).width(110f);
            r.add(" /s");
        }).left().padBottom(6f).row();
        RecipeTree.Step root = RecipeTree.plan(item, treeRate);
        ChainGraph graph = new ChainGraph(root, this::open);
        ScrollPane pane = new ScrollPane(graph, Styles.smallPane);
        pane.setScrollingDisabledY(true);
        page.add(pane).width(textWidth()).left().row();

        Table host = new Table().left();
        host.left().defaults().left();
        WikiUI.header(host, Core.bundle.get("eclipse-wiki.tree.raw"));
        Table raw = new Table().left();
        ObjectFloatMap<UnlockableContent> totals = RecipeTree.totals(root);
        Seq<UnlockableContent> keys = totals.keys().toSeq().sort(Structs.comparingFloat(c -> -totals.get(c, 0f)));
        int n = 0;
        for(UnlockableContent c : keys){
            link(raw, c, perSecond(totals.get(c, 0f)));
            if(++n % 3 == 0) raw.row();
        }
        host.add(raw).left().row();
        WikiUI.header(host, Core.bundle.get("eclipse-wiki.tree.machines"));
        Table machines = new Table().left();
        ObjectFloatMap<Block> counts = RecipeTree.machines(root);
        n = 0;
        for(Block b : counts.keys().toSeq().sort(Structs.comparingFloat(x -> -counts.get(x, 0f)))){
            link(machines, b, "×" + Strings.autoFixed(counts.get(b, 0f), 2));
            if(++n % 3 == 0) machines.row();
        }
        host.add(machines).left().row();
        int tier = RecipeTree.maxTier(root);
        host.table(t -> {
            t.left();
            WikiUI.tag(t, Core.bundle.format("eclipse-wiki.chain.power", Strings.autoFixed(RecipeTree.power(root) * 60f, 0)), Pal.accent);
            if(tier > 0) WikiUI.tag(t, Core.bundle.format("eclipse-wiki.tag.tier-power", RecipeCrafter.roman(tier), Strings.autoFixed(RecipeCrafter.tierPower[tier] * 60f, 0)), WikiUI.tierColor(tier));
        }).left().padTop(8f).row();
        host.add("[lightgray]" + Core.bundle.get("eclipse-wiki.tree.hint")).wrap().width(textWidth()).left().padTop(6f).padBottom(8f).row();
        page.add(host).left().row();
    }

    // ---------------- units ----------------

    void unitOverview(UnitType u, float w){
        page.add("[lightgray]" + Core.bundle.format("eclipse-wiki.unit-stats", (int)u.health, Strings.autoFixed(u.armor, 1),
            Strings.autoFixed(u.speed * 60f / tilesize, 1), Core.bundle.get(u.flying ? "eclipse-wiki.air" : u.naval ? "eclipse-wiki.naval" : "eclipse-wiki.ground"))).left().padTop(4f).row();

        Seq<String> when = GuideChapters.schedule(u);
        if(when.any()){
            WikiUI.header(page, Core.bundle.get("eclipse-wiki.when"));
            for(String line : when) page.add("• " + line).wrap().width(w).left().row();
        }
        String counter = "wiki." + u.name + ".counter";
        if(Core.bundle.has(counter)) page.add(WikiUI.callout(1, Core.bundle.get("eclipse-wiki.counter"), Core.bundle.get(counter), w)).width(w).left().padTop(8f).row();

        Seq<Ability> shown = Seq.with(u.abilities).select(a -> a.display);
        if(shown.any()){
            WikiUI.header(page, Core.bundle.get("eclipse-wiki.abilities"));
            for(Ability a : shown){
                page.table(WikiUI.card(), t -> {
                    t.left().defaults().left();
                    t.add(a.localized()).color(Pal.accent).row();
                    Table stats = new Table().left();
                    a.addStats(stats);
                    for(var cell : stats.getCells()) if(cell.get() instanceof Label l){
                        l.setWrap(true);
                        cell.width(w - 40f).left();
                    }
                    t.add(stats).left();
                }).width(w).left().padBottom(4f).row();
            }
        }

        Table built = new Table().left();
        for(Block b : content.blocks()){
            if(b instanceof UnitFactory f) for(UnitFactory.UnitPlan p : f.plans){
                if(p.unit != u) continue;
                link(built, b, Core.bundle.format("eclipse-wiki.seconds", Strings.autoFixed(p.time / 60f, 1)));
                built.row();
                Table cost = new Table().left();
                stacks(cost, p.requirements);
                built.add(cost).left().padLeft(24f).row();
            }
            if(b instanceof Reconstructor r) for(UnitType[] up : r.upgrades){
                if(up[1] != u) continue;
                built.table(row -> {
                    row.left();
                    link(row, up[0], null);
                    row.add(" → ").padRight(4f);
                    link(row, b, null);
                }).left().row();
            }
        }
        WikiUI.header(page, Core.bundle.get("eclipse-wiki.built-in"));
        if(built.hasChildren()) page.add(built).left().row();
        else page.add("[lightgray]" + Core.bundle.get("eclipse-wiki.enemy-only")).left().row();
    }

    // ---------------- logic ----------------

    static boolean hasLogic(Block b){
        return !eclipse.logic.EclipseLogic.fields(b).isEmpty() || !eclipse.logic.EclipseLogic.settings(b).isEmpty() || b instanceof eclipse.logistics.FlowMeter;
    }

    /** What a processor can read from and set on this block (generated from EclipseLogic, so it can't go out of date). */
    void logicFacts(Block b){
        Seq<eclipse.logic.EclipseLogic.Field> fields = eclipse.logic.EclipseLogic.fields(b);
        Seq<eclipse.logic.EclipseLogic.Setting> settings = eclipse.logic.EclipseLogic.settings(b);
        boolean config = b instanceof RecipeCrafter, shoot = b instanceof eclipse.defense.OrbitalPlatform, flow = b instanceof eclipse.logistics.FlowMeter;
        WikiUI.header(page, Core.bundle.get("eclipse-wiki.logic"));
        Table t = new Table().left();
        t.defaults().left().padBottom(2f);
        for(var f : fields) logicRow(t, "esensor result block " + f.name(), "eclipse-logic.field." + f.name());
        for(var s : settings) logicRow(t, "econtrol " + s.name() + " block " + (s == eclipse.logic.EclipseLogic.Setting.target ? "x y" : "value"), "eclipse-logic.setting." + s.name());
        if(config) logicRow(t, "sensor result block @config", "eclipse-logic.config");
        if(shoot) logicRow(t, "control shoot block x y 1", "eclipse-logic.shoot");
        if(flow) logicRow(t, "sensor result block @copper", "eclipse-logic.flow");
        page.add(t).left().row();
        page.add("[lightgray]" + Core.bundle.get("eclipse-wiki.logic.hint")).wrap().width(textWidth()).left().padBottom(8f).row();
    }

    void logicRow(Table t, String code, String key){
        t.add("[#8fd8ff]" + code).left().padRight(12f);
        t.add("[lightgray]" + Core.bundle.get(key)).wrap().width(Math.max(textWidth() - 360f, 200f)).left().row();
    }

    /** The logic guide's generated part: what each status code means, and every block a processor can read. */
    void logicGuide(){
        WikiUI.header(page, Core.bundle.get("eclipse-wiki.logic.codes"));
        Table codes = new Table().left();
        for(MachineStatus.Kind k : MachineStatus.Kind.values()){
            codes.add("[#8fd8ff]" + k.ordinal()).left().padRight(12f);
            codes.add("[lightgray]" + Core.bundle.get("eclipse-logic.status." + k.name())).left().row();
        }
        page.add(codes).left().row();
        WikiUI.header(page, Core.bundle.get("eclipse-wiki.logic.blocks"));
        Table blocks = new Table().left();
        int n = 0;
        for(Block b : content.blocks()){
            if(!isEclipse(b) || !b.isVisible() || eclipse.logic.EclipseLogic.fields(b).isEmpty()) continue;
            link(blocks, b, null);
            if(++n % 3 == 0) blocks.row();
        }
        page.add(blocks).left().padBottom(8f).row();
    }

    // ---------------- facts read from the game ----------------

    public static boolean makes(Block b, UnlockableContent mat){
        if(b instanceof GenericCrafter g){
            if(g.outputItems != null) for(ItemStack s : g.outputItems) if(s.item == mat) return true;
            if(g.outputLiquids != null) for(LiquidStack s : g.outputLiquids) if(s.liquid == mat) return true;
        }
        if(b instanceof Separator sep) for(ItemStack s : sep.results) if(s.item == mat) return true;
        if(b instanceof RecipeCrafter rc && mat instanceof Liquid l && rc.makesLiquid(l)) return true;
        if(b instanceof OreBlock ore) return ore.itemDrop == mat;
        if(b instanceof ModuleAssembler a && mat instanceof AugmentModule m) return a.canMake(m);
        return false;
    }

    public static boolean uses(Block b, UnlockableContent mat){
        for(Consume cons : b.consumers){
            if(cons instanceof ConsumeItems ci && mat instanceof Item i && contains(ci.items, i)) return true;
            if(cons instanceof ConsumeLiquid cl && cl.liquid == mat) return true;
            if(cons instanceof ConsumeLiquids cls) for(LiquidStack s : cls.liquids) if(s.liquid == mat) return true;
            if(cons instanceof ConsumeItemFilter f && mat instanceof Item i && f.filter.get(i)) return true;
            if(cons instanceof ConsumeLiquidFilter f && mat instanceof Liquid l && f.filter.get(l)) return true;
        }
        if(b instanceof eclipse.world.blocks.ChemicalReactor r && r.catalyst == mat) return true;
        if(b instanceof RecipeCrafter rc && mat instanceof Item i && (rc.usesItem(i) || rc.catalyzes(i))) return true;
        if(b instanceof RecipeCrafter rc && mat instanceof Liquid l && rc.usesLiquid(l)) return true;
        if(b instanceof ItemTurret it && mat instanceof Item i && it.ammoTypes.containsKey(i)) return true;
        if(b instanceof LiquidTurret lt && mat instanceof Liquid l && lt.ammoTypes.containsKey(l)) return true;
        return false;
    }

    static boolean contains(ItemStack[] stacks, Item item){
        for(ItemStack s : stacks) if(s.item == item) return true;
        return false;
    }

    // ---------------- small UI helpers ----------------

    static void sub(Table t, String key){
        WikiUI.header(t, Core.bundle.get(key));
    }

    void stacks(Table t, ItemStack[] stacks){
        Table row = new Table().left();
        for(int i = 0; i < stacks.length; i++){
            link(row, stacks[i].item, String.valueOf(stacks[i].amount));
            if(i % 3 == 2) row.row();
        }
        t.add(row).left().row();
    }

    void row(Table t, UnlockableContent c, @Nullable String amount){
        Table r = new Table().left();
        link(r, c, amount);
        t.add(r).left().row();
    }

    /** A clickable icon + name (+ amount) with a hover card: ECLIPSE content opens its wiki entry, vanilla content the game's database. */
    public void link(Table t, UnlockableContent c, @Nullable String amount){
        WikiUI.link(t, c, amount, this::open);
    }

    void open(UnlockableContent c){
        if(isEclipse(c)) open(c, null, true);
        else ui.content.show(c);
    }

    static String perCraft(int amount, float seconds){
        return seconds > 0f ? Core.bundle.format("eclipse-wiki.per-craft", amount, Strings.autoFixed(seconds, 1)) : "x" + amount;
    }

    static String perSecond(float amount){
        return Core.bundle.format("eclipse-wiki.per-second", Strings.autoFixed(amount, 1));
    }

    /** Bundle key of an entry's wiki text: its own, or for module tiers the family's (they differ only in strength). */
    public static String textKey(UnlockableContent c){
        String own = "wiki." + c.name;
        if(c instanceof AugmentModule && !Core.bundle.has(own)) return familyKey(c);
        return own;
    }

    /** "wiki.eclipse-production-accelerator" for "eclipse-production-accelerator-2". */
    public static String familyKey(UnlockableContent c){
        return "wiki." + c.name.replaceAll("-\\d+$", "");
    }

    /** Every ECLIPSE block, item, liquid and unit that gets a wiki entry. */
    public static Seq<UnlockableContent> entries(){
        Seq<UnlockableContent> out = new Seq<>();
        for(ContentType type : new ContentType[]{ContentType.item, ContentType.liquid, ContentType.block, ContentType.unit}){
            for(Content c : content.getBy(type)){
                if(c instanceof UnlockableContent u && isEclipse(u) && (!(u instanceof Block b) || b instanceof OreBlock || b.isVisible())) out.add(u);
            }
        }
        return out;
    }

    public static boolean isEclipse(UnlockableContent c){
        return c.name.startsWith("eclipse-");
    }

    /** The menu icon of a guide: its topic's content icon, a play symbol for Getting Started, a book if unknown. */
    public static arc.scene.style.Drawable guideIcon(String id){
        if(id.equals("start")) return Icon.play;
        for(String[] g : guideIcons){
            if(!g[0].equals(id)) continue;
            UnlockableContent c = byName(g[1]);
            if(c != null) return new arc.scene.style.TextureRegionDrawable(c.uiIcon);
        }
        return Icon.book;
    }

    /** A key binding's current key, or "-" when unbound or on mobile. */
    static String keyName(@Nullable KeyBind bind){
        return bind == null || bind.value.key == null ? "-" : bind.value.key.toString();
    }

    static @Nullable UnlockableContent byName(String name){
        for(ContentType type : new ContentType[]{ContentType.block, ContentType.item, ContentType.liquid, ContentType.unit}){
            Content c = content.getByName(type, name);
            if(c instanceof UnlockableContent u) return u;
        }
        return null;
    }
}
