package eclipse.ui;

import arc.*;
import arc.struct.*;
import eclipse.augment.*;
import eclipse.world.blocks.*;
import mindustry.ctype.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.production.*;

import static mindustry.Vars.*;

/**
 * The wiki's search: names first, and for the material that matches best, where it comes from and where it goes
 * (the ore mineral that yields it, the machines that make it, the ones that use it), each with the reason it matched.
 * Plain data, so the tests can check it without a UI.
 */
public class WikiSearch{
    /** One result: an entry (content or guide id) and why it matched. */
    public static class Hit{
        public @arc.util.Nullable UnlockableContent content;
        public @arc.util.Nullable String guide;
        public String reason = "";
    }

    /** A titled group of results (bundle key of the title). */
    public static class Group{
        public String key;
        public final Seq<Hit> hits = new Seq<>();

        Group(String key){
            this.key = key;
        }
    }

    public static Seq<Group> search(String query, boolean unlockedOnly){
        Seq<Group> out = new Seq<>();
        String q = query.trim().toLowerCase();
        if(q.isEmpty()) return out;

        Group guides = new Group("eclipse-wiki.search.guides");
        for(String[] g : EclipseWiki.guides){
            String name = Core.bundle.get("eclipse-wiki.guide." + g[0] + ".name", g[0]);
            Hit h = new Hit();
            h.guide = g[0];
            if(name.toLowerCase().contains(q)) h.reason = Core.bundle.get("eclipse-wiki.search.name");
            else if(Core.bundle.get("eclipse-wiki.guide." + g[0] + ".text", "").toLowerCase().contains(q)) h.reason = Core.bundle.get("eclipse-wiki.search.text");
            else continue;
            guides.hits.add(h);
        }
        guides.hits.sort(h -> h.reason.equals(Core.bundle.get("eclipse-wiki.search.name")) ? 0 : 1);

        Group names = new Group("eclipse-wiki.search.names");
        for(UnlockableContent c : EclipseWiki.entries()){
            if(unlockedOnly && !c.unlockedNow()) continue;
            if(!c.localizedName.toLowerCase().contains(q)) continue;
            Hit h = new Hit();
            h.content = c;
            h.reason = kind(c);
            names.hits.add(h);
        }
        //names starting with the query first, then shorter names (closer matches)
        names.hits.sort(h -> (h.content.localizedName.toLowerCase().startsWith(q) ? 0 : 1000) + h.content.localizedName.length());
        if(names.hits.any()) out.add(names);

        //the best matching material: where it comes from and where it goes
        UnlockableContent mat = null;
        for(Hit h : names.hits) if(h.content instanceof Item && !(h.content instanceof AugmentModule) || h.content instanceof Liquid){ mat = h.content; break; }
        if(mat != null){
            Group ore = new Group("eclipse-wiki.search.ore"), makes = new Group("eclipse-wiki.search.makes"), uses = new Group("eclipse-wiki.search.uses");
            for(Block b : content.blocks()){
                if(b instanceof OreBlock o && o.itemDrop != null && o.itemDrop != mat && b.name.startsWith("eclipse-") && yields(o.itemDrop, mat)){
                    Hit h = new Hit();
                    h.content = o.itemDrop;
                    h.reason = Core.bundle.format("eclipse-wiki.search.yields", mat.localizedName);
                    ore.hits.add(h);
                }
                if(!b.isVisible() || b instanceof OreBlock) continue;
                if(EclipseWiki.makes(b, mat)){
                    Hit h = new Hit();
                    h.content = b;
                    h.reason = from(b, mat);
                    makes.hits.add(h);
                }
                if(EclipseWiki.uses(b, mat)){
                    Hit h = new Hit();
                    h.content = b;
                    h.reason = into(b, mat);
                    uses.hits.add(h);
                }
            }
            for(Group g : new Group[]{ore, makes, uses}) if(g.hits.any()) out.add(g);
        }
        if(guides.hits.any()) out.add(guides);
        return out;
    }

    /** True if some machine turns this ore mineral into the material. */
    static boolean yields(Item mineral, UnlockableContent mat){
        for(Block b : content.blocks()){
            if(b instanceof RecipeCrafter rc){
                for(RecipeCrafter.Recipe r : rc.recipes){
                    if(r.inputs.length > 0 && r.inputs[0].item == mineral) for(ItemStack s : r.outputs) if(s.item == mat) return true;
                }
            }
        }
        return false;
    }

    /** "from Pentlandite": the first input of the recipe that makes it. */
    static String from(Block b, UnlockableContent mat){
        if(b instanceof RecipeCrafter rc){
            for(RecipeCrafter.Recipe r : rc.recipes){
                boolean makes = false;
                for(ItemStack s : r.outputs) if(s.item == mat) makes = true;
                for(LiquidStack s : r.liquidOutputs) if(s.liquid == mat) makes = true;
                if(!makes) continue;
                if(r.inputs.length > 0) return Core.bundle.format("eclipse-wiki.search.from", r.inputs[0].item.localizedName);
                if(r.liquidInputs.length > 0) return Core.bundle.format("eclipse-wiki.search.from", r.liquidInputs[0].liquid.localizedName);
            }
        }
        return "";
    }

    /** "for Hot Superalloy": the product of the first recipe that uses it. */
    static String into(Block b, UnlockableContent mat){
        if(b instanceof RecipeCrafter rc){
            for(RecipeCrafter.Recipe r : rc.recipes){
                boolean used = r.catalyst == mat;
                for(ItemStack s : r.inputs) if(s.item == mat) used = true;
                for(LiquidStack s : r.liquidInputs) if(s.liquid == mat) used = true;
                if(used) return Core.bundle.format("eclipse-wiki.search.for", r.output().localizedName);
            }
        }
        if(b instanceof GenericCrafter g && g.outputItems != null && g.outputItems.length > 0) return Core.bundle.format("eclipse-wiki.search.for", g.outputItems[0].item.localizedName);
        return "";
    }

    static String kind(UnlockableContent c){
        if(c instanceof AugmentModule) return Core.bundle.get("eclipse-wiki.kind.module-short");
        if(c instanceof Item) return Core.bundle.get("eclipse-wiki.kind.item");
        if(c instanceof Liquid) return Core.bundle.get("eclipse-wiki.kind.liquid");
        if(c instanceof UnitType) return Core.bundle.get("eclipse-wiki.kind.unit");
        if(c instanceof OreBlock) return Core.bundle.get("eclipse-wiki.ores");
        if(c instanceof Block b) return Core.bundle.get("eclipse-wiki.cat." + b.category.name());
        return "";
    }
}
