package eclipse.ui;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.graphics.*;
import mindustry.mod.Mods.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

/**
 * The startup loading screen in ECLIPSE's look, from the moment ECLIPSE itself is loaded (the first moments, before
 * mods load, stay the game's own): the menu's grid and logo, the eclipse where the menu has it, forming as loading
 * goes on (the sun at 0%, the moon almost over it at 100%), and a thin gold progress bar under the logo. The menu's
 * intro then goes on from there: the diamond ring, the corona, the menu fading in.
 * <p>
 * It takes the place of the game's load renderer (a private field of the launcher); off with the ECLIPSE menu
 * backdrop, and back to the game's own screen if anything fails.
 */
public class EclipseLoading extends LoadRenderer{
    /** Set once this screen has drawn: the menu's intro goes on from where it ended. */
    static boolean drawn;

    static final Color track = Color.valueOf("282b36");

    @Nullable TextureRegion logo, glow;
    /** Frames spent looking for the logo: ECLIPSE joins the game's mod list a little after this screen starts. */
    int logoTries;
    float progress;
    boolean failed;
    /** Photo mode (menu review): two shots of this screen, at about a third and near the end. */
    boolean review;
    int shots;

    /** Swaps the game's loading screen for this one. Call from the mod's constructor (during loading, off the render thread). */
    public static void install(){
        if(headless) return;
        Core.app.post(() -> {
            try{
                if(Core.settings != null && !Core.settings.getBool(MenuBackdrop.setting, true)) return;
                ApplicationListener launcher = Core.app.getListeners().find(l -> l instanceof ClientLauncher);
                if(launcher == null) return;
                LoadRenderer old = Reflect.get(ClientLauncher.class, launcher, "loader");
                if(old == null) return; //loading is already done
                Reflect.set(ClientLauncher.class, launcher, "loader", new EclipseLoading());
                old.dispose();
            }catch(Throwable e){
                Log.err("[ECLIPSE] loading screen: keeping the game's", e);
            }
        });
    }

    EclipseLoading(){
        try{
            Fi flag = dataDirectory.child(PhotoShoot.flag);
            review = flag.exists() && flag.readString().trim().equals("menu-review");
        }catch(Throwable ignored){
        }
    }

    static @Nullable TextureRegion region(Fi file){
        if(!file.exists()) return null;
        Texture t = new Texture(file);
        t.setFilter(Texture.TextureFilter.nearest);
        return new TextureRegion(t);
    }

    @Override
    public void draw(){
        if(failed){
            super.draw();
            return;
        }
        try{
            float w = Core.graphics.getWidth(), h = Core.graphics.getHeight(), px = Scl.scl(1f);
            progress += (Core.assets.getProgress() - progress) * 0.12f;
            if(logo == null && logoTries++ < 1200){
                LoadedMod mod = mods.getMod("eclipse");
                if(mod != null){
                    logo = region(mod.root.child("sprites/logo.png"));
                    glow = region(mod.root.child("sprites/logo-glow.png"));
                    logoTries = 1200;
                }
            }
            Core.graphics.clear(MenuBackdrop.background);
            Draw.proj(0f, 0f, w, h);
            MenuBackdrop.gridLines(0f, 0f, w, h);
            //the eclipse forming with the loading; at 100% the moon is where the menu's intro picks it up (1 s in)
            MenuBackdrop.loadingScreen = true;
            try{
                MenuBackdrop.drawIntro(MenuBackdrop.eclipseX(w, h), MenuBackdrop.eclipseY(w, h), MenuBackdrop.eclipseR(w, h), Time.globalTime, progress, px);
            }finally{
                MenuBackdrop.loadingScreen = false;
            }
            MenuBackdrop.atmosphere(0f, 0f, w, h);
            float[] at = MenuBackdrop.logoPlace(w, h);
            float lw = at[2], lh = logo == null ? lw * 0.194f : lw * logo.height / logo.width, left = at[0] - lw / 2f, below = at[1] - lh / 2f;
            if(logo != null) MenuBackdrop.drawLogo(logo, glow, at[0], at[1], lw, lh, 1f);
            //a thin gold bar under the logo, where the menu's buttons will be
            float barY = below - 52f * px, barH = 4f * px;
            Draw.color(track);
            Fill.crect(left, barY, lw, barH);
            Draw.color(MenuBackdrop.gold);
            Fill.crect(left, barY, lw * Math.min(progress, 1f), barH);
            Draw.reset();
            Draw.flush();
            drawn = true;
            if(review && (shots == 0 && progress > 0.3f || shots == 1 && progress > 0.85f)){
                ScreenUtils.saveScreenshot(dataDirectory.child("screenshots").child("eclipse-shot-loading-" + (++shots) + ".png"));
            }
        }catch(Throwable e){
            failed = true;
            Log.err("[ECLIPSE] loading screen failed, back to the game's", e);
        }
    }

    @Override
    public void dispose(){
        super.dispose();
        if(logo != null) logo.texture.dispose();
        if(glow != null) glow.texture.dispose();
    }
}
