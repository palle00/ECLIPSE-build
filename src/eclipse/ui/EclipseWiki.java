package eclipse.ui;

import arc.*;
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
 * The ECLIPSE wiki, opened from the pause menu. Guide pages explain the systems; every ECLIPSE block, item, liquid,
 * module and unit has an entry with a hand-written explanation (bundle key "wiki.&lt;content name&gt;") plus facts read
 * from the game itself (cost, inputs, outputs, what makes it, what uses it), so the numbers can't go out of date.
 */
public class EclipseWiki extends BaseDialog{
    /** Guide pages, in menu order: id (bundle key eclipse-wiki.guide.&lt;id&gt;.name / .text) and related content names. */
    public static final String[][] guides = {
        {"start", "eclipse-gear-press", "eclipse-wire-drawing-machine", "eclipse-rivet", "eclipse-flow-meter", "eclipse-metal-extruder", "eclipse-component-depot", "eclipse-water-treatment-plant", "eclipse-augmentation-coupler", "eclipse-ore-crusher"},
        {"components", "eclipse-titanium-gear", "eclipse-industrial-bearing", "eclipse-precision-component", "eclipse-superalloy-frame", "eclipse-experimental-core", "eclipse-gear-press", "eclipse-component-depot"},
        {"metallurgy", "eclipse-ore-nickel", "eclipse-ore-chromium", "eclipse-ore-cobalt", "eclipse-ore-tungsten", "eclipse-ore-molybdenum", "eclipse-ore-platinum", "eclipse-ore-crusher", "eclipse-alloy-furnace", "eclipse-carbide-drill"},
        {"heat", "eclipse-industrial-heater", "eclipse-electric-heater", "eclipse-geothermal-heat-tap", "eclipse-thermal-storage", "eclipse-heat-exchanger"},
        {"chemistry", "eclipse-sulfuric-acid", "eclipse-acidic-waste", "eclipse-polymer-conduit", "eclipse-industrial-neutralizer", "eclipse-catalyst-plant", "eclipse-high-pressure-reactor"},
        {"electronics", "eclipse-copper-wire", "eclipse-circuit-board", "eclipse-microchip", "eclipse-control-unit"},
        {"power", "eclipse-steam-turbine", "eclipse-hydrogen-fuel-cell", "eclipse-fission-reactor", "eclipse-industrial-battery", "eclipse-world-engine"},
        {"logistics", "eclipse-three-way-sorter", "eclipse-distribution-hub", "eclipse-priority-merger", "eclipse-controlled-splitter", "eclipse-line-balancer", "eclipse-smart-unloader", "eclipse-batch-gate", "eclipse-liquid-overflow-valve"},
        {"augmentation", "eclipse-augmentation-coupler", "eclipse-basic-module-assembler", "eclipse-production-accelerator-1", "eclipse-module-recycling-facility"},
        {"robotics", "eclipse-robot-dispatch-hub", "eclipse-logistics-drone", "eclipse-repair-drone", "eclipse-shield-drone"},
        {"defense", "eclipse-wave-intelligence-scanner", "eclipse-wave-disruption-array", "eclipse-unit-shield-beacon", "eclipse-emergency-repair-dispatch-bay"},
        {"military", "eclipse-acid-mortar", "eclipse-heat-lance", "eclipse-minelayer", "eclipse-jammer", "eclipse-piledriver", "eclipse-kestrel", "eclipse-thunderhead", "eclipse-cloudbreaker", "eclipse-rivet", "eclipse-sickle", "eclipse-tremor", "eclipse-apiary", "eclipse-event-horizon", "eclipse-reaper", "eclipse-wraith", "eclipse-orbital-weapons-platform", "eclipse-titan-manufacturing-complex"},
        {"factions", "eclipse-mite", "eclipse-wasp", "eclipse-spitter", "eclipse-brood", "eclipse-behemoth", "eclipse-colossus", "eclipse-leviathan"},
        {"exploration", "eclipse-aetherite", "eclipse-ore-aetherite", "eclipse-aetherite-module-forge", "eclipse-cooling-tower", "eclipse-biomass-cultivator"},
    };

    /** The icon of each guide in the menu: an ECLIPSE content name whose icon stands for the topic (start uses a vanilla icon). */
    public static final String[][] guideIcons = {
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
    };

    final Seq<Object> history = new Seq<>();
    @Nullable Object current;
    String query = "";
    final Table list = new Table(), page = new Table();
    ScrollPane pageScroll;

    /** Adds the wiki button to the pause menu. Call on ClientLoadEvent. */
    public static @Nullable EclipseWiki instance;
    /** Opens the wiki on whatever ECLIPSE block or unit is under the cursor (or selected for building). I by default. */
    public static @Nullable arc.input.KeyBind entryKey;

    /** Registers the key early (mod constructor) so the Controls menu lists it. Desktop clients only. */
    public static void registerKey(){
        if(headless || mobile) return;
        entryKey = arc.input.KeyBind.add("eclipse_wiki_entry", arc.input.KeyCode.i, "eclipse");
        Events.run(mindustry.game.EventType.Trigger.update, () -> {
            if(instance == null || !Core.input.keyTap(entryKey) || Core.scene.getKeyboardFocus() instanceof TextField) return;
            if(instance.isShown()){
                instance.hide();
                return;
            }
            UnlockableContent c = hoveredContent();
            instance.show();
            if(c != null) instance.open(c, true);
        });
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
                //as wide as the widest button (e.g. "Host Multiplayer Game"), not the narrow centered quit button
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
        buttons.button("@eclipse-wiki.previous", Icon.left, this::back).disabled(b -> history.isEmpty()).size(210f, 64f);

        cont.table(left -> {
            left.top();
            left.table(s -> {
                s.image(Icon.zoom).padRight(6f);
                s.field("", text -> {
                    query = text.trim().toLowerCase();
                    buildList();
                }).growX().get().setMessageText(Core.bundle.get("eclipse-wiki.search"));
            }).growX().padBottom(6f).row();
            left.pane(Styles.smallPane, list).grow().get().setScrollingDisabledX(true);
        }).width(mobile ? 240f : 320f).growY().padRight(10f);
        pageScroll = cont.pane(page).grow().get();
        pageScroll.setScrollingDisabledX(true);

        shown(() -> {
            buildList();
            if(current == null) open(guides[0][0], false);
        });
    }

    // ---- left: guides and the catalogue ----

    void buildList(){
        list.clear();
        list.top().left().defaults().left().growX();

        if(query.isEmpty()){
            header(list, "eclipse-wiki.guides");
            for(String[] g : guides){
                String id = g[0];
                list.button(b -> {
                    b.left();
                    b.image(guideIcon(id)).size(24f).padRight(8f);
                    b.add(Core.bundle.get("eclipse-wiki.guide." + id + ".name")).left().growX().wrap();
                }, Styles.flatt, () -> open(id, true)).height(38f).row();
            }
        }

        section("eclipse-wiki.items", all(c -> c instanceof Item && !(c instanceof AugmentModule)));
        section("eclipse-wiki.modules", all(c -> c instanceof AugmentModule));
        section("eclipse-wiki.liquids", all(c -> c instanceof Liquid));
        for(Category cat : Category.all){
            section("eclipse-wiki.cat." + cat.name(), all(c -> c instanceof Block b && !(b instanceof OreBlock) && b.category == cat && b.isVisible()));
        }
        section("eclipse-wiki.ores", all(c -> c instanceof OreBlock));
        section("eclipse-wiki.units", all(c -> c instanceof UnitType));
    }

    /** All ECLIPSE content matching the filter and the search text. */
    Seq<UnlockableContent> all(arc.func.Boolf<UnlockableContent> filter){
        Seq<UnlockableContent> out = new Seq<>();
        for(ContentType type : new ContentType[]{ContentType.item, ContentType.liquid, ContentType.block, ContentType.unit}){
            for(Content c : content.getBy(type)){
                if(c instanceof UnlockableContent u && isEclipse(u) && filter.get(u)
                    && (query.isEmpty() || u.localizedName.toLowerCase().contains(query))) out.add(u);
            }
        }
        return out;
    }

    void section(String key, Seq<UnlockableContent> entries){
        if(entries.isEmpty()) return;
        header(list, key);
        for(UnlockableContent c : entries){
            list.button(b -> {
                b.left();
                b.image(c.uiIcon).size(24f).padRight(8f);
                b.add(c.localizedName).left().growX().wrap();
            }, Styles.flatt, () -> open(c, true)).height(36f).padLeft(4f).row();
        }
    }

    static void header(Table t, String key){
        t.add("[accent]" + Core.bundle.get(key)).padTop(10f).padBottom(2f).row();
        t.image().color(Pal.accent).height(2f).growX().padBottom(4f).row();
    }

    // ---- right: pages ----

    /** Opens a guide (String id) or a content entry. */
    public void open(Object target, boolean remember){
        if(remember && current != null && current != target) history.add(current);
        current = target;
        page.clear();
        page.top().left().defaults().left();
        if(target instanceof String id) guidePage(id);
        else if(target instanceof UnlockableContent c) contentPage(c);
        pageScroll.setScrollY(0f);
    }

    void back(){
        if(history.isEmpty()) return;
        open(history.pop(), false);
    }

    float textWidth(){
        return Math.max(Math.min(Core.graphics.getWidth() / Scl.scl(1f) - (mobile ? 300f : 420f), 720f), 220f);
    }

    void guidePage(String id){
        page.add("[accent]" + Core.bundle.get("eclipse-wiki.guide." + id + ".name")).style(Styles.outlineLabel).padBottom(8f).row();
        page.add(Core.bundle.get("eclipse-wiki.guide." + id + ".text")).wrap().width(textWidth()).padBottom(10f).row();
        for(String[] g : guides){
            if(!g[0].equals(id) || g.length < 2) continue;
            sub(page, "eclipse-wiki.related");
            Table links = new Table().left();
            for(int i = 1; i < g.length; i++){
                UnlockableContent c = byName(g[i]);
                if(c != null) link(links, c, null);
                if(i % 3 == 0) links.row();
            }
            page.add(links).left().row();
        }
    }

    void contentPage(UnlockableContent c){
        page.table(head -> {
            head.left();
            head.image(c.uiIcon).size(64f).padRight(12f);
            head.table(n -> {
                n.left().defaults().left();
                n.add("[accent]" + c.localizedName).style(Styles.outlineLabel).row();
                n.add("[lightgray]" + kind(c)).row();
            });
        }).padBottom(10f).row();

        String key = textKey(c);
        String text = Core.bundle.has(key) ? Core.bundle.get(key) : c.description;
        if(text != null) page.add(text).wrap().width(textWidth()).padBottom(8f).row();
        //the short in-game description of a module tier, under its family's explanation
        if(c instanceof AugmentModule && c.description != null && !key.equals("wiki." + c.name)) page.add("[lightgray]" + c.description).wrap().width(textWidth()).padBottom(8f).row();

        //an animated minimal setup for crafters, built from the recipe
        if(c instanceof Block sb && SetupDiagram.supports(sb)){
            sub(page, "eclipse-wiki.setup");
            page.add(new SetupDiagram(sb)).left().padBottom(6f).row();
            page.add("[lightgray]" + Core.bundle.get("eclipse-wiki.setup.hint")).wrap().width(textWidth()).left().padBottom(8f).row();
        }
        if(c instanceof Block b) blockFacts(b);
        if(c instanceof Item item) materialFacts(item);
        if(c instanceof Liquid liquid) materialFacts(liquid);
        if(c instanceof UnitType u) unitFacts(u);

        if(c.techNode != null && c.techNode.parent != null){
            sub(page, "eclipse-wiki.research");
            Table t = new Table().left();
            link(t, c.techNode.parent.content, null);
            page.add(t).left().row();
        }
        page.button("@eclipse-wiki.stats", Icon.info, Styles.flatt, () -> ui.content.show(c)).size(240f, 44f).padTop(14f).row();
    }

    String kind(UnlockableContent c){
        if(c instanceof AugmentModule m) return Core.bundle.format("eclipse-wiki.kind.module", m.romanTier());
        if(c instanceof OreBlock) return Core.bundle.get("eclipse-wiki.ores");
        if(c instanceof Block b) return Core.bundle.get("eclipse-wiki.cat." + b.category.name());
        if(c instanceof Item) return Core.bundle.get("eclipse-wiki.kind.item");
        if(c instanceof Liquid) return Core.bundle.get("eclipse-wiki.kind.liquid");
        return Core.bundle.get("eclipse-wiki.kind.unit");
    }

    // ---- facts read from the game ----

    void blockFacts(Block b){
        if(b instanceof OreBlock ore && ore.itemDrop != null){
            sub(page, "eclipse-wiki.yields");
            row(page, ore.itemDrop, null);
            return;
        }
        if(b.requirements != null && b.requirements.length > 0){
            sub(page, "eclipse-wiki.cost");
            stacks(page, b.requirements);
        }

        //what it needs to run
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
        if(b instanceof eclipse.world.blocks.ChemicalReactor r && r.catalyst != null){
            link(needs, r.catalyst, Core.bundle.format("eclipse-wiki.catalyst", r.craftsPerCatalyst));
            needs.row();
        }
        if(needs.hasChildren()){
            sub(page, "eclipse-wiki.needs");
            page.add(needs).left().row();
        }

        //what it makes
        Table makes = new Table().left();
        if(b instanceof GenericCrafter g){
            if(g.outputItems != null) for(ItemStack s : g.outputItems){ link(makes, s.item, perCraft(s.amount, seconds)); makes.row(); }
            if(g.outputLiquids != null) for(LiquidStack s : g.outputLiquids){ link(makes, s.liquid, perSecond(s.amount * 60f)); makes.row(); }
        }
        if(b instanceof Separator sep) for(ItemStack s : sep.results){ link(makes, s.item, null); makes.row(); }
        if(b instanceof HeatProducer hp) makes.add("[lightgray]" + Core.bundle.format("eclipse-wiki.heat-out", Strings.autoFixed(hp.heatOutput, 1))).left().row();
        if(b instanceof PowerGenerator pg && pg.powerProduction > 0f) makes.add("[lightgray]" + Core.bundle.format("eclipse-wiki.power-out", Strings.autoFixed(pg.powerProduction * 60f, 1))).left().row();
        if(b instanceof Drill d) makes.add("[lightgray]" + Core.bundle.format("eclipse-wiki.drill", d.tier)).left().row();
        if(b instanceof UnitFactory f) for(UnitFactory.UnitPlan p : f.plans){ link(makes, p.unit, null); makes.row(); }
        if(b instanceof ModuleAssembler a) for(AugmentModule m : a.modules()){ link(makes, m, null); makes.row(); }
        if(makes.hasChildren()){
            sub(page, "eclipse-wiki.makes");
            page.add(makes).left().row();
        }

        //ammunition
        Table ammo = new Table().left();
        if(b instanceof ItemTurret it) for(Item i : it.ammoTypes.keys()) link(ammo, i, null);
        if(b instanceof LiquidTurret lt) for(Liquid l : lt.ammoTypes.keys()) link(ammo, l, null);
        if(ammo.hasChildren()){
            sub(page, "eclipse-wiki.ammo");
            page.add(ammo).left().row();
        }
    }

    void materialFacts(UnlockableContent mat){
        //made by
        Table madeBy = new Table().left();
        int n = 0;
        for(Block b : content.blocks()){
            if(makes(b, mat)){
                link(madeBy, b, null);
                if(++n % 3 == 0) madeBy.row();
            }
        }
        if(madeBy.hasChildren()){
            sub(page, "eclipse-wiki.made-by");
            page.add(madeBy).left().row();
        }
        if(mat instanceof AugmentModule m && m.recipe.length > 0){
            sub(page, "eclipse-wiki.recipe");
            stacks(page, m.recipe);
        }

        //used by: machine inputs, ammunition, module recipes, unit costs
        Table usedBy = new Table().left();
        n = 0;
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
            sub(page, "eclipse-wiki.used-by");
            page.add(usedBy).left().row();
        }

        //build costs
        if(mat instanceof Item i){
            Table builds = new Table().left();
            n = 0;
            for(Block b : content.blocks()){
                if(b.requirements != null && b.isVisible() && contains(b.requirements, i)){
                    builds.button(bt -> bt.image(b.uiIcon).size(32f), Styles.clearNonei, () -> open(b)).size(40f).tooltip(b.localizedName);
                    if(++n % 12 == 0) builds.row();
                }
            }
            if(builds.hasChildren()){
                sub(page, "eclipse-wiki.build-cost-of");
                page.add(builds).left().row();
            }
        }
    }

    void unitFacts(UnitType u){
        Table t = new Table().left();
        t.add("[lightgray]" + Core.bundle.format("eclipse-wiki.unit-stats", (int)u.health, Strings.autoFixed(u.armor, 1),
            Strings.autoFixed(u.speed * 60f / tilesize, 1), Core.bundle.get(u.flying ? "eclipse-wiki.air" : u.naval ? "eclipse-wiki.naval" : "eclipse-wiki.ground"))).left().row();
        page.add(t).left().padTop(4f).row();

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
                    row.add(" -> ").padRight(4f);
                    link(row, b, null);
                }).left().row();
            }
        }
        sub(page, "eclipse-wiki.built-in");
        if(built.hasChildren()) page.add(built).left().row();
        else page.add("[lightgray]" + Core.bundle.get("eclipse-wiki.enemy-only")).left().row();
    }

    static boolean makes(Block b, UnlockableContent mat){
        if(b instanceof GenericCrafter g){
            if(g.outputItems != null) for(ItemStack s : g.outputItems) if(s.item == mat) return true;
            if(g.outputLiquids != null) for(LiquidStack s : g.outputLiquids) if(s.liquid == mat) return true;
        }
        if(b instanceof Separator sep) for(ItemStack s : sep.results) if(s.item == mat) return true;
        if(b instanceof OreBlock ore) return ore.itemDrop == mat;
        if(b instanceof ModuleAssembler a && mat instanceof AugmentModule m) return a.canMake(m);
        return false;
    }

    static boolean uses(Block b, UnlockableContent mat){
        for(Consume cons : b.consumers){
            if(cons instanceof ConsumeItems ci && mat instanceof Item i && contains(ci.items, i)) return true;
            if(cons instanceof ConsumeLiquid cl && cl.liquid == mat) return true;
            if(cons instanceof ConsumeLiquids cls) for(LiquidStack s : cls.liquids) if(s.liquid == mat) return true;
            if(cons instanceof ConsumeItemFilter f && mat instanceof Item i && f.filter.get(i)) return true;
            if(cons instanceof ConsumeLiquidFilter f && mat instanceof Liquid l && f.filter.get(l)) return true;
        }
        if(b instanceof eclipse.world.blocks.ChemicalReactor r && r.catalyst == mat) return true;
        if(b instanceof ItemTurret it && mat instanceof Item i && it.ammoTypes.containsKey(i)) return true;
        if(b instanceof LiquidTurret lt && mat instanceof Liquid l && lt.ammoTypes.containsKey(l)) return true;
        return false;
    }

    static boolean contains(ItemStack[] stacks, Item item){
        for(ItemStack s : stacks) if(s.item == item) return true;
        return false;
    }

    // ---- small UI helpers ----

    static void sub(Table t, String key){
        t.add("[accent]" + Core.bundle.get(key)).padTop(10f).padBottom(2f).row();
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

    /** A clickable icon + name (+ amount): ECLIPSE content opens its wiki entry, vanilla content the game's database. */
    void link(Table t, UnlockableContent c, @Nullable String amount){
        t.button(b -> {
            b.left();
            b.image(c.uiIcon).size(24f).padRight(4f);
            b.add((amount == null ? "" : "[lightgray]" + amount + "[] ") + c.localizedName).left();
        }, Styles.flatt, () -> open(c)).height(32f).pad(1f).padRight(10f).left();
    }

    void open(UnlockableContent c){
        if(isEclipse(c)) open(c, true);
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

    static boolean isEclipse(UnlockableContent c){
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

    static @Nullable UnlockableContent byName(String name){
        for(ContentType type : new ContentType[]{ContentType.block, ContentType.item, ContentType.liquid, ContentType.unit}){
            Content c = content.getByName(type, name);
            if(c instanceof UnlockableContent u) return u;
        }
        return null;
    }
}
