package eclipse.world.blocks;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import eclipse.augment.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * An ECLIPSE core: a vanilla core (placed over a smaller core to upgrade it, more storage, a better core unit, a higher
 * unit cap) with a built-in power node. Every few seconds it links itself with laser links to the power nodes and
 * batteries of its team within {@link #laserRange} tiles (up to {@link #maxNodes}), so the grids around the core join
 * into one. Players can also link power nodes to it by hand. Like vanilla cores it refuses augmentation modules.
 */
public class EclipseCore extends CoreBlock{
    /** "-glow": the eclipse emblem's corona, pulsing slowly in gold. */
    public arc.graphics.g2d.TextureRegion glowRegion;
    /** Power link reach in tiles, and how many links the core makes by itself. */
    public float laserRange = 12f;
    public int maxNodes = 10;
    /** Ticks between looks for new power nodes and batteries to link. */
    public float linkInterval = 120f;

    public EclipseCore(String name){
        super(name);
        hasPower = true;
        connectedPower = true;
        //a link toggles like a power node's (server-validated, synced)
        config(Integer.class, (EclipseCoreBuild b, Integer pos) -> b.toggleLink(pos));
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.powerRange, laserRange, StatUnit.blocks);
        stats.add(Stat.powerConnections, maxNodes, StatUnit.none);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, laserRange * tilesize, Pal.placing);
    }

    /** What the core links to by itself: power nodes and batteries, the hubs of a grid. */
    public static boolean hub(Building b){
        return b.block instanceof PowerNode || (b.block.consPower != null && b.block.consPower.buffered);
    }

    @Override
    public void load(){
        super.load();
        glowRegion = arc.Core.atlas.find(name + "-glow");
    }

    public class EclipseCoreBuild extends CoreBuild{
        float linkTimer;

        public boolean linkable(Building other){
            return other != null && other != this && other.team == team && other.power != null && other.isValid()
                && !PowerNode.insulated(this, other) && overlaps(x, y, other.tile, laserRange * tilesize);
        }

        boolean overlaps(float srcx, float srcy, mindustry.world.Tile other, float range){
            return Intersector.overlaps(Tmp.cr1.set(srcx, srcy, range), other.getHitbox(Tmp.r1));
        }

        /** Adds or removes a laser link, the way a power node does. */
        public void toggleLink(int pos){
            Building other = world.build(pos);
            if(power.links.contains(pos)){
                power.links.removeValue(pos);
                if(other != null && other.power != null) other.power.links.removeValue(pos());
                PowerGraph mine = new PowerGraph();
                mine.reflow(this);
                if(other != null && other.power != null && other.power.graph != mine){
                    PowerGraph theirs = new PowerGraph();
                    theirs.reflow(other);
                }
            }else if(linkable(other) && power.links.size < maxNodes){
                power.links.addUnique(other.pos());
                other.power.links.addUnique(pos());
                power.graph.addGraph(other.power.graph);
            }
        }

        @Override
        public void updateTile(){
            super.updateTile();
            if(net.client() || (linkTimer += delta()) < linkInterval) return;
            linkTimer = 0f;
            //link the nearest hubs that are on another grid, while links are free
            indexer.eachBlock(this, laserRange * tilesize + size * tilesize / 2f, other -> hub(other) && other.power.graph != power.graph && linkable(other),
                other -> {
                    if(power.links.size < maxNodes && other.power.graph != power.graph) configure(other.pos());
                });
        }

        @Override
        public void draw(){
            super.draw();
            if(glowRegion != null && glowRegion.found()){
                Draw.z(Layer.blockAdditive);
                Draw.blend(arc.graphics.Blending.additive);
                Draw.color(arc.graphics.Color.valueOf("ffb648"), 0.45f + Mathf.absin(Time.time, 30f, 0.4f));
                Draw.rect(glowRegion, x, y);
                Draw.blend();
                Draw.reset();
            }
            if(power == null || power.links.isEmpty()) return;
            //power nodes draw their own laser to the core; the core draws the rest (batteries)
            PowerNode node = (PowerNode)Blocks.powerNode;
            Draw.z(Layer.power);
            Draw.color(node.laserColor1, node.laserColor2, (1f - power.graph.getSatisfaction()) * 0.86f + Mathf.absin(3f, 0.1f));
            Draw.alpha(renderer.laserOpacity);
            for(int i = 0; i < power.links.size; i++){
                Building other = world.build(power.links.get(i));
                if(other == null || other.block instanceof PowerNode) continue;
                node.drawLaser(x, y, other.x, other.y, size, other.block.size);
            }
            Draw.reset();
        }

        @Override
        public void drawSelect(){
            super.drawSelect();
            Drawf.dashCircle(x, y, laserRange * tilesize, Pal.accent);
        }

        //augmentation modules are machine parts, not building materials (see CoreRules)
        @Override
        public boolean acceptItem(Building source, Item item){
            return !(item instanceof AugmentModule) && super.acceptItem(source, item);
        }

        @Override
        public int acceptStack(Item item, int amount, Teamc source){
            return item instanceof AugmentModule ? 0 : super.acceptStack(item, amount, source);
        }
    }
}
