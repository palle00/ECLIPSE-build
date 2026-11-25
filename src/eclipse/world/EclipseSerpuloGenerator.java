package eclipse.world;

import arc.math.*;
import arc.struct.*;
import arc.util.noise.*;
import mindustry.content.*;
import mindustry.game.*;
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
            if(o == oreNickel || o == oreChromium || o == oreCobalt || o == oreTungsten || o == oreMolybdenum || o == orePlatinum || o == oreAetherite || o == oreBauxite) return;
            if(o == Blocks.oreCopper || o == Blocks.oreLead || o == Blocks.oreTitanium || o == Blocks.oreCoal || o == Blocks.oreThorium || o == Blocks.oreScrap) vanillaOre = true;
        }
        if(vanillaOre) addOres(world.tiles, seed, true);
    }

    /**
     * Scatters ECLIPSE ore veins on bare ground that has no vanilla ore.
     * Rarer metals need more distance from the player's core and appear in smaller veins.
     */
    public static void addOres(Tiles tiles, int seed){
        addOres(tiles, seed, false);
    }

    /**
     * As {@link #addOres(Tiles, int)}; with fair on, a map with cores of two or more teams (one the game lets you play as
     * PvP) is treated fairly: rarity grows with the distance to the nearest core of any team, so every base gets the same
     * rings, and on a mirror-symmetric map the veins are mirrored too, so both sides get the same ore.
     */
    public static void addOres(Tiles tiles, int seed, boolean fair){
        addOres(tiles, seed, fair, eclipseOres());
    }

    /** ECLIPSE's ores: ore, noise scale, threshold near the core, threshold at the far edge, minimum distance (0-1 of map). */
    public static Object[][] eclipseOres(){
        return new Object[][]{
            {oreNickel, 26f, 0.84f, 0.79f, 0.00f},
            {oreChromium, 24f, 0.85f, 0.80f, 0.10f},
            {oreCobalt, 22f, 0.86f, 0.81f, 0.20f},
            {oreTungsten, 20f, 0.87f, 0.82f, 0.35f},
            {oreMolybdenum, 22f, 0.86f, 0.81f, 0.15f},
            {orePlatinum, 16f, 0.89f, 0.84f, 0.45f},
            {oreAetherite, 14f, 0.91f, 0.86f, 0.55f},
            {oreBauxite, 25f, 0.845f, 0.795f, 0.05f}, //aluminium is a mid-game staple: fairly common
        };
    }

    /** As {@link #addOres(Tiles, int, boolean)}, with a table of ores in the {@link #eclipseOres()} format. */
    public static void addOres(Tiles tiles, int seed, boolean fair, Object[][] ores){

        Tile core = null;
        Seq<Tile> cores = new Seq<>();
        ObjectSet<Team> teams = new ObjectSet<>();
        for(Tile t : tiles){
            if(t.build instanceof CoreBuild c && c.tile == t && c.team != Team.derelict){
                if(core == null) core = t;
                cores.add(t);
                teams.add(c.team);
            }
        }
        float cx = core == null ? tiles.width / 2f : core.x, cy = core == null ? tiles.height / 2f : core.y;
        float maxDst = Mathf.dst(tiles.width, tiles.height) / 2f;
        //fair: distance to the nearest core, scaled by the largest such distance on the map; noise read at a mirrored tile
        boolean several = fair && teams.size > 1;
        float[] nearest = null;
        int[] partner = null;
        if(several){
            nearest = new float[tiles.width * tiles.height];
            float far = 1f;
            for(Tile t : tiles){
                float d = Float.MAX_VALUE;
                for(Tile c : cores) d = Math.min(d, Mathf.dst(t.x, t.y, c.x, c.y));
                nearest[t.x + t.y * tiles.width] = d;
                far = Math.max(far, d);
            }
            maxDst = far;
            partner = mirror(tiles, cores);
        }

        for(int i = 0; i < ores.length; i++){
            Block ore = (Block)ores[i][0];
            float scale = (float)ores[i][1], near = (float)ores[i][2], far = (float)ores[i][3], minDst = (float)ores[i][4];
            int oreSeed = seed + 9173 * (i + 1);

            for(Tile t : tiles){
                if(t.block() != Blocks.air || t.overlay() != Blocks.air || !t.floor().hasSurface() || t.floor().isLiquid) continue;
                int ti = t.x + t.y * tiles.width;
                float dst = Mathf.clamp((several ? nearest[ti] : Mathf.dst(t.x, t.y, cx, cy)) / maxDst);
                if(dst < minDst) continue;
                float threshold = Mathf.lerp(near, far, dst);
                //on a mirrored map both tiles of a pair read the noise at the same one of them
                Tile at = partner == null || partner[ti] >= ti ? t : tiles.get(partner[ti] % tiles.width, partner[ti] / tiles.width);
                if(Simplex.noise2d(oreSeed, 2, 0.5, 1f / scale, at.x, at.y) > threshold){
                    t.setOverlay(ore);
                }
            }
        }
    }

    /**
     * The map's mirror symmetry, if it has one: for each tile (by array index) the index of its mirror image, or null when
     * no mirroring (left-right, top-bottom, a half turn, or either diagonal on square maps) maps at least 95% of the
     * floors and walls onto equal ones and every core onto a core.
     */
    static int[] mirror(Tiles tiles, Seq<Tile> cores){
        int w = tiles.width, h = tiles.height, best = -1;
        float bestMatch = 0.95f;
        for(int k = 0; k < 5; k++){
            if(k >= 3 && w != h) break;
            int same = 0;
            for(Tile t : tiles){
                Tile m = tiles.get(mx(k, t.x, t.y, w, h), my(k, t.x, t.y, w, h));
                if(m.floor() == t.floor() && m.block() == t.block()) same++;
            }
            float match = same / (float)(w * h);
            if(match < bestMatch) continue;
            boolean coresMatch = true;
            for(Tile c : cores){
                int x = mx(k, c.x, c.y, w, h), y = my(k, c.x, c.y, w, h);
                if(!cores.contains(o -> Math.abs(o.x - x) <= 2 && Math.abs(o.y - y) <= 2)) coresMatch = false;
            }
            if(coresMatch){
                best = k;
                bestMatch = match;
            }
        }
        if(best < 0) return null;
        int[] out = new int[w * h];
        for(Tile t : tiles) out[t.x + t.y * w] = mx(best, t.x, t.y, w, h) + my(best, t.x, t.y, w, h) * w;
        return out;
    }

    /** Mirror k of (x, y): 0 left-right, 1 top-bottom, 2 half turn, 3 main diagonal, 4 other diagonal. */
    static int mx(int k, int x, int y, int w, int h){
        return k == 0 || k == 2 ? w - 1 - x : k == 1 ? x : k == 3 ? y : h - 1 - y;
    }

    static int my(int k, int x, int y, int w, int h){
        return k == 1 || k == 2 ? h - 1 - y : k == 0 ? y : k == 3 ? x : w - 1 - x;
    }
}
