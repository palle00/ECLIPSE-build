package eclipse.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.util.*;
import eclipse.world.blocks.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.heat.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.production.HeatCrafter.*;

import static mindustry.Vars.*;

/**
 * Hints drawn in the world (desktop):
 * <ul>
 * <li>Liquid outputs by side: coloured arrows at each fixed output side of a machine, while placing it (next to the
 * game's own liquid icons) and while hovering a built ECLIPSE machine (icons and arrows).</li>
 * <li>Heat: while placing a heater, the block in front of it is outlined with how much heat it would get and what it
 * needs, or a warning if it does not use heat.</li>
 * <li>Ratios: while placing a machine next to one that feeds it (or that it feeds), how many suppliers keep one
 * consumer running, from both machines' rates.</li>
 * </ul>
 */
public class PlacementHints{

    public static void init(){
        if(headless) return;
        Events.run(Trigger.draw, PlacementHints::draw);
    }

    static void draw(){
        if(!state.isGame() || player == null || mobile || Core.scene.hasMouse()) return;
        Vec2 m = Core.input.mouseWorld();
        Block selected = control.input.block;
        Draw.z(Layer.overlayUI);
        if(selected != null){
            int tx = World.toTile(m.x - selected.offset), ty = World.toTile(m.y - selected.offset);
            int rotation = selected.rotate ? control.input.rotation : 0;
            if(selected instanceof eclipse.world.blocks.RecipeCrafter rc){
                //lastConfig is only set by a pick, and is then what the placed machine gets
                eclipse.world.blocks.RecipeCrafter.Recipe r = rc.lastConfig instanceof Integer v ? rc.unpackRecipe(v) : null;
                if(r == null) r = rc.recipes.find(x -> x.liquidSides.length > 0);
                if(r != null) recipeArrows(rc, r, tx * tilesize + selected.offset, ty * tilesize + selected.offset, rotation);
            }else if(selected instanceof GenericCrafter g) outputArrows(g, tx * tilesize + selected.offset, ty * tilesize + selected.offset, rotation, false);
            if(selected instanceof HeatProducer heater) heatPreview(heater, tx, ty, rotation);
            ratioHint(selected, tx, ty);
        }else{
            Building b = world.buildWorld(m.x, m.y);
            if(b instanceof eclipse.world.blocks.RecipeCrafter.RecipeBuild r && r.recipe != null) recipeArrows((eclipse.world.blocks.RecipeCrafter)b.block, r.recipe, b.x, b.y, b.rotation);
            else if(b != null && b.block instanceof GenericCrafter g && b.block.name.startsWith("eclipse-")) outputArrows(g, b.x, b.y, b.rotation, true);
        }
        Draw.reset();
    }

    static final arc.struct.ObjectFloatMap<mindustry.ctype.UnlockableContent> nIn = new arc.struct.ObjectFloatMap<>(), nOut = new arc.struct.ObjectFloatMap<>(),
        bIn = new arc.struct.ObjectFloatMap<>(), bOut = new arc.struct.ObjectFloatMap<>();

    /** Supplier/consumer ratios with the machines touching a crafter about to be placed at (tx, ty). */
    static void ratioHint(Block block, int tx, int ty){
        if(!(block instanceof GenericCrafter)) return;
        int lo = (block.size - 1) / 2, hi = block.size / 2;
        arc.struct.ObjectSet<Building> near = new arc.struct.ObjectSet<>();
        for(int x = tx - lo - 1; x <= tx + hi + 1; x++){
            for(int y = ty - lo - 1; y <= ty + hi + 1; y++){
                boolean edgeX = x == tx - lo - 1 || x == tx + hi + 1, edgeY = y == ty - lo - 1 || y == ty + hi + 1;
                if(edgeX == edgeY) continue; //inside the footprint, or a corner (not touching)
                Building b = world.build(x, y);
                if(b != null && b.team == player.team()) near.add(b);
            }
        }
        if(near.isEmpty()) return;
        eclipse.world.blocks.RecipeCrafter.Recipe picked = block instanceof eclipse.world.blocks.RecipeCrafter rc && rc.lastConfig instanceof Integer v ? rc.unpackRecipe(v) : null;
        arc.struct.Seq<eclipse.world.blocks.RecipeCrafter.Recipe> candidates = new arc.struct.Seq<>();
        if(block instanceof eclipse.world.blocks.RecipeCrafter rc) candidates.addAll(picked != null ? arc.struct.Seq.with(picked) : rc.recipes);
        else candidates.add((eclipse.world.blocks.RecipeCrafter.Recipe)null);
        arc.struct.Seq<String> lines = new arc.struct.Seq<>();
        for(Building n : near){
            nIn.clear();
            nOut.clear();
            if(!Rates.base(n, nIn, nOut)) continue;
            for(eclipse.world.blocks.RecipeCrafter.Recipe r : candidates){
                bIn.clear();
                bOut.clear();
                if(!Rates.base(block, r, bIn, bOut)) continue;
                int before = lines.size;
                for(var e : bIn) if(nOut.get(e.key, 0f) > 0f){
                    lines.add(Core.bundle.format("eclipse-hint.ratio-in", Strings.autoFixed(e.value / nOut.get(e.key, 0f), 2), n.block.localizedName, e.key.localizedName));
                }
                for(var e : bOut) if(nIn.get(e.key, 0f) > 0f){
                    lines.add(Core.bundle.format("eclipse-hint.ratio-out", Strings.autoFixed(nIn.get(e.key, 0f) / e.value, 2), n.block.localizedName, e.key.localizedName));
                }
                if(lines.size > before) break; //a universal machine: the first recipe that links to this neighbour
            }
            if(lines.size >= 3) break;
        }
        if(lines.isEmpty()) return;
        block.drawPlaceText(lines.toString(" | "), tx, ty + hi + 1, true);
    }

    /** Arrows for a universal machine's recipe with fixed liquid output sides. */
    static void recipeArrows(eclipse.world.blocks.RecipeCrafter rc, eclipse.world.blocks.RecipeCrafter.Recipe r, float x, float y, int rotation){
        for(int i = 0; i < r.liquidOutputs.length; i++){
            if(r.sideOf(i) >= 0) arrow(rc, r.liquidOutputs[i].liquid, r.sideOf(i), x, y, rotation);
        }
    }

    /** An arrow (and, for built machines, the liquid's icon) at each side with a fixed liquid output. */
    static void outputArrows(GenericCrafter g, float x, float y, int rotation, boolean icons){
        if(g.outputLiquids == null || g.liquidOutputDirections == null) return;
        if(icons) g.drawOverlay(x, y, g.rotate ? rotation : 0);
        for(int i = 0; i < g.outputLiquids.length && i < g.liquidOutputDirections.length; i++){
            int dir = g.liquidOutputDirections[i];
            if(dir == -1) continue;
            arrow(g, g.outputLiquids[i].liquid, dir, x, y, rotation);
        }
    }

    /** One arrow at a fixed liquid output side, in the liquid's colour. */
    static void arrow(Block g, Liquid liquid, int dir, float x, float y, int rotation){
        int d = (dir + (g.rotate ? rotation : 0)) % 4;
        //outward and sideways unit vectors; the arrow sits just inside the edge, pointing out (the liquid icon is just outside)
        float ux = Geometry.d4x(d), uy = Geometry.d4y(d), sx = -uy, sy = ux, edge = g.size * tilesize / 2f;
        float bx = x + ux * (edge - 4f), by = y + uy * (edge - 4f), tipX = x + ux * (edge - 0.5f), tipY = y + uy * (edge - 0.5f);
        Draw.color(Pal.darkestGray);
        Fill.tri(bx + sx * 2.9f - ux * 0.8f, by + sy * 2.9f - uy * 0.8f, bx - sx * 2.9f - ux * 0.8f, by - sy * 2.9f - uy * 0.8f, tipX + ux * 1f, tipY + uy * 1f);
        Draw.color(liquid.color);
        Fill.tri(bx + sx * 2f, by + sy * 2f, bx - sx * 2f, by - sy * 2f, tipX, tipY);
    }

    /** The building on the tile right in front of a block of this size placed at (tx, ty) facing rotation (odd or even size). */
    public static @Nullable Building heatTarget(Block heater, int tx, int ty, int rotation){
        int dx = Geometry.d4x(rotation), dy = Geometry.d4y(rotation), s = heater.size;
        int fx = tx + (dx > 0 ? s / 2 + 1 : dx < 0 ? -(s - 1) / 2 - 1 : 0);
        int fy = ty + (dy > 0 ? s / 2 + 1 : dy < 0 ? -(s - 1) / 2 - 1 : 0);
        return world.build(fx, fy);
    }

    /** Outlines the block a heater would heat, with the heat it adds and what that block needs. */
    static void heatPreview(HeatProducer heater, int tx, int ty, int rotation){
        Building target = heatTarget(heater, tx, ty, rotation);
        if(target == null) return;

        float heat = heater instanceof GeothermalHeater geo ? geo.sumAttribute(geo.attribute, tx, ty) * geo.heatOutput : heater.heatOutput;
        String text;
        Color color;
        float need = -1f, now = 0f;
        if(target instanceof HeatCrafterBuild h){
            need = h.heatRequirement();
            now = h.heat;
        }else if(target instanceof eclipse.world.blocks.RecipeCrafter.RecipeBuild r && ((eclipse.world.blocks.RecipeCrafter)r.block).usesHeat){
            need = r.heatRequirement();
            now = r.heat;
        }
        boolean noHeatUse = target instanceof eclipse.world.blocks.RecipeCrafter.RecipeBuild r && !((eclipse.world.blocks.RecipeCrafter)r.block).usesHeat;
        if(need >= 0f){
            boolean enough = now + heat >= need - 0.001f;
            text = Core.bundle.format("eclipse-hint.heat-to", Strings.autoFixed(heat, 1), target.block.localizedName, Strings.autoFixed(now + heat, 1), Strings.autoFixed(need, 1));
            color = enough ? Pal.accent : Pal.lightOrange;
        }else if(!noHeatUse && (target instanceof HeatBlock || target instanceof HeatConsumer)){
            text = Core.bundle.format("eclipse-hint.heat-relay", Strings.autoFixed(heat, 1), target.block.localizedName);
            color = Pal.accent;
        }else{
            text = Core.bundle.format("eclipse-hint.heat-waste", target.block.localizedName);
            color = Pal.remove;
        }
        Lines.stroke(1f, color);
        Lines.square(target.x, target.y, target.block.size * tilesize / 2f + 1f);
        Draw.reset();
        target.block.drawPlaceText(text, target.tileX(), target.tileY(), color != Pal.remove);
    }
}
