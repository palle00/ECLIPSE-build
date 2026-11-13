package eclipse.defense;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * Wave Disruption Array: tap and activate to add {@link #delay} ticks to the countdown of the wave that is coming.
 * Rules (defence spec §6, §8):
 * - only before the wave spawns, only on timed-wave maps, only when fully charged and powered;
 * - one shared team-wide lockout of {@link #lockWaves} waves, kept in the map rules (saved and sent to clients),
 *   so several arrays can never stack;
 * - only the server changes the countdown; clients receive it through the normal state sync;
 * - wave spacing, waitEnemies and wave contents are never touched; manually sending a wave ignores the delay.
 */
public class WaveDisruptionArray extends Block{
    /** Shared with any later wave-delay device (Temporal Interference Array). */
    public static final String lockTag = "eclipse-wave-delay-wave";
    public static final Stat delayStat = new Stat("eclipse-wave-delay", StatCat.function);

    public float delay = 20f * 60f;
    public int lockWaves = 2;
    /** Ticks of full power needed to charge for one activation. */
    public float chargeTime = 30f * 60f;
    public Color fieldColor = Color.valueOf("c88fff");

    public WaveDisruptionArray(String name){
        super(name);
        update = true;
        solid = true;
        configurable = true;
        hasPower = true;
        sync = true;
        config(Boolean.class, (DisruptionBuild b, Boolean on) -> b.activate());
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(delayStat, Core.bundle.format("stat.eclipse-wave-delay-value", (int)(delay / 60f), lockWaves));
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("eclipse-charge", (DisruptionBuild e) -> new Bar(
            () -> Core.bundle.format("bar.eclipse-array-charge", (int)(e.charge * 100)),
            () -> fieldColor,
            () -> e.charge));
    }

    /** The wave number from which a delay may be used again (team-wide, saved in the map rules). */
    public static int unlockWave(){
        String v = state.rules.tags.get(lockTag);
        return v == null ? 0 : Strings.parseInt(v, 0);
    }

    public class DisruptionBuild extends Building{
        public float charge;

        /** Why the array can't be activated right now, or null if it can. */
        public @Nullable String blocker(){
            if(!WaveInfo.timedWaves()) return Core.bundle.get("eclipse-array.no-timer");
            if(state.gameOver || logic.isWaitingWave() || spawner.isSpawning() || state.wavetime < 60f) return Core.bundle.get("eclipse-array.wave-started");
            if(efficiency <= 0f) return Core.bundle.get("eclipse-array.no-power");
            if(charge < 1f) return Core.bundle.format("eclipse-array.charging", (int)(charge * 100));
            if(state.wave < unlockWave()) return Core.bundle.format("eclipse-array.locked", unlockWave());
            return null;
        }

        @Override
        public void updateTile(){
            if(charge < 1f) charge = Math.min(1f, charge + efficiency * delta() / chargeTime);
        }

        void activate(){
            if(blocker() != null) return;
            charge = 0f;
            //the countdown and the lockout are server state; clients get both through the normal sync
            if(net.client()) return;
            state.wavetime += delay;
            state.rules.tags.put(lockTag, String.valueOf(state.wave + lockWaves));
            if(net.server()) Call.setRules(state.rules);
        }

        @Override
        public void buildConfiguration(Table table){
            table.background(Styles.black6);
            table.margin(6f);
            table.label(() -> {
                String b = blocker();
                return b == null ? Core.bundle.format("eclipse-array.ready", (int)(delay / 60f)) : "[lightgray]" + b;
            }).minWidth(240f).row();
            //the message is shown only to the player who pressed the button, not to everyone the config reaches
            table.button(Core.bundle.get("eclipse-array.activate"), Styles.flatt, () -> {
                if(blocker() != null) return;
                configure(true);
                ui.showInfoToast(Core.bundle.format("eclipse-array.activated", (int)(delay / 60f)), 3f);
            })
                .disabled(b -> blocker() != null).growX().height(44f).padTop(4f);
        }

        @Override
        public void draw(){
            super.draw();
            if(charge >= 1f && efficiency > 0f){
                Draw.z(Layer.effect);
                Draw.color(fieldColor, 0.25f + Mathf.absin(Time.time, 10f, 0.25f));
                Fill.circle(x, y, 3f);
                Draw.reset();
            }
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(charge);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1) charge = read.f();
        }
    }
}
