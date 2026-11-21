package eclipse.ui;

import arc.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;

/**
 * A one-time welcome shown a few seconds into the first game after ECLIPSE is installed (after the deployment intro):
 * where the wiki is, the keys as currently bound, and a button to the Getting Started guide. Shown once per install
 * (setting {@link #shownKey}); not in the editor or while photos.ps1 is taking screenshots.
 */
public class WelcomeHint{
    public static final String shownKey = "eclipse-welcome-shown";

    public static void install(){
        Events.on(WorldLoadEvent.class, e -> {
            if(Core.settings.getBool(shownKey, false)) return;
            //after the deployment intro (7 s), so it doesn't cover the landing
            Time.run(60f * 8.5f, WelcomeHint::show);
        });
    }

    static void show(){
        if(Core.settings.getBool(shownKey, false) || !state.isGame() || state.rules.editor || PhotoShoot.running) return;
        Core.settings.put(shownKey, true);
        BaseDialog dialog = new BaseDialog("@eclipse-welcome.title");
        String text = mobile ? Core.bundle.get("eclipse-welcome.text.mobile")
            : Core.bundle.format("eclipse-welcome.text", EclipseWiki.keyName(EclipseWiki.entryKey), EclipseWiki.keyName(StallOverlay.key),
                EclipseWiki.keyName(ProductionStats.key), EclipseWiki.keyName(eclipse.input.UpgradePlanner.key));
        dialog.cont.add(text).wrap().width(Math.min(Core.graphics.getWidth() / arc.scene.ui.layout.Scl.scl(1f) - 60f, 560f)).left();
        dialog.buttons.defaults().size(240f, 54f).pad(4f);
        dialog.buttons.button("@eclipse-welcome.start", Icon.book, () -> {
            dialog.hide();
            if(EclipseWiki.instance == null) return;
            EclipseWiki.instance.show();
            EclipseWiki.instance.open("start", true);
        });
        dialog.buttons.button("@eclipse-welcome.close", Icon.ok, dialog::hide);
        dialog.closeOnBack();
        dialog.show();
    }
}
