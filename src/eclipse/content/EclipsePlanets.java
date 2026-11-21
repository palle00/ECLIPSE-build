package eclipse.content;

import arc.graphics.*;
import arc.struct.*;
import eclipse.world.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.graphics.g3d.*;
import mindustry.type.*;
import mindustry.world.*;

/**
 * ECLIPSE's own planets (user decisions 2026-11-18): one per enemy faction, each attacked mainly by "its" faction
 * ({@link Factions#apply}). Research is shared with Serpulo (same tech tree), sectors are generated
 * ({@link FactionPlanetGenerator}: Serpulo's layouts, reshaped per world), and they are reached from Serpulo the way
 * Erekir is (launch candidates).
 * - Tide: an ocean world of islands, with a spawn point at sea for the submarines.
 * - Hive: spore jungle, the Swarm's world.
 * - Ironhold: scorched industrial ground, the Dominion's world.
 */
public class EclipsePlanets{
    public static Planet tide, hive, ironhold;

    public static void load(){
        tide = planet("tide", new FactionPlanetGenerator(0.87f, -0.02f, true, new ObjectMap<>(), Color.clear, 0f),
            Color.valueOf("2f6fd8"), Color.valueOf("5a8fd8"), Color.valueOf("d8eeff"), 52f);

        hive = planet("hive", new FactionPlanetGenerator(1.01f, 0.04f, false, ObjectMap.of(
                Blocks.stone, Blocks.moss, Blocks.basalt, Blocks.moss, Blocks.dacite, Blocks.moss, Blocks.salt, Blocks.moss,
                Blocks.grass, Blocks.sporeMoss, Blocks.snow, Blocks.sporeMoss, Blocks.iceSnow, Blocks.sporeMoss, Blocks.ice, Blocks.sporeMoss,
                //no deep water: its spawn points can end up at sea, and the Swarm walks
                Blocks.water, Blocks.darksandTaintedWater, Blocks.deepwater, Blocks.darksandTaintedWater, Blocks.taintedWater, Blocks.darksandTaintedWater, Blocks.deepTaintedWater, Blocks.darksandTaintedWater, Blocks.sandWater, Blocks.darksandTaintedWater,
                Blocks.stoneWall, Blocks.sporeWall, Blocks.sandWall, Blocks.sporeWall, Blocks.daciteWall, Blocks.sporeWall, Blocks.saltWall, Blocks.sporeWall,
                Blocks.iceWall, Blocks.sporeWall, Blocks.snowWall, Blocks.sporePine, Blocks.pine, Blocks.sporePine
            ), Color.valueOf("7a4fa0"), 0.35f),
            Color.valueOf("8a4fc0"), Color.valueOf("b37aff"), Color.valueOf("d9c2f0"), 66f);

        ironhold = planet("ironhold", new FactionPlanetGenerator(1.01f, 0.06f, false, ObjectMap.of(
                Blocks.stone, Blocks.metalFloorDamaged, Blocks.salt, Blocks.metalFloor, Blocks.dacite, Blocks.basalt,
                Blocks.grass, Blocks.darksand, Blocks.sand, Blocks.darksand, Blocks.moss, Blocks.darksand, Blocks.sporeMoss, Blocks.darksand,
                Blocks.snow, Blocks.basalt, Blocks.iceSnow, Blocks.basalt, Blocks.ice, Blocks.basalt,
                //no deep water: its spawn points can end up at sea, and the Dominion's tanks drive
                Blocks.deepwater, Blocks.water, Blocks.deepTaintedWater, Blocks.taintedWater,
                Blocks.stoneWall, Blocks.duneWall, Blocks.sandWall, Blocks.duneWall, Blocks.daciteWall, Blocks.duneWall, Blocks.saltWall, Blocks.duneWall,
                Blocks.iceWall, Blocks.duneWall, Blocks.snowWall, Blocks.duneWall, Blocks.sporeWall, Blocks.duneWall, Blocks.pine, Blocks.duneWall, Blocks.sporePine, Blocks.duneWall
            ), Color.valueOf("4a4a52"), 0.4f),
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
