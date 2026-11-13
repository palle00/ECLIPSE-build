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
            if(selected instanceof GenericCrafter g) outputArrows(g, tx * tilesize + selected.offset, ty * tilesize + selected.offset, rotation, false);
            if(selected instanceof HeatProducer heater) heatPreview(heater, tx, ty, rotation);
        }else{
            Building b = world.buildWorld(m.x, m.y);
            if(b != null && b.block instanceof GenericCrafter g && b.block.name.startsWith("eclipse-")) outputArrows(g, b.x, b.y, b.rotation, true);
        }
        Draw.reset();
    }

    /** An arrow (and, for built machines, the liquid's icon) at each side with a fixed liquid output. */
    static void outputArrows(GenericCrafter g, float x, float y, int rotation, boolean icons){
        if(g.outputLiquids == null || g.liquidOutputDirections == null) return;
        if(icons) g.drawOverlay(x, y, g.rotate ? rotation : 0);
        for(int i = 0; i < g.outputLiquids.length && i < g.liquidOutputDirections.length; i++){
            int dir = g.liquidOutputDirections[i];
            if(dir == -1) continue;
            int d = (dir + (g.rotate ? rotation : 0)) % 4;
            Liquid liquid = g.outputLiquids[i].liquid;
            //outward and sideways unit vectors; the arrow sits just inside the edge, pointing out (the liquid icon is just outside)
            float ux = Geometry.d4x(d), uy = Geometry.d4y(d), sx = -uy, sy = ux, edge = g.size * tilesize / 2f;
            float bx = x + ux * (edge - 4f), by = y + uy * (edge - 4f), tipX = x + ux * (edge - 0.5f), tipY = y + uy * (edge - 0.5f);
            Draw.color(Pal.darkestGray);
            Fill.tri(bx + sx * 2.9f - ux * 0.8f, by + sy * 2.9f - uy * 0.8f, bx - sx * 2.9f - ux * 0.8f, by - sy * 2.9f - uy * 0.8f, tipX + ux * 1f, tipY + uy * 1f);
            Draw.color(liquid.color);
            Fill.tri(bx + sx * 2f, by + sy * 2f, bx - sx * 2f, by - sy * 2f, tipX, tipY);
        }
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
        if(target instanceof HeatCrafterBuild h){
            float need = h.heatRequirement(), now = h.heat;
            boolean enough = now + heat >= need - 0.001f;
            text = Core.bundle.format("eclipse-hint.heat-to", Strings.autoFixed(heat, 1), target.block.localizedName, Strings.autoFixed(now + heat, 1), Strings.autoFixed(need, 1));
            color = enough ? Pal.accent : Pal.lightOrange;
        }else if(target instanceof HeatBlock || target instanceof HeatConsumer){
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
