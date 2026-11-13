package eclipse.world;

import arc.math.*;
import arc.util.noise.*;
import mindustry.content.*;
import mindustry.maps.planet.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static eclipse.content.blocks.EnvironmentBlocks.*;
import static mindustry.Vars.*;

/** Vanilla Serpulo generation, plus an extra pass that places ECLIPSE ores on newly generated sectors. */
public class EclipseSerpuloGenerator extends SerpuloPlanetGenerator{

    @Override
    protected void generate(){
        super.generate();
        addOres(tiles, seed + sector.id);
    }

    /**
     * Custom games: premade Serpulo maps have fixed ores, so ECLIPSE ores are added when such a map loads.
     * Only maps that have vanilla ores and no ECLIPSE ores yet: real playable maps, not test or editor worlds,
     * and a save that already got them is left alone. Campaign sectors use the generator instead.
     */
    public static void addToCustomMap(){
        if(net.client() || state.rules.sector != null || state.rules.editor || state.rules.planet != Planets.serpulo) return;
        boolean vanillaOre = false;
        int seed = world.width() * 7919 + world.height(); //state.map is not set yet; derive a per-map seed from its layout
        for(Tile t : world.tiles){
            Block o = t.overlay();
            seed = seed * 31 + o.id;
            if(o == oreNickel || o == oreChromium || o == oreCobalt || o == oreTungsten || o == oreMolybdenum || o == orePlatinum || o == oreAetherite) return;
            if(o == Blocks.oreCopper || o == Blocks.oreLead || o == Blocks.oreTitanium || o == Blocks.oreCoal || o == Blocks.oreThorium || o == Blocks.oreScrap) vanillaOre = true;
        }
        if(vanillaOre) addOres(world.tiles, seed);
    }

    /**
     * Scatters ECLIPSE ore veins on bare ground that has no vanilla ore.
     * Rarer metals need more distance from the player's core and appear in smaller veins.
     */
    public static void addOres(Tiles tiles, int seed){
        // ore, noise scale, threshold near the core, threshold at the far edge, minimum distance (0-1 of map)
        Object[][] ores = {
            {oreNickel, 26f, 0.84f, 0.79f, 0.00f},
            {oreChromium, 24f, 0.85f, 0.80f, 0.10f},
            {oreCobalt, 22f, 0.86f, 0.81f, 0.20f},
            {oreTungsten, 20f, 0.87f, 0.82f, 0.35f},
            {oreMolybdenum, 22f, 0.86f, 0.81f, 0.15f},
            {orePlatinum, 16f, 0.89f, 0.84f, 0.45f},
            {oreAetherite, 14f, 0.91f, 0.86f, 0.55f},
        };

        Tile core = null;
        for(Tile t : tiles){
            if(t.build instanceof CoreBuild){
                core = t;
                break;
            }
        }
        float cx = core == null ? tiles.width / 2f : core.x, cy = core == null ? tiles.height / 2f : core.y;
        float maxDst = Mathf.dst(tiles.width, tiles.height) / 2f;

        for(int i = 0; i < ores.length; i++){
            Block ore = (Block)ores[i][0];
            float scale = (float)ores[i][1], near = (float)ores[i][2], far = (float)ores[i][3], minDst = (float)ores[i][4];
            int oreSeed = seed + 9173 * (i + 1);

            for(Tile t : tiles){
                if(t.block() != Blocks.air || t.overlay() != Blocks.air || !t.floor().hasSurface() || t.floor().isLiquid) continue;
                float dst = Mathf.clamp(Mathf.dst(t.x, t.y, cx, cy) / maxDst);
                if(dst < minDst) continue;
                float threshold = Mathf.lerp(near, far, dst);
                if(Simplex.noise2d(oreSeed, 2, 0.5, 1f / scale, t.x, t.y) > threshold){
                    t.setOverlay(ore);
                }
            }
        }
    }
}
