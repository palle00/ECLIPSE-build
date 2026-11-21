package eclipse.world;

import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.maps.planet.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.CoreBlock.*;

/**
 * The generator of ECLIPSE's faction planets: Serpulo's generator (with ECLIPSE ores), reshaped per world.
 * - Sea level: Serpulo's terrain height is (pow(noise * heightScl, 2.3) + waterOffset) / (1 + waterOffset) and the lowest
 *   heights are water; Serpulo uses 1.01 and 0.04. Lower values make an ocean world.
 * - A spawn point out at sea (for the Tide's submarines), if asked for.
 * - A floor and wall swap table, so the same layouts become spore jungle or scorched industrial ground.
 * - A tint for the planet seen from space.
 */
public class FactionPlanetGenerator extends EclipseSerpuloGenerator{
    public final boolean seaSpawn;
    public final ObjectMap<Block, Block> swap;
    public final Color tint;
    public final float tintAmount;

    public FactionPlanetGenerator(float heightScl, float waterOffset, boolean seaSpawn, ObjectMap<Block, Block> swap, Color tint, float tintAmount){
        this.seaSpawn = seaSpawn;
        this.swap = swap;
        this.tint = tint;
        this.tintAmount = tintAmount;
        //package-private in Serpulo's generator
        Reflect.set(SerpuloPlanetGenerator.class, this, "heightScl", heightScl);
        Reflect.set(SerpuloPlanetGenerator.class, this, "waterOffset", waterOffset);
    }

    @Override
    protected void generate(){
        super.generate();
        if(swap.size > 0){
            for(Tile t : tiles){
                Block floor = swap.get(t.floor());
                if(floor != null) t.setFloor(floor.asFloor());
                Block wall = swap.get(t.block());
                if(wall != null && t.build == null) t.setBlock(wall);
            }
        }
        if(seaSpawn) addSeaSpawn(tiles);
    }

    @Override
    public void getColor(Vec3 position, Color out){
        super.getColor(position, out);
        if(tintAmount > 0f) out.lerp(tint, tintAmount);
    }

    /** Puts a spawn point on the deep-water tile farthest from the player's core, if there is deep water. */
    public static void addSeaSpawn(Tiles tiles){
        Tile core = null;
        for(Tile t : tiles){
            if(t.build instanceof CoreBuild){
                core = t;
                break;
            }
        }
        if(core == null) return;
        Tile best = null;
        float bestDst = -1f;
        for(Tile t : tiles){
            if(t.floor() != Blocks.deepwater || t.block() != Blocks.air || t.overlay() != Blocks.air) continue;
            //away from the map edge, so units can leave it
            if(t.x < 6 || t.y < 6 || t.x > tiles.width - 7 || t.y > tiles.height - 7) continue;
            float dst = Mathf.dst2(t.x, t.y, core.x, core.y);
            if(dst > bestDst){
                bestDst = dst;
                best = t;
            }
        }
        if(best != null) best.setOverlay(Blocks.spawn);
    }
}
