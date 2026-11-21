package eclipse.ui;

import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.type.*;
import mindustry.world.*;

/**
 * A build example for the wiki: real blocks on a tile grid. The same description draws the animated picture
 * ({@link BuildDiagram}), labels every belt with its rate, builds itself in steps, and becomes the schematic that
 * "Build this" puts on the player's cursor, so the picture, the numbers and the schematic can never disagree.
 * Coordinates are the game's: tiles, x to the right, y up; a part sits at its building tile (the centre tile of an odd
 * size, the lower left of the middle for an even one), exactly as a schematic stores it.
 */
public class BuildLayout{
    /** Shown above the picture ("Minimal: 1 machine", "Full line: 3 furnaces, 1 freezer"). */
    public String title = "";
    public int w, h;
    public final Seq<Part> parts = new Seq<>();
    public final Seq<Flow> flows = new Seq<>();
    public final Seq<Label> labels = new Seq<>();
    /** One sentence per build step; parts, flows and labels carry the step they appear in (1-based). */
    public final Seq<String> steps = new Seq<>();
    /** Where things enter and leave the build (outside its bounds): sources and sinks for the tests, arrows in the picture. */
    public final Seq<Port> ports = new Seq<>();
    /** What it makes at full speed, per second (the main product), and the totals shown beside the picture. */
    public @Nullable UnlockableContent product;
    public float productRate;
    public final Seq<String[]> totals = new Seq<>();
    /** The recipe configured into the machines it shows (for the page's recipe buttons). */
    public @Nullable Object recipe;

    /** A building in the layout. */
    public static class Part{
        public Block block;
        public int x, y, rotation, step = 1;
        public @Nullable Object config;
        /** What the hover box says; the first line is the title. */
        public String info = "";
        /** The liquid a conduit carries (drawn flowing in the picture). */
        public @Nullable Liquid liquid;

        public int minX(){ return x - (block.size - 1) / 2; }
        public int minY(){ return y - (block.size - 1) / 2; }
        public boolean covers(int tx, int ty){ return tx >= minX() && ty >= minY() && tx < minX() + block.size && ty < minY() + block.size; }
    }

    /** Items riding a path: one item every {@code interval} seconds, cycling through {@code items}. Points are tile centres. */
    public static class Flow{
        public Item[] items;
        public float[] path;
        /** Optional: item i takes paths[(i / perPath) % paths.length] (one router line feeding several machines in turn). */
        public @Nullable float[][] paths;
        public int perPath = 1;
        public float interval, offset;
        public int step = 1;

        public float[] pathOf(int i){
            return paths == null ? path : paths[(i / perPath) % paths.length];
        }
    }

    public static class Label{
        public float x, y;
        public String text;
        public int step = 1;
        /** 0 rate (blue), 1 heat (orange), 2 power (gold). */
        public int kind;
        /** -1 text ends at x, 0 centred, 1 starts at x. */
        public int align = 1;
    }

    /** A source or sink just outside the layout: the tile and the direction it feeds or takes from. */
    public static class Port{
        public enum Kind{items, liquid, output, liquidOutput, excess, power}
        public Kind kind;
        public int x, y;
        public Item[] items = {};
        public @Nullable Liquid liquid;
    }

    public Part add(Block block, int x, int y, int rotation, @Nullable Object config, int step, String info){
        Part p = new Part();
        p.block = block;
        p.x = x;
        p.y = y;
        p.rotation = rotation;
        p.config = config;
        p.step = step;
        p.info = info;
        parts.add(p);
        return p;
    }

    public @Nullable Part at(int tx, int ty){
        for(int i = parts.size - 1; i >= 0; i--) if(parts.get(i).covers(tx, ty)) return parts.get(i);
        return null;
    }

    public Label label(float x, float y, String text, int kind, int align, int step){
        Label l = new Label();
        l.x = x;
        l.y = y;
        l.text = text;
        l.kind = kind;
        l.align = align;
        l.step = step;
        labels.add(l);
        return l;
    }

    public Port port(Port.Kind kind, int x, int y){
        Port p = new Port();
        p.kind = kind;
        p.x = x;
        p.y = y;
        ports.add(p);
        return p;
    }

    /** Every block it takes, with counts, in the order they first appear. */
    public OrderedMap<Block, Integer> bill(){
        OrderedMap<Block, Integer> out = new OrderedMap<>();
        for(Part p : parts) out.put(p.block, out.get(p.block, 0) + 1);
        return out;
    }

    /**
     * The layout as a schematic: the same blocks, rotations and configs (universal machines carry their recipe, power
     * nodes their links). Power nodes come last, so their links find the buildings already placed.
     */
    /** The lower left corner of everything placed: layout tile (x0, y0) is schematic tile (0, 0). */
    public Point2 origin(){
        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE;
        for(Part p : parts){
            x0 = Math.min(x0, p.minX());
            y0 = Math.min(y0, p.minY());
        }
        return new Point2(x0, y0);
    }

    public Schematic schematic(){
        Seq<Schematic.Stile> tiles = new Seq<>();
        Seq<Part> ordered = parts.copy().sort(p -> p.block instanceof mindustry.world.blocks.power.PowerNode ? 1 : 0);
        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE;
        for(Part p : parts){
            x0 = Math.min(x0, p.minX());
            y0 = Math.min(y0, p.minY());
            x1 = Math.max(x1, p.minX() + p.block.size - 1);
            y1 = Math.max(y1, p.minY() + p.block.size - 1);
        }
        for(Part p : ordered){
            Object config = p.config instanceof Point2[] links ? copy(links) : p.config;
            tiles.add(new Schematic.Stile(p.block, p.x - x0, p.y - y0, config, (byte)p.rotation));
        }
        StringMap tags = new StringMap();
        tags.put("name", title);
        return new Schematic(tiles, tags, x1 - x0 + 1, y1 - y0 + 1);
    }

    static Point2[] copy(Point2[] links){
        Point2[] out = new Point2[links.length];
        for(int i = 0; i < links.length; i++) out[i] = links[i].cpy();
        return out;
    }

    /** Power node links from a node to these parts, relative to the node (the node's own config format). */
    public static Point2[] links(Part node, Seq<Part> targets){
        Point2[] out = new Point2[targets.size];
        for(int i = 0; i < targets.size; i++) out[i] = new Point2(targets.get(i).x - node.x, targets.get(i).y - node.y);
        return out;
    }
}
