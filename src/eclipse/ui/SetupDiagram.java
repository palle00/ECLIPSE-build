package eclipse.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.consumers.*;

import static mindustry.Vars.*;

/**
 * An animated "minimal setup" for a crafter in the ECLIPSE wiki (in the spirit of AE2's guide pictures), built from
 * the block's own recipe so it never goes out of date:
 * - item inputs arrive on a belt from the left, the real ingredients riding it;
 * - liquid inputs arrive in conduits, the liquid flowing;
 * - item outputs leave on a belt, liquid outputs on their fixed sides (liquidOutputDirections) or any free side;
 * - heat crafters get a heater on a free side, with the heat glow on the edge it feeds;
 * - powered machines get a power node and its laser.
 * The machine itself runs: its rotor turns, its press presses, its glow pulses. Uses Time.globalTime, so it also
 * moves while the game is paused (the wiki is opened from the pause menu).
 */
public class SetupDiagram extends Element{
    /** Tiles of belt or conduit between the machine and the edge of the picture. */
    static final int margin = 3;

    final Block block;
    final int n, grid;
    final Seq<Item> itemsIn = new Seq<>(), itemsOut = new Seq<>();
    final Seq<Liquid> liquidsIn = new Seq<>();
    final Seq<Line> lines = new Seq<>();
    boolean power;
    int heaterSide = -1, heaterSlot;
    final TextureRegion[] belt = new TextureRegion[4];
    TextureRegion conduitBottom, conduitTop, rotator, press, glow, heat, heater, node;
    float ts;

    /** One belt or conduit run: side of the machine (0 right, 1 up, 2 left, 3 down), slot along that side. */
    static class Line{
        int side, slot;
        boolean out;
        Seq<Item> items;
        Liquid liquid;

        Line(int side, int slot, boolean out){
            this.side = side;
            this.slot = slot;
            this.out = out;
        }
    }

    /** True if a setup picture can be drawn for this block. */
    public static boolean supports(Block b){
        return b instanceof GenericCrafter || b instanceof Separator;
    }

    public SetupDiagram(Block b){
        block = b;
        n = b.size;
        grid = n + margin * 2;

        for(Consume c : b.consumers){
            if(c.optional) continue;
            if(c instanceof ConsumeItems ci) for(ItemStack s : ci.items) itemsIn.addUnique(s.item);
            else if(c instanceof ConsumeLiquid cl) liquidsIn.addUnique(cl.liquid);
            else if(c instanceof ConsumeLiquids cls) for(LiquidStack s : cls.liquids) liquidsIn.addUnique(s.liquid);
            else if(c instanceof ConsumePower cp && (cp.usage > 0f || cp.buffered)) power = true;
        }
        Seq<LiquidStack> liquidsOut = new Seq<>();
        int[] dirs = null;
        if(b instanceof GenericCrafter g){
            if(g.outputItems != null) for(ItemStack s : g.outputItems) itemsOut.addUnique(s.item);
            if(g.outputLiquids != null) liquidsOut.addAll(g.outputLiquids);
            dirs = g.liquidOutputDirections;
        }else if(b instanceof Separator sep){
            for(ItemStack s : sep.results) itemsOut.addUnique(s.item);
        }

        //each side has n slots; fixed liquid outputs claim their side first, then the belts, then liquid inputs, then heat
        boolean[][] used = new boolean[4][n];
        int mid = n / 2;
        Seq<Line> anySide = new Seq<>();
        for(int i = 0; i < liquidsOut.size; i++){
            int dir = dirs != null && i < dirs.length ? dirs[i] : -1;
            //output sides are relative to the way the machine faces; the picture shows rotating machines facing up,
            //so "left" and "right" in the descriptions are left and right here
            if(dir >= 0 && b.rotate) dir = (dir + 1) % 4;
            Line l = new Line(dir, mid, true);
            l.liquid = liquidsOut.get(i).liquid;
            if(dir >= 0 && dir < 4){
                l.slot = free(used[dir], mid);
                used[dir][l.slot] = true;
                lines.add(l);
            }else anySide.add(l);
        }
        if(itemsIn.any()){
            Line l = new Line(2, free(used[2], mid), false);
            l.items = itemsIn;
            used[2][l.slot] = true;
            lines.add(l);
        }
        if(itemsOut.any()){
            int side = !full(used[0]) ? 0 : 3;
            Line l = new Line(side, free(used[side], mid), true);
            l.items = itemsOut;
            used[side][l.slot] = true;
            lines.add(l);
        }
        for(Line l : anySide){
            l.side = pick(used, 0, 3, 1);
            l.slot = free(used[l.side], mid);
            used[l.side][l.slot] = true;
            lines.add(l);
        }
        for(Liquid liquid : liquidsIn){
            //rotating machines: feed from the back, keeping the front (marked) clear
            Line l = new Line(b.rotate ? pick(used, 3, 1, 2) : pick(used, 1, 3, 2), 0, false);
            l.slot = free(used[l.side], mid);
            used[l.side][l.slot] = true;
            l.liquid = liquid;
            lines.add(l);
        }
        if(b instanceof HeatCrafter){
            //the heater (2x2) needs two free neighbouring slots on one side
            for(int side : new int[]{3, 1, 2, 0}){
                for(int k = 0; k + 1 < n; k++){
                    if(!used[side][k] && !used[side][k + 1]){
                        heaterSide = side;
                        heaterSlot = k;
                        break;
                    }
                }
                if(heaterSide >= 0) break;
            }
        }

        for(int i = 0; i < 4; i++) belt[i] = Core.atlas.find("titanium-conveyor-0-" + i);
        conduitBottom = Core.atlas.find("conduit-bottom");
        conduitTop = Core.atlas.find("conduit-top-0");
        rotator = region(b.name + "-rotator");
        press = region(b.name + "-press");
        glow = region(b.name + "-glow");
        heat = region(b.name + "-heat");
        Block heaterBlock = content.block("eclipse-electric-heater");
        heater = heaterBlock != null ? heaterBlock.fullIcon : null;
        node = Blocks.powerNode.fullIcon;
    }

    static @Nullable TextureRegion region(String name){
        return Core.atlas.has(name) ? Core.atlas.find(name) : null;
    }

    /** A free slot on a side, as close to the middle as possible (the middle itself if free). */
    int free(boolean[] side, int mid){
        for(int d = 0; d < n; d++){
            if(mid + d < n && !side[mid + d]) return mid + d;
            if(mid - d >= 0 && !side[mid - d]) return mid - d;
        }
        return mid;
    }

    static boolean full(boolean[] side){
        for(boolean b : side) if(!b) return false;
        return true;
    }

    /** The first side in the preference order that still has a free slot. */
    static int pick(boolean[][] used, int... order){
        for(int side : order) if(!full(used[side])) return side;
        for(int side = 0; side < 4; side++) if(!full(used[side])) return side;
        return order[0];
    }

    @Override
    public float getPrefWidth(){
        return grid * Scl.scl(34f);
    }

    @Override
    public float getPrefHeight(){
        return grid * Scl.scl(34f);
    }

    /** Centre of a tile (grid coordinates, origin bottom left) in screen space. */
    float tx(float gx){
        return x + (gx + 0.5f) * ts;
    }

    float ty(float gy){
        return y + (gy + 0.5f) * ts;
    }

    /** Grid tile of slot k on a side, at distance d (1 = touching the machine) outwards. */
    float[] tile(int side, int slot, int d){
        int m = margin;
        return switch(side){
            case 0 -> new float[]{m + n - 1 + d, m + slot};
            case 1 -> new float[]{m + slot, m + n - 1 + d};
            case 2 -> new float[]{m - d, m + slot};
            default -> new float[]{m + slot, m - d};
        };
    }

    @Override
    public void draw(){
        validate();
        ts = Math.min(width, height) / grid;
        float t = Time.globalTime;
        float cycle = (t % 120f) / 120f;

        //floor
        Draw.color(Color.valueOf("1d1e24"));
        Fill.crect(x, y, width, height);
        Draw.color(Color.valueOf("26272f"));
        for(int i = 0; i <= grid; i++){
            Fill.crect(x + i * ts - 0.5f, y, 1f, height);
            Fill.crect(x, y + i * ts - 0.5f, width, 1f);
        }

        //belts and conduits
        for(Line l : lines){
            //flow direction on screen: outputs flow outwards, inputs towards the machine
            float rot = l.out ? l.side * 90f : (l.side * 90f + 180f) % 360f;
            for(int d = 1; d <= margin; d++){
                float[] g = tile(l.side, l.slot, d);
                if(l.items != null){
                    Draw.color();
                    Draw.rect(belt[(int)(t / 4f) % 4], tx(g[0]), ty(g[1]), ts, ts, rot);
                }else{
                    Draw.color();
                    if(conduitBottom.found()) Draw.rect(conduitBottom, tx(g[0]), ty(g[1]), ts, ts, rot);
                    //a dark bore with a narrower stream of the liquid in it
                    boolean across = l.side % 2 == 0;
                    Draw.color(Color.valueOf("2a2b33"));
                    Fill.crect(tx(g[0]) - ts * (across ? 0.5f : 0.25f), ty(g[1]) - ts * (across ? 0.25f : 0.5f), ts * (across ? 1f : 0.5f), ts * (across ? 0.5f : 1f));
                    Draw.color(l.liquid.color, 0.9f);
                    Fill.crect(tx(g[0]) - ts * (across ? 0.5f : 0.14f), ty(g[1]) - ts * (across ? 0.14f : 0.5f), ts * (across ? 1f : 0.28f), ts * (across ? 0.28f : 1f));
                    Draw.color();
                    if(conduitTop.found()) Draw.rect(conduitTop, tx(g[0]), ty(g[1]), ts, ts, rot);
                }
            }
            //what moves along it
            float dx = Angles.trnsx(rot, 1f), dy = Angles.trnsy(rot, 1f);
            float[] start = l.out ? tile(l.side, l.slot, 1) : tile(l.side, l.slot, margin);
            float sx = start[0] - dx * 0.5f, sy = start[1] - dy * 0.5f;
            if(l.items != null){
                float speed = 0.02f, spacing = 0.75f;
                for(float p = (t * speed) % spacing; p < margin; p += spacing){
                    int index = (int)Math.floor((t * speed - p) / spacing);
                    Item item = l.items.get(Math.floorMod(index, l.items.size));
                    Draw.color();
                    Draw.rect(item.fullIcon, tx(sx + dx * p), ty(sy + dy * p), ts * 0.55f, ts * 0.55f);
                }
            }else{
                Draw.color(Tmp.c1.set(l.liquid.color).lerp(Color.white, 0.5f), 0.8f);
                for(float p = (t * 0.03f) % 1f; p < margin; p += 1f){
                    Fill.square(tx(sx + dx * p), ty(sy + dy * p), ts * 0.1f, 45f);
                }
            }
        }

        //the machine, running
        float mx = tx(margin + (n - 1) / 2f), my = ty(margin + (n - 1) / 2f), size = n * ts;
        Draw.color();
        Draw.rect(block.fullIcon, mx, my, size, size);
        if(rotator != null) Draw.rect(rotator, mx, my, size, size, t * 2f);
        if(press != null){
            float down = Mathf.slope(Mathf.curve(cycle, 0.7f, 1f));
            Draw.color(Tmp.c1.set(Color.white).mul(1f - 0.18f * down));
            Draw.rect(press, mx, my, size * (1f - 0.07f * down), size * (1f - 0.07f * down));
            Draw.color();
        }
        if(glow != null){
            Draw.blend(Blending.additive);
            Draw.color(Pal.accent, 0.35f + Mathf.absin(t, 8f, 0.35f));
            Draw.rect(glow, mx, my, size, size);
            Draw.blend();
        }

        //rotating machines face up in the picture: mark the front
        if(block.rotate){
            float fy = my + size / 2f + ts * 0.15f;
            Draw.color(Pal.accent);
            Fill.tri(mx - ts * 0.25f, fy, mx + ts * 0.25f, fy, mx, fy + ts * 0.3f);
        }

        //heat: the heater facing the machine, and the input glow on the edge it feeds
        if(heaterSide >= 0 && heater != null){
            float[] a = tile(heaterSide, heaterSlot, 1), b = tile(heaterSide, heaterSlot + 1, 1);
            float hx = (tx(a[0]) + tx(b[0])) / 2f, hy = (ty(a[1]) + ty(b[1])) / 2f;
            //2x2: push it half a tile further out so it sits beside the machine
            hx += Angles.trnsx(heaterSide * 90f, ts * 0.5f);
            hy += Angles.trnsy(heaterSide * 90f, ts * 0.5f);
            Draw.color();
            Draw.rect(heater, hx, hy, ts * 2f, ts * 2f, (heaterSide * 90f + 180f) % 360f);
            if(heat != null){
                Draw.blend(Blending.additive);
                Draw.color(Color.valueOf("ff9a3c"), 0.5f + Mathf.absin(t, 10f, 0.3f));
                Draw.rect(heat, mx, my, size, size, heaterSide * 90f);
                Draw.blend();
            }
        }

        //power: a node in the corner and its laser to the machine
        if(power){
            float nx = tx(0.5f), ny = ty(grid - 1.5f);
            Draw.color(Pal.powerLight, 0.8f);
            Lines.stroke(Math.max(1f, ts * 0.12f));
            Lines.line(nx, ny, mx, my);
            Draw.color();
            Draw.rect(node, nx, ny, ts, ts);
        }
        Draw.reset();
    }
}
