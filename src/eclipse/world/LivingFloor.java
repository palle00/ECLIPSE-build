package eclipse.world;

import arc.graphics.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

/**
 * A floor that gives off particles now and then: bubbles from the deep ocean, spores from brood moss, embers from
 * scorched rock. Floors can't animate their texture, so this is their moving part. Only one tile in {@link #every}
 * emits (picked by position, so it stays put), and only while nothing is built on it; the game runs this for floor
 * tiles that ask for it, and effects off screen are skipped by the game.
 */
public class LivingFloor extends Floor{
    public Effect effect = Fx.none;
    public Color effectColor = Color.white;
    /** Average ticks between two effects of one emitting tile. */
    public float effectSpacing = 90f;
    public int every = 8;

    public LivingFloor(String name){
        super(name);
    }

    public LivingFloor(String name, int variants){
        super(name, variants);
    }

    @Override
    public boolean updateRender(Tile tile){
        return effect != Fx.none && Mathf.randomSeed(tile.pos(), 0, every - 1) == 0;
    }

    @Override
    public void renderUpdate(UpdateRenderState state){
        Tile tile = state.tile;
        if(tile.block() != Blocks.air) return;
        if((state.data += Time.delta) >= effectSpacing){
            //a random wait so neighbouring emitters drift out of step
            state.data = Mathf.random(-effectSpacing * 0.5f, 0f);
            effect.at(tile.worldx() + Mathf.range(3f), tile.worldy() + Mathf.range(3f), effectColor);
        }
    }
}
