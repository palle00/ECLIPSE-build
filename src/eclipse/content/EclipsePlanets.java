package eclipse.content;

import arc.graphics.*;
import arc.struct.*;
import eclipse.world.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.graphics.g3d.*;
import mindustry.type.*;
import mindustry.world.*;

import static eclipse.content.blocks.PlanetTerrain.*;

/**
 * ECLIPSE's own planets (user decisions 2026-11-18): one per enemy faction, each attacked mainly by "its" faction
 * ({@link Factions#apply}). Research is shared with Serpulo (same tech tree), sectors are generated
 * ({@link FactionPlanetGenerator}: Serpulo's layouts, reshaped per world, with each planet's own terrain from
 * {@link eclipse.content.blocks.PlanetTerrain} in place of every Serpulo floor, wall, tree and boulder), and they are
 * reached from Serpulo the way Erekir is (launch candidates).
 * - Tide: an ocean world of islands, with a spawn point at sea for the submarines.
 * - Hive: spore jungle, the Swarm's world.
 * - Ironhold: scorched industrial ground, the Dominion's world.
 */
public class EclipsePlanets{
    public static Planet tide, hive, ironhold;

    public static void load(){
        tide = planet("tide", new FactionPlanetGenerator(0.87f, -0.02f, true, ObjectMap.of(
                Blocks.deepwater, tideAbyss, Blocks.deepTaintedWater, tideAbyss, Blocks.water, tideLagoon, Blocks.taintedWater, tideLagoon,
                Blocks.sandWater, tideReefWater, Blocks.darksandWater, tideBlackSandWater, Blocks.darksandTaintedWater, tideBlackSandWater,
                Blocks.sand, tideReefSand, Blocks.darksand, tideBlackSand, Blocks.dirt, tideBlackSand, Blocks.mud, tideBlackSand, Blocks.charr, tideBlackSand,
                Blocks.moss, tideCoral, Blocks.sporeMoss, tideCoral, Blocks.grass, tideCoral,
                Blocks.stone, tideBarnacleRock, Blocks.basalt, tideBarnacleRock, Blocks.shale, tideBarnacleRock, Blocks.dacite, tideBarnacleRock, Blocks.craters, tideBarnacleRock,
                Blocks.salt, tideBarnacleRock, Blocks.snow, tideBarnacleRock, Blocks.ice, tideBarnacleRock, Blocks.iceSnow, tideBarnacleRock,
                Blocks.sandWall, tideSandstoneWall, Blocks.saltWall, tideSandstoneWall, Blocks.snowWall, tideSandstoneWall, Blocks.iceWall, tideSandstoneWall,
                Blocks.daciteWall, tideSandstoneWall, Blocks.dirtWall, tideSandstoneWall,
                Blocks.duneWall, tideBasaltWall, Blocks.stoneWall, tideBasaltWall, Blocks.shaleWall, tideBasaltWall,
                Blocks.sporeWall, tideCoralWall,
                Blocks.sporePine, tidePalm, Blocks.pine, tidePalm, Blocks.snowPine, tidePalm, Blocks.whiteTree, tidePalm, Blocks.whiteTreeDead, tidePalm, Blocks.shrubs, tidePalm,
                Blocks.boulder, tideShell, Blocks.sandBoulder, tideShell, Blocks.snowBoulder, tideShell, Blocks.basaltBoulder, tideShell,
                Blocks.shaleBoulder, tideShell, Blocks.daciteBoulder, tideShell, Blocks.sporeCluster, tideShell,
                Blocks.tar, tideOilSeep, Blocks.hotrock, tideVentRock, Blocks.magmarock, tideVent,
                Blocks.metalFloor, tideSunkenPlating, Blocks.metalFloorDamaged, tideSunkenPlating,
                Blocks.darkPanel3, tideSunkenPanel, Blocks.darkPanel4, tideSunkenPanel, Blocks.darkPanel6, tideSunkenPanel, Blocks.darkMetal, tideWreckWall
            )),
            Color.valueOf("2f6fd8"), Color.valueOf("5a8fd8"), Color.valueOf("d8eeff"), 52f);

        hive = planet("hive", new FactionPlanetGenerator(1.01f, 0.04f, false, ObjectMap.of(
                //no deep water: its spawn points can end up at sea, and the Swarm walks
                Blocks.deepwater, hiveBileShallows, Blocks.deepTaintedWater, hiveBileShallows, Blocks.water, hiveBileShallows, Blocks.taintedWater, hiveBileShallows,
                Blocks.sandWater, hiveBileShallows, Blocks.darksandWater, hiveBileShallows, Blocks.darksandTaintedWater, hiveBileShallows,
                Blocks.moss, hiveCreep, Blocks.grass, hiveCreep, Blocks.sporeMoss, hiveBroodMoss,
                Blocks.sand, hiveSporeSand, Blocks.darksand, hiveSporeSand, Blocks.dirt, hiveSporeSand, Blocks.mud, hiveSporeSand, Blocks.salt, hiveSporeSand, Blocks.charr, hiveSporeSand,
                Blocks.shale, hiveChitin, Blocks.stone, hiveChitin, Blocks.basalt, hiveChitin, Blocks.dacite, hiveChitin, Blocks.craters, hiveChitin,
                Blocks.snow, hiveChitin, Blocks.ice, hiveChitin, Blocks.iceSnow, hiveChitin,
                Blocks.sporeWall, hiveFleshWall, Blocks.stoneWall, hiveFleshWall, Blocks.sandWall, hiveFleshWall, Blocks.saltWall, hiveFleshWall,
                Blocks.snowWall, hiveFleshWall, Blocks.iceWall, hiveFleshWall, Blocks.dirtWall, hiveFleshWall,
                Blocks.duneWall, hiveChitinWall, Blocks.shaleWall, hiveChitinWall, Blocks.daciteWall, hiveChitinWall,
                Blocks.sporePine, hiveStalk, Blocks.pine, hiveStalk, Blocks.snowPine, hiveStalk, Blocks.whiteTree, hiveStalk, Blocks.whiteTreeDead, hiveStalk, Blocks.shrubs, hiveStalk,
                Blocks.boulder, hivePod, Blocks.sandBoulder, hivePod, Blocks.snowBoulder, hivePod, Blocks.basaltBoulder, hivePod,
                Blocks.shaleBoulder, hivePod, Blocks.daciteBoulder, hivePod, Blocks.sporeCluster, hivePod,
                Blocks.tar, hiveResin, Blocks.hotrock, hiveFeverFlesh, Blocks.magmarock, hiveHeart,
                Blocks.metalFloor, hiveOvergrownPlating, Blocks.metalFloorDamaged, hiveOvergrownPlating,
                Blocks.darkPanel3, hiveOvergrownPanel, Blocks.darkPanel4, hiveOvergrownPanel, Blocks.darkPanel6, hiveOvergrownPanel, Blocks.darkMetal, hiveEngulfedWall
            )),
            Color.valueOf("8a4fc0"), Color.valueOf("b37aff"), Color.valueOf("d9c2f0"), 66f);

        ironhold = planet("ironhold", new FactionPlanetGenerator(1.01f, 0.06f, false, ObjectMap.of(
                //no deep water: its spawn points can end up at sea, and the Dominion's tanks drive
                Blocks.deepwater, ironholdSludge, Blocks.deepTaintedWater, ironholdSludge, Blocks.water, ironholdSludge, Blocks.taintedWater, ironholdSludge,
                Blocks.sandWater, ironholdSludgeShallows, Blocks.darksandWater, ironholdSludgeShallows, Blocks.darksandTaintedWater, ironholdSludgeShallows,
                Blocks.sand, ironholdAsh, Blocks.darksand, ironholdAsh, Blocks.dirt, ironholdAsh, Blocks.mud, ironholdAsh, Blocks.charr, ironholdAsh,
                Blocks.grass, ironholdAsh, Blocks.moss, ironholdAsh, Blocks.sporeMoss, ironholdAsh,
                Blocks.basalt, ironholdScorchedRock, Blocks.dacite, ironholdScorchedRock, Blocks.shale, ironholdScorchedRock, Blocks.craters, ironholdScorchedRock,
                Blocks.snow, ironholdScorchedRock, Blocks.ice, ironholdScorchedRock, Blocks.iceSnow, ironholdScorchedRock,
                Blocks.salt, ironholdPlating, Blocks.stone, ironholdPlating,
                Blocks.duneWall, ironholdSlagWall, Blocks.sandWall, ironholdSlagWall, Blocks.saltWall, ironholdSlagWall, Blocks.daciteWall, ironholdSlagWall, Blocks.dirtWall, ironholdSlagWall,
                Blocks.sporeWall, ironholdScrapWall, Blocks.shaleWall, ironholdScrapWall, Blocks.stoneWall, ironholdScrapWall, Blocks.snowWall, ironholdScrapWall, Blocks.iceWall, ironholdScrapWall,
                Blocks.sporePine, ironholdScrapWall, Blocks.pine, ironholdScrapWall, Blocks.snowPine, ironholdScrapWall, Blocks.whiteTree, ironholdScrapWall,
                Blocks.whiteTreeDead, ironholdScrapWall, Blocks.shrubs, ironholdScrapWall,
                Blocks.boulder, ironholdScrap, Blocks.sandBoulder, ironholdScrap, Blocks.snowBoulder, ironholdScrap, Blocks.basaltBoulder, ironholdScrap,
                Blocks.shaleBoulder, ironholdScrap, Blocks.daciteBoulder, ironholdScrap, Blocks.sporeCluster, ironholdScrap,
                Blocks.tar, ironholdTar, Blocks.hotrock, ironholdCinder, Blocks.magmarock, ironholdMagma,
                Blocks.metalFloor, ironholdPlating, Blocks.metalFloorDamaged, ironholdPlating,
                Blocks.darkPanel3, ironholdDarkPlating, Blocks.darkPanel4, ironholdDarkPlating, Blocks.darkPanel6, ironholdDarkPlating, Blocks.darkMetal, ironholdBulwarkWall
            )),
            Color.valueOf("c0603a"), Color.valueOf("e0683a"), Color.valueOf("8a8a92"), 80f);
    }

    static Planet planet(String name, FactionPlanetGenerator gen, Color atmosphereTint, Color iconTint, Color cloudTint, float orbit){
        Planet p = new Planet(name, Planets.sun, 1f, 3){{
            generator = gen; //(parameters are named *Tint: Planet has its own icon and color fields that would shadow them)
            meshLoader = () -> new HexMesh(this, 6);
            cloudMeshLoader = () -> new MultiMesh(
                new HexSkyMesh(this, 11, 0.15f, 0.13f, 5, cloudTint.cpy().a(0.75f), 2, 0.45f, 0.9f, 0.38f),
                new HexSkyMesh(this, 1, 0.6f, 0.16f, 5, Color.white.cpy().lerp(cloudTint, 0.4f).a(0.75f), 2, 0.45f, 1f, 0.41f)
            );
            atmosphereColor = atmosphereTint;
            atmosphereRadIn = 0.02f;
            atmosphereRadOut = 0.3f;
            iconColor = iconTint;
            landCloudColor = cloudTint;
            orbitRadius = orbit;
            //selectable on the planet map from the start, like Serpulo and Erekir (the map only offers unlocked planets,
            //planets you already hold a sector on, or launch targets)
            alwaysUnlocked = true;
            startSector = 15;
            allowLaunchToNumbered = true;
            allowLaunchSchematics = true;
            allowLaunchLoadout = true;
            allowSectorInvasion = true;
            allowWaves = true;
            defaultCore = Blocks.coreShard;
            //shared research: the Serpulo tree (vanilla + ECLIPSE) counts here too
            techTree = Planets.serpulo.techTree;
            ruleSetter = r -> {
                r.waveTeam = Team.crux;
                r.placeRangeCheck = false;
            };
            launchCandidates.add(Planets.serpulo);
        }};
        Planets.serpulo.launchCandidates.add(p);
        //Serpulo content is marked as shown on Serpulo only (that is how Erekir hides it); the shared tree must show it here too
        Planets.serpulo.techTree.addPlanet(p);
        return p;
    }
}
