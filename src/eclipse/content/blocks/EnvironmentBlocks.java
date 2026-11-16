package eclipse.content.blocks;

import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static eclipse.content.EclipseItems.*;

/** Ores. Generated on new Serpulo sectors (see EclipseSerpuloGenerator) and placeable in the map editor. */
public class EnvironmentBlocks{
    public static Block oreNickel, oreChromium, oreCobalt, oreTungsten, oreMolybdenum, orePlatinum, oreAetherite, oreBauxite;

    public static void load(){
        oreNickel = new OreBlock("ore-nickel", pentlandite); //nickel ore
        oreChromium = new OreBlock("ore-chromium", chromite); //chromium ore
        oreCobalt = new OreBlock("ore-cobalt", cobaltite); //cobalt ore
        oreTungsten = new OreBlock("ore-tungsten", wolframite); //tungsten ore
        oreMolybdenum = new OreBlock("ore-molybdenum", molybdenite); //molybdenum ore
        orePlatinum = new OreBlock("ore-platinum", sperrylite); //platinum ore
        oreAetherite = new OreBlock("ore-aetherite", aetherite);
        oreBauxite = new OreBlock("ore-bauxite", bauxite);
    }
}
