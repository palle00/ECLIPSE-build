package eclipse.defense;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

/**
 * Jammer: a powered support block that pulses every few seconds and jams enemy units in range,
 * slowing their fire rate for a short time. Bounded on purpose: fixed radius and interval, the status
 * refreshes rather than stacks, and bosses are jammed for half as long. It never touches enemy buildings.
 */
public class Jammer extends Block{
    public float range = 120f, interval = 180f, duration = 240f, bossFactor = 0.5f;
    public StatusEffect status;
    public Color color = Color.valueOf("b37aff");
    public TextureRegion dishRegion, glowRegion;

    public static final Effect pulse = new Effect(45f, 400f, e -> {
        Draw.color(e.color, e.fout() * 0.8f);
        Lines.stroke(3f * e.fout());
        Lines.circle(e.x, e.y, e.rotation * e.fin(Interp.pow2Out));
    });

    public Jammer(String name){
        super(name);
        update = true;
        solid = true;
        hasPower = true;
        group = BlockGroup.turrets;
        envEnabled |= Env.space;
    }

    @Override
    public void load(){
        super.load();
        dishRegion = Core.atlas.find(name + "-dish");
        glowRegion = Core.atlas.find(name + "-glow");
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.range, range / 8f, StatUnit.blocks);
        stats.add(Stat.reload, 60f / interval, StatUnit.perSecond);
        stats.add(Stat.status, StatValues.statusEffects(arc.struct.Seq.with(status)));
        stats.add(Stat.cooldownTime, duration / 60f, StatUnit.seconds);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        Drawf.dashCircle(x * 8f + offset, y * 8f + offset, range, color);
    }

    @Override
    protected TextureRegion[] icons(){
        return new TextureRegion[]{region, dishRegion};
    }

    public class JammerBuild extends Building{
        public float charge, spin, flash;
        /** Enemy units jammed by the last pulse (for tests and the status display). */
        public int lastJammed;

        @Override
        public void updateTile(){
            spin += efficiency * delta() * 2f;
            flash = Mathf.approachDelta(flash, 0f, 1f / 30f);
            if(efficiency <= 0f) return;
            charge += delta() * efficiency;
            if(charge < interval) return;
            charge = 0f;
            flash = 1f;
            int[] n = {0};
            Units.nearbyEnemies(team, x, y, range, u -> {
                u.apply(status, u.hasEffect(StatusEffects.boss) ? duration * bossFactor : duration);
                n[0]++;
            });
            lastJammed = n[0];
            pulse.at(x, y, range, color);
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);
            Draw.rect(dishRegion, x, y, spin);
            if(flash > 0f){
                Draw.blend(Blending.additive);
                Draw.color(color, flash);
                Draw.rect(glowRegion, x, y, spin);
                Draw.blend();
                Draw.reset();
            }
        }

        @Override
        public void drawSelect(){
            Drawf.dashCircle(x, y, range, color);
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(charge);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            charge = read.f();
        }
    }
}
