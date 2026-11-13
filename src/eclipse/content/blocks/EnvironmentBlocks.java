package eclipse.content.blocks;

import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static eclipse.content.EclipseItems.*;

/** Ores. Generated on new Serpulo sectors (see EclipseSerpuloGenerator) and placeable in the map editor. */
public class EnvironmentBlocks{
    public static Block oreNickel, oreChromium, oreCobalt, oreTungsten, oreMolybdenum, orePlatinum, oreAetherite;

    public static void load(){
        oreNickel = new OreBlock("ore-nickel", nickel);
        oreChromium = new OreBlock("ore-chromium", chromium);
        oreCobalt = new OreBlock("ore-cobalt", cobalt);
        oreTungsten = new OreBlock("ore-tungsten", tungsten);
        oreMolybdenum = new OreBlock("ore-molybdenum", molybdenum);
        orePlatinum = new OreBlock("ore-platinum", platinum);
        oreAetherite = new OreBlock("ore-aetherite", aetherite);
    }
}
