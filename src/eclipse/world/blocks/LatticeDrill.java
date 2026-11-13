package eclipse.world.blocks;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * Lattice Drill (endgame): a drill core that, once built, grows a mining lattice over the connected patch of the ore
 * it stands on, {@link #growPerSecond} tiles a second up to {@link #maxTiles}. Every covered tile mines into the core,
 * which outputs everything. The lattice follows only the same ore and only free ground: a building placed on a covered
 * tile cuts it out, and two lattices never claim the same tile. Power use grows with the covered area.
 * <p>
 * Growth is a breadth-first spread from the core, so it is the same on every client; the save keeps the covered count
 * and the lattice regrows to it at once on load.
 * <p>
 * Optional coolant boosts mining and growth: the better the coolant (higher heat capacity), the bigger the boost
 * (water about 1.6x, cryofluid about 2.35x, liquid nitrogen about 2.65x). Corrosive liquids and gases are refused.
 * <p>
 * Looks: a faint tint over the covered patch, a glowing outline along its border, and drill heads on arms that swing
 * from covered tile to covered tile (one per {@link #tilesPerHead} tiles, at most {@link #maxHeads}); purely visual.
 */
public class LatticeDrill extends Block{
    public static final Stat latticeStat = new Stat("eclipse-lattice", StatCat.crafting),
        boostStat = new Stat("eclipse-lattice-boost", StatCat.optional);

    public int tier = 4;
    public int maxTiles = 200;
    public float growPerSecond = 3f;
    /** Ticks for one covered tile to mine one item: base + per point of ore hardness. */
    public float drillTime = 180f, hardnessTime = 25f;
    /** Power per tick: base, plus this much per covered tile. */
    public float basePower = 3f, powerPerTile = 0.05f;
    /** Coolant used while boosting (per tick), and how much boost one point of the coolant's heat capacity gives. */
    public float boostUse = 0.2f, boostPerHeatCapacity = 1.5f;
    public int tilesPerHead = 70, maxHeads = 3;
    public TextureRegion nodeRegion, headRegion;

    /** Lattice tiles claimed by any lattice drill (tile position -> drill building id), so two never overlap. */
    static final IntIntMap claims = new IntIntMap();

    public LatticeDrill(String name){
        super(name);
        update = true;
        solid = true;
        hasItems = true;
        hasPower = true;
        hasLiquids = true;
        itemCapacity = 100;
        liquidCapacity = 30f;
        group = BlockGroup.drills;
        ambientSound = mindustry.gen.Sounds.loopDrill;
        ambientSoundVolume = 0.08f;
        consumePowerDynamic((LatticeBuild b) -> basePower + b.covered.size * powerPerTile);
        consume(new ConsumeLiquidFilter(LatticeDrill::coolant, boostUse)).boost();
    }

    static{
        Events.on(WorldLoadEvent.class, e -> claims.clear());
    }

    /** Liquids that can boost a lattice: non-corrosive liquid coolants. */
    public static boolean coolant(Liquid l){
        return l.coolant && !l.gas && !(l instanceof eclipse.type.CorrosiveLiquid) && l.temperature <= 0.5f;
    }

    /** Speed multiplier when fully boosted with this liquid. */
    public float boostFor(@Nullable Liquid l){
        return l == null || !coolant(l) ? 1f : 1f + l.heatCapacity * boostPerHeatCapacity;
    }

    @Override
    public void load(){
        super.load();
        nodeRegion = Core.atlas.find(name + "-node");
        headRegion = Core.atlas.find(name + "-head");
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.tiles, StatValues.drillables(drillTime, hardnessTime, size * size, new ObjectFloatMap<>(), b -> b instanceof mindustry.world.blocks.environment.OreBlock ore
            && ore.itemDrop != null && ore.itemDrop.hardness <= tier));
        stats.add(latticeStat, Core.bundle.format("stat.eclipse-lattice-value", maxTiles, Strings.autoFixed(growPerSecond, 1), Strings.autoFixed(powerPerTile * 60f, 1)));
        StringBuilder boosts = new StringBuilder();
        for(Liquid l : content.liquids()){
            if(!coolant(l) || l.isHidden()) continue;
            if(boosts.length() > 0) boosts.append(", ");
            boosts.append(l.localizedName).append(" ").append(Strings.autoFixed(boostFor(l), 2)).append("x");
        }
        stats.add(boostStat, boosts.toString());
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("eclipse-lattice", (LatticeBuild e) -> new Bar(
            () -> Core.bundle.format("bar.eclipse-lattice", e.covered.size, e.ore == null ? "-" : e.ore.localizedName, Strings.autoFixed(e.rate(), 1)),
            () -> e.ore == null ? Pal.gray : e.ore.color,
            () -> e.covered.size / (float)maxTiles));
    }

    /** The ore a core placed here would mine: the most common minable ore under it, or null. */
    public @Nullable Item oreAt(int tx, int ty){
        ObjectIntMap<Item> count = new ObjectIntMap<>();
        Tile center = world.tile(tx, ty);
        if(center == null) return null;
        center.getLinkedTilesAs(this, t -> {
            Item drop = t.drop();
            if(drop != null && drop.hardness <= tier && t.overlay() != null && t.overlay().itemDrop == drop) count.increment(drop);
        });
        Item best = null;
        int bestCount = 0;
        for(ObjectIntMap.Entry<Item> e : count) if(e.value > bestCount){ best = e.key; bestCount = e.value; }
        return best;
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        return oreAt(tile.x, tile.y) != null;
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        Item ore = oreAt(x, y);
        drawPlaceText(ore == null ? Core.bundle.get("eclipse-lattice.no-ore") : Core.bundle.format("eclipse-lattice.place", ore.localizedName), x, y, ore != null);
    }

    /** A drill head (visual only): position, the covered tile it is heading for, spin, and time spent on its tile. */
    static class Head{
        float x, y, spin, work;
        int target = -1;

        Head(float x, float y){
            this.x = x;
            this.y = y;
        }
    }

    public class LatticeBuild extends Building{
        public @Nullable Item ore;
        /** Covered tiles in growth order (packed positions), the core's own ore tiles first. */
        public final IntSeq covered = new IntSeq();
        final IntSet coveredSet = new IntSet();
        /** Edges of the covered area (x1, y1, x2, y2 in world units), rebuilt when the area changes. */
        final FloatSeq outline = new FloatSeq();
        boolean outlineDirty = true;
        final Seq<Head> heads = new Seq<>();
        /** Covered count to regrow to after loading. */
        int target = -1;
        float growTimer, checkTimer, progress;

        public float rate(){
            if(ore == null) return 0f;
            return covered.size * 60f / (drillTime + hardnessTime * ore.hardness) * speed();
        }

        /** Efficiency times the coolant boost. */
        public float speed(){
            return efficiency * Mathf.lerp(1f, boostFor(liquids.current()), optionalEfficiency);
        }

        @Override
        public void created(){
            super.created();
            ore = oreAt(tile.x, tile.y);
        }

        boolean free(Tile t){
            if(t == null || ore == null || t.drop() != ore || t.overlay() == null || t.overlay().itemDrop != ore) return false;
            if(t.build != null && t.build != this) return false;
            if(t.build == null && t.solid()) return false;
            int owner = claims.get(t.pos(), -1);
            return owner == -1 || owner == id;
        }

        /** The next tile the lattice would cover: the nearest free ore tile next to the lattice, breadth-first from the core. */
        @Nullable Tile nextTile(){
            IntSet seen = new IntSet();
            arc.struct.Queue<Tile> queue = new arc.struct.Queue<>();
            tile.getLinkedTiles(t -> {
                if(free(t) && seen.add(t.pos())) queue.addLast(t);
            });
            while(!queue.isEmpty()){
                Tile t = queue.removeFirst();
                if(!coveredSet.contains(t.pos())) return t;
                for(int d = 0; d < 4; d++){
                    Tile n = t.nearby(d);
                    if(n != null && free(n) && seen.add(n.pos())) queue.addLast(n);
                }
            }
            return null;
        }

        void cover(Tile t){
            covered.add(t.pos());
            coveredSet.add(t.pos());
            claims.put(t.pos(), id);
            outlineDirty = true;
        }

        /** Drops covered tiles that are no longer free (a building was placed, the ore is gone). */
        void prune(){
            for(int i = covered.size - 1; i >= 0; i--){
                int pos = covered.get(i);
                Tile t = world.tile(pos);
                if(!free(t)){
                    covered.removeIndex(i);
                    coveredSet.remove(pos);
                    outlineDirty = true;
                    if(claims.get(pos, -1) == id) claims.remove(pos);
                }
            }
        }

        @Override
        public void updateTile(){
            if(ore == null) ore = oreAt(tile.x, tile.y);
            //after loading: regrow at once to the saved size
            if(target >= 0){
                while(covered.size < target){
                    Tile t = nextTile();
                    if(t == null) break;
                    cover(t);
                }
                target = -1;
            }
            if((checkTimer -= Time.delta) <= 0f){
                checkTimer = 60f;
                prune();
            }
            float speed = speed();
            if(covered.size < maxTiles && speed > 0f){
                growTimer += Time.delta * speed;
                float perTile = 60f / growPerSecond;
                while(growTimer >= perTile && covered.size < maxTiles){
                    growTimer -= perTile;
                    Tile t = nextTile();
                    if(t == null){
                        growTimer = 0f;
                        break;
                    }
                    cover(t);
                }
            }

            if(ore != null && items.total() < itemCapacity){
                progress += delta() * speed * covered.size / (drillTime + hardnessTime * ore.hardness);
                int whole = (int)progress;
                if(whole > 0){
                    int added = Math.min(whole, itemCapacity - items.total());
                    items.add(ore, added);
                    progress -= whole;
                }
            }
            if(ore != null) dump(ore);
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return false;
        }

        @Override
        public void onRemoved(){
            for(int i = 0; i < covered.size; i++) if(claims.get(covered.get(i), -1) == id) claims.remove(covered.get(i));
            covered.clear();
            coveredSet.clear();
            outlineDirty = true;
            super.onRemoved();
        }

        void rebuildOutline(){
            outline.clear();
            float h = tilesize / 2f;
            for(int i = 0; i < covered.size; i++){
                int pos = covered.get(i);
                int x = Point2.x(pos), y = Point2.y(pos);
                float wx = x * tilesize, wy = y * tilesize;
                if(!coveredSet.contains(Point2.pack(x + 1, y))) outline.addAll(wx + h, wy - h, wx + h, wy + h);
                if(!coveredSet.contains(Point2.pack(x - 1, y))) outline.addAll(wx - h, wy - h, wx - h, wy + h);
                if(!coveredSet.contains(Point2.pack(x, y + 1))) outline.addAll(wx - h, wy + h, wx + h, wy + h);
                if(!coveredSet.contains(Point2.pack(x, y - 1))) outline.addAll(wx - h, wy - h, wx + h, wy - h);
            }
            outlineDirty = false;
        }

        /** Moves the drill heads: each swings to a covered tile, works it for a moment, then picks another. */
        void updateHeads(){
            int want = covered.isEmpty() ? 0 : Math.min(maxHeads, 1 + covered.size / tilesPerHead);
            while(heads.size < want) heads.add(new Head(x, y));
            if(heads.size > want) heads.truncate(want);
            float speed = speed();
            for(Head h : heads){
                h.spin += Time.delta * (2f + 14f * speed);
                if(h.target < 0 || h.target >= covered.size){
                    h.target = Mathf.random(covered.size - 1);
                    h.work = 0f;
                    continue;
                }
                int pos = covered.get(h.target);
                float tx = Point2.x(pos) * tilesize, ty = Point2.y(pos) * tilesize;
                if(Mathf.within(h.x, h.y, tx, ty, 1.5f)){
                    h.work += Time.delta * Math.max(speed, 0.2f);
                    if(speed > 0f && Mathf.chanceDelta(0.08f * speed)) Fx.mineSmall.at(tx + Mathf.range(3f), ty + Mathf.range(3f), 0f, ore.color);
                    if(h.work > 50f){
                        h.target = Mathf.random(covered.size - 1);
                        h.work = 0f;
                    }
                }else{
                    float step = Math.min(Mathf.dst(h.x, h.y, tx, ty), Time.delta * (0.6f + 1.8f * speed));
                    float ang = Angles.angle(h.x, h.y, tx, ty);
                    h.x += Angles.trnsx(ang, step);
                    h.y += Angles.trnsy(ang, step);
                }
            }
        }

        @Override
        public void draw(){
            if(ore != null && !covered.isEmpty()){
                if(outlineDirty) rebuildOutline();
                Color tint = Tmp.c1.set(ore.color).lerp(Color.white, 0.35f);
                float pulse = Mathf.absin(Time.time, 12f, 1f);

                //a faint tint over the covered patch, with a sparse pattern of lattice nodes
                Draw.z(Layer.blockUnder);
                for(int i = 0; i < covered.size; i++){
                    int pos = covered.get(i);
                    int px = Point2.x(pos), py = Point2.y(pos);
                    float wx = px * tilesize, wy = py * tilesize;
                    Draw.color(tint, 0.12f + 0.06f * Mathf.absin(Time.time - i * 4f, 14f, 1f) * efficiency);
                    Fill.square(wx, wy, tilesize / 2f);
                    if(nodeRegion.found() && (px + py) % 3 == 0){
                        Draw.color(tint, 0.3f);
                        Draw.rect(nodeRegion, wx, wy);
                    }
                }

                //a glowing outline along the patch border: dark halo, bright core line
                Draw.color(Tmp.c2.set(tint).mul(0.55f), 0.85f);
                Lines.stroke(2.6f);
                for(int i = 0; i < outline.size; i += 4) Lines.line(outline.items[i], outline.items[i + 1], outline.items[i + 2], outline.items[i + 3], false);
                Draw.color(tint, 0.7f + 0.3f * pulse * efficiency);
                Lines.stroke(1f);
                for(int i = 0; i < outline.size; i += 4) Lines.line(outline.items[i], outline.items[i + 1], outline.items[i + 2], outline.items[i + 3], false);

                //drill heads on arms swinging out from the core
                if(!state.isPaused()) updateHeads();
                float edge = size * tilesize / 2f - 3f;
                for(Head h : heads){
                    float ang = Angles.angle(x, y, h.x, h.y);
                    float sx = x + Angles.trnsx(ang, edge), sy = y + Angles.trnsy(ang, edge);
                    if(Mathf.dst(x, y, h.x, h.y) > edge + 2f){
                        Draw.z(Layer.blockOver);
                        Draw.color(Pal.darkerMetal);
                        Lines.stroke(3.5f);
                        Lines.line(sx, sy, h.x, h.y);
                        Draw.color(tint, 0.85f);
                        Lines.stroke(1.2f);
                        Lines.line(sx, sy, h.x, h.y);
                    }
                    Draw.z(Layer.blockOver + 0.1f);
                    Draw.color();
                    if(headRegion.found()) Draw.rect(headRegion, h.x, h.y, h.spin);
                    Draw.color(tint, 0.5f + 0.5f * pulse);
                    Fill.circle(h.x, h.y, 1.3f);
                }
                Draw.reset();
                Draw.z(Layer.block);
            }
            super.draw();
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(progress);
            write.i(covered.size);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1){
                progress = read.f();
                target = read.i();
            }
        }
    }
}
