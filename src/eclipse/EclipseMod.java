package eclipse;

import arc.*;
import arc.files.*;
import arc.util.*;
import eclipse.augment.*;
import eclipse.content.*;
import eclipse.content.blocks.*;
import eclipse.systems.*;
import eclipse.ui.*;
import eclipse.world.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.maps.*;
import mindustry.mod.*;

import static mindustry.Vars.*;

public class EclipseMod extends Mod{
    public static final String showcaseName = "ECLIPSE Showcase";

    public EclipseMod(){
        Corrosion.init();
        Augmentation.init();
        TurretStats.init(); //after Augmentation, so its extra laser damage is counted
        eclipse.ui.MachineStatus.install();
        Milestones.init();
        eclipse.robotics.RobotAI.registerEvents();
        //Upgrade Section key and hooks (desktop): registered early so the Controls menu lists the key
        eclipse.input.UpgradePlanner.init();
        eclipse.ui.PlacementHints.init(); //liquid output arrows and heat preview while placing (desktop)
        eclipse.ui.EclipseWiki.registerKey(); //I over an ECLIPSE block opens its wiki page
        //custom games on premade Serpulo maps get ECLIPSE ores too (before the renderer caches the floor)
        Events.on(WorldLoadEndEvent.class, e -> EclipseSerpuloGenerator.addToCustomMap());

        Events.on(ClientLoadEvent.class, e -> {
            Log.info("[ECLIPSE] client loaded");
            CompactCoreDisplay.install();
            EclipseWiki.install();
            MenuBackdrop.install();
        });

        //developer convenience: showcase.ps1 -Open drops this one-shot flag to jump straight into the showcase map
        Events.on(ClientLoadEvent.class, e -> {
            //photos.ps1: store-page screenshots of the showcase, then the game quits by itself
            if(PhotoShoot.check()) return;
            Fi flag = dataDirectory.child("eclipse-open-showcase");
            if(!flag.exists()) return;
            flag.delete();
            Map map = maps.all().find(m -> m.name().equals(showcaseName));
            if(map != null) control.playMap(map, map.applyRules(Gamemode.sandbox));
        });
    }

    @Override
    public void loadContent(){
        EclipseItems.load();
        EclipseLiquids.load();
        eclipse.exploration.Biomes.load(); //before the blocks that use its floor attribute
        EnvironmentBlocks.load();
        ComponentBlocks.load();
        ComponentBlocks.patchVanilla();
        ComponentBlocks.loadDepot();
        CoreRules.patchVanillaCores();
        ChemicalBlocks.load();
        PetrochemicalBlocks.load();
        BiologyBlocks.load();
        RefiningBlocks.load();
        DefenseBlocks.load();
        ExplorationBlocks.load();
        RoboticsContent.load();
        EndgameBlocks.load();
        MetallurgyBlocks.load();
        ElectronicsBlocks.load();
        NuclearBlocks.load();
        PowerBlocks.load();
        TransportBlocks.load();
        TransportBlocks.patchVanilla(); //junction and bridges keep up with the fast ECLIPSE belts
        LogisticsBlocks.load();
        AugmentContent.load();
        Munitions.load(); //military M1: ECLIPSE ammunition for vanilla turrets
        MilitaryBlocks.load(); //military M2: new turrets
        MilitaryUnits.load(); //military M3: tank and strike unit lines in the vanilla factories
        Factions.load(); //military M4: Swarm and Dominion join the waves of new custom games
        MilitaryEndgame.load(); //military M5: titan complex, orbital platform, EW drone
        EclipseTechTree.load();
        Animations.load(); //after every crafter exists: product-coloured glow and turning parts while working
        Polish.load(); //after Animations: working sounds, lights and effects by kind of machine
        MenuOrder.load(); //last: places ECLIPSE content next to its vanilla counterparts in the menus

        //new procedurally generated Serpulo sectors also get ECLIPSE ores
        Planets.serpulo.generator = new EclipseSerpuloGenerator();
    }
}
