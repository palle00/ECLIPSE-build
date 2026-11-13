package eclipse.exploration;

import mindustry.content.*;
import mindustry.world.meta.*;

/**
 * Environment effects tied to vanilla Serpulo floors, so they work on every map (no new planet):
 * - hot rock / magma rock (vanilla heat attribute): the Geothermal Heat Tap makes free heat;
 * - ice and snow ("eclipse-cold"): the Cooling Tower and Cryogenic Condenser run faster;
 * - spore moss and moss (vanilla spores attribute): the Biomass Cultivator grows faster.
 * Must load before the blocks that use {@link #cold}.
 */
public class Biomes{
    public static Attribute cold;

    public static void load(){
        cold = Attribute.add("eclipse-cold");
        Blocks.ice.attributes.set(cold, 0.4f);
        Blocks.iceSnow.attributes.set(cold, 0.3f);
        Blocks.snow.attributes.set(cold, 0.25f);
    }
}
