package eclipse.world;

import arc.util.*;
import mindustry.content.*;
import mindustry.world.*;

import static mindustry.Vars.*;

/**
 * ECLIPSE on Erekir (user, 2026-11-25: Erekir must work in 1.0; plan and decisions in EREKIR_SUPPORT.md, option C).
 * <ul>
 * <li>Research and content: ECLIPSE and the Serpulo base it builds on are in Serpulo's research tree, which is now also
 * shown on Erekir (as on ECLIPSE's own planets). Research is shared; Erekir's research screen can switch to that tree.</li>
 * <li>Resources: Erekir has no copper, lead, titanium, coal or scrap and none of ECLIPSE's ores, and its campaign sectors
 * are mostly hand-made maps. So the first time any Erekir map loads (campaign sector or custom game), those ores are
 * scattered on open ground, rarer further from the core, once per map (a rules tag).</li>
 * <li>The factions join Erekir's waves, tougher there (Factions.local); tungsten and carbide are vanilla's (EclipseItems).</li>
 * </ul>
 */
public class ErekirSupport{
    /** Rules tag: this map already got its ores. */
    public static final String oresTag = "eclipse-erekir-ores";

    /** Shows the Serpulo tree (vanilla Serpulo and ECLIPSE) on Erekir. Call after the tech tree and the planets exist. */
    public static void load(){
        Planets.serpulo.techTree.addPlanet(Planets.erekir);
    }

    /** The Serpulo base ores the ECLIPSE chains need, in the EclipseSerpuloGenerator.eclipseOres() format. */
    public static Object[][] baseOres(){
        return new Object[][]{
            {Blocks.oreCopper, 28f, 0.81f, 0.78f, 0.00f},
            {Blocks.oreLead, 28f, 0.82f, 0.79f, 0.00f},
            {Blocks.oreCoal, 24f, 0.85f, 0.81f, 0.08f},
            {Blocks.oreScrap, 22f, 0.87f, 0.83f, 0.05f},
            {Blocks.oreTitanium, 24f, 0.86f, 0.81f, 0.15f},
            {Blocks.oreThorium, 18f, 0.90f, 0.86f, 0.40f},
        };
    }

    /** Erekir maps get their ores on their first load. Call on WorldLoadEndEvent (server or single player). */
    public static void addOres(){
        if(net.client() || state.rules.editor || state.rules.planet != Planets.erekir || state.rules.tags.containsKey(oresTag)) return;
        int seed = world.width() * 7919 + world.height() + (state.rules.sector == null ? 0 : state.rules.sector.id * 31);
        Object[][] base = baseOres(), eclipse = EclipseSerpuloGenerator.eclipseOres();
        Object[][] all = new Object[base.length + eclipse.length][];
        System.arraycopy(base, 0, all, 0, base.length);
        System.arraycopy(eclipse, 0, all, base.length, eclipse.length);
        EclipseSerpuloGenerator.addOres(world.tiles, seed, true, all);
        state.rules.tags.put(oresTag, "1");
        int placed = 0;
        for(Tile t : world.tiles) if(t.overlay() != Blocks.air && t.overlay() instanceof mindustry.world.blocks.environment.OreBlock) placed++;
        Log.info("[ECLIPSE] Erekir map: Serpulo and ECLIPSE ores added (@ ore tiles in total)", placed);
    }
}
