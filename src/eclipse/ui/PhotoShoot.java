package eclipse.ui;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.io.*;
import mindustry.maps.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;

/**
 * Developer photo mode for store-page screenshots, started by the one-shot flag file {@link #flag} in the data folder.
 * If the flag holds the path of a map (the photo scene from scene.ps1), that map is loaded; otherwise the showcase.
 * The HUD is hidden, everything runs for a while, then the camera visits each shot (kept inside the
 * map), saving eclipse-shot-*.png to the screenshots folder. An enemy wave is sent at the turret line before the
 * defense shot, the last shots are at night with lighting, the wiki gets one shot, and the game quits normally.
 */
public class PhotoShoot{
    public static final String flag = "eclipse-photo-shoot";

    /** name, anchor (block or unit name without "eclipse-", or vanilla name with "!"), camera offset in tiles, zoom, night, enemies. */
    record Shot(String name, String anchor, float dx, float dy, float zoom, boolean night, boolean enemies){
        Shot(String name, String anchor, float dx, float dy, float zoom){
            this(name, anchor, dx, dy, zoom, false, false);
        }
    }

    static final Shot[] sceneShots = {
        new Shot("overview", "!core-shard", 20, 12, 2.4f),
        new Shot("chemistry", "electrolysis-plant", 2, -4, 4.4f),
        new Shot("fission", "fission-reactor", 3, -2, 4.4f),
        new Shot("components", "component-depot", -6, -3, 4.4f),
        new Shot("smelting", "alloy-furnace", -6, 0, 4f),
        new Shot("lattice", "lattice-drill", 11, 1, 3.4f),
        new Shot("defense", "thunderhead", -4, -3, 3.4f, false, true),
        new Shot("chemistry-night", "electrolysis-plant", 2, -4, 4.4f, true, false),
        new Shot("fission-night", "fission-reactor", 3, -2, 4.4f, true, false),
        new Shot("smelting-night", "alloy-furnace", -6, 0, 4f, true, false),
        //wide night shot between the chemistry, the reactor and the smelter: the cover art is cut from its middle
        new Shot("hero-night", "fission-reactor", -4, 8, 3f, true, false),
    };

    static Shot[] showcaseShots = {
        new Shot("chemistry", "electrolysis-plant", 18, -2, 3.2f),
        new Shot("military", "piledriver", -14, 2, 3.2f),
        new Shot("lattice", "lattice-drill", 5, 1, 4f),
    };

    static final float settle = 50f, enemyLead = 60f * 6f;
    static float warmupTime = 60f * 40f;
    /** The block whose wiki page gets the last shot (art mode: the first block named). */
    static String wikiPage = "eclipse-alloy-furnace";
    static Shot[] shots = showcaseShots;
    static boolean running;
    static int index;
    static float timer;
    static int phase; //0 warmup, 1 shots, 2 wiki, 3 done
    static boolean enemiesSent;

    /** Starts the photo mode if the flag file exists (deletes it). Call on ClientLoadEvent. */
    public static boolean check(){
        Fi f = dataDirectory.child(flag);
        Log.info("[ECLIPSE] photo mode flag @: @", f.absolutePath(), f.exists() ? "found" : "absent");
        if(!f.exists()) return false;
        String path = f.readString().trim();
        f.delete();
        //first a shot of the main menu with the ECLIPSE backdrop, then the map
        Time.runTask(150f, () -> {
            save("menu");
            //"menu": only the menu shot (photos.ps1 -MenuOnly)
            if(path.equals("menu")) Core.app.exit();
            else if(path.startsWith("art:")) art(path.substring(4));
            else open(path);
        });
        return true;
    }

    static void open(String path){
        if(!path.isEmpty() && new Fi(path).exists()){
            try{
                Map map = MapIO.createMap(new Fi(path), true);
                shots = sceneShots;
                String areas = map.tags.get("eclipse-gallery");
                if(areas != null) gallery(areas);
                Log.info("[ECLIPSE] photo mode: opening the scene @", path);
                control.playMap(map, map.rules());
                begin();
                return;
            }catch(Throwable e){
                Log.err("[ECLIPSE] photo mode: could not load the scene", e);
            }
        }
        shots = showcaseShots;
        start(0);
    }

    /**
     * Art review (photos.ps1 -Art): close-ups of the named ECLIPSE blocks running on the showcase map, next to the
     * vanilla belts, sources and blocks around them, at normal gameplay zoom. Shots are named art-&lt;block&gt;.
     */
    static void art(String list){
        arc.struct.Seq<Shot> art = new arc.struct.Seq<>();
        for(String n : list.split(",")) if(!n.isBlank()) art.add(new Shot("art-" + n.trim(), n.trim(), 0f, 0f, 5f));
        shots = showcaseShots = art.toArray(Shot.class);
        if(art.any()) wikiPage = "eclipse-" + art.first().anchor();
        warmupTime = 60f * 15f;
        start(0);
    }

    /**
     * Gallery map (photos.ps1 -Gallery): one shot per area stored in the map ("name:x,y,w,h;..."), each framed to fit
     * the screen. Block status icons are hidden for the shots (the setting is restored before the game quits), and every
     * machine's inputs, heat and outputs are kept topped up so all of them run.
     */
    static void gallery(String areas){
        arc.struct.Seq<Shot> list = new arc.struct.Seq<>();
        for(String a : areas.split(";")){
            String[] kv = a.split(":");
            if(kv.length == 2) list.add(new Shot("gallery-" + kv[0], "@" + kv[1], 0f, 0f, -1f));
        }
        shots = list.toArray(Shot.class);
        galleryMode = true;
        warmupTime = 60f * 10f;
        statusBefore = Core.settings.getBool("blockstatus", true);
        Core.settings.put("blockstatus", false);
        Events.run(Trigger.update, () -> { if(galleryMode && state.isGame()) feed(); });
    }

    static boolean galleryMode, statusBefore = true;

    /** Keeps every ECLIPSE crafter running for the gallery: inputs and heat topped up, outputs taken away. */
    static void feed(){
        Groups.build.each(b -> {
            if(!b.block.name.startsWith("eclipse-") || !(b.block instanceof mindustry.world.blocks.production.GenericCrafter
                || b.block instanceof mindustry.world.blocks.production.Separator)) return;
            for(mindustry.world.consumers.Consume c : b.block.consumers){
                if(c instanceof mindustry.world.consumers.ConsumeItems ci && b.items != null){
                    for(ItemStack st : ci.items) if(b.items.get(st.item) < st.amount * 2) b.items.set(st.item, Math.min(b.block.itemCapacity, st.amount * 4));
                }else if(c instanceof mindustry.world.consumers.ConsumeLiquid cl && b.liquids != null){
                    b.liquids.set(cl.liquid, b.block.liquidCapacity * 0.8f);
                }else if(c instanceof mindustry.world.consumers.ConsumeLiquids cls && b.liquids != null){
                    for(LiquidStack st : cls.liquids) b.liquids.set(st.liquid, b.block.liquidCapacity * 0.8f);
                }
            }
            if(b.block instanceof mindustry.world.blocks.production.GenericCrafter g){
                if(g.outputItems != null && b.items != null) for(ItemStack st : g.outputItems) b.items.set(st.item, 0);
                if(g.outputLiquids != null && b.liquids != null) for(LiquidStack st : g.outputLiquids) b.liquids.set(st.liquid, 0f);
            }
            if(b.block instanceof mindustry.world.blocks.production.Separator sep && b.items != null) for(ItemStack st : sep.results) b.items.set(st.item, 0);
            if(b instanceof mindustry.world.blocks.production.HeatCrafter.HeatCrafterBuild h) h.heat = ((mindustry.world.blocks.production.HeatCrafter)b.block).heatRequirement;
        });
    }

    /** Opens the showcase once its map is registered (retried for a few seconds after startup). */
    static void start(int attempt){
        Map map = maps.all().find(m -> m.name().equals(eclipse.EclipseMod.showcaseName));
        if(map == null){
            Log.info("[ECLIPSE] photo mode: showcase map not registered yet (attempt @, @ maps known)", attempt, maps.all().size);
            if(attempt < 20) Time.runTask(30f, () -> start(attempt + 1));
            return;
        }
        Log.info("[ECLIPSE] photo mode: opening the showcase");
        control.playMap(map, map.applyRules(Gamemode.sandbox));
        begin();
    }

    static void begin(){
        running = true;
        Events.run(Trigger.update, PhotoShoot::update);
    }

    static void update(){
        if(!running || !state.isGame()) return;
        timer += Time.delta;
        ui.hudfrag.shown = phase == 2;

        if(phase == 0){
            if(timer >= warmupTime){
                phase = 1;
                timer = 0f;
            }
            return;
        }
        if(phase == 1){
            Shot s = shots[index];
            //the enemy wave needs a head start to reach the turrets
            if(s.enemies && !enemiesSent){
                enemiesSent = true;
                sendEnemies(s);
                timer = -enemyLead;
            }
            lighting(s.night);
            aim(s);
            if(timer < settle) return;
            save(s.name);
            timer = 0f;
            if(++index >= shots.length && galleryMode){
                Core.settings.put("blockstatus", statusBefore);
                galleryMode = false;
                running = false;
                Log.info("[ECLIPSE] photo mode (gallery) done");
                Core.app.exit();
                return;
            }
            if(index >= shots.length){
                lighting(false);
                phase = 2;
                EclipseWiki wiki = EclipseWiki.instance;
                Content furnace = content.getByName(ContentType.block, wikiPage);
                if(wiki != null){
                    wiki.show();
                    if(furnace instanceof UnlockableContent u) wiki.open(u, true);
                }
            }
            return;
        }
        if(phase == 2){
            if(timer < settle) return;
            save("wiki");
            if(EclipseWiki.instance != null) EclipseWiki.instance.hide();
            phase = 3;
            running = false;
            Log.info("[ECLIPSE] photo mode done: screenshots in @", screenshotDirectory.absolutePath());
            Core.app.exit();
        }
    }

    static void lighting(boolean night){
        state.rules.lighting = night;
        state.rules.ambientLight = new Color(0.02f, 0.03f, 0.08f, 0.82f);
    }

    /** Ground and air enemies south of the shot's anchor, heading north past the turrets. */
    static void sendEnemies(Shot s){
        Position at = anchor(s.anchor);
        if(at == null) return;
        UnitType[] ground = {UnitTypes.dagger, UnitTypes.dagger, UnitTypes.mace, UnitTypes.mace, UnitTypes.fortress, eclipse.content.MilitaryUnits.bulwark};
        UnitType[] air = {UnitTypes.flare, UnitTypes.flare, UnitTypes.flare, UnitTypes.horizon, UnitTypes.horizon, UnitTypes.zenith};
        Rand rand = new Rand(7);
        for(UnitType type : ground) spawn(type, at.getX() + rand.range(10f) * tilesize, at.getY() - (16f + rand.random(4f)) * tilesize);
        for(UnitType type : air) spawn(type, at.getX() + rand.range(12f) * tilesize, at.getY() - (24f + rand.random(6f)) * tilesize);
    }

    static void spawn(UnitType type, float x, float y){
        Unit u = type.create(state.rules.waveTeam);
        u.set(x, y);
        u.rotation = 90f;
        u.add();
    }

    /** Keeps the camera on a shot's anchor, inside the map (the HUD's own camera control is paused while panning). */
    static void aim(Shot s){
        if(control.input instanceof DesktopInput d) d.panning = true;
        if(s.anchor.startsWith("@")){
            String[] v = s.anchor.substring(1).split(",");
            float ax = Float.parseFloat(v[0]), ay = Float.parseFloat(v[1]), aw = Float.parseFloat(v[2]), ah = Float.parseFloat(v[3]);
            //fit the area with a one-tile border
            renderer.setScale(Math.min(Core.graphics.getWidth() / ((aw + 2f) * tilesize), Core.graphics.getHeight() / ((ah + 2f) * tilesize)));
            //centred on the area, but never past the map's edge
            Core.camera.width = Core.graphics.getWidth() / renderer.getScale();
            Core.camera.height = Core.graphics.getHeight() / renderer.getScale();
            float cx = (ax + aw / 2f) * tilesize - tilesize / 2f, cy = (ay + ah / 2f) * tilesize - tilesize / 2f;
            float halfW = Core.camera.width / 2f, halfH = Core.camera.height / 2f;
            float maxX = world.width() * tilesize - tilesize / 2f, maxY = world.height() * tilesize - tilesize / 2f;
            cx = halfW * 2f >= maxX ? maxX / 2f : Mathf.clamp(cx, halfW, maxX - halfW);
            cy = halfH * 2f >= maxY ? maxY / 2f : Mathf.clamp(cy, halfH, maxY - halfH);
            Core.camera.position.set(cx, cy);
            return;
        }
        Position at = anchor(s.anchor);
        renderer.setScale(s.zoom);
        if(at == null) return;
        float halfW = Core.camera.width / 2f, halfH = Core.camera.height / 2f;
        float x = at.getX() + s.dx * tilesize, y = at.getY() + s.dy * tilesize;
        float maxX = world.width() * tilesize - tilesize / 2f, maxY = world.height() * tilesize - tilesize / 2f;
        x = halfW * 2f >= maxX ? maxX / 2f : Mathf.clamp(x, halfW, maxX - halfW);
        y = halfH * 2f >= maxY ? maxY / 2f : Mathf.clamp(y, halfH, maxY - halfH);
        Core.camera.position.set(x, y);
    }

    static @Nullable Position anchor(String name){
        String full = name.startsWith("!") ? name.substring(1) : "eclipse-" + name;
        Unit unit = Groups.unit.find(u -> u.type.name.equals(full));
        if(unit != null) return unit;
        Building[] found = {null};
        world.tiles.eachTile(t -> {
            if(found[0] == null && t.build != null && t.isCenter() && t.build.block.name.equals(full)) found[0] = t.build;
        });
        return found[0];
    }

    static void save(String name){
        Fi file = screenshotDirectory.child("eclipse-shot-" + name + ".png");
        ScreenUtils.saveScreenshot(file);
        Log.info("[ECLIPSE] saved @", file.absolutePath());
    }
}
